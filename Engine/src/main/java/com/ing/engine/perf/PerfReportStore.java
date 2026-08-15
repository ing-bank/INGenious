package com.ing.engine.perf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reads persisted performance runs ({@code Results/Performance/<name>/<ts>/}):
 * run metadata ({@code run.json}) and headline metrics from the k6
 * {@code summary.json} export.
 */
public final class PerfReportStore {

    private PerfReportStore() {}

    /** Newest run folder across all scripts, or null when nothing ran yet. */
    public static File latestRunDir(PerfWorkspace workspace) {
        List<File> runs = workspace.listRuns();
        return runs.isEmpty() ? null : runs.get(0);
    }

    /** run.json as a JsonNode (null when missing/unreadable). */
    public static JsonNode runMeta(File runDir) {
        try {
            File meta = new File(runDir, "run.json");
            return meta.isFile() ? new ObjectMapper().readTree(meta) : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Numeric key metrics for comparisons: iterations, requests, rps,
     * errorRate (0..1), avg, p95, max (ms). Missing metrics are absent.
     */
    public static Map<String, Double> numericHeadline(File runDir) {
        Map<String, Double> out = new LinkedHashMap<>();
        File summaryFile = new File(runDir, "summary.json");
        if (!summaryFile.isFile()) {
            return out;
        }
        JsonNode metrics;
        try {
            metrics = new ObjectMapper().readTree(summaryFile).path("metrics");
        } catch (Exception e) {
            return out;
        }
        if (metrics.path("iterations").has("count")) {
            out.put("iterations", metrics.path("iterations").path("count").asDouble());
        }
        if (metrics.path("http_reqs").has("count")) {
            out.put("requests", metrics.path("http_reqs").path("count").asDouble());
        }
        if (metrics.path("http_reqs").has("rate")) {
            out.put("rps", metrics.path("http_reqs").path("rate").asDouble());
        }
        if (metrics.path("http_req_failed").has("value")) {
            out.put("errorRate", metrics.path("http_req_failed").path("value").asDouble());
        }
        JsonNode duration = metrics.path("http_req_duration");
        if (duration.has("avg")) {
            out.put("avg", duration.path("avg").asDouble());
        }
        if (duration.has("p(95)")) {
            out.put("p95", duration.path("p(95)").asDouble());
        }
        if (duration.has("max")) {
            out.put("max", duration.path("max").asDouble());
        }
        return out;
    }

    /** One comparison row: metric, baseline, candidate, delta. */
    public static final class CompareRow {
        public final String metric;
        public final double baseline;
        public final double candidate;
        /** Percentage change vs baseline (0 when baseline is 0). */
        public final double deltaPercent;
        /** True when this metric got worse (higher latency / error rate). */
        public final boolean regression;

        CompareRow(String metric, double baseline, double candidate) {
            this.metric = metric;
            this.baseline = baseline;
            this.candidate = candidate;
            this.deltaPercent = baseline == 0 ? 0 : ((candidate - baseline) / baseline) * 100.0;
            boolean higherIsWorse =
                "errorRate".equals(metric) ||
                "avg".equals(metric) ||
                "p95".equals(metric) ||
                "max".equals(metric);
            this.regression = higherIsWorse && candidate > baseline * 1.05; // 5% tolerance
        }
    }

    /**
     * Compare two runs metric by metric; also flags thresholds that passed
     * in the baseline but fail in the candidate (returned via
     * {@code thresholdRegressions}).
     */
    public static List<CompareRow> compare(
        File baselineRun,
        File candidateRun,
        List<String> thresholdRegressions
    ) {
        Map<String, Double> a = numericHeadline(baselineRun);
        Map<String, Double> b = numericHeadline(candidateRun);
        List<CompareRow> rows = new java.util.ArrayList<>();
        for (Map.Entry<String, Double> e : a.entrySet()) {
            Double candidate = b.get(e.getKey());
            if (candidate != null) {
                rows.add(
                    new CompareRow(e.getKey(), e.getValue().doubleValue(), candidate.doubleValue())
                );
            }
        }
        Map<String, Boolean> baseThresholds = thresholds(baselineRun);
        Map<String, Boolean> candidateThresholds = thresholds(candidateRun);
        for (Map.Entry<String, Boolean> e : candidateThresholds.entrySet()) {
            Boolean was = baseThresholds.get(e.getKey());
            if (Boolean.TRUE.equals(was) && !e.getValue().booleanValue()) {
                thresholdRegressions.add(e.getKey());
            }
        }
        return rows;
    }

    /**
     * JUnit XML for a run's thresholds (one testcase per threshold) — lets CI
     * systems fail performance gates natively.
     */
    public static String toJUnitXml(File runDir, String suiteName) {
        Map<String, Boolean> thresholds = thresholds(runDir);
        StringBuilder xml = new StringBuilder();
        int failures = 0;
        for (Boolean ok : thresholds.values()) {
            if (!ok.booleanValue()) {
                failures++;
            }
        }
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml
            .append("<testsuite name=\"")
            .append(escapeXml(suiteName))
            .append("\" tests=\"")
            .append(thresholds.size())
            .append("\" failures=\"")
            .append(failures)
            .append("\">\n");
        for (Map.Entry<String, Boolean> e : thresholds.entrySet()) {
            xml
                .append("  <testcase classname=\"k6.thresholds\" name=\"")
                .append(escapeXml(e.getKey()))
                .append("\"");
            if (e.getValue().booleanValue()) {
                xml.append("/>\n");
            } else {
                xml.append(">\n    <failure message=\"threshold crossed\"/>\n  </testcase>\n");
            }
        }
        xml.append("</testsuite>\n");
        return xml.toString();
    }

    private static String escapeXml(String s) {
        return s
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }

    /**
     * Headline metrics from summary.json in display order:
     * iterations, http_reqs, error rate, duration avg/p95/max, checks,
     * data sent/received. Missing metrics are simply absent.
     */
    public static Map<String, String> headline(File runDir) {
        Map<String, String> out = new LinkedHashMap<>();
        File summaryFile = new File(runDir, "summary.json");
        if (!summaryFile.isFile()) {
            return out;
        }
        JsonNode summary;
        try {
            summary = new ObjectMapper().readTree(summaryFile);
        } catch (Exception e) {
            return out;
        }
        JsonNode metrics = summary.path("metrics");
        putCount(out, "iterations", metrics.path("iterations"), "count");
        putCount(out, "http requests", metrics.path("http_reqs"), "count");
        JsonNode failed = metrics.path("http_req_failed");
        if (failed.has("value")) {
            out.put(
                "error rate",
                String.format(Locale.ROOT, "%.2f%%", failed.get("value").asDouble() * 100)
            );
        }
        JsonNode duration = metrics.path("http_req_duration");
        putMillis(out, "duration avg", duration, "avg");
        putMillis(out, "duration p95", duration, "p(95)");
        putMillis(out, "duration max", duration, "max");
        JsonNode checks = metrics.path("checks");
        if (checks.has("passes") || checks.has("fails")) {
            out.put(
                "checks",
                checks.path("passes").asLong(0) +
                " passed / " +
                checks.path("fails").asLong(0) +
                " failed"
            );
        }
        putBytes(out, "data sent", metrics.path("data_sent"), "count");
        putBytes(out, "data received", metrics.path("data_received"), "count");
        return out;
    }

    /** All thresholds and whether each passed (from summary.json). */
    public static Map<String, Boolean> thresholds(File runDir) {
        Map<String, Boolean> out = new LinkedHashMap<>();
        File summaryFile = new File(runDir, "summary.json");
        if (!summaryFile.isFile()) {
            return out;
        }
        try {
            JsonNode metrics = new ObjectMapper().readTree(summaryFile).path("metrics");
            Iterator<String> names = metrics.fieldNames();
            while (names.hasNext()) {
                String metric = names.next();
                JsonNode th = metrics.get(metric).path("thresholds");
                if (!th.isObject()) {
                    continue;
                }
                Iterator<String> exprs = th.fieldNames();
                while (exprs.hasNext()) {
                    String expr = exprs.next();
                    // --summary-export emits plain booleans where TRUE means
                    // "threshold crossed" (failed); end-of-test summary objects
                    // use {"ok": bool} instead. Handle both.
                    JsonNode v = th.get(expr);
                    boolean ok = v.isBoolean() ? !v.asBoolean() : v.path("ok").asBoolean(false);
                    out.put(metric + ": " + expr, ok);
                }
            }
        } catch (Exception e) {
            // unreadable summary -> empty map
        }
        return out;
    }

    private static void putCount(Map<String, String> out, String label, JsonNode m, String field) {
        if (m.has(field)) {
            out.put(label, String.valueOf(m.get(field).asLong()));
        }
    }

    private static void putMillis(Map<String, String> out, String label, JsonNode m, String field) {
        if (m.has(field)) {
            out.put(label, String.format(Locale.ROOT, "%.2f ms", m.get(field).asDouble()));
        }
    }

    private static void putBytes(Map<String, String> out, String label, JsonNode m, String field) {
        if (!m.has(field)) {
            return;
        }
        double bytes = m.get(field).asDouble();
        String formatted;
        if (bytes >= 1024 * 1024) {
            formatted = String.format(Locale.ROOT, "%.1f MB", bytes / (1024 * 1024));
        } else if (bytes >= 1024) {
            formatted = String.format(Locale.ROOT, "%.1f kB", bytes / 1024);
        } else {
            formatted = String.format(Locale.ROOT, "%.0f B", bytes);
        }
        out.put(label, formatted);
    }
}

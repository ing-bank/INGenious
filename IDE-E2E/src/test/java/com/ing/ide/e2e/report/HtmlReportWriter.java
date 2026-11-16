package com.ing.ide.e2e.report;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Writes a self-contained {@code report.html} in the run folder: run summary, then each scenario
 * with its test cases, results, failure details and evidence.
 */
public final class HtmlReportWriter implements ReportWriter {
    /** File name of the report inside the run folder. */
    public static final String FILE_NAME = "report.html";

    private static final String STYLE = String.join(
        "",
        "body{font-family:-apple-system,Segoe UI,Roboto,sans-serif;margin:0;background:#f5f5f7;color:#222}",
        "header{background:#4b0082;color:#fff;padding:20px 32px}",
        "header h1{margin:0 0 8px;font-size:22px}",
        ".meta{font-size:13px;opacity:.9}",
        ".meta span{margin-right:24px}",
        "main{padding:24px 32px}",
        ".summary{display:flex;gap:12px;margin-bottom:24px}",
        ".tile{background:#fff;border-radius:8px;padding:12px 20px;box-shadow:0 1px 3px #0002;min-width:90px}",
        ".tile b{display:block;font-size:24px}",
        "section.scenario{background:#fff;border-radius:8px;box-shadow:0 1px 3px #0002;margin-bottom:24px}",
        "section.scenario>h2{margin:0;padding:14px 20px;font-size:17px;border-bottom:1px solid #eee}",
        ".code{font-family:Menlo,Consolas,monospace;color:#4b0082;margin-right:8px}",
        ".testcase{padding:14px 20px;border-bottom:1px solid #f0f0f0}",
        ".testcase h3{margin:0 0 6px;font-size:15px}",
        ".info{font-size:12px;color:#666;margin-bottom:8px}",
        ".status{display:inline-block;padding:2px 10px;border-radius:10px;font-size:12px;font-weight:600;color:#fff;margin-left:8px}",
        ".PASSED{background:#2e7d32}.FAILED{background:#c62828}.SKIPPED{background:#9e9e9e}",
        "details pre{background:#fff4f4;border:1px solid #f3c6c6;padding:10px;overflow:auto;font-size:12px}",
        ".evidence{display:flex;flex-wrap:wrap;gap:12px;margin-top:8px}",
        "figure{margin:0;width:240px}",
        "figure img,figure video{width:240px;border:1px solid #ddd;border-radius:4px;background:#fafafa}",
        "figure.recording,figure.recording video{width:640px}",
        "figcaption{font-size:11px;color:#555;word-break:break-all}"
    );

    @Override
    public String format() {
        return "html";
    }

    @Override
    public void write(final Path runDir, final RunInfo run, final List<TestCaseResult> results)
        throws IOException {
        Files.writeString(runDir.resolve(FILE_NAME), render(run, results), StandardCharsets.UTF_8);
    }

    private static String render(final RunInfo run, final List<TestCaseResult> results) {
        final StringBuilder html = new StringBuilder(8192);
        html
            .append("<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\">")
            .append("<title>INGenious UI Test Report - ")
            .append(esc(run.runId()))
            .append("</title><style>")
            .append(STYLE)
            .append("</style></head><body>");
        appendHeader(html, run);
        html.append("<main>");
        appendSummary(html, results);
        groupByScenario(results).forEach((scenario, testCases) -> appendScenario(html, testCases));
        html.append("</main></body></html>\n");
        return html.toString();
    }

    private static void appendHeader(final StringBuilder html, final RunInfo run) {
        html
            .append("<header><h1>INGenious UI Test Report</h1><div class=\"meta\">")
            .append(meta("Run", run.runId()))
            .append(meta("INGenious", run.ideVersion()))
            .append(meta("Input mode", run.inputMode()))
            .append(meta("OS", run.os()))
            .append(meta("Java", run.java()))
            .append(meta("Generated", run.generatedAt()))
            .append("</div></header>");
    }

    private static void appendSummary(
        final StringBuilder html,
        final List<TestCaseResult> results
    ) {
        html.append("<div class=\"summary\">").append(tile("Total", results.size()));
        for (final TestStatus status : TestStatus.values()) {
            html.append(
                tile(status.name(), results.stream().filter(r -> r.status() == status).count())
            );
        }
        html.append("</div>");
    }

    private static void appendScenario(
        final StringBuilder html,
        final List<TestCaseResult> testCases
    ) {
        final TestIdentity scenario = testCases.get(0).identity();
        html
            .append("<section class=\"scenario\"><h2><span class=\"code\">")
            .append(esc(scenario.scenarioCode()))
            .append("</span>")
            .append(esc(scenario.scenarioDescription()))
            .append(badge(scenarioStatus(testCases)))
            .append("</h2>");
        testCases.forEach(testCase -> appendTestCase(html, testCase));
        html.append("</section>");
    }

    private static void appendTestCase(final StringBuilder html, final TestCaseResult testCase) {
        html
            .append("<div class=\"testcase\"><h3><span class=\"code\">")
            .append(esc(testCase.identity().testCaseCode()))
            .append("</span>")
            .append(esc(testCase.identity().testCaseDescription()))
            .append(badge(testCase.status()))
            .append("</h3><div class=\"info\">Started ")
            .append(esc(testCase.startedAt()))
            .append(" &middot; ")
            .append(String.format("%.1f s", testCase.durationMs() / 1000.0))
            .append("</div>");
        if (testCase.message() != null) {
            html
                .append("<details")
                .append(testCase.status() == TestStatus.FAILED ? " open" : "")
                .append("><summary>Details</summary><pre>")
                .append(esc(testCase.message()))
                .append("</pre></details>");
        }
        html.append("<div class=\"evidence\">");
        testCase.evidence().forEach(evidence -> appendEvidence(html, evidence));
        html.append("</div></div>");
    }

    private static void appendEvidence(final StringBuilder html, final Evidence evidence) {
        final String src = esc(evidence.path());
        html.append(evidence.isRecording() ? "<figure class=\"recording\">" : "<figure>");
        if (evidence.isRecording()) {
            html
                .append("<video controls preload=\"metadata\" src=\"")
                .append(src)
                .append("\"></video>");
        } else {
            html
                .append("<a href=\"")
                .append(src)
                .append("\" target=\"_blank\"><img loading=\"lazy\" src=\"")
                .append(src)
                .append("\" alt=\"")
                .append(esc(evidence.title()))
                .append("\"></a>");
        }
        html.append("<figcaption>").append(esc(evidence.title()));
        if (evidence.isRecording()) {
            // Fallback for viewers without H.264 support (e.g. some embedded browsers)
            html
                .append(" &middot; <a href=\"")
                .append(src)
                .append("\" target=\"_blank\">open recording</a>");
        }
        html.append("</figcaption></figure>");
    }

    private static Map<String, List<TestCaseResult>> groupByScenario(
        final List<TestCaseResult> results
    ) {
        return results
            .stream()
            .collect(
                Collectors.groupingBy(
                    r -> r.identity().scenarioCode(),
                    LinkedHashMap::new,
                    Collectors.toList()
                )
            );
    }

    private static TestStatus scenarioStatus(final List<TestCaseResult> testCases) {
        if (testCases.stream().anyMatch(t -> t.status() == TestStatus.FAILED)) {
            return TestStatus.FAILED;
        }
        if (testCases.stream().allMatch(t -> t.status() == TestStatus.SKIPPED)) {
            return TestStatus.SKIPPED;
        }
        return TestStatus.PASSED;
    }

    private static String badge(final TestStatus status) {
        return "<span class=\"status " + status.name() + "\">" + status.name() + "</span>";
    }

    private static String tile(final String label, final long count) {
        return "<div class=\"tile\"><b>" + count + "</b>" + esc(label) + "</div>";
    }

    private static String meta(final String label, final String value) {
        return "<span>" + esc(label) + ": <b>" + esc(value) + "</b></span>";
    }

    private static String esc(final String text) {
        if (text == null) {
            return "";
        }
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }
}

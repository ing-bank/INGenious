package com.ing.engine.perf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Live metrics source for a running k6 process, backed by k6's REST API
 * ({@code k6 run --address 127.0.0.1:<port>}).
 *
 * <p>Endpoints used:
 * <ul>
 *   <li>{@code GET /v1/status} — vus, paused, running, tainted</li>
 *   <li>{@code GET /v1/metrics} — counters/trends/rates/gauges</li>
 *   <li>{@code PATCH /v1/status} — stop / pause / scale VUs</li>
 * </ul>
 */
public final class K6MetricsTap {
    private static final HttpClient HTTP = HttpClient
        .newBuilder()
        .connectTimeout(Duration.ofSeconds(2))
        .build();

    private K6MetricsTap() {}

    /** True when the k6 REST API answers on the port. */
    public static boolean isUp(int apiPort) {
        return status(apiPort) != null;
    }

    /** Raw /v1/status attributes, or null when unreachable. */
    public static JsonNode status(int apiPort) {
        JsonNode root = get(apiPort, "/v1/status");
        return root == null ? null : root.path("data").path("attributes");
    }

    /**
     * Compact live snapshot in display order: vus, iterations, rps, p95,
     * avg, error rate, data. Empty map when the API is unreachable
     * (run finished or not yet started).
     */
    public static Map<String, String> snapshot(int apiPort) {
        Map<String, String> out = new LinkedHashMap<>();
        JsonNode status = status(apiPort);
        if (status == null) {
            return out;
        }
        out.put("vus", String.valueOf(status.path("vus").asInt()));
        if (status.path("paused").asBoolean(false)) {
            out.put("paused", "true");
        }
        JsonNode metrics = get(apiPort, "/v1/metrics");
        if (metrics == null) {
            return out;
        }
        for (JsonNode m : metrics.path("data")) {
            String id = m.path("id").asText("");
            JsonNode sample = m.path("attributes").path("sample");
            switch (id) {
                case "iterations":
                    out.put("iterations", String.valueOf(sample.path("count").asLong()));
                    break;
                case "http_reqs":
                    out.put(
                        "rps",
                        String.format(java.util.Locale.ROOT, "%.1f", sample.path("rate").asDouble())
                    );
                    break;
                case "http_req_duration":
                    out.put(
                        "p95",
                        String.format(
                            java.util.Locale.ROOT,
                            "%.1f ms",
                            sample.path("p(95)").asDouble()
                        )
                    );
                    out.put(
                        "avg",
                        String.format(
                            java.util.Locale.ROOT,
                            "%.1f ms",
                            sample.path("avg").asDouble()
                        )
                    );
                    break;
                case "http_req_failed":
                    out.put(
                        "errorRate",
                        String.format(
                            java.util.Locale.ROOT,
                            "%.2f%%",
                            sample.path("rate").asDouble() * 100
                        )
                    );
                    break;
                case "checks":
                    out.put(
                        "checksOk",
                        String.format(
                            java.util.Locale.ROOT,
                            "%.1f%%",
                            sample.path("rate").asDouble() * 100
                        )
                    );
                    break;
                default:
                // other metrics not part of the headline snapshot
            }
        }
        return out;
    }

    /**
     * Numeric snapshot for charting: vus, iterations, rps, p95, avg,
     * errorRate (0..1). Missing values are absent.
     */
    public static Map<String, Double> numericSnapshot(int apiPort) {
        Map<String, Double> out = new LinkedHashMap<>();
        JsonNode status = status(apiPort);
        if (status == null) {
            return out;
        }
        out.put("vus", (double) status.path("vus").asInt());
        JsonNode metrics = get(apiPort, "/v1/metrics");
        if (metrics == null) {
            return out;
        }
        for (JsonNode m : metrics.path("data")) {
            String id = m.path("id").asText("");
            JsonNode sample = m.path("attributes").path("sample");
            switch (id) {
                case "iterations":
                    out.put("iterations", sample.path("count").asDouble());
                    break;
                case "http_reqs":
                    out.put("rps", sample.path("rate").asDouble());
                    break;
                case "http_req_duration":
                    out.put("p95", sample.path("p(95)").asDouble());
                    out.put("avg", sample.path("avg").asDouble());
                    break;
                case "http_req_failed":
                    out.put("errorRate", sample.path("rate").asDouble());
                    break;
                default:
                // not charted
            }
        }
        return out;
    }

    /** Ask k6 to stop the run gracefully. Returns true when acknowledged. */
    public static boolean stop(int apiPort) {
        return patchStatus(apiPort, "{\"stopped\": true}");
    }

    /** Pause / resume the run. */
    public static boolean pause(int apiPort, boolean paused) {
        return patchStatus(apiPort, "{\"paused\": " + paused + "}");
    }

    /**
     * Scale the running test to the given number of VUs (also raises
     * vus-max so scaling up works). Returns false when k6 rejects it
     * (some executors do not support external scaling).
     */
    public static boolean scale(int apiPort, int vus) {
        return patchStatus(apiPort, "{\"vus\": " + vus + ", \"vus-max\": " + vus + "}");
    }

    // ------------------------------------------------------------------
    // internals
    // ------------------------------------------------------------------

    private static JsonNode get(int apiPort, String path) {
        try {
            HttpRequest request = HttpRequest
                .newBuilder(URI.create("http://127.0.0.1:" + apiPort + path))
                .timeout(Duration.ofSeconds(2))
                .GET()
                .build();
            HttpResponse<String> response = HTTP.send(
                request,
                HttpResponse.BodyHandlers.ofString()
            );
            if (response.statusCode() != 200) {
                return null;
            }
            return new ObjectMapper().readTree(response.body());
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean patchStatus(int apiPort, String attributes) {
        try {
            String body =
                "{\"data\": {\"type\": \"status\", \"id\": \"default\", \"attributes\": " +
                attributes +
                "}}";
            HttpRequest request = HttpRequest
                .newBuilder(URI.create("http://127.0.0.1:" + apiPort + "/v1/status"))
                .timeout(Duration.ofSeconds(3))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
                .build();
            HttpResponse<String> response = HTTP.send(
                request,
                HttpResponse.BodyHandlers.ofString()
            );
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception e) {
            return false;
        }
    }
}

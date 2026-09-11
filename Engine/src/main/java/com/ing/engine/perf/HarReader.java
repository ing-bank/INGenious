package com.ing.engine.perf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Reads HAR (HTTP Archive) recordings into {@link HttpRequestSpec}s suitable
 * for k6 script generation.
 *
 * <p>Filtering mirrors k6-studio's generator defaults:
 * <ul>
 *   <li>static assets (images, fonts, stylesheets, scripts, media) are dropped</li>
 *   <li>OPTIONS preflights are dropped</li>
 *   <li>hop-by-hop / auto-computed headers are dropped</li>
 *   <li>credential headers (Authorization, Cookie) are scrubbed with a warning
 *       — correlation rules (Phase 6) are the sanctioned way to re-inject them</li>
 * </ul>
 */
public final class HarReader {

    /** Parse result: requests plus generation warnings. */
    public static final class Result {
        public final List<HttpRequestSpec> requests = new ArrayList<>();
        public final List<String> warnings = new ArrayList<>();
    }

    private static final String[] STATIC_EXTENSIONS = {
        ".css",
        ".js",
        ".mjs",
        ".map",
        ".png",
        ".jpg",
        ".jpeg",
        ".gif",
        ".webp",
        ".svg",
        ".ico",
        ".woff",
        ".woff2",
        ".ttf",
        ".otf",
        ".eot",
        ".mp4",
        ".webm",
        ".mp3",
        ".wav"
    };

    private static final String[] STATIC_MIME_PREFIXES = {
        "image/",
        "font/",
        "audio/",
        "video/",
        "text/css",
        "application/javascript",
        "text/javascript",
        "application/font"
    };

    /** Headers never worth replaying (auto-computed or hop-by-hop). */
    private static final String[] DROPPED_HEADERS = {
        "content-length",
        "host",
        "connection",
        "accept-encoding",
        "upgrade-insecure-requests",
        "sec-ch-ua",
        "sec-ch-ua-mobile",
        "sec-ch-ua-platform",
        "sec-fetch-site",
        "sec-fetch-mode",
        "sec-fetch-dest",
        "sec-fetch-user",
        "pragma",
        "if-none-match",
        "if-modified-since"
    };

    /** Credential headers scrubbed by default (see class doc). */
    private static final String[] SCRUBBED_HEADERS = { "authorization", "cookie" };

    private HarReader() {}

    /**
     * @param harFile         the .har file
     * @param urlFilter       optional substring filter on the URL (null = all)
     * @param includeStatic   when true, static assets are kept
     */
    public static Result read(File harFile, String urlFilter, boolean includeStatic)
        throws Exception {
        JsonNode har = new ObjectMapper().readTree(harFile);
        JsonNode entries = har.path("log").path("entries");
        if (!entries.isArray()) {
            throw new IllegalArgumentException("No log.entries array in HAR file: " + harFile);
        }
        Result result = new Result();
        boolean scrubbed = false;
        for (JsonNode entry : entries) {
            JsonNode req = entry.path("request");
            String method = req.path("method").asText("GET").toLowerCase(Locale.ROOT);
            String url = req.path("url").asText("");
            if (url.isEmpty()) {
                continue;
            }
            if (urlFilter != null && !url.contains(urlFilter)) {
                continue;
            }
            if ("options".equals(method)) {
                continue;
            }
            String mime = entry.path("response").path("content").path("mimeType").asText("");
            if (!includeStatic && isStaticAsset(url, mime)) {
                continue;
            }
            HttpRequestSpec spec = new HttpRequestSpec();
            spec.method = method;
            spec.url = url;
            spec.name = method.toUpperCase(Locale.ROOT) + " " + pathOf(url);
            String body = req.path("postData").path("text").asText("");
            if (!body.isEmpty()) {
                spec.body = body;
            }
            for (JsonNode h : req.path("headers")) {
                String name = h.path("name").asText("");
                String value = h.path("value").asText("");
                if (name.isEmpty() || name.startsWith(":")) {
                    continue; // HTTP/2 pseudo-headers
                }
                String lower = name.toLowerCase(Locale.ROOT);
                if (contains(DROPPED_HEADERS, lower)) {
                    continue;
                }
                if (contains(SCRUBBED_HEADERS, lower)) {
                    spec.comments.add(
                        "TODO: header '" +
                        name +
                        "' was scrubbed (credentials); re-inject via a correlation rule or env var"
                    );
                    // analysis-only copy so auto-correlation can still see it
                    spec.scrubbedHeaders.add(new String[] { name, value });
                    scrubbed = true;
                    continue;
                }
                spec.headers.add(new String[] { name, value });
            }
            int status = entry.path("response").path("status").asInt(0);
            if (status > 0) {
                spec.checkStatus = status;
            }
            String responseText = entry.path("response").path("content").path("text").asText("");
            if (!responseText.isEmpty()) {
                spec.recordedResponseBody = responseText;
            }
            result.requests.add(spec);
        }
        if (scrubbed) {
            result.warnings.add(
                "Credential headers (Authorization/Cookie) were scrubbed from the recording; the script may need auth wiring."
            );
        }
        if (result.requests.isEmpty()) {
            result.warnings.add(
                "No requests matched" +
                (urlFilter == null ? "" : " urlFilter '" + urlFilter + "'") +
                (includeStatic ? "" : " (static assets are filtered by default)")
            );
        }
        return result;
    }

    static boolean isStaticAsset(String url, String mimeType) {
        String path = pathOf(url).toLowerCase(Locale.ROOT);
        for (String ext : STATIC_EXTENSIONS) {
            if (path.endsWith(ext)) {
                return true;
            }
        }
        String mime = mimeType == null ? "" : mimeType.toLowerCase(Locale.ROOT);
        for (String prefix : STATIC_MIME_PREFIXES) {
            if (mime.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /** Path + query of a URL (falls back to the raw string on parse issues). */
    static String pathOf(String url) {
        try {
            java.net.URI uri = java.net.URI.create(url);
            String path = uri.getPath();
            if (path == null || path.isEmpty()) {
                path = "/";
            }
            return path;
        } catch (Exception e) {
            return url;
        }
    }

    private static boolean contains(String[] haystack, String needle) {
        for (String h : haystack) {
            if (h.equals(needle)) {
                return true;
            }
        }
        return false;
    }
}

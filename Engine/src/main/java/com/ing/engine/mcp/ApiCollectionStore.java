package com.ing.engine.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.ing.datalib.api.APICollection;
import com.ing.datalib.api.APIEnvironment;
import com.ing.datalib.api.APIRequest;
import com.ing.datalib.api.APIResponse;
import com.ing.datalib.api.AuthConfig;
import com.ing.datalib.api.KeyValuePair;
import com.ing.datalib.api.RequestBody;
import com.ing.datalib.api.importer.NormalizedCollection;
import com.ing.datalib.api.importer.NormalizedRequest;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Headless persistence + execution helpers for the API collection-first workflow.
 *
 * <p>Reads/writes the same on-disk layout as the IDE API tester
 * ({@code <project>/api/collections/*.json}, {@code api/environments/*.json},
 * {@code api/history/*.json}) using plain Jackson, and executes {@link APIRequest}s
 * with the JDK {@link HttpClient}. Kept in the Engine module so the MCP tools can use
 * it without depending on the IDE.</p>
 */
public final class ApiCollectionStore {
    private static final Pattern VAR = Pattern.compile("\\{\\{([^}]+)\\}\\}");

    private ApiCollectionStore() {}

    private static ObjectMapper mapper() {
        ObjectMapper m = new ObjectMapper();
        m.enable(SerializationFeature.INDENT_OUTPUT);
        return m;
    }

    // ------------------------------------------------------------------
    // paths
    // ------------------------------------------------------------------

    public static File apiDir(File projectDir) {
        return new File(projectDir, "api");
    }

    public static File collectionsDir(File projectDir) {
        return new File(apiDir(projectDir), "collections");
    }

    public static File environmentsDir(File projectDir) {
        return new File(apiDir(projectDir), "environments");
    }

    public static File historyDir(File projectDir) {
        return new File(apiDir(projectDir), "history");
    }

    public static String sanitize(String name) {
        if (name == null || name.isEmpty()) return "unnamed";
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    // ------------------------------------------------------------------
    // collection persistence
    // ------------------------------------------------------------------

    public static List<APICollection> listCollections(File projectDir) {
        List<APICollection> out = new ArrayList<>();
        File dir = collectionsDir(projectDir);
        File[] files = dir.listFiles((d, n) -> n.toLowerCase().endsWith(".json"));
        if (files == null) return out;
        ObjectMapper m = mapper();
        for (File f : files) {
            try {
                out.add(m.readValue(f, APICollection.class));
            } catch (IOException ignored) {}
        }
        return out;
    }

    public static APICollection loadCollection(File projectDir, String name) {
        File byFile = new File(collectionsDir(projectDir), sanitize(name) + ".json");
        ObjectMapper m = mapper();
        if (byFile.isFile()) {
            try {
                return m.readValue(byFile, APICollection.class);
            } catch (IOException ignored) {}
        }
        for (APICollection c : listCollections(projectDir)) {
            if (c.getName() != null && c.getName().equalsIgnoreCase(name)) return c;
        }
        return null;
    }

    public static void saveCollection(File projectDir, APICollection collection) {
        File dir = collectionsDir(projectDir);
        dir.mkdirs();
        File f = new File(dir, sanitize(collection.getName()) + ".json");
        try {
            mapper().writeValue(f, collection);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save collection: " + e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------
    // environment persistence
    // ------------------------------------------------------------------

    public static APIEnvironment loadEnvironment(File projectDir, String name) {
        if (name == null) return null;
        File byFile = new File(environmentsDir(projectDir), sanitize(name) + ".json");
        ObjectMapper m = mapper();
        if (byFile.isFile()) {
            try {
                return m.readValue(byFile, APIEnvironment.class);
            } catch (IOException ignored) {}
        }
        File dir = environmentsDir(projectDir);
        File[] files = dir.listFiles((d, n) -> n.toLowerCase().endsWith(".json"));
        if (files != null) {
            for (File f : files) {
                try {
                    APIEnvironment e = m.readValue(f, APIEnvironment.class);
                    if (e.getName() != null && e.getName().equalsIgnoreCase(name)) return e;
                } catch (IOException ignored) {}
            }
        }
        return null;
    }

    public static void saveEnvironment(File projectDir, APIEnvironment env) {
        File dir = environmentsDir(projectDir);
        dir.mkdirs();
        File f = new File(dir, sanitize(env.getName()) + ".json");
        try {
            mapper().writeValue(f, env);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save environment: " + e.getMessage(), e);
        }
    }

    public static List<String> listEnvironmentNames(File projectDir) {
        List<String> out = new ArrayList<>();
        File dir = environmentsDir(projectDir);
        File[] files = dir.listFiles((d, n) -> n.toLowerCase().endsWith(".json"));
        if (files == null) return out;
        ObjectMapper m = mapper();
        for (File f : files) {
            try {
                out.add(m.readValue(f, APIEnvironment.class).getName());
            } catch (IOException ignored) {}
        }
        return out;
    }

    // ------------------------------------------------------------------
    // import conversion
    // ------------------------------------------------------------------

    /** Flatten a parsed collection into a persistable {@link APICollection}. */
    public static APICollection fromNormalized(NormalizedCollection nc, String name) {
        APICollection c = new APICollection(name != null ? name : nc.getName());
        if (nc.getDescription() != null) c.setDescription(nc.getDescription());
        List<APIRequest> reqs = new ArrayList<>();
        for (NormalizedRequest nr : nc.getRequests()) {
            if (nr != null && nr.getRequest() != null) reqs.add(nr.getRequest());
        }
        c.setRequests(reqs);
        return c;
    }

    // ------------------------------------------------------------------
    // execution
    // ------------------------------------------------------------------

    public static String substitute(String s, Map<String, String> vars) {
        if (s == null || vars == null || vars.isEmpty()) return s;
        Matcher mm = VAR.matcher(s);
        StringBuffer sb = new StringBuffer();
        while (mm.find()) {
            String key = mm.group(1).trim();
            String val = vars.getOrDefault(key, mm.group(0));
            mm.appendReplacement(sb, Matcher.quoteReplacement(val));
        }
        mm.appendTail(sb);
        return sb.toString();
    }

    /** Execute a single request against the given variable set; never throws. */
    public static APIResponse execute(APIRequest req, Map<String, String> vars) {
        long start = System.currentTimeMillis();
        try {
            String url = substitute(req.getUrl(), vars);
            List<String> query = new ArrayList<>();
            if (req.getQueryParams() != null) {
                for (KeyValuePair kv : req.getQueryParams()) {
                    if (kv == null || !kv.isEnabled() || kv.getKey() == null) continue;
                    query.add(enc(kv.getKey()) + "=" + enc(substitute(kv.getValue(), vars)));
                }
            }
            AuthConfig auth = req.getAuth();
            Map<String, String> headers = new LinkedHashMap<>();
            if (req.getHeaders() != null) {
                for (KeyValuePair kv : req.getHeaders()) {
                    if (kv == null || !kv.isEnabled() || kv.getKey() == null) continue;
                    headers.put(kv.getKey(), substitute(kv.getValue(), vars));
                }
            }
            applyAuth(auth, headers, query, vars);

            if (!query.isEmpty()) {
                url = url + (url.contains("?") ? "&" : "?") + String.join("&", query);
            }

            HttpRequest.BodyPublisher body = HttpRequest.BodyPublishers.noBody();
            RequestBody rb = req.getBody();
            String method = req.getMethod() == null ? "GET" : req.getMethod().name();
            if (
                rb != null &&
                rb.getBodyType() == RequestBody.BodyType.RAW &&
                rb.getRawContent() != null
            ) {
                body = HttpRequest.BodyPublishers.ofString(substitute(rb.getRawContent(), vars));
                headers.putIfAbsent("Content-Type", rb.getContentType());
            } else if (rb != null && rb.getBodyType() == RequestBody.BodyType.URL_ENCODED) {
                List<String> form = new ArrayList<>();
                if (rb.getUrlEncodedData() != null) {
                    for (KeyValuePair kv : rb.getUrlEncodedData()) {
                        if (kv == null || !kv.isEnabled() || kv.getKey() == null) continue;
                        form.add(enc(kv.getKey()) + "=" + enc(substitute(kv.getValue(), vars)));
                    }
                }
                body = HttpRequest.BodyPublishers.ofString(String.join("&", form));
                headers.putIfAbsent("Content-Type", "application/x-www-form-urlencoded");
            }

            HttpRequest.Builder b = HttpRequest
                .newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(req.getTimeout() > 0 ? req.getTimeout() : 30000))
                .method(method, body);
            for (Map.Entry<String, String> h : headers.entrySet()) {
                if (h.getValue() != null) b.header(h.getKey(), h.getValue());
            }

            HttpClient client = HttpClient
                .newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .followRedirects(
                    req.isFollowRedirects() ? HttpClient.Redirect.NORMAL : HttpClient.Redirect.NEVER
                )
                .build();
            HttpResponse<String> resp = client.send(
                b.build(),
                HttpResponse.BodyHandlers.ofString()
            );
            long elapsed = System.currentTimeMillis() - start;
            Map<String, List<String>> respHeaders = new LinkedHashMap<>(resp.headers().map());
            return new APIResponse(resp.statusCode(), resp.body(), respHeaders, elapsed);
        } catch (Exception e) {
            APIResponse err = APIResponse.error(e.getMessage());
            err.setResponseTimeMs(System.currentTimeMillis() - start);
            return err;
        }
    }

    private static void applyAuth(
        AuthConfig auth,
        Map<String, String> headers,
        List<String> query,
        Map<String, String> vars
    ) {
        if (auth == null || auth.getAuthType() == null) return;
        switch (auth.getAuthType()) {
            case BASIC:
                String user = substitute(auth.getBasicUsername(), vars);
                String pass = substitute(auth.getBasicPassword(), vars);
                String token = Base64
                    .getEncoder()
                    .encodeToString(
                        ((user == null ? "" : user) + ":" + (pass == null ? "" : pass)).getBytes(
                                StandardCharsets.UTF_8
                            )
                    );
                headers.put("Authorization", "Basic " + token);
                break;
            case BEARER:
                String prefix = auth.getBearerPrefix() == null ? "Bearer" : auth.getBearerPrefix();
                headers.put(
                    "Authorization",
                    prefix + " " + substitute(auth.getBearerToken(), vars)
                );
                break;
            case API_KEY:
                String name = auth.getApiKeyName();
                String value = substitute(auth.getApiKeyValue(), vars);
                if (name == null) break;
                if (auth.getApiKeyLocation() == AuthConfig.ApiKeyLocation.QUERY_PARAM) {
                    query.add(enc(name) + "=" + enc(value));
                } else {
                    headers.put(name, value);
                }
                break;
            default:
                break;
        }
    }

    private static String enc(String s) {
        return java.net.URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }
}

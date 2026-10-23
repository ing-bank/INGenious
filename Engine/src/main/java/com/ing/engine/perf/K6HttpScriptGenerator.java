package com.ing.engine.perf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ing.datalib.component.Project;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.component.TestStep;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Generates k6 HTTP (protocol-level) scripts from INGenious API test cases
 * or pre-extracted {@link HttpRequestSpec} lists (HAR path).
 *
 * <p>Test-case translation follows the engine's real Webservice semantics
 * (see com.ing.engine.commands.webservice.Webservice / GeneralWebservice):
 * <ul>
 *   <li>state (endpoint, headers, url params) is tracked per step OBJECT name
 *       (the "connection key")</li>
 *   <li>{@code addHeader} input is {@code Name=Value}; headers are consumed
 *       and cleared by the next request on the same key</li>
 *   <li>the payload of POST/PUT/PATCH/deleteWithPayload is the request step's
 *       own Input</li>
 *   <li>{@code assertResponseCode} checks the response of its key's last
 *       request</li>
 * </ul>
 *
 * <p>Unsupported actions are never silently dropped: they surface as
 * {@code // TODO} comments in the script plus warnings in the result.
 */
public final class K6HttpScriptGenerator {

    /** Extraction outcome: ordered requests plus generation warnings. */
    public static final class Result {
        public final List<HttpRequestSpec> requests = new ArrayList<>();
        public final List<String> warnings = new ArrayList<>();
    }

    private K6HttpScriptGenerator() {}

    // ==================================================================
    // Test case -> requests
    // ==================================================================

    public static Result fromTestCase(Project project, TestCase testCase) {
        Result result = new Result();
        Map<String, ConnectionState> connections = new HashMap<>();
        Map<String, HttpRequestSpec> lastRequestPerKey = new HashMap<>();
        for (TestStep step : TestCaseFlattener.flatten(project, testCase, result.warnings)) {
            handleStep(
                safe(step.getAction()),
                safe(step.getObject()),
                safe(step.getInput()),
                connections,
                lastRequestPerKey,
                result
            );
        }
        if (result.requests.isEmpty()) {
            result.warnings.add(
                "No API request steps (setEndPoint + *RestRequest) found in the test case."
            );
        }
        return result;
    }

    /** Mutable per-connection-key builder state. */
    private static final class ConnectionState {
        String endpoint;
        final List<String[]> headers = new ArrayList<>();
        final List<String> urlParams = new ArrayList<>();
    }

    private static void handleStep(
        String action,
        String object,
        String input,
        Map<String, ConnectionState> connections,
        Map<String, HttpRequestSpec> lastRequestPerKey,
        Result result
    ) {
        ConnectionState conn = connections.computeIfAbsent(object, k -> new ConnectionState());
        String lower = action.toLowerCase(Locale.ROOT);
        switch (lower) {
            case "setendpoint":
                conn.endpoint = resolveInput(input, result, action);
                return;
            case "addheader":
                {
                    String header = resolveInput(input, result, action);
                    int eq = header.indexOf('=');
                    if (eq <= 0) {
                        result.warnings.add("addHeader input is not Name=Value, skipped: " + input);
                        return;
                    }
                    conn.headers.add(
                        new String[] { header.substring(0, eq), header.substring(eq + 1) }
                    );
                    return;
                }
            case "addurlparam":
                conn.urlParams.add(resolveInput(input, result, action));
                return;
            case "getrestrequest":
                emitRequest("get", object, null, conn, lastRequestPerKey, result);
                return;
            case "postrestrequest":
            case "postsoaprequest":
                emitRequest(
                    "post",
                    object,
                    resolveInput(input, result, action),
                    conn,
                    lastRequestPerKey,
                    result
                );
                return;
            case "putrestrequest":
                emitRequest(
                    "put",
                    object,
                    resolveInput(input, result, action),
                    conn,
                    lastRequestPerKey,
                    result
                );
                return;
            case "patchrestrequest":
                emitRequest(
                    "patch",
                    object,
                    resolveInput(input, result, action),
                    conn,
                    lastRequestPerKey,
                    result
                );
                return;
            case "deleterestrequest":
                emitRequest("delete", object, null, conn, lastRequestPerKey, result);
                return;
            case "deletewithpayload":
                emitRequest(
                    "delete",
                    object,
                    resolveInput(input, result, action),
                    conn,
                    lastRequestPerKey,
                    result
                );
                return;
            case "assertresponsecode":
                {
                    HttpRequestSpec last = lastRequestPerKey.get(object);
                    String expected = resolveInput(input, result, action);
                    if (last == null) {
                        result.warnings.add(
                            "assertResponseCode before any request on '" + object + "', skipped."
                        );
                        return;
                    }
                    try {
                        last.checkStatus = Integer.valueOf(expected.trim());
                    } catch (NumberFormatException e) {
                        result.warnings.add("Non-numeric assertResponseCode input: " + input);
                    }
                    return;
                }
            default:
                {
                    // Anything else (JSON asserts, variable stores, browser steps…)
                    // is out of scope for protocol-level v1: keep visible, never drop.
                    HttpRequestSpec last = lastRequestPerKey.get(object);
                    String note =
                        "TODO: unsupported action '" +
                        action +
                        "'" +
                        (input.isEmpty() ? "" : " (input: " + brief(input) + ")");
                    if (last != null) {
                        last.comments.add(note);
                    } else {
                        result.warnings.add(note);
                    }
                    result.warnings.add("Unsupported action not translated: " + action);
                }
        }
    }

    private static void emitRequest(
        String method,
        String key,
        String body,
        ConnectionState conn,
        Map<String, HttpRequestSpec> lastRequestPerKey,
        Result result
    ) {
        if (conn.endpoint == null || conn.endpoint.isEmpty()) {
            result.warnings.add(
                "Request '" + method + "' on '" + key + "' has no setEndPoint before it, skipped."
            );
            return;
        }
        HttpRequestSpec spec = new HttpRequestSpec();
        spec.method = method;
        spec.url = withParams(conn.endpoint, conn.urlParams);
        spec.name = method.toUpperCase(Locale.ROOT) + " " + HarReader.pathOf(spec.url);
        if (body != null && !body.isEmpty()) {
            spec.body = body;
        }
        spec.headers.addAll(conn.headers);
        // mirror engine behaviour: headers + params are consumed by the request
        conn.headers.clear();
        conn.urlParams.clear();
        result.requests.add(spec);
        lastRequestPerKey.put(key, spec);
    }

    private static String withParams(String endpoint, List<String> params) {
        if (params.isEmpty()) {
            return endpoint;
        }
        StringBuilder sb = new StringBuilder(endpoint);
        sb.append(endpoint.contains("?") ? "&" : "?");
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) {
                sb.append('&');
            }
            sb.append(params.get(i));
        }
        return sb.toString();
    }

    /** Input grammar handling — see {@link TestCaseFlattener#resolveInput}. */
    private static String resolveInput(String input, Result result, String action) {
        return TestCaseFlattener.resolveInput(input, result.warnings, action);
    }

    // ==================================================================
    // requests -> k6 JS
    // ==================================================================

    /**
     * Emit the full script.
     *
     * @param source     provenance label, e.g. "TestPlan/Login/TC_Login.yaml"
     * @param regenerate CLI hint, e.g. "ingenious perf export CLIDemo/API/TC --type http"
     * @param profile    load profile driving the options block
     * @param requests   ordered requests
     * @param warnings   generation warnings, embedded as comments near the top
     */
    public static String generate(
        String source,
        String regenerate,
        PerfProfile profile,
        List<HttpRequestSpec> requests,
        List<String> warnings
    ) {
        return generate(source, regenerate, profile, requests, warnings, null);
    }

    /**
     * Emit the full script, honouring an applied {@link RuleEngine.Result}
     * (capture variable declarations + SharedArray data files).
     */
    public static String generate(
        String source,
        String regenerate,
        PerfProfile profile,
        List<HttpRequestSpec> requests,
        List<String> warnings,
        RuleEngine.Result rules
    ) {
        ObjectMapper mapper = new ObjectMapper();
        StringBuilder body = new StringBuilder();
        body.append("import http from 'k6/http';\n");
        body.append("import { check, group, sleep } from 'k6';\n");
        boolean hasDataFiles = rules != null && !rules.dataFiles.isEmpty();
        if (hasDataFiles) {
            body.append("import { SharedArray } from 'k6/data';\n");
        }
        body.append('\n');
        if (!warnings.isEmpty()) {
            body.append("// Generation warnings:\n");
            for (String w : warnings) {
                body.append("//   - ").append(w.replace("\n", " ")).append('\n');
            }
            body.append('\n');
        }
        if (hasDataFiles) {
            for (Map.Entry<String, String> e : rules.dataFiles.entrySet()) {
                body
                    .append("const ")
                    .append(e.getKey())
                    .append(" = new SharedArray(")
                    .append(js(e.getKey()))
                    .append(", function () {\n")
                    .append("  return JSON.parse(open('../data/")
                    .append(e.getValue())
                    .append("'));\n});\n");
            }
            body.append('\n');
        }
        body.append("export const options = ");
        body.append(prettyJson(mapper, profile.toOptionsNode(mapper), 0));
        body.append(";\n\n");
        body.append("export default function () {\n");
        if (rules != null && !rules.captureVars.isEmpty()) {
            for (String var : rules.captureVars) {
                body.append("  let ").append(var).append(";\n");
            }
        }
        for (HttpRequestSpec spec : requests) {
            emitGroup(body, spec);
        }
        body.append("  sleep(1);\n");
        body.append("}\n");
        return ScriptProvenance.wrap(source, regenerate, profile.name, body.toString());
    }

    private static void emitGroup(StringBuilder out, HttpRequestSpec spec) {
        out.append("  group(").append(js(spec.name)).append(", function () {\n");
        for (String comment : spec.comments) {
            out.append("    // ").append(comment.replace("\n", " ")).append('\n');
        }
        boolean hasHeaders = !spec.headers.isEmpty();
        if (hasHeaders) {
            out.append("    const params = { headers: {\n");
            for (String[] h : spec.headers) {
                out
                    .append("      ")
                    .append(js(h[0]))
                    .append(": ")
                    .append(jsValue(h[1]))
                    .append(",\n");
            }
            out.append("    } };\n");
        }
        String method = "delete".equals(spec.method) ? "del" : spec.method;
        out.append("    const res = http.").append(method).append("(").append(jsValue(spec.url));
        boolean methodTakesBody = !"get".equals(spec.method);
        if (methodTakesBody) {
            out.append(", ").append(spec.body == null ? "null" : jsValue(spec.body));
        }
        if (hasHeaders) {
            out.append(", params");
        }
        out.append(");\n");
        for (String[] capture : spec.captures) {
            out.append("    ").append(capture[0]).append(" = ").append(capture[1]).append(";\n");
        }
        if (spec.checkStatus != null) {
            out
                .append("    check(res, { ")
                .append(js("status is " + spec.checkStatus))
                .append(": (r) => r.status === ")
                .append(spec.checkStatus)
                .append(" });\n");
        }
        for (String contains : spec.bodyContainsChecks) {
            out
                .append("    check(res, { ")
                .append(js("body contains " + brief(contains)))
                .append(": (r) => String(r.body).includes(")
                .append(js(contains))
                .append(") });\n");
        }
        out.append("  });\n");
    }

    /** Single-quoted JS string literal with full escaping. */
    static String js(String value) {
        StringBuilder sb = new StringBuilder("'");
        String s = value == null ? "" : value;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\'':
                    sb.append("\\'");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.append("'").toString();
    }

    /** Placeholder injected by the RuleEngine: ${corr_x}, ${__ENV.X}, ${data_x[...]...}. */
    private static final java.util.regex.Pattern PLACEHOLDER = java.util.regex.Pattern.compile(
        "\\$\\{(corr_[A-Za-z0-9_]+|__ENV\\.[A-Za-z0-9_]+|data_[A-Za-z0-9_]+\\[[^}]*)}"
    );

    /**
     * Emit a value string: plain single-quoted literal, or — when it carries
     * RuleEngine placeholders — a backtick template literal where only our
     * placeholders stay live and everything else is escaped.
     */
    static String jsValue(String value) {
        String s = value == null ? "" : value;
        if (!PLACEHOLDER.matcher(s).find()) {
            return js(s);
        }
        StringBuilder sb = new StringBuilder("`");
        int index = 0;
        java.util.regex.Matcher matcher = PLACEHOLDER.matcher(s);
        while (matcher.find()) {
            sb.append(escapeTemplate(s.substring(index, matcher.start())));
            sb.append(matcher.group()); // live placeholder
            index = matcher.end();
        }
        sb.append(escapeTemplate(s.substring(index)));
        return sb.append('`').toString();
    }

    private static String escapeTemplate(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\') {
                sb.append("\\\\");
            } else if (c == '`') {
                sb.append("\\`");
            } else if (c == '$' && i + 1 < s.length() && s.charAt(i + 1) == '{') {
                sb.append("\\$"); // non-placeholder interpolation must stay inert
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\r') {
                sb.append("\\r");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** Pretty-print a JsonNode as an indented JS object literal. */
    static String prettyJson(ObjectMapper mapper, JsonNode node, int indent) {
        try {
            String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
            // Jackson uses 2-space indent + "\n"; re-indent for embedding.
            if (indent <= 0) {
                return json;
            }
            String pad = " ".repeat(indent);
            return json.replace("\n", "\n" + pad);
        } catch (Exception e) {
            return node.toString();
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private static String brief(String s) {
        return s.length() <= 60 ? s : s.substring(0, 57) + "...";
    }
}

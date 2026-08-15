package com.ing.engine.perf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Applies ordered {@link PerfRule}s to an extracted request list before k6
 * emission, and proposes correlation rules from HAR recordings (the
 * k6-studio flow: record → propose → review → generate).
 *
 * <p>Substituted values become {@code ${...}} placeholders inside the spec
 * strings; {@link K6HttpScriptGenerator} emits any string containing a
 * placeholder as a JS template literal.
 */
public final class RuleEngine {

    /** Application outcome: transformed in place + emission requirements. */
    public static final class Result {
        /** JS variables to declare at function scope: let corr_x; */
        public final Set<String> captureVars = new LinkedHashSet<>();
        /** SharedArray data files: JS var name -> file name under Performance/data/. */
        public final Map<String, String> dataFiles = new LinkedHashMap<>();
        public final List<String> warnings = new ArrayList<>();
        public int applied;
    }

    private RuleEngine() {}

    // ==================================================================
    // apply
    // ==================================================================

    public static Result apply(List<HttpRequestSpec> requests, List<PerfRule> rules) {
        Result result = new Result();
        for (PerfRule rule : rules) {
            String type = rule.type == null ? "" : rule.type.toLowerCase(Locale.ROOT);
            switch (type) {
                case "correlation":
                    applyCorrelation(requests, rule, result);
                    break;
                case "parameterization":
                    applyParameterization(requests, rule, result);
                    break;
                case "verification":
                    applyVerification(requests, rule, result);
                    break;
                case "headerfilter":
                    applyHeaderFilter(requests, rule, result);
                    break;
                default:
                    result.warnings.add("Unknown rule type skipped: " + rule.type);
            }
        }
        return result;
    }

    private static void applyCorrelation(
        List<HttpRequestSpec> requests,
        PerfRule rule,
        Result result
    ) {
        if (rule.value.isEmpty() || rule.name.isEmpty()) {
            result.warnings.add("correlation rule needs 'name' and 'value', skipped.");
            return;
        }
        String var = "corr_" + sanitize(rule.name);
        int sourceIndex = findSourceIndex(requests, rule);
        if (sourceIndex < 0) {
            result.warnings.add(
                "correlation '" + rule.name + "': no source request found, skipped."
            );
            return;
        }
        HttpRequestSpec source = requests.get(sourceIndex);
        source.captures.add(new String[] { var, captureExpression(rule) });
        source.comments.add("correlation '" + rule.name + "' captured here");
        String placeholder = "${" + var + "}";
        boolean replacedAnywhere = false;
        for (int i = sourceIndex + 1; i < requests.size(); i++) {
            replacedAnywhere |= substituteLiteral(requests.get(i), rule, placeholder);
        }
        if (!replacedAnywhere) {
            result.warnings.add(
                "correlation '" + rule.name + "': value never reappears after its source."
            );
        }
        result.captureVars.add(var);
        result.applied++;
    }

    private static int findSourceIndex(List<HttpRequestSpec> requests, PerfRule rule) {
        for (int i = 0; i < requests.size(); i++) {
            HttpRequestSpec r = requests.get(i);
            if (!rule.extractSource.isEmpty()) {
                if (r.url.contains(rule.extractSource)) {
                    return i;
                }
            } else if (
                r.recordedResponseBody != null && r.recordedResponseBody.contains(rule.value)
            ) {
                return i;
            }
        }
        return -1;
    }

    /** JS expression (uses the group's {@code res}) extracting the value. */
    static String captureExpression(PerfRule rule) {
        String from = rule.extractFrom == null ? "json" : rule.extractFrom.toLowerCase(Locale.ROOT);
        switch (from) {
            case "header":
                return "res.headers[" + K6HttpScriptGenerator.js(rule.extractSelector) + "]";
            case "regex":
                return (
                    "(res.body.match(new RegExp(" +
                    K6HttpScriptGenerator.js(rule.extractSelector) +
                    ")) || [])[1]"
                );
            case "json":
            default:
                return "res.json(" + K6HttpScriptGenerator.js(rule.extractSelector) + ")";
        }
    }

    private static boolean substituteLiteral(
        HttpRequestSpec request,
        PerfRule rule,
        String placeholder
    ) {
        boolean everywhere = rule.replaceIn.isEmpty();
        boolean changed = false;
        if ((everywhere || rule.replaceIn.contains("url")) && request.url.contains(rule.value)) {
            request.url = request.url.replace(rule.value, placeholder);
            changed = true;
        }
        if (
            (everywhere || rule.replaceIn.contains("body")) &&
            request.body != null &&
            request.body.contains(rule.value)
        ) {
            request.body = request.body.replace(rule.value, placeholder);
            changed = true;
        }
        if (everywhere || rule.replaceIn.contains("header")) {
            for (String[] h : request.headers) {
                if (h[1] != null && h[1].contains(rule.value)) {
                    h[1] = h[1].replace(rule.value, placeholder);
                    changed = true;
                }
            }
            // credential headers scrubbed at import time come back to life
            // once their dynamic part is correlated (the k6-studio flow)
            Iterator<String[]> scrubbed = request.scrubbedHeaders.iterator();
            while (scrubbed.hasNext()) {
                String[] h = scrubbed.next();
                if (h[1] != null && h[1].contains(rule.value)) {
                    request.headers.add(
                        new String[] { h[0], h[1].replace(rule.value, placeholder) }
                    );
                    request.comments.add(
                        "header '" + h[0] + "' re-injected via correlation '" + rule.name + "'"
                    );
                    scrubbed.remove();
                    changed = true;
                }
            }
        }
        return changed;
    }

    private static void applyParameterization(
        List<HttpRequestSpec> requests,
        PerfRule rule,
        Result result
    ) {
        String replacement;
        switch (rule.valueSource == null ? "literal" : rule.valueSource.toLowerCase(Locale.ROOT)) {
            case "env":
                replacement = "${__ENV." + rule.valueName + "}";
                break;
            case "datafile":
                {
                    if (rule.valueFile.isEmpty() || rule.valueName.isEmpty()) {
                        result.warnings.add("dataFile rule needs 'file' and 'column', skipped.");
                        return;
                    }
                    String var = "data_" + sanitize(stripExtension(rule.valueFile));
                    result.dataFiles.put(var, rule.valueFile);
                    replacement =
                        "${" + var + "[__ITER % " + var + ".length]." + rule.valueName + "}";
                    break;
                }
            case "literal":
            default:
                replacement = rule.valueName;
        }
        Pattern pattern = null;
        if (!rule.matchRegex.isEmpty()) {
            pattern = Pattern.compile(rule.matchRegex);
        } else if (rule.matchLiteral.isEmpty()) {
            result.warnings.add("parameterization rule needs match.literal or match.regex.");
            return;
        }
        boolean changed = false;
        for (HttpRequestSpec request : requests) {
            String in = rule.matchIn == null ? "body" : rule.matchIn.toLowerCase(Locale.ROOT);
            if ("url".equals(in)) {
                String updated = replaceIn(request.url, rule.matchLiteral, pattern, replacement);
                changed |= !updated.equals(request.url);
                request.url = updated;
            } else if ("header".equals(in)) {
                for (String[] h : request.headers) {
                    String updated = replaceIn(h[1], rule.matchLiteral, pattern, replacement);
                    changed |= updated != null && !updated.equals(h[1]);
                    h[1] = updated;
                }
            } else if (request.body != null) {
                String updated = replaceIn(request.body, rule.matchLiteral, pattern, replacement);
                changed |= !updated.equals(request.body);
                request.body = updated;
            }
        }
        if (!changed) {
            result.warnings.add(
                "parameterization matched nothing: " +
                (rule.matchRegex.isEmpty() ? rule.matchLiteral : rule.matchRegex)
            );
        } else {
            result.applied++;
        }
    }

    private static String replaceIn(
        String haystack,
        String literal,
        Pattern pattern,
        String replacement
    ) {
        if (haystack == null) {
            return null;
        }
        if (pattern != null) {
            Matcher matcher = pattern.matcher(haystack);
            return matcher.replaceAll(Matcher.quoteReplacement(replacement));
        }
        return haystack.replace(literal, replacement);
    }

    private static void applyVerification(
        List<HttpRequestSpec> requests,
        PerfRule rule,
        Result result
    ) {
        boolean matched = false;
        for (HttpRequestSpec request : requests) {
            if (!rule.matchUrl.isEmpty() && !request.url.contains(rule.matchUrl)) {
                continue;
            }
            matched = true;
            if (rule.checkStatus > 0) {
                request.checkStatus = Integer.valueOf(rule.checkStatus);
            }
            if (!rule.checkBodyContains.isEmpty()) {
                request.bodyContainsChecks.add(rule.checkBodyContains);
            }
        }
        if (matched) {
            result.applied++;
        } else {
            result.warnings.add("verification matched no request: " + rule.matchUrl);
        }
    }

    private static void applyHeaderFilter(
        List<HttpRequestSpec> requests,
        PerfRule rule,
        Result result
    ) {
        if (rule.header.isEmpty()) {
            result.warnings.add("headerFilter rule needs 'header', skipped.");
            return;
        }
        boolean redact = "redact".equalsIgnoreCase(rule.action);
        for (HttpRequestSpec request : requests) {
            Iterator<String[]> it = request.headers.iterator();
            while (it.hasNext()) {
                String[] h = it.next();
                if (h[0].equalsIgnoreCase(rule.header)) {
                    if (redact) {
                        h[1] = "REDACTED";
                    } else {
                        it.remove();
                    }
                }
            }
        }
        result.applied++;
    }

    // ==================================================================
    // auto-correlation proposal (HAR)
    // ==================================================================

    private static final int MIN_TOKEN_LENGTH = 8;
    private static final Pattern TOKEN_SHAPE = Pattern.compile("[A-Za-z0-9_./+=-]{8,}");

    /**
     * k6-studio-style heuristic: a JSON string value from the response of
     * request <i>n</i> (≥ 8 chars, token-shaped) that reappears verbatim in
     * the URL, body or headers of a request <i>m &gt; n</i> becomes a
     * proposed correlation rule. Review before applying blindly.
     */
    public static List<PerfRule> proposeCorrelations(List<HttpRequestSpec> requests) {
        List<PerfRule> proposals = new ArrayList<>();
        Set<String> seenValues = new LinkedHashSet<>();
        ObjectMapper mapper = new ObjectMapper();
        for (int i = 0; i < requests.size(); i++) {
            HttpRequestSpec source = requests.get(i);
            if (source.recordedResponseBody == null || source.recordedResponseBody.isEmpty()) {
                continue;
            }
            JsonNode json;
            try {
                json = mapper.readTree(source.recordedResponseBody);
            } catch (Exception e) {
                continue; // non-JSON responses are skipped in v1
            }
            Map<String, String> candidates = new LinkedHashMap<>();
            collectStringLeaves(json, "", candidates);
            for (Map.Entry<String, String> candidate : candidates.entrySet()) {
                String value = candidate.getValue();
                if (
                    value.length() < MIN_TOKEN_LENGTH ||
                    !TOKEN_SHAPE.matcher(value).matches() ||
                    seenValues.contains(value)
                ) {
                    continue;
                }
                if (appearsLater(requests, i, value)) {
                    PerfRule rule = new PerfRule();
                    rule.type = "correlation";
                    rule.name = leafName(candidate.getKey());
                    rule.value = value;
                    rule.extractFrom = "json";
                    rule.extractSelector = candidate.getKey();
                    rule.extractSource = HarReader.pathOf(source.url);
                    proposals.add(rule);
                    seenValues.add(value);
                }
            }
        }
        return proposals;
    }

    private static void collectStringLeaves(JsonNode node, String path, Map<String, String> out) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            Iterator<String> fields = node.fieldNames();
            while (fields.hasNext()) {
                String field = fields.next();
                collectStringLeaves(
                    node.get(field),
                    path.isEmpty() ? field : path + "." + field,
                    out
                );
            }
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                collectStringLeaves(node.get(i), path + "." + i, out);
            }
        } else if (node.isTextual()) {
            out.put(path, node.asText());
        }
    }

    private static boolean appearsLater(List<HttpRequestSpec> requests, int after, String value) {
        for (int i = after + 1; i < requests.size(); i++) {
            HttpRequestSpec r = requests.get(i);
            if (r.url.contains(value)) {
                return true;
            }
            if (r.body != null && r.body.contains(value)) {
                return true;
            }
            for (String[] h : r.headers) {
                if (h[1] != null && h[1].contains(value)) {
                    return true;
                }
            }
            // scrubbed credential headers are analysed too (Authorization: Bearer <token>)
            for (String[] h : r.scrubbedHeaders) {
                if (h[1] != null && h[1].contains(value)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String leafName(String dotPath) {
        int dot = dotPath.lastIndexOf('.');
        String leaf = dot >= 0 ? dotPath.substring(dot + 1) : dotPath;
        return leaf.isEmpty() ? "value" : leaf;
    }

    private static String sanitize(String s) {
        String cleaned = s.replaceAll("[^A-Za-z0-9_]", "_");
        return cleaned.isEmpty() ? "value" : cleaned;
    }

    private static String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }
}

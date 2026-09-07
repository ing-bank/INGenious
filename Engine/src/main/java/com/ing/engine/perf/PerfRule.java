package com.ing.engine.perf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * One generator rule (k6-studio style), persisted per script as an ordered
 * list in {@code Performance/rules/<script>.rules.yaml}.
 *
 * <p>Types:
 * <ul>
 *   <li>{@code correlation} — capture a dynamic value from the response of
 *       the request that produced it and substitute every later literal
 *       occurrence (token, session id, CSRF...)</li>
 *   <li>{@code parameterization} — swap a recorded literal for an env var,
 *       another literal, or a JSON data-file column (SharedArray)</li>
 *   <li>{@code verification} — add status / body-contains checks to matching
 *       requests</li>
 *   <li>{@code headerFilter} — drop or redact a header everywhere</li>
 * </ul>
 *
 * Example YAML:
 * <pre>
 * - type: correlation
 *   name: token
 *   value: "eyJhbGciOi..."          # the recorded literal to replace
 *   extract: { from: json, selector: "data.token", source: "/login" }
 *   replaceIn: [url, body, header]
 * - type: parameterization
 *   match: { in: body, literal: "john@acme.test" }
 *   value: { source: env, name: USER_EMAIL }
 * - type: verification
 *   match: { url: "/checkout" }
 *   check: { status: 200, bodyContains: "orderId" }
 * - type: headerFilter
 *   header: X-Request-Id
 *   action: drop
 * </pre>
 */
public final class PerfRule {
    public String type = "";
    /** correlation: capture variable name (JS: corr_<name>). */
    public String name = "";
    /** correlation: the recorded literal value to hunt for in later requests. */
    public String value = "";
    /** correlation: from = json | regex | header. */
    public String extractFrom = "json";
    /** correlation: gjson dot-path, regex with group 1, or header name. */
    public String extractSelector = "";
    /** correlation: URL substring identifying the SOURCE request (optional). */
    public String extractSource = "";
    /** correlation: where to substitute (url/body/header); empty = everywhere. */
    public final List<String> replaceIn = new ArrayList<>();

    /** parameterization: where to match — body | url | header. */
    public String matchIn = "body";
    /** parameterization: literal to replace (exact string). */
    public String matchLiteral = "";
    /** parameterization: regex whose ENTIRE match is replaced (alternative). */
    public String matchRegex = "";
    /** parameterization: value source — literal | env | dataFile. */
    public String valueSource = "literal";
    /** parameterization: literal replacement / env var name / data column. */
    public String valueName = "";
    /** parameterization (dataFile): JSON file under Performance/data/. */
    public String valueFile = "";

    /** verification: URL substring selecting the requests to check. */
    public String matchUrl = "";
    /** verification: expected status (0 = don't touch). */
    public int checkStatus;
    /** verification: response body must contain this text. */
    public String checkBodyContains = "";

    /** headerFilter: header name (case-insensitive). */
    public String header = "";
    /** headerFilter: drop (remove) or redact (blank the value). */
    public String action = "drop";

    // ------------------------------------------------------------------
    // YAML
    // ------------------------------------------------------------------

    public static List<PerfRule> load(File rulesFile) throws Exception {
        List<PerfRule> out = new ArrayList<>();
        if (rulesFile == null || !rulesFile.isFile()) {
            return out;
        }
        JsonNode root = new YAMLMapper().readTree(rulesFile);
        if (root == null || !root.isArray()) {
            return out;
        }
        for (JsonNode n : root) {
            out.add(fromNode(n));
        }
        return out;
    }

    public static void save(List<PerfRule> rules, File rulesFile) throws Exception {
        YAMLMapper yaml = new YAMLMapper();
        ArrayNode root = yaml.createArrayNode();
        for (PerfRule rule : rules) {
            root.add(rule.toNode(yaml));
        }
        if (rulesFile.getParentFile() != null) {
            rulesFile.getParentFile().mkdirs();
        }
        yaml.writerWithDefaultPrettyPrinter().writeValue(rulesFile, root);
    }

    static PerfRule fromNode(JsonNode n) {
        PerfRule rule = new PerfRule();
        rule.type = n.path("type").asText("");
        rule.name = n.path("name").asText("");
        rule.value = n.path("value").isObject() ? "" : n.path("value").asText("");
        JsonNode extract = n.path("extract");
        rule.extractFrom = extract.path("from").asText("json");
        rule.extractSelector = extract.path("selector").asText("");
        rule.extractSource = extract.path("source").asText("");
        if (n.path("replaceIn").isArray()) {
            for (JsonNode r : n.get("replaceIn")) {
                rule.replaceIn.add(r.asText());
            }
        }
        JsonNode match = n.path("match");
        rule.matchIn = match.path("in").asText("body");
        rule.matchLiteral = match.path("literal").asText("");
        rule.matchRegex = match.path("regex").asText("");
        rule.matchUrl = match.path("url").asText("");
        JsonNode value = n.path("value");
        if (value.isObject()) {
            rule.valueSource = value.path("source").asText("literal");
            rule.valueName = value.path("name").asText(value.path("column").asText(""));
            rule.valueFile = value.path("file").asText("");
        }
        JsonNode check = n.path("check");
        rule.checkStatus = check.path("status").asInt(0);
        rule.checkBodyContains = check.path("bodyContains").asText("");
        rule.header = n.path("header").asText("");
        rule.action = n.path("action").asText("drop");
        return rule;
    }

    ObjectNode toNode(ObjectMapper mapper) {
        ObjectNode n = mapper.createObjectNode();
        n.put("type", type);
        switch (type) {
            case "correlation":
                {
                    n.put("name", name);
                    n.put("value", value);
                    ObjectNode extract = n.putObject("extract");
                    extract.put("from", extractFrom);
                    extract.put("selector", extractSelector);
                    if (!extractSource.isEmpty()) {
                        extract.put("source", extractSource);
                    }
                    if (!replaceIn.isEmpty()) {
                        ArrayNode arr = n.putArray("replaceIn");
                        for (String r : replaceIn) {
                            arr.add(r);
                        }
                    }
                    break;
                }
            case "parameterization":
                {
                    ObjectNode match = n.putObject("match");
                    match.put("in", matchIn);
                    if (!matchLiteral.isEmpty()) {
                        match.put("literal", matchLiteral);
                    }
                    if (!matchRegex.isEmpty()) {
                        match.put("regex", matchRegex);
                    }
                    ObjectNode value = n.putObject("value");
                    value.put("source", valueSource);
                    if (!valueName.isEmpty()) {
                        value.put("name", valueName);
                    }
                    if (!valueFile.isEmpty()) {
                        value.put("file", valueFile);
                    }
                    break;
                }
            case "verification":
                {
                    ObjectNode match = n.putObject("match");
                    match.put("url", matchUrl);
                    ObjectNode check = n.putObject("check");
                    if (checkStatus > 0) {
                        check.put("status", checkStatus);
                    }
                    if (!checkBodyContains.isEmpty()) {
                        check.put("bodyContains", checkBodyContains);
                    }
                    break;
                }
            case "headerFilter":
                {
                    n.put("header", header);
                    n.put("action", action);
                    break;
                }
            default:
            // unknown types round-trip with just their type
        }
        return n;
    }

    /** Default rules file for a script: Performance/rules/<base>.rules.yaml. */
    public static File defaultRulesFile(PerfWorkspace workspace, String scriptBaseName) {
        return new File(workspace.rulesDir(), scriptBaseName + ".rules.yaml");
    }
}

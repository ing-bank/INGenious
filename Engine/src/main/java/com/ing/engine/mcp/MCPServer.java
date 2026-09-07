package com.ing.engine.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MCP (Model Context Protocol) server for INGenious.
 *
 * <p>Exposes the full {@code ingenious} CLI surface to AI agents (GitHub
 * Copilot, Claude Desktop, Cursor, etc.) so they can drive INGenious from
 * natural-language commands like
 * <em>"create a smoke test for the login page and run it headless"</em>.
 *
 * <p>Protocol: JSON-RPC 2.0 over stdio, newline-delimited, UTF-8. Implements
 * the {@code 2024-11-05} revision of the MCP specification.
 *
 * <p>Logs go to {@code stderr} only (when verbose), never stdout, so the
 * JSON-RPC stream stays clean.
 */
public class MCPServer {
    static final String PROTOCOL_VERSION = "2024-11-05";
    static final String SERVER_NAME = "ingenious-mcp-server";
    static final String SERVER_VERSION = "2.0.0";

    private final String defaultProject;
    private final boolean verbose;
    private final ObjectMapper json = new ObjectMapper();
    private final AtomicBoolean running = new AtomicBoolean(true);

    private final MCPTools tools;
    private final MCPPrompts prompts;
    private final MCPResources resources;

    public MCPServer(String defaultProject, boolean verbose) {
        this.defaultProject = defaultProject;
        this.verbose = verbose;
        this.tools = new MCPTools(defaultProject);
        this.prompts = new MCPPrompts();
        this.resources = new MCPResources(defaultProject);
    }

    /** Block on stdin reading JSON-RPC messages until EOF or {@code shutdown}. */
    public void start() throws IOException {
        log("INGenious MCP Server v" + SERVER_VERSION + " starting on stdio");
        log("Default project: " + (defaultProject != null ? defaultProject : "<none>"));

        // CRITICAL: capture the real stdout for JSON-RPC writes BEFORE we
        // redirect System.out. Datalib (and any plugin) is free to write
        // diagnostics with System.out.println(...) – those would otherwise
        // corrupt the framing protocol. Route them to stderr instead.
        java.io.PrintStream realStdout = System.out;
        System.setOut(System.err);

        Runtime
            .getRuntime()
            .addShutdownHook(
                new Thread(
                    () -> {
                        running.set(false);
                        log("Shutting down");
                    },
                    "mcp-shutdown"
                )
            );

        try (
            BufferedReader in = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8)
            )
        ) {
            PrintWriter out = new PrintWriter(
                new OutputStreamWriter(realStdout, StandardCharsets.UTF_8),
                true
            );

            String line;
            while (running.get() && (line = in.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                log("RX: " + line);
                String response = handleMessage(line);
                if (response != null) {
                    // notifications get no response
                    out.println(response);
                    out.flush();
                    log("TX: " + response);
                }
            }
        }

        log("MCP server stopped");
    }

    // ------------------------------------------------------------------
    // dispatch
    // ------------------------------------------------------------------

    private String handleMessage(String raw) {
        JsonNode req;
        try {
            req = json.readTree(raw);
        } catch (Exception e) {
            return error(null, -32700, "Parse error: " + e.getMessage());
        }

        String method = req.path("method").asText(null);
        JsonNode idNode = req.get("id");
        JsonNode params = req.path("params");

        if (method == null) {
            return error(idNode, -32600, "Invalid request: missing method");
        }

        // notifications/* have no id and never get a response
        boolean isNotification = idNode == null || idNode.isNull();

        try {
            switch (method) {
                case "initialize":
                    return success(idNode, handleInitialize(params));
                case "initialized":
                case "notifications/initialized":
                    return null; // ack-less notification
                case "ping":
                    return success(idNode, json.createObjectNode());
                case "shutdown":
                    running.set(false);
                    return success(idNode, json.createObjectNode());
                case "tools/list":
                    return success(idNode, tools.list(json));
                case "tools/call":
                    return success(idNode, tools.call(json, params));
                case "prompts/list":
                    return success(idNode, prompts.list(json));
                case "prompts/get":
                    return success(idNode, prompts.get(json, params));
                case "resources/list":
                    return success(idNode, resources.list(json));
                case "resources/read":
                    return success(idNode, resources.read(json, params));
                case "logging/setLevel":
                    return success(idNode, json.createObjectNode());
                default:
                    if (isNotification) return null;
                    return error(idNode, -32601, "Method not found: " + method);
            }
        } catch (MCPException e) {
            return isNotification ? null : error(idNode, e.code, e.getMessage(), e.data);
        } catch (Exception e) {
            if (verbose) e.printStackTrace(System.err);
            return isNotification
                ? null
                : error(idNode, -32603, "Internal error: " + e.getMessage());
        }
    }

    private ObjectNode handleInitialize(JsonNode params) {
        ObjectNode caps = json.createObjectNode();
        caps.putObject("tools").put("listChanged", false);
        caps.putObject("prompts").put("listChanged", false);
        caps.putObject("resources").put("subscribe", false).put("listChanged", false);
        caps.putObject("logging");

        ObjectNode info = json.createObjectNode();
        info.put("name", SERVER_NAME);
        info.put("version", SERVER_VERSION);
        info.put("title", "INGenious Test Automation");

        ObjectNode result = json.createObjectNode();
        result.put("protocolVersion", PROTOCOL_VERSION);
        result.set("capabilities", caps);
        result.set("serverInfo", info);
        // Conventions are injected into every client's model context so all
        // front-ends (IDE chat, REPL, external MCP clients) behave the same.
        result.put("instructions", ConventionCatalog.condensedInstructions());
        return result;
    }

    // ------------------------------------------------------------------
    // JSON-RPC envelope helpers
    // ------------------------------------------------------------------

    private String success(JsonNode id, JsonNode result) {
        ObjectNode env = json.createObjectNode();
        env.put("jsonrpc", "2.0");
        if (id != null && !id.isNull()) env.set("id", id); else env.set(
            "id",
            JsonNodeFactory.instance.nullNode()
        );
        env.set("result", result == null ? json.createObjectNode() : result);
        return env.toString();
    }

    private String error(JsonNode id, int code, String message) {
        return error(id, code, message, null);
    }

    private String error(JsonNode id, int code, String message, JsonNode data) {
        ObjectNode env = json.createObjectNode();
        env.put("jsonrpc", "2.0");
        if (id != null && !id.isNull()) env.set("id", id); else env.set(
            "id",
            JsonNodeFactory.instance.nullNode()
        );
        ObjectNode err = env.putObject("error");
        err.put("code", code);
        err.put("message", message);
        if (data != null) err.set("data", data);
        return env.toString();
    }

    /** Helper to convert a (string→string) map to a JSON object. */
    static ObjectNode toJson(ObjectMapper mapper, Map<String, ?> m) {
        ObjectNode n = mapper.createObjectNode();
        if (m == null) return n;
        for (Map.Entry<String, ?> e : m.entrySet()) {
            Object v = e.getValue();
            if (v == null) n.putNull(e.getKey()); else if (v instanceof Integer) n.put(
                e.getKey(),
                (Integer) v
            ); else if (v instanceof Long) n.put(e.getKey(), (Long) v); else if (
                v instanceof Double
            ) n.put(e.getKey(), (Double) v); else if (v instanceof Boolean) n.put(
                e.getKey(),
                (Boolean) v
            ); else if (v instanceof Map) n.set(
                e.getKey(),
                toJson(mapper, (Map<String, ?>) v)
            ); else n.put(e.getKey(), v.toString());
        }
        return n;
    }

    /** Convert a string list to a JSON array. */
    static ArrayNode toJsonArray(ObjectMapper mapper, Iterable<String> items) {
        ArrayNode arr = mapper.createArrayNode();
        if (items != null) for (String s : items) arr.add(new TextNode(s));
        return arr;
    }

    /** Wrap an arbitrary Java object's {@code toString()} as an MCP text content block. */
    static ObjectNode textContent(ObjectMapper mapper, String text) {
        ObjectNode content = mapper.createObjectNode();
        ArrayNode arr = content.putArray("content");
        ObjectNode item = arr.addObject();
        item.put("type", "text");
        item.put("text", text == null ? "" : text);
        return content;
    }

    /** Wrap arbitrary structured data as MCP content (text block with JSON). */
    static ObjectNode jsonContent(ObjectMapper mapper, JsonNode data) {
        ObjectNode content = mapper.createObjectNode();
        ArrayNode arr = content.putArray("content");
        ObjectNode item = arr.addObject();
        item.put("type", "text");
        try {
            item.put("text", mapper.writerWithDefaultPrettyPrinter().writeValueAsString(data));
        } catch (Exception e) {
            item.put("text", String.valueOf(data));
        }
        // also include structured form so capable clients can use it directly
        content.set("structuredContent", data);
        return content;
    }

    // ------------------------------------------------------------------
    // diagnostics
    // ------------------------------------------------------------------

    void log(String message) {
        if (verbose) {
            System.err.println("[mcp] " + message);
        }
    }

    /** Thrown by tools/prompts/resources to abort with a JSON-RPC error. */
    static class MCPException extends RuntimeException {
        final int code;
        final JsonNode data;

        MCPException(int code, String message) {
            this(code, message, null);
        }

        MCPException(int code, String message, JsonNode data) {
            super(message);
            this.code = code;
            this.data = data;
        }
    }

    /** Quick lookup of common params (project, etc.). */
    static String paramOrDefault(JsonNode args, String key, String fallback) {
        if (args == null || args.isMissingNode() || args.isNull()) return fallback;
        JsonNode n = args.get(key);
        if (n == null || n.isNull()) return fallback;
        return n.asText(fallback);
    }

    static String requiredParam(JsonNode args, String key) {
        String v = paramOrDefault(args, key, null);
        if (v == null || v.isEmpty()) {
            throw new MCPException(-32602, "Missing required parameter: " + key);
        }
        return v;
    }

    /** All registered tools and prompts share these defaults. */
    Map<String, String> defaults() {
        Map<String, String> d = new LinkedHashMap<>();
        if (defaultProject != null) d.put("project", defaultProject);
        return d;
    }
}

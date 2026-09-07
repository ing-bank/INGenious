package com.ing.engine.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Public, in-process entry point to the INGenious MCP tool surface.
 *
 * <p>Wraps the package-private {@link MCPTools} so other modules (e.g. the IDE
 * AI assistant sidebar) can list and invoke the same tools the stdio MCP
 * server exposes &mdash; without spawning a subprocess or opening a socket.
 * The heavy lifting (validation, Datalib mutations, subprocess runs) is shared
 * verbatim with the external server via {@link MCPTools}.</p>
 */
public final class MCPToolFacade {
    private final ObjectMapper mapper;
    private final MCPTools tools;

    public MCPToolFacade(String defaultProject) {
        this(new ObjectMapper(), defaultProject);
    }

    public MCPToolFacade(ObjectMapper mapper, String defaultProject) {
        this.mapper = mapper == null ? new ObjectMapper() : mapper;
        this.tools = new MCPTools(defaultProject);
    }

    /**
     * Returns the {@code tools/list} descriptor array. Each element is an object
     * with {@code name}, {@code description}, and {@code inputSchema} (JSON
     * Schema) fields.
     */
    public JsonNode listTools() {
        JsonNode listing = tools.list(mapper);
        return listing.path("tools");
    }

    /**
     * Invokes a tool by name with the given arguments.
     *
     * @param name      the tool name, e.g. {@code ingenious_testcase_create}
     * @param arguments the tool arguments object (may be {@code null})
     * @return the tool's raw structured result on success, or an object with an
     *         {@code error} field (plus optional {@code code} and {@code data})
     *         on failure. This method never throws.
     */
    public JsonNode callTool(String name, JsonNode arguments) {
        ObjectNode params = mapper.createObjectNode();
        params.put("name", name);
        params.set(
            "arguments",
            arguments == null || arguments.isNull() ? mapper.createObjectNode() : arguments
        );
        try {
            JsonNode wrapped = tools.call(mapper, params);
            JsonNode structured = wrapped.get("structuredContent");
            return structured != null ? structured : wrapped;
        } catch (MCPServer.MCPException e) {
            ObjectNode err = mapper.createObjectNode();
            err.put("error", e.getMessage() == null ? "Tool failed" : e.getMessage());
            err.put("code", e.code);
            if (e.data != null) {
                err.set("data", e.data);
            }
            return err;
        } catch (Exception e) {
            ObjectNode err = mapper.createObjectNode();
            err.put("error", e.getMessage() == null ? e.toString() : e.getMessage());
            return err;
        }
    }

    /** True if a tool with this exact name exists in the catalog. */
    public boolean isKnownTool(String name) {
        if (name == null) {
            return false;
        }
        for (JsonNode t : listTools()) {
            if (name.equals(t.path("name").asText())) {
                return true;
            }
        }
        return false;
    }
}

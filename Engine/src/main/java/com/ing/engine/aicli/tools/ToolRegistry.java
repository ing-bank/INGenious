package com.ing.engine.aicli.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ing.engine.mcp.MCPToolFacade;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * Single source of truth for all INGenious capabilities.
 *
 * <p>Built from two sources:
 * <ol>
 *   <li>The complete MCP tool surface via {@link MCPToolFacade} (in-process,
 *       shared verbatim with the stdio MCP server — one implementation, two
 *       front-ends), and</li>
 *   <li>{@link ToolPlugin}s discovered through {@link ServiceLoader}.</li>
 * </ol>
 */
public final class ToolRegistry {
    private static final String MCP_PREFIX = "ingenious_";

    private static final Set<String> MUTATING_VERBS = Set.of(
        "create",
        "add",
        "set",
        "delete",
        "edit",
        "insert",
        "move",
        "remove",
        "import",
        "save",
        "update",
        "generate",
        "export"
    );

    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, Tool> byId = new LinkedHashMap<>();
    private final List<String> pluginSummaries = new ArrayList<>();

    private ToolRegistry() {}

    /** Build the full registry: MCP surface + ServiceLoader plugins. */
    public static ToolRegistry create() {
        ToolRegistry r = new ToolRegistry();
        MCPToolFacade facade = new MCPToolFacade(r.mapper, null);
        for (JsonNode descriptor : facade.listTools()) {
            r.register(new FacadeTool(facade, descriptor));
        }
        try {
            for (ToolPlugin plugin : ServiceLoader.load(ToolPlugin.class)) {
                int added = 0;
                for (Tool t : plugin.tools()) {
                    if (r.register(t)) added++;
                }
                r.pluginSummaries.add(
                    plugin.name() + " " + plugin.version() + " (" + added + " tools)"
                );
            }
        } catch (ServiceConfigurationError e) {
            System.err.println("[aicli] plugin discovery failed: " + e.getMessage());
        }
        return r;
    }

    private boolean register(Tool t) {
        return byId.putIfAbsent(t.id(), t) == null;
    }

    /** Look up by short id ({@code testcase_create}) or qualified name. */
    public Tool get(String idOrName) {
        if (idOrName == null) return null;
        return byId.get(normalize(idOrName));
    }

    public static String normalize(String name) {
        String n = name.trim();
        return n.startsWith(MCP_PREFIX) ? n.substring(MCP_PREFIX.length()) : n;
    }

    public Collection<Tool> all() {
        return Collections.unmodifiableCollection(byId.values());
    }

    public Map<String, List<Tool>> byCategory() {
        Map<String, List<Tool>> out = new LinkedHashMap<>();
        for (Tool t : byId.values()) {
            out.computeIfAbsent(t.category(), k -> new ArrayList<>()).add(t);
        }
        return out;
    }

    public List<String> pluginSummaries() {
        return Collections.unmodifiableList(pluginSummaries);
    }

    /**
     * Compact one-line-per-tool listing for AI planner prompts:
     * {@code id(required*, optional?, ...): description}.
     */
    public String promptCatalog() {
        StringBuilder sb = new StringBuilder();
        for (Tool t : byId.values()) {
            sb.append("- ").append(t.id()).append('(');
            JsonNode schema = t.inputSchema();
            JsonNode props = schema.path("properties");
            Set<String> required = new java.util.LinkedHashSet<>();
            schema.path("required").forEach(n -> required.add(n.asText()));
            List<String> parts = new ArrayList<>();
            required.forEach(p -> parts.add(p + "*"));
            props
                .fieldNames()
                .forEachRemaining(
                    p -> {
                        if (!required.contains(p)) parts.add(p + "?");
                    }
                );
            sb.append(String.join(", ", parts)).append("): ");
            String desc = t.description() == null ? "" : t.description().replace('\n', ' ');
            sb.append(desc.length() > 140 ? desc.substring(0, 140) + "…" : desc);
            sb.append('\n');
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // classification
    // ------------------------------------------------------------------

    static boolean classifyMutating(String shortId) {
        if (shortId.startsWith("gen_from_") || shortId.equals("gen_testcase")) return true;
        if (shortId.startsWith("browser_")) return shortId.equals("browser_session_save");
        if (shortId.equals("gen_list")) return false;
        if (shortId.equals("apicollection_to_testcase")) return true;
        for (String token : shortId.split("_")) {
            if (MUTATING_VERBS.contains(token)) return true;
        }
        return false;
    }

    static String categoryOf(String shortId, boolean mutating) {
        String head = shortId.split("_")[0].toLowerCase(Locale.ROOT);
        switch (head) {
            case "data":
            case "env":
            case "config":
                return "data";
            case "gen":
            case "import":
                return "generation";
            case "run":
                return "execution";
            case "report":
            case "doctor":
                return "reporting";
            case "browser":
                return "browser";
            case "action":
                return "discovery";
            default:
                return mutating ? "authoring" : "discovery";
        }
    }

    // ------------------------------------------------------------------
    // MCP-backed tool
    // ------------------------------------------------------------------

    private static final class FacadeTool implements Tool {
        private final MCPToolFacade facade;
        private final String qualifiedName;
        private final String id;
        private final String description;
        private final JsonNode inputSchema;
        private final boolean mutating;
        private final String category;

        FacadeTool(MCPToolFacade facade, JsonNode descriptor) {
            this.facade = facade;
            this.qualifiedName = descriptor.path("name").asText();
            this.id = normalize(qualifiedName);
            this.description = descriptor.path("description").asText("");
            this.inputSchema = descriptor.path("inputSchema");
            this.mutating = classifyMutating(id);
            this.category = categoryOf(id, mutating);
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public String qualifiedName() {
            return qualifiedName;
        }

        @Override
        public String category() {
            return category;
        }

        @Override
        public String description() {
            return description;
        }

        @Override
        public JsonNode inputSchema() {
            return inputSchema;
        }

        @Override
        public boolean mutatesFiles() {
            return mutating;
        }

        @Override
        public JsonNode execute(JsonNode args) throws ToolException {
            JsonNode result = facade.callTool(qualifiedName, args);
            if (result != null && result.has("error") && result.size() <= 3) {
                throw new ToolException(
                    result.path("error").asText("Tool failed"),
                    result.path("code").asInt(-32603),
                    result.get("data")
                );
            }
            return result;
        }
    }
}

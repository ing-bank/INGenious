package com.ing.ide.main.mainui.components.aichat.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ing.datalib.component.Project;
import com.ing.engine.mcp.MCPToolFacade;
import com.ing.ide.main.mainui.AppMainFrame;
import com.ing.ide.main.mainui.components.aichat.model.Tool;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

/**
 * In-process bridge from the AI assistant to the full INGenious MCP tool
 * surface. Delegates every call to {@link MCPToolFacade} in the Engine module
 * &mdash; the same code path the external stdio MCP server uses &mdash; so the
 * IDE assistant and external agents share one implementation of all tools.
 *
 * <p>The active project's absolute path is injected as the {@code project}
 * argument on every call, so tools always act on the project currently open in
 * the IDE. After a successful mutating call, registered {@link RefreshListener}s
 * are notified on the EDT so the relevant view can reload.</p>
 */
public class MCPToolBridge implements ToolProvider {
    private static final Logger LOG = Logger.getLogger(MCPToolBridge.class.getName());

    private final AppMainFrame mainFrame;
    private final ObjectMapper mapper = new ObjectMapper();
    private final MCPToolFacade facade;
    private final List<RefreshListener> refreshListeners = new ArrayList<>();

    /** Tools that only read state and may run without user approval. */
    private static final Set<String> READ_ONLY = new HashSet<>(
        Arrays.asList(
            "ingenious_project_list",
            "ingenious_project_info",
            "ingenious_scenario_list",
            "ingenious_scenario_info",
            "ingenious_testcase_list",
            "ingenious_testcase_show",
            "ingenious_testcase_validate",
            "ingenious_testset_list",
            "ingenious_testset_show",
            "ingenious_action_list",
            "ingenious_action_search",
            "ingenious_action_info",
            "ingenious_action_categories",
            "ingenious_object_list",
            "ingenious_object_show",
            "ingenious_object_search",
            "ingenious_data_show",
            "ingenious_data_get",
            "ingenious_env_list",
            "ingenious_gen_list",
            "ingenious_report_latest",
            "ingenious_report_history",
            "ingenious_report_failures",
            "ingenious_report_show",
            "ingenious_report_compare",
            "ingenious_config_get",
            "ingenious_config_show",
            "ingenious_config_drivers",
            "ingenious_run_status",
            "ingenious_run_logs",
            "ingenious_run_dry",
            "ingenious_doctor",
            "ingenious_browser_session_snapshot",
            "ingenious_browser_inspect"
        )
    );

    /** Tools whose success should trigger a UI refresh. */
    private static final Set<String> MUTATIONS = new HashSet<>(
        Arrays.asList(
            "ingenious_testcase_create",
            "ingenious_testcase_add_step",
            "ingenious_testcase_edit_step",
            "ingenious_testcase_insert_step",
            "ingenious_testcase_remove_step",
            "ingenious_testcase_move_step",
            "ingenious_testcase_delete",
            "ingenious_testcase_parameterize",
            "ingenious_scenario_create",
            "ingenious_scenario_delete",
            "ingenious_object_add",
            "ingenious_object_update",
            "ingenious_object_delete",
            "ingenious_object_import_page",
            "ingenious_data_sheet_create",
            "ingenious_data_column_add",
            "ingenious_data_row_add",
            "ingenious_data_set",
            "ingenious_data_row_delete",
            "ingenious_data_import",
            "ingenious_env_create",
            "ingenious_env_delete",
            "ingenious_testset_create",
            "ingenious_testset_add",
            "ingenious_gen_testcase",
            "ingenious_gen_from_openapi",
            "ingenious_gen_from_har",
            "ingenious_data_generate",
            "ingenious_import_curl",
            "ingenious_import_postman",
            "ingenious_import_bruno",
            "ingenious_import_playwright",
            "ingenious_config_set",
            "ingenious_run",
            "ingenious_run_async"
        )
    );

    public MCPToolBridge(AppMainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.facade = new MCPToolFacade(mapper, currentProjectPath());
    }

    public void addRefreshListener(RefreshListener listener) {
        if (listener != null) {
            refreshListeners.add(listener);
        }
    }

    // ── ToolProvider ───────────────────────────────────────────────────────

    @Override
    public List<Tool> toolDefinitions() {
        List<Tool> result = new ArrayList<>();
        try {
            for (JsonNode t : facade.listTools()) {
                String name = t.path("name").asText();
                String description = t.path("description").asText("");
                JsonNode schema = t.path("inputSchema");
                result.add(new Tool(name, description, schema));
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Failed to build tool definitions", ex);
        }
        return result;
    }

    @Override
    public boolean isKnownTool(String toolName) {
        return facade.isKnownTool(toolName);
    }

    @Override
    public boolean isReadOnly(String toolName) {
        return toolName != null && READ_ONLY.contains(toolName);
    }

    @Override
    public ToolResult execute(String toolName, JsonNode args) {
        ObjectNode enriched = withProject(args);
        JsonNode result;
        try {
            result = facade.callTool(toolName, enriched);
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Tool execution failed: " + toolName, ex);
            return ToolResult.error("Tool failed: " + ex.getMessage());
        }

        boolean isError = result != null && result.has("error");
        if (!isError && MUTATIONS.contains(toolName)) {
            fireRefresh(toolName, result);
        }

        String content = toPretty(result);
        return isError ? ToolResult.error(content) : ToolResult.ok(content);
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    /** Injects the current project's absolute path unless the caller set one. */
    private ObjectNode withProject(JsonNode args) {
        ObjectNode obj = (args != null && args.isObject())
            ? ((ObjectNode) args).deepCopy()
            : mapper.createObjectNode();
        if (!obj.has("project") || obj.path("project").asText("").isEmpty()) {
            String path = currentProjectPath();
            if (path != null && !path.isEmpty()) {
                obj.put("project", path);
            }
        }
        return obj;
    }

    private String currentProjectPath() {
        try {
            Project p = mainFrame.getProject();
            return p == null ? null : p.getLocation();
        } catch (Exception ex) {
            return null;
        }
    }

    private void fireRefresh(String toolName, JsonNode result) {
        SwingUtilities.invokeLater(
            () -> {
                for (RefreshListener l : refreshListeners) {
                    try {
                        l.onMutation(toolName, result);
                    } catch (Exception ex) {
                        LOG.log(Level.FINE, "Refresh listener failed for " + toolName, ex);
                    }
                }
            }
        );
    }

    private String toPretty(JsonNode node) {
        if (node == null) {
            return "{}";
        }
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
        } catch (Exception ex) {
            return String.valueOf(node);
        }
    }
}

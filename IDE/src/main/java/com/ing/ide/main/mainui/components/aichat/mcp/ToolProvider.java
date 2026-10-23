package com.ing.ide.main.mainui.components.aichat.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.ing.ide.main.mainui.components.aichat.model.Tool;
import java.util.List;

/**
 * Abstraction over a tool backend used by the agent loop. Implemented by both
 * the legacy {@link INGeniousToolServer} (8 hand-written tools) and the
 * {@link MCPToolBridge} (the full in-process MCP surface). The
 * {@code AgentOrchestrator} depends on this interface so either backend can be
 * plugged in.
 */
public interface ToolProvider {
    /** Tool definitions to advertise to the model, OpenAI function schema. */
    List<Tool> toolDefinitions();

    /** True if the named tool exists and can be dispatched. */
    boolean isKnownTool(String toolName);

    /** True if the tool only reads state and may run without user approval. */
    boolean isReadOnly(String toolName);

    /** Executes one tool call with parsed JSON arguments. Never throws. */
    ToolResult execute(String toolName, JsonNode args);
}

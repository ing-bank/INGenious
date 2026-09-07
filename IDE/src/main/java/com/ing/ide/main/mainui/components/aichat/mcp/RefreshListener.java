package com.ing.ide.main.mainui.components.aichat.mcp;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Callback fired after a mutating MCP tool completes successfully, so the IDE
 * can refresh the relevant view (test design tree, Object Repository, data
 * sheets, execution panel, dashboard). Always invoked on the EDT.
 */
public interface RefreshListener {
    /**
     * @param toolName the MCP tool that mutated state, e.g.
     *                 {@code ingenious_testcase_create}
     * @param result   the tool's structured result
     */
    void onMutation(String toolName, JsonNode result);
}

package com.ing.engine.aicli.tools;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * A single INGenious capability that the interactive AI CLI (and any other
 * front-end, e.g. the MCP server) can discover and invoke.
 *
 * <p>Tools are the unit of planning: the {@code Planner} composes them into
 * {@code Plan}s and the {@code ExecutionEngine} runs them. Implementations
 * must be side-effect-free unless {@link #mutatesFiles()} is {@code true}.
 */
public interface Tool {
    /** Short id, e.g. {@code testcase_create}. */
    String id();

    /** Fully qualified name as exposed over MCP, e.g. {@code ingenious_testcase_create}. */
    String qualifiedName();

    /** One of: discovery, authoring, data, generation, execution, reporting, browser. */
    String category();

    String description();

    /** JSON Schema describing the arguments object. */
    JsonNode inputSchema();

    /** True if the tool writes project files (drives approval mode + undo journaling). */
    boolean mutatesFiles();

    /**
     * Execute the tool.
     *
     * @param args arguments object (never null; may be empty)
     * @return structured JSON result
     */
    JsonNode execute(JsonNode args) throws ToolException;
}

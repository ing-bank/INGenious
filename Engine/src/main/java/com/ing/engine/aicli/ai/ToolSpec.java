package com.ing.engine.aicli.ai;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * OpenAI-style function/tool definition sent to a tool-calling provider.
 */
public final class ToolSpec {
    public final String name;
    public final String description;
    public final JsonNode parameters;

    public ToolSpec(String name, String description, JsonNode parameters) {
        this.name = name;
        this.description = description;
        this.parameters = parameters;
    }
}

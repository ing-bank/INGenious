package com.ing.engine.aicli.ai;

/** A single tool/function call requested by the model. */
public final class AgentToolCall {
    public final String id;
    public final String name;
    public final String argumentsJson;

    public AgentToolCall(String id, String name, String argumentsJson) {
        this.id = id;
        this.name = name;
        this.argumentsJson = argumentsJson;
    }
}

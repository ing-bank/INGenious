package com.ing.engine.aicli.ai;

import java.util.List;

/** One turn returned by a tool-calling provider: text and/or tool calls. */
public final class AgentReply {
    public final String content;
    public final List<AgentToolCall> toolCalls;

    public AgentReply(String content, List<AgentToolCall> toolCalls) {
        this.content = content;
        this.toolCalls = toolCalls;
    }

    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }
}

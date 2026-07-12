package com.ing.engine.aicli.ai;

import java.util.List;

/**
 * A message in a tool-calling conversation. Richer than {@link ChatMessage}:
 * assistant messages may carry {@code tool_calls}, and {@code tool} messages
 * carry the id/name of the call they answer.
 */
public final class AgentMessage {
    public final String role; // system | user | assistant | tool
    public final String content; // may be null (assistant tool-call turns)
    public final List<AgentToolCall> toolCalls; // assistant only
    public final String toolCallId; // tool only
    public final String toolName; // tool only

    private AgentMessage(
        String role,
        String content,
        List<AgentToolCall> toolCalls,
        String toolCallId,
        String toolName
    ) {
        this.role = role;
        this.content = content;
        this.toolCalls = toolCalls;
        this.toolCallId = toolCallId;
        this.toolName = toolName;
    }

    public static AgentMessage system(String content) {
        return new AgentMessage("system", content, null, null, null);
    }

    public static AgentMessage user(String content) {
        return new AgentMessage("user", content, null, null, null);
    }

    public static AgentMessage assistant(String content) {
        return new AgentMessage("assistant", content, null, null, null);
    }

    public static AgentMessage assistantToolCalls(String content, List<AgentToolCall> toolCalls) {
        return new AgentMessage("assistant", content, toolCalls, null, null);
    }

    public static AgentMessage toolResult(String toolCallId, String toolName, String content) {
        return new AgentMessage("tool", content, null, toolCallId, toolName);
    }
}

package com.ing.ide.main.mainui.components.aichat.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * A single message in a chat conversation, following the OpenAI-compatible
 * schema used by the GitHub Models API.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatMessage {
    /** Well-known role values. */
    public static final String ROLE_SYSTEM = "system";
    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";
    public static final String ROLE_TOOL = "tool";

    @JsonProperty("role")
    private String role;

    @JsonProperty("content")
    private String content;

    /** Tool calls requested by the assistant (assistant messages only). */
    @JsonProperty("tool_calls")
    private List<ToolCall> toolCalls;

    /** Links a tool-result message back to the originating tool call. */
    @JsonProperty("tool_call_id")
    private String toolCallId;

    /** Tool name for a tool-result message. */
    @JsonProperty("name")
    private String name;

    public ChatMessage() {}

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public static ChatMessage system(String content) {
        return new ChatMessage(ROLE_SYSTEM, content);
    }

    public static ChatMessage user(String content) {
        return new ChatMessage(ROLE_USER, content);
    }

    public static ChatMessage assistant(String content) {
        return new ChatMessage(ROLE_ASSISTANT, content);
    }

    /** Builds a tool-result message answering a specific tool call. */
    public static ChatMessage toolResult(String toolCallId, String toolName, String content) {
        ChatMessage m = new ChatMessage(ROLE_TOOL, content);
        m.toolCallId = toolCallId;
        m.name = toolName;
        return m;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<ToolCall> getToolCalls() {
        return toolCalls;
    }

    public void setToolCalls(List<ToolCall> toolCalls) {
        this.toolCalls = toolCalls;
    }

    public String getToolCallId() {
        return toolCallId;
    }

    public void setToolCallId(String toolCallId) {
        this.toolCallId = toolCallId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

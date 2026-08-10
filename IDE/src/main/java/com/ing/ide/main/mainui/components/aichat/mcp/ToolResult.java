package com.ing.ide.main.mainui.components.aichat.mcp;

/**
 * Result of executing a tool call. {@code content} is fed back to the model as
 * the tool message; {@code error} marks failures (including validation
 * rejections) so the model can correct itself.
 */
public class ToolResult {
    private final boolean error;
    private final String content;

    private ToolResult(boolean error, String content) {
        this.error = error;
        this.content = content;
    }

    public static ToolResult ok(String content) {
        return new ToolResult(false, content);
    }

    public static ToolResult error(String message) {
        return new ToolResult(true, message);
    }

    public boolean isError() {
        return error;
    }

    public String getContent() {
        return content;
    }
}

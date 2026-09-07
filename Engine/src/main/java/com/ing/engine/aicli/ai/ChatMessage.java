package com.ing.engine.aicli.ai;

/** One chat turn. Role is {@code system}, {@code user}, or {@code assistant}. */
public final class ChatMessage {
    private final String role;
    private final String content;

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public String role() {
        return role;
    }

    public String content() {
        return content;
    }

    public static ChatMessage system(String content) {
        return new ChatMessage("system", content);
    }

    public static ChatMessage user(String content) {
        return new ChatMessage("user", content);
    }

    public static ChatMessage assistant(String content) {
        return new ChatMessage("assistant", content);
    }
}

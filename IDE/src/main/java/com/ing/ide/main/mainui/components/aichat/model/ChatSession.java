package com.ing.ide.main.mainui.components.aichat.model;

import java.util.ArrayList;
import java.util.List;

/**
 * In-memory state for a single chat conversation: the selected model, the
 * ordered message history, and cumulative token usage across all turns.
 */
public class ChatSession {
    private String model;
    private final List<ChatMessage> messages = new ArrayList<>();
    private final TokenUsage cumulativeUsage = new TokenUsage();

    public ChatSession() {}

    public ChatSession(String model) {
        this.model = model;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public void addMessage(ChatMessage message) {
        if (message != null) {
            messages.add(message);
        }
    }

    public TokenUsage getCumulativeUsage() {
        return cumulativeUsage;
    }

    /** Accumulates usage reported for a single completed turn. */
    public void recordUsage(TokenUsage turnUsage) {
        cumulativeUsage.add(turnUsage);
    }

    public void clear() {
        messages.clear();
        cumulativeUsage.setPromptTokens(0);
        cumulativeUsage.setCompletionTokens(0);
        cumulativeUsage.setTotalTokens(0);
    }
}

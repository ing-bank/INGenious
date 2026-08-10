package com.ing.ide.main.mainui.components.aichat.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Token usage accounting returned by the GitHub Models chat completions API in
 * the {@code usage} object of a response (OpenAI-compatible shape).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TokenUsage {
    @JsonProperty("prompt_tokens")
    private int promptTokens;

    @JsonProperty("completion_tokens")
    private int completionTokens;

    @JsonProperty("total_tokens")
    private int totalTokens;

    public TokenUsage() {}

    public TokenUsage(int promptTokens, int completionTokens, int totalTokens) {
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.totalTokens = totalTokens;
    }

    public int getPromptTokens() {
        return promptTokens;
    }

    public void setPromptTokens(int promptTokens) {
        this.promptTokens = promptTokens;
    }

    public int getCompletionTokens() {
        return completionTokens;
    }

    public void setCompletionTokens(int completionTokens) {
        this.completionTokens = completionTokens;
    }

    public int getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(int totalTokens) {
        this.totalTokens = totalTokens;
    }

    /**
     * Adds another usage record to this one, accumulating each counter. Used to
     * track cumulative usage across a conversation.
     */
    public void add(TokenUsage other) {
        if (other == null) {
            return;
        }
        this.promptTokens += other.promptTokens;
        this.completionTokens += other.completionTokens;
        this.totalTokens += other.totalTokens;
    }

    @Override
    public String toString() {
        return (
            "prompt=" + promptTokens + ", completion=" + completionTokens + ", total=" + totalTokens
        );
    }
}

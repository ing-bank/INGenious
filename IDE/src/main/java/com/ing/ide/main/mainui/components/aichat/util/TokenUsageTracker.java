package com.ing.ide.main.mainui.components.aichat.util;

import com.ing.ide.main.mainui.components.aichat.model.TokenUsage;

/**
 * Tracks token usage for a chat session: the most recent turn plus a running
 * cumulative total, and the latest rate-limit snapshot reported by the API.
 * Produces a short status string for the panel footer / status bar.
 */
public class TokenUsageTracker {
    private final TokenUsage cumulative = new TokenUsage();
    private TokenUsage lastTurn = new TokenUsage();

    // "Credits" = billable model requests. Tracked per task and per session.
    private int taskRequests;
    private int sessionRequests;
    private final TokenUsage taskUsage = new TokenUsage();

    private String rateLimitRemaining;
    private String rateLimitLimit;
    private String rateLimitReset;

    /** Marks the start of a new user task so per-task credits restart at zero. */
    public synchronized void beginTask() {
        taskRequests = 0;
        taskUsage.setPromptTokens(0);
        taskUsage.setCompletionTokens(0);
        taskUsage.setTotalTokens(0);
    }

    /** Records usage for one model response (one credit) within the current task. */
    public synchronized void record(TokenUsage turn) {
        if (turn == null) {
            return;
        }
        this.lastTurn = turn;
        this.cumulative.add(turn);
        this.taskUsage.add(turn);
        this.taskRequests++;
        this.sessionRequests++;
    }

    public synchronized void recordRateLimit(String remaining, String limit, String reset) {
        this.rateLimitRemaining = remaining;
        this.rateLimitLimit = limit;
        this.rateLimitReset = reset;
    }

    public synchronized TokenUsage getCumulative() {
        return cumulative;
    }

    public synchronized TokenUsage getLastTurn() {
        return lastTurn;
    }

    public synchronized void reset() {
        cumulative.setPromptTokens(0);
        cumulative.setCompletionTokens(0);
        cumulative.setTotalTokens(0);
        lastTurn = new TokenUsage();
        taskRequests = 0;
        sessionRequests = 0;
        taskUsage.setPromptTokens(0);
        taskUsage.setCompletionTokens(0);
        taskUsage.setTotalTokens(0);
    }

    /** Builds a compact summary suitable for a footer label. */
    public synchronized String statusText() {
        StringBuilder sb = new StringBuilder();
        sb
            .append("Credits this task: ")
            .append(taskRequests)
            .append(taskRequests == 1 ? " request" : " requests")
            .append(" · ")
            .append(taskUsage.getTotalTokens())
            .append(" tokens (")
            .append(taskUsage.getPromptTokens())
            .append(" in / ")
            .append(taskUsage.getCompletionTokens())
            .append(" out)")
            .append("  •  Session: ")
            .append(sessionRequests)
            .append(sessionRequests == 1 ? " request" : " requests")
            .append(" · ")
            .append(cumulative.getTotalTokens())
            .append(" tokens");
        if (rateLimitRemaining != null && rateLimitLimit != null) {
            sb.append("  •  Rate: ").append(rateLimitRemaining).append('/').append(rateLimitLimit);
        }
        return sb.toString();
    }
}

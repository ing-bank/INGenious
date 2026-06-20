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

    private String rateLimitRemaining;
    private String rateLimitLimit;
    private String rateLimitReset;

    /** Records usage for a completed turn and adds it to the cumulative total. */
    public synchronized void record(TokenUsage turn) {
        if (turn == null) {
            return;
        }
        this.lastTurn = turn;
        this.cumulative.add(turn);
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
    }

    /** Builds a compact summary suitable for a footer label. */
    public synchronized String statusText() {
        StringBuilder sb = new StringBuilder();
        sb
            .append("Last turn: ")
            .append(lastTurn.getPromptTokens())
            .append(" in / ")
            .append(lastTurn.getCompletionTokens())
            .append(" out")
            .append("  •  Session total: ")
            .append(cumulative.getTotalTokens())
            .append(" tokens");
        if (rateLimitRemaining != null && rateLimitLimit != null) {
            sb.append("  •  Rate: ").append(rateLimitRemaining).append('/').append(rateLimitLimit);
        }
        return sb.toString();
    }
}

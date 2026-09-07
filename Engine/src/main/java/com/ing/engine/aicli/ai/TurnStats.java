package com.ing.engine.aicli.ai;

/**
 * Per-turn usage summary for a self-agentic provider, used to render a
 * GitHub-Copilot-style status line (time · elapsed · model · credits).
 */
public final class TurnStats {
    public final long elapsedMillis;
    public final double credits;
    public final String model;
    public final long inputTokens;
    public final long outputTokens;

    public TurnStats(
        long elapsedMillis,
        double credits,
        String model,
        long inputTokens,
        long outputTokens
    ) {
        this.elapsedMillis = elapsedMillis;
        this.credits = credits;
        this.model = model;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
    }
}

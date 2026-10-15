package com.ing.engine.aicli.ai;

/**
 * How much the assistant may act on its own before checking in with the user.
 * Shared between the AI CLI and the IDE assistant so both surfaces give the
 * model identical instructions for the same choice.
 */
public enum OperatingMode {
    /** Works the request to completion on its own, self-correcting on failure. */
    UNATTENDED,
    /** Pauses at logical checkpoints — and on the first failure — to ask the user for help. */
    ATTENDED;

    /** Parses a persisted/CLI-flag value, defaulting to {@link #UNATTENDED} for anything else. */
    public static OperatingMode fromString(String s) {
        return s != null && s.trim().equalsIgnoreCase("attended") ? ATTENDED : UNATTENDED;
    }

    public String label() {
        return this == ATTENDED ? "attended" : "unattended";
    }

    /**
     * Instruction block appended to the agent's system prompt. Kept in one place so the
     * AI CLI and the IDE assistant behave identically for the same mode.
     */
    public String policyText() {
        if (this == ATTENDED) {
            return (
                "OPERATING MODE: ATTENDED — the user is watching this turn and available to help.\n" +
                "- For multi-phase work (e.g. browser test authoring: discover -> Playwright import -> " +
                "refine/parameterize -> validate -> run), finish one phase at a time and briefly state " +
                "what you completed before moving to the next.\n" +
                "- Attempt a run/execution step ONCE. If it fails, STOP — do not keep retrying, re-running, " +
                "or re-discovering within this turn. End the turn with a plain-text report covering: " +
                "(1) what you attempted, (2) the test case as it stands (scenario/test case name, steps, " +
                "objects used), (3) the relevant Object Repository entries (page, locators), (4) the data/" +
                "sheet rows involved, and (5) a specific question about what might be wrong. Wait for the " +
                "user's reply instead of guessing further — they usually know more about this project and " +
                "INGenious than you do."
            );
        }
        return (
            "OPERATING MODE: UNATTENDED — no one is watching this turn; work the request to completion " +
            "on your own. After a failed run/validate, diagnose from the failure details, fix the cause, " +
            "and re-run once more before reporting back. Only stop and ask the user if you are genuinely " +
            "blocked (e.g. missing credentials, or an ambiguous requirement with no reasonable default)."
        );
    }
}

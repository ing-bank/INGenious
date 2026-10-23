package com.ing.engine.aicli.ai;

/**
 * Streaming tool-activity callbacks for self-agentic providers (the Copilot
 * SDK/CLI). The REPL supplies a listener so it can show tools live during the
 * "Thinking" phase and build a final summary report.
 */
public interface CopilotActivity {
    /** A tool call is starting. {@code argsSummary} is a short, one-line preview. */
    void onToolStart(String toolCallId, String toolName, String argsSummary);

    /**
     * A tool call finished. {@code resultText} is the tool's full result payload
     * (untruncated where possible) so the listener can parse it as JSON for a
     * detailed report; truncate it yourself for any live, one-line preview.
     */
    void onToolComplete(String toolCallId, String toolName, boolean success, String resultText);
}

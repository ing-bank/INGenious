package com.ing.engine.aicli.ai;

import java.util.List;

/**
 * Provider abstraction: the planner and conversation layers never see
 * concrete provider types, so GitHub Copilot, OpenAI-compatible endpoints,
 * and local models are interchangeable via configuration.
 */
public interface AiProvider {
    /** Stable id, e.g. {@code copilot} or {@code openai}. */
    String id();

    String model();

    /** Human-readable status line for {@code /status}. */
    String describe();

    /** Blocking chat completion; returns the assistant message content. */
    String chat(List<ChatMessage> messages) throws AiException;

    /** Cumulative AI consumption (requests/credits + tokens) since this provider was created. */
    default Usage usage() {
        return null;
    }

    /** True when this provider supports OpenAI-style tool-calling (the ReAct loop). */
    default boolean supportsTools() {
        return false;
    }

    /**
     * True when this provider runs its own agentic loop (it plans, calls tools,
     * and iterates internally), so the REPL should hand it the user's prompt via
     * {@link #chat(List)} and print the final answer instead of driving INGenious's
     * own planner or ReAct loop. Used by the Copilot SDK/CLI provider.
     */
    default boolean isSelfAgentic() {
        return false;
    }

    /**
     * Registers a listener for live tool activity (self-agentic providers only).
     * Pass {@code null} to clear. No-op for providers that don't call tools.
     */
    default void setActivity(CopilotActivity activity) {}

    /**
     * Usage stats for the most recent {@link #chat(List)} turn (elapsed, credits,
     * model), or {@code null} if the provider doesn't report them.
     */
    default TurnStats lastTurnStats() {
        return null;
    }

    /**
     * Blocking tool-calling completion: send the conversation plus tool
     * definitions and return the assistant's text and/or requested tool calls.
     * Providers that do not support tools throw by default.
     */
    default AgentReply chatWithTools(List<AgentMessage> messages, List<ToolSpec> tools)
        throws AiException {
        throw new AiException("This provider does not support tool-calling.");
    }

    /** Failure talking to (or authenticating with) a provider. */
    class AiException extends Exception {

        public AiException(String message) {
            super(message);
        }

        public AiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

package com.ing.ide.main.mainui.components.aichat.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ing.ide.main.mainui.components.aichat.client.GitHubModelsClient;
import com.ing.ide.main.mainui.components.aichat.mcp.ToolProvider;
import com.ing.ide.main.mainui.components.aichat.mcp.ToolResult;
import com.ing.ide.main.mainui.components.aichat.model.ChatCompletionRequest;
import com.ing.ide.main.mainui.components.aichat.model.ChatCompletionResponse;
import com.ing.ide.main.mainui.components.aichat.model.ChatMessage;
import com.ing.ide.main.mainui.components.aichat.model.ChatSession;
import com.ing.ide.main.mainui.components.aichat.model.TokenUsage;
import com.ing.ide.main.mainui.components.aichat.model.ToolCall;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Drives the agent tool-calling loop: sends the conversation plus tool
 * definitions to the model, executes any returned tool calls through the
 * validated {@link INGeniousToolServer} (gated by an approval callback for
 * mutating tools), feeds the results back, and repeats until the model returns
 * a final textual answer or a safety limit is reached.
 *
 * <p>Runs synchronously on the calling (background) thread; the tool server
 * marshals mutations onto the EDT itself.</p>
 */
public class AgentOrchestrator {
    private static final Logger LOG = Logger.getLogger(AgentOrchestrator.class.getName());

    private static final int MAX_ITERATIONS = 20;

    /** Default cap on consecutive failed generation attempts before the loop stops and reports the failure. */
    public static final int DEFAULT_MAX_FAILED_ATTEMPTS = 5;

    // Cap tool-result text fed back to the model so the request body stays small
    // (large results otherwise accumulate and trip the provider's payload limit).
    private static final int MAX_TOOL_RESULT_CHARS = 6000;

    private final GitHubModelsClient client;
    private final ToolProvider toolServer;
    private final ObjectMapper mapper = new ObjectMapper();
    private final int maxFailedAttempts;

    private volatile boolean cancelled;

    public AgentOrchestrator(GitHubModelsClient client, ToolProvider toolServer) {
        this(client, toolServer, DEFAULT_MAX_FAILED_ATTEMPTS);
    }

    /**
     * @param maxFailedAttempts consecutive tool-call failures allowed before the loop stops
     *                          early (reset by any successful tool call); values &lt;= 0 fall
     *                          back to {@link #DEFAULT_MAX_FAILED_ATTEMPTS}.
     */
    public AgentOrchestrator(
        GitHubModelsClient client,
        ToolProvider toolServer,
        int maxFailedAttempts
    ) {
        this.client = client;
        this.toolServer = toolServer;
        this.maxFailedAttempts =
            maxFailedAttempts > 0 ? maxFailedAttempts : DEFAULT_MAX_FAILED_ATTEMPTS;
    }

    /** Approval gate for mutating tool calls. Implementations may block (e.g. a dialog). */
    public interface ApprovalGate {
        boolean approve(String toolName, String argumentsJson);
    }

    /** Progress and result callbacks for the agent turn. */
    public interface AgentListener {
        void onAssistantText(String text);

        void onToolStart(String toolName, String argumentsJson);

        void onToolResult(String toolName, boolean error, String summary);

        void onComplete();

        void onError(Throwable error);

        /** Reports the token usage of one model response (one credit) as it happens. */
        default void onUsage(TokenUsage usage) {}
    }

    public void cancel() {
        this.cancelled = true;
    }

    /**
     * Runs the agent loop against the given session.
     *
     * @param token    GitHub access token
     * @param session  conversation (its message history is extended in place)
     * @param gate     approval gate for mutating tools
     * @param listener progress/result callbacks
     */
    public void run(String token, ChatSession session, ApprovalGate gate, AgentListener listener) {
        cancelled = false;
        int consecutiveFailures = 0;
        try {
            for (int i = 0; i < MAX_ITERATIONS; i++) {
                if (cancelled) {
                    listener.onError(new InterruptedException("Agent cancelled."));
                    return;
                }
                ChatCompletionRequest request = new ChatCompletionRequest(
                    session.getModel(),
                    new ArrayList<>(session.getMessages()),
                    false
                );
                request.setTools(toolServer.toolDefinitions());

                ChatCompletionResponse response = client.complete(token, request);
                if (response.getUsage() != null) {
                    session.recordUsage(response.getUsage());
                    listener.onUsage(response.getUsage());
                }
                ChatMessage assistant = firstMessage(response);
                if (assistant == null) {
                    listener.onError(new IllegalStateException("Empty response from model."));
                    return;
                }
                session.addMessage(assistant);

                List<ToolCall> toolCalls = assistant.getToolCalls();
                if (toolCalls == null || toolCalls.isEmpty()) {
                    // Final answer.
                    if (assistant.getContent() != null) {
                        listener.onAssistantText(assistant.getContent());
                    }
                    listener.onComplete();
                    return;
                }

                // Surface any interim assistant text alongside the tool calls.
                if (assistant.getContent() != null && !assistant.getContent().isBlank()) {
                    listener.onAssistantText(assistant.getContent());
                }

                for (ToolCall call : toolCalls) {
                    if (cancelled) {
                        listener.onError(new InterruptedException("Agent cancelled."));
                        return;
                    }
                    ToolResult result = handleToolCall(call, gate, listener);
                    session.addMessage(
                        ChatMessage.toolResult(
                            call.getId(),
                            call.getName(),
                            cap(result.getContent())
                        )
                    );

                    if (result.isError()) {
                        consecutiveFailures++;
                        LOG.log(
                            Level.WARNING,
                            "Generation attempt failed ({0}/{1}): tool={2}, error={3}",
                            new Object[] {
                                consecutiveFailures,
                                maxFailedAttempts,
                                call.getName(),
                                result.getContent()
                            }
                        );
                    } else {
                        consecutiveFailures = 0;
                    }
                    if (consecutiveFailures >= maxFailedAttempts) {
                        listener.onError(
                            new IllegalStateException(
                                "Stopped after " +
                                maxFailedAttempts +
                                " consecutive failed generation attempts to avoid unnecessary AI " +
                                "requests. Review the errors above, then adjust your prompt, context, " +
                                "or inputs before trying again."
                            )
                        );
                        return;
                    }
                }
            }
            listener.onError(
                new IllegalStateException(
                    "Agent stopped after " + MAX_ITERATIONS + " steps without finishing."
                )
            );
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Agent loop failed", ex);
            listener.onError(ex);
        }
    }

    private ToolResult handleToolCall(ToolCall call, ApprovalGate gate, AgentListener listener) {
        String name = call.getName();
        String argsJson = call.getArguments() == null ? "{}" : call.getArguments();
        listener.onToolStart(name, argsJson);

        if (!toolServer.isKnownTool(name)) {
            ToolResult err = ToolResult.error("Unknown tool: " + name);
            listener.onToolResult(name, true, err.getContent());
            return err;
        }

        JsonNode args;
        try {
            JsonNode parsed = mapper.readTree(argsJson);
            args = parsed != null && parsed.isObject() ? parsed : mapper.createObjectNode();
        } catch (Exception ex) {
            ToolResult err = ToolResult.error("Could not parse tool arguments: " + ex.getMessage());
            listener.onToolResult(name, true, err.getContent());
            return err;
        }

        // Mutating tools require approval; read-only tools auto-run.
        if (!toolServer.isReadOnly(name)) {
            boolean approved = gate.approve(name, prettyArgs(args));
            if (!approved) {
                ToolResult denied = ToolResult.error(
                    "The user declined this action. Do not retry it; ask how to proceed."
                );
                listener.onToolResult(name, true, "Declined by user");
                return denied;
            }
        }

        ToolResult result = toolServer.execute(name, args);
        listener.onToolResult(name, result.isError(), result.getContent());
        return result;
    }

    private ChatMessage firstMessage(ChatCompletionResponse response) {
        if (response.getChoices() == null || response.getChoices().isEmpty()) {
            return null;
        }
        return response.getChoices().get(0).getMessage();
    }

    private String prettyArgs(JsonNode args) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(args);
        } catch (Exception ex) {
            return args.toString();
        }
    }

    private static String cap(String s) {
        if (s == null) {
            return "";
        }
        if (s.length() <= MAX_TOOL_RESULT_CHARS) {
            return s;
        }
        int dropped = s.length() - MAX_TOOL_RESULT_CHARS;
        return (
            s.substring(0, MAX_TOOL_RESULT_CHARS) +
            "\n…(truncated " +
            dropped +
            " chars; narrow the query or page the results)"
        );
    }
}

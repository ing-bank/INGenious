package com.ing.engine.aicli.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ing.engine.aicli.ai.AgentMessage;
import com.ing.engine.aicli.ai.AgentReply;
import com.ing.engine.aicli.ai.AgentToolCall;
import com.ing.engine.aicli.ai.AiProvider;
import com.ing.engine.aicli.ai.OperatingMode;
import com.ing.engine.aicli.ai.ToolSpec;
import com.ing.engine.aicli.execution.ExecutionEngine;
import com.ing.engine.aicli.execution.FileChange;
import com.ing.engine.aicli.execution.ProjectSnapshot;
import com.ing.engine.aicli.execution.UndoJournal;
import com.ing.engine.aicli.tools.Tool;
import com.ing.engine.aicli.tools.ToolException;
import com.ing.engine.aicli.tools.ToolRegistry;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Interactive ReAct agent loop for the CLI: sends the conversation plus the
 * full tool catalog to a tool-calling provider, executes the tool calls it
 * requests (mutating ones gated by an approval callback), feeds the results
 * back, and repeats until the model returns a final textual answer or a safety
 * limit is reached.
 *
 * <p>This mirrors the IDE's {@code AgentOrchestrator} so the CLI and IDE share
 * the same "observe → decide → act" behaviour over the same tools, instead of
 * the CLI's older one-shot planner (which could not inspect tool output before
 * committing to a plan).</p>
 */
public final class AgentLoop {
    private static final Logger LOG = Logger.getLogger(AgentLoop.class.getName());

    private static final int MAX_ITERATIONS = 25;
    private static final int MAX_RESULT_CHARS = 6000;

    /** Default cap on consecutive failed generation attempts before the loop stops; see {@link ProviderConfig#maxFailedAttempts}. */
    public static final int DEFAULT_MAX_FAILED_ATTEMPTS = 5;

    /**
     * Tool ids that mark a natural pause point in ATTENDED mode: either the end of a
     * phase (import/create, on success) or a step whose failure should not be retried
     * blindly (run/validate). Bare ids as returned by {@link Tool#id()}.
     */
    private static final java.util.Set<String> CHECKPOINT_ON_SUCCESS = java.util.Set.of(
        "import_playwright",
        "testcase_create"
    );
    private static final java.util.Set<String> CHECKPOINT_ON_FAILURE = java.util.Set.of(
        "run",
        "run_async",
        "testcase_validate"
    );

    /** Approval decision for a mutating tool call. */
    public enum Approval {
        YES,
        NO,
        ALL
    }

    /** UI callbacks so the REPL controls prompts and progress rendering. */
    public interface Ui {
        Approval approve(String tool, String prettyArgs);

        void onAssistantText(String text);

        void onToolResult(String tool, boolean error, String summary);

        void thinking(boolean on);

        /**
         * Called in ATTENDED mode when a tool call reaches a checkpoint (see
         * {@link #CHECKPOINT_ON_SUCCESS}/{@link #CHECKPOINT_ON_FAILURE}). {@code resultJson} is the
         * tool's full result so the UI can show the user enough to help (test case, OR, data, …).
         * Return free-text guidance to inject as the next user turn, or {@code null}/blank to let
         * the agent continue on its own.
         */
        default String checkpoint(String tool, boolean success, String resultJson) {
            return null;
        }
    }

    /** Result of an agent turn. */
    public static final class Outcome {
        public final String text;
        public final List<FileChange> changes;
        public final boolean hitLimit;
        /** True when the loop stopped early because {@link #DEFAULT_MAX_FAILED_ATTEMPTS} consecutive generation attempts failed. */
        public final boolean failureLimitReached;

        Outcome(
            String text,
            List<FileChange> changes,
            boolean hitLimit,
            boolean failureLimitReached
        ) {
            this.text = text;
            this.changes = changes;
            this.hitLimit = hitLimit;
            this.failureLimitReached = failureLimitReached;
        }
    }

    private final ToolRegistry registry;
    private final ObjectMapper mapper;

    public AgentLoop(ToolRegistry registry, ObjectMapper mapper) {
        this.registry = registry;
        this.mapper = mapper;
    }

    /**
     * Runs the loop. {@code messages} is extended in place with the assistant
     * and tool turns.
     */
    public Outcome run(
        AiProvider provider,
        List<AgentMessage> messages,
        String projectArg,
        Path projectRoot,
        UndoJournal journal,
        String goal,
        Ui ui
    )
        throws AiProvider.AiException {
        return run(
            provider,
            messages,
            projectArg,
            projectRoot,
            journal,
            goal,
            ui,
            OperatingMode.UNATTENDED
        );
    }

    /**
     * Runs the loop. {@code messages} is extended in place with the assistant
     * and tool turns. In {@link OperatingMode#ATTENDED}, checkpoint tool results
     * (see {@link #CHECKPOINT_ON_SUCCESS}/{@link #CHECKPOINT_ON_FAILURE}) pause via
     * {@link Ui#checkpoint(String, boolean, String)} before the loop continues.
     * Uses {@link #DEFAULT_MAX_FAILED_ATTEMPTS} as the consecutive-failure cap.
     */
    public Outcome run(
        AiProvider provider,
        List<AgentMessage> messages,
        String projectArg,
        Path projectRoot,
        UndoJournal journal,
        String goal,
        Ui ui,
        OperatingMode mode
    )
        throws AiProvider.AiException {
        return run(
            provider,
            messages,
            projectArg,
            projectRoot,
            journal,
            goal,
            ui,
            mode,
            DEFAULT_MAX_FAILED_ATTEMPTS
        );
    }

    /**
     * Runs the loop. {@code messages} is extended in place with the assistant
     * and tool turns. Stops early, without further AI requests, once
     * {@code maxFailedAttempts} consecutive tool-call failures are observed
     * (reset by any successful tool call); see {@link Outcome#failureLimitReached}.
     */
    public Outcome run(
        AiProvider provider,
        List<AgentMessage> messages,
        String projectArg,
        Path projectRoot,
        UndoJournal journal,
        String goal,
        Ui ui,
        OperatingMode mode,
        int maxFailedAttempts
    )
        throws AiProvider.AiException {
        Map<String, byte[]> snapshot = null;
        if (projectRoot != null) {
            try {
                snapshot = ProjectSnapshot.take(projectRoot);
            } catch (IOException e) {
                snapshot = null; // undo/diff unavailable; loop still runs
            }
        }

        List<ToolSpec> specs = buildSpecs();
        String finalText = null;
        boolean hitLimit = true;
        boolean failureLimitReached = false;
        boolean approveAll = false;
        int consecutiveFailures = 0;

        iterations:for (int i = 0; i < MAX_ITERATIONS; i++) {
            ui.thinking(true);
            AgentReply reply;
            try {
                reply = provider.chatWithTools(messages, specs);
            } finally {
                ui.thinking(false);
            }

            if (!reply.hasToolCalls()) {
                finalText = reply.content;
                if (finalText != null && !finalText.isBlank()) {
                    ui.onAssistantText(finalText);
                }
                messages.add(AgentMessage.assistant(finalText));
                hitLimit = false;
                break;
            }

            // Record the assistant's tool-call turn (with any interim text).
            messages.add(AgentMessage.assistantToolCalls(reply.content, reply.toolCalls));
            if (reply.content != null && !reply.content.isBlank()) {
                ui.onAssistantText(reply.content);
            }

            for (AgentToolCall call : reply.toolCalls) {
                Tool tool = registry.get(call.name);
                if (tool == null) {
                    String err = errorJson("Unknown tool: " + call.name);
                    ui.onToolResult(call.name, true, "unknown tool");
                    messages.add(AgentMessage.toolResult(call.id, call.name, err));
                    consecutiveFailures++;
                    LOG.log(
                        Level.WARNING,
                        "Generation attempt failed ({0}/{1}): unknown tool {2}",
                        new Object[] { consecutiveFailures, maxFailedAttempts, call.name }
                    );
                    if (consecutiveFailures >= maxFailedAttempts) {
                        failureLimitReached = true;
                        finalText = retryLimitMessage(maxFailedAttempts);
                        ui.onAssistantText(finalText);
                        messages.add(AgentMessage.assistant(finalText));
                        hitLimit = false;
                        break iterations;
                    }
                    continue;
                }

                ObjectNode args = parseArgs(call.argumentsJson);
                ExecutionEngine.injectProject(tool, args, projectArg);

                if (tool.mutatesFiles() && !approveAll) {
                    Approval decision = ui.approve(call.name, pretty(args));
                    if (decision == Approval.NO) {
                        String denied = errorJson(
                            "The user declined this action. Do not retry it; ask how to proceed."
                        );
                        ui.onToolResult(call.name, true, "declined by user");
                        messages.add(AgentMessage.toolResult(call.id, call.name, denied));
                        continue;
                    }
                    if (decision == Approval.ALL) {
                        approveAll = true;
                    }
                }

                String result;
                boolean error = false;
                try {
                    JsonNode out = execute(tool, args);
                    result = cap(out.toString());
                    ui.onToolResult(call.name, false, summarize(out));
                } catch (ToolException e) {
                    error = true;
                    result = errorJson(e.getMessage());
                    ui.onToolResult(call.name, true, e.getMessage());
                } catch (RuntimeException e) {
                    error = true;
                    result = errorJson(String.valueOf(e.getMessage()));
                    ui.onToolResult(call.name, true, String.valueOf(e.getMessage()));
                }
                messages.add(AgentMessage.toolResult(call.id, call.name, result));

                if (error) {
                    consecutiveFailures++;
                    LOG.log(
                        Level.WARNING,
                        "Generation attempt failed ({0}/{1}): tool={2}, error={3}",
                        new Object[] { consecutiveFailures, maxFailedAttempts, call.name, result }
                    );
                } else {
                    consecutiveFailures = 0;
                }
                if (consecutiveFailures >= maxFailedAttempts) {
                    failureLimitReached = true;
                    finalText = retryLimitMessage(maxFailedAttempts);
                    ui.onAssistantText(finalText);
                    messages.add(AgentMessage.assistant(finalText));
                    hitLimit = false;
                    break iterations;
                }

                if (mode == OperatingMode.ATTENDED && isCheckpoint(tool.id(), error)) {
                    String guidance = ui.checkpoint(call.name, !error, result);
                    if (guidance != null && !guidance.isBlank()) {
                        messages.add(AgentMessage.user(guidance));
                    }
                }
            }
        }

        List<FileChange> changes = new ArrayList<>();
        if (snapshot != null) {
            try {
                changes.addAll(ProjectSnapshot.diff(snapshot, projectRoot));
                if (journal != null && !changes.isEmpty()) {
                    journal.record("agent-" + System.currentTimeMillis(), goal, changes);
                }
            } catch (IOException e) {
                // mutation manifest unavailable; not fatal
            }
        }
        return new Outcome(finalText, changes, hitLimit, failureLimitReached);
    }

    /** User-facing message shown when the consecutive-failure cap stops the loop (ATDD scenario 2). */
    private static String retryLimitMessage(int maxFailedAttempts) {
        return (
            "Stopped after " +
            maxFailedAttempts +
            " consecutive failed generation attempts to avoid " +
            "unnecessary AI requests. Review the errors above, then adjust your prompt, context, or " +
            "inputs before trying again."
        );
    }

    /** True when {@code toolId}'s outcome (success or failure) should pause the loop in ATTENDED mode. */
    private boolean isCheckpoint(String toolId, boolean error) {
        return error
            ? CHECKPOINT_ON_FAILURE.contains(toolId)
            : CHECKPOINT_ON_SUCCESS.contains(toolId);
    }

    private List<ToolSpec> buildSpecs() {
        List<ToolSpec> specs = new ArrayList<>();
        for (Tool t : registry.all()) {
            specs.add(new ToolSpec(t.qualifiedName(), t.description(), t.inputSchema()));
        }
        return specs;
    }

    /** Suppress stray Datalib System.out chatter (except for streaming run tools). */
    private JsonNode execute(Tool tool, ObjectNode args) throws ToolException {
        if (tool.id().startsWith("run")) {
            return tool.execute(args);
        }
        java.io.PrintStream original = System.out;
        System.setOut(new java.io.PrintStream(java.io.OutputStream.nullOutputStream()));
        try {
            return tool.execute(args);
        } finally {
            System.setOut(original);
        }
    }

    private ObjectNode parseArgs(String argsJson) {
        try {
            JsonNode parsed = mapper.readTree(argsJson == null ? "{}" : argsJson);
            return parsed != null && parsed.isObject()
                ? (ObjectNode) parsed
                : mapper.createObjectNode();
        } catch (Exception e) {
            return mapper.createObjectNode();
        }
    }

    private String errorJson(String message) {
        return mapper.createObjectNode().put("error", message).toString();
    }

    private String cap(String s) {
        if (s == null) return "";
        return s.length() <= MAX_RESULT_CHARS ? s : s.substring(0, MAX_RESULT_CHARS) + "…";
    }

    private String pretty(JsonNode args) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(args);
        } catch (Exception e) {
            return args.toString();
        }
    }

    private String summarize(JsonNode result) {
        if (result == null) return "done";
        for (String key : new String[] { "message", "status", "summary" }) {
            JsonNode v = result.path(key);
            if (v.isTextual() && !v.asText().isBlank()) {
                return trunc(v.asText());
            }
        }
        if (result.path("created").isBoolean() && result.path("created").asBoolean()) {
            String name = result.path("testcase").asText(result.path("name").asText(""));
            return name.isEmpty() ? "created" : "created " + name;
        }
        if (result.isArray()) {
            return result.size() + " result(s)";
        }
        return trunc(result.toString());
    }

    private String trunc(String s) {
        String one = s.replace('\n', ' ');
        return one.length() <= 90 ? one : one.substring(0, 90) + "…";
    }
}

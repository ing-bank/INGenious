package com.ing.engine.aicli.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ing.engine.aicli.ai.AgentMessage;
import com.ing.engine.aicli.ai.AgentReply;
import com.ing.engine.aicli.ai.AgentToolCall;
import com.ing.engine.aicli.ai.AiProvider;
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
    private static final int MAX_ITERATIONS = 25;
    private static final int MAX_RESULT_CHARS = 6000;

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
    }

    /** Result of an agent turn. */
    public static final class Outcome {
        public final String text;
        public final List<FileChange> changes;
        public final boolean hitLimit;

        Outcome(String text, List<FileChange> changes, boolean hitLimit) {
            this.text = text;
            this.changes = changes;
            this.hitLimit = hitLimit;
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
        boolean approveAll = false;

        for (int i = 0; i < MAX_ITERATIONS; i++) {
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
                    // keep going: the model can read the error and self-correct
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
        return new Outcome(finalText, changes, hitLimit);
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

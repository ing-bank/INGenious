package com.ing.engine.aicli.execution;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ing.engine.aicli.planning.Plan;
import com.ing.engine.aicli.planning.Plan.PlanStep;
import com.ing.engine.aicli.planning.PlanValidator;
import com.ing.engine.aicli.tools.Tool;
import com.ing.engine.aicli.tools.ToolException;
import com.ing.engine.aicli.tools.ToolRegistry;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Runs a validated {@link Plan}: topological step order, project-arg
 * injection, {@code ${stepId.out.field}} output piping, streaming progress
 * events, mutation manifests via before/after snapshots, and undo journaling.
 * Steps run sequentially (mutating steps must never interleave).
 */
public final class ExecutionEngine {
    private static final Pattern REF = Pattern.compile(
        "\\$\\{(?<step>[A-Za-z0-9_-]+)\\.out\\.(?<path>[A-Za-z0-9_.]+)\\}"
    );

    private final ObjectMapper mapper = new ObjectMapper();
    private final ToolRegistry registry;

    public ExecutionEngine(ToolRegistry registry) {
        this.registry = registry;
    }

    /**
     * @param projectArg  value injected as {@code project} when a tool accepts it
     * @param projectRoot project directory for snapshots/undo (may be null)
     * @param journal     undo journal (may be null)
     */
    public ExecutionResult execute(
        Plan plan,
        String projectArg,
        Path projectRoot,
        UndoJournal journal,
        ExecutionListener listener
    ) {
        ExecutionResult result = new ExecutionResult(plan.id, plan.goal);
        List<PlanStep> order = PlanValidator.topologicalOrder(plan);
        listener.onPlanStart(plan, order.size());

        Map<String, byte[]> snapshot = null;
        boolean mutating = plan.hasMutatingSteps(registry);
        if (mutating && projectRoot != null) {
            try {
                snapshot = ProjectSnapshot.take(projectRoot);
            } catch (IOException e) {
                snapshot = null; // execution proceeds; undo unavailable
            }
        }

        int index = 0;
        for (PlanStep step : order) {
            index++;
            Tool tool = registry.get(step.tool);
            listener.onStepStart(step, index, order.size());
            try {
                ObjectNode args = step.args.deepCopy();
                resolveReferences(args, result);
                String unresolved = firstUnresolvedRef(args);
                if (unresolved != null) {
                    throw new RuntimeException(
                        "Unresolved reference " +
                        unresolved +
                        " — the referenced step produced no such field. " +
                        "Action lookups expose the action under 'name' (not 'action')."
                    );
                }
                injectProject(tool, args, projectArg);
                JsonNode stepResult = executeStep(step, tool, args);
                result.stepResults.put(step.id, stepResult);
                listener.onStepSuccess(step, stepResult, summarize(step, stepResult));
            } catch (ToolException e) {
                result.success = false;
                result.failedStepId = step.id;
                result.error = e.getMessage();
                listener.onStepFailure(step, e.getMessage(), e.suggestions());
                break;
            } catch (RuntimeException e) {
                result.success = false;
                result.failedStepId = step.id;
                result.error = String.valueOf(e.getMessage());
                listener.onStepFailure(step, String.valueOf(e.getMessage()), List.of());
                break;
            }
        }

        if (snapshot != null) {
            try {
                result.changes.addAll(ProjectSnapshot.diff(snapshot, projectRoot));
                if (journal != null && !result.changes.isEmpty()) {
                    journal.record(plan.id, plan.goal, result.changes);
                }
            } catch (IOException e) {
                // mutation manifest unavailable; not fatal
            }
        }

        listener.onPlanComplete(result);
        return result;
    }

    /**
     * Execute a step, suppressing stray Datalib {@code System.out} chatter so
     * the conversation stays clean. Tools that stream useful live output
     * (test runs) are exempt.
     */
    private JsonNode executeStep(PlanStep step, Tool tool, ObjectNode args) throws ToolException {
        if (step.tool.startsWith("run")) {
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

    /** Add {@code project} when the tool declares it and the plan didn't set it. */
    public static void injectProject(Tool tool, ObjectNode args, String projectArg) {
        if (
            projectArg != null &&
            !projectArg.isBlank() &&
            tool.inputSchema().path("properties").has("project") &&
            !args.hasNonNull("project")
        ) {
            args.put("project", projectArg);
        }
    }

    /** Replace {@code ${sN.out.a.b}} tokens in string values with prior step output. */
    private void resolveReferences(JsonNode node, ExecutionResult result) {
        if (node instanceof ObjectNode) {
            ObjectNode obj = (ObjectNode) node;
            Iterator<String> fields = obj.fieldNames();
            java.util.List<String> names = new java.util.ArrayList<>();
            fields.forEachRemaining(names::add);
            for (String f : names) {
                JsonNode child = obj.get(f);
                if (child.isTextual()) {
                    JsonNode whole = resolveWholeReference(child.asText(), result);
                    if (whole != null) {
                        obj.set(f, whole);
                    } else {
                        obj.put(f, resolveText(child.asText(), result));
                    }
                } else {
                    resolveReferences(child, result);
                }
            }
        } else if (node instanceof ArrayNode) {
            ArrayNode arr = (ArrayNode) node;
            for (int i = 0; i < arr.size(); i++) {
                JsonNode child = arr.get(i);
                if (child.isTextual()) {
                    JsonNode whole = resolveWholeReference(child.asText(), result);
                    if (whole != null) {
                        arr.set(i, whole);
                    } else {
                        arr.set(
                            i,
                            mapper.getNodeFactory().textNode(resolveText(child.asText(), result))
                        );
                    }
                } else {
                    resolveReferences(child, result);
                }
            }
        }
    }

    /**
     * When a string is EXACTLY one {@code ${sN.out.x}} token and the referenced
     * value is structured (array/object), return that node so plans can pipe
     * whole step lists (e.g. testcase_show → testcase_create). Null otherwise.
     */
    private JsonNode resolveWholeReference(String text, ExecutionResult result) {
        Matcher m = REF.matcher(text.trim());
        if (!m.matches()) return null;
        JsonNode stepResult = result.stepResults.get(m.group("step"));
        if (stepResult == null) return null;
        JsonNode v = navigate(stepResult, m.group("path"));
        if (v.isMissingNode() || v.isNull()) return null;
        return v.isValueNode() ? mapper.getNodeFactory().textNode(v.asText()) : v.deepCopy();
    }

    private String resolveText(String text, ExecutionResult result) {
        if (!text.contains("${")) return text;
        Matcher m = REF.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            JsonNode stepResult = result.stepResults.get(m.group("step"));
            String replacement = m.group(0);
            if (stepResult != null) {
                JsonNode v = navigate(stepResult, m.group("path"));
                if (!v.isMissingNode() && !v.isNull()) {
                    replacement = v.isValueNode() ? v.asText() : v.toString();
                }
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * Navigate a dotted path from a step result. Object fields are looked up by
     * name; when the current node is an array and the segment is an integer, it
     * is used as the array index (so {@code ${s1.out.0.name}} indexes the first
     * element of an array result). A bare integer segment on an array with a
     * following field is common for search/list tools that return arrays.
     */
    private static JsonNode navigate(JsonNode root, String dottedPath) {
        JsonNode v = root;
        for (String part : dottedPath.split("\\.")) {
            if (v.isArray() && part.matches("\\d+")) {
                v = v.path(Integer.parseInt(part));
            } else {
                v = v.path(part);
            }
        }
        return v;
    }

    /** Returns the first still-unresolved {@code ${sN.out.x}} token, or null. */
    private static String firstUnresolvedRef(JsonNode node) {
        if (node == null) return null;
        if (node.isTextual()) {
            Matcher m = REF.matcher(node.asText());
            return m.find() ? m.group(0) : null;
        }
        if (node.isObject() || node.isArray()) {
            for (JsonNode child : node) {
                String r = firstUnresolvedRef(child);
                if (r != null) return r;
            }
        }
        return null;
    }

    /** Short human summary of a tool result for the ✓ line. */
    static String summarize(PlanStep step, JsonNode result) {
        if (result == null) return step.tool;
        for (String key : new String[] { "message", "status", "summary" }) {
            JsonNode v = result.path(key);
            if (v.isTextual() && !v.asText().isBlank()) {
                return step.tool + " — " + truncate(v.asText(), 90);
            }
        }
        if (result.path("created").isBoolean() && result.path("created").asBoolean()) {
            String name = result.path("testcase").asText(result.path("name").asText(""));
            return step.tool + (name.isEmpty() ? " — created" : " — created " + name);
        }
        if (result.path("valid").isBoolean()) {
            return step.tool + (result.path("valid").asBoolean() ? " — valid" : " — INVALID");
        }
        return step.tool + " — " + truncate(result.toString(), 90);
    }

    private static String truncate(String s, int max) {
        String one = s.replace('\n', ' ');
        return one.length() <= max ? one : one.substring(0, max) + "…";
    }
}

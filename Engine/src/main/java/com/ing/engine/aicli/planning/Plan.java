package com.ing.engine.aicli.planning;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ing.engine.aicli.tools.Tool;
import com.ing.engine.aicli.tools.ToolRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * A reviewable, replayable execution plan: an ordered DAG of tool
 * invocations. Plans are pure data (JSON) so they can be shown to the user
 * ({@code /plan}), approved, executed, journaled, and undone.
 */
public final class Plan {
    public final String id;
    public final String goal;
    public final List<PlanStep> steps;

    public Plan(String goal, List<PlanStep> steps) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.goal = goal == null ? "" : goal;
        this.steps = Collections.unmodifiableList(new ArrayList<>(steps));
    }

    public static Plan fromJson(JsonNode node, ObjectMapper mapper) {
        List<PlanStep> steps = new ArrayList<>();
        int i = 1;
        for (JsonNode s : node.path("steps")) {
            String stepId = s.path("id").asText("s" + i);
            String tool = s.path("tool").asText("");
            ObjectNode args = s.path("args").isObject()
                ? ((ObjectNode) s.get("args")).deepCopy()
                : mapper.createObjectNode();
            List<String> deps = new ArrayList<>();
            for (JsonNode d : s.path("dependsOn")) deps.add(d.asText());
            steps.add(new PlanStep(stepId, tool, args, deps));
            i++;
        }
        return new Plan(node.path("goal").asText(""), steps);
    }

    public ObjectNode toJson(ObjectMapper mapper) {
        ObjectNode n = mapper.createObjectNode();
        n.put("id", id);
        n.put("goal", goal);
        ArrayNode arr = n.putArray("steps");
        for (PlanStep s : steps) {
            ObjectNode sn = arr.addObject();
            sn.put("id", s.id);
            sn.put("tool", s.tool);
            sn.set("args", s.args);
            if (!s.dependsOn.isEmpty()) {
                ArrayNode deps = sn.putArray("dependsOn");
                s.dependsOn.forEach(deps::add);
            }
        }
        return n;
    }

    public boolean hasMutatingSteps(ToolRegistry registry) {
        for (PlanStep s : steps) {
            Tool t = registry.get(s.tool);
            if (t != null && t.mutatesFiles()) return true;
        }
        return false;
    }

    /** One tool invocation within a plan. */
    public static final class PlanStep {
        public final String id;
        public final String tool;
        public final ObjectNode args;
        public final List<String> dependsOn;

        public PlanStep(String id, String tool, ObjectNode args, List<String> dependsOn) {
            this.id = id;
            this.tool = ToolRegistry.normalize(tool);
            this.args = args;
            this.dependsOn = Collections.unmodifiableList(new ArrayList<>(dependsOn));
        }
    }
}

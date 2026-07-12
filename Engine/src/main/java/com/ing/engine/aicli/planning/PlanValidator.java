package com.ing.engine.aicli.planning;

import com.ing.engine.aicli.planning.Plan.PlanStep;
import com.ing.engine.aicli.tools.ToolRegistry;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Deterministic guardrail around plans (AI-generated or otherwise): every
 * referenced tool must exist, dependencies must reference known steps, and
 * the dependency graph must be acyclic.
 */
public final class PlanValidator {

    private PlanValidator() {}

    /** Returns a list of problems; empty means valid. */
    public static List<String> validate(Plan plan, ToolRegistry registry) {
        List<String> errors = new ArrayList<>();
        if (plan.steps.isEmpty()) {
            errors.add("Plan has no steps.");
            return errors;
        }
        Set<String> ids = new HashSet<>();
        for (PlanStep s : plan.steps) {
            if (!ids.add(s.id)) {
                errors.add("Duplicate step id: " + s.id);
            }
            if (registry.get(s.tool) == null) {
                errors.add("Unknown tool '" + s.tool + "' in step " + s.id);
            }
        }
        for (PlanStep s : plan.steps) {
            for (String dep : s.dependsOn) {
                if (!ids.contains(dep)) {
                    errors.add("Step " + s.id + " depends on unknown step '" + dep + "'");
                }
            }
        }
        if (errors.isEmpty()) {
            try {
                topologicalOrder(plan);
            } catch (IllegalArgumentException e) {
                errors.add(e.getMessage());
            }
        }
        return errors;
    }

    /** Kahn's algorithm; preserves declaration order among ready steps. */
    public static List<PlanStep> topologicalOrder(Plan plan) {
        Map<String, PlanStep> byId = new HashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> dependents = new HashMap<>();
        for (PlanStep s : plan.steps) {
            byId.put(s.id, s);
            inDegree.put(s.id, s.dependsOn.size());
        }
        for (PlanStep s : plan.steps) {
            for (String dep : s.dependsOn) {
                dependents.computeIfAbsent(dep, k -> new ArrayList<>()).add(s.id);
            }
        }
        Deque<String> ready = new ArrayDeque<>();
        for (PlanStep s : plan.steps) {
            if (s.dependsOn.isEmpty()) ready.add(s.id);
        }
        List<PlanStep> order = new ArrayList<>();
        while (!ready.isEmpty()) {
            String id = ready.poll();
            order.add(byId.get(id));
            for (String next : dependents.getOrDefault(id, List.of())) {
                int deg = inDegree.merge(next, -1, Integer::sum);
                if (deg == 0) ready.add(next);
            }
        }
        if (order.size() != plan.steps.size()) {
            throw new IllegalArgumentException("Plan has a dependency cycle.");
        }
        return order;
    }
}

package com.ing.engine.aicli.execution;

import com.fasterxml.jackson.databind.JsonNode;
import com.ing.engine.aicli.planning.Plan;
import com.ing.engine.aicli.planning.Plan.PlanStep;
import java.util.List;

/** Progress events streamed by the {@link ExecutionEngine} to UI renderers. */
public interface ExecutionListener {
    default void onPlanStart(Plan plan, int totalSteps) {}

    default void onStepStart(PlanStep step, int index, int total) {}

    default void onStepSuccess(PlanStep step, JsonNode result, String summary) {}

    default void onStepFailure(PlanStep step, String error, List<String> suggestions) {}

    default void onPlanComplete(ExecutionResult result) {}
}

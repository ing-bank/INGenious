package com.ing.engine.aicli.execution;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Outcome of executing a {@code Plan}: per-step results plus file changes. */
public final class ExecutionResult {
    public final String planId;
    public final String goal;
    public final Map<String, JsonNode> stepResults = new LinkedHashMap<>();
    public final List<FileChange> changes = new ArrayList<>();
    public boolean success = true;
    public String failedStepId;
    public String error;

    public ExecutionResult(String planId, String goal) {
        this.planId = planId;
        this.goal = goal;
    }
}

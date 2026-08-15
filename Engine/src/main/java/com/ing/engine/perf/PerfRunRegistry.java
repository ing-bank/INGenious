package com.ing.engine.perf;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks asynchronous k6 runs. In-memory for the current process (MCP
 * server, IDE) plus file-based discovery through the {@code run.json}
 * persisted at start, so a separate CLI invocation can status/cancel a
 * detached run.
 */
public final class PerfRunRegistry {
    private static final Map<String, PerfRunHandle> LIVE = new ConcurrentHashMap<>();

    private PerfRunRegistry() {}

    static void register(PerfRunHandle handle) {
        LIVE.put(handle.runId, handle);
    }

    /**
     * Find a run by id ("<script>/<timestamp>") — in-memory first, then via
     * the persisted run.json under Results/Performance/.
     */
    public static PerfRunHandle find(PerfWorkspace workspace, String runId) {
        PerfRunHandle live = LIVE.get(runId);
        if (live != null) {
            return live;
        }
        File runDir = new File(workspace.resultsDir(), runId);
        JsonNode meta = PerfReportStore.runMeta(runDir);
        if (meta == null || !meta.hasNonNull("pid")) {
            return null;
        }
        return PerfRunHandle.fromRunMeta(runDir, meta);
    }

    /**
     * The newest run whose run.json says RUNNING (validating liveness via
     * the pid), or null. Used when status/cancel are called without an id.
     */
    public static PerfRunHandle latestRunning(PerfWorkspace workspace) {
        for (File runDir : workspace.listRuns()) {
            JsonNode meta = PerfReportStore.runMeta(runDir);
            if (meta == null || !"RUNNING".equals(meta.path("status").asText(""))) {
                continue;
            }
            PerfRunHandle handle = PerfRunHandle.fromRunMeta(runDir, meta);
            if (handle.isAlive()) {
                return handle;
            }
        }
        return null;
    }

    /** All runs currently marked RUNNING and actually alive. */
    public static List<PerfRunHandle> running(PerfWorkspace workspace) {
        List<PerfRunHandle> out = new ArrayList<>();
        for (File runDir : workspace.listRuns()) {
            JsonNode meta = PerfReportStore.runMeta(runDir);
            if (meta == null || !"RUNNING".equals(meta.path("status").asText(""))) {
                continue;
            }
            PerfRunHandle handle = PerfRunHandle.fromRunMeta(runDir, meta);
            if (handle.isAlive()) {
                out.add(handle);
            }
        }
        return out;
    }
}

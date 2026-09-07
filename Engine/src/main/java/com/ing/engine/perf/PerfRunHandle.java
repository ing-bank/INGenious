package com.ing.engine.perf;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.File;
import java.util.Optional;

/**
 * Handle to an asynchronous k6 run. Works both for runs started in this
 * process (live {@link Process}) and for runs discovered from a persisted
 * {@code run.json} (cross-process: the CLI exits after {@code --detach},
 * a later CLI invocation controls the run via pid + k6 REST API).
 */
public final class PerfRunHandle {
    /** "<scriptBase>/<timestamp>", matches the run folder path. */
    public final String runId;
    public final File runDir;
    public final String script;
    public final long pid;
    public final int apiPort;
    /** 0 when the web dashboard was not enabled. */
    public final int dashboardPort;
    /** Live process when started in this JVM; null for discovered runs. */
    final Process process;

    PerfRunHandle(
        String runId,
        File runDir,
        String script,
        long pid,
        int apiPort,
        int dashboardPort,
        Process process
    ) {
        this.runId = runId;
        this.runDir = runDir;
        this.script = script;
        this.pid = pid;
        this.apiPort = apiPort;
        this.dashboardPort = dashboardPort;
        this.process = process;
    }

    /** Rehydrate a handle from a persisted run.json. */
    public static PerfRunHandle fromRunMeta(File runDir, JsonNode meta) {
        return new PerfRunHandle(
            meta.path("runId").asText(runDir.getParentFile().getName() + "/" + runDir.getName()),
            runDir,
            meta.path("script").asText(""),
            meta.path("pid").asLong(0),
            meta.path("apiPort").asInt(0),
            meta.path("dashboardPort").asInt(0),
            null
        );
    }

    public boolean isAlive() {
        if (process != null) {
            return process.isAlive();
        }
        if (pid <= 0) {
            return false;
        }
        Optional<ProcessHandle> handle = ProcessHandle.of(pid);
        return handle.isPresent() && handle.get().isAlive();
    }

    /**
     * Lifecycle phase:
     * <ul>
     *   <li>{@code RUNNING} — process alive, test executing</li>
     *   <li>{@code DRAINING} — test complete but the process is waiting for
     *       web-dashboard viewers (open SSE connections) to disconnect. k6
     *       exits — and flushes summary.json — about a second after the last
     *       dashboard tab closes.</li>
     *   <li>{@code FINISHED} — process gone</li>
     * </ul>
     */
    public String phase() {
        if (!isAlive()) {
            return "FINISHED";
        }
        if (apiPort > 0) {
            JsonNode status = K6MetricsTap.status(apiPort);
            if (status != null && !status.path("running").asBoolean(true)) {
                return "DRAINING";
            }
        }
        return "RUNNING";
    }

    /** Exit code when the process ended in this JVM; null otherwise. */
    public Integer exitCode() {
        if (process != null && !process.isAlive()) {
            return process.exitValue();
        }
        return null;
    }

    public String dashboardUrl() {
        return dashboardPort > 0 ? "http://127.0.0.1:" + dashboardPort : null;
    }

    /**
     * Cancel the run: graceful stop via the k6 REST API first, then a
     * process kill as fallback. Returns true when the run is (now) down.
     */
    public boolean cancel() {
        if (!isAlive()) {
            return true;
        }
        if (apiPort > 0 && K6MetricsTap.stop(apiPort)) {
            // graceful stop: give k6 a moment to flush the summary
            for (int i = 0; i < 20 && isAlive(); i++) {
                sleep(250);
            }
        }
        if (isAlive()) {
            if (process != null) {
                process.destroy();
            } else {
                ProcessHandle.of(pid).ifPresent(ProcessHandle::destroy);
            }
            for (int i = 0; i < 8 && isAlive(); i++) {
                sleep(250);
            }
            if (isAlive()) {
                if (process != null) {
                    process.destroyForcibly();
                } else {
                    ProcessHandle.of(pid).ifPresent(ProcessHandle::destroyForcibly);
                }
            }
        }
        return !isAlive();
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

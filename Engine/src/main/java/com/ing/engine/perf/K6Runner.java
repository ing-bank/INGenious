package com.ing.engine.perf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Spawns {@code k6 run} for a script and materializes the run folder under
 * {@code Results/Performance/<script>/<timestamp>/}:
 *
 * <pre>
 *   summary.json   k6 --summary-export
 *   run.json       INGenious metadata (script, profile, exit code, k6 version)
 *   output.log     captured stdout/stderr (validate mode only)
 * </pre>
 *
 * <p>k6 exit code 99 means "thresholds crossed" — the run executed fully but
 * failed its pass/fail criteria; callers should report that distinctly from
 * a crash.
 */
public final class K6Runner {
    /** k6's documented exit code for failed thresholds. */
    public static final int EXIT_THRESHOLDS_FAILED = 99;

    public static final class RunResult {
        public File runDir;
        public File summaryFile;
        public int exitCode;
        public boolean thresholdsFailed;
        /** Captured output (validate mode); null when IO was inherited. */
        public String output;
    }

    private K6Runner() {}

    /**
     * Load run: k6 output streams straight to the console (live progress bar),
     * summary + metadata land in the run folder.
     *
     * @param extraArgs extra k6 CLI args (e.g. --vus 5 --duration 30s), may be empty
     */
    public static RunResult run(
        String k6Binary,
        File script,
        PerfWorkspace workspace,
        String profileName,
        List<String> extraArgs
    )
        throws Exception {
        return execute(k6Binary, script, workspace, profileName, extraArgs, false, null);
    }

    /**
     * Load run with captured output (for embedding hosts like the MCP server,
     * where inheriting IO would corrupt the JSON-RPC stdout stream).
     */
    public static RunResult runCaptured(
        String k6Binary,
        File script,
        PerfWorkspace workspace,
        String profileName,
        List<String> extraArgs
    )
        throws Exception {
        return execute(k6Binary, script, workspace, profileName, extraArgs, true, null);
    }

    /**
     * Validate (debug) run — k6-studio's Validator: 1 VU, 1 iteration.
     * HTTP scripts additionally get full request/response tracing. Browser
     * scripts are switched via the K6_PERF_VALIDATE env var instead of
     * --vus/--iterations flags, because CLI executor flags REPLACE the
     * scenario definition and would drop the browser type option.
     */
    public static RunResult validate(String k6Binary, File script, PerfWorkspace workspace)
        throws Exception {
        List<String> args = new ArrayList<>();
        java.util.Map<String, String> env = new java.util.LinkedHashMap<>();
        if (isBrowserScript(script)) {
            env.put("K6_PERF_VALIDATE", "1");
        } else {
            args.add("--vus");
            args.add("1");
            args.add("--iterations");
            args.add("1");
            args.add("--http-debug=full");
        }
        return execute(k6Binary, script, workspace, "validate", args, true, env);
    }

    /** True when the script imports the k6 browser module. */
    static boolean isBrowserScript(File script) {
        try {
            String head = new String(
                java.nio.file.Files.readAllBytes(script.toPath()),
                StandardCharsets.UTF_8
            );
            return head.contains("k6/browser");
        } catch (Exception e) {
            return false;
        }
    }

    private static RunResult execute(
        String k6Binary,
        File script,
        PerfWorkspace workspace,
        String profileName,
        List<String> extraArgs,
        boolean captureOutput,
        java.util.Map<String, String> env
    )
        throws Exception {
        if (k6Binary == null) {
            throw new IllegalStateException("k6 binary not found. " + K6Locator.installHint());
        }
        if (!script.isFile()) {
            throw new IllegalArgumentException("Script not found: " + script);
        }
        File runDir = newRunDir(workspace, script);
        File summary = new File(runDir, "summary.json");

        List<String> cmd = new ArrayList<>();
        cmd.add(k6Binary);
        cmd.add("run");
        cmd.add("--summary-export");
        cmd.add(summary.getAbsolutePath());
        if (extraArgs != null) {
            cmd.addAll(extraArgs);
        }
        cmd.add(script.getAbsolutePath());

        long startedAt = System.currentTimeMillis();
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(script.getParentFile());
        if (env != null) {
            pb.environment().putAll(env);
        }
        String captured = null;
        Process process;
        if (captureOutput) {
            pb.redirectErrorStream(true);
            process = pb.start();
            captured = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            process.waitFor();
        } else {
            pb.inheritIO();
            process = pb.start();
            process.waitFor();
        }
        int exit = process.exitValue();

        RunResult result = new RunResult();
        result.runDir = runDir;
        result.summaryFile = summary.isFile() ? summary : null;
        result.exitCode = exit;
        result.thresholdsFailed = exit == EXIT_THRESHOLDS_FAILED;
        result.output = captured;
        if (captured != null) {
            java.nio.file.Files.write(
                new File(runDir, "output.log").toPath(),
                captured.getBytes(StandardCharsets.UTF_8)
            );
        }
        writeRunMeta(runDir, script, profileName, cmd, k6Binary, startedAt, exit);
        return result;
    }

    private static File newRunDir(PerfWorkspace workspace, File script) {
        String base = script.getName();
        int dot = base.lastIndexOf('.');
        if (dot > 0) {
            base = base.substring(0, dot);
        }
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        File dir = new File(workspace.resultsDir(), base + "/" + stamp);
        dir.mkdirs();
        return dir;
    }

    // ==================================================================
    // asynchronous (live) runs — Phase 4
    // ==================================================================

    /**
     * Start a detached k6 run with the REST API bound to a free port (live
     * metrics via {@link K6MetricsTap}) and, optionally, the built-in k6 web
     * dashboard (live graphs in a browser + report.html export at run end).
     *
     * <p>Output goes to {@code <runDir>/output.log}; {@code run.json} is
     * written immediately with {@code status: RUNNING} + pid + ports so any
     * other process (a later CLI call, the IDE) can find and control the run.
     * A watcher thread finalizes run.json when the process exits — and if the
     * spawning JVM dies first, liveness falls back to a pid probe.
     */
    public static PerfRunHandle startAsync(
        String k6Binary,
        File script,
        PerfWorkspace workspace,
        String profileName,
        List<String> extraArgs,
        boolean dashboard
    )
        throws Exception {
        if (k6Binary == null) {
            throw new IllegalStateException("k6 binary not found. " + K6Locator.installHint());
        }
        if (!script.isFile()) {
            throw new IllegalArgumentException("Script not found: " + script);
        }
        File runDir = newRunDir(workspace, script);
        File summary = new File(runDir, "summary.json");
        int apiPort = freePort();
        int dashboardPort = dashboard ? freePort() : 0;

        List<String> cmd = new ArrayList<>();
        cmd.add(k6Binary);
        cmd.add("run");
        cmd.add("--address");
        cmd.add("127.0.0.1:" + apiPort);
        cmd.add("--summary-export");
        cmd.add(summary.getAbsolutePath());
        if (extraArgs != null) {
            cmd.addAll(extraArgs);
        }
        cmd.add(script.getAbsolutePath());

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(script.getParentFile());
        pb.redirectErrorStream(true);
        pb.redirectOutput(new File(runDir, "output.log"));
        if (dashboard) {
            pb.environment().put("K6_WEB_DASHBOARD", "true");
            pb.environment().put("K6_WEB_DASHBOARD_PORT", String.valueOf(dashboardPort));
            pb
                .environment()
                .put("K6_WEB_DASHBOARD_EXPORT", new File(runDir, "report.html").getAbsolutePath());
        }
        long startedAt = System.currentTimeMillis();
        Process process = pb.start();

        String runId = runDir.getParentFile().getName() + "/" + runDir.getName();
        PerfRunHandle handle = new PerfRunHandle(
            runId,
            runDir,
            script.getAbsolutePath(),
            process.pid(),
            apiPort,
            dashboardPort,
            process
        );
        writeAsyncRunMeta(handle, profileName, cmd, k6Binary, startedAt, null);
        PerfRunRegistry.register(handle);

        // finalize run.json when k6 exits (daemon: don't block JVM shutdown)
        Thread watcher = new Thread(
            () -> {
                try {
                    int exit = process.waitFor();
                    writeAsyncRunMeta(handle, profileName, cmd, k6Binary, startedAt, exit);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            },
            "k6-run-watcher-" + runId
        );
        watcher.setDaemon(true);
        watcher.start();
        return handle;
    }

    /**
     * Refresh a persisted run.json for a run whose watcher died with its
     * spawning JVM: if the pid is gone and status still says RUNNING, mark
     * it FINISHED (exit code unknown, thresholds read from summary.json).
     */
    public static void reconcileRunMeta(PerfRunHandle handle) {
        JsonNode meta = PerfReportStore.runMeta(handle.runDir);
        if (meta == null || !"RUNNING".equals(meta.path("status").asText(""))) {
            return;
        }
        if (handle.isAlive()) {
            return;
        }
        try {
            ObjectMapper json = new ObjectMapper();
            ObjectNode updated = (ObjectNode) meta;
            updated.put("status", "FINISHED");
            updated.put("finishedAt", Instant.now().toString());
            json
                .writerWithDefaultPrettyPrinter()
                .writeValue(new File(handle.runDir, "run.json"), updated);
        } catch (Exception e) {
            // best effort
        }
    }

    private static void writeAsyncRunMeta(
        PerfRunHandle handle,
        String profileName,
        List<String> cmd,
        String k6Binary,
        long startedAt,
        Integer exitCode
    ) {
        try {
            ObjectMapper json = new ObjectMapper();
            ObjectNode meta = json.createObjectNode();
            meta.put("runId", handle.runId);
            meta.put("script", handle.script);
            meta.put("profile", profileName);
            meta.put("startedAt", Instant.ofEpochMilli(startedAt).toString());
            meta.put("pid", handle.pid);
            meta.put("apiPort", handle.apiPort);
            meta.put("dashboardPort", handle.dashboardPort);
            if (exitCode == null) {
                meta.put("status", "RUNNING");
            } else {
                meta.put("status", "FINISHED");
                meta.put("finishedAt", Instant.now().toString());
                meta.put("exitCode", exitCode.intValue());
                meta.put("thresholdsFailed", exitCode.intValue() == EXIT_THRESHOLDS_FAILED);
            }
            String version = K6Locator.version(k6Binary);
            meta.put("k6Version", version == null ? "" : version);
            meta.put("command", String.join(" ", cmd));
            json
                .writerWithDefaultPrettyPrinter()
                .writeValue(new File(handle.runDir, "run.json"), meta);
        } catch (Exception e) {
            // metadata is best-effort
        }
    }

    private static int freePort() throws Exception {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static void writeRunMeta(
        File runDir,
        File script,
        String profileName,
        List<String> cmd,
        String k6Binary,
        long startedAt,
        int exitCode
    ) {
        try {
            ObjectMapper json = new ObjectMapper();
            ObjectNode meta = json.createObjectNode();
            meta.put("script", script.getAbsolutePath());
            meta.put("profile", profileName);
            meta.put("startedAt", Instant.ofEpochMilli(startedAt).toString());
            meta.put("finishedAt", Instant.now().toString());
            meta.put("exitCode", exitCode);
            meta.put("thresholdsFailed", exitCode == EXIT_THRESHOLDS_FAILED);
            String version = K6Locator.version(k6Binary);
            meta.put("k6Version", version == null ? "" : version);
            meta.put("command", String.join(" ", cmd));
            json.writerWithDefaultPrettyPrinter().writeValue(new File(runDir, "run.json"), meta);
        } catch (Exception e) {
            // metadata is best-effort; the run itself already happened
        }
    }
}

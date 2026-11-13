package com.ing.engine.aicli.ai;

import com.github.copilot.AllowCopilotExperimental;
import com.github.copilot.CopilotClient;
import com.github.copilot.CopilotSession;
import com.github.copilot.SystemMessageMode;
import com.github.copilot.generated.AssistantMessageEvent;
import com.github.copilot.generated.AssistantUsageEvent;
import com.github.copilot.generated.SessionUsageInfoEvent;
import com.github.copilot.generated.ToolExecutionCompleteEvent;
import com.github.copilot.generated.ToolExecutionStartEvent;
import com.github.copilot.generated.rpc.McpServer;
import com.github.copilot.generated.rpc.McpServerStatus;
import com.github.copilot.generated.rpc.SessionMcpListResult;
import com.github.copilot.rpc.CopilotClientOptions;
import com.github.copilot.rpc.McpServerConfig;
import com.github.copilot.rpc.McpStdioServerConfig;
import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.ModelInfo;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.SessionConfig;
import com.github.copilot.rpc.SystemMessageConfig;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * GitHub Copilot provider that drives the official Copilot CLI through the
 * {@code copilot-sdk-java} SDK, instead of talking to any HTTP endpoint.
 *
 * <p><strong>Option B ("delegate agency"):</strong> the Copilot CLI runs its own
 * agentic loop and is given the existing INGenious MCP server
 * ({@code Control server mcp}) as a stdio MCP server, so the model can call all
 * of INGenious's {@code ingenious_*} tools directly. This provider is therefore
 * {@linkplain #isSelfAgentic() self-agentic}: the REPL hands it the user's prompt
 * and prints the final answer, rather than running INGenious's own ReAct loop.
 *
 * <p>Prerequisite: the {@code copilot} CLI must be installed, on {@code PATH},
 * and authenticated ({@code copilot auth login}). This is a proof of concept.
 */
public final class CopilotSdkProvider implements AiProvider {
    private static final String MCP_SERVER_NAME = "ingenious";
    private static final String MCP_STDERR_LOG = ".ingenious-mcp-stderr.log";
    private static final String MCP_ARGFILE = ".ingenious-mcp.args";
    private static final String MCP_TIMEOUT_PROPERTY = "ingenious.mcp.startupTimeoutSec";
    private static final String MCP_TIMEOUT_ENV = "INGENIOUS_MCP_STARTUP_TIMEOUT";
    private static final long DEFAULT_MCP_STARTUP_TIMEOUT_SEC = 120;
    private static final long MCP_LIST_TIMEOUT_SEC = 30;
    private static final long MCP_POLL_INTERVAL_MS = 500;
    private static final int MCP_LOG_TAIL_BYTES = 16_384;
    private static final int MCP_LOG_TAIL_LINES = 40;

    private final String requestedModel;
    private final Supplier<String> projectDir;
    private final Usage usage = new Usage();
    private volatile String activeModel;

    private CopilotClient client;
    private CopilotSession session;
    private boolean firstTurn = true;
    private volatile int lastContextTokens;
    private volatile CopilotActivity activity;
    private final Map<String, String> toolNamesById = new ConcurrentHashMap<>();

    // Per-turn accounting, accumulated from AssistantUsageEvent (fires on SDK threads).
    private final java.util.concurrent.atomic.DoubleAdder turnCredits = new java.util.concurrent.atomic.DoubleAdder();
    private final java.util.concurrent.atomic.LongAdder turnInputTokens = new java.util.concurrent.atomic.LongAdder();
    private final java.util.concurrent.atomic.LongAdder turnOutputTokens = new java.util.concurrent.atomic.LongAdder();
    private volatile String turnModel;
    private volatile TurnStats lastTurnStats;

    // Activity-based turn watchdog: keep a long-but-progressing turn alive and
    // fail only on a genuine stall (no tool/token events while nothing runs).
    private volatile long lastActivityAt;
    private final AtomicInteger activeTools = new AtomicInteger();

    public CopilotSdkProvider(String model, Supplier<String> projectDir) {
        this.requestedModel = model == null || model.isBlank() ? null : model;
        this.activeModel = requestedModel != null ? requestedModel : "Copilot default";
        this.projectDir = projectDir != null ? projectDir : () -> null;
    }

    @Override
    public String id() {
        return "copilot-sdk";
    }

    @Override
    public String model() {
        return activeModel;
    }

    public String configuredModel() {
        return requestedModel == null ? "" : requestedModel;
    }

    @Override
    public String describe() {
        return "GitHub Copilot SDK (drives the Copilot CLI, model " + activeModel + ")";
    }

    @Override
    public Usage usage() {
        return usage.copy();
    }

    @Override
    public boolean isSelfAgentic() {
        return true;
    }

    @Override
    public void setActivity(CopilotActivity activity) {
        this.activity = activity;
    }

    @Override
    public TurnStats lastTurnStats() {
        return lastTurnStats;
    }

    /**
     * Eagerly starts the Copilot CLI client and opens the session, so the caller
     * can validate that the CLI is installed and authenticated before the first
     * prompt. Throws {@link AiException} on failure.
     */
    public void warmUp() throws AiException {
        ensureSession();
    }

    @Override
    public String chat(List<ChatMessage> messages) throws AiException {
        ensureSession();
        String prompt = buildPrompt(messages);
        turnCredits.reset();
        turnInputTokens.reset();
        turnOutputTokens.reset();
        turnModel = null;
        long start = System.nanoTime();
        markActivity();
        try {
            CompletableFuture<AssistantMessageEvent> future = session.sendAndWait(
                new MessageOptions().setPrompt(prompt),
                absoluteCeilingMs()
            );
            AssistantMessageEvent reply = awaitWithHeartbeat(future, inactivityTimeoutMs());
            usage.requests++;
            usage.totalTokens = lastContextTokens;
            long elapsedMillis = (System.nanoTime() - start) / 1_000_000L;
            lastTurnStats =
                new TurnStats(
                    elapsedMillis,
                    turnCredits.sum(),
                    turnModel != null ? turnModel : activeModel,
                    turnInputTokens.sum(),
                    turnOutputTokens.sum()
                );
            if (reply == null || reply.getData() == null) {
                return "";
            }
            String content = reply.getData().content();
            return content == null ? "" : content;
        } catch (AiException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiException("Interrupted while waiting for Copilot.", e);
        } catch (Exception e) {
            throw new AiException("Copilot SDK request failed: " + rootMessage(e), e);
        }
    }

    private void markActivity() {
        lastActivityAt = System.currentTimeMillis();
    }

    /**
     * Wait for the turn to finish, keeping it alive while it makes progress.
     * Aborts only on a genuine stall: no tool/token activity for
     * {@code inactivityMs} while no tool call is in flight. A long-but-active
     * turn (many tool calls, a live test run) is bounded only by the absolute
     * ceiling passed to {@code sendAndWait}.
     */
    private AssistantMessageEvent awaitWithHeartbeat(
        CompletableFuture<AssistantMessageEvent> future,
        long inactivityMs
    )
        throws InterruptedException, java.util.concurrent.ExecutionException, AiException {
        while (true) {
            try {
                return future.get(5, TimeUnit.SECONDS);
            } catch (TimeoutException te) {
                long idle = System.currentTimeMillis() - lastActivityAt;
                if (activeTools.get() == 0 && idle >= inactivityMs) {
                    future.cancel(true);
                    throw new AiException(
                        "Copilot turn stalled: no tool or token activity for " +
                        (idle / 1000) +
                        "s (limit " +
                        (inactivityMs / 1000) +
                        "s) with no tool call in flight - the Copilot CLI likely hung, so the " +
                        "turn was aborted. Tune with -Dingenious.copilot.inactivityTimeoutMs " +
                        "and -Dingenious.copilot.maxTurnMs (or the INGENIOUS_COPILOT_* env vars)."
                    );
                }
                // else: a tool is running, or the model is still within the
                // inactivity window - keep waiting; the turn is progressing.
            }
        }
    }

    /** Inactivity limit (ms) before a stalled turn is aborted. Default 5 min. */
    private static long inactivityTimeoutMs() {
        return longConfig(
            "ingenious.copilot.inactivityTimeoutMs",
            "INGENIOUS_COPILOT_INACTIVITY_MS",
            300_000L
        );
    }

    /** Absolute ceiling (ms) for a single turn regardless of activity. Default 30 min. */
    private static long absoluteCeilingMs() {
        return longConfig(
            "ingenious.copilot.maxTurnMs",
            "INGENIOUS_COPILOT_MAX_TURN_MS",
            1_800_000L
        );
    }

    private static long longConfig(String sysProp, String envVar, long def) {
        String v = System.getProperty(sysProp);
        if (v == null || v.isBlank()) {
            v = System.getenv(envVar);
        }
        if (v != null && !v.isBlank()) {
            try {
                long n = Long.parseLong(v.trim());
                if (n > 0) return n;
            } catch (NumberFormatException ignored) {
                // fall through to default
            }
        }
        return def;
    }

    /**
     * Lists models visible to the authenticated Copilot CLI account.
     *
     * <p>Used by `/model list` so the CLI can offer model selection without
     * requiring any OpenAI-compatible HTTP endpoint.
     */
    public static List<String> listAvailableModels() throws AiException {
        CopilotClient c = null;
        try {
            c = new CopilotClient(new CopilotClientOptions().setCwd(runtimeRoot()));
            c.start().get();
            List<ModelInfo> infos = c.listModels().get();
            List<String> ids = new ArrayList<>();
            if (infos != null) {
                for (ModelInfo info : infos) {
                    if (info != null && info.getId() != null && !info.getId().isBlank()) {
                        ids.add(info.getId());
                    }
                }
            }
            Collections.sort(ids);
            return ids;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiException("Interrupted while listing Copilot models.", e);
        } catch (Exception e) {
            throw new AiException("Could not list Copilot models: " + rootMessage(e), e);
        } finally {
            if (c != null) {
                try {
                    c.close();
                } catch (Exception ignored) {
                    // best effort
                }
            }
        }
    }

    /** Lazily starts the CLI client and opens a session wired to the INGenious MCP server. */
    private synchronized void ensureSession() throws AiException {
        if (session != null) {
            return;
        }
        try {
            // Run the Copilot CLI (and the MCP subprocess it spawns) from the
            // INGenious runtime root so relative plugins/ Configuration/ Projects/
            // resolve; the CLI otherwise starts with no cwd.
            client = new CopilotClient(new CopilotClientOptions().setCwd(runtimeRoot()));
            client.start().get();
            SessionConfig config = new SessionConfig()
                .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                .setSystemMessage(
                    new SystemMessageConfig()
                        .setMode(SystemMessageMode.APPEND)
                        .setContent(buildSystemPrompt())
                )
                .setMcpServers(ingeniousMcpServer());
            String sessionModel = selectSessionModel(requestedModel, availableModelIds(client));
            if (sessionModel != null) {
                config.setModel(sessionModel);
                activeModel = sessionModel;
            } else {
                activeModel = "Copilot default";
            }
            String dir = projectDir.get();
            if (dir != null && !dir.isBlank()) {
                config.setWorkingDirectory(dir);
            }
            long logOffset = mcpStderrLog().length();
            session = client.createSession(config).get();
            session.on(
                SessionUsageInfoEvent.class,
                ev -> {
                    markActivity();
                    if (ev.getData() != null && ev.getData().currentTokens() != null) {
                        lastContextTokens = ev.getData().currentTokens().intValue();
                    }
                }
            );
            session.on(
                AssistantUsageEvent.class,
                ev -> {
                    markActivity();
                    AssistantUsageEvent.AssistantUsageEventData d = ev.getData();
                    if (d == null) {
                        return;
                    }
                    if (d.model() != null && !d.model().isBlank()) {
                        turnModel = d.model();
                    }
                    if (d.inputTokens() != null) {
                        turnInputTokens.add(d.inputTokens());
                    }
                    if (d.outputTokens() != null) {
                        turnOutputTokens.add(d.outputTokens());
                    }
                    // Credits = AI Units; the CLI reports them as nano-AIU.
                    if (d.copilotUsage() != null && d.copilotUsage().totalNanoAiu() != null) {
                        turnCredits.add(d.copilotUsage().totalNanoAiu() / 1_000_000_000.0);
                    }
                }
            );
            session.on(
                ToolExecutionStartEvent.class,
                ev -> {
                    activeTools.incrementAndGet();
                    markActivity();
                    ToolExecutionStartEvent.ToolExecutionStartEventData d = ev.getData();
                    if (d == null) {
                        return;
                    }
                    String name = d.mcpToolName() != null ? d.mcpToolName() : d.toolName();
                    if (name == null || name.isBlank()) {
                        name = "tool";
                    }
                    toolNamesById.put(d.toolCallId(), name);
                    CopilotActivity a = activity;
                    if (a != null) {
                        a.onToolStart(d.toolCallId(), name, summarizeArgs(d.arguments()));
                    }
                }
            );
            session.on(
                ToolExecutionCompleteEvent.class,
                ev -> {
                    if (activeTools.get() > 0) {
                        activeTools.decrementAndGet();
                    }
                    markActivity();
                    ToolExecutionCompleteEvent.ToolExecutionCompleteEventData d = ev.getData();
                    if (d == null) {
                        return;
                    }
                    String name = toolNamesById.getOrDefault(d.toolCallId(), "tool");
                    boolean ok = Boolean.TRUE.equals(d.success());
                    String summary = ok ? resultText(d) : errorText(d);
                    CopilotActivity a = activity;
                    if (a != null) {
                        a.onToolComplete(d.toolCallId(), name, ok, summary);
                    }
                }
            );
            awaitIngeniousMcp(logOffset);
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
        } catch (IngeniousMcpUnavailableException e) {
            resetSession();
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            resetSession();
            throw new AiException("Interrupted while starting Copilot.", e);
        } catch (Exception e) {
            resetSession();
            throw new AiException(
                "Could not start the Copilot CLI via the SDK: " +
                rootMessage(e) +
                ". Ensure the GitHub Copilot CLI is installed, on PATH, and authenticated " +
                "(run 'copilot --version' and 'copilot auth login').",
                e
            );
        }
    }

    /** Closes and forgets the client/session so the next request retries from scratch. */
    private void resetSession() {
        shutdown();
        session = null;
        client = null;
    }

    /**
     * Waits until the Copilot CLI reports the {@code ingenious} MCP server as connected.
     * The CLI connects MCP servers asynchronously and silently drops ones that fail, which
     * otherwise surfaces only as "'ingenious' MCP not discoverable" from the model.
     */
    @AllowCopilotExperimental
    private void awaitIngeniousMcp(long logOffset) throws Exception {
        long deadline = System.currentTimeMillis() + mcpStartupTimeoutSec() * 1000L;
        McpServer last = null;
        while (true) {
            SessionMcpListResult servers = session
                .getRpc()
                .mcp.list()
                .get(MCP_LIST_TIMEOUT_SEC, TimeUnit.SECONDS);
            last = findIngeniousServer(servers);
            McpServerStatus status = last != null ? last.status() : null;
            if (status == McpServerStatus.CONNECTED) {
                return;
            }
            boolean pending = status == null || status == McpServerStatus.PENDING;
            if (!pending || System.currentTimeMillis() >= deadline) {
                throw new IngeniousMcpUnavailableException(describeMcpFailure(last, logOffset));
            }
            Thread.sleep(MCP_POLL_INTERVAL_MS);
        }
    }

    @AllowCopilotExperimental
    private static McpServer findIngeniousServer(SessionMcpListResult result) {
        if (result == null || result.servers() == null) {
            return null;
        }
        return result
            .servers()
            .stream()
            .filter(s -> s != null && MCP_SERVER_NAME.equals(s.name()))
            .findFirst()
            .orElse(null);
    }

    @AllowCopilotExperimental
    private String describeMcpFailure(McpServer server, long logOffset) {
        File log = mcpStderrLog();
        String tail = readLogTail(log, logOffset);
        boolean jvmStarted = tail.contains("[ingenious-mcp] starting");
        String status = server == null
            ? "not registered"
            : server.status() == null ? "unknown" : server.status().getValue();
        StringBuilder sb = new StringBuilder();
        sb
            .append("The INGenious MCP server ('")
            .append(MCP_SERVER_NAME)
            .append("') did not start, so INGenious tools are unavailable to Copilot.\n")
            .append("Status reported by the Copilot CLI: ")
            .append(status);
        if (server != null && server.error() != null && !server.error().isBlank()) {
            sb.append("\nCopilot CLI error: ").append(server.error().strip());
        }
        sb.append("\nLikely cause: ");
        if (!jvmStarted) {
            sb
                .append(
                    "the MCP process never reached INGenious startup. Check that Java exists at '"
                )
                .append(javaExecutable())
                .append("', that the install path '")
                .append(runtimeRoot())
                .append(
                    "' contains lib/, and that security software does not block spawning java."
                );
        } else if (
            tail.contains("Exception") || tail.contains("Error") || tail.contains("[mcp] fatal")
        ) {
            sb.append("the MCP server crashed during startup (see log below).");
        } else if (server == null || server.status() == McpServerStatus.PENDING) {
            sb
                .append("the MCP server started but did not finish connecting within ")
                .append(mcpStartupTimeoutSec())
                .append("s. Increase it with -D")
                .append(MCP_TIMEOUT_PROPERTY)
                .append("=<seconds> or env ")
                .append(MCP_TIMEOUT_ENV)
                .append('.');
        } else {
            sb.append("the MCP server started but the Copilot CLI rejected the connection.");
        }
        sb.append("\nMCP startup log: ").append(log.getAbsolutePath());
        if (!tail.isBlank()) {
            sb.append("\n--- log tail ---\n").append(tail.strip());
        }
        return sb.toString();
    }

    private static String readLogTail(File log, long offset) {
        if (!log.isFile()) {
            return "";
        }
        try (RandomAccessFile raf = new RandomAccessFile(log, "r")) {
            long start = offset > raf.length() ? 0 : offset;
            int len = (int) Math.min(raf.length() - start, MCP_LOG_TAIL_BYTES);
            raf.seek(raf.length() - len);
            byte[] buf = new byte[len];
            raf.readFully(buf);
            String text = new String(buf, StandardCharsets.UTF_8);
            String[] lines = text.split("\\R");
            int from = Math.max(0, lines.length - MCP_LOG_TAIL_LINES);
            return String.join("\n", Arrays.asList(lines).subList(from, lines.length));
        } catch (IOException e) {
            return "";
        }
    }

    private static File mcpStderrLog() {
        return new File(runtimeRoot(), MCP_STDERR_LOG);
    }

    private static long mcpStartupTimeoutSec() {
        String raw = System.getProperty(MCP_TIMEOUT_PROPERTY, System.getenv(MCP_TIMEOUT_ENV));
        if (raw != null) {
            try {
                long v = Long.parseLong(raw.trim());
                if (v > 0) {
                    return v;
                }
            } catch (NumberFormatException ignored) {
                // fall through to default
            }
        }
        return DEFAULT_MCP_STARTUP_TIMEOUT_SEC;
    }

    private static String javaExecutable() {
        String exe = File.separatorChar == '\\' ? "java.exe" : "java";
        return System.getProperty("java.home") + File.separator + "bin" + File.separator + exe;
    }

    private static List<String> availableModelIds(CopilotClient client) throws Exception {
        List<ModelInfo> infos = client.listModels().get();
        List<String> ids = new ArrayList<>();
        if (infos != null) {
            for (ModelInfo info : infos) {
                if (info != null && info.getId() != null && !info.getId().isBlank()) {
                    ids.add(info.getId());
                }
            }
        }
        return ids;
    }

    static String selectSessionModel(String requested, List<String> available) {
        if (requested == null || requested.isBlank()) {
            return null;
        }
        if (available == null || available.isEmpty()) {
            return requested;
        }
        return available.contains(requested) ? requested : null;
    }

    /** Launches this same JVM's engine as an INGenious stdio MCP server for the CLI. */
    private Map<String, McpServerConfig> ingeniousMcpServer()
        throws IngeniousMcpUnavailableException {
        String javaBin = javaExecutable();
        String root = runtimeRoot();
        List<String> args = new ArrayList<>();
        // Force the engine's relative plugins/ Configuration/ Projects/ lookups to
        // resolve against the runtime root even if the CLI spawns us elsewhere.
        args.add("-Duser.dir=" + root);
        // Pass the classpath via a java @argfile rather than -cp: an expanded -cp with
        // ~200 absolute jar paths exceeds the Windows command-line limit on deep install
        // paths, and a shell would glob-expand a bare lib/* wildcard into many arguments.
        args.add("@" + writeClasspathArgFile().getAbsolutePath());
        args.add("com.ing.engine.core.Control");
        args.add("server");
        args.add("mcp");
        String dir = projectDir.get();
        if (dir != null && !dir.isBlank()) {
            args.add("-p");
            args.add(new File(dir).getAbsolutePath());
        }
        Map<String, String> env = new HashMap<>(System.getenv());
        // Capture the child's startup stderr so MCP launch failures are diagnosable.
        env.put("INGENIOUS_MCP_STDERR", mcpStderrLog().getAbsolutePath());
        McpStdioServerConfig ingenious = new McpStdioServerConfig()
            .setCommand(javaBin)
            .setArgs(args)
            // The Copilot CLI skips any MCP server that doesn't allowlist tools;
            // "*" exposes all INGenious tools to the model.
            .setTools(List.of("*"))
            .setEnv(env)
            // The INGenious engine resolves Configuration/, plugins/, and Projects/
            // relative to its working directory, so pin it to the runtime root the
            // AI CLI was launched from — NOT the Copilot session working directory.
            .setWorkingDirectory(root);
        Map<String, McpServerConfig> servers = new HashMap<>();
        servers.put("ingenious", ingenious);
        return servers;
    }

    /** INGenious runtime root (the launch directory holding lib/, Configuration/, Projects/). */
    private static String runtimeRoot() {
        return System.getProperty("user.dir");
    }

    /**
     * Writes the {@code -cp} option for the MCP child JVM to a java launcher argfile
     * ({@code java @file}) in the runtime root, falling back to the temp directory when
     * the runtime root is not writable.
     */
    private static File writeClasspathArgFile() throws IngeniousMcpUnavailableException {
        String content = classpathArgFileContent(absoluteClasspath());
        File target = new File(runtimeRoot(), MCP_ARGFILE);
        try {
            Files.writeString(target.toPath(), content, StandardCharsets.UTF_8);
            return target;
        } catch (IOException primary) {
            try {
                Path temp = Files.createTempFile("ingenious-mcp-", ".args");
                Files.writeString(temp, content, StandardCharsets.UTF_8);
                temp.toFile().deleteOnExit();
                return temp.toFile();
            } catch (IOException fallback) {
                throw new IngeniousMcpUnavailableException(
                    "INGenious MCP not started: could not write the MCP launcher argfile " +
                    target.getAbsolutePath() +
                    " (" +
                    primary.getMessage() +
                    ") or a temp file (" +
                    fallback.getMessage() +
                    "). Check write permissions on the INGenious folder.",
                    fallback
                );
            }
        }
    }

    /**
     * Renders {@code -cp <classpath>} in java argfile syntax: the value is quoted so
     * paths with spaces stay one argument, and backslashes/quotes are escaped because
     * the launcher treats {@code \} as an escape character inside quotes.
     */
    static String classpathArgFileContent(String classpath) {
        String escaped = classpath.replace("\\", "\\\\").replace("\"", "\\\"");
        return "-cp\n\"" + escaped + "\"\n";
    }

    /**
     * Makes every {@code java.class.path} entry absolute while keeping {@code dir/*}
     * wildcards intact (the java launcher expands them in {@code -cp}), so the child
     * JVM finds the same jars regardless of the directory it is spawned in.
     */
    static String absoluteClasspath() {
        String raw = System.getProperty("java.class.path", "");
        List<String> entries = new ArrayList<>();
        for (String entry : raw.split(File.pathSeparator)) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            if (entry.endsWith("*")) {
                String dir = entry.substring(0, entry.length() - 1);
                File abs = new File(dir.isEmpty() ? "." : dir).getAbsoluteFile();
                entries.add(abs.getPath() + File.separator + "*");
            } else {
                entries.add(new File(entry).getAbsolutePath());
            }
        }
        return String.join(File.pathSeparator, entries);
    }

    /**
     * The Copilot CLI keeps its own conversation state across turns, so only the
     * latest user message is sent. INGenious authoring conventions (the system
     * message) are prepended once, on the first turn.
     */
    private String buildPrompt(List<ChatMessage> messages) {
        String user = "";
        for (ChatMessage m : messages) {
            if ("user".equals(m.role())) {
                user = m.content();
            }
        }
        firstTurn = false;
        return user;
    }

    private String buildSystemPrompt() {
        return (
            "You are the INGenious test-automation assistant running inside the INGenious AI CLI. " +
            "An MCP server named 'ingenious' is connected and exposes tools (their names contain " +
            "'ingenious') for listing, searching, creating, editing, validating, and RUNNING " +
            "INGenious test artifacts (scenarios, test cases, test sets, object repository, data). " +
            "Always use these MCP tools to do the work inside the active INGenious project. " +
            "Never generate standalone code (JavaScript/Jest, pytest, etc.) or look for package.json / " +
            "pom.xml; INGenious test cases are steps created via the tools. To run a test, call the " +
            "ingenious run tool — do NOT claim you lack a runner. Only claim a tool is unavailable if " +
            "an actual tool call fails, and then report the exact tool error."
        );
    }

    private void shutdown() {
        try {
            if (session != null) {
                session.close();
            }
        } catch (Exception ignored) {
            // best effort
        }
        try {
            if (client != null) {
                client.close();
            }
        } catch (Exception ignored) {
            // best effort
        }
    }

    private static String oneLine(String s, int max) {
        if (s == null) {
            return "";
        }
        String one = s.replace('\n', ' ').replace('\r', ' ').trim();
        return one.length() <= max ? one : one.substring(0, Math.max(0, max - 1)) + "\u2026";
    }

    private static String summarizeArgs(Object args) {
        if (args == null) {
            return "";
        }
        return oneLine(String.valueOf(args), 80);
    }

    // Kept large (not a display width) — the REPL's final tool report parses this
    // as JSON to render a proper breakdown, so the full payload must survive here;
    // only the live "Thinking…" preview line truncates it for display.
    private static final int RESULT_CAP = 20_000;

    private static String resultText(ToolExecutionCompleteEvent.ToolExecutionCompleteEventData d) {
        if (d.result() != null) {
            String content = d.result().content();
            if (content != null && !content.isBlank()) {
                return oneLine(content, RESULT_CAP);
            }
        }
        return "ok";
    }

    private static String errorText(ToolExecutionCompleteEvent.ToolExecutionCompleteEventData d) {
        if (d.error() != null && d.error().message() != null) {
            return oneLine(d.error().message(), RESULT_CAP);
        }
        return "failed";
    }

    private static String rootMessage(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
        }
        String msg = cur.getMessage();
        return msg != null ? msg : cur.getClass().getSimpleName();
    }
}

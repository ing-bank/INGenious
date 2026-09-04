package com.ing.engine.aicli.ai;

import com.github.copilot.CopilotClient;
import com.github.copilot.CopilotSession;
import com.github.copilot.SystemMessageMode;
import com.github.copilot.generated.AssistantMessageEvent;
import com.github.copilot.generated.AssistantUsageEvent;
import com.github.copilot.generated.SessionUsageInfoEvent;
import com.github.copilot.generated.ToolExecutionCompleteEvent;
import com.github.copilot.generated.ToolExecutionStartEvent;
import com.github.copilot.rpc.CopilotClientOptions;
import com.github.copilot.rpc.McpServerConfig;
import com.github.copilot.rpc.McpStdioServerConfig;
import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.ModelInfo;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.SessionConfig;
import com.github.copilot.rpc.SystemMessageConfig;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * GitHub Copilot provider that drives the official Copilot CLI through the
 * {@code copilot-sdk-java} SDK, instead of talking to any HTTP endpoint or the
 * VS Code bridge.
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
    private final String model;
    private final Supplier<String> projectDir;
    private final Usage usage = new Usage();

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

    public CopilotSdkProvider(String model, Supplier<String> projectDir) {
        this.model = model == null || model.isBlank() ? "claude-sonnet-4.5" : model;
        this.projectDir = projectDir != null ? projectDir : () -> null;
    }

    @Override
    public String id() {
        return "copilot-sdk";
    }

    @Override
    public String model() {
        return model;
    }

    @Override
    public String describe() {
        return "GitHub Copilot SDK (drives the Copilot CLI, model " + model + ")";
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
        try {
            AssistantMessageEvent reply = session
                .sendAndWait(new MessageOptions().setPrompt(prompt), 300_000L)
                .get();
            usage.requests++;
            usage.totalTokens = lastContextTokens;
            long elapsedMillis = (System.nanoTime() - start) / 1_000_000L;
            lastTurnStats =
                new TurnStats(
                    elapsedMillis,
                    turnCredits.sum(),
                    turnModel != null ? turnModel : model,
                    turnInputTokens.sum(),
                    turnOutputTokens.sum()
                );
            if (reply == null || reply.getData() == null) {
                return "";
            }
            String content = reply.getData().content();
            return content == null ? "" : content;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiException("Interrupted while waiting for Copilot.", e);
        } catch (Exception e) {
            throw new AiException("Copilot SDK request failed: " + rootMessage(e), e);
        }
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
                .setModel(model)
                .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                .setSystemMessage(
                    new SystemMessageConfig()
                        .setMode(SystemMessageMode.APPEND)
                        .setContent(buildSystemPrompt())
                )
                .setMcpServers(ingeniousMcpServer());
            String dir = projectDir.get();
            if (dir != null && !dir.isBlank()) {
                config.setWorkingDirectory(dir);
            }
            session = client.createSession(config).get();
            session.on(
                SessionUsageInfoEvent.class,
                ev -> {
                    if (ev.getData() != null && ev.getData().currentTokens() != null) {
                        lastContextTokens = ev.getData().currentTokens().intValue();
                    }
                }
            );
            session.on(
                AssistantUsageEvent.class,
                ev -> {
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
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiException("Interrupted while starting Copilot.", e);
        } catch (Exception e) {
            shutdown();
            throw new AiException(
                "Could not start the Copilot CLI via the SDK: " +
                rootMessage(e) +
                ". Ensure the GitHub Copilot CLI is installed, on PATH, and authenticated " +
                "(run 'copilot --version' and 'copilot auth login').",
                e
            );
        }
    }

    /** Launches this same JVM's engine as an INGenious stdio MCP server for the CLI. */
    private Map<String, McpServerConfig> ingeniousMcpServer() {
        String javaBin =
            System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        String root = runtimeRoot();
        List<String> args = new ArrayList<>();
        // Force the engine's relative plugins/ Configuration/ Projects/ lookups to
        // resolve against the runtime root even if the CLI spawns us elsewhere.
        args.add("-Duser.dir=" + root);
        args.add("-cp");
        args.add(expandedClasspath());
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
        env.put("INGENIOUS_MCP_STDERR", root + File.separator + ".ingenious-mcp-stderr.log");
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
     * Resolves {@code java.class.path} to an explicit, absolute classpath with every
     * jar listed individually. The launcher uses relative wildcard entries
     * ({@code lib/*:lib/clib/*}); the Copilot CLI spawns the MCP command through a
     * shell, which would glob-expand a wildcard into multiple arguments and break
     * {@code -cp}, so wildcards are expanded here into concrete jar paths.
     */
    private static String expandedClasspath() {
        String raw = System.getProperty("java.class.path", "");
        String[] entries = raw.split(File.pathSeparator);
        List<String> jars = new ArrayList<>();
        for (String entry : entries) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            if (entry.endsWith("*")) {
                String dirPath = entry.substring(0, entry.length() - 1);
                File dir = new File(dirPath).getAbsoluteFile();
                File[] found = dir.listFiles(
                    (d, name) -> {
                        String lower = name.toLowerCase();
                        return lower.endsWith(".jar");
                    }
                );
                if (found != null) {
                    for (File jar : found) {
                        jars.add(jar.getAbsolutePath());
                    }
                }
            } else {
                jars.add(new File(entry).getAbsolutePath());
            }
        }
        if (jars.isEmpty()) {
            return new File(raw).getAbsolutePath();
        }
        return String.join(File.pathSeparator, jars);
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

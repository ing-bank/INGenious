package com.ing.engine.aicli.repl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ing.engine.aicli.agent.AgentLoop;
import com.ing.engine.aicli.ai.AgentMessage;
import com.ing.engine.aicli.ai.AiProvider;
import com.ing.engine.aicli.ai.ChatMessage;
import com.ing.engine.aicli.ai.Planner;
import com.ing.engine.aicli.ai.ProviderConfig;
import com.ing.engine.aicli.ai.TokenStore;
import com.ing.engine.aicli.conversation.ConversationManager;
import com.ing.engine.aicli.conversation.SessionContext;
import com.ing.engine.aicli.execution.ExecutionEngine;
import com.ing.engine.aicli.execution.ExecutionListener;
import com.ing.engine.aicli.execution.ExecutionResult;
import com.ing.engine.aicli.execution.FileChange;
import com.ing.engine.aicli.execution.UndoJournal;
import com.ing.engine.aicli.planning.Plan;
import com.ing.engine.aicli.planning.Plan.PlanStep;
import com.ing.engine.aicli.planning.WorkflowCatalog;
import com.ing.engine.aicli.tools.Tool;
import com.ing.engine.aicli.tools.ToolException;
import com.ing.engine.aicli.tools.ToolRegistry;
import com.ing.engine.aicli.ui.AiBanner;
import com.ing.engine.aicli.ui.MarkdownRenderer;
import com.ing.engine.aicli.ui.Panels;
import com.ing.engine.aicli.ui.ResultRenderer;
import com.ing.engine.aicli.ui.Spinner;
import com.ing.engine.aicli.ui.Theme;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * The interactive AI CLI: a conversational REPL over the {@link ToolRegistry}.
 *
 * <p>Input routing: slash commands → {@link SlashCommands}; recognized intents
 * → deterministic {@link WorkflowCatalog} plans (zero LLM calls); everything
 * else → the AI {@link Planner}. All plans flow through the same
 * {@link ExecutionEngine} with approvals, streaming progress, and undo.
 */
public final class Repl {
    private static final Pattern EXECUTE_PENDING = Pattern.compile(
        "(?i)^(y|yes|proceed|ok|(execute|run)( the)? plan)$"
    );

    final ObjectMapper mapper = new ObjectMapper();
    final Path cwd;
    final Theme theme;
    final Panels panels;
    final MarkdownRenderer markdown;
    final ToolRegistry registry;
    final ResultRenderer results;
    final SessionContext session;
    final ConversationManager convo;
    final ProviderConfig aiConfig;
    final TokenStore tokens;
    final ExecutionEngine engine;
    final Planner planner;
    final AgentLoop agent;

    private final boolean noBanner;
    private AiProvider provider;
    private UndoJournal journal;
    private String journalRoot;
    LineReader reader;
    private Terminal terminal;

    public Repl(String projectOverride, boolean noBanner) {
        // Datalib logs INFO chatter through JUL; keep the conversation clean.
        java
            .util.logging.Logger.getLogger("com.ing.datalib")
            .setLevel(java.util.logging.Level.WARNING);
        this.cwd = Path.of(System.getProperty("user.dir"));
        this.theme = Theme.auto();
        this.panels = new Panels(theme);
        this.markdown = new MarkdownRenderer(theme);
        this.registry = ToolRegistry.create();
        this.results = new ResultRenderer(theme);
        this.session = SessionContext.load(cwd);
        this.convo = new ConversationManager(cwd);
        this.aiConfig = ProviderConfig.load();
        this.tokens = new TokenStore();
        this.engine = new ExecutionEngine(registry);
        this.planner = new Planner(registry);
        this.agent = new AgentLoop(registry, mapper);
        this.noBanner = noBanner;
        if (projectOverride != null && !projectOverride.isBlank()) {
            session.setProject(projectOverride, cwd);
        }
        session.autoDetectProject(cwd);
    }

    public int run() {
        try {
            terminal = TerminalBuilder.builder().system(true).dumb(true).build();
            List<String> completions = new ArrayList<>(SlashCommands.COMMANDS);
            registry.all().forEach(t -> completions.add(t.id()));
            reader =
                LineReaderBuilder
                    .builder()
                    .terminal(terminal)
                    .completer(new StringsCompleter(completions))
                    .variable(
                        LineReader.HISTORY_FILE,
                        Path.of(System.getProperty("user.home"), ".ingenious", "repl_history")
                    )
                    .build();
        } catch (Exception e) {
            System.err.println("Could not initialize the terminal: " + e.getMessage());
            return 1;
        }

        if (!noBanner) banner();
        SlashCommands slash = new SlashCommands(this);

        while (true) {
            String line;
            try {
                line = reader.readLine(prompt());
            } catch (UserInterruptException e) {
                System.out.println(theme.dim("(Ctrl-D or /exit to quit)"));
                continue;
            } catch (EndOfFileException e) {
                break;
            }
            if (line == null) break;
            line = line.trim();
            if (line.isEmpty()) continue;
            try {
                if (line.startsWith("/")) {
                    if (!slash.handle(line)) break;
                } else {
                    handleNatural(line);
                }
                // 2 blank lines between any response and the next prompt
                System.out.println();
                System.out.println();
            } catch (Exception e) {
                System.out.println(theme.fail("Error: " + e.getMessage()));
            }
        }
        session.save();
        System.out.println(theme.dim("Goodbye."));
        return 0;
    }

    // ------------------------------------------------------------------
    // natural language
    // ------------------------------------------------------------------

    void handleNatural(String input) {
        if (session.pendingPlan() != null && EXECUTE_PENDING.matcher(input).matches()) {
            runPlan(session.pendingPlan());
            return;
        }
        // Bare "run" / "execute" launches the interactive run builder.
        if (input.matches("(?i)run|execute|run test|run a test")) {
            interactiveRun();
            return;
        }
        java.util.Optional<WorkflowCatalog.Match> match = WorkflowCatalog.match(input);
        if (match.isPresent()) {
            WorkflowCatalog.Workflow wf = match.get().workflow();
            System.out.println(theme.dim("Workflow: " + wf.id() + " — " + wf.description()));
            Map<String, String> values = collectParams(wf, match.get().extracted());
            if (values == null) return; // aborted
            Plan plan = wf.build(values);
            session.setPendingPlan(plan);
            showPlan(plan);
            confirmAndRun(plan);
            return;
        }
        agentRequest(input);
    }

    /** Prompt interactively for missing required workflow parameters. */
    private Map<String, String> collectParams(
        WorkflowCatalog.Workflow wf,
        Map<String, String> extracted
    ) {
        Map<String, String> values = new LinkedHashMap<>(extracted);
        for (WorkflowCatalog.Param p : wf.params()) {
            if (values.containsKey(p.name())) continue;
            if (p.defaultValue() != null) {
                values.put(p.name(), p.defaultValue());
                continue;
            }
            if (!p.required()) continue;
            try {
                String v = reader.readLine("  " + p.prompt() + ": ");
                if (v == null || v.isBlank()) {
                    System.out.println(theme.yellow("Cancelled (missing " + p.name() + ")."));
                    return null;
                }
                values.put(p.name(), v.trim());
            } catch (UserInterruptException | EndOfFileException e) {
                System.out.println(theme.yellow("Cancelled."));
                return null;
            }
        }
        return values;
    }

    private void aiRequest(String input) {
        AiProvider p = ensureProvider();
        if (p == null) return;
        com.ing.engine.aicli.ai.Usage before = p.usage();
        List<ChatMessage> tail = convo.tail();
        convo.addUser(input);
        Spinner spinner = new Spinner(theme);
        spinner.start("Thinking…");
        Planner.Outcome outcome;
        try {
            outcome = planner.plan(input, session.summary(), tail, p);
        } catch (AiProvider.AiException e) {
            spinner.fail(e.getMessage());
            return;
        }
        spinner.stop();
        printCredits(p, before);
        if (outcome.answer() != null) {
            markdown.print(outcome.answer());
            convo.addAssistant(outcome.answer());
            return;
        }
        Plan plan = outcome.plan();
        convo.addAssistant("Proposed plan: " + plan.toJson(mapper).toString());
        session.setPendingPlan(plan);
        showPlan(plan);
        confirmAndRun(plan);
    }

    /**
     * Turn for a self-agentic provider: the provider (Copilot CLI) plans and
     * calls INGenious MCP tools itself, so we only relay the prompt and print
     * the final answer.
     */
    private void selfAgenticRequest(AiProvider p, String input) {
        com.ing.engine.aicli.ai.Usage before = p.usage();
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.system(agentSystemPrompt()));
        messages.add(ChatMessage.user(input));
        convo.addUser(input);

        final Spinner spinner = new Spinner(theme);
        final List<ToolActivity> activities = new ArrayList<>();
        final boolean[] spinning = { false };
        final Object lock = new Object();

        spinner.start("Thinking…");
        spinning[0] = true;

        // Live tool feedback during the "Thinking" phase; each call renders as it happens.
        p.setActivity(
            new com.ing.engine.aicli.ai.CopilotActivity() {

                @Override
                public void onToolStart(String toolCallId, String toolName, String argsSummary) {
                    synchronized (lock) {
                        if (spinning[0]) {
                            spinner.stop();
                            spinning[0] = false;
                        }
                        System.out.println(
                            "  " +
                            theme.brightPurple(Theme.DOT) +
                            " " +
                            theme.bold(toolName) +
                            (
                                argsSummary == null || argsSummary.isEmpty()
                                    ? ""
                                    : theme.dim("  " + argsSummary)
                            )
                        );
                    }
                }

                @Override
                public void onToolComplete(
                    String toolCallId,
                    String toolName,
                    boolean success,
                    String resultSummary
                ) {
                    synchronized (lock) {
                        activities.add(new ToolActivity(toolName, success, resultSummary));
                        String mark = success ? theme.green(Theme.CHECK) : theme.red(Theme.CROSS);
                        System.out.println(
                            "    " +
                            mark +
                            " " +
                            theme.dim(
                                com.ing.engine.aicli.ai.ToolReportUtil.truncate(resultSummary, 68)
                            )
                        );
                    }
                }
            }
        );

        String answer;
        try {
            answer = p.chat(messages);
        } catch (AiProvider.AiException e) {
            synchronized (lock) {
                if (spinning[0]) {
                    spinner.stop();
                }
            }
            p.setActivity(null);
            System.out.println(theme.fail(e.getMessage()));
            return;
        }
        synchronized (lock) {
            if (spinning[0]) {
                spinner.stop();
                spinning[0] = false;
            }
        }
        p.setActivity(null);

        if (answer != null && !answer.isBlank()) {
            markdown.print(answer);
            convo.addAssistant(answer);
        }
        printToolReport(activities);
        printTurnStatus(p, before);
        session.save();
    }

    /** GitHub-Copilot-style turn footer: {@code 8:47 PM  8m 46s • Claude Opus 4.8 • 445.0 credits}. */
    private void printTurnStatus(AiProvider p, com.ing.engine.aicli.ai.Usage before) {
        com.ing.engine.aicli.ai.TurnStats st = p.lastTurnStats();
        if (st == null) {
            printCredits(p, before);
            return;
        }
        String time = new java.text.SimpleDateFormat("h:mm a", java.util.Locale.US)
        .format(new java.util.Date());
        String elapsed = com.ing.engine.aicli.ai.TurnStatusFormatter.formatDuration(
            st.elapsedMillis
        );
        String modelName = com.ing.engine.aicli.ai.TurnStatusFormatter.displayModel(
            st.model != null ? st.model : p.model()
        );
        String credits = String.format(java.util.Locale.US, "%.1f", st.credits);
        String sep = theme.dim(" • ");
        System.out.println(
            theme.dim(time + "  " + elapsed) +
            sep +
            theme.brightPurple(modelName) +
            sep +
            theme.bold(credits + " credits")
        );
    }

    /** One recorded tool invocation for the final self-agentic report. */
    private static final class ToolActivity {
        final String name;
        final boolean success;
        final String summary;

        ToolActivity(String name, boolean success, String summary) {
            this.name = name;
            this.success = success;
            this.summary = summary;
        }
    }

    /**
     * Renders the final tool report: a compact badge-tagged row per tool plus a
     * pill summary, followed by a proper detail box (table/info box, via the
     * same {@link ResultRenderer} used for {@code /tools run}) for every tool
     * whose result parses as structured JSON — not just a truncated JSON blob.
     */
    private void printToolReport(List<ToolActivity> acts) {
        if (acts == null || acts.isEmpty()) {
            return;
        }
        int nameW = 0;
        for (ToolActivity a : acts) {
            nameW = Math.max(nameW, a.name.length());
        }
        int ok = 0;
        int info = 0;
        int warn = 0;
        int fail = 0;
        List<String> rows = new ArrayList<>();
        List<ToolActivity> detailed = new ArrayList<>();
        List<JsonNode> detailedJson = new ArrayList<>();
        for (ToolActivity a : acts) {
            JsonNode parsed = a.success
                ? com.ing.engine.aicli.ai.ToolReportUtil.parseJsonQuiet(a.summary)
                : null;
            String kind = com.ing.engine.aicli.ai.ToolReportUtil.classify(
                a.name,
                a.success,
                a.summary,
                parsed
            );
            String badge;
            if ("FAIL".equals(kind)) {
                badge = theme.badgeFail("FAIL");
                fail++;
            } else if ("WARN".equals(kind)) {
                badge = theme.badgeWarn("WARN");
                warn++;
            } else if ("INFO".equals(kind)) {
                badge = theme.badgeInfo("INFO");
                info++;
            } else {
                badge = theme.badgeOk(" OK ");
                ok++;
            }
            String pad = " ".repeat(Math.max(0, nameW - a.name.length()));
            rows.add(
                badge +
                "  " +
                theme.bold(a.name) +
                pad +
                "  " +
                theme.dim(
                    com.ing.engine.aicli.ai.ToolReportUtil.shortSummary(
                        a.summary,
                        a.success,
                        parsed
                    )
                )
            );
            if (parsed != null && ((parsed.isObject() && parsed.size() > 0) || parsed.isArray())) {
                detailed.add(a);
                detailedJson.add(parsed);
            }
        }
        List<String> pills = new ArrayList<>();
        pills.add(theme.badgeOk(ok + " OK"));
        if (info > 0) {
            pills.add(theme.badgeInfo(info + " INFO"));
        }
        if (warn > 0) {
            pills.add(theme.badgeWarn(warn + " WARN"));
        }
        if (fail > 0) {
            pills.add(theme.badgeFail(fail + " FAIL"));
        }
        rows.add("");
        rows.add(String.join("  ", pills));
        panels.print("Tools used (" + acts.size() + ")", rows);
        for (int i = 0; i < detailed.size(); i++) {
            System.out.println();
            results.print(detailed.get(i).name, detailedJson.get(i));
        }
    }

    /** Best-effort JSON parse; returns null for plain text (e.g. "ok", error strings). */
    private JsonNode parseJsonQuiet(String s) {
        return com.ing.engine.aicli.ai.ToolReportUtil.parseJsonQuiet(s);
    }

    /** Short one-line preview for the compact report row (not the detail box). */
    private String shortSummary(ToolActivity a, JsonNode parsed) {
        return com.ing.engine.aicli.ai.ToolReportUtil.shortSummary(a.summary, a.success, parsed);
    }

    /** Classify a tool row into a badge kind: OK, INFO (read-only), WARN, or FAIL. */
    private String classify(ToolActivity a, JsonNode parsed) {
        return com.ing.engine.aicli.ai.ToolReportUtil.classify(
            a.name,
            a.success,
            a.summary,
            parsed
        );
    }

    /** Collapse to a single line and cap at {@code max} visible chars. */
    private static String truncate(String s, int max) {
        return com.ing.engine.aicli.ai.ToolReportUtil.truncate(s, max);
    }

    // ------------------------------------------------------------------
    // interactive agent (ReAct loop over the same tools as the IDE)
    // ------------------------------------------------------------------

    private void agentRequest(String input) {
        AiProvider p = ensureProvider();
        if (p == null) return;
        // Self-agentic providers (e.g. the Copilot SDK/CLI) run their own tool
        // loop against the INGenious MCP server, so just hand them the prompt.
        if (p.isSelfAgentic()) {
            selfAgenticRequest(p, input);
            return;
        }
        // Providers without tool-calling (e.g. plain OpenAI text models) fall
        // back to the one-shot planner path.
        if (!p.supportsTools()) {
            aiRequest(input);
            return;
        }
        com.ing.engine.aicli.ai.Usage before = p.usage();

        List<AgentMessage> messages = new ArrayList<>();
        messages.add(AgentMessage.system(agentSystemPrompt()));
        for (ChatMessage m : convo.tail()) {
            messages.add(toAgentMessage(m));
        }
        messages.add(AgentMessage.user(input));
        convo.addUser(input);

        Spinner spinner = new Spinner(theme);
        boolean[] spinning = { false };

        AgentLoop.Ui ui = new AgentLoop.Ui() {

            @Override
            public AgentLoop.Approval approve(String tool, String prettyArgs) {
                if (spinning[0]) {
                    spinner.stop();
                    spinning[0] = false;
                }
                System.out.println(
                    theme.yellow(Theme.WARN) + " " + theme.bold(tool) + theme.dim(" " + prettyArgs)
                );
                try {
                    String ans = reader.readLine("Proceed? [Y/n/a(ll)] ");
                    if (ans == null) return AgentLoop.Approval.NO;
                    ans = ans.trim().toLowerCase(java.util.Locale.ROOT);
                    if (ans.equals("a") || ans.equals("all")) return AgentLoop.Approval.ALL;
                    if (ans.isEmpty() || ans.equals("y") || ans.equals("yes")) {
                        return AgentLoop.Approval.YES;
                    }
                    return AgentLoop.Approval.NO;
                } catch (UserInterruptException | EndOfFileException e) {
                    return AgentLoop.Approval.NO;
                }
            }

            @Override
            public void onAssistantText(String text) {
                if (spinning[0]) {
                    spinner.stop();
                    spinning[0] = false;
                }
                markdown.print(text);
            }

            @Override
            public void onToolResult(String tool, boolean error, String summary) {
                if (spinning[0]) {
                    spinner.stop();
                    spinning[0] = false;
                }
                if (error) {
                    System.out.println(theme.fail(Theme.CROSS + " " + tool + " — " + summary));
                } else {
                    System.out.println(
                        theme.green(Theme.CHECK) + " " + tool + theme.dim(" — " + summary)
                    );
                }
            }

            @Override
            public void thinking(boolean on) {
                if (on) {
                    spinner.start("Thinking…");
                    spinning[0] = true;
                } else if (spinning[0]) {
                    spinner.stop();
                    spinning[0] = false;
                }
            }
        };

        Path root = session.projectPath() != null ? Path.of(session.projectPath()) : null;
        AgentLoop.Outcome outcome;
        try {
            outcome = agent.run(p, messages, projectArg(), root, journal(), input, ui);
        } catch (AiProvider.AiException e) {
            if (spinning[0]) spinner.stop();
            System.out.println(theme.fail("AI error: " + e.getMessage()));
            return;
        }

        if (outcome.text != null && !outcome.text.isBlank()) {
            convo.addAssistant(outcome.text);
        }
        if (!outcome.changes.isEmpty()) {
            System.out.println(theme.bold("Files:"));
            List<String> paths = new ArrayList<>();
            for (FileChange c : outcome.changes) {
                String label;
                if ("created".equals(c.kind())) {
                    label = theme.green("created ");
                } else if ("deleted".equals(c.kind())) {
                    label = theme.red("deleted ");
                } else {
                    label = theme.yellow("modified");
                }
                System.out.println("  " + label + " " + c.relPath);
                paths.add(c.relPath);
            }
            session.rememberFiles(paths);
        }
        if (outcome.hitLimit) {
            System.out.println(
                theme.yellow("Agent stopped at the step limit; ask it to continue if needed.")
            );
        }
        printCredits(p, before);
        session.save();
    }

    /** Print the AI credits (model requests + tokens) consumed since {@code before}. */
    private void printCredits(AiProvider p, com.ing.engine.aicli.ai.Usage before) {
        if (p == null) return;
        com.ing.engine.aicli.ai.Usage total = p.usage();
        if (total == null) return;
        com.ing.engine.aicli.ai.Usage delta = total.since(before);
        if (delta.isEmpty()) return;
        System.out.println(theme.dim("AI credits: " + delta.summary()));
    }

    private AgentMessage toAgentMessage(ChatMessage m) {
        String role = m.role();
        if ("assistant".equals(role)) return AgentMessage.assistant(m.content());
        if ("system".equals(role)) return AgentMessage.system(m.content());
        return AgentMessage.user(m.content());
    }

    private String agentSystemPrompt() {
        return (
            "You are the INGenious test-automation assistant. Accomplish the user's request by " +
            "CALLING TOOLS, working iteratively: call a tool, read its result, then decide the next " +
            "tool. DISCOVER before you mutate — use action_search/action_info to find the exact " +
            "action names and their input requirements, and scenario_list/testcase_list/object_list " +
            "to see what already exists. Never guess action names, fields, or object types; look them " +
            "up. Do not fabricate results.\n\n" +
            com.ing.engine.mcp.ConventionCatalog.condensedInstructions() +
            "\n\n" +
            "Session context:\n" +
            session.summary() +
            "\n\n" +
            "Guidance:\n" +
            "- Only attach an 'input' to a step when the action's metadata allows it. Actions with " +
            "input=NO (e.g. getRestRequest) must have NO input.\n" +
            "- For a REST API test use THREE steps: setEndPoint (input = the URL), then getRestRequest " +
            "(NO input), then an assert action (input = the expected value). Search with " +
            "category:\"API\" for Webservice actions.\n" +
            "- The active project is injected automatically; you do not need to pass a 'project' arg.\n" +
            "- After creating or editing a test case, call testcase_validate and fix any errors it " +
            "reports before finishing.\n" +
            "- When the work is complete, reply with a short plain-text summary (no tool call)."
        );
    }

    // ------------------------------------------------------------------
    // plans
    // ------------------------------------------------------------------

    void showPlan(Plan plan) {
        List<String> lines = new ArrayList<>();
        int i = 1;
        for (PlanStep s : plan.steps) {
            Tool t = registry.get(s.tool);
            String marker = t != null && t.mutatesFiles() ? theme.yellow(Theme.WARN) + " " : "  ";
            String args = compactArgs(s.args);
            lines.add(i++ + " " + marker + theme.bold(s.tool) + theme.dim(args));
        }
        panels.print("Plan — " + plan.goal, lines);
    }

    private String compactArgs(ObjectNode args) {
        if (args == null || args.isEmpty()) return "";
        String s = args.toString();
        return "  " + (s.length() > 70 ? s.substring(0, 70) + "…" : s);
    }

    void confirmAndRun(Plan plan) {
        if (plan.hasMutatingSteps(registry)) {
            try {
                String answer = reader.readLine("Proceed? [Y/n] ");
                if (answer != null && !answer.isBlank() && !answer.trim().matches("(?i)y|yes")) {
                    System.out.println(
                        theme.dim("Plan kept pending — run later with /approve or /plan run.")
                    );
                    return;
                }
            } catch (UserInterruptException | EndOfFileException e) {
                System.out.println(theme.dim("Plan kept pending — run later with /approve."));
                return;
            }
        }
        runPlan(plan);
    }

    void runPlan(Plan plan) {
        Spinner spinner = new Spinner(theme);
        ExecutionListener listener = new ExecutionListener() {

            @Override
            public void onStepStart(PlanStep step, int index, int total) {
                spinner.start("[" + index + "/" + total + "] " + step.tool);
            }

            @Override
            public void onStepSuccess(PlanStep step, JsonNode result, String summary) {
                spinner.succeed(summary);
            }

            @Override
            public void onStepFailure(PlanStep step, String error, List<String> suggestions) {
                spinner.fail(step.tool + " — " + error);
                if (!suggestions.isEmpty()) {
                    System.out.println(
                        theme.dim("  Did you mean: " + String.join(", ", suggestions) + "?")
                    );
                }
            }
        };

        Path root = session.projectPath() != null ? Path.of(session.projectPath()) : null;
        ExecutionResult result = engine.execute(plan, projectArg(), root, journal(), listener);
        spinner.stop();

        // Show captured run logs immediately after execution.
        for (JsonNode stepResult : result.stepResults.values()) {
            JsonNode output = stepResult.path("output");
            if (output.isTextual() && !output.asText().isBlank()) {
                System.out.println();
                System.out.println(
                    theme.bold("Run logs") +
                    theme.dim(" (" + stepResult.path("status").asText("") + ")")
                );
                System.out.println(theme.dim("─".repeat(60)));
                System.out.println(output.asText().stripTrailing());
                System.out.println(theme.dim("─".repeat(60)));
            }
        }

        if (!result.changes.isEmpty()) {
            System.out.println(theme.bold("Files:"));
            List<String> paths = new ArrayList<>();
            for (FileChange c : result.changes) {
                String label;
                if ("created".equals(c.kind())) {
                    label = theme.green("created ");
                } else if ("deleted".equals(c.kind())) {
                    label = theme.red("deleted ");
                } else {
                    label = theme.yellow("modified");
                }
                System.out.println("  " + label + " " + c.relPath);
                paths.add(c.relPath);
            }
            session.rememberFiles(paths);
        }

        // Confirm the key entities each step touched (scenario, test case, …).
        printEntityConfirmations(result);

        if (result.success) {
            System.out.println(theme.green(Theme.CHECK) + " " + theme.bold(theme.green("Done.")));
            session.setPendingPlan(null);
        } else {
            System.out.println(
                theme.dim(
                    "Plan stopped at step " + result.failedStepId + ". Fix and retry, or /undo."
                )
            );
            session.setPendingPlan(null);
            offerRepair(plan, result);
        }
        session.save();
    }

    /**
     * Emit "✓ Scenario — X" / "✓ Testcase — Y" style confirmations, drawn from
     * the fields the executed steps returned. Deduplicated and shown in order.
     */
    private void printEntityConfirmations(ExecutionResult result) {
        java.util.LinkedHashMap<String, String> confirmed = new java.util.LinkedHashMap<>();
        // field name in tool result -> human label
        String[][] fields = {
            { "scenario", "Scenario" },
            { "testcase", "Testcase" },
            { "testset", "Testset" },
            { "release", "Release" },
            { "page", "Object page" },
            { "sheet", "Data sheet" }
        };
        for (JsonNode stepResult : result.stepResults.values()) {
            for (String[] f : fields) {
                JsonNode v = stepResult.path(f[0]);
                if (v.isTextual() && !v.asText().isBlank()) {
                    confirmed.put(f[1], v.asText());
                }
            }
        }
        for (Map.Entry<String, String> e : confirmed.entrySet()) {
            System.out.println(
                theme.green(Theme.CHECK) + " " + theme.bold(e.getKey()) + " — " + e.getValue()
            );
        }
    }

    /** Phase-5 repair loop: on step failure, let the AI propose a corrected plan. */
    private void offerRepair(Plan plan, ExecutionResult result) {
        if (!aiAvailable()) return;
        try {
            String answer = reader.readLine("Ask AI to propose a fix? [y/N] ");
            if (answer == null || !answer.trim().matches("(?i)y|yes")) return;
        } catch (UserInterruptException | EndOfFileException e) {
            return;
        }
        String request =
            "The following plan failed.\nPlan: " +
            plan.toJson(mapper).toString() +
            "\nFailed step: " +
            result.failedStepId +
            "\nError: " +
            result.error +
            "\nPropose a corrected plan that achieves the original goal. " +
            "Skip steps that already succeeded unless they must be redone.";
        aiRequest(request);
    }

    /** True when an AI provider is usable right now (no hints printed). */
    private boolean aiAvailable() {
        AiProvider p = currentProviderUnchecked();
        if (p instanceof com.ing.engine.aicli.ai.CopilotProvider) {
            return ((com.ing.engine.aicli.ai.CopilotProvider) p).isLoggedIn();
        }
        return p != null;
    }

    /** Run a single tool directly (used by {@code /tools run}). */
    void runTool(String toolId, ObjectNode args) {
        Tool tool = registry.get(toolId);
        if (tool == null) {
            System.out.println(theme.fail("Unknown tool: " + toolId + " (see /tools)"));
            return;
        }
        // Fill common entity args (scenario/testcase/…) interactively when missing.
        if (!fillEntityArgs(tool, args)) return;
        ExecutionEngine.injectProject(tool, args, projectArg());
        Spinner spinner = new Spinner(theme);
        spinner.start(tool.id());
        try {
            JsonNode result = executeQuietly(tool, args);
            spinner.succeed(tool.id());
            results.print(toolId, result);
        } catch (ToolException e) {
            spinner.fail(tool.id() + " — " + e.getMessage());
            if (!e.suggestions().isEmpty()) {
                System.out.println(
                    theme.dim("  Did you mean: " + String.join(", ", e.suggestions()) + "?")
                );
            }
        }
    }

    /**
     * Execute a tool while suppressing any stray {@code System.out} chatter the
     * underlying Datalib code emits (e.g. "properties file already exists"), so
     * tool responses stay clean. The structured result is returned unchanged.
     */
    JsonNode executeQuietly(Tool tool, JsonNode args) throws ToolException {
        java.io.PrintStream original = System.out;
        System.setOut(new java.io.PrintStream(java.io.OutputStream.nullOutputStream()));
        try {
            return tool.execute(args);
        } finally {
            System.setOut(original);
        }
    }

    /**
     * Interactively resolve required entity arguments (scenario, testcase) that
     * the caller did not supply, by presenting a numbered selection list.
     *
     * @return true to proceed, false if the user cancelled.
     */
    private boolean fillEntityArgs(Tool tool, ObjectNode args) {
        JsonNode required = tool.inputSchema().path("required");
        java.util.Set<String> req = new java.util.LinkedHashSet<>();
        required.forEach(n -> req.add(n.asText()));

        boolean needsScenario = req.contains("scenario") && !args.hasNonNull("scenario");
        boolean needsTestcase = req.contains("testcase") && !args.hasNonNull("testcase");

        if (needsScenario) {
            List<String> scenarios = listScenarios();
            String chosen = selectFrom("Scenario", scenarios, false);
            if (chosen == null) {
                System.out.println(theme.yellow("Cancelled."));
                return false;
            }
            args.put("scenario", chosen);
        }
        if (needsTestcase) {
            String scenario = args.path("scenario").asText(null);
            List<String> testcases = listTestCases(scenario);
            String chosen = selectFrom("Test case", testcases, false);
            if (chosen == null) {
                System.out.println(theme.yellow("Cancelled."));
                return false;
            }
            args.put("testcase", chosen);
        }
        return true;
    }

    /**
     * Interactive {@code run} builder: choose a single test case or a test set,
     * then guide the user through the required selections and execute.
     */
    private void interactiveRun() {
        if (session.project() == null) {
            System.out.println(theme.fail("No project selected. Use /project first."));
            return;
        }
        String mode = selectFrom(
            "What do you want to run",
            List.of("Single test case", "Test set (collection)"),
            false
        );
        if (mode == null) {
            System.out.println(theme.yellow("Cancelled."));
            return;
        }
        ObjectNode runArgs = mapper.createObjectNode();
        String goal;
        if (mode.startsWith("Single")) {
            String scenario = selectFrom("Scenario", listScenarios(), false);
            if (scenario == null) return;
            String testcase = selectFrom("Test case", listTestCases(scenario), false);
            if (testcase == null) return;
            String browser = selectFrom("Browser", listBrowsers(), false);
            if (browser == null) return;
            runArgs.put("target", session.project() + "/" + scenario + "/" + testcase);
            runArgs.put("browser", browser);
            goal = "Run test case " + scenario + "/" + testcase;
        } else {
            String release = selectFrom("Release", listReleases(), false);
            if (release == null) return;
            String testset = selectFrom("Test set", listTestSets(release), false);
            if (testset == null) return;
            runArgs.put("target", session.project() + "/" + release + "/" + testset);
            goal = "Run test set " + release + "/" + testset;
        }
        Plan plan = new Plan(goal, List.of(new Plan.PlanStep("s1", "run", runArgs, List.of())));
        session.setPendingPlan(plan);
        showPlan(plan);
        confirmAndRun(plan);
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    String projectArg() {
        return session.projectPath() != null ? session.projectPath() : session.project();
    }

    // ------------------------------------------------------------------
    // interactive selection
    // ------------------------------------------------------------------

    /** Returned by {@link #selectFrom} when the user picks "Create new…". */
    static final String CREATE_NEW = "\u0000__create__";

    /**
     * Present options and read a selection. In an interactive terminal this
     * uses a pointer (▶) navigable with the arrow keys; otherwise it falls back
     * to a numbered list. Returns the chosen value, {@link #CREATE_NEW} when
     * allowCreate is set and that option is picked, or null if cancelled.
     */
    String selectFrom(String title, List<String> options, boolean allowCreate) {
        return selectFrom(title, options, allowCreate, null);
    }

    /** Same as {@link #selectFrom(String, List, boolean)}, marking {@code current} as the active choice. */
    String selectFrom(String title, List<String> options, boolean allowCreate, String current) {
        if (options.isEmpty() && !allowCreate) {
            System.out.println();
            System.out.println(theme.yellow("No " + title.toLowerCase() + "s available."));
            return null;
        }
        if (interactiveTerminal()) {
            return selectArrow(title, options, allowCreate, current);
        }
        return selectNumbered(title, options, allowCreate, current);
    }

    /** True when the terminal supports raw-mode arrow-key navigation. */
    private boolean interactiveTerminal() {
        if (terminal == null) return false;
        String type = terminal.getType();
        return (
            type != null &&
            !Terminal.TYPE_DUMB.equals(type) &&
            !Terminal.TYPE_DUMB_COLOR.equals(type) &&
            theme.ansiEnabled()
        );
    }

    /** Arrow-key pointer selection (▶). Falls back to numbered on any error. */
    private String selectArrow(
        String title,
        List<String> options,
        boolean allowCreate,
        String current
    ) {
        List<String> items = new ArrayList<>(options);
        if (allowCreate) items.add("Create new…");

        System.out.println();
        System.out.println(
            theme.bold(title + ":") + theme.dim("   (↑/↓ move · Enter select · Esc cancel)")
        );

        int cursor = current == null ? 0 : Math.max(0, options.indexOf(current));
        org.jline.terminal.Attributes prev = terminal.enterRawMode();
        try {
            renderMenu(items, cursor, current);
            org.jline.utils.NonBlockingReader rd = terminal.reader();
            while (true) {
                int c = rd.read();
                if (c < 0) {
                    return null; // EOF / stream closed
                } else if (c == 13 || c == 10) {
                    break; // Enter
                } else if (c == 3 || c == 4 || c == 'q') {
                    return null; // Ctrl-C / Ctrl-D / q
                } else if (c == 27) {
                    int c1 = rd.read(60);
                    if (c1 == '[' || c1 == 'O') {
                        int c2 = rd.read(60);
                        if (c2 == 'A') cursor =
                            (cursor - 1 + items.size()) % items.size(); else if (c2 == 'B') cursor =
                            (cursor + 1) % items.size();
                    } else {
                        return null; // lone Esc
                    }
                } else if (c == 'k') {
                    cursor = (cursor - 1 + items.size()) % items.size();
                } else if (c == 'j') {
                    cursor = (cursor + 1) % items.size();
                } else if (c >= '1' && c <= '9') {
                    int idx = c - '1';
                    if (idx < items.size()) cursor = idx;
                }
                redrawMenu(items, cursor, current);
            }
        } catch (java.io.IOException e) {
            return selectNumbered(title, options, allowCreate, current);
        } finally {
            terminal.setAttributes(prev);
        }
        if (allowCreate && cursor == items.size() - 1) return CREATE_NEW;
        return options.get(cursor);
    }

    private void renderMenu(List<String> items, int cursor, String current) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            sb.append(menuLine(items.get(i), i == cursor, current)).append("\r\n");
        }
        System.out.print(sb);
        System.out.flush();
    }

    private void redrawMenu(List<String> items, int cursor, String current) {
        StringBuilder sb = new StringBuilder();
        sb.append("\u001b[").append(items.size()).append('A'); // cursor up N lines
        for (int i = 0; i < items.size(); i++) {
            sb
                .append("\u001b[2K")
                .append(menuLine(items.get(i), i == cursor, current))
                .append("\r\n");
        }
        System.out.print(sb);
        System.out.flush();
    }

    private String menuLine(String item, boolean selected, String current) {
        String marker = item.equals(current) ? theme.dim(" (current)") : "";
        if (selected) {
            return (
                "  " + theme.purple("\u25b6") + " " + theme.bold(theme.brightPurple(item)) + marker
            );
        }
        return "    " + item + marker;
    }

    /** Numbered selection fallback (non-interactive terminals / pipes). */
    private String selectNumbered(
        String title,
        List<String> options,
        boolean allowCreate,
        String current
    ) {
        System.out.println();
        System.out.println(theme.bold(title + ":"));
        for (int i = 0; i < options.size(); i++) {
            String marker = options.get(i).equals(current) ? "  " + theme.dim("(current)") : "";
            System.out.println(
                "  " + theme.purple(String.valueOf(i + 1)) + ") " + options.get(i) + marker
            );
        }
        int createIdx = -1;
        int max = options.size();
        if (allowCreate) {
            createIdx = options.size() + 1;
            max = createIdx;
            System.out.println(
                "  " + theme.purple(String.valueOf(createIdx)) + ") " + theme.dim("Create new…")
            );
        }
        try {
            String v = reader.readLine("  Select [1-" + max + "]: ");
            if (v == null || v.isBlank()) return null;
            v = v.trim();
            for (String o : options) {
                if (o.equalsIgnoreCase(v)) return o;
            }
            int idx = Integer.parseInt(v);
            if (allowCreate && idx == createIdx) return CREATE_NEW;
            if (idx >= 1 && idx <= options.size()) return options.get(idx - 1);
            System.out.println(theme.yellow("Invalid selection."));
            return null;
        } catch (NumberFormatException e) {
            System.out.println(theme.yellow("Invalid selection."));
            return null;
        } catch (UserInterruptException | EndOfFileException e) {
            return null;
        }
    }

    /** Collect a distinct field's values from a list-returning tool. */
    private List<String> pluck(String toolId, ObjectNode args, String field) {
        List<String> out = new ArrayList<>();
        Tool tool = registry.get(toolId);
        if (tool == null) return out;
        ExecutionEngine.injectProject(tool, args, projectArg());
        try {
            JsonNode result = executeQuietly(tool, args);
            JsonNode array = result.isArray() ? result : firstArray(result);
            if (array != null) {
                for (JsonNode n : array) {
                    JsonNode v = n.path(field);
                    if (v.isTextual() && !out.contains(v.asText())) out.add(v.asText());
                }
            }
        } catch (Exception ignored) {
            // selection lists are best-effort
        }
        return out;
    }

    private static JsonNode firstArray(JsonNode node) {
        if (node == null || !node.isObject()) return null;
        Iterator<Map.Entry<String, JsonNode>> it = node.fields();
        while (it.hasNext()) {
            JsonNode v = it.next().getValue();
            if (v.isArray()) return v;
        }
        return null;
    }

    List<String> listProjects() {
        return pluck("project_list", mapper.createObjectNode(), "name");
    }

    List<String> listScenarios() {
        return pluck("scenario_list", mapper.createObjectNode(), "name");
    }

    List<String> listTestCases(String scenario) {
        ObjectNode a = mapper.createObjectNode();
        if (scenario != null) a.put("scenario", scenario);
        return pluck("testcase_list", a, "testcase");
    }

    List<String> listReleases() {
        return pluck("testset_list", mapper.createObjectNode(), "release");
    }

    List<String> listTestSets(String release) {
        ObjectNode a = mapper.createObjectNode();
        if (release != null) a.put("release", release);
        return pluck("testset_list", a, "testset");
    }

    /**
     * Browser/driver options for a run: the standard engines plus any device
     * emulators configured for the active project (Settings/Emulators.json).
     */
    List<String> listBrowsers() {
        List<String> out = new ArrayList<>(
            List.of("Chromium", "Firefox", "WebKit", "No Browser", "SAP")
        );
        String root = session.projectPath();
        if (root != null) {
            java.nio.file.Path emulators = Path.of(root, "Settings", "Emulators.json");
            if (java.nio.file.Files.exists(emulators)) {
                try {
                    JsonNode arr = mapper.readTree(emulators.toFile());
                    if (arr.isArray()) {
                        for (JsonNode n : arr) {
                            String name = n.path("Name").asText(null);
                            if (name != null && !name.isBlank() && !out.contains(name)) {
                                out.add(name);
                            }
                        }
                    }
                } catch (Exception ignored) {
                    // emulator list is best-effort
                }
            }
        }
        return out;
    }

    UndoJournal journal() {
        String root = session.projectPath();
        if (root == null) return null;
        if (journal == null || !root.equals(journalRoot)) {
            journal = new UndoJournal(Path.of(root));
            journalRoot = root;
        }
        return journal;
    }

    AiProvider ensureProvider() {
        if (provider == null) {
            provider = aiConfig.createProvider(tokens, () -> session.projectPath());
        }
        if (
            provider instanceof com.ing.engine.aicli.ai.CopilotProvider &&
            !((com.ing.engine.aicli.ai.CopilotProvider) provider).isLoggedIn()
        ) {
            System.out.println(
                theme.yellow(
                    "AI is not configured yet. Run /login to sign in with GitHub Copilot, " +
                    "or /model provider openai to use an OpenAI-compatible endpoint."
                )
            );
            System.out.println(
                theme.dim("Deterministic workflows and /tools work without AI — try /help.")
            );
            return null;
        }
        return provider;
    }

    void resetProvider() {
        provider = null;
    }

    AiProvider currentProviderUnchecked() {
        if (provider == null) provider =
            aiConfig.createProvider(tokens, () -> session.projectPath());
        return provider;
    }

    private String prompt() {
        String name = session.project() == null ? "" : session.project();
        // Project name in bold purple — it is the active context for all commands.
        String label = name.isEmpty() ? "" : theme.bold(theme.purple(name)) + " ";
        return label + theme.purple(Theme.ARROW) + " ";
    }

    private void banner() {
        AiBanner.print(theme, session, currentProviderUnchecked());
    }
}

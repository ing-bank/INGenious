package com.ing.engine.aicli.repl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ing.engine.aicli.ai.AiProvider;
import com.ing.engine.aicli.ai.ChatMessage;
import com.ing.engine.aicli.ai.CopilotProvider;
import com.ing.engine.aicli.ai.CopilotSdkProvider;
import com.ing.engine.aicli.ai.ProviderConfig;
import com.ing.engine.aicli.planning.WorkflowCatalog;
import com.ing.engine.aicli.tools.Tool;
import com.ing.engine.aicli.ui.Theme;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Dispatch for {@code /command} input in the interactive AI CLI. */
final class SlashCommands {
    static final List<String> COMMANDS = List.of(
        "/help",
        "/tools",
        "/workflows",
        "/history",
        "/plan",
        "/approve",
        "/clear",
        "/context",
        "/model",
        "/login",
        "/undo",
        "/redo",
        "/status",
        "/config",
        "/project",
        "/exit",
        "/quit"
    );

    private final Repl repl;
    private final Theme t;

    SlashCommands(Repl repl) {
        this.repl = repl;
        this.t = repl.theme;
    }

    /** Returns false when the REPL should exit. */
    boolean handle(String line) {
        String[] parts = line.split("\\s+", 2);
        String cmd = parts[0].toLowerCase();
        String rest = parts.length > 1 ? parts[1].trim() : "";

        switch (cmd) {
            case "/exit":
            case "/quit":
                return false;
            case "/help":
                help();
                return true;
            case "/tools":
                tools(rest);
                return true;
            case "/workflows":
                workflows();
                return true;
            case "/history":
                history();
                return true;
            case "/plan":
                plan(rest);
                return true;
            case "/approve":
                approve();
                return true;
            case "/clear":
                clear(rest);
                return true;
            case "/context":
                context();
                return true;
            case "/model":
                model(rest);
                return true;
            case "/login":
                login();
                return true;
            case "/undo":
                undo();
                return true;
            case "/redo":
                redo();
                return true;
            case "/status":
                status();
                return true;
            case "/config":
                config(rest);
                return true;
            case "/project":
                project(rest);
                return true;
            default:
                System.out.println(t.fail("Unknown command: " + cmd + " (see /help)"));
                return true;
        }
    }

    private void help() {
        List<String> lines = new ArrayList<>();
        lines.add(t.bold("/help") + "               this overview");
        lines.add(
            t.bold("/tools [cat]") +
            "        list tools; " +
            t.dim("/tools run <tool> [json]") +
            " invokes one"
        );
        lines.add(t.bold("/workflows") + "          deterministic workflows (no AI needed)");
        lines.add(t.bold("/plan [run]") + "         show / execute the pending plan");
        lines.add(t.bold("/approve") + "            execute the pending plan");
        lines.add(
            t.bold("/undo /redo") + "         revert / re-apply the last plan's file changes"
        );
        lines.add(t.bold("/project <name>") + "     set the active project");
        lines.add(t.bold("/context") + "            session memory (project, recent files, …)");
        lines.add(t.bold("/history") + "            recent conversation turns");
        lines.add(
            t.bold("/model [...]") +
            "        show/set AI model; " +
            t.dim("/model list") +
            " to pick from available"
        );
        lines.add(t.bold("/login") + "              sign in with GitHub Copilot (device flow)");
        lines.add(t.bold("/config [get|set]") + "   project configuration");
        lines.add(t.bold("/status") + "             provider, project, pending plan, undo depth");
        lines.add(
            t.bold("/clear [--all]") + "      clear conversation (--all also clears session facts)"
        );
        lines.add(t.bold("/exit") + "               quit");
        repl.panels.print("Commands", lines);
        System.out.println(
            t.dim("Anything else is treated as a request, e.g. \"create a login test\".")
        );
    }

    private void tools(String rest) {
        if (rest.startsWith("run ")) {
            String[] p = rest.substring(4).trim().split("\\s+", 2);
            ObjectNode args;
            try {
                args =
                    p.length > 1
                        ? (ObjectNode) repl.mapper.readTree(p[1])
                        : repl.mapper.createObjectNode();
            } catch (Exception e) {
                System.out.println(t.fail("Arguments must be a JSON object: " + e.getMessage()));
                return;
            }
            repl.runTool(p[0], args);
            return;
        }
        String filter = rest.isBlank() ? null : rest.toLowerCase();
        Map<String, List<Tool>> byCat = repl.registry.byCategory();
        int total = 0;
        for (Map.Entry<String, List<Tool>> e : byCat.entrySet()) {
            if (filter != null && !e.getKey().toLowerCase().startsWith(filter)) continue;
            System.out.println(
                t.bold(t.brightPurple(e.getKey())) + t.dim(" (" + e.getValue().size() + ")")
            );
            for (Tool tool : e.getValue()) {
                String marker = tool.mutatesFiles() ? t.yellow(Theme.WARN) : " ";
                String desc = tool.description().replace('\n', ' ');
                if (desc.length() > 70) desc = desc.substring(0, 70) + "…";
                System.out.printf("  %s %-28s %s%n", marker, tool.id(), t.dim(desc));
                total++;
            }
        }
        System.out.println(
            t.dim(total + " tools. Invoke directly: /tools run <tool> {\"arg\":\"value\"}")
        );
    }

    private void workflows() {
        for (WorkflowCatalog.Workflow w : WorkflowCatalog.all()) {
            System.out.println("  " + t.bold(w.id()) + "  " + t.dim(w.description()));
        }
    }

    private void history() {
        List<ChatMessage> turns = repl.convo.turns();
        if (turns.isEmpty()) {
            System.out.println(t.dim("No conversation yet."));
            return;
        }
        int from = Math.max(0, turns.size() - 20);
        for (ChatMessage m : turns.subList(from, turns.size())) {
            String content = m.content().replace('\n', ' ');
            if (content.length() > 100) content = content.substring(0, 100) + "…";
            String role = "user".equals(m.role()) ? t.cyan("you") : t.brightPurple("ai ");
            System.out.println("  " + role + "  " + content);
        }
    }

    private void plan(String rest) {
        com.ing.engine.aicli.planning.Plan pending = repl.session.pendingPlan();
        if (pending == null) {
            System.out.println(t.dim("No pending plan. Describe what you want to do."));
            return;
        }
        if ("run".equalsIgnoreCase(rest)) {
            repl.runPlan(pending);
        } else {
            repl.showPlan(pending);
            System.out.println(t.dim("Execute with /approve or /plan run."));
        }
    }

    private void approve() {
        com.ing.engine.aicli.planning.Plan pending = repl.session.pendingPlan();
        if (pending == null) {
            System.out.println(t.dim("Nothing to approve."));
            return;
        }
        repl.runPlan(pending);
    }

    private void clear(String rest) {
        repl.convo.clear();
        if ("--all".equals(rest)) {
            repl.session.clearFacts();
            repl.session.save();
            System.out.println(t.dim("Conversation and session facts cleared."));
        } else {
            System.out.println(
                t.dim("Conversation cleared (session facts kept; use /clear --all).")
            );
        }
    }

    private void context() {
        repl.panels.print("Context", List.of(repl.session.summary().split("\n")));
    }

    private void model(String rest) {
        if (rest.isBlank()) {
            System.out.println("  Provider: " + t.bold(repl.aiConfig.provider));
            System.out.println("  Model:    " + t.bold(repl.aiConfig.model));
            if ("openai".equals(repl.aiConfig.provider)) {
                System.out.println("  Base URL: " + repl.aiConfig.baseUrl);
                System.out.println("  API key:  env " + repl.aiConfig.apiKeyEnv);
            }
            System.out.println(
                t.dim(
                    "Change with /model list (pick from available), /model <name>, " +
                    "/model provider bridge|openai|copilot|copilot-sdk, /model url <baseUrl>"
                )
            );
            return;
        }
        String[] p = rest.split("\\s+", 2);
        String selector = p[0].toLowerCase();
        if ("list".equals(selector) || "ls".equals(selector) || "models".equals(selector)) {
            modelList();
            return;
        }
        try {
            if ("provider".equals(selector)) {
                if (
                    p.length < 2 ||
                    !(
                        p[1].equals("openai") ||
                        p[1].equals("copilot") ||
                        p[1].equals("copilot-sdk") ||
                        p[1].equals("bridge")
                    )
                ) {
                    System.out.println(
                        t.fail("Usage: /model provider bridge|openai|copilot|copilot-sdk")
                    );
                    return;
                }
                repl.aiConfig.provider = p[1];
            } else if ("url".equals(selector)) {
                if (p.length < 2) {
                    System.out.println(t.fail("Usage: /model url <baseUrl>"));
                    return;
                }
                repl.aiConfig.baseUrl = p[1];
            } else {
                repl.aiConfig.model = rest;
            }
            repl.aiConfig.save();
            repl.resetProvider();
            System.out.println(
                t.ok("AI config updated: " + repl.currentProviderUnchecked().describe())
            );
        } catch (Exception e) {
            System.out.println(t.fail("Could not save AI config: " + e.getMessage()));
        }
    }

    /** Lists models from the current provider's endpoint and lets the user pick one. */
    private void modelList() {
        if ("copilot-sdk".equalsIgnoreCase(repl.aiConfig.provider)) {
            List<String> ids;
            try {
                ids = CopilotSdkProvider.listAvailableModels();
            } catch (AiProvider.AiException e) {
                System.out.println(t.fail(e.getMessage()));
                System.out.println(
                    t.dim("Verify the Copilot CLI auth in this shell: copilot auth login")
                );
                return;
            }
            if (ids.isEmpty()) {
                System.out.println(t.fail("No models returned by the Copilot CLI."));
                return;
            }
            String chosen = repl.selectFrom(
                "Available models (copilot-sdk)",
                ids,
                false,
                repl.aiConfig.model
            );
            if (chosen != null) applyModel(chosen);
            return;
        }

        String base = modelsBaseUrl();
        if (base == null || base.isBlank()) {
            System.out.println(
                t.fail(
                    "No models endpoint available for provider '" + repl.aiConfig.provider + "'."
                )
            );
            if ("copilot".equalsIgnoreCase(repl.aiConfig.provider)) {
                System.out.println(
                    t.dim("Switch to the bridge with /model provider bridge (VS Code Copilot).")
                );
            }
            return;
        }
        List<String> ids;
        try {
            ids = fetchModels(base);
        } catch (Exception e) {
            System.out.println(
                t.fail("Could not fetch models from " + base + "/models: " + e.getMessage())
            );
            if ("bridge".equalsIgnoreCase(repl.aiConfig.provider)) {
                System.out.println(t.dim("Is VS Code open with the INGenious bridge running?"));
            }
            return;
        }
        if (ids.isEmpty()) {
            System.out.println(t.fail("No models returned by " + base + "/models."));
            return;
        }
        String chosen = repl.selectFrom(
            "Available models (" + base + ")",
            ids,
            false,
            repl.aiConfig.model
        );
        if (chosen != null) applyModel(chosen);
    }

    /** Resolves the base URL (…/v1) whose /models endpoint should be queried. */
    private String modelsBaseUrl() {
        if ("bridge".equalsIgnoreCase(repl.aiConfig.provider)) {
            String u = ProviderConfig.discoverBridgeBaseUrl();
            if (u != null && !u.isBlank()) {
                return u;
            }
        }
        if ("copilot".equalsIgnoreCase(repl.aiConfig.provider)) {
            // Copilot has no HTTP /models endpoint of its own; fall back to the
            // bridge if one is running.
            return ProviderConfig.discoverBridgeBaseUrl();
        }
        return repl.aiConfig.baseUrl;
    }

    /** GET {base}/models and return the sorted list of model ids. */
    private List<String> fetchModels(String base) throws Exception {
        String url = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        HttpRequest.Builder rb = HttpRequest
            .newBuilder(URI.create(url + "/models"))
            .timeout(Duration.ofSeconds(15))
            .header("Accept", "application/json")
            .GET();
        if ("openai".equalsIgnoreCase(repl.aiConfig.provider) && repl.aiConfig.apiKeyEnv != null) {
            String key = System.getenv(repl.aiConfig.apiKeyEnv);
            if (key != null && !key.isBlank()) {
                rb.header("Authorization", "Bearer " + key);
            }
        }
        HttpResponse<String> resp = HttpClient
            .newHttpClient()
            .send(rb.build(), HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() / 100 != 2) {
            throw new RuntimeException("HTTP " + resp.statusCode());
        }
        JsonNode data = repl.mapper.readTree(resp.body()).path("data");
        List<String> ids = new ArrayList<>();
        if (data.isArray()) {
            for (JsonNode m : data) {
                String id = m.path("id").asText(null);
                if (id != null && !id.isBlank()) {
                    ids.add(id);
                }
            }
        }
        Collections.sort(ids);
        return ids;
    }

    /** Persists the chosen model and rebuilds the provider. */
    private void applyModel(String id) {
        repl.aiConfig.model = id;
        try {
            repl.aiConfig.save();
            repl.resetProvider();
            System.out.println(
                t.ok("Model set to " + id + " — " + repl.currentProviderUnchecked().describe())
            );
        } catch (Exception e) {
            System.out.println(t.fail("Could not save AI config: " + e.getMessage()));
        }
    }

    private void login() {
        AiProvider p = repl.currentProviderUnchecked();
        if (p instanceof CopilotProvider) {
            try {
                ((CopilotProvider) p).login();
            } catch (AiProvider.AiException e) {
                System.out.println(t.fail(e.getMessage()));
            }
        } else if ("copilot-sdk".equalsIgnoreCase(repl.aiConfig.provider)) {
            System.out.println(
                t.dim(
                    "copilot-sdk uses the external Copilot CLI auth in your shell; " +
                    "run 'copilot auth login' if needed."
                )
            );
        } else {
            System.out.println(
                t.dim(
                    "The openai provider reads its key from env " +
                    repl.aiConfig.apiKeyEnv +
                    ". Switch to Copilot with /model provider copilot."
                )
            );
        }
    }

    private void undo() {
        com.ing.engine.aicli.execution.UndoJournal j = repl.journal();
        if (j == null) {
            System.out.println(t.fail("No project selected (/project <name>)."));
            return;
        }
        try {
            System.out.println(t.ok("Undone: " + j.undo()));
        } catch (Exception e) {
            System.out.println(t.dim(e.getMessage()));
        }
    }

    private void redo() {
        com.ing.engine.aicli.execution.UndoJournal j = repl.journal();
        if (j == null) {
            System.out.println(t.fail("No project selected (/project <name>)."));
            return;
        }
        try {
            System.out.println(t.ok("Redone: " + j.redo()));
        } catch (Exception e) {
            System.out.println(t.dim(e.getMessage()));
        }
    }

    private void status() {
        System.out.println(
            "  Project:  " +
            (repl.session.project() == null ? t.dim("(none)") : t.bold(repl.session.project()))
        );
        System.out.println("  AI:       " + t.dim(repl.currentProviderUnchecked().describe()));
        com.ing.engine.aicli.ai.Usage usage = repl.currentProviderUnchecked().usage();
        if (usage != null && !usage.isEmpty()) {
            System.out.println("  Credits:  " + t.dim(usage.summary() + " this session"));
        }
        System.out.println(
            "  Pending:  " +
            (
                repl.session.pendingPlan() == null
                    ? t.dim("(no plan)")
                    : t.bold(repl.session.pendingPlan().goal)
            )
        );
        com.ing.engine.aicli.execution.UndoJournal j = repl.journal();
        if (j != null) {
            System.out.println("  Undo:     " + j.undoCount() + " entries, redo: " + j.redoCount());
        }
        List<String> plugins = repl.registry.pluginSummaries();
        if (!plugins.isEmpty()) {
            System.out.println("  Plugins:  " + String.join(", ", plugins));
        }
    }

    private void config(String rest) {
        try {
            if (rest.isBlank() || "show".equals(rest)) {
                repl.runTool("config_show", repl.mapper.createObjectNode());
            } else if (rest.startsWith("get ")) {
                ObjectNode args = repl.mapper.createObjectNode();
                args.put("key", rest.substring(4).trim());
                repl.runTool("config_get", args);
            } else if (rest.startsWith("set ")) {
                String[] kv = rest.substring(4).trim().split("\\s+", 2);
                if (kv.length < 2) {
                    System.out.println(t.fail("Usage: /config set <key> <value>"));
                    return;
                }
                ObjectNode args = repl.mapper.createObjectNode();
                args.put("key", kv[0]);
                args.put("value", kv[1]);
                repl.runTool("config_set", args);
            } else {
                System.out.println(t.fail("Usage: /config [show|get <key>|set <key> <value>]"));
            }
        } catch (Exception e) {
            System.out.println(t.fail("Config failed: " + e.getMessage()));
        }
    }

    private void project(String rest) {
        if (rest.isBlank()) {
            List<String> projects = repl.listProjects();
            String chosen = repl.selectFrom("Projects", projects, true);
            if (chosen == null) {
                System.out.println(t.dim("No project selected."));
                return;
            }
            if (Repl.CREATE_NEW.equals(chosen)) {
                createProject();
                return;
            }
            applyProject(chosen);
            return;
        }
        applyProject(rest);
    }

    /** Set the active project by name and enrich session facts. */
    private void applyProject(String name) {
        repl.session.setProject(name, repl.cwd);
        repl.session.save();
        if (repl.session.projectPath() != null) {
            System.out.println(t.ok("Project: " + t.bold(t.brightPurple(repl.session.project()))));
            enrich();
        } else {
            System.out.println(
                t.yellow(
                    "Project set to '" +
                    name +
                    "' but no directory found under ./Projects — tools may not resolve it."
                )
            );
        }
    }

    /** Prompt for a new project name and create it via the project_create tool. */
    private void createProject() {
        try {
            String name = repl.reader.readLine("  New project name: ");
            if (name == null || name.isBlank()) {
                System.out.println(t.dim("Cancelled."));
                return;
            }
            ObjectNode args = repl.mapper.createObjectNode();
            args.put("name", name.trim());
            repl.runTool("project_create", args);
            applyProject(name.trim());
        } catch (Exception e) {
            System.out.println(t.fail("Could not create project: " + e.getMessage()));
        }
    }

    /** Pull framework/language facts from project_info into session memory. */
    private void enrich() {
        Tool info = repl.registry.get("project_info");
        if (info == null) return;
        try {
            ObjectNode args = repl.mapper.createObjectNode();
            args.put("project", repl.projectArg());
            JsonNode result = repl.executeQuietly(info, args);
            String type = result.path("type").asText(result.path("framework").asText(null));
            if (type != null && !type.isBlank()) repl.session.setFramework(type);
            repl.session.save();
        } catch (Exception ignored) {
            // enrichment is optional
        }
    }
}

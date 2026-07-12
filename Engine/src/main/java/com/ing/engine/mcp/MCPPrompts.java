package com.ing.engine.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pre-built prompt templates for natural-language workflows.
 *
 * <p>Prompts are reusable LLM instructions a host application can surface
 * to the user as slash-commands, palette entries, or auto-suggestions:
 * "/ingenious create test", "/ingenious explain failure", etc.
 */
final class MCPPrompts {
    private final Map<String, Prompt> registry = new LinkedHashMap<>();

    MCPPrompts() {
        register(
            new Prompt(
                "create_test_case",
                "Build a new INGenious test case from a plain-English description.",
                arg("project", "Project name or absolute path.", false),
                arg("scenario", "Scenario folder under TestPlan/ (will be created).", true),
                arg("testcase", "Test case name to create.", true),
                arg("description", "What the test should do, in plain language.", true),
                arg("browser", "Optional: target browser. Defaults to Chromium.", false)
            )
        );

        register(
            new Prompt(
                "convert_manual_steps",
                "Convert a numbered list of manual test steps into INGenious actions, then create the test case.",
                arg("project", "Project name or absolute path.", false),
                arg("scenario", "Scenario folder under TestPlan/.", true),
                arg("testcase", "Test case name to create.", true),
                arg("steps", "The manual steps, one per line.", true)
            )
        );

        register(
            new Prompt(
                "explain_failure",
                "Investigate the most recent failure for a target and explain what went wrong.",
                arg("project", "Project name or absolute path.", false),
                arg("target", "'<Scenario>/<TestCase>' or '<Release>/<TestSet>'.", true)
            )
        );

        register(
            new Prompt(
                "debug_test",
                "Walk through a test case step-by-step, identifying any fragile or risky steps.",
                arg("project", "Project name or absolute path.", false),
                arg("scenario", "Scenario name.", true),
                arg("testcase", "Test case name.", true)
            )
        );

        register(
            new Prompt(
                "suggest_locator",
                "Suggest a stable Playwright locator for a UI element described in English.",
                arg("description", "Plain-language description of the element to locate.", true),
                arg("html", "Optional HTML snippet for additional context.", false)
            )
        );

        register(
            new Prompt(
                "review_test_case",
                "Review a test case for best practices, missing waits, brittle locators, and bad assertions.",
                arg("project", "Project name or absolute path.", false),
                arg("scenario", "Scenario name.", true),
                arg("testcase", "Test case name.", true)
            )
        );

        register(
            new Prompt(
                "run_and_summarize",
                "Run a test target, then summarise pass/fail counts and the top reasons for failure.",
                arg("project", "Project name or absolute path.", false),
                arg(
                    "target",
                    "<Project>/<Scenario>/<TestCase> or <Project>/<Release>/<TestSet>.",
                    true
                ),
                arg("browser", "Optional browser. Defaults to Chromium.", false)
            )
        );

        register(
            new Prompt(
                "author_by_archetype",
                "Generate a test case from an archetype template, fill its parameters, validate, and run.",
                arg("project", "Project name or absolute path.", false),
                arg(
                    "archetype",
                    "Archetype name (see ingenious_gen_list), e.g. browser-login.",
                    true
                ),
                arg("scenario", "Target scenario.", true),
                arg("testcase", "Test case name to create.", true),
                arg(
                    "description",
                    "What the test should do / values to use, in plain language.",
                    false
                )
            )
        );

        register(
            new Prompt(
                "build_data_driven_suite",
                "Turn a test case into a parameterised, data-driven suite backed by a data sheet.",
                arg("project", "Project name or absolute path.", false),
                arg("scenario", "Scenario name.", true),
                arg("testcase", "Test case to parameterise.", true),
                arg("sheet", "Data sheet name to create/populate.", true)
            )
        );

        register(
            new Prompt(
                "harden_test",
                "Make a test case more robust: replace brittle locators/sleeps, add waits and assertions.",
                arg("project", "Project name or absolute path.", false),
                arg("scenario", "Scenario name.", true),
                arg("testcase", "Test case name.", true)
            )
        );

        register(
            new Prompt(
                "triage_run",
                "Diagnose a failing/regressed target: compare runs, read failures, and propose fixes.",
                arg("project", "Project name or absolute path.", false),
                arg("target", "'<Scenario>/<TestCase>' or '<Release>/<TestSet>'.", true)
            )
        );

        register(
            new Prompt(
                "record_browser_test",
                "Drive a live Playwright Agent CLI session to author a browser test hands-free.",
                arg("project", "Project name or absolute path.", false),
                arg("scenario", "Target scenario.", true),
                arg("testcase", "Test case name to create.", true),
                arg("url", "Start URL to open in the session.", true)
            )
        );

        register(
            new Prompt(
                "bootstrap_project",
                "Create a new project and seed it with a sample archetype test case per capability.",
                arg("name", "New project name.", true),
                arg("parentDir", "Parent directory (default: current working dir).", false)
            )
        );

        register(
            new Prompt(
                "make_data_driven",
                "Externalise a test case's hard-coded values into data sheets using the " +
                "scan/apply parameterization workflow.",
                arg("project", "Project name or absolute path.", false),
                arg("scenario", "Scenario containing the test case.", true),
                arg("testcase", "Test case to parameterize.", true)
            )
        );

        register(
            new Prompt(
                "extract_reusable",
                "Find repeated step sequences and extract them into reusable components " +
                "(user intents), recomposing the test cases with Execute steps.",
                arg("project", "Project name or absolute path.", false),
                arg("scenario", "Restrict the analysis to one scenario (optional).", false)
            )
        );
    }

    JsonNode list(ObjectMapper json) {
        ObjectNode out = json.createObjectNode();
        ArrayNode arr = out.putArray("prompts");
        for (Prompt p : registry.values()) {
            ObjectNode n = arr.addObject();
            n.put("name", p.name);
            n.put("description", p.description);
            ArrayNode argsArr = n.putArray("arguments");
            for (Arg a : p.args) {
                ObjectNode an = argsArr.addObject();
                an.put("name", a.name);
                an.put("description", a.description);
                an.put("required", a.required);
            }
        }
        return out;
    }

    JsonNode get(ObjectMapper json, JsonNode params) {
        String name = MCPServer.requiredParam(params, "name");
        Prompt prompt = registry.get(name);
        if (prompt == null) throw new MCPServer.MCPException(-32602, "Unknown prompt: " + name);
        JsonNode args = params.path("arguments");

        // verify required args
        for (Arg a : prompt.args) {
            if (a.required && MCPServer.paramOrDefault(args, a.name, "").isEmpty()) {
                throw new MCPServer.MCPException(
                    -32602,
                    "Missing required argument '" + a.name + "' for prompt '" + name + "'"
                );
            }
        }

        String rendered = prompt.render(args);

        ObjectNode out = json.createObjectNode();
        out.put("description", prompt.description);
        ArrayNode messages = out.putArray("messages");
        ObjectNode msg = messages.addObject();
        msg.put("role", "user");
        ObjectNode content = msg.putObject("content");
        content.put("type", "text");
        content.put("text", rendered);
        return out;
    }

    private void register(Prompt p) {
        registry.put(p.name, p);
    }

    private static Arg arg(String n, String d, boolean req) {
        return new Arg(n, d, req);
    }

    // ------------------------------------------------------------------

    /** A prompt template with named arguments. {{arg}} placeholders are substituted at render time. */
    static class Prompt {
        final String name;
        final String description;
        final Arg[] args;

        Prompt(String name, String description, Arg... args) {
            this.name = name;
            this.description = description;
            this.args = args;
        }

        String render(JsonNode bound) {
            switch (name) {
                case "create_test_case":
                    return tplCreateTestCase(bound);
                case "convert_manual_steps":
                    return tplConvertManualSteps(bound);
                case "explain_failure":
                    return tplExplainFailure(bound);
                case "debug_test":
                    return tplDebugTest(bound);
                case "suggest_locator":
                    return tplSuggestLocator(bound);
                case "review_test_case":
                    return tplReviewTestCase(bound);
                case "run_and_summarize":
                    return tplRunAndSummarize(bound);
                case "author_by_archetype":
                    return tplAuthorByArchetype(bound);
                case "build_data_driven_suite":
                    return tplBuildDataDrivenSuite(bound);
                case "harden_test":
                    return tplHardenTest(bound);
                case "triage_run":
                    return tplTriageRun(bound);
                case "record_browser_test":
                    return tplRecordBrowserTest(bound);
                case "bootstrap_project":
                    return tplBootstrapProject(bound);
                case "make_data_driven":
                    return tplMakeDataDriven(bound);
                case "extract_reusable":
                    return tplExtractReusable(bound);
                default:
                    return description;
            }
        }
    }

    static class Arg {
        final String name;
        final String description;
        final boolean required;

        Arg(String name, String description, boolean required) {
            this.name = name;
            this.description = description;
            this.required = required;
        }
    }

    // ==================================================================
    // template bodies
    // ==================================================================

    private static String tplCreateTestCase(JsonNode args) {
        String project = MCPServer.paramOrDefault(args, "project", "<default>");
        String scenario = MCPServer.paramOrDefault(args, "scenario", "");
        String testcase = MCPServer.paramOrDefault(args, "testcase", "");
        String desc = MCPServer.paramOrDefault(args, "description", "");
        String browser = MCPServer.paramOrDefault(args, "browser", "Chromium");

        return (
            "You are operating an INGenious test automation project via its MCP tools.\n" +
            "\n" +
            "Task: create a new test case.\n" +
            "  Project : " +
            project +
            "\n" +
            "  Scenario: " +
            scenario +
            "\n" +
            "  Test case: " +
            testcase +
            "\n" +
            "  Browser : " +
            browser +
            "\n" +
            "\n" +
            "Goal\n" +
            "----\n" +
            desc +
            "\n" +
            "\n" +
            "Procedure\n" +
            "---------\n" +
            "1. Call `ingenious_action_list` (and/or `ingenious_action_search`) to discover\n" +
            "   the exact action names that exist for browser/API/etc. operations. Do NOT\n" +
            "   invent action names – every step MUST reference a real action.\n" +
            "2. Compose an ordered list of steps. Each step is an object with the keys\n" +
            "   {action, object, input, condition, description}. Leave empty strings for\n" +
            "   fields that don't apply.\n" +
            "3. Call `ingenious_testcase_create` with the project, scenario, testcase and\n" +
            "   the steps array. The scenario will be created if it doesn't exist.\n" +
            "4. Call `ingenious_testcase_show` to confirm the file was written correctly.\n" +
            "5. Optionally call `ingenious_run` to execute it and report the outcome.\n" +
            "\n" +
            "Constraints\n" +
            "-----------\n" +
            "* The first step of a browser test must Open the browser.\n" +
            "* Use semantic object names (e.g. `LoginPage.usernameField`) rather than raw\n" +
            "  selectors when an Object Repository entry exists or can plausibly be added.\n" +
            "* Add explicit waits/assertions – the test must fail loudly if behaviour drifts.\n"
        );
    }

    private static String tplConvertManualSteps(JsonNode args) {
        String project = MCPServer.paramOrDefault(args, "project", "<default>");
        String scenario = MCPServer.paramOrDefault(args, "scenario", "");
        String testcase = MCPServer.paramOrDefault(args, "testcase", "");
        String steps = MCPServer.paramOrDefault(args, "steps", "");

        return (
            "You are converting manual test steps into a runnable INGenious test case.\n" +
            "\n" +
            "Target: " +
            project +
            " / " +
            scenario +
            " / " +
            testcase +
            "\n" +
            "\n" +
            "Manual steps (verbatim):\n" +
            "---\n" +
            steps +
            "\n" +
            "---\n" +
            "\n" +
            "Procedure\n" +
            "---------\n" +
            "1. For each manual step, decide which INGenious action implements it. Discover\n" +
            "   valid actions with `ingenious_action_search` – never guess names.\n" +
            "2. Produce a step list (see `ingenious_testcase_create` schema).\n" +
            "3. Call `ingenious_testcase_create` to materialise it.\n" +
            "4. Briefly list any manual steps that didn't have a clean automated equivalent\n" +
            "   so the user can refine them.\n"
        );
    }

    private static String tplExplainFailure(JsonNode args) {
        String project = MCPServer.paramOrDefault(args, "project", "<default>");
        String target = MCPServer.paramOrDefault(args, "target", "");

        return (
            "Investigate the latest failure for " +
            project +
            " / " +
            target +
            ".\n" +
            "\n" +
            "Procedure\n" +
            "---------\n" +
            "1. Call `ingenious_report_latest` for the target to load the run metadata.\n" +
            "2. Call `ingenious_report_failures` to see which test cases failed.\n" +
            "3. For each failure, call `ingenious_testcase_show` on the failing test case\n" +
            "   so you can correlate steps with reported errors.\n" +
            "4. Produce a short, plain-language explanation containing:\n" +
            "     * Which steps failed, and the symptom message.\n" +
            "     * The most likely root cause (env, locator, timing, data, assertion).\n" +
            "     * A concrete suggested fix the user can act on next.\n"
        );
    }

    private static String tplDebugTest(JsonNode args) {
        String project = MCPServer.paramOrDefault(args, "project", "<default>");
        String scenario = MCPServer.paramOrDefault(args, "scenario", "");
        String testcase = MCPServer.paramOrDefault(args, "testcase", "");
        return (
            "Walk through " +
            project +
            " / " +
            scenario +
            " / " +
            testcase +
            " step-by-step and identify risks.\n" +
            "\n" +
            "1. Call `ingenious_testcase_show` to read the steps.\n" +
            "2. For each step, comment on:\n" +
            "     * Is the action appropriate (use `ingenious_action_info` if unsure)?\n" +
            "     * Is the locator/object name likely to be stable?\n" +
            "     * Are there missing waits or assertions around it?\n" +
            "3. Suggest at most 5 concrete changes ranked by impact.\n"
        );
    }

    private static String tplSuggestLocator(JsonNode args) {
        String desc = MCPServer.paramOrDefault(args, "description", "");
        String html = MCPServer.paramOrDefault(args, "html", "");
        return (
            "Suggest the most stable Playwright locator for this UI element.\n" +
            "\n" +
            "Element description: " +
            desc +
            "\n" +
            (html.isEmpty() ? "" : "\nDOM context:\n```html\n" + html + "\n```\n") +
            "\n" +
            "Prefer in order:\n" +
            "  1. getByRole / getByLabel / getByPlaceholder / getByText (semantic)\n" +
            "  2. data-testid / data-* attributes\n" +
            "  3. stable id / name / aria-label\n" +
            "  4. CSS / XPath only as a last resort\n" +
            "\n" +
            "Return: the recommended locator, why, and one fallback option.\n"
        );
    }

    private static String tplReviewTestCase(JsonNode args) {
        String project = MCPServer.paramOrDefault(args, "project", "<default>");
        String scenario = MCPServer.paramOrDefault(args, "scenario", "");
        String testcase = MCPServer.paramOrDefault(args, "testcase", "");
        return (
            "Review " +
            project +
            " / " +
            scenario +
            " / " +
            testcase +
            " for quality.\n" +
            "\n" +
            "1. Call `ingenious_testcase_show` to read all steps.\n" +
            "2. Check for: brittle CSS/XPath, missing assertions, hard-coded waits,\n" +
            "   duplicated setup that should be a reusable, environment-specific data\n" +
            "   leaking into the steps, and ambiguous error messages.\n" +
            "3. Output a markdown table with columns: Step | Issue | Severity | Fix.\n"
        );
    }

    private static String tplRunAndSummarize(JsonNode args) {
        String project = MCPServer.paramOrDefault(args, "project", "<default>");
        String target = MCPServer.paramOrDefault(args, "target", "");
        String browser = MCPServer.paramOrDefault(args, "browser", "Chromium");
        return (
            "Run " +
            target +
            " and report the outcome.\n" +
            "\n" +
            "1. Call `ingenious_run` with target='" +
            target +
            "' and browser='" +
            browser +
            "'.\n" +
            "2. When it completes, call `ingenious_report_latest` and\n" +
            "   `ingenious_report_failures` for the target.\n" +
            "3. Give the user a one-paragraph summary:\n" +
            "     * total / pass / fail / skipped\n" +
            "     * top three reasons for failure (if any) in plain English\n" +
            "     * a link path to the HTML report (Results/.../Latest)\n"
        );
    }

    private static String tplAuthorByArchetype(JsonNode args) {
        String project = MCPServer.paramOrDefault(args, "project", "<default>");
        String archetype = MCPServer.paramOrDefault(args, "archetype", "");
        String scenario = MCPServer.paramOrDefault(args, "scenario", "");
        String testcase = MCPServer.paramOrDefault(args, "testcase", "");
        String desc = MCPServer.paramOrDefault(args, "description", "");
        return (
            "Author a test case from the '" +
            archetype +
            "' archetype.\n" +
            "  Project : " +
            project +
            "\n" +
            "  Scenario: " +
            scenario +
            "\n" +
            "  Test case: " +
            testcase +
            "\n" +
            (desc.isEmpty() ? "" : "\nIntent / values:\n" + desc + "\n") +
            "\nProcedure\n---------\n" +
            "1. Call `ingenious_gen_list` to read the archetype's parameters.\n" +
            "2. Map the intent/values above to those parameters.\n" +
            "3. Call `ingenious_gen_testcase` with archetype='" +
            archetype +
            "', the\n" +
            "   scenario, testcase and a `params` object. Inspect `unresolvedParams` in\n" +
            "   the result \u2013 any listed token still needs a real value or locator.\n" +
            "4. Resolve locators with `ingenious_object_search` (or add them with\n" +
            "   `ingenious_object_add`) and patch steps via `ingenious_testcase_edit_step`.\n" +
            "5. Call `ingenious_testcase_validate`, then `ingenious_run` to confirm.\n"
        );
    }

    private static String tplBuildDataDrivenSuite(JsonNode args) {
        String project = MCPServer.paramOrDefault(args, "project", "<default>");
        String scenario = MCPServer.paramOrDefault(args, "scenario", "");
        String testcase = MCPServer.paramOrDefault(args, "testcase", "");
        String sheet = MCPServer.paramOrDefault(args, "sheet", "");
        return (
            "Turn " +
            project +
            " / " +
            scenario +
            " / " +
            testcase +
            " into a data-driven suite backed by sheet '" +
            sheet +
            "'.\n" +
            "\nProcedure\n---------\n" +
            "1. Call `ingenious_testcase_show` to see which values are hard-coded in the\n" +
            "   step inputs \u2013 those become columns.\n" +
            "2. Create the sheet: `ingenious_data_sheet_create` then\n" +
            "   `ingenious_data_column_add` for each column.\n" +
            "3. Populate it: `ingenious_data_generate` for synthetic rows, or\n" +
            "   `ingenious_data_row_add`/`ingenious_data_set` for explicit values. Bind rows\n" +
            "   to " +
            scenario +
            "/" +
            testcase +
            " with `ingenious_data_row_add`.\n" +
            "4. Replace the hard-coded step inputs with ${column} tokens using\n" +
            "   `ingenious_testcase_edit_step`.\n" +
            "5. Validate and run to confirm the iterations execute.\n"
        );
    }

    private static String tplHardenTest(JsonNode args) {
        String project = MCPServer.paramOrDefault(args, "project", "<default>");
        String scenario = MCPServer.paramOrDefault(args, "scenario", "");
        String testcase = MCPServer.paramOrDefault(args, "testcase", "");
        return (
            "Harden " +
            project +
            " / " +
            scenario +
            " / " +
            testcase +
            ".\n" +
            "\nProcedure\n---------\n" +
            "1. Call `ingenious_testcase_show` and `ingenious_testcase_validate`.\n" +
            "2. For each fragile step, apply a fix with `ingenious_testcase_edit_step`\n" +
            "   or `ingenious_testcase_insert_step`:\n" +
            "     * Replace raw CSS/XPath with a stable locator \u2013 use\n" +
            "       `ingenious_object_search` or `ingenious_browser_inspect`, and register it\n" +
            "       with `ingenious_object_add`.\n" +
            "     * Replace fixed sleeps with an explicit `waitForElementToBeVisible`.\n" +
            "     * Add an assertion after each meaningful action.\n" +
            "3. Re-validate. Report the before/after step count and the changes made.\n"
        );
    }

    private static String tplTriageRun(JsonNode args) {
        String project = MCPServer.paramOrDefault(args, "project", "<default>");
        String target = MCPServer.paramOrDefault(args, "target", "");
        return (
            "Triage " +
            project +
            " / " +
            target +
            ".\n" +
            "\nProcedure\n---------\n" +
            "1. Call `ingenious_report_history` to list recent runs.\n" +
            "2. Call `ingenious_report_compare` on the two newest runs to spot regressions.\n" +
            "3. Call `ingenious_report_failures`, then `ingenious_report_show` on the newest\n" +
            "   run to read the failing executions in detail.\n" +
            "4. For each failure, correlate with the steps via `ingenious_testcase_show`.\n" +
            "5. Propose a concrete fix per failure and, where safe, offer to apply it with\n" +
            "   `ingenious_testcase_edit_step`. Then re-run with `ingenious_run rerun=true`.\n"
        );
    }

    private static String tplRecordBrowserTest(JsonNode args) {
        String project = MCPServer.paramOrDefault(args, "project", "<default>");
        String scenario = MCPServer.paramOrDefault(args, "scenario", "");
        String testcase = MCPServer.paramOrDefault(args, "testcase", "");
        String url = MCPServer.paramOrDefault(args, "url", "");
        return (
            "Author a browser test for " +
            project +
            " / " +
            scenario +
            " / " +
            testcase +
            " by driving a live Playwright Agent CLI session.\n" +
            "\nProcedure\n---------\n" +
            "1. Call `ingenious_doctor` to confirm @playwright/cli is available.\n" +
            "2. Start the session: `ingenious_browser_session_start` name='" +
            testcase +
            "' url='" +
            url +
            "'. Read the returned accessibility snapshot \u2013 elements carry\n" +
            "   refs like e21.\n" +
            "3. Perform the flow one step at a time with `ingenious_browser_session_do`\n" +
            "   (e.g. command='fill e5 \"user@example.com\"', then 'click e21'). Re-read the\n" +
            "   snapshot after each command to pick the next ref.\n" +
            "4. When the flow is complete, call `ingenious_browser_session_save` with the\n" +
            "   scenario and testcase, then `ingenious_browser_session_close`.\n" +
            "5. Call `ingenious_testcase_show` and harden locators (the ref placeholders\n" +
            "   should be replaced with Object-Repository entries).\n"
        );
    }

    private static String tplBootstrapProject(JsonNode args) {
        String name = MCPServer.paramOrDefault(args, "name", "");
        String parentDir = MCPServer.paramOrDefault(args, "parentDir", "");
        return (
            "Bootstrap a new INGenious project named '" +
            name +
            "'" +
            (parentDir.isEmpty() ? "" : " under " + parentDir) +
            ".\n" +
            "\nProcedure\n---------\n" +
            "1. Call `ingenious_project_create` with name='" +
            name +
            "'" +
            (parentDir.isEmpty() ? "" : " and parentDir='" + parentDir + "'") +
            ".\n" +
            "2. Call `ingenious_gen_list` to see the archetypes.\n" +
            "3. Seed one sample per capability with `ingenious_gen_testcase`:\n" +
            "     * a Browser sample (e.g. archetype='browser-flow', scenario='Web')\n" +
            "     * an API sample (e.g. archetype='api-get', scenario='API')\n" +
            "   Provide sensible placeholder params (a public demo URL works).\n" +
            "4. Call `ingenious_run_dry` on each to confirm they resolve.\n" +
            "5. Summarise what was created and suggest the next step to the user.\n"
        );
    }

    private static String tplMakeDataDriven(JsonNode args) {
        String project = MCPServer.paramOrDefault(args, "project", "<default>");
        String scenario = MCPServer.paramOrDefault(args, "scenario", "");
        String testcase = MCPServer.paramOrDefault(args, "testcase", "");
        return (
            "Make " +
            project +
            " / " +
            scenario +
            " / " +
            testcase +
            " data-driven.\n" +
            "\nProcedure\n---------\n" +
            "1. Call `ingenious_testcase_parameterize` with mode='scan' to list every\n" +
            "   hard-coded value: whole @literal inputs and individual JSON/XML payload\n" +
            "   fields, each with a suggested sheet/column name.\n" +
            "2. Show the candidate list to the user and ask which values to externalise\n" +
            "   (payload candidates support per-field selection via their paths). If the\n" +
            "   user says 'everything', use mode='all'.\n" +
            "3. Apply with mode='selected' plus a selections array (or mode='all').\n" +
            "   Values move into a data-sheet row keyed to this test case; whole inputs\n" +
            "   become Sheet:Column and payload fields become {Sheet:Column} tokens.\n" +
            "4. To add more test iterations, append rows with `ingenious_data_row_add`\n" +
            "   using the same columns.\n" +
            "5. Finish with `ingenious_testcase_validate` and summarise the changes.\n"
        );
    }

    private static String tplExtractReusable(JsonNode args) {
        String project = MCPServer.paramOrDefault(args, "project", "<default>");
        String scenario = MCPServer.paramOrDefault(args, "scenario", "");
        return (
            "Extract reusable components in " +
            project +
            (scenario.isEmpty() ? "" : " (scenario " + scenario + ")") +
            ".\n" +
            "\nProcedure\n---------\n" +
            "1. Call `ingenious_testcase_validate` on the project; W10 warnings list test\n" +
            "   cases sharing identical step sequences, W9 flags long monolithic tests.\n" +
            "2. For each repeated sequence, read the involved test cases with\n" +
            "   `ingenious_testcase_show` and identify the user intent it represents\n" +
            "   (e.g. 'Launch the App', 'Fill Income').\n" +
            "3. Create the reusable with `ingenious_testcase_create` using reusable=true,\n" +
            "   an intent-group scenario (e.g. 'Common' or 'Flow' \u2013 the name must differ\n" +
            "   from every TestPlan scenario) and the shared steps.\n" +
            "4. Replace the duplicated steps in each test case with a single Execute step:\n" +
            "   object='Execute', action='<ReusableScenario>:<ReusableName>',\n" +
            "   reference='[Project]' (use `ingenious_testcase_edit_step` /\n" +
            "   `ingenious_testcase_remove_step`).\n" +
            "5. Re-run `ingenious_testcase_validate` to confirm everything resolves, then\n" +
            "   summarise the extracted intents.\n"
        );
    }
}

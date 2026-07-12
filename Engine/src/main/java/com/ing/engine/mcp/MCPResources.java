package com.ing.engine.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Read-only MCP resources – browsable context that helps LLMs answer
 * questions about the workspace without needing to call tools first.
 *
 * <p>Resources use {@code ingenious://} URIs. Tools remain the way to
 * perform actions; resources are for passive context.
 */
final class MCPResources {
    private final String defaultProject;

    MCPResources(String defaultProject) {
        this.defaultProject = defaultProject;
    }

    JsonNode list(ObjectMapper json) {
        ObjectNode out = json.createObjectNode();
        ArrayNode arr = out.putArray("resources");

        addResource(
            arr,
            "ingenious://catalog/actions",
            "Action catalog",
            "All available test actions with categories and descriptions.",
            "application/json"
        );

        addResource(
            arr,
            "ingenious://docs/getting-started",
            "Getting started",
            "How to drive INGenious via MCP – essential workflow primer.",
            "text/markdown"
        );

        addResource(
            arr,
            "ingenious://docs/step-schema",
            "Test step schema",
            "Field layout for a single INGenious test step.",
            "text/markdown"
        );

        addResource(
            arr,
            "ingenious://catalog/archetypes",
            "Test-case archetypes",
            "Templates (browser/API/hybrid) used by ingenious_gen_testcase, with parameters.",
            "application/json"
        );

        addResource(
            arr,
            "ingenious://docs/best-practices",
            "Authoring best practices",
            "Locator, wait, assertion and data-driven conventions for durable tests.",
            "text/markdown"
        );

        addResource(
            arr,
            "ingenious://docs/conventions",
            "Authoring conventions (authoritative)",
            "The full INGenious convention reference: input grammar, naming model, " +
            "parameterization workflow and lint rules enforced by the tools.",
            "text/markdown"
        );

        if (defaultProject != null) {
            addResource(
                arr,
                "ingenious://project/" + safeUri(defaultProject) + "/summary",
                "Project summary",
                "Scenario and test case overview for the default project.",
                "application/json"
            );
        }
        return out;
    }

    JsonNode read(ObjectMapper json, JsonNode params) {
        String uri = MCPServer.requiredParam(params, "uri");

        if ("ingenious://catalog/actions".equals(uri)) {
            return wrap(json, uri, "application/json", actionsJson(json));
        }
        if ("ingenious://docs/getting-started".equals(uri)) {
            return wrap(json, uri, "text/markdown", gettingStarted());
        }
        if ("ingenious://docs/step-schema".equals(uri)) {
            return wrap(json, uri, "text/markdown", stepSchema());
        }
        if ("ingenious://catalog/archetypes".equals(uri)) {
            return wrap(json, uri, "application/json", archetypesJson(json));
        }
        if ("ingenious://docs/best-practices".equals(uri)) {
            return wrap(json, uri, "text/markdown", bestPractices());
        }
        if ("ingenious://docs/conventions".equals(uri)) {
            return wrap(json, uri, "text/markdown", ConventionCatalog.conventionsDoc());
        }
        if (uri.startsWith("ingenious://project/") && uri.endsWith("/summary")) {
            String proj = uri.substring(
                "ingenious://project/".length(),
                uri.length() - "/summary".length()
            );
            return wrap(json, uri, "application/json", projectSummaryJson(json, proj).toString());
        }
        throw new MCPServer.MCPException(-32602, "Unknown resource URI: " + uri);
    }

    // ------------------------------------------------------------------

    private void addResource(
        ArrayNode arr,
        String uri,
        String name,
        String description,
        String mime
    ) {
        ObjectNode n = arr.addObject();
        n.put("uri", uri);
        n.put("name", name);
        n.put("description", description);
        n.put("mimeType", mime);
    }

    private ObjectNode wrap(ObjectMapper json, String uri, String mime, String text) {
        ObjectNode out = json.createObjectNode();
        ArrayNode contents = out.putArray("contents");
        ObjectNode item = contents.addObject();
        item.put("uri", uri);
        item.put("mimeType", mime);
        item.put("text", text);
        return out;
    }

    private String actionsJson(ObjectMapper json) {
        ArrayNode arr = json.createArrayNode();
        for (ActionCatalog.ActionInfo a : ActionCatalog.all()) {
            ObjectNode n = arr.addObject();
            n.put("name", a.name);
            n.put("category", a.category);
            n.put("objectType", a.objectType);
            n.put("description", a.description);
        }
        try {
            return json.writerWithDefaultPrettyPrinter().writeValueAsString(arr);
        } catch (Exception e) {
            return arr.toString();
        }
    }

    private String archetypesJson(ObjectMapper json) {
        ArrayNode arr = json.createArrayNode();
        for (ArchetypeCatalog.Archetype a : ArchetypeCatalog.all()) {
            ObjectNode n = arr.addObject();
            n.put("name", a.name);
            n.put("category", a.category);
            n.put("description", a.description);
            ArrayNode params = n.putArray("parameters");
            for (String p : a.parameters) params.add(p);
            ArrayNode steps = n.putArray("steps");
            for (ArchetypeCatalog.Step s : a.steps) {
                ObjectNode sn = steps.addObject();
                sn.put("action", s.action);
                sn.put("object", s.object);
                sn.put("input", s.input);
            }
        }
        try {
            return json.writerWithDefaultPrettyPrinter().writeValueAsString(arr);
        } catch (Exception e) {
            return arr.toString();
        }
    }

    private ObjectNode projectSummaryJson(ObjectMapper json, String projectName) {
        File dir = resolveProject(projectName);
        ObjectNode out = json.createObjectNode();
        out.put("name", dir.getName());
        out.put("path", dir.getAbsolutePath());

        File testPlan = new File(dir, "TestPlan");
        ArrayNode scenarios = out.putArray("scenarios");
        if (testPlan.isDirectory()) {
            File[] sdirs = testPlan.listFiles(File::isDirectory);
            if (sdirs != null) {
                Arrays.sort(sdirs, Comparator.comparing(File::getName));
                for (File s : sdirs) {
                    ObjectNode n = scenarios.addObject();
                    n.put("name", s.getName());
                    File[] csvs = s.listFiles(f -> f.isFile() && f.getName().endsWith(".csv"));
                    n.put("testCases", csvs == null ? 0 : csvs.length);
                }
            }
        }

        File testLab = new File(dir, "TestLab");
        ArrayNode releases = out.putArray("releases");
        if (testLab.isDirectory()) {
            File[] rdirs = testLab.listFiles(File::isDirectory);
            if (rdirs != null) {
                Arrays.sort(rdirs, Comparator.comparing(File::getName));
                for (File r : rdirs) {
                    ObjectNode n = releases.addObject();
                    n.put("name", r.getName());
                    File[] csvs = r.listFiles(f -> f.isFile() && f.getName().endsWith(".csv"));
                    n.put("testSets", csvs == null ? 0 : csvs.length);
                }
            }
        }
        return out;
    }

    private File resolveProject(String name) {
        File abs = new File(name);
        if (abs.isAbsolute() && abs.isDirectory()) return abs;
        String cwd = System.getProperty("user.dir");
        File rel = new File(cwd, name);
        if (rel.isDirectory()) return rel;
        File under = new File(cwd, "Projects" + File.separator + name);
        if (under.isDirectory()) return under;
        throw new MCPServer.MCPException(-32602, "Project not found: " + name);
    }

    private String safeUri(String name) {
        return name.replace(" ", "%20");
    }

    // ------------------------------------------------------------------
    // static docs
    // ------------------------------------------------------------------

    private static String gettingStarted() {
        return (
            "# Driving INGenious from MCP\n\n" +
            "INGenious is a test automation framework. Through this MCP server you can\n" +
            "discover, create, run and debug automated tests using natural language.\n\n" +
            "## Typical flow\n\n" +
            "1. **Discover projects** – `ingenious_project_list`.\n" +
            "2. **Inspect a project** – `ingenious_project_info`, `ingenious_scenario_list`.\n" +
            "3. **Discover actions** – `ingenious_action_list` (and `ingenious_action_search`).\n" +
            "   *Always* use these before composing steps: never invent action names.\n" +
            "4. **Create a test case** – `ingenious_testcase_create` with `steps`:\n" +
            "   ```json\n" +
            "   { \"action\": \"Open\",  \"object\": \"\",       \"input\": \"@Browser\" }\n" +
            "   { \"action\": \"GoTo\",  \"object\": \"\",       \"input\": \"@https://example.com\" }\n" +
            "   { \"action\": \"Click\", \"object\": \"page.btn\" }\n" +
            "   ```\n" +
            "5. **Parameterize** – `ingenious_testcase_parameterize` (mode=scan, then apply)\n" +
            "   to move hard-coded values into data sheets.\n" +
            "6. **Run** – `ingenious_run` with `target=\"Project/Scenario/TestCase\"`.\n" +
            "7. **Triage** – on failure, `ingenious_report_failures` then\n" +
            "   `ingenious_testcase_show` to correlate steps with errors.\n\n" +
            "## Path conventions\n\n" +
            "* Test cases live at `TestPlan/<Scenario>/<TestCase>.csv`.\n" +
            "* Test sets   live at `TestLab/<Release>/<TestSet>.csv`.\n" +
            "* Reports     live at `Results/{TestDesign|TestExecution}/.../Latest/`.\n\n" +
            "## Prompts available\n\n" +
            "* `create_test_case`     – English description → working test case\n" +
            "* `convert_manual_steps` – manual steps → automated steps\n" +
            "* `explain_failure`      – diagnose the latest failure\n" +
            "* `debug_test`           – step-by-step review with risk callouts\n" +
            "* `suggest_locator`      – propose a stable locator\n" +
            "* `review_test_case`     – best-practice review\n" +
            "* `run_and_summarize`    – execute and report\n"
        );
    }

    private static String stepSchema() {
        return (
            "# INGenious test step schema\n\n" +
            "Each step in a test case has these fields:\n\n" +
            "| Field        | Required | Notes |\n" +
            "|--------------|----------|-------|\n" +
            "| `action`     | yes      | Must be a real action name (list them with `ingenious_action_list`) or, for Execute steps, `<ReusableScenario>:<ReusableName>`. |\n" +
            "| `object`     | varies   | Object reference (`Page.element`), `Webservice` for API steps, or `Execute` for reusable calls. Never `@`-prefixed. |\n" +
            "| `input`      | varies   | `@literal` for hard-coded values, `Sheet:Column` for data-driven values, raw payload body (with optional `{Sheet:Column}` tokens) for request actions. |\n" +
            "| `condition`  | optional | Additional argument required by some actions. |\n" +
            "| `description`| optional | Human-readable description shown in reports. |\n" +
            "| `reference`  | optional | OR page reference `[Project] <PageName>`, or `[Project]` on Execute steps. |\n\n" +
            "## Common idioms\n\n" +
            "* **Open browser**: `action=\"Open\", input=\"@Browser\"`.\n" +
            "* **Navigate**: `action=\"GoTo\", input=\"@https://...\"`.\n" +
            "* **Click an OR object**: `action=\"Click\", object=\"LoginPage.submitBtn\"`.\n" +
            "* **Set text from data**: `action=\"Fill\", object=\"LoginPage.user\", input=\"LoginData:Username\"`.\n" +
            "* **Assertion**: `action=\"assertElementContainsText\", object=\"Home.greeting\", input=\"@Welcome\"`.\n" +
            "* **Call a reusable**: `object=\"Execute\", action=\"Common:Launch the App\", reference=\"[Project]\"`.\n\n" +
            "Full grammar: read `ingenious://docs/conventions`.\n"
        );
    }

    private static String bestPractices() {
        return (
            "# INGenious authoring best practices\n\n" +
            "## Locators\n" +
            "1. Prefer semantic Object-Repository entries (`LoginPage.userField`) over raw\n" +
            "   selectors embedded in steps.\n" +
            "2. When you must use a selector, prefer role/label/test-id over brittle CSS/XPath.\n" +
            "3. Discover existing locators with `ingenious_object_search`; add new ones with\n" +
            "   `ingenious_object_add` or scrape a page via `ingenious_object_import_page`.\n\n" +
            "## Waits\n" +
            "* Never use fixed sleeps. Insert `waitForElementToBeVisible` (or a related\n" +
            "  `waitFor*` action) before interacting with or asserting on an element.\n\n" +
            "## Assertions\n" +
            "* Every meaningful action should be followed by an assertion so drift fails\n" +
            "  loudly \u2013 e.g. `assertElementIsVisible`, `assertElementContainsText`,\n" +
            "  `assertResponseCode`, `assertJSONelementEquals`.\n\n" +
            "## Data-driven tests\n" +
            "* Author steps with `@literal` values first, then externalise them with\n" +
            "  `ingenious_testcase_parameterize`: whole inputs become `Sheet:Column`\n" +
            "  references and API payload fields become embedded `{Sheet:Column}` tokens,\n" +
            "  backed by a data-sheet row keyed to the test case.\n" +
            "* Manage sheets directly with `ingenious_data_sheet_create`,\n" +
            "  `ingenious_data_column_add`, `ingenious_data_generate` and `ingenious_data_row_add`.\n\n" +
            "## Reusables\n" +
            "* Extract repeated flows into reusable components (user intents such as\n" +
            "  'Launch the App' or 'Fill Income') and compose test cases from them with\n" +
            "  `object=Execute, action=<ReusableScenario>:<ReusableName>`.\n\n" +
            "## Authoring workflow\n" +
            "`discover \u2192 compose (or ingenious_gen_testcase) \u2192 ingenious_testcase_parameterize \u2192\n" +
            "ingenious_testcase_validate \u2192 ingenious_run \u2192 triage (ingenious_report_failures / _compare)`.\n\n" +
            "## Archetypes\n" +
            "Start from a template with `ingenious_gen_list` + `ingenious_gen_testcase` rather\n" +
            "than a blank step list; then resolve any `unresolvedParams` it reports.\n\n" +
            "Authoritative rules: read `ingenious://docs/conventions`.\n"
        );
    }
}

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

        addResource(arr, "ingenious://catalog/actions",
                "Action catalog",
                "All available test actions with categories and descriptions.",
                "application/json");

        addResource(arr, "ingenious://docs/getting-started",
                "Getting started",
                "How to drive INGenious via MCP – essential workflow primer.",
                "text/markdown");

        addResource(arr, "ingenious://docs/step-schema",
                "Test step schema",
                "Field layout for a single INGenious test step.",
                "text/markdown");

        if (defaultProject != null) {
            addResource(arr, "ingenious://project/" + safeUri(defaultProject) + "/summary",
                    "Project summary",
                    "Scenario and test case overview for the default project.",
                    "application/json");
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
        if (uri.startsWith("ingenious://project/") && uri.endsWith("/summary")) {
            String proj = uri.substring("ingenious://project/".length(),
                    uri.length() - "/summary".length());
            return wrap(json, uri, "application/json", projectSummaryJson(json, proj).toString());
        }
        throw new MCPServer.MCPException(-32602, "Unknown resource URI: " + uri);
    }

    // ------------------------------------------------------------------

    private void addResource(ArrayNode arr, String uri, String name,
                             String description, String mime) {
        ObjectNode n = arr.addObject();
        n.put("uri",         uri);
        n.put("name",        name);
        n.put("description", description);
        n.put("mimeType",    mime);
    }

    private ObjectNode wrap(ObjectMapper json, String uri, String mime, String text) {
        ObjectNode out = json.createObjectNode();
        ArrayNode contents = out.putArray("contents");
        ObjectNode item = contents.addObject();
        item.put("uri",      uri);
        item.put("mimeType", mime);
        item.put("text",     text);
        return out;
    }

    private String actionsJson(ObjectMapper json) {
        ArrayNode arr = json.createArrayNode();
        for (ActionCatalog.ActionInfo a : ActionCatalog.all()) {
            ObjectNode n = arr.addObject();
            n.put("name",        a.name);
            n.put("category",    a.category);
            n.put("objectType",  a.objectType);
            n.put("description", a.description);
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
        return "# Driving INGenious from MCP\n\n"
             + "INGenious is a test automation framework. Through this MCP server you can\n"
             + "discover, create, run and debug automated tests using natural language.\n\n"
             + "## Typical flow\n\n"
             + "1. **Discover projects** – `ingenious_project_list`.\n"
             + "2. **Inspect a project** – `ingenious_project_info`, `ingenious_scenario_list`.\n"
             + "3. **Discover actions** – `ingenious_action_list` (and `ingenious_action_search`).\n"
             + "   *Always* use these before composing steps: never invent action names.\n"
             + "4. **Create a test case** – `ingenious_testcase_create` with `steps`:\n"
             + "   ```json\n"
             + "   { \"action\": \"Open\",  \"object\": \"\",       \"input\": \"@Browser\" }\n"
             + "   { \"action\": \"GoTo\",  \"object\": \"\",       \"input\": \"https://example.com\" }\n"
             + "   { \"action\": \"Click\", \"object\": \"page.btn\" }\n"
             + "   ```\n"
             + "5. **Run** – `ingenious_run` with `target=\"Project/Scenario/TestCase\"`.\n"
             + "6. **Triage** – on failure, `ingenious_report_failures` then\n"
             + "   `ingenious_testcase_show` to correlate steps with errors.\n\n"
             + "## Path conventions\n\n"
             + "* Test cases live at `TestPlan/<Scenario>/<TestCase>.csv`.\n"
             + "* Test sets   live at `TestLab/<Release>/<TestSet>.csv`.\n"
             + "* Reports     live at `Results/{TestDesign|TestExecution}/.../Latest/`.\n\n"
             + "## Prompts available\n\n"
             + "* `create_test_case`     – English description → working test case\n"
             + "* `convert_manual_steps` – manual steps → automated steps\n"
             + "* `explain_failure`      – diagnose the latest failure\n"
             + "* `debug_test`           – step-by-step review with risk callouts\n"
             + "* `suggest_locator`      – propose a stable locator\n"
             + "* `review_test_case`     – best-practice review\n"
             + "* `run_and_summarize`    – execute and report\n";
    }

    private static String stepSchema() {
        return "# INGenious test step schema\n\n"
             + "Each step in a test case has these fields:\n\n"
             + "| Field        | Required | Notes |\n"
             + "|--------------|----------|-------|\n"
             + "| `action`     | yes      | Must be a real action name. List them with `ingenious_action_list`. |\n"
             + "| `object`     | varies   | Object reference (`Page.element`) or runtime ref like `@Browser`. |\n"
             + "| `input`      | varies   | Literal value, `@variable` reference, or `@Data.column` from test data. |\n"
             + "| `condition`  | optional | Additional argument required by some actions. |\n"
             + "| `description`| optional | Human-readable description shown in reports. |\n"
             + "| `reference`  | optional | Cross-reference to another test case or reusable. |\n\n"
             + "## Common idioms\n\n"
             + "* **Open browser**: `action=\"Open\", input=\"@Browser\"`.\n"
             + "* **Navigate**: `action=\"GoTo\", input=\"https://...\"`.\n"
             + "* **Click an OR object**: `action=\"Click\", object=\"LoginPage.submitBtn\"`.\n"
             + "* **Set text from data**: `action=\"Set\", object=\"LoginPage.user\", input=\"@Data.username\"`.\n"
             + "* **Assertion**: `action=\"VerifyText\", object=\"Home.greeting\", input=\"Welcome\"`.\n";
    }
}

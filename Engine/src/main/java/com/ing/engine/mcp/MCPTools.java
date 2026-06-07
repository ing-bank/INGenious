package com.ing.engine.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ing.datalib.component.Project;
import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.component.TestStep;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Tool registry and dispatcher for the {@link MCPServer}.
 *
 * <p>Each tool is named with the {@code ingenious_<area>_<verb>} convention
 * so AI agents can pattern-match on them, and each is backed by a real
 * Datalib / filesystem / subprocess operation – nothing returned by these
 * tools is mocked.
 */
final class MCPTools {

    private final String defaultProject;
    /** Background runs keyed by their assigned run id. */
    private final Map<String, RunHandle> runs = new ConcurrentHashMap<>();

    MCPTools(String defaultProject) {
        this.defaultProject = defaultProject;
    }

    // ==================================================================
    // tool descriptors – tools/list
    // ==================================================================

    JsonNode list(ObjectMapper json) {
        ObjectNode result = json.createObjectNode();
        ArrayNode arr = result.putArray("tools");

        addTool(arr, "ingenious_project_list",
                "List all INGenious test automation projects under a base directory "
                + "(defaults to ./Projects).",
                schema(json)
                        .optional("basePath", "string",
                                "Directory to scan. Defaults to ./Projects in the server's CWD.")
                        .build());

        addTool(arr, "ingenious_project_info",
                "Get summary information (scenario count, test case count, location) for one project.",
                schema(json)
                        .required("project", "string", "Project name or absolute path.")
                        .build());

        addTool(arr, "ingenious_scenario_list",
                "List all scenarios (TestPlan folders) in a project, with their test case counts.",
                schema(json)
                        .optional("project", "string", "Project name or absolute path.")
                        .build());

        addTool(arr, "ingenious_scenario_create",
                "Create a new scenario folder. Pass reusable=true to create under ReusableComponents/ instead of TestPlan/.",
                schema(json)
                        .optional("project", "string", "Project name or absolute path.")
                        .required("scenario", "string", "Scenario name to create.")
                        .optional("reusable", "boolean", "Create under ReusableComponents/ (default false).")
                        .build());

        addTool(arr, "ingenious_testcase_list",
                "List test cases in a project, optionally filtered by scenario.",
                schema(json)
                        .optional("project",  "string", "Project name or absolute path.")
                        .optional("scenario", "string", "Filter to a single scenario.")
                        .build());

        addTool(arr, "ingenious_testcase_show",
                "Show the full step-by-step contents of a test case.",
                schema(json)
                        .optional("project",  "string", "Project name or absolute path.")
                        .required("scenario", "string", "Scenario name.")
                        .required("testcase", "string", "Test case name (no extension).")
                        .build());

        addTool(arr, "ingenious_testcase_create",
                "Create a new test case (default format: YAML), or one pre-populated with steps. "
                + "Steps may be supplied as an array of {action, object, input, condition, description}. "
                + "Pass reusable=true to create under ReusableComponents/<scenario>/ instead of TestPlan/.",
                schema(json)
                        .optional("project",  "string", "Project name or absolute path.")
                        .required("scenario", "string", "Scenario name (created if missing).")
                        .required("testcase", "string", "New test case name.")
                        .optional("format",   "string", "YAML (default) or CSV.")
                        .optional("reusable", "boolean", "Create as a reusable component (default false).")
                        .optionalArray("steps",
                                "Optional list of step objects to insert immediately.",
                                stepItemSchema(json))
                        .build());

        addTool(arr, "ingenious_testcase_add_step",
                "Append a single step to an existing test case.",
                schema(json)
                        .optional("project",     "string", "Project name or absolute path.")
                        .required("scenario",    "string", "Scenario name.")
                        .required("testcase",    "string", "Test case name.")
                        .required("action",      "string", "Action name (see ingenious_action_list).")
                        .optional("object",      "string", "Object reference (page.element or @Browser etc.).")
                        .optional("input",       "string", "Input value or @variable.")
                        .optional("condition",   "string", "Optional condition.")
                        .optional("description", "string", "Optional human-readable description.")
                        .build());

        addTool(arr, "ingenious_testcase_delete",
                "Delete a test case CSV file.",
                schema(json)
                        .optional("project",  "string", "Project name or absolute path.")
                        .required("scenario", "string", "Scenario name.")
                        .required("testcase", "string", "Test case name.")
                        .build());

        addTool(arr, "ingenious_testset_list",
                "List test sets (TestLab/<release>/<set>.csv) in a project, "
                + "optionally filtered by release.",
                schema(json)
                        .optional("project", "string", "Project name or absolute path.")
                        .optional("release", "string", "Filter to one release.")
                        .build());

        addTool(arr, "ingenious_testset_show",
                "Read the contents of a test set CSV (list of scenario/testcase rows).",
                schema(json)
                        .optional("project", "string", "Project name or absolute path.")
                        .required("release", "string", "Release name.")
                        .required("testset", "string", "Test set name (without .csv).")
                        .build());

        addTool(arr, "ingenious_action_list",
                "List available test actions (Browser, API, Database, Mobile, Kafka, General). "
                + "Use this before creating steps so the AI picks actions that actually exist.",
                schema(json)
                        .optional("category", "string",
                                "Filter to one of: Browser, API, Database, Mobile, Kafka, General.")
                        .optional("limit",    "integer", "Cap the number of results returned.")
                        .build());

        addTool(arr, "ingenious_action_search",
                "Free-text search across action names, descriptions and object types.",
                schema(json)
                        .required("query", "string", "Search term.")
                        .build());

        addTool(arr, "ingenious_action_info",
                "Get detailed metadata for a single action.",
                schema(json)
                        .required("action", "string", "Action name (e.g. 'Click', 'GET', 'Set').")
                        .build());

        addTool(arr, "ingenious_action_categories",
                "Counts of available actions per category.",
                schema(json).build());

        addTool(arr, "ingenious_run",
                "Execute a test case OR a test set synchronously and return the captured output. "
                + "The target may be '<Project>/<Scenario>/<TestCase>' or '<Project>/<Release>/<TestSet>' "
                + "(matches the `ingenious run` CLI auto-detection).",
                schema(json)
                        .required("target",   "string", "<Project>/<Scenario>/<TestCase> or <Project>/<Release>/<TestSet>.")
                        .optional("browser",  "string", "Chromium | Firefox | WebKit | 'No Browser' (aliases: NoBrowser, no-browser). Default Chromium.")
                        .optional("headless", "boolean", "Run headless. Default false.")
                        .optional("parallel", "integer", "Thread count for test sets. Default 1.")
                        .optional("tags",     "string", "Comma-separated tag filter (test sets only).")
                        .optional("timeoutSeconds", "integer",
                                "Wall-clock timeout for the whole run. Default 1800.")
                        .build());

        addTool(arr, "ingenious_run_async",
                "Start a test run in the background and return a runId. "
                + "Poll with ingenious_run_status / fetch output with ingenious_run_logs.",
                schema(json)
                        .required("target",   "string", "<Project>/<Scenario>/<TestCase> or <Project>/<Release>/<TestSet>.")
                        .optional("browser",  "string", "Chromium | Firefox | WebKit | 'No Browser' (aliases: NoBrowser, no-browser).")
                        .optional("headless", "boolean", "Run headless.")
                        .optional("parallel", "integer", "Thread count for test sets.")
                        .optional("tags",     "string", "Comma-separated tag filter.")
                        .build());

        addTool(arr, "ingenious_run_status",
                "Status of a previously started async run (or list all when no runId is given).",
                schema(json)
                        .optional("runId", "string", "Run id returned by ingenious_run_async.")
                        .build());

        addTool(arr, "ingenious_run_logs",
                "Captured stdout/stderr of an async run (last N lines).",
                schema(json)
                        .required("runId", "string", "Run id returned by ingenious_run_async.")
                        .optional("tail",  "integer", "Number of trailing lines. Default 200.")
                        .build());

        addTool(arr, "ingenious_run_cancel",
                "Cancel an in-flight async run.",
                schema(json)
                        .required("runId", "string", "Run id to cancel.")
                        .build());

        addTool(arr, "ingenious_report_latest",
                "Get the latest run report summary for a target.",
                schema(json)
                        .optional("project", "string", "Project name or absolute path.")
                        .required("target",
                                "string",
                                "'<Scenario>/<TestCase>' (TestDesign) or '<Release>/<TestSet>' (TestExecution).")
                        .build());

        addTool(arr, "ingenious_report_history",
                "List the last N timestamped runs for a target.",
                schema(json)
                        .optional("project", "string", "Project name or absolute path.")
                        .required("target",  "string",
                                "'<Scenario>/<TestCase>' or '<Release>/<TestSet>'.")
                        .optional("limit",   "integer", "Max entries. Default 10.")
                        .build());

        addTool(arr, "ingenious_report_failures",
                "List failed test cases from the latest run of a target (parses Latest/data.js).",
                schema(json)
                        .optional("project", "string", "Project name or absolute path.")
                        .required("target",  "string",
                                "'<Scenario>/<TestCase>' or '<Release>/<TestSet>'.")
                        .build());

        addTool(arr, "ingenious_config_get",
                "Read a project Configuration property (or all of them).",
                schema(json)
                        .optional("project", "string", "Project name or absolute path.")
                        .optional("key",     "string", "Property key; omit to dump all.")
                        .optional("file",    "string", "Filename under Configuration/. "
                                + "Defaults to 'Global Settings.properties'.")
                        .build());

        addTool(arr, "ingenious_config_set",
                "Update a project Configuration property.",
                schema(json)
                        .optional("project", "string", "Project name or absolute path.")
                        .required("key",     "string", "Property key.")
                        .required("value",   "string", "New value.")
                        .optional("file",    "string", "Filename under Configuration/. "
                                + "Defaults to 'Global Settings.properties'.")
                        .build());

        // -----------------------------------------------------------
        // project create
        // -----------------------------------------------------------
        addTool(arr, "ingenious_project_create",
                "Create a new INGenious project with the full folder layout "
                + "(TestPlan/, ReusableComponents/, ObjectRepository/, TestData/, "
                + "TestLab/, Settings/, Configuration/, Results/). Defaults the "
                + "test case format to YAML.",
                schema(json)
                        .required("name", "string", "Project name (folder will be created under parent directory).")
                        .optional("parentDir", "string", "Parent directory (default: current working dir).")
                        .optional("format", "string", "Default test case format: YAML (default) or CSV.")
                        .optional("noSample", "boolean", "Skip creating the default sample scenario/test case.")
                        .build());

        // -----------------------------------------------------------
        // data sheet / row / column / env
        // -----------------------------------------------------------
        addTool(arr, "ingenious_data_sheet_create",
                "Create a new test data sheet in one or all environments.",
                schema(json)
                        .optional("project", "string", "Project name or absolute path.")
                        .required("sheet", "string", "New sheet name.")
                        .optional("env", "string", "Target environment ('all' or environment name; default: all).")
                        .build());

        addTool(arr, "ingenious_data_row_add",
                "Add a row binding a scenario/test case (or reusable scenario/component) to a data sheet. "
                + "Extra column values may be supplied via the columns object.",
                schema(json)
                        .optional("project", "string", "Project name or absolute path.")
                        .required("sheet", "string", "Sheet name (created if missing).")
                        .required("scenario", "string", "Scenario name (or reusable-scenario name when reusable=true).")
                        .required("testcase", "string", "Test case name (or reusable-component name when reusable=true).")
                        .optional("reusable", "boolean", "Treat scenario/testcase as a reusable component reference.")
                        .optional("iteration", "string", "Iteration number (default 1).")
                        .optional("subIteration", "string", "Sub-iteration number (default 1).")
                        .optional("env", "string", "Target environment ('all' or environment name; default: all).")
                        .optional("columns", "object", "Map of column name to value (extra columns are added on-demand).")
                        .build());

        addTool(arr, "ingenious_data_column_add",
                "Add a column to a data sheet in one or all environments.",
                schema(json)
                        .optional("project", "string", "Project name or absolute path.")
                        .required("sheet", "string", "Sheet name (created if missing).")
                        .required("column", "string", "New column name.")
                        .optional("env", "string", "Target environment ('all' or environment name; default: all).")
                        .build());

        addTool(arr, "ingenious_env_list",
                "List all configured test data environments.",
                schema(json)
                        .optional("project", "string", "Project name or absolute path.")
                        .build());

        addTool(arr, "ingenious_env_create",
                "Create a new environment, optionally cloning sheets from an existing one.",
                schema(json)
                        .optional("project", "string", "Project name or absolute path.")
                        .required("env", "string", "New environment name.")
                        .optional("from", "string", "Source environment to clone sheets from.")
                        .optional("withGlobal", "boolean", "Also clone global data (default false).")
                        .build());

        addTool(arr, "ingenious_env_delete",
                "Delete a test data environment.",
                schema(json)
                        .optional("project", "string", "Project name or absolute path.")
                        .required("env", "string", "Environment name to delete.")
                        .build());

        // -----------------------------------------------------------
        // importers
        // -----------------------------------------------------------
        addTool(arr, "ingenious_import_curl",
                "Import a single curl command as an API test case (Webservice steps).",
                schema(json)
                        .optional("project", "string", "Project name or absolute path.")
                        .required("curl", "string", "The curl command string.")
                        .optional("scenario", "string", "Scenario name (default: Imported).")
                        .optional("testcase", "string", "Test case name (default: derived from URL).")
                        .optional("reusable", "boolean", "Create as a reusable component (default false).")
                        .build());

        addTool(arr, "ingenious_import_postman",
                "Import a Postman collection (.json) as test cases or reusable components.",
                schema(json)
                        .optional("project", "string", "Project name or absolute path.")
                        .required("file", "string", "Path to the Postman collection JSON file.")
                        .optional("scenario", "string", "Target scenario name (default: Postman).")
                        .optional("reusable", "boolean", "Import as reusable components.")
                        .optional("conflict", "string", "Conflict policy: skip | overwrite | rename (default: rename).")
                        .build());

        addTool(arr, "ingenious_import_bruno",
                "Import a Bruno collection (file or directory) as test cases or reusable components.",
                schema(json)
                        .optional("project", "string", "Project name or absolute path.")
                        .required("file", "string", "Path to the Bruno collection file or root directory.")
                        .optional("scenario", "string", "Target scenario name (default: Bruno).")
                        .optional("reusable", "boolean", "Import as reusable components.")
                        .optional("conflict", "string", "Conflict policy: skip | overwrite | rename (default: rename).")
                        .build());

        addTool(arr, "ingenious_import_playwright",
                "Import a Playwright recording (Java source from codegen) as a test case. Uses the same parser as the IDE's Tools \u2192 Import Playwright Recording.",
                schema(json)
                        .optional("project", "string", "Project name or absolute path.")
                        .required("file", "string", "Path to the recording file (.txt or .java).")
                        .optional("scenario", "string", "Target scenario name (default: derived from file name).")
                        .optional("testcase", "string", "Test case name (default: derived from file name).")
                        .build());

        return result;
    }

    private void addTool(ArrayNode arr, String name, String description, ObjectNode inputSchema) {
        ObjectNode tool = arr.addObject();
        tool.put("name", name);
        tool.put("description", description);
        tool.set("inputSchema", inputSchema);
    }

    // ==================================================================
    // tools/call dispatch
    // ==================================================================

    JsonNode call(ObjectMapper json, JsonNode params) {
        String name = MCPServer.requiredParam(params, "name");
        JsonNode args = params.path("arguments");

        switch (name) {
            case "ingenious_project_list":    return MCPServer.jsonContent(json, projectList(json, args));
            case "ingenious_project_info":    return MCPServer.jsonContent(json, projectInfo(json, args));
            case "ingenious_scenario_list":   return MCPServer.jsonContent(json, scenarioList(json, args));
            case "ingenious_scenario_create": return MCPServer.jsonContent(json, scenarioCreate(json, args));
            case "ingenious_testcase_list":   return MCPServer.jsonContent(json, testCaseList(json, args));
            case "ingenious_testcase_show":   return MCPServer.jsonContent(json, testCaseShow(json, args));
            case "ingenious_testcase_create": return MCPServer.jsonContent(json, testCaseCreate(json, args));
            case "ingenious_testcase_add_step": return MCPServer.jsonContent(json, testCaseAddStep(json, args));
            case "ingenious_testcase_delete": return MCPServer.jsonContent(json, testCaseDelete(json, args));
            case "ingenious_testset_list":    return MCPServer.jsonContent(json, testSetList(json, args));
            case "ingenious_testset_show":    return MCPServer.jsonContent(json, testSetShow(json, args));
            case "ingenious_action_list":     return MCPServer.jsonContent(json, actionList(json, args));
            case "ingenious_action_search":   return MCPServer.jsonContent(json, actionSearch(json, args));
            case "ingenious_action_info":     return MCPServer.jsonContent(json, actionInfo(json, args));
            case "ingenious_action_categories": return MCPServer.jsonContent(json, actionCategories(json));
            case "ingenious_run":             return MCPServer.jsonContent(json, runSync(json, args));
            case "ingenious_run_async":       return MCPServer.jsonContent(json, runAsync(json, args));
            case "ingenious_run_status":      return MCPServer.jsonContent(json, runStatus(json, args));
            case "ingenious_run_logs":        return MCPServer.jsonContent(json, runLogs(json, args));
            case "ingenious_run_cancel":      return MCPServer.jsonContent(json, runCancel(json, args));
            case "ingenious_report_latest":   return MCPServer.jsonContent(json, reportLatest(json, args));
            case "ingenious_report_history":  return MCPServer.jsonContent(json, reportHistory(json, args));
            case "ingenious_report_failures": return MCPServer.jsonContent(json, reportFailures(json, args));
            case "ingenious_config_get":      return MCPServer.jsonContent(json, configGet(json, args));
            case "ingenious_config_set":      return MCPServer.jsonContent(json, configSet(json, args));

            case "ingenious_project_create":  return MCPServer.jsonContent(json, projectCreate(json, args));

            case "ingenious_data_sheet_create":  return MCPServer.jsonContent(json, dataSheetCreate(json, args));
            case "ingenious_data_row_add":       return MCPServer.jsonContent(json, dataRowAdd(json, args));
            case "ingenious_data_column_add":    return MCPServer.jsonContent(json, dataColumnAdd(json, args));

            case "ingenious_env_list":        return MCPServer.jsonContent(json, envList(json, args));
            case "ingenious_env_create":      return MCPServer.jsonContent(json, envCreate(json, args));
            case "ingenious_env_delete":      return MCPServer.jsonContent(json, envDelete(json, args));

            case "ingenious_import_curl":         return MCPServer.jsonContent(json, importCurl(json, args));
            case "ingenious_import_postman":      return MCPServer.jsonContent(json, importPostman(json, args));
            case "ingenious_import_bruno":        return MCPServer.jsonContent(json, importBruno(json, args));
            case "ingenious_import_playwright":   return MCPServer.jsonContent(json, importPlaywright(json, args));
            default:
                throw new MCPServer.MCPException(-32601, "Unknown tool: " + name);
        }
    }

    // ==================================================================
    // project tools
    // ==================================================================

    private JsonNode projectList(ObjectMapper json, JsonNode args) {
        String basePath = MCPServer.paramOrDefault(args, "basePath",
                System.getProperty("user.dir") + File.separator + "Projects");
        File baseDir = new File(basePath);
        ArrayNode out = json.createArrayNode();
        if (!baseDir.isDirectory()) return out;

        File[] candidates = baseDir.listFiles(File::isDirectory);
        if (candidates == null) return out;
        Arrays.sort(candidates, Comparator.comparing(File::getName));

        for (File dir : candidates) {
            // a directory is a project iff it has a TestPlan/ subfolder
            if (!new File(dir, Project.TEST_PLAN_DIR).isDirectory()) continue;
            ObjectNode p = out.addObject();
            p.put("name", dir.getName());
            p.put("path", dir.getAbsolutePath());
        }
        return out;
    }

    private JsonNode projectInfo(ObjectMapper json, JsonNode args) {
        File dir = resolveProject(MCPServer.requiredParam(args, "project"));
        Project project = loadProject(dir);

        ObjectNode out = json.createObjectNode();
        out.put("name",      project.getName());
        out.put("location",  project.getLocation());
        out.put("scenarios", project.getScenarios().size());
        int tcCount = 0;
        for (Scenario s : project.getScenarios()) tcCount += s.getTestCases().size();
        out.put("testCases", tcCount);
        return out;
    }

    // ==================================================================
    // scenario tools
    // ==================================================================

    private JsonNode scenarioList(ObjectMapper json, JsonNode args) {
        Project p = loadProject(resolveProject(projectArg(args)));
        ArrayNode out = json.createArrayNode();
        for (Scenario s : p.getScenarios()) {
            ObjectNode n = out.addObject();
            n.put("name",      s.getName());
            n.put("testCases", s.getTestCases().size());
        }
        return out;
    }

    private JsonNode scenarioCreate(ObjectMapper json, JsonNode args) {
        Project p = loadProject(resolveProject(projectArg(args)));
        String name = MCPServer.requiredParam(args, "scenario");
        boolean reusable = boolArg(args, "reusable", false);
        Scenario existing = reusable
                ? p.getReusableScenarioByName(name)
                : p.getScenarioByName(name);
        if (existing != null) {
            return json.createObjectNode().put("created", false).put("scenario", name)
                    .put("reusable", reusable)
                    .put("message", (reusable ? "Reusable scenario" : "Scenario") + " already exists");
        }
        Scenario created = reusable ? p.addReusableScenario(name) : p.addScenario(name);
        if (created == null) {
            throw new MCPServer.MCPException(-32603, "Failed to create scenario: " + name);
        }
        new File(created.getLocation()).mkdirs();
        p.save();
        return json.createObjectNode().put("created", true).put("scenario", name)
                .put("reusable", reusable);
    }

    // ==================================================================
    // test case tools
    // ==================================================================

    private JsonNode testCaseList(ObjectMapper json, JsonNode args) {
        Project p = loadProject(resolveProject(projectArg(args)));
        String scenFilter = MCPServer.paramOrDefault(args, "scenario", null);
        ArrayNode out = json.createArrayNode();
        for (Scenario s : p.getScenarios()) {
            if (scenFilter != null && !s.getName().equals(scenFilter)) continue;
            for (TestCase tc : s.getTestCases()) {
                ensureLoaded(tc);
                ObjectNode n = out.addObject();
                n.put("scenario", s.getName());
                n.put("testcase", tc.getName());
                n.put("steps",    tc.getTestSteps().size());
            }
        }
        return out;
    }

    private JsonNode testCaseShow(ObjectMapper json, JsonNode args) {
        Project p = loadProject(resolveProject(projectArg(args)));
        String scenName = MCPServer.requiredParam(args, "scenario");
        String tcName   = MCPServer.requiredParam(args, "testcase");
        Scenario s = p.getScenarioByName(scenName);
        if (s == null) throw new MCPServer.MCPException(-32602, "Scenario not found: " + scenName);
        TestCase tc = s.getTestCaseByName(tcName);
        if (tc == null) throw new MCPServer.MCPException(-32602, "Test case not found: " + tcName);
        ensureLoaded(tc);

        ObjectNode out = json.createObjectNode();
        out.put("project",  p.getName());
        out.put("scenario", s.getName());
        out.put("testcase", tc.getName());
        ArrayNode steps = out.putArray("steps");
        int i = 1;
        for (TestStep st : tc.getTestSteps()) {
            ObjectNode step = steps.addObject();
            step.put("step",        i++);
            step.put("action",      st.getAction());
            step.put("object",      st.getObject());
            step.put("input",       st.getInput());
            step.put("condition",   st.getCondition());
            step.put("description", st.getDescription());
            step.put("reference",   st.getReference());
        }
        return out;
    }

    private JsonNode testCaseCreate(ObjectMapper json, JsonNode args) {
        Project p = loadProject(resolveProject(projectArg(args)));
        String scenName = MCPServer.requiredParam(args, "scenario");
        String tcName   = MCPServer.requiredParam(args, "testcase");
        boolean reusable = boolArg(args, "reusable", false);
        String explicitFormat = MCPServer.paramOrDefault(args, "format", null);

        Scenario s = reusable
                ? p.getReusableScenarioByName(scenName)
                : p.getScenarioByName(scenName);
        if (s == null) {
            s = reusable ? p.addReusableScenario(scenName) : p.addScenario(scenName);
            if (s == null) throw new MCPServer.MCPException(-32603,
                    "Failed to create scenario: " + scenName);
            new File(s.getLocation()).mkdirs();
        }
        TestCase existing = s.getTestCaseByName(tcName);
        if (existing != null) {
            throw new MCPServer.MCPException(-32602, "Test case already exists: " + tcName);
        }

        // Honour explicit format if given, else prefer YAML by default (the
        // project default), but if the scenario already has CSV siblings keep
        // CSV so the IDE and `ingenious run` stay consistent.
        String originalFormat = null;
        try { originalFormat = p.getInfo().getTestCaseFormat(); } catch (Exception ignored) {}
        String chosenFormat = explicitFormat != null && !explicitFormat.isEmpty()
                ? explicitFormat.toUpperCase(Locale.ROOT)
                : detectScenarioFormatPreferYaml(s);
        try { p.getInfo().setTestCaseFormat(chosenFormat); } catch (Exception ignored) {}

        TestCase tc = s.addTestCase(tcName);
        if (tc == null) {
            try { p.getInfo().setTestCaseFormat(originalFormat); } catch (Exception ignored) {}
            throw new MCPServer.MCPException(-32603,
                    "Failed to create test case: " + tcName);
        }

        try {
            JsonNode steps = args == null ? null : args.get("steps");
            if (steps != null && steps.isArray()) {
                for (JsonNode raw : steps) appendStep(tc, raw);
            }
            tc.save();
        } finally {
            try { p.getInfo().setTestCaseFormat(originalFormat); } catch (Exception ignored) {}
        }
        return json.createObjectNode()
                .put("created",  true)
                .put("scenario", scenName)
                .put("testcase", tcName)
                .put("reusable", reusable)
                .put("format",   chosenFormat)
                .put("steps",    tc.getTestSteps().size());
    }

    /**
     * Picks the on-disk format for a newly-created test case in {@code s}.
     * Prefers CSV if any CSV sibling exists, then YAML, then defaults to CSV
     * (matches what {@code ingenious run} can resolve).
     */
    private static String detectScenarioFormat(Scenario s) {
        try {
            File dir = new File(s.getLocation());
            if (dir.isDirectory()) {
                boolean hasCsv = false, hasYaml = false;
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        String n = f.getName().toLowerCase(Locale.ROOT);
                        if (n.endsWith(".csv"))  hasCsv  = true;
                        else if (n.endsWith(".yaml") || n.endsWith(".yml")) hasYaml = true;
                    }
                }
                if (hasCsv)  return "CSV";
                if (hasYaml) return "YAML";
            }
        } catch (Exception ignored) {}
        return "CSV";
    }

    /** Same probe as {@link #detectScenarioFormat} but defaults to YAML when neither exists. */
    private static String detectScenarioFormatPreferYaml(Scenario s) {
        try {
            File dir = new File(s.getLocation());
            if (dir.isDirectory()) {
                boolean hasCsv = false, hasYaml = false;
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        String n = f.getName().toLowerCase(Locale.ROOT);
                        if (n.endsWith(".csv"))  hasCsv  = true;
                        else if (n.endsWith(".yaml") || n.endsWith(".yml")) hasYaml = true;
                    }
                }
                if (hasYaml) return "YAML";
                if (hasCsv)  return "CSV";
            }
        } catch (Exception ignored) {}
        return "YAML";
    }

    /** Read a boolean MCP argument with a default. */
    private static boolean boolArg(JsonNode args, String key, boolean def) {
        if (args == null) return def;
        JsonNode n = args.get(key);
        if (n == null || n.isNull()) return def;
        if (n.isBoolean()) return n.asBoolean();
        if (n.isTextual())  return Boolean.parseBoolean(n.asText());
        return def;
    }

    private JsonNode testCaseAddStep(ObjectMapper json, JsonNode args) {
        Project p = loadProject(resolveProject(projectArg(args)));
        String scenName = MCPServer.requiredParam(args, "scenario");
        String tcName   = MCPServer.requiredParam(args, "testcase");
        Scenario s = p.getScenarioByName(scenName);
        if (s == null) throw new MCPServer.MCPException(-32602, "Scenario not found: " + scenName);
        TestCase tc = s.getTestCaseByName(tcName);
        if (tc == null) throw new MCPServer.MCPException(-32602, "Test case not found: " + tcName);
        ensureLoaded(tc);

        appendStep(tc, args);
        tc.save();
        return json.createObjectNode()
                .put("added", true)
                .put("totalSteps", tc.getTestSteps().size());
    }

    /**
     * Datalib's {@link TestCase#getTestSteps()} returns the in-memory list
     * which is empty for test cases that haven't been opened in the UI yet.
     * We have to call the lazy loader explicitly before reading or appending.
     */
    private void ensureLoaded(TestCase tc) {
        try {
            tc.loadTestCaseTableModel();
        } catch (Exception ignored) {
            // best-effort – an unreadable test case still appears in the list
        }
    }

    private void appendStep(TestCase tc, JsonNode raw) {
        TestStep step = tc.addNewStep();
        step.setAction     (MCPServer.paramOrDefault(raw, "action",      ""));
        step.setObject     (MCPServer.paramOrDefault(raw, "object",      ""));
        step.setInput      (MCPServer.paramOrDefault(raw, "input",       ""));
        step.setCondition  (MCPServer.paramOrDefault(raw, "condition",   ""));
        step.setDescription(MCPServer.paramOrDefault(raw, "description", ""));
        step.setReference  (MCPServer.paramOrDefault(raw, "reference",   ""));
    }

    private JsonNode testCaseDelete(ObjectMapper json, JsonNode args) {
        File dir = resolveProject(projectArg(args));
        String scenName = MCPServer.requiredParam(args, "scenario");
        String tcName   = MCPServer.requiredParam(args, "testcase");
        File scenDir = new File(new File(dir, Project.TEST_PLAN_DIR), scenName);
        // Datalib persists test cases as either .yaml (default) or .csv.
        // Probe both, in preferred order, and fall back to a directory scan
        // so renamed/legacy extensions still match.
        File target = null;
        for (String ext : new String[] { ".yaml", ".yml", ".csv" }) {
            File f = new File(scenDir, tcName + ext);
            if (f.isFile()) { target = f; break; }
        }
        if (target == null && scenDir.isDirectory()) {
            File[] matches = scenDir.listFiles(f -> f.isFile()
                    && f.getName().regionMatches(true, 0, tcName + ".", 0, tcName.length() + 1));
            if (matches != null && matches.length > 0) target = matches[0];
        }
        if (target == null) {
            throw new MCPServer.MCPException(-32602,
                    "Test case file not found under: " + scenDir);
        }
        if (!target.delete()) {
            throw new MCPServer.MCPException(-32603, "Failed to delete: " + target);
        }
        return json.createObjectNode().put("deleted", true).put("path", target.getAbsolutePath());
    }

    // ==================================================================
    // test set tools (TestLab/)
    // ==================================================================

    private JsonNode testSetList(ObjectMapper json, JsonNode args) {
        File dir = resolveProject(projectArg(args));
        String releaseFilter = MCPServer.paramOrDefault(args, "release", null);
        File testLab = new File(dir, "TestLab");
        ArrayNode out = json.createArrayNode();
        if (!testLab.isDirectory()) return out;
        File[] releases = testLab.listFiles(File::isDirectory);
        if (releases == null) return out;
        Arrays.sort(releases, Comparator.comparing(File::getName));
        for (File rel : releases) {
            if (releaseFilter != null && !rel.getName().equals(releaseFilter)) continue;
            File[] sets = rel.listFiles(f -> f.isFile() && f.getName().endsWith(".csv"));
            if (sets == null) continue;
            Arrays.sort(sets, Comparator.comparing(File::getName));
            for (File set : sets) {
                ObjectNode n = out.addObject();
                n.put("release", rel.getName());
                n.put("testset", set.getName().replaceFirst("\\.csv$", ""));
                n.put("path",    set.getAbsolutePath());
            }
        }
        return out;
    }

    private JsonNode testSetShow(ObjectMapper json, JsonNode args) {
        File dir     = resolveProject(projectArg(args));
        String rel   = MCPServer.requiredParam(args, "release");
        String set   = MCPServer.requiredParam(args, "testset");
        File csv = new File(new File(new File(dir, "TestLab"), rel), set + ".csv");
        if (!csv.isFile()) {
            throw new MCPServer.MCPException(-32602, "Test set not found: " + csv);
        }
        ObjectNode out = json.createObjectNode();
        out.put("release", rel);
        out.put("testset", set);
        ArrayNode rows = out.putArray("rows");
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(csv), StandardCharsets.UTF_8))) {
            String line;
            boolean header = true;
            int rowNum = 0;
            while ((line = br.readLine()) != null) {
                if (header) { header = false; continue; }
                if (line.trim().isEmpty()) continue;
                String[] cols = line.split(",", -1);
                ObjectNode r = rows.addObject();
                r.put("row",      ++rowNum);
                r.put("scenario", cols.length > 0 ? cols[0] : "");
                r.put("testcase", cols.length > 1 ? cols[1] : "");
                r.put("browser",  cols.length > 2 ? cols[2] : "");
                r.put("tags",     cols.length > 3 ? cols[3] : "");
            }
        } catch (IOException e) {
            throw new MCPServer.MCPException(-32603, "Failed to read test set: " + e.getMessage());
        }
        return out;
    }

    // ==================================================================
    // action tools
    // ==================================================================

    private JsonNode actionList(ObjectMapper json, JsonNode args) {
        String category = MCPServer.paramOrDefault(args, "category", null);
        int limit = -1;
        JsonNode lim = args == null ? null : args.get("limit");
        if (lim != null && lim.isInt()) limit = lim.asInt();

        List<ActionCatalog.ActionInfo> actions = ActionCatalog.byCategory(category);
        ArrayNode out = json.createArrayNode();
        int i = 0;
        for (ActionCatalog.ActionInfo a : actions) {
            if (limit > 0 && i++ >= limit) break;
            out.add(actionToJson(json, a));
        }
        return out;
    }

    private JsonNode actionSearch(ObjectMapper json, JsonNode args) {
        String query = MCPServer.requiredParam(args, "query");
        ArrayNode out = json.createArrayNode();
        for (ActionCatalog.ActionInfo a : ActionCatalog.search(query)) {
            out.add(actionToJson(json, a));
        }
        return out;
    }

    private JsonNode actionInfo(ObjectMapper json, JsonNode args) {
        String name = MCPServer.requiredParam(args, "action");
        ActionCatalog.ActionInfo a = ActionCatalog.find(name);
        if (a == null) throw new MCPServer.MCPException(-32602, "Action not found: " + name);
        return actionToJson(json, a);
    }

    private JsonNode actionCategories(ObjectMapper json) {
        ObjectNode out = json.createObjectNode();
        for (Map.Entry<String, Integer> e : ActionCatalog.categoryCounts().entrySet()) {
            out.put(e.getKey(), e.getValue());
        }
        return out;
    }

    private ObjectNode actionToJson(ObjectMapper json, ActionCatalog.ActionInfo a) {
        ObjectNode n = json.createObjectNode();
        n.put("name",               a.name);
        n.put("category",           a.category);
        n.put("objectType",         a.objectType);
        n.put("description",        a.description);
        n.put("inputRequired",      a.inputRequired);
        n.put("conditionSupported", a.conditionSupported);
        return n;
    }

    // ==================================================================
    // run tools – subprocess executions
    // ==================================================================

    private JsonNode runSync(ObjectMapper json, JsonNode args) {
        RunSpec spec = parseRunSpec(args);
        int timeoutSec = 1800;
        JsonNode t = args == null ? null : args.get("timeoutSeconds");
        if (t != null && t.isInt()) timeoutSec = t.asInt();

        RunHandle h = startRun(spec);
        try {
            boolean completed = h.process.waitFor(timeoutSec, TimeUnit.SECONDS);
            if (!completed) {
                h.process.destroyForcibly();
                h.exitCode = -1;
                h.status = "TIMEOUT";
            } else {
                h.exitCode = h.process.exitValue();
                h.status = h.exitCode == 0 ? "PASS" : "FAIL";
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            h.process.destroyForcibly();
            h.status = "INTERRUPTED";
        }
        h.endedAt = System.currentTimeMillis();

        ObjectNode out = json.createObjectNode();
        out.put("runId",    h.id);
        out.put("target",   spec.target);
        out.put("status",   h.status);
        out.put("exitCode", h.exitCode);
        out.put("durationMs", h.endedAt - h.startedAt);
        out.put("command",  String.join(" ", h.command));
        out.put("output",   tail(h.output.toString(), 400));
        return out;
    }

    private JsonNode runAsync(ObjectMapper json, JsonNode args) {
        RunSpec spec = parseRunSpec(args);
        RunHandle h = startRun(spec);
        return json.createObjectNode()
                .put("runId",  h.id)
                .put("target", spec.target)
                .put("status", "RUNNING")
                .put("startedAt", h.startedAt);
    }

    private JsonNode runStatus(ObjectMapper json, JsonNode args) {
        String runId = MCPServer.paramOrDefault(args, "runId", null);
        if (runId == null) {
            ArrayNode out = json.createArrayNode();
            for (RunHandle h : runs.values()) out.add(runHandleSummary(json, h));
            return out;
        }
        RunHandle h = runs.get(runId);
        if (h == null) throw new MCPServer.MCPException(-32602, "Unknown runId: " + runId);
        return runHandleSummary(json, h);
    }

    private ObjectNode runHandleSummary(ObjectMapper json, RunHandle h) {
        if (h.endedAt == 0 && !h.process.isAlive()) {
            h.exitCode = h.process.exitValue();
            h.status   = h.exitCode == 0 ? "PASS" : "FAIL";
            h.endedAt  = System.currentTimeMillis();
        }
        ObjectNode n = json.createObjectNode();
        n.put("runId",     h.id);
        n.put("target",    h.spec.target);
        n.put("status",    h.status);
        n.put("alive",     h.process.isAlive());
        n.put("startedAt", h.startedAt);
        if (h.endedAt > 0) {
            n.put("endedAt",    h.endedAt);
            n.put("durationMs", h.endedAt - h.startedAt);
            n.put("exitCode",   h.exitCode);
        }
        return n;
    }

    private JsonNode runLogs(ObjectMapper json, JsonNode args) {
        String runId = MCPServer.requiredParam(args, "runId");
        int tail = 200;
        JsonNode t = args == null ? null : args.get("tail");
        if (t != null && t.isInt()) tail = t.asInt();
        RunHandle h = runs.get(runId);
        if (h == null) throw new MCPServer.MCPException(-32602, "Unknown runId: " + runId);
        return json.createObjectNode()
                .put("runId",  runId)
                .put("output", tailLines(h.output.toString(), tail));
    }

    private JsonNode runCancel(ObjectMapper json, JsonNode args) {
        String runId = MCPServer.requiredParam(args, "runId");
        RunHandle h = runs.get(runId);
        if (h == null) throw new MCPServer.MCPException(-32602, "Unknown runId: " + runId);
        if (h.process.isAlive()) {
            h.process.destroy();
            try { h.process.waitFor(5, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
            if (h.process.isAlive()) h.process.destroyForcibly();
            h.status  = "CANCELLED";
            h.endedAt = System.currentTimeMillis();
        }
        return runHandleSummary(json, h);
    }

    private RunHandle startRun(RunSpec spec) {
        List<String> cmd = new ArrayList<>();
        cmd.add(javaBinary());
        cmd.add("-Dfile.encoding=UTF-8");
        cmd.add("-cp");
        cmd.add(System.getProperty("java.class.path"));
        // The CLI's `main` lives on Control, not INGeniousCLI – Control routes
        // between the legacy and Picocli front-ends. Calling INGeniousCLI
        // directly fails with "Main method not found".
        //
        // Control.isNewCLICommand() looks at args[0] only, so the subcommand
        // (`run`) MUST be the first argument; global flags like `--no-color`
        // would prevent picocli routing entirely. We deliberately omit
        // `--no-color` – stdout is captured, terminal coloring is irrelevant.
        cmd.add("com.ing.engine.core.Control");
        cmd.add("run");
        cmd.add(spec.target);
        if (spec.browser != null)  { cmd.add("-b"); cmd.add(spec.browser); }
        if (spec.headless)         { cmd.add("--headless"); }
        if (spec.parallel > 1)     { cmd.add("--parallel"); cmd.add(Integer.toString(spec.parallel)); }
        if (spec.tags != null && !spec.tags.isEmpty()) {
            cmd.add("-t");
            cmd.add(spec.tags);
        }

        ProcessBuilder pb = new ProcessBuilder(cmd).redirectErrorStream(true);
        pb.directory(new File(System.getProperty("user.dir")));
        try {
            Process p = pb.start();
            RunHandle h = new RunHandle();
            h.id        = "run-" + UUID.randomUUID().toString().substring(0, 8);
            h.spec      = spec;
            h.command   = cmd;
            h.process   = p;
            h.startedAt = System.currentTimeMillis();
            h.status    = "RUNNING";
            runs.put(h.id, h);

            // pump output asynchronously
            Thread t = new Thread(() -> {
                try (BufferedReader r = new BufferedReader(
                        new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = r.readLine()) != null) {
                        synchronized (h.output) {
                            h.output.append(line).append('\n');
                            // cap to ~2MB to avoid OOM on long runs
                            if (h.output.length() > 2_000_000) {
                                int drop = h.output.length() - 1_500_000;
                                h.output.delete(0, drop);
                            }
                        }
                    }
                } catch (IOException ignored) { }
            }, "mcp-run-pump-" + h.id);
            t.setDaemon(true);
            t.start();
            return h;
        } catch (IOException e) {
            throw new MCPServer.MCPException(-32603, "Failed to start subprocess: " + e.getMessage());
        }
    }

    private RunSpec parseRunSpec(JsonNode args) {
        RunSpec s = new RunSpec();
        s.target   = MCPServer.requiredParam(args, "target");
        s.browser  = com.ing.engine.cli.lib.BrowserNames.normalize(
                MCPServer.paramOrDefault(args, "browser", null));
        s.tags     = MCPServer.paramOrDefault(args, "tags", null);
        s.headless = false;
        s.parallel = 1;
        if (args != null) {
            JsonNode h = args.get("headless");
            if (h != null && h.isBoolean()) s.headless = h.asBoolean();
            JsonNode p = args.get("parallel");
            if (p != null && p.isInt())     s.parallel = p.asInt();
        }
        return s;
    }

    private String javaBinary() {
        String home = System.getProperty("java.home");
        File java = new File(home, "bin/java");
        if (!java.exists()) {
            File w = new File(home, "bin/java.exe");
            if (w.exists()) return w.getAbsolutePath();
        }
        return java.getAbsolutePath();
    }

    // ==================================================================
    // report tools – data.js parsing (matches RunCommand.rerunFailed)
    // ==================================================================

    private JsonNode reportLatest(ObjectMapper json, JsonNode args) {
        File latest = locateLatestRun(args);
        return readDataJs(json, latest);
    }

    private JsonNode reportHistory(ObjectMapper json, JsonNode args) {
        File dir = locateRunDir(args);
        int limit = 10;
        JsonNode l = args == null ? null : args.get("limit");
        if (l != null && l.isInt()) limit = l.asInt();
        File[] entries = dir.listFiles(f -> f.isDirectory() && !f.getName().equals("Latest"));
        ArrayNode out = json.createArrayNode();
        if (entries == null) return out;
        Arrays.sort(entries, Comparator.comparingLong(File::lastModified).reversed());
        for (int i = 0; i < Math.min(limit, entries.length); i++) {
            ObjectNode n = out.addObject();
            n.put("runId",    entries[i].getName());
            n.put("modified", entries[i].lastModified());
            n.put("path",     entries[i].getAbsolutePath());
        }
        return out;
    }

    private JsonNode reportFailures(ObjectMapper json, JsonNode args) {
        File latest = locateLatestRun(args);
        JsonNode summary = readDataJs(json, latest);
        ArrayNode failures = json.createArrayNode();
        JsonNode executions = summary.path("EXECUTIONS");
        if (executions.isArray()) {
            for (JsonNode tc : executions) {
                if ("FAIL".equalsIgnoreCase(tc.path("status").asText())) {
                    failures.add(tc);
                }
            }
        } else if ("FAIL".equalsIgnoreCase(summary.path("status").asText())) {
            failures.add(summary);
        }
        ObjectNode out = json.createObjectNode();
        out.put("count", failures.size());
        out.set("failures", failures);
        return out;
    }

    private File locateRunDir(JsonNode args) {
        File project = resolveProject(projectArg(args));
        String target = MCPServer.requiredParam(args, "target");
        String[] parts = target.split("/");
        if (parts.length != 2) {
            throw new MCPServer.MCPException(-32602,
                    "target must be '<Scenario>/<TestCase>' or '<Release>/<TestSet>'");
        }
        File design = new File(project, "Results/TestDesign/" + parts[0] + "/" + parts[1]);
        File exec   = new File(project, "Results/TestExecution/" + parts[0] + "/" + parts[1]);
        if (design.isDirectory()) return design;
        if (exec.isDirectory())   return exec;
        throw new MCPServer.MCPException(-32602, "No results directory found for: " + target);
    }

    private File locateLatestRun(JsonNode args) {
        File dir = locateRunDir(args);
        File latest = new File(dir, "Latest");
        if (!latest.isDirectory()) {
            throw new MCPServer.MCPException(-32602, "Latest/ folder not present in: " + dir);
        }
        return latest;
    }

    private JsonNode readDataJs(ObjectMapper json, File latest) {
        File js = new File(latest, "data.js");
        if (!js.isFile()) throw new MCPServer.MCPException(-32602, "data.js not found in: " + latest);
        try {
            String content = Files.readString(js.toPath(), StandardCharsets.UTF_8);
            int eq = content.indexOf('=');
            int semi = content.lastIndexOf(';');
            if (eq < 0 || semi <= eq) {
                throw new MCPServer.MCPException(-32603, "Could not parse data.js");
            }
            return json.readTree(content.substring(eq + 1, semi).trim());
        } catch (IOException e) {
            throw new MCPServer.MCPException(-32603, "Failed to read data.js: " + e.getMessage());
        }
    }

    // ==================================================================
    // config tools
    // ==================================================================

    private JsonNode configGet(ObjectMapper json, JsonNode args) {
        File f = configFile(args);
        if (!f.isFile()) throw new MCPServer.MCPException(-32602,
                "Configuration file not found: " + f);
        Properties props = new Properties();
        try (FileInputStream is = new FileInputStream(f)) { props.load(is); }
        catch (IOException e) {
            throw new MCPServer.MCPException(-32603, "Failed to read config: " + e.getMessage());
        }
        String key = MCPServer.paramOrDefault(args, "key", null);
        ObjectNode out = json.createObjectNode();
        if (key != null) {
            out.put("key",   key);
            out.put("value", props.getProperty(key, ""));
            return out;
        }
        Set<String> keys = new LinkedHashSet<>(props.stringPropertyNames());
        for (String k : keys) out.put(k, props.getProperty(k));
        return out;
    }

    private JsonNode configSet(ObjectMapper json, JsonNode args) {
        File f   = configFile(args);
        String k = MCPServer.requiredParam(args, "key");
        String v = MCPServer.requiredParam(args, "value");
        Properties props = new Properties();
        if (f.isFile()) {
            try (FileInputStream is = new FileInputStream(f)) { props.load(is); }
            catch (IOException e) {
                throw new MCPServer.MCPException(-32603, "Failed to read config: " + e.getMessage());
            }
        } else {
            f.getParentFile().mkdirs();
        }
        props.setProperty(k, v);
        try (FileOutputStream os = new FileOutputStream(f)) {
            props.store(os, "Updated by INGenious MCP");
        } catch (IOException e) {
            throw new MCPServer.MCPException(-32603, "Failed to write config: " + e.getMessage());
        }
        return json.createObjectNode().put("updated", true).put("key", k).put("value", v);
    }

    private File configFile(JsonNode args) {
        File dir = resolveProject(projectArg(args));
        String name = MCPServer.paramOrDefault(args, "file", "Global Settings.properties");
        return new File(new File(dir, "Configuration"), name);
    }

    // ==================================================================
    // project create
    // ==================================================================

    private JsonNode projectCreate(ObjectMapper json, JsonNode args) {
        String name = MCPServer.requiredParam(args, "name");
        String parent = MCPServer.paramOrDefault(args, "parentDir", System.getProperty("user.dir"));
        String format = MCPServer.paramOrDefault(args, "format", "YAML").toUpperCase(Locale.ROOT);
        boolean noSample = boolArg(args, "noSample", false);
        File parentDir = new File(parent).getAbsoluteFile();
        File projectDir = new File(parentDir, name);
        if (projectDir.exists()) {
            throw new MCPServer.MCPException(-32602, "Project already exists: " + projectDir);
        }
        try {
            projectDir.mkdirs();
            new File(projectDir, "TestPlan").mkdirs();
            new File(projectDir, "ReusableComponents").mkdirs();
            new File(projectDir, "ObjectRepository").mkdirs();
            new File(projectDir, "TestData").mkdirs();
            new File(projectDir, "TestLab").mkdirs();
            new File(projectDir, "Settings").mkdirs();
            new File(projectDir, "Results").mkdirs();
            new File(projectDir, "Configuration").mkdirs();
            Project p = new Project(name, parentDir.getAbsolutePath(), "csv");
            if (!noSample) p.createProject();
            if (p.getInfo() != null) p.getInfo().setTestCaseFormat(format);
            p.save();
        } catch (Exception e) {
            throw new MCPServer.MCPException(-32603, "Failed to create project: " + e.getMessage());
        }
        return json.createObjectNode()
                .put("created",  true)
                .put("name",     name)
                .put("location", projectDir.getAbsolutePath())
                .put("format",   format);
    }

    // ==================================================================
    // data sheet / row / column
    // ==================================================================

    private JsonNode dataSheetCreate(ObjectMapper json, JsonNode args) {
        Project p = loadProject(resolveProject(projectArg(args)));
        String sheet = MCPServer.requiredParam(args, "sheet");
        String envName = MCPServer.paramOrDefault(args, "env", null);
        com.ing.datalib.component.EnvTestData env = p.getTestData();
        java.util.Collection<com.ing.datalib.component.TestData> targets =
                com.ing.engine.cli.commands.DataCommand.pickEnvs(env, envName);
        if (targets.isEmpty()) throw new MCPServer.MCPException(-32602,
                "Environment not found: " + envName);
        int added = 0;
        for (com.ing.datalib.component.TestData td : targets) {
            if (td.getByName(sheet) != null) continue;
            td.addTestData(td.getNewTestData(sheet));
            added++;
        }
        env.save();
        p.save();
        return json.createObjectNode().put("sheet", sheet).put("environments", added);
    }

    private JsonNode dataRowAdd(ObjectMapper json, JsonNode args) {
        Project p = loadProject(resolveProject(projectArg(args)));
        String sheet = MCPServer.requiredParam(args, "sheet");
        String scnName = MCPServer.requiredParam(args, "scenario");
        String tcName  = MCPServer.requiredParam(args, "testcase");
        boolean reusable = boolArg(args, "reusable", false);
        String iter    = MCPServer.paramOrDefault(args, "iteration", "1");
        String subIter = MCPServer.paramOrDefault(args, "subIteration", "1");
        String envName = MCPServer.paramOrDefault(args, "env", null);
        JsonNode colObj = args == null ? null : args.get("columns");

        com.ing.datalib.component.EnvTestData env = p.getTestData();
        java.util.Collection<com.ing.datalib.component.TestData> targets =
                com.ing.engine.cli.commands.DataCommand.pickEnvs(env, envName);
        if (targets.isEmpty()) throw new MCPServer.MCPException(-32602,
                "Environment not found: " + envName);
        int added = 0;
        for (com.ing.datalib.component.TestData td : targets) {
            com.ing.datalib.testdata.model.TestDataModel model = td.getByName(sheet);
            if (model == null) model = td.addTestData(td.getNewTestData(sheet));
            model.loadTableModel();
            com.ing.datalib.testdata.model.Record rec = model.addRecord();
            rec.setScenario(reusable ? ("(R) " + scnName) : scnName);
            rec.setTestcase(tcName);
            rec.setIteration(iter);
            rec.setSubIteration(subIter);
            if (colObj != null && colObj.isObject()) {
                int row = model.getRowCount() - 1;
                java.util.Iterator<String> fields = colObj.fieldNames();
                while (fields.hasNext()) {
                    String k = fields.next();
                    String v = colObj.get(k).asText();
                    int idx = model.getColumnIndex(k);
                    if (idx < 0) { model.addColumn(k); idx = model.getColumnIndex(k); }
                    if (idx >= 0) model.setValueAt(v, row, idx);
                }
            }
            added++;
        }
        env.save();
        p.save();
        return json.createObjectNode()
                .put("sheet", sheet)
                .put("environments", added)
                .put("scenario", scnName)
                .put("testcase", tcName)
                .put("reusable", reusable);
    }

    private JsonNode dataColumnAdd(ObjectMapper json, JsonNode args) {
        Project p = loadProject(resolveProject(projectArg(args)));
        String sheet = MCPServer.requiredParam(args, "sheet");
        String column = MCPServer.requiredParam(args, "column");
        String envName = MCPServer.paramOrDefault(args, "env", null);
        com.ing.datalib.component.EnvTestData env = p.getTestData();
        java.util.Collection<com.ing.datalib.component.TestData> targets =
                com.ing.engine.cli.commands.DataCommand.pickEnvs(env, envName);
        if (targets.isEmpty()) throw new MCPServer.MCPException(-32602,
                "Environment not found: " + envName);
        int added = 0;
        for (com.ing.datalib.component.TestData td : targets) {
            com.ing.datalib.testdata.model.TestDataModel model = td.getByName(sheet);
            if (model == null) model = td.addTestData(td.getNewTestData(sheet));
            model.loadTableModel();
            if (model.getColumnIndex(column) < 0) {
                model.addColumn(column);
                added++;
            }
        }
        env.save();
        p.save();
        return json.createObjectNode()
                .put("sheet", sheet)
                .put("column", column)
                .put("environments", added);
    }

    // ==================================================================
    // environments
    // ==================================================================

    private JsonNode envList(ObjectMapper json, JsonNode args) {
        Project p = loadProject(resolveProject(projectArg(args)));
        ArrayNode out = json.createArrayNode();
        for (String e : p.getTestData().getEnvironments()) out.add(e);
        return out;
    }

    private JsonNode envCreate(ObjectMapper json, JsonNode args) {
        Project p = loadProject(resolveProject(projectArg(args)));
        String envName = MCPServer.requiredParam(args, "env");
        String from = MCPServer.paramOrDefault(args, "from", null);
        boolean withGlobal = boolArg(args, "withGlobal", false);
        com.ing.datalib.component.EnvTestData env = p.getTestData();
        if (from != null && !from.isEmpty()) {
            com.ing.datalib.component.TestData src = env.getTestDataFor(from);
            if (src == null) throw new MCPServer.MCPException(-32602,
                    "Source env not found: " + from);
            java.util.List<String> sheets = new java.util.ArrayList<>();
            for (com.ing.datalib.testdata.model.TestDataModel m : src.getTestDataList()) {
                sheets.add(m.getName());
            }
            env.createNewEnvironment(envName, from, sheets, withGlobal);
        } else {
            env.createNewEnvironment(envName);
        }
        env.save();
        p.save();
        return json.createObjectNode()
                .put("created", true)
                .put("env", envName)
                .put("clonedFrom", from);
    }

    private JsonNode envDelete(ObjectMapper json, JsonNode args) {
        Project p = loadProject(resolveProject(projectArg(args)));
        String envName = MCPServer.requiredParam(args, "env");
        com.ing.datalib.component.EnvTestData env = p.getTestData();
        if (env.getTestDataFor(envName) == null) {
            throw new MCPServer.MCPException(-32602, "Environment not found: " + envName);
        }
        env.deleteEnvironment(envName);
        env.save();
        p.save();
        return json.createObjectNode().put("deleted", true).put("env", envName);
    }

    // ==================================================================
    // importers (curl / postman / bruno / playwright)
    // ==================================================================

    private JsonNode importCurl(ObjectMapper json, JsonNode args) {
        Project p = loadProject(resolveProject(projectArg(args)));
        String curl = MCPServer.requiredParam(args, "curl");
        String scenName = MCPServer.paramOrDefault(args, "scenario", "Imported");
        String tcName   = MCPServer.paramOrDefault(args, "testcase", null);
        boolean reusable = boolArg(args, "reusable", false);
        if (!com.ing.datalib.api.CurlParser.looksLikeCurl(curl)) {
            throw new MCPServer.MCPException(-32602, "Input does not look like a curl command.");
        }
        com.ing.datalib.api.APIRequest req = com.ing.datalib.api.CurlParser.parse(curl);
        Scenario scn = ensureScenario(p, scenName, reusable);
        String name = (tcName != null && !tcName.isEmpty())
                ? com.ing.datalib.api.importer.ImportUtils.sanitizeFileName(tcName)
                : deriveRequestName(req);
        if (scn.getTestCaseByName(name) != null) {
            throw new MCPServer.MCPException(-32602,
                    "Test case already exists: " + scn.getName() + "/" + name);
        }
        TestCase tc = com.ing.engine.cli.lib.RequestToTestCaseBuilder.build(req, scn, name);
        p.save();
        if (tc == null) throw new MCPServer.MCPException(-32603, "Failed to build test case");
        return json.createObjectNode()
                .put("created", true)
                .put("scenario", scn.getName())
                .put("testcase", name)
                .put("reusable", reusable)
                .put("steps", tc.getTestSteps().size());
    }

    private JsonNode importPostman(ObjectMapper json, JsonNode args) {
        return importCollection(json, args,
                new com.ing.datalib.api.importer.postman.PostmanImporter(), "Postman");
    }

    private JsonNode importBruno(ObjectMapper json, JsonNode args) {
        return importCollection(json, args,
                new com.ing.datalib.api.importer.bruno.BrunoImporter(), "Bruno");
    }

    private JsonNode importCollection(ObjectMapper json, JsonNode args,
                                      com.ing.datalib.api.importer.spi.CollectionImporter importer,
                                      String label) {
        Project p = loadProject(resolveProject(projectArg(args)));
        String filePath = MCPServer.requiredParam(args, "file");
        File file = new File(filePath);
        if (!file.exists()) throw new MCPServer.MCPException(-32602,
                label + " source not found: " + filePath);
        if (!importer.supports(file)) throw new MCPServer.MCPException(-32602,
                file.getName() + " is not recognised as a " + label + " source.");
        String scenName = MCPServer.paramOrDefault(args, "scenario", label);
        boolean reusable = boolArg(args, "reusable", false);
        String conflict = MCPServer.paramOrDefault(args, "conflict", "rename").toLowerCase(Locale.ROOT);

        java.util.List<com.ing.datalib.api.importer.ImportWarning> warnings = new java.util.ArrayList<>();
        com.ing.datalib.api.importer.NormalizedCollection coll;
        try {
            coll = importer.parse(file, warnings);
        } catch (com.ing.datalib.api.importer.ImportException ie) {
            throw new MCPServer.MCPException(-32603,
                    label + " parse failed: " + ie.getMessage());
        }
        Scenario scn = ensureScenario(p, scenName, reusable);
        int created = 0, skipped = 0, renamed = 0;
        for (com.ing.datalib.api.importer.NormalizedRequest nreq : coll.getRequests()) {
            if (nreq == null || nreq.getRequest() == null) continue;
            com.ing.datalib.api.APIRequest req = nreq.getRequest();
            String base = (req.getName() != null && !req.getName().isEmpty())
                    ? req.getName() : deriveRequestName(req);
            String name = com.ing.datalib.api.importer.ImportUtils.sanitizeFileName(base);
            if (scn.getTestCaseByName(name) != null) {
                switch (conflict) {
                    case "skip":
                        skipped++;
                        continue;
                    case "overwrite": {
                        TestCase old = scn.getTestCaseByName(name);
                        File f = new File(old.getLocation());
                        if (f.exists()) f.delete();
                        scn.getTestCases().remove(old);
                        break;
                    }
                    case "rename":
                    default: {
                        String candidate = name;
                        int n = 2;
                        while (scn.getTestCaseByName(candidate) != null) {
                            candidate = name + "_" + (n++);
                        }
                        name = candidate;
                        renamed++;
                        break;
                    }
                }
            }
            TestCase tc = com.ing.engine.cli.lib.RequestToTestCaseBuilder.build(req, scn, name);
            if (tc != null) created++;
        }
        p.save();
        ObjectNode out = json.createObjectNode();
        out.put("source", label);
        out.put("scenario", scn.getName());
        out.put("reusable", reusable);
        out.put("created", created);
        out.put("renamed", renamed);
        out.put("skipped", skipped);
        ArrayNode ws = out.putArray("warnings");
        for (com.ing.datalib.api.importer.ImportWarning w : warnings) ws.add(w.getMessage());
        return out;
    }

    private JsonNode importPlaywright(ObjectMapper json, JsonNode args) {
        Project p = loadProject(resolveProject(projectArg(args)));
        String filePath = MCPServer.requiredParam(args, "file");
        File file = new File(filePath);
        if (!file.isFile()) throw new MCPServer.MCPException(-32602,
                "Recording file not found: " + filePath);
        String scenName = MCPServer.paramOrDefault(args, "scenario", null);
        String tcName = MCPServer.paramOrDefault(args, "testcase", null);
        com.ing.datalib.api.importer.playwright.PlaywrightRecordingImporter.Result r;
        try {
            r = com.ing.datalib.api.importer.playwright.PlaywrightRecordingImporter.importInto(
                    p, file, scenName, tcName);
        } catch (RuntimeException e) {
            throw new MCPServer.MCPException(-32603, "Playwright import failed: " + e.getMessage());
        }
        p.save();
        p.reload();
        if (r.stepCount == 0) throw new MCPServer.MCPException(-32603,
                "No recognised Playwright steps in: " + file.getName());
        com.fasterxml.jackson.databind.node.ObjectNode out = json.createObjectNode()
                .put("created", true)
                .put("scenario", r.scenarioName)
                .put("testcase", r.testCaseName)
                .put("steps", r.stepCount);
        com.fasterxml.jackson.databind.node.ArrayNode ws = out.putArray("warnings");
        for (String w : r.warnings) ws.add(w);
        return out;
    }

    private static Scenario ensureScenario(Project p, String name, boolean reusable) {
        Scenario s = reusable ? p.getReusableScenarioByName(name) : p.getScenarioByName(name);
        if (s == null) {
            s = reusable ? p.addReusableScenario(name) : p.addScenario(name);
            new File(s.getLocation()).mkdirs();
        }
        return s;
    }

    private static String deriveRequestName(com.ing.datalib.api.APIRequest req) {
        if (req.getName() != null && !req.getName().isEmpty()) {
            return com.ing.datalib.api.importer.ImportUtils.sanitizeFileName(req.getName());
        }
        String url = req.getUrl() == null ? "request" : req.getUrl();
        String path = url.replaceAll("https?://[^/]+", "");
        if (path.isEmpty() || "/".equals(path)) path = url;
        path = path.replaceAll("[?#].*$", "").replaceAll("/+$", "");
        if (path.startsWith("/")) path = path.substring(1);
        if (path.isEmpty()) path = "request";
        String method = req.getMethod() == null ? "GET" : req.getMethod().name();
        return com.ing.datalib.api.importer.ImportUtils.sanitizeFileName(
                method + "_" + path.replace('/', '_'));
    }

    // ==================================================================
    // helpers
    // ==================================================================

    private String projectArg(JsonNode args) {
        String name = MCPServer.paramOrDefault(args, "project", defaultProject);
        if (name == null || name.isEmpty()) {
            throw new MCPServer.MCPException(-32602,
                    "No project specified and the MCP server has no default. "
                  + "Pass 'project' or launch the server with --project.");
        }
        return name;
    }

    /** Resolve a project by abs path, ./<name>, or ./Projects/<name>. */
    private File resolveProject(String name) {
        File abs = new File(name);
        if (abs.isAbsolute() && abs.isDirectory()) return abs;
        String cwd = System.getProperty("user.dir");
        File rel = new File(cwd, name);
        if (rel.isDirectory()) return rel;
        File underProjects = new File(cwd, "Projects" + File.separator + name);
        if (underProjects.isDirectory()) return underProjects;
        throw new MCPServer.MCPException(-32602, "Project not found: " + name
                + " (looked in $cwd, $cwd/Projects, and as an absolute path)");
    }

    private Project loadProject(File dir) {
        try {
            return new Project(dir.getAbsolutePath());
        } catch (Exception e) {
            throw new MCPServer.MCPException(-32603,
                    "Failed to load project at " + dir + ": " + e.getMessage());
        }
    }

    private String tail(String s, int maxLines) {
        if (s == null) return "";
        return tailLines(s, maxLines);
    }

    private String tailLines(String s, int n) {
        if (s == null || n <= 0) return "";
        String[] lines = s.split("\\R");
        if (lines.length <= n) return s;
        StringBuilder sb = new StringBuilder();
        for (int i = lines.length - n; i < lines.length; i++) {
            sb.append(lines[i]).append('\n');
        }
        return sb.toString();
    }

    // ==================================================================
    // schema builder
    // ==================================================================

    private SchemaBuilder schema(ObjectMapper json) {
        return new SchemaBuilder(json);
    }

    /**
     * JSON Schema for a single step item used by {@code ingenious_testcase_create.steps[]}.
     * All fields optional except {@code action}.
     */
    private static ObjectNode stepItemSchema(ObjectMapper json) {
        ObjectNode item = json.createObjectNode();
        item.put("type", "object");
        ObjectNode p = item.putObject("properties");
        p.putObject("action").put("type", "string")
                .put("description", "Action name (see ingenious_action_list).");
        p.putObject("object").put("type", "string")
                .put("description", "Object reference (page.element or @Browser etc.).");
        p.putObject("input").put("type", "string")
                .put("description", "Input value or @variable.");
        p.putObject("condition").put("type", "string")
                .put("description", "Optional condition.");
        p.putObject("description").put("type", "string")
                .put("description", "Optional human-readable description.");
        item.putArray("required").add("action");
        item.put("additionalProperties", true);
        return item;
    }

    /** Tiny fluent helper to keep JSON Schema definitions readable. */
    static class SchemaBuilder {
        private final ObjectMapper json;
        private final ObjectNode props;
        private final ArrayNode required;

        SchemaBuilder(ObjectMapper json) {
            this.json     = json;
            this.props    = json.createObjectNode();
            this.required = json.createArrayNode();
        }

        SchemaBuilder required(String name, String type, String description) {
            ObjectNode p = props.putObject(name);
            p.put("type", type);
            p.put("description", description);
            required.add(name);
            return this;
        }

        SchemaBuilder optional(String name, String type, String description) {
            ObjectNode p = props.putObject(name);
            p.put("type", type);
            p.put("description", description);
            return this;
        }

        /**
         * Declare an optional array property whose elements follow {@code items}.
         * VS Code's MCP client validates that every {@code "type": "array"} entry
         * has an accompanying {@code items} schema, so we always supply one.
         */
        SchemaBuilder optionalArray(String name, String description, ObjectNode items) {
            ObjectNode p = props.putObject(name);
            p.put("type", "array");
            p.put("description", description);
            p.set("items", items);
            return this;
        }

        SchemaBuilder requiredArray(String name, String description, ObjectNode items) {
            ObjectNode p = props.putObject(name);
            p.put("type", "array");
            p.put("description", description);
            p.set("items", items);
            required.add(name);
            return this;
        }

        ObjectNode build() {
            ObjectNode schema = json.createObjectNode();
            schema.put("type", "object");
            schema.set("properties", props);
            if (required.size() > 0) schema.set("required", required);
            schema.put("additionalProperties", true);
            return schema;
        }
    }

    // ==================================================================
    // run-state holder
    // ==================================================================

    static class RunSpec {
        String  target;
        String  browser;
        String  tags;
        boolean headless;
        int     parallel;
    }

    static class RunHandle {
        String      id;
        RunSpec     spec;
        List<String> command;
        Process     process;
        long        startedAt;
        long        endedAt;
        int         exitCode;
        String      status;
        StringBuilder output = new StringBuilder(8192);
    }
}

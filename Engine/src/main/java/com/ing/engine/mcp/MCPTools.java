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
import java.util.HashSet;
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
    /** Live Playwright Agent CLI authoring sessions keyed by session name. */
    private final Map<String, PwSession> pwSessions = new ConcurrentHashMap<>();

    MCPTools(String defaultProject) {
        this.defaultProject = defaultProject;
    }

    // ==================================================================
    // tool descriptors – tools/list
    // ==================================================================

    JsonNode list(ObjectMapper json) {
        ObjectNode result = json.createObjectNode();
        ArrayNode arr = result.putArray("tools");

        addTool(
            arr,
            "ingenious_project_list",
            "List all INGenious test automation projects under a base directory " +
            "(defaults to ./Projects).",
            schema(json)
                .optional(
                    "basePath",
                    "string",
                    "Directory to scan. Defaults to ./Projects in the server's CWD."
                )
                .build()
        );

        addTool(
            arr,
            "ingenious_project_info",
            "Get summary information (scenario count, test case count, location) for one project.",
            schema(json).required("project", "string", "Project name or absolute path.").build()
        );

        addTool(
            arr,
            "ingenious_scenario_list",
            "List all scenarios (TestPlan folders) in a project, with their test case counts.",
            schema(json).optional("project", "string", "Project name or absolute path.").build()
        );

        addTool(
            arr,
            "ingenious_scenario_create",
            "Create a new scenario folder. Pass reusable=true to create under ReusableComponents/ instead of TestPlan/.",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .required("scenario", "string", "Scenario name to create.")
                .optional(
                    "reusable",
                    "boolean",
                    "Create under ReusableComponents/ (default false)."
                )
                .build()
        );

        addTool(
            arr,
            "ingenious_testcase_list",
            "List test cases in a project, optionally filtered by scenario.",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .optional("scenario", "string", "Filter to a single scenario.")
                .build()
        );

        addTool(
            arr,
            "ingenious_testcase_show",
            "Show the full step-by-step contents of a test case.",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .required("scenario", "string", "Scenario name.")
                .required("testcase", "string", "Test case name (no extension).")
                .build()
        );

        addTool(
            arr,
            "ingenious_testcase_create",
            "Create a new test case (default format: YAML), or one pre-populated with steps. " +
            "Steps may be supplied as an array of {action, object, input, condition, description}. " +
            "Pass reusable=true to create under ReusableComponents/<scenario>/ instead of TestPlan/.",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .required("scenario", "string", "Scenario name (created if missing).")
                .required("testcase", "string", "New test case name.")
                .optional("format", "string", "YAML (default) or CSV.")
                .optional("reusable", "boolean", "Create as a reusable component (default false).")
                .optional(
                    "ifExists",
                    "string",
                    "When the test case exists: error (default) | skip | overwrite."
                )
                .optional(
                    "dryRun",
                    "boolean",
                    "Preview only \u2013 report what would be created without writing (default false)."
                )
                .optionalArray(
                    "steps",
                    "Optional list of step objects to insert immediately.",
                    stepItemSchema(json)
                )
                .build()
        );

        addTool(
            arr,
            "ingenious_testcase_add_step",
            "Append a single step to an existing test case.",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .required("scenario", "string", "Scenario name.")
                .required("testcase", "string", "Test case name.")
                .required("action", "string", "Action name (see ingenious_action_list).")
                .optional("object", "string", "Object reference (page.element or @Browser etc.).")
                .optional("input", "string", "Input value or @variable.")
                .optional("condition", "string", "Optional condition.")
                .optional("description", "string", "Optional human-readable description.")
                .build()
        );

        addTool(
            arr,
            "ingenious_testcase_delete",
            "Delete a test case CSV file.",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .required("scenario", "string", "Scenario name.")
                .required("testcase", "string", "Test case name.")
                .build()
        );

        addTool(
            arr,
            "ingenious_testset_list",
            "List test sets (TestLab/<release>/<set>.csv) in a project, " +
            "optionally filtered by release.",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .optional("release", "string", "Filter to one release.")
                .build()
        );

        addTool(
            arr,
            "ingenious_testset_show",
            "Read the contents of a test set CSV (list of scenario/testcase rows).",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .required("release", "string", "Release name.")
                .required("testset", "string", "Test set name (without .csv).")
                .build()
        );

        addTool(
            arr,
            "ingenious_action_list",
            "List available test actions (Browser, API, Database, Mobile, Kafka, General). " +
            "Use this before creating steps so the AI picks actions that actually exist.",
            schema(json)
                .optional(
                    "category",
                    "string",
                    "Filter to one of: Browser, API, Database, Mobile, Kafka, General."
                )
                .optional("limit", "integer", "Cap the number of results returned.")
                .build()
        );

        addTool(
            arr,
            "ingenious_action_search",
            "Free-text, synonym-aware search across action names, descriptions and " +
            "object types, ranked best-match first. Optionally filter by category " +
            "(Browser, API, Mobile, Database, Kafka, General) to disambiguate " +
            "actions that exist for several object types.",
            schema(json)
                .required("query", "string", "Search term.")
                .optional(
                    "category",
                    "string",
                    "Restrict results to one category: Browser, API, Mobile, " +
                    "Database, Kafka, or General. Use the step's object type " +
                    "(e.g. API for Webservice steps)."
                )
                .build()
        );

        addTool(
            arr,
            "ingenious_action_info",
            "Get detailed metadata for a single action.",
            schema(json)
                .required("action", "string", "Action name (e.g. 'Click', 'GET', 'Set').")
                .build()
        );

        addTool(
            arr,
            "ingenious_action_categories",
            "Counts of available actions per category.",
            schema(json).build()
        );

        addTool(
            arr,
            "ingenious_run",
            "Execute a test case OR a test set synchronously and return the captured output. " +
            "The target may be '<Project>/<Scenario>/<TestCase>' or '<Project>/<Release>/<TestSet>' " +
            "(matches the `ingenious run` CLI auto-detection).",
            schema(json)
                .required(
                    "target",
                    "string",
                    "<Project>/<Scenario>/<TestCase> or <Project>/<Release>/<TestSet>."
                )
                .optional(
                    "browser",
                    "string",
                    "Chromium | Firefox | WebKit | 'No Browser' (aliases: NoBrowser, no-browser). Default Chromium."
                )
                .optional("headless", "boolean", "Run headless. Default false.")
                .optional("parallel", "integer", "Thread count for test sets. Default 1.")
                .optional("tags", "string", "Comma-separated tag filter (test sets only).")
                .optional(
                    "rerun",
                    "boolean",
                    "Re-execute only the test cases that failed in the last run of the target. Default false."
                )
                .optional(
                    "timeoutSeconds",
                    "integer",
                    "Wall-clock timeout for the whole run. Default 1800."
                )
                .build()
        );

        addTool(
            arr,
            "ingenious_run_async",
            "Start a test run in the background and return a runId. " +
            "Poll with ingenious_run_status / fetch output with ingenious_run_logs.",
            schema(json)
                .required(
                    "target",
                    "string",
                    "<Project>/<Scenario>/<TestCase> or <Project>/<Release>/<TestSet>."
                )
                .optional(
                    "browser",
                    "string",
                    "Chromium | Firefox | WebKit | 'No Browser' (aliases: NoBrowser, no-browser)."
                )
                .optional("headless", "boolean", "Run headless.")
                .optional("parallel", "integer", "Thread count for test sets.")
                .optional("tags", "string", "Comma-separated tag filter.")
                .build()
        );

        addTool(
            arr,
            "ingenious_run_status",
            "Status of a previously started async run (or list all when no runId is given).",
            schema(json)
                .optional("runId", "string", "Run id returned by ingenious_run_async.")
                .build()
        );

        addTool(
            arr,
            "ingenious_run_logs",
            "Captured stdout/stderr of an async run (last N lines).",
            schema(json)
                .required("runId", "string", "Run id returned by ingenious_run_async.")
                .optional("tail", "integer", "Number of trailing lines. Default 200.")
                .build()
        );

        addTool(
            arr,
            "ingenious_run_cancel",
            "Cancel an in-flight async run.",
            schema(json).required("runId", "string", "Run id to cancel.").build()
        );

        addTool(
            arr,
            "ingenious_report_latest",
            "Get the latest run report summary for a target.",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .required(
                    "target",
                    "string",
                    "'<Scenario>/<TestCase>' (TestDesign) or '<Release>/<TestSet>' (TestExecution)."
                )
                .build()
        );

        addTool(
            arr,
            "ingenious_report_history",
            "List the last N timestamped runs for a target.",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .required("target", "string", "'<Scenario>/<TestCase>' or '<Release>/<TestSet>'.")
                .optional("limit", "integer", "Max entries. Default 10.")
                .build()
        );

        addTool(
            arr,
            "ingenious_report_failures",
            "List failed test cases from the latest run of a target (parses Latest/data.js).",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .required("target", "string", "'<Scenario>/<TestCase>' or '<Release>/<TestSet>'.")
                .build()
        );

        addTool(
            arr,
            "ingenious_config_get",
            "Read a project Configuration property (or all of them).",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .optional("key", "string", "Property key; omit to dump all.")
                .optional(
                    "file",
                    "string",
                    "Filename under Configuration/. " + "Defaults to 'Global Settings.properties'."
                )
                .build()
        );

        addTool(
            arr,
            "ingenious_config_set",
            "Update a project Configuration property.",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .required("key", "string", "Property key.")
                .required("value", "string", "New value.")
                .optional(
                    "file",
                    "string",
                    "Filename under Configuration/. " + "Defaults to 'Global Settings.properties'."
                )
                .build()
        );

        // -----------------------------------------------------------
        // project create
        // -----------------------------------------------------------
        addTool(
            arr,
            "ingenious_project_create",
            "Create a new INGenious project with the full folder layout " +
            "(TestPlan/, ReusableComponents/, ObjectRepository/, TestData/, " +
            "TestLab/, Settings/, Configuration/, Results/). Defaults the " +
            "test case format to YAML.",
            schema(json)
                .required(
                    "name",
                    "string",
                    "Project name (folder will be created under parent directory)."
                )
                .optional(
                    "parentDir",
                    "string",
                    "Parent directory (default: ./Projects in the current working dir)."
                )
                .optional("format", "string", "Default test case format: YAML (default) or CSV.")
                .optional(
                    "noSample",
                    "boolean",
                    "Skip creating the default sample scenario/test case."
                )
                .build()
        );

        // -----------------------------------------------------------
        // data sheet / row / column / env
        // -----------------------------------------------------------
        addTool(
            arr,
            "ingenious_data_sheet_create",
            "Create a new test data sheet in one or all environments.",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .required("sheet", "string", "New sheet name.")
                .optional(
                    "env",
                    "string",
                    "Target environment ('all' or environment name; default: all)."
                )
                .build()
        );

        addTool(
            arr,
            "ingenious_data_row_add",
            "Add a row binding a scenario/test case (or reusable scenario/component) to a data sheet. " +
            "Extra column values may be supplied via the columns object.",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .required("sheet", "string", "Sheet name (created if missing).")
                .required(
                    "scenario",
                    "string",
                    "Scenario name (or reusable-scenario name when reusable=true)."
                )
                .required(
                    "testcase",
                    "string",
                    "Test case name (or reusable-component name when reusable=true)."
                )
                .optional(
                    "reusable",
                    "boolean",
                    "Treat scenario/testcase as a reusable component reference."
                )
                .optional("iteration", "string", "Iteration number (default 1).")
                .optional("subIteration", "string", "Sub-iteration number (default 1).")
                .optional(
                    "env",
                    "string",
                    "Target environment ('all' or environment name; default: all)."
                )
                .optional(
                    "columns",
                    "object",
                    "Map of column name to value (extra columns are added on-demand)."
                )
                .build()
        );

        addTool(
            arr,
            "ingenious_data_column_add",
            "Add a column to a data sheet in one or all environments.",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .required("sheet", "string", "Sheet name (created if missing).")
                .required("column", "string", "New column name.")
                .optional(
                    "env",
                    "string",
                    "Target environment ('all' or environment name; default: all)."
                )
                .build()
        );

        addTool(
            arr,
            "ingenious_env_list",
            "List all configured test data environments.",
            schema(json).optional("project", "string", "Project name or absolute path.").build()
        );

        addTool(
            arr,
            "ingenious_env_create",
            "Create a new environment, optionally cloning sheets from an existing one.",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .required("env", "string", "New environment name.")
                .optional("from", "string", "Source environment to clone sheets from.")
                .optional("withGlobal", "boolean", "Also clone global data (default false).")
                .build()
        );

        addTool(
            arr,
            "ingenious_env_delete",
            "Delete a test data environment.",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .required("env", "string", "Environment name to delete.")
                .build()
        );

        // -----------------------------------------------------------
        // importers
        // -----------------------------------------------------------
        addTool(
            arr,
            "ingenious_import_curl",
            "Import a single curl command as an API test case (Webservice steps).",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .required("curl", "string", "The curl command string.")
                .optional("scenario", "string", "Scenario name (default: Imported).")
                .optional("testcase", "string", "Test case name (default: derived from URL).")
                .optional("reusable", "boolean", "Create as a reusable component (default false).")
                .build()
        );

        addTool(
            arr,
            "ingenious_import_postman",
            "Import a Postman collection (.json) as test cases or reusable components.",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .required("file", "string", "Path to the Postman collection JSON file.")
                .optional("scenario", "string", "Target scenario name (default: Postman).")
                .optional("reusable", "boolean", "Import as reusable components.")
                .optional(
                    "conflict",
                    "string",
                    "Conflict policy: skip | overwrite | rename (default: rename)."
                )
                .build()
        );

        addTool(
            arr,
            "ingenious_import_bruno",
            "Import a Bruno collection (file or directory) as test cases or reusable components.",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .required("file", "string", "Path to the Bruno collection file or root directory.")
                .optional("scenario", "string", "Target scenario name (default: Bruno).")
                .optional("reusable", "boolean", "Import as reusable components.")
                .optional(
                    "conflict",
                    "string",
                    "Conflict policy: skip | overwrite | rename (default: rename)."
                )
                .build()
        );

        addTool(
            arr,
            "ingenious_import_playwright",
            "Import a Playwright recording (Java source from codegen) as a test case. Uses the same parser as the IDE's Tools \u2192 Import Playwright Recording.",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .required("file", "string", "Path to the recording file (.txt or .java).")
                .optional(
                    "scenario",
                    "string",
                    "Target scenario name (default: derived from file name)."
                )
                .optional("testcase", "string", "Test case name (default: derived from file name).")
                .build()
        );

        // -----------------------------------------------------------
        // scenario info / delete
        // -----------------------------------------------------------
        addTool(
            arr,
            "ingenious_scenario_info",
            "Show details of a scenario: its test cases and per-test-case step counts.",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .required("scenario", "string", "Scenario name.")
                .optional("reusable", "boolean", "Look under ReusableComponents/ (default false).")
                .build()
        );

        addTool(
            arr,
            "ingenious_scenario_delete",
            "Delete a scenario and all its test cases from TestPlan/ (or ReusableComponents/). Irreversible.",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .required("scenario", "string", "Scenario name to delete.")
                .optional("reusable", "boolean", "Delete from ReusableComponents/ (default false).")
                .build()
        );

        // -----------------------------------------------------------
        // testcase validate
        // -----------------------------------------------------------
        addTool(
            arr,
            "ingenious_testcase_validate",
            "Lint test case(s) against INGenious conventions: unknown actions, broken " +
            "Execute/reusable references, missing data references, hard-coded literals, " +
            "missing assertions and more. Returns errors, warnings and info with rule ids. " +
            "Omit scenario/testcase to validate the whole project.",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .optional("scenario", "string", "Restrict to one scenario.")
                .optional("testcase", "string", "Restrict to one test case (requires scenario).")
                .optional(
                    "reusable",
                    "boolean",
                    "Validate ReusableComponents/ instead of TestPlan/ (default false)."
                )
                .build()
        );

        // -----------------------------------------------------------
        // testcase parameterize (data-driven conversion)
        // -----------------------------------------------------------
        addTool(
            arr,
            "ingenious_testcase_parameterize",
            "Externalise hard-coded values of a test case (or reusable) into a data sheet. " +
            "Call with mode=scan first: it lists every candidate - whole @literal inputs and " +
            "individual JSON payload fields - with suggested sheet/column names. Then apply with " +
            "mode=all, or mode=selected plus a selections array to parameterize only some values " +
            "(for API payloads pick individual JSON paths). Values move into a data-sheet row keyed " +
            "to this test case; inputs are rewritten as Sheet:Column, payload fields as {Sheet:Column}.",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .required("scenario", "string", "Scenario name.")
                .required("testcase", "string", "Test case (or reusable) name.")
                .optional("reusable", "boolean", "Target a reusable component (default false).")
                .optional("mode", "string", "scan (default) | all | selected.")
                .optional(
                    "sheet",
                    "string",
                    "Target data sheet name (default: derived from the test case)."
                )
                .optional("env", "string", "Data environment to write to (default: all).")
                .optional("iteration", "string", "Data row iteration (default 1).")
                .optionalArray(
                    "selections",
                    "For mode=selected: candidate ids from the scan, or objects " +
                    "{id, column?, sheet?, paths?:[{path, column?}]} to override names or pick " +
                    "individual payload fields.",
                    selectionItemSchema(json)
                )
                .optional("dryRun", "boolean", "Preview the plan without writing (default false).")
                .build()
        );

        // -----------------------------------------------------------
        // test set create / add
        // -----------------------------------------------------------
        addTool(
            arr,
            "ingenious_testset_create",
            "Create an empty test set under TestLab/<release>/. Creates the release if needed.",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .required("release", "string", "Release name.")
                .required("testset", "string", "Test set name.")
                .build()
        );

        addTool(
            arr,
            "ingenious_testset_add",
            "Append a test case row to a test set (creates the test set/release if missing).",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .required("release", "string", "Release name.")
                .required("testset", "string", "Test set name.")
                .required("scenario", "string", "Scenario name to execute.")
                .required("testcase", "string", "Test case name to execute.")
                .optional("browser", "string", "Browser column value (default Chrome).")
                .optional("iteration", "string", "Iteration number (default 1).")
                .optional(
                    "execute",
                    "boolean",
                    "Whether the row is enabled for execution (default true)."
                )
                .optional(
                    "dryRun",
                    "boolean",
                    "Preview only \u2013 report the row that would be added without writing."
                )
                .build()
        );

        // -----------------------------------------------------------
        // object repository
        // -----------------------------------------------------------
        addTool(
            arr,
            "ingenious_object_list",
            "List pages in the Object Repository with object counts.",
            schema(json).optional("project", "string", "Project name or absolute path.").build()
        );

        addTool(
            arr,
            "ingenious_object_show",
            "Show all objects (name, type, locator, value, description) on an Object Repository page.",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .required("page", "string", "Page name (without .csv).")
                .build()
        );

        addTool(
            arr,
            "ingenious_object_search",
            "Search objects across all Object Repository pages by name, locator, or value.",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .required("query", "string", "Case-insensitive substring to match.")
                .build()
        );

        // -----------------------------------------------------------
        // data show / get / set
        // -----------------------------------------------------------
        addTool(
            arr,
            "ingenious_data_show",
            "Show the columns and rows of a test data sheet (environment-aware).",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .required("sheet", "string", "Data sheet name.")
                .optional("env", "string", "Environment name (default: first environment).")
                .optional("limit", "integer", "Max rows to return (default 50).")
                .build()
        );

        addTool(
            arr,
            "ingenious_data_get",
            "Read a single cell from a data sheet by column name and 1-based row number.",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .required("sheet", "string", "Data sheet name.")
                .required("column", "string", "Column name.")
                .optional("row", "integer", "1-based data row (default 1).")
                .optional("env", "string", "Environment name (default: first environment).")
                .build()
        );

        addTool(
            arr,
            "ingenious_data_set",
            "Write a single cell in a data sheet. Adds the column and rows on demand. " +
            "Applies to one or all environments.",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .required("sheet", "string", "Data sheet name.")
                .required("column", "string", "Column name.")
                .required("value", "string", "Value to write.")
                .optional("row", "integer", "1-based data row (default 1).")
                .optional("env", "string", "Target environment ('all' or a name; default all).")
                .optional(
                    "dryRun",
                    "boolean",
                    "Preview only \u2013 report the write without persisting (default false)."
                )
                .build()
        );

        // -----------------------------------------------------------
        // report show / compare
        // -----------------------------------------------------------
        addTool(
            arr,
            "ingenious_report_show",
            "Show the full parsed report (data.js) for a specific historical run.",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .required("target", "string", "'<Scenario>/<TestCase>' or '<Release>/<TestSet>'.")
                .required(
                    "runId",
                    "string",
                    "Timestamped run folder name (see ingenious_report_history)."
                )
                .build()
        );

        addTool(
            arr,
            "ingenious_report_compare",
            "Compare pass/fail totals between two historical runs of the same target.",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .required("target", "string", "'<Scenario>/<TestCase>' or '<Release>/<TestSet>'.")
                .required("runA", "string", "First run folder name.")
                .required("runB", "string", "Second run folder name.")
                .build()
        );

        // -----------------------------------------------------------
        // config show
        // -----------------------------------------------------------
        addTool(
            arr,
            "ingenious_config_show",
            "List the configuration files under Configuration/ (use ingenious_config_get to dump one).",
            schema(json).optional("project", "string", "Project name or absolute path.").build()
        );

        // -----------------------------------------------------------
        // test case step editing (Phase 2)
        // -----------------------------------------------------------
        addTool(
            arr,
            "ingenious_testcase_edit_step",
            "Replace fields of a single step (1-based index). Only supplied fields change.",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .required("scenario", "string", "Scenario name.")
                .required("testcase", "string", "Test case name.")
                .required("index", "integer", "1-based step index to edit.")
                .optional("action", "string", "New action.")
                .optional("object", "string", "New object reference.")
                .optional("input", "string", "New input value.")
                .optional("condition", "string", "New condition.")
                .optional("description", "string", "New description.")
                .optional("reference", "string", "New reference.")
                .build()
        );

        addTool(
            arr,
            "ingenious_testcase_insert_step",
            "Insert a new step at a 1-based index (existing steps shift down).",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .required("scenario", "string", "Scenario name.")
                .required("testcase", "string", "Test case name.")
                .required("index", "integer", "1-based index to insert at.")
                .required("action", "string", "Action name.")
                .optional("object", "string", "Object reference.")
                .optional("input", "string", "Input value.")
                .optional("condition", "string", "Condition.")
                .optional("description", "string", "Description.")
                .build()
        );

        addTool(
            arr,
            "ingenious_testcase_remove_step",
            "Delete a step by 1-based index.",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .required("scenario", "string", "Scenario name.")
                .required("testcase", "string", "Test case name.")
                .required("index", "integer", "1-based step index to remove.")
                .build()
        );

        addTool(
            arr,
            "ingenious_testcase_move_step",
            "Move a step from one 1-based index to another.",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .required("scenario", "string", "Scenario name.")
                .required("testcase", "string", "Test case name.")
                .required("from", "integer", "1-based source index.")
                .required("to", "integer", "1-based destination index.")
                .build()
        );

        // -----------------------------------------------------------
        // object repository write (Phase 2)
        // -----------------------------------------------------------
        addTool(
            arr,
            "ingenious_object_add",
            "Add a web object (locator) to an Object Repository page as YAML " +
            "(ObjectRepository/Web/<page>.yaml); creates the page if missing.",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .required("page", "string", "Object Repository page name.")
                .required("name", "string", "Object name.")
                .optional("type", "string", "Ignored (web objects only); kept for compatibility.")
                .optional(
                    "locator",
                    "string",
                    "Locator strategy: role, text, label, placeholder, css, xpath, testId, " +
                    "altText, title, jsPath, chainedLocator. id/name/class map to a css selector."
                )
                .optional("value", "string", "Locator value (selector / accessible name).")
                .optional(
                    "description",
                    "string",
                    "Ignored by the YAML model; kept for compatibility."
                )
                .optional(
                    "dryRun",
                    "boolean",
                    "Preview only \u2013 report whether the object would be added (default false)."
                )
                .build()
        );

        addTool(
            arr,
            "ingenious_object_update",
            "Update the locator of an existing web object on a page (YAML model). " +
            "Only supplied fields change.",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .required("page", "string", "Page name.")
                .required("name", "string", "Object name to update.")
                .optional("type", "string", "Ignored (web objects only); kept for compatibility.")
                .optional(
                    "locator",
                    "string",
                    "New locator strategy (role, text, label, css, xpath, testId, ...)."
                )
                .optional("value", "string", "New locator value.")
                .optional(
                    "description",
                    "string",
                    "Ignored by the YAML model; kept for compatibility."
                )
                .build()
        );

        addTool(
            arr,
            "ingenious_object_delete",
            "Delete an object from an Object Repository page.",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .required("page", "string", "Page name.")
                .required("name", "string", "Object name to delete.")
                .build()
        );

        // -----------------------------------------------------------
        // data row delete (Phase 2)
        // -----------------------------------------------------------
        addTool(
            arr,
            "ingenious_data_row_delete",
            "Delete a data-sheet row by 1-based index across one or all environments.",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .required("sheet", "string", "Data sheet name.")
                .required("row", "integer", "1-based data row to delete.")
                .optional("env", "string", "Target environment ('all' or a name; default all).")
                .build()
        );

        addTool(
            arr,
            "ingenious_data_import",
            "Import a CSV file into a data sheet: creates the sheet, adds columns from the header, " +
            "and appends the rows (environment-aware).",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .required("file", "string", "Path to the source CSV file.")
                .optional("sheet", "string", "Target sheet name (default: source file name).")
                .optional("env", "string", "Target environment ('all' or a name; default all).")
                .build()
        );

        // -----------------------------------------------------------
        // report export + config drivers + run dry-run (follow-ups)
        // -----------------------------------------------------------
        addTool(
            arr,
            "ingenious_report_export",
            "Export a run report to a file as json, csv, or junit.",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .required("target", "string", "'<Scenario>/<TestCase>' or '<Release>/<TestSet>'.")
                .optional("runId", "string", "Run folder name (default: Latest).")
                .optional("format", "string", "json | csv | junit (default json).")
                .optional("output", "string", "Output file path (default: alongside CWD).")
                .build()
        );

        addTool(
            arr,
            "ingenious_config_drivers",
            "Check local browser driver / Playwright CLI availability and versions.",
            schema(json).build()
        );

        addTool(
            arr,
            "ingenious_run_dry",
            "Resolve and validate a run target without executing it. Reports whether the " +
            "scenario/test case (or release/test set) exists and how many steps/rows it has.",
            schema(json)
                .required(
                    "target",
                    "string",
                    "<Project>/<Scenario>/<TestCase> or <Project>/<Release>/<TestSet>."
                )
                .build()
        );

        // -----------------------------------------------------------
        // environment diagnostics
        // -----------------------------------------------------------
        addTool(
            arr,
            "ingenious_doctor",
            "Environment health check: JDK, Playwright Agent CLI (@playwright/cli), browser " +
            "drivers, k6 load generator, and (optionally) a project's folder layout.",
            schema(json)
                .optional("project", "string", "Project to health-check (optional).")
                .build()
        );

        // -----------------------------------------------------------
        // Performance Studio (k6) — Phase 1: export / run / validate / report
        // -----------------------------------------------------------
        addTool(
            arr,
            "ingenious_perf_export",
            "Generate a k6 load-test script from a test case or a HAR recording. type=http " +
            "(default) emits a protocol-level script from API steps or HAR entries; type=browser " +
            "emits a k6/browser script from web steps + Object Repository locators (test cases " +
            "only). Writes <project>/Performance/scripts/<name>.js with the load profile baked " +
            "into the options block. Refuses to overwrite hand-edited scripts unless force=true. " +
            "Unsupported actions become // TODO comments plus warnings (never dropped silently).",
            schema(json)
                .required(
                    "target",
                    "string",
                    "<Project>/<Scenario>/<TestCase> (test case) or an absolute .har file path."
                )
                .optional("type", "string", "http (default) or browser.")
                .optional("project", "string", "Project for .har exports (script destination).")
                .optional(
                    "profile",
                    "string",
                    "Load profile: smoke (default), average, stress, spike, soak, or a project profile."
                )
                .optional("urlFilter", "string", "HAR only: keep requests whose URL contains this.")
                .optional(
                    "includeStatic",
                    "boolean",
                    "HAR only: keep static assets (default false)."
                )
                .optional(
                    "autoCorrelate",
                    "boolean",
                    "HAR only: propose correlation rules (response tokens reappearing in later " +
                    "requests), persist them to Performance/rules/<script>.rules.yaml and apply " +
                    "them. Existing rules files are always applied automatically."
                )
                .optional("force", "boolean", "Overwrite a hand-edited script (default false).")
                .build()
        );
        addTool(
            arr,
            "ingenious_perf_run",
            "Execute a k6 script (load run) with the preinstalled k6 binary. Blocks until the " +
            "run finishes; results land in Results/Performance/<script>/<timestamp>/ " +
            "(summary.json + run.json). Returns headline metrics, threshold pass/fail and the " +
            "output tail. Exit code 99 = thresholds failed.",
            schema(json)
                .required(
                    "script",
                    "string",
                    "Script name (Performance/scripts) or absolute .js path."
                )
                .optional("project", "string", "Project name or absolute path.")
                .optional("vus", "number", "Override: number of virtual users.")
                .optional("duration", "string", "Override: duration, e.g. 30s or 2m.")
                .build()
        );
        addTool(
            arr,
            "ingenious_perf_validate",
            "Debug-run a k6 script: 1 VU, 1 iteration, full HTTP request/response trace " +
            "(k6-studio's Validator). Always do this before a load run. Returns the trace tail, " +
            "check results and headline metrics.",
            schema(json)
                .required(
                    "script",
                    "string",
                    "Script name (Performance/scripts) or absolute .js path."
                )
                .optional("project", "string", "Project name or absolute path.")
                .build()
        );
        addTool(
            arr,
            "ingenious_perf_report",
            "Read persisted performance runs from Results/Performance/. mode=latest (default) " +
            "returns the newest run's metadata, headline metrics and thresholds; mode=history " +
            "lists all runs.",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .optional("mode", "string", "latest (default) or history.")
                .build()
        );
        addTool(
            arr,
            "ingenious_perf_record_start",
            "Start recording browser traffic to a HAR file (Playwright context capture, no " +
            "proxy). Opens chromium at the URL; interact (or let the flow run), then call " +
            "ingenious_perf_record_stop with the returned recordingId. The HAR lands in " +
            "<project>/Performance/recordings/ and feeds ingenious_perf_export.",
            schema(json)
                .required("url", "string", "Start URL to open and record.")
                .optional(
                    "project",
                    "string",
                    "Project name or absolute path (recording destination)."
                )
                .optional("headless", "boolean", "Record without a visible window (default false).")
                .build()
        );
        addTool(
            arr,
            "ingenious_perf_record_stop",
            "Stop a running HAR recording (flushes and returns the .har path, ready for " +
            "ingenious_perf_export).",
            schema(json)
                .required("recordingId", "string", "Id returned by ingenious_perf_record_start.")
                .build()
        );
        addTool(
            arr,
            "ingenious_perf_run_async",
            "Start a k6 load run in the background and return immediately with a runId. " +
            "Enables the k6 REST API (live metrics via ingenious_perf_status) and, when " +
            "dashboard=true, the k6 web dashboard (live graphs in a browser + report.html " +
            "export at run end). Control the run with perf_status / perf_logs / perf_scale / " +
            "perf_cancel.",
            schema(json)
                .required(
                    "script",
                    "string",
                    "Script name (Performance/scripts) or absolute .js path."
                )
                .optional("project", "string", "Project name or absolute path.")
                .optional("vus", "number", "Override: number of virtual users.")
                .optional("duration", "string", "Override: duration, e.g. 30s or 2m.")
                .optional("dashboard", "boolean", "Enable the k6 web dashboard (default true).")
                .build()
        );
        addTool(
            arr,
            "ingenious_perf_status",
            "Status of an async k6 run: RUNNING with a live metrics snapshot (vus, rps, p95, " +
            "error rate) polled from the k6 REST API, or FINISHED with the persisted summary + " +
            "thresholds. Omit runId for the newest running test.",
            schema(json)
                .optional("runId", "string", "<script>/<timestamp> from ingenious_perf_run_async.")
                .optional("project", "string", "Project name or absolute path.")
                .build()
        );
        addTool(
            arr,
            "ingenious_perf_logs",
            "Tail the console output of an async k6 run.",
            schema(json)
                .optional("runId", "string", "Run id; default: newest running.")
                .optional("lines", "number", "Tail this many lines (default 40).")
                .optional("project", "string", "Project name or absolute path.")
                .build()
        );
        addTool(
            arr,
            "ingenious_perf_cancel",
            "Stop an async k6 run: graceful stop via the k6 REST API, process kill as fallback.",
            schema(json)
                .optional("runId", "string", "Run id; default: newest running.")
                .optional("project", "string", "Project name or absolute path.")
                .build()
        );
        addTool(
            arr,
            "ingenious_perf_scale",
            "Change the VU count of a RUNNING k6 test via the REST API (interactive load " +
            "shaping). Some executors (e.g. ramping stages) reject external scaling.",
            schema(json)
                .required("vus", "number", "Target number of virtual users.")
                .optional("runId", "string", "Run id; default: newest running.")
                .optional("project", "string", "Project name or absolute path.")
                .build()
        );
        addTool(
            arr,
            "ingenious_perf_compare",
            "Compare two performance runs metric by metric (iterations, rps, error rate, " +
            "avg/p95/max) with % deltas, flagging latency/error regressions (>5%) and " +
            "thresholds that passed in the baseline but fail in the candidate. Use run ids " +
            "from ingenious_perf_report mode=history.",
            schema(json)
                .required("baseline", "string", "Baseline run id (<script>/<timestamp>).")
                .required("candidate", "string", "Candidate run id (<script>/<timestamp>).")
                .optional("project", "string", "Project name or absolute path.")
                .build()
        );

        // -----------------------------------------------------------
        // Playwright Agent CLI - live browser authoring (Phase 4)
        // Requires @playwright/cli (npm i -g @playwright/cli). All tools
        // degrade with a clear message when it is not installed.
        // -----------------------------------------------------------
        addTool(
            arr,
            "ingenious_browser_discover",
            "Deterministic entry point for browser-flow DISCOVERY: use this whenever the intent " +
            "is to work out a UI flow whose objects/locators are NOT yet in the Object Repository. " +
            "Opens a live @playwright/cli session at the URL, returns the first accessibility " +
            "snapshot plus a fixed discovery protocol, and pre-binds scenario/testcase/page so " +
            "ingenious_browser_session_save can translate the discovered locators into WebOR " +
            "objects and linked test steps. Requires @playwright/cli.",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .required("url", "string", "Start URL to open for discovery.")
                .required(
                    "prompt",
                    "string",
                    "The browser flow to discover, in any format (plain English, BDD, steps)."
                )
                .optional("scenario", "string", "Target scenario for the generated test case.")
                .optional("testcase", "string", "Target test case name to create on save.")
                .optional(
                    "page",
                    "string",
                    "Object Repository page for discovered locators (default derived from testcase)."
                )
                .optional(
                    "browser",
                    "string",
                    "chromium | firefox | webkit | chrome (default chromium)."
                )
                .optional("headed", "boolean", "Run headed (default false = headless).")
                .optional("reusable", "boolean", "Save as a reusable component (default false).")
                .optional("session", "string", "Session name (default derived from testcase).")
                .build()
        );

        addTool(
            arr,
            "ingenious_browser_session_start",
            "Start a named Playwright Agent CLI browser session and open a URL. Returns the " +
            "first accessibility snapshot with element refs (e.g. e21). Requires @playwright/cli.",
            schema(json)
                .required("name", "string", "Session name (used for subsequent commands).")
                .required("url", "string", "URL to open.")
                .optional(
                    "browser",
                    "string",
                    "chromium | firefox | webkit | chrome (default chromium)."
                )
                .optional("headed", "boolean", "Run headed (default false = headless).")
                .build()
        );

        addTool(
            arr,
            "ingenious_browser_session_do",
            "Run one Playwright CLI command in a session (e.g. 'click e21', 'fill e5 hello'). " +
            "The action is recorded as an INGenious step and the new page snapshot is returned.",
            schema(json)
                .required("name", "string", "Session name.")
                .required("command", "string", "Playwright CLI command, e.g. \"click e21\".")
                .build()
        );

        addTool(
            arr,
            "ingenious_browser_session_snapshot",
            "Return the current accessibility snapshot (ref'd element tree) for a session.",
            schema(json).required("name", "string", "Session name.").build()
        );

        addTool(
            arr,
            "ingenious_browser_session_save",
            "Flush the recorded steps of a session into an INGenious test case " +
            "(wrapped with OpenBrowser / CloseBrowser). Discovered locators are translated into " +
            "Object-Repository objects on 'page' and the steps are linked to them.",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .required("name", "string", "Session name.")
                .optional(
                    "scenario",
                    "string",
                    "Target scenario (defaults to the value bound by ingenious_browser_discover)."
                )
                .optional(
                    "testcase",
                    "string",
                    "Target test case name (defaults to the value bound by ingenious_browser_discover)."
                )
                .optional(
                    "page",
                    "string",
                    "Object Repository page for discovered locators (default derived from testcase)."
                )
                .optional("reusable", "boolean", "Create as a reusable component (default false).")
                .build()
        );

        addTool(
            arr,
            "ingenious_browser_session_close",
            "Close a Playwright Agent CLI session and discard its recording buffer.",
            schema(json).required("name", "string", "Session name.").build()
        );

        addTool(
            arr,
            "ingenious_browser_inspect",
            "Open a URL in a throwaway Playwright session, snapshot it, and return the ref'd " +
            "accessibility tree so an agent can pick a stable locator. Requires @playwright/cli.",
            schema(json)
                .required("url", "string", "URL to inspect.")
                .optional("describe", "string", "What element you're looking for (echoed back).")
                .optional("browser", "string", "chromium | firefox | webkit (default chromium).")
                .build()
        );

        addTool(
            arr,
            "ingenious_object_import_page",
            "Scrape a live URL's interactive elements (via a Playwright snapshot) into an Object " +
            "Repository page. Requires @playwright/cli.",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .required("url", "string", "URL to scrape.")
                .required("page", "string", "Object Repository page name to create/append.")
                .optional("browser", "string", "chromium | firefox | webkit (default chromium).")
                .build()
        );

        // -----------------------------------------------------------
        // Phase 3: archetype-driven generation
        // -----------------------------------------------------------
        addTool(
            arr,
            "ingenious_gen_list",
            "List the available test-case archetypes (templates) with their parameters.",
            schema(json)
                .optional("category", "string", "Filter by category (Browser/API/General).")
                .build()
        );

        addTool(
            arr,
            "ingenious_gen_testcase",
            "Generate a test case from an archetype (see ingenious_gen_list), substituting " +
            "${token} parameters. Unfilled tokens are left intact for later refinement.",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .required("archetype", "string", "Archetype name, e.g. browser-login, api-get.")
                .required("scenario", "string", "Target scenario (created if missing).")
                .required("testcase", "string", "Test case name to create.")
                .optional("params", "object", "Map of token name to value for the archetype.")
                .optional("reusable", "boolean", "Create as a reusable component (default false).")
                .optional(
                    "ifExists",
                    "string",
                    "When the test case exists: error (default) | skip | overwrite."
                )
                .optional(
                    "parameterize",
                    "boolean",
                    "After generating, externalise all hard-coded values into a data sheet " +
                    "(runs ingenious_testcase_parameterize mode=all; default false)."
                )
                .optional(
                    "sheet",
                    "string",
                    "Data sheet name used when parameterize=true (default: derived from the test case)."
                )
                .optional(
                    "dryRun",
                    "boolean",
                    "Preview only \u2013 report the generated steps without writing (default false)."
                )
                .build()
        );

        addTool(
            arr,
            "ingenious_data_generate",
            "Generate synthetic data rows into a sheet. Columns are typed (name, email, phone, " +
            "uuid, int, number, bool, date, city, word, sentence, firstname, lastname). No external deps.",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .required("sheet", "string", "Target data sheet (created if missing).")
                .required("rows", "integer", "Number of rows to generate.")
                .optionalArray(
                    "columns",
                    "Column specs: [{name, type}]. type defaults to 'word'.",
                    genColumnItemSchema(json)
                )
                .optional("env", "string", "Target environment ('all' or a name; default all).")
                .optional("seed", "integer", "Optional RNG seed for reproducible data.")
                .build()
        );

        addTool(
            arr,
            "ingenious_gen_from_openapi",
            "Generate one API test case per operation from an OpenAPI 3 spec (YAML or JSON).",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .required("file", "string", "Path to the OpenAPI spec (.yaml/.yml/.json).")
                .optional("scenario", "string", "Target scenario (default: API).")
                .optional(
                    "baseUrl",
                    "string",
                    "Base URL to prepend to paths (else the spec's first server)."
                )
                .optional("reusable", "boolean", "Create as reusable components (default false).")
                .build()
        );

        addTool(
            arr,
            "ingenious_gen_from_har",
            "Generate API test cases from a HAR capture (browser/proxy network export).",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .required("file", "string", "Path to the .har file.")
                .optional("scenario", "string", "Target scenario (default: Recorded).")
                .optional(
                    "urlFilter",
                    "string",
                    "Only include entries whose URL contains this substring."
                )
                .optional("reusable", "boolean", "Create as reusable components (default false).")
                .build()
        );

        // -----------------------------------------------------------
        // API collection-first workflow
        // -----------------------------------------------------------
        addTool(
            arr,
            "ingenious_apicollection_import",
            "Stage 1: ingest APIs (Postman/Bruno file or a curl command) into a persisted " +
            "API collection under api/collections/<name>.json. Does NOT create a test case.",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .required("name", "string", "Collection name to create/overwrite.")
                .optional(
                    "format",
                    "string",
                    "Source format: postman | bruno | curl (default: auto from file)."
                )
                .optional(
                    "file",
                    "string",
                    "Path to the Postman/Bruno collection file (for postman/bruno)."
                )
                .optional("curl", "string", "A curl command string (for format=curl).")
                .build()
        );
        addTool(
            arr,
            "ingenious_apicollection_list",
            "List persisted API collections in the project with their request counts.",
            schema(json).optional("project", "string", "Project name or absolute path.").build()
        );
        addTool(
            arr,
            "ingenious_apicollection_show",
            "Show a persisted API collection's requests (method, url, headers, body).",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .required("name", "string", "Collection name.")
                .build()
        );
        addTool(
            arr,
            "ingenious_apicollection_env_set",
            "Create/update an API environment (api/environments/<env>.json) with a base URL " +
            "and variables used by apicollection_run.",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .required("env", "string", "Environment name.")
                .optional("baseUrl", "string", "Base URL stored as variable 'baseUrl'.")
                .optional(
                    "vars",
                    "string",
                    "JSON object of extra variables, e.g. {\"token\":\"abc\"}."
                )
                .build()
        );
        addTool(
            arr,
            "ingenious_apicollection_run",
            "Stage 2: execute a collection's requests against an environment; capture status, " +
            "headers, latency and body into api/history/<run>.json. Hits live endpoints.",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .required("name", "string", "Collection name.")
                .optional("env", "string", "Environment name to resolve {{vars}} (optional).")
                .build()
        );
        addTool(
            arr,
            "ingenious_apicollection_request_run",
            "Stage 2: execute a single named request from a collection ad-hoc and return the " +
            "observed response.",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .required("name", "string", "Collection name.")
                .required("request", "string", "Request name within the collection.")
                .optional("env", "string", "Environment name to resolve {{vars}} (optional).")
                .build()
        );
        addTool(
            arr,
            "ingenious_apicollection_to_testcase",
            "Stage 3: promote a collection into an INGenious YAML test case (Webservice steps), " +
            "seeding assertResponseCode from the latest run when available.",
            schema(json)
                .optional("project", "string", "Project name or absolute path.")
                .required("name", "string", "Collection name.")
                .optional("scenario", "string", "Target scenario (default: the collection name).")
                .optional("testcase", "string", "Test case name (default: the collection name).")
                .optional("env", "string", "Environment name (used if a fresh run is needed).")
                .optional(
                    "reusable",
                    "boolean",
                    "Create under a reusable scenario (default false)."
                )
                .optional("ifExists", "string", "error | skip | overwrite (default error).")
                .optional("dryRun", "boolean", "Report what would be created without writing.")
                .build()
        );

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
            case "ingenious_project_list":
                return MCPServer.jsonContent(json, projectList(json, args));
            case "ingenious_project_info":
                return MCPServer.jsonContent(json, projectInfo(json, args));
            case "ingenious_scenario_list":
                return MCPServer.jsonContent(json, scenarioList(json, args));
            case "ingenious_scenario_create":
                return MCPServer.jsonContent(json, scenarioCreate(json, args));
            case "ingenious_testcase_list":
                return MCPServer.jsonContent(json, testCaseList(json, args));
            case "ingenious_testcase_show":
                return MCPServer.jsonContent(json, testCaseShow(json, args));
            case "ingenious_testcase_create":
                return MCPServer.jsonContent(json, testCaseCreate(json, args));
            case "ingenious_testcase_add_step":
                return MCPServer.jsonContent(json, testCaseAddStep(json, args));
            case "ingenious_testcase_delete":
                return MCPServer.jsonContent(json, testCaseDelete(json, args));
            case "ingenious_testset_list":
                return MCPServer.jsonContent(json, testSetList(json, args));
            case "ingenious_testset_show":
                return MCPServer.jsonContent(json, testSetShow(json, args));
            case "ingenious_action_list":
                return MCPServer.jsonContent(json, actionList(json, args));
            case "ingenious_action_search":
                return MCPServer.jsonContent(json, actionSearch(json, args));
            case "ingenious_action_info":
                return MCPServer.jsonContent(json, actionInfo(json, args));
            case "ingenious_action_categories":
                return MCPServer.jsonContent(json, actionCategories(json));
            case "ingenious_run":
                return MCPServer.jsonContent(json, runSync(json, args));
            case "ingenious_run_async":
                return MCPServer.jsonContent(json, runAsync(json, args));
            case "ingenious_run_status":
                return MCPServer.jsonContent(json, runStatus(json, args));
            case "ingenious_run_logs":
                return MCPServer.jsonContent(json, runLogs(json, args));
            case "ingenious_run_cancel":
                return MCPServer.jsonContent(json, runCancel(json, args));
            case "ingenious_report_latest":
                return MCPServer.jsonContent(json, reportLatest(json, args));
            case "ingenious_report_history":
                return MCPServer.jsonContent(json, reportHistory(json, args));
            case "ingenious_report_failures":
                return MCPServer.jsonContent(json, reportFailures(json, args));
            case "ingenious_config_get":
                return MCPServer.jsonContent(json, configGet(json, args));
            case "ingenious_config_set":
                return MCPServer.jsonContent(json, configSet(json, args));
            case "ingenious_project_create":
                return MCPServer.jsonContent(json, projectCreate(json, args));
            case "ingenious_data_sheet_create":
                return MCPServer.jsonContent(json, dataSheetCreate(json, args));
            case "ingenious_data_row_add":
                return MCPServer.jsonContent(json, dataRowAdd(json, args));
            case "ingenious_data_column_add":
                return MCPServer.jsonContent(json, dataColumnAdd(json, args));
            case "ingenious_env_list":
                return MCPServer.jsonContent(json, envList(json, args));
            case "ingenious_env_create":
                return MCPServer.jsonContent(json, envCreate(json, args));
            case "ingenious_env_delete":
                return MCPServer.jsonContent(json, envDelete(json, args));
            case "ingenious_import_curl":
                return MCPServer.jsonContent(json, importCurl(json, args));
            case "ingenious_import_postman":
                return MCPServer.jsonContent(json, importPostman(json, args));
            case "ingenious_import_bruno":
                return MCPServer.jsonContent(json, importBruno(json, args));
            case "ingenious_import_playwright":
                return MCPServer.jsonContent(json, importPlaywright(json, args));
            case "ingenious_scenario_info":
                return MCPServer.jsonContent(json, scenarioInfo(json, args));
            case "ingenious_scenario_delete":
                return MCPServer.jsonContent(json, scenarioDelete(json, args));
            case "ingenious_testcase_validate":
                return MCPServer.jsonContent(json, testCaseValidate(json, args));
            case "ingenious_testcase_parameterize":
                return MCPServer.jsonContent(json, testCaseParameterize(json, args));
            case "ingenious_testset_create":
                return MCPServer.jsonContent(json, testSetCreate(json, args));
            case "ingenious_testset_add":
                return MCPServer.jsonContent(json, testSetAdd(json, args));
            case "ingenious_object_list":
                return MCPServer.jsonContent(json, objectList(json, args));
            case "ingenious_object_show":
                return MCPServer.jsonContent(json, objectShow(json, args));
            case "ingenious_object_search":
                return MCPServer.jsonContent(json, objectSearch(json, args));
            case "ingenious_data_show":
                return MCPServer.jsonContent(json, dataShow(json, args));
            case "ingenious_data_get":
                return MCPServer.jsonContent(json, dataGet(json, args));
            case "ingenious_data_set":
                return MCPServer.jsonContent(json, dataSet(json, args));
            case "ingenious_report_show":
                return MCPServer.jsonContent(json, reportShow(json, args));
            case "ingenious_report_compare":
                return MCPServer.jsonContent(json, reportCompare(json, args));
            case "ingenious_config_show":
                return MCPServer.jsonContent(json, configShow(json, args));
            case "ingenious_testcase_edit_step":
                return MCPServer.jsonContent(json, testCaseEditStep(json, args));
            case "ingenious_testcase_insert_step":
                return MCPServer.jsonContent(json, testCaseInsertStep(json, args));
            case "ingenious_testcase_remove_step":
                return MCPServer.jsonContent(json, testCaseRemoveStep(json, args));
            case "ingenious_testcase_move_step":
                return MCPServer.jsonContent(json, testCaseMoveStep(json, args));
            case "ingenious_object_add":
                return MCPServer.jsonContent(json, objectAdd(json, args));
            case "ingenious_object_update":
                return MCPServer.jsonContent(json, objectUpdate(json, args));
            case "ingenious_object_delete":
                return MCPServer.jsonContent(json, objectDelete(json, args));
            case "ingenious_data_row_delete":
                return MCPServer.jsonContent(json, dataRowDelete(json, args));
            case "ingenious_data_import":
                return MCPServer.jsonContent(json, dataImport(json, args));
            case "ingenious_report_export":
                return MCPServer.jsonContent(json, reportExport(json, args));
            case "ingenious_config_drivers":
                return MCPServer.jsonContent(json, configDrivers(json, args));
            case "ingenious_run_dry":
                return MCPServer.jsonContent(json, runDry(json, args));
            case "ingenious_doctor":
                return MCPServer.jsonContent(json, doctor(json, args));
            case "ingenious_perf_export":
                return MCPServer.jsonContent(json, perfExport(json, args));
            case "ingenious_perf_run":
                return MCPServer.jsonContent(json, perfRun(json, args));
            case "ingenious_perf_validate":
                return MCPServer.jsonContent(json, perfValidate(json, args));
            case "ingenious_perf_report":
                return MCPServer.jsonContent(json, perfReport(json, args));
            case "ingenious_perf_record_start":
                return MCPServer.jsonContent(json, perfRecordStart(json, args));
            case "ingenious_perf_record_stop":
                return MCPServer.jsonContent(json, perfRecordStop(json, args));
            case "ingenious_perf_run_async":
                return MCPServer.jsonContent(json, perfRunAsync(json, args));
            case "ingenious_perf_status":
                return MCPServer.jsonContent(json, perfStatus(json, args));
            case "ingenious_perf_logs":
                return MCPServer.jsonContent(json, perfLogs(json, args));
            case "ingenious_perf_cancel":
                return MCPServer.jsonContent(json, perfCancel(json, args));
            case "ingenious_perf_scale":
                return MCPServer.jsonContent(json, perfScale(json, args));
            case "ingenious_perf_compare":
                return MCPServer.jsonContent(json, perfCompare(json, args));
            case "ingenious_browser_discover":
                return MCPServer.jsonContent(json, browserDiscover(json, args));
            case "ingenious_browser_session_start":
                return MCPServer.jsonContent(json, browserSessionStart(json, args));
            case "ingenious_browser_session_do":
                return MCPServer.jsonContent(json, browserSessionDo(json, args));
            case "ingenious_browser_session_snapshot":
                return MCPServer.jsonContent(json, browserSessionSnapshot(json, args));
            case "ingenious_browser_session_save":
                return MCPServer.jsonContent(json, browserSessionSave(json, args));
            case "ingenious_browser_session_close":
                return MCPServer.jsonContent(json, browserSessionClose(json, args));
            case "ingenious_browser_inspect":
                return MCPServer.jsonContent(json, browserInspect(json, args));
            case "ingenious_object_import_page":
                return MCPServer.jsonContent(json, objectImportPage(json, args));
            case "ingenious_gen_list":
                return MCPServer.jsonContent(json, genList(json, args));
            case "ingenious_gen_testcase":
                return MCPServer.jsonContent(json, genTestCase(json, args));
            case "ingenious_data_generate":
                return MCPServer.jsonContent(json, dataGenerate(json, args));
            case "ingenious_gen_from_openapi":
                return MCPServer.jsonContent(json, genFromOpenApi(json, args));
            case "ingenious_gen_from_har":
                return MCPServer.jsonContent(json, genFromHar(json, args));
            case "ingenious_apicollection_import":
                return MCPServer.jsonContent(json, apiCollectionImport(json, args));
            case "ingenious_apicollection_list":
                return MCPServer.jsonContent(json, apiCollectionList(json, args));
            case "ingenious_apicollection_show":
                return MCPServer.jsonContent(json, apiCollectionShow(json, args));
            case "ingenious_apicollection_env_set":
                return MCPServer.jsonContent(json, apiCollectionEnvSet(json, args));
            case "ingenious_apicollection_run":
                return MCPServer.jsonContent(json, apiCollectionRun(json, args));
            case "ingenious_apicollection_request_run":
                return MCPServer.jsonContent(json, apiCollectionRequestRun(json, args));
            case "ingenious_apicollection_to_testcase":
                return MCPServer.jsonContent(json, apiCollectionToTestcase(json, args));
            default:
                throw new MCPServer.MCPException(-32601, "Unknown tool: " + name);
        }
    }

    // ==================================================================
    // project tools
    // ==================================================================

    private JsonNode projectList(ObjectMapper json, JsonNode args) {
        String basePath = MCPServer.paramOrDefault(
            args,
            "basePath",
            System.getProperty("user.dir") + File.separator + "Projects"
        );
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
        out.put("name", project.getName());
        out.put("location", project.getLocation());
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
            n.put("name", s.getName());
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
            return json
                .createObjectNode()
                .put("created", false)
                .put("scenario", name)
                .put("reusable", reusable)
                .put("message", (reusable ? "Reusable scenario" : "Scenario") + " already exists");
        }
        Scenario created = reusable ? p.addReusableScenario(name) : p.addScenario(name);
        if (created == null) {
            throw new MCPServer.MCPException(-32603, "Failed to create scenario: " + name);
        }
        new File(created.getLocation()).mkdirs();
        p.save();
        return json
            .createObjectNode()
            .put("created", true)
            .put("scenario", name)
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
                n.put("steps", tc.getTestSteps().size());
            }
        }
        return out;
    }

    private JsonNode testCaseShow(ObjectMapper json, JsonNode args) {
        Project p = loadProject(resolveProject(projectArg(args)));
        String scenName = MCPServer.requiredParam(args, "scenario");
        String tcName = MCPServer.requiredParam(args, "testcase");
        Scenario s = p.getScenarioByName(scenName);
        if (s == null) throw notFound(
            -32602,
            "Scenario not found: " + scenName,
            scenarioNames(p),
            scenName
        );
        TestCase tc = s.getTestCaseByName(tcName);
        if (tc == null) throw notFound(
            -32602,
            "Test case not found: " + tcName,
            testCaseNames(s),
            tcName
        );
        ensureLoaded(tc);

        ObjectNode out = json.createObjectNode();
        out.put("project", p.getName());
        out.put("scenario", s.getName());
        out.put("testcase", tc.getName());
        ArrayNode steps = out.putArray("steps");
        int i = 1;
        for (TestStep st : tc.getTestSteps()) {
            ObjectNode step = steps.addObject();
            step.put("step", i++);
            step.put("action", st.getAction());
            step.put("object", st.getObject());
            step.put("input", st.getInput());
            step.put("condition", st.getCondition());
            step.put("description", st.getDescription());
            step.put("reference", st.getReference());
        }
        return out;
    }

    private JsonNode testCaseCreate(ObjectMapper json, JsonNode args) {
        Project p = loadProject(resolveProject(projectArg(args)));
        String scenName = MCPServer.requiredParam(args, "scenario");
        String tcName = MCPServer.requiredParam(args, "testcase");
        boolean reusable = boolArg(args, "reusable", false);
        boolean dryRun = boolArg(args, "dryRun", false);
        String ifExists = MCPServer
            .paramOrDefault(args, "ifExists", "error")
            .toLowerCase(Locale.ROOT);
        String explicitFormat = MCPServer.paramOrDefault(args, "format", null);

        JsonNode reqSteps = args == null ? null : args.get("steps");
        int reqStepCount = (reqSteps != null && reqSteps.isArray()) ? reqSteps.size() : 0;

        Scenario s = reusable
            ? p.getReusableScenarioByName(scenName)
            : p.getScenarioByName(scenName);
        boolean scenarioExists = s != null;
        TestCase existing = s != null ? s.getTestCaseByName(tcName) : null;

        if (dryRun) {
            return json
                .createObjectNode()
                .put("dryRun", true)
                .put("wouldCreate", existing == null)
                .put("scenario", scenName)
                .put("scenarioExists", scenarioExists)
                .put("testcase", tcName)
                .put("testcaseExists", existing != null)
                .put("reusable", reusable)
                .put("steps", reqStepCount);
        }

        if (s == null) {
            s = reusable ? p.addReusableScenario(scenName) : p.addScenario(scenName);
            if (s == null) throw new MCPServer.MCPException(
                -32603,
                "Failed to create scenario: " + scenName
            );
            new File(s.getLocation()).mkdirs();
        }
        existing = s.getTestCaseByName(tcName);
        if (existing != null) {
            switch (ifExists) {
                case "skip":
                    ensureLoaded(existing);
                    return json
                        .createObjectNode()
                        .put("created", false)
                        .put("existing", true)
                        .put("scenario", scenName)
                        .put("testcase", tcName)
                        .put("reusable", reusable)
                        .put("steps", existing.getTestSteps().size());
                case "overwrite":
                    File old = new File(existing.getLocation());
                    if (old.exists()) old.delete();
                    s.getTestCases().remove(existing);
                    break;
                case "error":
                default:
                    throw new MCPServer.MCPException(
                        -32602,
                        "Test case already exists: " +
                        tcName +
                        " (pass ifExists=skip|overwrite to change this)"
                    );
            }
        }

        // Honour explicit format if given, else prefer YAML by default (the
        // project default), but if the scenario already has CSV siblings keep
        // CSV so the IDE and `ingenious run` stay consistent.
        String originalFormat = null;
        try {
            originalFormat = p.getInfo().getTestCaseFormat();
        } catch (Exception ignored) {}
        String chosenFormat = explicitFormat != null && !explicitFormat.isEmpty()
            ? explicitFormat.toUpperCase(Locale.ROOT)
            : detectScenarioFormatPreferYaml(s);
        try {
            p.getInfo().setTestCaseFormat(chosenFormat);
        } catch (Exception ignored) {}

        TestCase tc = s.addTestCase(tcName);
        if (tc == null) {
            try {
                p.getInfo().setTestCaseFormat(originalFormat);
            } catch (Exception ignored) {}
            throw new MCPServer.MCPException(-32603, "Failed to create test case: " + tcName);
        }

        List<String> stepWarnings = new ArrayList<>();
        int literalCount = 0;
        try {
            if (reqSteps != null && reqSteps.isArray()) {
                for (JsonNode raw : reqSteps) {
                    if (appendStep(tc, raw, stepWarnings)) literalCount++;
                }
            }
            tc.save();
        } finally {
            try {
                p.getInfo().setTestCaseFormat(originalFormat);
            } catch (Exception ignored) {}
        }
        ObjectNode out = json
            .createObjectNode()
            .put("created", true)
            .put("scenario", scenName)
            .put("testcase", tcName)
            .put("reusable", reusable)
            .put("format", chosenFormat)
            .put("steps", tc.getTestSteps().size());
        String w1 = StepNormalizer.literalSummary(literalCount);
        if (w1 != null) stepWarnings.add(w1);
        if (!stepWarnings.isEmpty()) {
            ArrayNode wa = out.putArray("warnings");
            for (String w : stepWarnings) wa.add(w);
        }
        return out;
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
                        if (n.endsWith(".csv")) hasCsv = true; else if (
                            n.endsWith(".yaml") || n.endsWith(".yml")
                        ) hasYaml = true;
                    }
                }
                if (hasCsv) return "CSV";
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
                        if (n.endsWith(".csv")) hasCsv = true; else if (
                            n.endsWith(".yaml") || n.endsWith(".yml")
                        ) hasYaml = true;
                    }
                }
                if (hasYaml) return "YAML";
                if (hasCsv) return "CSV";
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
        if (n.isTextual()) return Boolean.parseBoolean(n.asText());
        return def;
    }

    private JsonNode testCaseAddStep(ObjectMapper json, JsonNode args) {
        Project p = loadProject(resolveProject(projectArg(args)));
        String scenName = MCPServer.requiredParam(args, "scenario");
        String tcName = MCPServer.requiredParam(args, "testcase");
        Scenario s = p.getScenarioByName(scenName);
        if (s == null) throw new MCPServer.MCPException(-32602, "Scenario not found: " + scenName);
        TestCase tc = s.getTestCaseByName(tcName);
        if (tc == null) throw new MCPServer.MCPException(-32602, "Test case not found: " + tcName);
        ensureLoaded(tc);

        List<String> stepWarnings = new ArrayList<>();
        boolean literal = appendStep(tc, args, stepWarnings);
        tc.save();
        String w1 = StepNormalizer.literalSummary(literal ? 1 : 0);
        if (w1 != null) stepWarnings.add(w1);
        ObjectNode out = json
            .createObjectNode()
            .put("added", true)
            .put("totalSteps", tc.getTestSteps().size());
        if (!stepWarnings.isEmpty()) {
            ArrayNode wa = out.putArray("warnings");
            for (String w : stepWarnings) wa.add(w);
        }
        return out;
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

    /**
     * Appends one step, routing the input through {@link StepNormalizer} so
     * every write path follows the ConventionCatalog grammar. Convention
     * warnings are appended to {@code warnings}; returns {@code true} when the
     * stored input is a hard-coded {@code @literal} (used for the W1 summary).
     */
    private boolean appendStep(TestCase tc, JsonNode raw, List<String> warnings) {
        TestStep step = tc.addNewStep();
        String action = MCPServer.paramOrDefault(raw, "action", "");
        String object = MCPServer.paramOrDefault(raw, "object", "");
        String input = MCPServer.paramOrDefault(raw, "input", "");
        String condition = MCPServer.paramOrDefault(raw, "condition", "");
        StepNormalizer.Result norm = StepNormalizer.normalize(
            "step " + tc.getTestSteps().size(),
            action,
            object,
            input,
            condition
        );
        if (!norm.errors.isEmpty()) {
            throw new MCPServer.MCPException(-32602, String.join("; ", norm.errors));
        }
        warnings.addAll(norm.warnings);
        step.setAction(action);
        step.setObject(object);
        step.setInput(norm.input);
        step.setCondition(norm.condition);
        step.setDescription(MCPServer.paramOrDefault(raw, "description", ""));
        step.setReference(MCPServer.paramOrDefault(raw, "reference", ""));
        return ConventionCatalog.isParameterizableLiteral(norm.input);
    }

    private JsonNode testCaseDelete(ObjectMapper json, JsonNode args) {
        File dir = resolveProject(projectArg(args));
        String scenName = MCPServer.requiredParam(args, "scenario");
        String tcName = MCPServer.requiredParam(args, "testcase");
        File scenDir = new File(new File(dir, Project.TEST_PLAN_DIR), scenName);
        // Datalib persists test cases as either .yaml (default) or .csv.
        // Probe both, in preferred order, and fall back to a directory scan
        // so renamed/legacy extensions still match.
        File target = null;
        for (String ext : new String[] { ".yaml", ".yml", ".csv" }) {
            File f = new File(scenDir, tcName + ext);
            if (f.isFile()) {
                target = f;
                break;
            }
        }
        if (target == null && scenDir.isDirectory()) {
            File[] matches = scenDir.listFiles(
                f ->
                    f.isFile() &&
                    f.getName().regionMatches(true, 0, tcName + ".", 0, tcName.length() + 1)
            );
            if (matches != null && matches.length > 0) target = matches[0];
        }
        if (target == null) {
            throw new MCPServer.MCPException(-32602, "Test case file not found under: " + scenDir);
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
                n.put("path", set.getAbsolutePath());
            }
        }
        return out;
    }

    private JsonNode testSetShow(ObjectMapper json, JsonNode args) {
        File dir = resolveProject(projectArg(args));
        String rel = MCPServer.requiredParam(args, "release");
        String set = MCPServer.requiredParam(args, "testset");
        File csv = new File(new File(new File(dir, "TestLab"), rel), set + ".csv");
        if (!csv.isFile()) {
            throw new MCPServer.MCPException(-32602, "Test set not found: " + csv);
        }
        ObjectNode out = json.createObjectNode();
        out.put("release", rel);
        out.put("testset", set);
        ArrayNode rows = out.putArray("rows");
        try (
            BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(csv), StandardCharsets.UTF_8)
            )
        ) {
            String line;
            boolean header = true;
            int rowNum = 0;
            while ((line = br.readLine()) != null) {
                if (header) {
                    header = false;
                    continue;
                }
                if (line.trim().isEmpty()) continue;
                String[] cols = line.split(",", -1);
                ObjectNode r = rows.addObject();
                r.put("row", ++rowNum);
                r.put("scenario", cols.length > 0 ? cols[0] : "");
                r.put("testcase", cols.length > 1 ? cols[1] : "");
                r.put("browser", cols.length > 2 ? cols[2] : "");
                r.put("tags", cols.length > 3 ? cols[3] : "");
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
        String category = MCPServer.paramOrDefault(args, "category", null);
        ArrayNode out = json.createArrayNode();
        for (ActionCatalog.ActionInfo a : ActionCatalog.search(query)) {
            if (category != null && !category.isBlank()) {
                if (
                    !a.category.equalsIgnoreCase(category) &&
                    !a.objectType.equalsIgnoreCase(category)
                ) {
                    continue;
                }
            }
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
        n.put("name", a.name);
        n.put("category", a.category);
        n.put("objectType", a.objectType);
        n.put("description", a.description);
        n.put("inputRequired", a.inputRequired);
        n.put("conditionSupported", a.conditionSupported);
        // Format spec (from @Args / sidecar, or inferred) so callers author the
        // Input/Condition in the exact expected grammar.
        ArgSpec spec = ActionSpecCatalog.forAction(a.name);
        n.put("inputType", spec.inputType().name());
        n.put("inputTypeLabel", spec.inputType().label());
        if (!spec.inputExample().isEmpty()) n.put("inputExample", spec.inputExample());
        n.put("inputAllowsData", spec.inputAllowsData());
        n.put("conditionKind", spec.conditionKind().name());
        if (!spec.conditionValues().isEmpty()) {
            ArrayNode cv = n.putArray("conditionValues");
            for (String v : spec.conditionValues()) cv.add(v);
        }
        if (!spec.conditionExample().isEmpty()) n.put("conditionExample", spec.conditionExample());
        if (!spec.help().isEmpty()) n.put("help", spec.help());
        // Field-specific hints (what the Input and Condition columns each expect).
        if (!spec.inputHint().isEmpty()) n.put("inputHint", spec.inputHint());
        if (!spec.conditionHint().isEmpty()) n.put("conditionHint", spec.conditionHint());
        n.put("formatSpecified", spec.isExplicit());
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
        out.put("runId", h.id);
        out.put("target", spec.target);
        out.put("status", h.status);
        out.put("exitCode", h.exitCode);
        out.put("durationMs", h.endedAt - h.startedAt);
        out.put("command", String.join(" ", h.command));
        out.put("output", tail(h.output.toString(), 400));
        return out;
    }

    private JsonNode runAsync(ObjectMapper json, JsonNode args) {
        RunSpec spec = parseRunSpec(args);
        RunHandle h = startRun(spec);
        return json
            .createObjectNode()
            .put("runId", h.id)
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
            h.status = h.exitCode == 0 ? "PASS" : "FAIL";
            h.endedAt = System.currentTimeMillis();
        }
        ObjectNode n = json.createObjectNode();
        n.put("runId", h.id);
        n.put("target", h.spec.target);
        n.put("status", h.status);
        n.put("alive", h.process.isAlive());
        n.put("startedAt", h.startedAt);
        if (h.endedAt > 0) {
            n.put("endedAt", h.endedAt);
            n.put("durationMs", h.endedAt - h.startedAt);
            n.put("exitCode", h.exitCode);
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
        return json
            .createObjectNode()
            .put("runId", runId)
            .put("output", tailLines(h.output.toString(), tail));
    }

    private JsonNode runCancel(ObjectMapper json, JsonNode args) {
        String runId = MCPServer.requiredParam(args, "runId");
        RunHandle h = runs.get(runId);
        if (h == null) throw new MCPServer.MCPException(-32602, "Unknown runId: " + runId);
        if (h.process.isAlive()) {
            h.process.destroy();
            try {
                h.process.waitFor(5, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {}
            if (h.process.isAlive()) h.process.destroyForcibly();
            h.status = "CANCELLED";
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
        if (spec.browser != null) {
            cmd.add("-b");
            cmd.add(spec.browser);
        }
        if (spec.headless) {
            cmd.add("--headless");
        }
        if (spec.parallel > 1) {
            cmd.add("--parallel");
            cmd.add(Integer.toString(spec.parallel));
        }
        if (spec.tags != null && !spec.tags.isEmpty()) {
            cmd.add("-t");
            cmd.add(spec.tags);
        }
        if (spec.rerun) {
            cmd.add("--rerun");
        }

        ProcessBuilder pb = new ProcessBuilder(cmd).redirectErrorStream(true);
        pb.directory(new File(System.getProperty("user.dir")));
        try {
            Process p = pb.start();
            RunHandle h = new RunHandle();
            h.id = "run-" + UUID.randomUUID().toString().substring(0, 8);
            h.spec = spec;
            h.command = cmd;
            h.process = p;
            h.startedAt = System.currentTimeMillis();
            h.status = "RUNNING";
            runs.put(h.id, h);

            // pump output asynchronously
            Thread t = new Thread(
                () -> {
                    try (
                        BufferedReader r = new BufferedReader(
                            new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8)
                        )
                    ) {
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
                    } catch (IOException ignored) {}
                },
                "mcp-run-pump-" + h.id
            );
            t.setDaemon(true);
            t.start();
            return h;
        } catch (IOException e) {
            throw new MCPServer.MCPException(
                -32603,
                "Failed to start subprocess: " + e.getMessage()
            );
        }
    }

    private RunSpec parseRunSpec(JsonNode args) {
        RunSpec s = new RunSpec();
        s.target = MCPServer.requiredParam(args, "target");
        s.browser =
            com.ing.engine.cli.lib.BrowserNames.normalize(
                MCPServer.paramOrDefault(args, "browser", null)
            );
        s.tags = MCPServer.paramOrDefault(args, "tags", null);
        s.headless = false;
        s.parallel = 1;
        s.rerun = boolArg(args, "rerun", false);
        if (args != null) {
            JsonNode h = args.get("headless");
            if (h != null && h.isBoolean()) s.headless = h.asBoolean();
            JsonNode p = args.get("parallel");
            if (p != null && p.isInt()) s.parallel = p.asInt();
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
            n.put("runId", entries[i].getName());
            n.put("modified", entries[i].lastModified());
            n.put("path", entries[i].getAbsolutePath());
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
            throw new MCPServer.MCPException(
                -32602,
                "target must be '<Scenario>/<TestCase>' or '<Release>/<TestSet>'"
            );
        }
        File design = new File(project, "Results/TestDesign/" + parts[0] + "/" + parts[1]);
        File exec = new File(project, "Results/TestExecution/" + parts[0] + "/" + parts[1]);
        if (design.isDirectory()) return design;
        if (exec.isDirectory()) return exec;
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
        if (!js.isFile()) throw new MCPServer.MCPException(
            -32602,
            "data.js not found in: " + latest
        );
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
        if (!f.isFile()) throw new MCPServer.MCPException(
            -32602,
            "Configuration file not found: " + f
        );
        Properties props = new Properties();
        try (FileInputStream is = new FileInputStream(f)) {
            props.load(is);
        } catch (IOException e) {
            throw new MCPServer.MCPException(-32603, "Failed to read config: " + e.getMessage());
        }
        String key = MCPServer.paramOrDefault(args, "key", null);
        ObjectNode out = json.createObjectNode();
        if (key != null) {
            out.put("key", key);
            out.put("value", props.getProperty(key, ""));
            return out;
        }
        Set<String> keys = new LinkedHashSet<>(props.stringPropertyNames());
        for (String k : keys) out.put(k, props.getProperty(k));
        return out;
    }

    private JsonNode configSet(ObjectMapper json, JsonNode args) {
        File f = configFile(args);
        String k = MCPServer.requiredParam(args, "key");
        String v = MCPServer.requiredParam(args, "value");
        Properties props = new Properties();
        if (f.isFile()) {
            try (FileInputStream is = new FileInputStream(f)) {
                props.load(is);
            } catch (IOException e) {
                throw new MCPServer.MCPException(
                    -32603,
                    "Failed to read config: " + e.getMessage()
                );
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
        String parent = MCPServer.paramOrDefault(
            args,
            "parentDir",
            System.getProperty("user.dir") + File.separator + "Projects"
        );
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
        return json
            .createObjectNode()
            .put("created", true)
            .put("name", name)
            .put("location", projectDir.getAbsolutePath())
            .put("format", format);
    }

    // ==================================================================
    // data sheet / row / column
    // ==================================================================

    private JsonNode dataSheetCreate(ObjectMapper json, JsonNode args) {
        Project p = loadProject(resolveProject(projectArg(args)));
        String sheet = MCPServer.requiredParam(args, "sheet");
        String envName = MCPServer.paramOrDefault(args, "env", null);
        com.ing.datalib.component.EnvTestData env = p.getTestData();
        java.util.Collection<com.ing.datalib.component.TestData> targets = com.ing.engine.cli.commands.DataCommand.pickEnvs(
            env,
            envName
        );
        if (targets.isEmpty()) throw new MCPServer.MCPException(
            -32602,
            "Environment not found: " + envName
        );
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
        String tcName = MCPServer.requiredParam(args, "testcase");
        boolean reusable = boolArg(args, "reusable", false);
        String iter = MCPServer.paramOrDefault(args, "iteration", "1");
        String subIter = MCPServer.paramOrDefault(args, "subIteration", "1");
        String envName = MCPServer.paramOrDefault(args, "env", null);
        JsonNode colObj = args == null ? null : args.get("columns");

        com.ing.datalib.component.EnvTestData env = p.getTestData();
        java.util.Collection<com.ing.datalib.component.TestData> targets = com.ing.engine.cli.commands.DataCommand.pickEnvs(
            env,
            envName
        );
        if (targets.isEmpty()) throw new MCPServer.MCPException(
            -32602,
            "Environment not found: " + envName
        );
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
                    if (idx < 0) {
                        model.addColumn(k);
                        idx = model.getColumnIndex(k);
                    }
                    if (idx >= 0) model.setValueAt(v, row, idx);
                }
            }
            added++;
        }
        env.save();
        p.save();
        return json
            .createObjectNode()
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
        java.util.Collection<com.ing.datalib.component.TestData> targets = com.ing.engine.cli.commands.DataCommand.pickEnvs(
            env,
            envName
        );
        if (targets.isEmpty()) throw new MCPServer.MCPException(
            -32602,
            "Environment not found: " + envName
        );
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
        return json
            .createObjectNode()
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
            if (src == null) throw new MCPServer.MCPException(
                -32602,
                "Source env not found: " + from
            );
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
        return json
            .createObjectNode()
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
        String tcName = MCPServer.paramOrDefault(args, "testcase", null);
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
            throw new MCPServer.MCPException(
                -32602,
                "Test case already exists: " + scn.getName() + "/" + name
            );
        }
        TestCase tc = com.ing.engine.cli.lib.RequestToTestCaseBuilder.build(req, scn, name);
        p.save();
        if (tc == null) throw new MCPServer.MCPException(-32603, "Failed to build test case");
        return json
            .createObjectNode()
            .put("created", true)
            .put("scenario", scn.getName())
            .put("testcase", name)
            .put("reusable", reusable)
            .put("steps", tc.getTestSteps().size());
    }

    private JsonNode importPostman(ObjectMapper json, JsonNode args) {
        return importCollection(
            json,
            args,
            new com.ing.datalib.api.importer.postman.PostmanImporter(),
            "Postman"
        );
    }

    private JsonNode importBruno(ObjectMapper json, JsonNode args) {
        return importCollection(
            json,
            args,
            new com.ing.datalib.api.importer.bruno.BrunoImporter(),
            "Bruno"
        );
    }

    private JsonNode importCollection(
        ObjectMapper json,
        JsonNode args,
        com.ing.datalib.api.importer.spi.CollectionImporter importer,
        String label
    ) {
        Project p = loadProject(resolveProject(projectArg(args)));
        String filePath = MCPServer.requiredParam(args, "file");
        File file = new File(filePath);
        if (!file.exists()) throw new MCPServer.MCPException(
            -32602,
            label + " source not found: " + filePath
        );
        if (!importer.supports(file)) throw new MCPServer.MCPException(
            -32602,
            file.getName() + " is not recognised as a " + label + " source."
        );
        String scenName = MCPServer.paramOrDefault(args, "scenario", label);
        boolean reusable = boolArg(args, "reusable", false);
        String conflict = MCPServer
            .paramOrDefault(args, "conflict", "rename")
            .toLowerCase(Locale.ROOT);

        java.util.List<com.ing.datalib.api.importer.ImportWarning> warnings = new java.util.ArrayList<>();
        com.ing.datalib.api.importer.NormalizedCollection coll;
        try {
            coll = importer.parse(file, warnings);
        } catch (com.ing.datalib.api.importer.ImportException ie) {
            throw new MCPServer.MCPException(-32603, label + " parse failed: " + ie.getMessage());
        }
        Scenario scn = ensureScenario(p, scenName, reusable);
        int created = 0, skipped = 0, renamed = 0;
        for (com.ing.datalib.api.importer.NormalizedRequest nreq : coll.getRequests()) {
            if (nreq == null || nreq.getRequest() == null) continue;
            com.ing.datalib.api.APIRequest req = nreq.getRequest();
            String base = (req.getName() != null && !req.getName().isEmpty())
                ? req.getName()
                : deriveRequestName(req);
            String name = com.ing.datalib.api.importer.ImportUtils.sanitizeFileName(base);
            if (scn.getTestCaseByName(name) != null) {
                switch (conflict) {
                    case "skip":
                        skipped++;
                        continue;
                    case "overwrite":
                        {
                            TestCase old = scn.getTestCaseByName(name);
                            File f = new File(old.getLocation());
                            if (f.exists()) f.delete();
                            scn.getTestCases().remove(old);
                            break;
                        }
                    case "rename":
                    default:
                        {
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
        if (!file.isFile()) throw new MCPServer.MCPException(
            -32602,
            "Recording file not found: " + filePath
        );
        String scenName = MCPServer.paramOrDefault(args, "scenario", null);
        String tcName = MCPServer.paramOrDefault(args, "testcase", null);
        com.ing.datalib.api.importer.playwright.PlaywrightRecordingImporter.Result r;
        try {
            r =
                com.ing.datalib.api.importer.playwright.PlaywrightRecordingImporter.importInto(
                    p,
                    file,
                    scenName,
                    tcName
                );
        } catch (RuntimeException e) {
            throw new MCPServer.MCPException(-32603, "Playwright import failed: " + e.getMessage());
        }
        p.save();
        p.reload();
        if (r.stepCount == 0) throw new MCPServer.MCPException(
            -32603,
            "No recognised Playwright steps in: " + file.getName()
        );
        com.fasterxml.jackson.databind.node.ObjectNode out = json
            .createObjectNode()
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
            method + "_" + path.replace('/', '_')
        );
    }

    // ==================================================================
    // API collection-first workflow
    // ==================================================================

    private JsonNode apiCollectionImport(ObjectMapper json, JsonNode args) {
        File dir = resolveProject(projectArg(args));
        String name = MCPServer.requiredParam(args, "name");
        String format = MCPServer.paramOrDefault(args, "format", null);
        String filePath = MCPServer.paramOrDefault(args, "file", null);
        String curl = MCPServer.paramOrDefault(args, "curl", null);

        com.ing.datalib.api.APICollection collection;
        boolean curlMode =
            (format != null && format.equalsIgnoreCase("curl")) ||
            (curl != null && filePath == null);
        if (curlMode) {
            if (curl == null || !com.ing.datalib.api.CurlParser.looksLikeCurl(curl)) {
                throw new MCPServer.MCPException(
                    -32602,
                    "Provide a valid 'curl' command for format=curl."
                );
            }
            com.ing.datalib.api.APIRequest req = com.ing.datalib.api.CurlParser.parse(curl);
            if (req.getName() == null || req.getName().isEmpty()) req.setName(
                deriveRequestName(req)
            );
            collection = new com.ing.datalib.api.APICollection(name);
            java.util.List<com.ing.datalib.api.APIRequest> reqs = new java.util.ArrayList<>();
            reqs.add(req);
            collection.setRequests(reqs);
        } else {
            if (filePath == null) throw new MCPServer.MCPException(
                -32602,
                "Provide 'file' (Postman/Bruno) or 'curl'."
            );
            File file = new File(filePath);
            if (!file.exists()) throw new MCPServer.MCPException(
                -32602,
                "Source not found: " + filePath
            );
            com.ing.datalib.api.importer.spi.CollectionImporter importer = pickImporter(
                format,
                file
            );
            if (importer == null) throw new MCPServer.MCPException(
                -32602,
                "Unrecognised collection format for: " +
                file.getName() +
                " (use format=postman|bruno)."
            );
            java.util.List<com.ing.datalib.api.importer.ImportWarning> warnings = new java.util.ArrayList<>();
            com.ing.datalib.api.importer.NormalizedCollection nc;
            try {
                nc = importer.parse(file, warnings);
            } catch (com.ing.datalib.api.importer.ImportException ie) {
                throw new MCPServer.MCPException(-32603, "Parse failed: " + ie.getMessage());
            }
            collection = ApiCollectionStore.fromNormalized(nc, name);
        }
        ApiCollectionStore.saveCollection(dir, collection);
        ObjectNode out = json.createObjectNode();
        out.put("saved", true);
        out.put("collection", collection.getName());
        out.put("requests", collection.getRequests() == null ? 0 : collection.getRequests().size());
        out.put(
            "path",
            new File(
                ApiCollectionStore.collectionsDir(dir),
                ApiCollectionStore.sanitize(collection.getName()) + ".json"
            )
            .getPath()
        );
        return out;
    }

    private com.ing.datalib.api.importer.spi.CollectionImporter pickImporter(
        String format,
        File file
    ) {
        if (format != null) {
            if (
                format.equalsIgnoreCase("postman")
            ) return new com.ing.datalib.api.importer.postman.PostmanImporter();
            if (
                format.equalsIgnoreCase("bruno")
            ) return new com.ing.datalib.api.importer.bruno.BrunoImporter();
        }
        com.ing.datalib.api.importer.postman.PostmanImporter pm = new com.ing.datalib.api.importer.postman.PostmanImporter();
        if (pm.supports(file)) return pm;
        com.ing.datalib.api.importer.bruno.BrunoImporter br = new com.ing.datalib.api.importer.bruno.BrunoImporter();
        if (br.supports(file)) return br;
        return null;
    }

    private JsonNode apiCollectionList(ObjectMapper json, JsonNode args) {
        File dir = resolveProject(projectArg(args));
        ArrayNode out = json.createArrayNode();
        for (com.ing.datalib.api.APICollection c : ApiCollectionStore.listCollections(dir)) {
            ObjectNode n = out.addObject();
            n.put("name", c.getName());
            n.put("requests", c.getRequests() == null ? 0 : c.getRequests().size());
        }
        return out;
    }

    private JsonNode apiCollectionShow(ObjectMapper json, JsonNode args) {
        File dir = resolveProject(projectArg(args));
        String name = MCPServer.requiredParam(args, "name");
        com.ing.datalib.api.APICollection c = ApiCollectionStore.loadCollection(dir, name);
        if (c == null) throw new MCPServer.MCPException(-32602, "Collection not found: " + name);
        ObjectNode out = json.createObjectNode();
        out.put("name", c.getName());
        ArrayNode reqs = out.putArray("requests");
        if (c.getRequests() != null) {
            for (com.ing.datalib.api.APIRequest r : c.getRequests()) {
                ObjectNode rn = reqs.addObject();
                rn.put("name", r.getName());
                rn.put("method", r.getMethod() == null ? null : r.getMethod().name());
                rn.put("url", r.getUrl());
            }
        }
        return out;
    }

    private JsonNode apiCollectionEnvSet(ObjectMapper json, JsonNode args) {
        File dir = resolveProject(projectArg(args));
        String envName = MCPServer.requiredParam(args, "env");
        String baseUrl = MCPServer.paramOrDefault(args, "baseUrl", null);
        String varsJson = MCPServer.paramOrDefault(args, "vars", null);
        com.ing.datalib.api.APIEnvironment env = ApiCollectionStore.loadEnvironment(dir, envName);
        if (env == null) env = new com.ing.datalib.api.APIEnvironment(envName);
        if (baseUrl != null) env.setVariable("baseUrl", baseUrl);
        if (varsJson != null && !varsJson.isEmpty()) {
            try {
                JsonNode node = json.readTree(varsJson);
                java.util.Iterator<String> it = node.fieldNames();
                while (it.hasNext()) {
                    String k = it.next();
                    env.setVariable(k, node.get(k).asText());
                }
            } catch (Exception e) {
                throw new MCPServer.MCPException(-32602, "Invalid 'vars' JSON: " + e.getMessage());
            }
        }
        ApiCollectionStore.saveEnvironment(dir, env);
        return json.createObjectNode().put("saved", true).put("env", env.getName());
    }

    private JsonNode apiCollectionRun(ObjectMapper json, JsonNode args) {
        File dir = resolveProject(projectArg(args));
        String name = MCPServer.requiredParam(args, "name");
        String envName = MCPServer.paramOrDefault(args, "env", null);
        com.ing.datalib.api.APICollection c = ApiCollectionStore.loadCollection(dir, name);
        if (c == null) throw new MCPServer.MCPException(-32602, "Collection not found: " + name);
        java.util.Map<String, String> vars = resolveVars(dir, envName);
        ObjectNode out = json.createObjectNode();
        out.put("collection", c.getName());
        if (envName != null) out.put("env", envName);
        ArrayNode results = out.putArray("results");
        int passed = 0, failed = 0;
        if (c.getRequests() != null) {
            for (com.ing.datalib.api.APIRequest r : c.getRequests()) {
                com.ing.datalib.api.APIResponse resp = ApiCollectionStore.execute(r, vars);
                ObjectNode rn = results.addObject();
                rn.put("request", r.getName());
                rn.put("method", r.getMethod() == null ? null : r.getMethod().name());
                rn.put("status", resp.getStatusCode());
                rn.put("timeMs", resp.getResponseTimeMs());
                if (resp.isError()) {
                    rn.put("error", resp.getErrorMessage());
                    failed++;
                } else if (resp.getStatusCode() >= 200 && resp.getStatusCode() < 400) {
                    passed++;
                } else {
                    failed++;
                }
                rn.put("bodyPreview", preview(resp.getBody(), 500));
            }
        }
        out.put("passed", passed);
        out.put("failed", failed);
        String runId = writeRunArtifact(dir, c, out);
        if (runId != null) out.put("runId", runId);
        return out;
    }

    private JsonNode apiCollectionRequestRun(ObjectMapper json, JsonNode args) {
        File dir = resolveProject(projectArg(args));
        String name = MCPServer.requiredParam(args, "name");
        String reqName = MCPServer.requiredParam(args, "request");
        String envName = MCPServer.paramOrDefault(args, "env", null);
        com.ing.datalib.api.APICollection c = ApiCollectionStore.loadCollection(dir, name);
        if (c == null) throw new MCPServer.MCPException(-32602, "Collection not found: " + name);
        com.ing.datalib.api.APIRequest req = null;
        if (c.getRequests() != null) {
            for (com.ing.datalib.api.APIRequest r : c.getRequests()) {
                if (r.getName() != null && r.getName().equalsIgnoreCase(reqName)) {
                    req = r;
                    break;
                }
            }
        }
        if (req == null) throw new MCPServer.MCPException(
            -32602,
            "Request not found in collection: " + reqName
        );
        java.util.Map<String, String> vars = resolveVars(dir, envName);
        com.ing.datalib.api.APIResponse resp = ApiCollectionStore.execute(req, vars);
        ObjectNode out = json.createObjectNode();
        out.put("request", req.getName());
        out.put("status", resp.getStatusCode());
        out.put("timeMs", resp.getResponseTimeMs());
        if (resp.isError()) out.put("error", resp.getErrorMessage());
        out.put("body", preview(resp.getBody(), 4000));
        return out;
    }

    private JsonNode apiCollectionToTestcase(ObjectMapper json, JsonNode args) {
        File dir = resolveProject(projectArg(args));
        Project p = loadProject(dir);
        String name = MCPServer.requiredParam(args, "name");
        com.ing.datalib.api.APICollection c = ApiCollectionStore.loadCollection(dir, name);
        if (c == null) throw new MCPServer.MCPException(-32602, "Collection not found: " + name);
        String scenName = MCPServer.paramOrDefault(args, "scenario", name);
        String tcName = MCPServer.paramOrDefault(args, "testcase", name);
        String envName = MCPServer.paramOrDefault(args, "env", null);
        boolean reusable = boolArg(args, "reusable", false);
        String ifExists = MCPServer
            .paramOrDefault(args, "ifExists", "error")
            .toLowerCase(Locale.ROOT);
        boolean dryRun = boolArg(args, "dryRun", false);

        String finalName = com.ing.datalib.api.importer.ImportUtils.sanitizeFileName(tcName);
        int requests = c.getRequests() == null ? 0 : c.getRequests().size();
        Scenario scn = reusable
            ? p.getReusableScenarioByName(scenName)
            : p.getScenarioByName(scenName);
        TestCase existing = scn == null ? null : scn.getTestCaseByName(finalName);
        if (dryRun) {
            return json
                .createObjectNode()
                .put("dryRun", true)
                .put("wouldCreate", existing == null)
                .put("scenario", scenName)
                .put("testcase", finalName)
                .put("requests", requests);
        }
        scn = ensureScenario(p, scenName, reusable);
        existing = scn.getTestCaseByName(finalName);
        if (existing != null) {
            switch (ifExists) {
                case "skip":
                    return json
                        .createObjectNode()
                        .put("created", false)
                        .put("existing", true)
                        .put("scenario", scn.getName())
                        .put("testcase", finalName);
                case "overwrite":
                    File f = new File(existing.getLocation());
                    if (f.exists()) f.delete();
                    scn.getTestCases().remove(existing);
                    break;
                default:
                    throw new MCPServer.MCPException(
                        -32602,
                        "Test case already exists: " + finalName + " (pass ifExists=skip|overwrite)"
                    );
            }
        }
        java.util.Map<String, String> vars = resolveVars(dir, envName);
        TestCase tc = scn.addTestCase(finalName);
        if (tc == null) throw new MCPServer.MCPException(
            -32603,
            "Failed to create test case: " + finalName
        );
        int asserts = 0;
        if (c.getRequests() != null) {
            for (com.ing.datalib.api.APIRequest r : c.getRequests()) {
                com.ing.engine.cli.lib.RequestToTestCaseBuilder.appendSteps(tc, r);
                if (envName != null) {
                    com.ing.datalib.api.APIResponse resp = ApiCollectionStore.execute(r, vars);
                    if (!resp.isError() && resp.getStatusCode() > 0) {
                        TestStep st = tc.addNewStep();
                        st.setObject("Webservice");
                        st.setAction("assertResponseCode");
                        st.setDescription("Assert status for " + r.getName());
                        st.setInput("@" + resp.getStatusCode());
                        asserts++;
                    }
                }
            }
        }
        tc.save();
        p.save();
        ObjectNode out = json.createObjectNode();
        out.put("created", true);
        out.put("scenario", scn.getName());
        out.put("testcase", finalName);
        out.put("reusable", reusable);
        out.put("requests", requests);
        out.put("steps", tc.getTestSteps().size());
        out.put("assertionsSeeded", asserts);
        out.put("assertionSource", envName != null ? "observed-run" : "none");
        return out;
    }

    private java.util.Map<String, String> resolveVars(File dir, String envName) {
        java.util.Map<String, String> vars = new java.util.LinkedHashMap<>();
        if (envName != null) {
            com.ing.datalib.api.APIEnvironment env = ApiCollectionStore.loadEnvironment(
                dir,
                envName
            );
            if (env == null) throw new MCPServer.MCPException(
                -32602,
                "Environment not found: " + envName
            );
            if (env.getVariables() != null) vars.putAll(env.getVariables());
        }
        return vars;
    }

    private static String preview(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    private String writeRunArtifact(File dir, com.ing.datalib.api.APICollection c, ObjectNode run) {
        try {
            File hist = ApiCollectionStore.historyDir(dir);
            hist.mkdirs();
            String runId =
                ApiCollectionStore.sanitize(c.getName()) + "-" + System.currentTimeMillis();
            new ObjectMapper()
                .writerWithDefaultPrettyPrinter()
                .writeValue(new File(hist, runId + ".json"), run);
            return runId;
        } catch (Exception e) {
            return null;
        }
    }

    // ==================================================================
    // scenario info / delete
    // ==================================================================

    private JsonNode scenarioInfo(ObjectMapper json, JsonNode args) {
        Project p = loadProject(resolveProject(projectArg(args)));
        String name = MCPServer.requiredParam(args, "scenario");
        boolean reusable = boolArg(args, "reusable", false);
        Scenario s = reusable ? p.getReusableScenarioByName(name) : p.getScenarioByName(name);
        if (s == null) throw new MCPServer.MCPException(-32602, "Scenario not found: " + name);
        ObjectNode out = json.createObjectNode();
        out.put("project", p.getName());
        out.put("scenario", s.getName());
        out.put("reusable", reusable);
        out.put("location", s.getLocation());
        int total = 0;
        ArrayNode tcs = out.putArray("testCases");
        for (TestCase tc : s.getTestCases()) {
            ensureLoaded(tc);
            int steps = tc.getTestSteps().size();
            total += steps;
            ObjectNode n = tcs.addObject();
            n.put("name", tc.getName());
            n.put("steps", steps);
        }
        out.put("testCaseCount", s.getTestCases().size());
        out.put("totalSteps", total);
        return out;
    }

    private JsonNode scenarioDelete(ObjectMapper json, JsonNode args) {
        Project p = loadProject(resolveProject(projectArg(args)));
        String name = MCPServer.requiredParam(args, "scenario");
        boolean reusable = boolArg(args, "reusable", false);
        Scenario s = reusable ? p.getReusableScenarioByName(name) : p.getScenarioByName(name);
        if (s == null) throw new MCPServer.MCPException(-32602, "Scenario not found: " + name);
        File dir = new File(s.getLocation());
        deleteRecursively(dir);
        return json
            .createObjectNode()
            .put("deleted", true)
            .put("scenario", name)
            .put("reusable", reusable)
            .put("path", dir.getAbsolutePath());
    }

    // ==================================================================
    // test case validate
    // ==================================================================

    private JsonNode testCaseValidate(ObjectMapper json, JsonNode args) {
        Project p = loadProject(resolveProject(projectArg(args)));
        String scenName = MCPServer.paramOrDefault(args, "scenario", null);
        String tcName = MCPServer.paramOrDefault(args, "testcase", null);
        boolean reusable = boolArg(args, "reusable", false);
        ArrayNode errors = json.createArrayNode();
        ArrayNode warnings = json.createArrayNode();
        ArrayNode info = json.createArrayNode();
        int checked = 0;

        // E5 – scenario names must be unique across TestPlan/ and ReusableComponents/.
        Set<String> planNames = new LinkedHashSet<>();
        for (Scenario s : p.getScenarios()) planNames.add(s.getName());
        for (Scenario rs : p.getReusableScenarios()) {
            if (planNames.contains(rs.getName())) {
                errors.add(
                    "scenario '" +
                    rs.getName() +
                    "' exists in both TestPlan/ and ReusableComponents/ [E5]"
                );
            }
        }

        List<Scenario> scope = reusable ? p.getReusableScenarios() : p.getScenarios();
        Map<String, List<String>> tcStepKeys = new LinkedHashMap<>(); // for W10
        Set<String> genericNamesFlagged = new HashSet<>(); // for I1
        for (Scenario s : scope) {
            if (scenName != null && !s.getName().equals(scenName)) continue;
            for (TestCase tc : s.getTestCases()) {
                if (tcName != null && !tc.getName().equals(tcName)) continue;
                ensureLoaded(tc);
                checked++;
                String path = s.getName() + "/" + tc.getName();
                // I1 – generic business-flow / user-journey names.
                for (String nm : new String[] { s.getName(), tc.getName() }) {
                    if (isGenericName(nm) && genericNamesFlagged.add(nm)) {
                        info.add(
                            "'" +
                            nm +
                            "' is a generic name; use a business flow / user journey name [I1]"
                        );
                    }
                }
                List<TestStep> steps = tc.getTestSteps();
                if (steps.isEmpty()) {
                    warnings.add(path + ": no steps defined");
                    continue;
                }
                int literals = 0, noDescription = 0, noWait = 0;
                boolean hasAssertion = false, hasExecute = false;
                Set<String> waitedObjects = new HashSet<>();
                List<String> stepKeys = new ArrayList<>();
                int i = 0;
                for (TestStep st : steps) {
                    int n = ++i;
                    String where = path + " step " + n;
                    String action = safeTrim(st.getAction());
                    String object = safeTrim(st.getObject());
                    String input = st.getInput() == null ? "" : st.getInput();
                    String condition = st.getCondition() == null ? "" : st.getCondition();
                    String desc = safeTrim(st.getDescription());
                    String reference = safeTrim(st.getReference());

                    // E8 – missing action.
                    if (action.isEmpty()) {
                        errors.add(where + ": missing action [E8]");
                        continue;
                    }
                    // W8 – MANUAL markers carry the note in action only.
                    if (action.toUpperCase(Locale.ROOT).startsWith("MANUAL")) {
                        if (!object.isEmpty() || !input.isEmpty()) {
                            warnings.add(
                                where +
                                ": MANUAL marker must keep object/input empty so it " +
                                "renders as a marker [W8]"
                            );
                        }
                        continue;
                    }
                    if ("Execute".equalsIgnoreCase(object)) {
                        hasExecute = true;
                        // E3 – Execute grammar + reusable existence.
                        int colon = action.indexOf(':');
                        if (colon <= 0 || colon == action.length() - 1) {
                            errors.add(
                                where +
                                ": Execute step action must be " +
                                "'<ReusableScenario>:<ReusableName>' [E3]"
                            );
                        } else {
                            String rScen = action.substring(0, colon).trim();
                            String rName = action.substring(colon + 1).trim();
                            Scenario rs = p.getReusableScenarioByName(rScen);
                            TestCase rtc = rs == null ? null : rs.getTestCaseByName(rName);
                            if (rtc == null) {
                                warnings.add(
                                    where +
                                    ": reusable '" +
                                    action +
                                    "' not found in this project (may be shared) [E3]"
                                );
                            }
                        }
                    } else if (ActionCatalog.find(action) == null) {
                        // E1 – unknown action. Reported as a warning because user-defined
                        // custom actions are legal and not part of the built-in catalog.
                        warnings.add(
                            where +
                            ": action '" +
                            action +
                            "' is not a known built-in (custom action?) [E1]"
                        );
                    } else {
                        // E9 – input-requirement mismatch against the action catalog.
                        ActionCatalog.ActionInfo ai = ActionCatalog.find(action);
                        boolean hasInput = !input.trim().isEmpty();
                        if ("NO".equalsIgnoreCase(ai.inputRequired) && hasInput) {
                            String shown = input.trim();
                            if (shown.length() > 40) shown = shown.substring(0, 40) + "…";
                            errors.add(
                                where +
                                ": action '" +
                                action +
                                "' takes no input, but input '" +
                                shown +
                                "' was provided [E9]"
                            );
                        } else if ("YES".equalsIgnoreCase(ai.inputRequired) && !hasInput) {
                            errors.add(
                                where +
                                ": action '" +
                                action +
                                "' requires an input value but none was provided [E9]"
                            );
                        }
                    }
                    // E7 – @-prefixed object.
                    if (object.startsWith("@") && !ConventionCatalog.isEngineDirective(object)) {
                        errors.add(where + ": object '" + object + "' must not be @-prefixed [E7]");
                    }
                    // E6 – GlobalData id in input.
                    if (ConventionCatalog.isGlobalDataId(input)) {
                        errors.add(
                            where +
                            ": GlobalData id '" +
                            input +
                            "' in step input; use a data-sheet cell + Sheet:Column [E6]"
                        );
                    }
                    // E11 – input/condition format must match the action's spec.
                    for (String v : ActionSpecCatalog
                        .forAction(action)
                        .validate(input, condition)) {
                        errors.add(where + ": " + v + " [E11]");
                    }
                    // E2 – OR page reference must exist.
                    String refPage = referencedPage(reference);
                    if (refPage != null && !orPageExists(p, refPage)) {
                        errors.add(where + ": referenced OR page '" + refPage + "' not found [E2]");
                    }
                    // E4 – data references must resolve.
                    if (ConventionCatalog.isDataRef(input)) {
                        String[] sc = input.split(":", 2);
                        if (!dataRefExists(p, sc[0], sc[1])) {
                            errors.add(where + ": data reference '" + input + "' not found [E4]");
                        }
                    } else {
                        java.util.regex.Matcher m = ConventionCatalog.PAYLOAD_TOKEN.matcher(input);
                        while (m.find()) {
                            if (!dataRefExists(p, m.group(1), m.group(2))) {
                                errors.add(
                                    where +
                                    ": payload token '{" +
                                    m.group(1) +
                                    ":" +
                                    m.group(2) +
                                    "}' not found [E4]"
                                );
                            }
                        }
                    }
                    // W2 – fixed sleeps.
                    if ("pause".equalsIgnoreCase(action) && input.matches("@\\d+")) {
                        warnings.add(where + ": fixed sleep; prefer a waitFor* action [W2]");
                    }
                    // W7 – secrets stored as literals.
                    if (
                        ConventionCatalog.isParameterizableLiteral(input) &&
                        (object + " " + desc).toLowerCase(Locale.ROOT)
                            .matches(".*(password|passwd|pwd|secret).*")
                    ) {
                        warnings.add(where + ": possible secret stored as a literal value [W7]");
                    }
                    if (ConventionCatalog.isParameterizableLiteral(input)) literals++;
                    if (action.toLowerCase(Locale.ROOT).startsWith("assert")) hasAssertion = true;
                    if (desc.isEmpty()) noDescription++;
                    // W3 – interactions without a preceding wait on the element.
                    String lower = action.toLowerCase(Locale.ROOT);
                    if (lower.startsWith("waitfor")) waitedObjects.add(object);
                    if (
                        INTERACTION_ACTIONS.contains(lower) &&
                        !object.isEmpty() &&
                        !"Execute".equalsIgnoreCase(object) &&
                        !"Webservice".equalsIgnoreCase(object) &&
                        !"Browser".equalsIgnoreCase(object) &&
                        waitedObjects.add("used:" + object) &&
                        !waitedObjects.contains(object)
                    ) {
                        noWait++;
                    }
                    stepKeys.add(lower + "|" + object.toLowerCase(Locale.ROOT));
                }
                // W1 – aggregate literal note.
                if (literals > 0) {
                    warnings.add(
                        path +
                        ": " +
                        literals +
                        " hard-coded @literal input(s); externalise with " +
                        "ingenious_testcase_parameterize [W1]"
                    );
                }
                // W4 – no assertions (Execute steps may assert internally).
                if (!hasAssertion && !hasExecute) {
                    warnings.add(path + ": no assertion step [W4]");
                }
                // W9 – long raw test case with no reusables.
                if (steps.size() >= 15 && !hasExecute) {
                    warnings.add(
                        path +
                        ": " +
                        steps.size() +
                        " raw steps and no Execute steps; extract reusable components [W9]"
                    );
                }
                // W3 – interactions without waits.
                if (noWait > 0) {
                    warnings.add(
                        path +
                        ": " +
                        noWait +
                        " interaction step(s) without a preceding waitFor* on the element [W3]"
                    );
                }
                // I3 – missing descriptions.
                if (noDescription > 0) {
                    info.add(path + ": " + noDescription + " step(s) without description [I3]");
                }
                tcStepKeys.put(path, stepKeys);
            }
        }
        // W10 – identical 3-step sequences shared by several test cases suggest a reusable.
        if (tcName == null && tcStepKeys.size() > 1) {
            Map<String, Set<String>> windows = new LinkedHashMap<>();
            for (Map.Entry<String, List<String>> e : tcStepKeys.entrySet()) {
                List<String> keys = e.getValue();
                for (int w = 0; w + 3 <= keys.size(); w++) {
                    String window = keys.get(w) + ";" + keys.get(w + 1) + ";" + keys.get(w + 2);
                    if (window.contains("execute|")) continue; // already reusable calls
                    windows.computeIfAbsent(window, k -> new LinkedHashSet<>()).add(e.getKey());
                }
            }
            int reported = 0;
            Set<String> reportedGroups = new HashSet<>();
            for (Map.Entry<String, Set<String>> e : windows.entrySet()) {
                if (e.getValue().size() < 2 || reported >= 5) continue;
                String group = String.join(", ", e.getValue());
                if (!reportedGroups.add(group)) continue; // one report per test-case group
                warnings.add(
                    "test cases [" +
                    group +
                    "] share an identical step sequence; extract a reusable component [W10]"
                );
                reported++;
            }
        }
        if (checked == 0) throw new MCPServer.MCPException(
            -32602,
            "No matching test cases to validate."
        );
        ObjectNode out = json.createObjectNode();
        out.put("checked", checked);
        out.put("valid", errors.size() == 0);
        out.set("errors", errors);
        out.set("warnings", warnings);
        out.set("info", info);
        return out;
    }

    private static String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }

    /** Element-interaction actions checked by lint rule W3. */
    private static final Set<String> INTERACTION_ACTIONS = new HashSet<>(
        Arrays.asList(
            "click",
            "fill",
            "check",
            "uncheck",
            "set",
            "type",
            "doubleclick",
            "rightclick",
            "hover",
            "presssequentially",
            "selectsinglebytext",
            "selectsinglebyindex",
            "selectsinglebyvalue",
            "draganddrop"
        )
    );

    /** True when a scenario / test case name is too generic to be meaningful (I1). */
    private static boolean isGenericName(String name) {
        if (name == null) return false;
        return name
            .trim()
            .toLowerCase(Locale.ROOT)
            .matches(
                "(test|tests|flow|flows|scenario|scenarios|sample|demo|suite|tc|new)?[ _-]?\\d*"
            );
    }

    /**
     * Extracts the OR page name from a step {@code reference} such as
     * {@code "[Project] LoginPage"}. Returns {@code null} when the reference
     * carries no page (plain {@code "[Project]"} on Execute steps) or points
     * to a shared repository we cannot resolve here.
     */
    private static String referencedPage(String reference) {
        if (reference == null || reference.isEmpty()) return null;
        if (reference.startsWith("[Shared]")) return null;
        String page = reference.startsWith("[Project]")
            ? reference.substring("[Project]".length()).trim()
            : reference.trim();
        return page.isEmpty() ? null : page;
    }

    /** True when an OR page file exists anywhere under ObjectRepository/. */
    private boolean orPageExists(Project p, String page) {
        File orDir = new File(p.getLocation(), "ObjectRepository");
        if (!orDir.isDirectory()) return true; // no OR at all – don't flood errors
        return orPageExistsIn(orDir, page);
    }

    private static boolean orPageExistsIn(File dir, String page) {
        File[] files = dir.listFiles();
        if (files == null) return false;
        for (File f : files) {
            if (f.isDirectory()) {
                if (orPageExistsIn(f, page)) return true;
            } else {
                String n = f.getName();
                int dot = n.lastIndexOf('.');
                String base = dot > 0 ? n.substring(0, dot) : n;
                String ext = dot > 0 ? n.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
                if (
                    base.equals(page) &&
                    (ext.equals("csv") || ext.equals("yaml") || ext.equals("yml"))
                ) return true;
            }
        }
        return false;
    }

    /** True when {@code sheet}/{@code column} exists in any environment's test data. */
    private boolean dataRefExists(Project p, String sheet, String column) {
        try {
            com.ing.datalib.component.EnvTestData env = p.getTestData();
            for (String e : env.getEnvironments()) {
                com.ing.datalib.component.TestData td = env.getTestDataFor(e);
                if (td == null) continue;
                com.ing.datalib.testdata.model.TestDataModel model = td.getByName(sheet);
                if (model == null) continue;
                model.loadTableModel();
                if (model.getColumnIndex(column) >= 0) return true;
            }
            return false;
        } catch (Exception ex) {
            return true; // best-effort – never fail validation on IO trouble
        }
    }

    // ==================================================================
    // testcase parameterize (hard-coded values -> data sheet)
    // ==================================================================

    private JsonNode testCaseParameterize(ObjectMapper json, JsonNode args) {
        Project p = loadProject(resolveProject(projectArg(args)));
        String scenName = MCPServer.requiredParam(args, "scenario");
        String tcName = MCPServer.requiredParam(args, "testcase");
        boolean reusable = boolArg(args, "reusable", false);
        String mode = MCPServer.paramOrDefault(args, "mode", "scan").toLowerCase(Locale.ROOT);
        String iteration = MCPServer.paramOrDefault(args, "iteration", "1");
        String envName = MCPServer.paramOrDefault(args, "env", null);
        boolean dryRun = boolArg(args, "dryRun", false);

        Scenario s = reusable
            ? p.getReusableScenarioByName(scenName)
            : p.getScenarioByName(scenName);
        if (s == null) throw new MCPServer.MCPException(-32602, "Scenario not found: " + scenName);
        TestCase tc = s.getTestCaseByName(tcName);
        if (tc == null) throw new MCPServer.MCPException(-32602, "Test case not found: " + tcName);
        ensureLoaded(tc);

        String defaultSheet = MCPServer.paramOrDefault(args, "sheet", null);
        if (defaultSheet == null || defaultSheet.isEmpty()) defaultSheet = deriveSheetName(tcName);

        // ---- phase A: scan --------------------------------------------
        List<ObjectNode> candidates = scanParameterizationCandidates(json, tc, defaultSheet);

        if ("scan".equals(mode)) {
            ObjectNode out = json.createObjectNode();
            out.put("scenario", scenName).put("testcase", tcName).put("sheet", defaultSheet);
            ArrayNode ca = out.putArray("candidates");
            for (ObjectNode c : candidates) ca.add(c);
            out.put(
                "hint",
                candidates.isEmpty()
                    ? "Nothing to parameterize."
                    : "Re-run with mode=all, or mode=selected plus selections=[ids or " +
                    "{id, column?, paths?}] to apply."
            );
            return out;
        }

        // ---- phase B: resolve selections ------------------------------
        List<ObjectNode> chosen = new ArrayList<>();
        Map<Integer, JsonNode> overrides = new LinkedHashMap<>();
        if ("all".equals(mode)) {
            chosen.addAll(candidates);
        } else if ("selected".equals(mode)) {
            JsonNode sel = args.get("selections");
            if (sel == null || !sel.isArray() || sel.size() == 0) throw new MCPServer.MCPException(
                -32602,
                "mode=selected requires a non-empty selections array (run mode=scan first)."
            );
            Map<Integer, ObjectNode> byId = new LinkedHashMap<>();
            for (ObjectNode c : candidates) byId.put(c.get("id").asInt(), c);
            for (JsonNode e : sel) {
                int id = e.isInt() ? e.asInt() : e.path("id").asInt(-1);
                ObjectNode c = byId.get(id);
                if (c == null) throw new MCPServer.MCPException(
                    -32602,
                    "Unknown candidate id in selections: " + id + " (run mode=scan first)."
                );
                chosen.add(c);
                if (e.isObject()) overrides.put(id, e);
            }
        } else {
            throw new MCPServer.MCPException(-32602, "mode must be scan | all | selected.");
        }
        if (chosen.isEmpty()) throw new MCPServer.MCPException(-32602, "Nothing selected.");

        // ---- build the plan: sheet -> column -> value ------------------
        // and per-step replacement instructions.
        Map<String, Map<String, String>> sheetValues = new LinkedHashMap<>();
        List<ObjectNode> replacements = new ArrayList<>();
        Set<String> usedColumns = new HashSet<>();
        for (ObjectNode c : chosen) {
            int id = c.get("id").asInt();
            int stepIdx = c.get("step").asInt();
            JsonNode ov = overrides.get(id);
            String sheet = ov != null && ov.hasNonNull("sheet")
                ? ov.get("sheet").asText()
                : c.get("sheet").asText();
            if ("input".equals(c.get("kind").asText())) {
                String column = ov != null && ov.hasNonNull("column")
                    ? ov.get("column").asText()
                    : c.get("column").asText();
                column = uniqueColumn(sheet, column, usedColumns);
                String value = c.get("value").asText();
                if (value.startsWith("@")) value = value.substring(1);
                sheetValues.computeIfAbsent(sheet, k -> new LinkedHashMap<>()).put(column, value);
                ObjectNode r = json.createObjectNode();
                r.put("step", stepIdx).put("kind", "input");
                r.put("sheet", sheet).put("column", column);
                replacements.add(r);
            } else {
                // payload: default = all fields; overrides may narrow via paths[].
                Map<String, String> pathColumns = new LinkedHashMap<>();
                if (ov != null && ov.has("paths") && ov.get("paths").isArray()) {
                    for (JsonNode pe : ov.get("paths")) {
                        if (pe.isTextual()) pathColumns.put(pe.asText(), null); else if (
                            pe.isObject() && pe.hasNonNull("path")
                        ) pathColumns.put(
                            pe.get("path").asText(),
                            pe.hasNonNull("column") ? pe.get("column").asText() : null
                        );
                    }
                }
                for (JsonNode f : c.withArray("fields")) {
                    String path = f.get("path").asText();
                    String columnOverride = null;
                    if (!pathColumns.isEmpty()) {
                        if (!pathColumns.containsKey(path)) continue;
                        columnOverride = pathColumns.get(path);
                    }
                    String column = columnOverride != null
                        ? columnOverride
                        : f.get("column").asText();
                    column = uniqueColumn(sheet, column, usedColumns);
                    sheetValues
                        .computeIfAbsent(sheet, k -> new LinkedHashMap<>())
                        .put(column, f.get("value").asText());
                    ObjectNode r = json.createObjectNode();
                    r.put("step", stepIdx).put("kind", "payload").put("path", path);
                    r.put("format", c.path("format").asText("json"));
                    r.put("sheet", sheet).put("column", column);
                    replacements.add(r);
                }
            }
        }
        if (replacements.isEmpty()) throw new MCPServer.MCPException(
            -32602,
            "Selections matched no parameterizable values."
        );

        String rowScenario = reusable ? "(R) " + scenName : scenName;
        ObjectNode out = json.createObjectNode();
        out.put("scenario", scenName).put("testcase", tcName).put("reusable", reusable);
        ObjectNode row = out.putObject("row");
        row.put("scenario", rowScenario).put("testcase", tcName).put("iteration", iteration);
        ArrayNode ra = out.putArray("replacements");
        for (ObjectNode r : replacements) ra.add(r);

        if (dryRun) {
            out.put("dryRun", true).put("parameterized", false);
            return out;
        }

        // ---- apply: write data sheets ----------------------------------
        com.ing.datalib.component.EnvTestData env = p.getTestData();
        java.util.Collection<com.ing.datalib.component.TestData> targets = com.ing.engine.cli.commands.DataCommand.pickEnvs(
            env,
            envName
        );
        if (targets.isEmpty()) throw new MCPServer.MCPException(
            -32602,
            "Environment not found: " + envName
        );
        for (Map.Entry<String, Map<String, String>> se : sheetValues.entrySet()) {
            for (com.ing.datalib.component.TestData td : targets) {
                writeDataRow(td, se.getKey(), rowScenario, tcName, iteration, se.getValue());
            }
        }
        env.save();

        // ---- apply: rewrite step inputs ---------------------------------
        List<TestStep> steps = tc.getTestSteps();
        for (ObjectNode r : replacements) {
            TestStep step = steps.get(r.get("step").asInt() - 1);
            String sheet = r.get("sheet").asText();
            String column = r.get("column").asText();
            if ("input".equals(r.get("kind").asText())) {
                step.setInput(sheet + ":" + column);
            } else if ("xml".equals(r.path("format").asText("json"))) {
                step.setInput(
                    replaceXmlLeaf(
                        step.getInput(),
                        r.get("path").asText(),
                        "{" + sheet + ":" + column + "}"
                    )
                );
            } else {
                step.setInput(
                    replaceJsonLeaf(
                        json,
                        step.getInput(),
                        r.get("path").asText(),
                        "{" + sheet + ":" + column + "}"
                    )
                );
            }
        }
        tc.setSaved(false);
        tc.save();
        p.save();

        out.put("parameterized", true).put("values", replacements.size());
        return out;
    }

    /** Scans a test case for parameterization candidates (scan phase). */
    private List<ObjectNode> scanParameterizationCandidates(
        ObjectMapper json,
        TestCase tc,
        String defaultSheet
    ) {
        List<ObjectNode> candidates = new ArrayList<>();
        Set<String> suggested = new HashSet<>();
        int id = 0, idx = 0;
        for (TestStep step : tc.getTestSteps()) {
            idx++;
            String action = safeTrim(step.getAction());
            String input = step.getInput() == null ? "" : step.getInput();
            if (input.isEmpty()) continue;
            if (ConventionCatalog.isPayloadAction(action)) {
                JsonNode body = tryParseJson(json, input);
                if (body != null) {
                    List<ObjectNode> fields = new ArrayList<>();
                    collectJsonLeaves(json, body, "$", fields, suggested);
                    if (!fields.isEmpty()) {
                        ObjectNode c = json.createObjectNode();
                        c.put("id", ++id).put("step", idx).put("action", action);
                        c.put("kind", "payload").put("format", "json").put("sheet", defaultSheet);
                        ArrayNode fa = c.putArray("fields");
                        for (ObjectNode f : fields) fa.add(f);
                        candidates.add(c);
                    }
                    continue;
                }
                org.w3c.dom.Document xml = tryParseXml(input);
                if (xml != null) {
                    List<ObjectNode> fields = new ArrayList<>();
                    org.w3c.dom.Element rootEl = xml.getDocumentElement();
                    collectXmlLeaves(json, rootEl, "/" + rootEl.getTagName(), fields, suggested);
                    if (!fields.isEmpty()) {
                        ObjectNode c = json.createObjectNode();
                        c.put("id", ++id).put("step", idx).put("action", action);
                        c.put("kind", "payload").put("format", "xml").put("sheet", defaultSheet);
                        ArrayNode fa = c.putArray("fields");
                        for (ObjectNode f : fields) fa.add(f);
                        candidates.add(c);
                    }
                    continue;
                }
            }
            if (ConventionCatalog.isParameterizableLiteral(input)) {
                ObjectNode c = json.createObjectNode();
                c.put("id", ++id).put("step", idx).put("action", action);
                c.put("kind", "input").put("value", input);
                c.put("sheet", defaultSheet);
                c.put("column", suggestColumn(safeTrim(step.getObject()), action, idx, suggested));
                candidates.add(c);
            }
        }
        return candidates;
    }

    /** Parses {@code input} as JSON, returning {@code null} when it is not JSON. */
    private static JsonNode tryParseJson(ObjectMapper json, String input) {
        String t = input.trim();
        if (!(t.startsWith("{") || t.startsWith("["))) return null;
        if (ConventionCatalog.containsPayloadTokens(t)) return null; // already parameterized
        try {
            JsonNode n = json.readTree(t);
            return (n != null && n.isContainerNode()) ? n : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** Parses {@code input} as XML, returning {@code null} when it is not XML. */
    private static org.w3c.dom.Document tryParseXml(String input) {
        String t = input.trim();
        if (!t.startsWith("<")) return null;
        if (ConventionCatalog.containsPayloadTokens(t)) return null; // already parameterized
        try {
            javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setXIncludeAware(false);
            dbf.setExpandEntityReferences(false);
            return dbf
                .newDocumentBuilder()
                .parse(new org.xml.sax.InputSource(new java.io.StringReader(t)));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Recursively collects XML leaves: attributes as {@code /a/b[1]/@attr} and
     * text-only elements as {@code /a/b[1]}, each with a suggested column name.
     */
    private void collectXmlLeaves(
        ObjectMapper json,
        org.w3c.dom.Element el,
        String path,
        List<ObjectNode> out,
        Set<String> suggested
    ) {
        org.w3c.dom.NamedNodeMap attrs = el.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            org.w3c.dom.Node a = attrs.item(i);
            String v = a.getNodeValue();
            if (v == null || v.isEmpty() || ConventionCatalog.containsPayloadTokens(v)) continue;
            ObjectNode f = json.createObjectNode();
            f.put("path", path + "/@" + a.getNodeName()).put("value", v);
            f.put("column", uniqueSuggestion(camelCase(a.getNodeName()), suggested));
            out.add(f);
        }
        Map<String, Integer> seen = new LinkedHashMap<>();
        boolean hasChildElements = false;
        org.w3c.dom.NodeList children = el.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            org.w3c.dom.Node n = children.item(i);
            if (n.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) continue;
            hasChildElements = true;
            org.w3c.dom.Element child = (org.w3c.dom.Element) n;
            int idx = seen.merge(child.getTagName(), 1, Integer::sum);
            collectXmlLeaves(
                json,
                child,
                path + "/" + child.getTagName() + "[" + idx + "]",
                out,
                suggested
            );
        }
        if (!hasChildElements) {
            String text = el.getTextContent() == null ? "" : el.getTextContent().trim();
            if (!text.isEmpty() && !ConventionCatalog.containsPayloadTokens(text)) {
                ObjectNode f = json.createObjectNode();
                f.put("path", path).put("value", text);
                f.put("column", uniqueSuggestion(camelCase(el.getTagName()), suggested));
                out.add(f);
            }
        }
    }

    /** Rewrites one XML leaf (element text or attribute) at {@code path} to {@code token}. */
    private static String replaceXmlLeaf(String body, String path, String token) {
        try {
            javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            org.w3c.dom.Document doc = dbf
                .newDocumentBuilder()
                .parse(new org.xml.sax.InputSource(new java.io.StringReader(body.trim())));
            String[] segs = path.split("/");
            org.w3c.dom.Element cur = doc.getDocumentElement();
            String attrName = null;
            for (int i = 2; i < segs.length; i++) { // segs[0]="", segs[1]=root (already selected)
                String seg = segs[i];
                if (seg.isEmpty()) continue;
                if (seg.startsWith("@")) {
                    attrName = seg.substring(1);
                    break;
                }
                String tag = seg;
                int want = 1;
                int br = seg.indexOf('[');
                if (br >= 0) {
                    tag = seg.substring(0, br);
                    want = Integer.parseInt(seg.substring(br + 1, seg.length() - 1));
                }
                org.w3c.dom.Element next = null;
                int count = 0;
                org.w3c.dom.NodeList children = cur.getChildNodes();
                for (int j = 0; j < children.getLength(); j++) {
                    org.w3c.dom.Node n = children.item(j);
                    if (
                        n.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE &&
                        ((org.w3c.dom.Element) n).getTagName().equals(tag)
                    ) {
                        count++;
                        if (count == want) {
                            next = (org.w3c.dom.Element) n;
                            break;
                        }
                    }
                }
                if (next == null) return body;
                cur = next;
            }
            // An attribute directly on the root arrives as segs[1]="root", segs[2]="@x";
            // an attribute path may also be the last segment after the walk above.
            if (attrName == null && segs.length > 2 && segs[segs.length - 1].startsWith("@")) {
                attrName = segs[segs.length - 1].substring(1);
            }
            if (attrName != null) {
                cur.setAttribute(attrName, token);
            } else {
                cur.setTextContent(token);
            }
            javax.xml.transform.Transformer tr = javax
                .xml.transform.TransformerFactory.newInstance()
                .newTransformer();
            tr.setOutputProperty(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "yes");
            java.io.StringWriter sw = new java.io.StringWriter();
            tr.transform(
                new javax.xml.transform.dom.DOMSource(doc),
                new javax.xml.transform.stream.StreamResult(sw)
            );
            return sw.toString();
        } catch (Exception e) {
            return body;
        }
    }

    /** Recursively collects scalar JSON leaves as {path, value, column} objects. */
    private void collectJsonLeaves(
        ObjectMapper json,
        JsonNode node,
        String path,
        List<ObjectNode> out,
        Set<String> suggested
    ) {
        if (node.isObject()) {
            java.util.Iterator<Map.Entry<String, JsonNode>> it = node.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                collectJsonLeaves(json, e.getValue(), path + "." + e.getKey(), out, suggested);
            }
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                collectJsonLeaves(json, node.get(i), path + "[" + i + "]", out, suggested);
            }
        } else if (node.isValueNode() && !node.isNull()) {
            String value = node.asText();
            if (ConventionCatalog.containsPayloadTokens(value)) return; // already done
            ObjectNode f = json.createObjectNode();
            f.put("path", path).put("value", value);
            f.put("column", uniqueSuggestion(camelCase(lastPathSegment(path)), suggested));
            out.add(f);
        }
    }

    /** Rewrites one JSON leaf at {@code path} to {@code token} and re-serializes. */
    private static String replaceJsonLeaf(
        ObjectMapper json,
        String body,
        String path,
        String token
    ) {
        try {
            JsonNode root = json.readTree(body);
            // Walk to the parent of the leaf.
            List<Object> segments = parseJsonPath(path);
            JsonNode parent = root;
            for (int i = 0; i < segments.size() - 1; i++) {
                Object seg = segments.get(i);
                parent =
                    seg instanceof Integer ? parent.get((Integer) seg) : parent.get((String) seg);
                if (parent == null) return body;
            }
            Object last = segments.get(segments.size() - 1);
            if (
                last instanceof Integer &&
                parent instanceof com.fasterxml.jackson.databind.node.ArrayNode
            ) {
                ((com.fasterxml.jackson.databind.node.ArrayNode) parent).set(
                        (Integer) last,
                        json.getNodeFactory().textNode(token)
                    );
            } else if (last instanceof String && parent instanceof ObjectNode) {
                ((ObjectNode) parent).put((String) last, token);
            } else {
                return body;
            }
            return json.writeValueAsString(root);
        } catch (Exception e) {
            return body;
        }
    }

    /** Parses {@code $.a.b[2].c} into segments [a, b, 2, c]. */
    private static List<Object> parseJsonPath(String path) {
        List<Object> out = new ArrayList<>();
        String rest = path.startsWith("$") ? path.substring(1) : path;
        java.util.regex.Matcher m = java
            .util.regex.Pattern.compile("\\.([^.\\[]+)|\\[(\\d+)\\]")
            .matcher(rest);
        while (m.find()) {
            if (m.group(1) != null) out.add(m.group(1)); else out.add(Integer.valueOf(m.group(2)));
        }
        return out;
    }

    private static String lastPathSegment(String path) {
        List<Object> segs = parseJsonPath(path);
        for (int i = segs.size() - 1; i >= 0; i--) {
            if (segs.get(i) instanceof String) return (String) segs.get(i);
        }
        return "Value";
    }

    /** Derives a data-sheet name from a test case name (alphanumeric CamelCase). */
    private static String deriveSheetName(String tcName) {
        String cc = camelCase(tcName);
        if (cc.isEmpty()) cc = "Data";
        return cc.length() > 25 ? cc.substring(0, 25) : cc;
    }

    /** CamelCases a free-form name: {@code account_number} -> {@code AccountNumber}. */
    private static String camelCase(String raw) {
        StringBuilder sb = new StringBuilder();
        boolean up = true;
        for (char c : raw.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                sb.append(up ? Character.toUpperCase(c) : c);
                up = false;
            } else {
                up = true;
            }
        }
        return sb.toString();
    }

    /** Suggests a column name from the step's object (preferred) or action. */
    private static String suggestColumn(
        String object,
        String action,
        int stepIdx,
        Set<String> suggested
    ) {
        String base;
        if (!object.isEmpty() && !"Webservice".equalsIgnoreCase(object)) {
            int dot = object.indexOf('.');
            base = camelCase(dot >= 0 ? object.substring(dot + 1) : object);
        } else {
            base = camelCase(action) + "Step" + stepIdx;
        }
        if (base.isEmpty()) base = "Value" + stepIdx;
        return uniqueSuggestion(base, suggested);
    }

    private static String uniqueSuggestion(String base, Set<String> suggested) {
        String candidate = base;
        int n = 2;
        while (!suggested.add(candidate)) candidate = base + (n++);
        return candidate;
    }

    private static String uniqueColumn(String sheet, String column, Set<String> used) {
        String key = sheet + ":" + column;
        if (used.add(key)) return column;
        int n = 2;
        while (!used.add(sheet + ":" + column + n)) n++;
        return column + n;
    }

    /**
     * Writes {@code columns} into the data-sheet row keyed to
     * ({@code rowScenario}, {@code tcName}, {@code iteration}) in {@code sheet},
     * creating the sheet, columns and row as needed. Reuses an existing row so
     * repeated parameterize runs stay idempotent.
     */
    private void writeDataRow(
        com.ing.datalib.component.TestData td,
        String sheet,
        String rowScenario,
        String tcName,
        String iteration,
        Map<String, String> columns
    ) {
        com.ing.datalib.testdata.model.TestDataModel model = td.getByName(sheet);
        if (model == null) model = td.addTestData(td.getNewTestData(sheet));
        model.loadTableModel();
        for (String col : columns.keySet()) {
            if (model.getColumnIndex(col) < 0) model.addColumn(col);
        }
        List<com.ing.datalib.testdata.model.Record> records = model.getRecords();
        int rowIdx = -1;
        for (int i = 0; i < records.size(); i++) {
            com.ing.datalib.testdata.model.Record rec = records.get(i);
            if (
                rowScenario.equals(rec.getScenario()) &&
                tcName.equals(rec.getTestcase()) &&
                iteration.equals(rec.getIteration())
            ) {
                rowIdx = i;
                break;
            }
        }
        if (rowIdx < 0) {
            com.ing.datalib.testdata.model.Record rec = model.addRecord();
            rec.setScenario(rowScenario);
            rec.setTestcase(tcName);
            rec.setIteration(iteration);
            rec.setSubIteration("1");
            rowIdx = model.getRowCount() - 1;
        }
        for (Map.Entry<String, String> e : columns.entrySet()) {
            int col = model.getColumnIndex(e.getKey());
            if (col >= 0) model.setValueAt(e.getValue(), rowIdx, col);
        }
    }

    // ==================================================================
    // test set create / add (TestLab/ via Datalib model)
    // ==================================================================

    private JsonNode testSetCreate(ObjectMapper json, JsonNode args) {
        Project p = loadProject(resolveProject(projectArg(args)));
        String rel = MCPServer.requiredParam(args, "release");
        String set = MCPServer.requiredParam(args, "testset");
        com.ing.datalib.component.Release release = openOrCreateRelease(p, rel);
        if (release.getTestSetByName(set) != null) {
            return json
                .createObjectNode()
                .put("created", false)
                .put("release", rel)
                .put("testset", set)
                .put("message", "Test set already exists");
        }
        com.ing.datalib.component.TestSet ts = release.addTestSet(set);
        if (ts == null) throw new MCPServer.MCPException(
            -32603,
            "Failed to create test set: " + set
        );
        ts.setSaved(false);
        ts.save();
        return json.createObjectNode().put("created", true).put("release", rel).put("testset", set);
    }

    private JsonNode testSetAdd(ObjectMapper json, JsonNode args) {
        Project p = loadProject(resolveProject(projectArg(args)));
        String rel = MCPServer.requiredParam(args, "release");
        String set = MCPServer.requiredParam(args, "testset");
        String scen = MCPServer.requiredParam(args, "scenario");
        String tc = MCPServer.requiredParam(args, "testcase");
        String browser = MCPServer.paramOrDefault(args, "browser", "Chrome");
        String iter = MCPServer.paramOrDefault(args, "iteration", "1");
        boolean execute = boolArg(args, "execute", true);
        if (boolArg(args, "dryRun", false)) {
            return json
                .createObjectNode()
                .put("dryRun", true)
                .put("wouldAdd", true)
                .put("release", rel)
                .put("testset", set)
                .put("scenario", scen)
                .put("testcase", tc);
        }

        com.ing.datalib.component.Release release = openOrCreateRelease(p, rel);
        com.ing.datalib.component.TestSet ts = release.getTestSetByName(set);
        if (ts == null) ts = release.addTestSet(set);
        if (ts == null) throw new MCPServer.MCPException(-32603, "Failed to open test set: " + set);
        // Load existing rows so we append instead of overwriting.
        ts.loadTestSetTableModel();
        // The YAML/CSV store seeds an empty default row (execute=true, no
        // scenario/testcase). Drop those placeholder rows so the test set only
        // ever contains real, executable entries.
        pruneBlankExecutionRows(ts);
        com.ing.datalib.component.ExecutionStep step = ts.addNewStep();
        step.setExecute(execute ? "true" : "false");
        step.setTestScenario(scen);
        step.setTestCase(tc);
        step.setIteration(iter);
        step.setBrowser(browser);
        ts.setSaved(false);
        ts.save();
        return json
            .createObjectNode()
            .put("added", true)
            .put("release", rel)
            .put("testset", set)
            .put("scenario", scen)
            .put("testcase", tc)
            .put("rows", ts.getTestSteps().size());
    }

    private com.ing.datalib.component.Release openOrCreateRelease(Project p, String rel) {
        com.ing.datalib.component.Release release = p.getReleaseByName(rel);
        if (release == null) release = p.addRelease(rel);
        if (release == null) throw new MCPServer.MCPException(
            -32603,
            "Failed to create release: " + rel
        );
        new File(release.getLocation()).mkdirs();
        return release;
    }

    /**
     * Remove placeholder execution rows that carry no scenario and no test case.
     * The test-set store seeds a default row on load; without this an added row
     * would leave a blank, non-executable entry behind.
     */
    private void pruneBlankExecutionRows(com.ing.datalib.component.TestSet ts) {
        List<com.ing.datalib.component.ExecutionStep> steps = ts.getTestSteps();
        for (int i = steps.size() - 1; i >= 0; i--) {
            com.ing.datalib.component.ExecutionStep s = steps.get(i);
            boolean noScenario =
                s.getTestScenarioName() == null || s.getTestScenarioName().trim().isEmpty();
            boolean noTestCase =
                s.getTestCaseName() == null || s.getTestCaseName().trim().isEmpty();
            if (noScenario && noTestCase) ts.removeRow(i);
        }
    }

    // ==================================================================
    // object repository (ObjectRepository/<page>.csv)
    // ==================================================================

    private JsonNode objectList(ObjectMapper json, JsonNode args) {
        Project p = loadProject(resolveProject(projectArg(args)));
        ArrayNode out = json.createArrayNode();
        LinkedHashMap<String, Integer> counts = new LinkedHashMap<>();
        com.ing.datalib.or.web.WebOR web = projectWebOR(p);
        if (web != null) {
            for (com.ing.datalib.or.web.WebORPage pg : web.getPages()) {
                counts.put(pg.getName(), pg.getObjectGroups().size());
            }
        }
        // Legacy CSV pages authored by older tool versions (kept for compatibility).
        File orDir = new File(p.getLocation(), "ObjectRepository");
        File[] csvPages = orDir.listFiles(
            f -> f.isFile() && f.getName().toLowerCase(Locale.ROOT).endsWith(".csv")
        );
        if (csvPages != null) {
            Arrays.sort(csvPages, Comparator.comparing(File::getName));
            for (File page : csvPages) {
                String pn = page.getName().replaceFirst("(?i)\\.csv$", "");
                if (!counts.containsKey(pn)) counts.put(pn, countDataRows(page));
            }
        }
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            out.addObject().put("page", e.getKey()).put("objects", e.getValue());
        }
        return out;
    }

    private JsonNode objectShow(ObjectMapper json, JsonNode args) {
        Project p = loadProject(resolveProject(projectArg(args)));
        String page = MCPServer.requiredParam(args, "page");
        com.ing.datalib.or.web.WebOR web = projectWebOR(p);
        com.ing.datalib.or.web.WebORPage pg = web == null ? null : web.getPageByName(page);
        if (pg != null) {
            ObjectNode out = json.createObjectNode();
            out.put("page", pg.getName());
            out.put("format", "yaml");
            ArrayNode objs = out.putArray("objects");
            for (com.ing.datalib.or.common.ObjectGroup<com.ing.datalib.or.web.WebORObject> g : pg.getObjectGroups()) {
                for (com.ing.datalib.or.web.WebORObject o : g.getObjects()) {
                    ObjectNode on = objs.addObject();
                    on.put("name", o.getName());
                    on.put("type", "WebElement");
                    ObjectNode locs = on.putObject("locators");
                    for (String prop : com.ing.datalib.or.web.WebOR.OBJECT_PROPS) {
                        String v = getWebAttr(o, prop);
                        if (!v.isEmpty()) locs.put(yamlLocatorKey(prop), v);
                    }
                    String frame = o.getFrame();
                    if (frame != null && !frame.isEmpty()) on.put("frame", frame);
                }
            }
            return out;
        }
        // Fallback: legacy CSV page authored by older tool versions.
        File orDir = new File(p.getLocation(), "ObjectRepository");
        File f = new File(orDir, page + ".csv");
        if (!f.isFile()) throw notFound(
            -32602,
            "Page not found: " + page,
            objectPageNames(args),
            page
        );
        ObjectNode out = json.createObjectNode();
        out.put("page", page);
        out.put("format", "csv");
        ArrayNode objs = out.putArray("objects");
        List<String> lines = readLines(f);
        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).trim().isEmpty()) continue;
            String[] c = lines.get(i).split(",", -1);
            ObjectNode o = objs.addObject();
            o.put("name", c.length > 0 ? c[0] : "");
            o.put("type", c.length > 1 ? c[1] : "");
            o.put("locator", c.length > 2 ? c[2] : "");
            o.put("value", c.length > 3 ? c[3] : "");
            o.put("description", c.length > 4 ? c[4] : "");
        }
        return out;
    }

    private JsonNode objectSearch(ObjectMapper json, JsonNode args) {
        Project p = loadProject(resolveProject(projectArg(args)));
        String q = MCPServer.requiredParam(args, "query").toLowerCase(Locale.ROOT);
        ArrayNode out = json.createArrayNode();
        Set<String> modelPages = new LinkedHashSet<>();
        com.ing.datalib.or.web.WebOR web = projectWebOR(p);
        if (web != null) {
            for (com.ing.datalib.or.web.WebORPage pg : web.getPages()) {
                modelPages.add(pg.getName());
                for (com.ing.datalib.or.common.ObjectGroup<com.ing.datalib.or.web.WebORObject> g : pg.getObjectGroups()) {
                    for (com.ing.datalib.or.web.WebORObject o : g.getObjects()) {
                        StringBuilder hay = new StringBuilder(
                            o.getName() == null ? "" : o.getName()
                        );
                        ObjectNode locs = json.createObjectNode();
                        for (String prop : com.ing.datalib.or.web.WebOR.OBJECT_PROPS) {
                            String v = getWebAttr(o, prop);
                            if (!v.isEmpty()) {
                                hay.append(' ').append(v);
                                locs.put(yamlLocatorKey(prop), v);
                            }
                        }
                        if (hay.toString().toLowerCase(Locale.ROOT).contains(q)) {
                            ObjectNode on = out.addObject();
                            on.put("page", pg.getName());
                            on.put("name", o.getName());
                            on.set("locators", locs);
                        }
                    }
                }
            }
        }
        // Legacy CSV pages not shadowed by a model page.
        File orDir = new File(p.getLocation(), "ObjectRepository");
        File[] pages = orDir.listFiles(
            f -> f.isFile() && f.getName().toLowerCase(Locale.ROOT).endsWith(".csv")
        );
        if (pages != null) {
            Arrays.sort(pages, Comparator.comparing(File::getName));
            for (File page : pages) {
                String pageName = page.getName().replaceFirst("(?i)\\.csv$", "");
                if (modelPages.contains(pageName)) continue;
                List<String> lines = readLines(page);
                for (int i = 1; i < lines.size(); i++) {
                    String line = lines.get(i);
                    if (line.trim().isEmpty()) continue;
                    if (!line.toLowerCase(Locale.ROOT).contains(q)) continue;
                    String[] c = line.split(",", -1);
                    ObjectNode o = out.addObject();
                    o.put("page", pageName);
                    o.put("name", c.length > 0 ? c[0] : "");
                    o.put("type", c.length > 1 ? c[1] : "");
                    o.put("locator", c.length > 2 ? c[2] : "");
                    o.put("value", c.length > 3 ? c[3] : "");
                }
            }
        }
        return out;
    }

    // ==================================================================
    // data show / get / set (environment-aware, via TestDataModel)
    // ==================================================================

    private JsonNode dataShow(ObjectMapper json, JsonNode args) {
        Project p = loadProject(resolveProject(projectArg(args)));
        String sheet = MCPServer.requiredParam(args, "sheet");
        String envName = MCPServer.paramOrDefault(args, "env", null);
        int limit = intArg(args, "limit", 50);
        com.ing.datalib.testdata.model.TestDataModel model = firstModel(p, sheet, envName);
        model.loadTableModel();
        ObjectNode out = json.createObjectNode();
        out.put("sheet", sheet);
        int colCount = model.getColumnCount();
        ArrayNode cols = out.putArray("columns");
        for (int c = 0; c < colCount; c++) cols.add(model.getColumnName(c));
        ArrayNode rows = out.putArray("rows");
        int rowCount = Math.min(model.getRowCount(), Math.max(0, limit));
        for (int r = 0; r < rowCount; r++) {
            ObjectNode row = rows.addObject();
            for (int c = 0; c < colCount; c++) {
                row.put(model.getColumnName(c), String.valueOf(model.getValueAt(r, c)));
            }
        }
        out.put("totalRows", model.getRowCount());
        return out;
    }

    private JsonNode dataGet(ObjectMapper json, JsonNode args) {
        Project p = loadProject(resolveProject(projectArg(args)));
        String sheet = MCPServer.requiredParam(args, "sheet");
        String column = MCPServer.requiredParam(args, "column");
        int row = intArg(args, "row", 1);
        String envName = MCPServer.paramOrDefault(args, "env", null);
        com.ing.datalib.testdata.model.TestDataModel model = firstModel(p, sheet, envName);
        model.loadTableModel();
        int col = model.getColumnIndex(column);
        if (col < 0) throw new MCPServer.MCPException(-32602, "Column not found: " + column);
        int idx = row - 1;
        if (idx < 0 || idx >= model.getRowCount()) {
            throw new MCPServer.MCPException(
                -32602,
                "Row out of range (1.." + model.getRowCount() + "): " + row
            );
        }
        return json
            .createObjectNode()
            .put("sheet", sheet)
            .put("column", column)
            .put("row", row)
            .put("value", String.valueOf(model.getValueAt(idx, col)));
    }

    private JsonNode dataSet(ObjectMapper json, JsonNode args) {
        Project p = loadProject(resolveProject(projectArg(args)));
        String sheet = MCPServer.requiredParam(args, "sheet");
        String column = MCPServer.requiredParam(args, "column");
        String value = MCPServer.requiredParam(args, "value");
        int row = intArg(args, "row", 1);
        String envName = MCPServer.paramOrDefault(args, "env", null);
        com.ing.datalib.component.EnvTestData env = p.getTestData();
        java.util.Collection<com.ing.datalib.component.TestData> targets = com.ing.engine.cli.commands.DataCommand.pickEnvs(
            env,
            envName
        );
        if (targets.isEmpty()) throw new MCPServer.MCPException(
            -32602,
            "Environment not found: " + envName
        );
        int idx = row - 1;
        if (idx < 0) throw new MCPServer.MCPException(-32602, "Row must be >= 1: " + row);
        if (boolArg(args, "dryRun", false)) {
            return json
                .createObjectNode()
                .put("dryRun", true)
                .put("sheet", sheet)
                .put("column", column)
                .put("row", row)
                .put("value", value);
        }
        int updated = 0;
        for (com.ing.datalib.component.TestData td : targets) {
            com.ing.datalib.testdata.model.TestDataModel model = td.getByName(sheet);
            if (model == null) continue;
            model.loadTableModel();
            int col = model.getColumnIndex(column);
            if (col < 0) {
                model.addColumn(column);
                col = model.getColumnIndex(column);
            }
            while (model.getRowCount() <= idx) model.addRecord();
            if (col >= 0) {
                model.setValueAt(value, idx, col);
                updated++;
            }
        }
        if (updated == 0) throw new MCPServer.MCPException(
            -32602,
            "Data sheet not found in any target environment: " + sheet
        );
        env.save();
        p.save();
        return json
            .createObjectNode()
            .put("sheet", sheet)
            .put("column", column)
            .put("row", row)
            .put("value", value)
            .put("environments", updated);
    }

    /** Resolve a {@link com.ing.datalib.testdata.model.TestDataModel} for a sheet in the first matching env. */
    private com.ing.datalib.testdata.model.TestDataModel firstModel(
        Project p,
        String sheet,
        String envName
    ) {
        com.ing.datalib.component.EnvTestData env = p.getTestData();
        java.util.Collection<com.ing.datalib.component.TestData> targets = com.ing.engine.cli.commands.DataCommand.pickEnvs(
            env,
            envName
        );
        if (targets.isEmpty()) throw new MCPServer.MCPException(
            -32602,
            "Environment not found: " + envName
        );
        for (com.ing.datalib.component.TestData td : targets) {
            com.ing.datalib.testdata.model.TestDataModel model = td.getByName(sheet);
            if (model != null) return model;
        }
        throw notFound(-32602, "Data sheet not found: " + sheet, sheetNames(env), sheet);
    }

    // ==================================================================
    // report show / compare
    // ==================================================================

    private JsonNode reportShow(ObjectMapper json, JsonNode args) {
        File dir = locateRunDir(args);
        String runId = MCPServer.requiredParam(args, "runId");
        File runDir = new File(dir, runId);
        if (!runDir.isDirectory()) throw new MCPServer.MCPException(
            -32602,
            "Run not found: " + runId
        );
        ObjectNode out = json.createObjectNode();
        out.put("runId", runId);
        out.set("report", readDataJs(json, runDir));
        return out;
    }

    private JsonNode reportCompare(ObjectMapper json, JsonNode args) {
        File dir = locateRunDir(args);
        String a = MCPServer.requiredParam(args, "runA");
        String b = MCPServer.requiredParam(args, "runB");
        ObjectNode out = json.createObjectNode();
        out.set("runA", summarizeRun(json, dir, a));
        out.set("runB", summarizeRun(json, dir, b));
        return out;
    }

    private ObjectNode summarizeRun(ObjectMapper json, File dir, String runId) {
        File runDir = new File(dir, runId);
        if (!runDir.isDirectory()) throw new MCPServer.MCPException(
            -32602,
            "Run not found: " + runId
        );
        JsonNode data = readDataJs(json, runDir);
        int pass = 0, fail = 0, total = 0;
        JsonNode ex = data.path("EXECUTIONS");
        if (ex.isArray()) {
            for (JsonNode tc : ex) {
                total++;
                String st = tc.path("status").asText("");
                if ("PASS".equalsIgnoreCase(st)) pass++; else if (
                    "FAIL".equalsIgnoreCase(st)
                ) fail++;
            }
        }
        ObjectNode o = json.createObjectNode();
        o.put("runId", runId);
        o.put("total", total);
        o.put("pass", pass);
        o.put("fail", fail);
        return o;
    }

    // ==================================================================
    // config show
    // ==================================================================

    private JsonNode configShow(ObjectMapper json, JsonNode args) {
        File dir = new File(resolveProject(projectArg(args)), "Configuration");
        ObjectNode out = json.createObjectNode();
        out.put("location", dir.getAbsolutePath());
        ArrayNode files = out.putArray("files");
        if (dir.isDirectory()) {
            File[] fs = dir.listFiles(
                f -> f.isFile() && f.getName().toLowerCase(Locale.ROOT).endsWith(".properties")
            );
            if (fs != null) {
                Arrays.sort(fs, Comparator.comparing(File::getName));
                for (File f : fs) files.add(f.getName());
            }
        }
        return out;
    }

    // ==================================================================
    // test case step editing (Phase 2)
    // ==================================================================

    private TestCase openTestCase(JsonNode args) {
        Project p = loadProject(resolveProject(projectArg(args)));
        String scenName = MCPServer.requiredParam(args, "scenario");
        String tcName = MCPServer.requiredParam(args, "testcase");
        Scenario s = p.getScenarioByName(scenName);
        if (s == null) throw notFound(
            -32602,
            "Scenario not found: " + scenName,
            scenarioNames(p),
            scenName
        );
        TestCase tc = s.getTestCaseByName(tcName);
        if (tc == null) throw notFound(
            -32602,
            "Test case not found: " + tcName,
            testCaseNames(s),
            tcName
        );
        ensureLoaded(tc);
        return tc;
    }

    private static int oneBasedIndex(JsonNode args, String key, int size) {
        int idx = intArg(args, key, -1);
        if (idx < 1 || idx > size) {
            throw new MCPServer.MCPException(
                -32602,
                key + " out of range (1.." + size + "): " + idx
            );
        }
        return idx - 1;
    }

    private JsonNode testCaseEditStep(ObjectMapper json, JsonNode args) {
        TestCase tc = openTestCase(args);
        int idx = oneBasedIndex(args, "index", tc.getTestSteps().size());
        TestStep step = tc.getTestSteps().get(idx);
        applyIfPresent(args, "action", step::setAction);
        applyIfPresent(args, "object", step::setObject);
        applyIfPresent(args, "input", step::setInput);
        applyIfPresent(args, "condition", step::setCondition);
        applyIfPresent(args, "description", step::setDescription);
        applyIfPresent(args, "reference", step::setReference);
        // Re-normalize the final action+input pair so edits stay grammar-conformant.
        StepNormalizer.Result norm = StepNormalizer.normalize(
            "step " + (idx + 1),
            step.getAction(),
            step.getObject(),
            step.getInput(),
            step.getCondition()
        );
        if (!norm.errors.isEmpty()) {
            throw new MCPServer.MCPException(-32602, String.join("; ", norm.errors));
        }
        step.setInput(norm.input);
        step.setCondition(norm.condition);
        tc.save();
        ObjectNode out = json.createObjectNode().put("edited", true).put("index", idx + 1);
        if (!norm.warnings.isEmpty()) {
            ArrayNode wa = out.putArray("warnings");
            for (String w : norm.warnings) wa.add(w);
        }
        return out;
    }

    private JsonNode testCaseInsertStep(ObjectMapper json, JsonNode args) {
        TestCase tc = openTestCase(args);
        int size = tc.getTestSteps().size();
        int idx = intArg(args, "index", size + 1) - 1;
        if (idx < 0 || idx > size) throw new MCPServer.MCPException(
            -32602,
            "index out of range (1.." + (size + 1) + ")"
        );
        TestStep step = tc.addNewStepAt(idx);
        String action = MCPServer.paramOrDefault(args, "action", "");
        String object = MCPServer.paramOrDefault(args, "object", "");
        String condition = MCPServer.paramOrDefault(args, "condition", "");
        StepNormalizer.Result norm = StepNormalizer.normalize(
            "step " + (idx + 1),
            action,
            object,
            MCPServer.paramOrDefault(args, "input", ""),
            condition
        );
        if (!norm.errors.isEmpty()) {
            throw new MCPServer.MCPException(-32602, String.join("; ", norm.errors));
        }
        step.setAction(action);
        step.setObject(object);
        step.setInput(norm.input);
        step.setCondition(norm.condition);
        step.setDescription(MCPServer.paramOrDefault(args, "description", ""));
        tc.save();
        ObjectNode out = json
            .createObjectNode()
            .put("inserted", true)
            .put("index", idx + 1)
            .put("totalSteps", tc.getTestSteps().size());
        if (!norm.warnings.isEmpty()) {
            ArrayNode wa = out.putArray("warnings");
            for (String w : norm.warnings) wa.add(w);
        }
        return out;
    }

    private JsonNode testCaseRemoveStep(ObjectMapper json, JsonNode args) {
        TestCase tc = openTestCase(args);
        int idx = oneBasedIndex(args, "index", tc.getTestSteps().size());
        tc.removeRow(idx);
        tc.save();
        return json
            .createObjectNode()
            .put("removed", true)
            .put("index", idx + 1)
            .put("totalSteps", tc.getTestSteps().size());
    }

    private JsonNode testCaseMoveStep(ObjectMapper json, JsonNode args) {
        TestCase tc = openTestCase(args);
        int size = tc.getTestSteps().size();
        int from = oneBasedIndex(args, "from", size);
        int to = oneBasedIndex(args, "to", size);
        TestStep src = tc.getTestSteps().get(from);
        String action = src.getAction(), object = src.getObject(), input = src.getInput(), condition = src.getCondition(), description = src.getDescription(), reference = src.getReference();
        tc.removeRow(from);
        int insertAt = to > from ? to : to; // 'to' is the desired final 0-based slot
        if (insertAt > tc.getTestSteps().size()) insertAt = tc.getTestSteps().size();
        TestStep ns = tc.addNewStepAt(insertAt);
        ns.setAction(action);
        ns.setObject(object);
        ns.setInput(input);
        ns.setCondition(condition);
        ns.setDescription(description);
        ns.setReference(reference);
        tc.save();
        return json
            .createObjectNode()
            .put("moved", true)
            .put("from", from + 1)
            .put("to", insertAt + 1);
    }

    private static void applyIfPresent(
        JsonNode args,
        String key,
        java.util.function.Consumer<String> setter
    ) {
        String v = MCPServer.paramOrDefault(args, key, null);
        if (v != null) setter.accept(v);
    }

    // ==================================================================
    // object repository write (Phase 2)
    // ==================================================================

    private File objectPageFile(JsonNode args, boolean createDir) {
        File orDir = new File(resolveProject(projectArg(args)), "ObjectRepository");
        if (createDir) orDir.mkdirs();
        return new File(orDir, MCPServer.requiredParam(args, "page") + ".csv");
    }

    private JsonNode objectAdd(ObjectMapper json, JsonNode args) {
        Project p = loadProject(resolveProject(projectArg(args)));
        String page = MCPServer.requiredParam(args, "page");
        String name = MCPServer.requiredParam(args, "name");
        String locator = MCPServer.paramOrDefault(args, "locator", "");
        String value = MCPServer.paramOrDefault(args, "value", "");
        com.ing.datalib.or.ObjectRepository orRepo = p.getObjectRepository();
        com.ing.datalib.or.web.WebOR web = orRepo == null ? null : orRepo.getWebOR();
        if (web == null) throw new MCPServer.MCPException(
            -32603,
            "Object Repository model unavailable for project."
        );
        com.ing.datalib.or.web.WebORPage orPage = web.getPageByName(page);
        boolean exists = orPage != null && orPage.getObjectGroupByName(name) != null;
        if (boolArg(args, "dryRun", false)) {
            return json
                .createObjectNode()
                .put("dryRun", true)
                .put("wouldAdd", !exists)
                .put("page", page)
                .put("name", name)
                .put("exists", exists);
        }
        if (exists) throw new MCPServer.MCPException(
            -32602,
            "Object already exists on page: " + name
        );
        if (orPage == null) orPage = web.addPage(page);
        com.ing.datalib.or.web.WebORObject o = orPage.addObject(name);
        if (o == null) throw new MCPServer.MCPException(-32603, "Failed to add object: " + name);
        if (!locator.isEmpty() || !value.isEmpty()) {
            String[] mapped = mapLocatorToAttr(locator, value);
            setWebAttr(o, mapped[0], mapped[1]);
        }
        orRepo.saveWebPageNow(orPage);
        return json
            .createObjectNode()
            .put("added", true)
            .put("page", page)
            .put("name", name)
            .put("format", "yaml");
    }

    private JsonNode objectUpdate(ObjectMapper json, JsonNode args) {
        Project p = loadProject(resolveProject(projectArg(args)));
        String page = MCPServer.requiredParam(args, "page");
        String name = MCPServer.requiredParam(args, "name");
        com.ing.datalib.or.web.WebOR web = projectWebOR(p);
        com.ing.datalib.or.web.WebORPage orPage = web == null ? null : web.getPageByName(page);
        com.ing.datalib.or.web.WebORObject o = null;
        if (orPage != null) {
            com.ing.datalib.or.common.ObjectGroup<com.ing.datalib.or.web.WebORObject> g = orPage.getObjectGroupByName(
                name
            );
            if (g != null && !g.getObjects().isEmpty()) o = g.getObjects().get(0);
        }
        if (o != null) {
            String locator = MCPServer.paramOrDefault(args, "locator", null);
            String value = MCPServer.paramOrDefault(args, "value", null);
            if (locator != null) {
                // An explicit strategy change replaces the object's locators so
                // no stale strategy is left behind.
                String[] mapped = mapLocatorToAttr(locator, value == null ? "" : value);
                clearWebLocators(o);
                setWebAttr(o, mapped[0], mapped[1]);
            } else if (value != null) {
                String prim = primaryLocatorProp(o);
                setWebAttr(o, prim == null ? "css" : prim, value);
            }
            p.getObjectRepository().saveWebPageNow(orPage);
            return json
                .createObjectNode()
                .put("updated", true)
                .put("page", page)
                .put("name", name)
                .put("format", "yaml");
        }
        // Fallback: legacy CSV page authored by older tool versions.
        return objectUpdateCsv(json, args);
    }

    private JsonNode objectDelete(ObjectMapper json, JsonNode args) {
        Project p = loadProject(resolveProject(projectArg(args)));
        String page = MCPServer.requiredParam(args, "page");
        String name = MCPServer.requiredParam(args, "name");
        com.ing.datalib.or.web.WebOR web = projectWebOR(p);
        com.ing.datalib.or.web.WebORPage orPage = web == null ? null : web.getPageByName(page);
        if (orPage != null && orPage.getObjectGroupByName(name) != null) {
            orPage.deleteObjectGroup(name);
            p.getObjectRepository().saveWebPageNow(orPage);
            return json
                .createObjectNode()
                .put("deleted", true)
                .put("page", page)
                .put("name", name)
                .put("format", "yaml");
        }
        // Fallback: legacy CSV page authored by older tool versions.
        return objectDeleteCsv(json, args);
    }

    // ---- legacy CSV fallbacks (only hit for pre-existing *.csv OR pages) ----

    private JsonNode objectUpdateCsv(ObjectMapper json, JsonNode args) {
        File f = objectPageFile(args, false);
        String name = MCPServer.requiredParam(args, "name");
        if (!f.isFile()) throw new MCPServer.MCPException(-32602, "Object not found: " + name);
        List<String> lines = readLines(f);
        boolean found = false;
        for (int i = 1; i < lines.size(); i++) {
            String[] c = splitTo(lines.get(i), 5);
            if (c[0].equals(name)) {
                if (MCPServer.paramOrDefault(args, "type", null) != null) c[1] =
                    args.get("type").asText();
                if (MCPServer.paramOrDefault(args, "locator", null) != null) c[2] =
                    args.get("locator").asText();
                if (MCPServer.paramOrDefault(args, "value", null) != null) c[3] =
                    args.get("value").asText();
                if (MCPServer.paramOrDefault(args, "description", null) != null) c[4] =
                    args.get("description").asText();
                lines.set(i, csvRow(c[0], c[1], c[2], c[3], c[4]));
                found = true;
                break;
            }
        }
        if (!found) throw new MCPServer.MCPException(-32602, "Object not found: " + name);
        writeLines(f, lines);
        return json.createObjectNode().put("updated", true).put("name", name).put("format", "csv");
    }

    private JsonNode objectDeleteCsv(ObjectMapper json, JsonNode args) {
        File f = objectPageFile(args, false);
        String name = MCPServer.requiredParam(args, "name");
        if (!f.isFile()) throw new MCPServer.MCPException(-32602, "Object not found: " + name);
        List<String> lines = readLines(f);
        boolean removed = false;
        for (int i = 1; i < lines.size(); i++) {
            String[] c = lines.get(i).split(",", -1);
            if (c.length > 0 && c[0].equals(name)) {
                lines.remove(i);
                removed = true;
                break;
            }
        }
        if (!removed) throw new MCPServer.MCPException(-32602, "Object not found: " + name);
        writeLines(f, lines);
        return json.createObjectNode().put("deleted", true).put("name", name).put("format", "csv");
    }

    /**
     * Map a user-facing locator strategy (id, css, xpath, role, text, label,
     * testId, ...) and value onto the WebOR model's attribute name + value.
     * Strategies with no native attribute (id, name, class, tag) are expressed
     * as an equivalent CSS selector so they remain resolvable.
     */
    private static String[] mapLocatorToAttr(String strategy, String value) {
        String s = strategy == null ? "" : strategy.trim().toLowerCase(Locale.ROOT);
        String v = value == null ? "" : value;
        switch (s) {
            case "xpath":
            case "xpath1":
                return new String[] { "xpath", v };
            case "role":
                return new String[] { "Role", v };
            case "text":
            case "linktext":
            case "link":
            case "partiallinktext":
                return new String[] { "Text", v };
            case "label":
                return new String[] { "Label", v };
            case "placeholder":
                return new String[] { "Placeholder", v };
            case "alttext":
            case "alt":
                return new String[] { "AltText", v };
            case "title":
                return new String[] { "Title", v };
            case "testid":
            case "data-testid":
            case "datatestid":
                return new String[] { "TestId", v };
            case "chainedlocator":
            case "chained":
                return new String[] { "ChainedLocator", v };
            case "jspath":
            case "js":
                return new String[] { "JSPath", v };
            case "id":
                return new String[] { "css", v.isEmpty() || v.startsWith("#") ? v : "#" + v };
            case "name":
                return new String[] { "css", v.isEmpty() ? v : "[name=\"" + v + "\"]" };
            case "class":
            case "classname":
                return new String[] { "css", v.isEmpty() || v.startsWith(".") ? v : "." + v };
            case "tag":
            case "tagname":
            case "css":
            default:
                return new String[] { "css", v };
        }
    }

    /** First OBJECT_PROP with a non-empty value on the object, or null. */
    private static String primaryLocatorProp(com.ing.datalib.or.web.WebORObject o) {
        for (String prop : com.ing.datalib.or.web.WebOR.OBJECT_PROPS) {
            if (!getWebAttr(o, prop).isEmpty()) return prop;
        }
        return null;
    }

    /** Clear every locator attribute on an object (used when replacing the locator). */
    private static void clearWebLocators(com.ing.datalib.or.web.WebORObject o) {
        for (String prop : com.ing.datalib.or.web.WebOR.OBJECT_PROPS) setWebAttr(o, prop, "");
    }

    private static String csvRow(String... cells) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) sb.append(',');
            // Naive CSV (matches the existing `ingenious object create`); strip
            // commas/newlines to keep the single-line row intact.
            sb.append(cells[i] == null ? "" : cells[i].replace(",", " ").replace("\n", " "));
        }
        return sb.toString();
    }

    private static String[] splitTo(String line, int n) {
        String[] c = line.split(",", -1);
        String[] out = new String[n];
        for (int i = 0; i < n; i++) out[i] = i < c.length ? c[i] : "";
        return out;
    }

    private static void writeLines(File f, List<String> lines) {
        try {
            f.getParentFile().mkdirs();
            Files.write(f.toPath(), lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new MCPServer.MCPException(-32603, "Failed to write file: " + e.getMessage());
        }
    }

    // ==================================================================
    // data row delete (Phase 2)
    // ==================================================================

    private JsonNode dataRowDelete(ObjectMapper json, JsonNode args) {
        Project p = loadProject(resolveProject(projectArg(args)));
        String sheet = MCPServer.requiredParam(args, "sheet");
        int row = intArg(args, "row", 0);
        String envName = MCPServer.paramOrDefault(args, "env", null);
        if (row < 1) throw new MCPServer.MCPException(-32602, "Row must be >= 1: " + row);
        com.ing.datalib.component.EnvTestData env = p.getTestData();
        java.util.Collection<com.ing.datalib.component.TestData> targets = com.ing.engine.cli.commands.DataCommand.pickEnvs(
            env,
            envName
        );
        if (targets.isEmpty()) throw new MCPServer.MCPException(
            -32602,
            "Environment not found: " + envName
        );
        int deleted = 0;
        for (com.ing.datalib.component.TestData td : targets) {
            com.ing.datalib.testdata.model.TestDataModel model = td.getByName(sheet);
            if (model == null) continue;
            model.loadTableModel();
            int idx = row - 1;
            if (idx < model.getRowCount()) {
                model.removeRecord(idx);
                deleted++;
            }
        }
        if (deleted == 0) throw new MCPServer.MCPException(
            -32602,
            "Row/sheet not found in any target environment."
        );
        env.save();
        p.save();
        return json
            .createObjectNode()
            .put("deleted", true)
            .put("sheet", sheet)
            .put("row", row)
            .put("environments", deleted);
    }

    private JsonNode dataImport(ObjectMapper json, JsonNode args) {
        Project p = loadProject(resolveProject(projectArg(args)));
        String filePath = MCPServer.requiredParam(args, "file");
        File src = new File(filePath);
        if (!src.isFile()) throw new MCPServer.MCPException(
            -32602,
            "Source file not found: " + filePath
        );
        List<String> lines = readLines(src);
        if (lines.isEmpty()) throw new MCPServer.MCPException(-32602, "Source file is empty.");
        String defaultSheet = src.getName().replaceFirst("(?i)\\.(csv|txt)$", "");
        String sheet = MCPServer.paramOrDefault(args, "sheet", defaultSheet);
        String envName = MCPServer.paramOrDefault(args, "env", null);
        String[] header = lines.get(0).split(",", -1);

        com.ing.datalib.component.EnvTestData env = p.getTestData();
        java.util.Collection<com.ing.datalib.component.TestData> targets = com.ing.engine.cli.commands.DataCommand.pickEnvs(
            env,
            envName
        );
        if (targets.isEmpty()) throw new MCPServer.MCPException(
            -32602,
            "Environment not found: " + envName
        );
        int rowsImported = 0;
        int envCount = 0;
        for (com.ing.datalib.component.TestData td : targets) {
            com.ing.datalib.testdata.model.TestDataModel model = td.getByName(sheet);
            if (model == null) model = td.addTestData(td.getNewTestData(sheet));
            model.loadTableModel();
            for (String col : header) {
                if (!col.trim().isEmpty() && model.getColumnIndex(col) < 0) model.addColumn(col);
            }
            for (int i = 1; i < lines.size(); i++) {
                if (lines.get(i).trim().isEmpty()) continue;
                String[] cells = lines.get(i).split(",", -1);
                model.addRecord();
                int rowIdx = model.getRowCount() - 1;
                for (int j = 0; j < header.length && j < cells.length; j++) {
                    if (header[j].trim().isEmpty()) continue;
                    int col = model.getColumnIndex(header[j]);
                    if (col >= 0) model.setValueAt(cells[j], rowIdx, col);
                }
                rowsImported++;
            }
            envCount++;
        }
        env.save();
        p.save();
        return json
            .createObjectNode()
            .put("imported", true)
            .put("sheet", sheet)
            .put("columns", header.length)
            .put("rowsPerEnv", lines.size() - 1)
            .put("environments", envCount);
    }

    // ==================================================================
    // report export (follow-up)
    // ==================================================================

    private JsonNode reportExport(ObjectMapper json, JsonNode args) {
        File dir = locateRunDir(args);
        String runId = MCPServer.paramOrDefault(args, "runId", null);
        File runDir = runId == null ? new File(dir, "Latest") : new File(dir, runId);
        if (!runDir.isDirectory()) throw new MCPServer.MCPException(
            -32602,
            "Run not found: " + (runId == null ? "Latest" : runId)
        );
        JsonNode data = readDataJs(json, runDir);
        String format = MCPServer.paramOrDefault(args, "format", "json").toLowerCase(Locale.ROOT);

        List<String[]> rows = new ArrayList<>(); // scenario, testcase, status
        int pass = 0, fail = 0;
        JsonNode ex = data.path("EXECUTIONS");
        if (ex.isArray()) {
            for (JsonNode tc : ex) {
                String scn = firstNonEmpty(tc, "scenarioName", "scenario", "TestScenario");
                String name = firstNonEmpty(tc, "testcaseName", "testcase", "testCase", "TestCase");
                String status = tc.path("status").asText("");
                if ("PASS".equalsIgnoreCase(status)) pass++; else if (
                    "FAIL".equalsIgnoreCase(status)
                ) fail++;
                rows.add(new String[] { scn, name, status });
            }
        }

        String body;
        String ext;
        switch (format) {
            case "csv":
                {
                    StringBuilder sb = new StringBuilder("Scenario,TestCase,Status\n");
                    for (String[] r : rows) sb.append(csvRow(r[0], r[1], r[2])).append('\n');
                    body = sb.toString();
                    ext = ".csv";
                    break;
                }
            case "junit":
                {
                    StringBuilder sb = new StringBuilder();
                    sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
                    sb
                        .append("<testsuite name=\"INGenious\" tests=\"")
                        .append(pass + fail)
                        .append("\" failures=\"")
                        .append(fail)
                        .append("\" errors=\"0\">\n");
                    for (String[] r : rows) {
                        sb
                            .append("  <testcase name=\"")
                            .append(xml(r[1]))
                            .append("\" classname=\"")
                            .append(xml(r[0]))
                            .append("\">");
                        if ("FAIL".equalsIgnoreCase(r[2])) sb.append(
                            "<failure message=\"Test failed\"/>"
                        );
                        sb.append("</testcase>\n");
                    }
                    sb.append("</testsuite>");
                    body = sb.toString();
                    ext = ".xml";
                    break;
                }
            default:
                body = data.toString();
                ext = ".json";
        }

        String output = MCPServer.paramOrDefault(args, "output", null);
        File outFile = output != null
            ? new File(output)
            : new File(System.getProperty("user.dir"), "report-" + runDir.getName() + ext);
        try {
            Files.writeString(outFile.toPath(), body, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new MCPServer.MCPException(-32603, "Failed to write export: " + e.getMessage());
        }
        return json
            .createObjectNode()
            .put("exported", true)
            .put("format", format)
            .put("path", outFile.getAbsolutePath())
            .put("testCases", rows.size())
            .put("pass", pass)
            .put("fail", fail);
    }

    private static String firstNonEmpty(JsonNode node, String... keys) {
        for (String k : keys) {
            String v = node.path(k).asText("");
            if (v != null && !v.isEmpty()) return v;
        }
        return "";
    }

    private static String xml(String s) {
        return s == null
            ? ""
            : s
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    // ==================================================================
    // config drivers + doctor (diagnostics)
    // ==================================================================

    private JsonNode configDrivers(ObjectMapper json, JsonNode args) {
        ObjectNode out = json.createObjectNode();
        ArrayNode drivers = out.putArray("drivers");
        for (String d : new String[] {
            "chromedriver",
            "geckodriver",
            "msedgedriver",
            "safaridriver"
        }) {
            ObjectNode n = drivers.addObject();
            n.put("driver", d);
            boolean present = onPath(d);
            n.put("present", present);
            n.put("version", present ? commandVersion(d) : "");
        }
        ObjectNode pw = out.putObject("playwrightCli");
        List<String> base = playwrightCliBase();
        pw.put("available", base != null);
        pw.put("invocation", base == null ? "" : String.join(" ", base));
        return out;
    }

    private JsonNode doctor(ObjectMapper json, JsonNode args) {
        ObjectNode out = json.createObjectNode();
        // JDK
        ObjectNode jdk = out.putObject("jdk");
        jdk.put("version", System.getProperty("java.version"));
        jdk.put("home", System.getProperty("java.home"));
        // Playwright Agent CLI
        List<String> base = playwrightCliBase();
        ObjectNode pw = out.putObject("playwrightCli");
        pw.put("available", base != null);
        pw.put("invocation", base == null ? "" : String.join(" ", base));
        pw.put("hint", base == null ? "Install with: npm i -g @playwright/cli" : "OK");
        // drivers
        ArrayNode drivers = out.putArray("drivers");
        for (String d : new String[] { "chromedriver", "geckodriver", "msedgedriver" }) {
            ObjectNode n = drivers.addObject();
            n.put("driver", d);
            n.put("present", onPath(d));
        }
        // k6 load generator (Performance Studio)
        ObjectNode k6 = out.putObject("k6");
        String k6Path = com.ing.engine.perf.K6Locator.resolve();
        k6.put("available", k6Path != null);
        k6.put("path", k6Path == null ? "" : k6Path);
        String k6Version = k6Path == null ? null : com.ing.engine.perf.K6Locator.version(k6Path);
        k6.put("version", k6Version == null ? "" : k6Version);
        k6.put("hint", k6Path == null ? com.ing.engine.perf.K6Locator.installHint() : "OK");
        // optional project health
        String proj = MCPServer.paramOrDefault(args, "project", defaultProject);
        if (proj != null && !proj.isEmpty()) {
            try {
                File dir = resolveProject(proj);
                ObjectNode p = out.putObject("project");
                p.put("name", dir.getName());
                p.put("path", dir.getAbsolutePath());
                ArrayNode missing = p.putArray("missingFolders");
                for (String d : new String[] {
                    "TestPlan",
                    "ReusableComponents",
                    "ObjectRepository",
                    "TestData",
                    "TestLab",
                    "Settings",
                    "Configuration",
                    "Results"
                }) {
                    if (!new File(dir, d).isDirectory()) missing.add(d);
                }
                p.put("healthy", missing.size() == 0);
            } catch (MCPServer.MCPException ex) {
                out.putObject("project").put("error", ex.getMessage());
            }
        }
        return out;
    }

    // ==================================================================
    // Performance Studio (k6) — Phase 1
    // ==================================================================

    private JsonNode perfExport(ObjectMapper json, JsonNode args) {
        String target = MCPServer.requiredParam(args, "target");
        String profileName = MCPServer.paramOrDefault(args, "profile", "smoke");
        boolean force = boolArg(args, "force", false);
        boolean isHar = target.toLowerCase(Locale.ROOT).endsWith(".har");
        String type = MCPServer.paramOrDefault(args, "type", "http");
        boolean browserType = "browser".equalsIgnoreCase(type);
        if (!browserType && !"http".equalsIgnoreCase(type)) {
            throw new MCPServer.MCPException(-32602, "type must be http or browser");
        }
        if (browserType && isHar) {
            throw new MCPServer.MCPException(
                -32602,
                "type=browser applies to test cases only; HAR recordings export as type=http"
            );
        }

        File projectDir;
        String baseName;
        String source;
        String regenerate;
        java.util.List<com.ing.engine.perf.HttpRequestSpec> requests = null;
        com.ing.engine.perf.K6BrowserScriptGenerator.Result browserGen = null;
        com.ing.engine.perf.RuleEngine.Result appliedRules = null;
        int proposedRules = 0;
        java.util.List<String> warnings;

        if (isHar) {
            File har = new File(target);
            if (!har.isFile()) {
                throw new MCPServer.MCPException(-32602, "HAR file not found: " + target);
            }
            projectDir = resolveProject(MCPServer.paramOrDefault(args, "project", defaultProject));
            com.ing.engine.perf.HarReader.Result read;
            try {
                read =
                    com.ing.engine.perf.HarReader.read(
                        har,
                        MCPServer.paramOrDefault(args, "urlFilter", null),
                        boolArg(args, "includeStatic", false)
                    );
            } catch (Exception e) {
                throw new MCPServer.MCPException(-32603, "Failed to parse HAR: " + e.getMessage());
            }
            requests = read.requests;
            warnings = read.warnings;
            baseName = har.getName().replaceAll("\\.har$", "");
            source = har.getName();
            regenerate =
                "ingenious perf export \"" + target + "\" --type http --profile " + profileName;
            // rules: load persisted set, optionally auto-propose, then apply
            try {
                File rulesFile = com.ing.engine.perf.PerfRule.defaultRulesFile(
                    new com.ing.engine.perf.PerfWorkspace(projectDir),
                    baseName
                );
                java.util.List<com.ing.engine.perf.PerfRule> ruleList = com.ing.engine.perf.PerfRule.load(
                    rulesFile
                );
                if (boolArg(args, "autoCorrelate", false)) {
                    for (com.ing.engine.perf.PerfRule proposal : com.ing.engine.perf.RuleEngine.proposeCorrelations(
                        requests
                    )) {
                        boolean duplicate = false;
                        for (com.ing.engine.perf.PerfRule existing : ruleList) {
                            if (
                                existing.type.equals(proposal.type) &&
                                existing.value.equals(proposal.value)
                            ) {
                                duplicate = true;
                                break;
                            }
                        }
                        if (!duplicate) {
                            ruleList.add(proposal);
                            proposedRules++;
                        }
                    }
                    if (proposedRules > 0) {
                        com.ing.engine.perf.PerfRule.save(ruleList, rulesFile);
                    }
                }
                if (!ruleList.isEmpty()) {
                    appliedRules = com.ing.engine.perf.RuleEngine.apply(requests, ruleList);
                    warnings.addAll(appliedRules.warnings);
                }
            } catch (Exception e) {
                warnings.add("Rules processing failed: " + e.getMessage());
            }
        } else {
            String[] parts = target.split("/");
            if (parts.length != 3) {
                throw new MCPServer.MCPException(
                    -32602,
                    "target must be <Project>/<Scenario>/<TestCase> or a .har path"
                );
            }
            projectDir = resolveProject(parts[0]);
            Project p = loadProject(projectDir);
            Scenario s = p.getScenarioByName(parts[1]);
            if (s == null) {
                throw notFound(
                    -32602,
                    "Scenario not found: " + parts[1],
                    scenarioNames(p),
                    parts[1]
                );
            }
            TestCase tc = s.getTestCaseByName(parts[2]);
            if (tc == null) {
                throw notFound(
                    -32602,
                    "Test case not found: " + parts[1] + "/" + parts[2],
                    testCaseNames(s),
                    parts[2]
                );
            }
            if (browserType) {
                browserGen = com.ing.engine.perf.K6BrowserScriptGenerator.fromTestCase(p, tc);
                warnings = browserGen.warnings;
            } else {
                com.ing.engine.perf.K6HttpScriptGenerator.Result gen = com.ing.engine.perf.K6HttpScriptGenerator.fromTestCase(
                    p,
                    tc
                );
                requests = gen.requests;
                warnings = gen.warnings;
            }
            baseName = parts[2];
            source = "TestPlan/" + parts[1] + "/" + parts[2];
            regenerate =
                "ingenious perf export \"" +
                target +
                "\" --type " +
                (browserType ? "browser" : "http") +
                " --profile " +
                profileName;
        }

        boolean nothing = browserType
            ? (browserGen == null || browserGen.actions == 0)
            : (requests == null || requests.isEmpty());
        if (nothing) {
            throw new MCPServer.MCPException(
                -32602,
                "Nothing to export: " + String.join("; ", warnings)
            );
        }
        com.ing.engine.perf.PerfProfile profile = com.ing.engine.perf.PerfProfile.resolve(
            profileName,
            projectDir
        );
        if (profile == null) {
            throw new MCPServer.MCPException(
                -32602,
                "Unknown profile: " +
                profileName +
                " (built-ins: smoke, average, stress, spike, soak)"
            );
        }
        String script;
        int itemCount;
        if (browserType) {
            script =
                com.ing.engine.perf.K6BrowserScriptGenerator.generate(
                    source,
                    regenerate,
                    profile,
                    browserGen.lines,
                    browserGen.warnings
                );
            itemCount = browserGen.actions;
        } else {
            script =
                com.ing.engine.perf.K6HttpScriptGenerator.generate(
                    source,
                    regenerate,
                    profile,
                    requests,
                    warnings,
                    appliedRules
                );
            itemCount = requests.size();
        }
        com.ing.engine.perf.PerfWorkspace ws = new com.ing.engine.perf.PerfWorkspace(projectDir);
        ws.ensure();
        File scriptFile = new File(ws.scriptsDir(), baseName + ".js");
        if (
            scriptFile.exists() &&
            com.ing.engine.perf.ScriptProvenance.isHandEdited(scriptFile) &&
            !force
        ) {
            throw new MCPServer.MCPException(
                -32602,
                "Refusing to overwrite hand-edited script: " + scriptFile + " (pass force=true)"
            );
        }
        try {
            java.nio.file.Files.write(
                scriptFile.toPath(),
                script.getBytes(java.nio.charset.StandardCharsets.UTF_8)
            );
        } catch (IOException e) {
            throw new MCPServer.MCPException(-32603, "Failed to write script: " + e.getMessage());
        }
        ObjectNode out = json.createObjectNode();
        out.put("script", scriptFile.getAbsolutePath());
        out.put("type", browserType ? "browser" : "http");
        out.put(browserType ? "actions" : "requests", itemCount);
        out.put("profile", profile.name);
        if (appliedRules != null) {
            out.put("rulesApplied", appliedRules.applied);
        }
        if (proposedRules > 0) {
            out.put("rulesProposed", proposedRules);
        }
        ArrayNode warn = out.putArray("warnings");
        for (String w : warnings) warn.add(w);
        out.put(
            "next",
            "Validate before load: ingenious_perf_validate {script: '" + baseName + "'}"
        );
        return out;
    }

    private JsonNode perfRun(ObjectMapper json, JsonNode args) {
        return perfExecute(json, args, false);
    }

    private JsonNode perfValidate(ObjectMapper json, JsonNode args) {
        return perfExecute(json, args, true);
    }

    private JsonNode perfExecute(ObjectMapper json, JsonNode args, boolean validate) {
        String k6 = com.ing.engine.perf.K6Locator.resolve();
        if (k6 == null) {
            throw new MCPServer.MCPException(
                -32603,
                "k6 not found. " + com.ing.engine.perf.K6Locator.installHint()
            );
        }
        String scriptArg = MCPServer.requiredParam(args, "script");
        File projectDir = null;
        String proj = MCPServer.paramOrDefault(args, "project", defaultProject);
        if (proj != null && !proj.isEmpty()) {
            projectDir = resolveProject(proj);
        }
        File script = com.ing.engine.perf.PerfWorkspace.resolveScript(scriptArg, projectDir);
        if (script == null) {
            throw new MCPServer.MCPException(
                -32602,
                "Script not found: " + scriptArg + " (export one with ingenious_perf_export)"
            );
        }
        if (projectDir == null) {
            projectDir = com.ing.engine.perf.PerfWorkspace.projectDirOfScript(script);
        }
        if (projectDir == null) {
            throw new MCPServer.MCPException(
                -32602,
                "Cannot determine the project for results; pass 'project'."
            );
        }
        com.ing.engine.perf.PerfWorkspace ws = new com.ing.engine.perf.PerfWorkspace(projectDir);
        com.ing.engine.perf.K6Runner.RunResult run;
        try {
            if (validate) {
                run = com.ing.engine.perf.K6Runner.validate(k6, script, ws);
            } else {
                java.util.List<String> extra = new ArrayList<>();
                if (args != null && args.has("vus")) {
                    extra.add("--vus");
                    extra.add(String.valueOf(args.get("vus").asInt()));
                }
                String duration = MCPServer.paramOrDefault(args, "duration", null);
                if (duration != null) {
                    extra.add("--duration");
                    extra.add(duration);
                }
                run = com.ing.engine.perf.K6Runner.runCaptured(k6, script, ws, "mcp", extra);
            }
        } catch (Exception e) {
            throw new MCPServer.MCPException(-32603, "k6 execution failed: " + e.getMessage());
        }
        ObjectNode out = json.createObjectNode();
        out.put("script", script.getAbsolutePath());
        out.put("runDir", run.runDir.getAbsolutePath());
        out.put("exitCode", run.exitCode);
        out.put("thresholdsFailed", run.thresholdsFailed);
        out.put("passed", run.exitCode == 0);
        perfAttachSummary(out, run.runDir);
        if (run.output != null) {
            out.put("outputTail", tailLines(run.output, 60));
        }
        return out;
    }

    private JsonNode perfReport(ObjectMapper json, JsonNode args) {
        File projectDir = resolveProject(MCPServer.paramOrDefault(args, "project", defaultProject));
        com.ing.engine.perf.PerfWorkspace ws = new com.ing.engine.perf.PerfWorkspace(projectDir);
        String mode = MCPServer.paramOrDefault(args, "mode", "latest");
        ObjectNode out = json.createObjectNode();
        if ("history".equalsIgnoreCase(mode)) {
            ArrayNode runs = out.putArray("runs");
            for (File run : ws.listRuns()) {
                ObjectNode n = runs.addObject();
                n.put("script", run.getParentFile().getName());
                n.put("timestamp", run.getName());
                n.put("path", run.getAbsolutePath());
                JsonNode meta = com.ing.engine.perf.PerfReportStore.runMeta(run);
                if (meta != null) {
                    n.put("exitCode", meta.path("exitCode").asInt());
                    n.put("profile", meta.path("profile").asText(""));
                }
            }
            return out;
        }
        File latest = com.ing.engine.perf.PerfReportStore.latestRunDir(ws);
        if (latest == null) {
            out.put("message", "No performance runs yet. Use ingenious_perf_run first.");
            return out;
        }
        out.put("script", latest.getParentFile().getName());
        out.put("timestamp", latest.getName());
        out.put("runDir", latest.getAbsolutePath());
        JsonNode meta = com.ing.engine.perf.PerfReportStore.runMeta(latest);
        if (meta != null) {
            out.set("meta", meta);
        }
        perfAttachSummary(out, latest);
        return out;
    }

    private void perfAttachSummary(ObjectNode out, File runDir) {
        Map<String, String> headline = com.ing.engine.perf.PerfReportStore.headline(runDir);
        if (!headline.isEmpty()) {
            ObjectNode h = out.putObject("summary");
            for (Map.Entry<String, String> e : headline.entrySet()) {
                h.put(e.getKey(), e.getValue());
            }
        }
        Map<String, Boolean> thresholds = com.ing.engine.perf.PerfReportStore.thresholds(runDir);
        if (!thresholds.isEmpty()) {
            ObjectNode t = out.putObject("thresholds");
            for (Map.Entry<String, Boolean> e : thresholds.entrySet()) {
                t.put(e.getKey(), e.getValue());
            }
        }
    }

    /** Live HAR recordings started via ingenious_perf_record_start. */
    private final Map<String, com.ing.engine.perf.PerfRecorder.Session> perfRecordings = new ConcurrentHashMap<>();

    private JsonNode perfRecordStart(ObjectMapper json, JsonNode args) {
        String url = MCPServer.requiredParam(args, "url");
        File projectDir = resolveProject(MCPServer.paramOrDefault(args, "project", defaultProject));
        com.ing.engine.perf.PerfWorkspace ws = new com.ing.engine.perf.PerfWorkspace(projectDir);
        ws.ensure();
        File harFile = new File(
            ws.recordingsDir(),
            com.ing.engine.perf.PerfRecorder.defaultName(url)
        );
        com.ing.engine.perf.PerfRecorder.Session session;
        try {
            session =
                com.ing.engine.perf.PerfRecorder.start(
                    url,
                    harFile,
                    boolArg(args, "headless", false)
                );
        } catch (Exception e) {
            throw new MCPServer.MCPException(
                -32603,
                "Failed to start recording: " + e.getMessage()
            );
        }
        perfRecordings.put(session.id, session);
        ObjectNode out = json.createObjectNode();
        out.put("recordingId", session.id);
        out.put("url", url);
        out.put("harFile", harFile.getAbsolutePath());
        out.put(
            "next",
            "Interact with the browser (or drive the flow), then call ingenious_perf_record_stop."
        );
        return out;
    }

    private JsonNode perfRecordStop(ObjectMapper json, JsonNode args) {
        String id = MCPServer.requiredParam(args, "recordingId");
        com.ing.engine.perf.PerfRecorder.Session session = perfRecordings.remove(id);
        if (session == null) {
            throw new MCPServer.MCPException(-32602, "Unknown recordingId: " + id);
        }
        File har = session.stop();
        ObjectNode out = json.createObjectNode();
        out.put("harFile", har.getAbsolutePath());
        out.put("exists", har.isFile());
        out.put("bytes", har.isFile() ? har.length() : 0);
        out.put(
            "next",
            "Generate a script: ingenious_perf_export {target: '" +
            har.getAbsolutePath() +
            "', urlFilter: '<host>'}"
        );
        return out;
    }

    // ==================================================================
    // Performance Studio — async runs + live metrics (Phase 4)
    // ==================================================================

    private JsonNode perfRunAsync(ObjectMapper json, JsonNode args) {
        String k6 = com.ing.engine.perf.K6Locator.resolve();
        if (k6 == null) {
            throw new MCPServer.MCPException(
                -32603,
                "k6 not found. " + com.ing.engine.perf.K6Locator.installHint()
            );
        }
        String scriptArg = MCPServer.requiredParam(args, "script");
        File projectDir = null;
        String proj = MCPServer.paramOrDefault(args, "project", defaultProject);
        if (proj != null && !proj.isEmpty()) {
            projectDir = resolveProject(proj);
        }
        File script = com.ing.engine.perf.PerfWorkspace.resolveScript(scriptArg, projectDir);
        if (script == null) {
            throw new MCPServer.MCPException(-32602, "Script not found: " + scriptArg);
        }
        if (projectDir == null) {
            projectDir = com.ing.engine.perf.PerfWorkspace.projectDirOfScript(script);
        }
        if (projectDir == null) {
            throw new MCPServer.MCPException(
                -32602,
                "Cannot determine the project for results; pass 'project'."
            );
        }
        java.util.List<String> extra = new ArrayList<>();
        if (args != null && args.has("vus")) {
            extra.add("--vus");
            extra.add(String.valueOf(args.get("vus").asInt()));
        }
        String duration = MCPServer.paramOrDefault(args, "duration", null);
        if (duration != null) {
            extra.add("--duration");
            extra.add(duration);
        }
        boolean dashboard = boolArg(args, "dashboard", true);
        com.ing.engine.perf.PerfRunHandle handle;
        try {
            handle =
                com.ing.engine.perf.K6Runner.startAsync(
                    k6,
                    script,
                    new com.ing.engine.perf.PerfWorkspace(projectDir),
                    "mcp",
                    extra,
                    dashboard
                );
        } catch (Exception e) {
            throw new MCPServer.MCPException(-32603, "Failed to start k6: " + e.getMessage());
        }
        ObjectNode out = json.createObjectNode();
        out.put("runId", handle.runId);
        out.put("pid", handle.pid);
        out.put("runDir", handle.runDir.getAbsolutePath());
        out.put("status", "RUNNING");
        if (handle.dashboardUrl() != null) {
            out.put("dashboardUrl", handle.dashboardUrl());
        }
        out.put(
            "next",
            "Poll ingenious_perf_status {runId: '" +
            handle.runId +
            "'} for live metrics; perf_cancel to stop early."
        );
        return out;
    }

    private com.ing.engine.perf.PerfRunHandle resolvePerfRun(JsonNode args) {
        File projectDir = resolveProject(MCPServer.paramOrDefault(args, "project", defaultProject));
        com.ing.engine.perf.PerfWorkspace ws = new com.ing.engine.perf.PerfWorkspace(projectDir);
        String runId = MCPServer.paramOrDefault(args, "runId", null);
        com.ing.engine.perf.PerfRunHandle handle = runId != null
            ? com.ing.engine.perf.PerfRunRegistry.find(ws, runId)
            : com.ing.engine.perf.PerfRunRegistry.latestRunning(ws);
        if (handle == null) {
            throw new MCPServer.MCPException(
                -32602,
                runId != null
                    ? "Run not found: " + runId
                    : "No running k6 run found (pass runId; see ingenious_perf_report mode=history)."
            );
        }
        return handle;
    }

    private JsonNode perfStatus(ObjectMapper json, JsonNode args) {
        com.ing.engine.perf.PerfRunHandle handle = resolvePerfRun(args);
        ObjectNode out = json.createObjectNode();
        out.put("runId", handle.runId);
        String phase = handle.phase();
        out.put("status", phase);
        if ("DRAINING".equals(phase)) {
            out.put(
                "message",
                "Test complete; k6 is waiting for dashboard viewers to disconnect. " +
                "Close the dashboard tab (or call ingenious_perf_cancel) to flush the summary."
            );
        }
        if (!"FINISHED".equals(phase)) {
            Map<String, String> live = com.ing.engine.perf.K6MetricsTap.snapshot(handle.apiPort);
            ObjectNode metrics = out.putObject("live");
            for (Map.Entry<String, String> e : live.entrySet()) {
                metrics.put(e.getKey(), e.getValue());
            }
            if (handle.dashboardUrl() != null) {
                out.put("dashboardUrl", handle.dashboardUrl());
            }
        } else {
            com.ing.engine.perf.K6Runner.reconcileRunMeta(handle);
            JsonNode meta = com.ing.engine.perf.PerfReportStore.runMeta(handle.runDir);
            if (meta != null) {
                out.put("exitCode", meta.path("exitCode").asInt(-1));
                out.put("thresholdsFailed", meta.path("thresholdsFailed").asBoolean(false));
            }
            perfAttachSummary(out, handle.runDir);
        }
        return out;
    }

    private JsonNode perfLogs(ObjectMapper json, JsonNode args) {
        com.ing.engine.perf.PerfRunHandle handle = resolvePerfRun(args);
        int lines = args != null && args.has("lines") ? args.get("lines").asInt(40) : 40;
        File log = new File(handle.runDir, "output.log");
        ObjectNode out = json.createObjectNode();
        out.put("runId", handle.runId);
        if (!log.isFile()) {
            out.put("logs", "");
            return out;
        }
        try {
            String content = new String(
                java.nio.file.Files.readAllBytes(log.toPath()),
                java.nio.charset.StandardCharsets.UTF_8
            );
            out.put("logs", tailLines(content, Math.max(1, lines)));
        } catch (IOException e) {
            throw new MCPServer.MCPException(-32603, "Cannot read logs: " + e.getMessage());
        }
        return out;
    }

    private JsonNode perfCancel(ObjectMapper json, JsonNode args) {
        com.ing.engine.perf.PerfRunHandle handle = resolvePerfRun(args);
        ObjectNode out = json.createObjectNode();
        out.put("runId", handle.runId);
        if (!handle.isAlive()) {
            out.put("status", "FINISHED");
            out.put("message", "Run already finished.");
            return out;
        }
        boolean down = handle.cancel();
        com.ing.engine.perf.K6Runner.reconcileRunMeta(handle);
        out.put("cancelled", down);
        out.put("status", down ? "CANCELLED" : "RUNNING");
        return out;
    }

    private JsonNode perfScale(ObjectMapper json, JsonNode args) {
        if (args == null || !args.has("vus")) {
            throw new MCPServer.MCPException(-32602, "Missing required parameter: vus");
        }
        int vus = args.get("vus").asInt();
        com.ing.engine.perf.PerfRunHandle handle = resolvePerfRun(args);
        if (!handle.isAlive()) {
            throw new MCPServer.MCPException(-32602, "Run already finished: " + handle.runId);
        }
        boolean ok = com.ing.engine.perf.K6MetricsTap.scale(handle.apiPort, vus);
        ObjectNode out = json.createObjectNode();
        out.put("runId", handle.runId);
        out.put("scaled", ok);
        out.put("vus", vus);
        if (!ok) {
            out.put(
                "message",
                "k6 rejected the scale request (executor may not support external VU control)."
            );
        }
        return out;
    }

    private JsonNode perfCompare(ObjectMapper json, JsonNode args) {
        String baselineId = MCPServer.requiredParam(args, "baseline");
        String candidateId = MCPServer.requiredParam(args, "candidate");
        File projectDir = resolveProject(MCPServer.paramOrDefault(args, "project", defaultProject));
        com.ing.engine.perf.PerfWorkspace ws = new com.ing.engine.perf.PerfWorkspace(projectDir);
        File baseline = new File(ws.resultsDir(), baselineId);
        File candidate = new File(ws.resultsDir(), candidateId);
        if (!new File(baseline, "summary.json").isFile()) {
            throw new MCPServer.MCPException(-32602, "No summary.json for baseline: " + baselineId);
        }
        if (!new File(candidate, "summary.json").isFile()) {
            throw new MCPServer.MCPException(
                -32602,
                "No summary.json for candidate: " + candidateId
            );
        }
        java.util.List<String> thresholdRegressions = new ArrayList<>();
        java.util.List<com.ing.engine.perf.PerfReportStore.CompareRow> rows = com.ing.engine.perf.PerfReportStore.compare(
            baseline,
            candidate,
            thresholdRegressions
        );
        ObjectNode out = json.createObjectNode();
        out.put("baseline", baselineId);
        out.put("candidate", candidateId);
        ArrayNode metrics = out.putArray("metrics");
        boolean anyRegression = !thresholdRegressions.isEmpty();
        for (com.ing.engine.perf.PerfReportStore.CompareRow row : rows) {
            anyRegression |= row.regression;
            ObjectNode n = metrics.addObject();
            n.put("metric", row.metric);
            n.put("baseline", row.baseline);
            n.put("candidate", row.candidate);
            n.put("deltaPercent", Math.round(row.deltaPercent * 10.0) / 10.0);
            n.put("regression", row.regression);
        }
        ArrayNode regressed = out.putArray("thresholdRegressions");
        for (String t : thresholdRegressions) {
            regressed.add(t);
        }
        out.put("regression", anyRegression);
        return out;
    }

    // ==================================================================
    // run dry-run (follow-up)
    // ==================================================================

    private JsonNode runDry(ObjectMapper json, JsonNode args) {
        String target = MCPServer.requiredParam(args, "target");
        String[] parts = target.split("/");
        if (parts.length != 3) {
            throw new MCPServer.MCPException(
                -32602,
                "target must be <Project>/<Scenario>/<TestCase> or <Project>/<Release>/<TestSet>"
            );
        }
        Project p = loadProject(resolveProject(parts[0]));
        ObjectNode out = json.createObjectNode();
        out.put("target", target);
        // Try test case first
        Scenario s = p.getScenarioByName(parts[1]);
        if (s != null && s.getTestCaseByName(parts[2]) != null) {
            TestCase tc = s.getTestCaseByName(parts[2]);
            ensureLoaded(tc);
            out.put("resolved", true);
            out.put("type", "testcase");
            out.put("scenario", parts[1]);
            out.put("testcase", parts[2]);
            out.put("steps", tc.getTestSteps().size());
            return out;
        }
        // Then test set
        com.ing.datalib.component.Release rel = p.getReleaseByName(parts[1]);
        if (rel != null) {
            com.ing.datalib.component.TestSet ts = rel.getTestSetByName(parts[2]);
            if (ts != null) {
                ts.loadTestSetTableModel();
                out.put("resolved", true);
                out.put("type", "testset");
                out.put("release", parts[1]);
                out.put("testset", parts[2]);
                out.put("rows", ts.getExecutableSteps().size());
                return out;
            }
        }
        out.put("resolved", false);
        out.put("message", "No matching test case (TestPlan) or test set (TestLab) for: " + target);
        return out;
    }

    // ==================================================================
    // Phase 3: archetype-driven generation
    // ==================================================================

    private JsonNode genList(ObjectMapper json, JsonNode args) {
        String category = MCPServer.paramOrDefault(args, "category", null);
        ArrayNode out = json.createArrayNode();
        for (ArchetypeCatalog.Archetype a : ArchetypeCatalog.all()) {
            if (category != null && !a.category.equalsIgnoreCase(category)) continue;
            ObjectNode n = out.addObject();
            n.put("name", a.name);
            n.put("category", a.category);
            n.put("description", a.description);
            ArrayNode params = n.putArray("parameters");
            for (String p : a.parameters) params.add(p);
            n.put("steps", a.steps.size());
        }
        return out;
    }

    private JsonNode genTestCase(ObjectMapper json, JsonNode args) {
        String archName = MCPServer.requiredParam(args, "archetype");
        ArchetypeCatalog.Archetype a = ArchetypeCatalog.find(archName);
        if (a == null) throw notFound(
            -32602,
            "Unknown archetype: " + archName + " (see ingenious_gen_list)",
            archetypeNames(),
            archName
        );
        Map<String, String> values = readStringMap(args == null ? null : args.get("params"));
        ObjectNode synthetic = json.createObjectNode();
        if (args != null && args.has("project")) synthetic.set("project", args.get("project"));
        if (boolArg(args, "dryRun", false)) synthetic.put("dryRun", true);
        String ifExistsFwd = MCPServer.paramOrDefault(args, "ifExists", null);
        if (ifExistsFwd != null) synthetic.put("ifExists", ifExistsFwd);
        synthetic.put("scenario", MCPServer.requiredParam(args, "scenario"));
        synthetic.put("testcase", MCPServer.requiredParam(args, "testcase"));
        synthetic.put("reusable", boolArg(args, "reusable", false));
        ArrayNode steps = synthetic.putArray("steps");
        Set<String> unresolved = new LinkedHashSet<>();
        for (ArchetypeCatalog.Step st : a.steps) {
            String object = ArchetypeCatalog.substitute(st.object, values);
            String input = ArchetypeCatalog.substitute(st.input, values);
            ObjectNode s = steps.addObject();
            s.put("action", st.action);
            s.put("object", object);
            s.put("input", input);
            s.put("description", st.description);
            unresolved.addAll(ArchetypeCatalog.unresolvedTokens(object));
            unresolved.addAll(ArchetypeCatalog.unresolvedTokens(input));
        }
        ObjectNode created = (ObjectNode) testCaseCreate(json, synthetic);
        created.put("archetype", a.name);
        ArrayNode ur = created.putArray("unresolvedParams");
        for (String t : unresolved) ur.add(t);
        // Optionally externalise the generated literals straight away so the
        // create -> parameterize workflow can be done in one call.
        if (
            boolArg(args, "parameterize", false) &&
            !boolArg(args, "dryRun", false) &&
            created.path("created").asBoolean(false)
        ) {
            ObjectNode pArgs = json.createObjectNode();
            if (args != null && args.has("project")) pArgs.set("project", args.get("project"));
            pArgs.put("scenario", MCPServer.requiredParam(args, "scenario"));
            pArgs.put("testcase", MCPServer.requiredParam(args, "testcase"));
            pArgs.put("reusable", boolArg(args, "reusable", false));
            pArgs.put("mode", "all");
            String sheet = MCPServer.paramOrDefault(args, "sheet", null);
            if (sheet != null) pArgs.put("sheet", sheet);
            try {
                created.set("parameterization", testCaseParameterize(json, pArgs));
            } catch (MCPServer.MCPException ex) {
                created.put("parameterizationSkipped", ex.getMessage());
            }
        }
        return created;
    }

    private JsonNode dataGenerate(ObjectMapper json, JsonNode args) {
        Project p = loadProject(resolveProject(projectArg(args)));
        String sheet = MCPServer.requiredParam(args, "sheet");
        int rows = intArg(args, "rows", 0);
        if (rows < 1) throw new MCPServer.MCPException(-32602, "rows must be >= 1");
        String envName = MCPServer.paramOrDefault(args, "env", null);
        long seed = intArg(args, "seed", -1);
        java.util.Random rnd = seed >= 0 ? new java.util.Random(seed) : new java.util.Random();

        List<String[]> colspec = new ArrayList<>(); // [name, type]
        JsonNode cols = args == null ? null : args.get("columns");
        if (cols != null && cols.isArray()) {
            for (JsonNode c : cols) {
                String cn = c.path("name").asText("");
                if (cn.isEmpty()) continue;
                colspec.add(new String[] { cn, c.path("type").asText("word") });
            }
        }
        if (colspec.isEmpty()) throw new MCPServer.MCPException(
            -32602,
            "At least one column {name,type} is required."
        );

        com.ing.datalib.component.EnvTestData env = p.getTestData();
        java.util.Collection<com.ing.datalib.component.TestData> targets = com.ing.engine.cli.commands.DataCommand.pickEnvs(
            env,
            envName
        );
        if (targets.isEmpty()) throw new MCPServer.MCPException(
            -32602,
            "Environment not found: " + envName
        );
        int envCount = 0;
        for (com.ing.datalib.component.TestData td : targets) {
            com.ing.datalib.testdata.model.TestDataModel model = td.getByName(sheet);
            if (model == null) model = td.addTestData(td.getNewTestData(sheet));
            model.loadTableModel();
            for (String[] cs : colspec) {
                if (model.getColumnIndex(cs[0]) < 0) model.addColumn(cs[0]);
            }
            for (int i = 0; i < rows; i++) {
                model.addRecord();
                int rowIdx = model.getRowCount() - 1;
                for (String[] cs : colspec) {
                    int col = model.getColumnIndex(cs[0]);
                    if (col >= 0) model.setValueAt(synthValue(cs[1], rnd, i), rowIdx, col);
                }
            }
            envCount++;
        }
        env.save();
        p.save();
        return json
            .createObjectNode()
            .put("generated", true)
            .put("sheet", sheet)
            .put("rowsPerEnv", rows)
            .put("columns", colspec.size())
            .put("environments", envCount);
    }

    private JsonNode genFromOpenApi(ObjectMapper json, JsonNode args) {
        String filePath = MCPServer.requiredParam(args, "file");
        File file = new File(filePath);
        if (!file.isFile()) throw new MCPServer.MCPException(
            -32602,
            "OpenAPI spec not found: " + filePath
        );
        String scenario = MCPServer.paramOrDefault(args, "scenario", "API");
        boolean reusable = boolArg(args, "reusable", false);
        JsonNode spec;
        try {
            // YAMLMapper reads JSON too (JSON is a YAML subset).
            spec = new com.fasterxml.jackson.dataformat.yaml.YAMLMapper().readTree(file);
        } catch (IOException e) {
            throw new MCPServer.MCPException(-32603, "Failed to parse OpenAPI: " + e.getMessage());
        }
        String base = MCPServer.paramOrDefault(args, "baseUrl", null);
        if (base == null || base.isEmpty()) {
            base = spec.path("servers").path(0).path("url").asText("");
        }
        JsonNode paths = spec.path("paths");
        if (!paths.isObject()) throw new MCPServer.MCPException(
            -32602,
            "No 'paths' object in the OpenAPI spec."
        );
        int created = 0, skipped = 0;
        Set<String> used = new LinkedHashSet<>();
        ArrayNode ops = json.createArrayNode();
        java.util.Iterator<Map.Entry<String, JsonNode>> pit = paths.fields();
        while (pit.hasNext()) {
            Map.Entry<String, JsonNode> pe = pit.next();
            String path = pe.getKey();
            for (String method : new String[] { "get", "post", "put", "patch", "delete" }) {
                JsonNode op = pe.getValue().get(method);
                if (op == null) continue;
                String url = base + path;
                String name = uniqueName(deriveApiName(method, path), used);
                ArrayNode steps = json.createArrayNode();
                addApiSteps(steps, method, url, op);
                try {
                    createGeneratedTestCase(json, args, scenario, name, reusable, steps);
                    created++;
                    ops.add(method.toUpperCase(Locale.ROOT) + " " + path);
                } catch (MCPServer.MCPException ex) {
                    skipped++;
                }
            }
        }
        return json
            .createObjectNode()
            .put("scenario", scenario)
            .put("created", created)
            .put("skipped", skipped)
            .set("operations", ops);
    }

    private JsonNode genFromHar(ObjectMapper json, JsonNode args) {
        String filePath = MCPServer.requiredParam(args, "file");
        File file = new File(filePath);
        if (!file.isFile()) throw new MCPServer.MCPException(
            -32602,
            "HAR file not found: " + filePath
        );
        String scenario = MCPServer.paramOrDefault(args, "scenario", "Recorded");
        String urlFilter = MCPServer.paramOrDefault(args, "urlFilter", null);
        boolean reusable = boolArg(args, "reusable", false);
        JsonNode har;
        try {
            har = new ObjectMapper().readTree(file);
        } catch (IOException e) {
            throw new MCPServer.MCPException(-32603, "Failed to parse HAR: " + e.getMessage());
        }
        JsonNode entries = har.path("log").path("entries");
        if (!entries.isArray()) throw new MCPServer.MCPException(
            -32602,
            "No log.entries array in the HAR file."
        );
        int created = 0, skipped = 0;
        Set<String> used = new LinkedHashSet<>();
        for (JsonNode entry : entries) {
            JsonNode req = entry.path("request");
            String method = req.path("method").asText("GET").toLowerCase(Locale.ROOT);
            String url = req.path("url").asText("");
            if (url.isEmpty()) continue;
            if (urlFilter != null && !url.contains(urlFilter)) continue;
            int status = entry.path("response").path("status").asInt(200);
            String body = req.path("postData").path("text").asText("");
            String name = uniqueName(deriveApiName(method, url), used);
            ArrayNode steps = json.createArrayNode();
            addStep(steps, "setEndPoint", "", url, "Set endpoint");
            String action = methodAction(method);
            addStep(steps, action, "", body, method.toUpperCase(Locale.ROOT) + " request");
            addStep(steps, "assertResponseCode", "", String.valueOf(status), "Verify status");
            try {
                createGeneratedTestCase(json, args, scenario, name, reusable, steps);
                created++;
            } catch (MCPServer.MCPException ex) {
                skipped++;
            }
        }
        return json
            .createObjectNode()
            .put("scenario", scenario)
            .put("created", created)
            .put("skipped", skipped);
    }

    // ---- Phase 3 helpers ---------------------------------------------

    private void createGeneratedTestCase(
        ObjectMapper json,
        JsonNode args,
        String scenario,
        String testcase,
        boolean reusable,
        ArrayNode steps
    ) {
        ObjectNode synthetic = json.createObjectNode();
        if (args != null && args.has("project")) synthetic.set("project", args.get("project"));
        synthetic.put("scenario", scenario);
        synthetic.put("testcase", testcase);
        synthetic.put("reusable", reusable);
        synthetic.set("steps", steps);
        testCaseCreate(json, synthetic);
    }

    private void addApiSteps(ArrayNode steps, String method, String url, JsonNode op) {
        addStep(steps, "setEndPoint", "", url, "Set endpoint");
        String action = methodAction(method);
        String body = "";
        if (
            ("post".equals(method) || "put".equals(method) || "patch".equals(method)) && op != null
        ) {
            addStep(steps, "addHeader", "", "Content-Type: application/json", "JSON content type");
        }
        addStep(steps, action, "", body, method.toUpperCase(Locale.ROOT) + " request");
        int status = firstSuccessStatus(op);
        addStep(steps, "assertResponseCode", "", String.valueOf(status), "Verify status code");
    }

    private static int firstSuccessStatus(JsonNode op) {
        if (op != null) {
            JsonNode responses = op.path("responses");
            if (responses.isObject()) {
                java.util.Iterator<String> it = responses.fieldNames();
                while (it.hasNext()) {
                    String code = it.next();
                    if (code.startsWith("2")) {
                        try {
                            return Integer.parseInt(code);
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        }
        return 200;
    }

    private static String methodAction(String method) {
        switch (method.toLowerCase(Locale.ROOT)) {
            case "post":
                return "postRestRequest";
            case "put":
                return "putRestRequest";
            case "patch":
                return "patchRestRequest";
            case "delete":
                return "deleteRestRequest";
            default:
                return "getRestRequest";
        }
    }

    private static void addStep(
        ArrayNode steps,
        String action,
        String object,
        String input,
        String description
    ) {
        ObjectNode s = steps.addObject();
        s.put("action", action);
        s.put("object", object);
        s.put("input", input);
        s.put("description", description);
    }

    private static String deriveApiName(String method, String pathOrUrl) {
        String path = pathOrUrl.replaceAll("https?://[^/]+", "");
        path = path.replaceAll("[?#].*$", "").replaceAll("[{}]", "").replaceAll("/+$", "");
        if (path.startsWith("/")) path = path.substring(1);
        if (path.isEmpty()) path = "root";
        String base = method.toUpperCase(Locale.ROOT) + "_" + path.replaceAll("[^A-Za-z0-9]+", "_");
        base = base.replaceAll("_+", "_").replaceAll("_+$", "");
        if (base.length() > 60) base = base.substring(0, 60);
        return base;
    }

    private static String uniqueName(String base, Set<String> used) {
        String candidate = base;
        int n = 2;
        while (used.contains(candidate)) candidate = base + "_" + (n++);
        used.add(candidate);
        return candidate;
    }

    private static Map<String, String> readStringMap(JsonNode obj) {
        Map<String, String> m = new LinkedHashMap<>();
        if (obj != null && obj.isObject()) {
            java.util.Iterator<Map.Entry<String, JsonNode>> it = obj.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                m.put(e.getKey(), e.getValue().asText());
            }
        }
        return m;
    }

    private static final String[] FIRST_NAMES = {
        "Alice",
        "Bob",
        "Carol",
        "David",
        "Eve",
        "Frank",
        "Grace",
        "Heidi",
        "Ivan",
        "Judy"
    };
    private static final String[] LAST_NAMES = {
        "Smith",
        "Jones",
        "Brown",
        "Taylor",
        "Wilson",
        "Davies",
        "Evans",
        "Thomas",
        "Roberts",
        "Walker"
    };
    private static final String[] CITIES = {
        "London",
        "Paris",
        "Berlin",
        "Tokyo",
        "Sydney",
        "Toronto",
        "Austin",
        "Dublin",
        "Oslo",
        "Madrid"
    };
    private static final String[] WORDS = {
        "alpha",
        "bravo",
        "charlie",
        "delta",
        "echo",
        "foxtrot",
        "golf",
        "hotel",
        "india",
        "juliet"
    };

    private static String synthValue(String type, java.util.Random rnd, int rowIndex) {
        String t = type == null ? "word" : type.toLowerCase(Locale.ROOT);
        String first = FIRST_NAMES[rnd.nextInt(FIRST_NAMES.length)];
        String last = LAST_NAMES[rnd.nextInt(LAST_NAMES.length)];
        switch (t) {
            case "name":
                return first + " " + last;
            case "firstname":
                return first;
            case "lastname":
                return last;
            case "email":
                return (
                    first.toLowerCase(Locale.ROOT) +
                    "." +
                    last.toLowerCase(Locale.ROOT) +
                    (rowIndex + 1) +
                    "@example.com"
                );
            case "phone":
                return String.format(
                    "+1-%03d-%03d-%04d",
                    rnd.nextInt(1000),
                    rnd.nextInt(1000),
                    rnd.nextInt(10000)
                );
            case "uuid":
                return UUID.randomUUID().toString();
            case "int":
                return String.valueOf(rnd.nextInt(1000));
            case "number":
                return String.format(Locale.ROOT, "%.2f", rnd.nextDouble() * 1000);
            case "bool":
                return rnd.nextBoolean() ? "true" : "false";
            case "date":
                {
                    java.time.LocalDate d = java.time.LocalDate.now().minusDays(rnd.nextInt(3650));
                    return d.toString();
                }
            case "city":
                return CITIES[rnd.nextInt(CITIES.length)];
            case "sentence":
                {
                    StringBuilder sb = new StringBuilder();
                    int words = 4 + rnd.nextInt(6);
                    for (int i = 0; i < words; i++) {
                        if (i > 0) sb.append(' ');
                        sb.append(WORDS[rnd.nextInt(WORDS.length)]);
                    }
                    return sb.toString();
                }
            case "word":
            default:
                return WORDS[rnd.nextInt(WORDS.length)];
        }
    }

    private static ObjectNode genColumnItemSchema(ObjectMapper json) {
        ObjectNode item = json.createObjectNode();
        item.put("type", "object");
        ObjectNode props = item.putObject("properties");
        props.putObject("name").put("type", "string").put("description", "Column name.");
        props
            .putObject("type")
            .put("type", "string")
            .put(
                "description",
                "Data type: name, firstname, lastname, email, phone, uuid, int, number, bool, date, city, word, sentence."
            );
        item.putArray("required").add("name");
        item.put("additionalProperties", true);
        return item;
    }

    // ==================================================================
    // Playwright Agent CLI - live browser authoring (Phase 4)
    // ==================================================================

    /**
     * Deterministic entry point for browser-flow discovery. Confirmed-intent
     * discoveries (objects/locators not yet in the Object Repository) route
     * here: it opens a live Playwright Agent CLI session at {@code url}, returns
     * the first accessibility snapshot, and pre-binds the target scenario /
     * test case / OR page onto the session so the follow-up
     * {@code ingenious_browser_session_save} can materialize everything with no
     * extra arguments. The only non-deterministic part is the exploration the
     * agent performs against the returned snapshot via
     * {@code ingenious_browser_session_do}.
     */
    private JsonNode browserDiscover(ObjectMapper json, JsonNode args) {
        String url = MCPServer.requiredParam(args, "url");
        String prompt = MCPServer.requiredParam(args, "prompt");
        String scenario = MCPServer.paramOrDefault(args, "scenario", null);
        String testcase = MCPServer.paramOrDefault(args, "testcase", null);
        String browser = MCPServer.paramOrDefault(args, "browser", "chromium");
        boolean headed = boolArg(args, "headed", false);
        boolean reusable = boolArg(args, "reusable", false);
        String session = MCPServer.paramOrDefault(
            args,
            "session",
            testcase != null && !testcase.isEmpty()
                ? sanitizeObjectName(testcase)
                : "discover-" + UUID.randomUUID().toString().substring(0, 8)
        );
        String page = MCPServer.paramOrDefault(
            args,
            "page",
            testcase != null && !testcase.isEmpty()
                ? capitalize(sanitizeObjectName(testcase)) + "Page"
                : null
        );

        // Fail fast (deterministically) if the CLI is not installed.
        if (playwrightCliBase() == null) {
            throw new MCPServer.MCPException(
                -32603,
                "Playwright Agent CLI not found. Install it with 'npm i -g @playwright/cli' " +
                "(or ensure 'npx' is on PATH), then retry. See ingenious_doctor."
            );
        }

        List<String> verb = new ArrayList<>();
        verb.add("open");
        verb.add(url);
        if (headed) verb.add("--headed");
        if (browser != null && !browser.isEmpty()) verb.add("--browser=" + browser);
        PwResult r = runPlaywright(session, verb, 120); // blocks until the CLI is ready

        PwSession s = new PwSession();
        s.name = session;
        s.browser = browser;
        s.startUrl = url;
        s.lastSnapshot = r.output;
        s.scenario = scenario;
        s.testcase = testcase;
        s.page = page;
        s.prompt = prompt;
        s.reusable = reusable;
        pwSessions.put(session, s);

        String protocol =
            "DETERMINISTIC BROWSER-DISCOVERY PROTOCOL (follow exactly):\n" +
            "1. Read the 'snapshot' below: interactive elements carry refs like e21.\n" +
            "2. Realise the flow described in 'prompt' one action at a time with\n" +
            "   ingenious_browser_session_do (session=\"" +
            session +
            "\"), e.g. command='fill e5 \"user@example.com\"' then 'click e21'.\n" +
            "   Wait for each call to return (it blocks on the CLI) and re-read the fresh\n" +
            "   snapshot before choosing the next ref. Do NOT invent refs or locators.\n" +
            "3. When the flow is complete call ingenious_browser_session_save\n" +
            "   (session=\"" +
            session +
            "\"); scenario/testcase/page are already bound. Discovered locators are\n" +
            "   translated into Object-Repository objects on page '" +
            (page == null ? "<derived>" : page) +
            "' and the recorded steps are linked to them automatically.\n" +
            "4. Finally call ingenious_browser_session_close (session=\"" +
            session +
            "\").";

        ObjectNode out = json.createObjectNode();
        out.put("session", session);
        out.put("url", url);
        out.put("prompt", prompt);
        out.put("browser", browser);
        if (scenario != null) out.put("scenario", scenario);
        if (testcase != null) out.put("testcase", testcase);
        if (page != null) out.put("page", page);
        out.put("exitCode", r.exitCode);
        out.put("protocol", protocol);
        out.put("snapshot", r.output);
        return out;
    }

    private JsonNode browserSessionStart(ObjectMapper json, JsonNode args) {
        String name = MCPServer.requiredParam(args, "name");
        String url = MCPServer.requiredParam(args, "url");
        String browser = MCPServer.paramOrDefault(args, "browser", "chromium");
        boolean headed = boolArg(args, "headed", false);
        List<String> verb = new ArrayList<>();
        verb.add("open");
        verb.add(url);
        if (headed) verb.add("--headed");
        if (browser != null && !browser.isEmpty()) verb.add("--browser=" + browser);
        PwResult r = runPlaywright(name, verb, 120);
        PwSession s = new PwSession();
        s.name = name;
        s.browser = browser;
        s.startUrl = url;
        s.lastSnapshot = r.output;
        pwSessions.put(name, s);
        return json
            .createObjectNode()
            .put("session", name)
            .put("browser", browser)
            .put("url", url)
            .put("exitCode", r.exitCode)
            .put("snapshot", r.output);
    }

    private JsonNode browserSessionDo(ObjectMapper json, JsonNode args) {
        String name = MCPServer.requiredParam(args, "name");
        PwSession s = pwSessions.get(name);
        if (s == null) throw new MCPServer.MCPException(
            -32602,
            "No such session: " + name + " (start one with ingenious_browser_session_start)"
        );
        String command = MCPServer.requiredParam(args, "command");
        List<String> tokens = tokenize(command);
        if (tokens.isEmpty()) throw new MCPServer.MCPException(-32602, "Empty command.");
        List<PlaywrightCliTranslator.Step> mapped = PlaywrightCliTranslator.translate(command);
        // Resolve each ref to a durable locator from the CURRENT (pre-action)
        // snapshot, while the ref is still valid. This is fully deterministic.
        Map<String, String[]> refs = parseSnapshotRefs(s.lastSnapshot);
        for (PlaywrightCliTranslator.Step st : mapped) {
            if (st.ref != null && refs.containsKey(st.ref)) {
                String[] rn = refs.get(st.ref);
                st.locator = ariaLocatorValue(rn[0], rn[1]);
            }
        }
        PwResult r = runPlaywright(name, tokens, 60);
        s.lastSnapshot = r.output;
        s.steps.addAll(mapped);
        ObjectNode out = json.createObjectNode();
        out.put("session", name);
        out.put("command", command);
        out.put("exitCode", r.exitCode);
        out.put("recordedSteps", s.steps.size());
        ArrayNode added = out.putArray("mappedSteps");
        for (PlaywrightCliTranslator.Step st : mapped) {
            ObjectNode so = added
                .addObject()
                .put("action", st.action)
                .put("object", st.object)
                .put("input", st.input);
            if (st.locator != null) so.put("locator", st.locator);
        }
        out.put("snapshot", r.output);
        return out;
    }

    private JsonNode browserSessionSnapshot(ObjectMapper json, JsonNode args) {
        String name = MCPServer.requiredParam(args, "name");
        PwSession s = pwSessions.get(name);
        if (s == null) throw new MCPServer.MCPException(-32602, "No such session: " + name);
        PwResult r = runPlaywright(name, Arrays.asList("snapshot"), 30);
        s.lastSnapshot = r.output;
        return json.createObjectNode().put("session", name).put("snapshot", r.output);
    }

    private JsonNode browserSessionSave(ObjectMapper json, JsonNode args) {
        String name = MCPServer.requiredParam(args, "name");
        PwSession s = pwSessions.get(name);
        if (s == null) throw new MCPServer.MCPException(-32602, "No such session: " + name);
        // scenario / testcase / reusable default to whatever ingenious_browser_discover
        // pre-bound on the session, so a discovery flow can save with no extra args.
        String scenName = MCPServer.paramOrDefault(args, "scenario", s.scenario);
        if (scenName == null || scenName.isEmpty()) scenName =
            MCPServer.requiredParam(args, "scenario");
        String tcName = MCPServer.paramOrDefault(args, "testcase", s.testcase);
        if (tcName == null || tcName.isEmpty()) tcName = MCPServer.requiredParam(args, "testcase");
        boolean reusable = boolArg(args, "reusable", s.reusable);
        String page = MCPServer.paramOrDefault(args, "page", s.page);
        if (page == null || page.isEmpty()) page = capitalize(sanitizeObjectName(tcName)) + "Page";
        Project p = loadProject(resolveProject(projectArg(args)));

        // ---- Materialize discovered locators into the Object Repository ----
        // Every recorded ref that carries a durable locator becomes (or reuses)
        // a WebOR object on `page`; the step is rewritten to reference it as
        // Page.object. Objects are written through the Datalib ObjectRepository
        // model, so they land as YAML (ObjectRepository/Web/<page>.yaml) - the
        // canonical format the engine and IDE actually load.
        int objectsCreated = materializeDiscoveredObjects(p, page, s.steps);

        // ---- Build the test case from the (now OR-linked) steps ----
        List<PlaywrightCliTranslator.Step> steps = new ArrayList<>();
        steps.add(new PlaywrightCliTranslator.Step("OpenBrowser", "", ""));
        if (s.startUrl != null && !s.startUrl.isEmpty()) {
            steps.add(new PlaywrightCliTranslator.Step("NavigateTo", "", s.startUrl));
        }
        steps.addAll(s.steps);
        steps.add(new PlaywrightCliTranslator.Step("CloseBrowser", "", ""));

        TestCase tc = buildTestCaseFromSteps(p, scenName, tcName, reusable, steps);
        p.save();
        return json
            .createObjectNode()
            .put("created", true)
            .put("scenario", scenName)
            .put("testcase", tcName)
            .put("reusable", reusable)
            .put("page", page)
            .put("objectsCreated", objectsCreated)
            .put("steps", tc.getTestSteps().size());
    }

    /** Derive an Object-Repository object name from an aria locator value. */
    private static String deriveNameFromAria(String locator) {
        java.util.regex.Matcher m = java
            .util.regex.Pattern.compile("name=\"([^\"]*)\"")
            .matcher(locator == null ? "" : locator);
        if (m.find() && !m.group(1).trim().isEmpty()) return sanitizeObjectName(m.group(1));
        java.util.regex.Matcher rm = java
            .util.regex.Pattern.compile("role=([a-zA-Z]+)")
            .matcher(locator == null ? "" : locator);
        return rm.find() ? sanitizeObjectName(rm.group(1)) : "element";
    }

    private static String uniqueObjectName(String base, Set<String> used) {
        String candidate = base;
        int n = 2;
        while (used.contains(candidate)) candidate = base + "_" + (n++);
        return candidate;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // ==================================================================
    // WebOR (YAML) model helpers - the canonical Object Repository format
    // ==================================================================

    /**
     * Writes the durable locators discovered during a browser session into the
     * project's WebOR as YAML (ObjectRepository/Web/&lt;page&gt;.yaml), reusing
     * existing objects with the same aria signature, and rewrites each step's
     * {@code object} to {@code page.objectName}. Returns the number of new
     * objects created.
     */
    private int materializeDiscoveredObjects(
        Project p,
        String page,
        List<PlaywrightCliTranslator.Step> steps
    ) {
        com.ing.datalib.or.ObjectRepository orRepo = p.getObjectRepository();
        com.ing.datalib.or.web.WebOR web = orRepo == null ? null : orRepo.getWebOR();
        if (web == null) return 0; // OR model unavailable - leave refs untouched
        com.ing.datalib.or.web.WebORPage orPage = web.getPageByName(page);
        if (orPage == null) orPage = web.addPage(page);
        if (orPage == null) return 0;

        Set<String> usedNames = new LinkedHashSet<>();
        Map<String, String> sigToName = new LinkedHashMap<>();
        for (com.ing.datalib.or.common.ObjectGroup<com.ing.datalib.or.web.WebORObject> g : orPage.getObjectGroups()) {
            for (com.ing.datalib.or.web.WebORObject o : g.getObjects()) {
                usedNames.add(o.getName());
                String sig = ariaSignatureOf(o);
                if (sig != null && !sig.isEmpty()) sigToName.put(sig, o.getName());
            }
        }

        int created = 0;
        for (PlaywrightCliTranslator.Step st : steps) {
            if (st.locator == null || st.locator.isEmpty()) continue;
            String sig = st.locator; // role=<role>[name="<name>"]
            String objName = sigToName.get(sig);
            if (objName == null) {
                objName = uniqueObjectName(deriveNameFromAria(sig), usedNames);
                usedNames.add(objName);
                sigToName.put(sig, objName);
                com.ing.datalib.or.web.WebORObject o = orPage.addObject(objName);
                if (o != null) {
                    String role = roleFromAria(sig);
                    String label = deriveLabelFromAria(sig);
                    if (role != null && !role.isEmpty()) setWebAttr(o, "Role", role);
                    if (label != null && !label.isEmpty()) {
                        setWebAttr(o, isFieldRole(role) ? "Label" : "Text", label);
                    }
                    created++;
                }
            }
            st.object = page + "." + objName;
        }
        if (created > 0) orRepo.saveWebPageNow(orPage);
        return created;
    }

    /** Set a WebOR locator attribute (Role, Text, Label, css, xpath, ...) by name. */
    private static void setWebAttr(
        com.ing.datalib.or.web.WebORObject o,
        String prop,
        String value
    ) {
        if (o.getAttributes() == null) return;
        for (com.ing.datalib.or.common.ORAttribute a : o.getAttributes()) {
            if (a.getName() != null && a.getName().equalsIgnoreCase(prop)) {
                a.setValue(value);
                return;
            }
        }
    }

    /** Read a WebOR locator attribute value (empty string if unset). */
    private static String getWebAttr(com.ing.datalib.or.web.WebORObject o, String prop) {
        if (o.getAttributes() == null) return "";
        for (com.ing.datalib.or.common.ORAttribute a : o.getAttributes()) {
            if (a.getName() != null && a.getName().equalsIgnoreCase(prop)) {
                return a.getValue() == null ? "" : a.getValue();
            }
        }
        return "";
    }

    /** Reconstruct the aria signature (role=..[name=".."]) of a stored WebOR object. */
    private static String ariaSignatureOf(com.ing.datalib.or.web.WebORObject o) {
        String role = getWebAttr(o, "Role");
        if (role == null || role.isEmpty()) return null;
        String label = getWebAttr(o, "Label");
        if (label.isEmpty()) label = getWebAttr(o, "Text");
        return ariaLocatorValue(role, label);
    }

    private static String roleFromAria(String aria) {
        java.util.regex.Matcher m = java
            .util.regex.Pattern.compile("role=([a-zA-Z]+)")
            .matcher(aria == null ? "" : aria);
        return m.find() ? m.group(1) : "";
    }

    private static String deriveLabelFromAria(String aria) {
        java.util.regex.Matcher m = java
            .util.regex.Pattern.compile("name=\"([^\"]*)\"")
            .matcher(aria == null ? "" : aria);
        return m.find() ? m.group(1) : "";
    }

    private static final Set<String> FIELD_ROLES = new LinkedHashSet<>(
        Arrays.asList(
            "textbox",
            "combobox",
            "searchbox",
            "checkbox",
            "radio",
            "spinbutton",
            "slider",
            "listbox"
        )
    );

    /** True for form-field roles addressed by label rather than visible text. */
    private static boolean isFieldRole(String role) {
        return role != null && FIELD_ROLES.contains(role.toLowerCase(Locale.ROOT));
    }

    /** The project-scope Web Object Repository (YAML/XML model), or null. */
    private static com.ing.datalib.or.web.WebOR projectWebOR(Project p) {
        com.ing.datalib.or.ObjectRepository or = p == null ? null : p.getObjectRepository();
        return or == null ? null : or.getWebOR();
    }

    /** Map an internal OR attribute name to the YAML element key used on disk. */
    private static String yamlLocatorKey(String prop) {
        switch (prop) {
            case "Role":
                return "role";
            case "Text":
                return "text";
            case "Label":
                return "label";
            case "Placeholder":
                return "placeholder";
            case "AltText":
                return "altText";
            case "Title":
                return "title";
            case "TestId":
                return "testId";
            case "ChainedLocator":
                return "chainedLocator";
            case "JSPath":
                return "jsPath";
            default:
                return prop; // xpath, css
        }
    }

    private JsonNode browserSessionClose(ObjectMapper json, JsonNode args) {
        String name = MCPServer.requiredParam(args, "name");
        PwSession s = pwSessions.remove(name);
        int recorded = s == null ? 0 : s.steps.size();
        try {
            runPlaywright(name, Arrays.asList("close"), 20);
        } catch (RuntimeException ignored) {
            // best-effort; the daemon may already be gone
        }
        return json
            .createObjectNode()
            .put("closed", true)
            .put("session", name)
            .put("discardedSteps", recorded);
    }

    private JsonNode browserInspect(ObjectMapper json, JsonNode args) {
        String url = MCPServer.requiredParam(args, "url");
        String describe = MCPServer.paramOrDefault(args, "describe", null);
        String browser = MCPServer.paramOrDefault(args, "browser", "chromium");
        String session = "inspect-" + UUID.randomUUID().toString().substring(0, 8);
        List<String> open = new ArrayList<>(Arrays.asList("open", url, "--browser=" + browser));
        try {
            runPlaywright(session, open, 120);
            PwResult snap = runPlaywright(session, Arrays.asList("snapshot"), 30);
            ObjectNode out = json.createObjectNode();
            out.put("url", url);
            if (describe != null) out.put("describe", describe);
            out.put("snapshot", snap.output);
            return out;
        } finally {
            try {
                runPlaywright(session, Arrays.asList("close"), 20);
            } catch (RuntimeException ignored) {}
        }
    }

    private JsonNode objectImportPage(ObjectMapper json, JsonNode args) {
        String url = MCPServer.requiredParam(args, "url");
        String page = MCPServer.requiredParam(args, "page");
        String browser = MCPServer.paramOrDefault(args, "browser", "chromium");
        String session = "scrape-" + UUID.randomUUID().toString().substring(0, 8);
        String snapshot;
        try {
            runPlaywright(session, Arrays.asList("open", url, "--browser=" + browser), 120);
            snapshot = runPlaywright(session, Arrays.asList("snapshot"), 30).output;
        } finally {
            try {
                runPlaywright(session, Arrays.asList("close"), 20);
            } catch (RuntimeException ignored) {}
        }
        Set<String> interactive = new LinkedHashSet<>(
            Arrays.asList(
                "button",
                "link",
                "textbox",
                "checkbox",
                "radio",
                "combobox",
                "listbox",
                "menuitem",
                "tab",
                "switch",
                "searchbox",
                "slider",
                "spinbutton"
            )
        );
        Project p = loadProject(resolveProject(projectArg(args)));
        com.ing.datalib.or.ObjectRepository orRepo = p.getObjectRepository();
        com.ing.datalib.or.web.WebOR web = orRepo == null ? null : orRepo.getWebOR();
        if (web == null) throw new MCPServer.MCPException(
            -32603,
            "Object Repository model unavailable for project."
        );
        com.ing.datalib.or.web.WebORPage orPage = web.getPageByName(page);
        if (orPage == null) orPage = web.addPage(page);
        Set<String> existing = new LinkedHashSet<>();
        for (com.ing.datalib.or.common.ObjectGroup<com.ing.datalib.or.web.WebORObject> g : orPage.getObjectGroups()) {
            for (com.ing.datalib.or.web.WebORObject o : g.getObjects()) existing.add(o.getName());
        }
        int created = 0;
        for (String line : snapshot.split("\\R")) {
            java.util.regex.Matcher m = SNAPSHOT_ROW.matcher(line);
            if (!m.find()) continue;
            String role = m.group(1).toLowerCase(Locale.ROOT);
            String label = m.group(2);
            if (!interactive.contains(role)) continue;
            String base = (label == null || label.isEmpty()) ? role : label;
            String objName = uniqueObjectName(sanitizeObjectName(base), existing);
            existing.add(objName);
            com.ing.datalib.or.web.WebORObject o = orPage.addObject(objName);
            if (o == null) continue;
            setWebAttr(o, "Role", role);
            if (label != null && !label.isEmpty()) {
                setWebAttr(o, isFieldRole(role) ? "Label" : "Text", label);
            }
            created++;
        }
        orRepo.saveWebPageNow(orPage);
        return json
            .createObjectNode()
            .put("page", page)
            .put("url", url)
            .put("format", "yaml")
            .put("objectsCreated", created)
            .put(
                "path",
                new File(new File(p.getLocation(), "ObjectRepository"), "Web").getAbsolutePath()
            );
    }

    private static String sanitizeObjectName(String s) {
        String t = s.trim().replaceAll("[^A-Za-z0-9]+", "_").replaceAll("^_+|_+$", "");
        if (t.isEmpty()) t = "element";
        if (t.length() > 40) t = t.substring(0, 40);
        return t.toLowerCase(Locale.ROOT);
    }

    /** Regex for a Playwright accessibility snapshot row: {@code - button "Submit" [ref=e21]}. */
    private static final java.util.regex.Pattern SNAPSHOT_ROW = java.util.regex.Pattern.compile(
        "-\\s*([a-zA-Z]+)\\s+\"([^\"]*)\"(?:.*?\\[ref=([a-zA-Z0-9]+)\\])?"
    );

    /**
     * Parse a live accessibility snapshot into a ref -&gt; [role, accessibleName]
     * map. This is the deterministic bridge that lets a recorded Playwright ref
     * (e.g. {@code e21}) be resolved to a durable Object-Repository locator.
     */
    private static Map<String, String[]> parseSnapshotRefs(String snapshot) {
        Map<String, String[]> out = new LinkedHashMap<>();
        if (snapshot == null) return out;
        for (String line : snapshot.split("\\R")) {
            java.util.regex.Matcher m = SNAPSHOT_ROW.matcher(line);
            if (!m.find()) continue;
            String ref = m.group(3);
            if (ref == null || ref.isEmpty()) continue;
            out.put(ref, new String[] { m.group(1).toLowerCase(Locale.ROOT), m.group(2) });
        }
        return out;
    }

    /**
     * Build a durable, comma-free INGenious aria locator value from a role and
     * accessible name (e.g. {@code role=button[name="Submit"]}). Commas are
     * avoided so the value survives the naive Object-Repository CSV writer.
     */
    private static String ariaLocatorValue(String role, String label) {
        String safeLabel = label == null ? "" : label.replace("\"", "'").replace(",", " ");
        return "role=" + role + (safeLabel.isEmpty() ? "" : "[name=\"" + safeLabel + "\"]");
    }

    private TestCase buildTestCaseFromSteps(
        Project p,
        String scenName,
        String tcName,
        boolean reusable,
        List<PlaywrightCliTranslator.Step> steps
    ) {
        Scenario s = ensureScenario(p, scenName, reusable);
        if (s.getTestCaseByName(tcName) != null) {
            throw new MCPServer.MCPException(-32602, "Test case already exists: " + tcName);
        }
        String originalFormat = null;
        try {
            originalFormat = p.getInfo().getTestCaseFormat();
        } catch (Exception ignored) {}
        try {
            p.getInfo().setTestCaseFormat(detectScenarioFormatPreferYaml(s));
        } catch (Exception ignored) {}
        TestCase tc = s.addTestCase(tcName);
        if (tc == null) {
            try {
                p.getInfo().setTestCaseFormat(originalFormat);
            } catch (Exception ignored) {}
            throw new MCPServer.MCPException(-32603, "Failed to create test case: " + tcName);
        }
        try {
            for (PlaywrightCliTranslator.Step st : steps) {
                TestStep step = tc.addNewStep();
                step.setAction(st.action == null ? "" : st.action);
                step.setObject(st.object == null ? "" : st.object);
                step.setInput(st.input == null ? "" : st.input);
            }
            tc.save();
        } finally {
            try {
                p.getInfo().setTestCaseFormat(originalFormat);
            } catch (Exception ignored) {}
        }
        return tc;
    }

    /** Locate the Playwright Agent CLI: prefer a global `playwright-cli`, else `npx @playwright/cli`. */
    private List<String> playwrightCliBase() {
        if (onPath("playwright-cli")) return new ArrayList<>(Arrays.asList("playwright-cli"));
        if (onPath("npx")) return new ArrayList<>(Arrays.asList("npx", "--yes", "@playwright/cli"));
        return null;
    }

    private boolean onPath(String bin) {
        String probe = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")
            ? "where"
            : "which";
        try {
            Process p = new ProcessBuilder(probe, bin).redirectErrorStream(true).start();
            boolean done = p.waitFor(8, TimeUnit.SECONDS);
            return done && p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private String commandVersion(String bin) {
        try {
            Process p = new ProcessBuilder(bin, "--version").redirectErrorStream(true).start();
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8)
            .trim();
            p.waitFor(8, TimeUnit.SECONDS);
            int nl = out.indexOf('\n');
            return nl > 0 ? out.substring(0, nl) : out;
        } catch (Exception e) {
            return "";
        }
    }

    private PwResult runPlaywright(String session, List<String> verbArgs, int timeoutSec) {
        List<String> base = playwrightCliBase();
        if (base == null) {
            throw new MCPServer.MCPException(
                -32603,
                "Playwright Agent CLI not found. Install it with 'npm i -g @playwright/cli' " +
                "(or ensure 'npx' is on PATH). See https://playwright.dev/agent-cli/introduction"
            );
        }
        List<String> cmd = new ArrayList<>(base);
        if (session != null && !session.isEmpty()) cmd.add("-s=" + session);
        cmd.addAll(verbArgs);
        ProcessBuilder pb = new ProcessBuilder(cmd).redirectErrorStream(true);
        pb.directory(new File(System.getProperty("user.dir")));
        try {
            Process p = pb.start();
            StringBuilder sb = new StringBuilder();
            Thread pump = new Thread(
                () -> {
                    try (
                        BufferedReader r = new BufferedReader(
                            new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8)
                        )
                    ) {
                        String line;
                        while ((line = r.readLine()) != null) {
                            synchronized (sb) {
                                sb.append(line).append('\n');
                                if (sb.length() > 500_000) sb.delete(0, sb.length() - 400_000);
                            }
                        }
                    } catch (IOException ignored) {}
                }
            );
            pump.setDaemon(true);
            pump.start();
            boolean done = p.waitFor(timeoutSec, TimeUnit.SECONDS);
            if (!done) {
                p.destroyForcibly();
                throw new MCPServer.MCPException(
                    -32603,
                    "Playwright CLI command timed out after " + timeoutSec + "s"
                );
            }
            pump.join(2000);
            PwResult r = new PwResult();
            r.exitCode = p.exitValue();
            synchronized (sb) {
                r.output = sb.toString();
            }
            return r;
        } catch (IOException e) {
            throw new MCPServer.MCPException(
                -32603,
                "Failed to run Playwright CLI: " + e.getMessage()
            );
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new MCPServer.MCPException(-32603, "Interrupted running Playwright CLI");
        }
    }

    /** Split a command line honouring double quotes (e.g. fill e5 "hello world"). */
    private static List<String> tokenize(String s) {
        List<String> out = new ArrayList<>();
        if (s == null) return out;
        java.util.regex.Matcher m = java
            .util.regex.Pattern.compile("\"([^\"]*)\"|(\\S+)")
            .matcher(s.trim());
        while (m.find()) {
            out.add(m.group(1) != null ? m.group(1) : m.group(2));
        }
        return out;
    }

    static class PwSession {
        String name;
        String browser;
        String startUrl;
        /** The most recent accessibility snapshot text (holds the live element refs). */
        String lastSnapshot = "";
        /** Discovery context pre-bound by ingenious_browser_discover (optional). */
        String scenario;
        String testcase;
        String page;
        String prompt;
        boolean reusable;
        final List<PlaywrightCliTranslator.Step> steps = new ArrayList<>();
    }

    static class PwResult {
        int exitCode;
        String output = "";
    }

    // ==================================================================
    // helpers
    // ==================================================================

    /** Build a not-found error enriched with "did you mean" suggestions (and structured error data). */
    private static MCPServer.MCPException notFound(
        int code,
        String message,
        java.util.Collection<String> candidates,
        String input
    ) {
        List<String> sugg = nearest(candidates, input, 3);
        if (sugg.isEmpty()) return new MCPServer.MCPException(code, message);
        String msg = message + " Did you mean: " + String.join(", ", sugg) + "?";
        ObjectNode data = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
        ArrayNode arr = data.putArray("suggestions");
        for (String s : sugg) arr.add(s);
        return new MCPServer.MCPException(code, msg, data);
    }

    private static List<String> nearest(
        java.util.Collection<String> candidates,
        String input,
        int max
    ) {
        List<String> out = new ArrayList<>();
        if (candidates == null || input == null || candidates.isEmpty()) return out;
        String in = input.toLowerCase(Locale.ROOT);
        List<String> list = new ArrayList<>(candidates);
        list.sort(Comparator.comparingInt(c -> matchScore(c.toLowerCase(Locale.ROOT), in)));
        for (String c : list) {
            if (matchScore(c.toLowerCase(Locale.ROOT), in) <= 2 + Math.max(3, in.length())) {
                out.add(c);
            }
            if (out.size() >= max) break;
        }
        return out;
    }

    private static int matchScore(String cand, String in) {
        if (cand.equals(in)) return 0;
        if (cand.contains(in) || in.contains(cand)) return 1;
        return 2 + levenshtein(cand, in);
    }

    private static int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] t = prev;
            prev = cur;
            cur = t;
        }
        return prev[b.length()];
    }

    private static List<String> scenarioNames(Project p) {
        List<String> out = new ArrayList<>();
        for (Scenario s : p.getScenarios()) out.add(s.getName());
        return out;
    }

    private static List<String> testCaseNames(Scenario s) {
        List<String> out = new ArrayList<>();
        if (s != null) for (TestCase tc : s.getTestCases()) out.add(tc.getName());
        return out;
    }

    private List<String> objectPageNames(JsonNode args) {
        List<String> out = new ArrayList<>();
        try {
            com.ing.datalib.or.web.WebOR web = projectWebOR(
                loadProject(resolveProject(projectArg(args)))
            );
            if (web != null) for (com.ing.datalib.or.web.WebORPage pg : web.getPages()) out.add(
                pg.getName()
            );
        } catch (RuntimeException ignored) {
            // best-effort suggestions only
        }
        File orDir = new File(resolveProject(projectArg(args)), "ObjectRepository");
        File[] pages = orDir.listFiles(
            f -> f.isFile() && f.getName().toLowerCase(Locale.ROOT).endsWith(".csv")
        );
        if (pages != null) for (File f : pages) {
            String pn = f.getName().replaceFirst("(?i)\\.csv$", "");
            if (!out.contains(pn)) out.add(pn);
        }
        return out;
    }

    private static List<String> sheetNames(com.ing.datalib.component.EnvTestData env) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (com.ing.datalib.component.TestData td : env.getAllEnvironments()) {
            for (com.ing.datalib.testdata.model.TestDataModel m : td.getTestDataList()) {
                names.add(m.getName());
            }
        }
        return new ArrayList<>(names);
    }

    private static List<String> archetypeNames() {
        List<String> out = new ArrayList<>();
        for (ArchetypeCatalog.Archetype a : ArchetypeCatalog.all()) out.add(a.name);
        return out;
    }

    private static void deleteRecursively(File f) {
        if (f == null) return;
        if (f.isDirectory()) {
            File[] kids = f.listFiles();
            if (kids != null) for (File k : kids) deleteRecursively(k);
        }
        f.delete();
    }

    private static int intArg(JsonNode args, String key, int def) {
        if (args == null) return def;
        JsonNode n = args.get(key);
        if (n == null || n.isNull()) return def;
        if (n.isInt()) return n.asInt();
        if (n.isTextual()) {
            try {
                return Integer.parseInt(n.asText().trim());
            } catch (NumberFormatException ignored) {
                return def;
            }
        }
        return def;
    }

    private static List<String> readLines(File f) {
        try {
            return Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    private static int countDataRows(File csv) {
        List<String> lines = readLines(csv);
        int count = 0;
        for (int i = 1; i < lines.size(); i++) {
            if (!lines.get(i).trim().isEmpty()) count++;
        }
        return count;
    }

    private String projectArg(JsonNode args) {
        String name = MCPServer.paramOrDefault(args, "project", defaultProject);
        if (name == null || name.isEmpty()) {
            throw new MCPServer.MCPException(
                -32602,
                "No project specified and the MCP server has no default. " +
                "Pass 'project' or launch the server with --project."
            );
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
        throw new MCPServer.MCPException(
            -32602,
            "Project not found: " +
            name +
            " (looked in $cwd, $cwd/Projects, and as an absolute path)"
        );
    }

    private Project loadProject(File dir) {
        try {
            return new Project(dir.getAbsolutePath());
        } catch (Exception e) {
            throw new MCPServer.MCPException(
                -32603,
                "Failed to load project at " + dir + ": " + e.getMessage()
            );
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
     * All fields optional except {@code action}. Field descriptions state the
     * ConventionCatalog input grammar so models compose conformant steps.
     */
    private static ObjectNode stepItemSchema(ObjectMapper json) {
        ObjectNode item = json.createObjectNode();
        item.put("type", "object");
        ObjectNode p = item.putObject("properties");
        p
            .putObject("action")
            .put("type", "string")
            .put(
                "description",
                "Action name (see ingenious_action_list) or, for Execute steps, " +
                "'<ReusableScenario>:<ReusableName>'. Never invent action names."
            );
        p
            .putObject("object")
            .put("type", "string")
            .put(
                "description",
                "Object reference (Page.element), 'Webservice' for API steps, or 'Execute' " +
                "for reusable calls. Never @-prefixed."
            );
        p
            .putObject("input")
            .put("type", "string")
            .put(
                "description",
                "Hard-coded values are @-prefixed (@200, @https://site). Data-driven values " +
                "use Sheet:Column. API payload bodies are raw (not @-prefixed) and may embed " +
                "{Sheet:Column} tokens. Never place GlobalData #ids here."
            );
        p
            .putObject("condition")
            .put("type", "string")
            .put(
                "description",
                "Optional per-action condition. Its format depends on the action - check " +
                "ingenious_action_info (conditionKind / conditionValues / conditionExample). " +
                "Leave empty when the action takes no condition."
            );
        p
            .putObject("description")
            .put("type", "string")
            .put("description", "Optional human-readable description.");
        item.putArray("required").add("action");
        item.put("additionalProperties", true);
        return item;
    }

    /**
     * JSON Schema for one {@code ingenious_testcase_parameterize.selections[]} item:
     * a candidate id or an object narrowing/renaming the parameterization.
     */
    private static ObjectNode selectionItemSchema(ObjectMapper json) {
        ObjectNode item = json.createObjectNode();
        ArrayNode types = item.putArray("type");
        types.add("integer");
        types.add("object");
        ObjectNode p = item.putObject("properties");
        p
            .putObject("id")
            .put("type", "integer")
            .put("description", "Candidate id from the scan result.");
        p
            .putObject("column")
            .put("type", "string")
            .put("description", "Override the suggested column name (input candidates).");
        p
            .putObject("sheet")
            .put("type", "string")
            .put("description", "Override the target data sheet for this candidate.");
        ObjectNode paths = p.putObject("paths");
        paths.put("type", "array");
        paths.put(
            "description",
            "Payload candidates only: which JSON paths to parameterize. Items are a path " +
            "string or {path, column}. Omit to parameterize every field of the payload."
        );
        paths.putObject("items").put("type", "object").put("additionalProperties", true);
        item.put("additionalProperties", true);
        return item;
    }

    /** Tiny fluent helper to keep JSON Schema definitions readable. */
    static class SchemaBuilder {
        private final ObjectMapper json;
        private final ObjectNode props;
        private final ArrayNode required;

        SchemaBuilder(ObjectMapper json) {
            this.json = json;
            this.props = json.createObjectNode();
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
        String target;
        String browser;
        String tags;
        boolean headless;
        int parallel;
        boolean rerun;
    }

    static class RunHandle {
        String id;
        RunSpec spec;
        List<String> command;
        Process process;
        long startedAt;
        long endedAt;
        int exitCode;
        String status;
        StringBuilder output = new StringBuilder(8192);
    }
}

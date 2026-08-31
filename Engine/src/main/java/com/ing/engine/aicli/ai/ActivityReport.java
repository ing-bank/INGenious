package com.ing.engine.aicli.ai;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Turns a turn's raw tool calls into a small set of user-facing "activities"
 * (e.g. "Test case created", "Test executed", "Objects captured") with a status
 * and a few human-readable detail lines. Read-only lookups (list/show/search/
 * info/get, action catalog, browser navigation, doctor) are collapsed into a
 * single minor-call count so the report highlights what actually happened rather
 * than every tool invocation.
 *
 * <p>Render-agnostic: the AI CLI renders the result with ANSI badges/pills and
 * the IDE assistant renders it as coloured HTML cards, both from this model.</p>
 */
public final class ActivityReport {

    public enum Status {
        OK,
        INFO,
        WARN,
        FAIL
    }

    /** One raw tool invocation to summarize. */
    public static final class Call {
        public final String name;
        public final boolean success;
        public final String result;

        public Call(String name, boolean success, String result) {
            this.name = name;
            this.success = success;
            this.result = result;
        }
    }

    /** One user-facing activity derived from a tool call. */
    public static final class Activity {
        public final String title;
        public final Status status;
        public final List<String> details;
        public final String tool;

        Activity(String title, Status status, List<String> details, String tool) {
            this.title = title;
            this.status = status;
            this.details = details == null ? new ArrayList<>() : details;
            this.tool = tool;
        }
    }

    /** The summarized report for a whole turn. */
    public static final class Result {
        public final List<Activity> activities;
        public final int okCount;
        public final int infoCount;
        public final int warnCount;
        public final int failCount;
        public final int minorCount;
        public final int totalCalls;

        Result(
            List<Activity> activities,
            int okCount,
            int infoCount,
            int warnCount,
            int failCount,
            int minorCount,
            int totalCalls
        ) {
            this.activities = activities;
            this.okCount = okCount;
            this.infoCount = infoCount;
            this.warnCount = warnCount;
            this.failCount = failCount;
            this.minorCount = minorCount;
            this.totalCalls = totalCalls;
        }

        public boolean isEmpty() {
            return activities.isEmpty() && minorCount == 0;
        }
    }

    private ActivityReport() {}

    /** Summarizes the given tool calls into a user-facing activity report. */
    public static Result summarize(List<Call> calls) {
        List<Activity> acts = new ArrayList<>();
        int ok = 0;
        int info = 0;
        int warn = 0;
        int fail = 0;
        int minor = 0;
        int total = calls == null ? 0 : calls.size();
        if (calls != null) {
            for (Call c : calls) {
                String bare = bareName(c.name);
                JsonNode json = ToolReportUtil.parseJsonQuiet(c.result);
                Activity a = describe(bare, c.success, json, c.result);
                if (a == null) {
                    minor++;
                    continue;
                }
                acts.add(a);
                switch (a.status) {
                    case OK:
                        ok++;
                        break;
                    case INFO:
                        info++;
                        break;
                    case WARN:
                        warn++;
                        break;
                    case FAIL:
                        fail++;
                        break;
                    default:
                        break;
                }
            }
        }
        return new Result(acts, ok, info, warn, fail, minor, total);
    }

    /**
     * Maps one tool call to an {@link Activity}, or {@code null} when it is a
     * successful read-only lookup that should be collapsed into the minor count.
     */
    private static Activity describe(String bare, boolean success, JsonNode j, String raw) {
        // Test-case authoring
        if (
            bare.equals("testcase_create") ||
            bare.equals("gen_testcase") ||
            bare.equals("apicollection_to_testcase")
        ) {
            return testCaseCreated(bare, success, j, raw);
        }
        if (bare.startsWith("gen_from_") || bare.startsWith("import_")) {
            return imported(bare, success, j, raw);
        }
        if (
            bare.endsWith("_add_step") ||
            bare.endsWith("_insert_step") ||
            bare.endsWith("_edit_step") ||
            bare.endsWith("_remove_step") ||
            bare.endsWith("_move_step")
        ) {
            return stepsUpdated(bare, success, j, raw);
        }
        if (bare.equals("testcase_parameterize")) {
            return parameterized(bare, success, j, raw);
        }
        if (bare.equals("testcase_validate")) {
            return validated(bare, success, j, raw);
        }
        if (bare.equals("testcase_delete")) {
            return removed("Test case removed", bare, success, j, raw, "testcase");
        }
        if (bare.equals("scenario_create")) {
            return scenarioCreated(bare, success, j, raw);
        }
        if (bare.equals("scenario_delete")) {
            return removed("Scenario removed", bare, success, j, raw, "scenario");
        }
        // Execution
        if (bare.equals("run") || bare.equals("run_async") || bare.equals("run_dry")) {
            return execution(bare, success, j, raw);
        }
        // Reports
        if (
            bare.equals("report_latest") ||
            bare.equals("report_failures") ||
            bare.equals("report_show") ||
            bare.equals("report_compare") ||
            bare.equals("report_history") ||
            bare.equals("report_export")
        ) {
            return report(bare, success, j, raw);
        }
        // Object repository
        if (bare.equals("object_add")) {
            return objects("Object added", bare, success, j, raw);
        }
        if (bare.equals("object_update")) {
            return objects("Object updated", bare, success, j, raw);
        }
        if (bare.equals("object_import_page") || bare.equals("browser_session_save")) {
            return objects("Objects captured", bare, success, j, raw);
        }
        if (bare.equals("object_delete")) {
            return removed("Object removed", bare, success, j, raw, "name");
        }
        // Test data
        if (
            bare.equals("data_sheet_create") ||
            bare.equals("data_row_add") ||
            bare.equals("data_row_delete") ||
            bare.equals("data_column_add") ||
            bare.equals("data_set") ||
            bare.equals("data_import") ||
            bare.equals("data_generate")
        ) {
            return dataUpdated(bare, success, j, raw);
        }
        // Test sets
        if (bare.equals("testset_create")) {
            return testSet("Test set created", bare, success, j, raw);
        }
        if (bare.equals("testset_add")) {
            return testSet("Test set updated", bare, success, j, raw);
        }
        // Project / configuration
        if (bare.equals("project_create")) {
            return projectCreated(bare, success, j, raw);
        }
        if (
            bare.equals("env_create") ||
            bare.equals("env_delete") ||
            bare.equals("config_set") ||
            bare.equals("apicollection_env_set")
        ) {
            return configUpdated(bare, success, j, raw);
        }
        // API collections
        if (bare.equals("apicollection_import")) {
            return apiImported(bare, success, j, raw);
        }
        if (bare.equals("apicollection_run") || bare.equals("apicollection_request_run")) {
            return apiRun(bare, success, j, raw);
        }
        // Performance
        if (bare.startsWith("perf_") && !isReadOnly(bare)) {
            return simpleMutation(friendly(bare), bare, success, j, raw);
        }

        // Fallbacks
        if (!success) {
            return new Activity(friendly(bare) + " failed", Status.FAIL, failDetails(j, raw), bare);
        }
        if (isReadOnly(bare)) {
            return null;
        }
        return new Activity(friendly(bare), Status.OK, genericDetails(j), bare);
    }

    // ── category builders ─────────────────────────────────────────────────

    private static Activity testCaseCreated(String bare, boolean ok, JsonNode j, String raw) {
        if (!ok) {
            return new Activity(
                "Test case creation failed",
                Status.FAIL,
                failDetails(j, raw),
                bare
            );
        }
        List<String> d = new ArrayList<>();
        addField(d, "Scenario", j, "scenario");
        addField(d, "Test case", j, "testcase");
        Integer steps = intOrNull(j, "steps");
        if (steps != null) {
            d.add(steps + (steps == 1 ? " step" : " steps"));
        }
        if (j != null && j.has("created") && !j.path("created").asBoolean(true)) {
            return new Activity("Test case already exists", Status.INFO, d, bare);
        }
        JsonNode unresolved = j == null ? null : j.path("unresolvedParams");
        if (unresolved != null && unresolved.isArray() && unresolved.size() > 0) {
            d.add(unresolved.size() + " unresolved parameter(s)");
            return new Activity("Test case created", Status.WARN, d, bare);
        }
        return new Activity("Test case created", Status.OK, d, bare);
    }

    private static Activity imported(String bare, boolean ok, JsonNode j, String raw) {
        String title = bare.startsWith("gen_from_") ? "Tests generated" : "Tests imported";
        if (!ok) {
            return new Activity(title + " failed", Status.FAIL, failDetails(j, raw), bare);
        }
        List<String> d = new ArrayList<>();
        Integer count = firstInt(j, "created", "imported", "count", "testcases", "total");
        if (count != null) {
            d.add(count + (count == 1 ? " test case" : " test cases"));
        }
        addField(d, "Scenario", j, "scenario");
        return new Activity(title, Status.OK, d, bare);
    }

    private static Activity stepsUpdated(String bare, boolean ok, JsonNode j, String raw) {
        if (!ok) {
            return new Activity("Test step update failed", Status.FAIL, failDetails(j, raw), bare);
        }
        List<String> d = new ArrayList<>();
        addField(d, "Test case", j, "testcase");
        Integer steps = intOrNull(j, "steps");
        if (steps != null) {
            d.add(steps + (steps == 1 ? " step" : " steps"));
        }
        return new Activity("Test steps updated", Status.OK, d, bare);
    }

    private static Activity parameterized(String bare, boolean ok, JsonNode j, String raw) {
        if (!ok) {
            return new Activity(
                "Data externalization failed",
                Status.FAIL,
                failDetails(j, raw),
                bare
            );
        }
        List<String> d = new ArrayList<>();
        addField(d, "Test case", j, "testcase");
        addField(d, "Sheet", j, "sheet");
        Integer n = firstInt(j, "externalized", "parameterized", "count");
        if (n != null) {
            d.add(n + " value(s) externalized");
        }
        return new Activity("Test data externalized", Status.OK, d, bare);
    }

    private static Activity validated(String bare, boolean ok, JsonNode j, String raw) {
        if (!ok) {
            return new Activity("Validation failed", Status.FAIL, failDetails(j, raw), bare);
        }
        boolean valid = j == null || j.path("valid").asBoolean(true);
        int errs = countArray(j, "errors");
        int warns = countArray(j, "warnings");
        List<String> d = new ArrayList<>();
        addField(d, "Test case", j, "testcase");
        d.add(valid && errs == 0 ? "Valid" : "Invalid");
        if (errs > 0) {
            d.add(errs + (errs == 1 ? " error" : " errors"));
        }
        if (warns > 0) {
            d.add(warns + (warns == 1 ? " warning" : " warnings"));
        }
        Status st;
        if (!valid || errs > 0) {
            st = Status.WARN;
        } else if (warns > 0) {
            st = Status.WARN;
        } else {
            st = Status.OK;
        }
        return new Activity("Test case validated", st, d, bare);
    }

    private static Activity scenarioCreated(String bare, boolean ok, JsonNode j, String raw) {
        if (!ok) {
            return new Activity("Scenario creation failed", Status.FAIL, failDetails(j, raw), bare);
        }
        List<String> d = new ArrayList<>();
        addField(d, "Scenario", j, "scenario");
        if (j != null && j.has("created") && !j.path("created").asBoolean(true)) {
            return new Activity("Scenario already exists", Status.INFO, d, bare);
        }
        return new Activity("Scenario created", Status.OK, d, bare);
    }

    private static Activity execution(String bare, boolean ok, JsonNode j, String raw) {
        if (!ok) {
            return new Activity("Test execution failed", Status.FAIL, failDetails(j, raw), bare);
        }
        String statusStr = str(j, "status");
        List<String> d = new ArrayList<>();
        addField(d, "Target", j, "target");
        if (statusStr != null) {
            d.add("Result: " + statusStr);
        }
        Integer passed = firstInt(j, "passed", "pass");
        Integer failed = firstInt(j, "failed", "fail");
        Integer totalTests = intOrNull(j, "total");
        if (totalTests != null) {
            d.add(totalTests + " test(s)");
        }
        if (passed != null) {
            d.add(passed + " passed");
        }
        if (failed != null) {
            d.add(failed + " failed");
        }
        Long dur = longOrNull(j, "durationMs");
        if (dur != null) {
            d.add("Duration " + TurnStatusFormatter.formatDuration(dur));
        }
        Status st;
        boolean running = "RUNNING".equalsIgnoreCase(statusStr);
        if ("FAIL".equalsIgnoreCase(statusStr) || (failed != null && failed > 0)) {
            st = "FAIL".equalsIgnoreCase(statusStr) ? Status.FAIL : Status.WARN;
        } else if (running) {
            st = Status.INFO;
        } else {
            st = Status.OK;
        }
        String title = running ? "Test execution started" : "Test executed";
        return new Activity(title, st, d, bare);
    }

    private static Activity report(String bare, boolean ok, JsonNode j, String raw) {
        if (!ok) {
            return new Activity("Report unavailable", Status.FAIL, failDetails(j, raw), bare);
        }
        List<String> d = new ArrayList<>();
        Integer total = intOrNull(j, "total");
        Integer pass = firstInt(j, "pass", "passed");
        Integer fail = firstInt(j, "fail", "failed");
        if (total != null) {
            d.add("Total " + total);
        }
        if (pass != null) {
            d.add(pass + " passed");
        }
        if (fail != null) {
            d.add(fail + " failed");
        }
        if (j != null && j.isArray()) {
            d.add(j.size() + " report(s)");
        }
        Status st = fail != null && fail > 0 ? Status.WARN : Status.INFO;
        return new Activity("Test results", st, d, bare);
    }

    private static Activity objects(String title, String bare, boolean ok, JsonNode j, String raw) {
        if (!ok) {
            return new Activity(title + " failed", Status.FAIL, failDetails(j, raw), bare);
        }
        List<String> d = new ArrayList<>();
        addField(d, "Page", j, "page");
        addField(d, "Object", j, "name");
        Integer created = intOrNull(j, "objectsCreated");
        if (created != null) {
            d.add(created + (created == 1 ? " object" : " objects"));
        }
        return new Activity(title, Status.OK, d, bare);
    }

    private static Activity dataUpdated(String bare, boolean ok, JsonNode j, String raw) {
        String title;
        if (bare.equals("data_sheet_create")) {
            title = "Data sheet created";
        } else if (bare.equals("data_import")) {
            title = "Test data imported";
        } else if (bare.equals("data_generate")) {
            title = "Test data generated";
        } else if (bare.equals("data_column_add")) {
            title = "Test data column added";
        } else if (bare.equals("data_row_add")) {
            title = "Test data row added";
        } else if (bare.equals("data_row_delete")) {
            title = "Test data row removed";
        } else {
            title = "Test data updated";
        }
        if (!ok) {
            return new Activity(title + " failed", Status.FAIL, failDetails(j, raw), bare);
        }
        List<String> d = new ArrayList<>();
        addField(d, "Sheet", j, "sheet");
        addField(d, "Column", j, "column");
        Integer rows = intOrNull(j, "rows");
        if (rows != null) {
            d.add(rows + (rows == 1 ? " row" : " rows"));
        }
        return new Activity(title, Status.OK, d, bare);
    }

    private static Activity testSet(String title, String bare, boolean ok, JsonNode j, String raw) {
        if (!ok) {
            return new Activity(title + " failed", Status.FAIL, failDetails(j, raw), bare);
        }
        List<String> d = new ArrayList<>();
        addField(d, "Test set", j, "name");
        Integer rows = intOrNull(j, "rows");
        if (rows != null) {
            d.add(rows + (rows == 1 ? " entry" : " entries"));
        }
        return new Activity(title, Status.OK, d, bare);
    }

    private static Activity projectCreated(String bare, boolean ok, JsonNode j, String raw) {
        if (!ok) {
            return new Activity("Project creation failed", Status.FAIL, failDetails(j, raw), bare);
        }
        List<String> d = new ArrayList<>();
        addField(d, "Project", j, "name");
        addField(d, "Location", j, "location");
        return new Activity("Project created", Status.OK, d, bare);
    }

    private static Activity configUpdated(String bare, boolean ok, JsonNode j, String raw) {
        String title = bare.startsWith("env_") ? "Environment updated" : "Configuration updated";
        if (!ok) {
            return new Activity(title + " failed", Status.FAIL, failDetails(j, raw), bare);
        }
        List<String> d = new ArrayList<>();
        addField(d, "Key", j, "key");
        addField(d, "Environment", j, "env");
        addField(d, "Sheet", j, "sheet");
        return new Activity(title, Status.OK, d, bare);
    }

    private static Activity apiImported(String bare, boolean ok, JsonNode j, String raw) {
        if (!ok) {
            return new Activity(
                "API collection import failed",
                Status.FAIL,
                failDetails(j, raw),
                bare
            );
        }
        List<String> d = new ArrayList<>();
        addField(d, "Collection", j, "name");
        Integer reqs = firstInt(j, "requests", "count", "total");
        if (reqs != null) {
            d.add(reqs + (reqs == 1 ? " request" : " requests"));
        }
        return new Activity("API collection imported", Status.OK, d, bare);
    }

    private static Activity apiRun(String bare, boolean ok, JsonNode j, String raw) {
        if (!ok) {
            return new Activity("API run failed", Status.FAIL, failDetails(j, raw), bare);
        }
        List<String> d = new ArrayList<>();
        addField(d, "Collection", j, "name");
        Integer passed = firstInt(j, "passed", "pass");
        Integer failed = firstInt(j, "failed", "fail");
        Integer statusCode = intOrNull(j, "status");
        if (statusCode != null) {
            d.add("Status " + statusCode);
        }
        if (passed != null) {
            d.add(passed + " passed");
        }
        if (failed != null) {
            d.add(failed + " failed");
        }
        Status st = failed != null && failed > 0 ? Status.WARN : Status.OK;
        return new Activity("API collection run", st, d, bare);
    }

    private static Activity simpleMutation(
        String title,
        String bare,
        boolean ok,
        JsonNode j,
        String raw
    ) {
        if (!ok) {
            return new Activity(title + " failed", Status.FAIL, failDetails(j, raw), bare);
        }
        return new Activity(title, Status.OK, genericDetails(j), bare);
    }

    private static Activity removed(
        String title,
        String bare,
        boolean ok,
        JsonNode j,
        String raw,
        String nameKey
    ) {
        if (!ok) {
            return new Activity(
                title.replace("removed", "removal failed"),
                Status.FAIL,
                failDetails(j, raw),
                bare
            );
        }
        List<String> d = new ArrayList<>();
        addField(d, "Name", j, nameKey);
        return new Activity(title, Status.OK, d, bare);
    }

    // ── helpers ───────────────────────────────────────────────────────────

    /** Read-only lookups that carry no user-facing "activity" when they succeed. */
    private static boolean isReadOnly(String bare) {
        return (
            bare.startsWith("action_") ||
            bare.startsWith("browser_") ||
            bare.endsWith("_list") ||
            bare.endsWith("_show") ||
            bare.endsWith("_search") ||
            bare.endsWith("_info") ||
            bare.endsWith("_get") ||
            bare.endsWith("_categories") ||
            bare.endsWith("_status") ||
            bare.endsWith("_logs") ||
            bare.equals("doctor") ||
            bare.equals("config_drivers")
        );
    }

    /** Normalizes a possibly-prefixed MCP tool name to its bare action, e.g. {@code testcase_create}. */
    static String bareName(String name) {
        if (name == null) {
            return "";
        }
        String n = name.trim().toLowerCase(Locale.ROOT);
        int idx = n.lastIndexOf("ingenious_");
        if (idx >= 0) {
            n = n.substring(idx + "ingenious_".length());
        }
        return n;
    }

    private static String friendly(String bare) {
        if (bare == null || bare.isEmpty()) {
            return "Action";
        }
        String s = bare.replace('_', ' ').trim();
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static void addField(List<String> d, String label, JsonNode j, String key) {
        String v = str(j, key);
        if (v != null) {
            d.add(label + ": " + v);
        }
    }

    private static String str(JsonNode j, String key) {
        if (j == null || !j.has(key) || j.get(key).isNull()) {
            return null;
        }
        String v = j.get(key).asText("").trim();
        return v.isEmpty() ? null : v;
    }

    private static Integer intOrNull(JsonNode j, String key) {
        if (j == null || !j.has(key) || !j.get(key).isNumber()) {
            return null;
        }
        return j.get(key).asInt();
    }

    private static Long longOrNull(JsonNode j, String key) {
        if (j == null || !j.has(key) || !j.get(key).isNumber()) {
            return null;
        }
        return j.get(key).asLong();
    }

    private static Integer firstInt(JsonNode j, String... keys) {
        for (String k : keys) {
            Integer v = intOrNull(j, k);
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    private static int countArray(JsonNode j, String key) {
        if (j == null || !j.path(key).isArray()) {
            return 0;
        }
        return j.path(key).size();
    }

    private static List<String> failDetails(JsonNode j, String raw) {
        List<String> d = new ArrayList<>();
        String msg = str(j, "error");
        if (msg == null) {
            msg = str(j, "message");
        }
        if (msg == null && raw != null && j == null) {
            msg = ToolReportUtil.truncate(raw, 120);
        }
        if (msg != null && !msg.isEmpty()) {
            d.add(msg);
        }
        return d;
    }

    private static List<String> genericDetails(JsonNode j) {
        List<String> d = new ArrayList<>();
        if (j == null || !j.isObject()) {
            return d;
        }
        String[] keys = { "name", "scenario", "testcase", "sheet", "page", "target", "path" };
        for (String k : keys) {
            String v = str(j, k);
            if (v != null) {
                d.add(cap(k) + ": " + v);
            }
            if (d.size() >= 3) {
                break;
            }
        }
        return d;
    }

    private static String cap(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}

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
 * <p>An agent often iterates on the same artifact — e.g. validate, fix a step,
 * validate again — before it converges. Those repeats are collapsed per
 * (action-kind, subject) into a single activity reflecting the <em>final</em>
 * state, with a "resolved after N attempts" note, so the OK/WARN/FAIL tallies
 * describe the actual outcome of the user's ask rather than every intermediate
 * self-correction.</p>
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
        /** Number of activities that took more than one attempt to reach their final state. */
        public final int retryCount;

        Result(
            List<Activity> activities,
            int okCount,
            int infoCount,
            int warnCount,
            int failCount,
            int minorCount,
            int totalCalls,
            int retryCount
        ) {
            this.activities = activities;
            this.okCount = okCount;
            this.infoCount = infoCount;
            this.warnCount = warnCount;
            this.failCount = failCount;
            this.minorCount = minorCount;
            this.totalCalls = totalCalls;
            this.retryCount = retryCount;
        }

        public boolean isEmpty() {
            return activities.isEmpty() && minorCount == 0;
        }
    }

    private ActivityReport() {}

    /** Summarizes the given tool calls into a user-facing activity report. */
    public static Result summarize(List<Call> calls) {
        List<Activity> raw = new ArrayList<>();
        List<String> groupKeys = new ArrayList<>();
        int minor = 0;
        int total = calls == null ? 0 : calls.size();
        if (calls != null) {
            for (Call c : calls) {
                String bare = bareName(c.name);
                boolean domain = isIngeniousTool(c.name);
                JsonNode json = ToolReportUtil.parseJsonQuiet(c.result);
                Activity a = describe(bare, c.success, json, c.result, domain);
                if (a == null) {
                    minor++;
                    continue;
                }
                raw.add(a);
                String subject = subjectOf(bare, json);
                groupKeys.add(subject == null ? null : groupCategory(bare) + '\u0000' + subject);
            }
        }
        int[] retries = new int[1];
        List<Activity> acts = collapseRetries(raw, groupKeys, retries);
        int ok = 0;
        int info = 0;
        int warn = 0;
        int fail = 0;
        for (Activity a : acts) {
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
        return new Result(acts, ok, info, warn, fail, minor, total, retries[0]);
    }

    /**
     * Collapses repeated activities that share a group key (same action-kind on
     * the same subject, e.g. repeated validation of the same test case) down to
     * one entry reflecting the last (final) attempt, annotated with how many
     * attempts it took. Activities without a resolvable subject are kept as-is.
     */
    private static List<Activity> collapseRetries(
        List<Activity> raw,
        List<String> groupKeys,
        int[] retriesOut
    ) {
        List<Activity> result = new ArrayList<>();
        java.util.Map<String, Integer> firstPos = new java.util.LinkedHashMap<>();
        java.util.Map<String, Integer> attempts = new java.util.HashMap<>();
        for (int i = 0; i < raw.size(); i++) {
            String key = groupKeys.get(i);
            Activity a = raw.get(i);
            if (key == null) {
                result.add(a);
                continue;
            }
            Integer pos = firstPos.get(key);
            if (pos == null) {
                firstPos.put(key, result.size());
                attempts.put(key, 1);
                result.add(a);
            } else {
                attempts.put(key, attempts.get(key) + 1);
                result.set(pos, a);
            }
        }
        int retried = 0;
        for (java.util.Map.Entry<String, Integer> e : firstPos.entrySet()) {
            int n = attempts.get(e.getKey());
            if (n <= 1) {
                continue;
            }
            retried++;
            int pos = e.getValue();
            Activity a = result.get(pos);
            String note = (a.status == Status.OK || a.status == Status.INFO)
                ? "Resolved after " + n + " attempts"
                : n + " attempts";
            result.set(pos, withNote(a, note));
        }
        retriesOut[0] = retried;
        return result;
    }

    private static Activity withNote(Activity a, String note) {
        List<String> d = new ArrayList<>(a.details);
        d.add(note);
        return new Activity(a.title, a.status, d, a.tool);
    }

    /** Normalizes bare tool-name variants that represent the same kind of action on a subject. */
    private static String groupCategory(String bare) {
        if (
            bare.endsWith("_add_step") ||
            bare.endsWith("_insert_step") ||
            bare.endsWith("_edit_step") ||
            bare.endsWith("_remove_step") ||
            bare.endsWith("_move_step")
        ) {
            return "steps";
        }
        if (bare.equals("run") || bare.equals("run_async") || bare.equals("run_dry")) {
            return "run";
        }
        return bare;
    }

    /**
     * Extracts a stable identifier for the artifact a call acted on (test case,
     * scenario, object, sheet/column, …), or {@code null} when none can be
     * determined — such calls are never collapsed with others.
     */
    private static String subjectOf(String bare, JsonNode j) {
        String tc = str(j, "testcase");
        if (tc != null) {
            return "testcase:" + tc;
        }
        String name = str(j, "name");
        if (name != null) {
            return "name:" + name;
        }
        String target = str(j, "target");
        if (target != null) {
            return "target:" + target;
        }
        String sheet = str(j, "sheet");
        if (sheet != null) {
            String col = str(j, "column");
            return col != null ? "sheet:" + sheet + ":" + col : "sheet:" + sheet;
        }
        String page = str(j, "page");
        if (page != null) {
            return "page:" + page;
        }
        String scenario = str(j, "scenario");
        if (scenario != null) {
            return "scenario:" + scenario;
        }
        // A whole-project validate (no scenario/testcase filter) carries no
        // identifying field at all; give it a fixed subject so repeated broad
        // validation passes still collapse into one final entry.
        if ("testcase_validate".equals(bare)) {
            return "validate:__all__";
        }
        return null;
    }

    /**
     * Maps one tool call to an {@link Activity}, or {@code null} when it is a
     * successful read-only lookup (or a non-INGenious housekeeping tool call,
     * e.g. the agent's own file/shell/skill-doc tools) that should be collapsed
     * into the minor count.
     */
    private static Activity describe(
        String bare,
        boolean success,
        JsonNode j,
        String raw,
        boolean domain
    ) {
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
        if (!domain || isReadOnly(bare)) {
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
        Integer steps = firstInt(j, "totalSteps", "steps");
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
        String tcName = str(j, "testcase");
        if (tcName != null) {
            d.add("Test case: " + tcName);
        } else {
            Integer checked = intOrNull(j, "checked");
            if (checked != null && checked > 1) {
                d.add("Checked " + checked + " test cases");
            }
        }
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
        String title = tcName != null ? "Test case validated" : "Test suite validated";
        return new Activity(title, st, d, bare);
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
        addPathField(d, "Location", j, "location");
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
            bare.equals("skill_read") ||
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

    /** Whether {@code name} is one of our own {@code ingenious_*} MCP tools, vs. an agent-internal one (file/shell/skill docs, …). */
    private static boolean isIngeniousTool(String name) {
        return name != null && name.trim().toLowerCase(Locale.ROOT).contains("ingenious_");
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

    /** Like {@link #addField}, but renders a filesystem path relative and short. */
    private static void addPathField(List<String> d, String label, JsonNode j, String key) {
        String v = str(j, key);
        if (v != null) {
            d.add(label + ": " + shortenPath(v));
        }
    }

    /** Working directory used to render absolute paths as relative (AI CLI and IDE both run from the project root). */
    private static final String CWD = System.getProperty("user.dir");
    /** Max visible length of a shortened path before its leading segments are elided. */
    private static final int MAX_PATH = 60;

    /** Strips the CWD prefix off an absolute path, then elides leading segments if still long. */
    private static String shortenPath(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        String rel = relativize(s);
        if (rel.length() <= MAX_PATH) {
            return rel;
        }
        String[] parts = rel.replace('\\', '/').split("/");
        String kept = "";
        for (int i = parts.length - 1; i >= 0; i--) {
            String candidate = kept.isEmpty() ? parts[i] : parts[i] + "/" + kept;
            if (!kept.isEmpty() && ("\u2026/" + candidate).length() > MAX_PATH) {
                break;
            }
            kept = candidate;
        }
        return "\u2026/" + kept;
    }

    /** Renders an absolute path under the working directory as a relative one. */
    private static String relativize(String s) {
        if (CWD == null || s.equals(CWD)) {
            return CWD == null ? s : ".";
        }
        String prefix = CWD + java.io.File.separator;
        return s.startsWith(prefix) ? s.substring(prefix.length()) : s;
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
                d.add(cap(k) + ": " + (k.equals("path") ? shortenPath(v) : v));
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

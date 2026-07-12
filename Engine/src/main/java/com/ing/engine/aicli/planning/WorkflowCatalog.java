package com.ing.engine.aicli.planning;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ing.engine.aicli.planning.Plan.PlanStep;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic workflows: frequently used intents mapped to predefined,
 * parameterized plans. These execute with zero LLM calls — the classifier
 * prefers this catalog over the AI planner for the hot path.
 */
public final class WorkflowCatalog {
    private static final ObjectMapper M = new ObjectMapper();

    /** A parameter a workflow needs before its plan can be instantiated. */
    public static final class Param {
        private final String name;
        private final String prompt;
        private final boolean required;
        private final String defaultValue;

        public Param(String name, String prompt, boolean required, String defaultValue) {
            this.name = name;
            this.prompt = prompt;
            this.required = required;
            this.defaultValue = defaultValue;
        }

        public String name() {
            return name;
        }

        public String prompt() {
            return prompt;
        }

        public boolean required() {
            return required;
        }

        public String defaultValue() {
            return defaultValue;
        }
    }

    /** A deterministic, parameterized plan template. */
    public interface Workflow {
        String id();

        String description();

        /** Match the user input; returns extracted params when triggered. */
        Optional<Map<String, String>> match(String input);

        List<Param> params();

        Plan build(Map<String, String> values);
    }

    /** A successful catalog match. */
    public static final class Match {
        private final Workflow workflow;
        private final Map<String, String> extracted;

        public Match(Workflow workflow, Map<String, String> extracted) {
            this.workflow = workflow;
            this.extracted = extracted;
        }

        public Workflow workflow() {
            return workflow;
        }

        public Map<String, String> extracted() {
            return extracted;
        }
    }

    private static final List<Workflow> WORKFLOWS = List.of(
        new CreateLoginTest(),
        new CreateApiTest(),
        new GenerateData(),
        new RunTests(),
        new DiscoverPageObjects(),
        new CloneTestCase(),
        new RerunFailed()
    );

    private WorkflowCatalog() {}

    public static List<Workflow> all() {
        return WORKFLOWS;
    }

    public static Optional<Match> match(String input) {
        for (Workflow w : WORKFLOWS) {
            Optional<Map<String, String>> m = w.match(input);
            if (m.isPresent()) return Optional.of(new Match(w, m.get()));
        }
        return Optional.empty();
    }

    private static ObjectNode args(Object... kv) {
        ObjectNode n = M.createObjectNode();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            Object v = kv[i + 1];
            if (v == null) continue;
            if (v instanceof Integer) n.put((String) kv[i], (Integer) v); else if (
                v instanceof Boolean
            ) n.put((String) kv[i], (Boolean) v); else if (v instanceof ObjectNode) n.set(
                (String) kv[i],
                (ObjectNode) v
            ); else n.put((String) kv[i], v.toString());
        }
        return n;
    }

    private static Map<String, String> groups(Matcher m, String... names) {
        Map<String, String> out = new LinkedHashMap<>();
        for (String name : names) {
            try {
                String v = m.group(name);
                if (v != null && !v.isBlank()) out.put(name, v.trim());
            } catch (IllegalArgumentException ignored) {
                // group not present in pattern
            }
        }
        return out;
    }

    // ------------------------------------------------------------------

    /** "create a login test" → browser-login archetype + validate. */
    private static final class CreateLoginTest implements Workflow {
        private static final Pattern P = Pattern.compile(
            "(?i)^create\\s+(a\\s+)?(browser\\s+)?login\\s+test.*$"
        );

        @Override
        public String id() {
            return "create-login-test";
        }

        @Override
        public String description() {
            return "Generate a browser login test from the browser-login archetype and validate it.";
        }

        @Override
        public Optional<Map<String, String>> match(String input) {
            Matcher m = P.matcher(input.trim());
            return m.matches() ? Optional.of(new LinkedHashMap<>()) : Optional.empty();
        }

        @Override
        public List<Param> params() {
            return List.of(
                new Param("url", "Login page URL", true, null),
                new Param("username", "Username to type", true, null),
                new Param("password", "Password to type", true, null),
                new Param("scenario", "Scenario name", false, "Login"),
                new Param("testcase", "Test case name", false, "LoginTest"),
                new Param("userField", "OR object for the username field", false, "username"),
                new Param("passField", "OR object for the password field", false, "password"),
                new Param("loginButton", "OR object for the login button", false, "loginButton"),
                new Param("dashboard", "OR object visible after login", false, "dashboard")
            );
        }

        @Override
        public Plan build(Map<String, String> v) {
            ObjectNode params = args(
                "url",
                v.get("url"),
                "username",
                v.get("username"),
                "password",
                v.get("password"),
                "userField",
                v.get("userField"),
                "passField",
                v.get("passField"),
                "loginButton",
                v.get("loginButton"),
                "dashboard",
                v.get("dashboard")
            );
            List<PlanStep> steps = new ArrayList<>();
            steps.add(
                new PlanStep(
                    "s1",
                    "gen_testcase",
                    args(
                        "archetype",
                        "browser-login",
                        "scenario",
                        v.get("scenario"),
                        "testcase",
                        v.get("testcase"),
                        "params",
                        params
                    ),
                    List.of()
                )
            );
            steps.add(
                new PlanStep(
                    "s2",
                    "testcase_validate",
                    args("scenario", v.get("scenario"), "testcase", v.get("testcase")),
                    List.of("s1")
                )
            );
            return new Plan(
                "Create login test " + v.get("scenario") + "/" + v.get("testcase"),
                steps
            );
        }
    }

    /** "create api test(s) for Customers" → api-get archetype + validate. */
    private static final class CreateApiTest implements Workflow {
        private static final Pattern P = Pattern.compile(
            "(?i)^create\\s+(an?\\s+)?api\\s+tests?(\\s+for\\s+(?<name>[\\w -]+))?\\s*$"
        );

        @Override
        public String id() {
            return "create-api-test";
        }

        @Override
        public String description() {
            return "Generate a GET API test from the api-get archetype and validate it.";
        }

        @Override
        public Optional<Map<String, String>> match(String input) {
            Matcher m = P.matcher(input.trim());
            if (!m.matches()) return Optional.empty();
            Map<String, String> out = groups(m, "name");
            if (out.containsKey("name")) {
                String name = out.remove("name").replaceAll("\\s+", "");
                out.put("testcase", name + "ApiTest");
            }
            return Optional.of(out);
        }

        @Override
        public List<Param> params() {
            return List.of(
                new Param("url", "Endpoint URL", true, null),
                new Param("status", "Expected HTTP status", false, "200"),
                new Param("scenario", "Scenario name", false, "API"),
                new Param("testcase", "Test case name", false, "ApiTest")
            );
        }

        @Override
        public Plan build(Map<String, String> v) {
            List<PlanStep> steps = new ArrayList<>();
            steps.add(
                new PlanStep(
                    "s1",
                    "gen_testcase",
                    args(
                        "archetype",
                        "api-get",
                        "scenario",
                        v.get("scenario"),
                        "testcase",
                        v.get("testcase"),
                        "params",
                        args("url", v.get("url"), "status", v.get("status"))
                    ),
                    List.of()
                )
            );
            steps.add(
                new PlanStep(
                    "s2",
                    "testcase_validate",
                    args("scenario", v.get("scenario"), "testcase", v.get("testcase")),
                    List.of("s1")
                )
            );
            return new Plan(
                "Create API test " + v.get("scenario") + "/" + v.get("testcase"),
                steps
            );
        }
    }

    /** "generate data" / "generate 20 rows of data for login". */
    private static final class GenerateData implements Workflow {
        private static final Pattern P = Pattern.compile(
            "(?i)^generate\\s+((?<rows>\\d+)\\s+(rows\\s+(of\\s+)?)?)?(test\\s+|synthetic\\s+)?data(\\s+for\\s+(?<sheet>[\\w-]+))?\\s*$"
        );

        @Override
        public String id() {
            return "generate-data";
        }

        @Override
        public String description() {
            return "Generate synthetic rows into a data sheet.";
        }

        @Override
        public Optional<Map<String, String>> match(String input) {
            Matcher m = P.matcher(input.trim());
            return m.matches() ? Optional.of(groups(m, "rows", "sheet")) : Optional.empty();
        }

        @Override
        public List<Param> params() {
            return List.of(
                new Param("sheet", "Data sheet name", true, null),
                new Param("rows", "Number of rows", false, "5")
            );
        }

        @Override
        public Plan build(Map<String, String> v) {
            int rows;
            try {
                rows = Integer.parseInt(v.getOrDefault("rows", "5"));
            } catch (NumberFormatException e) {
                rows = 5;
            }
            return new Plan(
                "Generate " + rows + " data rows into sheet " + v.get("sheet"),
                List.of(
                    new PlanStep(
                        "s1",
                        "data_generate",
                        args("sheet", v.get("sheet"), "rows", rows),
                        List.of()
                    )
                )
            );
        }
    }

    /** "run all smoke tests" / "run regression tests". */
    private static final class RunTests implements Workflow {
        private static final Pattern P = Pattern.compile(
            "(?i)^run\\s+(all\\s+)?((?<tags>[\\w-]+)\\s+)?tests\\s*$"
        );

        @Override
        public String id() {
            return "run-tests";
        }

        @Override
        public String description() {
            return "Execute a test set (optionally filtered by tag) and report results.";
        }

        @Override
        public Optional<Map<String, String>> match(String input) {
            Matcher m = P.matcher(input.trim());
            if (!m.matches()) return Optional.empty();
            Map<String, String> out = groups(m, "tags");
            if ("all".equalsIgnoreCase(out.get("tags"))) out.remove("tags");
            return Optional.of(out);
        }

        @Override
        public List<Param> params() {
            return List.of(
                new Param(
                    "target",
                    "Run target (<Project>/<Release>/<TestSet> or <Project>/<Scenario>/<TestCase>)",
                    true,
                    null
                ),
                new Param("tags", "Tag filter (comma-separated)", false, null),
                new Param("headless", "Run headless? (true/false)", false, "true")
            );
        }

        @Override
        public Plan build(Map<String, String> v) {
            ObjectNode a = args("target", v.get("target"));
            if (v.get("tags") != null) a.put("tags", v.get("tags"));
            a.put("headless", Boolean.parseBoolean(v.getOrDefault("headless", "true")));
            return new Plan(
                "Run tests: " + v.get("target"),
                List.of(new PlanStep("s1", "run", a, List.of()))
            );
        }
    }

    /**
     * "generate page objects" / "import page objects from <url>" — §5.1
     * discovery step: scrape a live page (playwright-cli) into the Web OR.
     */
    private static final class DiscoverPageObjects implements Workflow {
        private static final Pattern P = Pattern.compile(
            "(?i)^(generate|discover|import)\\s+page\\s+objects?(\\s+(from|for)\\s+(?<url>\\S+))?\\s*$"
        );

        @Override
        public String id() {
            return "discover-page-objects";
        }

        @Override
        public String description() {
            return "Scrape a live page's elements into an Object Repository page (needs @playwright/cli).";
        }

        @Override
        public Optional<Map<String, String>> match(String input) {
            Matcher m = P.matcher(input.trim());
            return m.matches() ? Optional.of(groups(m, "url")) : Optional.empty();
        }

        @Override
        public List<Param> params() {
            return List.of(
                new Param("url", "Page URL to scrape", true, null),
                new Param("page", "Object Repository page name", true, null)
            );
        }

        @Override
        public Plan build(Map<String, String> v) {
            List<PlanStep> steps = new ArrayList<>();
            steps.add(
                new PlanStep(
                    "s1",
                    "object_import_page",
                    args("url", v.get("url"), "page", v.get("page")),
                    List.of()
                )
            );
            steps.add(
                new PlanStep("s2", "object_show", args("page", v.get("page")), List.of("s1"))
            );
            return new Plan("Discover page objects from " + v.get("url"), steps);
        }
    }

    /**
     * "clone testcase API/PingApiTest as PingCopy" — pipes the steps array
     * from testcase_show straight into testcase_create.
     */
    private static final class CloneTestCase implements Workflow {
        private static final Pattern P = Pattern.compile(
            "(?i)^clone\\s+(test\\s*case\\s+)?(?<scenario>[\\w-]+)/(?<testcase>[\\w-]+)\\s+(as|to)\\s+(?<newname>[\\w-]+)\\s*$"
        );

        @Override
        public String id() {
            return "clone-testcase";
        }

        @Override
        public String description() {
            return "Copy an existing test case's steps into a new test case in the same scenario.";
        }

        @Override
        public Optional<Map<String, String>> match(String input) {
            Matcher m = P.matcher(input.trim());
            return m.matches()
                ? Optional.of(groups(m, "scenario", "testcase", "newname"))
                : Optional.empty();
        }

        @Override
        public List<Param> params() {
            return List.of(
                new Param("scenario", "Scenario name", true, null),
                new Param("testcase", "Test case to clone", true, null),
                new Param("newname", "New test case name", true, null)
            );
        }

        @Override
        public Plan build(Map<String, String> v) {
            List<PlanStep> steps = new ArrayList<>();
            steps.add(
                new PlanStep(
                    "s1",
                    "testcase_show",
                    args("scenario", v.get("scenario"), "testcase", v.get("testcase")),
                    List.of()
                )
            );
            steps.add(
                new PlanStep(
                    "s2",
                    "testcase_create",
                    args(
                        "scenario",
                        v.get("scenario"),
                        "testcase",
                        v.get("newname"),
                        "steps",
                        "${s1.out.steps}"
                    ),
                    List.of("s1")
                )
            );
            steps.add(
                new PlanStep(
                    "s3",
                    "testcase_validate",
                    args("scenario", v.get("scenario"), "testcase", v.get("newname")),
                    List.of("s2")
                )
            );
            return new Plan(
                "Clone " + v.get("scenario") + "/" + v.get("testcase") + " as " + v.get("newname"),
                steps
            );
        }
    }

    /** "fix failing tests" / "rerun failed tests" — report failures, then re-execute only them. */
    private static final class RerunFailed implements Workflow {
        private static final Pattern P = Pattern.compile(
            "(?i)^(fix|re-?run)\\s+(the\\s+)?fail(ing|ed)\\s+tests?\\s*$"
        );

        @Override
        public String id() {
            return "rerun-failed";
        }

        @Override
        public String description() {
            return "List the last run's failures for a target, then re-execute only the failed test cases.";
        }

        @Override
        public Optional<Map<String, String>> match(String input) {
            Matcher m = P.matcher(input.trim());
            return m.matches() ? Optional.of(new LinkedHashMap<>()) : Optional.empty();
        }

        @Override
        public List<Param> params() {
            return List.of(
                new Param(
                    "target",
                    "Run target (<Project>/<Release>/<TestSet> or <Project>/<Scenario>/<TestCase>)",
                    true,
                    null
                )
            );
        }

        @Override
        public Plan build(Map<String, String> v) {
            String runTarget = v.get("target");
            // report_* targets omit the leading <Project>/ segment
            String reportTarget = runTarget.contains("/")
                ? runTarget.substring(runTarget.indexOf('/') + 1)
                : runTarget;
            List<PlanStep> steps = new ArrayList<>();
            steps.add(
                new PlanStep("s1", "report_failures", args("target", reportTarget), List.of())
            );
            ObjectNode runArgs = args("target", runTarget);
            runArgs.put("rerun", true);
            runArgs.put("headless", true);
            steps.add(new PlanStep("s2", "run", runArgs, List.of("s1")));
            return new Plan("Re-run failed tests of " + runTarget, steps);
        }
    }
}

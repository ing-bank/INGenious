package com.ing.engine.cli.commands;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ing.engine.cli.INGeniousCLI;
import java.io.File;
import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/**
 * Test execution commands.
 *
 * <p>The simplest form auto-detects the executable type:
 * <pre>
 *   ingenious run <Project>/<Scenario>/<TestCase>   # runs a test case
 *   ingenious run <Project>/<Release>/<TestSet>     # runs a test set
 * </pre>
 *
 * <p>The {@code testcase / testset / tags / rerun} sub-subcommands remain
 * available for explicit / advanced invocations.
 */
@Command(
    name = "run",
    mixinStandardHelpOptions = true,
    description = "Execute tests (auto-detects test case vs test set from <Project>/<X>/<Y>)",
    subcommands = {
        RunCommand.TestCaseRunCommand.class,
        RunCommand.TestSetRunCommand.class,
        RunCommand.TagsRunCommand.class,
        RunCommand.RerunCommand.class
    }
)
public class RunCommand implements Callable<Integer> {
    @ParentCommand
    private INGeniousCLI parent;

    @Parameters(
        index = "0",
        arity = "0..1",
        paramLabel = "<Project>/<X>/<Y>",
        description = {
            "Auto-detected target. Either:",
            "  <Project>/<Scenario>/<TestCase>  - a test case under TestPlan/",
            "  <Project>/<Release>/<TestSet>    - a test set under TestLab/",
            "<Project> may be a folder under the current directory,",
            "under ./Projects/, or an absolute path."
        }
    )
    private String autoPath;

    @Option(
        names = { "-b", "--browser" },
        description = "Browser to use (Chromium, Firefox, WebKit). Default: Chromium",
        defaultValue = "Chromium"
    )
    private String browser;

    @Option(names = { "--headless" }, description = "Run in headless mode")
    private boolean headless;

    @Option(
        names = { "--parallel" },
        description = "Number of parallel threads (test sets only). Default: 1",
        defaultValue = "1"
    )
    private int parallel;

    @Option(
        names = { "-t", "--tags" },
        split = ",",
        description = "Filter by tag(s). Repeat the flag or comma-separate values" +
        " (e.g. -t @smoke,@api). Test sets only."
    )
    private List<String> tags;

    @Option(
        names = { "--rerun" },
        description = "Re-execute only the test cases that failed in the last run" +
        " of the detected target. Reads from Results/.../Latest/data.js."
    )
    private boolean rerun;

    @Mixin
    OverrideOptions overrides;

    @Override
    public Integer call() {
        INGeniousCLI cli = INGeniousCLI.getInstance();

        if (autoPath == null || autoPath.isEmpty()) {
            System.out.println("See 'ingenious run --help' for usage.");
            return 0;
        }

        overrides.applyAll();

        String[] parts = autoPath.split("/");
        if (parts.length != 3) {
            cli.printError(
                "Path must be '<Project>/<Scenario>/<TestCase>' " +
                "or '<Project>/<Release>/<TestSet>'."
            );
            return 1;
        }
        String projectName = parts[0];
        String group = parts[1];
        String name = parts[2];

        File projectDir = resolveProjectDir(projectName);
        if (projectDir == null) {
            cli.printError("Project not found: " + projectName);
            cli.printInfo(
                "Looked in: ./" +
                projectName +
                ", ./Projects/" +
                projectName +
                ", and as an absolute path."
            );
            return 1;
        }

        File testCaseCsv = new File(projectDir, "TestPlan/" + group + "/" + name + ".csv");
        File testSetCsv = new File(projectDir, "TestLab/" + group + "/" + name + ".csv");
        boolean isTestCase = testCaseCsv.isFile();
        boolean isTestSet = testSetCsv.isFile();

        if (isTestCase && isTestSet) {
            cli.printError("Ambiguous: path matches both a test case and a test set.");
            cli.printInfo("  TestCase: " + testCaseCsv.getPath());
            cli.printInfo("  TestSet : " + testSetCsv.getPath());
            cli.printInfo("Use 'ingenious run testcase' or 'ingenious run testset' explicitly.");
            return 1;
        }
        if (!isTestCase && !isTestSet) {
            cli.printError("Not found as a test case or test set.");
            cli.printInfo("  Tried: " + testCaseCsv.getPath());
            cli.printInfo("  Tried: " + testSetCsv.getPath());
            return 1;
        }

        String kind = isTestCase ? "TestCase" : "TestSet";
        cli.printCallout("Detected " + kind, projectName + "/" + group + "/" + name);

        // --rerun short-circuits the normal flow: look up the previous run
        // for this exact target, pick the failed test cases, and re-execute
        // only those.
        if (rerun) {
            return rerunFailed(cli, projectDir, group, name, isTestCase);
        }

        // --parallel only applies to test sets. Warn (don't fail) if the
        // user passed it for a test case so they get useful feedback.
        if (parallel > 1 && isTestCase) {
            cli.printWarning("--parallel is ignored for test cases (applies to test sets only).");
        }
        if (tags != null && !tags.isEmpty() && isTestCase) {
            cli.printWarning("--tags is ignored for test cases (applies to test sets only).");
        }

        List<String> args = new ArrayList<>();
        args.add("-run");
        args.add("-project_location");
        args.add(projectDir.getAbsolutePath());
        if (isTestCase) {
            args.add("-scenario");
            args.add(group);
            args.add("-testcase");
            args.add(name);
        } else {
            args.add("-release");
            args.add(group);
            args.add("-testset");
            args.add(name);
            if (parallel > 1) {
                args.add("-setEnv");
                args.add("run.ThreadCount=" + parallel);
            }
            if (tags != null && !tags.isEmpty()) {
                args.add("-tags");
                args.add(String.join(",", tags));
            }
        }
        args.add("-browser");
        args.add(browser);
        if (headless) {
            args.add("-op_setHeadless");
            args.add("true");
        }

        try {
            com.ing.engine.core.Control.main(args.toArray(new String[0]));
            return 0;
        } catch (Exception e) {
            cli.printError("Execution failed: " + e.getMessage());
            return 1;
        }
    }

    /**
     * Resolve a project folder by name, trying (in order):
     *   1. as an absolute path
     *   2. as a folder under the current working directory
     *   3. as a folder under {@code ./Projects/}
     * Returns {@code null} if none of those resolve to a directory.
     */
    private static File resolveProjectDir(String name) {
        File abs = new File(name);
        if (abs.isAbsolute() && abs.isDirectory()) {
            return abs;
        }
        String cwd = System.getProperty("user.dir");
        File rel = new File(cwd, name);
        if (rel.isDirectory()) {
            return rel;
        }
        File underProjects = new File(cwd, "Projects/" + name);
        if (underProjects.isDirectory()) {
            return underProjects;
        }
        return null;
    }

    /**
     * Re-execute only the failed test cases from the previous run of the
     * given target. Reads the {@code Latest/data.js} produced by the
     * reporter, parses the JSON payload after {@code var DATA=}, and
     * collects every entry whose {@code status} is {@code FAIL}.
     *
     * <p>For a test-set target we use {@code Results/TestExecution/<release>/<set>/Latest};
     * for a test-case target we use {@code Results/TestDesign/<scenario>/<tc>/Latest}.
     *
     * <p>Each failed case is re-launched as a single test-case execution
     * (the cleanest, most predictable approach — preserves browser/headless
     * flags and avoids fighting the engine's own test-set selection logic).
     */
    private int rerunFailed(
        INGeniousCLI cli,
        File projectDir,
        String group,
        String name,
        boolean isTestCase
    ) {
        File latest = new File(
            projectDir,
            (isTestCase ? "Results/TestDesign/" : "Results/TestExecution/") +
            group +
            "/" +
            name +
            "/Latest"
        );
        File dataJs = new File(latest, "data.js");
        if (!dataJs.isFile()) {
            cli.printError("No previous run found for this target.");
            cli.printInfo("Expected: " + dataJs.getPath());
            cli.printInfo("Run it once normally before using --rerun.");
            return 1;
        }

        // Collect (scenarioName, testcaseName) for every failed entry.
        List<String[]> failed = new ArrayList<>();
        try {
            String content = Files.readString(dataJs.toPath());
            int eq = content.indexOf('=');
            int semi = content.lastIndexOf(';');
            if (eq < 0 || semi <= eq) {
                cli.printError("Could not parse " + dataJs.getName() + " (unexpected format).");
                return 1;
            }
            String json = content.substring(eq + 1, semi).trim();
            JsonNode root = new ObjectMapper().readTree(json);
            JsonNode executions = root.path("EXECUTIONS");
            if (executions.isArray()) {
                for (JsonNode tc : executions) {
                    if ("FAIL".equalsIgnoreCase(tc.path("status").asText())) {
                        failed.add(
                            new String[] {
                                tc.path("scenarioName").asText(),
                                tc.path("testcaseName").asText()
                            }
                        );
                    }
                }
            } else if ("FAIL".equalsIgnoreCase(root.path("status").asText())) {
                // Single test-case data.js — no EXECUTIONS array.
                failed.add(
                    new String[] {
                        root.path("scenarioName").asText(group),
                        root.path("testcaseName").asText(name)
                    }
                );
            }
        } catch (Exception e) {
            cli.printError("Could not read " + dataJs.getName() + ": " + e.getMessage());
            return 1;
        }

        if (failed.isEmpty()) {
            cli.printSuccess("No failed test cases in the last run — nothing to rerun.");
            return 0;
        }

        cli.printHeader("Failed test cases (" + failed.size() + ")");
        for (String[] tc : failed) {
            System.out.println(
                "  " +
                cli.style().cyan(com.ing.engine.cli.output.Style.ICON_BULLET) +
                " " +
                tc[0] +
                "/" +
                tc[1]
            );
        }

        // Re-execute each as an individual test case. Stop on the first
        // engine-level exception but keep going across PASS/FAIL outcomes.
        //
        // NB: every override prefix the user supplied on the outer 'run'
        // (the full Stage-7 matrix: --set-env / --driver / --user / --tm /
        // --capability / --db / --context / --api / --kafka-ssl /
        // --lambdatest-cap / --browser-arg / --browser-set / --device /
        // --tm-module) was already applied by RunCommand.call() into the
        // static SystemDefaults.EnvVars map BEFORE we got here. Each
        // Control.main(...) call below spins up a fresh ProjectRunner whose
        // overrideWithEnv() reads from that same static map, so the entire
        // prefix catalogue propagates to every rerun automatically — no
        // need to re-serialise the flags into the legacy args array.
        int idx = 0;
        for (String[] tc : failed) {
            idx++;
            cli.printCallout("Rerun " + idx + "/" + failed.size(), tc[0] + "/" + tc[1]);
            List<String> args = new ArrayList<>();
            args.add("-run");
            args.add("-project_location");
            args.add(projectDir.getAbsolutePath());
            args.add("-scenario");
            args.add(tc[0]);
            args.add("-testcase");
            args.add(tc[1]);
            args.add("-browser");
            args.add(browser);
            if (headless) {
                args.add("-op_setHeadless");
                args.add("true");
            }
            try {
                com.ing.engine.core.Control.main(args.toArray(new String[0]));
            } catch (Exception e) {
                cli.printError(
                    "Execution failed for " + tc[0] + "/" + tc[1] + ": " + e.getMessage()
                );
                return 1;
            }
        }
        return 0;
    }

    /**
     * Run a specific test case.
     */
    @Command(
        name = "testcase",
        mixinStandardHelpOptions = true,
        description = "Run a specific test case"
    )
    public static class TestCaseRunCommand implements Callable<Integer> {
        @ParentCommand
        private RunCommand parent;

        @Parameters(index = "0", description = "Test case path (Scenario/TestCase)")
        private String testCasePath;

        @Option(names = { "-p", "--project" }, description = "Project path")
        private String projectPath;

        @Option(
            names = { "-b", "--browser" },
            description = "Browser to use (Chromium, Firefox, WebKit)",
            defaultValue = "Chromium"
        )
        private String browser;

        @Option(names = { "-e", "--env" }, description = "Environment name")
        private String environment;

        @Option(names = { "--headless" }, description = "Run in headless mode")
        private boolean headless;

        @Option(
            names = { "--parallel" },
            description = "Number of parallel threads",
            defaultValue = "1"
        )
        private int parallel;

        @Option(
            names = { "--timeout" },
            description = "Default timeout in seconds",
            defaultValue = "30"
        )
        private int timeout;

        @Option(names = { "--dry-run" }, description = "Validate without executing")
        private boolean dryRun;

        @Mixin
        OverrideOptions overrides;

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();
            overrides.applyAll();

            String path = projectPath != null ? projectPath : cli.getProjectPath();
            if (path == null || path.isEmpty()) {
                cli.printError("Project path required. Use --project or -p flag.");
                return 1;
            }

            // Parse test case path
            String[] parts = testCasePath.split("/");
            if (parts.length != 2) {
                cli.printError("Invalid test case path. Use: Scenario/TestCase");
                return 1;
            }

            String scenarioName = parts[0];
            String testCaseName = parts[1];

            // Build execution configuration
            Map<String, Object> config = new LinkedHashMap<>();
            config.put("mode", "testcase");
            config.put("project", path);
            config.put("scenario", scenarioName);
            config.put("testcase", testCaseName);
            config.put("browser", browser);
            config.put("headless", headless);
            config.put("parallel", parallel);
            config.put("timeout", timeout);

            if (environment != null) {
                config.put("environment", environment);
            }

            if (dryRun) {
                cli.printInfo("Dry run - validating configuration:");
                System.out.println(cli.getOutputFormatter().formatKeyValue(config));
                cli.printSuccess("Configuration valid.");
                return 0;
            }

            // Execute test
            cli.printInfo("Starting test execution...");
            System.out.println("  Scenario: " + scenarioName);
            System.out.println("  TestCase: " + testCaseName);
            System.out.println("  Browser: " + browser + (headless ? " (headless)" : ""));

            try {
                // Call the actual execution engine
                return executeTest(config);
            } catch (Exception e) {
                cli.printError("Execution failed: " + e.getMessage());
                return 1;
            }
        }

        private int executeTest(Map<String, Object> config) {
            INGeniousCLI cli = INGeniousCLI.getInstance();

            // Build arguments for Control class
            List<String> args = new ArrayList<>();
            args.add("-run");
            args.add("-project_location");
            args.add(config.get("project").toString());
            args.add("-scenario");
            args.add(config.get("scenario").toString());
            args.add("-testcase");
            args.add(config.get("testcase").toString());
            args.add("-browser");
            args.add(config.get("browser").toString());

            if ((boolean) config.getOrDefault("headless", false)) {
                args.add("-op_setHeadless");
                args.add("true");
            }

            try {
                // Execute using the existing Control infrastructure
                com.ing.engine.core.Control.main(args.toArray(new String[0]));
                return 0;
            } catch (Exception e) {
                cli.printError("Execution error: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Run a test set (release/test set combination).
     */
    @Command(name = "testset", mixinStandardHelpOptions = true, description = "Run a test set")
    public static class TestSetRunCommand implements Callable<Integer> {
        @ParentCommand
        private RunCommand parent;

        @Option(names = { "-p", "--project" }, description = "Project path")
        private String projectPath;

        @Option(names = { "-r", "--release" }, description = "Release name", required = true)
        private String release;

        @Option(names = { "-t", "--testset" }, description = "Test set name", required = true)
        private String testset;

        @Option(
            names = { "-b", "--browser" },
            description = "Browser to use (Chromium, Firefox, WebKit)",
            defaultValue = "Chromium"
        )
        private String browser;

        @Option(names = { "--headless" }, description = "Run in headless mode")
        private boolean headless;

        @Option(
            names = { "--parallel" },
            description = "Number of parallel threads",
            defaultValue = "1"
        )
        private int parallel;

        @Option(names = { "--dry-run" }, description = "Validate without executing")
        private boolean dryRun;

        @Mixin
        OverrideOptions overrides;

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();
            overrides.applyAll();

            String path = projectPath != null ? projectPath : cli.getProjectPath();
            if (path == null || path.isEmpty()) {
                cli.printError("Project path required.");
                return 1;
            }

            Map<String, Object> config = new LinkedHashMap<>();
            config.put("mode", "testset");
            config.put("project", path);
            config.put("release", release);
            config.put("testset", testset);
            config.put("browser", browser);
            config.put("headless", headless);
            config.put("parallel", parallel);

            if (dryRun) {
                cli.printInfo("Dry run - configuration:");
                System.out.println(cli.getOutputFormatter().formatKeyValue(config));
                return 0;
            }

            cli.printInfo("Running test set: " + release + "/" + testset);

            try {
                List<String> args = new ArrayList<>();
                args.add("-run");
                args.add("-project_location");
                args.add(path);
                args.add("-release");
                args.add(release);
                args.add("-testset");
                args.add(testset);
                args.add("-browser");
                args.add(browser);
                if (parallel > 1) {
                    args.add("-setEnv");
                    args.add("run.ThreadCount=" + parallel);
                }

                if (headless) {
                    args.add("-op_setHeadless");
                    args.add("true");
                }

                com.ing.engine.core.Control.main(args.toArray(new String[0]));
                return 0;
            } catch (Exception e) {
                cli.printError("Execution failed: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Run tests by tags.
     */
    @Command(
        name = "tags",
        mixinStandardHelpOptions = true,
        description = "Run tests matching tags"
    )
    public static class TagsRunCommand implements Callable<Integer> {
        @ParentCommand
        private RunCommand parent;

        @Parameters(description = "Tag(s) to match", arity = "1..*")
        private List<String> tags;

        @Option(names = { "-p", "--project" }, description = "Project path")
        private String projectPath;

        @Option(
            names = { "-b", "--browser" },
            description = "Browser to use (Chromium, Firefox, WebKit)",
            defaultValue = "Chromium"
        )
        private String browser;

        @Option(names = { "--headless" }, description = "Run in headless mode")
        private boolean headless;

        @Option(names = { "--and" }, description = "Match all tags (AND logic)")
        private boolean matchAll;

        @Option(names = { "--dry-run" }, description = "Show matching tests without running")
        private boolean dryRun;

        @Mixin
        OverrideOptions overrides;

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();
            overrides.applyAll();

            String path = projectPath != null ? projectPath : cli.getProjectPath();
            if (path == null || path.isEmpty()) {
                cli.printError("Project path required.");
                return 1;
            }

            String tagExpression = matchAll
                ? String.join(" AND ", tags)
                : String.join(" OR ", tags);

            cli.printInfo("Matching tests with tags: " + tagExpression);

            if (dryRun) {
                cli.printInfo("Dry run mode - discovering matching tests...");
                // TODO: Implement tag-based test discovery
                cli.printWarning("Tag-based filtering will be executed at runtime.");
                return 0;
            }

            try {
                List<String> args = new ArrayList<>();
                args.add("-run");
                args.add("-project_location");
                args.add(path);
                args.add("-browser");
                args.add(browser);
                args.add("-tags");
                args.add(String.join(",", tags));

                if (headless) {
                    args.add("-op_setHeadless");
                    args.add("true");
                }

                com.ing.engine.core.Control.main(args.toArray(new String[0]));
                return 0;
            } catch (Exception e) {
                cli.printError("Execution failed: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Rerun failed test cases from the previous execution of a given target.
     *
     * <p>Thin sugar over {@code ingenious run <path> --rerun}: takes the
     * same {@code <Project>/<X>/<Y>} auto-detected path so users don't have
     * to remember which sub-flavour applies. The actual logic lives on the
     * parent {@link RunCommand#rerunFailed} method.
     */
    @Command(
        name = "rerun",
        mixinStandardHelpOptions = true,
        description = "Rerun only the failed test cases from the last execution"
    )
    public static class RerunCommand implements Callable<Integer> {
        @ParentCommand
        private RunCommand parent;

        @Parameters(
            index = "0",
            arity = "0..1",
            paramLabel = "<Project>/<X>/<Y>",
            description = "Same auto-detected path as 'ingenious run'."
        )
        private String autoPath;

        @Option(
            names = { "-b", "--browser" },
            description = "Browser to use (Chromium, Firefox, WebKit). Default: Chromium",
            defaultValue = "Chromium"
        )
        private String browser;

        @Option(names = { "--headless" }, description = "Run in headless mode")
        private boolean headless;

        @Mixin
        OverrideOptions overrides;

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();
            overrides.applyAll();

            if (autoPath == null || autoPath.isEmpty()) {
                cli.printError("Path required: ingenious run rerun <Project>/<X>/<Y>");
                return 1;
            }

            // Forward to the auto-detect 'run' command with --rerun set.
            // Building the args this way keeps a single source of truth for
            // auto-detection, error messages, and the data.js parsing.
            List<String> forward = new ArrayList<>();
            forward.add("run");
            forward.add("--rerun");
            forward.add("-b");
            forward.add(browser);
            if (headless) forward.add("--headless");
            forward.add(autoPath);
            return INGeniousCLI.execute(forward.toArray(new String[0]));
        }
    }
}

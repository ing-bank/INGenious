package com.ing.engine.cli.commands;

import com.ing.engine.cli.INGeniousCLI;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

import java.io.File;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * Test execution commands.
 * Provides comprehensive test running capabilities with multiple modes.
 */
@Command(
    name = "run",
    description = "Execute tests",
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

    @Override
    public Integer call() {
        System.out.println("Use 'ingenious run <subcommand>' - see 'ingenious run --help'");
        return 0;
    }

    /**
     * Run a specific test case.
     */
    @Command(name = "testcase", description = "Run a specific test case")
    public static class TestCaseRunCommand implements Callable<Integer> {

        @ParentCommand
        private RunCommand parent;

        @Parameters(index = "0", description = "Test case path (Scenario/TestCase)")
        private String testCasePath;

        @Option(names = {"-p", "--project"}, description = "Project path")
        private String projectPath;

        @Option(names = {"-b", "--browser"}, description = "Browser to use", defaultValue = "Chrome")
        private String browser;

        @Option(names = {"-e", "--env"}, description = "Environment name")
        private String environment;

        @Option(names = {"--headless"}, description = "Run in headless mode")
        private boolean headless;

        @Option(names = {"--parallel"}, description = "Number of parallel threads", defaultValue = "1")
        private int parallel;

        @Option(names = {"--timeout"}, description = "Default timeout in seconds", defaultValue = "30")
        private int timeout;

        @Option(names = {"--dry-run"}, description = "Validate without executing")
        private boolean dryRun;

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();
            
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
    @Command(name = "testset", description = "Run a test set")
    public static class TestSetRunCommand implements Callable<Integer> {

        @ParentCommand
        private RunCommand parent;

        @Option(names = {"-p", "--project"}, description = "Project path")
        private String projectPath;

        @Option(names = {"-r", "--release"}, description = "Release name", required = true)
        private String release;

        @Option(names = {"-t", "--testset"}, description = "Test set name", required = true)
        private String testset;

        @Option(names = {"-b", "--browser"}, description = "Browser to use", defaultValue = "Chrome")
        private String browser;

        @Option(names = {"--headless"}, description = "Run in headless mode")
        private boolean headless;

        @Option(names = {"--parallel"}, description = "Number of parallel threads", defaultValue = "1")
        private int parallel;

        @Option(names = {"--dry-run"}, description = "Validate without executing")
        private boolean dryRun;

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();
            
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
                args.add("-setThreads");
                args.add(String.valueOf(parallel));
                
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
    @Command(name = "tags", description = "Run tests matching tags")
    public static class TagsRunCommand implements Callable<Integer> {

        @ParentCommand
        private RunCommand parent;

        @Parameters(description = "Tag(s) to match", arity = "1..*")
        private List<String> tags;

        @Option(names = {"-p", "--project"}, description = "Project path")
        private String projectPath;

        @Option(names = {"-b", "--browser"}, description = "Browser to use", defaultValue = "Chrome")
        private String browser;

        @Option(names = {"--headless"}, description = "Run in headless mode")
        private boolean headless;

        @Option(names = {"--and"}, description = "Match all tags (AND logic)")
        private boolean matchAll;

        @Option(names = {"--dry-run"}, description = "Show matching tests without running")
        private boolean dryRun;

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();
            
            String path = projectPath != null ? projectPath : cli.getProjectPath();
            if (path == null || path.isEmpty()) {
                cli.printError("Project path required.");
                return 1;
            }

            String tagExpression = matchAll ? String.join(" AND ", tags) : String.join(" OR ", tags);
            
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
     * Rerun failed tests from previous execution.
     */
    @Command(name = "rerun", description = "Rerun failed tests from last execution")
    public static class RerunCommand implements Callable<Integer> {

        @ParentCommand
        private RunCommand parent;

        @Option(names = {"-p", "--project"}, description = "Project path")
        private String projectPath;

        @Option(names = {"-b", "--browser"}, description = "Browser to use")
        private String browser;

        @Option(names = {"--headless"}, description = "Run in headless mode")
        private boolean headless;

        @Option(names = {"--run-id"}, description = "Specific run ID to rerun from")
        private String runId;

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();
            
            String path = projectPath != null ? projectPath : cli.getProjectPath();
            if (path == null || path.isEmpty()) {
                cli.printError("Project path required.");
                return 1;
            }

            cli.printInfo("Looking for failed tests from previous run...");

            try {
                // Find the latest results directory
                File resultsDir = new File(path, "Results");
                if (!resultsDir.exists()) {
                    cli.printError("No results found. Run tests first.");
                    return 1;
                }

                File[] runs = resultsDir.listFiles(File::isDirectory);
                if (runs == null || runs.length == 0) {
                    cli.printError("No previous runs found.");
                    return 1;
                }

                // Sort by last modified to get latest
                Arrays.sort(runs, Comparator.comparingLong(File::lastModified).reversed());
                
                File latestRun = runId != null 
                    ? Arrays.stream(runs).filter(f -> f.getName().contains(runId)).findFirst().orElse(runs[0])
                    : runs[0];

                cli.printInfo("Rerunning failed tests from: " + latestRun.getName());

                List<String> args = new ArrayList<>();
                args.add("-run");
                args.add("-project_location");
                args.add(path);
                args.add("-rerun");
                args.add(latestRun.getAbsolutePath());
                
                if (browser != null) {
                    args.add("-browser");
                    args.add(browser);
                }
                
                if (headless) {
                    args.add("-op_setHeadless");
                    args.add("true");
                }
                
                com.ing.engine.core.Control.main(args.toArray(new String[0]));
                return 0;
                
            } catch (Exception e) {
                cli.printError("Rerun failed: " + e.getMessage());
                return 1;
            }
        }
    }
}

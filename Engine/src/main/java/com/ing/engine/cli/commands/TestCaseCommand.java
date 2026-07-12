package com.ing.engine.cli.commands;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.component.TestStep;
import com.ing.engine.cli.INGeniousCLI;
import java.io.File;
import java.util.*;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/**
 * Test case management commands.
 */
@Command(
    name = "testcase",
    description = "Test case management commands",
    subcommands = {
        TestCaseCommand.ListCommand.class,
        TestCaseCommand.ShowCommand.class,
        TestCaseCommand.CreateCommand.class,
        TestCaseCommand.ValidateCommand.class
    }
)
public class TestCaseCommand implements Callable<Integer> {
    @ParentCommand
    private INGeniousCLI parent;

    @Override
    public Integer call() {
        System.out.println(
            "Use 'ingenious testcase <subcommand>' - see 'ingenious testcase --help'"
        );
        return 0;
    }

    /**
     * List test cases.
     */
    @Command(name = "list", description = "List all test cases")
    public static class ListCommand implements Callable<Integer> {
        @ParentCommand
        private TestCaseCommand parent;

        @Option(names = { "-p", "--project" }, description = "Project path")
        private String projectPath;

        @Option(names = { "-s", "--scenario" }, description = "Filter by scenario name")
        private String scenarioFilter;

        @Option(names = { "--with-steps" }, description = "Include step count")
        private boolean withSteps;

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();

            String path = projectPath != null ? projectPath : cli.getProjectPath();
            if (path == null || path.isEmpty()) {
                cli.printError("Project path required. Use --project or -p flag.");
                return 1;
            }

            try {
                Project project = new Project(path);

                List<String> headers = withSteps
                    ? Arrays.asList("Scenario", "Test Case", "Steps", "Description")
                    : Arrays.asList("Scenario", "Test Case", "Description");

                List<List<String>> rows = new ArrayList<>();

                for (Scenario scenario : project.getScenarios()) {
                    if (scenarioFilter != null && !scenario.getName().contains(scenarioFilter)) {
                        continue;
                    }

                    for (TestCase tc : scenario.getTestCases()) {
                        List<String> row = new ArrayList<>();
                        row.add(scenario.getName());
                        row.add(tc.getName());

                        if (withSteps) {
                            row.add(String.valueOf(tc.getTestSteps().size()));
                        }

                        row.add(""); // No description field available
                        rows.add(row);
                    }
                }

                if (rows.isEmpty()) {
                    cli.printWarning("No test cases found.");
                    return 0;
                }

                System.out.println(cli.getOutputFormatter().formatTable(headers, rows));
                cli.printInfo("\nTotal: " + rows.size() + " test cases");
                return 0;
            } catch (Exception e) {
                cli.printError("Failed to list test cases: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Show test case details with steps.
     */
    @Command(name = "show", description = "Show test case details and steps")
    public static class ShowCommand implements Callable<Integer> {
        @ParentCommand
        private TestCaseCommand parent;

        @Parameters(index = "0", description = "Test case path (Scenario/TestCase)")
        private String testCasePath;

        @Option(names = { "-p", "--project" }, description = "Project path")
        private String projectPath;

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();

            String path = projectPath != null ? projectPath : cli.getProjectPath();
            if (path == null || path.isEmpty()) {
                cli.printError("Project path required.");
                return 1;
            }

            // Parse scenario/testcase path
            String[] parts = testCasePath.split("/");
            if (parts.length != 2) {
                cli.printError("Invalid format. Use: Scenario/TestCase");
                return 1;
            }

            String scenarioName = parts[0];
            String testCaseName = parts[1];

            try {
                Project project = new Project(path);

                Scenario scenario = project
                    .getScenarios()
                    .stream()
                    .filter(s -> s.getName().equals(scenarioName))
                    .findFirst()
                    .orElse(null);

                if (scenario == null) {
                    cli.printError("Scenario not found: " + scenarioName);
                    return 1;
                }

                TestCase testCase = scenario
                    .getTestCases()
                    .stream()
                    .filter(tc -> tc.getName().equals(testCaseName))
                    .findFirst()
                    .orElse(null);

                if (testCase == null) {
                    cli.printError("Test case not found: " + testCaseName);
                    return 1;
                }

                // Show test case info
                Map<String, Object> info = new LinkedHashMap<>();
                info.put("scenario", scenarioName);
                info.put("testCase", testCaseName);
                info.put("steps", testCase.getTestSteps().size());
                info.put("description", ""); // No description field available

                System.out.println(cli.getOutputFormatter().formatKeyValue(info));

                // Show steps table
                if (!testCase.getTestSteps().isEmpty()) {
                    System.out.println("\nSteps:");
                    List<String> headers = Arrays.asList(
                        "#",
                        "Action",
                        "Object",
                        "Input",
                        "Condition"
                    );
                    List<List<String>> rows = new ArrayList<>();

                    int stepNum = 1;
                    for (TestStep step : testCase.getTestSteps()) {
                        rows.add(
                            Arrays.asList(
                                String.valueOf(stepNum++),
                                step.getAction() != null ? step.getAction() : "",
                                step.getObject() != null ? step.getObject() : "",
                                step.getInput() != null ? step.getInput() : "",
                                step.getCondition() != null ? step.getCondition() : ""
                            )
                        );
                    }

                    System.out.println(cli.getOutputFormatter().formatTable(headers, rows));
                }

                return 0;
            } catch (Exception e) {
                cli.printError("Failed to show test case: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Create a new test case.
     */
    @Command(name = "create", description = "Create a new test case (defaults to YAML format)")
    public static class CreateCommand implements Callable<Integer> {
        @ParentCommand
        private TestCaseCommand parent;

        @Parameters(index = "0", description = "Test case path (Scenario/TestCase)")
        private String testCasePath;

        @Option(names = { "-p", "--project" }, description = "Project path")
        private String projectPath;

        @Option(names = { "--description", "-d" }, description = "Test case description")
        private String description;

        @Option(
            names = { "--reusable" },
            description = "Create under ReusableComponents/ instead of TestPlan/"
        )
        private boolean reusable;

        @Option(
            names = { "--format" },
            description = "Test case file format: ${COMPLETION-CANDIDATES} (default: YAML)",
            defaultValue = "YAML"
        )
        private FormatChoice format;

        public enum FormatChoice {
            YAML,
            CSV
        }

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();

            String path = projectPath != null ? projectPath : cli.getProjectPath();
            if (path == null || path.isEmpty()) {
                cli.printError("Project path required.");
                return 1;
            }

            String[] parts = testCasePath.split("/");
            if (parts.length != 2) {
                cli.printError("Invalid format. Use: Scenario/TestCase");
                return 1;
            }

            String scenarioName = parts[0];
            String testCaseName = parts[1];

            String previousFormat = null;
            try {
                Project project = new Project(path);

                // Temporarily switch project default format so TestCase.save()
                // honours --format for THIS test case. Restore on the way out.
                if (project.getInfo() != null) {
                    previousFormat = project.getInfo().getTestCaseFormat();
                    project.getInfo().setTestCaseFormat(format.name());
                }

                Scenario scenario = reusable
                    ? project.getReusableScenarioByName(scenarioName)
                    : project.getScenarioByName(scenarioName);
                if (scenario == null) {
                    scenario =
                        reusable
                            ? project.addReusableScenario(scenarioName)
                            : project.addScenario(scenarioName);
                    new File(scenario.getLocation()).mkdirs();
                    cli.printInfo(
                        "Created " + (reusable ? "reusable " : "") + "scenario: " + scenarioName
                    );
                }

                if (scenario.getTestCaseByName(testCaseName) != null) {
                    cli.printError("Test case already exists: " + testCasePath);
                    return 1;
                }

                TestCase tc = scenario.addTestCase(testCaseName);
                if (tc == null) {
                    cli.printError("Failed to add test case to scenario model.");
                    return 1;
                }
                if (description != null && !description.isEmpty()) {
                    TestStep s = tc.addNewStep();
                    s.setDescription(description);
                }
                tc.save();
                project.save();

                cli.printSuccess(
                    "Created test case: " +
                    testCasePath +
                    " (" +
                    format.name() +
                    ")" +
                    (reusable ? " [reusable]" : "")
                );
                return 0;
            } catch (Exception e) {
                cli.printError("Failed to create test case: " + e.getMessage());
                return 1;
            } finally {
                // Best-effort restore of original project format on disk.
                if (previousFormat != null) {
                    try {
                        Project p = new Project(path);
                        if (p.getInfo() != null) {
                            p.getInfo().setTestCaseFormat(previousFormat);
                            p.save();
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    /**
     * Validate test case.
     */
    @Command(name = "validate", description = "Validate test case")
    public static class ValidateCommand implements Callable<Integer> {
        @ParentCommand
        private TestCaseCommand parent;

        @Parameters(
            index = "0",
            description = "Test case path (Scenario/TestCase)",
            defaultValue = ""
        )
        private String testCasePath;

        @Option(names = { "-p", "--project" }, description = "Project path")
        private String projectPath;

        @Option(names = { "--all" }, description = "Validate all test cases")
        private boolean validateAll;

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();

            String path = projectPath != null ? projectPath : cli.getProjectPath();
            if (path == null || path.isEmpty()) {
                cli.printError("Project path required.");
                return 1;
            }

            try {
                Project project = new Project(path);
                List<String> errors = new ArrayList<>();
                List<String> warnings = new ArrayList<>();

                if (validateAll || testCasePath.isEmpty()) {
                    // Validate all test cases
                    for (Scenario scenario : project.getScenarios()) {
                        for (TestCase tc : scenario.getTestCases()) {
                            validateTestCase(scenario.getName(), tc, errors, warnings);
                        }
                    }
                } else {
                    // Validate specific test case
                    String[] parts = testCasePath.split("/");
                    if (parts.length != 2) {
                        cli.printError("Invalid format. Use: Scenario/TestCase");
                        return 1;
                    }

                    Scenario scenario = project
                        .getScenarios()
                        .stream()
                        .filter(s -> s.getName().equals(parts[0]))
                        .findFirst()
                        .orElse(null);

                    if (scenario == null) {
                        cli.printError("Scenario not found: " + parts[0]);
                        return 1;
                    }

                    TestCase tc = scenario
                        .getTestCases()
                        .stream()
                        .filter(t -> t.getName().equals(parts[1]))
                        .findFirst()
                        .orElse(null);

                    if (tc == null) {
                        cli.printError("Test case not found: " + parts[1]);
                        return 1;
                    }

                    validateTestCase(scenario.getName(), tc, errors, warnings);
                }

                // Output results
                if (errors.isEmpty() && warnings.isEmpty()) {
                    cli.printSuccess("Validation passed!");
                    return 0;
                }

                if (!errors.isEmpty()) {
                    System.out.println("\nErrors:");
                    errors.forEach(cli::printError);
                }

                if (!warnings.isEmpty()) {
                    System.out.println("\nWarnings:");
                    warnings.forEach(cli::printWarning);
                }

                return errors.isEmpty() ? 0 : 1;
            } catch (Exception e) {
                cli.printError("Validation failed: " + e.getMessage());
                return 1;
            }
        }

        private void validateTestCase(
            String scenarioName,
            TestCase tc,
            List<String> errors,
            List<String> warnings
        ) {
            String tcPath = scenarioName + "/" + tc.getName();

            if (tc.getTestSteps().isEmpty()) {
                warnings.add(tcPath + ": No test steps defined");
            }

            for (int i = 0; i < tc.getTestSteps().size(); i++) {
                TestStep step = tc.getTestSteps().get(i);
                int stepNum = i + 1;

                if (step.getAction() == null || step.getAction().trim().isEmpty()) {
                    errors.add(tcPath + " Step " + stepNum + ": Missing action");
                }
            }
        }
    }
}

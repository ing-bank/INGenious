package com.ing.engine.cli.commands;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.component.TestStep;
import com.ing.engine.cli.INGeniousCLI;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

import java.io.File;
import java.util.*;
import java.util.concurrent.Callable;

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
        System.out.println("Use 'ingenious testcase <subcommand>' - see 'ingenious testcase --help'");
        return 0;
    }

    /**
     * List test cases.
     */
    @Command(name = "list", description = "List all test cases")
    public static class ListCommand implements Callable<Integer> {

        @ParentCommand
        private TestCaseCommand parent;

        @Option(names = {"-p", "--project"}, description = "Project path")
        private String projectPath;

        @Option(names = {"-s", "--scenario"}, description = "Filter by scenario name")
        private String scenarioFilter;

        @Option(names = {"--with-steps"}, description = "Include step count")
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

        @Option(names = {"-p", "--project"}, description = "Project path")
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
                
                Scenario scenario = project.getScenarios().stream()
                        .filter(s -> s.getName().equals(scenarioName))
                        .findFirst()
                        .orElse(null);

                if (scenario == null) {
                    cli.printError("Scenario not found: " + scenarioName);
                    return 1;
                }

                TestCase testCase = scenario.getTestCases().stream()
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
                    List<String> headers = Arrays.asList("#", "Action", "Object", "Input", "Condition");
                    List<List<String>> rows = new ArrayList<>();

                    int stepNum = 1;
                    for (TestStep step : testCase.getTestSteps()) {
                        rows.add(Arrays.asList(
                            String.valueOf(stepNum++),
                            step.getAction() != null ? step.getAction() : "",
                            step.getObject() != null ? step.getObject() : "",
                            step.getInput() != null ? step.getInput() : "",
                            step.getCondition() != null ? step.getCondition() : ""
                        ));
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
    @Command(name = "create", description = "Create a new test case")
    public static class CreateCommand implements Callable<Integer> {

        @ParentCommand
        private TestCaseCommand parent;

        @Parameters(index = "0", description = "Test case path (Scenario/TestCase)")
        private String testCasePath;

        @Option(names = {"-p", "--project"}, description = "Project path")
        private String projectPath;

        @Option(names = {"--description", "-d"}, description = "Test case description")
        private String description;

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
                File scenarioDir = new File(path, "TestPlan/" + scenarioName);
                if (!scenarioDir.exists()) {
                    scenarioDir.mkdirs();
                    cli.printInfo("Created scenario: " + scenarioName);
                }

                File testCaseFile = new File(scenarioDir, testCaseName + ".csv");
                if (testCaseFile.exists()) {
                    cli.printError("Test case already exists: " + testCasePath);
                    return 1;
                }

                // Create test case CSV with headers
                try (java.io.PrintWriter writer = new java.io.PrintWriter(testCaseFile)) {
                    writer.println("Step,Execute,ObjectName,Reference,Action,Input,Condition,Description");
                    writer.println("1,Y,,,Open Browser,@Browser,,Open browser for testing");
                }

                cli.printSuccess("Created test case: " + testCasePath);
                return 0;
                
            } catch (Exception e) {
                cli.printError("Failed to create test case: " + e.getMessage());
                return 1;
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

        @Parameters(index = "0", description = "Test case path (Scenario/TestCase)", defaultValue = "")
        private String testCasePath;

        @Option(names = {"-p", "--project"}, description = "Project path")
        private String projectPath;

        @Option(names = {"--all"}, description = "Validate all test cases")
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

                    Scenario scenario = project.getScenarios().stream()
                            .filter(s -> s.getName().equals(parts[0]))
                            .findFirst()
                            .orElse(null);

                    if (scenario == null) {
                        cli.printError("Scenario not found: " + parts[0]);
                        return 1;
                    }

                    TestCase tc = scenario.getTestCases().stream()
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

        private void validateTestCase(String scenarioName, TestCase tc, List<String> errors, List<String> warnings) {
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

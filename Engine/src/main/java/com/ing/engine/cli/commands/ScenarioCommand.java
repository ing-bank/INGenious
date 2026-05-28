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
 * Scenario management commands.
 */
@Command(
    name = "scenario",
    description = "Scenario management commands",
    subcommands = {
        ScenarioCommand.ListCommand.class,
        ScenarioCommand.InfoCommand.class,
        ScenarioCommand.CreateCommand.class,
        ScenarioCommand.DeleteCommand.class
    }
)
public class ScenarioCommand implements Callable<Integer> {

    @ParentCommand
    private INGeniousCLI parent;

    @Override
    public Integer call() {
        System.out.println("Use 'ingenious scenario <subcommand>' - see 'ingenious scenario --help'");
        return 0;
    }

    /**
     * List scenarios in a project.
     */
    @Command(name = "list", description = "List all scenarios in a project")
    public static class ListCommand implements Callable<Integer> {

        @ParentCommand
        private ScenarioCommand parent;

        @Option(names = {"-p", "--project"}, description = "Project path")
        private String projectPath;

        @Option(names = {"--with-testcases"}, description = "Include test case details")
        private boolean withTestCases;

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
                
                List<String> headers = withTestCases 
                    ? Arrays.asList("Scenario", "Test Cases", "Total Steps", "Reusable")
                    : Arrays.asList("Scenario", "Test Cases");
                    
                List<List<String>> rows = new ArrayList<>();

                for (Scenario scenario : project.getScenarios()) {
                    List<String> row = new ArrayList<>();
                    row.add(scenario.getName());
                    row.add(String.valueOf(scenario.getTestCases().size()));
                    
                    if (withTestCases) {
                        int totalSteps = scenario.getTestCases().stream()
                                .mapToInt(tc -> tc.getTestSteps().size())
                                .sum();
                        row.add(String.valueOf(totalSteps));
                        row.add("No"); // Reusable check not available via API
                    }
                    
                    rows.add(row);
                }

                if (rows.isEmpty()) {
                    cli.printWarning("No scenarios found in project.");
                    return 0;
                }

                System.out.println(cli.getOutputFormatter().formatTable(headers, rows));
                return 0;
                
            } catch (Exception e) {
                cli.printError("Failed to list scenarios: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Show scenario information.
     */
    @Command(name = "info", description = "Show scenario details")
    public static class InfoCommand implements Callable<Integer> {

        @ParentCommand
        private ScenarioCommand parent;

        @Parameters(index = "0", description = "Scenario name")
        private String scenarioName;

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

                Map<String, Object> info = new LinkedHashMap<>();
                info.put("name", scenario.getName());
                info.put("testCases", scenario.getTestCases().size());
                info.put("reusable", false); // Reusable check not available via API
                
                int totalSteps = scenario.getTestCases().stream()
                        .mapToInt(tc -> tc.getTestSteps().size())
                        .sum();
                info.put("totalSteps", totalSteps);

                System.out.println(cli.getOutputFormatter().formatKeyValue(info));
                
                // List test cases
                if (!scenario.getTestCases().isEmpty()) {
                    System.out.println("\nTest Cases:");
                    List<String> headers = Arrays.asList("Name", "Steps", "Description");
                    List<List<String>> rows = new ArrayList<>();
                    
                    for (TestCase tc : scenario.getTestCases()) {
                        rows.add(Arrays.asList(
                            tc.getName(),
                            String.valueOf(tc.getTestSteps().size()),
                            "" // No description field available
                        ));
                    }
                    
                    System.out.println(cli.getOutputFormatter().formatTable(headers, rows));
                }
                
                return 0;
                
            } catch (Exception e) {
                cli.printError("Failed to get scenario info: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Create a new scenario.
     */
    @Command(name = "create", description = "Create a new scenario")
    public static class CreateCommand implements Callable<Integer> {

        @ParentCommand
        private ScenarioCommand parent;

        @Parameters(index = "0", description = "Scenario name")
        private String scenarioName;

        @Option(names = {"-p", "--project"}, description = "Project path")
        private String projectPath;

        @Option(names = {"--reusable"}, description = "Create as reusable scenario")
        private boolean reusable;

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
                
                // Check if scenario already exists
                boolean exists = project.getScenarios().stream()
                        .anyMatch(s -> s.getName().equals(scenarioName));
                
                if (exists) {
                    cli.printError("Scenario already exists: " + scenarioName);
                    return 1;
                }

                // Create scenario directory
                File scenarioDir = new File(path, "TestPlan/" + scenarioName);
                scenarioDir.mkdirs();

                cli.printSuccess("Created scenario: " + scenarioName);
                return 0;
                
            } catch (Exception e) {
                cli.printError("Failed to create scenario: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Delete a scenario.
     */
    @Command(name = "delete", description = "Delete a scenario")
    public static class DeleteCommand implements Callable<Integer> {

        @ParentCommand
        private ScenarioCommand parent;

        @Parameters(index = "0", description = "Scenario name")
        private String scenarioName;

        @Option(names = {"-p", "--project"}, description = "Project path")
        private String projectPath;

        @Option(names = {"--force", "-f"}, description = "Force delete without confirmation")
        private boolean force;

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();
            
            String path = projectPath != null ? projectPath : cli.getProjectPath();
            if (path == null || path.isEmpty()) {
                cli.printError("Project path required.");
                return 1;
            }

            if (!force) {
                cli.printWarning("Use --force to confirm deletion of scenario: " + scenarioName);
                return 1;
            }

            try {
                File scenarioDir = new File(path, "TestPlan/" + scenarioName);
                
                if (!scenarioDir.exists()) {
                    cli.printError("Scenario not found: " + scenarioName);
                    return 1;
                }

                deleteDirectory(scenarioDir);
                cli.printSuccess("Deleted scenario: " + scenarioName);
                return 0;
                
            } catch (Exception e) {
                cli.printError("Failed to delete scenario: " + e.getMessage());
                return 1;
            }
        }

        private void deleteDirectory(File dir) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            dir.delete();
        }
    }
}

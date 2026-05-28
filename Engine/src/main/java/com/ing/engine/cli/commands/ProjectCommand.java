package com.ing.engine.cli.commands;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.component.Release;
import com.ing.datalib.component.TestSet;
import com.ing.engine.cli.INGeniousCLI;
import com.ing.engine.cli.output.OutputFormatter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

import java.io.File;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * Project management commands.
 */
@Command(
    name = "project",
    description = "Project management commands",
    subcommands = {
        ProjectCommand.ListCommand.class,
        ProjectCommand.InfoCommand.class,
        ProjectCommand.ValidateCommand.class,
        ProjectCommand.CreateCommand.class
    }
)
public class ProjectCommand implements Callable<Integer> {

    @ParentCommand
    private INGeniousCLI parent;

    @Override
    public Integer call() {
        System.out.println("Use 'ingenious project <subcommand>' - see 'ingenious project --help'");
        return 0;
    }

    /**
     * List projects in a directory.
     */
    @Command(name = "list", description = "List all projects in a directory")
    public static class ListCommand implements Callable<Integer> {

        @ParentCommand
        private ProjectCommand parent;

        @Parameters(index = "0", description = "Directory to search for projects", defaultValue = ".")
        private File directory;

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();
            
            if (!directory.exists() || !directory.isDirectory()) {
                cli.printError("Directory not found: " + directory.getAbsolutePath());
                return 1;
            }

            List<String> headers = Arrays.asList("Project Name", "Path", "Scenarios", "Test Cases");
            List<List<String>> rows = new ArrayList<>();

            // Find projects (directories with .project file or TestPlan folder)
            File[] subdirs = directory.listFiles(File::isDirectory);
            if (subdirs != null) {
                for (File subdir : subdirs) {
                    if (isProjectDirectory(subdir)) {
                        try {
                            Project project = new Project(subdir.getAbsolutePath());
                            int scenarioCount = project.getScenarios().size();
                            int testCaseCount = project.getScenarios().stream()
                                    .mapToInt(s -> s.getTestCases().size())
                                    .sum();
                            
                            rows.add(Arrays.asList(
                                project.getName(),
                                subdir.getAbsolutePath(),
                                String.valueOf(scenarioCount),
                                String.valueOf(testCaseCount)
                            ));
                        } catch (Exception e) {
                            rows.add(Arrays.asList(subdir.getName(), subdir.getAbsolutePath(), "?", "?"));
                        }
                    }
                }
            }

            if (rows.isEmpty()) {
                cli.printWarning("No projects found in: " + directory.getAbsolutePath());
                return 0;
            }

            System.out.println(cli.getOutputFormatter().formatTable(headers, rows));
            return 0;
        }

        private boolean isProjectDirectory(File dir) {
            return new File(dir, "TestPlan").exists() || 
                   new File(dir, ".project").exists() ||
                   new File(dir, "ObjectRepository").exists();
        }
    }

    /**
     * Show project information.
     */
    @Command(name = "info", description = "Show project information")
    public static class InfoCommand implements Callable<Integer> {

        @ParentCommand
        private ProjectCommand parent;

        @Parameters(index = "0", description = "Project path", defaultValue = "")
        private String projectPath;

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();
            
            String path = projectPath.isEmpty() ? cli.getProjectPath() : projectPath;
            if (path == null || path.isEmpty()) {
                cli.printError("Project path required. Use --project or specify as argument.");
                return 1;
            }

            File projectDir = new File(path);
            if (!projectDir.exists()) {
                cli.printError("Project not found: " + path);
                return 1;
            }

            try {
                Project project = new Project(projectDir.getAbsolutePath());
                
                Map<String, Object> info = new LinkedHashMap<>();
                info.put("name", project.getName());
                info.put("location", project.getLocation());
                info.put("scenarios", project.getScenarios().size());
                
                int testCaseCount = project.getScenarios().stream()
                        .mapToInt(s -> s.getTestCases().size())
                        .sum();
                info.put("testCases", testCaseCount);
                
                info.put("releases", project.getReleases().size());
                
                int testSetCount = project.getReleases().stream()
                        .mapToInt(r -> r.getTestSets().size())
                        .sum();
                info.put("testSets", testSetCount);
                
                // Object Repository info
                if (project.getObjectRepository() != null && project.getObjectRepository().getWebOR() != null) {
                    int pageCount = project.getObjectRepository().getWebOR().getPages().size();
                    int objectCount = project.getObjectRepository().getWebOR().getPages().stream()
                            .mapToInt(p -> p.getChildCount())
                            .sum();
                    info.put("pages", pageCount);
                    info.put("objects", objectCount);
                }

                System.out.println(cli.getOutputFormatter().formatKeyValue(info));
                return 0;
                
            } catch (Exception e) {
                cli.printError("Failed to load project: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Validate project structure.
     */
    @Command(name = "validate", description = "Validate project structure and configuration")
    public static class ValidateCommand implements Callable<Integer> {

        @ParentCommand
        private ProjectCommand parent;

        @Parameters(index = "0", description = "Project path", defaultValue = "")
        private String projectPath;

        @Option(names = "--strict", description = "Enable strict validation")
        private boolean strict;

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();
            
            String path = projectPath.isEmpty() ? cli.getProjectPath() : projectPath;
            if (path == null || path.isEmpty()) {
                cli.printError("Project path required.");
                return 1;
            }

            File projectDir = new File(path);
            List<String> errors = new ArrayList<>();
            List<String> warnings = new ArrayList<>();

            // Check required directories
            checkDirectory(projectDir, "TestPlan", errors);
            checkDirectory(projectDir, "ObjectRepository", errors);
            checkDirectory(projectDir, "Settings", warnings);
            checkDirectory(projectDir, "TestData", warnings);

            try {
                Project project = new Project(projectDir.getAbsolutePath());
                
                // Validate scenarios have test cases
                for (Scenario scenario : project.getScenarios()) {
                    if (scenario.getTestCases().isEmpty()) {
                        warnings.add("Scenario '" + scenario.getName() + "' has no test cases");
                    }
                    
                    // Validate test cases have steps
                    for (TestCase tc : scenario.getTestCases()) {
                        if (tc.getTestSteps().isEmpty()) {
                            warnings.add("Test case '" + scenario.getName() + "/" + tc.getName() + "' has no steps");
                        }
                    }
                }

                // Validate releases have test sets
                for (Release release : project.getReleases()) {
                    if (release.getTestSets().isEmpty()) {
                        warnings.add("Release '" + release.getName() + "' has no test sets");
                    }
                }

            } catch (Exception e) {
                errors.add("Failed to load project: " + e.getMessage());
            }

            // Output results
            if (errors.isEmpty() && warnings.isEmpty()) {
                cli.printSuccess("Project validation passed!");
                return 0;
            }

            if (!errors.isEmpty()) {
                System.out.println("\nErrors:");
                for (String error : errors) {
                    cli.printError(error);
                }
            }

            if (!warnings.isEmpty()) {
                System.out.println("\nWarnings:");
                for (String warning : warnings) {
                    cli.printWarning(warning);
                }
            }

            return errors.isEmpty() ? 0 : 1;
        }

        private void checkDirectory(File base, String name, List<String> errors) {
            File dir = new File(base, name);
            if (!dir.exists()) {
                errors.add("Missing directory: " + name);
            }
        }
    }

    /**
     * Create a new project.
     */
    @Command(name = "create", description = "Create a new project")
    public static class CreateCommand implements Callable<Integer> {

        @ParentCommand
        private ProjectCommand parent;

        @Parameters(index = "0", description = "Project name")
        private String projectName;

        @Option(names = {"-d", "--directory"}, description = "Parent directory", defaultValue = ".")
        private File directory;

        @Option(names = {"--template"}, description = "Project template (web, mobile, api)")
        private String template;

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();
            
            File projectDir = new File(directory, projectName);
            
            if (projectDir.exists()) {
                cli.printError("Project already exists: " + projectDir.getAbsolutePath());
                return 1;
            }

            try {
                // Create project structure
                projectDir.mkdirs();
                new File(projectDir, "TestPlan").mkdirs();
                new File(projectDir, "ObjectRepository").mkdirs();
                new File(projectDir, "TestData").mkdirs();
                new File(projectDir, "Settings").mkdirs();
                new File(projectDir, "Results").mkdirs();

                // Create default scenario
                File defaultScenario = new File(projectDir, "TestPlan/NewScenario");
                defaultScenario.mkdirs();
                new File(defaultScenario, "NewTestCase.csv").createNewFile();

                // Create default release and testset
                File defaultRelease = new File(projectDir, "TestPlan/NewRelease");
                defaultRelease.mkdirs();
                new File(defaultRelease, "NewTestSet.csv").createNewFile();

                cli.printSuccess("Project created: " + projectDir.getAbsolutePath());
                
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("name", projectName);
                result.put("location", projectDir.getAbsolutePath());
                result.put("template", template != null ? template : "default");
                
                System.out.println(cli.getOutputFormatter().formatKeyValue(result));
                return 0;
                
            } catch (Exception e) {
                cli.printError("Failed to create project: " + e.getMessage());
                return 1;
            }
        }
    }
}

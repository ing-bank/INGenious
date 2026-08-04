package com.ing.engine.cli.commands;

import com.ing.engine.cli.INGeniousCLI;
import java.io.File;
import java.util.*;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/**
 * Test set management commands.
 */
@Command(
    name = "testset",
    mixinStandardHelpOptions = true,
    description = "Test set management commands",
    subcommands = {
        TestSetCommand.ListCommand.class,
        TestSetCommand.ShowCommand.class,
        TestSetCommand.CreateCommand.class,
        TestSetCommand.AddCommand.class
    }
)
public class TestSetCommand implements Callable<Integer> {
    @ParentCommand
    private INGeniousCLI parent;

    @Override
    public Integer call() {
        System.out.println("Use 'ingenious testset <subcommand>' - see 'ingenious testset --help'");
        return 0;
    }

    /**
     * List test sets.
     */
    @Command(name = "list", description = "List all test sets")
    public static class ListCommand implements Callable<Integer> {
        @ParentCommand
        private TestSetCommand parent;

        @Option(names = { "-p", "--project" }, description = "Project path")
        private String projectPath;

        @Option(names = { "-r", "--release" }, description = "Filter by release")
        private String release;

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();

            String path = projectPath != null ? projectPath : cli.getProjectPath();
            if (path == null || path.isEmpty()) {
                cli.printError("Project path required. Use --project or -p flag.");
                return 1;
            }

            try {
                File testLabDir = new File(path, "TestLab");
                if (!testLabDir.exists()) {
                    cli.printWarning("No test sets found (TestLab folder missing).");
                    return 0;
                }

                List<String> headers = Arrays.asList("Release", "Test Set", "Test Cases");
                List<List<String>> rows = new ArrayList<>();

                File[] releases = testLabDir.listFiles(File::isDirectory);
                if (releases != null) {
                    for (File releaseDir : releases) {
                        if (release != null && !releaseDir.getName().contains(release)) {
                            continue;
                        }

                        File[] testSets = releaseDir.listFiles(
                            f ->
                                f.isFile() &&
                                (
                                    f.getName().endsWith(".csv") ||
                                    f.getName().endsWith(".yaml") ||
                                    f.getName().endsWith(".yml")
                                )
                        );
                        if (testSets != null) {
                            for (File ts : testSets) {
                                String tsName = ts.getName();
                                if (tsName.endsWith(".csv")) {
                                    tsName = tsName.substring(0, tsName.length() - 4);
                                } else if (tsName.endsWith(".yaml") || tsName.endsWith(".yml")) {
                                    tsName = tsName.substring(0, tsName.lastIndexOf('.'));
                                }
                                int tcCount = countTestCases(ts);
                                rows.add(
                                    Arrays.asList(
                                        releaseDir.getName(),
                                        tsName,
                                        String.valueOf(tcCount)
                                    )
                                );
                            }
                        }
                    }
                }

                if (rows.isEmpty()) {
                    cli.printWarning("No test sets found.");
                    return 0;
                }

                System.out.println(cli.getOutputFormatter().formatTable(headers, rows));
                cli.printInfo("\nTotal: " + rows.size() + " test sets");
                return 0;
            } catch (Exception e) {
                cli.printError("Failed to list test sets: " + e.getMessage());
                return 1;
            }
        }

        private int countTestCases(File testSetFile) {
            try (Scanner scanner = new Scanner(testSetFile)) {
                int count = 0;
                boolean header = true;
                while (scanner.hasNextLine()) {
                    scanner.nextLine();
                    if (header) {
                        header = false;
                        continue;
                    }
                    count++;
                }
                return count;
            } catch (Exception e) {
                return 0;
            }
        }
    }

    /**
     * Show test set details.
     */
    @Command(name = "show", description = "Show test set contents")
    public static class ShowCommand implements Callable<Integer> {
        @ParentCommand
        private TestSetCommand parent;

        @Parameters(index = "0", description = "Test set path (Release/TestSet)")
        private String testSetPath;

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

            String[] parts = testSetPath.split("/");
            if (parts.length != 2) {
                cli.printError("Invalid format. Use: Release/TestSet");
                return 1;
            }

            try {
                // Try .csv, .yaml, .yml under TestLab/
                File csvFile = new File(path, "TestLab/" + parts[0] + "/" + parts[1] + ".csv");
                File yamlFile = new File(path, "TestLab/" + parts[0] + "/" + parts[1] + ".yaml");
                File ymlFile = new File(path, "TestLab/" + parts[0] + "/" + parts[1] + ".yml");
                File testSetFile = csvFile.isFile()
                    ? csvFile
                    : yamlFile.isFile() ? yamlFile : ymlFile.isFile() ? ymlFile : null;
                if (testSetFile == null) {
                    cli.printError("Test set not found: " + testSetPath);
                    return 1;
                }

                cli.printInfo("Test Set: " + testSetPath);
                System.out.println();

                List<String> headers = new ArrayList<>();
                List<List<String>> rows = new ArrayList<>();

                try (Scanner scanner = new Scanner(testSetFile)) {
                    boolean isHeader = true;
                    while (scanner.hasNextLine()) {
                        String line = scanner.nextLine();
                        String[] cols = line.split(",", -1);

                        if (isHeader) {
                            headers.addAll(Arrays.asList(cols));
                            isHeader = false;
                        } else {
                            rows.add(Arrays.asList(cols));
                        }
                    }
                }

                System.out.println(cli.getOutputFormatter().formatTable(headers, rows));
                return 0;
            } catch (Exception e) {
                cli.printError("Failed to show test set: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Create a new test set.
     */
    @Command(name = "create", description = "Create a new test set")
    public static class CreateCommand implements Callable<Integer> {
        @ParentCommand
        private TestSetCommand parent;

        @Parameters(index = "0", description = "Test set path (Release/TestSet)")
        private String testSetPath;

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

            String[] parts = testSetPath.split("/");
            if (parts.length != 2) {
                cli.printError("Invalid format. Use: Release/TestSet");
                return 1;
            }

            try {
                File releaseDir = new File(path, "TestLab/" + parts[0]);
                releaseDir.mkdirs();

                File testSetFile = new File(releaseDir, parts[1] + ".csv");
                if (testSetFile.exists()) {
                    cli.printError("Test set already exists: " + testSetPath);
                    return 1;
                }

                // Create with headers
                try (java.io.PrintWriter writer = new java.io.PrintWriter(testSetFile)) {
                    writer.println(
                        "Execute,Scenario,TestCase,Browser,Iteration,Platform,Description"
                    );
                }

                cli.printSuccess("Created test set: " + testSetPath);
                return 0;
            } catch (Exception e) {
                cli.printError("Failed to create test set: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Add test case to a test set.
     */
    @Command(name = "add", description = "Add test case to a test set")
    public static class AddCommand implements Callable<Integer> {
        @ParentCommand
        private TestSetCommand parent;

        @Option(
            names = { "--testset", "-t" },
            description = "Test set path (Release/TestSet)",
            required = true
        )
        private String testSetPath;

        @Option(
            names = { "--testcase", "-c" },
            description = "Test case path (Scenario/TestCase)",
            required = true
        )
        private String testCase;

        @Option(names = { "-p", "--project" }, description = "Project path")
        private String projectPath;

        @Option(names = { "--browser", "-b" }, description = "Browser", defaultValue = "Chrome")
        private String browser;

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();

            String path = projectPath != null ? projectPath : cli.getProjectPath();
            if (path == null || path.isEmpty()) {
                cli.printError("Project path required.");
                return 1;
            }

            String[] tsParts = testSetPath.split("/");
            String[] tcParts = testCase.split("/");

            if (tsParts.length != 2 || tcParts.length != 2) {
                cli.printError("Invalid path format.");
                return 1;
            }

            try {
                File testSetFile = new File(
                    path,
                    "TestLab/" + tsParts[0] + "/" + tsParts[1] + ".csv"
                );
                if (!testSetFile.exists()) {
                    cli.printError("Test set not found: " + testSetPath);
                    return 1;
                }

                // Append test case
                try (
                    java.io.FileWriter fw = new java.io.FileWriter(testSetFile, true);
                    java.io.PrintWriter writer = new java.io.PrintWriter(fw)
                ) {
                    writer.println("Y," + tcParts[0] + "," + tcParts[1] + "," + browser + ",1,,");
                }

                cli.printSuccess("Added " + testCase + " to " + testSetPath);
                return 0;
            } catch (Exception e) {
                cli.printError("Failed to add test case: " + e.getMessage());
                return 1;
            }
        }
    }
}

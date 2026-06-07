package com.ing.engine.cli.commands;

import com.ing.datalib.component.EnvTestData;
import com.ing.datalib.component.Project;
import com.ing.datalib.component.TestData;
import com.ing.datalib.testdata.model.Record;
import com.ing.datalib.testdata.model.TestDataModel;
import com.ing.engine.cli.INGeniousCLI;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * Test data management commands.
 */
@Command(
    name = "data",
    description = "Test data management",
    subcommands = {
        DataCommand.ListCommand.class,
        DataCommand.ShowCommand.class,
        DataCommand.GetCommand.class,
        DataCommand.SetCommand.class,
        DataCommand.ImportCommand.class,
        DataCommand.SheetCommand.class,
        DataCommand.RowCommand.class,
        DataCommand.ColumnCommand.class,
        DataCommand.EnvCommand.class
    }
)
public class DataCommand implements Callable<Integer> {

    @ParentCommand
    private INGeniousCLI parent;

    @Override
    public Integer call() {
        System.out.println("Use 'ingenious data <subcommand>' - see 'ingenious data --help'");
        return 0;
    }

    /**
     * List data sheets/environments.
     */
    @Command(name = "list", description = "List data sheets")
    public static class ListCommand implements Callable<Integer> {

        @ParentCommand
        private DataCommand parent;

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
                File testDataDir = new File(path, "TestData");
                if (!testDataDir.exists()) {
                    cli.printWarning("No test data found.");
                    return 0;
                }

                List<String> headers = Arrays.asList("Data Sheet", "Rows");
                List<List<String>> rows = new ArrayList<>();

                File[] dataFiles = testDataDir.listFiles(f -> f.isFile() && 
                    (f.getName().endsWith(".csv") || f.getName().endsWith(".xlsx")));
                    
                if (dataFiles != null) {
                    for (File df : dataFiles) {
                        int rowCount = countDataRows(df);
                        rows.add(Arrays.asList(df.getName(), String.valueOf(rowCount)));
                    }
                }

                if (rows.isEmpty()) {
                    cli.printWarning("No data sheets found.");
                    return 0;
                }

                System.out.println(cli.getOutputFormatter().formatTable(headers, rows));
                return 0;
                
            } catch (Exception e) {
                cli.printError("Failed to list data: " + e.getMessage());
                return 1;
            }
        }

        private int countDataRows(File file) {
            if (file.getName().endsWith(".csv")) {
                try (Scanner scanner = new Scanner(file)) {
                    int count = 0;
                    while (scanner.hasNextLine()) {
                        scanner.nextLine();
                        count++;
                    }
                    return Math.max(0, count - 1); // Exclude header
                } catch (Exception e) {
                    return 0;
                }
            }
            return 0; // For xlsx, would need Apache POI
        }
    }

    /**
     * Show data sheet contents.
     */
    @Command(name = "show", description = "Show data sheet contents")
    public static class ShowCommand implements Callable<Integer> {

        @ParentCommand
        private DataCommand parent;

        @Parameters(index = "0", description = "Data sheet name")
        private String sheetName;

        @Option(names = {"-p", "--project"}, description = "Project path")
        private String projectPath;

        @Option(names = {"--limit"}, description = "Number of rows to show", defaultValue = "20")
        private int limit;

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();
            
            String path = projectPath != null ? projectPath : cli.getProjectPath();
            if (path == null || path.isEmpty()) {
                cli.printError("Project path required.");
                return 1;
            }

            try {
                File dataFile = new File(path, "TestData/" + sheetName);
                if (!dataFile.exists()) {
                    // Try with .csv extension
                    dataFile = new File(path, "TestData/" + sheetName + ".csv");
                }
                
                if (!dataFile.exists()) {
                    cli.printError("Data sheet not found: " + sheetName);
                    return 1;
                }

                List<String> headers = new ArrayList<>();
                List<List<String>> rows = new ArrayList<>();

                try (Scanner scanner = new Scanner(dataFile)) {
                    boolean isHeader = true;
                    int rowCount = 0;
                    
                    while (scanner.hasNextLine() && rowCount < limit) {
                        String line = scanner.nextLine();
                        String[] cols = line.split(",", -1);
                        
                        if (isHeader) {
                            headers.addAll(Arrays.asList(cols));
                            isHeader = false;
                        } else {
                            rows.add(Arrays.asList(cols));
                            rowCount++;
                        }
                    }
                }

                System.out.println(cli.getOutputFormatter().formatTable(headers, rows));
                return 0;
                
            } catch (Exception e) {
                cli.printError("Failed to show data: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Get a specific data value.
     */
    @Command(name = "get", description = "Get a specific data value")
    public static class GetCommand implements Callable<Integer> {

        @ParentCommand
        private DataCommand parent;

        @Parameters(index = "0", description = "Data reference (Sheet:Column:Row)")
        private String reference;

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

            // Parse reference: Sheet:Column:Row or Sheet.Column[Row]
            String[] parts = reference.split(":");
            if (parts.length != 3) {
                cli.printError("Invalid reference format. Use: Sheet:Column:Row");
                return 1;
            }

            String sheet = parts[0];
            String column = parts[1];
            int row;
            try {
                row = Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) {
                cli.printError("Invalid row number: " + parts[2]);
                return 1;
            }

            try {
                File dataFile = new File(path, "TestData/" + sheet + ".csv");
                if (!dataFile.exists()) {
                    cli.printError("Data sheet not found: " + sheet);
                    return 1;
                }

                String value = readCellValue(dataFile, column, row);
                if (value == null) {
                    cli.printWarning("Value not found");
                } else {
                    System.out.println(value);
                }
                
                return 0;
                
            } catch (Exception e) {
                cli.printError("Failed to get data: " + e.getMessage());
                return 1;
            }
        }

        private String readCellValue(File file, String column, int targetRow) throws Exception {
            try (Scanner scanner = new Scanner(file)) {
                if (!scanner.hasNextLine()) return null;
                
                // Find column index
                String headerLine = scanner.nextLine();
                String[] headers = headerLine.split(",", -1);
                int colIndex = -1;
                for (int i = 0; i < headers.length; i++) {
                    if (headers[i].equalsIgnoreCase(column)) {
                        colIndex = i;
                        break;
                    }
                }
                
                if (colIndex == -1) return null;
                
                // Find row
                int currentRow = 1;
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    if (currentRow == targetRow) {
                        String[] cols = line.split(",", -1);
                        if (colIndex < cols.length) {
                            return cols[colIndex];
                        }
                        return null;
                    }
                    currentRow++;
                }
            }
            return null;
        }
    }

    /**
     * Set a specific data value.
     */
    @Command(name = "set", description = "Set a specific data value")
    public static class SetCommand implements Callable<Integer> {

        @ParentCommand
        private DataCommand parent;

        @Parameters(index = "0", description = "Data reference (Sheet:Column:Row)")
        private String reference;

        @Parameters(index = "1", description = "Value to set")
        private String value;

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

            String[] parts = reference.split(":");
            if (parts.length != 3) {
                cli.printError("Invalid reference format. Use: Sheet:Column:Row");
                return 1;
            }

            String sheet = parts[0];
            String column = parts[1];
            int targetRow;
            try {
                targetRow = Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) {
                cli.printError("Invalid row number: " + parts[2]);
                return 1;
            }

            try {
                File dataFile = new File(path, "TestData/" + sheet + ".csv");
                if (!dataFile.exists()) {
                    cli.printError("Data sheet not found: " + sheet);
                    return 1;
                }

                // Read all lines
                List<String> lines = new ArrayList<>();
                try (Scanner scanner = new Scanner(dataFile)) {
                    while (scanner.hasNextLine()) {
                        lines.add(scanner.nextLine());
                    }
                }

                if (lines.isEmpty()) {
                    cli.printError("Data sheet is empty");
                    return 1;
                }

                // Find column index
                String[] headers = lines.get(0).split(",", -1);
                int colIndex = -1;
                for (int i = 0; i < headers.length; i++) {
                    if (headers[i].equalsIgnoreCase(column)) {
                        colIndex = i;
                        break;
                    }
                }

                if (colIndex == -1) {
                    cli.printError("Column not found: " + column);
                    return 1;
                }

                if (targetRow >= lines.size()) {
                    cli.printError("Row not found: " + targetRow);
                    return 1;
                }

                // Update value
                String[] cols = lines.get(targetRow).split(",", -1);
                if (colIndex < cols.length) {
                    cols[colIndex] = value;
                    lines.set(targetRow, String.join(",", cols));
                }

                // Write back
                try (PrintWriter writer = new PrintWriter(dataFile)) {
                    for (String line : lines) {
                        writer.println(line);
                    }
                }

                cli.printSuccess("Updated " + reference + " = " + value);
                return 0;
                
            } catch (Exception e) {
                cli.printError("Failed to set data: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Import data from a file.
     */
    @Command(name = "import", description = "Import data from CSV/JSON")
    public static class ImportCommand implements Callable<Integer> {

        @ParentCommand
        private DataCommand parent;

        @Parameters(index = "0", description = "Source file path")
        private String sourcePath;

        @Option(names = {"--name", "-n"}, description = "Target data sheet name")
        private String targetName;

        @Option(names = {"-p", "--project"}, description = "Project path")
        private String projectPath;

        @Option(names = {"--overwrite"}, description = "Overwrite existing")
        private boolean overwrite;

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();
            
            String path = projectPath != null ? projectPath : cli.getProjectPath();
            if (path == null || path.isEmpty()) {
                cli.printError("Project path required.");
                return 1;
            }

            File sourceFile = new File(sourcePath);
            if (!sourceFile.exists()) {
                cli.printError("Source file not found: " + sourcePath);
                return 1;
            }

            String name = targetName != null ? targetName : sourceFile.getName();
            if (!name.endsWith(".csv") && !name.endsWith(".xlsx")) {
                name = name + ".csv";
            }

            try {
                File targetDir = new File(path, "TestData");
                targetDir.mkdirs();
                
                File targetFile = new File(targetDir, name);
                if (targetFile.exists() && !overwrite) {
                    cli.printError("Target exists. Use --overwrite to replace.");
                    return 1;
                }

                // Copy file
                java.nio.file.Files.copy(sourceFile.toPath(), targetFile.toPath(), 
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                cli.printSuccess("Imported data to: " + name);
                return 0;
                
            } catch (Exception e) {
                cli.printError("Import failed: " + e.getMessage());
                return 1;
            }
        }
    }

    // -----------------------------------------------------------------
    // Sheet management
    // -----------------------------------------------------------------

    @Command(name = "sheet", description = "Manage test data sheets",
            subcommands = { SheetCommand.CreateCommand.class })
    public static class SheetCommand implements Callable<Integer> {
        @ParentCommand private DataCommand parent;
        @Override public Integer call() {
            System.out.println("Use 'ingenious data sheet <subcommand>' - see 'ingenious data sheet --help'");
            return 0;
        }

        @Command(name = "create", description = "Create a new test data sheet")
        public static class CreateCommand implements Callable<Integer> {
            @ParentCommand private SheetCommand parent;

            @Parameters(index = "0", description = "Sheet name")
            private String sheetName;

            @Option(names = {"-p", "--project"}, description = "Project path")
            private String projectPath;

            @Option(names = {"-e", "--env"}, description = "Target environment (default: all)")
            private String envName;

            @Override
            public Integer call() {
                INGeniousCLI cli = INGeniousCLI.getInstance();
                String path = projectPath != null ? projectPath : cli.getProjectPath();
                if (path == null || path.isEmpty()) { cli.printError("Project path required."); return 1; }
                try {
                    Project project = new Project(path);
                    EnvTestData env = project.getTestData();
                    Collection<TestData> targets = pickEnvs(env, envName);
                    if (targets.isEmpty()) { cli.printError("Environment not found: " + envName); return 1; }
                    int added = 0;
                    for (TestData td : targets) {
                        if (td.getByName(sheetName) != null) continue;
                        td.addTestData(td.getNewTestData(sheetName));
                        added++;
                    }
                    env.save();
                    project.save();
                    cli.printSuccess("Created data sheet '" + sheetName + "' in " + added + " environment(s).");
                    return 0;
                } catch (Exception e) {
                    cli.printError("Failed to create sheet: " + e.getMessage());
                    return 1;
                }
            }
        }
    }

    // -----------------------------------------------------------------
    // Row management
    // -----------------------------------------------------------------

    @Command(name = "row", description = "Manage rows inside a data sheet",
            subcommands = { RowCommand.AddCommand.class })
    public static class RowCommand implements Callable<Integer> {
        @ParentCommand private DataCommand parent;
        @Override public Integer call() {
            System.out.println("Use 'ingenious data row <subcommand>' - see 'ingenious data row --help'");
            return 0;
        }

        @Command(name = "add", description =
                "Add a row binding a (Scenario, TestCase) - or reusable scenario/component - to a data sheet")
        public static class AddCommand implements Callable<Integer> {
            @ParentCommand private RowCommand parent;

            @Parameters(index = "0", description = "Sheet name")
            private String sheetName;

            @Option(names = {"--scenario"}, required = true,
                    description = "Scenario name (or reusable-scenario name if --reusable)")
            private String scenarioName;

            @Option(names = {"--testcase"}, required = true,
                    description = "Test case name (or reusable-component name if --reusable)")
            private String testCaseName;

            @Option(names = {"--reusable"},
                    description = "Treat scenario/testcase as a reusable-component reference")
            private boolean reusable;

            @Option(names = {"--iteration"}, description = "Iteration number", defaultValue = "1")
            private String iteration;

            @Option(names = {"--sub-iteration"}, description = "Sub-iteration number", defaultValue = "1")
            private String subIteration;

            @Option(names = {"-c", "--col"}, description = "Column value as name=value (repeatable)")
            private List<String> cols;

            @Option(names = {"-p", "--project"}, description = "Project path")
            private String projectPath;

            @Option(names = {"-e", "--env"}, description = "Target environment (default: all)")
            private String envName;

            @Override
            public Integer call() {
                INGeniousCLI cli = INGeniousCLI.getInstance();
                String path = projectPath != null ? projectPath : cli.getProjectPath();
                if (path == null || path.isEmpty()) { cli.printError("Project path required."); return 1; }
                try {
                    Project project = new Project(path);
                    EnvTestData env = project.getTestData();
                    Collection<TestData> targets = pickEnvs(env, envName);
                    if (targets.isEmpty()) { cli.printError("Environment not found: " + envName); return 1; }
                    int added = 0;
                    for (TestData td : targets) {
                        TestDataModel model = td.getByName(sheetName);
                        if (model == null) {
                            model = td.addTestData(td.getNewTestData(sheetName));
                        }
                        model.loadTableModel();
                        Record rec = model.addRecord();
                        // Scenario/Flow are positionally 0/1; setters keep us format-agnostic.
                        String scnDisplay = reusable ? ("(R) " + scenarioName) : scenarioName;
                        rec.setScenario(scnDisplay);
                        rec.setTestcase(testCaseName);
                        rec.setIteration(iteration);
                        rec.setSubIteration(subIteration);
                        // Extra column values
                        if (cols != null) {
                            int rowIdx = model.getRowCount() - 1;
                            for (String kv : cols) {
                                int eq = kv.indexOf('=');
                                if (eq <= 0) continue;
                                String k = kv.substring(0, eq).trim();
                                String v = kv.substring(eq + 1);
                                int colIdx = model.getColumnIndex(k);
                                if (colIdx < 0) {
                                    model.addColumn(k);
                                    colIdx = model.getColumnIndex(k);
                                }
                                if (colIdx >= 0) model.setValueAt(v, rowIdx, colIdx);
                            }
                        }
                        added++;
                    }
                    env.save();
                    project.save();
                    cli.printSuccess("Added row in " + added + " environment(s) to sheet '" + sheetName + "'");
                    return 0;
                } catch (Exception e) {
                    cli.printError("Failed to add row: " + e.getMessage());
                    return 1;
                }
            }
        }
    }

    // -----------------------------------------------------------------
    // Column management
    // -----------------------------------------------------------------

    @Command(name = "column", description = "Manage columns of a data sheet",
            subcommands = { ColumnCommand.AddCommand.class })
    public static class ColumnCommand implements Callable<Integer> {
        @ParentCommand private DataCommand parent;
        @Override public Integer call() {
            System.out.println("Use 'ingenious data column <subcommand>' - see 'ingenious data column --help'");
            return 0;
        }

        @Command(name = "add", description = "Add a column to a data sheet (all environments or a specific one)")
        public static class AddCommand implements Callable<Integer> {
            @ParentCommand private ColumnCommand parent;

            @Parameters(index = "0", description = "Sheet name") private String sheetName;
            @Parameters(index = "1", description = "Column name") private String columnName;

            @Option(names = {"-p", "--project"}, description = "Project path")
            private String projectPath;

            @Option(names = {"-e", "--env"},
                    description = "Limit to one environment (default: all)")
            private String envName;

            @Override
            public Integer call() {
                INGeniousCLI cli = INGeniousCLI.getInstance();
                String path = projectPath != null ? projectPath : cli.getProjectPath();
                if (path == null || path.isEmpty()) { cli.printError("Project path required."); return 1; }
                try {
                    Project project = new Project(path);
                    EnvTestData env = project.getTestData();
                    Collection<TestData> targets = pickEnvs(env, envName);
                    if (targets.isEmpty()) { cli.printError("Environment not found: " + envName); return 1; }
                    int touched = 0;
                    for (TestData td : targets) {
                        TestDataModel model = td.getByName(sheetName);
                        if (model == null) {
                            model = td.addTestData(td.getNewTestData(sheetName));
                        }
                        model.loadTableModel();
                        if (model.getColumnIndex(columnName) < 0) {
                            model.addColumn(columnName);
                            touched++;
                        }
                    }
                    env.save();
                    project.save();
                    cli.printSuccess("Added column '" + columnName + "' in " + touched + " environment(s).");
                    return 0;
                } catch (Exception e) {
                    cli.printError("Failed to add column: " + e.getMessage());
                    return 1;
                }
            }
        }
    }

    // -----------------------------------------------------------------
    // Environment management
    // -----------------------------------------------------------------

    @Command(name = "env", description = "Manage test data environments",
            subcommands = {
                EnvCommand.ListCommand.class,
                EnvCommand.CreateCommand.class,
                EnvCommand.DeleteCommand.class
            })
    public static class EnvCommand implements Callable<Integer> {
        @ParentCommand private DataCommand parent;
        @Override public Integer call() {
            System.out.println("Use 'ingenious data env <subcommand>' - see 'ingenious data env --help'");
            return 0;
        }

        @Command(name = "list", description = "List environments")
        public static class ListCommand implements Callable<Integer> {
            @ParentCommand private EnvCommand parent;
            @Option(names = {"-p", "--project"}, description = "Project path")
            private String projectPath;

            @Override
            public Integer call() {
                INGeniousCLI cli = INGeniousCLI.getInstance();
                String path = projectPath != null ? projectPath : cli.getProjectPath();
                if (path == null || path.isEmpty()) { cli.printError("Project path required."); return 1; }
                try {
                    Project project = new Project(path);
                    Set<String> envs = project.getTestData().getEnvironments();
                    if (envs.isEmpty()) { cli.printInfo("No environments configured."); return 0; }
                    for (String e : envs) System.out.println(e);
                    return 0;
                } catch (Exception e) {
                    cli.printError("Failed to list envs: " + e.getMessage());
                    return 1;
                }
            }
        }

        @Command(name = "create", description = "Create a new environment (optionally cloning from another)")
        public static class CreateCommand implements Callable<Integer> {
            @ParentCommand private EnvCommand parent;

            @Parameters(index = "0", description = "Environment name") private String envName;

            @Option(names = {"--from"}, description = "Clone all sheets from this existing environment")
            private String cloneFrom;

            @Option(names = {"--with-global"},
                    description = "When cloning, also clone global data")
            private boolean withGlobal;

            @Option(names = {"-p", "--project"}, description = "Project path")
            private String projectPath;

            @Override
            public Integer call() {
                INGeniousCLI cli = INGeniousCLI.getInstance();
                String path = projectPath != null ? projectPath : cli.getProjectPath();
                if (path == null || path.isEmpty()) { cli.printError("Project path required."); return 1; }
                try {
                    Project project = new Project(path);
                    EnvTestData env = project.getTestData();
                    if (cloneFrom != null && !cloneFrom.isEmpty()) {
                        TestData src = env.getTestDataFor(cloneFrom);
                        if (src == null) { cli.printError("Source env not found: " + cloneFrom); return 1; }
                        List<String> sheets = new ArrayList<>();
                        for (TestDataModel m : src.getTestDataList()) sheets.add(m.getName());
                        env.createNewEnvironment(envName, cloneFrom, sheets, withGlobal);
                    } else {
                        env.createNewEnvironment(envName);
                    }
                    env.save();
                    project.save();
                    cli.printSuccess("Created environment: " + envName);
                    return 0;
                } catch (Exception e) {
                    cli.printError("Failed to create env: " + e.getMessage());
                    return 1;
                }
            }
        }

        @Command(name = "delete", description = "Delete an environment")
        public static class DeleteCommand implements Callable<Integer> {
            @ParentCommand private EnvCommand parent;

            @Parameters(index = "0", description = "Environment name") private String envName;

            @Option(names = {"--force", "-f"}, description = "Skip confirmation")
            private boolean force;

            @Option(names = {"-p", "--project"}, description = "Project path")
            private String projectPath;

            @Override
            public Integer call() {
                INGeniousCLI cli = INGeniousCLI.getInstance();
                String path = projectPath != null ? projectPath : cli.getProjectPath();
                if (path == null || path.isEmpty()) { cli.printError("Project path required."); return 1; }
                if (!force) { cli.printWarning("Use --force to confirm deletion of environment: " + envName); return 1; }
                try {
                    Project project = new Project(path);
                    EnvTestData env = project.getTestData();
                    if (env.getTestDataFor(envName) == null) {
                        cli.printError("Environment not found: " + envName);
                        return 1;
                    }
                    env.deleteEnvironment(envName);
                    env.save();
                    project.save();
                    cli.printSuccess("Deleted environment: " + envName);
                    return 0;
                } catch (Exception e) {
                    cli.printError("Failed to delete env: " + e.getMessage());
                    return 1;
                }
            }
        }
    }

    /**
     * Returns the {@link TestData} containers to operate on. {@code null}
     * or "all" returns every configured environment.
     */
    public static Collection<TestData> pickEnvs(EnvTestData env, String envName) {
        if (envName == null || envName.isEmpty() || "all".equalsIgnoreCase(envName)) {
            return env.getAllEnvironments();
        }
        TestData td = env.getTestDataFor(envName);
        return td == null ? Collections.emptyList() : Collections.singletonList(td);
    }
}

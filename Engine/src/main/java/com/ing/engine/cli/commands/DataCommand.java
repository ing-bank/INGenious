package com.ing.engine.cli.commands;

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
        DataCommand.ImportCommand.class
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
}

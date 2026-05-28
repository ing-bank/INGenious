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
 * Page object/object repository management commands.
 */
@Command(
    name = "object",
    aliases = {"objects", "or"},
    description = "Object repository management",
    subcommands = {
        ObjectCommand.ListCommand.class,
        ObjectCommand.ShowCommand.class,
        ObjectCommand.SearchCommand.class,
        ObjectCommand.CreateCommand.class
    }
)
public class ObjectCommand implements Callable<Integer> {

    @ParentCommand
    private INGeniousCLI parent;

    @Override
    public Integer call() {
        System.out.println("Use 'ingenious object <subcommand>' - see 'ingenious object --help'");
        return 0;
    }

    /**
     * List pages/object groups.
     */
    @Command(name = "list", description = "List pages in object repository")
    public static class ListCommand implements Callable<Integer> {

        @ParentCommand
        private ObjectCommand parent;

        @Option(names = {"-p", "--project"}, description = "Project path")
        private String projectPath;

        @Option(names = {"--with-count"}, description = "Show object count per page")
        private boolean withCount;

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();
            
            String path = projectPath != null ? projectPath : cli.getProjectPath();
            if (path == null || path.isEmpty()) {
                cli.printError("Project path required.");
                return 1;
            }

            try {
                File orDir = new File(path, "ObjectRepository");
                if (!orDir.exists()) {
                    cli.printWarning("No object repository found.");
                    return 0;
                }

                List<String> headers = withCount 
                    ? Arrays.asList("Page", "Objects")
                    : Arrays.asList("Page");
                List<List<String>> rows = new ArrayList<>();

                File[] pages = orDir.listFiles(f -> f.isFile() && f.getName().endsWith(".csv"));
                if (pages != null) {
                    for (File page : pages) {
                        String pageName = page.getName().replace(".csv", "");
                        if (withCount) {
                            int count = countObjects(page);
                            rows.add(Arrays.asList(pageName, String.valueOf(count)));
                        } else {
                            rows.add(Arrays.asList(pageName));
                        }
                    }
                }

                if (rows.isEmpty()) {
                    cli.printWarning("No pages found in object repository.");
                    return 0;
                }

                System.out.println(cli.getOutputFormatter().formatTable(headers, rows));
                cli.printInfo("\nTotal: " + rows.size() + " pages");
                return 0;
                
            } catch (Exception e) {
                cli.printError("Failed to list pages: " + e.getMessage());
                return 1;
            }
        }

        private int countObjects(File page) {
            try (Scanner scanner = new Scanner(page)) {
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
     * Show objects in a page.
     */
    @Command(name = "show", description = "Show objects in a page")
    public static class ShowCommand implements Callable<Integer> {

        @ParentCommand
        private ObjectCommand parent;

        @Parameters(index = "0", description = "Page name")
        private String pageName;

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
                File pageFile = new File(path, "ObjectRepository/" + pageName + ".csv");
                if (!pageFile.exists()) {
                    cli.printError("Page not found: " + pageName);
                    return 1;
                }

                cli.printInfo("Page: " + pageName);
                System.out.println();

                List<String> headers = Arrays.asList("Name", "Type", "Locator", "Value");
                List<List<String>> rows = new ArrayList<>();

                try (Scanner scanner = new Scanner(pageFile)) {
                    boolean isHeader = true;
                    while (scanner.hasNextLine()) {
                        String line = scanner.nextLine();
                        if (isHeader) {
                            isHeader = false;
                            continue;
                        }
                        
                        String[] cols = line.split(",", -1);
                        if (cols.length >= 4) {
                            rows.add(Arrays.asList(
                                cols[0], // Name
                                cols.length > 1 ? cols[1] : "", // Type
                                cols.length > 2 ? cols[2] : "", // Locator
                                truncate(cols.length > 3 ? cols[3] : "", 40) // Value
                            ));
                        }
                    }
                }

                System.out.println(cli.getOutputFormatter().formatTable(headers, rows));
                cli.printInfo("\nTotal: " + rows.size() + " objects");
                return 0;
                
            } catch (Exception e) {
                cli.printError("Failed to show page: " + e.getMessage());
                return 1;
            }
        }

        private String truncate(String text, int maxLength) {
            if (text.length() <= maxLength) return text;
            return text.substring(0, maxLength - 3) + "...";
        }
    }

    /**
     * Search for objects across all pages.
     */
    @Command(name = "search", description = "Search for objects")
    public static class SearchCommand implements Callable<Integer> {

        @ParentCommand
        private ObjectCommand parent;

        @Parameters(index = "0", description = "Search query")
        private String query;

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
                File orDir = new File(path, "ObjectRepository");
                if (!orDir.exists()) {
                    cli.printWarning("No object repository found.");
                    return 0;
                }

                String queryLower = query.toLowerCase();
                List<String> headers = Arrays.asList("Page", "Object", "Type", "Locator");
                List<List<String>> rows = new ArrayList<>();

                File[] pages = orDir.listFiles(f -> f.isFile() && f.getName().endsWith(".csv"));
                if (pages != null) {
                    for (File page : pages) {
                        String pageName = page.getName().replace(".csv", "");
                        
                        try (Scanner scanner = new Scanner(page)) {
                            boolean isHeader = true;
                            while (scanner.hasNextLine()) {
                                String line = scanner.nextLine();
                                if (isHeader) {
                                    isHeader = false;
                                    continue;
                                }
                                
                                if (line.toLowerCase().contains(queryLower)) {
                                    String[] cols = line.split(",", -1);
                                    if (cols.length >= 3) {
                                        rows.add(Arrays.asList(
                                            pageName,
                                            cols[0],
                                            cols.length > 1 ? cols[1] : "",
                                            cols.length > 2 ? cols[2] : ""
                                        ));
                                    }
                                }
                            }
                        }
                    }
                }

                if (rows.isEmpty()) {
                    cli.printWarning("No objects found matching: " + query);
                    return 0;
                }

                System.out.println(cli.getOutputFormatter().formatTable(headers, rows));
                cli.printInfo("\nFound " + rows.size() + " matching objects");
                return 0;
                
            } catch (Exception e) {
                cli.printError("Search failed: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Create a new page or object.
     */
    @Command(name = "create", description = "Create a new page or add object")
    public static class CreateCommand implements Callable<Integer> {

        @ParentCommand
        private ObjectCommand parent;

        @Option(names = {"--page"}, description = "Page name")
        private String page;

        @Option(names = {"--name", "-n"}, description = "Object name")
        private String objectName;

        @Option(names = {"--type"}, description = "Object type", defaultValue = "WebElement")
        private String objectType;

        @Option(names = {"--locator"}, description = "Locator type (id, css, xpath)")
        private String locator;

        @Option(names = {"--value"}, description = "Locator value")
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

            if (page == null) {
                cli.printError("Page name required. Use --page");
                return 1;
            }

            try {
                File orDir = new File(path, "ObjectRepository");
                orDir.mkdirs();

                File pageFile = new File(orDir, page + ".csv");
                
                if (objectName == null) {
                    // Create new page only
                    if (pageFile.exists()) {
                        cli.printError("Page already exists: " + page);
                        return 1;
                    }
                    
                    try (PrintWriter writer = new PrintWriter(pageFile)) {
                        writer.println("Name,Type,Locator,Value,Description");
                    }
                    
                    cli.printSuccess("Created page: " + page);
                } else {
                    // Add object to page
                    if (!pageFile.exists()) {
                        // Create page first
                        try (PrintWriter writer = new PrintWriter(pageFile)) {
                            writer.println("Name,Type,Locator,Value,Description");
                        }
                    }
                    
                    try (FileWriter fw = new FileWriter(pageFile, true);
                         PrintWriter writer = new PrintWriter(fw)) {
                        writer.println(objectName + "," + objectType + "," + 
                                     (locator != null ? locator : "") + "," +
                                     (value != null ? value : "") + ",");
                    }
                    
                    cli.printSuccess("Added object " + objectName + " to page " + page);
                }
                
                return 0;
                
            } catch (Exception e) {
                cli.printError("Failed to create: " + e.getMessage());
                return 1;
            }
        }
    }
}

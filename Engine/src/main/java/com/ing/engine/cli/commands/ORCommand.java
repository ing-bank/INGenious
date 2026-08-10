package com.ing.engine.cli.commands;

import com.ing.engine.cli.INGeniousCLI;
import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/**
 * Object repository management commands.
 */
@Command(
    name = "or",
    mixinStandardHelpOptions = true,
    description = "Object repository management",
    subcommands = {
        ORCommand.ListCommand.class, ORCommand.ShowCommand.class, ORCommand.SearchCommand.class
    }
)
public class ORCommand implements Callable<Integer> {
    @ParentCommand
    private INGeniousCLI parent;

    @Override
    public Integer call() {
        System.out.println("Use 'ingenious or <subcommand>' - see 'ingenious or --help'");
        return 0;
    }

    /**
     * List pages/object groups.
     */
    @Command(name = "list", description = "List pages in object repository")
    public static class ListCommand implements Callable<Integer> {
        @ParentCommand
        private ORCommand parent;

        @Option(names = { "-p", "--project" }, description = "Project path")
        private String projectPath;

        @Option(names = { "--with-count" }, description = "Show object count per page")
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
                    ? Arrays.asList("Page", "Format", "Objects")
                    : Arrays.asList("Page", "Format");
                List<List<String>> rows = new ArrayList<>();

                // Collect all .yaml / .yml pages recursively (the modern YAML OR format)
                List<File> yamlPages = new ArrayList<>();
                findFiles(orDir, yamlPages);
                for (File page : yamlPages) {
                    String rel = orDir.toURI().relativize(page.toURI()).getPath();
                    String ext = "YAML";
                    if (rel.endsWith(".yaml")) {
                        rel = rel.substring(0, rel.length() - 5);
                    } else if (rel.endsWith(".yml")) {
                        rel = rel.substring(0, rel.length() - 4);
                    }
                    if (withCount) {
                        int count = countYamlObjects(page);
                        rows.add(Arrays.asList(rel, ext, String.valueOf(count)));
                    } else {
                        rows.add(Arrays.asList(rel, ext));
                    }
                }

                // Also check for legacy IOR.object / MOR.object / SharedOR.object (XML format)
                String[] legacyObjectFiles = {
                    "IOR.object",
                    "MOR.object",
                    "SapOR.object",
                    "StructuredDataOR.object"
                };
                for (String legacyName : legacyObjectFiles) {
                    File legacy = new File(path, legacyName);
                    if (legacy.isFile()) {
                        String pageName = legacyName.replace(".object", "");
                        if (withCount) {
                            rows.add(Arrays.asList(pageName, "XML (legacy)", "?"));
                        } else {
                            rows.add(Arrays.asList(pageName, "XML (legacy)"));
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

        private void findFiles(File dir, List<File> result) {
            File[] files = dir.listFiles();
            if (files == null) return;
            for (File f : files) {
                if (f.isDirectory()) {
                    findFiles(f, result);
                } else {
                    String name = f.getName().toLowerCase();
                    if (name.endsWith(".yaml") || name.endsWith(".yml")) {
                        result.add(f);
                    }
                }
            }
        }

        private int countYamlObjects(File page) {
            try (Scanner scanner = new Scanner(page)) {
                int count = 0;
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    if (
                        !line.isEmpty() &&
                        !line.startsWith(" ") &&
                        !line.startsWith("\t") &&
                        !line.startsWith("#") &&
                        !line.startsWith("-") &&
                        !line.startsWith("---") &&
                        !line.startsWith("...")
                    ) {
                        count++;
                    }
                }
                return Math.max(0, count);
            } catch (Exception e) {
                return 0;
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
        private ORCommand parent;

        @Parameters(index = "0", description = "Page name")
        private String pageName;

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
                            rows.add(
                                Arrays.asList(
                                    cols[0], // Name
                                    cols.length > 1 ? cols[1] : "", // Type
                                    cols.length > 2 ? cols[2] : "", // Locator
                                    truncate(cols.length > 3 ? cols[3] : "", 40) // Value
                                )
                            );
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
        private ORCommand parent;

        @Parameters(index = "0", description = "Search query")
        private String query;

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
                                        rows.add(
                                            Arrays.asList(
                                                pageName,
                                                cols[0],
                                                cols.length > 1 ? cols[1] : "",
                                                cols.length > 2 ? cols[2] : ""
                                            )
                                        );
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
}

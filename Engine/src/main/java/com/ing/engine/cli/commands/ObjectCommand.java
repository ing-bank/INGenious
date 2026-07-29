package com.ing.engine.cli.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.ing.engine.cli.INGeniousCLI;
import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/**
 * Object management inside pages of the object repository.
 * Where "or" manages pages, this command manages objects within pages.
 * Supports YAML (primary) and CSV (legacy) formats.
 */
@Command(
    name = "object",
    aliases = { "objects" },
    mixinStandardHelpOptions = true,
    description = "Object repository - manage objects within pages",
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

    private static final List<String> LOCATOR_KEYS = Arrays.asList(
        "css",
        "xpath",
        "id",
        "name",
        "className",
        "class",
        "label",
        "text",
        "role",
        "placeholder",
        "testId",
        "data-test",
        "ariaLabel",
        "alt",
        "title",
        "href",
        "chainedLocator",
        "ng-model",
        "data-qa"
    );

    static String[] detectLocator(Map<String, Object> props) {
        if (props == null) return new String[] { "", "" };
        for (String key : LOCATOR_KEYS) {
            Object val = props.get(key);
            if (val != null && !val.toString().isEmpty()) {
                return new String[] { key, val.toString() };
            }
        }
        for (Map.Entry<String, Object> e : props.entrySet()) {
            if (e.getValue() != null && !e.getValue().toString().isEmpty()) {
                return new String[] { e.getKey(), e.getValue().toString() };
            }
        }
        return new String[] { "", "" };
    }

    static String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> readYaml(File file) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        return mapper.readValue(file, LinkedHashMap.class);
    }

    static class PageInfo {
        final String name;
        final File file;

        PageInfo(String name, File file) {
            this.name = name;
            this.file = file;
        }
    }

    static class ObjectEntry {
        final String pageName;
        final String objectName;
        final String locatorType;
        final String locatorValue;
        final Map<String, String> properties;

        ObjectEntry(
            String pageName,
            String objectName,
            String locatorType,
            String locatorValue,
            Map<String, String> properties
        ) {
            this.pageName = pageName;
            this.objectName = objectName;
            this.locatorType = locatorType;
            this.locatorValue = locatorValue;
            this.properties = properties;
        }
    }

    static List<PageInfo> findPages(File orDir) {
        List<PageInfo> pages = new ArrayList<>();
        List<File> yamlFiles = new ArrayList<>();
        findYamlFiles(orDir, yamlFiles);
        for (File f : yamlFiles) {
            try {
                Map<String, Object> data = readYaml(f);
                String name = data.containsKey("page")
                    ? data.get("page").toString()
                    : stripExt(f.getName());
                pages.add(new PageInfo(name, f));
            } catch (Exception ignored) {}
        }

        List<File> csvFiles = new ArrayList<>();
        findCsvFiles(orDir, csvFiles);
        for (File f : csvFiles) {
            String name = stripExt(f.getName());
            boolean dup = false;
            for (PageInfo p : pages) {
                if (p.name.equalsIgnoreCase(name)) {
                    dup = true;
                    break;
                }
            }
            if (!dup) pages.add(new PageInfo(name, f));
        }
        pages.sort(Comparator.comparing(p -> p.name));
        return pages;
    }

    static List<ObjectEntry> loadObjects(File orDir, String pageFilter) {
        List<ObjectEntry> result = new ArrayList<>();
        for (PageInfo page : findPages(orDir)) {
            if (
                pageFilter != null && !page.name.toLowerCase().contains(pageFilter.toLowerCase())
            ) continue;
            result.addAll(loadObjectsFromPage(page));
        }
        return result;
    }

    static List<ObjectEntry> loadObjectsFromPage(PageInfo page) {
        String lower = page.file.getName().toLowerCase();
        if (lower.endsWith(".yaml") || lower.endsWith(".yml")) return loadYamlObjects(
            page
        ); else if (lower.endsWith(".csv")) return loadCsvObjects(page);
        return Collections.emptyList();
    }

    static PageInfo findPage(File orDir, String pageName) {
        for (PageInfo p : findPages(orDir)) {
            if (p.name.equalsIgnoreCase(pageName)) return p;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static List<ObjectEntry> loadYamlObjects(PageInfo page) {
        List<ObjectEntry> result = new ArrayList<>();
        try {
            Map<String, Object> data = readYaml(page.file);
            String pageName = data.containsKey("page") ? data.get("page").toString() : page.name;

            Map<String, Object> elements = (Map<String, Object>) data.get("elements");
            if (elements == null) return result;

            List<String> keys = new ArrayList<>(elements.keySet());
            Collections.sort(keys, String.CASE_INSENSITIVE_ORDER);

            for (String objName : keys) {
                Object raw = elements.get(objName);
                Map<String, String> props = new LinkedHashMap<>();
                String locType = "";
                String locValue = "";

                if (raw instanceof Map) {
                    Map<String, Object> rawMap = (Map<String, Object>) raw;
                    for (Map.Entry<String, Object> e : rawMap.entrySet()) {
                        props.put(e.getKey(), e.getValue() != null ? e.getValue().toString() : "");
                    }
                    String[] loc = detectLocator(rawMap);
                    locType = loc[0];
                    locValue = loc[1];
                } else if (raw != null) {
                    String str = raw.toString();
                    props.put("value", str);
                    locValue = str;
                }

                result.add(new ObjectEntry(pageName, objName, locType, locValue, props));
            }
        } catch (Exception ignored) {}
        return result;
    }

    private static List<ObjectEntry> loadCsvObjects(PageInfo page) {
        List<ObjectEntry> result = new ArrayList<>();
        try (Scanner scanner = new Scanner(page.file)) {
            boolean isHeader = true;
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (isHeader) {
                    isHeader = false;
                    continue;
                }
                String[] cols = line.split(",", -1);
                if (cols.length >= 1 && !cols[0].trim().isEmpty()) {
                    String objName = cols[0].trim();
                    String locType = cols.length > 1 ? cols[1].trim() : "";
                    String locValue = cols.length > 2 ? cols[2].trim() : "";
                    String val = cols.length > 3 ? cols[3].trim() : "";
                    Map<String, String> props = new LinkedHashMap<>();
                    props.put("Type", locType);
                    props.put("Locator", locValue);
                    props.put("Value", val);
                    result.add(new ObjectEntry(page.name, objName, locType, locValue, props));
                }
            }
        } catch (Exception ignored) {}
        return result;
    }

    private static void findYamlFiles(File dir, List<File> result) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) findYamlFiles(f, result); else {
                String n = f.getName().toLowerCase();
                if (n.endsWith(".yaml") || n.endsWith(".yml")) result.add(f);
            }
        }
    }

    private static void findCsvFiles(File dir, List<File> result) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) findCsvFiles(f, result); else if (
                f.getName().toLowerCase().endsWith(".csv")
            ) result.add(f);
        }
    }

    private static String stripExt(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    @Command(name = "list", description = "List objects across pages in the object repository")
    public static class ListCommand implements Callable<Integer> {
        @ParentCommand
        private ObjectCommand parent;

        @Option(names = { "-p", "--project" }, description = "Project path")
        private String projectPath;

        @Option(
            names = { "--page" },
            description = "Filter by page name (case-insensitive substring)"
        )
        private String pageFilter;

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();
            String path = projectPath != null ? projectPath : cli.getProjectPath();
            if (path == null || path.isEmpty()) {
                cli.printError("Project path required.");
                return 1;
            }

            File orDir = new File(path, "ObjectRepository");
            if (!orDir.exists()) {
                cli.printWarning("No object repository found at " + orDir.getPath());
                return 0;
            }

            List<ObjectEntry> objects = loadObjects(orDir, pageFilter);
            if (objects.isEmpty()) {
                cli.printWarning("No objects found.");
                return 0;
            }

            List<String> headers = Arrays.asList("Page", "Object", "Locator", "Value");
            List<List<String>> rows = new ArrayList<>();
            for (ObjectEntry obj : objects) rows.add(
                Arrays.asList(
                    obj.pageName,
                    obj.objectName,
                    obj.locatorType,
                    truncate(obj.locatorValue, 50)
                )
            );
            System.out.println(cli.getOutputFormatter().formatTable(headers, rows));
            long pageCount = objects.stream().map(o -> o.pageName).distinct().count();
            cli.printInfo("\nTotal: " + objects.size() + " objects across " + pageCount + " pages");
            return 0;
        }
    }

    @Command(name = "show", description = "Show full details of an object (page and object name)")
    public static class ShowCommand implements Callable<Integer> {
        @ParentCommand
        private ObjectCommand parent;

        @Parameters(index = "0", description = "Page name")
        private String pageNameParam;

        @Parameters(index = "1", description = "Object name")
        private String objectName;

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

            File orDir = new File(path, "ObjectRepository");
            if (!orDir.exists()) {
                cli.printError("No object repository found.");
                return 1;
            }

            PageInfo page = findPage(orDir, pageNameParam);
            if (page == null) {
                cli.printError("Page not found: " + pageNameParam);
                return 1;
            }

            List<ObjectEntry> objs = loadObjectsFromPage(page);
            ObjectEntry found = null;
            for (ObjectEntry o : objs) {
                if (o.objectName.equalsIgnoreCase(objectName)) {
                    found = o;
                    break;
                }
            }
            if (found == null) {
                cli.printError("Object not found: " + objectName + " in page " + page.name);
                return 1;
            }

            cli.printInfo("Page: " + page.name + "  /  Object: " + found.objectName);
            System.out.println();
            if (found.properties == null || found.properties.isEmpty()) {
                cli.printWarning("Object has no properties.");
                return 0;
            }

            List<String> headers = Arrays.asList("Property", "Value");
            List<List<String>> rows = new ArrayList<>();
            for (Map.Entry<String, String> e : found.properties.entrySet()) rows.add(
                Arrays.asList(e.getKey(), truncate(e.getValue(), 80))
            );
            System.out.println(cli.getOutputFormatter().formatTable(headers, rows));
            cli.printInfo("\n" + rows.size() + " properties");
            return 0;
        }
    }

    @Command(name = "search", description = "Search for objects across all pages")
    public static class SearchCommand implements Callable<Integer> {
        @ParentCommand
        private ObjectCommand parent;

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

            File orDir = new File(path, "ObjectRepository");
            if (!orDir.exists()) {
                cli.printWarning("No object repository found.");
                return 0;
            }

            String q = query.toLowerCase();
            List<ObjectEntry> all = loadObjects(orDir, null);
            List<ObjectEntry> matches = new ArrayList<>();
            for (ObjectEntry obj : all) {
                if (obj.objectName.toLowerCase().contains(q)) {
                    matches.add(obj);
                    continue;
                }
                if (obj.locatorType.toLowerCase().contains(q)) {
                    matches.add(obj);
                    continue;
                }
                if (obj.locatorValue.toLowerCase().contains(q)) {
                    matches.add(obj);
                    continue;
                }
                if (obj.properties != null) {
                    for (String val : obj.properties.values()) {
                        if (val.toLowerCase().contains(q)) {
                            matches.add(obj);
                            break;
                        }
                    }
                }
            }

            if (matches.isEmpty()) {
                cli.printWarning("No objects found matching: " + query);
                return 0;
            }

            List<String> headers = Arrays.asList("Page", "Object", "Locator", "Value");
            List<List<String>> rows = new ArrayList<>();
            for (ObjectEntry obj : matches) rows.add(
                Arrays.asList(
                    obj.pageName,
                    obj.objectName,
                    obj.locatorType,
                    truncate(obj.locatorValue, 50)
                )
            );
            System.out.println(cli.getOutputFormatter().formatTable(headers, rows));
            cli.printInfo("\nFound " + matches.size() + " matching objects");
            return 0;
        }
    }

    @Command(name = "create", description = "Create a new object in an existing page")
    public static class CreateCommand implements Callable<Integer> {
        @ParentCommand
        private ObjectCommand parent;

        @Option(names = { "--page" }, required = true, description = "Page name")
        private String pageNameParam;

        @Option(names = { "--name", "-n" }, required = true, description = "Object name")
        private String objectName;

        @Option(names = { "--type" }, description = "Locator type (css, xpath, id, label, ...)")
        private String locatorType;

        @Option(names = { "--value" }, description = "Locator value")
        private String locatorValue;

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

            File orDir = new File(path, "ObjectRepository");
            if (!orDir.exists()) {
                cli.printError("Object repository not found at " + orDir.getPath());
                return 1;
            }

            PageInfo page = findPage(orDir, pageNameParam);
            if (page == null) {
                cli.printError("Page not found: " + pageNameParam);
                List<PageInfo> all = findPages(orDir);
                if (!all.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < all.size(); i++) {
                        if (i > 0) sb.append(", ");
                        sb.append(all.get(i).name);
                    }
                    cli.printInfo("Available pages: " + sb);
                }
                return 1;
            }

            String fname = page.file.getName().toLowerCase();
            if (!fname.endsWith(".yaml") && !fname.endsWith(".yml")) {
                cli.printError(
                    "Cannot add objects to non-YAML pages (" + fname + "). Use YAML format."
                );
                return 1;
            }

            try {
                Map<String, Object> data = readYaml(page.file);
                @SuppressWarnings("unchecked")
                Map<String, Object> elements = (Map<String, Object>) data.get("elements");
                if (elements == null) {
                    elements = new LinkedHashMap<>();
                    data.put("elements", elements);
                }
                if (elements.containsKey(objectName)) {
                    cli.printError(
                        "Object '" + objectName + "' already exists in page " + page.name
                    );
                    return 1;
                }

                Map<String, String> newObj = new LinkedHashMap<>();
                if (locatorType != null && !locatorType.isEmpty()) {
                    newObj.put(locatorType, locatorValue != null ? locatorValue : "");
                } else if (locatorValue != null && !locatorValue.isEmpty()) {
                    if (locatorValue.startsWith("//") || locatorValue.startsWith("(//")) newObj.put(
                        "xpath",
                        locatorValue
                    ); else if (
                        locatorValue.startsWith("#") ||
                        locatorValue.startsWith(".") ||
                        locatorValue.startsWith("[")
                    ) newObj.put("css", locatorValue); else newObj.put("label", locatorValue);
                }
                elements.put(objectName, newObj);
                ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
                mapper.writerWithDefaultPrettyPrinter().writeValue(page.file, data);
                cli.printSuccess("Created object '" + objectName + "' in page '" + page.name + "'");
                return 0;
            } catch (Exception e) {
                cli.printError("Failed to create object: " + e.getMessage());
                return 1;
            }
        }
    }
}

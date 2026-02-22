package com.ing.engine.cli.commands;

import com.ing.engine.cli.INGeniousCLI;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.ObjectType;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Action discovery and documentation commands.
 * Critical for AI/Copilot integration - exposes all available test actions.
 */
@Command(
    name = "action",
    aliases = {"actions"},
    description = "Discover and document available test actions",
    subcommands = {
        ActionCommand.ListCommand.class,
        ActionCommand.SearchCommand.class,
        ActionCommand.InfoCommand.class,
        ActionCommand.CategoriesCommand.class
    }
)
public class ActionCommand implements Callable<Integer> {

    @ParentCommand
    private INGeniousCLI parent;
    
    // Category mapping from ObjectType to user-friendly names
    private static final Map<String, String> CATEGORY_MAPPING = new LinkedHashMap<>();
    static {
        // Browser actions (Playwright, Browser, Web)
        CATEGORY_MAPPING.put("PLAYWRIGHT", "Browser");
        CATEGORY_MAPPING.put("BROWSER", "Browser");
        CATEGORY_MAPPING.put("WEB", "Browser");
        
        // API actions (Webservice)
        CATEGORY_MAPPING.put("WEBSERVICE", "API");
        
        // Database actions
        CATEGORY_MAPPING.put("DATABASE", "Database");
        
        // Kafka/Queue actions
        CATEGORY_MAPPING.put("KAFKA", "Kafka");
        CATEGORY_MAPPING.put("QUEUE", "Kafka");
        
        // Mobile actions
        CATEGORY_MAPPING.put("MOBILE", "Mobile");
        CATEGORY_MAPPING.put("APP", "Mobile");
        
        // General actions
        CATEGORY_MAPPING.put("GENERAL", "General");
        CATEGORY_MAPPING.put("ANY", "General");
        CATEGORY_MAPPING.put("DATA", "General");
        CATEGORY_MAPPING.put("FILE", "General");
        CATEGORY_MAPPING.put("IMAGE", "General");
        CATEGORY_MAPPING.put("STRINGOPERATIONS", "General");
        CATEGORY_MAPPING.put("PROTRACTORJS", "General");
    }

    @Override
    public Integer call() {
        INGeniousCLI cli = INGeniousCLI.getInstance();
        System.out.println("Available action types:");
        System.out.println("  Browser   - Web browser automation actions (Playwright)");
        System.out.println("  API       - REST/SOAP web service actions");
        System.out.println("  Database  - Database query and verification actions");
        System.out.println("  Kafka     - Kafka and message queue actions");
        System.out.println("  Mobile    - Mobile app automation actions");
        System.out.println("  General   - General purpose utility actions");
        System.out.println("\nUsage: ingenious action list <type>");
        System.out.println("       ingenious action list Browser");
        System.out.println("       ingenious action list --all");
        return 0;
    }

    /**
     * List all available actions.
     */
    @Command(name = "list", description = "List available test actions by type")
    public static class ListCommand implements Callable<Integer> {

        @ParentCommand
        private ActionCommand parent;

        @Parameters(index = "0", arity = "0..1", 
                   description = "Action type: Browser, API, Database, Kafka, Mobile, General")
        private String actionType;

        @Option(names = {"--all"}, description = "List all actions from all types")
        private boolean showAll;

        @Option(names = {"--limit"}, description = "Maximum number of results")
        private Integer limit;

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();
            
            // If no type specified and --all not set, show help
            if (actionType == null && !showAll) {
                System.out.println("Please specify an action type or use --all flag.\n");
                System.out.println("Available types:");
                System.out.println("  Browser   - Web browser automation (Playwright)");
                System.out.println("  API       - REST/SOAP web service actions");
                System.out.println("  Database  - Database operations");
                System.out.println("  Kafka     - Message queue actions");
                System.out.println("  Mobile    - Mobile app automation");
                System.out.println("  General   - Utility actions");
                System.out.println("\nExamples:");
                System.out.println("  ingenious action list Browser");
                System.out.println("  ingenious action list API");
                System.out.println("  ingenious action list --all");
                return 0;
            }
            
            try {
                List<ActionInfo> actions = discoverAllActions();
                
                // Filter by type if specified
                if (actionType != null && !showAll) {
                    String normalizedType = normalizeType(actionType);
                    if (normalizedType == null) {
                        cli.printError("Unknown action type: " + actionType);
                        System.out.println("\nValid types: Browser, API, Database, Kafka, Mobile, General");
                        return 1;
                    }
                    actions.removeIf(a -> !a.category.equalsIgnoreCase(normalizedType));
                }
                
                // Sort by category, then name
                actions.sort(Comparator.comparing((ActionInfo a) -> a.category).thenComparing(a -> a.name));
                
                // Apply limit
                if (limit != null && limit > 0 && actions.size() > limit) {
                    actions = actions.subList(0, limit);
                }

                if (actions.isEmpty()) {
                    if (actionType != null) {
                        cli.printWarning("No actions found for type: " + actionType);
                    } else {
                        cli.printWarning("No actions found.");
                    }
                    return 0;
                }

                // Format output
                List<String> headers = Arrays.asList("Name", "Category", "Object Type", "Description");
                List<List<String>> rows = new ArrayList<>();

                for (ActionInfo action : actions) {
                    rows.add(Arrays.asList(
                        action.name,
                        action.category,
                        action.objectType,
                        truncate(action.description, 50)
                    ));
                }

                System.out.println(cli.getOutputFormatter().formatTable(headers, rows));
                
                String typeInfo = (actionType != null && !showAll) ? " (" + normalizeType(actionType) + ")" : "";
                cli.printInfo("\nTotal: " + actions.size() + " actions" + typeInfo);
                
                return 0;
                
            } catch (Exception e) {
                cli.printError("Failed to list actions: " + e.getMessage());
                e.printStackTrace();
                return 1;
            }
        }
        
        private String normalizeType(String type) {
            if (type == null) return null;
            String lower = type.toLowerCase();
            switch (lower) {
                case "browser":
                case "web":
                case "playwright":
                    return "Browser";
                case "api":
                case "webservice":
                case "rest":
                case "soap":
                    return "API";
                case "database":
                case "db":
                    return "Database";
                case "kafka":
                case "queue":
                case "mq":
                    return "Kafka";
                case "mobile":
                case "app":
                    return "Mobile";
                case "general":
                case "common":
                case "utility":
                    return "General";
                default:
                    return null;
            }
        }
    }

    /**
     * Search for actions by name or description.
     */
    @Command(name = "search", description = "Search for actions by name or description")
    public static class SearchCommand implements Callable<Integer> {

        @ParentCommand
        private ActionCommand parent;

        @Parameters(index = "0", description = "Search query")
        private String query;

        @Option(names = {"--limit"}, description = "Maximum number of results", defaultValue = "20")
        private int limit;

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();
            
            try {
                List<ActionInfo> actions = discoverAllActions();
                String lowerQuery = query.toLowerCase();
                
                // Score and filter actions
                List<Map.Entry<ActionInfo, Integer>> scored = new ArrayList<>();
                
                for (ActionInfo action : actions) {
                    int score = 0;
                    
                    // Exact name match
                    if (action.name.equalsIgnoreCase(query)) {
                        score += 100;
                    }
                    // Name contains
                    else if (action.name.toLowerCase().contains(lowerQuery)) {
                        score += 50;
                    }
                    // Description contains
                    if (action.description.toLowerCase().contains(lowerQuery)) {
                        score += 20;
                    }
                    // Category match
                    if (action.category.toLowerCase().contains(lowerQuery)) {
                        score += 10;
                    }
                    
                    if (score > 0) {
                        scored.add(new AbstractMap.SimpleEntry<>(action, score));
                    }
                }
                
                // Sort by score descending
                scored.sort((a, b) -> b.getValue().compareTo(a.getValue()));
                
                // Apply limit
                if (scored.size() > limit) {
                    scored = scored.subList(0, limit);
                }

                if (scored.isEmpty()) {
                    cli.printWarning("No actions found matching: " + query);
                    return 0;
                }

                // Format output
                List<String> headers = Arrays.asList("Name", "Category", "Description", "Match");
                List<List<String>> rows = new ArrayList<>();

                for (Map.Entry<ActionInfo, Integer> entry : scored) {
                    ActionInfo action = entry.getKey();
                    rows.add(Arrays.asList(
                        action.name,
                        action.category,
                        truncate(action.description, 40),
                        entry.getValue() + "%"
                    ));
                }

                System.out.println(cli.getOutputFormatter().formatTable(headers, rows));
                cli.printInfo("\nFound " + scored.size() + " matching actions");
                
                return 0;
                
            } catch (Exception e) {
                cli.printError("Search failed: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Show detailed information about an action.
     */
    @Command(name = "info", description = "Show detailed action information")
    public static class InfoCommand implements Callable<Integer> {

        @ParentCommand
        private ActionCommand parent;

        @Parameters(index = "0", description = "Action name")
        private String actionName;

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();
            
            try {
                List<ActionInfo> actions = discoverAllActions();
                
                // Find matching action
                ActionInfo found = actions.stream()
                        .filter(a -> a.name.equalsIgnoreCase(actionName))
                        .findFirst()
                        .orElse(null);

                if (found == null) {
                    cli.printError("Action not found: " + actionName);
                    
                    // Suggest similar actions
                    List<String> suggestions = actions.stream()
                            .filter(a -> a.name.toLowerCase().contains(actionName.toLowerCase().substring(0, Math.min(4, actionName.length()))))
                            .map(a -> a.name)
                            .limit(5)
                            .toList();
                    
                    if (!suggestions.isEmpty()) {
                        cli.printInfo("Did you mean: " + String.join(", ", suggestions));
                    }
                    
                    return 1;
                }

                // Display action details
                Map<String, Object> info = new LinkedHashMap<>();
                info.put("name", found.name);
                info.put("category", found.category);
                info.put("objectType", found.objectType);
                info.put("description", found.description);
                info.put("inputRequired", found.inputRequired);
                info.put("conditionSupported", found.conditionSupported);
                
                if (found.parameters != null && !found.parameters.isEmpty()) {
                    info.put("parameters", found.parameters);
                }
                
                if (found.example != null && !found.example.isEmpty()) {
                    info.put("example", found.example);
                }

                System.out.println(cli.getOutputFormatter().formatKeyValue(info));
                
                return 0;
                
            } catch (Exception e) {
                cli.printError("Failed to get action info: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * List action categories.
     */
    @Command(name = "categories", description = "List action categories with counts")
    public static class CategoriesCommand implements Callable<Integer> {

        @ParentCommand
        private ActionCommand parent;

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();
            
            try {
                List<ActionInfo> actions = discoverAllActions();
                
                // Count by category
                Map<String, Integer> categoryCounts = new LinkedHashMap<>();
                for (ActionInfo action : actions) {
                    categoryCounts.merge(action.category, 1, Integer::sum);
                }
                
                // Sort by count descending
                List<Map.Entry<String, Integer>> sorted = new ArrayList<>(categoryCounts.entrySet());
                sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));

                // Format output
                List<String> headers = Arrays.asList("Category", "Action Count");
                List<List<String>> rows = new ArrayList<>();

                for (Map.Entry<String, Integer> entry : sorted) {
                    rows.add(Arrays.asList(entry.getKey(), entry.getValue().toString()));
                }

                System.out.println(cli.getOutputFormatter().formatTable(headers, rows));
                cli.printInfo("\nTotal: " + actions.size() + " actions in " + categoryCounts.size() + " categories");
                
                return 0;
                
            } catch (Exception e) {
                cli.printError("Failed to list categories: " + e.getMessage());
                return 1;
            }
        }
    }

    // Helper methods

    private static String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    /**
     * Discover all @Action annotated methods from all command packages.
     */
    private static List<ActionInfo> discoverAllActions() {
        List<ActionInfo> actions = new ArrayList<>();
        
        // Known command classes - explicit list based on actual JAR contents
        String[] commandClasses = {
            // Browser commands
            "com.ing.engine.commands.browser.Basic",
            "com.ing.engine.commands.browser.Assertions",
            "com.ing.engine.commands.browser.CheckBox",
            "com.ing.engine.commands.browser.Command",
            "com.ing.engine.commands.browser.CommonMethods",
            "com.ing.engine.commands.browser.Cookies",
            "com.ing.engine.commands.browser.Dialogs",
            "com.ing.engine.commands.browser.DownloadFiles",
            "com.ing.engine.commands.browser.DragTo",
            "com.ing.engine.commands.browser.DynamicObject",
            "com.ing.engine.commands.browser.Focus",
            "com.ing.engine.commands.browser.General",
            "com.ing.engine.commands.browser.JSCommands",
            "com.ing.engine.commands.browser.Keys",
            "com.ing.engine.commands.browser.MouseClick",
            "com.ing.engine.commands.browser.Performance",
            "com.ing.engine.commands.browser.RequestFulfill",
            "com.ing.engine.commands.browser.Scroll",
            "com.ing.engine.commands.browser.SelectOptions",
            "com.ing.engine.commands.browser.StorageState",
            "com.ing.engine.commands.browser.Switch",
            "com.ing.engine.commands.browser.TextInput",
            "com.ing.engine.commands.browser.UploadFiles",
            "com.ing.engine.commands.browser.WaitFor",
            // Mobile commands
            "com.ing.engine.commands.mobile.AppiumDeviceCommands",
            "com.ing.engine.commands.mobile.AssertElement",
            "com.ing.engine.commands.mobile.Assertions",
            "com.ing.engine.commands.mobile.Basic",
            "com.ing.engine.commands.mobile.ByLabel",
            "com.ing.engine.commands.mobile.CheckBox",
            "com.ing.engine.commands.mobile.CommonMethods",
            "com.ing.engine.commands.mobile.DynamicObject",
            "com.ing.engine.commands.mobile.JSCommands",
            "com.ing.engine.commands.mobile.MobileGeneral",
            "com.ing.engine.commands.mobile.Performance",
            "com.ing.engine.commands.mobile.RelativeCommand",
            "com.ing.engine.commands.mobile.Scroll",
            "com.ing.engine.commands.mobile.SwitchTo",
            "com.ing.engine.commands.mobile.Table",
            "com.ing.engine.commands.mobile.WaitFor",
            "com.ing.engine.commands.mobile.WebButton",
            // Database commands
            "com.ing.engine.commands.database.Database",
            "com.ing.engine.commands.database.General",
            // Webservice commands
            "com.ing.engine.commands.webservice.Webservice",
            // Queue commands
            "com.ing.engine.commands.queue.QueueOperations",
            // General commands
            "com.ing.engine.commands.general.GeneralOperations",
            // File commands
            "com.ing.engine.commands.file.FileOperations",
            // String operations
            "com.ing.engine.commands.stringOperations.StringOperations",
            // Accessibility
            "com.ing.engine.commands.aXe.Accessibility",
            // Synthetic Data
            "com.ing.engine.commands.syntheticData.SyntheticDataGenerator",
            // Galen visual testing commands
            "com.ing.engine.commands.galenCommands.Align",
            "com.ing.engine.commands.galenCommands.Attribute",
            "com.ing.engine.commands.galenCommands.Centered",
            "com.ing.engine.commands.galenCommands.ColorScheme",
            "com.ing.engine.commands.galenCommands.Contains",
            "com.ing.engine.commands.galenCommands.CssProperties",
            "com.ing.engine.commands.galenCommands.Direction",
            "com.ing.engine.commands.galenCommands.General",
            "com.ing.engine.commands.galenCommands.Image",
            "com.ing.engine.commands.galenCommands.Inside",
            "com.ing.engine.commands.galenCommands.Near",
            "com.ing.engine.commands.galenCommands.On",
            "com.ing.engine.commands.galenCommands.PageDump",
            "com.ing.engine.commands.galenCommands.Report",
            "com.ing.engine.commands.galenCommands.Text",
            "com.ing.engine.commands.galenCommands.Title",
            "com.ing.engine.commands.galenCommands.Url",
            "com.ing.engine.commands.galenCommands.WidthAndHeight"
        };
        
        for (String className : commandClasses) {
            try {
                Class<?> clazz = Class.forName(className);
                for (Method method : clazz.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(Action.class)) {
                        Action annotation = method.getAnnotation(Action.class);
                        ObjectType objType = annotation.object();
                        String category = mapObjectTypeToCategory(objType);
                        
                        actions.add(new ActionInfo(
                            method.getName(),
                            category,
                            objType.name(),
                            annotation.desc(),
                            annotation.input().name(),
                            annotation.condition().name()
                        ));
                    }
                }
            } catch (ClassNotFoundException e) {
                // Class not available - skip
            } catch (NoClassDefFoundError e) {
                // Dependency not available - skip
            } catch (Exception e) {
                // Skip problematic classes
            }
        }
        
        // Remove duplicates based on name
        Map<String, ActionInfo> unique = new LinkedHashMap<>();
        for (ActionInfo action : actions) {
            unique.putIfAbsent(action.name, action);
        }
        
        return new ArrayList<>(unique.values());
    }

    /**
     * Map ObjectType enum to user-friendly category.
     */
    private static String mapObjectTypeToCategory(ObjectType objType) {
        String mapping = CATEGORY_MAPPING.get(objType.name());
        return mapping != null ? mapping : "General";
    }

    /**
     * Internal action info holder.
     */
    private static class ActionInfo {
        String name;
        String category;
        String objectType;
        String description;
        String inputRequired;
        String conditionSupported;
        String parameters;
        String example;

        ActionInfo(String name, String category, String objectType, String description, 
                   String inputRequired, String conditionSupported) {
            this.name = name;
            this.category = category;
            this.objectType = objectType;
            this.description = description;
            this.inputRequired = inputRequired;
            this.conditionSupported = conditionSupported;
        }
    }
}

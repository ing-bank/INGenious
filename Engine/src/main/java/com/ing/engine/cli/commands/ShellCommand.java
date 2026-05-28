package com.ing.engine.cli.commands;

import com.ing.engine.cli.INGeniousCLI;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * Interactive shell command for REPL-style interaction.
 * Uses JLine for enhanced command line features.
 */
@Command(
    name = "shell",
    aliases = {"interactive", "repl"},
    description = "Start interactive shell session"
)
public class ShellCommand implements Callable<Integer> {

    @ParentCommand
    private INGeniousCLI parent;

    @Option(names = {"-p", "--project"}, description = "Default project to work with")
    private String projectPath;

    @Option(names = {"--no-banner"}, description = "Don't show the welcome banner")
    private boolean noBanner;

    @Option(names = {"--history"}, description = "History file path")
    private String historyFile;

    private final Map<String, String> variables = new HashMap<>();
    private boolean running = true;

    @Override
    public Integer call() {
        INGeniousCLI cli = INGeniousCLI.getInstance();
        
        if (!noBanner) {
            printBanner();
        }

        if (projectPath != null) {
            variables.put("project", projectPath);
            cli.printInfo("Project: " + projectPath);
        }

        System.out.println("Type 'help' for commands, 'exit' to quit.\n");

        try {
            // Try to use JLine if available
            return runWithJLine();
        } catch (NoClassDefFoundError e) {
            // Fall back to basic console
            return runBasicShell();
        }
    }

    private int runWithJLine() {
        try {
            // Use reflection to load JLine to avoid hard dependency
            Class<?> terminalBuilderClass = Class.forName("org.jline.terminal.TerminalBuilder");
            Class<?> lineReaderBuilderClass = Class.forName("org.jline.reader.LineReaderBuilder");
            
            Object terminal = terminalBuilderClass.getMethod("terminal").invoke(null);
            Object lineReaderBuilder = lineReaderBuilderClass.getMethod("builder").invoke(null);
            lineReaderBuilder = lineReaderBuilder.getClass()
                .getMethod("terminal", Class.forName("org.jline.terminal.Terminal"))
                .invoke(lineReaderBuilder, terminal);
            
            // Set up completer
            Object completer = createCompleter();
            if (completer != null) {
                lineReaderBuilder = lineReaderBuilder.getClass()
                    .getMethod("completer", Class.forName("org.jline.reader.Completer"))
                    .invoke(lineReaderBuilder, completer);
            }
            
            Object reader = lineReaderBuilder.getClass().getMethod("build").invoke(lineReaderBuilder);
            
            // Read loop
            while (running) {
                try {
                    String line = (String) reader.getClass()
                        .getMethod("readLine", String.class)
                        .invoke(reader, getPrompt());
                    
                    if (line == null) break;
                    
                    processCommand(line.trim());
                    
                } catch (Exception e) {
                    if (e.getClass().getSimpleName().equals("EndOfFileException")) {
                        break;
                    }
                    if (e.getClass().getSimpleName().equals("UserInterruptException")) {
                        System.out.println("Use 'exit' to quit");
                        continue;
                    }
                    throw e;
                }
            }
            
            return 0;
            
        } catch (ClassNotFoundException e) {
            // JLine not available, fall back to basic
            return runBasicShell();
        } catch (Exception e) {
            INGeniousCLI.getInstance().printError("Shell error: " + e.getMessage());
            return 1;
        }
    }

    private int runBasicShell() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        
        while (running) {
            try {
                System.out.print(getPrompt());
                System.out.flush();
                
                String line = reader.readLine();
                if (line == null) break;
                
                processCommand(line.trim());
                
            } catch (IOException e) {
                INGeniousCLI.getInstance().printError("Read error: " + e.getMessage());
                break;
            }
        }
        
        return 0;
    }

    private Object createCompleter() {
        try {
            Class<?> stringCompleterClass = Class.forName("org.jline.reader.impl.completer.StringsCompleter");
            Class<?> aggregateCompleterClass = Class.forName("org.jline.reader.impl.completer.AggregateCompleter");
            
            // Create completers for commands and subcommands
            List<String> commands = Arrays.asList(
                "help", "exit", "quit", "clear",
                "project", "scenario", "testcase", "run", "action",
                "config", "report", "status", "history",
                "set", "get", "alias", "env"
            );
            
            Object stringCompleter = stringCompleterClass.getConstructor(Collection.class).newInstance(commands);
            
            return aggregateCompleterClass.getConstructor(Class.forName("org.jline.reader.Completer[]"))
                .newInstance(new Object[]{new Object[]{stringCompleter}});
            
        } catch (Exception e) {
            return null;
        }
    }

    private String getPrompt() {
        String projectName = variables.containsKey("project") 
            ? new File(variables.get("project")).getName() 
            : "~";
        return "ingenious:" + projectName + "> ";
    }

    private void processCommand(String input) {
        if (input.isEmpty()) return;
        
        INGeniousCLI cli = INGeniousCLI.getInstance();
        
        // Expand variables
        input = expandVariables(input);
        
        // Parse command
        String[] parts = input.split("\\s+", 2);
        String command = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1] : "";

        switch (command) {
            case "exit":
            case "quit":
            case "q":
                running = false;
                System.out.println("Goodbye!");
                break;

            case "help":
            case "?":
                printHelp(args);
                break;

            case "clear":
            case "cls":
                clearScreen();
                break;

            case "set":
                handleSet(args);
                break;

            case "get":
                handleGet(args);
                break;

            case "env":
                printVariables();
                break;

            case "project":
                handleProjectCommand(args);
                break;

            case "scenario":
                handleScenarioCommand(args);
                break;

            case "testcase":
            case "tc":
                handleTestCaseCommand(args);
                break;

            case "run":
                handleRunCommand(args);
                break;

            case "action":
            case "actions":
                handleActionCommand(args);
                break;

            case "config":
                handleConfigCommand(args);
                break;

            case "report":
                handleReportCommand(args);
                break;

            case "status":
                printStatus();
                break;

            case "history":
                handleHistoryCommand(args);
                break;

            case "cd":
                handleCd(args);
                break;

            case "ls":
            case "list":
                handleList(args);
                break;

            default:
                // Try to execute as full CLI command
                executeCliCommand(input);
        }
    }

    private String expandVariables(String input) {
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            input = input.replace("$" + entry.getKey(), entry.getValue());
            input = input.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        return input;
    }

    private void handleSet(String args) {
        String[] parts = args.split("=", 2);
        if (parts.length == 2) {
            variables.put(parts[0].trim(), parts[1].trim());
            System.out.println(parts[0].trim() + " = " + parts[1].trim());
        } else if (!args.isEmpty()) {
            System.out.println("Usage: set <name>=<value>");
        } else {
            printVariables();
        }
    }

    private void handleGet(String args) {
        if (args.isEmpty()) {
            printVariables();
        } else {
            String value = variables.get(args);
            if (value != null) {
                System.out.println(args + " = " + value);
            } else {
                System.out.println(args + " is not set");
            }
        }
    }

    private void printVariables() {
        if (variables.isEmpty()) {
            System.out.println("No variables set.");
        } else {
            System.out.println("Variables:");
            variables.forEach((k, v) -> System.out.println("  " + k + " = " + v));
        }
    }

    private void handleProjectCommand(String args) {
        if (args.isEmpty()) {
            // Show current project
            if (variables.containsKey("project")) {
                System.out.println("Current project: " + variables.get("project"));
            } else {
                System.out.println("No project selected. Use 'project <path>' to set one.");
            }
        } else if (args.equals("list")) {
            executeCliCommand("project list");
        } else if (args.equals("info")) {
            String project = variables.get("project");
            if (project != null) {
                executeCliCommand("project info --project " + project);
            } else {
                System.out.println("No project selected.");
            }
        } else {
            // Set project
            variables.put("project", args);
            System.out.println("Project set to: " + args);
        }
    }

    private void handleScenarioCommand(String args) {
        String project = variables.get("project");
        String prefix = project != null ? "--project " + project + " " : "";
        
        if (args.isEmpty() || args.equals("list")) {
            executeCliCommand("scenario list " + prefix);
        } else if (args.startsWith("info ")) {
            executeCliCommand("scenario info " + prefix + args.substring(5));
        } else {
            System.out.println("Usage: scenario [list|info <name>]");
        }
    }

    private void handleTestCaseCommand(String args) {
        String project = variables.get("project");
        String prefix = project != null ? "--project " + project + " " : "";
        
        if (args.isEmpty() || args.equals("list")) {
            executeCliCommand("testcase list " + prefix);
        } else if (args.startsWith("show ")) {
            executeCliCommand("testcase show " + prefix + args.substring(5));
        } else if (args.startsWith("create ")) {
            executeCliCommand("testcase create " + prefix + args.substring(7));
        } else {
            System.out.println("Usage: testcase [list|show <Scenario/TestCase>|create <Scenario/TestCase>]");
        }
    }

    private void handleRunCommand(String args) {
        String project = variables.get("project");
        String prefix = project != null ? "--project " + project + " " : "";
        
        if (args.isEmpty()) {
            System.out.println("Usage: run <Scenario/TestCase> [--browser Chrome]");
        } else {
            executeCliCommand("run testcase " + prefix + args);
        }
    }

    private void handleActionCommand(String args) {
        if (args.isEmpty() || args.equals("list")) {
            executeCliCommand("action list --limit 20");
        } else if (args.startsWith("search ")) {
            executeCliCommand("action search " + args.substring(7));
        } else if (args.startsWith("info ")) {
            executeCliCommand("action info " + args.substring(5));
        } else if (args.equals("categories")) {
            executeCliCommand("action categories");
        } else {
            System.out.println("Usage: action [list|search <query>|info <name>|categories]");
        }
    }

    private void handleConfigCommand(String args) {
        String project = variables.get("project");
        String prefix = project != null ? "--project " + project + " " : "";
        
        if (args.isEmpty() || args.equals("show")) {
            executeCliCommand("config show " + prefix);
        } else if (args.startsWith("get ")) {
            executeCliCommand("config get " + prefix + args.substring(4));
        } else if (args.startsWith("set ")) {
            executeCliCommand("config set " + prefix + args.substring(4));
        } else {
            System.out.println("Usage: config [show|get <key>|set <key> <value>]");
        }
    }

    private void handleReportCommand(String args) {
        String project = variables.get("project");
        String prefix = project != null ? "--project " + project + " " : "";
        
        if (args.isEmpty() || args.equals("latest")) {
            executeCliCommand("report latest " + prefix);
        } else if (args.equals("history")) {
            executeCliCommand("report history " + prefix);
        } else if (args.startsWith("show ")) {
            executeCliCommand("report show " + prefix + args.substring(5));
        } else {
            System.out.println("Usage: report [latest|history|show <runId>]");
        }
    }

    private void handleHistoryCommand(String args) {
        // Show command history (stub for now)
        System.out.println("Command history feature requires JLine3.");
        System.out.println("Use arrow keys to navigate history if JLine3 is available.");
    }

    private void handleCd(String args) {
        if (args.isEmpty()) {
            // Go to home
            variables.remove("project");
            System.out.println("Project unset.");
        } else if (args.equals("-")) {
            // Go to previous
            String prev = variables.get("previousProject");
            if (prev != null) {
                String current = variables.get("project");
                variables.put("project", prev);
                variables.put("previousProject", current);
                System.out.println("Project: " + prev);
            }
        } else {
            // Set project
            String current = variables.get("project");
            if (current != null) {
                variables.put("previousProject", current);
            }
            variables.put("project", args);
            System.out.println("Project: " + args);
        }
    }

    private void handleList(String args) {
        String project = variables.get("project");
        if (project == null) {
            executeCliCommand("project list");
        } else {
            executeCliCommand("scenario list --project " + project);
        }
    }

    private void printStatus() {
        System.out.println("Session Status:");
        System.out.println("  Project: " + variables.getOrDefault("project", "(none)"));
        System.out.println("  Variables: " + variables.size());
        System.out.println("  Working Dir: " + System.getProperty("user.dir"));
    }

    private void executeCliCommand(String command) {
        try {
            // Parse and execute via Picocli
            String[] args = command.trim().split("\\s+");
            int result = new CommandLine(new INGeniousCLI()).execute(args);
            
            if (result != 0) {
                System.out.println("Command returned: " + result);
            }
        } catch (Exception e) {
            INGeniousCLI.getInstance().printError("Command failed: " + e.getMessage());
        }
    }

    private void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private void printBanner() {
        // Color #7724FF (Purple/Violet) using 24-bit ANSI escape
        String purple = "\u001b[38;2;119;36;255m";
        String brightPurple = "\u001b[38;2;147;92;255m";
        String lightPurple = "\u001b[38;2;180;140;255m";
        String white = "\u001b[38;2;255;255;255m";
        String reset = "\u001b[0m";
        String bold = "\u001b[1m";
        
        System.out.println();
        System.out.println(purple + "    ██╗" + brightPurple + "███╗   ██╗" + purple + " ██████╗ " + brightPurple + "███████╗" + purple + "███╗   ██╗" + brightPurple + "██╗" + purple + " ██████╗ " + brightPurple + "██╗   ██╗" + purple + "███████╗" + reset);
        System.out.println(purple + "    ██║" + brightPurple + "████╗  ██║" + purple + "██╔════╝ " + brightPurple + "██╔════╝" + purple + "████╗  ██║" + brightPurple + "██║" + purple + "██╔═══██╗" + brightPurple + "██║   ██║" + purple + "██╔════╝" + reset);
        System.out.println(brightPurple + "    ██║" + lightPurple + "██╔██╗ ██║" + brightPurple + "██║  ███╗" + lightPurple + "█████╗  " + brightPurple + "██╔██╗ ██║" + lightPurple + "██║" + brightPurple + "██║   ██║" + lightPurple + "██║   ██║" + brightPurple + "███████╗" + reset);
        System.out.println(brightPurple + "    ██║" + purple + "██║╚██╗██║" + brightPurple + "██║   ██║" + purple + "██╔══╝  " + brightPurple + "██║╚██╗██║" + purple + "██║" + brightPurple + "██║   ██║" + purple + "██║   ██║" + brightPurple + "╚════██║" + reset);
        System.out.println(lightPurple + "    ██║" + brightPurple + "██║ ╚████║" + lightPurple + "╚██████╔╝" + brightPurple + "███████╗" + lightPurple + "██║ ╚████║" + brightPurple + "██║" + lightPurple + "╚██████╔╝" + brightPurple + "╚██████╔╝" + lightPurple + "███████║" + reset);
        System.out.println(purple + "    ╚═╝" + lightPurple + "╚═╝  ╚═══╝" + purple + " ╚═════╝ " + lightPurple + "╚══════╝" + purple + "╚═╝  ╚═══╝" + lightPurple + "╚═╝" + purple + " ╚═════╝ " + lightPurple + " ╚═════╝ " + purple + "╚══════╝" + reset);
        System.out.println();
        System.out.println(bold + purple + "    ╔══════════════════════════════════════════════════════════════════════╗" + reset);
        System.out.println(bold + purple + "    ║" + white + "       ✦  T E S T   A U T O M A T I O N   F R A M E W O R K  ✦        " + purple + "║" + reset);
        System.out.println(bold + purple + "    ║" + lightPurple + "                     Interactive Shell v2.3.1                       " + purple + "║" + reset);
        System.out.println(bold + purple + "    ╚══════════════════════════════════════════════════════════════════════╝" + reset);
        System.out.println();
    }

    private void printHelp(String topic) {
        if (topic.isEmpty()) {
            System.out.println("INGenious Interactive Shell Commands:");
            System.out.println();
            System.out.println("Navigation:");
            System.out.println("  project [path]      Set or show current project");
            System.out.println("  cd <path>           Change project (cd - for previous)");
            System.out.println("  ls, list            List scenarios or projects");
            System.out.println();
            System.out.println("Test Management:");
            System.out.println("  scenario list       List scenarios");
            System.out.println("  testcase list       List test cases (tc for short)");
            System.out.println("  testcase show <path>  Show test case steps");
            System.out.println("  action search <q>   Search for actions");
            System.out.println();
            System.out.println("Execution:");
            System.out.println("  run <Scenario/TC>   Run a test case");
            System.out.println("  report latest       Show latest results");
            System.out.println("  report history      Show run history");
            System.out.println();
            System.out.println("Configuration:");
            System.out.println("  config show         Show configuration");
            System.out.println("  config get <key>    Get config value");
            System.out.println("  config set <k> <v>  Set config value");
            System.out.println();
            System.out.println("Shell:");
            System.out.println("  set <name>=<val>    Set variable");
            System.out.println("  get <name>          Get variable");
            System.out.println("  env                 Show variables");
            System.out.println("  status              Show session status");
            System.out.println("  clear               Clear screen");
            System.out.println("  exit                Exit shell");
            System.out.println();
            System.out.println("Use 'help <command>' for more details.");
        } else {
            // Topic-specific help
            switch (topic.toLowerCase()) {
                case "run":
                    System.out.println("run - Execute test cases");
                    System.out.println();
                    System.out.println("Usage:");
                    System.out.println("  run <Scenario/TestCase>           Run a specific test");
                    System.out.println("  run <Scenario/TestCase> --headless  Run headless");
                    System.out.println("  run <Scenario/TestCase> --browser Firefox");
                    System.out.println();
                    System.out.println("Variables:");
                    System.out.println("  $project will be used if set");
                    break;
                    
                case "action":
                case "actions":
                    System.out.println("action - Discover available test actions");
                    System.out.println();
                    System.out.println("Usage:");
                    System.out.println("  action list           List all actions");
                    System.out.println("  action search <q>     Search for actions");
                    System.out.println("  action info <name>    Get action details");
                    System.out.println("  action categories     List categories");
                    break;
                    
                case "testcase":
                case "tc":
                    System.out.println("testcase - Manage test cases");
                    System.out.println();
                    System.out.println("Usage:");
                    System.out.println("  testcase list         List all test cases");
                    System.out.println("  testcase show <path>  Show test case steps");
                    System.out.println("  testcase create <path>  Create new test case");
                    System.out.println();
                    System.out.println("Path format: Scenario/TestCase");
                    break;
                    
                default:
                    System.out.println("No help available for: " + topic);
            }
        }
    }
}

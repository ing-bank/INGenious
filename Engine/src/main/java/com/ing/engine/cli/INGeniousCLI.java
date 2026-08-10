package com.ing.engine.cli;

import com.ing.engine.cli.commands.*;
import com.ing.engine.cli.output.OutputFormatter;
import com.ing.engine.cli.output.Style;
import com.ing.engine.constants.SystemDefaults;
import java.io.PrintWriter;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Option;

/**
 * INGenious CLI - The comprehensive command-line interface for INGenious Test Automation Platform.
 *
 * Supports subcommands for project management, test execution, reporting, and AI/Copilot integration.
 */
@Command(
    name = "ingenious",
    mixinStandardHelpOptions = false,
    versionProvider = INGeniousVersionProvider.class,
    description = "INGenious Test Automation Platform - Command Line Interface",
    subcommands = {
        HelpCommand.class,
        ProjectCommand.class,
        ScenarioCommand.class,
        TestCaseCommand.class,
        TestSetCommand.class,
        ORCommand.class,
        ObjectCommand.class,
        DataCommand.class,
        ActionCommand.class,
        RunCommand.class,
        ReportCommand.class,
        ConfigCommand.class,
        UpgradeCommand.class,
        ServerCommand.class,
        ShellCommand.class,
        LegacyCommand.class
    },
    synopsisHeading = "%nUsage: ",
    descriptionHeading = "%nDescription:%n",
    parameterListHeading = "%nParameters:%n",
    optionListHeading = "%nOptions:%n",
    commandListHeading = "%nCommands:%n"
)
public class INGeniousCLI implements Callable<Integer> {
    @Option(
        names = { "-h", "--help" },
        usageHelp = true,
        description = "Show this help message and exit"
    )
    private boolean helpRequested;

    @Option(
        names = { "-v", "-V", "--version" },
        versionHelp = true,
        description = "Print version information and exit"
    )
    private boolean versionRequested;

    @Option(names = { "--json" }, description = "Output in JSON format")
    private boolean jsonOutput;

    @Option(names = { "--yaml" }, description = "Output in YAML format")
    private boolean yamlOutput;

    @Option(names = { "-q", "--quiet" }, description = "Minimal output")
    private boolean quiet;

    @Option(names = { "--no-color" }, description = "Disable colored output")
    private boolean noColor;

    @Option(names = { "--project", "-p" }, description = "Project path (used globally)")
    private String projectPath;

    // Shared state for subcommands
    private static INGeniousCLI instance;
    private OutputFormatter outputFormatter;
    private Style style;

    public INGeniousCLI() {
        instance = this;
    }

    public static INGeniousCLI getInstance() {
        return instance;
    }

    /** Lazy-built shared Style, honouring --no-color. */
    public Style style() {
        if (style == null) {
            style = new Style(!noColor);
        }
        return style;
    }

    public boolean isJsonOutput() {
        return jsonOutput;
    }

    public boolean isYamlOutput() {
        return yamlOutput;
    }

    public boolean isQuiet() {
        return quiet;
    }

    public boolean isNoColor() {
        return noColor;
    }

    public String getProjectPath() {
        return projectPath;
    }

    public void setProjectPath(String path) {
        this.projectPath = path;
    }

    public OutputFormatter getOutputFormatter() {
        if (outputFormatter == null) {
            if (jsonOutput) {
                outputFormatter = OutputFormatter.json();
            } else if (yamlOutput) {
                outputFormatter = OutputFormatter.yaml();
            } else {
                outputFormatter = OutputFormatter.table(!noColor);
            }
        }
        return outputFormatter;
    }

    @Override
    public Integer call() {
        // When called without subcommand, show banner and help
        printBanner();
        CommandLine.usage(this, System.out);
        return 0;
    }

    /**
     * Print the INGenious ASCII art banner with #7724FF color.
     */
    private void printBanner() {
        if (quiet) return;

        // Color #7724FF (Purple/Violet) using 24-bit ANSI escape
        String p = noColor ? "" : "\u001b[38;2;119;36;255m"; // purple #7724FF
        String b = noColor ? "" : "\u001b[38;2;147;92;255m"; // bright purple
        String l = noColor ? "" : "\u001b[38;2;180;140;255m"; // light purple
        String w = noColor ? "" : "\u001b[38;2;255;255;255m"; // white
        String r = noColor ? "" : "\u001b[0m"; // reset
        String bo = noColor ? "" : "\u001b[1m"; // bold

        System.out.println();
        System.out.println(
            p +
            "    ██╗" +
            b +
            "███╗   ██╗" +
            p +
            " ██████╗ " +
            b +
            "███████╗" +
            p +
            "███╗   ██╗" +
            b +
            "██╗" +
            p +
            " ██████╗ " +
            b +
            "██╗   ██╗" +
            p +
            "███████╗" +
            r
        );
        System.out.println(
            p +
            "    ██║" +
            b +
            "████╗  ██║" +
            p +
            "██╔════╝ " +
            b +
            "██╔════╝" +
            p +
            "████╗  ██║" +
            b +
            "██║" +
            p +
            "██╔═══██╗" +
            b +
            "██║   ██║" +
            p +
            "██╔════╝" +
            r
        );
        System.out.println(
            b +
            "    ██║" +
            l +
            "██╔██╗ ██║" +
            b +
            "██║  ███╗" +
            l +
            "█████╗  " +
            b +
            "██╔██╗ ██║" +
            l +
            "██║" +
            b +
            "██║   ██║" +
            l +
            "██║   ██║" +
            b +
            "███████╗" +
            r
        );
        System.out.println(
            b +
            "    ██║" +
            p +
            "██║╚██╗██║" +
            b +
            "██║   ██║" +
            p +
            "██╔══╝  " +
            b +
            "██║╚██╗██║" +
            p +
            "██║" +
            b +
            "██║   ██║" +
            p +
            "██║   ██║" +
            b +
            "╚════██║" +
            r
        );
        System.out.println(
            l +
            "    ██║" +
            b +
            "██║ ╚████║" +
            l +
            "╚██████╔╝" +
            b +
            "███████╗" +
            l +
            "██║ ╚████║" +
            b +
            "██║" +
            l +
            "╚██████╔╝" +
            b +
            "╚██████╔╝" +
            l +
            "███████║" +
            r
        );
        System.out.println(
            p +
            "    ╚═╝" +
            l +
            "╚═╝  ╚═══╝" +
            p +
            " ╚═════╝ " +
            l +
            "╚══════╝" +
            p +
            "╚═╝  ╚═══╝" +
            l +
            "╚═╝" +
            p +
            " ╚═════╝ " +
            l +
            " ╚═════╝ " +
            p +
            "╚══════╝" +
            r
        );
        System.out.println();
        System.out.println(
            bo + l + "              ═══════════════════════════════════════════════════════════" + r
        );
        System.out.println(
            bo + w + "               ✦  T E S T   A U T O M A T I O N   F R A M E W O R K  ✦" + r
        );
        System.out.println(
            bo + b + "                              Version " + SystemDefaults.getBuildVersion() + r
        );
        System.out.println(
            bo + l + "              ═══════════════════════════════════════════════════════════" + r
        );
        System.out.println();
    }

    /**
     * Main entry point for the CLI.
     */
    public static int execute(String[] args) {
        INGeniousCLI cli = new INGeniousCLI();
        CommandLine cmd = new CommandLine(cli)
            .setOut(new PrintWriter(System.out, true))
            .setErr(new PrintWriter(System.err, true))
            .setColorScheme(CommandLine.Help.defaultColorScheme(CommandLine.Help.Ansi.AUTO));

        // Configure for better error handling
        cmd.setExecutionExceptionHandler(
            (ex, commandLine, parseResult) -> {
                commandLine
                    .getErr()
                    .println(commandLine.getColorScheme().errorText("Error: " + ex.getMessage()));
                if (System.getProperty("ingenious.debug") != null) {
                    ex.printStackTrace(commandLine.getErr());
                }
                return 1;
            }
        );

        return cmd.execute(args);
    }

    /**
     * Alias for execute() - called from Control.java.
     */
    public static int run(String[] args) {
        return execute(args);
    }

    /**
     * Check if legacy CLI arguments are being used.
     */
    public static boolean isLegacyArgs(String[] args) {
        if (args == null || args.length == 0) {
            return false;
        }
        // Legacy args start with single dash and are from the old CLI
        String firstArg = args[0];
        return (
            firstArg.startsWith("-") &&
            !firstArg.startsWith("--") &&
            (
                firstArg.equals("-run") ||
                firstArg.equals("-rerun") ||
                firstArg.equals("-help") ||
                firstArg.equals("-project_location") ||
                firstArg.equals("-latest_exe_status")
            )
        );
    }

    /**
     * Print a success message with optional color.
     */
    public void printSuccess(String message) {
        if (!quiet) {
            System.out.println(style().success(Style.ICON_OK) + " " + message);
        }
    }

    /**
     * Print an error message with optional color.
     */
    public void printError(String message) {
        System.err.println(style().failure(Style.ICON_ERR) + " " + style().red(message));
    }

    /**
     * Print a warning message with optional color.
     */
    public void printWarning(String message) {
        if (!quiet) {
            System.out.println(style().warning(Style.ICON_WARN) + " " + style().yellow(message));
        }
    }

    /**
     * Print an info message.
     */
    public void printInfo(String message) {
        if (!quiet) {
            System.out.println(style().cyan(Style.ICON_INFO) + " " + message);
        }
    }

    /**
     * Print a section header (blank line + bold cyan + dim underline).
     * The underline is capped at 60 chars so long titles don't blow up
     * vertically on narrow terminals.
     */
    public void printHeader(String title) {
        if (quiet) return;
        int rule = Math.min(60, Math.max(8, title.length()));
        System.out.println();
        System.out.println(style().header(title));
        System.out.println(style().dim("─".repeat(rule)));
    }

    /**
     * Print a compact callout box that highlights one fact (e.g. the
     * auto-detected target of {@code ingenious run …}).
     */
    public void printCallout(String label, String value) {
        if (quiet) {
            System.out.println(label + ": " + value);
            return;
        }
        Style s = style();
        String arrow = s.cyan(Style.ICON_ARROW);
        System.out.println();
        System.out.println("  " + arrow + " " + s.bold(label) + "  " + s.header(value));
    }
}

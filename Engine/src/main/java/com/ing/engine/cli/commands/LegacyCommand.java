package com.ing.engine.cli.commands;

import com.ing.engine.cli.INGeniousCLI;
import com.ing.engine.cli.LookUp;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Unmatched;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Legacy command wrapper for backward compatibility.
 * Routes legacy CLI arguments (-run, -project_location, etc.) to the original LookUp class.
 */
@Command(
    name = "legacy",
    hidden = true,
    description = "Execute using legacy CLI arguments (for backward compatibility)"
)
public class LegacyCommand implements Callable<Integer> {

    @ParentCommand
    private INGeniousCLI parent;

    @Option(names = {"-run"}, description = "Execute tests")
    private boolean run;

    @Option(names = {"-rerun"}, description = "Rerun failed tests")
    private String rerunPath;

    @Option(names = {"-project_location"}, description = "Project location")
    private String projectLocation;

    @Option(names = {"-scenario"}, description = "Scenario name")
    private String scenario;

    @Option(names = {"-testcase"}, description = "Test case name")
    private String testcase;

    @Option(names = {"-release"}, description = "Release name")
    private String release;

    @Option(names = {"-testset"}, description = "Test set name")
    private String testset;

    @Option(names = {"-browser"}, description = "Browser name")
    private String browser;

    @Option(names = {"-setThreads"}, description = "Number of threads")
    private String threads;

    @Option(names = {"-tags"}, description = "Test tags")
    private String tags;

    @Option(names = {"-env"}, description = "Environment")
    private String environment;

    @Option(names = {"-latest_exe_status"}, description = "Get latest execution status")
    private boolean latestStatus;

    @Unmatched
    private List<String> unmatchedArgs;

    @Override
    public Integer call() {
        // Reconstruct legacy arguments
        List<String> legacyArgs = new ArrayList<>();

        if (run) {
            legacyArgs.add("-run");
        }

        if (rerunPath != null) {
            legacyArgs.add("-rerun");
            legacyArgs.add(rerunPath);
        }

        if (projectLocation != null) {
            legacyArgs.add("-project_location");
            legacyArgs.add(projectLocation);
        }

        if (scenario != null) {
            legacyArgs.add("-scenario");
            legacyArgs.add(scenario);
        }

        if (testcase != null) {
            legacyArgs.add("-testcase");
            legacyArgs.add(testcase);
        }

        if (release != null) {
            legacyArgs.add("-release");
            legacyArgs.add(release);
        }

        if (testset != null) {
            legacyArgs.add("-testset");
            legacyArgs.add(testset);
        }

        if (browser != null) {
            legacyArgs.add("-browser");
            legacyArgs.add(browser);
        }

        if (threads != null) {
            legacyArgs.add("-setThreads");
            legacyArgs.add(threads);
        }

        if (tags != null) {
            legacyArgs.add("-tags");
            legacyArgs.add(tags);
        }

        if (environment != null) {
            legacyArgs.add("-env");
            legacyArgs.add(environment);
        }

        if (latestStatus) {
            legacyArgs.add("-latest_exe_status");
        }

        // Add any unmatched arguments
        if (unmatchedArgs != null) {
            legacyArgs.addAll(unmatchedArgs);
        }

        // Execute via legacy LookUp
        try {
            LookUp.exe(legacyArgs.toArray(new String[0]));
            return 0;
        } catch (Exception e) {
            INGeniousCLI.getInstance().printError("Legacy execution failed: " + e.getMessage());
            return 1;
        }
    }

    /**
     * Convert new-style arguments to legacy format.
     * This helps bridge the gap between new and old CLI.
     */
    public static String[] convertToLegacyArgs(String[] newArgs) {
        List<String> legacyArgs = new ArrayList<>();
        
        for (int i = 0; i < newArgs.length; i++) {
            String arg = newArgs[i];
            
            switch (arg) {
                case "--project":
                case "-p":
                    legacyArgs.add("-project_location");
                    if (i + 1 < newArgs.length) {
                        legacyArgs.add(newArgs[++i]);
                    }
                    break;
                    
                case "--scenario":
                case "-s":
                    legacyArgs.add("-scenario");
                    if (i + 1 < newArgs.length) {
                        legacyArgs.add(newArgs[++i]);
                    }
                    break;
                    
                case "--testcase":
                case "-t":
                    legacyArgs.add("-testcase");
                    if (i + 1 < newArgs.length) {
                        legacyArgs.add(newArgs[++i]);
                    }
                    break;
                    
                case "--browser":
                case "-b":
                    legacyArgs.add("-browser");
                    if (i + 1 < newArgs.length) {
                        legacyArgs.add(newArgs[++i]);
                    }
                    break;
                    
                case "--parallel":
                    legacyArgs.add("-setThreads");
                    if (i + 1 < newArgs.length) {
                        legacyArgs.add(newArgs[++i]);
                    }
                    break;
                    
                case "--headless":
                    legacyArgs.add("-op_setHeadless");
                    legacyArgs.add("true");
                    break;
                    
                default:
                    // Pass through as-is
                    legacyArgs.add(arg);
            }
        }
        
        return legacyArgs.toArray(new String[0]);
    }
}

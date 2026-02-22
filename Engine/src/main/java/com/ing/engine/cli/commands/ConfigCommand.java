package com.ing.engine.cli.commands;

import com.ing.engine.cli.INGeniousCLI;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * Configuration management commands.
 */
@Command(
    name = "config",
    description = "Configuration management",
    subcommands = {
        ConfigCommand.ShowCommand.class,
        ConfigCommand.GetCommand.class,
        ConfigCommand.SetCommand.class,
        ConfigCommand.DriversCommand.class,
        ConfigCommand.ResetCommand.class
    }
)
public class ConfigCommand implements Callable<Integer> {

    @ParentCommand
    private INGeniousCLI parent;

    @Override
    public Integer call() {
        System.out.println("Use 'ingenious config <subcommand>' - see 'ingenious config --help'");
        return 0;
    }

    /**
     * Show all configuration.
     */
    @Command(name = "show", description = "Show all configuration settings")
    public static class ShowCommand implements Callable<Integer> {

        @ParentCommand
        private ConfigCommand parent;

        @Option(names = {"-p", "--project"}, description = "Project path")
        private String projectPath;

        @Option(names = {"--global"}, description = "Show global configuration")
        private boolean global;

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();
            
            try {
                if (global) {
                    // Show global configuration
                    File globalConfig = new File(System.getProperty("user.home"), ".ingenious/config.properties");
                    if (globalConfig.exists()) {
                        Properties props = loadProperties(globalConfig);
                        System.out.println("Global Configuration:");
                        System.out.println(cli.getOutputFormatter().formatKeyValue(propsToMap(props)));
                    } else {
                        cli.printWarning("No global configuration found.");
                    }
                    return 0;
                }

                String path = projectPath != null ? projectPath : cli.getProjectPath();
                if (path == null || path.isEmpty()) {
                    cli.printError("Project path required. Use --project or --global");
                    return 1;
                }

                // Show project configuration
                File projectConfig = new File(path, "Configuration/Global Settings.properties");
                if (!projectConfig.exists()) {
                    projectConfig = new File(path, "Configuration/project.properties");
                }

                if (!projectConfig.exists()) {
                    cli.printWarning("No project configuration found.");
                    return 0;
                }

                Properties props = loadProperties(projectConfig);
                System.out.println("Project Configuration (" + projectConfig.getName() + "):");
                System.out.println(cli.getOutputFormatter().formatKeyValue(propsToMap(props)));
                
                return 0;
                
            } catch (Exception e) {
                cli.printError("Failed to show configuration: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Get a specific configuration value.
     */
    @Command(name = "get", description = "Get a configuration value")
    public static class GetCommand implements Callable<Integer> {

        @ParentCommand
        private ConfigCommand parent;

        @Parameters(index = "0", description = "Configuration key")
        private String key;

        @Option(names = {"-p", "--project"}, description = "Project path")
        private String projectPath;

        @Option(names = {"--global"}, description = "Get from global configuration")
        private boolean global;

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();
            
            try {
                File configFile;
                
                if (global) {
                    configFile = new File(System.getProperty("user.home"), ".ingenious/config.properties");
                } else {
                    String path = projectPath != null ? projectPath : cli.getProjectPath();
                    if (path == null || path.isEmpty()) {
                        cli.printError("Project path required.");
                        return 1;
                    }
                    configFile = new File(path, "Configuration/Global Settings.properties");
                }

                if (!configFile.exists()) {
                    cli.printError("Configuration file not found.");
                    return 1;
                }

                Properties props = loadProperties(configFile);
                String value = props.getProperty(key);
                
                if (value == null) {
                    cli.printWarning(key + " is not set");
                    return 0;
                }
                
                System.out.println(value);
                return 0;
                
            } catch (Exception e) {
                cli.printError("Failed to get configuration: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Set a configuration value.
     */
    @Command(name = "set", description = "Set a configuration value")
    public static class SetCommand implements Callable<Integer> {

        @ParentCommand
        private ConfigCommand parent;

        @Parameters(index = "0", description = "Configuration key")
        private String key;

        @Parameters(index = "1", description = "Configuration value")
        private String value;

        @Option(names = {"-p", "--project"}, description = "Project path")
        private String projectPath;

        @Option(names = {"--global"}, description = "Set in global configuration")
        private boolean global;

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();
            
            try {
                File configFile;
                
                if (global) {
                    File globalDir = new File(System.getProperty("user.home"), ".ingenious");
                    globalDir.mkdirs();
                    configFile = new File(globalDir, "config.properties");
                } else {
                    String path = projectPath != null ? projectPath : cli.getProjectPath();
                    if (path == null || path.isEmpty()) {
                        cli.printError("Project path required.");
                        return 1;
                    }
                    configFile = new File(path, "Configuration/Global Settings.properties");
                }

                Properties props = configFile.exists() ? loadProperties(configFile) : new Properties();
                String oldValue = props.getProperty(key);
                props.setProperty(key, value);
                
                try (FileOutputStream fos = new FileOutputStream(configFile)) {
                    props.store(fos, "INGenious Configuration");
                }
                
                if (oldValue != null) {
                    cli.printSuccess("Updated " + key + ": " + oldValue + " -> " + value);
                } else {
                    cli.printSuccess("Set " + key + " = " + value);
                }
                
                return 0;
                
            } catch (Exception e) {
                cli.printError("Failed to set configuration: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Manage browser drivers.
     */
    @Command(name = "drivers", description = "Manage browser drivers")
    public static class DriversCommand implements Callable<Integer> {

        @ParentCommand
        private ConfigCommand parent;

        @Option(names = {"--check"}, description = "Check driver versions")
        private boolean check;

        @Option(names = {"--update"}, description = "Update drivers")
        private boolean update;

        @Option(names = {"--browser"}, description = "Specific browser driver")
        private String browser;

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();
            
            if (check) {
                cli.printInfo("Checking browser driver versions...");
                
                List<String> headers = Arrays.asList("Browser", "Driver", "Version", "Status");
                List<List<String>> rows = new ArrayList<>();
                
                // Check Chrome driver
                rows.add(Arrays.asList("Chrome", "chromedriver", getDriverVersion("chromedriver"), checkDriverStatus("chromedriver")));
                rows.add(Arrays.asList("Firefox", "geckodriver", getDriverVersion("geckodriver"), checkDriverStatus("geckodriver")));
                rows.add(Arrays.asList("Edge", "msedgedriver", getDriverVersion("msedgedriver"), checkDriverStatus("msedgedriver")));
                rows.add(Arrays.asList("Safari", "safaridriver", getDriverVersion("safaridriver"), checkDriverStatus("safaridriver")));
                
                System.out.println(cli.getOutputFormatter().formatTable(headers, rows));
                return 0;
            }
            
            if (update) {
                cli.printInfo("WebDriverManager will automatically download drivers when needed.");
                cli.printInfo("To force update, run: mvn dependency:resolve -U");
                return 0;
            }
            
            // Default: show driver info
            cli.printInfo("Use --check to verify drivers, --update to update them.");
            return 0;
        }
        
        private String getDriverVersion(String driver) {
            try {
                ProcessBuilder pb = new ProcessBuilder(driver, "--version");
                pb.redirectErrorStream(true);
                Process p = pb.start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line = reader.readLine();
                p.waitFor();
                return line != null ? line.split(" ")[0] : "Unknown";
            } catch (Exception e) {
                return "Not found";
            }
        }
        
        private String checkDriverStatus(String driver) {
            try {
                ProcessBuilder pb = new ProcessBuilder("which", driver);
                Process p = pb.start();
                int exitCode = p.waitFor();
                return exitCode == 0 ? "OK" : "Missing";
            } catch (Exception e) {
                return "Unknown";
            }
        }
    }

    /**
     * Reset configuration to defaults.
     */
    @Command(name = "reset", description = "Reset configuration to defaults")
    public static class ResetCommand implements Callable<Integer> {

        @ParentCommand
        private ConfigCommand parent;

        @Option(names = {"-p", "--project"}, description = "Project path")
        private String projectPath;

        @Option(names = {"--global"}, description = "Reset global configuration")
        private boolean global;

        @Option(names = {"--confirm"}, description = "Confirm reset without prompting")
        private boolean confirm;

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();
            
            if (!confirm) {
                cli.printWarning("This will reset configuration to defaults.");
                cli.printInfo("Use --confirm to proceed.");
                return 0;
            }
            
            try {
                if (global) {
                    File globalConfig = new File(System.getProperty("user.home"), ".ingenious/config.properties");
                    if (globalConfig.exists()) {
                        globalConfig.delete();
                        cli.printSuccess("Global configuration reset.");
                    }
                    return 0;
                }

                String path = projectPath != null ? projectPath : cli.getProjectPath();
                if (path == null || path.isEmpty()) {
                    cli.printError("Project path required.");
                    return 1;
                }

                // Create default configuration
                File configFile = new File(path, "Configuration/Global Settings.properties");
                Properties defaults = new Properties();
                defaults.setProperty("browser", "Chrome");
                defaults.setProperty("timeout", "30");
                defaults.setProperty("headless", "false");
                defaults.setProperty("screenshots", "on-failure");
                defaults.setProperty("video", "false");
                
                try (FileOutputStream fos = new FileOutputStream(configFile)) {
                    defaults.store(fos, "INGenious Default Configuration");
                }
                
                cli.printSuccess("Configuration reset to defaults.");
                return 0;
                
            } catch (Exception e) {
                cli.printError("Reset failed: " + e.getMessage());
                return 1;
            }
        }
    }

    // Helper methods

    private static Properties loadProperties(File file) throws IOException {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(file)) {
            props.load(fis);
        }
        return props;
    }

    private static Map<String, Object> propsToMap(Properties props) {
        Map<String, Object> map = new LinkedHashMap<>();
        props.stringPropertyNames().stream()
             .sorted()
             .forEach(key -> map.put(key, props.getProperty(key)));
        return map;
    }
}

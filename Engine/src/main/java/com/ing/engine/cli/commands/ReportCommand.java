package com.ing.engine.cli.commands;

import com.ing.engine.cli.INGeniousCLI;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * Report management commands.
 */
@Command(
    name = "report",
    description = "Report management commands",
    subcommands = {
        ReportCommand.LatestCommand.class,
        ReportCommand.HistoryCommand.class,
        ReportCommand.ShowCommand.class,
        ReportCommand.ExportCommand.class,
        ReportCommand.CompareCommand.class
    }
)
public class ReportCommand implements Callable<Integer> {

    @ParentCommand
    private INGeniousCLI parent;

    @Override
    public Integer call() {
        System.out.println("Use 'ingenious report <subcommand>' - see 'ingenious report --help'");
        return 0;
    }

    /**
     * Show latest test results.
     */
    @Command(name = "latest", description = "Show latest test execution results")
    public static class LatestCommand implements Callable<Integer> {

        @ParentCommand
        private ReportCommand parent;

        @Option(names = {"-p", "--project"}, description = "Project path")
        private String projectPath;

        @Option(names = {"--summary"}, description = "Show only summary")
        private boolean summaryOnly;

        @Option(names = {"--open"}, description = "Open report in browser")
        private boolean openReport;

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();
            
            String path = projectPath != null ? projectPath : cli.getProjectPath();
            if (path == null || path.isEmpty()) {
                cli.printError("Project path required.");
                return 1;
            }

            try {
                File resultsDir = new File(path, "Results");
                if (!resultsDir.exists()) {
                    cli.printWarning("No results found.");
                    return 0;
                }

                // Find latest run
                File[] runs = resultsDir.listFiles(File::isDirectory);
                if (runs == null || runs.length == 0) {
                    cli.printWarning("No test runs found.");
                    return 0;
                }

                Arrays.sort(runs, Comparator.comparingLong(File::lastModified).reversed());
                File latestRun = runs[0];

                cli.printInfo("Latest Run: " + latestRun.getName());
                System.out.println();

                // Parse and display results
                Map<String, Object> summary = parseResultSummary(latestRun);
                System.out.println(cli.getOutputFormatter().formatKeyValue(summary));

                if (!summaryOnly) {
                    // Show test case details
                    List<TestResult> results = parseTestResults(latestRun);
                    if (!results.isEmpty()) {
                        System.out.println("\nTest Cases:");
                        List<String> headers = Arrays.asList("Scenario", "TestCase", "Status", "Duration");
                        List<List<String>> rows = new ArrayList<>();
                        
                        for (TestResult result : results) {
                            rows.add(Arrays.asList(
                                result.scenario,
                                result.testCase,
                                formatStatus(result.status),
                                result.duration + "ms"
                            ));
                        }
                        
                        System.out.println(cli.getOutputFormatter().formatTable(headers, rows));
                    }
                }

                if (openReport) {
                    File reportFile = new File(latestRun, "summary.html");
                    if (!reportFile.exists()) {
                        reportFile = new File(latestRun, "index.html");
                    }
                    if (reportFile.exists()) {
                        java.awt.Desktop.getDesktop().open(reportFile);
                        cli.printInfo("Opened report in browser.");
                    }
                }

                return 0;
                
            } catch (Exception e) {
                cli.printError("Failed to get results: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Show test execution history.
     */
    @Command(name = "history", description = "Show test execution history")
    public static class HistoryCommand implements Callable<Integer> {

        @ParentCommand
        private ReportCommand parent;

        @Option(names = {"-p", "--project"}, description = "Project path")
        private String projectPath;

        @Option(names = {"-n", "--last"}, description = "Number of runs to show", defaultValue = "10")
        private int lastN;

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();
            
            String path = projectPath != null ? projectPath : cli.getProjectPath();
            if (path == null || path.isEmpty()) {
                cli.printError("Project path required.");
                return 1;
            }

            try {
                File resultsDir = new File(path, "Results");
                if (!resultsDir.exists()) {
                    cli.printWarning("No results found.");
                    return 0;
                }

                File[] runs = resultsDir.listFiles(File::isDirectory);
                if (runs == null || runs.length == 0) {
                    cli.printWarning("No test runs found.");
                    return 0;
                }

                // Sort by last modified (newest first)
                Arrays.sort(runs, Comparator.comparingLong(File::lastModified).reversed());
                
                // Limit to lastN
                int count = Math.min(lastN, runs.length);

                List<String> headers = Arrays.asList("Run ID", "Date/Time", "Passed", "Failed", "Total", "Pass Rate");
                List<List<String>> rows = new ArrayList<>();
                
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

                for (int i = 0; i < count; i++) {
                    File run = runs[i];
                    Map<String, Object> summary = parseResultSummary(run);
                    
                    int passed = (int) summary.getOrDefault("passed", 0);
                    int failed = (int) summary.getOrDefault("failed", 0);
                    int total = passed + failed;
                    String passRate = total > 0 ? String.format("%.1f%%", (passed * 100.0 / total)) : "N/A";
                    
                    rows.add(Arrays.asList(
                        run.getName(),
                        sdf.format(new Date(run.lastModified())),
                        String.valueOf(passed),
                        String.valueOf(failed),
                        String.valueOf(total),
                        passRate
                    ));
                }

                System.out.println(cli.getOutputFormatter().formatTable(headers, rows));
                cli.printInfo("\nShowing " + count + " of " + runs.length + " runs");
                
                return 0;
                
            } catch (Exception e) {
                cli.printError("Failed to get history: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Show specific run details.
     */
    @Command(name = "show", description = "Show details of a specific run")
    public static class ShowCommand implements Callable<Integer> {

        @ParentCommand
        private ReportCommand parent;

        @Parameters(index = "0", description = "Run ID or folder name")
        private String runId;

        @Option(names = {"-p", "--project"}, description = "Project path")
        private String projectPath;

        @Option(names = {"--failed-only"}, description = "Show only failed tests")
        private boolean failedOnly;

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();
            
            String path = projectPath != null ? projectPath : cli.getProjectPath();
            if (path == null || path.isEmpty()) {
                cli.printError("Project path required.");
                return 1;
            }

            try {
                File resultsDir = new File(path, "Results");
                File runDir = new File(resultsDir, runId);
                
                if (!runDir.exists()) {
                    // Try to find partial match
                    File[] runs = resultsDir.listFiles(f -> f.isDirectory() && f.getName().contains(runId));
                    if (runs != null && runs.length > 0) {
                        runDir = runs[0];
                    } else {
                        cli.printError("Run not found: " + runId);
                        return 1;
                    }
                }

                cli.printInfo("Run: " + runDir.getName());
                System.out.println();

                Map<String, Object> summary = parseResultSummary(runDir);
                System.out.println(cli.getOutputFormatter().formatKeyValue(summary));

                // Show test details
                List<TestResult> results = parseTestResults(runDir);
                
                if (failedOnly) {
                    results.removeIf(r -> !"FAIL".equalsIgnoreCase(r.status));
                }
                
                if (!results.isEmpty()) {
                    System.out.println("\nTest Results:");
                    List<String> headers = Arrays.asList("Scenario", "TestCase", "Status", "Duration", "Error");
                    List<List<String>> rows = new ArrayList<>();
                    
                    for (TestResult result : results) {
                        rows.add(Arrays.asList(
                            result.scenario,
                            result.testCase,
                            formatStatus(result.status),
                            result.duration + "ms",
                            truncate(result.error, 40)
                        ));
                    }
                    
                    System.out.println(cli.getOutputFormatter().formatTable(headers, rows));
                }
                
                return 0;
                
            } catch (Exception e) {
                cli.printError("Failed to show run: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Export report in various formats.
     */
    @Command(name = "export", description = "Export report in various formats")
    public static class ExportCommand implements Callable<Integer> {

        @ParentCommand
        private ReportCommand parent;

        @Option(names = {"-p", "--project"}, description = "Project path")
        private String projectPath;

        @Option(names = {"--run-id"}, description = "Run ID to export (default: latest)")
        private String runId;

        @Option(names = {"--format"}, description = "Export format (json, csv, junit)", defaultValue = "json")
        private String format;

        @Option(names = {"-o", "--output"}, description = "Output file path")
        private String outputPath;

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();
            
            String path = projectPath != null ? projectPath : cli.getProjectPath();
            if (path == null || path.isEmpty()) {
                cli.printError("Project path required.");
                return 1;
            }

            try {
                File resultsDir = new File(path, "Results");
                File runDir;
                
                if (runId != null) {
                    runDir = new File(resultsDir, runId);
                } else {
                    File[] runs = resultsDir.listFiles(File::isDirectory);
                    if (runs == null || runs.length == 0) {
                        cli.printWarning("No runs found.");
                        return 0;
                    }
                    Arrays.sort(runs, Comparator.comparingLong(File::lastModified).reversed());
                    runDir = runs[0];
                }

                if (!runDir.exists()) {
                    cli.printError("Run not found: " + runId);
                    return 1;
                }

                List<TestResult> results = parseTestResults(runDir);
                Map<String, Object> summary = parseResultSummary(runDir);
                
                String output;
                String extension;
                
                switch (format.toLowerCase()) {
                    case "csv":
                        output = exportToCsv(results);
                        extension = ".csv";
                        break;
                    case "junit":
                        output = exportToJUnit(results, summary);
                        extension = ".xml";
                        break;
                    default:
                        output = exportToJson(results, summary);
                        extension = ".json";
                }

                if (outputPath != null) {
                    Files.writeString(Paths.get(outputPath), output);
                    cli.printSuccess("Exported to: " + outputPath);
                } else {
                    String defaultOutput = "report-" + runDir.getName() + extension;
                    Files.writeString(Paths.get(defaultOutput), output);
                    cli.printSuccess("Exported to: " + defaultOutput);
                }
                
                return 0;
                
            } catch (Exception e) {
                cli.printError("Export failed: " + e.getMessage());
                return 1;
            }
        }
        
        private String exportToJson(List<TestResult> results, Map<String, Object> summary) {
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            sb.append("  \"summary\": {\n");
            for (Map.Entry<String, Object> entry : summary.entrySet()) {
                sb.append("    \"").append(entry.getKey()).append("\": ");
                if (entry.getValue() instanceof Number) {
                    sb.append(entry.getValue());
                } else {
                    sb.append("\"").append(entry.getValue()).append("\"");
                }
                sb.append(",\n");
            }
            sb.append("  },\n");
            sb.append("  \"testCases\": [\n");
            for (int i = 0; i < results.size(); i++) {
                TestResult r = results.get(i);
                sb.append("    {");
                sb.append("\"scenario\":\"").append(r.scenario).append("\",");
                sb.append("\"testCase\":\"").append(r.testCase).append("\",");
                sb.append("\"status\":\"").append(r.status).append("\",");
                sb.append("\"duration\":").append(r.duration);
                if (r.error != null && !r.error.isEmpty()) {
                    sb.append(",\"error\":\"").append(r.error.replace("\"", "\\\"")).append("\"");
                }
                sb.append("}");
                if (i < results.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("  ]\n");
            sb.append("}");
            return sb.toString();
        }
        
        private String exportToCsv(List<TestResult> results) {
            StringBuilder sb = new StringBuilder();
            sb.append("Scenario,TestCase,Status,Duration,Error\n");
            for (TestResult r : results) {
                sb.append("\"").append(r.scenario).append("\",");
                sb.append("\"").append(r.testCase).append("\",");
                sb.append("\"").append(r.status).append("\",");
                sb.append(r.duration).append(",");
                sb.append("\"").append(r.error != null ? r.error.replace("\"", "\"\"") : "").append("\"\n");
            }
            return sb.toString();
        }
        
        private String exportToJUnit(List<TestResult> results, Map<String, Object> summary) {
            int passed = (int) summary.getOrDefault("passed", 0);
            int failed = (int) summary.getOrDefault("failed", 0);
            
            StringBuilder sb = new StringBuilder();
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            sb.append("<testsuite name=\"INGenious\" tests=\"").append(passed + failed).append("\" ");
            sb.append("failures=\"").append(failed).append("\" errors=\"0\">\n");
            
            for (TestResult r : results) {
                sb.append("  <testcase name=\"").append(r.testCase).append("\" ");
                sb.append("classname=\"").append(r.scenario).append("\" ");
                sb.append("time=\"").append(r.duration / 1000.0).append("\">\n");
                
                if ("FAIL".equalsIgnoreCase(r.status)) {
                    sb.append("    <failure message=\"Test failed\">");
                    if (r.error != null) {
                        sb.append("<![CDATA[").append(r.error).append("]]>");
                    }
                    sb.append("</failure>\n");
                }
                
                sb.append("  </testcase>\n");
            }
            
            sb.append("</testsuite>");
            return sb.toString();
        }
    }

    /**
     * Compare two test runs.
     */
    @Command(name = "compare", description = "Compare two test runs")
    public static class CompareCommand implements Callable<Integer> {

        @ParentCommand
        private ReportCommand parent;

        @Parameters(index = "0", description = "First run ID")
        private String run1;

        @Parameters(index = "1", description = "Second run ID")
        private String run2;

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
                File resultsDir = new File(path, "Results");
                File runDir1 = findRun(resultsDir, run1);
                File runDir2 = findRun(resultsDir, run2);

                if (runDir1 == null || runDir2 == null) {
                    cli.printError("One or both runs not found.");
                    return 1;
                }

                Map<String, Object> summary1 = parseResultSummary(runDir1);
                Map<String, Object> summary2 = parseResultSummary(runDir2);

                System.out.println("Comparison: " + runDir1.getName() + " vs " + runDir2.getName());
                System.out.println();

                List<String> headers = Arrays.asList("Metric", run1, run2, "Delta");
                List<List<String>> rows = new ArrayList<>();

                int passed1 = (int) summary1.getOrDefault("passed", 0);
                int passed2 = (int) summary2.getOrDefault("passed", 0);
                int failed1 = (int) summary1.getOrDefault("failed", 0);
                int failed2 = (int) summary2.getOrDefault("failed", 0);
                int total1 = passed1 + failed1;
                int total2 = passed2 + failed2;

                rows.add(Arrays.asList("Passed", String.valueOf(passed1), String.valueOf(passed2), formatDelta(passed2 - passed1)));
                rows.add(Arrays.asList("Failed", String.valueOf(failed1), String.valueOf(failed2), formatDelta(failed2 - failed1, true)));
                rows.add(Arrays.asList("Total", String.valueOf(total1), String.valueOf(total2), formatDelta(total2 - total1)));
                
                double rate1 = total1 > 0 ? (passed1 * 100.0 / total1) : 0;
                double rate2 = total2 > 0 ? (passed2 * 100.0 / total2) : 0;
                rows.add(Arrays.asList("Pass Rate", String.format("%.1f%%", rate1), String.format("%.1f%%", rate2), String.format("%+.1f%%", rate2 - rate1)));

                System.out.println(cli.getOutputFormatter().formatTable(headers, rows));
                
                return 0;
                
            } catch (Exception e) {
                cli.printError("Comparison failed: " + e.getMessage());
                return 1;
            }
        }
    }

    // Helper methods

    private static File findRun(File resultsDir, String runId) {
        File runDir = new File(resultsDir, runId);
        if (runDir.exists()) return runDir;
        
        File[] matches = resultsDir.listFiles(f -> f.isDirectory() && f.getName().contains(runId));
        return (matches != null && matches.length > 0) ? matches[0] : null;
    }

    private static Map<String, Object> parseResultSummary(File runDir) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("runId", runDir.getName());
        
        // Try to parse summary from JSON or HTML
        File summaryJson = new File(runDir, "summary.json");
        if (summaryJson.exists()) {
            try {
                String content = Files.readString(summaryJson.toPath());
                // Simple JSON parsing for summary values
                if (content.contains("\"passed\"")) {
                    String passed = content.replaceAll(".*\"passed\"\\s*:\\s*(\\d+).*", "$1");
                    summary.put("passed", Integer.parseInt(passed));
                }
                if (content.contains("\"failed\"")) {
                    String failed = content.replaceAll(".*\"failed\"\\s*:\\s*(\\d+).*", "$1");
                    summary.put("failed", Integer.parseInt(failed));
                }
            } catch (Exception e) {
                // Fall back to defaults
            }
        }
        
        // Count result files if no summary
        if (!summary.containsKey("passed")) {
            int passed = 0, failed = 0;
            File[] scenarioDirs = runDir.listFiles(File::isDirectory);
            if (scenarioDirs != null) {
                for (File scenario : scenarioDirs) {
                    File[] testCases = scenario.listFiles(File::isDirectory);
                    if (testCases != null) {
                        for (File tc : testCases) {
                            File statusFile = new File(tc, "status.txt");
                            if (statusFile.exists()) {
                                try {
                                    String status = Files.readString(statusFile.toPath()).trim();
                                    if ("PASS".equalsIgnoreCase(status)) passed++;
                                    else failed++;
                                } catch (Exception e) {
                                    failed++;
                                }
                            }
                        }
                    }
                }
            }
            summary.put("passed", passed);
            summary.put("failed", failed);
        }
        
        return summary;
    }

    private static List<TestResult> parseTestResults(File runDir) {
        List<TestResult> results = new ArrayList<>();
        
        File[] scenarioDirs = runDir.listFiles(File::isDirectory);
        if (scenarioDirs == null) return results;
        
        for (File scenario : scenarioDirs) {
            File[] testCases = scenario.listFiles(File::isDirectory);
            if (testCases == null) continue;
            
            for (File tc : testCases) {
                TestResult result = new TestResult();
                result.scenario = scenario.getName();
                result.testCase = tc.getName();
                result.status = "PASS";
                result.duration = 0;
                result.error = "";
                
                File statusFile = new File(tc, "status.txt");
                if (statusFile.exists()) {
                    try {
                        result.status = Files.readString(statusFile.toPath()).trim();
                    } catch (Exception e) {
                        result.status = "UNKNOWN";
                    }
                }
                
                File errorFile = new File(tc, "error.txt");
                if (errorFile.exists()) {
                    try {
                        result.error = Files.readString(errorFile.toPath()).trim();
                    } catch (Exception e) {
                        // Ignore
                    }
                }
                
                results.add(result);
            }
        }
        
        return results;
    }

    private static String formatStatus(String status) {
        if ("PASS".equalsIgnoreCase(status)) return "✓ PASS";
        if ("FAIL".equalsIgnoreCase(status)) return "✗ FAIL";
        return status;
    }

    private static String formatDelta(int delta) {
        return formatDelta(delta, false);
    }

    private static String formatDelta(int delta, boolean invertPositive) {
        if (delta == 0) return "0";
        String sign = delta > 0 ? "+" : "";
        return sign + delta;
    }

    private static String truncate(String text, int maxLength) {
        if (text == null || text.isEmpty()) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    private static class TestResult {
        String scenario;
        String testCase;
        String status;
        long duration;
        String error;
    }
}

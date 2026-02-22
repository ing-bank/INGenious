
package com.ing.engine.reporting;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import com.ing.engine.constants.FilePath;
import com.ing.engine.core.RunContext;
import com.ing.engine.core.TMIntegration;
import com.ing.engine.reporting.impl.excel.ExcelSummaryHandler;
import com.ing.engine.reporting.impl.handlers.PrimaryHandler;
import com.ing.engine.reporting.impl.handlers.SummaryHandler;
import com.ing.engine.reporting.impl.html.HtmlSummaryHandler;
import com.ing.engine.reporting.impl.slack.SlackSummaryHandler;
import com.ing.engine.reporting.impl.sync.SAPISummaryHandler;
import com.ing.engine.reporting.impl.rp.RPSummaryHandler;
import com.ing.engine.reporting.impl.extent.ExtentSummaryHandler;
import com.ing.engine.reporting.impl.azure.AzureSummaryHandler;
import com.ing.engine.reporting.intf.OverviewReport;
import com.ing.engine.reporting.performance.har.Har;
import com.ing.engine.reporting.sync.Sync;
import com.ing.engine.reporting.util.DateTimeUtils;
import com.ing.engine.reporting.util.TestInfo;
import com.ing.engine.support.Status;

public final class SummaryReport implements OverviewReport {

   

    public boolean RunComplete = false;

    DateTimeUtils RunTime;

    public Sync sync;

    public Date startDate;

    public Date endDate;

    private static final List<SummaryHandler> REPORT_HANDLERS = new ArrayList<>();
   
    public PrimaryHandler pHandler;
    
    // Execution tracking
    private int totalTestCases = 0;
    private int passedTestCases = 0;
    private int failedTestCases = 0;
    private long executionStartTime = 0;

    public SummaryReport() {        

        register(new ExtentSummaryHandler(this), true);
        register(new RPSummaryHandler(this), true);
        register(new HtmlSummaryHandler(this), true);
        register(new SAPISummaryHandler(this));
        register(new ExcelSummaryHandler(this));
        register(new SlackSummaryHandler(this));
        register(new AzureSummaryHandler(this));
    }

    @SuppressWarnings("rawtypes")
    public void addHar(Har<String, Har.Log> h, TestCaseReport report, String pageName) {
        for (SummaryHandler handler : REPORT_HANDLERS) {
            handler.addHar(h, report, pageName);
        }
    }

    /**
     * initialize the report data file.
     *
     * @param runTime
     * @param size
     */
    @Override
    public synchronized void createReport(String runTime, int size) {
        executionStartTime = System.currentTimeMillis();
        totalTestCases = 0;
        passedTestCases = 0;
        failedTestCases = 0;
        
        for (SummaryHandler handler : REPORT_HANDLERS) {
            handler.createReport(runTime, size);
        }
    }

    /**
     * update the result of each test case result
     *
     * @param runContext
     * @param report
     * @param state
     * @param executionTime
     */
    @Override
    public synchronized void updateTestCaseResults(RunContext runContext, TestCaseReport report, Status state,
            String executionTime) {
        // Track results
        totalTestCases++;
        String statusEmoji;
        if (state != null && (state.toString().contains("PASS") || state.toString().contains("DONE"))) {
            passedTestCases++;
            statusEmoji = "✅";
        } else {
            failedTestCases++;
            statusEmoji = "❌";
        }
        
        // Print test case result with emoji
        System.out.println("══════════════════════════════════════════════════════════════════════════════");
        System.out.println(statusEmoji + " Test Case: " + runContext.Scenario + ":" + runContext.TestCase + 
                           " | Status: " + state + " | Time: " + executionTime);
        System.out.println("══════════════════════════════════════════════════════════════════════════════");
        System.out.println();
        
        for (SummaryHandler handler : REPORT_HANDLERS) {
            handler.updateTestCaseResults(runContext, report, state, executionTime);
        }
        if (TMIntegration.isEnabled()) {
            updateTMResults(runContext, report, executionTime, state);
        }
    }

    private void updateTMResults(RunContext runContext, TestCaseReport report,
            String executionTime, Status state) {
        if (sync != null && sync.isConnected()) {
            System.out.println("[Uploading Results to Test management]");
            TestInfo tc = new TestInfo(runContext.Scenario, runContext.TestCase, runContext.Description,
					runContext.Iteration, executionTime, FilePath.getDate(), FilePath.getTime(), runContext.BrowserName,
					runContext.BrowserVersion, runContext.PlatformValue, startDate, endDate);
            List<File> attach = new ArrayList<>();
           // attach.add(new File(FilePath.getCurrentResultsPath(), report.getFile().getName()));
            /*
            * create temp. console to avoid error from jira server on sending a open stream
             */
            //File tmpConsole = createTmpConsole(new File(FilePath.getCurrentResultsPath(), "console.txt"));
            String logPrefix = tc.testScenario + "_" + tc.testCase;
            File testcaseLog = new File(FilePath.getCurrentTestCaseLogsLocation()+File.separator+logPrefix+".txt");
            Optional.ofNullable(testcaseLog).ifPresent(attach::add);
            String prefix = tc.testScenario + "_" + tc.testCase + "_Step-";
            File imgFolder = new File(FilePath.getCurrentResultsPath() + File.separator + "img");
            if (imgFolder.exists()) {
                for (File image : imgFolder.listFiles()) {
                    if (image.getName().startsWith(prefix)) {
                        attach.add(image);
                    }
                }
            }
            File payloadFolder = new File(FilePath.getCurrentResultsPath() + File.separator + "webservice");
            if (payloadFolder.exists()) {
                for (File payload : payloadFolder.listFiles()) {
                    if (payload.getName().startsWith(prefix)) {
                        attach.add(payload);
                    }
                }
            }
            String status = state.equals(Status.PASS) ? "Passed" : "Failed";
            if (!sync.updateResults(tc, status, attach)) {
                report.updateTestLog(sync.getModule(), "Unable to Update Results to "
                        + sync.getModule(), Status.DEBUG);
            }
          //  Optional.ofNullable(tmpConsole).ifPresent(File::delete);
        } else {
            System.out.println("[ERROR:UNABLE TO REACH TEST MANAGEMENT MODULE!!!]");
            report.updateTestLog("Error", "Unable to Connect to TM Module", Status.DEBUG);
        }
    }

    public File createTmpConsole(File console) {
        try {
            File tmpConsole = File.createTempFile("console", ".txt");
            Files.copy(console.toPath(), tmpConsole.toPath(), StandardCopyOption.REPLACE_EXISTING);
            tmpConsole.deleteOnExit();
            return tmpConsole;
        } catch (IOException ex) {
            return null;
        }
    }

    /**
     * finalize the summary report creation
     *
     * @throws Exception
     */
    @Override
    public synchronized void finalizeReport() throws Exception {
        RunComplete = true;
        for (SummaryHandler handler : REPORT_HANDLERS) {
            handler.finalizeReport();
        }
        
        // Print execution summary
        printExecutionSummary();
        
        afterReportComplete();
    }
    
    /**
     * Print execution summary with emojis
     */
    private void printExecutionSummary() {
        long totalDuration = System.currentTimeMillis() - executionStartTime;
        long minutes = (totalDuration / 1000) / 60;
        long seconds = (totalDuration / 1000) % 60;
        
        String overallStatus = (failedTestCases == 0) ? "✅ PASSED" : "❌ FAILED";
        
        System.out.println();
        System.out.println("╔═════════════════════════════════════════════════════════════════════════════╗");
        System.out.println(formatBoxLine("🏁 EXECUTION SUMMARY 🏁", true));
        System.out.println("╠═════════════════════════════════════════════════════════════════════════════╣");
        System.out.println(formatBoxLine("📊 Total Tests:  " + totalTestCases, false));
        System.out.println(formatBoxLine("✅ Passed:       " + passedTestCases, false));
        System.out.println(formatBoxLine("❌ Failed:       " + failedTestCases, false));
        System.out.println(formatBoxLine("⏱️  Duration:     " + String.format("%dm %ds", minutes, seconds), false));
        System.out.println("╠═════════════════════════════════════════════════════════════════════════════╣");
        System.out.println(formatBoxLine("Overall Status: " + overallStatus, false));
        System.out.println("╚═════════════════════════════════════════════════════════════════════════════╝");
        System.out.println();
    }
    
    /**
     * Format a line to fit in the box with proper padding and alignment
     * @param content The content to display
     * @param centered Whether to center the content
     * @return Formatted line with box borders
     */
    private String formatBoxLine(String content, boolean centered) {
        final int BOX_WIDTH = 77; // Total width between ║ and ║
        
        // Calculate visual width (emojis count as 2 chars in most terminals)
        int visualWidth = getVisualWidth(content);
        
        if (centered) {
            int totalPadding = BOX_WIDTH - visualWidth;
            int leftPadding = totalPadding / 2;
            int rightPadding = totalPadding - leftPadding;
            return "║" + " ".repeat(Math.max(0, leftPadding)) + content + " ".repeat(Math.max(0, rightPadding)) + "║";
        } else {
            // Left-aligned with 2 spaces prefix
            int remainingSpace = BOX_WIDTH - visualWidth - 2;
            return "║  " + content + " ".repeat(Math.max(0, remainingSpace)) + "║";
        }
    }
    
    /**
     * Calculate visual width of string accounting for emojis (which display as 2 chars wide)
     * @param str The string to measure
     * @return Visual width in terminal
     */
    private int getVisualWidth(String str) {
        int width = 0;
        int i = 0;
        while (i < str.length()) {
            int codePoint = str.codePointAt(i);
            int charCount = Character.charCount(codePoint);
            
            // Check if this is a variation selector (U+FE00-U+FE0F) - should not add width
            if (codePoint >= 0xFE00 && codePoint <= 0xFE0F) {
                i += charCount;
                continue;
            }
            
            // Detect emoji characters (display as 2 chars wide in terminal)
            if (isEmoji(codePoint)) {
                width += 2;
            } else {
                width += 1;
            }
            
            i += charCount;
        }
        return width;
    }
    
    /**
     * Check if a Unicode code point is an emoji
     * @param codePoint The Unicode code point
     * @return true if emoji, false otherwise
     */
    private boolean isEmoji(int codePoint) {
        return (codePoint >= 0x1F300 && codePoint <= 0x1F9FF) || // Misc Symbols and Pictographs + Supplemental
               (codePoint >= 0x2600 && codePoint <= 0x27BF) ||   // Misc Symbols + Dingbats
               (codePoint >= 0x1F000 && codePoint <= 0x1F2FF) ||  // Mahjong, Domino, Playing Cards
               (codePoint >= 0x231A && codePoint <= 0x23FF) ||    // Misc Technical (includes ⏱️)
               (codePoint >= 0x2B50 && codePoint <= 0x2B55) ||    // Stars
               (codePoint == 0x2705) ||  // ✅ White Heavy Check Mark
               (codePoint == 0x274C) ||  // ❌ Cross Mark
               (codePoint == 0x203C) ||  // ‼️ Double Exclamation Mark
               (codePoint == 0x2049);    // ⁉️ Exclamation Question Mark
    }

    /**
     * update the result when any error in execution
     *
     * @param testScenario
     * @param testCase
     * @param Iteration
     * @param testDescription
     * @param executionTime
     * @param fileName
     * @param state
     * @param Browser
     */
    @Override
    public void updateTestCaseResults(String testScenario, String testCase, String Iteration, String testDescription,
            String executionTime, String fileName, Status state, String Browser) {
        for (SummaryHandler handler : REPORT_HANDLERS) {
            handler.updateTestCaseResults(testScenario, testCase, Iteration, testDescription, executionTime, fileName,
                    state, Browser);
        }
    }

    

    public void afterReportComplete() throws Exception {
    	  
    }
    
    public Boolean isPassed() {
        return !pHandler.getCurrentStatus().equals(Status.FAIL);
    }

    public static void register(SummaryHandler summaryHandler) {
        if (!REPORT_HANDLERS.contains(summaryHandler)) {
            REPORT_HANDLERS.add(summaryHandler);
        }
    }

    public static void reset() {
        REPORT_HANDLERS.clear();
    }

    private void register(SummaryHandler summaryHandler, boolean primaryHandler) {
        register(summaryHandler);
        if (primaryHandler) {
            pHandler = (PrimaryHandler) summaryHandler;
        }
    }

}

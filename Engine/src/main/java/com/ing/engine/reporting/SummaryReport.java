
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
        afterReportComplete();
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
        System.out.println("\n======================== [UPDATING SUMMARY] ========================\n");
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

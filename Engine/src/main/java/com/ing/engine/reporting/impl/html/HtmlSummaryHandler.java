package com.ing.engine.reporting.impl.html;

import com.ing.engine.constants.FilePath;
import com.ing.engine.constants.SystemDefaults;
import com.ing.engine.core.Control;
import com.ing.engine.core.RunContext;
import com.ing.engine.core.RunManager;
import com.ing.engine.reporting.SummaryReport;
import com.ing.engine.reporting.TestCaseReport;

import com.ing.engine.reporting.impl.handlers.PrimaryHandler;
import com.ing.engine.reporting.impl.handlers.SummaryHandler;
import com.ing.engine.reporting.impl.html.bdd.CucumberReport;
import com.ing.engine.reporting.performance.PerformanceReport;
import com.ing.engine.reporting.performance.har.Har;
import com.ing.engine.reporting.util.DateTimeUtils;
import com.ing.engine.reporting.util.RDS;
import com.ing.engine.reporting.util.ReportUtils;
import com.ing.engine.support.DesktopApi;
import com.ing.engine.support.Status;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 *
 *
 */
@SuppressWarnings("rawtypes")
public class HtmlSummaryHandler extends SummaryHandler implements PrimaryHandler {

    private static final Logger LOGGER = Logger.getLogger(HtmlSummaryHandler.class.getName());

    JSONObject testSetData = new JSONObject();
    JSONArray executions = new JSONArray();
    public boolean RunComplete = false;
    int FailedTestCases = 0;
    int PassedTestCases = 0;
    int noTests = 0;
    DateTimeUtils RunTime;
    public PerformanceReport perf;

    public HtmlSummaryHandler(SummaryReport report) {
        super(report);
        if (Control.exe.getExecSettings().getRunSettings().isPerformanceLogEnabled()) {
            perf = new PerformanceReport();
        }
        createReportIfNotExists(FilePath.getResultsPath());

    }

    @Override
    public void addHar(Har<String, Har.Log> h, TestCaseReport report, String pageName) {
        if (perf != null) {
            perf.addHar(h, report, pageName);
        }
    }

    private void createReportIfNotExists(String path) {
        File file = new File(path + File.separator + "media");
        if (!file.exists()) {
            file.mkdirs();
            try {
                FileUtils.copyDirectory(new File(FilePath.getReportResourcePath()), file);
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * initialize the report data file.
     *
     * @param runTime
     * @param size
     */
    @SuppressWarnings("unchecked")
    @Override
    public synchronized void createReport(String runTime, int size) {

        try {
            ReportUtils.loadDefaultTheme(testSetData);
            RunTime = new DateTimeUtils();
            new File(FilePath.getCurrentResultsPath()).mkdirs();
            testSetData.put(RDS.TestSet.PROJECT_NAME, RunManager.getGlobalSettings().getProjectName());
            testSetData.put(RDS.TestSet.RELEASE_NAME, RunManager.getGlobalSettings().getRelease());
            testSetData.put(RDS.TestSet.TESTSET_NAME, RunManager.getGlobalSettings().getTestSet());
            testSetData.put(RDS.TestSet.ITERATION_MODE,
                    Control.exe.getExecSettings().getRunSettings().getIterationMode());
            testSetData.put(RDS.TestSet.RUN_CONFIG, Control.exe.getExecSettings().getRunSettings().getExecutionMode());
            testSetData.put(RDS.TestSet.MAX_THREADS, Control.exe.getExecSettings().getRunSettings().getThreadCount());
            testSetData.put(RDS.TestSet.BDD_STYLE, Control.exe.getExecSettings().getRunSettings().isBddReportEnabled());
            testSetData.put(RDS.TestSet.PERF_REPORT, Control.exe.getExecSettings().getRunSettings().isPerformanceLogEnabled());
            testSetData.put(RDS.TestSet.START_TIME, runTime);
            testSetData.put(RDS.TestSet.TEST_RUN, RunManager.getGlobalSettings().isTestRun());
            testSetData.put(RDS.TestSet.NO_OF_TESTS, size);

        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
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
    @SuppressWarnings("unchecked")
    @Override
    public synchronized void updateTestCaseResults(RunContext runContext, TestCaseReport report, Status state,
            String executionTime) {

        executions.add(report.getData());
        String status;
        if (state.equals(Status.PASS)) {
            status = "Passed";
            PassedTestCases++;
        } else {
            FailedTestCases++;
            status = "Failed";
        }
        ReportUtils.updateStatus(status, runContext);

        if (perf != null) {
            perf.updateTestCase(report.Scenario, report.TestCase);
        }
        updateResults();
    }

    /**
     * update the test set details to the json data file and write the data file
     */
    @SuppressWarnings("unchecked")
    public synchronized void updateResults() {
        String exeTime = RunTime.timeRun();
        String endTime = DateTimeUtils.DateTimeNow();

        try {
            if (RunComplete) {
                testSetData.put(RDS.TestSet.EXECUTIONS, executions);
                testSetData.put(RDS.TestSet.END_TIME, endTime);
                testSetData.put(RDS.TestSet.EXE_TIME, exeTime);

                testSetData.put(RDS.TestSet.NO_OF_FAIL_TESTS, String.valueOf(FailedTestCases));
                testSetData.put(RDS.TestSet.NO_OF_PASS_TESTS, String.valueOf(PassedTestCases));
                RDS.writeToDataJS(FilePath.getCurrentReportDataPath(), testSetData);
            } else {

            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }

    }

    /**
     * finalize the summary report creation
     */
    @Override
    public synchronized void finalizeReport() {
        RunComplete = true;
        updateResults();
        if (!RunManager.getGlobalSettings().isTestRun()) {
            updateReportHistoryData();
        }
        try {
            if (SystemDefaults.CLVars.containsKey("createStandaloneReport")) {
                createStandaloneHtmls();
            } else {
                createHtmls();
            }

            createBddReport();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
        printReport();
        createLatest();
        launchResultSummary();
    }

    private void createHtmls() throws IOException {
        FileUtils.copyFileToDirectory(new File(FilePath.getSummaryHTMLPath()),
                new File(FilePath.getCurrentResultsPath()));
        FileUtils.copyFileToDirectory(new File(FilePath.getDetailedHTMLPath()),
                new File(FilePath.getCurrentResultsPath()));
        if (perf != null) {
            perf.exportReport();
            FileUtils.copyFileToDirectory(new File(FilePath.getPerfReportHTMLPath()),
                    new File(FilePath.getCurrentResultsPath()));
        }
        if (Control.exe.getExecSettings().getRunSettings().isVideoEnabled()) {
            FileUtils.copyFileToDirectory(new File(FilePath.getVideoReportHTMLPath()),
                    new File(FilePath.getCurrentResultsPath()));
        }
    }

    private void createStandaloneHtmls() throws IOException {

        createReportIfNotExists(FilePath.getCurrentResultsPath());

        String summaryHtml = FileUtils.readFileToString(new File(FilePath.getSummaryHTMLPath()), Charset.defaultCharset());
        summaryHtml = summaryHtml.replaceAll("../../../../media", "media");
        FileUtils.writeStringToFile(new File(FilePath.getCurrentSummaryHTMLPath()), summaryHtml, Charset.defaultCharset());

        String detailedHtml = FileUtils.readFileToString(new File(FilePath.getDetailedHTMLPath()), Charset.defaultCharset());
        detailedHtml = detailedHtml.replaceAll("../../../../media", "media");
        FileUtils.writeStringToFile(new File(FilePath.getCurrentDetailedHTMLPath()), detailedHtml, Charset.defaultCharset());

        if (perf != null) {
            perf.exportReport();
            String perfHtml = FileUtils.readFileToString(new File(FilePath.getPerfReportHTMLPath()), Charset.defaultCharset());
            perfHtml = perfHtml.replaceAll("../../../../media", "media");
            FileUtils.writeStringToFile(new File(FilePath.getCurrentPerfReportHTMLPath()), perfHtml, Charset.defaultCharset());

        }
    }

    private void createBddReport() throws Exception {
        if (Control.exe.getExecSettings().getRunSettings().isBddReportEnabled()) {
            CucumberReport.get().ifPresent(this::createCucumberBddReport);
        }
    }

    private void createCucumberBddReport(CucumberReport reporter) {
        try {
            System.out.print("Generating BDD-Report...");
            reporter.toCucumberReport(testSetData.toString(),
                    new File(FilePath.getCurrentResultsPath(), "bdd-report.json"));
            System.out.println("Done!");
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    private synchronized void createLatest() {
        try {
            File latestResult = new File(FilePath.getLatestResultsLocation());
            if (latestResult.exists()) {
                FileUtils.deleteDirectory(latestResult);
            }
            latestResult.mkdirs();
            FileUtils.copyDirectory(new File(FilePath.getCurrentResultsPath()), latestResult);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public boolean isExtentEnabled() {
        if (!RunManager.getGlobalSettings().isTestRun()) {
            return Control.getCurrentProject().getProjectSettings()
                    .getExecSettings(RunManager.getGlobalSettings().getRelease(), RunManager.getGlobalSettings().getTestSet()).getRunSettings().isExtentReport();
        }

        return false;
    }

    /**
     * open the summary report when execution is finished
     */
    public synchronized void launchResultSummary() {
        if (!isExtentEnabled()) {
            if (SystemDefaults.canLaunchSummary()) {
                DesktopApi.open(new File(FilePath.getCurrentSummaryHTMLPath()));
            }
        }
    }

    /**
     * updates the history of execution report
     */
    @SuppressWarnings("unchecked")
    private void updateReportHistoryData() {
        File file = new File(FilePath.getCurrentReportHistoryDataPath());
        ObjectMapper objectMapper = new ObjectMapper();
        String name = "var reportName=\"" + RunManager.getGlobalSettings().getRelease() + ":"
                + RunManager.getGlobalSettings().getTestSet() + "\";";
        String varaible = "var dataSet=";
        ArrayList<Map<String, String>> reportlist = new ArrayList<>();
        try {
            FileUtils.copyFileToDirectory(new File(FilePath.getReportHistoryHTMLPath()),
                    new File(FilePath.getCurrentResultsLocation()));
            if (file.exists()) {
                String value = FileUtils.readFileToString(file, Charset.defaultCharset());
                value = value.replace(name, "").replace(varaible, "");
                reportlist = objectMapper.readValue(value, ArrayList.class);
            } else {
                file.createNewFile();
            }
            reportlist.add(getReportData());
            String jsonVal = objectMapper.writeValueAsString(reportlist);
            FileUtils.writeStringToFile(file, name + varaible + jsonVal, Charset.defaultCharset());
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    /**
     *
     * @return the test set result details
     */
    private Map<String, String> getReportData() {
        Map<String, String> reportMap = new HashMap<>();
        reportMap.put("ExecutionDate", FilePath.getDate() + " " + FilePath.getTime());
        reportMap.put("ExecTC", String.valueOf(PassedTestCases + FailedTestCases));
        reportMap.put("PassTC", String.valueOf(PassedTestCases));
        reportMap.put("FailTC", String.valueOf(FailedTestCases));
        reportMap.put("ExecTime", RunTime.timeRun());
        reportMap.put("ReportPath", FilePath.getCurrentSummaryHTMLPathRelative());
        return reportMap;
    }

    private void printReport() {
        System.out.println("-----------------------------------------------------");
        print("ExecutionDate", FilePath.getDate() + " " + FilePath.getTime());
        print("Executed TestCases", String.valueOf(PassedTestCases + FailedTestCases));
        print("Passed TestCases", String.valueOf(PassedTestCases));
        print("Failed TestCases", String.valueOf(FailedTestCases));
        print("Time Taken", RunTime.timeRun());
        print("ReportPath", "file:///" + FilePath.getCurrentSummaryHTMLPath());
        System.out.println("-----------------------------------------------------");
    }

    private void print(String key, Object val) {
        System.out.println(String.format("%-20s : %s", key, val));
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

        System.out.println("--------------->[UPDATING SUMMARY]");
        if (state.equals(Status.PASS)) {
            PassedTestCases++;
        } else {
            FailedTestCases++;
        }
    }

    @Override
    public Object getData() {
        return testSetData;
    }

    @Override
    public File getFile() {
        return new File(FilePath.getCurrentSummaryHTMLPath());
    }

    @Override
    public Status getCurrentStatus() {
        if (FailedTestCases > 0 || PassedTestCases == 0) {
            return Status.FAIL;
        } else {
            return Status.PASS;
        }
    }

}

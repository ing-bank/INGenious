package com.ing.engine.reporting.impl.azure;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TimeZone;
import java.util.UUID;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;

import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import com.ing.engine.constants.AppResourcePath;
import com.ing.engine.constants.FilePath;
import com.ing.engine.core.Control;
import com.ing.engine.core.RunContext;
import com.ing.engine.core.RunManager;
import com.ing.engine.drivers.PlaywrightDriverCreation;
import com.ing.engine.drivers.WebDriverCreation;
import com.ing.engine.reporting.TestCaseReport;
import com.ing.engine.reporting.impl.azureNunit.AzureReport;
import com.ing.engine.reporting.impl.handlers.PrimaryHandler;
import com.ing.engine.reporting.impl.handlers.TestCaseHandler;
import com.ing.engine.reporting.util.DateTimeUtils;
import com.ing.engine.reporting.util.RDS;
import com.ing.engine.reporting.util.RDS.TestCase;
import com.ing.engine.reporting.util.ReportUtils;
import com.ing.engine.support.Status;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings({"unchecked"})
public class AzureTestCaseHandler extends TestCaseHandler implements PrimaryHandler {

    private static final Logger LOGGER = Logger.getLogger(AzureTestCaseHandler.class.getName());

    JSONObject testCaseData = new JSONObject();
    JSONArray Steps = new JSONArray();
    JSONObject iteration;
    JSONObject reusable;
    String DATAF = "[<DATA>]";
    boolean isIteration = true;
    Stack<JSONObject> reusableStack = new Stack<>();

    private StringBuffer SourceDoc;
    public File ReportFile;
    public HttpClient client = HttpClientBuilder.create().useSystemProperties().build();
    public static Map<String, String> itemIds = new HashMap<String, String>();

    String CurrentComponent = "";

    int ComponentCounter = 0;
    int iterCounter = 0;

    int FailedSteps = 0;
    int PassedSteps = 0;
    int DoneSteps = 0;

    String testcasename = "";
    String description = "";
    String platform = "";
    String browserName = "";
    String iterationValue = "";
    String attachments = "";

    String messageCDATA = "";
    String stacktraceData = "";

    public AzureTestCaseHandler(TestCaseReport report) {
        super(report);
    }

    public boolean isAzureEnabled() {
        if (!RunManager.getGlobalSettings().isTestRun()) {
            return Control.getCurrentProject().getProjectSettings()
                    .getExecSettings(RunManager.getGlobalSettings().getRelease(), RunManager.getGlobalSettings().getTestSet()).getRunSettings().isAzureEnabled();
        }
        return false;
    }

    @Override
    public void setPlaywrightDriver(PlaywrightDriverCreation driver) {
        testCaseData.put(TestCase.B_VERSION, getPlaywrightDriver().getBrowserVersion());
        platform = System.getProperty("os.name") + " " + System.getProperty("os.version") + " " + System.getProperty("os.arch");
        browserName = getPlaywrightDriver().getCurrentBrowser();
    }

    @Override
    public void setWebDriver(WebDriverCreation driver) {
        testCaseData.put(TestCase.B_VERSION, getWebDriver().getCurrentBrowserVersion());
        platform = getWebDriver().getPlatform();
        browserName = getWebDriver().getCurrentBrowser();
    }

    @Override
    public void createReport(RunContext runContext, String runTime) {
        if (isAzureEnabled()) {
            try {
                testcasename = runContext.Scenario + ":" + runContext.TestCase;
                description = runContext.Description;
                iterationValue = runContext.Iteration;
                addTestCase();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        testCaseData.put(TestCase.SCENARIO_NAME, runContext.Scenario);
        testCaseData.put(TestCase.TESTCASE_NAME, runContext.TestCase);
        testCaseData.put(TestCase.DESCRIPTION, runContext.Description);
        testCaseData.put(TestCase.START_TIME, runTime);
        testCaseData.put(TestCase.ITERATION_TYPE, runContext.Iteration);
    }

    @Override
    public Object getData() {
        // TODO Auto-generated method stub
        return testCaseData;
    }

    @Override
    public File getFile() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void updateTestLog(String stepName, String stepDescription, Status state, String link, List<String> links) {

        String time = DateTimeUtils.DateTimeNow();
        String stepData = "";
        JSONObject step;
        try {

            step = RDS.getNewStep(getStep().Description);
            JSONObject data = (JSONObject) step.get(RDS.Step.DATA);
            data.put(RDS.Step.Data.STEP_NO, getStepCount());
            data.put(RDS.Step.Data.STEP_NAME, stepName);
            data.put(RDS.Step.Data.ACTION, getStep().Action);
            data.put(RDS.Step.Data.DESCRIPTION, ReportUtils.resolveDesc(stepDescription));
            data.put(RDS.Step.Data.TIME_STAMP, time);
            data.put(RDS.Step.Data.STATUS, state.toString());
            stepData = String.format("[%s]   | %s", state, ReportUtils.resolveDesc(stepDescription));

            stepData = stepData.replaceAll("\"", "--");
            stepData = stepData.replaceAll("\\r\\n|\\r|\\n", "");
            stepData = stepData.replaceAll("<br>", "");
            stepData = stepData.replaceAll("#CTAG", "");

            if (link != null) {
                data.put(RDS.Step.Data.LINK, link);
            }
            putStatus(state, links, link, data);
            String filename = "";

            if (data.get(RDS.Step.Data.LINK) != null) {
                filename = AppResourcePath.getCurrentResultsPath() + data.get(RDS.Step.Data.LINK);
            }

            String payloadfiles = testCaseData.get(TestCase.SCENARIO_NAME)
                    + "_"
                    + testCaseData.get(TestCase.TESTCASE_NAME)
                    + "_Step-"
                    + getStepCount()
                    + "_";

            String linkPath = AppResourcePath.getCurrentResultsPath() + link + File.separator + payloadfiles;

            if (linkPath.contains("webservice")) {
                data.put(RDS.Step.Data.LINK, linkPath);
            }

            if (isAzureEnabled()) {
                try {

                    createLogNodes(stepData, state.toString(), filename);
                } catch (IOException | ParseException e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                }
            }
            if (isIteration) {
                ((JSONArray) iteration.get(RDS.Step.DATA)).add(step);
            } else {
                ((JSONArray) reusable.get(RDS.Step.DATA)).add(step);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * creates new iteration object
     *
     * @param iterationNo
     */
    @Override
    public void startIteration(int iterationNo) {
        reusableStack.clear();
        ++iterCounter;
        String Iterationid = "Iteration_" + iterationNo;
        iteration = RDS.getNewIteration(Iterationid);
        isIteration = true;
    }

    /**
     * creates new reusable object
     *
     * @param component
     * @param desc
     */
    @Override
    public void startComponent(String component, String desc) {
        reusable = RDS.getNewReusable(component, desc);
        reusableStack.push(reusable);
        isIteration = false;
    }

    @Override
    public void endComponent(String string) {
        reusable.put(RDS.Step.END_TIME, DateTimeUtils.DateTimeNow());
        if (reusable.get(TestCase.STATUS).equals("")) {
            /* status not is updated set it to FAIL */
            reusable.put(TestCase.STATUS, "FAIL");
        }

        /*
		 * remove the reusable from the stack then fall back to iteration if stack is
		 * empty else update the outer reusable status.
         */
        reusableStack.pop();
        if (reusableStack.empty()) {
            ((JSONArray) iteration.get(RDS.Step.DATA)).add(reusable);
            reusable = null;
            isIteration = true;
        } else {
            ((JSONArray) reusableStack.peek().get(RDS.Step.DATA)).add(reusable);
            reusableStack.peek().put(TestCase.STATUS, reusable.get(TestCase.STATUS));
            reusable = reusableStack.peek();
        }

    }

    @Override
    public void endIteration(int CurrentTestCaseIteration) {
        if (iteration.get(TestCase.STATUS).equals("")) {
            iteration.put(TestCase.STATUS, "FAIL");
        }
        Steps.add(iteration);
    }

    private void onSetpDone() {
        DoneSteps++;
        if (reusable != null && reusable.get(TestCase.STATUS).equals("")) {
            reusable.put(TestCase.STATUS, "PASS");
        }
        if (iteration != null && iteration.get(TestCase.STATUS).equals("")) {
            iteration.put(TestCase.STATUS, "PASS");
        }
    }

    private void onSetpPassed() {
        PassedSteps++;
        if (reusable != null && reusable.get(TestCase.STATUS).equals("")) {
            reusable.put(TestCase.STATUS, "PASS");
        }
        if (iteration != null && iteration.get(TestCase.STATUS).equals("")) {
            iteration.put(TestCase.STATUS, "PASS");
        }
    }

    private void onSetpFailed() {
        FailedSteps++;
        if (iteration != null) {
            iteration.put(TestCase.STATUS, "FAIL");
        }
        if (reusable != null) {
            reusable.put(TestCase.STATUS, "FAIL");
        }
    }

    private void putStatus(Status state, List<String> optional, String optionalLink, JSONObject data) {
        switch (state) {
            case DONE:
            case PASSNS:
                onSetpDone();
                break;
            case PASS:
            case FAIL:
            case SCREENSHOT:
                takeScreenShot(state, optional, optionalLink, data);
                break;
            case DEBUG:
            case WARNING:
            case FAILNS:
                onSetpFailed();
                break;

        }
    }

    private void takeScreenShot(Status status, List<String> optional, String optionalLink, JSONObject data) {
        String imgSrc = getScreenShotName();
        switch (status) {
            case PASS:
            case FAIL:
                if (!canTakeScreenShot(status)) {
                    break;
                }
                if (optionalLink != null) {
                    break;
                }
            case SCREENSHOT:
                takeSSAndPutDetail(data, optional, imgSrc);
                break;
            default:
                break;
        }
    }

    private Boolean canTakeScreenShot(Status status) {
        if (status.equals(Status.FAIL)) {
            onSetpFailed();
            return screenShotSettings().matches("(Fail|Both)");
        }
        if (status.equals(Status.PASS)) {
            onSetpPassed();
            return screenShotSettings().matches("(Pass|Both)");

        }
        return false;
    }

    private static String screenShotSettings() {
        return Control.exe.getExecSettings().getRunSettings().getScreenShotFor();
    }

    /**
     * takes new screen shot and updates the the json object for that step
     *
     * @param data
     * @param imgSrc
     */
    private void takeSSAndPutDetail(JSONObject data, List<String> optional, String imgSrc) {
        if (optional != null && optional.size() == 3) {
            data.put(RDS.Step.Data.EXPECTED, optional.get(0));
            data.put(RDS.Step.Data.ACTUAL, optional.get(1));
            data.put(RDS.Step.Data.COMPARISION, optional.get(2));
        } else {
            if (optional != null) {
                data.put(RDS.Step.Data.OBJECTS, optional.get(0));
            }
            if (ReportUtils.takeScreenshot(getPlaywrightDriver(), getWebDriver(), imgSrc)) {
                data.put(RDS.Step.Data.LINK, imgSrc);
            }
        }

    }

    /**
     * finalize the test case execution and create standalone test case report
     * file for upload purpose
     *
     * @return
     */
    @Override
    public Status finalizeReport() {
        updateResults();
        String prefix = testCaseData.get(TestCase.SCENARIO_NAME) + "_" + testCaseData.get(TestCase.TESTCASE_NAME);
        File logsFolder = new File(FilePath.getCurrentTestCaseLogsLocation());
        String logPath = logsFolder.getAbsolutePath() + File.separator + prefix + ".txt";
        attachments += "<attachment><filePath>" + logPath + "</filePath></attachment>" + "\n";

        File videoFolder = new File(FilePath.getCurrentTestCaseVideosLocation());
        if (videoFolder.exists()) {
            File testCaseVideo = new File(FilePath.getCurrentTestCaseVideosLocation() + File.separator + prefix);
            for (File fileEntry : testCaseVideo.listFiles()) {
                this.attachments += "<attachment><filePath>" + fileEntry.getAbsolutePath() + "</filePath></attachment>" + "\n";
            }
        }

        String status = "";
        String noError = "This Test Case has no error. For details see the steps below:" + "\n";
        if (this.stacktraceData.isEmpty()) {
            AzureReport.message = "<message><![CDATA[" + noError + this.messageCDATA + "]]></message>";
        } else {
            AzureReport.message = "<message><![CDATA[" + this.messageCDATA + "]]></message>";
            AzureReport.stacktrace = "<stack-trace><![CDATA[" + this.stacktraceData + "]]></stack-trace>";
        }
        AzureReport.failures = "<failure>" + AzureReport.message + AzureReport.stacktrace + "</failure>";
        AzureReport.attachments = "<attachments>" + attachments + "</attachments>";
        if (testCaseData.get(TestCase.STATUS).equals("PASS") || testCaseData.get(TestCase.STATUS).equals("DONE")
                || testCaseData.get(TestCase.STATUS).equals("COMPLETE")) {
            status = "Passed";
            AzureReport.passed++;
        } else {
            status = "Failed";
            AzureReport.failed++;
        }

        AzureReport.testcase += "<test-case id=\"" + getUUID() + "\" name=\"" + testCaseData.get(TestCase.TESTCASE_NAME)
                + "\" fullname=\"" + testCaseData.get(TestCase.TESTCASE_NAME) + "\" result=\"" + status + "\" time=\""
                + duration(testCaseData.get(TestCase.EXE_TIME).toString()) + "\">" + AzureReport.failures + AzureReport.attachments
                + "</test-case>";
        AzureReport.testsuite += AzureReport.testcase;
        return report.getCurrentStatus();

    }

    public String getUUID() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }

    /**
     * update the test case execution details to the json DATA file
     *
     * @return
     */
    private void updateResults() {
        String endTime = DateTimeUtils.DateTimeNow();
        String exeTime = startTime().timeRun();
        testCaseData.put(TestCase.STEPS, Steps);
        testCaseData.put(TestCase.END_TIME, endTime);
        testCaseData.put(TestCase.EXE_TIME, exeTime);
        testCaseData.put(TestCase.ITERATIONS, iterCounter);
        testCaseData.put(TestCase.NO_OF_TESTS, getStepCount());
        testCaseData.put(TestCase.NO_OF_FAIL_TESTS, String.valueOf(this.FailedSteps));
        testCaseData.put(TestCase.NO_OF_PASS_TESTS, String.valueOf(this.DoneSteps + this.PassedSteps));
        testCaseData.put(TestCase.STATUS, getCurrentStatus().toString());

    }

    private DateTimeUtils startTime() {
        return report.startTime;
    }

    public String duration(String executionTime) {
        long seconds = 0;
        try {
            DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date;
            date = dateFormat.parse(executionTime);
            seconds = date.getTime() / 1000L;
            AzureReport.totalDuration += seconds;
        } catch (java.text.ParseException e) {
            e.printStackTrace();
        }
        return seconds + ".000";
    }

    @Override
    public Status getCurrentStatus() {
        if (FailedSteps > 0 || (PassedSteps + DoneSteps) == 0) {
            return Status.FAIL;
        } else {
            return Status.PASS;
        }
    }

    public void addTestCase() throws ClientProtocolException, IOException, ParseException {
        this.messageCDATA = AzureReport.CDATA;
        this.stacktraceData = AzureReport.stacktraceData;
    }

    public void createLogNodes(String stepdata, String status, String filepath) throws IOException, ParseException {
        String prefix = testCaseData.get(TestCase.SCENARIO_NAME) + "_" + testCaseData.get(TestCase.TESTCASE_NAME);
        this.messageCDATA += "Step " + getStepCount() + ":   " + stepdata + "\n";
        if (filepath.equalsIgnoreCase("")) {

            if (status.contains("PASS") || status.contains("DONE") || status.contains("COMPLETE")) {

            } else {
                this.stacktraceData += "Step " + getStepCount() + ":   " + stepdata + "\n";
            }

        } else {
            File attachment = new File(new File(filepath).getCanonicalPath());
            if (attachment.isDirectory()) {
                for (File fileEntry : attachment.listFiles()) {
                    if (fileEntry.getName().contains(prefix)) {
                        this.attachments += "<attachment><filePath>" + fileEntry.getAbsolutePath() + "</filePath></attachment>" + "\n";
                    }
                }
            } else {
                filepath = attachment.getAbsolutePath();
                this.attachments += "<attachment><filePath>" + filepath + "</filePath></attachment>" + "\n";
            }

            if (status.contains("PASS") || status.contains("DONE") || status.contains("COMPLETE")) {

            } else {
                this.stacktraceData += "Step " + getStepCount() + ":   " + stepdata + "\n";
            }
        }

    }
}

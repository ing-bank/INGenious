package com.ing.engine.reporting.impl.extent;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.markuputils.ExtentColor;
import com.aventstack.extentreports.markuputils.MarkupHelper;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.ing.engine.constants.AppResourcePath;
import com.ing.engine.core.Control;
import com.ing.engine.core.RunContext;
import com.ing.engine.core.RunManager;
import com.ing.datalib.model.Tag;
import com.ing.datalib.model.Tags;
import com.ing.engine.drivers.PlaywrightDriverCreation;
import com.ing.engine.drivers.WebDriverCreation;
import com.ing.engine.reporting.TestCaseReport;
import com.ing.engine.reporting.extentreport.ExtentReport;
import com.ing.engine.reporting.impl.handlers.PrimaryHandler;
import com.ing.engine.reporting.impl.handlers.TestCaseHandler;
import com.ing.engine.reporting.util.DateTimeUtils;
import com.ing.engine.reporting.util.RDS;
import com.ing.engine.reporting.util.RDS.TestCase;
import com.ing.engine.reporting.util.ReportUtils;
import com.ing.engine.support.Status;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import java.util.Base64;

@SuppressWarnings({"unchecked"})
public class ExtentTestCaseHandler extends TestCaseHandler implements PrimaryHandler {

    private static final Logger LOGGER = Logger.getLogger(ExtentTestCaseHandler.class.getName());

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
    String browserVersion = "";
    String iterationValue = "";
    ExtentReport extentReport = new ExtentReport();

    ExtentTest test;
    int index = 0;

    public ExtentTestCaseHandler(TestCaseReport report) {
        super(report);
    }

    public boolean isExtentEnabled() {
        if (!RunManager.getGlobalSettings().isTestRun()) {
            return Control.getCurrentProject().getProjectSettings()
                    .getExecSettings(RunManager.getGlobalSettings().getRelease(), RunManager.getGlobalSettings().getTestSet()).getRunSettings().isExtentReport();
        }
        return false;
    }

    @Override
    public void setPlaywrightDriver(PlaywrightDriverCreation driver) {
        testCaseData.put(TestCase.B_VERSION, getPlaywrightDriver().getBrowserVersion());
        testCaseData.put(TestCase.PLATFORM, System.getProperty("os.name") + " " + System.getProperty("os.version") + " " + System.getProperty("os.arch"));
        testCaseData.put(TestCase.BROWSER, getPlaywrightDriver().getCurrentBrowser());
    }

    @Override
    public void setWebDriver(WebDriverCreation driver) {
        testCaseData.put(TestCase.B_VERSION, getWebDriver().getCurrentBrowserVersion());
        testCaseData.put(TestCase.PLATFORM, getWebDriver().getPlatform());
        testCaseData.put(TestCase.BROWSER, getWebDriver().getCurrentBrowser());
    }

    @Override
    public void createReport(RunContext runContext, String runTime) {
        if (isExtentEnabled()) {
            try {
                testcasename = runContext.Scenario + " : " + runContext.TestCase;
                Tags tags = Control.getCurrentProject().getInfo().getData().findOrCreate(runContext.TestCase, runContext.Scenario).getTags();
                description = runContext.Description;
                iterationValue = runContext.Iteration;
                createTest(testcasename, "", "", "", tags);
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

            if (isExtentEnabled()) {
                try {
                    sendLog(getStep().Action, payloadfiles, state.toString(), stepData, filename);
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
        this.CurrentComponent = component;
        this.test.info(MarkupHelper.createLabel("Reusable Component : [" + this.CurrentComponent + "] starts here", ExtentColor.GREY));
    }

    @Override
    public void endComponent(String string) {
        reusable.put(RDS.Step.END_TIME, DateTimeUtils.DateTimeNow());
        if (reusable.get(TestCase.STATUS).equals("")) {
            /* status not is updated set it to FAIL */
            reusable.put(TestCase.STATUS, "FAIL");
        }
        this.test.info(MarkupHelper.createLabel("Reusable Component : [" + this.CurrentComponent + "] ends here", ExtentColor.GREY));
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
        this.test.assignDevice("[" + testCaseData.get(TestCase.PLATFORM).toString() + "]  [" + testCaseData.get(TestCase.BROWSER).toString() + " : " + testCaseData.get(TestCase.B_VERSION).toString() + "]");
        this.test.assignCategory("Platform: " + testCaseData.get(TestCase.PLATFORM).toString());
        this.test.assignCategory("Browser: " + testCaseData.get(TestCase.BROWSER).toString());
        return report.getCurrentStatus();
    }

    /*
	 * private static final Logger LOG =
	 * Logger.getLogger(TestCaseReport.class.getName());
     */
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
        if (getStepCount() == 1 && this.PassedSteps == 0 && this.DoneSteps != 1) {
            this.test.fail(MarkupHelper.createLabel("An exception has occured! Please check the console.txt for details.", ExtentColor.RED));
        }

    }

    private DateTimeUtils startTime() {
        return report.startTime;
    }

    @Override
    public Status getCurrentStatus() {
        if (FailedSteps > 0 || (PassedSteps + DoneSteps) == 0) {
            return Status.FAIL;
        } else {
            return Status.PASS;
        }
    }

    public void createTest(String testcaseName, String platform, String browser, String browserVersion, Tags tags) throws ClientProtocolException, IOException, ParseException {
        this.test = extentReport.extentReports.createTest(testcaseName);
        if (!tags.isEmpty()) {
            for (Tag tag : tags) {
                this.test.assignCategory(tag.toString().replace("@", ""));
            }
        }
    }

    public void sendLog(String action, String payloadfiles, String status, String teststepdata, String filename) throws IOException, ParseException {
        String actionInfo = "[" + action + "] - ";
        if (filename.equalsIgnoreCase("")) {
            if (status.contains("DONE") || status.contains("COMPLETE")) {
                this.test.info(actionInfo + teststepdata.replace("[" + status + "]", "").replace("|", "").trim());
            } else if (status.contains("WARNING")) {
                this.test.warning(actionInfo + teststepdata.replace("[" + status + "]", "").replace("|", "").trim());
            } else if (status.contains("PASS")) {
                this.test.pass(MarkupHelper.createLabel(
                        actionInfo + teststepdata.replace("[" + status + "]", "").replace("|", "").trim(), ExtentColor.GREEN));
            } else {
                this.test.fail(MarkupHelper.createLabel(
                        actionInfo + teststepdata.replace("[" + status + "]", "").replace("|", "").trim(), ExtentColor.RED));
            }
        } else {
            String encodedString = "";
            File file = new File(new File(filename).getCanonicalPath());
            if (!file.isDirectory()) {
                byte[] fileContent = FileUtils.readFileToByteArray(file);
                encodedString = "data:image/png;base64," + Base64.getEncoder().encodeToString(fileContent);
                if (status.contains("DONE") || status.contains("COMPLETE")) {
                    this.test.info(actionInfo + teststepdata.replace("[" + status + "]", "").replace("|", "").trim());
                } else if (status.contains("WARNING")) {
                    this.test.warning(actionInfo + teststepdata.replace("[" + status + "]", "").replace("|", "").trim());
                } else if (status.contains("PASS")) {
                    this.test.pass(actionInfo + teststepdata.replace("[" + status + "]", "").replace("|", "").trim(),
                            MediaEntityBuilder.createScreenCaptureFromBase64String(encodedString).build());
                } else {
                    this.test.fail(actionInfo + teststepdata.replace("[" + status + "]", "").replace("|", "").trim(),
                            MediaEntityBuilder.createScreenCaptureFromBase64String(encodedString).build());
                }
            } else {
                String request = "";
                String response = "";
                for (File fileEntry : file.listFiles()) {
                    if (fileEntry.getName().contains(payloadfiles + "Request.txt")) {
                        request = fileEntry.getAbsolutePath();
                    } else if (fileEntry.getName().contains(payloadfiles + "Response.txt")) {
                        response = fileEntry.getAbsolutePath();
                    }
                }

                if (status.contains("COMPLETE")) {
                    if (!request.isEmpty()) {
                        this.test.info(actionInfo + "Click to view : <a href='" + request + "'>Request</a>  |   <a href='" + response + "'>Response</a>");
                    } else {
                        this.test.info(actionInfo + "Click to view : <a href='" + response + "'>Response</a>");
                    }
                } else if (status.contains("DONE")) {
                    this.test.info(actionInfo + teststepdata.replace("[" + status + "]", "").replace("|", "").trim());
                } else if (status.contains("WARNING")) {
                    this.test.warning(actionInfo + teststepdata.replace("[" + status + "]", "").replace("|", "").trim());
                } else if (status.contains("PASS")) {
                    this.test.pass(actionInfo + teststepdata.replace("[" + status + "]", "").replace("|", "").trim());
                } else {
                    this.test.fail(actionInfo + teststepdata.replace("[" + status + "]", "").replace("|", "").trim());
                }
            }
        }
    }

    public boolean isBrowserExecution(RunContext runContext) {
        boolean isBrowserExecution = false;
        try {
            String browserName = runContext.BrowserName;
            if (browserName.equals("Chromium") || browserName.equals("WebKit") || browserName.equals("Firefox")) {
                isBrowserExecution = true;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return isBrowserExecution;
    }
}

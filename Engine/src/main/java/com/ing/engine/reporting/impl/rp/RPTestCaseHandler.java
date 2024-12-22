
package com.ing.engine.reporting.impl.rp;

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

import com.ing.engine.constants.AppResourcePath;
import com.ing.engine.core.Control;
import com.ing.engine.core.RunContext;
import com.ing.engine.core.RunManager;
import com.ing.engine.drivers.PlaywrightDriverCreation;
import com.ing.engine.drivers.WebDriverCreation;
import com.ing.engine.reporting.TestCaseReport;
import com.ing.engine.reporting.impl.handlers.PrimaryHandler;
import com.ing.engine.reporting.impl.handlers.TestCaseHandler;
import com.ing.engine.reporting.reportportal.ReportPortalClient;
import com.ing.engine.reporting.util.DateTimeUtils;
import com.ing.engine.reporting.util.RDS;
import com.ing.engine.reporting.util.RDS.TestCase;
import com.ing.engine.reporting.util.ReportUtils;
import com.ing.engine.support.Status;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings({"unchecked"})
public class RPTestCaseHandler extends TestCaseHandler implements PrimaryHandler {
    private static final Logger LOGGER = Logger.getLogger(RPTestCaseHandler.class.getName());

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
    
    public RPTestCaseHandler(TestCaseReport report) {
        super(report);
    }
    
    public boolean isRPEnabled() {
        if (!RunManager.getGlobalSettings().isTestRun()) {
            return Control.getCurrentProject().getProjectSettings()
                    .getExecSettings(RunManager.getGlobalSettings().getRelease(), RunManager.getGlobalSettings().getTestSet()).getRunSettings().isRPUpdate();
        }
        return false;
    }
    
    private String getRPValue(String property) {
        return Control.getCurrentProject().getProjectSettings().getRPSettings().getProperty(property);
    }
    
    @Override
    public void setPlaywrightDriver(PlaywrightDriverCreation driver) {
        testCaseData.put(TestCase.B_VERSION, getPlaywrightDriver().getBrowserVersion());
        testCaseData.put(TestCase.PLATFORM, System.getProperty("os.name")+ " " +System.getProperty("os.version")+ " " +System.getProperty("os.arch"));
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
        System.out.println("Starting test case exec : " + runContext.TestCase);
        if (isRPEnabled()) {
            try {
               // testcasename = runContext.Scenario + "_" + runContext.TestCase + "_" + runContext.Iteration + "_" + runContext.BrowserName; 
                testcasename = runContext.Scenario + ":" + runContext.TestCase;
            	description = runContext.Description;
            	platform = runContext.PlatformValue;
            	browserName = runContext.BrowserName;
            	iterationValue = runContext.Iteration;
            	startItem(getRPValue("rp.endpoint"), getRPValue("rp.uuid"), RunManager.getGlobalSettings().getTestSet(), getRPValue("rp.project"), ReportPortalClient.LaunchID, testcasename);
            } catch (Exception e) {
                // TODO Auto-generated catch block
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
    public void updateTestLog(String stepName, String stepDescription, Status state,
            String link, List<String> links) {
        
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
            
            //System.out.println("stepData : " + stepData);
            
            if (link != null) {
                data.put(RDS.Step.Data.LINK, link);
            }
            putStatus(state, links, link, data);
            String filename = "";
            
            if (data.get(RDS.Step.Data.LINK) != null) {
                filename = AppResourcePath.getCurrentResultsPath() + data.get(RDS.Step.Data.LINK);
            }
            
            String payloadfile = testCaseData.get(TestCase.SCENARIO_NAME)
            		             + "_"
            		             + testCaseData.get(TestCase.TESTCASE_NAME)
            		             + "_Step-"
            		             + getStepCount()
            		             + "_Response.txt";
            
            if (isRPEnabled()) {
                try {
                    sendLog(payloadfile,getRPValue("rp.endpoint"), getRPValue("rp.uuid"), RunManager.getGlobalSettings().getTestSet(), getRPValue("rp.project"), testcasename,
                            state.toString(), stepData, filename);
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
        
        /* remove the reusable from the stack then fall back to iteration
        if stack is empty else update the outer reusable status. */
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
            if (ReportUtils.takeScreenshot(getPlaywrightDriver(),getWebDriver(), imgSrc)) {
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
        return report.getCurrentStatus();
    }
    
    /*private static final Logger LOG = Logger.getLogger(TestCaseReport.class.getName());*/
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
    
    /* private void printReport() {
    System.out.println("\n---------------------------------------------------");
    print("Testcase Name", testCaseData.get(TestCase.SCENARIO_NAME)
    + ":" + testCaseData.get(TestCase.TESTCASE_NAME));
    print("Executed Steps", testCaseData.get(TestCase.NO_OF_TESTS));
    print("Passed Steps", testCaseData.get(TestCase.NO_OF_PASS_TESTS));
    print("Failed Steps", testCaseData.get(TestCase.NO_OF_FAIL_TESTS));
    print("Time Taken", testCaseData.get(TestCase.EXE_TIME));
    System.out.println("-----------------------------------------------------\n");
    }
    
    private void print(String key, Object val) {
    System.out.println(String.format("%-20s : %s", key, val));
    }*/
    @Override
    public Status getCurrentStatus() {
        if (FailedSteps > 0 || (PassedSteps + DoneSteps) == 0) {
            return Status.FAIL;
        } else {
            return Status.PASS;
        }
    }
    
    public void startItem(String rp_endpoint, String rp_uuid, String rp_launch, String rp_project, String launchId,
            String testcaseName) throws ClientProtocolException, IOException, ParseException {
        ReportPortalClient.startItem(rp_endpoint, rp_uuid, rp_launch, rp_project, launchId, testcaseName, browserName, platform, iterationValue, description);
    }
    
    public void sendLog(String payloadfile,String rp_endpoint, String rp_uuid, String rp_launch, String rp_project, String testitemID,
            String status, String teststepdata, String filename) throws IOException, ParseException {
        ReportPortalClient.sendLog(payloadfile,rp_endpoint, rp_uuid, rp_launch, rp_project, testitemID, status, teststepdata, filename);
        
    }
}

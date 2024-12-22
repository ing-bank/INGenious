package com.ing.engine.reporting;

import com.ing.engine.constants.AppResourcePath;
import com.ing.engine.constants.FilePath;
import com.ing.engine.constants.SystemDefaults;
import com.ing.engine.core.Control;
import com.ing.engine.core.RunContext;
import com.ing.engine.core.RunManager;
import com.ing.engine.drivers.PlaywrightDriverCreation;
import com.ing.engine.reporting.impl.handlers.PrimaryHandler;
import com.ing.engine.reporting.impl.handlers.TestCaseHandler;
import com.ing.engine.reporting.impl.html.HtmlTestCaseHandler;
import com.ing.engine.reporting.intf.Report;
import com.ing.engine.reporting.util.DateTimeUtils;
import com.ing.engine.support.Status;
import com.ing.engine.support.Step;
import com.ing.engine.support.methodInf.MethodInfoManager;
import com.ing.engine.reporting.impl.azure.AzureTestCaseHandler;
import com.ing.engine.reporting.impl.rp.RPTestCaseHandler;
import com.ing.engine.reporting.impl.extent.ExtentTestCaseHandler;
import java.util.Date;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.io.FileWriter;
import org.json.simple.JSONObject;
import com.ing.engine.drivers.WebDriverCreation;

public final class TestCaseReport implements Report {

    public static volatile int tcCount;

    public String Scenario;
    public String TestCase;
    public String screenShotFileName;
    public String RequestFileName;
    public String ResponseFileName;
    public String LogFileName;
    private static final String FileExt = ".txt";
    private static final String LogFolderName = "logs";
    private StringBuilder sb;

    public boolean runComplete = false;

    int iterCounter = 0;
    int stepNo = 0;

    public final DateTimeUtils startTime;
    PlaywrightDriverCreation playwrightdriver;
    WebDriverCreation webDriver;

    Step curr;
    Status currentStatus;

    public PrimaryHandler primaryHandler;
    private final List<TestCaseHandler> handlers;
    private static String folderName = "webservice";

    public TestCaseReport() {
        ++tcCount;
        startTime = new DateTimeUtils();
        handlers = new ArrayList<>();
        if (isExtentEnabled()) {
            register(new ExtentTestCaseHandler(this), true);
        }
        if (isRPEnabled()) {
            register(new RPTestCaseHandler(this), true);
        }
        register(new HtmlTestCaseHandler(this), true);
        if (isAzureEnabled()) {
            register(new AzureTestCaseHandler(this), true);
        }
    }

    public boolean isExtentEnabled() {
        if (!RunManager.getGlobalSettings().isTestRun()) {
            return Control.getCurrentProject().getProjectSettings()
                    .getExecSettings(RunManager.getGlobalSettings().getRelease(), RunManager.getGlobalSettings().getTestSet()).getRunSettings().isExtentReport();
        }
        return false;
    }

    public boolean isRPEnabled() {
        if (!RunManager.getGlobalSettings().isTestRun()) {
            return Control.getCurrentProject().getProjectSettings()
                    .getExecSettings(RunManager.getGlobalSettings().getRelease(), RunManager.getGlobalSettings().getTestSet()).getRunSettings().isRPUpdate();
        }
        return false;
    }

    public boolean isAzureEnabled() {
        if (!RunManager.getGlobalSettings().isTestRun()) {
            return Control.getCurrentProject().getProjectSettings()
                    .getExecSettings(RunManager.getGlobalSettings().getRelease(), RunManager.getGlobalSettings().getTestSet()).getRunSettings().isAzureEnabled();
        }
        return false;
    }

    public void setPlaywrightDriver(PlaywrightDriverCreation driver) {
        playwrightdriver = driver;
        for (TestCaseHandler handler : handlers) {
            handler.setPlaywrightDriver(driver);
        }
    }

    public void setWebDriver(WebDriverCreation driver) {
        webDriver = driver;
        for (TestCaseHandler handler : handlers) {
            handler.setWebDriver(driver);
        }
    }

    /**
     * updates the current step details and resolves DESCRIPTION if not
     * available
     *
     * @param curr
     *
     */
    public void updateStepDetails(Step curr) {
        this.curr = curr;
        if (this.curr.Description == null || this.curr.Description.trim().isEmpty()) {
            this.curr.Description = MethodInfoManager.getResolvedDescriptionFor(curr.toTestStep());
        }
    }

    /**
     * initializes the test case report details
     *
     * @param runContext
     * @param runTime
     */
    @Override
    public synchronized void createReport(RunContext runContext, String runTime) {
        this.Scenario = runContext.Scenario;
        this.TestCase = runContext.TestCase;
        this.sb = new StringBuilder();
        this.sb.append(createRunInfoString(runContext.Scenario, runContext.TestCase, runContext.BrowserName, runContext.BrowserVersionValue, runContext.PlatformValue, runContext.Iteration));
        for (TestCaseHandler handler : handlers) {
            handler.createReport(runContext, runTime);
        }
    }
    //<editor-fold defaultstate="collapsed" desc="wrapper functions">

    public void updateTestLog(String stepName, String stepDescription, Status state) {
        if (state == Status.COMPLETE) {
            String location = File.separator + folderName;
            updateTestLog(stepName, stepDescription, state, location, null);
        } else {
            updateTestLog(stepName, stepDescription, state, null, null);
        }
    }

    public void updateTestLog(String stepName, String stepDescription, Status state, String optionalLink) {
        updateTestLog(stepName, stepDescription, state, optionalLink, null);
    }

    /**
     * updates the step results to the test case json DATA
     *
     * @param stepName
     * @param stepDescription
     * @param state
     * @param optional
     */
    public void updateTestLog(String stepName, String stepDescription, Status state, List<String> optional) {
        updateTestLog(stepName, stepDescription, state, null, optional);
    }
//</editor-fold>

    @Override
    public void updateTestLog(String stepName, String stepDescription, Status state,
            String optionalLink, List<String> optional) {
        currentStatus = state;
        stepNo++;
        setScreenShotName();
        String emoji = "";
        if(state.toString().contains("PASS"))
          emoji = "✅";
        if(state.toString().contains("FAIL"))
          emoji = "❌";
        if(state.toString().contains("DONE"))
          emoji = "🟢"; 
        if(state.toString().contains("WARNING"))
          emoji = "🟡"; 
        if(state.toString().contains("DEBUG"))
          emoji = "🔴"; 
        System.out.println(String.format("[%s]   | %s " + emoji, state, stepDescription));
        System.out.println(String.format("\n%99s\n", "=").replace(" ", "="));
        String stepInfo = stepLevelLog(String.valueOf(getStep().StepNum), getStep().ObjectName, getStep().Action, getStep().Input, getStep().Condition, state, stepDescription);
        this.sb.append(stepInfo).append("\n");
        for (TestCaseHandler handler : handlers) {
            handler.updateTestLog(stepName, stepDescription, state, optionalLink, optional);
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
        runComplete = true;
        for (TestCaseHandler handler : handlers) {
            handler.finalizeReport();
        }
        JSONObject testcasedata = (JSONObject) primaryHandler.getData();
        String testcase = testcasedata.get("testcaseName").toString();
        String scenario = testcasedata.get("scenarioName").toString();
        String eSteps = testcasedata.get("noTests").toString();
        String pSteps = testcasedata.get("nopassTests").toString();
        String fSteps = testcasedata.get("nofailTests").toString();
        String exeTime = testcasedata.get("exeTime").toString();

        this.sb.append(closingLog(scenario + ":" + testcase, eSteps, pSteps, fSteps, exeTime));
        log(this.sb.toString());
        //   log(System.getProperty("line.separator")+"Status:"+primaryHandler.getCurrentStatus());
        return (currentStatus = primaryHandler.getCurrentStatus());
    }

    private void setScreenShotName() {
        screenShotFileName = getNewScreenShotName();
    }

    //<editor-fold defaultstate="collapsed" desc="flow-control">
    /**
     * creates new iteration object
     *
     * @param iteration
     */
    @Override
    public void startIteration(int iteration) {
        stepNo = 0;
        iterCounter++;
        for (TestCaseHandler handler : handlers) {
            handler.startIteration(iteration);
        }
    }

    /**
     * creates new reusable object
     *
     * @param component
     * @param desc
     */
    @Override
    public void startComponent(String component, String desc) {
        for (TestCaseHandler handler : handlers) {
            handler.startComponent(component, desc);
        }
    }

    @Override
    public void endComponent(String component) {
        for (TestCaseHandler handler : handlers) {
            handler.endComponent(component);
        }
    }

    @Override
    public void endIteration(int iteration) {
        for (TestCaseHandler handler : handlers) {
            handler.endIteration(iteration);
        }
    }
//</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="external-API">
    /**
     *
     * @return the screen shot NAME for the current step
     */
    @Override
    public PlaywrightDriverCreation getPlaywrightDriver() {
        return playwrightdriver;
    }

    @Override
    public WebDriverCreation getWebDriver() {
        return webDriver;
    }

    public int getIter() {
        return iterCounter;
    }

    public Object getData() {
        return primaryHandler.getData();
    }

    public File getFile() {
        return primaryHandler.getFile();
    }

    public static synchronized int getTestCaseNumber() {
        return tcCount;
    }

    @Override
    public String getScreenShotName() {
        return screenShotFileName;
    }

    @Override
    public String getNewScreenShotName() {
        return File.separator
                + "img"
                + File.separator
                + Scenario
                + "_"
                + TestCase
                + "_Step-"
                + stepNo + "_"
                + DateTimeUtils.TimeNowForFolder()
                + ".png";
    }

    public String getWebserviceResponseFileName() {
        int currentStep = stepNo + 1;
        return File.separator
                + "webservice"
                + File.separator
                + Scenario
                + "_"
                + TestCase
                + "_Step-"
                + currentStep + "_"
                + "Response"
                + FileExt;
    }

    public String getWebserviceRequestFileName() {
        int currentStep = stepNo + 1;
        return File.separator
                + "webservice"
                + File.separator
                + Scenario
                + "_"
                + TestCase
                + "_Step-"
                + currentStep + "_"
                + "Request"
                + FileExt;
    }

    public String getPdfResultName() {
        return Scenario
                + "_"
                + TestCase
                + "_Step-"
                + stepNo + "_"
                + DateTimeUtils.TimeNowForFolder()
                + ".pdf";
    }

    public String getLogFileName() {
        return File.separator
                + Scenario
                + "_"
                + TestCase
                + FileExt;
    }

    @Override
    public File getReportLoc() {
        return new File(FilePath.getCurrentResultsPath());
    }

    @Override
    public Step getStep() {
        return curr;
    }

    public String getTestCaseName() {
        return TestCase;
    }

    public String getScenarioName() {
        return Scenario;
    }

    public Status getCurrentStatus() {
        return currentStatus;
    }

    public Boolean isStepPassed() {
        if (currentStatus != null) {
            return currentStatus.equals(Status.PASS) || currentStatus.equals(Status.DONE)
                    || currentStatus.equals(Status.SCREENSHOT) || currentStatus.equals(Status.WARNING)
                    || currentStatus.equals(Status.COMPLETE);
        }
        return false;
    }

    @Override
    public int getStepCount() {
        return stepNo;
    }

//</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="handler-registration">
    public void register(TestCaseHandler testCaseHandler) {
        if (!handlers.contains(testCaseHandler)) {
            handlers.add(testCaseHandler);
        }
    }

    public void register(TestCaseHandler testCaseHandler, boolean primaryHandler) {
        register(testCaseHandler);
        if (primaryHandler) {
            this.primaryHandler = (PrimaryHandler) testCaseHandler;
        }
    }
//</editor-fold>

    private String createRunInfoString(String Scenario, String TestCase, String Browser, String BrowserVersion, String Platform, String iteration) {
        String runInfo = "Run Information" + "\n"
                + "========================" + "\n"
                + "INGenious Playwright Studio                :  " + SystemDefaults.getBuildVersion() + "\n"
                + "java.runtime.name                          :  " + System.getProperty("java.runtime.name") + "\n"
                + "java.version                               :  " + System.getProperty("java.version") + "\n"
                + "java.home                                  :  " + System.getProperty("java.home") + "\n"
                + "os.name                                    :  " + System.getProperty("os.name") + "\n"
                + "os.arch                                    :  " + System.getProperty("os.arch") + "\n"
                + "os.version                                 :  " + System.getProperty("os.version") + "\n"
                + "file.encoding                              :  " + System.getProperty("file.encoding") + "\n"
                + "========================\n"
                + "Run Started on " + new Date().toString() + "\n\n"
                + "Scenario         :  [" + Scenario + "]\n"
                + "TestCase         :  [" + TestCase + "]\n"
                + "Browser          :  [" + Browser + "]\n"
                + "Browser Version  :  [" + BrowserVersion + "]\n"
                + "Platform         :  [" + Platform + "]\n"
                + "----------------------------------------------------------\n"
                + "Initializing Report\n"
                + "Running Iteration :  [" + iteration + "]\n\n";

        return runInfo;
    }

    private String stepLevelLog(String Step, String Object, String Action, String Input, String Condition, Status state, String stepDesc) {
        String stepInfo = String.format("\n%99s\n", "=").replace(" ", "=") + "\n"
                + "Step:" + String.valueOf(getStep().StepNum) + "  |  Object:" + getStep().ObjectName + "  |  Action:" + getStep().Action + "  |  Input:" + getStep().Input + "  |  Condition:" + getStep().Condition + "  | @" + DateTimeUtils.DateTimeNow() + "\n"
                + String.format("[%s]   | %s", state, stepDesc) + "\n";
        return stepInfo;
    }

    private String closingLog(String TestCase, String eSteps, String pSteps, String fSteps, String exeTime) {
        String closeInfo = "---------------------------------------------------" + "\n"
                + "Testcase Name        : " + TestCase + "\n"
                + "Executed Steps       : " + eSteps + "\n"
                + "Passed Steps         : " + pSteps + "\n"
                + "Failed Steps         : " + fSteps + "\n"
                + "Time Taken           : " + exeTime + "\n"
                + "---------------------------------------------------" + "\n";
        return closeInfo;
    }

    private void log(String info) {
        String path = AppResourcePath.getCurrentResultsPath() + File.separator + LogFolderName;
        String fileName = path + getLogFileName();
        File f = new File(fileName);
        FileWriter fr = null;

        try {
            if (!f.exists()) {
                new File(path).mkdir();
                f.createNewFile();
            }
            fr = new FileWriter(f, true);
            fr.write(info);
            fr.write(System.getProperty("line.separator"));
        } catch (Exception e) {
            System.out.println("Error in generation of seperate logs.");
            e.printStackTrace();
        } finally {
            try {
                fr.close();
            } catch (Exception e) {
            }
        }
    }

}

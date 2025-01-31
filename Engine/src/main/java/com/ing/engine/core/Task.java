package com.ing.engine.core;

import com.github.javafaker.Faker;
import com.ing.datalib.component.Project;
import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.settings.RunSettings;
import static com.ing.engine.commands.browser.Command.faker;
import com.ing.engine.constants.SystemDefaults;
import com.ing.engine.drivers.PlaywrightDriverCreation;
import com.ing.engine.execution.data.Parameter;
import com.ing.engine.execution.data.UserDataAccess;
import com.ing.engine.execution.exception.DriverClosedException;
import com.ing.engine.execution.exception.TestFailedException;
import com.ing.engine.execution.exception.UnCaughtException;
import com.ing.engine.execution.run.TestCaseRunner;
import com.ing.engine.reporting.TestCaseReport;
import com.ing.engine.reporting.util.DateTimeUtils;
import com.ing.engine.support.Status;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ing.engine.drivers.WebDriverCreation;
import java.util.Locale;

public class Task implements Runnable {

    TestCaseReport report;
    RunContext runContext;
    PlaywrightDriverCreation playwrightDriver;
    DateTimeUtils runTime;
    UserDataAccess userData;
    TestCaseRunner runner;
    WebDriverCreation webDriver;

    public Task(RunContext RC) {
        runContext = RC;
    }

    public Project project() {
        return Control.exe.getProject();
    }

    private static RunSettings getRunSettings() {
        return Control.exe.getExecSettings().getRunSettings();
    }

    @Override
    public void run() {
        runTime = new DateTimeUtils();
        report = new TestCaseReport();
        TestCase stc = getTestCase();
        if (stc != null) {
            runner = new TestCaseRunner(Control.exe, stc);
        } else {
            runner = new TestCaseRunner(Control.exe, runContext.Scenario,
                    runContext.TestCase);
        }
        report.createReport(runContext, DateTimeUtils.DateTimeNow());
        int iter = 1;
        Date startexecDate = new Date();
        if (RunManager.getGlobalSettings().isTestRun()) {
            runner.setMaxIter(1);
        } else {
            runner.setMaxIter(Parameter.resolveMaxIter(runContext.Iteration));
            iter = Parameter.resolveStartIter(runContext.Iteration);
        }

        while (!SystemDefaults.stopExecution.get() && iter <= runner.getMaxIter()) {
            try {
                System.out.println("👉 Running Iteration " + iter);
                runIteration(iter++);
                if (isPlaywrightExecution()) {
                    closePlaywrightInstance(iter - 1);
                }
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
        Date endEexcDate = new Date();

        if (report != null) {
            Status s = report.finalizeReport();
            Control.ReportManager.startDate = startexecDate;
            Control.ReportManager.endDate = endEexcDate;
            Control.ReportManager.updateTestCaseResults(runContext, report, s, runTime.timeRun());
            SystemDefaults.reportComplete.set(false);
        }
    }

    private void closePlaywrightInstance(int iter) {
        String browserName = playwrightDriver.getCurrentBrowser();
        if (playwrightDriver != null) {
            playwrightDriver.closeBrowser();
            playwrightDriver.playwright.close();
        }
        String closureConfirmationText = "Playwright instance with [" + browserName + "] has been closed for Iteration : " + iter;
        System.out.println("\n");
        for (int i = 0; i < closureConfirmationText.length() + 7; i++) {
            System.out.print("-");
        }
        System.out.println();
        System.out.println("| " + closureConfirmationText + " |");
        for (int i = 0; i < closureConfirmationText.length() + 7; i++) {
            System.out.print("-");
        }
        System.out.println("\n");
    }

    private TestCase getTestCase() {
        try {
            Scenario scn = project().getScenarioByName(runContext.Scenario);
            if (scn != null) {
                TestCase stc = scn.getTestCaseByName(runContext.TestCase);
                if (stc != null) {
                    return stc;
                } else {
                    LOG.log(Level.WARNING, "Testcase [{0}] not found", runContext.Scenario);
                }
            } else {
                LOG.log(Level.WARNING, "Scenario [{0}] not found", runContext.Scenario);
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Unable to load TestaCase", ex);
        }
        return null;
    }
    private static final Logger LOG = Logger.getLogger(Task.class.getName());

    public boolean runIteration(int iter) {
        boolean success = false;
        try {
            SystemDefaults.reportComplete.set(true);
            report.startIteration(iter);
            faker.put(runContext.Scenario + runContext.TestCase, new Faker(new Locale("en-US")));
            if (isPlaywrightExecution()) {
                playwrightDriver = getPlaywrightDriver();
                launchPlaywright();
            } else  {
                webDriver = getWebDriver();
                launchWebDriver();
            }
            SystemDefaults.stopCurrentIteration.set(false);
            runner.run(createControl(), iter);
            success = true;
        } catch (DriverClosedException ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
            report.updateTestLog("DriverClosedException", ex.getMessage(), Status.FAILNS);
        } catch (TestFailedException ex) {
            onFail(ex, ex.getMessage(), Status.DEBUG);
        } catch (UnCaughtException ex) {
            onError(ex, "Unhandled Error", ex.getMessage());
        } catch (Throwable ex) {
            onError(ex, "Error", ex.getMessage());
        } finally {
            if (isPlaywrightExecution()) {
              closePlaywrightDriver();
            }
            else {
                closeWebDriver();
            }
                
            report.endIteration(iter);
        }

        return success;
    }

    private void closePlaywrightDriver() {
        if (playwrightDriver != null && !getRunSettings().useExistingDriver()) {
            try {
                playwrightDriver.closeBrowser();
            } catch (Exception ex) {
                System.out.println("Driver Closed Unexpectedly");
                onError(ex, "Driver Error", ex.getMessage());
                LOG.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
    }
    
    private void closeWebDriver() {
        if (webDriver.driver != null && !getRunSettings().useExistingDriver()) {
            try {
                webDriver.driver.quit();
            } catch (Exception ex) {
                System.out.println("Driver Closed Unexpectedly");
                onError(ex, "Driver Error", ex.getMessage());
                LOG.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
    }

    private void launchPlaywright() throws UnCaughtException {
        if (!getRunSettings().useExistingDriver() || playwrightDriver.page == null) {
            playwrightDriver.launchDriver(runContext);
        }
        report.setPlaywrightDriver(playwrightDriver);
    }

    private void launchWebDriver() throws UnCaughtException {
        if (!getRunSettings().useExistingDriver() || webDriver.driver == null) {
            webDriver.launchDriver(runContext);
        }
        report.setWebDriver(webDriver);
    }

    private CommandControl createControl() {
        return new CommandControl(playwrightDriver, playwrightDriver, playwrightDriver, webDriver, report) {
            @Override
            public void execute(String com, int sub) {
                runner.runTestCase(com, sub);
            }

            @Override
            public void executeAction(String action) {
                runner.runAction(action);
            }

            @Override
            public Object context() {
                return runner;
            }
        };
    }

    private void onError(Throwable ex, String err, String desc) {
        onError(ex, err, desc, Status.DEBUG);
    }

    private void onFail(Throwable ex, String desc, Status s) {
        onError(ex, "[Breaking execution!]", desc, s);
    }

    private void onError(Throwable ex, String err, String desc, Status s) {
        if (ex != null) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
        }
        if (report != null) {
            report.updateTestLog(err, desc, s);
        }
    }

    private PlaywrightDriverCreation getPlaywrightDriver() {
        PlaywrightDriverCreation playwrightDriver;
        if (!getRunSettings().useExistingDriver()
                || Control.getPlaywrightDriver() == null) {
            playwrightDriver = new PlaywrightDriverCreation();
            Control.setPlaywrightDriver(playwrightDriver);
        } else {
            playwrightDriver = Control.getPlaywrightDriver();
        }
        return playwrightDriver;
    }

    private WebDriverCreation getWebDriver() {
        WebDriverCreation webDriver;
        if (!getRunSettings().useExistingDriver()
                || Control.getWebDriver() == null) {
            webDriver = new WebDriverCreation();
            Control.setWebDriver(webDriver);
        } else {
            webDriver = Control.getWebDriver();
        }
        return webDriver;
    }

    public boolean isPlaywrightExecution() {
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


    public boolean isWebDriverExecution() {
         return !isPlaywrightExecution();
    }

}

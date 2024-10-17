package com.ing.engine.core;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.settings.RunSettings;
import com.ing.engine.constants.SystemDefaults;
import com.ing.engine.drivers.PlaywrightDriver;
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

//Added for Mobile
import com.ing.engine.drivers.MobileDriver;

public class Task implements Runnable {

    TestCaseReport report;
    RunContext runContext;
    PlaywrightDriver playwrightDriver;
    DateTimeUtils runTime;
    UserDataAccess userData;
    TestCaseRunner runner;
    MobileDriver mobileDriver;

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
                System.out.println("Running Iteration " + iter);
                runIteration(iter++);
                if(isBrowserExecution())
                closePlaywrightInstance(iter-1);
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
//        mobileDriver=getMobileDriver();
//        launchEmulator();
         if(isMobileExecution())
        {
            mobileDriver=getMobileDriver();
            try {
            SystemDefaults.reportComplete.set(true);
            report.startIteration(iter);
            launchEmulator();
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
            report.endIteration(iter);
        }
        return success;

        }
        if(isBrowserExecution())
        {
        playwrightDriver = getDriver();
        try {
            SystemDefaults.reportComplete.set(true);
            report.startIteration(iter);
            launchBrowser();
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
            if (playwrightDriver != null && !getRunSettings().useExistingDriver()) {
                try {
                    playwrightDriver.closeBrowser();
                } catch (Exception ex) {
                    System.out.println("Driver Closed Unexpectedly");
                    onError(ex, "Driver Error", ex.getMessage());
                    LOG.log(Level.SEVERE, ex.getMessage(), ex);
                }
            }
            report.endIteration(iter);
        }
        return success;
                }

        return success;
    }
 
    private void launchBrowser() throws UnCaughtException {
        if (!getRunSettings().useExistingDriver() || playwrightDriver.page == null) {
            playwrightDriver.launchDriver(runContext);
        }
        report.setDriver(playwrightDriver);
    }
    
        private void launchEmulator() throws UnCaughtException {
        if (!getRunSettings().useExistingDriver() || mobileDriver.driver == null) {
            mobileDriver.launchDriver(runContext);
        }
        report.setMobileDriver(mobileDriver);
    }

    private CommandControl createControl() {
        return new CommandControl(playwrightDriver, playwrightDriver, playwrightDriver, mobileDriver, report) {
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

    private PlaywrightDriver getDriver() {
        PlaywrightDriver seDriver;
        if (!getRunSettings().useExistingDriver()
                || Control.getPlaywrightDriver() == null) {
            seDriver = new PlaywrightDriver();
            Control.setSeDriver(seDriver);
        } else {
            seDriver = Control.getPlaywrightDriver();
        }
        return seDriver;
    }
    
      private MobileDriver getMobileDriver() {
        MobileDriver mobileDriver;
        if (!getRunSettings().useExistingDriver()
                || Control.getMobileDriver() == null) {
            mobileDriver=new MobileDriver();
//            mobileDriver=mobileDriver.launchDriver(runContext);
            Control.setMobileDriver(mobileDriver);
        } else {
            mobileDriver = Control.getMobileDriver();
        }
        return mobileDriver;
    }

         public boolean isBrowserExecution() {
        boolean isBrowserExecution = false;
        try {
            String browserName = runContext.BrowserName;
            if (browserName.equals("Chromium") || browserName.equals("Webkit") || browserName.equals("Firefox")) {
                isBrowserExecution = true;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return isBrowserExecution;
    }

    public boolean isNoBrowserExecution() {
        boolean isNoBrowserExecution = false;
        try {
            String browserName = runContext.BrowserName;
            if (browserName.equals("NoBrowser")) {
                isNoBrowserExecution = true;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return isNoBrowserExecution;
    }

    public boolean isMobileExecution() {
        boolean isMobileExecution = false;
        try {
            if (!isBrowserExecution() && !isNoBrowserExecution()) {
                isMobileExecution = true;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return isMobileExecution;
    }


}


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

public class Task implements Runnable {

    TestCaseReport report;
    RunContext runContext;
    PlaywrightDriver playwrightDriver;
    DateTimeUtils runTime;
    UserDataAccess userData;
    TestCaseRunner runner;

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

    private void launchBrowser() throws UnCaughtException {
        if (!getRunSettings().useExistingDriver() || playwrightDriver.page == null) {
            playwrightDriver.launchDriver(runContext);
        }
        report.setDriver(playwrightDriver);
    }

    private CommandControl createControl() {
        return new CommandControl(playwrightDriver,playwrightDriver,playwrightDriver, report) {
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

}

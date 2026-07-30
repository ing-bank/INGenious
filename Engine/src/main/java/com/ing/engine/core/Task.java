package com.ing.engine.core;

import static com.ing.engine.commands.browser.Command.faker;

import com.github.javafaker.Faker;
import com.ing.datalib.component.Project;
import com.ing.datalib.component.ReusableRef;
import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.settings.RunSettings;
import com.ing.engine.constants.SystemDefaults;
import com.ing.engine.drivers.PlaywrightDriverCreation;
import com.ing.engine.drivers.SAPSessionCreation;
import com.ing.engine.drivers.WebDriverCreation;
import com.ing.engine.execution.data.Parameter;
import com.ing.engine.execution.data.UserDataAccess;
import com.ing.engine.execution.exception.DriverClosedException;
import com.ing.engine.execution.exception.TestFailedException;
import com.ing.engine.execution.exception.UnCaughtException;
import com.ing.engine.execution.exception.data.DataNotFoundException;
import com.ing.engine.execution.run.TestCaseRunner;
import com.ing.engine.reporting.TestCaseReport;
import com.ing.engine.reporting.util.DateTimeUtils;
import com.ing.ingenious.api.status.Status;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openqa.selenium.JavascriptExecutor;

public class Task implements Runnable {
    TestCaseReport report;
    RunContext runContext;
    PlaywrightDriverCreation playwrightDriver;
    DateTimeUtils runTime;
    UserDataAccess userData;
    TestCaseRunner runner;
    WebDriverCreation webDriver;
    SAPSessionCreation session;

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
            // Running a Project/Shared reusable standalone (not nested under an "Execute"
            // step) makes it its OWN root - without this, its resolved scope would stay
            // null and data lookups could match a same-named row from the other scope.
            runner.setResolvedReusableScope(resolvedScopeOf(stc));
        } else {
            runner = new TestCaseRunner(Control.exe, runContext.Scenario, runContext.TestCase);
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
                runIteration(iter++);
                if (isPlaywrightExecution() && isLocalExecution()) {
                    closePlaywrightInstance(iter - 1);
                }
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
        Date endEexcDate = new Date();

        if (report != null) {
            Status s = report.finalizeReport();
            //setLambdaTags();
            if (!isLocalExecution()) {
                if (s.toString().equals("PASS")) setLambdaStatus(
                    "passed",
                    ""
                ); else setLambdaStatus("failed", "");
            }
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
        System.out.println("Playwright [" + browserName + "] closed for Iteration " + iter);
    }

    private TestCase getTestCase() {
        try {
            // When the caller knows exactly which scope this run was requested from
            // (e.g. the IDE run button on an open Project/Shared reusable tab), look up
            // that scope directly. This avoids Project.getScenarioByName(), which searches
            // Test Plan + Project Reusable + Shared Reusable combined and would otherwise
            // silently return the wrong scope's test case when names collide.
            if ("PROJECT".equalsIgnoreCase(runContext.ReusableScope)) {
                return testCaseFrom(
                    project().getReusableScenarioByName(runContext.Scenario),
                    "project reusable"
                );
            }
            if ("SHARED".equalsIgnoreCase(runContext.ReusableScope)) {
                return testCaseFrom(
                    project().getSharedReusableScenarioByName(runContext.Scenario),
                    "shared reusable"
                );
            }

            // No scope hint (e.g. a CLI run by name only): fall back to an explicit
            // Test Plan -> Project Reusable -> Shared Reusable priority search.
            TestCase stc = testCaseFrom(
                project().getTestPlanScenarioByName(runContext.Scenario),
                "test plan"
            );
            if (stc != null) {
                return stc;
            }

            TestCase stcR = testCaseFrom(
                project().getReusableScenarioByName(runContext.Scenario),
                "project reusable"
            );
            if (stcR != null) {
                return stcR;
            }

            TestCase stcS = testCaseFrom(
                project().getSharedReusableScenarioByName(runContext.Scenario),
                "shared reusable"
            );
            if (stcS != null) {
                return stcS;
            }

            // Nothing matched — produce a clearer warning listing where we looked
            LOG.log(
                Level.WARNING,
                "Testcase [{0}] not found in scenario [{1}] (searched test plan, project reusable and shared reusable)",
                new Object[] { runContext.TestCase, runContext.Scenario }
            );
            return null;
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Unable to load TestCase", ex);
            return null;
        }
    }

    private TestCase testCaseFrom(Scenario scn, String scopeLabel) {
        if (scn == null) {
            return null;
        }
        TestCase stc = scn.getTestCaseByName(runContext.TestCase);
        if (stc == null) {
            LOG.log(
                Level.FINE,
                "Testcase [{0}] not found in {1} scenario [{2}]",
                new Object[] { runContext.TestCase, scopeLabel, runContext.Scenario }
            );
        }
        return stc;
    }

    /**
     * Determines the reusable scope of a resolved TestCase from its owning scenario's
     * source, so a standalone run of a Project/Shared reusable carries the same scope
     * information that a nested "Execute" call would resolve for it.
     *
     * @param stc the resolved test case
     * @return PROJECT/SHARED for a reusable component, null for a Test Plan test case
     */
    private ReusableRef.Scope resolvedScopeOf(TestCase stc) {
        Scenario scn = stc.getScenario();
        if (scn == null) {
            return null;
        }
        switch (scn.getSource()) {
            case REUSABLE_COMPONENTS:
                return ReusableRef.Scope.PROJECT;
            case SHARED_REUSABLE_COMPONENTS:
                return ReusableRef.Scope.SHARED;
            case TEST_PLAN:
            default:
                return null;
        }
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
            } else if (isSAPExecution()) {
                session = getSAPSession();
                launchSap();
            } else {
                webDriver = getWebDriver();
                launchWebDriver();
            }
            SystemDefaults.stopCurrentIteration.set(false);
            runner.run(createControl(), iter);
            success = true;
        } catch (DataNotFoundException ex) {
            if (!ex.cause.isEndData()) {
                LOG.log(Level.SEVERE, ex.getMessage(), ex);
                report.updateTestLog("DataNotFoundException", ex.getMessage(), Status.DEBUG);
            }
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
            } else if (isSAPExecution()) {
                // Do nothing
            } else {
                if (webDriver.isLambdaTestExecutionPlatform()) {
                    JavascriptExecutor js = (JavascriptExecutor) webDriver.driver;
                    if (report.finalizeReport().toString().equalsIgnoreCase("PASS")) {
                        js.executeScript("lambda-status=passed");
                    } else {
                        js.executeScript("lambda-status=failed");
                    }
                }
                closeWebDriver();
            }

            report.endIteration(iter);
        }

        return success;
    }

    private void closePlaywrightDriver() {
        if (
            playwrightDriver != null && !getRunSettings().useExistingDriver() && isLocalExecution()
        ) {
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

    private void launchPlaywright() throws UnCaughtException, UnsupportedEncodingException {
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

    private void launchSap() throws UnCaughtException {
        if (!getRunSettings().useExistingDriver() || session.session == null) {
            session.launchSession(runContext);
        }
        report.setSapSession(session);
    }

    private CommandControl createControl() {
        return new CommandControl(
            playwrightDriver,
            playwrightDriver,
            playwrightDriver,
            webDriver,
            session,
            report
        ) {

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
        if (!getRunSettings().useExistingDriver() || Control.getPlaywrightDriver() == null) {
            playwrightDriver = new PlaywrightDriverCreation();
            Control.setPlaywrightDriver(playwrightDriver);
        } else {
            playwrightDriver = Control.getPlaywrightDriver();
        }
        return playwrightDriver;
    }

    private WebDriverCreation getWebDriver() {
        WebDriverCreation webDriver;
        if (!getRunSettings().useExistingDriver() || Control.getWebDriver() == null) {
            webDriver = new WebDriverCreation();
            Control.setWebDriver(webDriver);
        } else {
            webDriver = Control.getWebDriver();
        }
        return webDriver;
    }

    private SAPSessionCreation getSAPSession() {
        SAPSessionCreation sapSession;
        if (!getRunSettings().useExistingDriver() || Control.getSapSession() == null) {
            session = new SAPSessionCreation();
            Control.setSapSession(session);
        } else {
            session = Control.getSapSession();
        }
        return session;
    }

    public boolean isLocalExecution() {
        return !Control.exe.getExecSettings().getRunSettings().isGridExecution();
    }

    public boolean isPlaywrightExecution() {
        boolean isBrowserExecution = false;
        try {
            String browserName = runContext.BrowserName;
            if (
                browserName.equals("Chromium") ||
                browserName.equals("WebKit") ||
                browserName.equals("Firefox")
            ) {
                isBrowserExecution = true;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return isBrowserExecution;
    }

    public boolean isSAPExecution() {
        boolean isSAPExecution = false;
        try {
            String browserName = runContext.BrowserName;
            if (browserName.equals("SAP")) {
                isSAPExecution = true;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return isSAPExecution;
    }

    public boolean isWebDriverExecution() {
        return !isPlaywrightExecution();
    }

    public void setLambdaStatus(String status, String remark) {
        if (playwrightDriver != null && playwrightDriver.page != null) {
            playwrightDriver.page.evaluate(
                "_ => {}",
                "lambdatest_action: { \"action\": \"setTestStatus\", \"arguments\": { \"status\": \"" +
                status +
                "\", \"remark\": \"" +
                remark +
                "\"}}"
            );
        }
    }
}

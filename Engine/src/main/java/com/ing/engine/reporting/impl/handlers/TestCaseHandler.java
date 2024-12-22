package com.ing.engine.reporting.impl.handlers;

import com.ing.engine.core.RunContext;
import com.ing.engine.drivers.PlaywrightDriverCreation;
import com.ing.engine.reporting.TestCaseReport;
import com.ing.engine.reporting.intf.Report;
import com.ing.engine.support.Status;
import com.ing.engine.support.Step;
import java.io.File;
import java.util.List;
import com.ing.engine.drivers.WebDriverCreation;

/**
 *
 *
 */
public class TestCaseHandler implements Report {

    public TestCaseReport report;

    public TestCaseHandler(TestCaseReport report) {
        this.report = report;
    }

    @Override
    public void startComponent(String component, String desc) {

    }

    @Override
    public void startIteration(int iteration) {

    }

    @Override
    public void endComponent(String string) {

    }

    @Override
    public void endIteration(int iteration) {

    }

    @Override
    public PlaywrightDriverCreation getPlaywrightDriver() {
        return report.getPlaywrightDriver();
    }

    @Override
    public WebDriverCreation getWebDriver() {
        return report.getWebDriver();
    }
    
    @Override
    public String getScreenShotName() {
        return report.getScreenShotName();
    }

    @Override
    public String getNewScreenShotName() {
        return report.getNewScreenShotName();
    }

    @Override
    public File getReportLoc() {
        return report.getReportLoc();
    }

    @Override
    public void createReport(RunContext runContext, String runTime) {
    }

    @Override
    public void updateTestLog(String stepName, String stepDescription, Status state, String link, List<String> links) {

    }

    @Override
    public Step getStep() {
        return report.getStep();
    }

    @Override
    public int getStepCount() {
        return report.getStepCount();
    }

    @Override
    public Status finalizeReport() {
        return null;
    }

    public void setPlaywrightDriver(PlaywrightDriverCreation driver) {

    }

    public void setWebDriver(WebDriverCreation driver) {

    }
}

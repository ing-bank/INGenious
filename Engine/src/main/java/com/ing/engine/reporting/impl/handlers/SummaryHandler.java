
package com.ing.engine.reporting.impl.handlers;

import com.ing.engine.core.RunContext;
import com.ing.engine.reporting.SummaryReport;
import com.ing.engine.reporting.TestCaseReport;
import com.ing.engine.reporting.intf.OverviewReport;
import com.ing.engine.reporting.performance.har.Har;
import com.ing.engine.support.Status;

/**
 *
 * 
 */
public class SummaryHandler implements OverviewReport {

    public SummaryReport report;

    public SummaryHandler(SummaryReport report) {
        this.report = report;
    }

    @Override
    public void createReport(String runTime,int size) {
   
    }

    @Override
    public void updateTestCaseResults(String testScenario, String testCase,
            String Iteration, String testDescription, String executionTime, 
            String fileName, Status state, String Browser) {
    }

    @Override
    public void updateTestCaseResults(RunContext runContext, TestCaseReport report,
            Status state, String executionTime) {
    }

    @Override
    public void finalizeReport() {

    }

    @SuppressWarnings("rawtypes")
	public void addHar(Har<String, Har.Log> h, TestCaseReport report, String pageName) {

    }

}

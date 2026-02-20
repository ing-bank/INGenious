
package com.ing.engine.reporting.intf;

import com.ing.engine.core.RunContext;
import com.ing.engine.reporting.TestCaseReport;
import com.ing.ingenious.api.status.Status;

public interface OverviewReport {

    public void createReport(String runTime,int size);

    public void updateTestCaseResults(String testScenario, String testCase,
            String Iteration, String testDescription, String executionTime,
            String fileName, Status state, String Browser);

    public void updateTestCaseResults(RunContext runContext,
            TestCaseReport report, Status state, String executionTime);

    public void finalizeReport() throws Exception;

}

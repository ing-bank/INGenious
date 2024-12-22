
package com.ing.engine.reporting.impl.sync;

import com.ing.datalib.settings.RunSettings;
import com.ing.engine.constants.FilePath;
import com.ing.engine.core.Control;
import com.ing.engine.core.RunContext;
import com.ing.engine.reporting.SummaryReport;
import com.ing.engine.reporting.TestCaseReport;
import com.ing.engine.reporting.impl.handlers.SummaryHandler;
import com.ing.engine.reporting.sync.sapi.ApiLink;
import com.ing.engine.support.Status;
import java.net.MalformedURLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONObject;

/**
 *
 * 
 */
public class SAPISummaryHandler extends SummaryHandler {

    public ApiLink sapi_Link;
    private int PassedTestCases;
    private int FailedTestCases;

    public SAPISummaryHandler(SummaryReport report) {
        super(report);
        try {
            sapi_Link = new ApiLink();
        } catch (MalformedURLException ex) {
        }
    }

    /**
     * initialize the report data file.
     *
     * @param runTime
     * @param size
     */
    @Override
    public synchronized void createReport(String runTime, int size) {

        try {
            if (sapi_Link != null) {
                sapi_Link.setThreads(getRunSettings().getThreadCount());
                sapi_Link.setStartTime(runTime);
                sapi_Link.setIterMode(getRunSettings().getIterationMode());
                sapi_Link.setExeMode(getRunSettings().getExecutionMode());
                sapi_Link.setNoTests(size);
                sapi_Link.setRunName(FilePath.getCurrentReportFolderName());
                sapi_Link.clientData((JSONObject) report.pHandler.getData());
                sapi_Link.update();
            } else {
                
            }
        } catch (Exception ex) {
            Logger.getLogger(SAPISummaryHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static RunSettings getRunSettings() {
        return Control.exe.getExecSettings().getRunSettings();
    }

    /**
     * update the result of each test case result
     *
     * @param runContext
     * @param report
     * @param state
     * @param executionTime
     */
    @Override
    public synchronized void updateTestCaseResults(RunContext runContext,
            TestCaseReport report, Status state, String executionTime) {
        if (sapi_Link != null) {
            if (state.equals(Status.PASS)) {
                PassedTestCases++;
            } else {
                FailedTestCases++;
            }
            System.out.println(String.format("Updating results to SAPI, Passed : [%s] : Failed [%s]",
                    PassedTestCases, FailedTestCases));
            sapi_Link.update(PassedTestCases, FailedTestCases);
        }

    }

}

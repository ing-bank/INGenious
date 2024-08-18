
package com.ing.engine.reporting.impl.slack;

import com.ing.engine.core.Control;
import com.ing.engine.core.RunContext;
import com.ing.engine.core.RunManager;
import com.ing.engine.reporting.SummaryReport;
import com.ing.engine.reporting.TestCaseReport;
import com.ing.engine.reporting.impl.handlers.PrimaryHandler;
import com.ing.engine.reporting.impl.handlers.SummaryHandler;
import com.ing.engine.reporting.util.DateTimeUtils;
import com.ing.engine.support.Status;
import com.ing.notification.slack.Slack;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class SlackSummaryHandler extends SummaryHandler implements PrimaryHandler {

    private static final Logger LOGGER = Logger.getLogger(SlackSummaryHandler.class.getName());
    JSONObject testSetData = new JSONObject();
    int FailedTestCases = 0;
    int PassedTestCases = 0;
    public boolean RunComplete = false;
    DateTimeUtils RunTime;
    int noTests = 0;

    public SlackSummaryHandler(SummaryReport report) {
        super(report);
    }

    @Override
    public Object getData() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public File getFile() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Status getCurrentStatus() {
        if (FailedTestCases > 0 || PassedTestCases == 0) {
            return Status.FAIL;
        } else {
            return Status.PASS;
        }
    }

    @Override
    public void updateTestCaseResults(String testScenario, String testCase, String Iteration, String testDescription,
            String executionTime, String fileName, Status state, String Browser) {
        if (state.equals(Status.PASS)) {
            PassedTestCases++;
        } else {
            FailedTestCases++;
        }
    }

    /**
     * update the result of each test case result
     *
     * @param runContext
     * @param report
     * @param state
     * @param executionTime
     */
    @SuppressWarnings("unchecked")
    @Override
    public synchronized void updateTestCaseResults(RunContext runContext, TestCaseReport report, Status state,
            String executionTime) {
        String status;
        if (state.equals(Status.PASS)) {
            status = "Passed";
            PassedTestCases++;
        } else {
            FailedTestCases++;
            status = "Failed";
        }
    }

    /**
     * finalize the summary report creation
     */
    @Override
    public synchronized void finalizeReport() {
        RunComplete = true;
        updateResults();
        try {
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    private void updateResults() {
        if (!RunManager.getGlobalSettings().isTestRun()) {
            if (Control.getCurrentProject().getProjectSettings().getExecSettings(RunManager.getGlobalSettings().getRelease(), RunManager.getGlobalSettings().getTestSet()).getRunSettings().isSendNotification()) {
                String exeTime = RunTime.timeRun();
                String endTime = DateTimeUtils.DateTimeNow();
                try {
                    if (RunComplete) {
                        byte[] bytes = endExecution(getCurrentStatus(), exeTime, endTime, FailedTestCases, PassedTestCases).getBytes();
                        if (Slack.sendNotification(bytes)) {
                            System.out.println("Execution end notification to Slack for project " + RunManager.getGlobalSettings().getProjectName() + " and testset " + RunManager.getGlobalSettings().getTestSet() + "run successfully");
                        }
                    } else {
                    }
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized void createReport(String runTime, int size) {
        if (!RunManager.getGlobalSettings().isTestRun()) {
            if (Control.getCurrentProject().getProjectSettings().getExecSettings(RunManager.getGlobalSettings().getRelease(), RunManager.getGlobalSettings().getTestSet()).getRunSettings().isSendNotification()) {
                try {
                    RunTime = new DateTimeUtils();
                    byte[] bytes = startExecution(runTime, size).getBytes();
                    if (Slack.sendNotification(bytes)) {
                        System.out.println("Execution start notification to Slack for project " + RunManager.getGlobalSettings().getProjectName() + " and testset " + RunManager.getGlobalSettings().getTestSet() + "run successfully");
                    }
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                }
            }
        }
    }

    private static String startExecution(String runTime, int size) {
        JSONObject startExecution = common();
        startExecution.put("attachments", loadfields(runTime, size));
        return startExecution.toJSONString();
    }

    private static String endExecution(Status s, String timeRun, String endTime, int FailedTestCases, int PassedTestCases) {
        JSONObject executionData = common();
        executionData.put("attachments", loadEndfields(s, timeRun, endTime, FailedTestCases, PassedTestCases));
        return executionData.toJSONString();
    }

    private static JSONObject common() {
        JSONObject commonData = new JSONObject();
        commonData.put("text", "*INGenious Run Report*");
        commonData.put("mrkdwn", "true");
        return commonData;
    }

    private static JSONArray loadfields(String runTime, int size) {
        JSONArray attachfields = new JSONArray();
        JSONObject attach = new JSONObject();
        attach.put("text", "Test Execution Report");

        JSONObject project = new JSONObject();
        project.put("title", "Project");
        project.put("value", RunManager.getGlobalSettings().getProjectName());
        project.put("short", true);

        JSONObject release = new JSONObject();
        release.put("title", "Release");
        release.put("value", RunManager.getGlobalSettings().getRelease());
        release.put("short", true);

        JSONObject testset = new JSONObject();
        testset.put("title", "Test suite");
        testset.put("value", RunManager.getGlobalSettings().getTestSet());
        testset.put("short", true);

        JSONObject iterMode = new JSONObject();
        iterMode.put("title", "Iteration Mode");
        iterMode.put("value", Control.exe.getExecSettings().getRunSettings().getIterationMode());
        iterMode.put("short", true);

        JSONObject runConfig = new JSONObject();
        runConfig.put("title", "Execution Mode");
        runConfig.put("value", Control.exe.getExecSettings().getRunSettings().getExecutionMode());
        runConfig.put("short", true);

        JSONObject threads = new JSONObject();
        threads.put("title", "Max Threads");
        threads.put("value", Control.exe.getExecSettings().getRunSettings().getThreadCount());
        threads.put("short", true);

        JSONObject startTime = new JSONObject();
        startTime.put("title", "Start Time");
        startTime.put("value", runTime);
        startTime.put("short", true);

        JSONObject sizeobj = new JSONObject();
        sizeobj.put("title", "No of Tests");
        sizeobj.put("value", size);
        sizeobj.put("short", true);

        JSONObject execution = new JSONObject();
        execution.put("title", "Execution Status");
        execution.put("value", "Started");
        execution.put("short", true);

        JSONArray fields = new JSONArray();
        fields.add(project);
        fields.add(release);
        fields.add(testset);
        fields.add(iterMode);
        fields.add(runConfig);
        fields.add(threads);
        fields.add(startTime);
        fields.add(sizeobj);
        fields.add(execution);

        attach.put("fields", fields);
        attachfields.add(attach);
        return attachfields;
    }

    private static JSONArray loadEndfields(Status s, String timeRun, String endtime, int FailedTestCases, int PassedTestCases) {
        JSONArray attachfields = new JSONArray();
        JSONObject attach = new JSONObject();
        attach.put("text", "Test Execution Report :memo:");
        attach.put("color", "#D3D3D3");

        JSONObject project = new JSONObject();
        project.put("title", "Project");
        project.put("value", RunManager.getGlobalSettings().getProjectName());
        project.put("short", true);

        JSONObject release = new JSONObject();
        release.put("title", "Release");
        release.put("value", RunManager.getGlobalSettings().getRelease());
        release.put("short", true);

        JSONObject testset = new JSONObject();
        testset.put("title", "Test suite");
        testset.put("value", RunManager.getGlobalSettings().getTestSet());
        testset.put("short", true);

        JSONObject iterMode = new JSONObject();
        iterMode.put("title", "Iteration Mode");
        iterMode.put("value", Control.exe.getExecSettings().getRunSettings().getIterationMode());
        iterMode.put("short", true);

        JSONObject runConfig = new JSONObject();
        runConfig.put("title", "Execution Mode");
        runConfig.put("value", Control.exe.getExecSettings().getRunSettings().getExecutionMode());
        runConfig.put("short", true);

        JSONObject threads = new JSONObject();
        threads.put("title", "Max Threads");
        threads.put("value", Control.exe.getExecSettings().getRunSettings().getThreadCount());
        threads.put("short", true);

        JSONObject timeTaken = new JSONObject();
        timeTaken.put("title", "Time Taken");
        timeTaken.put("value", timeRun);
        timeTaken.put("short", true);

        JSONObject endTime = new JSONObject();
        endTime.put("title", "End Time");
        endTime.put("value", endtime);
        endTime.put("short", true);

        JSONArray fields = new JSONArray();
        fields.add(project);
        fields.add(release);
        fields.add(testset);
        fields.add(iterMode);
        fields.add(runConfig);
        fields.add(threads);
        fields.add(timeTaken);
        fields.add(endTime);

        attach.put("fields", fields);
        
        JSONObject passed = new JSONObject();
        passed.put("color", "#008000");
        passed.put("text", "*Passed :* "+PassedTestCases);
        
        JSONObject failed = new JSONObject();
        failed.put("color", "#FF0000");
        failed.put("text", "*Failed :* "+FailedTestCases);
        
        attachfields.add(passed);
        attachfields.add(failed);
        attachfields.add(attach);
        return attachfields;
    }

    private static String getColor(Status s) {
        if (s.toString().equals("PASS")) {
            return "#008000";
        } else {
            return "#FF0000";
        }
    }

}

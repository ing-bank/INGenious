package com.ing.engine.reporting.impl.azure;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import org.json.simple.JSONObject;

import com.ing.engine.constants.AppResourcePath;
import com.ing.engine.constants.FilePath;
import com.ing.engine.core.Control;
import com.ing.engine.core.RunContext;
import com.ing.engine.core.RunManager;
import com.ing.engine.reporting.TestCaseReport;
import com.ing.engine.reporting.impl.azureNunit.AzureReport;
import com.ing.engine.reporting.impl.handlers.PrimaryHandler;
import com.ing.engine.reporting.impl.handlers.TestCaseHandler;
import com.ing.engine.reporting.util.RDS.TestCase;
import com.ing.engine.reporting.util.ReportUtils;
import com.ing.engine.support.Status;

@SuppressWarnings({"unchecked"})
public class AzureTestCaseHandler extends TestCaseHandler implements PrimaryHandler {
    private final JSONObject testCaseData = new JSONObject();

    private String scenarioName;
    private String testCaseName;

    private int FailedSteps = 0;
    private int PassedSteps = 0;
    private int DoneSteps = 0;
    private final List<String> attachmentPaths = new ArrayList<>();

    private String messageCDATA = "";
    private String stacktraceData = "";

    public AzureTestCaseHandler(TestCaseReport report) {
        super(report);
    }

    private boolean isAzureEnabled() {
        if (!RunManager.getGlobalSettings().isTestRun()) {
            return Control.getCurrentProject().getProjectSettings()
                    .getExecSettings(RunManager.getGlobalSettings().getRelease(), RunManager.getGlobalSettings().getTestSet()).getRunSettings().isAzureEnabled();
        }
        return false;
    }

    @Override
    public void createReport(RunContext runContext, String runTime) {
        if (isAzureEnabled()) messageCDATA = "";

        scenarioName = runContext.Scenario;
        testCaseName = runContext.TestCase;

        testCaseData.put(TestCase.SCENARIO_NAME, scenarioName);
        testCaseData.put(TestCase.TESTCASE_NAME, testCaseName);
        testCaseData.put(TestCase.DESCRIPTION, runContext.Description);
        testCaseData.put(TestCase.START_TIME, runTime);
        testCaseData.put(TestCase.ITERATION_TYPE, runContext.Iteration);
    }

    @Override
    public Object getData() {
        // TODO Auto-generated method stub
        return testCaseData;
    }

    @Override
    public File getFile() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void updateTestLog(String stepName, String stepDescription, Status state, String link, List<String> links) {
        if (!isAzureEnabled()) return;

        try {
            String stepData = String.format("[%s]   | %s", state, ReportUtils.resolveDesc(stepDescription));

            stepData = stepData.replaceAll("\"", "--");
            stepData = stepData.replaceAll("\\r\\n|\\r|\\n", "");
            stepData = stepData.replaceAll("<br>", "");
            stepData = stepData.replaceAll("#CTAG", "");

            String screenshotSrc = putStatus(state);
            String filename = "";

            if (screenshotSrc != null) {
                filename = AppResourcePath.getCurrentResultsPath() + screenshotSrc;
            }

            createLogNodes(stepData, state, filename);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private String putStatus(Status state) {
        switch (state) {
            case DONE:
            case PASSNS:
                DoneSteps++;
                break;
            case PASS:
            case FAIL:
            case DEBUG:
            case SCREENSHOT:
                if (state == Status.FAIL || state == Status.DEBUG) FailedSteps++;
                if (state == Status.PASS) PassedSteps++;
                if (!canTakeScreenShot(state)) break;

                String imgSrc = getScreenShotName();
                if (ReportUtils.takeScreenshot(getPlaywrightDriver(), getWebDriver(), imgSrc)) return imgSrc;

                break;
            case WARNING:
            case FAILNS:
                FailedSteps++;
                break;
        }

        return null;
    }

    private Boolean canTakeScreenShot(Status status) {
        if (status.equals(Status.FAIL) || status.equals(Status.DEBUG)) {
            return screenShotSettings().matches("(Fail|Both)");
        }
        if (status.equals(Status.PASS)) {
            return screenShotSettings().matches("(Pass|Both)");
        }
        return false;
    }

    private String screenShotSettings() {
        return Control.exe.getExecSettings().getRunSettings().getScreenShotFor();
    }

    /**
     * finalize the test case execution and create standalone test case report
     * file for upload purpose
     *
     * @return
     */
    @Override
    public Status finalizeReport() {
        testCaseData.put(TestCase.NO_OF_TESTS, getStepCount());
        testCaseData.put(TestCase.NO_OF_FAIL_TESTS, String.valueOf(this.FailedSteps));
        testCaseData.put(TestCase.NO_OF_PASS_TESTS, String.valueOf(this.DoneSteps + this.PassedSteps));
        testCaseData.put(TestCase.EXE_TIME, report.startTime.timeRun());

        String prefix = scenarioName + "_" + testCaseName;
        File logsFolder = new File(FilePath.getCurrentTestCaseLogsLocation());
        String logPath = logsFolder.getAbsolutePath() + File.separator + prefix + ".txt";
        attachmentPaths.add(logPath);

        File videoFolder = new File(FilePath.getCurrentTestCaseVideosLocation());
        if (videoFolder.exists()) {
            File testCaseVideo = new File(FilePath.getCurrentTestCaseVideosLocation() + File.separator + prefix);
            for (File fileEntry : testCaseVideo.listFiles()) {
                attachmentPaths.add(fileEntry.getAbsolutePath());
            }
        }

        String result, message;
        String stacktrace = "";
        String noError = "This Test Case has no error. For details see the steps below:" + "\n";
        if (this.stacktraceData.isEmpty()) {
            message = "<message><![CDATA[" + noError + this.messageCDATA + "]]></message>";
        } else {
            message = "<message><![CDATA[" + this.messageCDATA + "]]></message>";
            stacktrace = "<stack-trace><![CDATA[" + this.stacktraceData + "]]></stack-trace>";
        }

        Status status = getCurrentStatus();
        if (status == Status.PASS || status == Status.DONE || status == Status.COMPLETE) {
            result = "Passed";
            AzureReport.passed++;
        } else {
            result = "Failed";
            AzureReport.failed++;
        }

        try (FileOutputStream out = new FileOutputStream(AzureReport.testCasesFile, true)) {
            String attachments = attachmentPaths
                    .stream().map(p -> "<attachment><filePath>" + p + "</filePath></attachment>")
                    .collect(Collectors.joining("\n"));

            String testCase = "<test-case "
                    + "id=\"" + UUID.randomUUID() + "\" "
                    + "name=\"" + testCaseName + "\" "
                    + "fullname=\"" + testCaseName + "\" "
                    + "result=\"" + result + "\" "
                    + "time=\"" + duration(testCaseData.get(TestCase.EXE_TIME).toString()) + "\" "
                    + ">"
                    + "<failure>" + message + stacktrace + "</failure>"
                    + "<attachments>" + attachments + "</attachments>"
                    + "</test-case>\n";

            out.write(testCase.getBytes());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return report.getCurrentStatus();
    }

    private String duration(String executionTime) {
        long seconds = 0;
        try {
            DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date;
            date = dateFormat.parse(executionTime);
            seconds = date.getTime() / 1000L;
            AzureReport.totalDuration += seconds;
        } catch (java.text.ParseException e) {
            e.printStackTrace();
        }
        return seconds + ".000";
    }

    @Override
    public Status getCurrentStatus() {
        if (FailedSteps > 0 || (PassedSteps + DoneSteps) == 0) {
            return Status.FAIL;
        } else {
            return Status.PASS;
        }
    }

    private void createLogNodes(String stepdata, Status status, String filepath) throws IOException {
        messageCDATA += "Step " + getStepCount() + ":   " + stepdata + "\n";

        if (status != Status.PASS && status != Status.DONE && status != Status.COMPLETE) {
            stacktraceData += "Step " + getStepCount() + ":   " + stepdata + "\n";
        }

        if (filepath.isEmpty()) return;

        File attachment = new File(new File(filepath).getCanonicalPath());
        if (attachment.isDirectory()) {
            String prefix = scenarioName + "_" + testCaseName;

            for (File fileEntry : attachment.listFiles()) {
                if (fileEntry.getName().contains(prefix)) {
                    attachmentPaths.add(fileEntry.getAbsolutePath());
                }
            }
        } else {
            attachmentPaths.add(attachment.getAbsolutePath());
        }
    }
}
package com.ing.engine.reporting.impl.azure;

import java.io.*;
import com.ing.engine.constants.FilePath;
import com.ing.engine.core.Control;
import com.ing.engine.core.RunManager;
import com.ing.engine.reporting.SummaryReport;
import com.ing.engine.reporting.impl.azureNunit.AzureReport;
import com.ing.engine.reporting.impl.handlers.PrimaryHandler;
import com.ing.engine.reporting.impl.handlers.SummaryHandler;
import com.ing.engine.support.Status;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;

public class AzureSummaryHandler extends SummaryHandler implements PrimaryHandler {

    private static final Logger LOGGER = Logger.getLogger(AzureSummaryHandler.class.getName());
    private String startTime;

    public AzureSummaryHandler(SummaryReport report) {
        super(report);
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized void createReport(String runTime, int size) {
        if (RunManager.getGlobalSettings().isTestRun()) return;
        if (!isAzureEnabled()) return;

        startTime = getDateDetails("time");
    }

    public boolean isAzureEnabled() {
        if (!RunManager.getGlobalSettings().isTestRun()) {
            return Control.getCurrentProject().getProjectSettings()
                    .getExecSettings(RunManager.getGlobalSettings().getRelease(), RunManager.getGlobalSettings().getTestSet()).getRunSettings().isAzureEnabled();
        }
        return false;
    }

    @Override
    public Object getData() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public File getFile() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Status getCurrentStatus() {
        if (AzureReport.failed > 0 || AzureReport.passed == 0) {
            return Status.FAIL;
        } else {
            return Status.PASS;
        }
    }

    @Override
    public synchronized void finalizeReport() {
        if (RunManager.getGlobalSettings().isTestRun()) return;
        if (!isAzureEnabled()) return;

        try {
            finishReport(FilePath.getCurrentAzureReportPath());
            FileUtils.copyFileToDirectory(new File(FilePath.getCurrentAzureReportPath()),
                    new File(FilePath.getLatestResultsLocation()),true);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    private String getDateDetails(String type) {
        LocalDateTime now = LocalDateTime.now();
        if ("date".equalsIgnoreCase(type)) {
            return now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } else {
            return now.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        }
    }

    private void finishReport(String AzureReportPath) throws IOException {
        String total = String.valueOf(AzureReport.passed + AzureReport.failed);
        String passed = String.valueOf(AzureReport.passed);
        String failed = String.valueOf(AzureReport.failed);
        String result = (AzureReport.failed > 0) ? "Failed" : "Passed";
        String duration = AzureReport.totalDuration + ".000";

        String testRun = "<test-run id=\"" + UUID.randomUUID() + "\" name=\"" + RunManager.getGlobalSettings().getTestSet()
                + "\" fullname=\"" + RunManager.getGlobalSettings().getTestSet() + "\" testcasecount=\"" + total
                + "\" passed=\"" + passed
                + "\" failed=\"" + failed
                + "\" result=\"" + result
                + "\" time=\"" + duration
                + "\" run-date=\"" + getDateDetails("date")
                + "\" start-time=\"" + startTime
                + "\" end-time=\"" + getDateDetails("time")
                + "\">" + "\n";

        String testSuite = "<test-suite id=\"" + UUID.randomUUID() + "\" type=\"Assembly\" name=\""
                + RunManager.getGlobalSettings().getTestSet() + "\" fullname=\""
                + RunManager.getGlobalSettings().getTestSet() + "\" testcasecount=\"" + total
                + "\" passed=\"" + passed
                + "\" failed=\"" + failed
                + "\" result=\"" + result
                + "\" time=\"" + duration
                + "\">" + "\n";

        FileOutputStream out = new FileOutputStream(AzureReportPath);

        out.write(testRun.getBytes());
        out.write(testSuite.getBytes());

        try (FileInputStream in = new FileInputStream(AzureReport.testCasesFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }

        out.write("</test-suite></test-run>".getBytes());
        out.close();

        System.out.println("\n-----------------------------------------------------");
        System.out.println("Azure Report Path: " + AzureReportPath);
        System.out.println("Azure Report XML generated");
        System.out.println("-----------------------------------------------------\n");

        resetAzureVars();
    }
    private void resetAzureVars() {
        AzureReport.totalDuration = 0;
        AzureReport.failed = 0;
        AzureReport.passed = 0;

        try {
            if (!AzureReport.testCasesFile.delete()) {
                throw new RuntimeException("Cannot delete temporary file.");
            }

            AzureReport.testCasesFile = File.createTempFile("test-cases", ".xml");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
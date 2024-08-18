
package com.ing.engine.reporting.impl.azure;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.text.SimpleDateFormat;

import org.apache.http.client.ClientProtocolException;
import org.json.simple.parser.ParseException;

import org.xml.sax.SAXException;

import com.ing.engine.constants.FilePath;
import com.ing.engine.core.Control;
import com.ing.engine.core.RunManager;
import com.ing.engine.reporting.SummaryReport;
import com.ing.engine.reporting.impl.azureNunit.AzureReport;
import com.ing.engine.reporting.impl.handlers.PrimaryHandler;
import com.ing.engine.reporting.impl.handlers.SummaryHandler;
import com.ing.engine.support.Status;

import java.util.Date;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;

public class AzureSummaryHandler extends SummaryHandler implements PrimaryHandler {

    private static final Logger LOGGER = Logger.getLogger(AzureSummaryHandler.class.getName());

    int FailedTestCases = 0;
    int PassedTestCases = 0;

    String testcasename = "";

    public AzureSummaryHandler(SummaryReport report) {
        super(report);
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized void createReport(String runTime, int size) {
        if (!RunManager.getGlobalSettings().isTestRun()) {
            try {
                startReport(RunManager.getGlobalSettings().getTestSet());
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
    }

    public boolean isAzureEnabled() {
        if (!RunManager.getGlobalSettings().isTestRun()) {
            return Control.getCurrentProject().getProjectSettings()
                    .getExecSettings(RunManager.getGlobalSettings().getRelease(), RunManager.getGlobalSettings().getTestSet()).getRunSettings().isAzureEnabled();
        }
        return false;
    }

    private void startReport(String testset) {
        System.out.println("testset : " + testset);
        if (isAzureEnabled()) {
            try {
                startReport();
            } catch (IOException | ParseException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }
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
        if (FailedTestCases > 0 || PassedTestCases == 0) {
            return Status.FAIL;
        } else {
            return Status.PASS;
        }
    }

    @Override
    public synchronized void finalizeReport() {
        if (!RunManager.getGlobalSettings().isTestRun()) {
            if (isAzureEnabled()) {
                try {
                    finishReport(FilePath.getCurrentAzureReportPath());
                    FileUtils.copyFileToDirectory(new File(FilePath.getCurrentAzureReportPath()),
                    new File(FilePath.getLatestResultsLocation()),true);
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                }
            }
        }

    }

    public String getUUID() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }

    public String getDateDetails(String type) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        String fulldate = formatter.format(date);
        if (type.equalsIgnoreCase("date")) {
            return (fulldate.split(" ")[0]);
        } else {
            return (fulldate.split(" ")[1]);
        }
    }

    public void startReport()
            throws ClientProtocolException, IOException, ParseException {
        AzureReport.startTime = getDateDetails("time");
    }

    public void finishReport(String AzureReportPath) throws ClientProtocolException, IOException, ParseException, SAXException {

        String total = String.valueOf(AzureReport.passed + AzureReport.failed);
        String passed = String.valueOf(AzureReport.passed);
        String failed = String.valueOf(AzureReport.failed);
        String result = "Passed";
        if (AzureReport.failed > 0) {
            result = "Failed";
        }
        String duration = AzureReport.totalDuration + ".000";

        AzureReport.testrun = "<test-run id=\"" + getUUID() + "\" name=\"" + RunManager.getGlobalSettings().getTestSet()
                + "\" fullname=\"" + RunManager.getGlobalSettings().getTestSet() + "\" testcasecount=\"" + total
                + "\" passed=\"" + passed
                + "\" failed=\"" + failed
                + "\" result=\"" + result
                + "\" time=\"" + duration
                + "\" run-date=\"" + getDateDetails("date")
                + "\" start-time=\"" + AzureReport.startTime
                + "\" end-time=\"" + getDateDetails("time")
                + "\">" + "\n";
        AzureReport.testsuite = "<test-suite id=\"" + getUUID() + "\" type=\"Assembly\" name=\""
                + RunManager.getGlobalSettings().getTestSet() + "\" fullname=\""
                + RunManager.getGlobalSettings().getTestSet() + "\" testcasecount=\"" + total
                + "\" passed=\"" + passed
                + "\" failed=\"" + failed
                + "\" result=\"" + result
                + "\" time=\"" + duration
                + "\">" + "\n";

        AzureReport.xmlData = AzureReport.testrun
                + AzureReport.testsuite
                + AzureReport.testcase
                + "</test-suite>"
                + "</test-run>";
        

        FileOutputStream out = new FileOutputStream(AzureReportPath);

        out.write(AzureReport.xmlData.getBytes());
        out.close();
        System.out.println("\n-----------------------------------------------------");
        System.out.println("Azure Report XML generated");
        System.out.println("-----------------------------------------------------\n");
		resetAzureVars();

    }
    
    private void resetAzureVars() {
    	AzureReport.totalDuration=0;
    	AzureReport.failed=0;
    	AzureReport.passed=0;
    	AzureReport.message="";
    	AzureReport.CDATA="";
    	AzureReport.stacktraceData="";
    	AzureReport.stacktrace="";
    	AzureReport.xmlData="";
    	AzureReport.testrun="";
    	AzureReport.testsuite="";
    	AzureReport.testcase="";
    	AzureReport.failures="";
    	AzureReport.attachments="";
    	AzureReport.startTime="";
    	AzureReport.endTime="";
    }

}

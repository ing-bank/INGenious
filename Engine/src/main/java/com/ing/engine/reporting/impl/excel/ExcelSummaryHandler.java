package com.ing.engine.reporting.impl.excel;

import com.ing.engine.constants.AppResourcePath;
import com.ing.engine.constants.FilePath;
import com.ing.engine.core.Control;
import com.ing.engine.core.RunContext;
import com.ing.engine.core.RunManager;
import com.ing.engine.reporting.SummaryReport;
import com.ing.engine.reporting.TestCaseReport;

import com.ing.engine.reporting.impl.handlers.PrimaryHandler;
import com.ing.engine.reporting.impl.handlers.SummaryHandler;
import com.ing.engine.reporting.performance.PerformanceReport;
import com.ing.engine.reporting.performance.har.Har;
import com.ing.engine.reporting.util.DateTimeUtils;
import com.ing.engine.reporting.util.RDS;
import com.ing.engine.reporting.util.ReportUtils;
import com.ing.engine.support.Status;

import java.awt.Font;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellUtil;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 *
 *
 */
@SuppressWarnings("rawtypes")
public class ExcelSummaryHandler extends SummaryHandler implements PrimaryHandler {

    private static final Logger LOGGER = Logger.getLogger(ExcelSummaryHandler.class.getName());

    static ArrayList<JSONObject> objectsarray = new ArrayList<JSONObject>();

    JSONObject testSetData = new JSONObject();
    JSONArray executions = new JSONArray();
    public boolean RunComplete = false;
    int FailedTestCases = 0;
    int PassedTestCases = 0;
    int noTests = 0;
    DateTimeUtils RunTime;
    public PerformanceReport perf;

    public ExcelSummaryHandler(SummaryReport report) {
        super(report);

    }

    @Override
    public void addHar(Har<String, Har.Log> h, TestCaseReport report, String pageName) {
        if (perf != null) {
            perf.addHar(h, report, pageName);
        }
    }

    /**
     * initialize the report data file.
     *
     * @param runTime
     * @param size
     */
    @SuppressWarnings("unchecked")
    @Override
    public synchronized void createReport(String runTime, int size) {

        try {
            ReportUtils.loadDefaultTheme(testSetData);
            RunTime = new DateTimeUtils();
            new File(FilePath.getCurrentResultsPath()).mkdirs();
            testSetData.put(RDS.TestSet.PROJECT_NAME, RunManager.getGlobalSettings().getProjectName());
            testSetData.put(RDS.TestSet.RELEASE_NAME, RunManager.getGlobalSettings().getRelease());
            testSetData.put(RDS.TestSet.TESTSET_NAME, RunManager.getGlobalSettings().getTestSet());
            testSetData.put(RDS.TestSet.ITERATION_MODE,
                    Control.exe.getExecSettings().getRunSettings().getIterationMode());
            testSetData.put(RDS.TestSet.RUN_CONFIG, Control.exe.getExecSettings().getRunSettings().getExecutionMode());
            testSetData.put(RDS.TestSet.MAX_THREADS, Control.exe.getExecSettings().getRunSettings().getThreadCount());
            testSetData.put(RDS.TestSet.BDD_STYLE, Control.exe.getExecSettings().getRunSettings().isBddReportEnabled());
            testSetData.put(RDS.TestSet.PERF_REPORT,
                    Control.exe.getExecSettings().getRunSettings().isPerformanceLogEnabled());
            testSetData.put(RDS.TestSet.START_TIME, runTime);
            testSetData.put(RDS.TestSet.TEST_RUN, RunManager.getGlobalSettings().isTestRun());
            testSetData.put(RDS.TestSet.NO_OF_TESTS, size);
            testSetData.put(RDS.TestSet.VIDEO_REPORT, Control.exe.getExecSettings().getRunSettings().isVideoEnabled());

        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
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

        executions.add(report.getData());
        String status;
        if (state.equals(Status.PASS)) {
            status = "Passed";
            PassedTestCases++;
        } else {
            FailedTestCases++;
            status = "Failed";
        }
        ReportUtils.updateStatus(status, runContext);

        if (perf != null) {
            perf.updateTestCase(report.Scenario, report.TestCase);
        }
        updateResults();
    }

    /**
     * update the test set details to the json data file and write the data file
     */
    @SuppressWarnings("unchecked")
    public synchronized void updateResults() {
        String exeTime = RunTime.timeRun();
        String endTime = DateTimeUtils.DateTimeNow();

        try {
            if (RunComplete) {
                testSetData.put(RDS.TestSet.EXECUTIONS, executions);
                testSetData.put(RDS.TestSet.END_TIME, endTime);
                testSetData.put(RDS.TestSet.EXE_TIME, exeTime);

                testSetData.put(RDS.TestSet.NO_OF_FAIL_TESTS, String.valueOf(FailedTestCases));
                testSetData.put(RDS.TestSet.NO_OF_PASS_TESTS, String.valueOf(PassedTestCases));
                RDS.writeToDataJS(FilePath.getCurrentReportDataPath(), testSetData);
            } else {

            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
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

            createExcel();

        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }

    }

    private void createExcel() throws Exception {

        if (!RunManager.getGlobalSettings().isTestRun()) {

            String current_release = RunManager.getGlobalSettings().getRelease();

            String current_testset = RunManager.getGlobalSettings().getTestSet();

            String case_check = Control.getCurrentProject().getProjectSettings()
                    .getExecSettings(current_release, current_testset).getRunSettings().getProperty("excelReport");

            if (case_check != null && case_check.equalsIgnoreCase("true")) {

                String OS = null;
                OS = System.getProperty("os.name");

                String datajspath = FilePath.getLatestResultsLocation() + File.separator + "data.js";

                try {

                    File file = new File(datajspath);
                    String jstr = FileUtils.readFileToString(file).replaceFirst("var DATA=", "");
                    String jsonString = jstr.substring(0, jstr.length() - 1);

                    String template = AppResourcePath.getConfigurationPath()
                            + "\\ReportTemplate\\excel\\excelreporttemplate.xlsx";

                    String excelreport = FilePath.getLatestResultsLocation() + File.separator
                            + "SummaryExcelReport.xlsx";
                    String excelreport_tm = FilePath.getCurrentResultsPath() + File.separator
                            + "SummaryExcelReport.xlsx";

                    File file1 = new File(template);
                    File file2 = new File(excelreport);

                    if (!file2.exists()) {
                        FileUtils.copyFile(file1, file2);
                    }

                    FileInputStream excelFile = new FileInputStream(new File(excelreport));
                    XSSFWorkbook workbook = new XSSFWorkbook(excelFile);

                    ArrayList<ArrayList<String>> listOLists = new ArrayList<ArrayList<String>>();

                    AtomicInteger sheetnumber = new AtomicInteger(1);

                    for (int i = 0; i < executions.size(); i++) {
                        XSSFFont font = workbook.createFont();
                        font.setBold(true);
                        XSSFCellStyle style = workbook.createCellStyle();
                        style.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
                        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                        style.setFont(font);

                        ArrayList<String> singleList = new ArrayList<String>();

                        JSONObject eachexecution = (JSONObject) executions.get(i);

                        //System.out.println("exec details" + eachexecution.toString());
                        String sheetname = sheetnumber.getAndIncrement() + "_" + eachexecution.get("testcaseName");

                        XSSFSheet sheet = workbook.createSheet(sheetname);
                        // TITLE FOR EVERY SHEET
                        XSSFRow title = sheet.createRow(0);

                        XSSFCell cell = title.createCell(0);

                        cell.setCellValue(eachexecution.get("scenarioName") + "_" + eachexecution.get("iterationType")
                                + "_" + eachexecution.get("testcaseName") + "_" + eachexecution.get("browser"));

                        XSSFCellStyle styletitile = workbook.createCellStyle();
                        styletitile.setFont(font);
                        cell.setCellStyle(styletitile);

                        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 14));
                        CellUtil.setAlignment(cell, HorizontalAlignment.CENTER);

                        XSSFRow row_header = sheet.createRow(1);

                        row_header.createCell(1).setCellValue("Step No");
                        row_header.createCell(2).setCellValue("Step Name");
                        row_header.createCell(3).setCellValue("Action");
                        row_header.createCell(4).setCellValue("Description");
                        row_header.createCell(5).setCellValue("Status");
                        row_header.createCell(6).setCellValue("Time Stamp");

                        row_header.getCell(1).setCellStyle(style);
                        row_header.getCell(2).setCellStyle(style);
                        row_header.getCell(3).setCellStyle(style);
                        row_header.getCell(4).setCellStyle(style);
                        row_header.getCell(5).setCellStyle(style);
                        row_header.getCell(6).setCellStyle(style);

                        AtomicInteger atomicInteger = new AtomicInteger(2);

                        JSONArray steparray = (JSONArray) eachexecution.get("STEPS");

                        JSONObject stepsobject = (JSONObject) steparray.get(0);
                        JSONArray steps = (JSONArray) stepsobject.get("data");

                        for (int j = 0; j < steps.size(); j++) {
                            JSONObject step = (JSONObject) steps.get(j);
                            Object detail = step.get("data");
                            parsearray(detail);

                        }

                        for (int j = 0; j < objectsarray.size(); j++) {

                            int index = atomicInteger.getAndIncrement();
                            Object data = null;

                            XSSFRow roww1 = sheet.createRow(index);

                            roww1.createCell(1).setCellValue(Integer.toString((int) objectsarray.get(j).get("stepno")));
                            roww1.createCell(2).setCellValue((String) objectsarray.get(j).get("stepName"));
                            roww1.createCell(3).setCellValue((String) objectsarray.get(j).get("action"));
                            roww1.createCell(4).setCellValue((String) objectsarray.get(j).get("description"));
                            roww1.createCell(5).setCellValue((String) objectsarray.get(j).get("status"));
                            roww1.createCell(6).setCellValue((String) objectsarray.get(j).get("tStamp"));

                        }

                        clearObjectArray();

                        for (int j = 0; j < objectsarray.size(); j++) {
                            //System.out.println("step number" + objectsarray.get(j).get("stepno"));
                            //System.out.println("step name " + objectsarray.get(j).get("tStamp"));
                            //System.out.println("number" + j);
                        }

                        // -----------------------
                        singleList.add((String) eachexecution.get("scenarioName"));
                        singleList.add((String) eachexecution.get("testcaseName"));
                        singleList.add((String) eachexecution.get("browser"));
                        singleList.add((String) eachexecution.get("exeTime"));
                        singleList.add((String) eachexecution.get("status"));
                        singleList.add((String) eachexecution.get("platform"));
                        singleList.add((String) eachexecution.get("iterationType"));
                        singleList.add((String) eachexecution.get("bversion"));
                        listOLists.add(singleList);

                    }

                    XSSFSheet sheet = workbook.getSheetAt(0);

                    XSSFRow header = sheet.getRow(0);
                    XSSFCell cellvalue = header.getCell(0);
                    if (cellvalue.getStringCellValue().equalsIgnoreCase("ReleaseName-Testsetname")) {
                        cellvalue.setCellValue(RunManager.getGlobalSettings().getRelease() + " - "
                                + RunManager.getGlobalSettings().getTestSet());
                    }

                    Iterator<ArrayList<String>> iterator = listOLists.iterator();
                    int row = 2;
                    while (iterator.hasNext()) {
                        ArrayList singleList = iterator.next();
                        Iterator<String> childiter = singleList.iterator();
                        int i = 1;
                        XSSFRow roww = sheet.createRow(row);
                        while (childiter.hasNext()) {
                            String s = childiter.next();

                            roww.createCell(i).setCellValue(s);
                            i++;
                        }
                        row++;

                    }

                    // Write content to excel files
                    FileOutputStream outputStream = new FileOutputStream(excelreport);
                    workbook.write(outputStream);

                    Iterator<Sheet> sheetIterator = workbook.sheetIterator();
                    DataFormatter dataFormatter = new DataFormatter();

                    FileOutputStream outputStreamrp = new FileOutputStream(excelreport_tm);
                    workbook.write(outputStreamrp);
                    workbook.close();
                    System.out.println("Latest Excel Report Path " + FilePath.getLatestResultsLocation());

                    System.out.println("-----------------------------------------------------");
                    System.out.println("EXCEL REPORT CREATED SUCCESSFULLY");
                    System.out.println("-----------------------------------------------------");

                    // launch excel sheet in case of Windows OS
                    if (OS.contains("Windows")) {
                        launchexcel();
                    }
                } catch (IOException e) {
                    System.err.println("IOException caught: " + e.getMessage());
                }

            }

        }

    }

    private static void parsearray(Object stepsdetails) {
        if (stepsdetails instanceof JSONArray) {
            int size = ((JSONArray) stepsdetails).size();
            for (int i = 0; i < size; i++) {
                JSONObject s = (JSONObject) ((JSONArray) stepsdetails).get(i);
                Object obj = s.get("data");
                if (obj instanceof JSONObject) {
                    objectsarray.add((JSONObject) s.get("data"));
                } else {
                    parsearray(s.get("data"));
                }
            }
        }
        if (stepsdetails instanceof JSONObject) {
            objectsarray.add((JSONObject) stepsdetails);
        }
    }

    private static void clearObjectArray() {
        objectsarray.clear();
    }

    private void launchexcel() throws IOException {
        String excelreport = FilePath.getCurrentResultsPath() + "\\SummaryExcelReport.xlsx";
        try {

            LOGGER.info("Trying To Open Excel");
            Runtime.getRuntime().exec("cmd /c start excel \"" + excelreport + "\"");
            LOGGER.info("Opened Excel Report Successfully");
        } catch (Exception E) {
            System.out.println("Make sure Excel location is added to system path" + E.getMessage());
            LOGGER.info("Unable To Open Report, Please Check If Excel location is added to System Path");

        }

    }

    /**
     * update the result when any error in execution
     *
     * @param testScenario
     * @param testCase
     * @param Iteration
     * @param testDescription
     * @param executionTime
     * @param fileName
     * @param state
     * @param Browser
     */
    @Override
    public void updateTestCaseResults(String testScenario, String testCase, String Iteration, String testDescription,
            String executionTime, String fileName, Status state, String Browser) {

        System.out.println("--------------->[UPDATING SUMMARY]");
        if (state.equals(Status.PASS)) {
            PassedTestCases++;
        } else {
            FailedTestCases++;
        }
    }

    @Override
    public Object getData() {
        return testSetData;
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

}

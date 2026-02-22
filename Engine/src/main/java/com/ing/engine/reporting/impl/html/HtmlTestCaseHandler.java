package com.ing.engine.reporting.impl.html;

import com.ing.datalib.util.data.FileScanner;
import com.ing.engine.constants.AppResourcePath;
import com.ing.engine.constants.FilePath;
import com.ing.engine.core.Control;
import com.ing.engine.core.RunContext;
import com.ing.engine.drivers.PlaywrightDriverCreation;
import com.ing.engine.drivers.WebDriverCreation;
import com.ing.engine.reporting.TestCaseReport;
import com.ing.engine.reporting.impl.handlers.PrimaryHandler;
import com.ing.engine.reporting.impl.handlers.TestCaseHandler;
import com.ing.engine.reporting.util.DateTimeUtils;
import com.ing.engine.reporting.util.RDS;
import com.ing.engine.reporting.util.RDS.TestCase;
import com.ing.engine.reporting.util.ReportUtils;
import com.ing.engine.support.Status;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.opentelemetry.exporter.logging.SystemOutLogRecordExporter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

/**
 *
 *
 */
@SuppressWarnings({"unchecked"})
public class HtmlTestCaseHandler extends TestCaseHandler implements PrimaryHandler {

    JSONObject testCaseData = new JSONObject();
    JSONArray Steps = new JSONArray();
    JSONObject iteration;
    JSONObject reusable;
    String DATAF = "[<DATA>]";
    boolean isIteration = true;
    Stack<JSONObject> reusableStack = new Stack<>();

    private StringBuffer SourceDoc;
    public File ReportFile;

    String CurrentComponent = "";

    int ComponentCounter = 0;
    int iterCounter = 0;

    int FailedSteps = 0;
    int PassedSteps = 0;
    int DoneSteps = 0;
    
    // Store video and trace paths before browser closes
    private String capturedVideoPath = null;
    private String capturedTracePath = null;

    public HtmlTestCaseHandler(TestCaseReport report) {
        super(report);

    }

    @Override
    public void setPlaywrightDriver(PlaywrightDriverCreation driver) {
        testCaseData.put(TestCase.B_VERSION, getPlaywrightDriver().getBrowserVersion());
        testCaseData.put(TestCase.PLATFORM, System.getProperty("os.name")+ " " +System.getProperty("os.version")+ " " +System.getProperty("os.arch"));
        testCaseData.put(TestCase.BROWSER, getPlaywrightDriver().getCurrentBrowser());
    }
    
    @Override
    public void setWebDriver(WebDriverCreation driver) {
        testCaseData.put(TestCase.B_VERSION, getWebDriver().getCurrentBrowserVersion());
        testCaseData.put(TestCase.PLATFORM, getWebDriver().getPlatform());
        testCaseData.put(TestCase.BROWSER, getWebDriver().getCurrentBrowser());
    }

    @Override
    public void createReport(RunContext runContext, String runTime) {
        try {
            // Check if modern report style is enabled
            boolean useModern = Control.exe.getExecSettings().getRunSettings().isModernReport();
            String reportFileName = runContext.getName() + (useModern ? "-v2.html" : ".html");
            ReportFile = new File(getReportLoc(), reportFileName);
            ReportFile.createNewFile();
            
            // Use appropriate template based on setting
            File templateFile = useModern 
                ? new File(FilePath.getTCReportTemplateV2()) 
                : new File(FilePath.getTCReportTemplate());
            SourceDoc = new StringBuffer(FileScanner.readFile(templateFile));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        testCaseData.put(TestCase.SCENARIO_NAME, runContext.Scenario);
        testCaseData.put(TestCase.TESTCASE_NAME, runContext.TestCase);
        testCaseData.put(TestCase.DESCRIPTION, runContext.Description);
        testCaseData.put(TestCase.START_TIME, runTime);
        testCaseData.put(TestCase.ITERATION_TYPE, runContext.Iteration);
    }

    @Override
    public void updateTestLog(String stepName, String stepDescription, Status state,
            String link, List<String> links) {

        String time = DateTimeUtils.DateTimeNow();
        JSONObject step;
        try {
            step = RDS.getNewStep(getStep().Description);

            JSONObject data = (JSONObject) step.get(RDS.Step.DATA);
            data.put(RDS.Step.Data.STEP_NO, getStepCount());
            data.put(RDS.Step.Data.STEP_NAME, stepName);
            data.put(RDS.Step.Data.ACTION, getStep().Action);
            data.put(RDS.Step.Data.DESCRIPTION, ReportUtils.resolveDesc(stepDescription));
            data.put(RDS.Step.Data.TIME_STAMP, time);
            data.put(RDS.Step.Data.STATUS, state.toString());

            String payloadfiles = "";
            String filename = "";
            if (link != null) {
                payloadfiles = testCaseData.get(TestCase.SCENARIO_NAME)
                        + "_"
                        + testCaseData.get(TestCase.TESTCASE_NAME)
                        + "_Step-"
                        + getStepCount()
                        + "_";
                filename = AppResourcePath.getCurrentResultsPath() + link + File.separator + payloadfiles;
                data.put(RDS.Step.Data.LINK, filename);
                
                // Embed payload content directly in JSON to avoid CORS issues with file:// protocol
                String requestFilePath = filename + "Request.txt";
                String responseFilePath = filename + "Response.txt";
                
                // Read and embed request payload if it exists
                File requestFile = new File(requestFilePath);
                if (requestFile.exists()) {
                    try {
                        String requestContent = new String(Files.readAllBytes(requestFile.toPath()), StandardCharsets.UTF_8);
                        data.put("requestPayload", requestContent);
                    } catch (Exception e) {
                        System.out.println("[PAYLOAD] Failed to read request file: " + requestFilePath);
                    }
                }
                
                // Read and embed response payload if it exists
                File responseFile = new File(responseFilePath);
                if (responseFile.exists()) {
                    try {
                        String responseContent = new String(Files.readAllBytes(responseFile.toPath()), StandardCharsets.UTF_8);
                        data.put("responsePayload", responseContent);
                    } catch (Exception e) {
                        System.out.println("[PAYLOAD] Failed to read response file: " + responseFilePath);
                    }
                }
               
            }
            /*if (link != null) {
               data.put(RDS.Step.Data.LINK, link);
            }*/
            
            putStatus(state, links, link, data);
            if (isIteration) {
                ((JSONArray) iteration.get(RDS.Step.DATA)).add(step);
            } else {
                ((JSONArray) reusable.get(RDS.Step.DATA)).add(step);
            }
            if (isVideoEnabled()) {
                if (isIteration) {
                    iteration.put(RDS.TestSet.VIDEO_REPORT_DIR, getPlaywrightDriver().page.video().path().toString());
                } else {
                    reusable.put(RDS.TestSet.VIDEO_REPORT_DIR, getPlaywrightDriver().page.video().path().toString());
                }
                // Capture video path early before browser closes (only need to do once)
                if (capturedVideoPath == null) {
                    capturedVideoPath = getVideoPathForTestCase();
                }
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    private Boolean isVideoEnabled(){
        return Control.exe.getExecSettings().getRunSettings().isVideoEnabled();
    }    
    private boolean isTracingEnabled() {
        return Control.exe.getExecSettings().getRunSettings().isTracingEnabled();
    }
    
    /**
     * Convert absolute path to relative path from current results directory
     * Does NOT validate file existence - used for video/trace paths that may not exist yet
     */
    private String convertToRelativePath(String rawPath) {
        if (rawPath == null || rawPath.trim().isEmpty()) {
            return null;
        }

        String normalizedPath = rawPath.replace("\\", "/");
        String currentResultsPath = FilePath.getCurrentResultsPath();

        if (currentResultsPath != null && !currentResultsPath.trim().isEmpty()) {
            String normalizedResultsPath = currentResultsPath.replace("\\", "/");

            if (normalizedPath.startsWith(normalizedResultsPath)) {
                String relativePath = normalizedPath.substring(normalizedResultsPath.length());
                if (!relativePath.startsWith("/")) {
                    relativePath = "/" + relativePath;
                }
                return relativePath;
            }

            if (!normalizedPath.startsWith("/")) {
                return "/" + normalizedPath;
            }
        }

        return normalizedPath;
    }
    
    private String resolveReportPathIfExists(String rawPath) {
        if (rawPath == null || rawPath.trim().isEmpty()) {
            return null;
        }

        String normalizedPath = rawPath.replace("\\", "/");
        String currentResultsPath = FilePath.getCurrentResultsPath();

        if (currentResultsPath != null && !currentResultsPath.trim().isEmpty()) {
            String normalizedResultsPath = currentResultsPath.replace("\\", "/");

            if (normalizedPath.startsWith(normalizedResultsPath)) {
                String relativePath = normalizedPath.substring(normalizedResultsPath.length());
                if (!relativePath.startsWith("/")) {
                    relativePath = "/" + relativePath;
                }
                File relativeFile = new File(currentResultsPath + relativePath);
                return relativeFile.exists() ? relativePath : null;
            }

            if (!normalizedPath.startsWith("/")) {
                String relativePath = "/" + normalizedPath;
                File relativeFile = new File(currentResultsPath + relativePath);
                return relativeFile.exists() ? relativePath : null;
            }
        }

        File absoluteFile = new File(normalizedPath);
        return absoluteFile.exists() ? normalizedPath : null;
    }
    private String getVideoPathForTestCase() {
        try {
            // First, try to extract video path from the Steps data (which was set during execution)
            if (Steps != null && !Steps.isEmpty()) {
                for (Object stepObj : Steps) {
                    JSONObject step = (JSONObject) stepObj;
                    if (step.containsKey(RDS.TestSet.VIDEO_REPORT_DIR)) {
                        Object videoPathObj = step.get(RDS.TestSet.VIDEO_REPORT_DIR);
                        if (videoPathObj != null) {
                            String videoPath = videoPathObj.toString();
                            if (!videoPath.isEmpty()) {
                                return videoPath;
                            }
                        }
                    }
                }
            }
            
            // Fallback: Try to get from Playwright driver
            if (getPlaywrightDriver() != null && getPlaywrightDriver().page != null && getPlaywrightDriver().page.video() != null) {
                return getPlaywrightDriver().page.video().path().toString();
            }
        } catch (Exception e) {
            // If video path cannot be determined, return null
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Get the trace path for the current test case
     * Traces are saved as: {ResultsPath}/traces/{Scenario}_{TestCase}_{Timestamp}/traces.zip
     */
    private String getTracePathForTestCase() {
        try {
            String currentResultsPath = FilePath.getCurrentResultsPath();
            String tracesDir = currentResultsPath + File.separator + "traces";
            File tracesDirFile = new File(tracesDir);
            
            if (!tracesDirFile.exists() || !tracesDirFile.isDirectory()) {
                return null;
            }
            
            // Look for trace folder matching scenario and test case name
            String scenarioName = (String) testCaseData.get(TestCase.SCENARIO_NAME);
            String testCaseName = (String) testCaseData.get(TestCase.TESTCASE_NAME);
            String searchPattern = scenarioName + "_" + testCaseName + "_";
            
            File[] matchingDirs = tracesDirFile.listFiles(file -> 
                file.isDirectory() && file.getName().startsWith(searchPattern)
            );
            
            if (matchingDirs != null && matchingDirs.length > 0) {
                // Get the most recent one (should be the only one for current execution)
                File traceFolder = matchingDirs[matchingDirs.length - 1];
                File traceZip = new File(traceFolder, "traces.zip");
                
                // Return path even if file doesn't exist yet (may still be writing)
                String fullTracePath = traceZip.getAbsolutePath();
                
                // Convert to relative path
                if (fullTracePath.startsWith(currentResultsPath)) {
                    String relativePath = fullTracePath.substring(currentResultsPath.length());
                    relativePath = relativePath.replace("\\", "/");
                    if (!relativePath.startsWith("/")) {
                        relativePath = "/" + relativePath;
                    }
                    return relativePath;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * creates new iteration object
     *
     * @param iterationNo
     */
    @Override
    public void startIteration(int iterationNo) {
        reusableStack.clear();
        ++iterCounter;
        String Iterationid = "Iteration_" + iterationNo;
        iteration = RDS.getNewIteration(Iterationid);
        isIteration = true;
    }

    /**
     * creates new reusable object
     *
     * @param component
     * @param desc
     */
    @Override
    public void startComponent(String component, String desc) {
        reusable = RDS.getNewReusable(component, desc);
        reusableStack.push(reusable);
        isIteration = false;
    }

    @Override
    public void endComponent(String string) {
        reusable.put(RDS.Step.END_TIME, DateTimeUtils.DateTimeNow());
        if (reusable.get(TestCase.STATUS).equals("")) {
            /*
            * status not is updated set it to FAIL 
             */
            reusable.put(TestCase.STATUS, "FAIL");
        }
        /*
        * remove the reusable from the stack then fall back to iteration 
        * if stack is empty else update the outer reusable status.
         */
        reusableStack.pop();
        if (reusableStack.empty()) {
            ((JSONArray) iteration.get(RDS.Step.DATA)).add(reusable);
            reusable = null;
            isIteration = true;
        } else {
            ((JSONArray) reusableStack.peek().get(RDS.Step.DATA)).add(reusable);
            reusableStack.peek().put(TestCase.STATUS, reusable.get(TestCase.STATUS));
            reusable = reusableStack.peek();
        }

    }

    @Override
    public void endIteration(int CurrentTestCaseIteration) {
        if (iteration.get(TestCase.STATUS).equals("")) {
            iteration.put(TestCase.STATUS, "FAIL");
        }
        Steps.add(iteration);
    }

    private void onSetpDone() {
        DoneSteps++;
        if (reusable != null && reusable.get(TestCase.STATUS).equals("")) {
            reusable.put(TestCase.STATUS, "PASS");
        }
        if (iteration != null && iteration.get(TestCase.STATUS).equals("")) {
            iteration.put(TestCase.STATUS, "PASS");
        }
    }

    private void onSetpPassed() {
        PassedSteps++;
        if (reusable != null && reusable.get(TestCase.STATUS).equals("")) {
            reusable.put(TestCase.STATUS, "PASS");
        }
        if (iteration != null && iteration.get(TestCase.STATUS).equals("")) {
            iteration.put(TestCase.STATUS, "PASS");
        }
    }

    private void onSetpFailed() {
        FailedSteps++;
        if (iteration != null) {
            iteration.put(TestCase.STATUS, "FAIL");
        }
        if (reusable != null) {
            reusable.put(TestCase.STATUS, "FAIL");
        }
    }

    private void putStatus(Status state, List<String> optional, String optionalLink, JSONObject data) {
        switch (state) {
            case DONE:
            case PASSNS:
                onSetpDone();
                break;
            case PASS:
            case FAIL:
            case SCREENSHOT:
                takeScreenShot(state, optional, optionalLink, data);
                break;
            case DEBUG:
            case WARNING:
            case FAILNS:
                onSetpFailed();
                break;
            case COMPLETE:
                onSetpPassed();
                break;

        }
    }

    private void takeScreenShot(Status status, List<String> optional, String optionalLink, JSONObject data) {
        String imgSrc = getScreenShotName();
        switch (status) {
            case PASS:
            case FAIL:
                if (!canTakeScreenShot(status)) {
                    break;
                }
                if (optionalLink != null) {
                    break;
                }
            case SCREENSHOT:
                takeSSAndPutDetail(data, optional, imgSrc);
                break;
            default:
                break;
        }
    }

    private Boolean canTakeScreenShot(Status status) {
        if (status.equals(Status.FAIL)) {
            onSetpFailed();
            return screenShotSettings().matches("(Fail|Both)");
        }
        if (status.equals(Status.PASS)) {
            onSetpPassed();
            return screenShotSettings().matches("(Pass|Both)");

        }
        return false;
    }

    private static String screenShotSettings() {
        return Control.exe.getExecSettings().getRunSettings().getScreenShotFor();
    }

    /**
     * takes new screen shot and updates the the json object for that step
     *
     * @param data
     * @param imgSrc
     */
    private void takeSSAndPutDetail(JSONObject data, List<String> optional, String imgSrc) {
        if (optional != null && optional.size() == 3) {
            data.put(RDS.Step.Data.EXPECTED, optional.get(0));
            data.put(RDS.Step.Data.ACTUAL, optional.get(1));
            data.put(RDS.Step.Data.COMPARISION, optional.get(2));
        } else {
            if (optional != null) {
                data.put(RDS.Step.Data.OBJECTS, optional.get(0));
            }
            // Always take screenshots for each step
            if (ReportUtils.takeScreenshot(getPlaywrightDriver(),getWebDriver(), imgSrc)) {
                data.put(RDS.Step.Data.LINK, imgSrc);
            }
        }

    }

    /**
     * finalize the test case execution and create standalone test case report
     * file for upload purpose
     *
     * @return
     */
    @Override
    public Status finalizeReport() {
        updateResults();
        
        try (BufferedWriter bufwriter = new BufferedWriter(new FileWriter(ReportFile));) {
            JSONObject singleTestcasereport = (JSONObject) testCaseData.clone();
            ReportUtils.loadDefaultTheme(singleTestcasereport);
            
            String jsonString = singleTestcasereport.toJSONString();
            // Embed aXe scripts BEFORE inserting JSON data.
            String templateWithAxeScripts = embedAxeDataInHtml(SourceDoc.toString());
            
            // Now replace the data token with JSON
            String tempDoc = templateWithAxeScripts.replace(DATAF, jsonString);
            
            // Fix CSS/JS paths: rewrite ../../../../media to media for standalone reports
            // This matches how HtmlSummaryHandler handles path rewriting
            tempDoc = tempDoc.replaceAll("\\.\\.\\./\\.\\.\\./\\.\\.\\./\\.\\.\\./media", "media");
            
            bufwriter.write(tempDoc);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        printReport();
        return report.getCurrentStatus();
    }
    private static final Logger LOG = Logger.getLogger(TestCaseReport.class.getName());

    /**
     * Embed aXe accessibility JSON data directly in the HTML as script tags
     * This allows the modal viewer to load data without XHR file access restrictions
     */
    private String embedAxeDataInHtml(String htmlContent) {
        try {
            String axePath = FilePath.getCurrentTestCaseAccessibilityLocation();
            
            File axeFolder = new File(axePath);
            
            if (!axeFolder.exists()) {
                return htmlContent;
            }
            
            File[] axeFiles = null;
            if (axeFolder.exists()) {
                // Find all axe-results.json files
                axeFiles = axeFolder.listFiles((dir, name) -> name.endsWith("axe-results.json"));
                if (axeFiles == null || axeFiles.length == 0) {
                    return htmlContent;
                }
            }
            
            StringBuilder axeScriptTags = new StringBuilder();
            
            if (axeFiles != null) {
                for (File axeFile : axeFiles) {
                    try {
                        // Read the JSON content
                        String jsonContent = new String(Files.readAllBytes(axeFile.toPath()), StandardCharsets.UTF_8);
                        // Prevent </script> from terminating the script tag early
                        jsonContent = jsonContent.replace("</script", "<\\/script");
                        
                        // Extract reusable name from filename
                        // Format: {scenario}_{reusable-name}_axe-results.json
                        String fileName = axeFile.getName();
                        String reusablePart = fileName.replace("_axe-results.json", "");
                        
                        // Extract just the reusable name (everything after the last underscore that's not part of "axe")
                        // For "Mortgage Calculator_Your Plans_axe-results.json", we want "Your Plans"
                        String[] parts = reusablePart.split("_");
                        if (parts.length >= 2) {
                            // Join everything except the first part (scenario name) with underscores
                            StringBuilder reusableName = new StringBuilder();
                            for (int i = 1; i < parts.length; i++) {
                                if (i > 1) reusableName.append("_");
                                reusableName.append(parts[i]);
                            }
                            
                            // Sanitize for ID: replace non-alphanumeric with hyphen
                            String sanitizedId = reusableName.toString().replaceAll("[^a-zA-Z0-9]", "-");
                            
                            // Create script tag with embedded JSON
                            axeScriptTags.append("<script type=\"application/json\" id=\"axe-data-")
                                .append(sanitizedId)
                                .append("\">\n")
                                .append(jsonContent)
                                .append("\n</script>\n");
                        }
                    } catch (Exception e) {
                        LOG.log(Level.WARNING, "Error reading aXe file: " + axeFile.getName(), e);
                    }
                }
            }
            
            // Insert script tags before closing </head> tag
            if (axeScriptTags.length() > 0) {
                htmlContent = htmlContent.replace("</head>", axeScriptTags.toString() + "</head>");
            }
            return htmlContent;
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error embedding aXe data", e);
            return htmlContent;
        }
    }

    /**
     * Extract trace data from traces.zip file
     * Parses trace.trace file and extracts actions, screenshots, and timing information
     */
    private JSONObject getTraceData() {
        try {
            String tracePath = (String) testCaseData.get(TestCase.TRACE_PATH);
            if (tracePath == null || tracePath.isEmpty()) {
                return null;
            }
            
            // Construct full path from relative path
            String currentResultsPath = FilePath.getCurrentResultsPath();
            String fullTracePath = currentResultsPath + tracePath;
            
            File traceZipFile = new File(fullTracePath);
            if (!traceZipFile.exists()) {
                return null;
            }
            
            JSONObject traceData = new JSONObject();
            JSONArray actions = new JSONArray();
            JSONArray screenshots = new JSONArray();
            long startTimestamp = Long.MAX_VALUE;
            long endTimestamp = 0;
            
            // Extract trace data from zip
            try (java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile(traceZipFile)) {
                // Get list of screenshot resources
                java.util.Enumeration<? extends java.util.zip.ZipEntry> entries = zipFile.entries();
                
                while (entries.hasMoreElements()) {
                    java.util.zip.ZipEntry entry = entries.nextElement();
                    
                    if (entry.getName().startsWith("resources/page@") && entry.getName().endsWith(".jpeg")) {
                        // Extract timestamp from filename: resources/page@{hash}-{timestamp}.jpeg
                        String filename = entry.getName();
                        String[] parts = filename.substring(filename.lastIndexOf('-') + 1).replace(".jpeg", "").split("");
                        if (parts.length > 0) {
                            try {
                                String timestamp = filename.substring(filename.lastIndexOf('-') + 1).replace(".jpeg", "");
                                JSONObject screenshot = new JSONObject();
                                screenshot.put("file", filename);
                                screenshot.put("timestamp", timestamp);
                                screenshots.add(screenshot);
                                
                                long ts = Long.parseLong(timestamp);
                                startTimestamp = Math.min(startTimestamp, ts);
                                endTimestamp = Math.max(endTimestamp, ts);
                            } catch (NumberFormatException e) {
                                // Skip invalid timestamps
                            }
                        }
                    }
                }
                
                // Parse trace.trace file for actions
                java.util.zip.ZipEntry traceEntry = zipFile.getEntry("trace.trace");
                if (traceEntry != null) {
                    java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(zipFile.getInputStream(traceEntry)));
                    String line;
                    int actionCount = 0;
                    
                    while ((line = reader.readLine()) != null && actionCount < 50) { // Limit to first 50 actions for performance
                        try {
                            JSONObject event = (JSONObject) new org.json.simple.parser.JSONParser().parse(line);
                            String type = (String) event.get("type");
                            
                            // Extract action events
                            if ("action".equals(type)) {
                                JSONObject action = new JSONObject();
                                action.put("method", event.get("method"));
                                action.put("params", event.get("params"));
                                action.put("startTime", event.get("startTime"));
                                action.put("endTime", event.get("endTime"));
                                actions.add(action);
                                actionCount++;
                            }
                        } catch (Exception e) {
                            // Skip invalid JSON lines
                        }
                    }
                    reader.close();
                }
            }
            
            // Build trace data object
            traceData.put("actions", actions);
            traceData.put("screenshots", screenshots);
            traceData.put("actionCount", actions.size());
            traceData.put("screenshotCount", screenshots.size());
            
            if (startTimestamp < Long.MAX_VALUE && endTimestamp > 0) {
                traceData.put("duration", (endTimestamp - startTimestamp) + "ms");
            }
            
            return !actions.isEmpty() || !screenshots.isEmpty() ? traceData : null;
            
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * update the test case execution details to the json DATA file
     *
     * @return
     */
    private void updateResults() {
        String endTime = DateTimeUtils.DateTimeNow();
        String exeTime = startTime().timeRun();
        testCaseData.put(TestCase.STEPS, Steps);
        testCaseData.put(TestCase.END_TIME, endTime);
        testCaseData.put(TestCase.EXE_TIME, exeTime);
        testCaseData.put(TestCase.ITERATIONS, iterCounter);
        testCaseData.put(TestCase.NO_OF_TESTS, getStepCount());
        testCaseData.put(TestCase.NO_OF_FAIL_TESTS, String.valueOf(this.FailedSteps));
        testCaseData.put(TestCase.NO_OF_PASS_TESTS, String.valueOf(this.DoneSteps + this.PassedSteps));
        testCaseData.put(TestCase.STATUS, getCurrentStatus().toString());
        
        // Add video path if video recording is enabled
        if (isVideoEnabled()) {
            // Try to get video path - first from captured value, then from getVideoPathForTestCase
            String videoPath = capturedVideoPath;
            
            // If not captured yet, try to get it now (before browser closes completely)
            if (videoPath == null || videoPath.trim().isEmpty()) {
                videoPath = getVideoPathForTestCase();
            }
            
            // Convert to relative path without validating existence (video file may not be fully written yet)
            if (videoPath != null && !videoPath.isEmpty()) {
                String relativePath = convertToRelativePath(videoPath);
                if (relativePath != null && !relativePath.isEmpty()) {
                    testCaseData.put(TestCase.VIDEO_PATH, relativePath);
                }
            }
        }
        
        // Add trace path if tracing is enabled
        if (isTracingEnabled()) {
            String tracePath = capturedTracePath;
            if (tracePath == null || tracePath.trim().isEmpty()) {
                tracePath = getTracePathForTestCase();
            }
            
            // Convert to relative path without validating existence (trace file may not be fully written yet)
            if (tracePath != null && !tracePath.isEmpty()) {
                String relativePath = convertToRelativePath(tracePath);
                if (relativePath != null && !relativePath.isEmpty()) {
                    testCaseData.put(TestCase.TRACE_PATH, relativePath);
                    
                    // Extract and add trace data for inline viewer (only if file exists now)
                    JSONObject traceData = getTraceData();
                    if (traceData != null) {
                        testCaseData.put(TestCase.TRACE_DATA, traceData);
                    }
                }
            }
        }

    }

    private DateTimeUtils startTime() {
        return report.startTime;
    }

    private void printReport() {
        System.out.println("\n---------------------------------------------------");
        print("Testcase Name", testCaseData.get(TestCase.SCENARIO_NAME)
                + ":" + testCaseData.get(TestCase.TESTCASE_NAME));
        print("Executed Steps", testCaseData.get(TestCase.NO_OF_TESTS));
        print("Passed Steps", testCaseData.get(TestCase.NO_OF_PASS_TESTS));
        print("Failed Steps", testCaseData.get(TestCase.NO_OF_FAIL_TESTS));
        print("Time Taken", testCaseData.get(TestCase.EXE_TIME));
        System.out.println("-----------------------------------------------------\n");
    }

    private void print(String key, Object val) {
        System.out.println(String.format("%-20s : %s", key, val));
    }

    @Override
    public Object getData() {
        return testCaseData;
    }

    @Override
    public File getFile() {
        return ReportFile;
    }

    @Override
    public Status getCurrentStatus() {
        if (FailedSteps > 0 || (PassedSteps + DoneSteps) == 0) {
            return Status.FAIL;
        } else {
            return Status.PASS;
        }
    }
}

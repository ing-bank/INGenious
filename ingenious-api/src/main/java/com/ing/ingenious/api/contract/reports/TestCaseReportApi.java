package com.ing.ingenious.api.contract.reports;

import com.ing.ingenious.api.status.Status;
import java.io.File;
import java.util.List;

/**
 * Contract for reporting and logging test case execution details.
 * <p>
 * <b>API-Plugin Contract:</b> All methods are required for integration with the reporting system. Implementations must provide the described behavior for correct plugin and framework operation.
 * </p>
 */
public interface TestCaseReportApi {
    /**
     * Updates the test log with a step name, description, and status.
     * @param stepName the name of the test step
     * @param stepDescription the description of the step
     * @param state the status of the step
     */
    void updateTestLog(String stepName, String stepDescription, Status state);

    /**
     * Updates the test log with a step name, description, status, and an optional link (e.g., screenshot).
     * @param stepName the name of the test step
     * @param stepDescription the description of the step
     * @param state the status of the step
     * @param optionalLink a link to additional information (e.g., screenshot)
     */
    void updateTestLog(String stepName, String stepDescription, Status state, String optionalLink);

    /**
     * Updates the test log with a step name, description, status, and a list of optional details.
     * @param stepName the name of the test step
     * @param stepDescription the description of the step
     * @param state the status of the step
     * @param optional a list of additional details
     */
    void updateTestLog(String stepName, String stepDescription, Status state, List<String> optional);

    /**
     * Updates the test log with a step name, description, status, an optional link, and a list of optional details.
     * @param stepName the name of the test step
     * @param stepDescription the description of the step
     * @param state the status of the step
     * @param optionalLink a link to additional information (e.g., screenshot)
     * @param optional a list of additional details
     */
    void updateTestLog(String stepName, String stepDescription, Status state, String optionalLink, List<String> optional);

    /**
     * Finalizes the test report and returns the report object.
     * @return the finalized report (cast to the implementation's report type)
     */
    Object finalizeReport();

    /**
     * Marks the start of a test iteration.
     * @param iteration the iteration number
     */
    void startIteration(int iteration);

    /**
     * Marks the start of a test component.
     * @param component the component name
     * @param desc the component description
     */
    void startComponent(String component, String desc);

    /**
     * Marks the end of a test component.
     * @param component the component name
     */
    void endComponent(String component);

    /**
     * Marks the end of a test iteration.
     * @param iteration the iteration number
     */
    void endIteration(int iteration);

    /**
     * Returns the Playwright driver instance used for the test.
     * @return the Playwright driver as Object (cast to Playwright/Page/BrowserContext as needed)
     */
    Object getPlaywrightDriver();

    /**
     * Returns the WebDriver instance used for the test.
     * @return the WebDriver as Object (cast to WebDriver as needed)
     */
    Object getWebDriver();

    /**
     * Returns the current iteration number.
     * @return the iteration number
     */
    int getIter();

    /**
     * Returns the test data object for the current test.
     * @return the test data as Object (cast to the implementation's data type)
     */
    Object getData();

    /**
     * Returns the file associated with the test (e.g., report file).
     * @return the file
     */
    File getFile();

    /**
     * Returns the name of the screenshot file for the current step.
     * @return the screenshot file name
     */
    String getScreenShotName();

    /**
     * Returns the name of a new screenshot file for the current step.
     * @return the new screenshot file name
     */
    String getNewScreenShotName();

    /**
     * Returns the file name of the web service response for the test.
     * @return the web service response file name
     */
    String getWebserviceResponseFileName();

    /**
     * Returns the file name of the web service request for the test.
     * @return the web service request file name
     */
    String getWebserviceRequestFileName();

    /**
     * Returns the file name of the PDF result for the test.
     * @return the PDF result file name
     */
    String getPdfResultName();

    /**
     * Returns the log file name for the test.
     * @return the log file name
     */
    String getLogFileName();

    /**
     * Returns the report location file.
     * @return the report location file
     */
    File getReportLoc();

    /**
     * Returns the current step object for the test.
     * @return the step object (cast to the implementation's step type)
     */
    Object getStep();

    /**
     * Returns the name of the current test case.
     * @return the test case name
     */
    String getTestCaseName();

    /**
     * Returns the name of the current scenario.
     * @return the scenario name
     */
    String getScenarioName();

    /**
     * Returns the current status object for the test.
     * @return the status object (cast to the implementation's status type)
     */
    Object getCurrentStatus();

    /**
     * Returns whether the current step has passed.
     * @return true if the step passed, false otherwise
     */
    Boolean isStepPassed();

    /**
     * Returns the number of steps in the test case.
     * @return the step count
     */
    int getStepCount();

    // void register(Object testCaseHandler);
    // void register(Object testCaseHandler, boolean primaryHandler);

    /**
     * Returns the test case number (static utility).
     * @return the test case number
     * @throws UnsupportedOperationException always (default implementation)
     */
    static int getTestCaseNumber() { throw new UnsupportedOperationException(); }
}

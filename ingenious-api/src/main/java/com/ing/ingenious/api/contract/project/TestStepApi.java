package com.ing.ingenious.api.contract.project;

/**
 * Contract for a single test-set row, backed by the framework's
 * {@code ExecutionStep}. Read-only: the underlying setters are test-authoring
 * mutators and are intentionally not exposed to plugins.
 */
public interface TestStepApi {

    /** @return the test scenario name */
    String getTestScenarioName();

    /** @return the test case name */
    String getTestCaseName();

    /** @return the configured iteration value */
    String getIteration();

    /** @return the last recorded execution status */
    String getStatus();

    /** @return whether this step is marked for execution */
    String getExecute();

    /** @return the configured platform */
    String getPlatform();

    /** @return the configured browser */
    String getBrowser();
}

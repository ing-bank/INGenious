package com.ing.ingenious.api.contract.execution;

import com.ing.ingenious.api.contract.project.ProjectApi;
import com.ing.ingenious.api.contract.project.ProjectTestDataApi;
import com.ing.ingenious.api.contract.project.TestSetApi;
import com.ing.ingenious.api.contract.settings.ExecutionSettingsApi;

/**
 * Contract for the test runner driving the current execution, backed by the
 * framework's {@code ProjectRunner}.
 */
public interface TestRunnerApi {

    /** @return the project loaded for this run */
    ProjectApi getProject();

    /** @return the execution settings in effect for this run */
    ExecutionSettingsApi getExecSettings();

    /** @return the test set being executed */
    TestSetApi getTestSet();

    /**
     * @return the environment this run resolves test data against: the
     * configured test environment if set, otherwise the project's default
     * environment
     */
    String runEnv();

    /** @return the project-wide test data provider for this run */
    ProjectTestDataApi dataProvider();

    /** @return whether execution should continue past a failed step */
    boolean isContinueOnError();

    /** @return whether this run reuses an already-open browser/driver */
    boolean useExistingBrowser();

    /** @return whether this run is executing in debug (step-through) mode */
    boolean isDebugExe();

    /**
     * Gets the data iterator for the given scenario/test case pair.
     * @param scenario the scenario name
     * @param testcase the test case name
     * @return the data iterator tracking resolved iteration bounds for this scenario/test case
     */
    DataIteratorApi getIterater(String scenario, String testcase);

    /**
     * Gets the data iterator for the given scenario#testcase key.
     * @param key the scenario#testcase key
     * @return the data iterator tracking resolved iteration bounds for this key
     */
    DataIteratorApi getIterater(String key);

    /** @return whether a failed test set should be retried */
    boolean retryExecution();

    /**
     * Updates retry bookkeeping after the run completes.
     * @param passed whether the run passed
     */
    void afterExecution(boolean passed);
}

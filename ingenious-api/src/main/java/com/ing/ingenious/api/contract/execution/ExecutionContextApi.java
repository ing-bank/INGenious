package com.ing.ingenious.api.contract.execution;

import com.ing.ingenious.api.contract.project.ProjectApi;

/**
 * Contract for the current run's execution context, backed by the
 * framework's static {@code Control} state: trigger id, start time,
 * currently loaded project, test runner, and summary report manager.
 */
public interface ExecutionContextApi {

    /** @return the trigger id for the current run */
    String getTriggerId();

    /** @return the start time of the current run */
    String getExecutionStartTime();

    /** @return the currently loaded project, or {@code null} if none is loaded */
    ProjectApi getCurrentProject();

    /** @return the test runner driving the current run */
    TestRunnerApi getRunner();

    /** @return the summary report manager for the current run */
    SummaryReportApi getReportManager();
}

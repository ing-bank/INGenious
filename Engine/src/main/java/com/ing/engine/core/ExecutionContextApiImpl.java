package com.ing.engine.core;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.ProjectApiImpl;
import com.ing.engine.execution.run.TestRunnerApiImpl;
import com.ing.engine.reporting.SummaryReportApiImpl;
import com.ing.ingenious.api.contract.execution.ExecutionContextApi;
import com.ing.ingenious.api.contract.execution.SummaryReportApi;
import com.ing.ingenious.api.contract.execution.TestRunnerApi;
import com.ing.ingenious.api.contract.project.ProjectApi;

/**
 * Delegates the {@link ExecutionContextApi} plugin contract to the
 * framework's static {@link Control} state.
 */
public final class ExecutionContextApiImpl implements ExecutionContextApi {
    public static final ExecutionContextApiImpl INSTANCE = new ExecutionContextApiImpl();

    private ExecutionContextApiImpl() {}

    @Override
    public String getTriggerId() {
        return Control.triggerId;
    }

    @Override
    public String getExecutionStartTime() {
        return Control.executionStartTime;
    }

    @Override
    public ProjectApi getCurrentProject() {
        Project project = Control.getCurrentProject();
        return project == null ? null : new ProjectApiImpl(project);
    }

    @Override
    public TestRunnerApi getRunner() {
        return Control.exe == null ? null : new TestRunnerApiImpl(Control.exe);
    }

    @Override
    public SummaryReportApi getReportManager() {
        return Control.ReportManager == null
            ? null
            : new SummaryReportApiImpl(Control.ReportManager);
    }
}

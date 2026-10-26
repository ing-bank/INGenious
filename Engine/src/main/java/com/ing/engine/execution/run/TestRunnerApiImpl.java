package com.ing.engine.execution.run;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.ProjectApiImpl;
import com.ing.datalib.component.ProjectTestDataApiImpl;
import com.ing.datalib.component.TestSet;
import com.ing.datalib.component.TestSetApiImpl;
import com.ing.datalib.settings.ExecutionSettingsApiImpl;
import com.ing.engine.execution.data.DataIteratorApiImpl;
import com.ing.ingenious.api.contract.execution.DataIteratorApi;
import com.ing.ingenious.api.contract.execution.TestRunnerApi;
import com.ing.ingenious.api.contract.project.ProjectApi;
import com.ing.ingenious.api.contract.project.ProjectTestDataApi;
import com.ing.ingenious.api.contract.project.TestSetApi;
import com.ing.ingenious.api.contract.settings.ExecutionSettingsApi;

/**
 * Delegates the {@link TestRunnerApi} plugin contract to a specific
 * {@link ProjectRunner} instance.
 */
public final class TestRunnerApiImpl implements TestRunnerApi {
    private final ProjectRunner runner;

    public TestRunnerApiImpl(ProjectRunner runner) {
        this.runner = runner;
    }

    @Override
    public ProjectApi getProject() {
        Project project = runner.getProject();
        return project == null ? null : new ProjectApiImpl(project);
    }

    @Override
    public ExecutionSettingsApi getExecSettings() {
        return new ExecutionSettingsApiImpl(runner.getExecSettings());
    }

    @Override
    public TestSetApi getTestSet() {
        TestSet testSet = runner.getTestSet();
        return testSet == null ? null : new TestSetApiImpl(testSet);
    }

    @Override
    public String runEnv() {
        return runner.runEnv();
    }

    @Override
    public ProjectTestDataApi dataProvider() {
        return new ProjectTestDataApiImpl(runner.dataProvider());
    }

    @Override
    public boolean isContinueOnError() {
        return runner.isContinueOnError();
    }

    @Override
    public boolean useExistingBrowser() {
        return runner.useExistingBrowser();
    }

    @Override
    public boolean isDebugExe() {
        return runner.isDebugExe();
    }

    @Override
    public DataIteratorApi getIterater(String scenario, String testcase) {
        return new DataIteratorApiImpl(runner.getIterater(scenario, testcase));
    }

    @Override
    public DataIteratorApi getIterater(String key) {
        return new DataIteratorApiImpl(runner.getIterater(key));
    }

    @Override
    public boolean retryExecution() {
        return runner.retryExecution();
    }

    @Override
    public void afterExecution(boolean passed) {
        runner.afterExecution(passed);
    }
}

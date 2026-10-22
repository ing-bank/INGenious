package com.ing.engine.execution.run;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ing.datalib.component.EnvTestData;
import com.ing.datalib.component.Project;
import com.ing.datalib.component.TestSet;
import com.ing.datalib.settings.ExecutionSettings;
import com.ing.engine.execution.data.DataIterator;
import org.testng.annotations.Test;

/**
 * Tests for {@link TestRunnerApiImpl} — verifies each accessor delegates to
 * the wrapped {@link ProjectRunner} instance.
 */
public class TestRunnerApiImplTest {

    @Test
    public void getProjectShouldWrapTheUnderlyingProject() {
        ProjectRunner runner = mock(ProjectRunner.class);
        Project project = mock(Project.class);
        when(runner.getProject()).thenReturn(project);

        assertThat(new TestRunnerApiImpl(runner).getProject()).isNotNull();
    }

    @Test
    public void getExecSettingsShouldWrapTheUnderlyingSettings() {
        ProjectRunner runner = mock(ProjectRunner.class);
        ExecutionSettings execSettings = mock(ExecutionSettings.class);
        when(runner.getExecSettings()).thenReturn(execSettings);

        assertThat(new TestRunnerApiImpl(runner).getExecSettings()).isNotNull();
    }

    @Test
    public void getTestSetShouldWrapTheUnderlyingTestSet() {
        ProjectRunner runner = mock(ProjectRunner.class);
        TestSet testSet = mock(TestSet.class);
        when(runner.getTestSet()).thenReturn(testSet);

        assertThat(new TestRunnerApiImpl(runner).getTestSet()).isNotNull();
    }

    @Test
    public void runEnvShouldDelegate() {
        ProjectRunner runner = mock(ProjectRunner.class);
        when(runner.runEnv()).thenReturn("QA");

        assertThat(new TestRunnerApiImpl(runner).runEnv()).isEqualTo("QA");
    }

    @Test
    public void dataProviderShouldWrapTheUnderlyingEnvTestData() {
        ProjectRunner runner = mock(ProjectRunner.class);
        EnvTestData envTestData = mock(EnvTestData.class);
        when(runner.dataProvider()).thenReturn(envTestData);

        assertThat(new TestRunnerApiImpl(runner).dataProvider()).isNotNull();
    }

    @Test
    public void flagAccessorsShouldDelegate() {
        ProjectRunner runner = mock(ProjectRunner.class);
        when(runner.isContinueOnError()).thenReturn(true);
        when(runner.useExistingBrowser()).thenReturn(true);
        when(runner.isDebugExe()).thenReturn(false);

        TestRunnerApiImpl api = new TestRunnerApiImpl(runner);

        assertThat(api.isContinueOnError()).isTrue();
        assertThat(api.useExistingBrowser()).isTrue();
        assertThat(api.isDebugExe()).isFalse();
    }

    @Test
    public void getIteraterOverloadsShouldWrapTheUnderlyingIterator() {
        ProjectRunner runner = mock(ProjectRunner.class);
        DataIterator iterator = mock(DataIterator.class);
        when(runner.getIterater("Scenario1#TestCase1")).thenReturn(iterator);
        when(runner.getIterater("Scenario1", "TestCase1")).thenReturn(iterator);

        TestRunnerApiImpl api = new TestRunnerApiImpl(runner);

        assertThat(api.getIterater("Scenario1#TestCase1")).isNotNull();
        assertThat(api.getIterater("Scenario1", "TestCase1")).isNotNull();
    }

    @Test
    public void retryExecutionShouldDelegate() {
        ProjectRunner runner = mock(ProjectRunner.class);
        when(runner.retryExecution()).thenReturn(true);

        assertThat(new TestRunnerApiImpl(runner).retryExecution()).isTrue();
    }

    @Test
    public void afterExecutionShouldDelegate() {
        ProjectRunner runner = mock(ProjectRunner.class);

        new TestRunnerApiImpl(runner).afterExecution(true);

        verify(runner).afterExecution(true);
    }
}

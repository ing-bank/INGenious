package com.ing.engine.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.ing.datalib.component.Project;
import com.ing.engine.execution.run.ProjectRunner;
import com.ing.engine.reporting.SummaryReport;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

/**
 * Tests for {@link ExecutionContextApiImpl} — verifies each accessor
 * delegates to the framework's static {@link Control} state.
 */
public class ExecutionContextApiImplTest {
    private final ExecutionContextApiImpl api = ExecutionContextApiImpl.INSTANCE;

    @AfterMethod
    public void tearDown() {
        Control.triggerId = null;
        Control.executionStartTime = null;
        Control.exe = null;
        Control.ReportManager = null;
    }

    @Test
    public void instanceShouldBeNonNullAndStable() {
        assertThat(ExecutionContextApiImpl.INSTANCE).isNotNull();
        assertThat(ExecutionContextApiImpl.INSTANCE).isSameAs(ExecutionContextApiImpl.INSTANCE);
    }

    @Test
    public void getTriggerIdShouldDelegate() {
        Control.triggerId = "trigger-1";
        assertThat(api.getTriggerId()).isEqualTo("trigger-1");
    }

    @Test
    public void getExecutionStartTimeShouldDelegate() {
        Control.executionStartTime = "2026-01-01T00:00:00";
        assertThat(api.getExecutionStartTime()).isEqualTo("2026-01-01T00:00:00");
    }

    @Test
    public void getCurrentProjectShouldReturnNullWhenNoRunnerIsLoaded() {
        Control.exe = null;
        assertThat(api.getCurrentProject()).isNull();
    }

    @Test
    public void getCurrentProjectShouldWrapTheRunnersProject() {
        ProjectRunner runner = mock(ProjectRunner.class);
        Project project = mock(Project.class);
        org.mockito.Mockito.when(runner.getProject()).thenReturn(project);
        Control.exe = runner;

        assertThat(api.getCurrentProject()).isNotNull();
    }

    @Test
    public void getRunnerShouldReturnNullWhenNoneIsLoaded() {
        Control.exe = null;
        assertThat(api.getRunner()).isNull();
    }

    @Test
    public void getRunnerShouldWrapTheStaticRunner() {
        Control.exe = mock(ProjectRunner.class);
        assertThat(api.getRunner()).isNotNull();
    }

    @Test
    public void getReportManagerShouldReturnNullWhenNoneIsLoaded() {
        Control.ReportManager = null;
        assertThat(api.getReportManager()).isNull();
    }

    @Test
    public void getReportManagerShouldWrapTheStaticReportManager() {
        Control.ReportManager = mock(SummaryReport.class);
        assertThat(api.getReportManager()).isNotNull();
    }
}

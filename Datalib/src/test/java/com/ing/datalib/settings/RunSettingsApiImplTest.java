package com.ing.datalib.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.testng.annotations.Test;

/**
 * Tests for {@link RunSettingsApiImpl} — verifies each accessor delegates to
 * the wrapped {@link RunSettings} instance, and the generic
 * get/put property path uses {@code getProperty}/{@code setProperty}.
 */
public class RunSettingsApiImplTest {

    @Test
    public void readOnlyAccessorsShouldDelegate() {
        RunSettings runSettings = mock(RunSettings.class);
        when(runSettings.isVideoEnabled()).thenReturn(true);
        when(runSettings.isTracingEnabled()).thenReturn(true);
        when(runSettings.isHARrecordingEnabled()).thenReturn(false);
        when(runSettings.isPerformanceLogEnabled()).thenReturn(false);
        when(runSettings.isBddReportEnabled()).thenReturn(true);
        when(runSettings.isModernReport()).thenReturn(true);
        when(runSettings.isGridExecution()).thenReturn(false);
        when(runSettings.getRemoteGridURL()).thenReturn("wss://cdp.lambdatest.com");
        when(runSettings.useExistingDriver()).thenReturn(false);
        when(runSettings.getTakeFullPageScreenShot()).thenReturn(true);
        when(runSettings.getScreenShotFor()).thenReturn("Both");
        when(runSettings.getThreadCount()).thenReturn(4);
        when(runSettings.getExecutionTimeOut()).thenReturn(300);
        when(runSettings.getExecutionMode()).thenReturn("Local");
        when(runSettings.getIterationMode()).thenReturn("ContinueOnError");
        when(runSettings.getRerunTimes()).thenReturn(2);
        when(runSettings.getTestEnv()).thenReturn("QA");

        RunSettingsApiImpl api = new RunSettingsApiImpl(runSettings);

        assertThat(api.isVideoEnabled()).isTrue();
        assertThat(api.isTracingEnabled()).isTrue();
        assertThat(api.isHARrecordingEnabled()).isFalse();
        assertThat(api.isPerformanceLogEnabled()).isFalse();
        assertThat(api.isBddReportEnabled()).isTrue();
        assertThat(api.isModernReport()).isTrue();
        assertThat(api.isGridExecution()).isFalse();
        assertThat(api.getRemoteGridURL()).isEqualTo("wss://cdp.lambdatest.com");
        assertThat(api.useExistingDriver()).isFalse();
        assertThat(api.getTakeFullPageScreenShot()).isTrue();
        assertThat(api.getScreenShotFor()).isEqualTo("Both");
        assertThat(api.getThreadCount()).isEqualTo(4);
        assertThat(api.getExecutionTimeOut()).isEqualTo(300);
        assertThat(api.getExecutionMode()).isEqualTo("Local");
        assertThat(api.getIterationMode()).isEqualTo("ContinueOnError");
        assertThat(api.getRerunTimes()).isEqualTo(2);
        assertThat(api.getTestEnv()).isEqualTo("QA");
    }

    @Test
    public void getPropertyShouldDelegate() {
        RunSettings runSettings = mock(RunSettings.class);
        when(runSettings.getProperty("CustomKey")).thenReturn("CustomValue");

        assertThat(new RunSettingsApiImpl(runSettings).getProperty("CustomKey"))
            .isEqualTo("CustomValue");
    }

    @Test
    public void putShouldDelegateToSetProperty() {
        RunSettings runSettings = mock(RunSettings.class);

        new RunSettingsApiImpl(runSettings).put("CustomKey", "CustomValue");

        verify(runSettings).setProperty("CustomKey", "CustomValue");
    }
}

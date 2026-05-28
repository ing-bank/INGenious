package com.ing.datalib.settings;

import static org.assertj.core.api.Assertions.assertThat;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests RunSettings typed getters and their default values.
 * RunSettings extends AbstractPropSettings which loads from a file,
 * but with a non-existent location it stays empty, exercising defaults.
 */
public class RunSettingsTest {

    private RunSettings rs;

    @BeforeMethod
    public void setUp() {
        // Use a non-existent directory so no file is loaded — tests defaults
        rs = new RunSettings("/nonexistent/path");
    }

    @Test
    public void testDefaultExecutionMode() {
        assertThat(rs.getExecutionMode()).isEqualTo("Local");
    }

    @Test
    public void testSetAndGetExecutionMode() {
        rs.setExecutionMode("grid");
        assertThat(rs.getExecutionMode()).isEqualTo("grid");
    }

    @Test
    public void testIsGridExecution() {
        assertThat(rs.isGridExecution()).isFalse();
        rs.setExecutionMode("Grid");
        assertThat(rs.isGridExecution()).isTrue();
    }

    @Test
    public void testDefaultExecutionTimeOut() {
        assertThat(rs.getExecutionTimeOut()).isEqualTo(300);
    }

    @Test
    public void testSetAndGetExecutionTimeOut() {
        rs.setExecutionTimeOut("600");
        assertThat(rs.getExecutionTimeOut()).isEqualTo(600);
    }

    @Test
    public void testDefaultThreadCount() {
        assertThat(rs.getThreadCount()).isEqualTo(1);
    }

    @Test
    public void testSetAndGetThreadCount() {
        rs.setThreadCount("4");
        assertThat(rs.getThreadCount()).isEqualTo(4);
    }

    @Test
    public void testDefaultScreenShotFor() {
        assertThat(rs.getScreenShotFor()).isEqualTo("Both");
    }

    @Test
    public void testDefaultUseExistingDriver() {
        assertThat(rs.useExistingDriver()).isFalse();
    }

    @Test
    public void testSetUseExistingDriver() {
        rs.useExistingDriver(true);
        assertThat(rs.useExistingDriver()).isTrue();
    }

    @Test
    public void testDefaultTakeFullPageScreenShot() {
        assertThat(rs.getTakeFullPageScreenShot()).isTrue();
    }

    @Test
    public void testDefaultPerformanceLogDisabled() {
        assertThat(rs.isPerformanceLogEnabled()).isFalse();
    }

    @Test
    public void testSetPerformanceLog() {
        rs.setReportPerformanceLog(true);
        assertThat(rs.isPerformanceLogEnabled()).isTrue();
    }

    @Test
    public void testDefaultVideoDisabled() {
        assertThat(rs.isVideoEnabled()).isFalse();
    }

    @Test
    public void testSetVideoEnabled() {
        rs.setVideoEnabled(true);
        assertThat(rs.isVideoEnabled()).isTrue();
    }

    @Test
    public void testDefaultTracingDisabled() {
        assertThat(rs.isTracingEnabled()).isFalse();
    }

    @Test
    public void testDefaultHARDisabled() {
        assertThat(rs.isHARrecordingEnabled()).isFalse();
    }

    @Test
    public void testDefaultBddReportDisabled() {
        assertThat(rs.isBddReportEnabled()).isFalse();
    }

    @Test
    public void testDefaultRemoteGridURL() {
        assertThat(rs.getRemoteGridURL()).isEqualTo("wss://cdp.lambdatest.com");
    }

    @Test
    public void testDefaultIterationMode() {
        assertThat(rs.getIterationMode()).isEqualTo("ContinueOnError");
    }

    @Test
    public void testDefaultRerunTimes() {
        assertThat(rs.getRerunTimes()).isEqualTo(0);
    }

    @Test
    public void testDefaultTestEnv() {
        assertThat(rs.getTestEnv()).isEqualTo("Default");
    }

    @Test
    public void testDefaultMailSend() {
        assertThat(rs.isMailSend()).isFalse();
    }

    @Test
    public void testDefaultExcelReport() {
        assertThat(rs.isExcelReport()).isFalse();
    }

    @Test
    public void testDefaultRPUpdate() {
        assertThat(rs.isRPUpdate()).isFalse();
    }

    @Test
    public void testDefaultAutoHealEnabled() {
        assertThat(rs.isAutoHealEnabled()).isFalse();
    }

    @Test
    public void testDefaultExtentReport() {
        assertThat(rs.isExtentReport()).isFalse();
    }

    @Test
    public void testDefaultAzureEnabled() {
        assertThat(rs.isAzureEnabled()).isFalse();
    }

    @Test
    public void testDefaultSlackNotification() {
        assertThat(rs.isSendNotification()).isFalse();
    }

    @Test
    public void testSetAndGetMultipleSettings() {
        rs.setExecutionMode("Grid");
        rs.setThreadCount("8");
        rs.setExecutionTimeOut("120");
        rs.setTestEnv("Staging");
        rs.setMailSend(true);
        rs.setExtentReport(true);

        assertThat(rs.getExecutionMode()).isEqualTo("Grid");
        assertThat(rs.getThreadCount()).isEqualTo(8);
        assertThat(rs.getExecutionTimeOut()).isEqualTo(120);
        assertThat(rs.getTestEnv()).isEqualTo("Staging");
        assertThat(rs.isMailSend()).isTrue();
        assertThat(rs.isExtentReport()).isTrue();
    }
}

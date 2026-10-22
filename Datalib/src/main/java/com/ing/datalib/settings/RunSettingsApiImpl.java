package com.ing.datalib.settings;

import com.ing.ingenious.api.contract.settings.RunSettingsApi;

/**
 * Delegates the {@link RunSettingsApi} plugin contract to a specific
 * {@link RunSettings} instance.
 */
public final class RunSettingsApiImpl implements RunSettingsApi {
    private final RunSettings runSettings;

    public RunSettingsApiImpl(RunSettings runSettings) {
        this.runSettings = runSettings;
    }

    @Override
    public boolean isVideoEnabled() {
        return runSettings.isVideoEnabled();
    }

    @Override
    public boolean isTracingEnabled() {
        return runSettings.isTracingEnabled();
    }

    @Override
    public boolean isHARrecordingEnabled() {
        return runSettings.isHARrecordingEnabled();
    }

    @Override
    public boolean isPerformanceLogEnabled() {
        return runSettings.isPerformanceLogEnabled();
    }

    @Override
    public boolean isBddReportEnabled() {
        return runSettings.isBddReportEnabled();
    }

    @Override
    public boolean isModernReport() {
        return runSettings.isModernReport();
    }

    @Override
    public boolean isGridExecution() {
        return runSettings.isGridExecution();
    }

    @Override
    public String getRemoteGridURL() {
        return runSettings.getRemoteGridURL();
    }

    @Override
    public boolean useExistingDriver() {
        return runSettings.useExistingDriver();
    }

    @Override
    public boolean getTakeFullPageScreenShot() {
        return runSettings.getTakeFullPageScreenShot();
    }

    @Override
    public String getScreenShotFor() {
        return runSettings.getScreenShotFor();
    }

    @Override
    public int getThreadCount() {
        return runSettings.getThreadCount();
    }

    @Override
    public int getExecutionTimeOut() {
        return runSettings.getExecutionTimeOut();
    }

    @Override
    public String getExecutionMode() {
        return runSettings.getExecutionMode();
    }

    @Override
    public String getIterationMode() {
        return runSettings.getIterationMode();
    }

    @Override
    public int getRerunTimes() {
        return runSettings.getRerunTimes();
    }

    @Override
    public String getTestEnv() {
        return runSettings.getTestEnv();
    }

    @Override
    public String getProperty(String key) {
        return runSettings.getProperty(key);
    }

    @Override
    public void put(String key, String value) {
        runSettings.setProperty(key, value);
    }
}

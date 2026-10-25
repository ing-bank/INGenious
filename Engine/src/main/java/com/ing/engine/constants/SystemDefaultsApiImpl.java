package com.ing.engine.constants;

import com.ing.ingenious.api.contract.SystemDefaultsApi;
import java.time.Duration;

/**
 * Delegates the {@link SystemDefaultsApi} plugin contract to the existing
 * {@link SystemDefaults} static fields and methods.
 */
public final class SystemDefaultsApiImpl implements SystemDefaultsApi {
    public static final SystemDefaultsApiImpl INSTANCE = new SystemDefaultsApiImpl();

    private SystemDefaultsApiImpl() {}

    @Override
    public Duration getWaitTime() {
        return SystemDefaults.waitTime;
    }

    @Override
    public void setWaitTime(Duration waitTime) {
        SystemDefaults.waitTime = waitTime;
    }

    @Override
    public Duration getElementWaitTime() {
        return SystemDefaults.elementWaitTime;
    }

    @Override
    public void setElementWaitTime(Duration elementWaitTime) {
        SystemDefaults.elementWaitTime = elementWaitTime;
    }

    @Override
    public boolean isStopExecution() {
        return SystemDefaults.stopExecution.get();
    }

    @Override
    public void setStopExecution(boolean stopExecution) {
        SystemDefaults.stopExecution.set(stopExecution);
    }

    @Override
    public boolean isPauseExecution() {
        return SystemDefaults.pauseExecution.get();
    }

    @Override
    public void setPauseExecution(boolean pauseExecution) {
        SystemDefaults.pauseExecution.set(pauseExecution);
    }

    @Override
    public boolean isStopCurrentIteration() {
        return SystemDefaults.stopCurrentIteration.get();
    }

    @Override
    public void setStopCurrentIteration(boolean stopCurrentIteration) {
        SystemDefaults.stopCurrentIteration.set(stopCurrentIteration);
    }

    @Override
    public boolean isNextStepFlag() {
        return SystemDefaults.nextStepflag.get();
    }

    @Override
    public void setNextStepFlag(boolean nextStepFlag) {
        SystemDefaults.nextStepflag.set(nextStepFlag);
    }

    @Override
    public boolean isReportComplete() {
        return SystemDefaults.reportComplete.get();
    }

    @Override
    public void setReportComplete(boolean reportComplete) {
        SystemDefaults.reportComplete.set(reportComplete);
    }

    @Override
    public boolean isDebugMode() {
        return SystemDefaults.debugMode.get();
    }

    @Override
    public void setDebugMode(boolean debugMode) {
        SystemDefaults.debugMode.set(debugMode);
    }

    @Override
    public String getCommandLineVariable(String key) {
        return SystemDefaults.CLVars.get(key);
    }

    @Override
    public void setCommandLineVariable(String key, String value) {
        SystemDefaults.CLVars.put(key, value);
    }

    @Override
    public void removeCommandLineVariable(String key) {
        SystemDefaults.CLVars.remove(key);
    }

    @Override
    public boolean hasCommandLineVariable(String key) {
        return SystemDefaults.CLVars.containsKey(key);
    }

    @Override
    public String getEnvironmentVariable(String key) {
        return SystemDefaults.EnvVars.get(key);
    }

    @Override
    public void setEnvironmentVariable(String key, String value) {
        SystemDefaults.EnvVars.put(key, value);
    }

    @Override
    public void removeEnvironmentVariable(String key) {
        SystemDefaults.EnvVars.remove(key);
    }

    @Override
    public boolean hasEnvironmentVariable(String key) {
        return SystemDefaults.EnvVars.containsKey(key);
    }

    @Override
    public String getBuildVersion() {
        return SystemDefaults.getBuildVersion();
    }

    @Override
    public boolean isDebug() {
        return SystemDefaults.debug();
    }

    @Override
    public boolean canLaunchSummary() {
        return SystemDefaults.canLaunchSummary();
    }

    @Override
    public void printSystemInfo() {
        SystemDefaults.printSystemInfo();
    }

    @Override
    public void pollWait() {
        SystemDefaults.pollWait();
    }
}

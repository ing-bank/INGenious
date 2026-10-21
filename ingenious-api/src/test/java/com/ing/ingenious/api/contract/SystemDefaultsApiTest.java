package com.ing.ingenious.api.contract;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contract test for {@link SystemDefaultsApi}. Uses a minimal in-test
 * implementation to verify the interface can be implemented and dispatched
 * without pulling in any Engine dependency.
 */
class SystemDefaultsApiTest {

    private final Map<String, String> clVars = new HashMap<>();
    private final Map<String, String> envVars = new HashMap<>();
    private Duration waitTime = Duration.ofSeconds(10);
    private Duration elementWaitTime = Duration.ofSeconds(10);
    private boolean stopExecution;
    private boolean pauseExecution;
    private boolean debugMode;

    private final SystemDefaultsApi api = new SystemDefaultsApi() {
        @Override
        public Duration getWaitTime() {
            return waitTime;
        }

        @Override
        public void setWaitTime(Duration value) {
            waitTime = value;
        }

        @Override
        public Duration getElementWaitTime() {
            return elementWaitTime;
        }

        @Override
        public void setElementWaitTime(Duration value) {
            elementWaitTime = value;
        }

        @Override
        public boolean isStopExecution() {
            return stopExecution;
        }

        @Override
        public void setStopExecution(boolean value) {
            stopExecution = value;
        }

        @Override
        public boolean isPauseExecution() {
            return pauseExecution;
        }

        @Override
        public void setPauseExecution(boolean value) {
            pauseExecution = value;
        }

        @Override
        public boolean isStopCurrentIteration() {
            return false;
        }

        @Override
        public void setStopCurrentIteration(boolean value) {}

        @Override
        public boolean isNextStepFlag() {
            return true;
        }

        @Override
        public void setNextStepFlag(boolean value) {}

        @Override
        public boolean isReportComplete() {
            return false;
        }

        @Override
        public void setReportComplete(boolean value) {}

        @Override
        public boolean isDebugMode() {
            return debugMode;
        }

        @Override
        public void setDebugMode(boolean value) {
            debugMode = value;
        }

        @Override
        public String getCommandLineVariable(String key) {
            return clVars.get(key);
        }

        @Override
        public void setCommandLineVariable(String key, String value) {
            clVars.put(key, value);
        }

        @Override
        public void removeCommandLineVariable(String key) {
            clVars.remove(key);
        }

        @Override
        public boolean hasCommandLineVariable(String key) {
            return clVars.containsKey(key);
        }

        @Override
        public String getEnvironmentVariable(String key) {
            return envVars.get(key);
        }

        @Override
        public void setEnvironmentVariable(String key, String value) {
            envVars.put(key, value);
        }

        @Override
        public void removeEnvironmentVariable(String key) {
            envVars.remove(key);
        }

        @Override
        public boolean hasEnvironmentVariable(String key) {
            return envVars.containsKey(key);
        }

        @Override
        public String getBuildVersion() {
            return "3.1.0";
        }

        @Override
        public boolean isDebug() {
            return debugMode;
        }

        @Override
        public boolean canLaunchSummary() {
            return !clVars.containsKey("dontLaunchSummary");
        }

        @Override
        public void printSystemInfo() {}

        @Override
        public void pollWait() {}
    };

    @Test
    void timingDefaultsShouldBeDispatched() {
        api.setWaitTime(Duration.ofSeconds(30));
        api.setElementWaitTime(Duration.ofSeconds(60));
        assertEquals(Duration.ofSeconds(30), api.getWaitTime());
        assertEquals(Duration.ofSeconds(60), api.getElementWaitTime());
    }

    @Test
    void executionControlFlagsShouldBeDispatched() {
        assertFalse(api.isStopExecution());
        api.setStopExecution(true);
        assertTrue(api.isStopExecution());

        assertFalse(api.isPauseExecution());
        api.setPauseExecution(true);
        assertTrue(api.isPauseExecution());
    }

    @Test
    void commandLineVariablesShouldBeKeyScoped() {
        assertFalse(api.hasCommandLineVariable("debug"));
        api.setCommandLineVariable("debug", "true");
        assertTrue(api.hasCommandLineVariable("debug"));
        assertEquals("true", api.getCommandLineVariable("debug"));
        api.removeCommandLineVariable("debug");
        assertFalse(api.hasCommandLineVariable("debug"));
    }

    @Test
    void environmentVariablesShouldBeKeyScoped() {
        assertFalse(api.hasEnvironmentVariable("API_HOST"));
        api.setEnvironmentVariable("API_HOST", "localhost");
        assertTrue(api.hasEnvironmentVariable("API_HOST"));
        assertEquals("localhost", api.getEnvironmentVariable("API_HOST"));
    }

    @Test
    void diagnosticsShouldBeDispatched() {
        assertEquals("3.1.0", api.getBuildVersion());
        assertTrue(api.canLaunchSummary());
        api.setCommandLineVariable("dontLaunchSummary", "true");
        assertFalse(api.canLaunchSummary());
    }
}

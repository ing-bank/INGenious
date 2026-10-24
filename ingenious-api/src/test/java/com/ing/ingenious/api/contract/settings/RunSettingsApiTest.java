package com.ing.ingenious.api.contract.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Contract test for {@link RunSettingsApi}. Uses a minimal in-test
 * implementation backed by a map to verify the interface's accessors behave
 * as expected.
 */
class RunSettingsApiTest {

    private final Map<String, String> properties = new HashMap<>();

    private final RunSettingsApi settings = new RunSettingsApi() {
        @Override
        public boolean isVideoEnabled() {
            return Boolean.parseBoolean(properties.get("recordVideo"));
        }

        @Override
        public boolean isTracingEnabled() {
            return Boolean.parseBoolean(properties.get("enableTracing"));
        }

        @Override
        public boolean isHARrecordingEnabled() {
            return Boolean.parseBoolean(properties.get("enableHAR"));
        }

        @Override
        public boolean isPerformanceLogEnabled() {
            return Boolean.parseBoolean(properties.get("reportPerformanceLog"));
        }

        @Override
        public boolean isBddReportEnabled() {
            return Boolean.parseBoolean(properties.get("bddReport"));
        }

        @Override
        public boolean isModernReport() {
            return Boolean.parseBoolean(properties.getOrDefault("ModernReport", "true"));
        }

        @Override
        public boolean isGridExecution() {
            return "grid".equalsIgnoreCase(getExecutionMode());
        }

        @Override
        public String getRemoteGridURL() {
            return properties.get("RemoteGridURL");
        }

        @Override
        public boolean useExistingDriver() {
            return Boolean.parseBoolean(properties.get("ExistingDriver"));
        }

        @Override
        public boolean getTakeFullPageScreenShot() {
            return Boolean.parseBoolean(properties.getOrDefault("TakeFullPageScreenShot", "true"));
        }

        @Override
        public String getScreenShotFor() {
            return properties.getOrDefault("ScreenShotFor", "Both");
        }

        @Override
        public int getThreadCount() {
            return Integer.parseInt(properties.getOrDefault("ThreadCount", "1"));
        }

        @Override
        public int getExecutionTimeOut() {
            return Integer.parseInt(properties.getOrDefault("ExecutionTimeOut", "300"));
        }

        @Override
        public String getExecutionMode() {
            return properties.getOrDefault("ExecutionMode", "Local");
        }

        @Override
        public String getIterationMode() {
            return properties.getOrDefault("IterationMode", "ContinueOnError");
        }

        @Override
        public int getRerunTimes() {
            return Integer.parseInt(properties.getOrDefault("RerunTimes", "0"));
        }

        @Override
        public String getTestEnv() {
            return properties.getOrDefault("TestEnv", "Default");
        }

        @Override
        public String getProperty(String key) {
            return properties.get(key);
        }

        @Override
        public void put(String key, String value) {
            properties.put(key, value);
        }
    };

    @Test
    void shouldDefaultToTheUnderlyingFrameworkDefaults() {
        assertFalse(settings.isVideoEnabled());
        assertEquals("Local", settings.getExecutionMode());
        assertFalse(settings.isGridExecution());
        assertEquals("Default", settings.getTestEnv());
        assertEquals(1, settings.getThreadCount());
        assertTrue(settings.isModernReport());
        assertNull(settings.getProperty("Unset"));
    }

    @Test
    void putShouldBeVisibleThroughTheTypedAccessors() {
        settings.put("ExecutionMode", "Grid");
        settings.put("TestEnv", "QA");
        settings.put("recordVideo", "true");

        assertTrue(settings.isGridExecution());
        assertEquals("QA", settings.getTestEnv());
        assertTrue(settings.isVideoEnabled());
        assertEquals("Grid", settings.getProperty("ExecutionMode"));
    }
}

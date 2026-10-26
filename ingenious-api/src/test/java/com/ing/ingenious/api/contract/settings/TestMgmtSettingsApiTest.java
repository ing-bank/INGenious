package com.ing.ingenious.api.contract.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Contract test for {@link TestMgmtSettingsApi}. Uses a minimal in-test
 * implementation backed by a map to verify the interface's accessors behave
 * as expected.
 */
class TestMgmtSettingsApiTest {

    private final Map<String, String> properties = new HashMap<>();

    private final TestMgmtSettingsApi settings = new TestMgmtSettingsApi() {
        @Override
        public String getUpdateResultsToTM() {
            return properties.getOrDefault("UpdateResultsToTM", "None");
        }

        @Override
        public void setUpdateResultsToTM(String value) {
            properties.put("UpdateResultsToTM", value);
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
    void shouldDefaultToNoneAndUpdateBothWays() {
        assertEquals("None", settings.getUpdateResultsToTM());
        settings.setUpdateResultsToTM("Azure");
        assertEquals("Azure", settings.getUpdateResultsToTM());
        assertEquals("Azure", settings.getProperty("UpdateResultsToTM"));
        assertNull(settings.getProperty("Unset"));
    }
}

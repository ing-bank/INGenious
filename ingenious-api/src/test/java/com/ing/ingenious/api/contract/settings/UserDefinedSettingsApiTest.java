package com.ing.ingenious.api.contract.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Contract test for {@link UserDefinedSettingsApi}. Uses a minimal in-test
 * implementation backed by a map to verify the interface's accessors behave
 * as expected.
 */
class UserDefinedSettingsApiTest {

    private final Map<String, String> properties = new HashMap<>();

    private final UserDefinedSettingsApi settings = new UserDefinedSettingsApi() {
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
    void putShouldBeVisibleThroughGetProperty() {
        assertNull(settings.getProperty("MyGlobalVar"));
        settings.put("MyGlobalVar", "value1");
        assertEquals("value1", settings.getProperty("MyGlobalVar"));
    }
}

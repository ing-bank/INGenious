package com.ing.ingenious.api.contract.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Contract test for {@link DriverSettingsApi}. Uses a minimal in-test
 * implementation backed by a map to verify the interface's accessors behave
 * as expected.
 */
class DriverSettingsApiTest {

    private final Map<String, String> properties = new HashMap<>();

    private final DriverSettingsApi settings = new DriverSettingsApi() {
        @Override
        public boolean useProxy() {
            return Boolean.parseBoolean(properties.getOrDefault("useProxy", "false"));
        }

        @Override
        public boolean selfSigned() {
            return Boolean.parseBoolean(properties.getOrDefault("selfSigned", "false"));
        }

        @Override
        public void put(String key, String value) {
            properties.put(key, value);
        }
    };

    @Test
    void shouldDefaultToFalseAndUpdateThroughPut() {
        assertFalse(settings.useProxy());
        assertFalse(settings.selfSigned());

        settings.put("useProxy", "true");
        settings.put("selfSigned", "true");

        assertTrue(settings.useProxy());
        assertTrue(settings.selfSigned());
        assertEquals("true", properties.get("useProxy"));
    }
}

package com.ing.ingenious.api.contract.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;

/**
 * Contract test for {@link ProjectSettingsApi}. Verifies the interface
 * dispatches to the expected sub-accessor instances.
 */
class ProjectSettingsApiTest {

    private final ExecutionSettingsApi execSettings = proxy(ExecutionSettingsApi.class);
    private final DriverSettingsApi driverSettings = proxy(DriverSettingsApi.class);
    private final UserDefinedSettingsApi userDefinedSettings = proxy(UserDefinedSettingsApi.class);

    private final ProjectSettingsApi settings = new ProjectSettingsApi() {
        @Override
        public ExecutionSettingsApi getExecSettings() {
            return execSettings;
        }

        @Override
        public String getLocation() {
            return "/project/Settings";
        }

        @Override
        public DriverSettingsApi getDriverSettings() {
            return driverSettings;
        }

        @Override
        public UserDefinedSettingsApi getUserDefinedSettings() {
            return userDefinedSettings;
        }
    };

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type) {
        return (T) Proxy.newProxyInstance(
            type.getClassLoader(),
            new Class<?>[] { type },
            (proxyInstance, method, args) -> null
        );
    }

    @Test
    void shouldDispatchToTheExpectedSubAccessors() {
        assertEquals("/project/Settings", settings.getLocation());
        assertSame(execSettings, settings.getExecSettings());
        assertSame(driverSettings, settings.getDriverSettings());
        assertSame(userDefinedSettings, settings.getUserDefinedSettings());
    }
}

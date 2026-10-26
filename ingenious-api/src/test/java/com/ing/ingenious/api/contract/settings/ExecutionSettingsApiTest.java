package com.ing.ingenious.api.contract.settings;

import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;

/**
 * Contract test for {@link ExecutionSettingsApi}. Verifies the interface
 * dispatches to the expected sub-accessor instances.
 */
class ExecutionSettingsApiTest {

    private final RunSettingsApi runSettings = proxy(RunSettingsApi.class);
    private final TestMgmtSettingsApi testMgmgtSettings = proxy(TestMgmtSettingsApi.class);

    private final ExecutionSettingsApi settings = new ExecutionSettingsApi() {
        @Override
        public RunSettingsApi getRunSettings() {
            return runSettings;
        }

        @Override
        public TestMgmtSettingsApi getTestMgmgtSettings() {
            return testMgmgtSettings;
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
        assertSame(runSettings, settings.getRunSettings());
        assertSame(testMgmgtSettings, settings.getTestMgmgtSettings());
    }
}

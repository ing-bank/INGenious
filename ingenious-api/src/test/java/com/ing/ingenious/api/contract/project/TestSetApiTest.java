package com.ing.ingenious.api.contract.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.ing.ingenious.api.contract.settings.ExecutionSettingsApi;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Contract test for {@link TestSetApi}. Verifies the interface dispatches to
 * the expected sub-accessors.
 */
class TestSetApiTest {

    private final List<TestStepApi> steps = new ArrayList<>();
    private final ExecutionSettingsApi execSettings = proxy(ExecutionSettingsApi.class);

    private final TestSetApi testSet = new TestSetApi() {
        @Override
        public List<TestStepApi> getTestSteps() {
            return steps;
        }

        @Override
        public ExecutionSettingsApi getExecSettings() {
            return execSettings;
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
        TestStepApi step = proxy(TestStepApi.class);
        steps.add(step);

        assertEquals(List.of(step), testSet.getTestSteps());
        assertSame(execSettings, testSet.getExecSettings());
    }
}

package com.ing.ingenious.api.contract.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Contract test for {@link ProjectTestDataApi}. Verifies the default
 * environment and environment lookup accessors behave as expected.
 */
class ProjectTestDataApiTest {

    private final Map<String, TestDataSetApi> byEnvironment = new HashMap<>();

    private final ProjectTestDataApi testData = new ProjectTestDataApi() {
        @Override
        public String defEnv() {
            return "Default";
        }

        @Override
        public TestDataSetApi getTestDataFor(String environment) {
            return byEnvironment.get(environment);
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
    void shouldReturnTheDefaultEnvironmentName() {
        assertEquals("Default", testData.defEnv());
    }

    @Test
    void shouldReturnNullForAnUnknownEnvironment() {
        assertNull(testData.getTestDataFor("QA"));
    }

    @Test
    void shouldReturnTheRegisteredEnvironmentTestDataSet() {
        TestDataSetApi qaData = proxy(TestDataSetApi.class);
        byEnvironment.put("QA", qaData);
        assertSame(qaData, testData.getTestDataFor("QA"));
    }
}

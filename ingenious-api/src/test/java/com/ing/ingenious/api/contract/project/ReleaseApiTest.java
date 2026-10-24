package com.ing.ingenious.api.contract.project;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Contract test for {@link ReleaseApi}. Verifies the test-set lookup
 * accessor behaves as expected.
 */
class ReleaseApiTest {

    private final Map<String, TestSetApi> testSets = new HashMap<>();

    private final ReleaseApi release = testSets::get;

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type) {
        return (T) Proxy.newProxyInstance(
            type.getClassLoader(),
            new Class<?>[] { type },
            (proxyInstance, method, args) -> null
        );
    }

    @Test
    void shouldReturnNullForAnUnknownTestSet() {
        assertNull(release.getTestSetByName("Unknown"));
    }

    @Test
    void shouldReturnTheRegisteredTestSet() {
        TestSetApi testSet = proxy(TestSetApi.class);
        testSets.put("TestSet1", testSet);
        assertSame(testSet, release.getTestSetByName("TestSet1"));
    }
}

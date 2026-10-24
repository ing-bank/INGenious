package com.ing.ingenious.api.contract.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.ing.ingenious.api.contract.data.TestDataViewApi;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Contract test for {@link TestDataSetApi}. Verifies the sheet-name and
 * sheet-lookup accessors behave as expected.
 */
class TestDataSetApiTest {

    private final List<String> sheetNames = new ArrayList<>();
    private final Map<String, TestDataViewApi> sheets = new HashMap<>();

    private final TestDataSetApi testDataSet = new TestDataSetApi() {
        @Override
        public List<String> getTestDataNames() {
            return sheetNames;
        }

        @Override
        public TestDataViewApi getByName(String name) {
            return sheets.get(name);
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
    void shouldReflectRegisteredSheetNames() {
        sheetNames.add("Sheet1");
        sheetNames.add("GlobalData");
        assertEquals(List.of("Sheet1", "GlobalData"), testDataSet.getTestDataNames());
    }

    @Test
    void shouldReturnNullForAnUnknownSheet() {
        assertNull(testDataSet.getByName("Unknown"));
    }

    @Test
    void shouldReturnTheRegisteredSheetView() {
        TestDataViewApi view = proxy(TestDataViewApi.class);
        sheets.put("Sheet1", view);
        assertSame(view, testDataSet.getByName("Sheet1"));
    }
}

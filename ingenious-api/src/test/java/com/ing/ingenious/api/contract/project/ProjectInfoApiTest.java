package com.ing.ingenious.api.contract.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Contract test for {@link ProjectInfoApi}. Uses a minimal in-test
 * implementation to verify the data-item-names accessor behaves as expected.
 */
class ProjectInfoApiTest {

    private final List<String> dataItemNames = new ArrayList<>();

    private final ProjectInfoApi info = () -> dataItemNames;

    @Test
    void shouldStartEmpty() {
        assertTrue(info.getDataItemNames().isEmpty());
    }

    @Test
    void shouldReflectRegisteredDataItems() {
        dataItemNames.add("Item1");
        dataItemNames.add("Item2");
        assertEquals(List.of("Item1", "Item2"), info.getDataItemNames());
    }
}

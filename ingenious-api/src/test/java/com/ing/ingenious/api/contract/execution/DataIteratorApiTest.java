package com.ing.ingenious.api.contract.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Contract test for {@link DataIteratorApi}. Uses a minimal in-test
 * implementation to verify the interface's accessors behave as expected.
 */
class DataIteratorApiTest {

    private final Set<String> resolvedSheets = new HashSet<>();
    private int maxIter = 1;

    private final DataIteratorApi iterator = new DataIteratorApi() {
        @Override
        public boolean isResolved() {
            return !resolvedSheets.isEmpty();
        }

        @Override
        public int getMaxIter() {
            return maxIter;
        }

        @Override
        public boolean isIterResolved(String sheet) {
            return resolvedSheets.contains(sheet);
        }
    };

    @Test
    void shouldReportUnresolvedUntilASheetIsRegistered() {
        assertFalse(iterator.isResolved());
        assertFalse(iterator.isIterResolved("Sheet1"));

        resolvedSheets.add("Sheet1");
        maxIter = 3;

        assertTrue(iterator.isResolved());
        assertTrue(iterator.isIterResolved("Sheet1"));
        assertFalse(iterator.isIterResolved("Sheet2"));
        assertEquals(3, iterator.getMaxIter());
    }
}

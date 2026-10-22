package com.ing.ingenious.api.contract.execution;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Contract test for {@link SummaryReportApi}. Uses a minimal in-test
 * implementation to verify the interface can be implemented and dispatched.
 */
class SummaryReportApiTest {

    private boolean passed;

    private final SummaryReportApi report = () -> passed;

    @Test
    void shouldReflectThePassedState() {
        assertFalse(report.isPassed());
        passed = true;
        assertTrue(report.isPassed());
    }
}

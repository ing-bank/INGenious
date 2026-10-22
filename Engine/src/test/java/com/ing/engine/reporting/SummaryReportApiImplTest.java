package com.ing.engine.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.testng.annotations.Test;

/**
 * Tests for {@link SummaryReportApiImpl} — verifies the passed-state
 * accessor delegates to the wrapped {@link SummaryReport} instance.
 */
public class SummaryReportApiImplTest {

    @Test
    public void isPassedShouldDelegate() {
        SummaryReport summaryReport = mock(SummaryReport.class);
        when(summaryReport.isPassed()).thenReturn(true);

        assertThat(new SummaryReportApiImpl(summaryReport).isPassed()).isTrue();
    }

    @Test
    public void isPassedShouldReflectAFailedRun() {
        SummaryReport summaryReport = mock(SummaryReport.class);
        when(summaryReport.isPassed()).thenReturn(false);

        assertThat(new SummaryReportApiImpl(summaryReport).isPassed()).isFalse();
    }
}

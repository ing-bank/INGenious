package com.ing.engine.reporting;

import com.ing.ingenious.api.contract.execution.SummaryReportApi;

/**
 * Delegates the {@link SummaryReportApi} plugin contract to a specific
 * {@link SummaryReport} instance.
 */
public final class SummaryReportApiImpl implements SummaryReportApi {
    private final SummaryReport summaryReport;

    public SummaryReportApiImpl(SummaryReport summaryReport) {
        this.summaryReport = summaryReport;
    }

    @Override
    public boolean isPassed() {
        return summaryReport.isPassed();
    }
}

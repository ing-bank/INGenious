package com.ing.ingenious.api.contract.execution;

/**
 * Contract for the current run's summary report, backed by the framework's
 * {@code SummaryReport}.
 */
public interface SummaryReportApi {

    /** @return whether the run has passed so far */
    boolean isPassed();
}

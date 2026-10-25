package com.ing.ingenious.api.contract.settings;

/**
 * Contract for a run's execution-mode settings, backed by the framework's
 * {@code RunSettings}. All accessors are read-only except the generic
 * {@link #put(String, String)}, which mirrors the underlying property-file
 * nature of these settings.
 */
public interface RunSettingsApi {

    /** @return whether video recording is enabled */
    boolean isVideoEnabled();

    /** @return whether Playwright tracing is enabled */
    boolean isTracingEnabled();

    /** @return whether HAR recording is enabled */
    boolean isHARrecordingEnabled();

    /** @return whether performance logging is enabled */
    boolean isPerformanceLogEnabled();

    /** @return whether BDD reporting is enabled */
    boolean isBddReportEnabled();

    /** @return whether the modern (v2) HTML report style is enabled */
    boolean isModernReport();

    /** @return whether this run executes on a remote grid */
    boolean isGridExecution();

    /** @return the configured remote grid/Appium endpoint URL */
    String getRemoteGridURL();

    /** @return whether this run reuses an already-open driver */
    boolean useExistingDriver();

    /** @return whether full-page screenshots are taken */
    boolean getTakeFullPageScreenShot();

    /** @return which outcomes screenshots are taken for (e.g. "Both", "Fail") */
    String getScreenShotFor();

    /** @return the configured thread count for parallel execution */
    int getThreadCount();

    /** @return the configured execution timeout, in seconds */
    int getExecutionTimeOut();

    /** @return the configured execution mode (e.g. "Local", "Grid") */
    String getExecutionMode();

    /** @return the configured iteration mode (e.g. "ContinueOnError") */
    String getIterationMode();

    /** @return the configured number of times a failed test set is rerun */
    int getRerunTimes();

    /**
     * Gets the configured test environment name (e.g. {@code "QA"},
     * {@code "Prod"}) used to resolve project test data.
     * <p>
     * Not to be confused with
     * {@link com.ing.ingenious.api.contract.paths.ProjectPathsApi#getTestEnv()},
     * which returns a filesystem <em>path</em>, not an environment name.
     * </p>
     * @return the configured test environment name
     */
    String getTestEnv();

    /**
     * @param key the property key
     * @return the raw property value, or {@code null} if not set
     */
    String getProperty(String key);

    /**
     * Sets a raw property value.
     * @param key the property key
     * @param value the property value
     */
    void put(String key, String value);
}

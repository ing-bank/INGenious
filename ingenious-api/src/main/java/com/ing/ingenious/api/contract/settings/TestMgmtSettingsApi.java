package com.ing.ingenious.api.contract.settings;

/**
 * Contract for a run's test-management integration settings, backed by the
 * framework's {@code TestMgmtSettings}.
 */
public interface TestMgmtSettingsApi {

    /** @return the configured test management system results are pushed to (e.g. "None", "Azure") */
    String getUpdateResultsToTM();

    /** @param value the test management system to push results to */
    void setUpdateResultsToTM(String value);

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

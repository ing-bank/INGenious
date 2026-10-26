package com.ing.ingenious.api.contract.settings;

/**
 * Contract for a project's user-defined (global variable) settings, backed
 * by the framework's {@code UserDefinedSettings}.
 */
public interface UserDefinedSettingsApi {

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

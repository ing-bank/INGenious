package com.ing.ingenious.api.contract.settings;

/**
 * Contract for a project's driver/proxy configuration, backed by the
 * framework's {@code DriverProperties}. The generic {@link #put(String, String)}
 * is the only write path exposed, since the underlying class's named setters
 * are currently dead code in Engine.
 */
public interface DriverSettingsApi {

    /** @return whether a proxy is configured for use */
    boolean useProxy();

    /** @return whether self-signed certificates are accepted */
    boolean selfSigned();

    /**
     * Sets a raw property value.
     * @param key the property key
     * @param value the property value
     */
    void put(String key, String value);
}

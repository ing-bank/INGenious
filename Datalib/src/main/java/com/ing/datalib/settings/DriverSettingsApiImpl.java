package com.ing.datalib.settings;

import com.ing.ingenious.api.contract.settings.DriverSettingsApi;

/**
 * Delegates the {@link DriverSettingsApi} plugin contract to a specific
 * {@link DriverProperties} instance.
 */
public final class DriverSettingsApiImpl implements DriverSettingsApi {
    private final DriverProperties driverProperties;

    public DriverSettingsApiImpl(DriverProperties driverProperties) {
        this.driverProperties = driverProperties;
    }

    @Override
    public boolean useProxy() {
        return driverProperties.useProxy();
    }

    @Override
    public boolean selfSigned() {
        return driverProperties.selfSigned();
    }

    @Override
    public void put(String key, String value) {
        driverProperties.setProperty(key, value);
    }
}

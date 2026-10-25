package com.ing.datalib.settings;

import com.ing.ingenious.api.contract.settings.UserDefinedSettingsApi;

/**
 * Delegates the {@link UserDefinedSettingsApi} plugin contract to a specific
 * {@link UserDefinedSettings} instance.
 */
public final class UserDefinedSettingsApiImpl implements UserDefinedSettingsApi {
    private final UserDefinedSettings userDefinedSettings;

    public UserDefinedSettingsApiImpl(UserDefinedSettings userDefinedSettings) {
        this.userDefinedSettings = userDefinedSettings;
    }

    @Override
    public String getProperty(String key) {
        return userDefinedSettings.getProperty(key);
    }

    @Override
    public void put(String key, String value) {
        userDefinedSettings.setProperty(key, value);
    }
}

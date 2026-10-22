package com.ing.datalib.settings;

import com.ing.ingenious.api.contract.settings.TestMgmtSettingsApi;

/**
 * Delegates the {@link TestMgmtSettingsApi} plugin contract to a specific
 * {@link TestMgmtSettings} instance.
 */
public final class TestMgmtSettingsApiImpl implements TestMgmtSettingsApi {
    private final TestMgmtSettings testMgmtSettings;

    public TestMgmtSettingsApiImpl(TestMgmtSettings testMgmtSettings) {
        this.testMgmtSettings = testMgmtSettings;
    }

    @Override
    public String getUpdateResultsToTM() {
        return testMgmtSettings.getUpdateResultsToTM();
    }

    @Override
    public void setUpdateResultsToTM(String value) {
        testMgmtSettings.setUpdateResultsToTM(value);
    }

    @Override
    public String getProperty(String key) {
        return testMgmtSettings.getProperty(key);
    }

    @Override
    public void put(String key, String value) {
        testMgmtSettings.setProperty(key, value);
    }
}

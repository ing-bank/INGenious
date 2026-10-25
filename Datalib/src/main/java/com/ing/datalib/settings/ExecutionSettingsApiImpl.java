package com.ing.datalib.settings;

import com.ing.ingenious.api.contract.settings.ExecutionSettingsApi;
import com.ing.ingenious.api.contract.settings.RunSettingsApi;
import com.ing.ingenious.api.contract.settings.TestMgmtSettingsApi;

/**
 * Delegates the {@link ExecutionSettingsApi} plugin contract to a specific
 * {@link ExecutionSettings} instance.
 */
public final class ExecutionSettingsApiImpl implements ExecutionSettingsApi {
    private final ExecutionSettings executionSettings;

    public ExecutionSettingsApiImpl(ExecutionSettings executionSettings) {
        this.executionSettings = executionSettings;
    }

    @Override
    public RunSettingsApi getRunSettings() {
        return new RunSettingsApiImpl(executionSettings.getRunSettings());
    }

    @Override
    public TestMgmtSettingsApi getTestMgmgtSettings() {
        return new TestMgmtSettingsApiImpl(executionSettings.getTestMgmgtSettings());
    }
}

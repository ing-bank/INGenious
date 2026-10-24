package com.ing.datalib.settings;

import com.ing.ingenious.api.contract.settings.DriverSettingsApi;
import com.ing.ingenious.api.contract.settings.ExecutionSettingsApi;
import com.ing.ingenious.api.contract.settings.ProjectSettingsApi;
import com.ing.ingenious.api.contract.settings.UserDefinedSettingsApi;

/**
 * Delegates the {@link ProjectSettingsApi} plugin contract to a specific
 * {@link ProjectSettings} instance.
 */
public final class ProjectSettingsApiImpl implements ProjectSettingsApi {
    private final ProjectSettings projectSettings;

    public ProjectSettingsApiImpl(ProjectSettings projectSettings) {
        this.projectSettings = projectSettings;
    }

    @Override
    public ExecutionSettingsApi getExecSettings() {
        return new ExecutionSettingsApiImpl(projectSettings.getExecSettings());
    }

    @Override
    public String getLocation() {
        return projectSettings.getLocation();
    }

    @Override
    public DriverSettingsApi getDriverSettings() {
        return new DriverSettingsApiImpl(projectSettings.getDriverSettings());
    }

    @Override
    public UserDefinedSettingsApi getUserDefinedSettings() {
        return new UserDefinedSettingsApiImpl(projectSettings.getUserDefinedSettings());
    }
}

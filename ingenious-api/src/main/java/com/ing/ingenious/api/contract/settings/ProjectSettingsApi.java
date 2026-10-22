package com.ing.ingenious.api.contract.settings;

/**
 * Contract for a project's settings tree, backed by the framework's
 * {@code ProjectSettings}.
 */
public interface ProjectSettingsApi {

    /** @return the project's default execution settings */
    ExecutionSettingsApi getExecSettings();

    /** @return the filesystem location of the project's Settings folder */
    String getLocation();

    /** @return the project's driver/proxy configuration */
    DriverSettingsApi getDriverSettings();

    /** @return the project's user-defined (global variable) settings */
    UserDefinedSettingsApi getUserDefinedSettings();
}

package com.ing.ingenious.api.contract.settings;

/**
 * Contract for a scope's combined execution settings (run settings and test
 * management settings), backed by the framework's {@code ExecutionSettings}.
 * <p>
 * This scope varies by caller: it may be project-wide or test-set-specific —
 * see {@link com.ing.ingenious.api.contract.execution.TestRunnerApi#getExecSettings()},
 * {@link com.ing.ingenious.api.contract.settings.ProjectSettingsApi#getExecSettings()},
 * and {@link com.ing.ingenious.api.contract.project.TestSetApi#getExecSettings()}.
 * </p>
 */
public interface ExecutionSettingsApi {

    /** @return the run's execution-mode settings */
    RunSettingsApi getRunSettings();

    /** @return the run's test-management integration settings */
    TestMgmtSettingsApi getTestMgmgtSettings();
}

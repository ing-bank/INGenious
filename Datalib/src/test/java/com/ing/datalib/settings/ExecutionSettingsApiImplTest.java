package com.ing.datalib.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.testng.annotations.Test;

/**
 * Tests for {@link ExecutionSettingsApiImpl} — verifies each accessor wraps
 * the wrapped {@link ExecutionSettings} instance's sub-settings.
 */
public class ExecutionSettingsApiImplTest {

    @Test
    public void getRunSettingsShouldWrapTheUnderlyingRunSettings() {
        ExecutionSettings execSettings = mock(ExecutionSettings.class);
        RunSettings runSettings = mock(RunSettings.class);
        when(execSettings.getRunSettings()).thenReturn(runSettings);

        assertThat(new ExecutionSettingsApiImpl(execSettings).getRunSettings()).isNotNull();
    }

    @Test
    public void getTestMgmgtSettingsShouldWrapTheUnderlyingSettings() {
        ExecutionSettings execSettings = mock(ExecutionSettings.class);
        TestMgmtSettings testMgmtSettings = mock(TestMgmtSettings.class);
        when(execSettings.getTestMgmgtSettings()).thenReturn(testMgmtSettings);

        assertThat(new ExecutionSettingsApiImpl(execSettings).getTestMgmgtSettings()).isNotNull();
    }
}

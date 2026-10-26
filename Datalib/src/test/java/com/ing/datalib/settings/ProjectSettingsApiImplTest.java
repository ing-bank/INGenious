package com.ing.datalib.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.testng.annotations.Test;

/**
 * Tests for {@link ProjectSettingsApiImpl} — verifies each accessor
 * delegates to the wrapped {@link ProjectSettings} instance.
 */
public class ProjectSettingsApiImplTest {

    @Test
    public void getLocationShouldDelegate() {
        ProjectSettings settings = mock(ProjectSettings.class);
        when(settings.getLocation()).thenReturn("/projects/MyProject/Settings");

        assertThat(new ProjectSettingsApiImpl(settings).getLocation())
            .isEqualTo("/projects/MyProject/Settings");
    }

    @Test
    public void getExecSettingsShouldWrapTheUnderlyingSettings() {
        ProjectSettings settings = mock(ProjectSettings.class);
        ExecutionSettings execSettings = mock(ExecutionSettings.class);
        when(settings.getExecSettings()).thenReturn(execSettings);

        assertThat(new ProjectSettingsApiImpl(settings).getExecSettings()).isNotNull();
    }

    @Test
    public void getDriverSettingsShouldWrapTheUnderlyingSettings() {
        ProjectSettings settings = mock(ProjectSettings.class);
        DriverProperties driverProperties = mock(DriverProperties.class);
        when(settings.getDriverSettings()).thenReturn(driverProperties);

        assertThat(new ProjectSettingsApiImpl(settings).getDriverSettings()).isNotNull();
    }

    @Test
    public void getUserDefinedSettingsShouldWrapTheUnderlyingSettings() {
        ProjectSettings settings = mock(ProjectSettings.class);
        UserDefinedSettings userDefinedSettings = mock(UserDefinedSettings.class);
        when(settings.getUserDefinedSettings()).thenReturn(userDefinedSettings);

        assertThat(new ProjectSettingsApiImpl(settings).getUserDefinedSettings()).isNotNull();
    }
}

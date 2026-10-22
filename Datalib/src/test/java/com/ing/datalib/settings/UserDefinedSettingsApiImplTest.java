package com.ing.datalib.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.testng.annotations.Test;

/**
 * Tests for {@link UserDefinedSettingsApiImpl} — verifies the generic
 * get/put property path delegates to the wrapped {@link UserDefinedSettings}
 * instance.
 */
public class UserDefinedSettingsApiImplTest {

    @Test
    public void getPropertyShouldDelegate() {
        UserDefinedSettings settings = mock(UserDefinedSettings.class);
        when(settings.getProperty("MyGlobalVar")).thenReturn("value1");

        assertThat(new UserDefinedSettingsApiImpl(settings).getProperty("MyGlobalVar"))
            .isEqualTo("value1");
    }

    @Test
    public void putShouldDelegateToSetProperty() {
        UserDefinedSettings settings = mock(UserDefinedSettings.class);

        new UserDefinedSettingsApiImpl(settings).put("MyGlobalVar", "value1");

        verify(settings).setProperty("MyGlobalVar", "value1");
    }
}

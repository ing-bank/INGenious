package com.ing.datalib.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.testng.annotations.Test;

/**
 * Tests for {@link DriverSettingsApiImpl} — verifies each accessor delegates
 * to the wrapped {@link DriverProperties} instance, and {@code put}
 * delegates to the generic {@code setProperty} write path (the only one
 * still wired up on {@link DriverProperties}).
 */
public class DriverSettingsApiImplTest {

    @Test
    public void useProxyAndSelfSignedShouldDelegate() {
        DriverProperties driverProperties = mock(DriverProperties.class);
        when(driverProperties.useProxy()).thenReturn(true);
        when(driverProperties.selfSigned()).thenReturn(false);

        DriverSettingsApiImpl api = new DriverSettingsApiImpl(driverProperties);

        assertThat(api.useProxy()).isTrue();
        assertThat(api.selfSigned()).isFalse();
    }

    @Test
    public void putShouldDelegateToSetProperty() {
        DriverProperties driverProperties = mock(DriverProperties.class);

        new DriverSettingsApiImpl(driverProperties).put("useProxy", "true");

        verify(driverProperties).setProperty("useProxy", "true");
    }
}

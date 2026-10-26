package com.ing.datalib.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.testng.annotations.Test;

/**
 * Tests for {@link TestMgmtSettingsApiImpl} — verifies each accessor
 * delegates to the wrapped {@link TestMgmtSettings} instance.
 */
public class TestMgmtSettingsApiImplTest {

    @Test
    public void getUpdateResultsToTMShouldDelegate() {
        TestMgmtSettings settings = mock(TestMgmtSettings.class);
        when(settings.getUpdateResultsToTM()).thenReturn("Azure");

        assertThat(new TestMgmtSettingsApiImpl(settings).getUpdateResultsToTM()).isEqualTo("Azure");
    }

    @Test
    public void setUpdateResultsToTMShouldDelegate() {
        TestMgmtSettings settings = mock(TestMgmtSettings.class);

        new TestMgmtSettingsApiImpl(settings).setUpdateResultsToTM("Azure");

        verify(settings).setUpdateResultsToTM("Azure");
    }

    @Test
    public void getPropertyShouldDelegate() {
        TestMgmtSettings settings = mock(TestMgmtSettings.class);
        when(settings.getProperty("CustomKey")).thenReturn("CustomValue");

        assertThat(new TestMgmtSettingsApiImpl(settings).getProperty("CustomKey"))
            .isEqualTo("CustomValue");
    }

    @Test
    public void putShouldDelegateToSetProperty() {
        TestMgmtSettings settings = mock(TestMgmtSettings.class);

        new TestMgmtSettingsApiImpl(settings).put("CustomKey", "CustomValue");

        verify(settings).setProperty("CustomKey", "CustomValue");
    }
}

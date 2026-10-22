package com.ing.datalib.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.testng.annotations.Test;

/**
 * Tests for {@link ProjectTestDataApiImpl} — verifies each accessor delegates
 * to the wrapped {@link EnvTestData} instance.
 */
public class ProjectTestDataApiImplTest {

    @Test
    public void defEnvShouldDelegate() {
        EnvTestData testData = mock(EnvTestData.class);
        when(testData.defEnv()).thenReturn("Default");

        assertThat(new ProjectTestDataApiImpl(testData).defEnv()).isEqualTo("Default");
    }

    @Test
    public void getTestDataForShouldWrapTheUnderlyingTestData() {
        EnvTestData envTestData = mock(EnvTestData.class);
        TestData testData = mock(TestData.class);
        when(envTestData.getTestDataFor("QA")).thenReturn(testData);

        assertThat(new ProjectTestDataApiImpl(envTestData).getTestDataFor("QA")).isNotNull();
    }

    @Test
    public void getTestDataForShouldReturnNullWhenNotFound() {
        EnvTestData envTestData = mock(EnvTestData.class);
        when(envTestData.getTestDataFor("Unknown")).thenReturn(null);

        assertThat(new ProjectTestDataApiImpl(envTestData).getTestDataFor("Unknown")).isNull();
    }
}

package com.ing.datalib.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.testng.annotations.Test;

/**
 * Tests for {@link TestStepApiImpl} — verifies each accessor delegates to
 * the wrapped {@link ExecutionStep} instance.
 */
public class TestStepApiImplTest {

    @Test
    public void eachAccessorShouldDelegateToTheUnderlyingStep() {
        ExecutionStep step = mock(ExecutionStep.class);
        when(step.getTestScenarioName()).thenReturn("Scenario1");
        when(step.getTestCaseName()).thenReturn("TestCase1");
        when(step.getIteration()).thenReturn("1");
        when(step.getStatus()).thenReturn("Pass");
        when(step.getExecute()).thenReturn("true");
        when(step.getPlatform()).thenReturn("Windows");
        when(step.getBrowser()).thenReturn("Chrome");

        TestStepApiImpl api = new TestStepApiImpl(step);

        assertThat(api.getTestScenarioName()).isEqualTo("Scenario1");
        assertThat(api.getTestCaseName()).isEqualTo("TestCase1");
        assertThat(api.getIteration()).isEqualTo("1");
        assertThat(api.getStatus()).isEqualTo("Pass");
        assertThat(api.getExecute()).isEqualTo("true");
        assertThat(api.getPlatform()).isEqualTo("Windows");
        assertThat(api.getBrowser()).isEqualTo("Chrome");
    }
}

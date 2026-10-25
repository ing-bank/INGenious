package com.ing.datalib.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ing.datalib.settings.ExecutionSettings;
import com.ing.ingenious.api.contract.project.TestStepApi;
import java.util.List;
import org.testng.annotations.Test;

/**
 * Tests for {@link TestSetApiImpl} — verifies the test-steps and
 * execution-settings accessors delegate to the wrapped {@link TestSet}
 * instance.
 */
public class TestSetApiImplTest {

    @Test
    public void getTestStepsShouldWrapEachUnderlyingStep() {
        TestSet testSet = mock(TestSet.class);
        ExecutionStep step1 = mock(ExecutionStep.class);
        ExecutionStep step2 = mock(ExecutionStep.class);
        when(testSet.getTestSteps()).thenReturn(List.of(step1, step2));

        List<TestStepApi> steps = new TestSetApiImpl(testSet).getTestSteps();

        assertThat(steps).hasSize(2);
    }

    @Test
    public void getExecSettingsShouldWrapTheUnderlyingSettings() {
        TestSet testSet = mock(TestSet.class);
        ExecutionSettings execSettings = mock(ExecutionSettings.class);
        when(testSet.getExecSettings()).thenReturn(execSettings);

        assertThat(new TestSetApiImpl(testSet).getExecSettings()).isNotNull();
    }
}

package com.ing.datalib.component;

import static org.assertj.core.api.Assertions.assertThat;
import org.testng.annotations.Test;

public class ExecutionStepTest {

    // ---- HEADERS enum tests ----

    @Test
    public void testHeadersEnumValues() {
        ExecutionStep.HEADERS[] headers = ExecutionStep.HEADERS.values();
        assertThat(headers).hasSize(8);
    }

    @Test
    public void testHeadersGetValues() {
        assertThat(ExecutionStep.HEADERS.getValues())
                .containsExactly("Execute", "TestScenario", "TestCase", "Iteration",
                        "Status", "Browser", "BrowserVersion", "Platform");
    }

    @Test
    public void testHeadersSize() {
        assertThat(ExecutionStep.HEADERS.size()).isEqualTo(8);
    }

    @Test
    public void testHeadersIndex() {
        assertThat(ExecutionStep.HEADERS.Execute.getIndex()).isEqualTo(0);
        assertThat(ExecutionStep.HEADERS.TestScenario.getIndex()).isEqualTo(1);
        assertThat(ExecutionStep.HEADERS.Platform.getIndex()).isEqualTo(7);
    }

    // ---- ExecutionStep tests using empty step constructor ----

    private ExecutionStep createEmptyStep() {
        // TestSet is needed by constructor, use mock-free approach:
        // TestSet constructor needs Project, but we only use empty step defaults
        // We create it via reflection-free approach - direct usage
        return new ExecutionStep(null);
    }

    @Test
    public void testEmptyStepDefaults() {
        // The constructor with null TestSet will work for loadEmptyStep
        ExecutionStep step = createEmptyStep();
        assertThat(step.getExecute()).isEqualTo("true");
        assertThat(step.getBrowserVersion()).isEqualTo("Default");
        assertThat(step.getPlatform()).isEqualTo("Any");
        assertThat(step.getStatus()).isEqualTo("NoRun");
        assertThat(step.getIteration()).isEqualTo("Single");
    }

    @Test
    public void testFluentSetters() {
        ExecutionStep step = createEmptyStep();
        ExecutionStep result = step.setTestScenario("Login")
                .setTestCase("TC01")
                .setBrowser("Chromium")
                .setBrowserVersion("120")
                .setPlatform("Windows")
                .setIteration("All")
                .setStatus("Pass");
        assertThat(result).isSameAs(step);
        assertThat(step.getTestScenarioName()).isEqualTo("Login");
        assertThat(step.getTestCaseName()).isEqualTo("TC01");
        assertThat(step.getBrowser()).isEqualTo("Chromium");
        assertThat(step.getBrowserVersion()).isEqualTo("120");
        assertThat(step.getPlatform()).isEqualTo("Windows");
        assertThat(step.getIteration()).isEqualTo("All");
        assertThat(step.getStatus()).isEqualTo("Pass");
    }

    @Test
    public void testSize() {
        ExecutionStep step = createEmptyStep();
        assertThat(step.size()).isEqualTo(ExecutionStep.HEADERS.size());
    }

    @Test
    public void testGetValueAt() {
        ExecutionStep step = createEmptyStep();
        step.setTestScenario("Login");
        assertThat(step.getValueAt(1)).isEqualTo("Login");
    }

    @Test
    public void testGetValueBy() {
        ExecutionStep step = createEmptyStep();
        step.setBrowser("Firefox");
        assertThat(step.getValueBy("Browser")).isEqualTo("Firefox");
    }

    @Test
    public void testPutValueAt() {
        ExecutionStep step = createEmptyStep();
        step.putValueAt(0, "false");
        assertThat(step.getExecute()).isEqualTo("false");
    }

    @Test
    public void testClearValues() {
        ExecutionStep step = createEmptyStep();
        step.clearValues();
        for (int i = 0; i < step.size(); i++) {
            assertThat(step.getValueAt(i)).isEmpty();
        }
    }

    @Test
    public void testIsEmptyAfterClear() {
        ExecutionStep step = createEmptyStep();
        step.clearValues();
        assertThat(step.isEmpty()).isTrue();
    }

    @Test
    public void testIsEmptyWithDefaultsFalse() {
        ExecutionStep step = createEmptyStep();
        assertThat(step.isEmpty()).isFalse();
    }

    @Test
    public void testCopyValuesTo() {
        ExecutionStep source = createEmptyStep();
        source.setTestScenario("Login").setBrowser("Firefox");

        ExecutionStep target = createEmptyStep();
        source.copyValuesTo(target);
        assertThat(target.getTestScenarioName()).isEqualTo("Login");
        assertThat(target.getBrowser()).isEqualTo("Firefox");
    }

    @Test
    public void testIsDuplicateTrue() {
        ExecutionStep a = createEmptyStep();
        ExecutionStep b = createEmptyStep();
        assertThat(a.isDuplicate(b)).isTrue();
    }

    @Test
    public void testIsDuplicateFalse() {
        ExecutionStep a = createEmptyStep();
        ExecutionStep b = createEmptyStep();
        b.setBrowser("Firefox");
        assertThat(a.isDuplicate(b)).isFalse();
    }

    @Test
    public void testToString() {
        ExecutionStep step = createEmptyStep();
        step.setTestScenario("Login").setTestCase("TC01").setBrowser("Chromium");
        String str = step.toString();
        assertThat(str).contains("Login");
        assertThat(str).contains("TC01");
        assertThat(str).contains("Chromium");
    }
}

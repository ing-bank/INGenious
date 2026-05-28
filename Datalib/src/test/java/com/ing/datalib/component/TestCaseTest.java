package com.ing.datalib.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.io.File;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests for TestCase — constructor name stripping, step management,
 * move/toggle operations, getKey, isReusable, copyValuesTo, save state.
 */
public class TestCaseTest {

    private Scenario scenario;
    private Project project;
    private TestCase testCase;

    @BeforeMethod
    public void setUp() {
        project = mock(Project.class);
        when(project.getLocation()).thenReturn("/tmp/test-project");
        scenario = mock(Scenario.class);
        when(scenario.getProject()).thenReturn(project);
        when(scenario.getName()).thenReturn("LoginScenario");
        when(scenario.getLocation()).thenReturn("/tmp/test-project/TestPlan/LoginScenario");

        testCase = new TestCase(scenario, "TC_Login.csv");
    }

    // ---- Constructor name stripping ----

    @Test
    public void testConstructorStripsCsvExtension() {
        assertThat(testCase.getName()).isEqualTo("TC_Login");
    }

    @Test
    public void testConstructorWithoutExtension() {
        TestCase tc = new TestCase(scenario, "TC_NoExt");
        assertThat(tc.getName()).isEqualTo("TC_NoExt");
    }

    // ---- getKey ----

    @Test
    public void testGetKey() {
        assertThat(testCase.getKey()).isEqualTo("LoginScenario#TC_Login");
    }

    // ---- getLocation ----

    @Test
    public void testGetLocation() {
        assertThat(testCase.getLocation()).isEqualTo(
                "/tmp/test-project/TestPlan/LoginScenario" + File.separator + "TC_Login.csv");
    }

    // ---- toString ----

    @Test
    public void testToString() {
        assertThat(testCase.toString()).isEqualTo("TC_Login");
    }

    // ---- isReusable / toggleAsReusable ----

    @Test
    public void testIsReusableDefault() {
        assertThat(testCase.isReusable()).isFalse();
    }

    @Test
    public void testToggleAsReusable() {
        testCase.toggleAsReusable();
        assertThat(testCase.isReusable()).isTrue();
        testCase.toggleAsReusable();
        assertThat(testCase.isReusable()).isFalse();
    }

    @Test
    public void testSetReusable() {
        Reusable r = new Reusable();
        testCase.setReusable(r);
        assertThat(testCase.isReusable()).isTrue();
        assertThat(testCase.getReusable()).isSameAs(r);
    }

    // ---- isSaved / setSaved ----

    @Test
    public void testInitiallySaved() {
        assertThat(testCase.isSaved()).isTrue();
    }

    @Test
    public void testSetSaved() {
        testCase.setSaved(false);
        assertThat(testCase.isSaved()).isFalse();
        testCase.setSaved(true);
        assertThat(testCase.isSaved()).isTrue();
    }

    // ---- addNewStep ----

    @Test
    public void testAddNewStep() {
        TestStep step = testCase.addNewStep();
        assertThat(step).isNotNull();
        assertThat(testCase.getTestSteps()).hasSize(1);
    }

    @Test
    public void testAddMultipleSteps() {
        testCase.addNewStep();
        testCase.addNewStep();
        testCase.addNewStep();
        assertThat(testCase.getTestSteps()).hasSize(3);
    }

    // ---- addNewStepAt ----

    @Test
    public void testAddNewStepAt() {
        testCase.addNewStep(); // index 0
        testCase.addNewStep(); // index 1
        TestStep inserted = testCase.addNewStepAt(1);
        assertThat(testCase.getTestSteps()).hasSize(3);
        assertThat(testCase.getTestSteps().get(1)).isSameAs(inserted);
    }

    // ---- moveRowsUp / moveRowsDown ----

    @Test
    public void testMoveRowsUp_atTop() {
        testCase.addNewStep();
        testCase.addNewStep();
        Boolean result = testCase.moveRowsUp(0, 0);
        assertThat(result).isFalse();
    }

    @Test
    public void testMoveRowsUp_success() {
        TestStep s1 = testCase.addNewStep();
        TestStep s2 = testCase.addNewStep();
        Boolean result = testCase.moveRowsUp(1, 1);
        assertThat(result).isTrue();
        assertThat(testCase.getTestSteps().get(0)).isSameAs(s2);
    }

    @Test
    public void testMoveRowsDown_atBottom() {
        testCase.addNewStep();
        testCase.addNewStep();
        Boolean result = testCase.moveRowsDown(1, 1);
        assertThat(result).isFalse();
    }

    @Test
    public void testMoveRowsDown_success() {
        TestStep s1 = testCase.addNewStep();
        TestStep s2 = testCase.addNewStep();
        Boolean result = testCase.moveRowsDown(0, 0);
        assertThat(result).isTrue();
        assertThat(testCase.getTestSteps().get(1)).isSameAs(s1);
    }

    // ---- clearSteps ----

    @Test
    public void testClearSteps() {
        testCase.addNewStep();
        testCase.addNewStep();
        testCase.clearSteps();
        assertThat(testCase.getTestSteps()).isEmpty();
    }

    // ---- copyValuesTo ----

    @Test
    public void testCopyValuesTo() {
        TestStep step = testCase.addNewStep();
        step.setObject("Button1");
        step.setAction("Click");

        TestCase target = new TestCase(scenario, "TC_Target");
        testCase.copyValuesTo(target);

        assertThat(target.getTestSteps()).hasSize(1);
        assertThat(target.getTestSteps().get(0).getObject()).isEqualTo("Button1");
        assertThat(target.getTestSteps().get(0).getAction()).isEqualTo("Click");
    }

    // ---- getScenario / getProject ----

    @Test
    public void testGetScenario() {
        assertThat(testCase.getScenario()).isSameAs(scenario);
    }

    @Test
    public void testGetProject() {
        assertThat(testCase.getProject()).isSameAs(project);
    }

    @Test
    public void testSetScenario() {
        Scenario newScenario = mock(Scenario.class);
        testCase.setScenario(newScenario);
        assertThat(testCase.getScenario()).isSameAs(newScenario);
    }

    // ---- TableModel methods ----

    @Test
    public void testGetRowCount() {
        assertThat(testCase.getRowCount()).isEqualTo(0);
        testCase.addNewStep();
        assertThat(testCase.getRowCount()).isEqualTo(1);
    }

    @Test
    public void testGetColumnCount() {
        assertThat(testCase.getColumnCount()).isEqualTo(TestStep.HEADERS.size());
    }

    @Test
    public void testGetColumnName() {
        assertThat(testCase.getColumnName(0)).isEqualTo("Step");
        assertThat(testCase.getColumnName(1)).isEqualTo("ObjectName");
    }

    @Test
    public void testIsCellEditable() {
        assertThat(testCase.isCellEditable(0, 0)).isFalse();
        assertThat(testCase.isCellEditable(0, 1)).isTrue();
    }

    // ---- printString ----

    @Test
    public void testPrintString() {
        testCase.addNewStep();
        String str = testCase.printString();
        assertThat(str).contains("TestCase - TC_Login");
        assertThat(str).contains("TestSteps - 1");
    }

    // ---- addReusableStep ----

    @Test
    public void testAddReusableStep() {
        testCase.addReusableStep("LoginScenario:LoginReusable");
        assertThat(testCase.getTestSteps()).hasSize(1);
        assertThat(testCase.getTestSteps().get(0).getObject()).isEqualTo("Execute");
        assertThat(testCase.getTestSteps().get(0).getAction()).isEqualTo("LoginScenario:LoginReusable");
    }
}

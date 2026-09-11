package com.ing.datalib.sap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.component.TestStep;
import java.io.File;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Tests for the shared SAP legacy-project rewrite helpers (import/migration IO). */
public class SapProjectRewriteHelperTest {
    private Scenario scenario;
    private TestCase testCase;

    @BeforeMethod
    public void setUp() {
        File tempProjectDir = new File(
            System.getProperty("java.io.tmpdir"),
            "SapRewriteHelperTest_" + System.nanoTime()
        );
        File testPlanDir = new File(tempProjectDir, "TestPlan");
        File scenarioDir = new File(testPlanDir, "SapScenario");

        Project project = mock(Project.class);
        when(project.getLocation()).thenReturn(tempProjectDir.getAbsolutePath());
        scenario = mock(Scenario.class);
        when(scenario.getProject()).thenReturn(project);
        when(scenario.getName()).thenReturn("SapScenario");
        when(scenario.getLocation()).thenReturn(scenarioDir.getAbsolutePath());

        testCase = new TestCase(scenario, "TC_Sap.csv");
    }

    @Test
    public void rewriteBrowserAssignment_sapBecomesNoBrowser() {
        assertThat(SapProjectRewriteHelper.rewriteBrowserAssignment("SAP")).isEqualTo("No Browser");
        assertThat(SapProjectRewriteHelper.rewriteBrowserAssignment("sap")).isEqualTo("No Browser");
        assertThat(SapProjectRewriteHelper.rewriteBrowserAssignment(" SAP "))
            .isEqualTo("No Browser");
    }

    @Test
    public void rewriteBrowserAssignment_leavesEverythingElseUntouched() {
        assertThat(SapProjectRewriteHelper.rewriteBrowserAssignment("Chromium"))
            .isEqualTo("Chromium");
        assertThat(SapProjectRewriteHelper.rewriteBrowserAssignment("No Browser"))
            .isEqualTo("No Browser");
        assertThat(SapProjectRewriteHelper.rewriteBrowserAssignment(null)).isNull();
        assertThat(SapProjectRewriteHelper.rewriteBrowserAssignment("")).isEqualTo("");
    }

    @Test
    public void ensureInitCloseConnectionSteps_injectsBothOnEmptyTestCase() {
        boolean changed = SapProjectRewriteHelper.ensureInitCloseConnectionSteps(testCase);

        assertThat(changed).isTrue();
        assertThat(testCase.getTestSteps()).hasSize(2);
        assertThat(testCase.getTestSteps().get(0).getAction()).isEqualTo("sapInitConnection");
        assertThat(testCase.getTestSteps().get(0).getInput()).isEmpty();
        assertThat(testCase.getTestSteps().get(1).getAction()).isEqualTo("sapCloseConnection");
        assertThat(testCase.getTestSteps().get(1).getInput()).isEmpty();
    }

    @Test
    public void ensureInitCloseConnectionSteps_wrapsExistingSteps() {
        TestStep middle = testCase.addNewStep();
        middle.setObject("SAP_SYSTEM").setAction("sapExecuteTransaction").setInput("VA01");

        SapProjectRewriteHelper.ensureInitCloseConnectionSteps(testCase);

        assertThat(testCase.getTestSteps()).hasSize(3);
        assertThat(testCase.getTestSteps().get(0).getAction()).isEqualTo("sapInitConnection");
        assertThat(testCase.getTestSteps().get(1)).isSameAs(middle);
        assertThat(testCase.getTestSteps().get(2).getAction()).isEqualTo("sapCloseConnection");
    }

    @Test
    public void ensureInitCloseConnectionSteps_idempotent_doesNotDoubleInject() {
        SapProjectRewriteHelper.ensureInitCloseConnectionSteps(testCase);
        boolean changedAgain = SapProjectRewriteHelper.ensureInitCloseConnectionSteps(testCase);

        assertThat(changedAgain).isFalse();
        assertThat(testCase.getTestSteps()).hasSize(2);
    }

    @Test
    public void hasLeadingInitConnection_skipsCommentedSteps() {
        TestStep commented = testCase.addNewStep();
        commented.setObject("SAP").setAction("sapClick");
        commented.toggleComment();
        TestStep real = testCase.addNewStep();
        real.setObject("SAP").setAction("sapInitConnection");

        assertThat(SapProjectRewriteHelper.hasLeadingInitConnection(testCase)).isTrue();
    }
}

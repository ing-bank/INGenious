package com.ing.testdata.csv;

import static org.assertj.core.api.Assertions.assertThat;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.testdata.TestDataFactory;
import com.ing.datalib.testdata.model.Record;
import com.ing.datalib.testdata.model.TestDataModel;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Regression tests for Part 2 of the Scope bug: converting a test case between Test Plan,
 * Project Reusables, and Shared Reusables must explicitly update the Scope of the matching
 * Test Data entry as part of the conversion itself, and must correctly disambiguate when the
 * same scenario+testcase name collides across scopes.
 */
public class ProjectTestDataScopeConversionTest {
    private Path tempDir;
    private String originalUserDir;
    private Project project;
    private TestDataModel dataModel;

    @BeforeMethod
    public void setUp() throws Exception {
        // Must run before the first Project is ever constructed in this JVM - Project's
        // EnvTestData looks up the "csv" provider via TestDataFactory, which only registers it
        // once this classpath scan has run (normally done once at app startup, e.g. IDE Main).
        TestDataFactory.load();

        tempDir = Files.createTempDirectory("project-scope-conversion-test");
        // Shared Reusables live at an app-root "Shared" folder resolved from user.dir - isolate it
        // under the temp dir for this test so we never touch the real repo checkout.
        originalUserDir = System.getProperty("user.dir");
        System.setProperty("user.dir", tempDir.toString());

        project = new Project(tempDir.resolve("SampleProject").toString(), "csv").createProject();
        dataModel = project.getTestData().defData().addTestData();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        System.setProperty("user.dir", originalUserDir);
        Files
            .walk(tempDir)
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
    }

    private Record addDataRow(String scenario, String testcase, String scope) {
        Record record = dataModel.addRecord();
        record.setScenario(scenario);
        record.setTestcase(testcase);
        record.setScope(scope);
        return record;
    }

    private String scopeOf(String scenario, String testcase) {
        for (Record record : dataModel.getRecords()) {
            if (record.getScenario().equals(scenario) && record.getTestcase().equals(testcase)) {
                return record.getScope();
            }
        }
        throw new AssertionError("No Test Data row found for " + scenario + "/" + testcase);
    }

    @Test
    public void moveTestPlanToProjectReusable_UpdatesScope() throws Exception {
        Scenario scenario = project.addScenario("Login");
        TestCase testCase = scenario.addTestCase("Step1");
        addDataRow("Login", "Step1", "");

        project.moveTestCaseToReusable(testCase);

        assertThat(scopeOf("Login", "Step1")).isEqualTo("[Project]");
    }

    @Test
    public void moveProjectReusableToTestPlan_UpdatesScope() throws Exception {
        Scenario scenario = project.addReusableScenario("Login");
        TestCase testCase = scenario.addTestCase("Step1");
        addDataRow("Login", "Step1", "[Project]");

        project.moveTestCaseToTestPlan(testCase);

        assertThat(scopeOf("Login", "Step1")).isEqualTo("");
    }

    @Test
    public void moveProjectReusableToSharedReusable_UpdatesScope() throws Exception {
        Scenario scenario = project.addReusableScenario("Login");
        TestCase testCase = scenario.addTestCase("Step1");
        addDataRow("Login", "Step1", "[Project]");

        project.moveTestCaseToSharedReusable(testCase);

        assertThat(scopeOf("Login", "Step1")).isEqualTo("[Shared]");
    }

    @Test
    public void moveSharedReusableToProjectReusable_UpdatesScope() throws Exception {
        Scenario scenario = project.addSharedReusableScenario("Login");
        TestCase testCase = scenario.addTestCase("Step1");
        addDataRow("Login", "Step1", "[Shared]");

        project.moveSharedReusableToReusable(testCase);

        assertThat(scopeOf("Login", "Step1")).isEqualTo("[Project]");
    }

    @Test
    public void moveSharedReusableToTestPlan_UpdatesScope() throws Exception {
        Scenario scenario = project.addSharedReusableScenario("Login");
        TestCase testCase = scenario.addTestCase("Step1");
        addDataRow("Login", "Step1", "[Shared]");

        project.moveTestCaseToTestPlan(testCase);

        assertThat(scopeOf("Login", "Step1")).isEqualTo("");
    }

    @Test
    public void copyProjectReusableToSharedReusable_DoesNotTouchScope() throws Exception {
        Scenario scenario = project.addReusableScenario("Login");
        TestCase testCase = scenario.addTestCase("Step1");
        addDataRow("Login", "Step1", "[Project]");

        project.copyTestCaseToSharedReusable(testCase);

        // Copy leaves the source test case (and its Test Data entry) exactly where it was.
        assertThat(scopeOf("Login", "Step1")).isEqualTo("[Project]");
    }

    @Test
    public void moveUpdatesOnlyTheCollidingEntryThatActuallyConverted() throws Exception {
        // Same Scenario+TestCase name ("Login"/"Step1") exists in BOTH Test Plan and Shared
        // Reusables already - a genuine name collision across scopes. Only the Test Plan
        // entry should have its Scope updated when the Test Plan test case is converted;
        // the colliding Shared entry, already correctly scoped, must be left untouched.
        Scenario testPlanScenario = project.addScenario("Login");
        TestCase testPlanTestCase = testPlanScenario.addTestCase("Step1");
        project.addSharedReusableScenario("Login").addTestCase("Step1");

        Record testPlanRow = dataModel.addRecord();
        testPlanRow.setScenario("Login");
        testPlanRow.setTestcase("Step1");
        testPlanRow.setScope("");

        Record sharedRow = dataModel.addRecord();
        sharedRow.setScenario("Login");
        sharedRow.setTestcase("Step1");
        sharedRow.setScope("[Shared]");

        project.moveTestCaseToReusable(testPlanTestCase);

        assertThat(dataModel.getRecords().get(0).getScope())
            .as("The converted Test Plan entry must now be scoped to Project")
            .isEqualTo("[Project]");
        assertThat(dataModel.getRecords().get(1).getScope())
            .as("The colliding Shared entry must be left untouched")
            .isEqualTo("[Shared]");
    }
}

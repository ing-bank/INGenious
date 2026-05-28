package com.ing.datalib.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests for Scenario — constructor-based loading, test case management,
 * getLocation, lookup, add/remove, reusable filtering.
 * Uses a temp directory to satisfy filesystem I/O in loadTestcases().
 */
public class ScenarioTest {

    private File tempProjectDir;
    private File testPlanDir;
    private File scenarioDir;
    private Project project;

    @BeforeMethod
    public void setUp() throws IOException {
        // Create temp project structure: {project}/TestPlan/{scenarioName}/
        tempProjectDir = new File(System.getProperty("java.io.tmpdir"),
                "ScenarioTest_" + System.nanoTime());
        testPlanDir = new File(tempProjectDir, "TestPlan");
        scenarioDir = new File(testPlanDir, "LoginScenario");
        scenarioDir.mkdirs();

        // Create some CSV files to simulate existing test cases
        createCsvFile(scenarioDir, "TC_Login.csv");
        createCsvFile(scenarioDir, "TC_Logout.csv");

        // Mock project to return temp dir as location
        project = mock(Project.class);
        when(project.getLocation()).thenReturn(tempProjectDir.getAbsolutePath());
    }

    @AfterMethod
    public void tearDown() {
        // Clean up temp files
        deleteRecursive(tempProjectDir);
    }

    private void createCsvFile(File dir, String name) throws IOException {
        File f = new File(dir, name);
        try (FileWriter w = new FileWriter(f)) {
            w.write("Step,ObjectName,Description,Action,Input,Condition,Reference\n");
        }
    }

    private void deleteRecursive(File f) {
        if (f == null || !f.exists()) return;
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        f.delete();
    }

    // ---- Constructor & Loading ----

    @Test
    public void testConstructorLoadsTestCases() {
        Scenario scenario = new Scenario(project, "LoginScenario");
        // Should have loaded TC_Login and TC_Logout
        assertThat(scenario.getTestCases()).hasSize(2);
    }

    @Test
    public void testConstructorEmptyDirectory() {
        File emptyDir = new File(testPlanDir, "EmptyScenario");
        emptyDir.mkdirs();
        Scenario scenario = new Scenario(project, "EmptyScenario");
        assertThat(scenario.getTestCases()).isEmpty();
    }

    @Test
    public void testConstructorNonExistentDirectory() {
        Scenario scenario = new Scenario(project, "NonExistent");
        assertThat(scenario.getTestCases()).isEmpty();
    }

    // ---- getLocation ----

    @Test
    public void testGetLocation() {
        Scenario scenario = new Scenario(project, "LoginScenario");
        assertThat(scenario.getLocation()).isEqualTo(
                tempProjectDir.getAbsolutePath() + File.separator + "TestPlan" + File.separator + "LoginScenario");
    }

    // ---- getName / toString ----

    @Test
    public void testGetName() {
        Scenario scenario = new Scenario(project, "LoginScenario");
        assertThat(scenario.getName()).isEqualTo("LoginScenario");
    }

    @Test
    public void testToString() {
        Scenario scenario = new Scenario(project, "LoginScenario");
        assertThat(scenario.toString()).isEqualTo("LoginScenario");
    }

    // ---- getTestCaseByName ----

    @Test
    public void testGetTestCaseByName_found() {
        Scenario scenario = new Scenario(project, "LoginScenario");
        // CSV names have .csv stripped: "TC_Login.csv" → "TC_Login"
        TestCase tc = scenario.getTestCaseByName("TC_Login");
        assertThat(tc).isNotNull();
        assertThat(tc.getName()).isEqualTo("TC_Login");
    }

    @Test
    public void testGetTestCaseByName_caseInsensitive() {
        Scenario scenario = new Scenario(project, "LoginScenario");
        TestCase tc = scenario.getTestCaseByName("tc_login");
        assertThat(tc).isNotNull();
    }

    @Test
    public void testGetTestCaseByName_notFound() {
        Scenario scenario = new Scenario(project, "LoginScenario");
        assertThat(scenario.getTestCaseByName("NonExistent")).isNull();
    }

    // ---- addTestCase ----

    @Test
    public void testAddTestCase_unique() {
        Scenario scenario = new Scenario(project, "LoginScenario");
        int before = scenario.getTestCases().size();
        TestCase tc = scenario.addTestCase("TC_New");
        assertThat(tc).isNotNull();
        assertThat(tc.getName()).isEqualTo("TC_New");
        assertThat(scenario.getTestCases()).hasSize(before + 1);
    }

    @Test
    public void testAddTestCase_duplicate() {
        Scenario scenario = new Scenario(project, "LoginScenario");
        TestCase tc = scenario.addTestCase("TC_Login");
        assertThat(tc).isNull(); // already exists
    }

    // ---- removeTestCase ----

    @Test
    public void testRemoveTestCase() {
        Scenario scenario = new Scenario(project, "LoginScenario");
        TestCase tc = scenario.getTestCaseByName("TC_Login");
        int before = scenario.getTestCases().size();
        scenario.removeTestCase(tc);
        assertThat(scenario.getTestCases()).hasSize(before - 1);
        assertThat(scenario.getTestCaseByName("TC_Login")).isNull();
    }

    // ---- Reusable filtering ----

    @Test
    public void testGetTestcasesAlone_excludesReusables() {
        Scenario scenario = new Scenario(project, "LoginScenario");
        // Mark first test case as reusable
        TestCase tc = scenario.getTestCases().get(0);
        tc.setReusable(new Reusable());

        assertThat(scenario.getTestcasesAlone()).hasSize(1);
        assertThat(scenario.getReusables()).hasSize(1);
    }

    @Test
    public void testGetTestcaseCount_excludesReusables() {
        Scenario scenario = new Scenario(project, "LoginScenario");
        scenario.getTestCases().get(0).setReusable(new Reusable());
        assertThat(scenario.getTestcaseCount()).isEqualTo(1);
    }

    @Test
    public void testGetReusableCount() {
        Scenario scenario = new Scenario(project, "LoginScenario");
        assertThat(scenario.getReusableCount()).isEqualTo(0);
        scenario.getTestCases().get(0).setReusable(new Reusable());
        assertThat(scenario.getReusableCount()).isEqualTo(1);
    }

    // ---- getProject ----

    @Test
    public void testGetProject() {
        Scenario scenario = new Scenario(project, "LoginScenario");
        assertThat(scenario.getProject()).isSameAs(project);
    }

    // ---- getIndexOfTestCaseByName ----

    @Test
    public void testGetIndexOfTestCaseByName() {
        Scenario scenario = new Scenario(project, "LoginScenario");
        int idx = scenario.getIndexOfTestCaseByName("TC_Login");
        assertThat(idx).isGreaterThanOrEqualTo(0);
    }

    @Test
    public void testGetIndexOfTestCaseByName_notFound() {
        Scenario scenario = new Scenario(project, "LoginScenario");
        assertThat(scenario.getIndexOfTestCaseByName("Missing")).isEqualTo(-1);
    }
}

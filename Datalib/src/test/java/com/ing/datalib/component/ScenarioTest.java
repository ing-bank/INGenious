package com.ing.datalib.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
        tempProjectDir =
            new File(System.getProperty("java.io.tmpdir"), "ScenarioTest_" + System.nanoTime());
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
        assertThat(scenario.getLocation())
            .isEqualTo(
                tempProjectDir.getAbsolutePath() +
                File.separator +
                "TestPlan" +
                File.separator +
                "LoginScenario"
            );
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

    // ---- getScopeLabel ----

    @Test
    public void testGetScopeLabel_testPlan() {
        Scenario scenario = new Scenario(project, "LoginScenario", Scenario.Source.TEST_PLAN);
        assertThat(scenario.getScopeLabel()).isEqualTo("TestPlan");
    }

    @Test
    public void testGetScopeLabel_reusableComponents() {
        Scenario scenario = new Scenario(
            project,
            "LoginScenario",
            Scenario.Source.REUSABLE_COMPONENTS
        );
        assertThat(scenario.getScopeLabel()).isEqualTo("Project");
    }

    @Test
    public void testGetScopeLabel_sharedReusableComponents() {
        Scenario scenario = new Scenario(
            project,
            "LoginScenario",
            Scenario.Source.SHARED_REUSABLE_COMPONENTS
        );
        assertThat(scenario.getScopeLabel()).isEqualTo("Shared");
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

    // ---- rename / renameReusable / renameSharedReusable ----
    //
    // Regression coverage for a rename-ordering bug: rename() used to physically
    // move the scenario directory, then run Project.refactorScenario() (which
    // reloads this scenario's own not-yet-opened test cases), and only update the
    // in-memory `name` field afterwards. Since getLocation() is derived from
    // `name`, any of this scenario's own unloaded test cases would try to load
    // from the old, already-renamed-away directory during the refactor pass —
    // silently failing, replacing their content with a synthetic blank step, and
    // (via save()) recreating an orphan directory under the old name. This is
    // specific to scenario rename: a test-case-only rename never moves a
    // scenario directory, so it never hits this window.

    @Test
    public void testRename_locationReflectsNewNameDuringRefactor() {
        Scenario scenario = new Scenario(project, "LoginScenario");
        File[] locationDuringRefactor = new File[1];
        doAnswer(
                invocation -> {
                    locationDuringRefactor[0] = new File(scenario.getLocation());
                    return null;
                }
            )
            .when(project)
            .refactorScenario("LoginScenario", "LoginScenarioRenamed");

        Boolean result = scenario.rename("LoginScenarioRenamed");

        assertThat(result).isTrue();
        assertThat(scenario.getName()).isEqualTo("LoginScenarioRenamed");
        assertThat(locationDuringRefactor[0]).isNotNull();
        assertThat(locationDuringRefactor[0]).exists();
        assertThat(locationDuringRefactor[0].getName()).isEqualTo("LoginScenarioRenamed");
    }

    @Test
    public void testRenameReusable_locationReflectsNewNameDuringRefactor() {
        new File(tempProjectDir, "ReusableComponents" + File.separator + "LoginScenario").mkdirs();
        Scenario scenario = new Scenario(
            project,
            "LoginScenario",
            Scenario.Source.REUSABLE_COMPONENTS
        );
        File[] locationDuringRefactor = new File[1];
        doAnswer(
                invocation -> {
                    locationDuringRefactor[0] = new File(scenario.getLocation());
                    return null;
                }
            )
            .when(project)
            .refactorScenario("LoginScenario", "LoginScenarioRenamed");

        Boolean result = scenario.renameReusable("LoginScenarioRenamed");

        assertThat(result).isTrue();
        assertThat(scenario.getName()).isEqualTo("LoginScenarioRenamed");
        assertThat(locationDuringRefactor[0]).exists();
        assertThat(locationDuringRefactor[0].getName()).isEqualTo("LoginScenarioRenamed");
    }

    @Test
    public void testRenameSharedReusable_locationReflectsNewNameDuringRefactor() {
        new File(tempProjectDir, "SharedReusableComponents" + File.separator + "LoginScenario")
        .mkdirs();
        Scenario scenario = new Scenario(
            project,
            "LoginScenario",
            Scenario.Source.SHARED_REUSABLE_COMPONENTS
        );
        File[] locationDuringRefactor = new File[1];
        doAnswer(
                invocation -> {
                    locationDuringRefactor[0] = new File(scenario.getLocation());
                    return null;
                }
            )
            .when(project)
            .refactorScenario("LoginScenario", "LoginScenarioRenamed");

        Boolean result = scenario.renameSharedReusable("LoginScenarioRenamed");

        assertThat(result).isTrue();
        assertThat(scenario.getName()).isEqualTo("LoginScenarioRenamed");
        assertThat(locationDuringRefactor[0]).exists();
        assertThat(locationDuringRefactor[0].getName()).isEqualTo("LoginScenarioRenamed");
    }

    @Test
    public void testRename_repeatedRenamesEachSeeCorrectLocation() {
        Scenario scenario = new Scenario(project, "LoginScenario");
        List<String> observedDirNames = new ArrayList<>();
        doAnswer(
                invocation -> {
                    observedDirNames.add(new File(scenario.getLocation()).getName());
                    return null;
                }
            )
            .when(project)
            .refactorScenario(anyString(), anyString());

        assertThat(scenario.rename("Step2")).isTrue();
        assertThat(scenario.rename("Step3")).isTrue();

        assertThat(scenario.getName()).isEqualTo("Step3");
        assertThat(observedDirNames).containsExactly("Step2", "Step3");
        verify(project).refactorScenario("LoginScenario", "Step2");
        verify(project).refactorScenario("Step2", "Step3");
    }

    @Test
    public void testRename_ownUnopenedTestCaseSurvivesRefactorWithoutCorruption()
        throws IOException {
        // Give TC_Login real, non-blank step content so corruption is detectable.
        File tcFile = new File(scenarioDir, "TC_Login.csv");
        try (FileWriter w = new FileWriter(tcFile)) {
            w.write("Step,ObjectName,Description,Action,Input,Condition,Reference\n");
            w.write("1,LoginButton,Click login,Click,,,\n");
        }

        Scenario scenario = new Scenario(project, "LoginScenario");
        TestCase tc = scenario.getTestCaseByName("TC_Login");
        // Not yet opened this session: steps aren't loaded into memory.
        assertThat(tc.getTestSteps()).isEmpty();

        // Mirror what Project.refactorScenario() does: refactor every scenario,
        // including this one, via the real (unmocked) Scenario/TestCase logic.
        doAnswer(
                invocation -> {
                    scenario.refactorScenario("LoginScenario", "LoginScenarioRenamed");
                    return null;
                }
            )
            .when(project)
            .refactorScenario("LoginScenario", "LoginScenarioRenamed");

        assertThat(scenario.rename("LoginScenarioRenamed")).isTrue();

        // No orphan directory should be left behind under the old name.
        assertThat(new File(testPlanDir, "LoginScenario")).doesNotExist();

        tc.reload();
        assertThat(tc.getTestSteps()).hasSize(1);
        assertThat(tc.getTestSteps().get(0).getObject()).isEqualTo("LoginButton");
    }

    // ---- renameReusable() must not evict a brand-new scenario from the project ----
    //
    // Root cause of a separate false-positive-red bug: a freshly created reusable
    // scenario has no directory on disk until its first test case is saved
    // (Project.addReusableScenario() never calls mkdirs()). renameReusable()'s
    // "clean up stale reusable scenarios" pass used to remove ANY scenario whose
    // directory doesn't currently exist -- including `this`, wrongly treating
    // "not yet persisted" the same as "deleted externally". That permanently
    // evicted the scenario from Project.reusableScenarios (though it stayed
    // visible in the tree, which holds its own reference), so any later
    // Project.getReusableScenarioByName() lookup -- e.g. from ActionRenderer
    // validating a dropped Execute step -- failed and showed a false-positive
    // "Reusable is not available in the Project" error until the project was
    // reloaded from disk.

    @Test
    public void testRenameReusable_doesNotEvictNewlyCreatedScenarioFromProject()
        throws IOException {
        // Faithfully replicate Project.getReusableScenarios()/getReusableScenarioByName()
        // (including the live disk-existence check), since the shared mock in setUp()
        // doesn't wire these up.
        List<Scenario> reusableScenarios = new ArrayList<>();
        when(project.getReusableScenarios()).thenReturn(reusableScenarios);
        when(project.getReusableScenarioByName(anyString()))
            .thenAnswer(
                inv -> {
                    String n = inv.getArgument(0);
                    for (Scenario s : reusableScenarios) {
                        if (s.getName().equalsIgnoreCase(n) && new File(s.getLocation()).exists()) {
                            return s;
                        }
                    }
                    return null;
                }
            );

        // A brand-new reusable scenario: no test case saved yet, so its directory
        // does not exist on disk (mirrors Project.addReusableScenario()).
        Scenario scenario = new Scenario(
            project,
            "NewScenario",
            Scenario.Source.REUSABLE_COMPONENTS
        );
        reusableScenarios.add(scenario);

        assertThat(scenario.renameReusable("test")).isTrue();
        assertThat(reusableScenarios).contains(scenario);

        TestCase newTc = scenario.addTestCase("NewTestCase");
        assertThat(newTc).isNotNull();

        Scenario found = project.getReusableScenarioByName("test");
        assertThat(found).isSameAs(scenario);
        assertThat(found.getTestCaseByName("NewTestCase")).isNotNull();
    }
}

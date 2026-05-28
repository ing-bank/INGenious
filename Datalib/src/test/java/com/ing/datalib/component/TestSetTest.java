package com.ing.datalib.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests for TestSet — constructor name stripping, step management,
 * getExecutableSteps, move operations, save state, getLocation, TableModel.
 */
public class TestSetTest {

    private Release release;
    private Project project;
    private TestSet testSet;
    private File tempProjectDir;

    @BeforeMethod
    public void setUp() throws IOException {
        // TestSet constructor creates ExecutionSettings that needs project location
        tempProjectDir = new File(System.getProperty("java.io.tmpdir"),
                "TestSetTest_" + System.nanoTime());
        new File(tempProjectDir, "Settings/TestExecution/Release1/TS_Smoke").mkdirs();

        project = mock(Project.class);
        when(project.getLocation()).thenReturn(tempProjectDir.getAbsolutePath());

        release = mock(Release.class);
        when(release.getProject()).thenReturn(project);
        when(release.getName()).thenReturn("Release1");
        when(release.getLocation()).thenReturn(
                tempProjectDir.getAbsolutePath() + File.separator + "TestLab" + File.separator + "Release1");

        testSet = new TestSet(release, "TS_Smoke.csv");
    }

    @AfterMethod
    public void tearDown() {
        deleteRecursive(tempProjectDir);
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

    // ---- Constructor name stripping ----

    @Test
    public void testConstructorStripsCsvExtension() {
        assertThat(testSet.getName()).isEqualTo("TS_Smoke");
    }

    @Test
    public void testConstructorWithoutExtension() {
        TestSet ts = new TestSet(release, "TS_NoExt");
        assertThat(ts.getName()).isEqualTo("TS_NoExt");
    }

    // ---- getLocation ----

    @Test
    public void testGetLocation() {
        String expected = release.getLocation() + File.separator + "TS_Smoke.csv";
        assertThat(testSet.getLocation()).isEqualTo(expected);
    }

    // ---- toString ----

    @Test
    public void testToString() {
        assertThat(testSet.toString()).isEqualTo("TS_Smoke");
    }

    // ---- isSaved / setSaved ----

    @Test
    public void testInitiallySaved() {
        assertThat(testSet.isSaved()).isTrue();
    }

    @Test
    public void testSetSaved() {
        testSet.setSaved(false);
        assertThat(testSet.isSaved()).isFalse();
        testSet.setSaved(true);
        assertThat(testSet.isSaved()).isTrue();
    }

    // ---- addNewStep ----

    @Test
    public void testAddNewStep() {
        ExecutionStep step = testSet.addNewStep();
        assertThat(step).isNotNull();
        assertThat(testSet.getTestSteps()).hasSize(1);
    }

    @Test
    public void testAddMultipleSteps() {
        testSet.addNewStep();
        testSet.addNewStep();
        testSet.addNewStep();
        assertThat(testSet.getTestSteps()).hasSize(3);
    }

    // ---- addNewStepAt ----

    @Test
    public void testAddNewStepAt() {
        testSet.addNewStep();
        testSet.addNewStep();
        ExecutionStep inserted = testSet.addNewStepAt(1);
        assertThat(testSet.getTestSteps()).hasSize(3);
        assertThat(testSet.getTestSteps().get(1)).isSameAs(inserted);
    }

    // ---- getExecutableSteps ----

    @Test
    public void testGetExecutableSteps_empty() {
        assertThat(testSet.getExecutableSteps()).isEmpty();
    }

    @Test
    public void testGetExecutableSteps_filtersCorrectly() {
        ExecutionStep s1 = testSet.addNewStep();
        s1.setExecute("true");
        ExecutionStep s2 = testSet.addNewStep();
        s2.setExecute("false");
        ExecutionStep s3 = testSet.addNewStep();
        s3.setExecute("true");

        assertThat(testSet.getExecutableSteps()).hasSize(2);
        assertThat(testSet.getExecutableSteps()).containsExactly(s1, s3);
    }

    // ---- moveRowsUp / moveRowsDown ----

    @Test
    public void testMoveRowsUp_atTop() {
        testSet.addNewStep();
        testSet.addNewStep();
        assertThat(testSet.moveRowsUp(0, 0)).isFalse();
    }

    @Test
    public void testMoveRowsUp_success() {
        ExecutionStep s1 = testSet.addNewStep();
        ExecutionStep s2 = testSet.addNewStep();
        assertThat(testSet.moveRowsUp(1, 1)).isTrue();
        assertThat(testSet.getTestSteps().get(0)).isSameAs(s2);
    }

    @Test
    public void testMoveRowsDown_atBottom() {
        testSet.addNewStep();
        testSet.addNewStep();
        assertThat(testSet.moveRowsDown(1, 1)).isFalse();
    }

    @Test
    public void testMoveRowsDown_success() {
        ExecutionStep s1 = testSet.addNewStep();
        ExecutionStep s2 = testSet.addNewStep();
        assertThat(testSet.moveRowsDown(0, 0)).isTrue();
        assertThat(testSet.getTestSteps().get(1)).isSameAs(s1);
    }

    // ---- getRelease / getProject ----

    @Test
    public void testGetRelease() {
        assertThat(testSet.getRelease()).isSameAs(release);
    }

    @Test
    public void testGetProject() {
        assertThat(testSet.getProject()).isSameAs(project);
    }

    @Test
    public void testSetRelease() {
        Release newRelease = mock(Release.class);
        when(newRelease.getProject()).thenReturn(project);
        testSet.setRelease(newRelease);
        assertThat(testSet.getRelease()).isSameAs(newRelease);
    }

    // ---- TableModel basics ----

    @Test
    public void testGetRowCount() {
        assertThat(testSet.getRowCount()).isEqualTo(0);
        testSet.addNewStep();
        assertThat(testSet.getRowCount()).isEqualTo(1);
    }

    @Test
    public void testGetColumnCount() {
        assertThat(testSet.getColumnCount()).isEqualTo(ExecutionStep.HEADERS.size());
    }

    @Test
    public void testGetColumnName() {
        assertThat(testSet.getColumnName(0)).isEqualTo("Execute");
        assertThat(testSet.getColumnName(1)).isEqualTo("TestScenario");
    }

    @Test
    public void testGetColumnClassFirstIsBool() {
        assertThat(testSet.getColumnClass(0)).isEqualTo(Boolean.class);
    }

    @Test
    public void testGetColumnClassOthersAreObject() {
        assertThat(testSet.getColumnClass(1)).isEqualTo(Object.class);
    }

    // ---- addRow ----

    @Test
    public void testAddRowReturnsTrue() {
        assertThat(testSet.addRow()).isTrue();
        assertThat(testSet.getTestSteps()).hasSize(1);
    }

    // ---- printString ----

    @Test
    public void testPrintString() {
        testSet.addNewStep();
        String str = testSet.printString();
        assertThat(str).contains("TestCase - TS_Smoke");
        assertThat(str).contains("TestSteps - 1");
    }

    // ---- resetExecSettingsLocation ----

    @Test
    public void testResetExecSettingsLocation() {
        // Just verify no exception
        when(release.getName()).thenReturn("Release2");
        testSet.resetExecSettingsLocation();
        // ExecutionSettings location should have been updated
        assertThat(testSet.getExecSettings()).isNotNull();
    }

    // ---- getExecSettings ----

    @Test
    public void testGetExecSettings() {
        assertThat(testSet.getExecSettings()).isNotNull();
    }
}

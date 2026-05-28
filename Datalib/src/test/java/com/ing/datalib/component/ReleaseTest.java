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
 * Tests for Release — constructor-based loading, test set management,
 * getLocation, lookup, add/remove, TableModel basics.
 * Uses a temp directory to satisfy filesystem I/O in loadTestSets().
 */
public class ReleaseTest {

    private File tempProjectDir;
    private File testLabDir;
    private File releaseDir;
    private Project project;

    @BeforeMethod
    public void setUp() throws IOException {
        // Create temp project structure: {project}/TestLab/{releaseName}/
        tempProjectDir = new File(System.getProperty("java.io.tmpdir"),
                "ReleaseTest_" + System.nanoTime());
        testLabDir = new File(tempProjectDir, "TestLab");
        releaseDir = new File(testLabDir, "Release1");
        releaseDir.mkdirs();

        // Also create Settings/TestExecution/Release1/ for TestSet's ExecutionSettings
        new File(tempProjectDir, "Settings/TestExecution/Release1").mkdirs();

        // Create CSV files to simulate existing test sets
        createCsvFile(releaseDir, "TS_Smoke.csv");
        createCsvFile(releaseDir, "TS_Regression.csv");

        project = mock(Project.class);
        when(project.getLocation()).thenReturn(tempProjectDir.getAbsolutePath());
    }

    @AfterMethod
    public void tearDown() {
        deleteRecursive(tempProjectDir);
    }

    private void createCsvFile(File dir, String name) throws IOException {
        File f = new File(dir, name);
        try (FileWriter w = new FileWriter(f)) {
            w.write("Execute,TestScenario,TestCase,Description,Iteration,Browser,Platform,Status\n");
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
    public void testConstructorLoadsTestSets() {
        Release release = new Release(project, "Release1");
        assertThat(release.getTestSets()).hasSize(2);
    }

    @Test
    public void testConstructorEmptyDirectory() {
        File emptyDir = new File(testLabDir, "EmptyRelease");
        emptyDir.mkdirs();
        // Create settings dir for this release too
        new File(tempProjectDir, "Settings/TestExecution/EmptyRelease").mkdirs();
        Release release = new Release(project, "EmptyRelease");
        assertThat(release.getTestSets()).isEmpty();
    }

    @Test
    public void testConstructorNonExistentDirectory() {
        Release release = new Release(project, "NonExistent");
        assertThat(release.getTestSets()).isEmpty();
    }

    // ---- getLocation ----

    @Test
    public void testGetLocation() {
        Release release = new Release(project, "Release1");
        assertThat(release.getLocation()).isEqualTo(
                tempProjectDir.getAbsolutePath() + File.separator + "TestLab" + File.separator + "Release1");
    }

    // ---- getName / toString ----

    @Test
    public void testGetName() {
        Release release = new Release(project, "Release1");
        assertThat(release.getName()).isEqualTo("Release1");
    }

    @Test
    public void testToString() {
        Release release = new Release(project, "Release1");
        assertThat(release.toString()).isEqualTo("Release1");
    }

    // ---- getTestSetByName ----

    @Test
    public void testGetTestSetByName_found() {
        Release release = new Release(project, "Release1");
        TestSet ts = release.getTestSetByName("TS_Smoke");
        assertThat(ts).isNotNull();
        assertThat(ts.getName()).isEqualTo("TS_Smoke");
    }

    @Test
    public void testGetTestSetByName_notFound() {
        Release release = new Release(project, "Release1");
        assertThat(release.getTestSetByName("NonExistent")).isNull();
    }

    // ---- addTestSet ----

    @Test
    public void testAddTestSet_unique() {
        Release release = new Release(project, "Release1");
        int before = release.getTestSets().size();
        TestSet ts = release.addTestSet("TS_New");
        assertThat(ts).isNotNull();
        assertThat(ts.getName()).isEqualTo("TS_New");
        assertThat(release.getTestSets()).hasSize(before + 1);
    }

    @Test
    public void testAddTestSet_duplicate() {
        Release release = new Release(project, "Release1");
        TestSet ts = release.addTestSet("TS_Smoke");
        assertThat(ts).isNull();
    }

    // ---- removeTestSet ----

    @Test
    public void testRemoveTestSet() {
        Release release = new Release(project, "Release1");
        TestSet ts = release.getTestSetByName("TS_Smoke");
        int before = release.getTestSets().size();
        release.removeTestSet(ts);
        assertThat(release.getTestSets()).hasSize(before - 1);
        assertThat(release.getTestSetByName("TS_Smoke")).isNull();
    }

    // ---- getIndexOfTestSetByName ----

    @Test
    public void testGetIndexOfTestSetByName() {
        Release release = new Release(project, "Release1");
        int idx = release.getIndexOfTestSetByName("TS_Smoke");
        assertThat(idx).isGreaterThanOrEqualTo(0);
    }

    @Test
    public void testGetIndexOfTestSetByName_notFound() {
        Release release = new Release(project, "Release1");
        assertThat(release.getIndexOfTestSetByName("Missing")).isEqualTo(-1);
    }

    // ---- getProject ----

    @Test
    public void testGetProject() {
        Release release = new Release(project, "Release1");
        assertThat(release.getProject()).isSameAs(project);
    }

    // ---- TableModel basics ----

    @Test
    public void testGetRowCount() {
        Release release = new Release(project, "Release1");
        assertThat(release.getRowCount()).isEqualTo(2);
    }

    @Test
    public void testGetColumnNameFirst() {
        Release release = new Release(project, "Release1");
        assertThat(release.getColumnName(0)).isEqualTo("TestSet");
    }

    @Test
    public void testGetColumnNameOther() {
        Release release = new Release(project, "Release1");
        assertThat(release.getColumnName(1)).isEqualTo("Component 1");
    }

    @Test
    public void testIsCellEditable() {
        Release release = new Release(project, "Release1");
        assertThat(release.isCellEditable(0, 0)).isFalse();
        assertThat(release.isCellEditable(0, 1)).isTrue();
    }

    @Test
    public void testAddRowReturnsFalse() {
        Release release = new Release(project, "Release1");
        assertThat(release.addRow()).isFalse();
    }

    // ---- printString ----

    @Test
    public void testPrintString() {
        Release release = new Release(project, "Release1");
        String str = release.printString();
        assertThat(str).contains("Release - Release1");
        assertThat(str).contains("TestSet - 2");
    }
}

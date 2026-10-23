package com.ing.testdata.csv;

import static org.assertj.core.api.Assertions.assertThat;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.TestData;
import com.ing.datalib.testdata.TestDataFactory;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Regression tests for the INGenious suggestion: "Allow duplication of environments from
 * another scope - list Project and Shared test data sheets in this selection window." A new
 * environment created in one store (Project or Shared Test Data) must be able to duplicate
 * sheets from an existing environment that lives in the <em>other</em> store.
 */
public class EnvTestDataCrossScopeNewEnvironmentTest {
    private Path tempDir;
    private String originalUserDir;
    private Project project;

    @BeforeMethod
    public void setUp() throws Exception {
        TestDataFactory.load();

        tempDir = Files.createTempDirectory("cross-scope-new-environment-test");
        originalUserDir = System.getProperty("user.dir");
        // Shared Test Data resolves from user.dir - isolate it under the temp dir.
        System.setProperty("user.dir", tempDir.toString());

        project = new Project(tempDir.resolve("SampleProject").toString(), "csv").createProject();
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

    private void importProjectSheet(String sheetName) throws IOException {
        File source = tempDir.resolve(sheetName + ".csv").toFile();
        try (FileWriter fw = new FileWriter(source)) {
            fw.write("Scenario,Flow,Scope,Iteration,SubIteration,URL\n");
            fw.write("Buy,Step1,,1,1,https://project\n");
        }
        project.getTestData().importTestData(source, Arrays.asList("Default"));
    }

    private void importSharedSheet(String sheetName) throws IOException {
        File source = tempDir.resolve("shared-" + sheetName + ".csv").toFile();
        try (FileWriter fw = new FileWriter(source)) {
            fw.write("Scenario,Flow,Scope,Iteration,SubIteration,URL\n");
            fw.write("Buy,Step1,,1,1,https://shared\n");
        }
        // importTestData keys the new sheet off the source file name.
        File renamed = tempDir.resolve(sheetName + ".csv").toFile();
        renamed.delete();
        source.renameTo(renamed);
        project.getSharedTestData().importTestData(renamed, Arrays.asList("Default"));
    }

    @Test
    public void newProjectEnvironment_CanDuplicateSheetFromSharedTestData() throws Exception {
        importSharedSheet("Basic");

        project
            .getTestData()
            .createNewEnvironment(
                "QA",
                project.getSharedTestData(),
                "Default",
                Collections.singletonList("Basic"),
                false
            );

        TestData qaEnv = project.getTestData().getTestDataFor("QA");
        assertThat(qaEnv).isNotNull();
        assertThat(qaEnv.getByNameIgnoreCase("Basic")).isNotNull();
        assertThat(qaEnv.getByNameIgnoreCase("Basic").getRecords()).hasSize(1);
    }

    @Test
    public void newSharedEnvironment_CanDuplicateSheetFromProjectTestData() throws Exception {
        importProjectSheet("Basic");

        project
            .getSharedTestData()
            .createNewEnvironment(
                "QA",
                project.getTestData(),
                "Default",
                Collections.singletonList("Basic"),
                false
            );

        TestData qaEnv = project.getSharedTestData().getTestDataFor("QA");
        assertThat(qaEnv).isNotNull();
        assertThat(qaEnv.getByNameIgnoreCase("Basic")).isNotNull();
        assertThat(qaEnv.getByNameIgnoreCase("Basic").getRecords()).hasSize(1);
    }

    @Test
    public void sameScopeDuplication_StillWorksUnchanged() throws Exception {
        importProjectSheet("Basic");

        project
            .getTestData()
            .createNewEnvironment("QA", "Default", Collections.singletonList("Basic"), false);

        TestData qaEnv = project.getTestData().getTestDataFor("QA");
        assertThat(qaEnv.getByNameIgnoreCase("Basic")).isNotNull();
    }
}

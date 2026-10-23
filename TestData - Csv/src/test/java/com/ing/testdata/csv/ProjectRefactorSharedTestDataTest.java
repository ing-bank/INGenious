package com.ing.testdata.csv;

import static org.assertj.core.api.Assertions.assertThat;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestData;
import com.ing.datalib.testdata.TestDataFactory;
import com.ing.datalib.testdata.model.Record;
import com.ing.datalib.testdata.model.TestDataModel;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Regression tests for #388 ("Fix testcase testdata rename issues"): {@link
 * Project#refactorScenario} / {@link Project#refactorTestCase} must also update rows in Shared
 * Test Data, not just the project's own Test Data.
 */
public class ProjectRefactorSharedTestDataTest {
    private Path tempDir;
    private String originalUserDir;
    private Project project;

    @BeforeMethod
    public void setUp() throws Exception {
        TestDataFactory.load();

        tempDir = Files.createTempDirectory("project-refactor-shared-testdata-test");
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

    private void importSharedSheet(String sheetName, String scope) throws IOException {
        File source = tempDir.resolve(sheetName + ".csv").toFile();
        try (FileWriter fw = new FileWriter(source)) {
            fw.write("Scenario,Flow,Scope,Iteration,SubIteration\n");
            fw.write("Login,TC_Login," + scope + ",1,1\n");
        }
        project.getSharedTestData().importTestData(source, Arrays.asList("Default"));
    }

    private Record firstRecord(TestData env, String sheetName) {
        TestDataModel model = env.getByNameIgnoreCase(sheetName);
        model.loadTableModel();
        return model.getRecords().get(0);
    }

    @Test
    public void refactorScenarioUpdatesSharedTestDataRows() throws Exception {
        importSharedSheet("Basic", "");

        project.refactorScenario("Login", "LoginRenamed", Scenario.Source.TEST_PLAN);

        TestData sharedDefault = project.getSharedTestData().getTestDataFor("Default");
        assertThat(firstRecord(sharedDefault, "Basic").getScenario()).isEqualTo("LoginRenamed");
    }

    @Test
    public void refactorTestCaseUpdatesSharedTestDataRows() throws Exception {
        importSharedSheet("Basic", "");

        project.refactorTestCase("Login", "TC_Login", "TC_LoginRenamed", Scenario.Source.TEST_PLAN);

        TestData sharedDefault = project.getSharedTestData().getTestDataFor("Default");
        assertThat(firstRecord(sharedDefault, "Basic").getTestcase()).isEqualTo("TC_LoginRenamed");
    }

    @Test
    public void refactorScenarioRespectsScopeInSharedTestDataRows() throws Exception {
        File source = tempDir.resolve("Scoped.csv").toFile();
        try (FileWriter fw = new FileWriter(source)) {
            fw.write("Scenario,Flow,Scope,Iteration,SubIteration\n");
            fw.write("Login,TC_Login,,1,1\n");
            fw.write("Login,TC_Login,[Project],1,1\n");
        }
        project.getSharedTestData().importTestData(source, Arrays.asList("Default"));

        project.refactorScenario("Login", "LoginRenamed", Scenario.Source.REUSABLE_COMPONENTS);

        TestData sharedDefault = project.getSharedTestData().getTestDataFor("Default");
        TestDataModel model = sharedDefault.getByNameIgnoreCase("Scoped");
        model.loadTableModel();
        assertThat(model.getRecords().get(0).getScenario()).isEqualTo("Login");
        assertThat(model.getRecords().get(1).getScenario()).isEqualTo("LoginRenamed");
    }
}

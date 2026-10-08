package com.ing.testdata.csv;

import static org.assertj.core.api.Assertions.assertThat;

import com.ing.datalib.component.EnvTestData;
import com.ing.datalib.component.Project;
import com.ing.datalib.testdata.TestDataFactory;
import com.ing.datalib.testdata.model.TestDataModel;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests for {@link EnvTestData#importTestData(File, java.util.Collection)} - importing a CSV
 * datasheet into one or more environments of either the project's own Test Data or the Shared
 * Test Data.
 */
public class EnvTestDataImportTest {
    private Path tempDir;
    private String originalUserDir;
    private Project project;

    @BeforeMethod
    public void setUp() throws Exception {
        // Registers the "csv" provider so Project's EnvTestData can resolve it.
        TestDataFactory.load();

        tempDir = Files.createTempDirectory("envtestdata-import-test");
        // Shared Test Data resolves from user.dir - isolate it under the temp dir.
        originalUserDir = System.getProperty("user.dir");
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

    private File writeSourceSheet(String fileName) throws IOException {
        File source = tempDir.resolve(fileName).toFile();
        try (FileWriter fw = new FileWriter(source)) {
            fw.write("Scenario,Flow,Scope,Iteration,SubIteration,Amount\n");
            fw.write("Buy,Step1,,1,1,100\n");
        }
        return source;
    }

    @Test
    public void importsSheetIntoEachSelectedProjectEnvironment() throws Exception {
        File source = writeSourceSheet("Orders.csv");
        EnvTestData env = project.getTestData();
        env.createNewEnvironment("QA");

        Map<String, TestDataModel> imported = env.importTestData(
            source,
            Arrays.asList("Default", "QA")
        );

        assertThat(imported.keySet()).containsExactlyInAnyOrder("Default", "QA");
        assertThat(new File(env.getTestDataFor("Default").getLocation(), "Orders.csv")).exists();
        assertThat(new File(env.getTestDataFor("QA").getLocation(), "Orders.csv")).exists();
        assertThat(env.getTestDataFor("QA").getByNameIgnoreCase("Orders")).isNotNull();
    }

    @Test
    public void skipsEnvironmentsThatAlreadyHaveASheetWithTheSameName() throws Exception {
        File source = writeSourceSheet("Orders.csv");
        EnvTestData env = project.getTestData();
        env.createNewEnvironment("QA");

        env.importTestData(source, Arrays.asList("Default"));
        Map<String, TestDataModel> second = env.importTestData(
            source,
            Arrays.asList("Default", "QA")
        );

        assertThat(second.keySet()).containsExactly("QA");
    }

    @Test
    public void ignoresUnknownEnvironments() throws Exception {
        File source = writeSourceSheet("Orders.csv");
        EnvTestData env = project.getTestData();

        Map<String, TestDataModel> imported = env.importTestData(
            source,
            Arrays.asList("Default", "DoesNotExist")
        );

        assertThat(imported.keySet()).containsExactly("Default");
    }

    @Test
    public void importsIntoSharedTestData() throws Exception {
        File source = writeSourceSheet("SharedOrders.csv");
        EnvTestData shared = project.getSharedTestData();

        Map<String, TestDataModel> imported = shared.importTestData(
            source,
            Arrays.asList("Default")
        );

        assertThat(imported.keySet()).containsExactly("Default");
        assertThat(new File(shared.getTestDataFor("Default").getLocation(), "SharedOrders.csv"))
            .exists();
        assertThat(shared.getTestDataFor("Default").getByNameIgnoreCase("SharedOrders"))
            .isNotNull();
    }
}

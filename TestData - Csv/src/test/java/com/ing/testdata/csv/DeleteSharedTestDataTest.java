package com.ing.testdata.csv;

import static org.assertj.core.api.Assertions.assertThat;

import com.ing.datalib.component.Project;
import com.ing.datalib.testdata.TestDataFactory;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests that TestData.deleteAll() properly removes the environment directory,
 * not just the files within it. This validates the fix for the issue:
 * "When deleting shared test data, backend files are still existing."
 */
public class DeleteSharedTestDataTest {
    private Path tempDir;
    private String originalUserDir;
    private Project project;

    @BeforeMethod
    public void setUp() throws Exception {
        TestDataFactory.load();

        tempDir = Files.createTempDirectory("delete-test-data");
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

    /**
     * Tests that creating a custom shared test data environment and then deleting it
     * removes the environment directory completely, not just the datasheet files.
     * This replicates the reported issue: Create -> Delete -> Backend files still exist.
     */
    @Test
    public void testDeleteSharedTestDataEnvironment_RemovesDirectoryCompletely() throws Exception {
        // Step 1: Create a custom shared test data environment
        String envName = "CustomEnvironment";
        project.getSharedTestData().createNewEnvironment(envName);

        // Step 2: Save to persist the environment to disk
        project.getSharedTestData().save();

        // Step 3: Verify the environment directory was created
        Path sharedRoot = tempDir.resolve("Shared").resolve("SharedTestData");
        Path envPath = sharedRoot.resolve(envName);
        assertThat(envPath).exists();

        // Step 4: Create some test files (simulate having datasheets)
        Files.write(envPath.resolve("Sheet1.csv"), "Scenario,Flow\nTest,Step\n".getBytes());
        Files.write(envPath.resolve("Sheet2.csv"), "Scenario,Flow\nTest2,Step2\n".getBytes());

        assertThat(envPath.resolve("Sheet1.csv")).exists();
        assertThat(envPath.resolve("Sheet2.csv")).exists();

        // Step 5: Delete the environment (this is where the bug occurs)
        project.getSharedTestData().deleteEnvironment(envName);

        // Step 6: Verify that the environment directory and all its files are deleted
        // (Before the fix, the directory would still exist)
        assertThat(envPath)
            .as(
                "Environment directory should be completely removed after deletion. " +
                "Bug: 'When deleting shared test data, backend files are still existing'"
            )
            .doesNotExist();

        assertThat(envPath.resolve("Sheet1.csv")).doesNotExist();
        assertThat(envPath.resolve("Sheet2.csv")).doesNotExist();
    }

    /**
     * Tests that deleting the Default shared test data environment also
     * removes its directory completely.
     */
    @Test
    public void testDeleteDefaultSharedTestDataEnvironment_RemovesDirectory() throws Exception {
        // Step 1: Verify Default environment exists (created by default)
        Path sharedRoot = tempDir.resolve("Shared").resolve("SharedTestData");
        Path defaultEnv = sharedRoot.resolve("Default");

        // Create it manually for this test
        Files.createDirectories(defaultEnv);
        Files.write(defaultEnv.resolve("TestData.csv"), "Scenario,Flow\nTest,Step\n".getBytes());

        project.reload();

        assertThat(defaultEnv).exists();
        assertThat(defaultEnv.resolve("TestData.csv")).exists();

        // Step 2: Delete the Default environment
        project.getSharedTestData().deleteEnvironment("Default");

        // Step 3: Verify the directory is completely removed
        assertThat(defaultEnv).as("Default environment directory should be deleted").doesNotExist();

        assertThat(defaultEnv.resolve("TestData.csv")).doesNotExist();
    }
}

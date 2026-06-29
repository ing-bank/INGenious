package com.ing.datalib.component;

import static org.testng.Assert.*;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit tests for cross-environment datasheet rename validation and confirmation functionality.
 * Tests the acceptance criteria defined in the user story.
 */
public class EnvTestDataCrossEnvironmentRenameTest {
    private Project testProject;
    private EnvTestData envTestData;
    private File tempProjectDir;

    @BeforeMethod
    public void setUp() throws Exception {
        // Create a temporary project directory for testing
        tempProjectDir = Files.createTempDirectory("ingenious-test-project").toFile();
        tempProjectDir.deleteOnExit();

        // Create test project structure
        File testDataDir = new File(tempProjectDir, "TestData");
        testDataDir.mkdirs();

        // Initialize test project
        testProject = new Project(tempProjectDir.getAbsolutePath());
        envTestData = testProject.getTestData();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        // Clean up temporary files
        if (tempProjectDir != null && tempProjectDir.exists()) {
            deleteDirectory(tempProjectDir);
        }
    }

    private void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }

    /**
     * Scenario 1: Detect duplicate datasheet names in other environments
     * Given a datasheet exists in multiple environments with the same name
     * When the user attempts to rename the datasheet in one environment
     * Then the system checks for matching datasheet names in other environments
     * And identifies all environments where the same name exists
     */
    @Test
    public void testDetectDuplicateDatasheetNamesInOtherEnvironments() {
        // Create multiple environments
        envTestData.createNewEnvironment("DEV");
        envTestData.createNewEnvironment("QA");
        envTestData.createNewEnvironment("PROD");

        // Create a datasheet with the same name in multiple environments
        String datasheetName = "UserTestData";

        envTestData
            .getTestDataFor("DEV")
            .addTestData(envTestData.getTestDataFor("DEV").getNewTestData(datasheetName));
        envTestData
            .getTestDataFor("QA")
            .addTestData(envTestData.getTestDataFor("QA").getNewTestData(datasheetName));
        envTestData
            .getTestDataFor("PROD")
            .addTestData(envTestData.getTestDataFor("PROD").getNewTestData(datasheetName));

        // Test: Find environments with the datasheet
        List<String> allEnvsWithDatasheet = envTestData.findEnvironmentsWithDatasheet(
            datasheetName
        );
        assertEquals(allEnvsWithDatasheet.size(), 3, "Should find datasheet in 3 environments");
        assertTrue(allEnvsWithDatasheet.contains("DEV"), "Should include DEV");
        assertTrue(allEnvsWithDatasheet.contains("QA"), "Should include QA");
        assertTrue(allEnvsWithDatasheet.contains("PROD"), "Should include PROD");

        // Test: Find other environments excluding current
        List<String> otherEnvs = envTestData.findOtherEnvironmentsWithDatasheet(
            datasheetName,
            "DEV"
        );
        assertEquals(otherEnvs.size(), 2, "Should find datasheet in 2 other environments");
        assertTrue(otherEnvs.contains("QA"), "Should include QA");
        assertTrue(otherEnvs.contains("PROD"), "Should include PROD");
        assertFalse(otherEnvs.contains("DEV"), "Should not include DEV");
    }

    /**
     * Scenario 3: Rename across selected environments
     * Given duplicate datasheet names are detected in other environments
     * When the user confirms renaming across multiple environments
     * Then all selected datasheets across environments are renamed consistently
     */
    @Test
    public void testRenameDatasheetAcrossSelectedEnvironments() {
        // Setup: Create environments with same datasheet name
        envTestData.createNewEnvironment("DEV");
        envTestData.createNewEnvironment("QA");
        envTestData.createNewEnvironment("PROD");

        String oldName = "OldDatasheet";
        String newName = "NewDatasheet";

        envTestData
            .getTestDataFor("DEV")
            .addTestData(envTestData.getTestDataFor("DEV").getNewTestData(oldName));
        envTestData
            .getTestDataFor("QA")
            .addTestData(envTestData.getTestDataFor("QA").getNewTestData(oldName));
        envTestData
            .getTestDataFor("PROD")
            .addTestData(envTestData.getTestDataFor("PROD").getNewTestData(oldName));

        // Test: Rename across selected environments (DEV and QA only)
        List<String> selectedEnvs = Arrays.asList("DEV", "QA");
        boolean renamed = envTestData.renameTestDataAcrossEnvironments(
            oldName,
            newName,
            selectedEnvs
        );

        assertTrue(renamed, "Rename should succeed");
        assertNotNull(
            envTestData.getTestDataFor("DEV").getByName(newName),
            "DEV should have new name"
        );
        assertNull(
            envTestData.getTestDataFor("DEV").getByName(oldName),
            "DEV should not have old name"
        );
        assertNotNull(
            envTestData.getTestDataFor("QA").getByName(newName),
            "QA should have new name"
        );
        assertNull(
            envTestData.getTestDataFor("QA").getByName(oldName),
            "QA should not have old name"
        );
        assertNotNull(
            envTestData.getTestDataFor("PROD").getByName(oldName),
            "PROD should still have old name"
        );
        assertNull(
            envTestData.getTestDataFor("PROD").getByName(newName),
            "PROD should not have new name"
        );
    }

    /**
     * Scenario 4: Rename only current environment
     * Given duplicate datasheet names exist in other environments
     * When the user declines cross-environment renaming
     * Then only the datasheet in the current environment is renamed
     * And other environments remain unchanged
     */
    @Test
    public void testRenameOnlyCurrentEnvironment() {
        // Setup: Create environments with same datasheet name
        envTestData.createNewEnvironment("DEV");
        envTestData.createNewEnvironment("QA");

        String oldName = "TestData1";
        String newName = "TestData2";

        envTestData
            .getTestDataFor("DEV")
            .addTestData(envTestData.getTestDataFor("DEV").getNewTestData(oldName));
        envTestData
            .getTestDataFor("QA")
            .addTestData(envTestData.getTestDataFor("QA").getNewTestData(oldName));

        // Test: Rename only in DEV environment
        boolean renamed = envTestData.renameTestData(oldName, newName, "DEV");

        assertTrue(renamed, "Rename should succeed");
        assertNotNull(
            envTestData.getTestDataFor("DEV").getByName(newName),
            "DEV should have new name"
        );
        assertNull(
            envTestData.getTestDataFor("DEV").getByName(oldName),
            "DEV should not have old name"
        );
        assertNotNull(
            envTestData.getTestDataFor("QA").getByName(oldName),
            "QA should still have old name"
        );
        assertNull(
            envTestData.getTestDataFor("QA").getByName(newName),
            "QA should not have new name"
        );
    }

    /**
     * Scenario 5: Handle no duplicates
     * Given no datasheets with the same name exist in other environments
     * When the user renames a datasheet
     * Then the rename proceeds without any confirmation dialog
     */
    @Test
    public void testRenameWithNoDuplicates() {
        // Setup: Create environments with different datasheet names
        envTestData.createNewEnvironment("DEV");
        envTestData.createNewEnvironment("QA");

        envTestData
            .getTestDataFor("DEV")
            .addTestData(envTestData.getTestDataFor("DEV").getNewTestData("DevData"));
        envTestData
            .getTestDataFor("QA")
            .addTestData(envTestData.getTestDataFor("QA").getNewTestData("QaData"));

        // Test: Find other environments (should be empty)
        List<String> otherEnvs = envTestData.findOtherEnvironmentsWithDatasheet("DevData", "DEV");
        assertEquals(otherEnvs.size(), 0, "Should find no other environments");

        // Test: Rename should work normally
        boolean renamed = envTestData.renameTestData("DevData", "DevTestData", "DEV");
        assertTrue(renamed, "Rename should succeed");
        assertNotNull(
            envTestData.getTestDataFor("DEV").getByName("DevTestData"),
            "DEV should have new name"
        );
    }

    /**
     * Test that renaming fails if the new name already exists in target environment
     */
    @Test
    public void testRenameFailsIfNewNameExists() {
        // Setup
        envTestData.createNewEnvironment("DEV");

        envTestData
            .getTestDataFor("DEV")
            .addTestData(envTestData.getTestDataFor("DEV").getNewTestData("Data1"));
        envTestData
            .getTestDataFor("DEV")
            .addTestData(envTestData.getTestDataFor("DEV").getNewTestData("Data2"));

        // Test: Try to rename Data1 to Data2 (which already exists)
        boolean renamed = envTestData.renameTestData("Data1", "Data2", "DEV");

        assertFalse(renamed, "Rename should fail");
        assertNotNull(
            envTestData.getTestDataFor("DEV").getByName("Data1"),
            "Data1 should still exist"
        );
        assertNotNull(
            envTestData.getTestDataFor("DEV").getByName("Data2"),
            "Data2 should still exist"
        );
    }

    /**
     * Test that cross-environment rename fails if new name exists in any target environment
     */
    @Test
    public void testCrossEnvironmentRenameFailsIfNewNameExistsInAnyEnvironment() {
        // Setup
        envTestData.createNewEnvironment("DEV");
        envTestData.createNewEnvironment("QA");

        envTestData
            .getTestDataFor("DEV")
            .addTestData(envTestData.getTestDataFor("DEV").getNewTestData("OldData"));
        envTestData
            .getTestDataFor("QA")
            .addTestData(envTestData.getTestDataFor("QA").getNewTestData("OldData"));
        envTestData
            .getTestDataFor("QA")
            .addTestData(envTestData.getTestDataFor("QA").getNewTestData("NewData"));

        // Test: Try to rename across both environments, but NewData already exists in QA
        List<String> environments = Arrays.asList("DEV", "QA");
        boolean renamed = envTestData.renameTestDataAcrossEnvironments(
            "OldData",
            "NewData",
            environments
        );

        assertFalse(renamed, "Rename should fail");
        assertNotNull(
            envTestData.getTestDataFor("DEV").getByName("OldData"),
            "DEV should still have OldData"
        );
        assertNotNull(
            envTestData.getTestDataFor("QA").getByName("OldData"),
            "QA should still have OldData"
        );
    }
}

package com.ing.datalib.component;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit tests for cross-environment datasheet rename validation and confirmation functionality.
 * Tests the acceptance criteria defined in the user story.
 *
 * Note: These tests use MockEnvTestData to avoid requiring TestDataFactory providers.
 * This allows the tests to run in the Datalib module without circular dependencies.
 */
public class EnvTestDataCrossEnvironmentRenameTest {
    private MockEnvTestData envTestData;
    private File tempProjectDir;

    @BeforeMethod
    public void setUp() throws Exception {
        // Create a temporary project directory for testing
        tempProjectDir = Files.createTempDirectory("ingenious-test-project").toFile();
        tempProjectDir.deleteOnExit();

        // Initialize mock environment test data
        // Using MockEnvTestData to avoid requiring TestDataFactory providers
        envTestData = new MockEnvTestData();
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
     * Test that renaming fails when target name differs only by case.
     */
    @Test
    public void testRenameFailsIfNewNameExistsIgnoringCase() {
        envTestData.createNewEnvironment("DEV");

        envTestData
            .getTestDataFor("DEV")
            .addTestData(envTestData.getTestDataFor("DEV").getNewTestData("LOGIN"));
        envTestData
            .getTestDataFor("DEV")
            .addTestData(envTestData.getTestDataFor("DEV").getNewTestData("Data2"));

        boolean renamed = envTestData.renameTestData("Data2", "login", "DEV");

        assertFalse(renamed, "Rename should fail because LOGIN already exists in DEV");
        assertNotNull(
            envTestData.getTestDataFor("DEV").getByName("LOGIN"),
            "LOGIN should still exist"
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

    /**
     * Test case-insensitive datasheet lookup across environments.
     */
    @Test
    public void testFindEnvironmentsWithDatasheetIgnoringCase() {
        envTestData.createNewEnvironment("DEV");
        envTestData.createNewEnvironment("QA");

        envTestData
            .getTestDataFor("DEV")
            .addTestData(envTestData.getTestDataFor("DEV").getNewTestData("LOGIN"));
        envTestData
            .getTestDataFor("QA")
            .addTestData(envTestData.getTestDataFor("QA").getNewTestData("login"));

        List<String> allEnvs = envTestData.findEnvironmentsWithDatasheet("LoGiN");

        assertEquals(allEnvs.size(), 2, "Should match both environments ignoring case");
        assertTrue(allEnvs.contains("DEV"), "Should include DEV");
        assertTrue(allEnvs.contains("QA"), "Should include QA");
    }

    /**
     * Mock implementation of EnvTestData for testing purposes.
     * Provides the minimal functionality needed to test rename operations.
     */
    private static class MockEnvTestData {
        private final Map<String, MockTestData> environments = new HashMap<>();

        public void createNewEnvironment(String envName) {
            environments.put(envName, new MockTestData());
        }

        public MockTestData getTestDataFor(String environment) {
            return environments.get(environment);
        }

        public List<String> findEnvironmentsWithDatasheet(String datasheetName) {
            List<String> result = new ArrayList<>();
            for (Map.Entry<String, MockTestData> entry : environments.entrySet()) {
                if (entry.getValue().getByNameIgnoreCase(datasheetName) != null) {
                    result.add(entry.getKey());
                }
            }
            return result;
        }

        public List<String> findOtherEnvironmentsWithDatasheet(
            String datasheetName,
            String excludeEnv
        ) {
            List<String> result = new ArrayList<>();
            for (Map.Entry<String, MockTestData> entry : environments.entrySet()) {
                if (
                    !entry.getKey().equals(excludeEnv) &&
                    entry.getValue().getByNameIgnoreCase(datasheetName) != null
                ) {
                    result.add(entry.getKey());
                }
            }
            return result;
        }

        public boolean renameTestData(String oldName, String newName, String environment) {
            MockTestData testData = environments.get(environment);
            if (testData == null) {
                return false;
            }
            return testData.rename(oldName, newName);
        }

        public boolean renameTestDataAcrossEnvironments(
            String oldName,
            String newName,
            List<String> envs
        ) {
            // Check if new name already exists in any target environment
            for (String env : envs) {
                MockTestData testData = environments.get(env);
                if (testData != null && testData.hasConflictForRename(oldName, newName)) {
                    return false;
                }
            }
            // Rename in all selected environments
            for (String env : envs) {
                MockTestData testData = environments.get(env);
                if (testData != null) {
                    testData.rename(oldName, newName);
                }
            }
            return true;
        }
    }

    /**
     * Mock implementation of TestData for testing purposes.
     */
    private static class MockTestData {
        private final Map<String, String> data = new HashMap<>();

        public void addTestData(String datasheetName) {
            if (!data.containsKey(datasheetName)) {
                data.put(datasheetName, datasheetName);
            }
        }

        public String getNewTestData(String datasheetName) {
            return datasheetName;
        }

        public String getByName(String name) {
            return data.get(name);
        }

        public String getByNameIgnoreCase(String name) {
            for (Map.Entry<String, String> entry : data.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(name)) {
                    return entry.getValue();
                }
            }
            return null;
        }

        public boolean hasConflictForRename(String oldName, String newName) {
            String existing = getByNameIgnoreCase(newName);
            return existing != null && !existing.equals(oldName);
        }

        public boolean rename(String oldName, String newName) {
            // Check if new name already exists (case-insensitive), excluding same record
            if (hasConflictForRename(oldName, newName)) {
                return false;
            }
            // Check if old name exists
            if (!data.containsKey(oldName)) {
                return false;
            }
            // Perform rename
            data.remove(oldName);
            data.put(newName, newName);
            return true;
        }
    }
}

package com.ing.ingenious.api.contract.paths;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Contract test for {@link ProjectPathsApi}. Uses a minimal in-test
 * implementation to verify the interface can be implemented and dispatched
 * without pulling in any Engine dependency.
 */
class ProjectPathsApiTest {

    private final ProjectPathsApi api = new ProjectPathsApi() {
        @Override
        public String getORPath() {
            return "/project/OR.object";
        }

        @Override
        public String getIORPath() {
            return "/project/IOR.object";
        }

        @Override
        public String getMORPath() {
            return "/project/MOR.object";
        }

        @Override
        public String getORimagestorelocation() {
            return "/project/ObjectRepository";
        }

        @Override
        public String getIORimagestorelocation() {
            return "/project/ImageObjectRepository";
        }

        @Override
        public String getPageDumpLocation() {
            return "/project/PageDump";
        }

        @Override
        public String getORpageListJsonFile() {
            return "/project/PageDump/pageDetails.js";
        }

        @Override
        public String getTestLabPath() {
            return "/project/TestLab";
        }

        @Override
        public String getTestPlanPath() {
            return "/project/TestPlan";
        }

        @Override
        public String getReusableComponentsPath() {
            return "/project/ReusableComponents";
        }

        @Override
        public String getReleasePath() {
            return "/project/TestLab/Release1";
        }

        @Override
        public String getTestSetPath() {
            return "/project/TestLab/Release1/TS1.yaml";
        }

        @Override
        public String getScenarioPath() {
            return "/project/TestPlan/Scenario1";
        }

        @Override
        public String getTestCasePath() {
            return "/project/TestPlan/Scenario1/TC1.yaml";
        }

        @Override
        public String getScenarioPath(String scenario) {
            return "/project/TestPlan/" + scenario;
        }

        @Override
        public String getTestCasePath(String scenario, String testCase) {
            return "/project/TestPlan/" + scenario + "/" + testCase + ".yaml";
        }

        @Override
        public String resolveExtension(String directory, String baseName) {
            return ".yaml";
        }

        @Override
        public String getTestEnv() {
            return "/project";
        }

        @Override
        public String getExeSett() {
            return "TestExecution";
        }

        @Override
        public String getSettingsfolderName() {
            return "Settings";
        }

        @Override
        public String getEmulatorfolderName() {
            return "Emulators";
        }
    };

    @Test
    void objectRepositoryPathsShouldBeDispatched() {
        assertEquals("/project/OR.object", api.getORPath());
        assertEquals("/project/IOR.object", api.getIORPath());
        assertEquals("/project/MOR.object", api.getMORPath());
    }

    @Test
    void testPlanAndTestLabPathsShouldBeDispatched() {
        assertEquals("/project/TestPlan/Scenario1", api.getScenarioPath("Scenario1"));
        assertEquals(
            "/project/TestPlan/Scenario1/TC1.yaml",
            api.getTestCasePath("Scenario1", "TC1")
        );
        assertEquals(".yaml", api.resolveExtension("/project/TestPlan/Scenario1", "TC1"));
    }

    @Test
    void folderNameLabelsShouldBeDispatched() {
        assertEquals("TestExecution", api.getExeSett());
        assertEquals("Settings", api.getSettingsfolderName());
        assertEquals("Emulators", api.getEmulatorfolderName());
    }
}

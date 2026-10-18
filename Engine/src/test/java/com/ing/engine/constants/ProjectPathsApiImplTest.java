package com.ing.engine.constants;

import static org.assertj.core.api.Assertions.assertThat;

import com.ing.engine.core.RunManager;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests for {@link ProjectPathsApiImpl} — verifies each method delegates to
 * the corresponding {@link FilePath} static.
 */
public class ProjectPathsApiImplTest {
    private final ProjectPathsApiImpl api = ProjectPathsApiImpl.INSTANCE;

    @BeforeClass
    public void initProjectPath() {
        // FilePath's project-relative methods (and getTestEnv() in
        // particular) read RunManager.getGlobalSettings().getProjectPath(),
        // which is null until a project is loaded. Setting it here avoids
        // a pre-existing NullPointerException in FilePath.getTestEnv()
        // (calls env.isEmpty() without a null check) without touching
        // FilePath itself.
        RunManager.getGlobalSettings().setProjectPath("/tmp/ingenious-test-project");
    }

    @Test
    public void instanceShouldBeNonNullAndStable() {
        assertThat(ProjectPathsApiImpl.INSTANCE).isNotNull();
        assertThat(ProjectPathsApiImpl.INSTANCE).isSameAs(ProjectPathsApiImpl.INSTANCE);
    }

    @Test
    public void objectRepositoryPathsShouldDelegateToFilePath() {
        assertThat(api.getORPath()).isEqualTo(FilePath.getORPath());
        assertThat(api.getIORPath()).isEqualTo(FilePath.getIORPath());
        assertThat(api.getMORPath()).isEqualTo(FilePath.getMORPath());
        assertThat(api.getORimagestorelocation()).isEqualTo(FilePath.getORimagestorelocation());
        assertThat(api.getIORimagestorelocation()).isEqualTo(FilePath.getIORimagestorelocation());
    }

    @Test
    public void pageDumpPathsShouldDelegateToFilePath() {
        assertThat(api.getPageDumpLocation()).isEqualTo(FilePath.getPageDumpLocation());
        assertThat(api.getORpageListJsonFile()).isEqualTo(FilePath.getORpageListJsonFile());
    }

    @Test
    public void testPlanAndTestLabPathsShouldDelegateToFilePath() {
        assertThat(api.getTestLabPath()).isEqualTo(FilePath.getTestLabPath());
        assertThat(api.getTestPlanPath()).isEqualTo(FilePath.getTestPlanPath());
        assertThat(api.getReusableComponentsPath()).isEqualTo(FilePath.getReusableComponentsPath());
        assertThat(api.getReleasePath()).isEqualTo(FilePath.getReleasePath());
        assertThat(api.getTestSetPath()).isEqualTo(FilePath.getTestSetPath());
        assertThat(api.getScenarioPath()).isEqualTo(FilePath.getScenarioPath());
        assertThat(api.getTestCasePath()).isEqualTo(FilePath.getTestCasePath());
    }

    @Test
    public void overloadedScenarioAndTestCasePathsShouldDelegateToFilePath() {
        assertThat(api.getScenarioPath("Scenario1"))
            .isEqualTo(FilePath.getScenarioPath("Scenario1"));
        assertThat(api.getTestCasePath("Scenario1", "TC1"))
            .isEqualTo(FilePath.getTestCasePath("Scenario1", "TC1"));
    }

    @Test
    public void resolveExtensionShouldDelegateToFilePath() {
        assertThat(api.resolveExtension("/some/dir", "TC1"))
            .isEqualTo(FilePath.resolveExtension("/some/dir", "TC1"));
    }

    @Test
    public void testEnvShouldDelegateToFilePath() {
        assertThat(api.getTestEnv()).isEqualTo(FilePath.getTestEnv());
    }

    @Test
    public void folderNameLabelsShouldDelegateToFilePath() {
        assertThat(api.getExeSett()).isEqualTo(FilePath.getExeSett());
        assertThat(api.getSettingsfolderName()).isEqualTo(FilePath.getSettingsfolderName());
        assertThat(api.getEmulatorfolderName()).isEqualTo(FilePath.getEmulatorfolderName());
    }
}

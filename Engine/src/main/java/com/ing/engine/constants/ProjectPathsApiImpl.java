package com.ing.engine.constants;

import com.ing.ingenious.api.contract.paths.ProjectPathsApi;

/**
 * Delegates the {@link ProjectPathsApi} plugin contract to the existing
 * {@link FilePath} static methods.
 */
public final class ProjectPathsApiImpl implements ProjectPathsApi {
    public static final ProjectPathsApiImpl INSTANCE = new ProjectPathsApiImpl();

    private ProjectPathsApiImpl() {}

    @Override
    public String getORPath() {
        return FilePath.getORPath();
    }

    @Override
    public String getIORPath() {
        return FilePath.getIORPath();
    }

    @Override
    public String getMORPath() {
        return FilePath.getMORPath();
    }

    @Override
    public String getORimagestorelocation() {
        return FilePath.getORimagestorelocation();
    }

    @Override
    public String getIORimagestorelocation() {
        return FilePath.getIORimagestorelocation();
    }

    @Override
    public String getPageDumpLocation() {
        return FilePath.getPageDumpLocation();
    }

    @Override
    public String getORpageListJsonFile() {
        return FilePath.getORpageListJsonFile();
    }

    @Override
    public String getTestLabPath() {
        return FilePath.getTestLabPath();
    }

    @Override
    public String getTestPlanPath() {
        return FilePath.getTestPlanPath();
    }

    @Override
    public String getReusableComponentsPath() {
        return FilePath.getReusableComponentsPath();
    }

    @Override
    public String getReleasePath() {
        return FilePath.getReleasePath();
    }

    @Override
    public String getTestSetPath() {
        return FilePath.getTestSetPath();
    }

    @Override
    public String getScenarioPath() {
        return FilePath.getScenarioPath();
    }

    @Override
    public String getTestCasePath() {
        return FilePath.getTestCasePath();
    }

    @Override
    public String getScenarioPath(String scenario) {
        return FilePath.getScenarioPath(scenario);
    }

    @Override
    public String getTestCasePath(String scenario, String testCase) {
        return FilePath.getTestCasePath(scenario, testCase);
    }

    @Override
    public String resolveExtension(String directory, String baseName) {
        return FilePath.resolveExtension(directory, baseName);
    }

    @Override
    public String getTestEnv() {
        return FilePath.getTestEnv();
    }

    @Override
    public String getExeSett() {
        return FilePath.getExeSett();
    }

    @Override
    public String getSettingsfolderName() {
        return FilePath.getSettingsfolderName();
    }

    @Override
    public String getEmulatorfolderName() {
        return FilePath.getEmulatorfolderName();
    }
}

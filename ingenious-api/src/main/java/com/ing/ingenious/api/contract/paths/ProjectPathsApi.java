package com.ing.ingenious.api.contract.paths;

/**
 * Contract for paths within the currently loaded project: object repositories,
 * TestPlan/TestLab/ReusableComponents trees, and the Release/TestSet/Scenario/TestCase
 * structure underneath them.
 * <p>
 * All methods resolve relative to the project path of the currently loaded
 * project; calling them before a project/run context is active may produce
 * paths built from empty or default values.
 * </p>
 */
public interface ProjectPathsApi {

    /** @return the path to the Object Repository file (OR.object) */
    String getORPath();

    /** @return the path to the Image Object Repository file (IOR.object) */
    String getIORPath();

    /** @return the path to the Mobile Object Repository file (MOR.object) */
    String getMORPath();

    /** @return the path to the folder storing Object Repository element images */
    String getORimagestorelocation();

    /** @return the path to the folder storing Image Object Repository images */
    String getIORimagestorelocation();

    /** @return the path to the project's PageDump data folder */
    String getPageDumpLocation();

    /** @return the path to the PageDump page listing JSON file */
    String getORpageListJsonFile();

    /** @return the path to the project's TestLab (execution) folder */
    String getTestLabPath();

    /** @return the path to the project's TestPlan (design) folder */
    String getTestPlanPath();

    /** @return the path to the project's ReusableComponents folder */
    String getReusableComponentsPath();

    /** @return the path to the current release folder under TestLab */
    String getReleasePath();

    /** @return the on-disk path of the current test set (YAML/YML if present, else legacy CSV) */
    String getTestSetPath();

    /** @return the path to the current scenario folder under TestPlan */
    String getScenarioPath();

    /** @return the on-disk path of the current test case (YAML/YML if present, else legacy CSV) */
    String getTestCasePath();

    /**
     * @param scenario the scenario name
     * @return the path to the given scenario folder under TestPlan
     */
    String getScenarioPath(String scenario);

    /**
     * @param scenario the scenario name
     * @param testCase the test case name
     * @return the on-disk path of the given test case (YAML/YML if present, else legacy CSV)
     */
    String getTestCasePath(String scenario, String testCase);

    /**
     * Resolves the file extension that should be used for a test case/test set base name,
     * preferring an existing YAML file ({@code .yaml}/{@code .yml}) over the legacy CSV format.
     * @param directory the directory containing the file
     * @param baseName the base file name, without extension
     * @return the resolved extension, including the leading dot
     */
    String resolveExtension(String directory, String baseName);

    /** @return the current project's path, or an empty string if not set */
    String getTestEnv();

    /** @return the folder name used for the Settings/TestExecution sub-folder */
    String getExeSett();

    /** @return the folder name used for project settings ({@code "Settings"}) */
    String getSettingsfolderName();

    /** @return the folder name used for emulator definitions ({@code "Emulators"}) */
    String getEmulatorfolderName();
}

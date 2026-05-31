
package com.ing.engine.constants;

import com.ing.engine.core.RunManager;
import java.io.File;

public class FilePath extends AppResourcePath {

    private final static String OR = "OR.object";
    private final static String IOR = "IOR.object";
    private final static String MOR = "MOR.object";
    /**
     * Legacy fallback extension. Use {@link #resolveExtension(String)} or the
     * {@code getTestCasePath} / {@code getTestSetPath} overloads to honour
     * YAML files written by the new test case format.
     */
    private final static String FORMAT = ".csv";
    private final static String YAML_FORMAT = ".yaml";
    private final static String YML_FORMAT = ".yml";
    private final static String DESIGN = "TestPlan";
    private final static String REUSABLE = "ReusableComponents";
    private final static String EXECUTION = "TestLab";
    private final static String IOR_DATA = "ImageObjectRepository";
    private final static String OR_DATA = "ObjectRepository";
    private final static String PAGEDUMP = "PageDump";

    private final static String PROJ_SETT = "Settings";
    private final static String SETT_EXECUTION = "TestExecution";

    public static String getExeSett() {
        return SETT_EXECUTION;
    }
    private final static String EMULATORS_LOC = "Emulators";

    public static String getSettingsfolderName() {
        return PROJ_SETT;
    }

    public static String getEmulatorfolderName() {
        return EMULATORS_LOC;
    }

    public static String getORPath() {
        return RunManager.getGlobalSettings().getProjectPath() + File.separatorChar + OR;
    }

    public static String getIORPath() {
        return RunManager.getGlobalSettings().getProjectPath() + File.separatorChar + IOR;
    }

    public static String getMORPath() {
        return RunManager.getGlobalSettings().getProjectPath() + File.separatorChar + MOR;
    }

    public static String getIORimagestorelocation() {
        return RunManager.getGlobalSettings().getProjectPath() + File.separatorChar + IOR_DATA;
    }

    public static String getORimagestorelocation() {
        return RunManager.getGlobalSettings().getProjectPath() + File.separatorChar + OR_DATA;
    }

    public static String getPageDumpLocation() {
        return RunManager.getGlobalSettings().getProjectPath() + File.separatorChar + PAGEDUMP;
    }

    public static String getORpageListJsonFile() {
        return getPageDumpLocation()
                + File.separatorChar + "pageDetails.js";
    }

    public static String getTestLabPath() {
        return RunManager.getGlobalSettings().getProjectPath() + File.separatorChar + EXECUTION;
    }

    public static String getTestPlanPath() {
        return RunManager.getGlobalSettings().getProjectPath() + File.separatorChar + DESIGN;
    }

    public static String getReusableComponentsPath() {
        return RunManager.getGlobalSettings().getProjectPath() + File.separatorChar + REUSABLE;
    }

    public static String getReleasePath() {
        return getTestLabPath() + File.separatorChar + RunManager.getGlobalSettings().getRelease();
    }

    public static String getTestSetPath() {
        return resolveFile(getReleasePath(), RunManager.getGlobalSettings().getTestSet());
    }

    public static String getScenarioPath() {
        return getTestPlanPath() + File.separatorChar
                + RunManager.getGlobalSettings().getScenario();
    }

    public static String getTestCasePath() {
        return resolveFile(getScenarioPath(), RunManager.getGlobalSettings().getTestCase());
    }

    public static String getScenarioPath(String scenario) {
        return getTestPlanPath() + File.separatorChar + scenario;
    }

    public static String getTestCasePath(String scenario, String testCase) {
        return resolveFile(getScenarioPath(scenario), testCase);
    }

    /**
     * Resolves the on-disk path for a test case / test set base name,
     * preferring an existing YAML file ({@code .yaml} / {@code .yml}) over
     * CSV. When no file exists yet, falls back to the legacy CSV extension to
     * preserve backward compatibility with code paths that create files on
     * demand.
     */
    private static String resolveFile(String directory, String baseName) {
        File dir = new File(directory);
        File yaml = new File(dir, baseName + YAML_FORMAT);
        if (yaml.isFile()) {
            return yaml.getPath();
        }
        File yml = new File(dir, baseName + YML_FORMAT);
        if (yml.isFile()) {
            return yml.getPath();
        }
        return directory + File.separatorChar + baseName + FORMAT;
    }

    /** Returns the extension that should be used for the given base file (existing wins, else {@code .csv}). */
    public static String resolveExtension(String directory, String baseName) {
        File dir = new File(directory);
        if (new File(dir, baseName + YAML_FORMAT).isFile()) {
            return YAML_FORMAT;
        }
        if (new File(dir, baseName + YML_FORMAT).isFile()) {
            return YML_FORMAT;
        }
        return FORMAT;
    }

    public static String getTestEnv() {
        String env = RunManager.getGlobalSettings().getProjectPath();
        if (env.isEmpty()) {
            return "";
        } else {
            return env;
        }
    }
}

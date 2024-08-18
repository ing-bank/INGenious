
package com.ing.engine.constants;

import com.ing.engine.core.RunManager;
import java.io.File;

public class FilePath extends AppResourcePath {

    private final static String OR = "OR.object";
    private final static String IOR = "IOR.object";
    private final static String MOR = "MOR.object";
    private final static String FORMAT = ".csv";
    private final static String DESIGN = "TestPlan";
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

    public static String getReleasePath() {
        return getTestLabPath() + File.separatorChar + RunManager.getGlobalSettings().getRelease();
    }

    public static String getTestSetPath() {
        return getReleasePath() + File.separatorChar + RunManager.getGlobalSettings().getTestSet() + FORMAT;
    }

    public static String getScenarioPath() {
        return getTestPlanPath() + File.separatorChar
                + RunManager.getGlobalSettings().getScenario();
    }

    public static String getTestCasePath() {
        return getScenarioPath() + File.separatorChar
                + RunManager.getGlobalSettings().getTestCase() + FORMAT;
    }

    public static String getScenarioPath(String scenario) {
        return getTestPlanPath() + File.separatorChar + scenario;
    }

    public static String getTestCasePath(String scenario, String testCase) {
        return getScenarioPath(scenario) + File.separatorChar + testCase
                + FORMAT;
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

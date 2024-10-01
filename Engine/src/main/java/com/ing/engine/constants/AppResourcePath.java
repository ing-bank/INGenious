package com.ing.engine.constants;

import com.ing.engine.core.RunManager;
import com.ing.engine.reporting.util.DateTimeUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 *
 */
public class AppResourcePath {

    private final static String RESULTS_FOLDER = "Results";
    private final static String REPORT_TEMPLATE_FOLDER = "ReportTemplate";
    private final static String CONFIG = "Configuration";
    private final static String GLOBAL_PROPERTIES = "Global Settings.properties";

    private final static String EXTERNAL_COMMANDS_CONFIG = "commands";

    private final static String SUMMARY_HTML = "summary.html";
    private final static String EXTENT_HTML = "extent.html";
    private final static String AZURE_XML = "azure.xml";
    private final static String EXTENT_PDF = "extent.pdf";
    private final static String TC_REPORT_HTML = "testCase.html";
    private final static String DETAILED_HTML = "detailed.html";
    private final static String PERF_HTML = "perfReport.html";
    private final static String REPORT_HISTORY_HTML = "ReportHistory.html";
    private final static String VIDEO_HTML = "videoReport.html";
    private final static String REPORT_DATA = "data.js";
    private final static String REPORT_HISTORY_DATA = "reportHistory.js";

    private final static String EXPLORER_CONFIG = "ExplorerConfig.properties";

    private final static String ENC = ".enc";

    private final static String CHROME_EMULATOR_FILE = "chrome-emulators.json";

    private final static String ADDON_LOCATION = "Extensions";

    private final static String FF_ADDON = "FireFox" + File.separator + "ingenious.xpi";

    private final static String CHROME_ADDON = "Chrome" + File.separator + "ingenious.crx";

    private final static String STEPMAP_FILE = "StepMap.csv";
    private static final String APP_SETTINGS = "app.settings";

    private static String date;
    private static String time;

    public static String getAppRoot() {
        try {
            // return System.getProperty("user.dir");
            return new File(System.getProperty("user.dir")).getCanonicalPath();
        } catch (IOException ex) {
            Logger.getLogger(AppResourcePath.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public static String getExternalCommandsConfig() {
        return getLibPath() + File.separator + EXTERNAL_COMMANDS_CONFIG;
    }

    public static String getPropertiesPath() {
        return getAppRoot() + File.separator + CONFIG + File.separator + GLOBAL_PROPERTIES;
    }

    public static String getConfigurationPath() {
        return getAppRoot() + File.separator + CONFIG;
    }

    public static String getLibPath() {
        return getAppRoot() + File.separator + "lib";
    }

    public static String getExplorerConfig() {
        return getConfigurationPath() + File.separator + EXPLORER_CONFIG;
    }

    public static String getChromeEmulatorsFile() {
        return getConfigurationPath() + File.separator + CHROME_EMULATOR_FILE;
    }

    public static String getReportThemePreviewPath() {
        return getConfigurationPath() + File.separator + REPORT_TEMPLATE_FOLDER + File.separator + "preview";
    }

    public static String getReportThemePath() {
        return getReportResourcePath() + File.separator + "theme";
    }

    public static String getReportResourcePath() {
        return getConfigurationPath() + File.separator + REPORT_TEMPLATE_FOLDER + File.separator + "media";
    }

    public static String getMailReportTemplatePath() {
        return getConfigurationPath() + File.separator + REPORT_TEMPLATE_FOLDER + File.separator + "mailReport";
    }

    public static String getaXeReportTemplatePath() {
        return getConfigurationPath() + File.separator + REPORT_TEMPLATE_FOLDER + File.separator + "aXe";
    }

    public static String getReportTemplatePath() {
        return getConfigurationPath() + File.separator + REPORT_TEMPLATE_FOLDER + File.separator + "html";
    }

    public static String getPageDumpResourcePath() {
        return getConfigurationPath() + File.separator + "PageDump";
    }

    public static String getEncFile() {
        return getConfigurationPath() + File.separator + ENC;
    }

    public static String getSummaryHTMLPath() {
        return getReportTemplatePath() + File.separator + SUMMARY_HTML;
    }

    public static String getDetailedHTMLPath() {
        return getReportTemplatePath() + File.separator + DETAILED_HTML;
    }

    public static String getTCReportTemplate() {
        return getReportTemplatePath() + File.separator + TC_REPORT_HTML;
    }

    public static String getReportHistoryHTMLPath() {
        return getReportTemplatePath() + File.separator + REPORT_HISTORY_HTML;
    }

    public static void initDateTime() {
        date = DateTimeUtils.DateNowForFolder();
        time = DateTimeUtils.TimeNowForFolder();
    }

    public static String getResultsPath() {
        return RunManager.getGlobalSettings().getProjectPath() + File.separator + RESULTS_FOLDER;
    }

    private static String getResultPath() {
        if (RunManager.getGlobalSettings().isTestRun()) {
            return File.separator + "TestDesign" + File.separator + RunManager.getGlobalSettings().getScenario()
                    + File.separator + RunManager.getGlobalSettings().getTestCase();
        }
        return File.separator + "TestExecution" + File.separator + RunManager.getGlobalSettings().getRelease()
                + File.separator + RunManager.getGlobalSettings().getTestSet();
    }

    public static String getCurrentResultsPath() {
        return getCurrentResultsLocation() + File.separator + date + " " + time;
    }

    public static String getCurrentResultsLocation() {
        return getResultsPath() + getResultPath();
    }

    public static String getCurrentTestCaseLogsLocation() {
        return getCurrentResultsLocation() + File.separator + date + " " + time + File.separator + "logs";
    }

    public static String getCurrentTestCaseAccessibilityLocation() {
        return getCurrentResultsLocation() + File.separator + date + " " + time + File.separator + "aXe";
    }

    public static String getCurrentTestCaseVideosLocation() {
        return getCurrentResultsLocation() + File.separator + date + " " + time + File.separator + "videos";
    }

    public static String getLatestResultsLocation() {
        return getCurrentResultsLocation() + File.separator + "Latest";
    }

    public static String getCurrentDetailedHTMLPath() {
        return getCurrentResultsPath() + File.separator + DETAILED_HTML;
    }

    public static String getCurrentExtentReportPath() {
        return getCurrentResultsPath() + File.separator + EXTENT_HTML;
    }

    public static String getCurrentAzureReportPath() {
        return getCurrentResultsPath() + File.separator + AZURE_XML;
    }

    public static String getCurrentExtentPDFReportPath() {
        return getCurrentResultsPath() + File.separator + EXTENT_PDF;
    }

    public static String getCurrentSummaryHTMLPath() {
        return getCurrentResultsPath() + File.separator + SUMMARY_HTML;
    }

    public static String getCurrentSummaryHTMLPathRelative() {
        return getCurrentReportFolderName() + File.separator + SUMMARY_HTML;
    }

    public static String getCurrentReportFolderName() {
        return date + " " + time;
    }

    public static String getCurrentPerfReportHarPath() {
        return getResultsPath() + File.separator + "perf" + File.separator + "har";
    }

    public static String getCurrentReportDataPath() {
        return getCurrentResultsPath() + File.separator + REPORT_DATA;
    }

    public static String getCurrentReportHistoryDataPath() {
        return getCurrentResultsLocation() + File.separator + REPORT_HISTORY_DATA;
    }

    public static String getDate() {
        return date;
    }

    public static String getTime() {
        return time;
    }

    /**
     * Addon Path
     *
     * @return
     */
    public static File getFireFoxAddOnPath() {
        return new File(getAddonPath() + File.separator + FF_ADDON);
    }

    public static File getChromeAddOnPath() {
        return new File(getAddonPath() + File.separator + CHROME_ADDON);
    }

    public static File getSafariAddOnPath() {
        return new File(getAddonPath() + File.separator + FF_ADDON);
    }

    public static String getAddonPath() {
        return getAppRoot() + File.separator + ADDON_LOCATION;
    }

    public static String getEngineJarPath() {
        try {
            return Files.find(
                    Paths.get(getAppRoot(), "lib"),
                    1,
                    (path, bfa) -> path.toFile().getName().matches("ingenious-engine.*.jar")
            ).map(Path::toString).findFirst().orElse("");
        } catch (IOException ex) {
            Logger.getLogger(AppResourcePath.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /**
     *
     * @return location of application configuration file
     */
    public static String getAppSettings() {
        return getConfigurationPath() + File.separator + APP_SETTINGS;
    }

    public static String getStepMapFile() {
        return getConfigurationPath() + File.separator + STEPMAP_FILE;
    }

    public static String getPropertiesPath(String fileName) {
        return getConfigurationPath() + File.separator + fileName;
    }

    public static String getPerfReportHTMLPath() {
        return getReportTemplatePath() + File.separator + PERF_HTML;
    }

    public static String getCurrentPerfReportHTMLPath() {
        return getCurrentResultsPath() + File.separator + PERF_HTML;
    }

    public static String getVideoReportHTMLPath() {
        return getReportTemplatePath() + File.separator + VIDEO_HTML;
    }

    public static String getCurrentVideoReportHTMLPath() {
        return getCurrentResultsPath() + File.separator + VIDEO_HTML;
    }
}

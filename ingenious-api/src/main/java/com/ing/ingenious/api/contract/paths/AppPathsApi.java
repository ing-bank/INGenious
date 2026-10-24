package com.ing.ingenious.api.contract.paths;

import java.io.File;

/**
 * Contract for installation-level paths: the application root, its
 * {@code Configuration}/{@code lib} folders, browser extensions, and the
 * running engine jar. These paths are independent of any loaded project.
 */
public interface AppPathsApi {

    /** @return the canonical path of the application's working directory */
    String getAppRoot();

    /** @return the path to the external commands configuration folder */
    String getExternalCommandsConfig();

    /** @return the path to the Global Settings.properties file */
    String getPropertiesPath();

    /**
     * @param fileName the properties file name
     * @return the path to the given file under the Configuration folder
     */
    String getPropertiesPath(String fileName);

    /** @return the path to the Configuration folder */
    String getConfigurationPath();

    /** @return the path to the lib folder */
    String getLibPath();

    /** @return the path to the ExplorerConfig.properties file */
    String getExplorerConfig();

    /** @return the path to the encryption key file */
    String getEncFile();

    /** @return the path to the app.settings file */
    String getAppSettings();

    /** @return the path to the StepMap.csv file */
    String getStepMapFile();

    /** @return the path to the browser extensions folder */
    String getAddonPath();

    /** @return the Firefox extension (.xpi) file */
    File getFireFoxAddOnPath();

    /** @return the Chrome extension (.crx) file */
    File getChromeAddOnPath();

    /** @return the Safari extension file */
    File getSafariAddOnPath();

    /** @return the path to the running ingenious-engine jar, or an empty string if not found */
    String getEngineJarPath();

    /** @return the path to the PageDump feature's static resources under Configuration */
    String getPageDumpResourcePath();
}

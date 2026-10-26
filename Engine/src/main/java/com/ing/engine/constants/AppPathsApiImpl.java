package com.ing.engine.constants;

import com.ing.ingenious.api.contract.paths.AppPathsApi;
import java.io.File;

/**
 * Delegates the {@link AppPathsApi} plugin contract to the existing
 * {@link AppResourcePath} static methods.
 */
public final class AppPathsApiImpl implements AppPathsApi {
    public static final AppPathsApiImpl INSTANCE = new AppPathsApiImpl();

    private AppPathsApiImpl() {}

    @Override
    public String getAppRoot() {
        return AppResourcePath.getAppRoot();
    }

    @Override
    public String getExternalCommandsConfig() {
        return AppResourcePath.getExternalCommandsConfig();
    }

    @Override
    public String getPropertiesPath() {
        return AppResourcePath.getPropertiesPath();
    }

    @Override
    public String getPropertiesPath(String fileName) {
        return AppResourcePath.getPropertiesPath(fileName);
    }

    @Override
    public String getConfigurationPath() {
        return AppResourcePath.getConfigurationPath();
    }

    @Override
    public String getLibPath() {
        return AppResourcePath.getLibPath();
    }

    @Override
    public String getExplorerConfig() {
        return AppResourcePath.getExplorerConfig();
    }

    @Override
    public String getEncFile() {
        return AppResourcePath.getEncFile();
    }

    @Override
    public String getAppSettings() {
        return AppResourcePath.getAppSettings();
    }

    @Override
    public String getStepMapFile() {
        return AppResourcePath.getStepMapFile();
    }

    @Override
    public String getAddonPath() {
        return AppResourcePath.getAddonPath();
    }

    @Override
    public File getFireFoxAddOnPath() {
        return AppResourcePath.getFireFoxAddOnPath();
    }

    @Override
    public File getChromeAddOnPath() {
        return AppResourcePath.getChromeAddOnPath();
    }

    @Override
    public File getSafariAddOnPath() {
        return AppResourcePath.getSafariAddOnPath();
    }

    @Override
    public String getEngineJarPath() {
        return AppResourcePath.getEngineJarPath();
    }

    @Override
    public String getPageDumpResourcePath() {
        return AppResourcePath.getPageDumpResourcePath();
    }
}

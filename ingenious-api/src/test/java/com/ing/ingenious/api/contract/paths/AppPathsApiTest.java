package com.ing.ingenious.api.contract.paths;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Contract test for {@link AppPathsApi}. Uses a minimal in-test
 * implementation to verify the interface can be implemented and dispatched
 * without pulling in any Engine dependency.
 */
class AppPathsApiTest {

    private final AppPathsApi api = new AppPathsApi() {
        @Override
        public String getAppRoot() {
            return "/app";
        }

        @Override
        public String getExternalCommandsConfig() {
            return "/app/lib/commands";
        }

        @Override
        public String getPropertiesPath() {
            return "/app/Configuration/Global Settings.properties";
        }

        @Override
        public String getPropertiesPath(String fileName) {
            return "/app/Configuration/" + fileName;
        }

        @Override
        public String getConfigurationPath() {
            return "/app/Configuration";
        }

        @Override
        public String getLibPath() {
            return "/app/lib";
        }

        @Override
        public String getExplorerConfig() {
            return "/app/Configuration/ExplorerConfig.properties";
        }

        @Override
        public String getEncFile() {
            return "/app/Configuration/.enc";
        }

        @Override
        public String getAppSettings() {
            return "/app/Configuration/app.settings";
        }

        @Override
        public String getStepMapFile() {
            return "/app/Configuration/StepMap.csv";
        }

        @Override
        public String getAddonPath() {
            return "/app/Extensions";
        }

        @Override
        public File getFireFoxAddOnPath() {
            return new File("/app/Extensions/FireFox/ingenious.xpi");
        }

        @Override
        public File getChromeAddOnPath() {
            return new File("/app/Extensions/Chrome/ingenious.crx");
        }

        @Override
        public File getSafariAddOnPath() {
            return new File("/app/Extensions/FireFox/ingenious.xpi");
        }

        @Override
        public String getEngineJarPath() {
            return "/app/lib/ingenious-engine-3.1.jar";
        }

        @Override
        public String getPageDumpResourcePath() {
            return "/app/Configuration/PageDump";
        }
    };

    @Test
    void installationPathsShouldBeDispatched() {
        assertEquals("/app", api.getAppRoot());
        assertEquals("/app/lib", api.getLibPath());
        assertEquals("/app/Configuration", api.getConfigurationPath());
        assertEquals("/app/Configuration/custom.properties", api.getPropertiesPath("custom.properties"));
    }

    @Test
    void addonPathsShouldReturnFileInstances() {
        assertEquals(new File("/app/Extensions/FireFox/ingenious.xpi"), api.getFireFoxAddOnPath());
        assertEquals(new File("/app/Extensions/Chrome/ingenious.crx"), api.getChromeAddOnPath());
    }
}

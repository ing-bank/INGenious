package com.ing.engine.constants;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

/**
 * Tests for {@link AppPathsApiImpl} — verifies each method delegates to the
 * corresponding {@link AppResourcePath} static.
 */
public class AppPathsApiImplTest {
    private final AppPathsApiImpl api = AppPathsApiImpl.INSTANCE;

    @Test
    public void instanceShouldBeNonNullAndStable() {
        assertThat(AppPathsApiImpl.INSTANCE).isNotNull();
        assertThat(AppPathsApiImpl.INSTANCE).isSameAs(AppPathsApiImpl.INSTANCE);
    }

    @Test
    public void installationPathsShouldDelegateToAppResourcePath() {
        assertThat(api.getAppRoot()).isEqualTo(AppResourcePath.getAppRoot());
        assertThat(api.getExternalCommandsConfig())
            .isEqualTo(AppResourcePath.getExternalCommandsConfig());
        assertThat(api.getPropertiesPath()).isEqualTo(AppResourcePath.getPropertiesPath());
        assertThat(api.getPropertiesPath("custom.properties"))
            .isEqualTo(AppResourcePath.getPropertiesPath("custom.properties"));
        assertThat(api.getConfigurationPath()).isEqualTo(AppResourcePath.getConfigurationPath());
        assertThat(api.getLibPath()).isEqualTo(AppResourcePath.getLibPath());
        assertThat(api.getExplorerConfig()).isEqualTo(AppResourcePath.getExplorerConfig());
    }

    @Test
    public void configFilePathsShouldDelegateToAppResourcePath() {
        assertThat(api.getEncFile()).isEqualTo(AppResourcePath.getEncFile());
        assertThat(api.getAppSettings()).isEqualTo(AppResourcePath.getAppSettings());
        assertThat(api.getStepMapFile()).isEqualTo(AppResourcePath.getStepMapFile());
        assertThat(api.getPageDumpResourcePath())
            .isEqualTo(AppResourcePath.getPageDumpResourcePath());
    }

    @Test
    public void addonPathsShouldDelegateToAppResourcePath() {
        assertThat(api.getAddonPath()).isEqualTo(AppResourcePath.getAddonPath());
        assertThat(api.getFireFoxAddOnPath()).isEqualTo(AppResourcePath.getFireFoxAddOnPath());
        assertThat(api.getChromeAddOnPath()).isEqualTo(AppResourcePath.getChromeAddOnPath());
        assertThat(api.getSafariAddOnPath()).isEqualTo(AppResourcePath.getSafariAddOnPath());
    }

    @Test
    public void engineJarPathShouldDelegateToAppResourcePath() {
        assertThat(api.getEngineJarPath()).isEqualTo(AppResourcePath.getEngineJarPath());
    }
}

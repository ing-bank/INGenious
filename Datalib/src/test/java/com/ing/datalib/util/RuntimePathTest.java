package com.ing.datalib.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import org.testng.annotations.Test;

public class RuntimePathTest {

    @Test
    public void appHomePropertyHasPriority() throws Exception {
        String originalAppHome = System.getProperty(RuntimePath.APP_HOME_PROPERTY);

        String configuredRoot =
            System.getProperty("java.io.tmpdir") + File.separator + "ingenious-runtime-test";

        try {
            System.setProperty(RuntimePath.APP_HOME_PROPERTY, configuredRoot);

            assertThat(RuntimePath.getAppRoot())
                .isEqualTo(new File(configuredRoot).getCanonicalPath());
        } finally {
            restoreProperty(RuntimePath.APP_HOME_PROPERTY, originalAppHome);
        }
    }

    @Test
    public void legacyFallbackUsesCurrentDirectory() throws Exception {
        String originalAppHome = System.getProperty(RuntimePath.APP_HOME_PROPERTY);

        try {
            System.clearProperty(RuntimePath.APP_HOME_PROPERTY);

            assertThat(RuntimePath.getAppRoot())
                .isEqualTo(new File(System.getProperty("user.dir")).getCanonicalPath());
        } finally {
            restoreProperty(RuntimePath.APP_HOME_PROPERTY, originalAppHome);
        }
    }

    @Test
    public void packagedConfigurationUsesAppHome() throws Exception {
        String originalAppHome = System.getProperty(RuntimePath.APP_HOME_PROPERTY);
        File appHome = java.nio.file.Files.createTempDirectory("ingenious-runtime-app").toFile();

        try {
            System.setProperty(RuntimePath.APP_HOME_PROPERTY, appHome.getCanonicalPath());

            assertThat(RuntimePath.getConfigurationPath())
                .isEqualTo(new File(appHome, "Configuration").getCanonicalPath());
        } finally {
            restoreProperty(RuntimePath.APP_HOME_PROPERTY, originalAppHome);
        }
    }

    @Test
    public void sourceDevelopmentConfigurationUsesResourceTree() throws Exception {
        String originalAppHome = System.getProperty(RuntimePath.APP_HOME_PROPERTY);
        String originalUserDirectory = System.getProperty("user.dir");

        File repository = java.nio.file.Files.createTempDirectory("ingenious-source-root").toFile();

        File sourceConfiguration = new File(
            repository,
            "Resources" + File.separator + "Runtime" + File.separator + "Configuration"
        );

        assertThat(sourceConfiguration.mkdirs()).isTrue();

        try {
            System.clearProperty(RuntimePath.APP_HOME_PROPERTY);
            System.setProperty("user.dir", repository.getCanonicalPath());

            assertThat(RuntimePath.getConfigurationPath())
                .isEqualTo(sourceConfiguration.getCanonicalPath());
        } finally {
            restoreProperty(RuntimePath.APP_HOME_PROPERTY, originalAppHome);
            restoreProperty("user.dir", originalUserDirectory);
        }
    }

    @Test
    public void legacyConfigurationPrecedesSourceResourceTree() throws Exception {
        String originalAppHome = System.getProperty(RuntimePath.APP_HOME_PROPERTY);
        String originalUserDirectory = System.getProperty("user.dir");

        File repository = java.nio.file.Files.createTempDirectory("ingenious-legacy-root").toFile();

        File legacyConfiguration = new File(repository, "Configuration");
        File sourceConfiguration = new File(
            repository,
            "Resources" + File.separator + "Runtime" + File.separator + "Configuration"
        );

        assertThat(legacyConfiguration.mkdirs()).isTrue();
        assertThat(sourceConfiguration.mkdirs()).isTrue();

        try {
            System.clearProperty(RuntimePath.APP_HOME_PROPERTY);
            System.setProperty("user.dir", repository.getCanonicalPath());

            assertThat(RuntimePath.getConfigurationPath())
                .isEqualTo(legacyConfiguration.getCanonicalPath());
        } finally {
            restoreProperty(RuntimePath.APP_HOME_PROPERTY, originalAppHome);
            restoreProperty("user.dir", originalUserDirectory);
        }
    }

    @Test
    public void childPathsUseRuntimeRoot() {
        assertThat(RuntimePath.getLibPath())
            .isEqualTo(new File(RuntimePath.getAppRoot(), "lib").getPath());

        assertThat(RuntimePath.getDriversPath())
            .isEqualTo(new File(RuntimePath.getLibPath(), "Drivers").getPath());
    }

    private static void restoreProperty(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }
}

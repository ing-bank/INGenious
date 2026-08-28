package com.ing.datalib.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import org.testng.annotations.Test;

public class WorkspacePathTest {

    @Test
    public void workspacePropertyHasHighestPriority() throws Exception {
        String originalWorkspace = System.getProperty(WorkspacePath.WORKSPACE_PROPERTY);

        String configuredWorkspace =
            System.getProperty("java.io.tmpdir") +
            File.separator +
            "ingenious-datalib-property-workspace";

        try {
            System.setProperty(WorkspacePath.WORKSPACE_PROPERTY, configuredWorkspace);

            assertThat(WorkspacePath.getWorkspaceRoot())
                .isEqualTo(new File(configuredWorkspace).getCanonicalPath());
        } finally {
            restoreProperty(WorkspacePath.WORKSPACE_PROPERTY, originalWorkspace);
        }
    }

    @Test
    public void legacyFallbackUsesCurrentDirectory() throws Exception {
        String originalWorkspace = System.getProperty(WorkspacePath.WORKSPACE_PROPERTY);

        String originalAppHome = System.getProperty(WorkspacePath.APP_HOME_PROPERTY);

        try {
            System.clearProperty(WorkspacePath.WORKSPACE_PROPERTY);
            System.clearProperty(WorkspacePath.APP_HOME_PROPERTY);

            assertThat(WorkspacePath.getWorkspaceRoot())
                .isEqualTo(new File(System.getProperty("user.dir")).getCanonicalPath());
        } finally {
            restoreProperty(WorkspacePath.WORKSPACE_PROPERTY, originalWorkspace);

            restoreProperty(WorkspacePath.APP_HOME_PROPERTY, originalAppHome);
        }
    }

    @Test
    public void workspaceEnvironmentPrecedesAutomaticDiscovery() throws Exception {
        File environmentWorkspace = new File(
            System.getProperty("java.io.tmpdir"),
            "ingenious-environment-workspace"
        );

        String actual = WorkspacePath.resolveWorkspaceRoot(
            null,
            environmentWorkspace.getPath(),
            "/Applications/INGenious.app/Contents/app",
            "Mac OS X",
            "/test/home",
            null,
            "/test/current"
        );

        assertThat(actual).isEqualTo(environmentWorkspace.getCanonicalPath());
    }

    @Test
    public void validSiblingWorkspaceIsUsedForPortableMacApplication() throws Exception {
        File distribution = createTemporaryDirectory("ingenious-portable");
        File appHome = new File(distribution, "INGenious.app/Contents/app");
        File workspace = new File(distribution, "Workspace");

        assertThat(appHome.mkdirs()).isTrue();
        createValidWorkspace(workspace);

        String actual = WorkspacePath.resolveWorkspaceRoot(
            null,
            null,
            appHome.getPath(),
            "Mac OS X",
            "/test/home",
            null,
            "/test/current"
        );

        assertThat(actual).isEqualTo(workspace.getCanonicalPath());
    }

    @Test
    public void missingSiblingUsesMacApplicationSupport() throws Exception {
        File distribution = createTemporaryDirectory("ingenious-installed");
        File appHome = new File(distribution, "INGenious.app/Contents/app");
        File userHome = createTemporaryDirectory("ingenious-user-home");

        assertThat(appHome.mkdirs()).isTrue();

        File expected = new File(
            new File(new File(userHome, "Library"), "Application Support"),
            "INGenious"
        );

        String actual = WorkspacePath.resolveWorkspaceRoot(
            null,
            null,
            appHome.getPath(),
            "Mac OS X",
            userHome.getPath(),
            null,
            "/test/current"
        );

        assertThat(actual).isEqualTo(expected.getCanonicalPath());
    }

    @Test
    public void incompleteSiblingUsesMacApplicationSupport() throws Exception {
        File distribution = createTemporaryDirectory("ingenious-incomplete");
        File appHome = new File(distribution, "INGenious.app/Contents/app");
        File workspace = new File(distribution, "Workspace");
        File userHome = createTemporaryDirectory("ingenious-user-home");

        assertThat(appHome.mkdirs()).isTrue();
        assertThat(new File(workspace, "Configuration").mkdirs()).isTrue();

        File expected = new File(
            new File(new File(userHome, "Library"), "Application Support"),
            "INGenious"
        );

        String actual = WorkspacePath.resolveWorkspaceRoot(
            null,
            null,
            appHome.getPath(),
            "Mac OS X",
            userHome.getPath(),
            null,
            "/test/current"
        );

        assertThat(actual).isEqualTo(expected.getCanonicalPath());
    }

    @Test
    public void packagedWindowsApplicationUsesLocalAppData() throws Exception {
        File localAppData = createTemporaryDirectory("ingenious-local-app-data");

        File expected = new File(localAppData, "INGenious");

        String actual = WorkspacePath.resolveWorkspaceRoot(
            null,
            null,
            "C:\\Program Files\\INGenious\\app",
            "Windows 11",
            "C:\\Users\\test",
            localAppData.getPath(),
            "C:\\test\\current"
        );

        assertThat(actual).isEqualTo(expected.getCanonicalPath());
    }

    @Test
    public void packagedWindowsApplicationFallsBackToUserProfile() throws Exception {
        File userHome = createTemporaryDirectory("ingenious-windows-user-home");

        File expected = new File(new File(new File(userHome, "AppData"), "Local"), "INGenious");

        String actual = WorkspacePath.resolveWorkspaceRoot(
            null,
            null,
            "C:\\Program Files\\INGenious\\app",
            "Windows 11",
            userHome.getPath(),
            null,
            "C:\\test\\current"
        );

        assertThat(actual).isEqualTo(expected.getCanonicalPath());
    }

    @Test
    public void nonMacPackagedApplicationUsesLegacyFallback() throws Exception {
        File currentDirectory = createTemporaryDirectory("ingenious-legacy");

        String actual = WorkspacePath.resolveWorkspaceRoot(
            null,
            null,
            "/test/packaged/application",
            "Linux",
            "/test/home",
            null,
            currentDirectory.getPath()
        );

        assertThat(actual).isEqualTo(currentDirectory.getCanonicalPath());
    }

    @Test
    public void childPathsUseWorkspaceRoot() {
        assertThat(WorkspacePath.getConfigurationPath())
            .isEqualTo(WorkspacePath.getWorkspaceRoot() + File.separator + "Configuration");

        assertThat(WorkspacePath.getProjectsPath())
            .isEqualTo(WorkspacePath.getWorkspaceRoot() + File.separator + "Projects");

        assertThat(WorkspacePath.getSharedPath())
            .isEqualTo(WorkspacePath.getWorkspaceRoot() + File.separator + "Shared");

        assertThat(WorkspacePath.getUserDefinedPath())
            .isEqualTo(WorkspacePath.getWorkspaceRoot() + File.separator + "UserDefined");

        assertThat(WorkspacePath.getPluginsPath())
            .isEqualTo(WorkspacePath.getWorkspaceRoot() + File.separator + "plugins");
    }

    private static File createTemporaryDirectory(String prefix) throws Exception {
        return java.nio.file.Files.createTempDirectory(prefix).toFile();
    }

    private static void createValidWorkspace(File workspace) {
        assertThat(new File(workspace, "Configuration").mkdirs()).isTrue();
        assertThat(new File(workspace, "Projects").mkdirs()).isTrue();
        assertThat(new File(workspace, "Shared").mkdirs()).isTrue();
    }

    private static void restoreProperty(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }
}

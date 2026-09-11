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
            "/test/current"
        );

        assertThat(actual).isEqualTo(workspace.getCanonicalPath());
    }

    @Test
    public void missingSiblingUsesDocumentsWorkspace() throws Exception {
        File distribution = createTemporaryDirectory("ingenious-installed");
        File appHome = new File(distribution, "INGenious.app/Contents/app");
        File userHome = createTemporaryDirectory("ingenious-user-home");

        assertThat(appHome.mkdirs()).isTrue();

        File expected = new File(new File(userHome, "Documents"), "INGenious Workspace");

        String actual = WorkspacePath.resolveWorkspaceRoot(
            null,
            null,
            appHome.getPath(),
            "Mac OS X",
            userHome.getPath(),
            "/test/current"
        );

        assertThat(actual).isEqualTo(expected.getCanonicalPath());
    }

    @Test
    public void incompleteSiblingUsesDocumentsWorkspace() throws Exception {
        File distribution = createTemporaryDirectory("ingenious-incomplete");
        File appHome = new File(distribution, "INGenious.app/Contents/app");
        File workspace = new File(distribution, "Workspace");
        File userHome = createTemporaryDirectory("ingenious-user-home");

        assertThat(appHome.mkdirs()).isTrue();
        assertThat(new File(workspace, "Configuration").mkdirs()).isTrue();

        File expected = new File(new File(userHome, "Documents"), "INGenious Workspace");

        String actual = WorkspacePath.resolveWorkspaceRoot(
            null,
            null,
            appHome.getPath(),
            "Mac OS X",
            userHome.getPath(),
            "/test/current"
        );

        assertThat(actual).isEqualTo(expected.getCanonicalPath());
    }

    @Test
    public void packagedWindowsApplicationUsesDocumentsWorkspace() throws Exception {
        File userHome = createTemporaryDirectory("ingenious-windows-user-home");

        File expected = new File(new File(userHome, "Documents"), "INGenious Workspace");

        String actual = WorkspacePath.resolveWorkspaceRoot(
            null,
            null,
            "C:\\Program Files\\INGenious\\app",
            "Windows 11",
            userHome.getPath(),
            "C:\\test\\current"
        );

        assertThat(actual).isEqualTo(expected.getCanonicalPath());
    }

    @Test
    public void installedApplicationUsesConfiguredWorkspaceBase() throws Exception {
        File userHome = createTemporaryDirectory("ingenious-user-home");
        File selectedBase = new File(userHome, "Selected Base With Spaces");
        File expected = new File(selectedBase, "INGenious Workspace");

        String actual = WorkspacePath.resolveWorkspaceRoot(
            null,
            null,
            "/Applications/INGenious.app/Contents/app",
            "Mac OS X",
            userHome.getPath(),
            "/test/current",
            selectedBase.getPath()
        );

        assertThat(actual).isEqualTo(expected.getCanonicalPath());
    }

    @Test
    public void portableWorkspaceIgnoresConfiguredWorkspaceBase() throws Exception {
        File distribution = createTemporaryDirectory("ingenious-portable");
        File appHome = new File(distribution, "INGenious.app/Contents/app");
        File workspace = new File(distribution, "Workspace");
        File selectedBase = new File(distribution, "Selected Base");

        assertThat(appHome.mkdirs()).isTrue();
        createValidWorkspace(workspace);

        String actual = WorkspacePath.resolveWorkspaceRoot(
            null,
            null,
            appHome.getPath(),
            "Mac OS X",
            "/test/home",
            "/test/current",
            selectedBase.getPath()
        );

        assertThat(actual).isEqualTo(workspace.getCanonicalPath());
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
            currentDirectory.getPath()
        );

        assertThat(actual).isEqualTo(currentDirectory.getCanonicalPath());
    }

    @Test
    public void installedApplicationLoadsSavedWorkspaceBase() throws Exception {
        File userHome = createTemporaryDirectory("saved-workspace-home");
        File appHome = new File(userHome, "Applications/INGenious.app/Contents/app");
        File selectedBase = new File(userHome, "Selected Base With Spaces");
        File expected = new File(selectedBase, "INGenious Workspace");

        assertThat(appHome.mkdirs()).isTrue();

        String originalWorkspace = System.getProperty(WorkspacePath.WORKSPACE_PROPERTY);
        String originalAppHome = System.getProperty(WorkspacePath.APP_HOME_PROPERTY);
        String originalUserHome = System.getProperty("user.home");
        String originalOsName = System.getProperty("os.name");

        try {
            WorkspacePreference.saveWorkspaceBase(userHome.getPath(), selectedBase.toPath());

            System.clearProperty(WorkspacePath.WORKSPACE_PROPERTY);
            System.setProperty(WorkspacePath.APP_HOME_PROPERTY, appHome.getPath());
            System.setProperty("user.home", userHome.getPath());
            System.setProperty("os.name", "Mac OS X");

            assertThat(WorkspacePath.getWorkspaceRoot()).isEqualTo(expected.getCanonicalPath());
        } finally {
            restoreProperty(WorkspacePath.WORKSPACE_PROPERTY, originalWorkspace);
            restoreProperty(WorkspacePath.APP_HOME_PROPERTY, originalAppHome);
            restoreProperty("user.home", originalUserHome);
            restoreProperty("os.name", originalOsName);
        }
    }

    @Test
    public void derivesWorkspaceFolderFromSelectedBase() throws Exception {
        File selectedBase = createTemporaryDirectory("selected-base");

        assertThat(WorkspacePath.deriveInstalledWorkspace(selectedBase.getPath()))
            .isEqualTo(new File(selectedBase, "INGenious Workspace").getCanonicalPath());
    }

    @Test
    public void explicitWorkspaceDisablesCustomization() {
        String original = System.getProperty(WorkspacePath.WORKSPACE_PROPERTY);

        try {
            System.setProperty(WorkspacePath.WORKSPACE_PROPERTY, "/explicit/Workspace");

            assertThat(WorkspacePath.isWorkspaceCustomizationAvailable()).isFalse();
        } finally {
            restoreProperty(WorkspacePath.WORKSPACE_PROPERTY, original);
        }
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

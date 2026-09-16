package com.ing.datalib.util;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Resolves the external, writable INGenious Workspace.
 *
 * <p>This class belongs to Datalib so both Datalib and Engine can use the same
 * Workspace resolution without introducing a dependency cycle.
 */
public final class WorkspacePath {
    public static final String WORKSPACE_PROPERTY = "ingenious.workspace";
    public static final String WORKSPACE_ENVIRONMENT = "INGENIOUS_WORKSPACE";
    public static final String APP_HOME_PROPERTY = "ingenious.app.home";

    private WorkspacePath() {}

    /**
     * Resolves the Workspace using the following precedence:
     *
     * <ol>
     *   <li>The ingenious.workspace system property</li>
     *   <li>The INGENIOUS_WORKSPACE environment variable</li>
     *   <li>The packaged-application user Workspace fallback</li>
     *   <li>The current directory for legacy compatibility</li>
     * </ol>
     *
     * @return canonical absolute Workspace path
     */
    public static String getWorkspaceRoot() {
        String userHome = System.getProperty("user.home");

        return resolveWorkspaceRoot(
            System.getProperty(WORKSPACE_PROPERTY),
            System.getenv(WORKSPACE_ENVIRONMENT),
            System.getProperty(APP_HOME_PROPERTY),
            System.getProperty("os.name"),
            userHome,
            System.getProperty("user.dir"),
            WorkspacePreference.loadWorkspaceBase(userHome)
        );
    }

    static String resolveWorkspaceRoot(
        String configuredPath,
        String environmentPath,
        String appHome,
        String osName,
        String userHome,
        String userDirectory
    ) {
        return resolveWorkspaceRoot(
            configuredPath,
            environmentPath,
            appHome,
            osName,
            userHome,
            userDirectory,
            null
        );
    }

    static String resolveWorkspaceRoot(
        String configuredPath,
        String environmentPath,
        String appHome,
        String osName,
        String userHome,
        String userDirectory,
        String workspaceBase
    ) {
        if (configuredPath != null && !configuredPath.isBlank()) {
            return canonicalPath(configuredPath);
        }

        if (environmentPath != null && !environmentPath.isBlank()) {
            return canonicalPath(environmentPath);
        }

        if (isPackagedMacApplication(appHome, osName)) {
            File portableWorkspace = new File(appHome, "../../../Workspace");

            if (isValidWorkspace(portableWorkspace)) {
                return canonicalPath(portableWorkspace.getPath());
            }

            return canonicalPath(installedWorkspace(userHome, workspaceBase).getPath());
        }

        if (isPackagedWindowsApplication(appHome, osName)) {
            return canonicalPath(installedWorkspace(userHome, workspaceBase).getPath());
        }

        return canonicalPath(userDirectory);
    }

    public static boolean isWorkspaceCustomizationAvailable() {
        String configuredPath = System.getProperty(WORKSPACE_PROPERTY);
        String environmentPath = System.getenv(WORKSPACE_ENVIRONMENT);
        String appHome = System.getProperty(APP_HOME_PROPERTY);
        String osName = System.getProperty("os.name");

        if (
            configuredPath != null &&
            !configuredPath.isBlank() ||
            environmentPath != null &&
            !environmentPath.isBlank()
        ) {
            return false;
        }

        if (isPackagedMacApplication(appHome, osName)) {
            return !isValidWorkspace(new File(appHome, "../../../Workspace"));
        }

        return isPackagedWindowsApplication(appHome, osName);
    }

    public static String getInstalledWorkspaceBase() {
        String userHome = System.getProperty("user.home");
        String configuredBase = WorkspacePreference.loadWorkspaceBase(userHome);

        File base = configuredBase == null || configuredBase.isBlank()
            ? new File(userHome, "Documents")
            : new File(configuredBase);

        return canonicalPath(base.getPath());
    }

    public static String deriveInstalledWorkspace(String basePath) {
        return canonicalPath(new File(basePath, "INGenious Workspace").getPath());
    }

    public static String getConfigurationPath() {
        return getWorkspaceRoot() + File.separator + "Configuration";
    }

    public static String getProjectsPath() {
        return getWorkspaceRoot() + File.separator + "Projects";
    }

    public static String getSharedPath() {
        return getWorkspaceRoot() + File.separator + "Shared";
    }

    public static String getUserDefinedPath() {
        return getWorkspaceRoot() + File.separator + "UserDefined";
    }

    /**
     * Returns the persistent directory containing user-installed plugins.
     */
    public static String getPluginsPath() {
        return getWorkspaceRoot() + File.separator + "plugins";
    }

    private static File installedWorkspace(String userHome, String workspaceBase) {
        File base = workspaceBase == null || workspaceBase.isBlank()
            ? new File(userHome, "Documents")
            : new File(workspaceBase);

        return new File(base, "INGenious Workspace");
    }

    private static boolean isPackagedMacApplication(String appHome, String osName) {
        return (
            appHome != null &&
            !appHome.isBlank() &&
            osName != null &&
            osName.regionMatches(true, 0, "Mac", 0, 3)
        );
    }

    private static boolean isPackagedWindowsApplication(String appHome, String osName) {
        return (
            appHome != null &&
            !appHome.isBlank() &&
            osName != null &&
            osName.regionMatches(true, 0, "Windows", 0, 7)
        );
    }

    private static boolean isValidWorkspace(File workspace) {
        return (
            workspace.isDirectory() &&
            new File(workspace, "Configuration").isDirectory() &&
            new File(workspace, "Projects").isDirectory() &&
            new File(workspace, "Shared").isDirectory()
        );
    }

    private static String canonicalPath(String value) {
        try {
            return new File(value).getCanonicalPath();
        } catch (IOException ex) {
            Logger
                .getLogger(WorkspacePath.class.getName())
                .log(Level.WARNING, "Could not resolve Workspace path: " + value, ex);

            return new File(value).getAbsolutePath();
        }
    }
}

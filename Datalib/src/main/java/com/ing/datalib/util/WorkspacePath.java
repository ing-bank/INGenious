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
        return resolveWorkspaceRoot(
            System.getProperty(WORKSPACE_PROPERTY),
            System.getenv(WORKSPACE_ENVIRONMENT),
            System.getProperty(APP_HOME_PROPERTY),
            System.getProperty("os.name"),
            System.getProperty("user.home"),
            System.getenv("LOCALAPPDATA"),
            System.getProperty("user.dir")
        );
    }

    static String resolveWorkspaceRoot(
        String configuredPath,
        String environmentPath,
        String appHome,
        String osName,
        String userHome,
        String localAppData,
        String userDirectory
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

            File library = new File(userHome, "Library");
            File applicationSupport = new File(library, "Application Support");

            return canonicalPath(new File(applicationSupport, "INGenious").getPath());
        }

        if (isPackagedWindowsApplication(appHome, osName)) {
            if (localAppData != null && !localAppData.isBlank()) {
                return canonicalPath(new File(localAppData, "INGenious").getPath());
            }

            File appData = new File(userHome, "AppData");
            File local = new File(appData, "Local");

            return canonicalPath(new File(local, "INGenious").getPath());
        }

        return canonicalPath(userDirectory);
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

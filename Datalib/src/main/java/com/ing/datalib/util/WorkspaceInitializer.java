package com.ing.datalib.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Initializes a writable Workspace from a packaged read-only template.
 *
 * <p>Existing files are always preserved. Missing directories and files are
 * copied from WorkspaceTemplate when the template is available.
 */
public final class WorkspaceInitializer {
    private static final String TEMPLATE_DIRECTORY = "WorkspaceTemplate";

    private WorkspaceInitializer() {}

    /**
     * Initializes the resolved Workspace when a packaged template is present.
     *
     * <p>Portable distributions already contain a complete Workspace, so this
     * method has no effect when WorkspaceTemplate is absent.
     */
    public static void initialize() {
        String appHome = System.getProperty(WorkspacePath.APP_HOME_PROPERTY);

        if (appHome == null || appHome.isBlank()) {
            return;
        }

        initialize(Path.of(appHome, TEMPLATE_DIRECTORY), Path.of(WorkspacePath.getWorkspaceRoot()));
    }

    static void initialize(Path template, Path workspace) {
        if (!Files.isDirectory(template) || isInitializedWorkspace(workspace)) {
            return;
        }

        try {
            initializeFromTemplate(template, workspace);
        } catch (IOException ex) {
            Logger
                .getLogger(WorkspaceInitializer.class.getName())
                .log(Level.SEVERE, "Could not initialize Workspace: " + workspace, ex);
        }
    }

    private static boolean isInitializedWorkspace(Path workspace) {
        return (
            Files.isDirectory(workspace) &&
            Files.isDirectory(workspace.resolve("Configuration")) &&
            Files.isDirectory(workspace.resolve("Projects")) &&
            Files.isDirectory(workspace.resolve("Shared"))
        );
    }

    private static void initializeFromTemplate(Path template, Path workspace) throws IOException {
        Files.createDirectories(workspace);

        try (Stream<Path> paths = Files.walk(template)) {
            for (Path source : paths
                .sorted(Comparator.comparingInt(Path::getNameCount).thenComparing(Path::toString))
                .toList()) {
                copyMissing(template, workspace, source);
            }
        }
    }

    private static void copyMissing(Path template, Path workspace, Path source) throws IOException {
        Path relative = template.relativize(source);
        Path destination = workspace.resolve(relative);

        if (Files.isDirectory(source)) {
            Files.createDirectories(destination);
        } else if (Files.notExists(destination)) {
            Files.createDirectories(destination.getParent());
            Files.copy(source, destination, StandardCopyOption.COPY_ATTRIBUTES);
        }
    }
}

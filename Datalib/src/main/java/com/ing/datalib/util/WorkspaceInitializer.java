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
        if (!Files.isDirectory(template)) {
            return;
        }

        try {
            Files.createDirectories(workspace);

            try (Stream<Path> paths = Files.walk(template)) {
                paths
                    .sorted(
                        Comparator.comparingInt(Path::getNameCount).thenComparing(Path::toString)
                    )
                    .forEach(source -> copyMissing(template, workspace, source));
            }
        } catch (IOException ex) {
            Logger
                .getLogger(WorkspaceInitializer.class.getName())
                .log(Level.SEVERE, "Could not initialize Workspace: " + workspace, ex);
        }
    }

    private static void copyMissing(Path template, Path workspace, Path source) {
        Path relative = template.relativize(source);
        Path destination = workspace.resolve(relative);

        try {
            if (Files.isDirectory(source)) {
                Files.createDirectories(destination);
            } else if (Files.notExists(destination)) {
                Files.createDirectories(destination.getParent());
                Files.copy(source, destination, StandardCopyOption.COPY_ATTRIBUTES);
            }
        } catch (IOException ex) {
            throw new WorkspaceInitializationException(
                "Could not copy Workspace entry: " + relative,
                ex
            );
        }
    }

    private static final class WorkspaceInitializationException extends RuntimeException {

        private WorkspaceInitializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

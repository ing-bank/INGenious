package com.ing.ide.uitest.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * The simulated INGenious installation the IDE runs in during UI tests.
 * <p>
 * The IDE resolves {@code Configuration/}, {@code Projects/}, {@code lib/} and {@code recent.items}
 * relative to {@code user.dir}, so the failsafe plugin starts the test JVM inside
 * {@code target/uitest-home}, which Maven assembles from {@code Resources/} plus the engine jar.
 */
public final class UiTestHome {
    private static final String TOUR_COMPLETED_KEY = "tourCompleted";
    private static final String LOAD_RECENT_KEY = "loadRecentProject";
    private static final String TEST_PROJECT_PREFIX = "UITest";
    private static final String FIXTURE_SOURCE = "Tutorial";

    private static final ObjectMapper JSON = new ObjectMapper();

    private final Path root;

    private UiTestHome(final Path root) {
        this.root = root;
    }

    /**
     * Returns the installation in the current working directory.
     *
     * @return the UI test home
     */
    public static UiTestHome current() {
        return new UiTestHome(Paths.get(System.getProperty("user.dir")).toAbsolutePath());
    }

    /**
     * Fails fast when the working directory is not a prepared INGenious installation,
     * e.g. when a test is started outside {@code mvn verify -Puitest}.
     *
     * @throws IllegalStateException if required folders or the engine jar are missing
     */
    public void verifyPrepared() {
        if (!Files.isDirectory(root.resolve("Configuration")) || !hasEngineJar()) {
            throw new IllegalStateException(
                "Working directory " +
                root +
                " is not a prepared INGenious installation. " +
                "Run the tests with: mvn verify -Puitest -pl IDE-UITest -am"
            );
        }
    }

    /**
     * Resets IDE state so every launch starts at the startup dialog without first-run popups.
     *
     * @throws IOException if the state files cannot be written
     */
    public void resetForFreshLaunch() throws IOException {
        deleteTestProjects();
        Files.deleteIfExists(root.resolve("recent.items"));
        writeSettings(false);
    }

    /**
     * Copies the bundled Tutorial project to a new test project, as a known starting point.
     *
     * @param name project (folder) name; must start with {@code UITest} so it is cleaned up next run
     * @return the project folder
     * @throws IOException if the copy fails
     */
    public Path createFixtureProject(final String name) throws IOException {
        if (!name.startsWith(TEST_PROJECT_PREFIX)) {
            throw new IllegalArgumentException(
                "Test project names must start with " + TEST_PROJECT_PREFIX + ": " + name
            );
        }
        final Path source = root.resolve("Projects").resolve(FIXTURE_SOURCE);
        final Path target = projectDir(name);
        try (Stream<Path> files = Files.walk(source)) {
            for (final Path file : (Iterable<Path>) files::iterator) {
                final Path copy = target.resolve(source.relativize(file).toString());
                if (Files.isDirectory(file)) {
                    Files.createDirectories(copy);
                } else {
                    Files.copy(file, copy);
                }
            }
        }
        return target;
    }

    /**
     * Makes the next launch open a project directly, skipping the startup dialog, as for a user
     * who has "Auto-open last project" enabled.
     *
     * @param project the project folder
     * @throws IOException if the state files cannot be written
     */
    public void openOnLaunch(final Path project) throws IOException {
        final String json = JSON.writeValueAsString(
            List.of(
                Map.of(
                    "projectName",
                    project.getFileName().toString(),
                    "location",
                    project.toAbsolutePath().toString()
                )
            )
        );
        Files.writeString(root.resolve("recent.items"), json);
        writeSettings(true);
    }

    private void deleteTestProjects() throws IOException {
        final Path projects = root.resolve("Projects");
        if (!Files.isDirectory(projects)) {
            return;
        }
        try (Stream<Path> dirs = Files.list(projects)) {
            for (final Path dir : (Iterable<Path>) dirs.filter(
                d -> d.getFileName().toString().startsWith(TEST_PROJECT_PREFIX)
            )::iterator) {
                try (Stream<Path> files = Files.walk(dir)) {
                    for (final Path file : (Iterable<Path>) files.sorted(
                        Comparator.reverseOrder()
                    )::iterator) {
                        Files.delete(file);
                    }
                }
            }
        }
    }

    private void writeSettings(final boolean loadRecent) throws IOException {
        final Properties settings = new Properties();
        settings.setProperty(TOUR_COMPLETED_KEY, "true");
        settings.setProperty(LOAD_RECENT_KEY, String.valueOf(loadRecent));
        try (
            OutputStream out = Files.newOutputStream(
                root.resolve("Configuration").resolve("app.settings")
            )
        ) {
            settings.store(out, "INGenious UI test defaults");
        }
    }

    /**
     * Returns the folder where a project with the given name is created.
     *
     * @param projectName the project name
     * @return the project folder
     */
    public Path projectDir(final String projectName) {
        return root.resolve("Projects").resolve(projectName);
    }

    private boolean hasEngineJar() {
        final Path lib = root.resolve("lib");
        if (!Files.isDirectory(lib)) {
            return false;
        }
        try (Stream<Path> files = Files.list(lib)) {
            return files.anyMatch(
                p -> p.getFileName().toString().matches("ingenious-engine.*\\.jar")
            );
        } catch (IOException e) {
            return false;
        }
    }
}

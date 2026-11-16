package com.ing.ide.e2e.support;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * The simulated INGenious installation the IDE runs in during E2E tests.
 * <p>
 * The IDE resolves {@code Configuration/}, {@code Projects/}, {@code lib/} and {@code recent.items}
 * relative to {@code user.dir}, so the failsafe plugin starts the test JVM inside
 * {@code target/e2e-home}, which Maven assembles from {@code Resources/} plus the engine jar.
 */
public final class E2EHome {
    private static final String TOUR_COMPLETED_KEY = "tourCompleted";

    private final Path root;

    private E2EHome(final Path root) {
        this.root = root;
    }

    /**
     * Returns the installation in the current working directory.
     *
     * @return the E2E home
     */
    public static E2EHome current() {
        return new E2EHome(Paths.get(System.getProperty("user.dir")).toAbsolutePath());
    }

    /**
     * Fails fast when the working directory is not a prepared INGenious installation,
     * e.g. when a test is started outside {@code mvn verify -Pe2e}.
     *
     * @throws IllegalStateException if required folders or the engine jar are missing
     */
    public void verifyPrepared() {
        if (!Files.isDirectory(root.resolve("Configuration")) || !hasEngineJar()) {
            throw new IllegalStateException(
                "Working directory " +
                root +
                " is not a prepared INGenious installation. " +
                "Run the tests with: mvn verify -Pe2e -pl IDE-E2E -am"
            );
        }
    }

    /**
     * Resets IDE state so every launch starts at the startup dialog without first-run popups.
     *
     * @throws IOException if the state files cannot be written
     */
    public void resetForFreshLaunch() throws IOException {
        Files.deleteIfExists(root.resolve("recent.items"));
        final Properties settings = new Properties();
        settings.setProperty(TOUR_COMPLETED_KEY, "true");
        try (
            OutputStream out = Files.newOutputStream(
                root.resolve("Configuration").resolve("app.settings")
            )
        ) {
            settings.store(out, "INGenious E2E defaults");
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

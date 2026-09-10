package com.ing.engine.cli.commands;

import static org.assertj.core.api.Assertions.assertThat;

import com.ing.engine.constants.AppResourcePath;
import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class RunCommandTest {
    private String originalUserDirectory;
    private String originalWorkspace;
    private Path testRoot;

    @BeforeMethod
    public void setUp() throws Exception {
        originalUserDirectory = System.getProperty("user.dir");
        originalWorkspace = System.getProperty(AppResourcePath.WORKSPACE_PROPERTY);
        testRoot = Files.createTempDirectory("ingenious-run-command-");
    }

    @AfterMethod
    public void tearDown() throws Exception {
        restoreProperty("user.dir", originalUserDirectory);
        restoreProperty(AppResourcePath.WORKSPACE_PROPERTY, originalWorkspace);

        if (testRoot != null && Files.exists(testRoot)) {
            try (var paths = Files.walk(testRoot)) {
                paths
                    .sorted(Comparator.reverseOrder())
                    .forEach(
                        path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (Exception ex) {
                                throw new AssertionError("Could not delete test path: " + path, ex);
                            }
                        }
                    );
            }
        }
    }

    @Test
    public void resolvesProjectFromConfiguredWorkspace() throws Exception {
        Path currentDirectory = Files.createDirectories(testRoot.resolve("current"));
        Path workspace = Files.createDirectories(testRoot.resolve("workspace"));
        Path expected = Files.createDirectories(workspace.resolve("Projects").resolve("Tutorial"));

        System.setProperty("user.dir", currentDirectory.toString());
        System.setProperty(AppResourcePath.WORKSPACE_PROPERTY, workspace.toString());

        assertThat(resolveProjectDir("Tutorial").getCanonicalFile())
            .isEqualTo(expected.toFile().getCanonicalFile());
    }

    @Test
    public void currentDirectoryProjectTakesPrecedenceOverWorkspace() throws Exception {
        Path currentDirectory = Files.createDirectories(testRoot.resolve("current"));
        Path expected = Files.createDirectories(currentDirectory.resolve("Tutorial"));
        Path workspace = Files.createDirectories(testRoot.resolve("workspace"));

        Files.createDirectories(workspace.resolve("Projects").resolve("Tutorial"));

        System.setProperty("user.dir", currentDirectory.toString());
        System.setProperty(AppResourcePath.WORKSPACE_PROPERTY, workspace.toString());

        assertThat(resolveProjectDir("Tutorial").getCanonicalFile())
            .isEqualTo(expected.toFile().getCanonicalFile());
    }

    @Test
    public void currentDirectoryProjectsTakesPrecedenceOverWorkspace() throws Exception {
        Path currentDirectory = Files.createDirectories(testRoot.resolve("current"));
        Path expected = Files.createDirectories(
            currentDirectory.resolve("Projects").resolve("Tutorial")
        );
        Path workspace = Files.createDirectories(testRoot.resolve("workspace"));

        Files.createDirectories(workspace.resolve("Projects").resolve("Tutorial"));

        System.setProperty("user.dir", currentDirectory.toString());
        System.setProperty(AppResourcePath.WORKSPACE_PROPERTY, workspace.toString());

        assertThat(resolveProjectDir("Tutorial").getCanonicalFile())
            .isEqualTo(expected.toFile().getCanonicalFile());
    }

    @Test
    public void absoluteProjectPathTakesPrecedence() throws Exception {
        Path currentDirectory = Files.createDirectories(testRoot.resolve("current"));
        Path workspace = Files.createDirectories(testRoot.resolve("workspace"));
        Path expected = Files.createDirectories(testRoot.resolve("absolute-project"));

        System.setProperty("user.dir", currentDirectory.toString());
        System.setProperty(AppResourcePath.WORKSPACE_PROPERTY, workspace.toString());

        assertThat(resolveProjectDir(expected.toString()).getCanonicalFile())
            .isEqualTo(expected.toFile().getCanonicalFile());
    }

    @Test
    public void returnsNullWhenProjectDoesNotExist() throws Exception {
        Path currentDirectory = Files.createDirectories(testRoot.resolve("current"));
        Path workspace = Files.createDirectories(testRoot.resolve("workspace"));

        System.setProperty("user.dir", currentDirectory.toString());
        System.setProperty(AppResourcePath.WORKSPACE_PROPERTY, workspace.toString());

        assertThat(resolveProjectDir("MissingProject")).isNull();
    }

    private static File resolveProjectDir(String name) throws Exception {
        Method method = RunCommand.class.getDeclaredMethod("resolveProjectDir", String.class);
        method.setAccessible(true);
        return (File) method.invoke(null, name);
    }

    private static void restoreProperty(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }
}

package com.ing.datalib.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.testng.annotations.Test;

public class WorkspaceRelocatorTest {

    @Test
    public void copiesCompleteWorkspaceAndPreservesSource() throws Exception {
        Path root = Files.createTempDirectory("workspace-relocator");
        Path source = root.resolve("Documents").resolve("INGenious Workspace");
        Path destination = root.resolve("Downloads").resolve("INGenious Workspace");

        createValidWorkspace(source);

        Path project = source
            .resolve("Projects")
            .resolve("Customer Project")
            .resolve("TestPlan")
            .resolve("Scenario.yaml");

        Path plugin = source.resolve("plugins").resolve("custom-plugin").resolve("plugin.jar");

        Path configuration = source.resolve("Configuration").resolve("user.properties");

        Path shared = source
            .resolve("Shared")
            .resolve("SharedObjectRepository")
            .resolve("object.yaml");

        Files.createDirectories(project.getParent());
        Files.createDirectories(plugin.getParent());
        Files.createDirectories(shared.getParent());

        Files.writeString(project, "scenario-content");
        Files.writeString(plugin, "plugin-content");
        Files.writeString(configuration, "theme=user-defined");
        Files.writeString(shared, "shared-content");

        WorkspaceRelocator.copyWorkspace(source, destination);

        assertThat(destination.resolve(source.relativize(project))).hasContent("scenario-content");
        assertThat(destination.resolve(source.relativize(plugin))).hasContent("plugin-content");
        assertThat(destination.resolve(source.relativize(configuration)))
            .hasContent("theme=user-defined");
        assertThat(destination.resolve(source.relativize(shared))).hasContent("shared-content");

        assertThat(project).hasContent("scenario-content");
        assertThat(plugin).hasContent("plugin-content");
        assertThat(configuration).hasContent("theme=user-defined");
        assertThat(shared).hasContent("shared-content");
        assertThat(source).isDirectory();
    }

    @Test
    public void supportsDestinationPathContainingSpaces() throws Exception {
        Path root = Files.createTempDirectory("workspace-spaces");
        Path source = root.resolve("Current INGenious Workspace");
        Path destination = root.resolve("Selected Base With Spaces").resolve("INGenious Workspace");

        createValidWorkspace(source);
        Files.writeString(source.resolve("Projects").resolve("Readme.txt"), "project-content");

        WorkspaceRelocator.copyWorkspace(source, destination);

        assertThat(destination.resolve("Projects").resolve("Readme.txt"))
            .hasContent("project-content");
    }

    @Test
    public void refusesToOverwriteExistingDestination() throws Exception {
        Path root = Files.createTempDirectory("workspace-existing");
        Path source = root.resolve("Source Workspace");
        Path destination = root.resolve("Existing Workspace");

        createValidWorkspace(source);
        createValidWorkspace(destination);

        Path destinationFile = destination.resolve("Projects").resolve("existing.txt");

        Files.writeString(destinationFile, "existing-content");

        assertThatThrownBy(() -> WorkspaceRelocator.copyWorkspace(source, destination))
            .isInstanceOf(FileAlreadyExistsException.class)
            .hasMessageContaining("destination Workspace already exists");

        assertThat(destinationFile).hasContent("existing-content");
        assertThat(source).isDirectory();
    }

    @Test
    public void rejectsIncompleteSourceWorkspace() throws Exception {
        Path root = Files.createTempDirectory("workspace-incomplete-source");
        Path source = root.resolve("Incomplete Workspace");
        Path destination = root.resolve("Destination Workspace");

        Files.createDirectories(source.resolve("Configuration"));
        Files.createDirectories(source.resolve("Projects"));

        assertThatThrownBy(() -> WorkspaceRelocator.copyWorkspace(source, destination))
            .isInstanceOf(java.io.IOException.class)
            .hasMessageContaining("current Workspace is incomplete or unavailable");

        assertThat(destination).doesNotExist();
        assertThat(source.resolve("Configuration")).isDirectory();
        assertThat(source.resolve("Projects")).isDirectory();
    }

    @Test
    public void rejectsDestinationInsideCurrentWorkspace() throws Exception {
        Path root = Files.createTempDirectory("workspace-child-destination");
        Path source = root.resolve("INGenious Workspace");
        Path destination = source.resolve("Projects").resolve("INGenious Workspace");

        createValidWorkspace(source);

        assertThatThrownBy(() -> WorkspaceRelocator.validateRelocationPaths(source, destination))
            .isInstanceOf(java.io.IOException.class)
            .hasMessageContaining("destination cannot be inside the active Workspace");

        assertThat(source).isDirectory();
        assertThat(destination).doesNotExist();
    }

    @Test
    public void rejectsDestinationThatContainsCurrentWorkspace() throws Exception {
        Path root = Files.createTempDirectory("workspace-parent-destination");
        Path destination = root.resolve("INGenious Workspace");
        Path source = destination.resolve("Nested").resolve("INGenious Workspace");

        createValidWorkspace(source);

        assertThatThrownBy(() -> WorkspaceRelocator.validateRelocationPaths(source, destination))
            .isInstanceOf(java.io.IOException.class)
            .hasMessageContaining("destination cannot contain the active Workspace");

        assertThat(source).isDirectory();
    }

    @Test
    public void rejectsSameWorkspaceAfterPathNormalization() throws Exception {
        Path root = Files.createTempDirectory("workspace-normalized-destination");
        Path source = root.resolve("INGenious Workspace");

        createValidWorkspace(source);

        Path equivalentDestination = source
            .resolve("Projects")
            .resolve("..")
            .resolve("..")
            .resolve("INGenious Workspace");

        assertThatThrownBy(
                () -> WorkspaceRelocator.validateRelocationPaths(source, equivalentDestination)
            )
            .isInstanceOf(java.io.IOException.class)
            .hasMessageContaining("already the active Workspace");

        assertThat(source).isDirectory();
    }

    @Test
    public void rejectsSymlinkDestinationInsideCurrentWorkspace() throws Exception {
        Path root = Files.createTempDirectory("workspace-symlink-destination");
        Path source = root.resolve("INGenious Workspace");
        Path sourceProjects = source.resolve("Projects");
        Path linkedBase = root.resolve("Linked Base");

        createValidWorkspace(source);

        try {
            Files.createSymbolicLink(linkedBase, sourceProjects);
        } catch (UnsupportedOperationException | java.nio.file.FileSystemException ex) {
            return;
        }

        Path destination = linkedBase.resolve("INGenious Workspace");

        assertThatThrownBy(() -> WorkspaceRelocator.validateRelocationPaths(source, destination))
            .isInstanceOf(java.io.IOException.class)
            .hasMessageContaining("destination cannot be inside the active Workspace");

        assertThat(source).isDirectory();
    }

    @Test
    public void verifiesIdenticalWorkspaceCopy() throws Exception {
        Path root = Files.createTempDirectory("workspace-verification");
        Path source = root.resolve("Current Workspace");
        Path destination = root.resolve("New Base").resolve("INGenious Workspace");

        createValidWorkspace(source);

        Path project = source
            .resolve("Projects")
            .resolve("Customer Project")
            .resolve("scenario.yaml");

        Path plugin = source.resolve("plugins").resolve("custom").resolve("plugin.jar");

        Files.createDirectories(project.getParent());
        Files.createDirectories(plugin.getParent());
        Files.writeString(project, "scenario-content");
        Files.writeString(plugin, "plugin-content");

        WorkspaceRelocator.copyWorkspace(source, destination);
        WorkspaceRelocator.verifyWorkspaceCopy(source, destination);

        assertThat(source).isDirectory();
        assertThat(destination).isDirectory();
    }

    @Test
    public void verificationRejectsModifiedDestinationFile() throws Exception {
        Path root = Files.createTempDirectory("workspace-verification-failure");
        Path source = root.resolve("Current Workspace");
        Path destination = root.resolve("New Base").resolve("INGenious Workspace");

        createValidWorkspace(source);

        Path sourceFile = source.resolve("Projects").resolve("project.yaml");

        Files.writeString(sourceFile, "source-content");

        WorkspaceRelocator.copyWorkspace(source, destination);

        Files.writeString(
            destination.resolve("Projects").resolve("project.yaml"),
            "changed-content"
        );

        assertThatThrownBy(() -> WorkspaceRelocator.verifyWorkspaceCopy(source, destination))
            .isInstanceOf(java.io.IOException.class)
            .hasMessageContaining("Destination file");

        assertThat(sourceFile).hasContent("source-content");
    }

    @Test
    public void verificationRejectsMissingDestinationFile() throws Exception {
        Path root = Files.createTempDirectory("workspace-missing-file");
        Path source = root.resolve("Current Workspace");
        Path destination = root.resolve("New Base").resolve("INGenious Workspace");

        createValidWorkspace(source);

        Path sourceFile = source.resolve("Shared").resolve("user-content.txt");

        Files.writeString(sourceFile, "user-content");

        WorkspaceRelocator.copyWorkspace(source, destination);

        Files.delete(destination.resolve("Shared").resolve("user-content.txt"));

        assertThatThrownBy(() -> WorkspaceRelocator.verifyWorkspaceCopy(source, destination))
            .isInstanceOf(java.io.IOException.class)
            .hasMessageContaining("Destination file is missing");

        assertThat(sourceFile).hasContent("user-content");
    }

    @Test
    public void deletesSourceAfterDestinationIsVerified() throws Exception {
        Path root = Files.createTempDirectory("workspace-verified-deletion");
        Path source = root.resolve("Current Workspace");
        Path destination = root.resolve("New Base").resolve("INGenious Workspace");

        createValidWorkspace(source);

        Path sourceProject = source.resolve("Projects").resolve("customer-project.yaml");

        Files.writeString(sourceProject, "customer-content");

        WorkspaceRelocator.copyWorkspace(source, destination);

        WorkspaceRelocator.deleteVerifiedSource(source, destination);

        assertThat(source).doesNotExist();
        assertThat(destination.resolve("Projects").resolve("customer-project.yaml"))
            .hasContent("customer-content");
    }

    @Test
    public void failedVerificationNeverDeletesSource() throws Exception {
        Path root = Files.createTempDirectory("workspace-protected-deletion");
        Path source = root.resolve("Current Workspace");
        Path destination = root.resolve("New Base").resolve("INGenious Workspace");

        createValidWorkspace(source);

        Path sourceProject = source.resolve("Projects").resolve("customer-project.yaml");

        Files.writeString(sourceProject, "original-content");

        WorkspaceRelocator.copyWorkspace(source, destination);

        Files.writeString(
            destination.resolve("Projects").resolve("customer-project.yaml"),
            "modified-content"
        );

        assertThatThrownBy(() -> WorkspaceRelocator.deleteVerifiedSource(source, destination))
            .isInstanceOf(java.io.IOException.class)
            .hasMessageContaining("Destination file");

        assertThat(source).isDirectory();
        assertThat(sourceProject).hasContent("original-content");
    }

    @Test
    public void identicalPathsCannotBeDeleted() throws Exception {
        Path root = Files.createTempDirectory("workspace-identical-deletion");
        Path workspace = root.resolve("INGenious Workspace");

        createValidWorkspace(workspace);

        assertThatThrownBy(() -> WorkspaceRelocator.deleteVerifiedSource(workspace, workspace))
            .isInstanceOf(java.io.IOException.class)
            .hasMessageContaining("source and destination Workspace paths are identical");

        assertThat(workspace).isDirectory();
    }

    @Test
    public void removesOnlyFailedDestinationAndPreservesSource() throws Exception {
        Path root = Files.createTempDirectory("workspace-rollback");
        Path source = root.resolve("Current Workspace");
        Path destination = root.resolve("Selected Base").resolve("INGenious Workspace");

        createValidWorkspace(source);

        Path userProject = source.resolve("Projects").resolve("user-project.yaml");

        Files.writeString(userProject, "user-content");

        WorkspaceRelocator.copyWorkspace(source, destination);
        WorkspaceRelocator.removeFailedDestination(destination);

        assertThat(destination).doesNotExist();
        assertThat(source).isDirectory();
        assertThat(userProject).hasContent("user-content");
    }

    @Test
    public void rejectsSameSourceAndDestination() throws Exception {
        Path root = Files.createTempDirectory("workspace-same-location");
        Path workspace = root.resolve("INGenious Workspace");

        createValidWorkspace(workspace);

        Path userFile = workspace.resolve("Projects").resolve("user-project.txt");

        Files.writeString(userFile, "user-content");

        assertThatThrownBy(() -> WorkspaceRelocator.copyWorkspace(workspace, workspace))
            .isInstanceOf(java.io.IOException.class)
            .hasMessageContaining("selected location is already the active Workspace");

        assertThat(userFile).hasContent("user-content");
        assertThat(workspace).isDirectory();
    }

    private static void createValidWorkspace(Path workspace) throws Exception {
        Files.createDirectories(workspace.resolve("Configuration"));
        Files.createDirectories(workspace.resolve("Projects"));
        Files.createDirectories(workspace.resolve("Shared"));
        Files.createDirectories(workspace.resolve("plugins"));
    }
}

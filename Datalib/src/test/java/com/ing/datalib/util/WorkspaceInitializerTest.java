package com.ing.datalib.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.testng.annotations.Test;

public class WorkspaceInitializerTest {

    @Test
    public void copiesMissingWorkspaceEntries() throws Exception {
        Path root = Files.createTempDirectory("workspace-initializer");
        Path template = root.resolve("WorkspaceTemplate");
        Path workspace = root.resolve("Workspace");

        Files.createDirectories(template.resolve("Configuration"));
        Files.createDirectories(template.resolve("Projects").resolve("Tutorial"));
        Files.createDirectories(template.resolve("Shared"));

        Files.writeString(
            template.resolve("Configuration").resolve("ExplorerConfig.properties"),
            "explorer=true"
        );

        Files.writeString(
            template.resolve("Projects").resolve("Tutorial").resolve("Readme.txt"),
            "tutorial"
        );

        Files.writeString(template.resolve("Shared").resolve(".gitkeep"), "");

        WorkspaceInitializer.initialize(template, workspace);

        assertThat(workspace.resolve("Configuration").resolve("ExplorerConfig.properties"))
            .hasContent("explorer=true");

        assertThat(workspace.resolve("Projects").resolve("Tutorial").resolve("Readme.txt"))
            .hasContent("tutorial");

        assertThat(workspace.resolve("Shared").resolve(".gitkeep")).exists();
    }

    @Test
    public void preservesExistingWorkspaceFiles() throws Exception {
        Path root = Files.createTempDirectory("workspace-preservation");
        Path template = root.resolve("WorkspaceTemplate");
        Path workspace = root.resolve("Workspace");

        Path templateSettings = template
            .resolve("Configuration")
            .resolve("ExplorerConfig.properties");

        Path workspaceSettings = workspace
            .resolve("Configuration")
            .resolve("ExplorerConfig.properties");

        Files.createDirectories(templateSettings.getParent());
        Files.createDirectories(workspaceSettings.getParent());

        Files.writeString(templateSettings, "template-value");
        Files.writeString(workspaceSettings, "user-value");

        WorkspaceInitializer.initialize(template, workspace);

        assertThat(workspaceSettings).hasContent("user-value");
    }

    @Test
    public void copiesMissingPluginFiles() throws Exception {
        Path root = Files.createTempDirectory("workspace-plugin-copy");
        Path template = root.resolve("WorkspaceTemplate");
        Path workspace = root.resolve("Workspace");
        Path templatePlugin = template.resolve("plugins").resolve("example").resolve("plugin.jar");

        Files.createDirectories(templatePlugin.getParent());
        Files.writeString(templatePlugin, "template-plugin");

        WorkspaceInitializer.initialize(template, workspace);

        assertThat(workspace.resolve("plugins").resolve("example").resolve("plugin.jar"))
            .hasContent("template-plugin");
    }

    @Test
    public void preservesExistingPluginFiles() throws Exception {
        Path root = Files.createTempDirectory("workspace-plugin-preservation");
        Path template = root.resolve("WorkspaceTemplate");
        Path workspace = root.resolve("Workspace");
        Path templatePlugin = template.resolve("plugins").resolve("example").resolve("plugin.jar");
        Path workspacePlugin = workspace
            .resolve("plugins")
            .resolve("example")
            .resolve("plugin.jar");

        Files.createDirectories(templatePlugin.getParent());
        Files.createDirectories(workspacePlugin.getParent());
        Files.writeString(templatePlugin, "template-plugin");
        Files.writeString(workspacePlugin, "user-plugin");

        WorkspaceInitializer.initialize(template, workspace);

        assertThat(workspacePlugin).hasContent("user-plugin");
    }

    @Test
    public void doesNothingWhenTemplateIsMissing() throws Exception {
        Path root = Files.createTempDirectory("workspace-no-template");
        Path template = root.resolve("MissingTemplate");
        Path workspace = root.resolve("Workspace");

        WorkspaceInitializer.initialize(template, workspace);

        assertThat(workspace).doesNotExist();
    }

    @Test
    public void doesNotCreateUserDefined() throws Exception {
        Path root = Files.createTempDirectory("workspace-user-defined");
        Path template = root.resolve("WorkspaceTemplate");
        Path workspace = root.resolve("Workspace");

        Files.createDirectories(template.resolve("Configuration"));
        Files.createDirectories(template.resolve("Projects"));
        Files.createDirectories(template.resolve("Shared"));

        WorkspaceInitializer.initialize(template, workspace);

        assertThat(workspace.resolve("UserDefined")).doesNotExist();
    }
}

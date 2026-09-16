package com.ing.engine.plugin.loader;

import static org.assertj.core.api.Assertions.assertThat;

import com.ing.datalib.util.WorkspacePath;
import java.nio.file.Files;
import java.nio.file.Path;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class PluginLoaderTest {
    private String originalWorkspace;

    @BeforeMethod
    public void rememberWorkspaceProperty() {
        originalWorkspace = System.getProperty(WorkspacePath.WORKSPACE_PROPERTY);
    }

    @AfterMethod
    public void restoreWorkspaceProperty() {
        if (originalWorkspace == null) {
            System.clearProperty(WorkspacePath.WORKSPACE_PROPERTY);
        } else {
            System.setProperty(WorkspacePath.WORKSPACE_PROPERTY, originalWorkspace);
        }
    }

    @Test
    public void missingPluginDirectoryReturnsEmptyList() throws Exception {
        Path workspace = Files.createTempDirectory("plugin-loader-missing");
        System.setProperty(WorkspacePath.WORKSPACE_PROPERTY, workspace.toString());

        assertThat(PluginLoader.loadAllPluginsEntryClasses()).isEmpty();
        assertThat(workspace.resolve("plugins")).doesNotExist();
    }

    @Test
    public void emptyPluginDirectoryReturnsEmptyList() throws Exception {
        Path workspace = Files.createTempDirectory("plugin-loader-empty");
        Path plugins = Files.createDirectories(workspace.resolve("plugins"));
        System.setProperty(WorkspacePath.WORKSPACE_PROPERTY, workspace.toString());

        assertThat(WorkspacePath.getPluginsPath()).isEqualTo(plugins.toFile().getCanonicalPath());
        assertThat(PluginLoader.loadAllPluginsEntryClasses()).isEmpty();
    }
}

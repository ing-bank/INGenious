package com.ing.datalib.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.testng.annotations.Test;

public class WorkspacePreferenceTest {

    @Test
    public void missingPreferenceReturnsNull() throws Exception {
        Path home = Files.createTempDirectory("workspace-pref-missing");

        assertThat(WorkspacePreference.loadWorkspaceBase(home.toString())).isNull();
    }

    @Test
    public void savesAndLoadsBasePathWithSpaces() throws Exception {
        Path home = Files.createTempDirectory("workspace-pref-home");
        Path base = home.resolve("Selected Base With Spaces");

        WorkspacePreference.saveWorkspaceBase(home.toString(), base);

        assertThat(WorkspacePreference.loadWorkspaceBase(home.toString()))
            .isEqualTo(base.toAbsolutePath().normalize().toString());
    }

    @Test
    public void preservesUnrelatedGlobalSettings() throws Exception {
        Path home = Files.createTempDirectory("workspace-pref-preserve");
        Path config = WorkspacePreference.configPath(home.toString());
        Files.createDirectories(config.getParent());
        Files.writeString(config, "perf.k6.path=/tools/k6\n");

        WorkspacePreference.saveWorkspaceBase(home.toString(), home.resolve("Automation"));

        Properties properties = new Properties();
        try (var input = Files.newInputStream(config)) {
            properties.load(input);
        }

        assertThat(properties.getProperty("perf.k6.path")).isEqualTo("/tools/k6");
        assertThat(properties.getProperty(WorkspacePreference.WORKSPACE_BASE_KEY))
            .isEqualTo(home.resolve("Automation").toAbsolutePath().normalize().toString());
    }
}

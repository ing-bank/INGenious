package com.ing.datalib.settings;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests for ReportPortalSettings — defaults and save/load.
 */
public class ReportPortalSettingsTest {

    private Path tempDir;

    @BeforeMethod
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("rp-settings-test");
    }

    @AfterMethod
    public void tearDown() throws IOException {
        Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    @Test
    public void testDefaultsLoaded() {
        ReportPortalSettings rp = new ReportPortalSettings(tempDir.toString());
        assertThat(rp.getProperty("rp.endpoint")).isEqualTo("http://localhost:8080");
        assertThat(rp.getProperty("rp.uuid")).isEmpty();
        assertThat(rp.getProperty("rp.project")).isEmpty();
    }

    @Test
    public void testSaveAndReload() {
        ReportPortalSettings rp = new ReportPortalSettings(tempDir.toString());
        rp.setProperty("rp.project", "my-project");
        rp.save();

        ReportPortalSettings reloaded = new ReportPortalSettings(tempDir.toString());
        assertThat(reloaded.getProperty("rp.project")).isEqualTo("my-project");
    }

    @Test
    public void testSize() {
        ReportPortalSettings rp = new ReportPortalSettings(tempDir.toString());
        assertThat(rp.size()).isEqualTo(3);
    }
}

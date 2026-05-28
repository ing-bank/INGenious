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
 * Tests for ExtentReportSettings — defaults and save/load.
 */
public class ExtentReportSettingsTest {

    private Path tempDir;

    @BeforeMethod
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("extent-settings-test");
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
        ExtentReportSettings es = new ExtentReportSettings(tempDir.toString());
        assertThat(es.getProperty("HTML-Theme")).isEqualTo("Dark");
    }

    @Test
    public void testSaveAndReload() {
        ExtentReportSettings es = new ExtentReportSettings(tempDir.toString());
        es.setProperty("HTML-Theme", "Light");
        es.save();

        ExtentReportSettings reloaded = new ExtentReportSettings(tempDir.toString());
        assertThat(reloaded.getProperty("HTML-Theme")).isEqualTo("Light");
    }

    @Test
    public void testSize() {
        ExtentReportSettings es = new ExtentReportSettings(tempDir.toString());
        assertThat(es.size()).isEqualTo(1);
    }
}

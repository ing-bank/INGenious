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
 * Tests for UserDefinedSettings — minimal AbstractPropSettings subclass.
 */
public class UserDefinedSettingsTest {

    private Path tempDir;

    @BeforeMethod
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("userdefined-test");
    }

    @AfterMethod
    public void tearDown() throws IOException {
        Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    @Test
    public void testInitiallyEmpty() {
        UserDefinedSettings uds = new UserDefinedSettings(tempDir.toString());
        assertThat(uds).isEmpty();
    }

    @Test
    public void testSetAndGet() {
        UserDefinedSettings uds = new UserDefinedSettings(tempDir.toString());
        uds.setProperty("custom.key", "custom.value");
        assertThat(uds.getProperty("custom.key")).isEqualTo("custom.value");
    }

    @Test
    public void testSaveAndReload() {
        UserDefinedSettings uds = new UserDefinedSettings(tempDir.toString());
        uds.setProperty("prop1", "val1");
        uds.setProperty("prop2", "val2");
        uds.save();

        UserDefinedSettings reloaded = new UserDefinedSettings(tempDir.toString());
        assertThat(reloaded.getProperty("prop1")).isEqualTo("val1");
        assertThat(reloaded.getProperty("prop2")).isEqualTo("val2");
    }

    @Test
    public void testLocationFormat() {
        UserDefinedSettings uds = new UserDefinedSettings(tempDir.toString());
        assertThat(uds.getLocation()).endsWith("userDefinedSettings.Properties");
    }
}

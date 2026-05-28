package com.ing.datalib.settings;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Properties;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests for AbstractPropSettings — properties-file-backed settings base class.
 */
public class AbstractPropSettingsTest {

    private Path tempDir;

    @BeforeMethod
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("abstract-prop-test");
    }

    @AfterMethod
    public void tearDown() throws IOException {
        Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    @Test
    public void testConstructorNoFile() {
        // No file exists — should create empty properties
        AbstractPropSettings settings = new AbstractPropSettings(tempDir.toString(), "NoSuchSettings");
        assertThat(settings).isEmpty();
    }

    @Test
    public void testGetLocationFormat() {
        AbstractPropSettings settings = new AbstractPropSettings(tempDir.toString(), "TestSettings");
        String expected = tempDir.toString() + File.separator + "TestSettings.Properties";
        assertThat(settings.getLocation()).isEqualTo(expected);
    }

    @Test
    public void testSaveAndReload() {
        AbstractPropSettings settings = new AbstractPropSettings(tempDir.toString(), "SaveTest");
        settings.setProperty("myKey", "myValue");
        settings.save();

        // Reload from same location
        AbstractPropSettings reloaded = new AbstractPropSettings(tempDir.toString(), "SaveTest");
        assertThat(reloaded.getProperty("myKey")).isEqualTo("myValue");
    }

    @Test
    public void testSetFromProperties() {
        AbstractPropSettings settings = new AbstractPropSettings(tempDir.toString(), "SetTest");
        settings.setProperty("old", "value");

        Properties newProps = new Properties();
        newProps.setProperty("new1", "a");
        newProps.setProperty("new2", "b");
        settings.set(newProps);

        assertThat(settings.getProperty("new1")).isEqualTo("a");
        assertThat(settings.getProperty("new2")).isEqualTo("b");
        assertThat(settings.getProperty("old")).isNull();
    }

    @Test
    public void testSetLocationChangesPath() {
        AbstractPropSettings settings = new AbstractPropSettings(tempDir.toString(), "LocTest");
        String newLoc = tempDir.resolve("newdir").toString();
        settings.setLocation(newLoc);
        assertThat(settings.getLocation()).startsWith(newLoc);
    }

    @Test
    public void testSetNameChangesFilename() {
        AbstractPropSettings settings = new AbstractPropSettings(tempDir.toString(), "OldName");
        settings.setName("NewName");
        assertThat(settings.getLocation()).contains("NewName.Properties");
    }

    @Test
    public void testSaveCreatesFile() {
        AbstractPropSettings settings = new AbstractPropSettings(tempDir.toString(), "CreateFile");
        settings.setProperty("k", "v");
        settings.save();

        File saved = new File(settings.getLocation());
        assertThat(saved).exists();
    }

    @Test
    public void testMultiplePropertiesSaveLoad() {
        AbstractPropSettings settings = new AbstractPropSettings(tempDir.toString(), "Multi");
        settings.setProperty("a", "1");
        settings.setProperty("b", "2");
        settings.setProperty("c", "3");
        settings.save();

        AbstractPropSettings reloaded = new AbstractPropSettings(tempDir.toString(), "Multi");
        assertThat(reloaded.size()).isEqualTo(3);
        assertThat(reloaded.getProperty("a")).isEqualTo("1");
        assertThat(reloaded.getProperty("b")).isEqualTo("2");
        assertThat(reloaded.getProperty("c")).isEqualTo("3");
    }
}

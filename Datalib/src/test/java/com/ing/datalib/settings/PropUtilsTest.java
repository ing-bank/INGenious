package com.ing.datalib.settings;

import static org.assertj.core.api.Assertions.assertThat;

import com.ing.datalib.util.data.LinkedProperties;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests for PropUtils — load/save of properties files.
 */
public class PropUtilsTest {
    private Path tempDir;

    @BeforeMethod
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("proputils-test");
    }

    @AfterMethod
    public void tearDown() throws IOException {
        Files
            .walk(tempDir)
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
    }

    @Test
    public void testLoadFromFile() throws IOException {
        File propFile = tempDir.resolve("test.properties").toFile();
        try (FileWriter fw = new FileWriter(propFile)) {
            fw.write("key1=value1\n");
            fw.write("key2=value2\n");
        }

        LinkedProperties props = PropUtils.load(propFile);

        assertThat(props.getProperty("key1")).isEqualTo("value1");
        assertThat(props.getProperty("key2")).isEqualTo("value2");
    }

    @Test
    public void testLoadIntoExistingProperties() throws IOException {
        File propFile = tempDir.resolve("test.properties").toFile();
        try (FileWriter fw = new FileWriter(propFile)) {
            fw.write("newKey=newValue\n");
        }

        LinkedProperties existing = new LinkedProperties();
        existing.put("existingKey", "existingValue");
        PropUtils.load(existing, propFile);

        assertThat(existing.getProperty("existingKey")).isEqualTo("existingValue");
        assertThat(existing.getProperty("newKey")).isEqualTo("newValue");
    }

    @Test
    public void testSaveCreatesFile() {
        String filename = tempDir.resolve("output.properties").toString();
        LinkedProperties props = new LinkedProperties();
        props.setProperty("a", "1");
        props.setProperty("b", "2");

        PropUtils.save(props, filename);

        File saved = new File(filename);
        assertThat(saved).exists();
    }

    @Test
    public void testSaveCreatesParentDirs() {
        String filename = tempDir.resolve("sub/dir/output.properties").toString();
        LinkedProperties props = new LinkedProperties();
        props.setProperty("x", "y");

        PropUtils.save(props, filename);

        assertThat(new File(filename)).exists();
    }

    @Test
    public void testSaveAndLoadRoundTrip() {
        String filename = tempDir.resolve("roundtrip.properties").toString();
        LinkedProperties original = new LinkedProperties();
        original.setProperty("alpha", "one");
        original.setProperty("beta", "two");
        original.setProperty("gamma", "three");

        PropUtils.save(original, filename);
        LinkedProperties loaded = PropUtils.load(new File(filename));

        assertThat(loaded.getProperty("alpha")).isEqualTo("one");
        assertThat(loaded.getProperty("beta")).isEqualTo("two");
        assertThat(loaded.getProperty("gamma")).isEqualTo("three");
    }

    @Test
    public void testSaveEscapesSpecialCharacters() throws IOException {
        String filename = tempDir.resolve("escaped.properties").toString();
        LinkedProperties props = new LinkedProperties();
        props.setProperty("path", "C:\\Users\\test");

        PropUtils.save(props, filename);

        String content = new String(Files.readAllBytes(new File(filename).toPath()));
        // Backslash should be escaped
        assertThat(content).contains("\\\\");
    }

    @Test
    public void testLoadNonExistentFileHandlesGracefully() {
        File missing = tempDir.resolve("missing.properties").toFile();
        LinkedProperties props = PropUtils.load(missing);
        // Should return empty properties (no crash)
        assertThat(props).isEmpty();
    }

    @Test
    public void testSaveEmptyProperties() {
        String filename = tempDir.resolve("empty.properties").toString();
        LinkedProperties props = new LinkedProperties();

        PropUtils.save(props, filename);

        assertThat(new File(filename)).exists();
        LinkedProperties loaded = PropUtils.load(new File(filename));
        assertThat(loaded).isEmpty();
    }

    @Test
    public void testSaveAndLoadPropertiesWithColon() {
        String filename = tempDir.resolve("colon.properties").toString();
        LinkedProperties original = new LinkedProperties();
        original.setProperty("appium:deviceName", "iPhone 14");
        original.setProperty("appium:platformName", "iOS");
        original.setProperty("normal", "value");

        PropUtils.save(original, filename);
        LinkedProperties loaded = PropUtils.load(new File(filename));

        assertThat(loaded.getProperty("appium:deviceName")).isEqualTo("iPhone 14");
        assertThat(loaded.getProperty("appium:platformName")).isEqualTo("iOS");
        assertThat(loaded.getProperty("normal")).isEqualTo("value");
    }

    @Test
    public void testSaveAndLoadPropertiesWithMixedSpecialChars() {
        String filename = tempDir.resolve("mixed.properties").toString();
        LinkedProperties original = new LinkedProperties();
        original.setProperty("appium:options", "value with spaces");
        original.setProperty("key with space", "value");
        original.setProperty("path", "C:\\Users\\test");
        original.setProperty("appium:automationName", "XCUITest");

        PropUtils.save(original, filename);
        LinkedProperties loaded = PropUtils.load(new File(filename));

        assertThat(loaded.getProperty("appium:options")).isEqualTo("value with spaces");
        assertThat(loaded.getProperty("key with space")).isEqualTo("value");
        assertThat(loaded.getProperty("path")).isEqualTo("C:\\Users\\test");
        assertThat(loaded.getProperty("appium:automationName")).isEqualTo("XCUITest");
    }
}

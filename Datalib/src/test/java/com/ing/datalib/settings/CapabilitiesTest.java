package com.ing.datalib.settings;

import static org.assertj.core.api.Assertions.assertThat;
import com.ing.datalib.util.data.LinkedProperties;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests for Capabilities — browser capability folder management.
 */
public class CapabilitiesTest {

    private Path tempDir;

    @BeforeMethod
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("capabilities-test");
    }

    @AfterMethod
    public void tearDown() throws IOException {
        Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    @Test
    public void testConstructorCreatesCapabilitiesFolder() {
        Capabilities caps = new Capabilities(tempDir.toString());
        File capsDir = new File(tempDir.toString() + File.separator + "Capabilities");
        assertThat(capsDir).exists().isDirectory();
    }

    @Test
    public void testDefaultBrowsersCreated() {
        Capabilities caps = new Capabilities(tempDir.toString());
        Map<String, LinkedProperties> browsers = caps.getBrowserCapabilties();

        assertThat(browsers).containsKeys("Chromium", "WebKit", "Firefox");
    }

    @Test
    public void testDefaultChromiumProperties() {
        Capabilities caps = new Capabilities(tempDir.toString());
        LinkedProperties chromium = caps.getCapabiltiesFor("Chromium");

        assertThat(chromium.getProperty("setHeadless")).isEqualTo("false");
        assertThat(chromium.getProperty("setTimeout")).isEqualTo("30000");
        assertThat(chromium.getProperty("setChannel")).isNotNull();
        assertThat(chromium.getProperty("setChromiumSandbox")).isNotNull();
    }

    @Test
    public void testDefaultFirefoxProperties() {
        Capabilities caps = new Capabilities(tempDir.toString());
        LinkedProperties firefox = caps.getCapabiltiesFor("Firefox");

        assertThat(firefox.getProperty("setHeadless")).isEqualTo("false");
        assertThat(firefox.getProperty("setTimeout")).isEqualTo("30000");
    }

    @Test
    public void testAddCapability() {
        Capabilities caps = new Capabilities(tempDir.toString());
        LinkedProperties custom = new LinkedProperties();
        custom.setProperty("key1", "val1");
        caps.addCapability("Custom", custom);

        assertThat(caps.getCapabiltiesFor("Custom")).isNotNull();
        assertThat(caps.getCapabiltiesFor("Custom").getProperty("key1")).isEqualTo("val1");
    }

    @Test
    public void testAddCapabilityEmpty() {
        Capabilities caps = new Capabilities(tempDir.toString());
        caps.addCapability("EmptyBrowser");

        assertThat(caps.getBrowserCapabilties()).containsKey("EmptyBrowser");
    }

    @Test
    public void testAddCapabilityPersistsToFile() {
        Capabilities caps = new Capabilities(tempDir.toString());
        LinkedProperties custom = new LinkedProperties();
        custom.setProperty("testKey", "testValue");
        caps.addCapability("Persisted", custom);

        File persisted = new File(caps.getCapLocation("Persisted"));
        assertThat(persisted).exists();
    }

    @Test
    public void testDeleteCapability() {
        Capabilities caps = new Capabilities(tempDir.toString());
        caps.addCapability("ToDelete", new LinkedProperties());
        caps.delete("ToDelete");

        assertThat(caps.getBrowserCapabilties()).doesNotContainKey("ToDelete");
        assertThat(new File(caps.getCapLocation("ToDelete"))).doesNotExist();
    }

    @Test
    public void testDeleteNonExistent() {
        Capabilities caps = new Capabilities(tempDir.toString());
        int sizeBefore = caps.getBrowserCapabilties().size();
        caps.delete("NonExistent");
        assertThat(caps.getBrowserCapabilties()).hasSize(sizeBefore);
    }

    @Test
    public void testRenameCapability() {
        Capabilities caps = new Capabilities(tempDir.toString());
        LinkedProperties props = new LinkedProperties();
        props.setProperty("rk", "rv");
        caps.addCapability("OldName", props);

        Boolean result = caps.rename("OldName", "NewName");

        assertThat(result).isTrue();
        assertThat(caps.getBrowserCapabilties()).containsKey("NewName");
        assertThat(caps.getBrowserCapabilties()).doesNotContainKey("OldName");
    }

    @Test
    public void testRenameToExistingNameFails() {
        Capabilities caps = new Capabilities(tempDir.toString());
        Boolean result = caps.rename("Chromium", "Firefox");
        assertThat(result).isFalse();
    }

    @Test
    public void testUpdateCapability() {
        Capabilities caps = new Capabilities(tempDir.toString());
        caps.updateCapabiltyFor("Chromium", "setHeadless", "true");
        assertThat(caps.getCapabiltiesFor("Chromium").getProperty("setHeadless")).isEqualTo("true");
    }

    @Test
    public void testSaveSpecificCapability() {
        Capabilities caps = new Capabilities(tempDir.toString());
        caps.getCapabiltiesFor("Chromium").setProperty("setHeadless", "true");
        caps.save("Chromium");

        Capabilities reloaded = new Capabilities(tempDir.toString());
        assertThat(reloaded.getCapabiltiesFor("Chromium").getProperty("setHeadless")).isEqualTo("true");
    }

    @Test
    public void testSaveAllCapabilities() {
        Capabilities caps = new Capabilities(tempDir.toString());
        caps.addCapability("Extra", new LinkedProperties());
        caps.save();

        Capabilities reloaded = new Capabilities(tempDir.toString());
        assertThat(reloaded.getBrowserCapabilties()).containsKey("Extra");
    }

    @Test
    public void testGetLocation() {
        Capabilities caps = new Capabilities(tempDir.toString());
        assertThat(caps.getLocation()).isEqualTo(tempDir.toString() + File.separator + "Capabilities");
    }

    @Test
    public void testGetCapLocation() {
        Capabilities caps = new Capabilities(tempDir.toString());
        String expected = tempDir.toString() + File.separator + "Capabilities" + File.separator + "Test.properties";
        assertThat(caps.getCapLocation("Test")).isEqualTo(expected);
    }

    @Test
    public void testAddDefaultAppiumCapability() {
        Capabilities caps = new Capabilities(tempDir.toString());
        caps.addDefaultAppiumCapability("Pixel5", "abc123", "com.app", "com.app.Main");

        LinkedProperties appium = caps.getCapabiltiesFor("Pixel5");
        assertThat(appium).isNotNull();
        assertThat(appium.getProperty("platformName")).isEqualTo("Android");
        assertThat(appium.getProperty("deviceName")).isEqualTo("Pixel5");
        assertThat(appium.getProperty("udid")).isEqualTo("abc123");
    }

    @Test
    public void testAddDefaultAppiumCapabilityNoBrowser() {
        Capabilities caps = new Capabilities(tempDir.toString());
        caps.addDefaultAppiumCapability("Device1");

        LinkedProperties appium = caps.getCapabiltiesFor("Device1");
        assertThat(appium.getProperty("browserName")).isEqualTo("chrome");
    }
}

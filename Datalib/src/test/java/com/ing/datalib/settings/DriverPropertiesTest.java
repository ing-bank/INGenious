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
 * Tests for DriverProperties — API configuration folder management.
 * Note: DriverProperties uses static fields, so tests must run sequentially.
 */
public class DriverPropertiesTest {

    private Path tempDir;

    @BeforeMethod
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("driverprops-test");
    }

    @AfterMethod
    public void tearDown() throws IOException {
        Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    @Test
    public void testConstructorCreatesAPIFolder() {
        DriverProperties dp = new DriverProperties(tempDir.toString());
        File apiDir = new File(tempDir.toString() + File.separator + "API");
        assertThat(apiDir).exists().isDirectory();
    }

    @Test
    public void testDefaultAPIConfigCreated() {
        DriverProperties dp = new DriverProperties(tempDir.toString());
        assertThat(dp.getAPIList()).contains("default");
    }

    @Test
    public void testDefaultAPIProperties() {
        DriverProperties dp = new DriverProperties(tempDir.toString());
        Properties defaultProps = dp.getAPIPropertiesFor("default");

        assertThat(defaultProps).isNotNull();
        assertThat(defaultProps.getProperty("api.alias")).isEqualTo("default");
        assertThat(defaultProps.getProperty("useProxy")).isEqualTo("false");
        assertThat(defaultProps.getProperty("sslCertificateVerification")).isEqualTo("false");
    }

    @Test
    public void testAddAPIProperty() {
        DriverProperties dp = new DriverProperties(tempDir.toString());
        dp.addAPIProperty("testAPI");

        assertThat(dp.getAPIList()).contains("testAPI");
        Properties testProps = dp.getAPIPropertiesFor("testAPI");
        assertThat(testProps.getProperty("api.alias")).isEqualTo("testAPI");
    }

    @Test
    public void testAddAPIWithProperties() {
        DriverProperties dp = new DriverProperties(tempDir.toString());
        Properties props = new Properties();
        props.setProperty("custom", "value");
        dp.addAPI("customAPI", props);

        assertThat(dp.getAPIPropertiesFor("customAPI").getProperty("custom")).isEqualTo("value");
    }

    @Test
    public void testDeleteAPI() {
        DriverProperties dp = new DriverProperties(tempDir.toString());
        dp.addAPIProperty("toDelete");
        dp.delete("toDelete");

        assertThat(dp.doesAPIconfigExist("toDelete")).isFalse();
    }

    @Test
    public void testDeleteRemovesFile() {
        DriverProperties dp = new DriverProperties(tempDir.toString());
        dp.addAPIProperty("toDelete2");
        String filePath = dp.getAPILocation("toDelete2");
        assertThat(new File(filePath)).exists();

        dp.delete("toDelete2");
        assertThat(new File(filePath)).doesNotExist();
    }

    @Test
    public void testDeleteNonExistent() {
        DriverProperties dp = new DriverProperties(tempDir.toString());
        int sizeBefore = dp.getAPIList().size();
        dp.delete("nonExistent");
        // List gets reloaded in getAPIList, just verify no crash
        assertThat(dp.getAPIList()).isNotNull();
    }

    @Test
    public void testDoesAPIconfigExist() {
        DriverProperties dp = new DriverProperties(tempDir.toString());
        assertThat(dp.doesAPIconfigExist("default")).isTrue();
        assertThat(dp.doesAPIconfigExist("noSuch")).isFalse();
    }

    @Test
    public void testSaveAll() {
        DriverProperties dp = new DriverProperties(tempDir.toString());
        dp.addAPIProperty("api1");
        dp.addAPIProperty("api2");
        dp.save();

        assertThat(new File(dp.getAPILocation("api1"))).exists();
        assertThat(new File(dp.getAPILocation("api2"))).exists();
    }

    @Test
    public void testGetLocation() {
        DriverProperties dp = new DriverProperties(tempDir.toString());
        assertThat(DriverProperties.getLocation()).isEqualTo(tempDir.toString() + File.separator + "API");
    }

    @Test
    public void testGetAPILocation() {
        DriverProperties dp = new DriverProperties(tempDir.toString());
        String expected = DriverProperties.getLocation() + File.separator + "test.properties";
        assertThat(dp.getAPILocation("test")).isEqualTo(expected);
    }

    @Test
    public void testPersistenceAcrossInstances() {
        DriverProperties dp1 = new DriverProperties(tempDir.toString());
        Properties props = new Properties();
        props.setProperty("persisted", "yes");
        dp1.addAPI("persist", props);

        DriverProperties dp2 = new DriverProperties(tempDir.toString());
        assertThat(dp2.getAPIList()).contains("persist");
        assertThat(dp2.getAPIPropertiesFor("persist").getProperty("persisted")).isEqualTo("yes");
    }

    @Test
    public void testAddAPIName() {
        DriverProperties dp = new DriverProperties(tempDir.toString());
        dp.addAPIName("nameOnly");
        // addAPIName adds to static in-memory list, but getAPIList() calls load()
        // which clears and reloads from disk. Verify API is not in config map.
        assertThat(dp.getAPIPropertiesFor("nameOnly")).isNull();
    }

    @Test
    public void testSetCurrLoadedAndGetProxy() {
        DriverProperties dp = new DriverProperties(tempDir.toString());
        dp.setCurrLoadedAPIConfig("default");

        assertThat(dp.useProxy()).isFalse();
        assertThat(dp.getUseProxy()).isEqualTo("false");
        assertThat(dp.getProxyHost()).isEmpty();
        assertThat(dp.getProxyPort()).isEmpty();
    }

    @Test
    public void testSSLProperties() {
        DriverProperties dp = new DriverProperties(tempDir.toString());
        dp.setCurrLoadedAPIConfig("default");

        assertThat(dp.sslCertificateVerification()).isFalse();
        assertThat(dp.selfSigned()).isFalse();
        assertThat(dp.getKeyStorePath()).isEmpty();
        assertThat(dp.getKeyStorePassword()).isEmpty();
    }

    @Test
    public void testDriverPaths() {
        DriverProperties dp = new DriverProperties(tempDir.toString());

        dp.setChromeDriverPath("/usr/bin/chrome");
        assertThat(dp.getChromeDriverPath()).isEqualTo("/usr/bin/chrome");

        dp.setGeckcoDriverPath("/usr/bin/gecko");
        assertThat(dp.getGeckcoDriverPath()).isEqualTo("/usr/bin/gecko");

        dp.setFirefoxBinaryPath("/usr/bin/firefox");
        assertThat(dp.getFirefoxBinaryPath()).isEqualTo("/usr/bin/firefox");
    }

    @Test
    public void testIEAndEdgeDriverPaths() {
        DriverProperties dp = new DriverProperties(tempDir.toString());

        dp.setIEDriverPath("C:\\ie.exe");
        assertThat(dp.getIEDriverPath()).isEqualTo("C:\\ie.exe");

        dp.setEdgeDriverPath("C:\\edge.exe");
        assertThat(dp.getEdgeDriverPath()).isEqualTo("C:\\edge.exe");
    }

    @Test
    public void testDefaultDriverPaths() {
        DriverProperties dp = new DriverProperties(tempDir.toString());
        // Should return defaults since not set
        assertThat(dp.getIEDriverPath()).contains("IEDriverServer");
        assertThat(dp.getEdgeDriverPath()).contains("MicrosoftWebDriver");
    }
}

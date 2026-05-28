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
 * Tests for LambdaTestCaps — default values and save/load cycle.
 */
public class LambdaTestCapsTest {

    private Path tempDir;

    @BeforeMethod
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("lambdatest-test");
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
        LambdaTestCaps caps = new LambdaTestCaps(tempDir.toString());
        assertThat(caps.getProperty("video")).isEqualTo("true");
        assertThat(caps.getProperty("console")).isEqualTo("true");
        assertThat(caps.getProperty("network")).isEqualTo("true");
        assertThat(caps.getProperty("resolution")).isEqualTo("1920x1080");
        assertThat(caps.getProperty("visual")).isEqualTo("true");
        assertThat(caps.getProperty("tunnel")).isEqualTo("false");
        assertThat(caps.getProperty("idleTimeout")).isEqualTo("300");
    }

    @Test
    public void testDefaultUserEmpty() {
        LambdaTestCaps caps = new LambdaTestCaps(tempDir.toString());
        assertThat(caps.getProperty("user")).isEmpty();
        assertThat(caps.getProperty("accessKey")).isEmpty();
    }

    @Test
    public void testUseSpecificBundleVersionDefault() {
        LambdaTestCaps caps = new LambdaTestCaps(tempDir.toString());
        assertThat(caps.getProperty("useSpecificBundleVersion")).isEqualTo("false");
    }

    @Test
    public void testSaveAndReload() {
        LambdaTestCaps caps = new LambdaTestCaps(tempDir.toString());
        caps.setProperty("user", "testUser");
        caps.setProperty("accessKey", "abc123");
        caps.save();

        LambdaTestCaps reloaded = new LambdaTestCaps(tempDir.toString());
        assertThat(reloaded.getProperty("user")).isEqualTo("testUser");
        assertThat(reloaded.getProperty("accessKey")).isEqualTo("abc123");
    }
}

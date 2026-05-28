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
 * Tests for KafkaSSLConfigurations — defaults and save/load.
 */
public class KafkaSSLConfigurationsTest {

    private Path tempDir;

    @BeforeMethod
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("kafkassl-test");
    }

    @AfterMethod
    public void tearDown() throws IOException {
        Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    @Test
    public void testProducerDefaults() {
        KafkaSSLConfigurations ks = new KafkaSSLConfigurations(tempDir.toString());
        assertThat(ks.getProperty("Producer_ssl_Enabled")).isEqualTo("false");
        assertThat(ks.getProperty("Producer_Keystore_Location")).isEmpty();
        assertThat(ks.getProperty("Producer_Keystore_Password")).isEmpty();
        assertThat(ks.getProperty("Producer_Key_Password")).isEmpty();
    }

    @Test
    public void testConsumerDefaults() {
        KafkaSSLConfigurations ks = new KafkaSSLConfigurations(tempDir.toString());
        assertThat(ks.getProperty("Consumer_ssl_Enabled")).isEqualTo("false");
        assertThat(ks.getProperty("Consumer_Keystore_Location")).isEmpty();
        assertThat(ks.getProperty("Consumer_Keystore_Password")).isEmpty();
        assertThat(ks.getProperty("Consumer_Key_Password")).isEmpty();
    }

    @Test
    public void testSaveAndReload() {
        KafkaSSLConfigurations ks = new KafkaSSLConfigurations(tempDir.toString());
        ks.setProperty("Producer_ssl_Enabled", "true");
        ks.save();

        KafkaSSLConfigurations reloaded = new KafkaSSLConfigurations(tempDir.toString());
        assertThat(reloaded.getProperty("Producer_ssl_Enabled")).isEqualTo("true");
    }

    @Test
    public void testSize() {
        KafkaSSLConfigurations ks = new KafkaSSLConfigurations(tempDir.toString());
        assertThat(ks.size()).isEqualTo(8);
    }
}

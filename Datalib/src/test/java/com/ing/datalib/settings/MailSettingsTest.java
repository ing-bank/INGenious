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
 * Tests for MailSettings — default values and save/load cycle.
 */
public class MailSettingsTest {

    private Path tempDir;

    @BeforeMethod
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("mailsettings-test");
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
        MailSettings ms = new MailSettings(tempDir.toString());
        assertThat(ms.getProperty("mail.smtp.auth")).isEqualTo("true");
        assertThat(ms.getProperty("mail.smtp.starttls.enable")).isEqualTo("true");
        assertThat(ms.getProperty("mail.smtp.starttls.required")).isEqualTo("true");
        assertThat(ms.getProperty("attach.reports")).isEqualTo("true");
        assertThat(ms.getProperty("mail.debug")).isEqualTo("false");
    }

    @Test
    public void testDefaultSubject() {
        MailSettings ms = new MailSettings(tempDir.toString());
        assertThat(ms.getProperty("msg.subject")).contains("Execution Report");
    }

    @Test
    public void testDefaultConnectionTimeout() {
        MailSettings ms = new MailSettings(tempDir.toString());
        assertThat(ms.getProperty("mail.smtp.connectiontimeout")).isEqualTo("10000");
    }

    @Test
    public void testSaveAndReload() {
        MailSettings ms = new MailSettings(tempDir.toString());
        ms.setProperty("mail.smtp.host", "smtp.example.com");
        ms.save();

        MailSettings reloaded = new MailSettings(tempDir.toString());
        assertThat(reloaded.getProperty("mail.smtp.host")).isEqualTo("smtp.example.com");
    }

    @Test
    public void testAllDefaultKeysPresent() {
        MailSettings ms = new MailSettings(tempDir.toString());
        assertThat(ms.size()).isGreaterThanOrEqualTo(14);
    }
}

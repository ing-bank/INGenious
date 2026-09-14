package com.ing.datalib.settings;

import static org.assertj.core.api.Assertions.assertThat;

import com.ing.datalib.util.data.LinkedProperties;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Tests for {@link SapConnections} — the Settings/SAP/&lt;alias&gt;.properties store. */
public class SapConnectionsTest {
    private Path tempDir;

    @BeforeMethod
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("sapconn-test");
    }

    @AfterMethod
    public void tearDown() throws IOException {
        Files
            .walk(tempDir)
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
    }

    private SapConnections store() {
        return new SapConnections(tempDir.toString());
    }

    @Test
    public void createsSapFolder() {
        store();
        assertThat(new File(tempDir.toString() + File.separator + "SAP")).isDirectory();
    }

    @Test
    public void addAndReadBack() {
        SapConnections s = store();
        s.addSap("QA");
        assertThat(s.getSapPropertiesFor("QA")).isNotNull();
        assertThat(new File(s.getSapLocation("QA"))).exists();

        SapConnections reloaded = store();
        assertThat(reloaded.getSapList()).containsExactly("QA");
    }

    @Test
    public void configFileIsNotAConnection() throws IOException {
        SapConnections s = store();
        Files.createFile(Path.of(s.getLocation(), "_config.properties"));
        assertThat(s.getSapList()).doesNotContain("_config");
    }

    @Test
    public void getOrCreateNeverReturnsNull() {
        SapConnections s = store();
        LinkedProperties p = s.getOrCreateSapPropertiesFor("Fresh");
        assertThat(p).isNotNull();
        p.update("connectionName", "QAS");
        assertThat(s.getSapPropertiesFor("Fresh").getProperty("connectionName")).isEqualTo("QAS");
    }

    @Test
    public void renameMovesFileAndReKeys() {
        SapConnections s = store();
        s.addSap("Old");
        assertThat(s.rename("Old", "New")).isTrue();
        assertThat(s.getSapList()).containsExactly("New");
        assertThat(new File(s.getSapLocation("Old"))).doesNotExist();
        assertThat(new File(s.getSapLocation("New"))).exists();
    }

    @Test
    public void renameRefusesOnCollision() {
        SapConnections s = store();
        s.addSap("A");
        s.addSap("B");
        assertThat(s.rename("A", "B")).isFalse();
    }

    @Test
    public void deleteRemovesFileAndEntry() {
        SapConnections s = store();
        s.addSap("Gone");
        s.delete("Gone");
        assertThat(s.getSapList()).doesNotContain("Gone");
        assertThat(new File(s.getSapLocation("Gone"))).doesNotExist();
    }

    @Test
    public void defaultTemplateHasExpectedKeys() {
        LinkedProperties p = store().defaultConnectionProperties();
        assertThat(p.getProperty("connectionName")).isNotNull();
        assertThat(p.getProperty("app")).contains("saplogon.exe");
        assertThat(p.getProperty("multiLogon")).isEqualTo("keepOthers");
    }
}

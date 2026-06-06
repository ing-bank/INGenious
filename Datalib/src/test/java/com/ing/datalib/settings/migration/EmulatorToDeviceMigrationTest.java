package com.ing.datalib.settings.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.ing.datalib.settings.Devices;
import com.ing.datalib.settings.Emulators;
import com.ing.datalib.settings.emulators.Device;
import com.ing.datalib.settings.emulators.Emulator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests for the one-time Emulators.json -> Devices.json migration.
 */
public class EmulatorToDeviceMigrationTest {

    private Path tempDir;

    @BeforeMethod
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("emu-mig-test");
    }

    @AfterMethod
    public void tearDown() throws IOException {
        if (tempDir != null) {
            Files.walk(tempDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    @Test
    public void migratesRemoteUrlEmulatorToDevice() {
        Emulators em = new Emulators(tempDir.toString());
        Devices dv = new Devices(tempDir.toString());
        em.addAppiumEmulator("Pixel5", "http://127.0.0.1:4723/");

        EmulatorToDeviceMigration.MigrationResult r = EmulatorToDeviceMigration.migrate(em, dv);

        assertThat(r.migrated).containsExactly("Pixel5");
        assertThat(em.getEmulator("Pixel5")).isNull();
        Device d = dv.getDevice("Pixel5");
        assertThat(d).isNotNull();
        assertThat(d.getRemoteUrl()).isEqualTo("http://127.0.0.1:4723/");
        assertThat(d.isLambdaTest()).isFalse();
    }

    @Test
    public void preservesSAPEmulator() {
        Emulators em = new Emulators(tempDir.toString());
        Devices dv = new Devices(tempDir.toString());
        em.addEmulator("SAP"); // Type defaults to "Emulator"

        EmulatorToDeviceMigration.migrate(em, dv);

        assertThat(em.getEmulator("SAP")).isNotNull();
        assertThat(dv.getDevice("SAP")).isNull();
    }

    @Test
    public void preservesLegacyNonRemoteUrlEntries() {
        Emulators em = new Emulators(tempDir.toString());
        Devices dv = new Devices(tempDir.toString());
        em.addEmulator("LegacyChrome"); // Type defaults to "Emulator"

        EmulatorToDeviceMigration.MigrationResult r = EmulatorToDeviceMigration.migrate(em, dv);

        assertThat(r.skippedLegacy).containsExactly("LegacyChrome");
        assertThat(em.getEmulator("LegacyChrome")).isNotNull();
        assertThat(dv.getDevice("LegacyChrome")).isNull();
    }

    @Test
    public void detectsLambdaTestUrl() {
        Emulators em = new Emulators(tempDir.toString());
        Devices dv = new Devices(tempDir.toString());
        em.addAppiumEmulator("LT_iPhone", "https://user:key@hub.lambdatest.com/wd/hub");

        EmulatorToDeviceMigration.migrate(em, dv);

        Device d = dv.getDevice("LT_iPhone");
        assertThat(d).isNotNull();
        assertThat(d.isLambdaTest()).isTrue();
    }

    @Test
    public void skipsWhenDeviceAlreadyExists() {
        Emulators em = new Emulators(tempDir.toString());
        Devices dv = new Devices(tempDir.toString());
        em.addAppiumEmulator("Shared", "http://from-emulator:4723/");
        dv.addDevice("Shared");
        dv.getDevice("Shared").setRemoteUrl("http://from-device:4723/");

        EmulatorToDeviceMigration.MigrationResult r = EmulatorToDeviceMigration.migrate(em, dv);

        assertThat(r.skippedExisting).containsExactly("Shared");
        assertThat(em.getEmulator("Shared")).isNull(); // dropped to avoid duplication
        assertThat(dv.getDevice("Shared").getRemoteUrl()).isEqualTo("http://from-device:4723/");
    }

    @Test
    public void isIdempotent() {
        Emulators em = new Emulators(tempDir.toString());
        Devices dv = new Devices(tempDir.toString());
        em.addAppiumEmulator("Pixel5", "http://127.0.0.1:4723/");

        EmulatorToDeviceMigration.migrate(em, dv);
        EmulatorToDeviceMigration.MigrationResult r2 = EmulatorToDeviceMigration.migrate(em, dv);

        assertThat(r2.changed()).isFalse();
        assertThat(dv.getDeviceNames()).containsExactly("Pixel5");
    }

    @Test
    public void persistsResultsToDisk() {
        Emulators em = new Emulators(tempDir.toString());
        Devices dv = new Devices(tempDir.toString());
        em.addAppiumEmulator("Pixel5", "http://127.0.0.1:4723/");

        EmulatorToDeviceMigration.migrate(em, dv);

        // Reload from disk
        Emulators em2 = new Emulators(tempDir.toString());
        Devices dv2 = new Devices(tempDir.toString());
        assertThat(em2.getEmulator("Pixel5")).isNull();
        assertThat(dv2.getDevice("Pixel5")).isNotNull();
    }

    @Test
    public void handlesEmptyStores() {
        Emulators em = new Emulators(tempDir.toString());
        Devices dv = new Devices(tempDir.toString());

        EmulatorToDeviceMigration.MigrationResult r = EmulatorToDeviceMigration.migrate(em, dv);

        assertThat(r.changed()).isFalse();
    }

    @Test
    public void migratesMultipleEntriesAtOnce() {
        Emulators em = new Emulators(tempDir.toString());
        Devices dv = new Devices(tempDir.toString());
        em.addEmulator("SAP");
        em.addAppiumEmulator("A", "http://127.0.0.1:4723/");
        em.addAppiumEmulator("B", "https://hub.lambdatest.com/wd/hub");
        em.addEmulator("LegacyZ");

        EmulatorToDeviceMigration.MigrationResult r = EmulatorToDeviceMigration.migrate(em, dv);

        assertThat(r.migrated).containsExactlyInAnyOrder("A", "B");
        assertThat(em.getEmulator("SAP")).isNotNull();
        assertThat(em.getEmulator("LegacyZ")).isNotNull();
        assertThat(em.getEmulator("A")).isNull();
        assertThat(em.getEmulator("B")).isNull();
        assertThat(dv.getDevice("A").isLambdaTest()).isFalse();
        assertThat(dv.getDevice("B").isLambdaTest()).isTrue();
    }

    @Test
    public void migrationSurvivesNullInputs() {
        EmulatorToDeviceMigration.MigrationResult r = EmulatorToDeviceMigration.migrate(null, null);
        assertThat(r.changed()).isFalse();
    }

    @Test
    public void preservesEmulatorWithMissingType() {
        Emulators em = new Emulators(tempDir.toString());
        Devices dv = new Devices(tempDir.toString());
        Emulator raw = new Emulator();
        raw.setName("NoType");
        em.getEmulators().add(raw);

        EmulatorToDeviceMigration.migrate(em, dv);

        assertThat(em.getEmulator("NoType")).isNotNull();
        assertThat(dv.getDevice("NoType")).isNull();
    }
}

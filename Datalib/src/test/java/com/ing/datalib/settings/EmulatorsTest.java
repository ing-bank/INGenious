package com.ing.datalib.settings;

import static org.assertj.core.api.Assertions.assertThat;
import com.ing.datalib.settings.emulators.Emulator;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests for Emulators — JSON-based emulator configuration management.
 */
public class EmulatorsTest {

    private Path tempDir;

    @BeforeMethod
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("emulators-test");
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
        Emulators em = new Emulators(tempDir.toString());
        assertThat(em.getEmulators()).isEmpty();
    }

    @Test
    public void testAddEmulator() {
        Emulators em = new Emulators(tempDir.toString());
        em.addEmulator("Pixel5");

        assertThat(em.getEmulators()).hasSize(1);
        assertThat(em.getEmulatorNames()).containsExactly("Pixel5");
    }

    @Test
    public void testAddEmulatorDefaults() {
        Emulators em = new Emulators(tempDir.toString());
        em.addEmulator("TestDevice");

        Emulator device = em.getEmulator("TestDevice");
        assertThat(device.getType()).isEqualTo("Emulator");
        assertThat(device.getDriver()).isEqualTo("Chrome");
    }

    @Test
    public void testAddDuplicateEmulator() {
        Emulators em = new Emulators(tempDir.toString());
        em.addEmulator("Same");
        em.addEmulator("Same");

        assertThat(em.getEmulators()).hasSize(1);
    }

    @Test
    public void testAddAppiumEmulator() {
        Emulators em = new Emulators(tempDir.toString());
        em.addAppiumEmulator("RemoteDevice", "http://localhost:4723");

        Emulator device = em.getEmulator("RemoteDevice");
        assertThat(device.getType()).isEqualTo("Remote URL");
        assertThat(device.getRemoteUrl()).isEqualTo("http://localhost:4723");
    }

    @Test
    public void testGetAppiumEmulatorNames() {
        Emulators em = new Emulators(tempDir.toString());
        em.addEmulator("Local");
        em.addAppiumEmulator("Remote", "http://appium:4723");

        List<String> appiumNames = em.getAppiumEmulatorNames();
        assertThat(appiumNames).containsExactly("Remote");
    }

    @Test
    public void testDeleteEmulator() {
        Emulators em = new Emulators(tempDir.toString());
        em.addEmulator("ToDelete");
        em.deleteEmulator("ToDelete");

        assertThat(em.getEmulators()).isEmpty();
    }

    @Test
    public void testDeleteNonExistent() {
        Emulators em = new Emulators(tempDir.toString());
        em.addEmulator("Keep");
        em.deleteEmulator("NoSuch");

        assertThat(em.getEmulators()).hasSize(1);
    }

    @Test
    public void testRenameEmulator() {
        Emulators em = new Emulators(tempDir.toString());
        em.addEmulator("OldName");

        Boolean result = em.renameEmulator("OldName", "NewName");

        assertThat(result).isTrue();
        assertThat(em.getEmulator("OldName")).isNull();
        assertThat(em.getEmulator("NewName")).isNotNull();
    }

    @Test
    public void testRenameToExistingFails() {
        Emulators em = new Emulators(tempDir.toString());
        em.addEmulator("First");
        em.addEmulator("Second");

        Boolean result = em.renameEmulator("First", "Second");
        assertThat(result).isFalse();
    }

    @Test
    public void testRenameNonExistentFails() {
        Emulators em = new Emulators(tempDir.toString());
        Boolean result = em.renameEmulator("NoSuch", "NewName");
        assertThat(result).isFalse();
    }

    @Test
    public void testSaveAndReload() {
        Emulators em = new Emulators(tempDir.toString());
        em.addEmulator("Device1");
        em.addAppiumEmulator("Device2", "http://appium:4723");
        em.save();

        Emulators reloaded = new Emulators(tempDir.toString());
        assertThat(reloaded.getEmulators()).hasSize(2);
        assertThat(reloaded.getEmulatorNames()).containsExactlyInAnyOrder("Device1", "Device2");
    }

    @Test
    public void testSaveCreatesFile() {
        Emulators em = new Emulators(tempDir.toString());
        em.addEmulator("AnyDevice");
        em.save();

        assertThat(new File(em.getLocation())).exists();
    }

    @Test
    public void testSaveCreatesParentDirectories() throws IOException {
        Path nested = tempDir.resolve("sub/dir");
        Emulators em = new Emulators(nested.toString());
        em.addEmulator("Test");
        em.save();

        assertThat(new File(em.getLocation())).exists();
    }

    @Test
    public void testGetLocation() {
        Emulators em = new Emulators(tempDir.toString());
        assertThat(em.getLocation()).isEqualTo(tempDir.toString() + File.separator + "Emulators.json");
    }

    @Test
    public void testReload() {
        Emulators em = new Emulators(tempDir.toString());
        em.addEmulator("Before");
        em.save();

        em.addEmulator("InMemoryOnly");
        assertThat(em.getEmulators()).hasSize(2);

        em.reload();
        assertThat(em.getEmulators()).hasSize(1);
        assertThat(em.getEmulatorNames()).containsExactly("Before");
    }

    @Test
    public void testGetNonExistentEmulator() {
        Emulators em = new Emulators(tempDir.toString());
        assertThat(em.getEmulator("NoSuch")).isNull();
    }

    @Test
    public void testDefaultEmulatorCap() {
        Emulators em = new Emulators(tempDir.toString());
        var props = em.defaultEmulatorCap();

        assertThat(props.getProperty("deviceName")).isEmpty();
        assertThat(props.getProperty("platformName")).isEmpty();
        assertThat(props.getProperty("platformVersion")).isEmpty();
        assertThat(props.getProperty("automationName")).isEmpty();
    }
}

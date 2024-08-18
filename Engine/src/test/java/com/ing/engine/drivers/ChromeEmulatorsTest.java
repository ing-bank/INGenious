
package com.ing.engine.drivers;

import com.ing.engine.support.DesktopApi;
import java.io.File;
import java.util.List;
import java.util.stream.Stream;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 *
 *
 */
public class ChromeEmulatorsTest {

    private static final boolean IS_TRAVIS_LINUX = isTravisLinux();

    @BeforeMethod
    public void before() {
        if (IS_TRAVIS_LINUX) {
            throw new SkipException("Skipped as running on Travis");
        }
    }

    /**
     * Test of getPrefLocation method, of class ChromeEmulators.
     */
    @Test(enabled = false)
    public void testGetPrefLocation() {
        System.out.println("getPrefLocation");
        File file = new File(ChromeEmulators.getPrefLocation(), "Preferences");
        if (!file.exists()) {
            System.out.println(file.getAbsolutePath());
            System.out.println("------------------------");
            Stream.of(file.listFiles())
                    .map(File::getAbsolutePath).forEach(System.out::println);
            fail("Unable to fine preference file");
        }
    }

    /**
     * Test of sync method, of class ChromeEmulators.
     */
    @Test(enabled = false)
    public void testSync() {
        System.out.println("sync");
        ChromeEmulators.sync();
        assertTrue(!ChromeEmulators.getEmulatorsList().isEmpty(), "EmulatorsList is Empty");

    }

    /**
     * Test of getEmulatorsList method, of class ChromeEmulators.
     */
    @Test(enabled = false)
    public void testGetEmulatorsList() {
        System.out.println("getEmulatorsList");
        List<String> result = ChromeEmulators.getEmulatorsList();
        assertTrue(Stream.of("Nexus 5", "Galaxy S5", "Nexus 6P", "iPhone 5", "iPhone 6 Plus")
                .allMatch(result::contains), "Some/all emulators missing in the EmulatorsList"
        );

    }

    private static boolean isTravisLinux() {
        return DesktopApi.getOs().isLinux()
                && Boolean.valueOf(System.getenv("TRAVIS"));
    }
}

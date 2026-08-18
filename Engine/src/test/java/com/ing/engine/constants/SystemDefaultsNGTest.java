package com.ing.engine.constants;

import static org.testng.Assert.assertTrue;

import java.util.regex.Pattern;
import org.testng.annotations.Test;

public class SystemDefaultsNGTest {

    /**
     * Test of getBuildVersion method, of class SystemDefaults.
     */
    @Test
    public void testGetBuildVersion() {
        System.out.println("getBuildVersion");
        String result = SystemDefaults.getBuildVersion();
        Pattern pattern = Pattern.compile(
            "^(?:(\\d+)\\.)?(?:(\\d+)\\.)?(\\*|\\d+)(?:-[0-9A-Za-z.-]+)?$"
        );
        assertTrue(
            pattern.matcher(result).matches(),
            "Unexpected Bundle-Version value: [" + result + "]"
        );
    }

    /**
     * Test of printSystemInfo method, of class SystemDefaults.
     */
    @Test
    public void testPrintSystemInfo() {
        System.out.println("printSystemInfo");
        SystemDefaults.printSystemInfo();
    }
}

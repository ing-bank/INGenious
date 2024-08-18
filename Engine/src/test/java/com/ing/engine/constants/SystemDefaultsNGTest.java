
package com.ing.engine.constants;

import java.util.regex.Pattern;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

public class SystemDefaultsNGTest {

    /**
     * Test of getBuildVersion method, of class SystemDefaults.
     */
    @Test
    public void testGetBuildVersion() {
        System.out.println("getBuildVersion");
        String result = SystemDefaults.getBuildVersion();
        Pattern pattern = Pattern.compile("^(?:(\\d+)\\.)?(?:(\\d+)\\.)?(\\*|\\d+)$");
        assertEquals(true, pattern.matcher(result).matches());
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

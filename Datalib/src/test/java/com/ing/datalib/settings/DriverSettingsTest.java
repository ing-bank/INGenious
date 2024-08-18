
package com.ing.datalib.settings;

import com.ing.datalib.settings.DriverSettings;
import java.lang.reflect.Method;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class DriverSettingsTest {

    private static boolean isWin;

    private DriverSettings ds;

    @BeforeClass
    public static void setUpClass() {
        isWin = System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    @BeforeMethod
    public void setUp(Method method) {
        ds = new DriverSettings("tmp");
        System.out.println("Running " + method.getName());
    }

    /**
     * Test of getGeckcoDriverPath method, of class DriverSettings.
     */
    @Test
    public void testGetGeckcoDriverPath() {
        String expResult;
        if (isWin) {
            expResult = "./lib/Drivers/geckodriver.exe";
        } else {
            expResult = "./lib/Drivers/geckodriver";
        }
        String result = ds.getGeckcoDriverPath();
        assertEquals(result, expResult);
    }

    /**
     * Test of getChromeDriverPath method, of class DriverSettings.
     */
    @Test
    public void testGetChromeDriverPath() {
        String expResult;
        if (isWin) {
            expResult = "./lib/Drivers/chromedriver.exe";
        } else {
            expResult = "./lib/Drivers/chromedriver";
        }
        String result = ds.getChromeDriverPath();
        assertEquals(result, expResult);
    }

    /**
     * Test of getIEDriverPath method, of class DriverSettings.
     */
    @Test
    public void testGetIEDriverPath() {
        String expResult = "./lib/Drivers/IEDriverServer.exe";
        String result = ds.getIEDriverPath();
        assertEquals(result, expResult);
    }

    /**
     * Test of getEdgeDriverPath method, of class DriverSettings.
     */
    @Test
    public void testGetEdgeDriverPath() {
        String expResult = "./lib/Drivers/MicrosoftWebDriver.exe";
        String result = ds.getEdgeDriverPath();
        assertEquals(result, expResult);
    }

    /**
     * Test of setGeckcoDriverPath method, of class DriverSettings.
     */
    @Test
    public void testSetGeckcoDriverPath() {
        String path = "./lib/gk";
        ds.setGeckcoDriverPath(path);
        assertEquals(ds.getGeckcoDriverPath(), path);
    }

    /**
     * Test of setChromeDriverPath method, of class DriverSettings.
     */
    @Test
    public void testSetChromeDriverPath() {
        String path = "./lib/chrome";
        ds.setChromeDriverPath(path);
        assertEquals(ds.getChromeDriverPath(), path);
    }

    /**
     * Test of setIEDriverPath method, of class DriverSettings.
     */
    @Test
    public void testSetIEDriverPath() {
        String path = "./lib/ie";
        ds.setIEDriverPath(path);
        assertEquals(ds.getIEDriverPath(), path);
    }

    /**
     * Test of setEdgeDriverPath method, of class DriverSettings.
     */
    @Test
    public void testSetEdgeDriverPath() {
        String path = "./lib/edge";
        ds.setEdgeDriverPath(path);
        assertEquals(ds.getEdgeDriverPath(), path);
    }

}

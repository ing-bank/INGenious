package com.ing.engine.drivers;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.ing.engine.drivers.sap.FakeSap;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import org.junit.Test;

/**
 * Covers the full-desktop {@code Robot} fallback path (no real {@code ActiveWindow} to read
 * bounds from) - a live SAP GUI session's {@code ActiveWindow}/{@code ScreenLeft}/etc. can't be
 * exercised without a real JACOB {@code Dispatch}, which Mockito cannot mock (native methods).
 */
public class SapDriverCreationTest {

    @Test
    public void fallsBackToFullDesktopWhenSessionHasNoRawDispatch() throws IOException {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        SapDriverCreation driver = new SapDriverCreation(new FakeSap.Session());
        File screenshot = driver.createScreenShot();

        assertNotNull(screenshot);
        assertTrue(screenshot.exists());
        assertTrue(screenshot.length() > 0);
        screenshot.delete();
    }

    @Test
    public void fallsBackToFullDesktopWhenSessionIsNull() throws IOException {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        SapDriverCreation driver = new SapDriverCreation(null);
        File screenshot = driver.createScreenShot();

        assertNotNull(screenshot);
        assertTrue(screenshot.exists());
        assertTrue(screenshot.length() > 0);
        screenshot.delete();
    }
}

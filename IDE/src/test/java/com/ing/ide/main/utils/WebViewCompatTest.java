package com.ing.ide.main.utils;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import javax.swing.SwingUtilities;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

public class WebViewCompatTest {

    @AfterMethod(alwaysRun = true)
    public void clearOverride() {
        System.clearProperty(WebViewCompat.OVERRIDE_PROPERTY);
    }

    @Test
    public void defaultFollowsTheJdkFeatureVersion() {
        System.clearProperty(WebViewCompat.OVERRIDE_PROPERTY);
        assertEquals(WebViewCompat.isScriptingSafe(), Runtime.version().feature() < 26);
    }

    @Test
    public void overridePropertyWinsOverTheJdkCheck() {
        System.setProperty(WebViewCompat.OVERRIDE_PROPERTY, "false");
        assertFalse(WebViewCompat.isScriptingSafe());
        System.setProperty(WebViewCompat.OVERRIDE_PROPERTY, "true");
        assertTrue(WebViewCompat.isScriptingSafe());
    }

    @Test
    public void consoleFactoryReturnsSwingViewWhenForced() throws Exception {
        System.setProperty(ConsoleView.FORCE_SWING_PROPERTY, "true");
        try {
            ConsoleView[] created = new ConsoleView[1];
            SwingUtilities.invokeAndWait(() -> created[0] = ConsoleView.create());
            assertTrue(created[0] instanceof ConsoleSwingView);
        } finally {
            System.clearProperty(ConsoleView.FORCE_SWING_PROPERTY);
        }
    }
}

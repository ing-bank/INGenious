package com.ing.ide.main;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class MainRenderingTest {

    @DataProvider
    public Object[][] renderingSettings() {
        return new Object[][] {
            { "Mac OS X", null, "true" },
            { "Windows 11", null, "false" },
            { "Linux", null, "false" },
            { "Mac OS X", "false", "false" },
            { "Windows 11", "true", "true" }
        };
    }

    @Test(dataProvider = "renderingSettings", singleThreaded = true)
    public void preservesExplicitHiDpiSettings(String os, String configured, String expected) {
        String originalOs = System.getProperty("os.name");
        String originalHiDpi = System.getProperty("prism.allowhidpi");
        try {
            System.setProperty("os.name", os);
            if (configured == null) {
                System.clearProperty("prism.allowhidpi");
            } else {
                System.setProperty("prism.allowhidpi", configured);
            }
            Main.configureJavaFxRendering();
            assertEquals(System.getProperty("prism.allowhidpi"), expected);
        } finally {
            System.setProperty("os.name", originalOs);
            if (originalHiDpi == null) {
                System.clearProperty("prism.allowhidpi");
            } else {
                System.setProperty("prism.allowhidpi", originalHiDpi);
            }
        }
    }
}

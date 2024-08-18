
package com.ing.ide.util.browser;

public class PlatformBrowser {

    public static Browser getBrowser() {
        return new JavaFXBrowser();
    }
}

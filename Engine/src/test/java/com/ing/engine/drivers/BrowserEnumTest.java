package com.ing.engine.drivers;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.ArrayList;
import org.testng.annotations.Test;

public class BrowserEnumTest {

    @Test
    public void testChromiumValue() {
        assertThat(PlaywrightDriverFactory.Browser.Chromium.getBrowserValue()).isEqualTo("Chromium");
    }

    @Test
    public void testWebKitValue() {
        assertThat(PlaywrightDriverFactory.Browser.WebKit.getBrowserValue()).isEqualTo("WebKit");
    }

    @Test
    public void testFirefoxValue() {
        assertThat(PlaywrightDriverFactory.Browser.Firefox.getBrowserValue()).isEqualTo("Firefox");
    }

    @Test
    public void testEmptyValue() {
        assertThat(PlaywrightDriverFactory.Browser.Empty.getBrowserValue()).isEqualTo("No Browser");
    }

    @Test
    public void testToString() {
        assertThat(PlaywrightDriverFactory.Browser.Chromium.toString()).isEqualTo("Chromium");
        assertThat(PlaywrightDriverFactory.Browser.Firefox.toString()).isEqualTo("Firefox");
    }

    @Test
    public void testFromStringExact() {
        assertThat(PlaywrightDriverFactory.Browser.fromString("Chromium"))
                .isEqualTo(PlaywrightDriverFactory.Browser.Chromium);
    }

    @Test
    public void testFromStringCaseInsensitive() {
        assertThat(PlaywrightDriverFactory.Browser.fromString("chromium"))
                .isEqualTo(PlaywrightDriverFactory.Browser.Chromium);
        assertThat(PlaywrightDriverFactory.Browser.fromString("FIREFOX"))
                .isEqualTo(PlaywrightDriverFactory.Browser.Firefox);
    }

    @Test
    public void testFromStringNoMatch() {
        assertThat(PlaywrightDriverFactory.Browser.fromString("Safari")).isNull();
    }

    @Test
    public void testGetValuesAsList() {
        ArrayList<String> values = PlaywrightDriverFactory.Browser.getValuesAsList();
        assertThat(values).containsExactly("Chromium", "WebKit", "Firefox", "No Browser");
    }

    @Test
    public void testEnumValuesCount() {
        assertThat(PlaywrightDriverFactory.Browser.values()).hasSize(4);
    }
}

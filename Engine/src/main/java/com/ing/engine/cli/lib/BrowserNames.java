package com.ing.engine.cli.lib;

import java.util.Locale;

/**
 * Canonical browser-name parsing for CLI flags and MCP tools.
 *
 * <p>The runtime ({@code WebDriverFactory}, {@code PlaywrightDriverFactory})
 * only recognises the four exact values {@code "Chromium"}, {@code "Firefox"},
 * {@code "WebKit"} and {@code "No Browser"}. End users (and AI agents driving
 * the MCP server) reliably type variants like {@code "no-browser"},
 * {@code "NoBrowser"} or {@code "chrome"} – this helper normalises them all to
 * the canonical form so the engine never has to deal with the cosmetic noise.
 */
public final class BrowserNames {

    public static final String CHROMIUM   = "Chromium";
    public static final String FIREFOX    = "Firefox";
    public static final String WEBKIT     = "WebKit";
    public static final String NO_BROWSER = "No Browser";

    private BrowserNames() {}

    /**
     * Returns the canonical browser name for {@code raw} or the raw value
     * itself if it isn't recognised (so misspellings still propagate to the
     * engine, which will produce a descriptive error). {@code null} is
     * passed through unchanged.
     */
    public static String normalize(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return s;
        String key = s.toLowerCase(Locale.ROOT).replaceAll("[\\s_-]+", "");
        switch (key) {
            case "chromium":
            case "chrome":
                return CHROMIUM;
            case "firefox":
            case "ff":
                return FIREFOX;
            case "webkit":
            case "safari":
                return WEBKIT;
            case "nobrowser":
            case "none":
            case "api":
            case "headlessapi":
                return NO_BROWSER;
            default:
                return s;
        }
    }
}

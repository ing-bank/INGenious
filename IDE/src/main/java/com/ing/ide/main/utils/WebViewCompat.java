package com.ing.ide.main.utils;

/**
 * Decides whether the embedded JavaFX WebView may be driven from Java via
 * {@code WebEngine.executeScript}.
 *
 * <p>On JDK 26 and later the JVM crashes (SIGSEGV in {@code jni_GetStaticFieldID}, called from
 * JavaFX WebKit's Java-to-JavaScript bridge) the first time a script is executed. This reproduces
 * with JavaFX 17 through 27, so it cannot be fixed by upgrading JavaFX. Loading a page, letting
 * it run its own JavaScript, and page-to-Java {@code alert} callbacks are not affected. Where
 * {@link #isScriptingSafe()} is false, feed scripts through a {@link LocalScriptBridge} instead.</p>
 */
public final class WebViewCompat {
    /** Set to {@code true} or {@code false} to override the JDK-based detection. */
    public static final String OVERRIDE_PROPERTY = "ingenious.webview.scripting";

    private static final int FIRST_UNSAFE_JDK = 26;

    private WebViewCompat() {}

    public static boolean isScriptingSafe() {
        String override = System.getProperty(OVERRIDE_PROPERTY);
        if (override != null) {
            return Boolean.parseBoolean(override.trim());
        }
        return Runtime.version().feature() < FIRST_UNSAFE_JDK;
    }
}

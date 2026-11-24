package com.ing.ide.main.utils;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;

/** A live execution console that can be fed stdout/stderr lines from any thread. */
public interface ConsoleView {
    /** Set to {@code true} to force the simpler Swing console instead of the WebView. */
    String FORCE_SWING_PROPERTY = "ingenious.console.swing";

    /**
     * Creates the console: the styled JavaFX WebView on every JDK (fed through a local script
     * bridge where {@code executeScript} is unsafe, see {@link WebViewCompat}), or the Swing
     * console if forced or if the bridge cannot start.
     */
    static ConsoleView create() {
        if (Boolean.getBoolean(FORCE_SWING_PROPERTY)) {
            return new ConsoleSwingView();
        }
        try {
            return new ConsoleWebView();
        } catch (RuntimeException ex) {
            Logger
                .getLogger(ConsoleView.class.getName())
                .log(Level.WARNING, "Console WebView unavailable, using the Swing console", ex);
            return new ConsoleSwingView();
        }
    }

    /** Notifies every live console that the app-wide theme changed. */
    static void applyThemeToAll(boolean dark) {
        ConsoleWebView.applyThemeToAll(dark);
        ConsoleSwingView.applyThemeToAll(dark);
    }

    JComponent getComponent();

    /** Appends a stdout line, classified into a status label from its leading [TAG]. */
    void appendLine(String text);

    /** Appends a stderr line; shown with the ERROR label when it has no status [TAG]. */
    void appendErrorLine(String text);

    void clear();

    void setDarkMode(boolean dark);
}

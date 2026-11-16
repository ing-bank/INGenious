package com.ing.ide.e2e.support;

import java.util.Locale;

/**
 * How E2E tests deliver user input to the IDE, selected with {@code -De2e.input=direct|robot}.
 */
public enum InputMode {
    /**
     * Calls the UI controls directly ({@code doClick}, {@code fire}, {@code setText}) and captures
     * screenshots by painting the application windows. Never touches the OS mouse or keyboard,
     * needs no macOS Accessibility/Screen Recording permission. Default for local runs.
     */
    DIRECT,

    /**
     * Moves the real OS mouse and keyboard and captures the whole desktop, exactly like a user.
     * Use on a dedicated or virtual display (CI, {@code xvfb-run}).
     */
    ROBOT;

    /** System property that selects the mode. */
    public static final String PROPERTY = "e2e.input";

    /**
     * @return the mode from {@value #PROPERTY}, {@link #DIRECT} when unset
     * @throws IllegalArgumentException if the value is not a known mode
     */
    public static InputMode fromSystemProperty() {
        final String value = System.getProperty(PROPERTY, "").trim();
        if (value.isEmpty()) {
            return DIRECT;
        }
        try {
            return valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Unknown -D" + PROPERTY + "=" + value + "; use 'direct' or 'robot'",
                e
            );
        }
    }
}

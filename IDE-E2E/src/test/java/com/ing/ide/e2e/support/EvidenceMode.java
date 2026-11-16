package com.ing.ide.e2e.support;

import java.util.Locale;

/**
 * What evidence each test case collects, selected with {@code -De2e.evidence=screenshot|recording|both}.
 */
public enum EvidenceMode {
    /** A PNG per step and per check. Default. */
    SCREENSHOT,

    /** One {@code recording.mp4} per test case; steps and checks appear as captions and boxes. */
    RECORDING,

    /** Screenshots and a recording. */
    BOTH;

    /** System property that selects the mode. */
    public static final String PROPERTY = "e2e.evidence";

    /**
     * @return the mode from {@value #PROPERTY}, {@link #SCREENSHOT} when unset
     * @throws IllegalArgumentException if the value is not a known mode
     */
    public static EvidenceMode fromSystemProperty() {
        final String value = System.getProperty(PROPERTY, "").trim();
        if (value.isEmpty()) {
            return SCREENSHOT;
        }
        try {
            return valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Unknown -D" + PROPERTY + "=" + value + "; use 'screenshot', 'recording' or 'both'",
                e
            );
        }
    }

    /**
     * @return whether step screenshots are saved
     */
    public boolean screenshots() {
        return this != RECORDING;
    }

    /**
     * @return whether the test case is recorded
     */
    public boolean recording() {
        return this != SCREENSHOT;
    }
}

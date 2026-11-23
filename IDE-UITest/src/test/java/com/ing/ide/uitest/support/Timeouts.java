package com.ing.ide.uitest.support;

import java.time.Duration;

/**
 * Shared timeouts for UI tests. Generous because CI agents and virtual displays are slow.
 */
public final class Timeouts {
    /** Time for the IDE to start and show its first window. */
    public static final Duration LAUNCH = Duration.ofSeconds(120);

    /** Time for a single UI step (dialog appears, project loads, window closes). */
    public static final Duration STEP = Duration.ofSeconds(60);

    private Timeouts() {}
}

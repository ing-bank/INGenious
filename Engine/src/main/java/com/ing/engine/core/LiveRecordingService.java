package com.ing.engine.core;

/**
 * Holds the optional {@link LiveRecordingHook} the IDE registers so the in-process Engine can
 * trigger live Playwright recording when a {@code RecordFromHere} step runs. When no hook is
 * registered (e.g. headless CLI execution) the recorder action falls back to a plain pause.
 */
public final class LiveRecordingService {
    private static volatile LiveRecordingHook hook;

    private LiveRecordingService() {}

    public static void setHook(LiveRecordingHook recordingHook) {
        hook = recordingHook;
    }

    public static LiveRecordingHook getHook() {
        return hook;
    }

    public static boolean isAvailable() {
        return hook != null;
    }
}

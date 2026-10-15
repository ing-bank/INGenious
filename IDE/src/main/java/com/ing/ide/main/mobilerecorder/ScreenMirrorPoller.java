package com.ing.ide.main.mobilerecorder;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Repeatedly pulls a screenshot from the live session on a background thread and
 * hands the raw PNG bytes to a callback. The callback is responsible for
 * marshalling onto the Swing EDT.
 */
public final class ScreenMirrorPoller {
    private static final Logger LOG = Logger.getLogger(ScreenMirrorPoller.class.getName());

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(
        r -> {
            Thread t = new Thread(r, "mobile-mirror-poller");
            t.setDaemon(true);
            return t;
        }
    );

    private ScheduledFuture<?> task;

    /**
     * Starts polling every {@code intervalMillis} (e.g. 300ms ~= 3fps, a
     * reasonable default over the Appium HTTP protocol).
     */
    public synchronized void start(
        MobileSessionManager session,
        Consumer<byte[]> onFrame,
        long intervalMillis
    ) {
        if (task != null) {
            throw new IllegalStateException("Poller already running; stop it first.");
        }
        task =
            executor.scheduleWithFixedDelay(
                () -> {
                    try {
                        if (session.isRunning()) {
                            onFrame.accept(session.captureScreenshot());
                        }
                    } catch (Exception e) {
                        LOG.log(Level.FINE, "Failed to capture screenshot", e);
                    }
                },
                0,
                intervalMillis,
                TimeUnit.MILLISECONDS
            );
    }

    public synchronized void stop() {
        if (task != null) {
            task.cancel(true);
            task = null;
        }
    }

    public synchronized boolean isRunning() {
        return task != null;
    }

    /** Shuts down the underlying executor entirely (call on window close). */
    public void shutdown() {
        stop();
        executor.shutdownNow();
    }
}

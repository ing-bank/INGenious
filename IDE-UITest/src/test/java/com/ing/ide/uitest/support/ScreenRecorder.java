package com.ing.ide.uitest.support;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.jcodec.api.awt.AWTSequenceEncoder;

/**
 * Records one test case to an H.264 MP4.
 * <p>
 * The video runs at {@link #FPS} frames per second, so pointer movement is smooth. Between user
 * actions a background thread captures the screen {@link #LIVE_CAPTURES_PER_SECOND} times a second
 * (capturing and encoding in pure Java is too slow for more) and repeats each capture to keep the
 * video in real time. During an action the background capture is paused and the action's own frames
 * (pointer steps, click) are added instead.
 */
final class ScreenRecorder {
    /** Video frame rate. */
    static final int FPS = 12;

    /** Background screen captures per second between actions. */
    static final int LIVE_CAPTURES_PER_SECOND = 4;

    private static final long FRAME_MS = 1000L / FPS;
    private static final long LIVE_INTERVAL_MS = 1000L / LIVE_CAPTURES_PER_SECOND;
    private static final int MAX_WIDTH = 1024;
    private static final int MAX_CATCH_UP_FRAMES = FPS * 2;

    private final Path file;
    private final InputMode mode;
    private final ScheduledExecutorService scheduler;
    private AWTSequenceEncoder encoder;
    private long startMillis;
    private long liveFrames;
    private int encodedFrames;
    private int width;
    private int height;
    private String caption;
    private long captionUntil;
    private boolean paused;
    private boolean errorReported;
    private Point virtualPointer;

    private ScreenRecorder(final Path file, final InputMode mode) throws IOException {
        this.file = file;
        this.mode = mode;
        Files.createDirectories(file.getParent());
        this.encoder = AWTSequenceEncoder.createSequenceEncoder(file.toFile(), FPS);
        this.scheduler =
            Executors.newSingleThreadScheduledExecutor(
                r -> {
                    final Thread thread = new Thread(r, "UITest-ScreenRecorder");
                    thread.setDaemon(true);
                    return thread;
                }
            );
    }

    /**
     * Starts recording.
     *
     * @param file target MP4 file
     * @param mode input mode, which decides how frames are captured
     * @return the running recorder
     * @throws IOException if the file cannot be created
     */
    static ScreenRecorder start(final Path file, final InputMode mode) throws IOException {
        final ScreenRecorder recorder = new ScreenRecorder(file, mode);
        recorder.startMillis = System.currentTimeMillis();
        recorder.scheduler.scheduleAtFixedRate(
            recorder::captureLiveFrame,
            0,
            LIVE_INTERVAL_MS,
            TimeUnit.MILLISECONDS
        );
        return recorder;
    }

    /**
     * Shows a caption on live frames for a while, e.g. the current step.
     *
     * @param text the caption
     * @param durationMs how long to show it
     */
    synchronized void caption(final String text, final long durationMs) {
        caption = text;
        captionUntil = System.currentTimeMillis() + durationMs;
    }

    /**
     * Sets where the pointer is drawn on live frames in direct mode, which has no real pointer.
     *
     * @param screenPoint pointer position in screen coordinates
     */
    synchronized void pointer(final Point screenPoint) {
        if (screenPoint != null) {
            virtualPointer = new Point(screenPoint);
        }
    }

    /**
     * Stops background captures while an action adds its own frames.
     */
    synchronized void pause() {
        paused = true;
    }

    /**
     * Resumes background captures from now, without catching up on the paused time.
     */
    synchronized void resume() {
        paused = false;
        liveFrames = dueFrames();
    }

    /**
     * Adds a single frame, e.g. one step of the pointer moving.
     *
     * @param shot the frame
     */
    synchronized void frame(final ScreenCapture.Shot shot) {
        if (encoder != null) {
            encode(toVideoFrame(shot.image));
        }
    }

    /**
     * Freezes the video on an (annotated) frame, e.g. a click or check result, without slowing the test.
     *
     * @param shot the frame
     * @param durationMs video time to show it
     */
    synchronized void hold(final ScreenCapture.Shot shot, final long durationMs) {
        if (encoder == null) {
            return;
        }
        final long copies = Math.max(1, durationMs / FRAME_MS);
        final BufferedImage frame = toVideoFrame(shot.image);
        for (long i = 0; i < copies; i++) {
            encode(frame);
        }
        // Resume live capture from now instead of catching up on the time spent encoding the hold
        liveFrames = dueFrames();
    }

    /**
     * Stops recording and finalises the file.
     *
     * @return the MP4, or {@code null} if nothing was recorded
     */
    Path stop() {
        scheduler.shutdown();
        try {
            scheduler.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        synchronized (this) {
            if (encoder == null) {
                return null;
            }
            try {
                if (encodedFrames > 0) {
                    encoder.finish();
                }
                encoder = null;
                if (encodedFrames == 0) {
                    Files.deleteIfExists(file);
                    return null;
                }
                return file;
            } catch (IOException e) {
                encoder = null;
                report(e);
                return null;
            }
        }
    }

    private void captureLiveFrame() {
        try {
            synchronized (this) {
                if (paused || encoder == null) {
                    return;
                }
            }
            final ScreenCapture.Shot shot = ScreenCapture.screen(mode);
            synchronized (this) {
                // An action may have started while capturing; its frames take precedence
                if (paused || encoder == null) {
                    return;
                }
                final Point pointer = ScreenCapture.currentPointer(mode, virtualPointer);
                if (pointer != null) {
                    ScreenCapture.pointer(shot, pointer, false);
                }
                if (caption != null && System.currentTimeMillis() < captionUntil) {
                    ScreenCapture.caption(shot, caption, ScreenCapture.STEP_COLOR);
                }
                final long due = dueFrames();
                final long copies = Math.min(Math.max(1, due - liveFrames), MAX_CATCH_UP_FRAMES);
                final BufferedImage frame = toVideoFrame(shot.image);
                for (long i = 0; i < copies; i++) {
                    encode(frame);
                }
                liveFrames = due;
            }
        } catch (RuntimeException e) {
            // Keep recording: the scheduler would silently stop on an uncaught exception
            report(e);
        }
    }

    private long dueFrames() {
        return (System.currentTimeMillis() - startMillis) / FRAME_MS + 1;
    }

    private void encode(final BufferedImage frame) {
        try {
            encoder.encodeImage(frame);
            encodedFrames++;
        } catch (IOException e) {
            report(e);
        }
    }

    // H.264 needs a fixed, even frame size; frames are scaled down for encoding speed.
    private BufferedImage toVideoFrame(final BufferedImage source) {
        if (width == 0) {
            final double scale = Math.min(1.0, (double) MAX_WIDTH / source.getWidth());
            width = even((int) (source.getWidth() * scale));
            height = even((int) (source.getHeight() * scale));
        }
        final BufferedImage frame = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        final Graphics2D g = frame.createGraphics();
        try {
            g.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR
            );
            g.drawImage(source, 0, 0, width, height, Color.BLACK, null);
        } finally {
            g.dispose();
        }
        return frame;
    }

    private static int even(final int value) {
        return Math.max(2, value - value % 2);
    }

    private void report(final Exception e) {
        if (!errorReported) {
            errorReported = true;
            System.err.println("[UI test recording] " + file + ": " + e);
        }
    }
}

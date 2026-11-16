package com.ing.ide.e2e.support;

import com.ing.ide.e2e.report.Evidence;
import com.ing.ide.e2e.report.StepRef;
import java.awt.Point;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Collects the evidence of the current test case in its folder
 * ({@code <run>/<scenario>/<test case>/}): step screenshots ({@code NN-<step>.png}) and/or a
 * {@code recording.mp4}, depending on {@link EvidenceMode}.
 */
public final class EvidenceCollector implements ActionObserver {
    private static final String RECORDING_FILE = "recording.mp4";
    private static final long STEP_CAPTION_MS = 1500;
    private static final long CHECK_HOLD_MS = 2000;
    private static final long ACTION_HOLD_MS = 500;

    private final InputMode inputMode;
    private final EvidenceMode evidenceMode;
    private final List<Evidence> collected = new ArrayList<>();
    private Path dir;
    private int counter;
    private ScreenRecorder recorder;
    private Point lastActionPoint;

    /**
     * @param inputMode how the IDE is driven, which decides how images are captured
     * @param evidenceMode what to collect
     * @param initialDir folder used until the first test case starts
     */
    public EvidenceCollector(
        final InputMode inputMode,
        final EvidenceMode evidenceMode,
        final Path initialDir
    ) {
        this.inputMode = inputMode;
        this.evidenceMode = evidenceMode;
        this.dir = initialDir;
    }

    /**
     * Starts collecting evidence for a new test case; starts its recording when enabled.
     *
     * @param evidenceDir the test case's evidence folder
     */
    public synchronized void startTestCase(final Path evidenceDir) {
        stopRecording();
        dir = evidenceDir;
        counter = 0;
        collected.clear();
        if (evidenceMode.recording()) {
            try {
                recorder = ScreenRecorder.start(dir.resolve(RECORDING_FILE), inputMode);
            } catch (IOException e) {
                throw new UncheckedIOException("Cannot start recording in " + dir, e);
            }
        }
    }

    /**
     * Marks a step: saves a screenshot and/or captions the recording.
     *
     * @param step short description of the step, used in the file name and caption
     */
    public synchronized void take(final String step) {
        take(step, StepRef.NONE);
    }

    /**
     * Marks a step and links its screenshot to a step or assert of the RTM test case.
     *
     * @param step short description of the step, used in the file name and caption
     * @param ref the RTM step or assert this screenshot proves
     */
    public synchronized void take(final String step, final StepRef ref) {
        if (evidenceMode.screenshots()) {
            write(step, screenshot(), step, ref);
        }
        if (recorder != null) {
            recorder.caption(step, STEP_CAPTION_MS);
        }
    }

    /**
     * Remembers where the user action happens (the pointer position for direct-mode screenshots) and
     * shows it in the recording: freezes on the target with the pointer, a click ripple and a caption.
     *
     * @param screenPoint where the action happens, or {@code null} if unknown (caption only)
     * @param description what the user does
     */
    @Override
    public synchronized void beforeAction(final Point screenPoint, final String description) {
        if (screenPoint != null) {
            lastActionPoint = new Point(screenPoint);
        }
        if (recorder == null) {
            return;
        }
        final ScreenCapture.Shot frame = ScreenCapture.screen(inputMode);
        if (screenPoint != null) {
            recorder.pointer(screenPoint);
            ScreenCapture.pointer(frame, screenPoint, true);
        }
        ScreenCapture.caption(frame, description, ScreenCapture.STEP_COLOR);
        recorder.hold(frame, ACTION_HOLD_MS);
        // The action replaces any earlier step caption on the live frames that follow
        recorder.caption(description, STEP_CAPTION_MS);
    }

    /**
     * Pauses the background recording, so the action's own frames are not interleaved with it.
     */
    @Override
    public synchronized void actionStarting() {
        if (recorder != null) {
            recorder.pause();
        }
    }

    /**
     * Records one step of the pointer moving to the action's target: the current screen with the
     * pointer drawn at that step's position.
     *
     * @param screenPoint pointer position
     * @param description the upcoming action, shown as caption
     */
    @Override
    public synchronized void pointerMoved(final Point screenPoint, final String description) {
        lastActionPoint = new Point(screenPoint);
        if (recorder == null) {
            return;
        }
        final ScreenCapture.Shot frame = ScreenCapture.screen(inputMode);
        ScreenCapture.pointer(frame, screenPoint, false);
        ScreenCapture.caption(frame, description, ScreenCapture.STEP_COLOR);
        recorder.pointer(screenPoint);
        recorder.frame(frame);
    }

    /**
     * Resumes the background recording after an action.
     */
    @Override
    public synchronized void actionFinished() {
        if (recorder != null) {
            recorder.resume();
        }
    }

    /**
     * Runs an assertion, then records the checked area boxed and captioned: green when the assertion
     * passed, red when it failed. A failed assertion is rethrown afterwards.
     *
     * @param step short description of the check, used in the file name
     * @param target the area that is checked and what is expected there
     * @param assertion the assertion to run
     */
    public synchronized void verify(
        final String step,
        final Highlight target,
        final Runnable assertion
    ) {
        verify(step, StepRef.NONE, target, assertion);
    }

    /**
     * Like {@link #verify(String, Highlight, Runnable)}, and links the screenshot to a step or
     * assert of the RTM test case.
     *
     * @param step short description of the check, used in the file name
     * @param ref the RTM step or assert this check proves
     * @param target the area that is checked and what is expected there
     * @param assertion the assertion to run
     */
    public synchronized void verify(
        final String step,
        final StepRef ref,
        final Highlight target,
        final Runnable assertion
    ) {
        AssertionError failure = null;
        try {
            assertion.run();
        } catch (AssertionError e) {
            failure = e;
        }
        final boolean passed = failure == null;
        if (evidenceMode.screenshots()) {
            final ScreenCapture.Shot shot = screenshot();
            ScreenCapture.annotate(shot, target, passed);
            write(
                passed ? step : step + "-FAILED",
                shot,
                (passed ? "PASSED: " : "FAILED: ") + target.label(),
                ref
            );
        }
        if (recorder != null) {
            final ScreenCapture.Shot frame = ScreenCapture.screen(inputMode);
            ScreenCapture.annotate(frame, target, passed);
            recorder.hold(frame, CHECK_HOLD_MS);
        }
        if (failure != null) {
            throw failure;
        }
    }

    /**
     * Captures the failed state: always a screenshot, plus a frozen captioned frame in the recording.
     *
     * @param step short description used in the file name and caption
     */
    public synchronized void captureFailure(final String step) {
        write(step, screenshot(), step, StepRef.NONE);
        if (recorder != null) {
            final ScreenCapture.Shot frame = ScreenCapture.screen(inputMode);
            ScreenCapture.caption(frame, step, ScreenCapture.FAIL_COLOR);
            recorder.hold(frame, CHECK_HOLD_MS);
        }
    }

    /**
     * Ends the current test case: finalises the recording.
     *
     * @return the recording (if any) followed by the screenshots, in capture order
     */
    public synchronized List<Evidence> complete() {
        final Path recording = stopRecording();
        final List<Evidence> all = new ArrayList<>();
        if (recording != null) {
            all.add(
                new Evidence(
                    "recording",
                    recording.toString(),
                    "Recording of the whole test case",
                    null,
                    null
                )
            );
        }
        all.addAll(collected);
        return all;
    }

    private ScreenCapture.Shot screenshot() {
        final ScreenCapture.Shot shot = ScreenCapture.windows(inputMode);
        final Point pointer = ScreenCapture.currentPointer(inputMode, lastActionPoint);
        if (pointer != null) {
            ScreenCapture.pointer(shot, pointer, false);
        }
        return shot;
    }

    private Path stopRecording() {
        if (recorder == null) {
            return null;
        }
        final Path recording = recorder.stop();
        recorder = null;
        return recording;
    }

    private void write(
        final String step,
        final ScreenCapture.Shot shot,
        final String caption,
        final StepRef ref
    ) {
        final String fileName = String.format(
            "%02d-%s.png",
            ++counter,
            step.replaceAll("[^A-Za-z0-9_-]+", "-")
        );
        final Path file = dir.resolve(fileName);
        try {
            Files.createDirectories(dir);
            Files.deleteIfExists(file);
            ImageIO.write(shot.image, "png", file.toFile());
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write screenshot " + file, e);
        }
        collected.add(
            new Evidence(
                fileName.substring(0, fileName.length() - ".png".length()),
                file.toString(),
                caption,
                ref.step(),
                ref.assertion()
            )
        );
    }
}

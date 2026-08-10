package com.ing.engine.core;

import com.ing.datalib.component.TestCase;

/**
 * Callback that lets the in-process Engine notify the IDE when a {@code RecordFromHere} step is
 * reached so the IDE can drive the same live Playwright recording experience used by the toolbar
 * record button (live step insertion, Web OR registration, green highlighting of new steps).
 * <p>
 * The Engine module cannot depend on the IDE module, so the IDE registers an implementation of
 * this interface into {@link LiveRecordingService} at startup. When the recorder action runs it
 * looks the hook up and invokes it. All methods are called on the Engine execution thread.
 * </p>
 */
public interface LiveRecordingHook {
    /**
     * Invoked when a {@code RecordFromHere} step starts executing. The IDE prepares a live
     * recording session: it creates the recorder output file, shows the running test case in the
     * editor and starts watching the file to insert recorded steps right after the supplied step
     * index (pushing any following steps down).
     *
     * @param testCase            the test case currently being executed
     * @param insertAfterStepIndex zero-based index of the executing {@code RecordFromHere} step;
     *                             recorded steps are inserted starting at {@code index + 1}
     * @return absolute path of the file the Playwright recorder should write generated Java code
     *         to, or {@code null} to skip live recording (the action then just pauses)
     */
    String onRecordingStarted(TestCase testCase, int insertAfterStepIndex);

    /**
     * Invoked once the Playwright recorder has been enabled on the live browser context and
     * execution is about to pause for the user to record.
     */
    void onRecordingReady();

    /**
     * Invoked after the user resumes execution from the Playwright inspector, signalling that the
     * recording session has finished and the captured steps can be finalized.
     */
    void onRecordingStopped();
}

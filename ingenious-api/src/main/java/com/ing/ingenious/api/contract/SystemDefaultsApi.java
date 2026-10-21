package com.ing.ingenious.api.contract;

import java.time.Duration;

/**
 * Contract for the framework's shared run-level defaults and control flags.
 * <p>
 * <b>Important:</b> the execution-control flags below ({@code stopExecution},
 * {@code pauseExecution}, {@code stopCurrentIteration}, {@code nextStepFlag},
 * {@code reportComplete}, {@code debugMode}) are the same global, run-wide
 * state used by the IDE's Stop/Pause/Step/Debug controls. They are not
 * scoped to the calling plugin or command: setting {@code stopExecution} to
 * {@code true} halts the entire run (every queued scenario/test case, not
 * just the current one), and setting {@code pauseExecution} to {@code true}
 * blocks execution until something sets it back to {@code false} — in a
 * headless run with no IDE attached, nothing will ever do that. Use these
 * setters only when a plugin genuinely needs to abort or pause the whole
 * run, not as a per-command control mechanism.
 * </p>
 */
public interface SystemDefaultsApi {

    // ── Timing defaults ──────────────────────────────────────────────────

    /** @return the default wait timeout used when no element-specific wait is configured */
    Duration getWaitTime();

    /** @param waitTime the default wait timeout to apply framework-wide */
    void setWaitTime(Duration waitTime);

    /** @return the default element wait timeout used by locator/wait operations */
    Duration getElementWaitTime();

    /** @param elementWaitTime the default element wait timeout to apply framework-wide */
    void setElementWaitTime(Duration elementWaitTime);

    // ── Execution control ────────────────────────────────────────────────

    /** @return whether a stop has been requested for the current run */
    boolean isStopExecution();

    /**
     * Requests or clears a stop for the entire run, not just the current command.
     * @param stopExecution true to request a stop of the whole run
     */
    void setStopExecution(boolean stopExecution);

    /** @return whether the run is currently paused */
    boolean isPauseExecution();

    /**
     * Pauses or resumes the entire run. A run paused with no IDE attached to
     * resume it will block indefinitely.
     * @param pauseExecution true to pause the whole run
     */
    void setPauseExecution(boolean pauseExecution);

    /** @return whether a stop has been requested for the current iteration only */
    boolean isStopCurrentIteration();

    /** @param stopCurrentIteration true to request a stop of the current iteration only */
    void setStopCurrentIteration(boolean stopCurrentIteration);

    /** @return whether the step-through debugger is allowed to advance to the next step */
    boolean isNextStepFlag();

    /** @param nextStepFlag true to allow the step-through debugger to advance */
    void setNextStepFlag(boolean nextStepFlag);

    /** @return whether the current run's report has completed */
    boolean isReportComplete();

    /** @param reportComplete the report-completion state for the current run */
    void setReportComplete(boolean reportComplete);

    /** @return whether the run is currently executing in debug mode */
    boolean isDebugMode();

    /**
     * Enables or disables debug mode for the entire run, affecting
     * breakpoint/step-through behavior for all subsequent steps.
     * @param debugMode true to enable debug mode
     */
    void setDebugMode(boolean debugMode);

    // ── Command-line and environment variables ───────────────────────────

    /**
     * @param key the variable name
     * @return the value of the given command-line variable, or null if not set
     */
    String getCommandLineVariable(String key);

    /**
     * @param key the variable name
     * @param value the value to set
     */
    void setCommandLineVariable(String key, String value);

    /** @param key the variable name to remove */
    void removeCommandLineVariable(String key);

    /**
     * @param key the variable name
     * @return true if the given command-line variable is set
     */
    boolean hasCommandLineVariable(String key);

    /**
     * @param key the variable name
     * @return the value of the given environment override variable, or null if not set
     */
    String getEnvironmentVariable(String key);

    /**
     * @param key the variable name
     * @param value the value to set
     */
    void setEnvironmentVariable(String key, String value);

    /** @param key the variable name to remove */
    void removeEnvironmentVariable(String key);

    /**
     * @param key the variable name
     * @return true if the given environment override variable is set
     */
    boolean hasEnvironmentVariable(String key);

    // ── Diagnostics ───────────────────────────────────────────────────────

    /** @return the running engine's build version */
    String getBuildVersion();

    /** @return whether the engine is running in debug mode (via debugMode or the "debug" command-line variable) */
    boolean isDebug();

    /** @return whether the summary report is allowed to launch after the run completes */
    boolean canLaunchSummary();

    /** Prints the current run's system/environment information to standard output. */
    void printSystemInfo();

    /** Blocks the calling thread for a short, fixed polling interval. */
    void pollWait();
}

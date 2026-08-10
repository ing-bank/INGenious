package com.ing.ide.main.mainui.components.testdesign.testcase.validation;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.component.TestStep;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.SwingUtilities;

/**
 * Central, view-agnostic validation helper that reuses the per-column cell
 * renderers to determine whether a {@link TestStep}, {@link TestCase} or
 * {@link Scenario} contains an IDE-level validation error (red marker).
 *
 * <p>This lets non-table consumers (e.g. the project/reusable trees) flag a
 * test case or scenario in red whenever any of its steps would be marked in
 * red in the test-case editor. It also propagates errors through reusable
 * references: a test case that invokes a reusable which itself has validation
 * errors is reported as having an error, and the invoking {@code Execute} step
 * is marked in red by {@link ObjectRenderer}/{@link ActionRenderer}.</p>
 *
 * <p>Test-case steps are loaded lazily (only when a test case is opened), so a
 * freshly imported project has no in-memory steps for the bulk of its test
 * cases. {@link #validateAllAsync(Project, Runnable)} performs a one-time
 * background pass that force-loads every test case, computes its validation
 * state and caches it, so the trees can be coloured immediately after a project
 * is opened without the user having to open each test case manually.</p>
 *
 * <p>The renderer instances are held per-thread so the background pass and the
 * Event Dispatch Thread never share the mutable renderer state.</p>
 */
public final class TestCaseValidation {
    private static final ThreadLocal<ObjectRenderer> OBJECT_RENDERER = ThreadLocal.withInitial(
        ObjectRenderer::new
    );
    private static final ThreadLocal<ActionRenderer> ACTION_RENDERER = ThreadLocal.withInitial(
        ActionRenderer::new
    );
    private static final ThreadLocal<InputRenderer> INPUT_RENDERER = ThreadLocal.withInitial(
        InputRenderer::new
    );
    private static final ThreadLocal<ReferenceRenderer> REFERENCE_RENDERER = ThreadLocal.withInitial(
        ReferenceRenderer::new
    );
    private static final ThreadLocal<ConditionRenderer> CONDITION_RENDERER = ThreadLocal.withInitial(
        ConditionRenderer::new
    );

    /**
     * Cached validation state per test case. Populated by the background pass
     * ({@link #validateAllAsync}) and refreshed live whenever a loaded test
     * case is re-evaluated. Used to colour test cases that have not been opened
     * (and therefore have no in-memory steps to validate on demand).
     */
    private static final Map<TestCase, Boolean> CACHE = new ConcurrentHashMap<>();

    /**
     * Guards against infinite recursion when reusable references form a cycle.
     * Holds the set of test cases currently being evaluated on this thread.
     */
    private static final ThreadLocal<Set<TestCase>> VISITING = ThreadLocal.withInitial(
        HashSet::new
    );

    private TestCaseValidation() {}

    /**
     * @param testCase the test case to inspect (may be {@code null})
     * @return {@code true} if any of its steps shows a validation error, or if
     *         it invokes a reusable that itself has a validation error
     */
    public static boolean hasError(TestCase testCase) {
        if (testCase == null) {
            return false;
        }
        // When the test case is already loaded (opened/edited) re-evaluate it
        // live so the trees stay accurate without an explicit cache refresh.
        if (!testCase.getTestSteps().isEmpty()) {
            return computeAndCache(testCase, false);
        }
        // Not yet loaded: fall back to the value computed by the background pass.
        Boolean cached = CACHE.get(testCase);
        return cached != null && cached;
    }

    /**
     * @param scenario the scenario to inspect (may be {@code null})
     * @return {@code true} if any contained test case has a validation error
     */
    public static boolean hasError(Scenario scenario) {
        if (scenario == null) {
            return false;
        }
        for (TestCase testCase : scenario.getTestCases()) {
            if (hasError(testCase)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Reports whether the reusable test case referenced by the given step has a
     * validation error. Used by the cell renderers to mark the invoking
     * {@code Execute} step in red.
     *
     * @param step the reusable-invoking step
     * @return {@code true} if the referenced reusable has a validation error
     */
    public static boolean reusableHasError(TestStep step) {
        TestCase reusable = resolveReusable(step);
        if (reusable == null) {
            return false;
        }
        Boolean cached = CACHE.get(reusable);
        if (cached != null) {
            return cached;
        }
        return computeAndCache(reusable, true);
    }

    /**
     * Runs a one-time validation pass over every test case (reusable components
     * first, then the test plan) on a background daemon thread, force-loading
     * each test case's steps and caching its validation state. The supplied
     * {@code onDone} callback is invoked on the Event Dispatch Thread once the
     * pass completes, typically to repaint the trees.
     *
     * @param project the project to validate (may be {@code null})
     * @param onDone  callback run on the EDT after validation completes
     */
    public static void validateAllAsync(Project project, Runnable onDone) {
        Thread worker = new Thread(
            () -> {
                try {
                    if (project != null) {
                        // Reusables first so test-plan references hit the cache.
                        for (Scenario scenario : project.getReusableScenarios()) {
                            for (TestCase testCase : scenario.getTestCases()) {
                                computeAndCache(testCase, true);
                            }
                        }
                        for (Scenario scenario : project.getScenarios()) {
                            for (TestCase testCase : scenario.getTestCases()) {
                                computeAndCache(testCase, true);
                            }
                        }
                    }
                } catch (Exception ignored) {
                    // Validation is best-effort; never fail project loading.
                } finally {
                    if (onDone != null) {
                        SwingUtilities.invokeLater(onDone);
                    }
                }
            },
            "tc-validation"
        );
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * Clears the cached validation state for all test cases. Call before a full
     * re-validation pass (e.g. when the project is reloaded).
     */
    public static void clearCache() {
        CACHE.clear();
    }

    /**
     * Computes and caches the validation state of a test case.
     *
     * @param testCase the test case to evaluate
     * @param force    when {@code true}, the test case's steps are loaded from
     *                 disk if not already in memory (used for test cases the
     *                 user has not opened)
     * @return {@code true} if the test case has a validation error
     */
    private static boolean computeAndCache(TestCase testCase, boolean force) {
        Set<TestCase> visiting = VISITING.get();
        if (!visiting.add(testCase)) {
            // Already being evaluated higher up the stack -> reusable cycle.
            return false;
        }
        try {
            if (force && testCase.getTestSteps().isEmpty()) {
                // Serialize loading per test case so the background pass and the
                // EDT cannot double-load the same steps.
                synchronized (testCase) {
                    if (testCase.getTestSteps().isEmpty()) {
                        testCase.loadTestCaseTableModel();
                    }
                }
            }
            boolean error = false;
            for (TestStep step : testCase.getTestSteps()) {
                if (stepHasError(step)) {
                    error = true;
                    break;
                }
            }
            CACHE.put(testCase, error);
            return error;
        } finally {
            visiting.remove(testCase);
        }
    }

    private static boolean stepHasError(TestStep step) {
        if (step.isCommented()) {
            return false;
        }
        return (
            OBJECT_RENDERER.get().hasError(step) ||
            ACTION_RENDERER.get().hasError(step) ||
            INPUT_RENDERER.get().hasError(step) ||
            REFERENCE_RENDERER.get().hasError(step) ||
            CONDITION_RENDERER.get().hasError(step)
        );
    }

    private static TestCase resolveReusable(TestStep step) {
        if (step == null || !step.isReusableStep()) {
            return null;
        }
        String[] data = step.getReusableData();
        if (data == null || data.length < 2) {
            return null;
        }
        Scenario scenario = step.getProject().getReusableScenarioByName(data[0]);
        return scenario != null ? scenario.getTestCaseByName(data[1]) : null;
    }
}

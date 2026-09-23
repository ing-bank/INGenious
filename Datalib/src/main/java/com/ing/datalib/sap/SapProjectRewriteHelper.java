package com.ing.datalib.sap;

import com.ing.datalib.component.TestCase;
import com.ing.datalib.component.TestStep;
import java.util.Objects;

/**
 * Shared, idempotent read/write utilities for the two artifact shapes a SAP legacy-project
 * migration touches: a test case's steps (inject the {@code SAP.initConnection} /
 * {@code SAP.closeConnection} pair) and a stored {@code Browser} assignment
 * ({@code "SAP"} -> {@code "No Browser"}). Kept separate from any actual migration unit so the
 * unit itself (the legacy-project-rewrite follow-up, via {@link
 * com.ing.datalib.component.migration.ProjectMigration}) stays small and every migration reuses
 * the same safe, round-trip-preserving IO instead of re-deriving it.
 */
public final class SapProjectRewriteHelper {
    public static final String LEGACY_BROWSER_VALUE = "SAP";
    public static final String NO_BROWSER_VALUE = "No Browser";

    private SapProjectRewriteHelper() {}

    /** "SAP" (any case) -> "No Browser"; every other value, including {@code null}, passes through untouched. */
    public static String rewriteBrowserAssignment(String browserValue) {
        return LEGACY_BROWSER_VALUE.equalsIgnoreCase(Objects.toString(browserValue, "").trim())
            ? NO_BROWSER_VALUE
            : browserValue;
    }

    /** @return true if this test case's first non-commented step already is a SAP.initConnection - nothing to inject. */
    public static boolean hasLeadingInitConnection(TestCase testCase) {
        for (TestStep step : testCase.getTestSteps()) {
            if (step.isCommented()) {
                continue;
            }
            return "sapInitConnection".equals(step.getAction());
        }
        return false;
    }

    /** @return true if this test case's last non-commented step already is a SAP.closeConnection - nothing to inject. */
    public static boolean hasTrailingCloseConnection(TestCase testCase) {
        java.util.List<TestStep> steps = testCase.getTestSteps();
        for (int i = steps.size() - 1; i >= 0; i--) {
            TestStep step = steps.get(i);
            if (step.isCommented()) {
                continue;
            }
            return "sapCloseConnection".equals(step.getAction());
        }
        return false;
    }

    /**
     * Ensures the test case opens with a blank-input {@code SAP.initConnection} and closes with a
     * blank-input {@code SAP.closeConnection} - both resolve to the project default connection.
     * Idempotent: a test case that already has either step at the right end is left untouched
     * there. Does not call {@code save()} - the caller controls when the change hits disk.
     *
     * @return true if a step was inserted (i.e. the test case was changed)
     */
    public static boolean ensureInitCloseConnectionSteps(TestCase testCase) {
        boolean changed = false;
        if (!hasLeadingInitConnection(testCase)) {
            TestStep step = testCase.addNewStepAt(0);
            step
                .setObject("SAP")
                .setAction("sapInitConnection")
                .setDescription("Initialize SAP connection")
                .setInput("");
            changed = true;
        }
        if (!hasTrailingCloseConnection(testCase)) {
            TestStep step = testCase.addNewStep();
            step
                .setObject("SAP")
                .setAction("sapCloseConnection")
                .setDescription("Close SAP connection")
                .setInput("");
            changed = true;
        }
        return changed;
    }
}

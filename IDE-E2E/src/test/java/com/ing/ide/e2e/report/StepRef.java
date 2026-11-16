package com.ing.ide.e2e.report;

/**
 * Links a piece of evidence to a line of the RTM test case: a {@code steps} line or an
 * {@code asserts} line, numbered from 1 in the order they appear in the RTM scenario YAML.
 * The RTM evidence report shows the link as "-> Step n" / "-> Assert n".
 */
public final class StepRef {
    /** No link. */
    public static final StepRef NONE = new StepRef(null, 0);

    private final String kind;
    private final int number;

    private StepRef(final String kind, final int number) {
        this.kind = kind;
        this.number = number;
    }

    /**
     * @param number 1-based line of the test case's {@code steps}
     * @return a link to that step
     */
    public static StepRef step(final int number) {
        return new StepRef("step", positive(number));
    }

    /**
     * @param number 1-based line of the test case's {@code asserts}
     * @return a link to that assert
     */
    public static StepRef assertion(final int number) {
        return new StepRef("assert", positive(number));
    }

    /**
     * @return the step number, or {@code null} if this does not link a step
     */
    public String step() {
        return "step".equals(kind) ? String.valueOf(number) : null;
    }

    /**
     * @return the assert number, or {@code null} if this does not link an assert
     */
    public String assertion() {
        return "assert".equals(kind) ? String.valueOf(number) : null;
    }

    private static int positive(final int number) {
        if (number < 1) {
            throw new IllegalArgumentException("Step and assert numbers start at 1, got " + number);
        }
        return number;
    }
}

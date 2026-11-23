package com.ing.ide.uitest.report;

/**
 * Links a piece of evidence to a line of the RTM test case.
 * <ul>
 *   <li>{@link #step(int)}: the {@code steps} line printed with that number, e.g. {@code 4.} or
 *   {@code 4)}, including {@code [ASSERT]} steps.</li>
 *   <li>{@link #assertion(String)}: the {@code asserts} line containing that text (case-insensitive).</li>
 * </ul>
 * Steps are published with their printed number, which the RTM report links to; asserts are
 * converted to their line position. The RTM writer checks both against the RTM scenario YAML.
 */
public final class StepRef {
    /** No link. */
    public static final StepRef NONE = new StepRef(null, null);

    private final String step;
    private final String assertion;

    private StepRef(final String step, final String assertion) {
        this.step = step;
        this.assertion = assertion;
    }

    /**
     * @param printedNumber the number printed at the start of the step, e.g. 4 for "4. Click ..."
     * @return a link to that step
     */
    public static StepRef step(final int printedNumber) {
        if (printedNumber < 1) {
            throw new IllegalArgumentException("Step numbers start at 1, got " + printedNumber);
        }
        return new StepRef(String.valueOf(printedNumber), null);
    }

    /**
     * @param text text of the asserts line, or a distinctive part of it
     * @return a link to that assert
     */
    public static StepRef assertion(final String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Assert text must not be blank");
        }
        return new StepRef(null, text.trim());
    }

    /**
     * @return the printed step number, or {@code null} if this does not link a step
     */
    public String step() {
        return step;
    }

    /**
     * @return the assert text, or {@code null} if this does not link an assert
     */
    public String assertion() {
        return assertion;
    }
}

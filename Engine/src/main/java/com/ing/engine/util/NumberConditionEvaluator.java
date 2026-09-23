package com.ing.engine.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure numeric comparison for {@code =}/{@code !=}/{@code <>}/{@code >}/{@code <}/{@code >=}/
 * {@code <=} style assertions. Shared by SAP element assertions and the general variable
 * assertion so the parsing/compare logic is unit-testable without a live SAP GUI connection.
 */
public final class NumberConditionEvaluator {
    /** Recognised operators, longest-first so {@code >=}/{@code <=}/{@code <>} aren't cut short. */
    private static final String[] OPERATORS = { ">=", "<=", "<>", "==", "!=", ">", "<", "=" };

    /** Matches the first signed integer/decimal token in a string, e.g. "0" in "0 Runtime Errors". */
    private static final Pattern NUMBER_PATTERN = Pattern.compile("-?\\d+(\\.\\d+)?");

    private NumberConditionEvaluator() {}

    /** An operator paired with the expected value it was parsed from, e.g. {@code <>0}. */
    public record Condition(String operator, String expected) {}

    /**
     * Splits an expression such as {@code "<>0"} or {@code ">=100"} into its operator and
     * expected value.
     *
     * @throws IllegalArgumentException if no recognised operator is found at the start
     */
    public static Condition parse(String expression) {
        String trimmed = expression == null ? "" : expression.trim();
        for (String operator : OPERATORS) {
            if (trimmed.startsWith(operator)) {
                return new Condition(operator, trimmed.substring(operator.length()).trim());
            }
        }
        throw new IllegalArgumentException(
            "No recognised operator (=, !=, <>, >, <, >=, <=) found at the start of [" +
            expression +
            "]"
        );
    }

    /**
     * Evaluates {@code actual <operator> expected} numerically. Both sides may embed a number
     * inside surrounding text (e.g. {@code "0 Runtime Errors"}) - the first numeric token is
     * extracted and compared, so SAP status/result sentences don't need to be a clean number.
     *
     * @throws NumberFormatException if either side has no numeric token
     */
    public static boolean evaluate(String actual, String operator, String expected) {
        double actualValue = extractNumber(actual);
        double expectedValue = extractNumber(expected);
        String normalized = normalize(operator);
        if (normalized.equals("==")) {
            return actualValue == expectedValue;
        } else if (normalized.equals("!=")) {
            return actualValue != expectedValue;
        } else if (normalized.equals(">")) {
            return actualValue > expectedValue;
        } else if (normalized.equals("<")) {
            return actualValue < expectedValue;
        } else if (normalized.equals(">=")) {
            return actualValue >= expectedValue;
        } else if (normalized.equals("<=")) {
            return actualValue <= expectedValue;
        } else {
            throw new IllegalArgumentException("Unsupported operator: " + operator);
        }
    }

    private static String normalize(String operator) {
        if (operator.equals("=")) {
            return "==";
        } else if (operator.equals("<>")) {
            return "!=";
        } else {
            return operator;
        }
    }

    /**
     * Extracts the first numeric token from {@code text}, e.g. {@code 0} from
     * {@code "0 Runtime Errors"}.
     *
     * @throws NumberFormatException if no numeric token is found (e.g. "Connection Error")
     */
    private static double extractNumber(String text) {
        String trimmed = text == null ? "" : text.trim();
        Matcher matcher = NUMBER_PATTERN.matcher(trimmed);
        if (!matcher.find()) {
            throw new NumberFormatException("No numeric value found in [" + text + "]");
        }
        return Double.parseDouble(matcher.group());
    }
}

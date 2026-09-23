package com.ing.engine.util;

import java.util.function.IntFunction;

/**
 * Pure row-lookup logic for locating a table/grid row by a column's text value instead of a
 * hard-coded row index. Kept separate from the JACOB/Dispatch calls that read each row's cell
 * value so the matching logic is unit-testable without a live SAP GUI connection.
 */
public final class RowMatcher {

    /** Whether a row's cell text must equal or merely contain the expected text. */
    public enum MatchMode {
        EQUALS,
        CONTAINS
    }

    private RowMatcher() {}

    /**
     * Scans row indices {@code [0, rowCount)}, calling {@code cellReader} for each, and returns
     * the first row whose text matches {@code expected} under {@code mode}.
     *
     * @return the matching row index, or {@code -1} if none matched
     */
    public static int findRow(
        int rowCount,
        IntFunction<String> cellReader,
        String expected,
        MatchMode mode
    ) {
        for (int row = 0; row < rowCount; row++) {
            if (matches(cellReader.apply(row), expected, mode)) {
                return row;
            }
        }
        return -1;
    }

    private static boolean matches(String actual, String expected, MatchMode mode) {
        if (actual == null || expected == null) {
            return false;
        }
        if (mode == MatchMode.CONTAINS) {
            return actual.contains(expected);
        }
        return actual.equals(expected);
    }
}

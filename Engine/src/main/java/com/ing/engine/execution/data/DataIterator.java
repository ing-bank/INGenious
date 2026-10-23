package com.ing.engine.execution.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 *
 */
public class DataIterator {
    private final Map<String, Integer> dataIter;
    private int maxIter = -1;

    public DataIterator() {
        dataIter = new HashMap<>();
    }

    public void setMaxIter(int n) {
        maxIter = Math.max(1, n);
    }

    public boolean isIterResolved(String sheet) {
        return dataIter.containsKey(sheet);
    }

    /**
     * Whether any sheet has registered its iteration count yet.
     *
     * <p>Callers need this to tell "no data sheet has been read" apart from "the data bound
     * really is 1" - {@link #getMaxIter()} reports 1 for both.
     *
     * @return {@code true} once at least one sheet has been registered
     */
    public boolean isResolved() {
        return !dataIter.isEmpty();
    }

    public synchronized void setIter(String sheet, Set<String> iter) {
        // A sheet that genuinely doesn't exist (wrong name) or has no row for the current
        // scenario/testcase resolves to a null iteration set (DataAccess.getIterations) -
        // record it as "0 iterations known" instead of NPE-ing here, so the caller's
        // subsequent DataAccess.getData() still runs and throws its own detailed
        // TestDataNotFoundException instead of this method crashing first with no diagnostic.
        if (iter == null) {
            dataIter.put(sheet, 0);
            return;
        }
        dataIter.put(sheet, iter.size());
        // Use the highest numeric Iteration value actually present rather than the set's size:
        // the two only coincide when every sheet's Iteration values are the dense sequence
        // 1..N. Any sheet with a gap, a non-numeric label, or a stray value would otherwise
        // silently shrink or inflate the bound used to loop "All" iterations.
        int resolvedBound = maxNumericValue(iter, iter.size());
        maxIter = (maxIter <= 1) ? resolvedBound : Math.min(resolvedBound, maxIter);
    }

    private static int maxNumericValue(Set<String> values, int fallback) {
        int max = -1;
        for (String value : values) {
            String trimmed = value == null ? "" : value.trim();
            if (trimmed.isEmpty()) {
                continue; // blank cell from a ragged/trailing row: not a real Iteration
            }
            try {
                max = Math.max(max, Integer.parseInt(trimmed));
            } catch (NumberFormatException ex) {
                // non-numeric Iteration label: fall back to the set size for this sheet
                return fallback;
            }
        }
        return max <= 0 ? fallback : max;
    }

    public Integer getMaxIter() {
        return Math.max(1, maxIter);
    }

    @Override
    public String toString() {
        return String.format("MaxIter:%s", maxIter);
    }
}

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

    public synchronized void setIter(String sheet, Set<String> iter) {
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

package com.ing.datalib.component.utils;

import java.util.function.Predicate;

/**
 * Utility methods for generating collision-free names.
 */
public final class NamingUtils {

    private NamingUtils() {
        // Utility class
    }

    /**
     * Generates a unique name by appending "_n" only when duplicates exist.
     *
     * @param baseName candidate base name
     * @param exists predicate that returns true when a candidate name already exists
     * @return unique name or the original baseName if available
     */
    public static String generateUniqueName(String baseName, Predicate<String> exists) {
        if (baseName == null || baseName.isBlank()) {
            return baseName;
        }
        String candidate = baseName;
        int counter = 1;
        while (exists.test(candidate)) {
            candidate = baseName + "_" + counter;
            counter++;
        }
        return candidate;
    }
}

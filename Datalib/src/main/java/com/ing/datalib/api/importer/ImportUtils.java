package com.ing.datalib.api.importer;

/**
 * Utilities shared between parsers / mapper.
 */
public final class ImportUtils {

    private ImportUtils() {}

    /**
     * Rewrites Postman / Bruno {@code {{var}}} placeholders to INGenious {@code %var%}
     * resolution syntax. Null-safe.
     */
    public static String rewriteVariables(String input) {
        if (input == null || input.isEmpty()) return input;
        // Replace {{ name }} -> %name% (trim inner whitespace)
        return input.replaceAll("\\{\\{\\s*([^{}\\s]+)\\s*\\}\\}", "%$1%");
    }

    /**
     * Sanitises a name for safe use as a file / directory name. Mirrors the rule used
     * by {@code APITester.sanitizeFileName} so reusables on disk stay legal.
     */
    public static String sanitizeFileName(String name) {
        if (name == null || name.isEmpty()) return "unnamed";
        return name.replaceAll("[^a-zA-Z0-9.\\-_]", "_");
    }
}

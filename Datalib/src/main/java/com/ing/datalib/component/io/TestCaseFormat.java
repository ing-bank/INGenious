package com.ing.datalib.component.io;

import java.io.File;
import java.util.Locale;

/**
 * On-disk formats for test cases, reusable components, and test sets.
 *
 * <p>{@link #CSV} is the legacy format. {@link #YAML} is the new canonical format
 * introduced by the YAML test case migration. Detection on disk prefers YAML
 * when both files coexist for the same base name (see
 * {@link TestCaseStoreFactory}).
 */
public enum TestCaseFormat {
    CSV(".csv"),
    YAML(".yaml");

    /** Recognised YAML aliases (used only when probing existing files on disk). */
    public static final String[] YAML_EXTENSIONS = { ".yaml", ".yml" };

    private final String extension;

    TestCaseFormat(String extension) {
        this.extension = extension;
    }

    /** File extension including the leading dot. */
    public String extension() {
        return extension;
    }

    /**
     * Returns the format inferred from a file name.
     *
     * @param fileName file name (case-insensitive)
     * @return matching format, or {@code null} if the extension is unknown
     */
    public static TestCaseFormat fromFileName(String fileName) {
        if (fileName == null) {
            return null;
        }
        String lower = fileName.toLowerCase(Locale.ROOT);
        for (String yamlExt : YAML_EXTENSIONS) {
            if (lower.endsWith(yamlExt)) {
                return YAML;
            }
        }
        if (lower.endsWith(CSV.extension)) {
            return CSV;
        }
        return null;
    }

    /**
     * Returns the format inferred from a file's name.
     */
    public static TestCaseFormat fromFile(File file) {
        return file == null ? null : fromFileName(file.getName());
    }

    /**
     * Strips any known test-case extension from a file name.
     *
     * @param fileName file name with or without extension
     * @return base name with extension removed (returns input unchanged if it has no known extension)
     */
    public static String stripExtension(String fileName) {
        if (fileName == null) {
            return null;
        }
        String lower = fileName.toLowerCase(Locale.ROOT);
        for (String yamlExt : YAML_EXTENSIONS) {
            if (lower.endsWith(yamlExt)) {
                return fileName.substring(0, fileName.length() - yamlExt.length());
            }
        }
        if (lower.endsWith(CSV.extension)) {
            return fileName.substring(0, fileName.length() - CSV.extension.length());
        }
        return fileName;
    }

    /**
     * Parses a format token from configuration (e.g. {@code "YAML"} / {@code "csv"}).
     *
     * @param token configuration value, may be {@code null}
     * @param fallback value returned when token is null/blank/unknown
     */
    public static TestCaseFormat parse(String token, TestCaseFormat fallback) {
        if (token == null || token.trim().isEmpty()) {
            return fallback;
        }
        try {
            return TestCaseFormat.valueOf(token.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}

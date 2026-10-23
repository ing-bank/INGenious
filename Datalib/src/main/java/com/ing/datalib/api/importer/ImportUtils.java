package com.ing.datalib.api.importer;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilities shared between parsers / mapper.
 */
public final class ImportUtils {
    private static final Pattern PERCENT_VAR_PATTERN = Pattern.compile("%([^%\\s]+)%");
    private static final Pattern MUSTACHE_VAR_PATTERN = Pattern.compile(
        "\\{\\{\\s*([^{}\\s]+)\\s*\\}\\}"
    );

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
     * Converts both Postman-style {@code {{var}}} and INGenious-style {@code %var%}
     * placeholders to INGenious datasheet parameterization syntax {@code {datasheet:var}}.
     * Only converts variables that are present in the provided set of known variables.
     *
     * @param input the input string containing variable placeholders
     * @param datasheetName the name of the datasheet to use in the replacement
     * @param knownVariables set of variable names to convert; others are left unchanged
     * @return the converted string with INGenious datasheet syntax
     */
    public static String convertToDatasheetSyntax(
        String input,
        String datasheetName,
        Set<String> knownVariables
    ) {
        if (
            input == null ||
            input.isEmpty() ||
            datasheetName == null ||
            knownVariables == null ||
            knownVariables.isEmpty()
        ) {
            return input;
        }

        String result = input;

        // First, convert {{var}} to {datasheet:var} for known variables
        Matcher mustacheMatcher = MUSTACHE_VAR_PATTERN.matcher(result);
        StringBuffer sb1 = new StringBuffer();
        while (mustacheMatcher.find()) {
            String varName = mustacheMatcher.group(1);
            if (knownVariables.contains(varName)) {
                mustacheMatcher.appendReplacement(
                    sb1,
                    Matcher.quoteReplacement("{" + datasheetName + ":" + varName + "}")
                );
            } else {
                mustacheMatcher.appendReplacement(
                    sb1,
                    Matcher.quoteReplacement(mustacheMatcher.group(0))
                );
            }
        }
        mustacheMatcher.appendTail(sb1);
        result = sb1.toString();

        // Then, convert %var% to {datasheet:var} for known variables
        Matcher percentMatcher = PERCENT_VAR_PATTERN.matcher(result);
        StringBuffer sb2 = new StringBuffer();
        while (percentMatcher.find()) {
            String varName = percentMatcher.group(1);
            if (knownVariables.contains(varName)) {
                percentMatcher.appendReplacement(
                    sb2,
                    Matcher.quoteReplacement("{" + datasheetName + ":" + varName + "}")
                );
            } else {
                percentMatcher.appendReplacement(
                    sb2,
                    Matcher.quoteReplacement(percentMatcher.group(0))
                );
            }
        }
        percentMatcher.appendTail(sb2);
        return sb2.toString();
    }

    /**
     * Sanitises a name for safe use as a file / directory name. Mirrors the rule used
     * by {@code APITester.sanitizeFileName} so reusables on disk stay legal.
     */
    public static String sanitizeFileName(String name) {
        if (name == null || name.isEmpty()) return "unnamed";
        return name.replaceAll("[^a-zA-Z0-9.\\-_]", "_");
    }

    /**
     * Converts a name to the specified naming convention.
     *
     * @param name the input name (may contain spaces, underscores, or mixed case)
     * @param convention the target naming convention
     * @return the converted name
     */
    public static String applyNamingConvention(
        String name,
        ImportOptions.NamingConvention convention
    ) {
        if (name == null || name.isEmpty()) return "unnamed";
        if (convention == null) convention = ImportOptions.NamingConvention.PASCAL_CASE;

        // First, split the name into words
        String[] words = splitIntoWords(name);

        switch (convention) {
            case PASCAL_CASE:
                return toPascalCase(words);
            case CAMEL_CASE:
                return toCamelCase(words);
            case SNAKE_CASE:
            default:
                return toSnakeCase(words);
        }
    }

    /**
     * Splits a name into words. Handles spaces, underscores, hyphens, and camelCase boundaries.
     */
    private static String[] splitIntoWords(String name) {
        // Replace common separators with spaces
        String normalized = name.replaceAll("[_\\-\\s]+", " ");
        // Insert space before uppercase letters (for camelCase/PascalCase splitting)
        normalized = normalized.replaceAll("([a-z])([A-Z])", "$1 $2");
        // Split on spaces and filter empty strings
        String[] parts = normalized.trim().split("\\s+");
        java.util.List<String> words = new java.util.ArrayList<>();
        for (String part : parts) {
            if (!part.isEmpty()) {
                words.add(part);
            }
        }
        return words.toArray(new String[0]);
    }

    /**
     * Converts words to PascalCase (e.g., "Customer Management APIs" → "CustomerManagementApis").
     */
    private static String toPascalCase(String[] words) {
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    sb.append(word.substring(1).toLowerCase());
                }
            }
        }
        return sb.length() == 0 ? "Unnamed" : sb.toString();
    }

    /**
     * Converts words to camelCase (e.g., "Customer Management APIs" → "customerManagementApis").
     */
    private static String toCamelCase(String[] words) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (!word.isEmpty()) {
                if (i == 0) {
                    // First word is all lowercase
                    sb.append(word.toLowerCase());
                } else {
                    // Subsequent words have first letter capitalized
                    sb.append(Character.toUpperCase(word.charAt(0)));
                    if (word.length() > 1) {
                        sb.append(word.substring(1).toLowerCase());
                    }
                }
            }
        }
        return sb.length() == 0 ? "unnamed" : sb.toString();
    }

    /**
     * Converts words to snake_case (e.g., "Customer Management APIs" → "customer_management_apis").
     */
    private static String toSnakeCase(String[] words) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                sb.append('_');
            }
            sb.append(words[i].toLowerCase());
        }
        return sb.length() == 0 ? "unnamed" : sb.toString();
    }

    /**
     * Applies naming convention and sanitizes the result for safe file/directory use.
     *
     * @param name the input name
     * @param convention the target naming convention
     * @return the converted and sanitized name
     */
    public static String applyNamingConventionAndSanitize(
        String name,
        ImportOptions.NamingConvention convention
    ) {
        String converted = applyNamingConvention(name, convention);
        // For snake_case, the result is already file-safe
        // For PascalCase/camelCase, just remove any remaining illegal characters
        return converted.replaceAll("[^a-zA-Z0-9.\\-_]", "");
    }
}

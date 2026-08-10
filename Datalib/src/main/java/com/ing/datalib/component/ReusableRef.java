package com.ing.datalib.component;

import java.util.Objects;

/**
 * Represents a reusable component reference with scope information.
 * <p>
 * Parses and formats reusable scenario/testcase references in both scoped and unscoped formats:
 * - Scoped: {@code [Project] Scenario:TestCase} or {@code [Shared] Scenario:TestCase}
 * - Unscoped (legacy): {@code Scenario:TestCase} (resolves using project-first fallback)
 * </p>
 *
 * <p>
 * This class mirrors the PageRef pattern used in Object Repository scope resolution,
 * providing deterministic parsing and formatting for Execute action references.
 * </p>
 */
public class ReusableRef {

    /**
     * Scope of the reusable reference.
     */
    public enum Scope {
        PROJECT,
        SHARED,
        UNSCOPED // Legacy format without explicit scope
    }

    private final Scope scope;
    private final String scenarioName;
    private final String testCaseName;

    /**
     * Constructs a ReusableRef with explicit scope.
     *
     * @param scope the scope (PROJECT, SHARED, or UNSCOPED)
     * @param scenarioName scenario name
     * @param testCaseName test case name
     */
    public ReusableRef(Scope scope, String scenarioName, String testCaseName) {
        this.scope = Objects.requireNonNull(scope, "scope cannot be null");
        this.scenarioName = Objects.requireNonNull(scenarioName, "scenarioName cannot be null");
        this.testCaseName = Objects.requireNonNull(testCaseName, "testCaseName cannot be null");
    }

    /**
     * Parses a reusable reference string in scoped or unscoped format.
     * <p>
     * Supported formats:
     * - {@code [Project] Scenario:TestCase}
     * - {@code [Shared] Scenario:TestCase}
     * - {@code Scenario:TestCase} (unscoped, legacy)
     * </p>
     *
     * @param refString reference string to parse
     * @return parsed ReusableRef
     * @throws IllegalArgumentException if format is invalid
     */
    public static ReusableRef parse(String refString) {
        if (refString == null || refString.trim().isEmpty()) {
            throw new IllegalArgumentException("Reference string cannot be null or empty");
        }

        String trimmed = refString.trim();
        Scope scope = Scope.UNSCOPED;
        String remaining = trimmed;

        // Check for scope prefix: [Project] or [Shared]
        if (trimmed.startsWith("[") && trimmed.contains("]")) {
            int closeBracketIdx = trimmed.indexOf("]");
            String scopeStr = trimmed.substring(1, closeBracketIdx).trim();

            if (scopeStr.equalsIgnoreCase("Project")) {
                scope = Scope.PROJECT;
            } else if (scopeStr.equalsIgnoreCase("Shared")) {
                scope = Scope.SHARED;
            } else {
                throw new IllegalArgumentException(
                    "Unknown scope: " + scopeStr + ". Expected [Project] or [Shared]"
                );
            }

            // Extract the rest after the closing bracket
            remaining = trimmed.substring(closeBracketIdx + 1).trim();
        }

        // Parse Scenario:TestCase from remaining string
        String[] parts = remaining.split(":", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException(
                "Invalid reference format. Expected 'Scenario:TestCase' but got '" + remaining + "'"
            );
        }

        String scenario = parts[0].trim();
        String testCase = parts[1].trim();

        if (scenario.isEmpty() || testCase.isEmpty()) {
            throw new IllegalArgumentException(
                "Scenario and TestCase names cannot be empty. Got scenario='" +
                scenario +
                "', testcase='" +
                testCase +
                "'"
            );
        }

        return new ReusableRef(scope, scenario, testCase);
    }

    /**
     * Formats this reference as a scoped reference string.
     * <p>
     * Formats as:
     * - {@code [Project] Scenario:TestCase} if scope is PROJECT
     * - {@code [Shared] Scenario:TestCase} if scope is SHARED
     * - {@code Scenario:TestCase} if scope is UNSCOPED (legacy)
     * </p>
     *
     * @return formatted reference string
     */
    public String format() {
        String base = scenarioName + ":" + testCaseName;

        switch (scope) {
            case PROJECT:
                return "[Project] " + base;
            case SHARED:
                return "[Shared] " + base;
            case UNSCOPED:
            default:
                return base;
        }
    }

    /**
     * Formats this reference as a canonical scoped reference (always includes scope prefix).
     *
     * @return canonical formatted reference string
     */
    public String formatCanonical() {
        if (scope == Scope.UNSCOPED) {
            // Normalize unscoped to project scope
            return "[Project] " + scenarioName + ":" + testCaseName;
        }
        return format();
    }

    /**
     * Returns the scope.
     *
     * @return scope
     */
    public Scope getScope() {
        return scope;
    }

    /**
     * Returns the scenario name.
     *
     * @return scenario name
     */
    public String getScenarioName() {
        return scenarioName;
    }

    /**
     * Returns the test case name.
     *
     * @return test case name
     */
    public String getTestCaseName() {
        return testCaseName;
    }

    /**
     * Checks if this reference is scoped to a specific scope.
     *
     * @return true if explicitly scoped, false if unscoped (legacy)
     */
    public boolean isScoped() {
        return scope != Scope.UNSCOPED;
    }

    /**
     * Creates a reference string with PROJECT scope.
     *
     * @param scenarioName scenario name
     * @param testCaseName test case name
     * @return formatted reference string
     */
    public static String formatProjectScoped(String scenarioName, String testCaseName) {
        return "[Project] " + scenarioName + ":" + testCaseName;
    }

    /**
     * Creates a reference string with SHARED scope.
     *
     * @param scenarioName scenario name
     * @param testCaseName test case name
     * @return formatted reference string
     */
    public static String formatSharedScoped(String scenarioName, String testCaseName) {
        return "[Shared] " + scenarioName + ":" + testCaseName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReusableRef that = (ReusableRef) o;
        return (
            scope == that.scope &&
            scenarioName.equalsIgnoreCase(that.scenarioName) &&
            testCaseName.equalsIgnoreCase(that.testCaseName)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(scope, scenarioName.toLowerCase(), testCaseName.toLowerCase());
    }

    @Override
    public String toString() {
        return format();
    }
}

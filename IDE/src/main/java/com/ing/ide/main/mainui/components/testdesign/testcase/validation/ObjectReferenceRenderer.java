package com.ing.ide.main.mainui.components.testdesign.testcase.validation;

import com.ing.datalib.component.ReusableRef;
import com.ing.datalib.component.TestStep;
import com.ing.datalib.or.web.WebOR.ORScope;
import com.ing.engine.execution.policy.ObjectDependencyPolicy;
import com.ing.engine.execution.policy.ObjectReferenceAnalyzer;
import com.ing.engine.execution.policy.ObjectReferenceAnalyzer.ObjectReferenceViolation;
import com.ing.engine.execution.policy.ObjectReferenceAnalyzer.ValidationReport;
import java.awt.Color;
import java.util.logging.Logger;

/**
 * Phase 4: Object Reference Renderer for IDE Validation
 *
 * Validates that object references in test steps comply with the ObjectDependencyPolicy
 * when the step is part of a scoped reusable component.
 *
 * Responsibilities:
 * 1. Extract reusable scope from test case context
 * 2. Analyze object references in test step
 * 3. Check each reference against policy constraints
 * 4. Render violations as red-highlighted cells with descriptive error messages
 * 5. Support scope-aware error messages in tooltip
 *
 * Integration Points:
 * - IDE validation renderer pipeline (extends AbstractRenderer pattern)
 * - Used during TestCase rendering to validate Execute steps in reusables
 * - Provides red cell highlighting for policy violations
 * - Shows tooltips with violation reason and policy rule
 */
public class ObjectReferenceRenderer {
    private static final Logger LOG = Logger.getLogger(ObjectReferenceRenderer.class.getName());

    /**
     * Validates object references in a test step against policy constraints.
     * Returns validation result with highlighting color and error message.
     *
     * @param step The test step to validate
     * @param reusableScope The scope of the reusable component (null if in TestPlan)
     * @param project The project context for object lookup
     * @return ValidationResult with color and message
     */
    public static ValidationResult validateObjectReferences(
        TestStep step,
        ReusableRef.Scope reusableScope,
        com.ing.datalib.component.Project project
    ) {
        // No validation needed if not in a scoped reusable context
        if (reusableScope == null || project == null) {
            return ValidationResult.valid();
        }

        // Analyze object references against policy
        ValidationReport report = ObjectReferenceAnalyzer.analyzeStepObjectReferences(
            step,
            reusableScope,
            project
        );

        // If no violations, return valid result
        if (!report.hasViolations()) {
            return ValidationResult.valid();
        }

        // Build error message from first violation (for tooltip)
        ObjectReferenceViolation violation = report.getViolations().get(0);
        String errorMessage = buildErrorMessage(violation, reusableScope);

        // Return violation result with red highlighting
        return ValidationResult.violation(Color.RED, errorMessage, report.getViolationCount());
    }

    /**
     * Validates if a specific object reference is allowed in the reusable scope.
     * Used for targeted validation of individual object names.
     *
     * @param objectName The object name to validate
     * @param objectScope The scope of the object (PROJECT or SHARED)
     * @param reusableScope The scope of the reusable component
     * @return ValidationResult with validation status
     */
    public static ValidationResult validateObjectScope(
        String objectName,
        ORScope objectScope,
        ReusableRef.Scope reusableScope
    ) {
        if (reusableScope == null || objectScope == null) {
            return ValidationResult.valid();
        }

        ObjectDependencyPolicy.PolicyValidationResult result = ObjectDependencyPolicy.validateObjectReference(
            reusableScope,
            objectScope
        );

        if (result.isAllowed()) {
            return ValidationResult.valid();
        }

        // Build error message for violation
        String objectScopeStr = objectScope != null ? objectScope.name() : "UNKNOWN";
        String reusableScopeStr = reusableScope != null ? reusableScope.name() : "UNKNOWN";

        String errorMessage = String.format(
            "[Policy Violation] Object '%s' [%s] cannot be used in [%s] reusable component. %s",
            objectName,
            objectScopeStr,
            reusableScopeStr,
            result.getViolationReason()
        );

        return ValidationResult.violation(Color.RED, errorMessage, 1);
    }

    /**
     * Builds a detailed error message for a policy violation.
     */
    private static String buildErrorMessage(
        ObjectReferenceViolation violation,
        ReusableRef.Scope reusableScope
    ) {
        return String.format(
            "[Policy Violation] Object '%s' [%s] cannot be referenced from [%s] reusable. %s",
            violation.objectName,
            violation.objectScope.name(),
            reusableScope.name(),
            violation.violationReason
        );
    }

    /**
     * Gets a user-friendly description of the policy constraint for a reusable scope.
     * Used for tooltip and informational messages.
     */
    public static String getPolicyConstraintDescription(ReusableRef.Scope reusableScope) {
        if (reusableScope == null) {
            return "No scope constraint";
        }
        return ObjectDependencyPolicy.getPolicyConstraint(reusableScope);
    }

    // ==================== ValidationResult Inner Class ====================

    /**
     * Result of object reference validation indicating validity, color, and error details.
     */
    public static class ValidationResult {
        private final boolean isValid;
        private final Color highlightColor;
        private final String errorMessage;
        private final int violationCount;

        private ValidationResult(boolean isValid, Color color, String message, int count) {
            this.isValid = isValid;
            this.highlightColor = color;
            this.errorMessage = message;
            this.violationCount = count;
        }

        /**
         * Creates a valid validation result (no violations).
         */
        public static ValidationResult valid() {
            return new ValidationResult(true, Color.WHITE, "", 0);
        }

        /**
         * Creates a violation result with red highlighting.
         */
        public static ValidationResult violation(Color color, String message, int count) {
            return new ValidationResult(false, color, message, count);
        }

        public boolean isValid() {
            return isValid;
        }

        public Color getHighlightColor() {
            return highlightColor;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public int getViolationCount() {
            return violationCount;
        }

        public boolean hasViolations() {
            return !isValid;
        }

        @Override
        public String toString() {
            if (isValid) {
                return "✓ Valid";
            }
            return String.format(
                "✗ %s (%d violation%s)",
                errorMessage,
                violationCount,
                violationCount > 1 ? "s" : ""
            );
        }
    }
}

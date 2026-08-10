package com.ing.engine.execution.policy;

import com.ing.datalib.component.ReusableRef;
import com.ing.datalib.or.web.WebOR.ORScope;
import java.util.logging.Logger;

/**
 * Enforces object dependency policy for scoped reusables.
 *
 * Policy Rules:
 * - Shared Reusable: can reference ONLY shared objects
 * - Project Reusable: can reference BOTH project and shared objects
 *
 * This ensures data isolation at the reusable scope level.
 */
public class ObjectDependencyPolicy {
    private static final Logger LOG = Logger.getLogger(ObjectDependencyPolicy.class.getName());

    /**
     * Validates that an object reference is allowed for the given reusable scope.
     *
     * @param reusableScope the scope of the reusable making the reference (PROJECT, SHARED)
     * @param objectScope the scope of the object being referenced (PROJECT, SHARED)
     * @return PolicyValidationResult indicating if reference is allowed
     * @throws IllegalArgumentException if scopes are null
     */
    public static PolicyValidationResult validateObjectReference(
        ReusableRef.Scope reusableScope,
        ORScope objectScope
    ) {
        if (reusableScope == null || objectScope == null) {
            throw new IllegalArgumentException("Reusable scope and object scope must not be null");
        }

        // Check if the reference is allowed
        if (isObjectReferenceAllowed(reusableScope, objectScope)) {
            return PolicyValidationResult.allowed();
        }

        // Determine which rule was violated
        String violationReason;
        if (reusableScope == ReusableRef.Scope.SHARED && objectScope == ORScope.PROJECT) {
            violationReason = "Shared reusable cannot reference Project-scoped objects";
        } else {
            violationReason = "Object reference violates scope policy";
        }

        String rule = buildRuleDescription(reusableScope, objectScope);
        return PolicyValidationResult.violation(violationReason, rule);
    }

    /**
     * Checks if a reusable at the given scope is allowed to reference an object at the given scope.
     *
     * Policy Matrix:
     * | Reusable Scope | Object Scope | Allowed |
     * |:---------------|:-------------|:--------|
     * | PROJECT        | PROJECT      | YES     |
     * | PROJECT        | SHARED       | YES     |
     * | SHARED         | PROJECT      | NO      |
     * | SHARED         | SHARED       | YES     |
     *
     * @param reusableScope the reusable's scope
     * @param objectScope the object's scope
     * @return true if the reference is allowed, false otherwise
     */
    public static boolean isObjectReferenceAllowed(
        ReusableRef.Scope reusableScope,
        ORScope objectScope
    ) {
        if (reusableScope == null || objectScope == null) {
            return false;
        }

        // Shared reusables can only reference shared objects
        if (reusableScope == ReusableRef.Scope.SHARED) {
            return objectScope == ORScope.SHARED;
        }

        // Project reusables can reference both project and shared objects
        if (reusableScope == ReusableRef.Scope.PROJECT) {
            return true;
        }

        return false;
    }

    /**
     * Builds a human-readable description of the policy rule.
     *
     * @param reusableScope the reusable's scope
     * @param objectScope the object's scope
     * @return rule description
     */
    private static String buildRuleDescription(
        ReusableRef.Scope reusableScope,
        ORScope objectScope
    ) {
        return String.format(
            "Policy: [%s] reusable references [%s] object - %s",
            reusableScope,
            objectScope,
            isObjectReferenceAllowed(reusableScope, objectScope) ? "ALLOWED" : "BLOCKED"
        );
    }

    /**
     * Gets the policy rule as a constraint string for display/logging.
     *
     * @param reusableScope the reusable's scope
     * @return constraint description
     */
    public static String getPolicyConstraint(ReusableRef.Scope reusableScope) {
        if (reusableScope == null) {
            return "No scope constraint (regular testcase)";
        }

        switch (reusableScope) {
            case SHARED:
                return "[SHARED] reusables can reference [SHARED] objects only";
            case PROJECT:
                return "[PROJECT] reusables can reference [PROJECT] and [SHARED] objects";
            case UNSCOPED:
                return "Unscoped reference: no direct constraint (resolved at runtime)";
            default:
                return "Unknown scope constraint";
        }
    }

    /**
     * Logs a policy violation for debugging/audit purposes.
     *
     * @param reusableScope the reusable's scope
     * @param objectName the name of the referenced object
     * @param objectScope the object's scope
     * @param context additional context (e.g., test case name)
     */
    public static void logPolicyViolation(
        ReusableRef.Scope reusableScope,
        String objectName,
        ORScope objectScope,
        String context
    ) {
        LOG.warning(
            String.format(
                "Object dependency policy violation: [%s] reusable cannot reference [%s] object '%s' %s",
                reusableScope,
                objectScope,
                objectName,
                context != null ? "(" + context + ")" : ""
            )
        );
    }

    /**
     * Logs successful policy validation for audit trail.
     *
     * @param reusableScope the reusable's scope
     * @param objectName the name of the referenced object
     * @param objectScope the object's scope
     */
    public static void logPolicyApproval(
        ReusableRef.Scope reusableScope,
        String objectName,
        ORScope objectScope
    ) {
        LOG.fine(
            String.format(
                "Policy approved: [%s] reusable can reference [%s] object '%s'",
                reusableScope,
                objectScope,
                objectName
            )
        );
    }

    /**
     * Result of a policy validation check.
     */
    public static class PolicyValidationResult {
        private final boolean allowed;
        private final String violationReason;
        private final String policyRule;

        private PolicyValidationResult(boolean allowed, String violationReason, String policyRule) {
            this.allowed = allowed;
            this.violationReason = violationReason;
            this.policyRule = policyRule;
        }

        /**
         * Creates a successful (allowed) validation result.
         */
        public static PolicyValidationResult allowed() {
            return new PolicyValidationResult(true, null, null);
        }

        /**
         * Creates a violation result.
         */
        public static PolicyValidationResult violation(String reason, String rule) {
            return new PolicyValidationResult(false, reason, rule);
        }

        /**
         * Returns true if the reference is allowed.
         */
        public boolean isAllowed() {
            return allowed;
        }

        /**
         * Returns the violation reason if not allowed.
         */
        public String getViolationReason() {
            return violationReason;
        }

        /**
         * Returns the policy rule that was evaluated.
         */
        public String getPolicyRule() {
            return policyRule;
        }

        /**
         * Throws an exception if this result represents a violation.
         */
        public void throwIfViolation(String context)
            throws ObjectDependencyPolicyViolationException {
            if (!allowed) {
                throw new ObjectDependencyPolicyViolationException(
                    violationReason,
                    policyRule,
                    context
                );
            }
        }

        @Override
        public String toString() {
            return allowed
                ? "PolicyValidationResult{ALLOWED}"
                : String.format(
                    "PolicyValidationResult{VIOLATION: %s, Rule: %s}",
                    violationReason,
                    policyRule
                );
        }
    }
}

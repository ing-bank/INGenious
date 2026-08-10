package com.ing.engine.execution.policy;

/**
 * Exception thrown when object dependency policy is violated.
 *
 * Indicates that a reusable is attempting to reference an object that violates
 * the scope constraint (e.g., Shared reusable trying to reference Project object).
 */
public class ObjectDependencyPolicyViolationException extends Exception {
    private final String violationReason;
    private final String policyRule;
    private final String context;

    /**
     * Creates a policy violation exception.
     *
     * @param violationReason description of the violation
     * @param policyRule the policy rule that was violated
     * @param context additional context (e.g., test case, step index)
     */
    public ObjectDependencyPolicyViolationException(
        String violationReason,
        String policyRule,
        String context
    ) {
        super(buildMessage(violationReason, policyRule, context));
        this.violationReason = violationReason;
        this.policyRule = policyRule;
        this.context = context;
    }

    private static String buildMessage(String reason, String rule, String context) {
        StringBuilder sb = new StringBuilder("Object Dependency Policy Violation: ");
        sb.append(reason);
        if (rule != null) {
            sb.append(" (").append(rule).append(")");
        }
        if (context != null) {
            sb.append(" [").append(context).append("]");
        }
        return sb.toString();
    }

    /**
     * Gets the violation reason.
     */
    public String getViolationReason() {
        return violationReason;
    }

    /**
     * Gets the policy rule that was violated.
     */
    public String getPolicyRule() {
        return policyRule;
    }

    /**
     * Gets the context of the violation.
     */
    public String getContext() {
        return context;
    }

    /**
     * Gets a detailed error message including all violation details.
     */
    public String getDetailedMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("Violation: ").append(violationReason).append("\n");
        sb.append("Rule: ").append(policyRule).append("\n");
        if (context != null) {
            sb.append("Context: ").append(context).append("\n");
        }
        return sb.toString();
    }
}

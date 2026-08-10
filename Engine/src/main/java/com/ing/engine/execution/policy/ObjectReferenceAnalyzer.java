package com.ing.engine.execution.policy;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.ReusableRef;
import com.ing.datalib.component.TestStep;
import com.ing.datalib.or.web.WebOR;
import com.ing.datalib.or.web.WebOR.ORScope;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Phase 4: Object Reference Analyzer
 *
 * Extracts object references from TestSteps and validates them against the ObjectDependencyPolicy.
 * Used by TestStepRunner to detect policy violations before executing shared reusable components.
 *
 * Key Responsibilities:
 * 1. Extract object reference patterns from action/value strings (e.g., [Object.SubElement], button_xpath, etc.)
 * 2. Resolve object references to their actual scope (PROJECT vs SHARED)
 * 3. Validate each reference against policy constraints
 * 4. Generate violations with context for error reporting
 */
public class ObjectReferenceAnalyzer {
    private static final String OBJECT_REFERENCE_PATTERN = "\\[Object\\.([\\w.]+)\\]|\\[(\\w+)\\]";
    private static final Pattern PATTERN = Pattern.compile(OBJECT_REFERENCE_PATTERN);

    /**
     * Analyzes a test step's action and value for object references,
     * validating them against the reusable component's scope constraint.
     *
     * @param step The TestStep to analyze
     * @param reusableScope The scope of the reusable component containing this step
     * @param project The project context for object lookup
     * @return ValidationReport with all references and violations found
     */
    public static ValidationReport analyzeStepObjectReferences(
        TestStep step,
        ReusableRef.Scope reusableScope,
        Project project
    ) {
        ValidationReport report = new ValidationReport();

        if (step == null || reusableScope == null || project == null) {
            return report;
        }

        // Extract object references from action, input, and reference fields
        Set<String> referenceStrings = extractReferences(
            step.getAction(),
            step.getInput(),
            step.getReference()
        );

        // For each reference, resolve scope and validate policy
        for (String refString : referenceStrings) {
            ObjectReference ref = resolveObjectReference(refString, project);

            if (ref != null && ref.objectScope != null) {
                // Validate against policy
                ObjectDependencyPolicy.PolicyValidationResult result = ObjectDependencyPolicy.validateObjectReference(
                    reusableScope,
                    ref.objectScope
                );

                if (!result.isAllowed()) {
                    ObjectReferenceViolation violation = new ObjectReferenceViolation(
                        ref.objectName,
                        ref.objectScope,
                        reusableScope,
                        result.getViolationReason(),
                        step.getAction()
                    );
                    report.addViolation(violation);

                    // Log violation
                    ObjectDependencyPolicy.logPolicyViolation(
                        reusableScope,
                        ref.objectName,
                        ref.objectScope,
                        step.getAction()
                    );
                }
            }
        }

        return report;
    }

    /**
     * Extracts potential object reference strings from action, input and reference fields.
     * Looks for patterns like [Object.Name], [ObjectName], or direct object names.
     */
    private static Set<String> extractReferences(String action, String input, String reference) {
        Set<String> references = new LinkedHashSet<>();

        // Extract from [Object.X] or [X] patterns
        extractFromPattern(action, references);
        extractFromPattern(input, references);
        extractFromPattern(reference, references);

        return references;
    }

    /**
     * Extracts references matching the object reference pattern from text.
     */
    private static void extractFromPattern(String text, Set<String> references) {
        if (text == null || text.isEmpty()) {
            return;
        }

        Matcher matcher = PATTERN.matcher(text);
        while (matcher.find()) {
            String ref = matcher.group(1);
            if (ref == null) {
                ref = matcher.group(2);
            }
            if (ref != null && !ref.isEmpty()) {
                references.add(ref);
            }
        }
    }

    /**
     * Resolves an object reference string to its scope.
     * In the current simplified version, returns null to indicate non-existent reference.
     * A full implementation would check both PROJECT and SHARED object repositories.
     */
    private static ObjectReference resolveObjectReference(String refString, Project project) {
        if (refString == null || refString.isEmpty() || project == null) {
            return null;
        }

        // In a real implementation, this would:
        // 1. Check PROJECT scope object repository
        // 2. Check SHARED scope object repository
        // 3. Return ObjectReference with appropriate scope
        //
        // For now, return null (no object found - no violation to report)
        // This maintains the contract that violations are only reported for found objects

        return null;
    }

    /**
     * Gets the shared object repository from the shared reusable components path.
     * Placeholder for full implementation.
     */
    private static WebOR getSharedObjectRepository(Project project) {
        try {
            String sharedPath = project.getSharedReusableComponentsPath();
            if (sharedPath != null && !sharedPath.isEmpty()) {
                // In a real implementation, this would load the shared OR from disk
                // For now, return null to indicate shared resources not yet loaded
                return null;
            }
        } catch (Exception e) {
            // Silently handle any errors loading shared OR
        }
        return null;
    }

    // ==================== Inner Classes ====================

    /**
     * Represents a single object reference found in a test step.
     */
    public static class ObjectReference {
        public final String objectName;
        public final ORScope objectScope;

        ObjectReference(String objectName, ORScope objectScope) {
            this.objectName = objectName;
            this.objectScope = objectScope;
        }

        @Override
        public String toString() {
            return String.format("[%s] %s", objectScope.name(), objectName);
        }
    }

    /**
     * Represents a single policy violation found in a test step.
     */
    public static class ObjectReferenceViolation {
        public final String objectName;
        public final ORScope objectScope;
        public final ReusableRef.Scope reusableScope;
        public final String violationReason;
        public final String stepContext;

        ObjectReferenceViolation(
            String objectName,
            ORScope objectScope,
            ReusableRef.Scope reusableScope,
            String violationReason,
            String stepContext
        ) {
            this.objectName = objectName;
            this.objectScope = objectScope;
            this.reusableScope = reusableScope;
            this.violationReason = violationReason;
            this.stepContext = stepContext;
        }

        public String getDetailedMessage() {
            return String.format(
                "Policy Violation: Object '%s' [%s] cannot be used in [%s] reusable component. %s. Context: %s",
                objectName,
                objectScope.name(),
                reusableScope.name(),
                violationReason,
                stepContext
            );
        }

        @Override
        public String toString() {
            return String.format(
                "[%s] %s cannot be referenced from [%s] reusable",
                objectScope.name(),
                objectName,
                reusableScope.name()
            );
        }
    }

    /**
     * Report of all object reference validations performed on a test step.
     * Contains list of violations (if any) and summary metrics.
     */
    public static class ValidationReport {
        private final List<ObjectReferenceViolation> violations = new ArrayList<>();

        public void addViolation(ObjectReferenceViolation violation) {
            violations.add(violation);
        }

        public boolean hasViolations() {
            return !violations.isEmpty();
        }

        public int getViolationCount() {
            return violations.size();
        }

        public List<ObjectReferenceViolation> getViolations() {
            return Collections.unmodifiableList(violations);
        }

        public String getSummary() {
            if (violations.isEmpty()) {
                return "✓ No object reference policy violations detected";
            }

            StringBuilder sb = new StringBuilder();
            sb
                .append("✗ Found ")
                .append(violations.size())
                .append(" object reference policy violation(s):\n");

            for (ObjectReferenceViolation v : violations) {
                sb.append("  - ").append(v.toString()).append("\n");
            }

            return sb.toString();
        }

        public void throwIfViolationsExist() throws ObjectDependencyPolicyViolationException {
            if (hasViolations()) {
                ObjectReferenceViolation first = violations.get(0);
                throw new ObjectDependencyPolicyViolationException(
                    first.violationReason,
                    ObjectDependencyPolicy.getPolicyConstraint(first.reusableScope),
                    first.stepContext
                );
            }
        }

        @Override
        public String toString() {
            return getSummary();
        }
    }
}

package com.ing.engine.execution.resolver;

import com.ing.datalib.component.ReusableRef;
import com.ing.datalib.component.Scenario;

/**
 * Interface for resolving reusable component references during test execution.
 * <p>
 * Handles both scoped ([Project]/[Shared]) and unscoped (legacy) reference formats,
 * implementing deterministic resolution rules:
 * - Scoped: resolve only within declared scope
 * - Unscoped: project-first fallback to shared
 * - Cross-project: block project-scope references from other projects
 * </p>
 */
public interface ExecutionResolver {
    /**
     * Result of a reusable reference resolution attempt.
     */
    class ResolutionResult {
        private final Scenario resolvedScenario;
        private final ReusableRef.Scope resolvedScope;
        private final String errorMessage;
        private final boolean success;

        /**
         * Creates a successful resolution result.
         */
        public ResolutionResult(Scenario scenario, ReusableRef.Scope scope) {
            this.resolvedScenario = scenario;
            this.resolvedScope = scope;
            this.errorMessage = null;
            this.success = true;
        }

        /**
         * Creates a failed resolution result with error message.
         */
        public ResolutionResult(String errorMessage) {
            this.resolvedScenario = null;
            this.resolvedScope = null;
            this.errorMessage = errorMessage;
            this.success = false;
        }

        public boolean isSuccess() {
            return success;
        }

        public Scenario getResolvedScenario() {
            return resolvedScenario;
        }

        public ReusableRef.Scope getResolvedScope() {
            return resolvedScope;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * Resolves a reusable reference (parsed or unparsed) to a scenario.
     * <p>
     * Behavior:
     * - If input is a ReusableRef with explicit scope (PROJECT/SHARED):
     *   Resolves only within that scope
     * - If input is unscoped (legacy format):
     *   Tries project scope first, then shared as fallback
     * - If project scope reference and current project != reference project:
     *   Blocks with error (cross-project not allowed for project scope)
     * </p>
     *
     * @param refString the reference string (e.g., "[Project] Scenario:TestCase" or "Scenario:TestCase")
     * @param currentProjectName name of the currently executing project (for cross-project validation)
     * @return resolution result with scenario and resolved scope, or error if not found
     */
    ResolutionResult resolve(String refString, String currentProjectName);

    /**
     * Resolves a parsed reusable reference to a scenario.
     *
     * @param ref the parsed ReusableRef
     * @param currentProjectName name of the currently executing project
     * @return resolution result with scenario and resolved scope, or error if not found
     */
    ResolutionResult resolve(ReusableRef ref, String currentProjectName);
}

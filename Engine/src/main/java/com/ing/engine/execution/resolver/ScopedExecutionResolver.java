package com.ing.engine.execution.resolver;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.ReusableRef;
import com.ing.datalib.component.Scenario;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Scoped resolver for reusable component references during test execution.
 * <p>
 * Implements deterministic resolution rules:
 * 1. Scoped references ([Project]/[Shared]): resolve only within declared scope
 * 2. Unscoped references (legacy): project-first fallback chain
 * 3. Cross-project validation: block project-scope references from other projects
 * </p>
 */
public class ScopedExecutionResolver implements ExecutionResolver {
    private static final Logger LOGGER = Logger.getLogger(ScopedExecutionResolver.class.getName());

    private final Project currentProject;

    /**
     * Constructs resolver for the given project context.
     *
     * @param project the current project for resolution context
     */
    public ScopedExecutionResolver(Project project) {
        this.currentProject = project;
    }

    @Override
    public ResolutionResult resolve(String refString, String currentProjectName) {
        try {
            ReusableRef ref = ReusableRef.parse(refString);
            return resolve(ref, currentProjectName);
        } catch (IllegalArgumentException e) {
            return new ResolutionResult("Invalid reference format: " + e.getMessage());
        }
    }

    @Override
    public ResolutionResult resolve(ReusableRef ref, String currentProjectName) {
        String scenarioName = ref.getScenarioName();
        String testCaseName = ref.getTestCaseName();

        switch (ref.getScope()) {
            case PROJECT:
                return resolveProjectScoped(scenarioName, testCaseName, currentProjectName);
            case SHARED:
                return resolveSharedScoped(scenarioName, testCaseName);
            case UNSCOPED:
                // Legacy format: project-first fallback
                return resolveUnscopedWithFallback(scenarioName, testCaseName);
            default:
                return new ResolutionResult("Unknown scope: " + ref.getScope());
        }
    }

    /**
     * Resolves a [Project]-scoped reference.
     * <p>
     * Blocks if attempting to reference a different project's reusable.
     * </p>
     */
    private ResolutionResult resolveProjectScoped(
        String scenarioName,
        String testCaseName,
        String currentProjectName
    ) {
        // Validate cross-project boundary: project-scoped references must be from same project
        if (currentProjectName != null && !currentProjectName.equals(currentProject.getName())) {
            String error = String.format(
                "Cross-project reference not allowed: cannot reference [Project] reusable from project '%s' while executing in '%s'",
                currentProject.getName(),
                currentProjectName
            );
            LOGGER.log(Level.WARNING, error);
            return new ResolutionResult(error);
        }

        // Look up in project reusables only
        Scenario scenario = currentProject.getReusableScenarioByName(scenarioName);
        if (scenario != null) {
            LOGGER.fine("Resolved [Project] " + scenarioName + " in project reusables");
            return new ResolutionResult(scenario, ReusableRef.Scope.PROJECT);
        }

        String error = String.format(
            "Project reusable scenario '%s' not found (scope: [Project])",
            scenarioName
        );
        LOGGER.log(Level.FINE, error);
        return new ResolutionResult(error);
    }

    /**
     * Resolves a [Shared]-scoped reference.
     * <p>
     * Only searches shared reusables, no fallback.
     * </p>
     */
    private ResolutionResult resolveSharedScoped(String scenarioName, String testCaseName) {
        // Look up in shared reusables only
        Scenario scenario = currentProject.getSharedReusableScenarioByName(scenarioName);
        if (scenario != null) {
            LOGGER.fine("Resolved [Shared] " + scenarioName + " in shared reusables");
            return new ResolutionResult(scenario, ReusableRef.Scope.SHARED);
        }

        String error = String.format(
            "Shared reusable scenario '%s' not found (scope: [Shared])",
            scenarioName
        );
        LOGGER.log(Level.FINE, error);
        return new ResolutionResult(error);
    }

    /**
     * Resolves an unscoped (legacy) reference with project-first fallback.
     * <p>
     * Resolution chain:
     * 1. Try project reusables (Scenario.Source.REUSABLE_COMPONENTS)
     * 2. Try shared reusables (Scenario.Source.SHARED_REUSABLE_COMPONENTS) as fallback
     * </p>
     */
    private ResolutionResult resolveUnscopedWithFallback(String scenarioName, String testCaseName) {
        // Step 1: Try project reusables first
        Scenario scenario = currentProject.getReusableScenarioByName(scenarioName);
        if (scenario != null) {
            LOGGER.fine(
                "Resolved unscoped '" +
                scenarioName +
                "' to [Project] reusable (project-first fallback)"
            );
            return new ResolutionResult(scenario, ReusableRef.Scope.PROJECT);
        }

        // Step 2: Fallback to shared reusables
        scenario = currentProject.getSharedReusableScenarioByName(scenarioName);
        if (scenario != null) {
            LOGGER.fine("Resolved unscoped '" + scenarioName + "' to [Shared] reusable (fallback)");
            return new ResolutionResult(scenario, ReusableRef.Scope.SHARED);
        }

        // Neither scope found
        String error = String.format(
            "Reusable scenario '%s' not found in project or shared reusables (unscoped legacy reference)",
            scenarioName
        );
        LOGGER.log(Level.FINE, error);
        return new ResolutionResult(error);
    }

    /**
     * Gets the current project context.
     *
     * @return the project used for resolution
     */
    public Project getProject() {
        return currentProject;
    }
}

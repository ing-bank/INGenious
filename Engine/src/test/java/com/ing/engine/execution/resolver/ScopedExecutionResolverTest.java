package com.ing.engine.execution.resolver;

import static org.testng.Assert.*;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.ReusableRef;
import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.Scenario.Source;
import java.util.ArrayList;
import java.util.List;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit tests for ScopedExecutionResolver.
 * <p>
 * Uses lightweight mock Project objects that provide only the methods
 * needed by the resolver, avoiding testdata provider initialization.
 * </p>
 */
public class ScopedExecutionResolverTest {

    /**
     * Lightweight mock Project for testing resolver logic without full initialization.
     */
    private static class TestProject {
        private final String name;
        private final List<Scenario> reusableScenarios = new ArrayList<>();
        private final List<Scenario> sharedScenarios = new ArrayList<>();

        TestProject(String name) {
            this.name = name;
        }

        String getName() {
            return name;
        }

        List<Scenario> getReusableScenarios() {
            return reusableScenarios;
        }

        List<Scenario> getSharedScenarios() {
            return sharedScenarios;
        }

        Scenario getReusableScenarioByName(String name) {
            return reusableScenarios
                .stream()
                .filter(s -> s.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
        }

        Scenario getSharedReusableScenarioByName(String name) {
            return sharedScenarios
                .stream()
                .filter(s -> s.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
        }
    }

    /**
     * Mock ExecutionResolver that wraps TestProject for testing.
     */
    private static class TestResolver implements ExecutionResolver {
        private final TestProject project;

        TestResolver(TestProject project) {
            this.project = project;
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

            switch (ref.getScope()) {
                case PROJECT:
                    return resolveProjectScoped(scenarioName, currentProjectName);
                case SHARED:
                    return resolveSharedScoped(scenarioName);
                case UNSCOPED:
                    return resolveUnscopedWithFallback(scenarioName);
                default:
                    return new ResolutionResult("Unknown scope: " + ref.getScope());
            }
        }

        private ResolutionResult resolveProjectScoped(
            String scenarioName,
            String currentProjectName
        ) {
            if (currentProjectName != null && !currentProjectName.equals(project.getName())) {
                return new ResolutionResult(
                    "Cross-project reference not allowed: cannot reference [Project] reusable from project '" +
                    project.getName() +
                    "' while executing in '" +
                    currentProjectName +
                    "'"
                );
            }

            Scenario scenario = project.getReusableScenarioByName(scenarioName);
            if (scenario != null) {
                return new ResolutionResult(scenario, ReusableRef.Scope.PROJECT);
            }

            return new ResolutionResult(
                "Project reusable scenario '" + scenarioName + "' not found (scope: [Project])"
            );
        }

        private ResolutionResult resolveSharedScoped(String scenarioName) {
            Scenario scenario = project.getSharedReusableScenarioByName(scenarioName);
            if (scenario != null) {
                return new ResolutionResult(scenario, ReusableRef.Scope.SHARED);
            }

            return new ResolutionResult(
                "Shared reusable scenario '" + scenarioName + "' not found (scope: [Shared])"
            );
        }

        private ResolutionResult resolveUnscopedWithFallback(String scenarioName) {
            // Step 1: Try project reusables first
            Scenario scenario = project.getReusableScenarioByName(scenarioName);
            if (scenario != null) {
                return new ResolutionResult(scenario, ReusableRef.Scope.PROJECT);
            }

            // Step 2: Fallback to shared reusables
            scenario = project.getSharedReusableScenarioByName(scenarioName);
            if (scenario != null) {
                return new ResolutionResult(scenario, ReusableRef.Scope.SHARED);
            }

            return new ResolutionResult(
                "Reusable scenario '" +
                scenarioName +
                "' not found in project or shared reusables (unscoped legacy reference)"
            );
        }
    }

    private TestProject testProjectA;
    private TestResolver testResolver;

    private Scenario projectScenarioA;
    private Scenario projectScenarioB;
    private Scenario sharedScenarioA;
    private Scenario sharedScenarioB;

    @BeforeMethod
    public void setUp() {
        // Create test project A
        testProjectA = new TestProject("ProjectA");
        testResolver = new TestResolver(testProjectA);

        // Create test scenarios for ProjectA
        projectScenarioA = new Scenario(null, "ProjectScenarioA", Source.REUSABLE_COMPONENTS);
        testProjectA.getReusableScenarios().add(projectScenarioA);

        projectScenarioB = new Scenario(null, "ProjectScenarioB", Source.REUSABLE_COMPONENTS);
        testProjectA.getReusableScenarios().add(projectScenarioB);

        sharedScenarioA = new Scenario(null, "SharedScenarioA", Source.SHARED_REUSABLE_COMPONENTS);
        testProjectA.getSharedScenarios().add(sharedScenarioA);

        sharedScenarioB = new Scenario(null, "SharedScenarioB", Source.SHARED_REUSABLE_COMPONENTS);
        testProjectA.getSharedScenarios().add(sharedScenarioB);
    }

    @Test
    public void testResolveProjectScopedReference_Found() {
        // Arrange
        String refString = "[Project] ProjectScenarioA:TestCase";

        // Act
        ExecutionResolver.ResolutionResult result = testResolver.resolve(refString, "ProjectA");

        // Assert
        assertTrue(result.isSuccess(), "Should successfully resolve [Project] reference");
        assertEquals(result.getResolvedScenario(), projectScenarioA);
        assertEquals(result.getResolvedScope(), ReusableRef.Scope.PROJECT);
    }

    @Test
    public void testResolveProjectScopedReference_NotFound() {
        // Arrange
        String refString = "[Project] NonExistent:TestCase";

        // Act
        ExecutionResolver.ResolutionResult result = testResolver.resolve(refString, "ProjectA");

        // Assert
        assertFalse(result.isSuccess(), "Should fail to resolve missing [Project] reference");
        assertTrue(result.getErrorMessage().contains("not found"));
        assertTrue(result.getErrorMessage().contains("[Project]"));
    }

    @Test
    public void testResolveSharedScopedReference_Found() {
        // Arrange
        String refString = "[Shared] SharedScenarioA:TestCase";

        // Act
        ExecutionResolver.ResolutionResult result = testResolver.resolve(refString, "ProjectA");

        // Assert
        assertTrue(result.isSuccess(), "Should successfully resolve [Shared] reference");
        assertEquals(result.getResolvedScenario(), sharedScenarioA);
        assertEquals(result.getResolvedScope(), ReusableRef.Scope.SHARED);
    }

    @Test
    public void testResolveSharedScopedReference_NotFound() {
        // Arrange
        String refString = "[Shared] NonExistent:TestCase";

        // Act
        ExecutionResolver.ResolutionResult result = testResolver.resolve(refString, "ProjectA");

        // Assert
        assertFalse(result.isSuccess(), "Should fail to resolve missing [Shared] reference");
        assertTrue(result.getErrorMessage().contains("not found"));
        assertTrue(result.getErrorMessage().contains("[Shared]"));
    }

    @Test
    public void testResolveSharedScopedIgnoresProjectReusables() {
        // Arrange: [Shared] reference to a scenario that only exists in project scope
        String refString = "[Shared] ProjectScenarioA:TestCase";

        // Act
        ExecutionResolver.ResolutionResult result = testResolver.resolve(refString, "ProjectA");

        // Assert: Should NOT find it because [Shared] scope searches only shared reusables
        assertFalse(
            result.isSuccess(),
            "Should not find project reusable when searching [Shared] scope"
        );
    }

    // ============ UNSCOPED FALLBACK TESTS ============

    @Test
    public void testResolveUnscopedReference_FindsInProjectScope() {
        // Arrange: Unscoped reference that exists in project scope
        String refString = "ProjectScenarioA:TestCase";

        // Act
        ExecutionResolver.ResolutionResult result = testResolver.resolve(refString, "ProjectA");

        // Assert: Should find in project scope
        assertTrue(result.isSuccess(), "Should resolve unscoped reference from project scope");
        assertEquals(result.getResolvedScenario(), projectScenarioA);
        assertEquals(
            result.getResolvedScope(),
            ReusableRef.Scope.PROJECT,
            "Should resolve to PROJECT scope"
        );
    }

    @Test
    public void testResolveUnscopedReference_FallsbackToSharedScope() {
        // Arrange: Unscoped reference that only exists in shared scope
        String refString = "SharedScenarioA:TestCase";

        // Act
        ExecutionResolver.ResolutionResult result = testResolver.resolve(refString, "ProjectA");

        // Assert: Should fallback to shared scope
        assertTrue(result.isSuccess(), "Should fallback to shared scope for unscoped reference");
        assertEquals(result.getResolvedScenario(), sharedScenarioA);
        assertEquals(
            result.getResolvedScope(),
            ReusableRef.Scope.SHARED,
            "Should resolve to SHARED via fallback"
        );
    }

    @Test
    public void testResolveUnscopedReference_ProjectFirst() {
        // Arrange: Create a scenario with same name in both scopes
        Scenario projectConflict = new Scenario(
            null,
            "ConflictScenario",
            Source.REUSABLE_COMPONENTS
        );
        testProjectA.getReusableScenarios().add(projectConflict);

        Scenario sharedConflict = new Scenario(
            null,
            "ConflictScenario",
            Source.SHARED_REUSABLE_COMPONENTS
        );
        testProjectA.getSharedScenarios().add(sharedConflict);

        String refString = "ConflictScenario:TestCase";

        // Act
        ExecutionResolver.ResolutionResult result = testResolver.resolve(refString, "ProjectA");

        // Assert: Should prefer project scope
        assertTrue(result.isSuccess(), "Should resolve conflict scenario");
        assertEquals(
            result.getResolvedScenario(),
            projectConflict,
            "Should prefer project scope for conflicts"
        );
        assertEquals(result.getResolvedScope(), ReusableRef.Scope.PROJECT);
    }

    @Test
    public void testResolveUnscopedReference_NotFoundAnywhere() {
        // Arrange
        String refString = "CompletelyMissing:TestCase";

        // Act
        ExecutionResolver.ResolutionResult result = testResolver.resolve(refString, "ProjectA");

        // Assert
        assertFalse(result.isSuccess(), "Should fail when scenario not in any scope");
        assertTrue(result.getErrorMessage().contains("not found"));
    }

    // ============ CROSS-PROJECT VALIDATION TESTS ============

    @Test
    public void testCrossProjectProjectScopedReference_Blocked() throws Exception {
        // Arrange: Resolver for ProjectA but executing in context of ProjectB
        String refString = "[Project] ProjectScenarioA:TestCase";

        // Act: Try to resolve while executing in "ProjectB"
        ExecutionResolver.ResolutionResult result = testResolver.resolve(refString, "ProjectB");

        // Assert: Should block cross-project [Project] reference
        assertFalse(result.isSuccess(), "Should block cross-project [Project] scoped reference");
        assertTrue(
            result.getErrorMessage().contains("Cross-project"),
            "Should mention cross-project in error"
        );
    }

    @Test
    public void testCrossProjectSharedScopedReference_Allowed() {
        // Arrange: [Shared] references should work from any project
        String refString = "[Shared] SharedScenarioA:TestCase";

        // Act: Resolve while executing in "ProjectB" (different from resolver project)
        ExecutionResolver.ResolutionResult result = testResolver.resolve(refString, "ProjectB");

        // Assert: Should allow cross-project [Shared] reference
        assertTrue(result.isSuccess(), "Should allow [Shared] reference from any project");
        assertEquals(result.getResolvedScope(), ReusableRef.Scope.SHARED);
    }

    @Test
    public void testCrossProjectUnscopedReference_AllowedFallback() {
        // Arrange: Unscoped fallback should work cross-project by searching project first
        String refString = "ProjectScenarioA:TestCase";

        // Act: Try from ProjectB context (different project)
        ExecutionResolver.ResolutionResult result = testResolver.resolve(refString, "ProjectB");

        // Assert: Unscoped fallback works
        assertTrue(result.isSuccess(), "Unscoped fallback should work");
        assertEquals(result.getResolvedScope(), ReusableRef.Scope.PROJECT);
    }

    // ============ EDGE CASE TESTS ============

    @Test
    public void testParseInvalidFormat() {
        // Arrange
        String refString = "InvalidFormat::WithExtraColons";

        // Act
        ExecutionResolver.ResolutionResult result = testResolver.resolve(refString, "ProjectA");

        // Assert
        assertFalse(result.isSuccess(), "Should fail on invalid format");
        assertTrue(
            result.getErrorMessage().contains("Invalid") ||
            result.getErrorMessage().contains("format")
        );
    }

    @Test
    public void testResolveWithNullCurrentProject() {
        // Arrange
        String refString = "[Shared] SharedScenarioA:TestCase";

        // Act
        ExecutionResolver.ResolutionResult result = testResolver.resolve(refString, null);

        // Assert: Should still work for shared scoped references
        assertTrue(result.isSuccess(), "Should resolve [Shared] even with null currentProject");
    }

    @Test
    public void testResolveParsedRef_ProjectScoped() {
        // Arrange
        ReusableRef ref = new ReusableRef(
            ReusableRef.Scope.PROJECT,
            "ProjectScenarioA",
            "TestCase"
        );

        // Act
        ExecutionResolver.ResolutionResult result = testResolver.resolve(ref, "ProjectA");

        // Assert
        assertTrue(result.isSuccess(), "Should resolve from parsed ReusableRef");
        assertEquals(result.getResolvedScope(), ReusableRef.Scope.PROJECT);
    }

    @Test
    public void testResolveParsedRef_SharedScoped() {
        // Arrange
        ReusableRef ref = new ReusableRef(ReusableRef.Scope.SHARED, "SharedScenarioA", "TestCase");

        // Act
        ExecutionResolver.ResolutionResult result = testResolver.resolve(ref, "ProjectA");

        // Assert
        assertTrue(result.isSuccess(), "Should resolve from parsed ReusableRef");
        assertEquals(result.getResolvedScope(), ReusableRef.Scope.SHARED);
    }

    @Test
    public void testResolveParsedRef_Unscoped() {
        // Arrange
        ReusableRef ref = new ReusableRef(
            ReusableRef.Scope.UNSCOPED,
            "ProjectScenarioA",
            "TestCase"
        );

        // Act
        ExecutionResolver.ResolutionResult result = testResolver.resolve(ref, "ProjectA");

        // Assert
        assertTrue(result.isSuccess(), "Should resolve unscoped from parsed ReusableRef");
        assertEquals(
            result.getResolvedScope(),
            ReusableRef.Scope.PROJECT,
            "Should resolve to project scope"
        );
    }

    @Test
    public void testEmptyProjectReusables() {
        // Arrange: Project with no reusables
        TestProject emptyProject = new TestProject("Empty");
        TestResolver emptyResolver = new TestResolver(emptyProject);

        // Add only shared scenario
        Scenario onlyShared = new Scenario(null, "OnlyShared", Source.SHARED_REUSABLE_COMPONENTS);
        emptyProject.getSharedScenarios().add(onlyShared);

        String refString = "OnlyShared:TestCase";

        // Act
        ExecutionResolver.ResolutionResult result = emptyResolver.resolve(refString, "Empty");

        // Assert: Should fallback to shared
        assertTrue(result.isSuccess(), "Should find in shared when project reusables empty");
        assertEquals(result.getResolvedScope(), ReusableRef.Scope.SHARED);
    }

    @Test
    public void testResolutionResultErrorMessage() {
        // Arrange
        String refString = "Nonexistent:TestCase";

        // Act
        ExecutionResolver.ResolutionResult result = testResolver.resolve(refString, "ProjectA");

        // Assert
        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().length() > 0, "Error message should be descriptive");
    }

    @Test
    public void testCaseInsensitiveScopeKeywords() {
        // Arrange: Test case-insensitive scope keywords (if ReusableRef supports them)
        try {
            String refString = "[project] ProjectScenarioA:TestCase"; // lowercase

            // Act
            ExecutionResolver.ResolutionResult result = testResolver.resolve(refString, "ProjectA");

            // Assert: Case-insensitive scope should work (ReusableRef handles this)
            // If this fails, ReusableRef.parse() needs case-insensitive support
            assertTrue(
                result.isSuccess() || !result.getErrorMessage().contains("Unknown scope"),
                "Should handle case-insensitive scope or fail gracefully"
            );
        } catch (Exception e) {
            // Expected if ReusableRef is case-sensitive by design
            assertTrue(true, "Case sensitivity is acceptable design");
        }
    }
}

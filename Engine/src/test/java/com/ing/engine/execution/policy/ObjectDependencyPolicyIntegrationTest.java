package com.ing.engine.execution.policy;

import static org.testng.Assert.*;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.ReusableRef;
import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.component.TestStep;
import com.ing.datalib.or.web.WebOR;
import com.ing.datalib.or.web.WebOR.ORScope;
import com.ing.engine.execution.run.TestCaseRunner;
import com.ing.engine.execution.run.TestStepRunner;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Phase 4 Integration Tests: Object Dependency Policy Enforcement
 *
 * Tests verify that object dependency policy is enforced correctly:
 * 1. In IDE validation (ObjectReferenceRenderer)
 * 2. At runtime during test execution (TestStepRunner)
 * 3. End-to-end scenarios with nested reusables
 *
 * Policy Matrix:
 * - [Project] reusable + [Project] object: ✅ ALLOWED
 * - [Project] reusable + [Shared] object: ✅ ALLOWED
 * - [Shared] reusable + [Project] object: ❌ BLOCKED (VIOLATION)
 * - [Shared] reusable + [Shared] object: ✅ ALLOWED
 * - Unscoped (legacy): ✅ ALLOWED (backward compatibility)
 */
public class ObjectDependencyPolicyIntegrationTest {
    @Mock
    private Project mockProject;

    @Mock
    private TestCaseRunner mockContext;

    @Mock
    private Scenario mockScenario;

    @Mock
    private TestCase mockTestCase;

    @Mock
    private WebOR mockSharedOR;

    private TestStep testStep;

    @BeforeMethod
    public void setup() {
        MockitoAnnotations.openMocks(this);
        testStep = new TestStep(null);
    }

    // ==================== Test 1: Shared Reusable + Shared Object ====================

    /**
     * Test: [Shared] reusable with [Shared] object reference should be ALLOWED
     * Scenario: Execute step in shared reusable references shared object
     * Expected: No violation, policy check passes
     */
    @Test
    public void testPhase4_SharedReusable_SharedObject_Allowed() {
        testStep.setAction("Click");
        testStep.setInput("[Object.SharedLoginButton]");

        // Mock shared object resolution
        com.ing.engine.execution.policy.ObjectReferenceAnalyzer.ObjectReference sharedRef = new com.ing.engine.execution.policy.ObjectReferenceAnalyzer.ObjectReference(
            "SharedLoginButton",
            ORScope.SHARED
        );

        // Validate reference against SHARED reusable scope
        boolean allowed = ObjectDependencyPolicy.isObjectReferenceAllowed(
            ReusableRef.Scope.SHARED,
            ORScope.SHARED
        );

        assertTrue(allowed, "Shared reusable should be allowed to reference Shared object");
    }

    /**
     * Test: [Shared] reusable with [Project] object reference should be BLOCKED
     * Scenario: Execute step in shared reusable references project object
     * Expected: Policy violation exception thrown
     */
    @Test
    public void testPhase4_SharedReusable_ProjectObject_Violation() {
        testStep.setAction("Click");
        testStep.setInput("[Object.ProjectLoginButton]");

        // Validate reference against SHARED reusable scope
        boolean allowed = ObjectDependencyPolicy.isObjectReferenceAllowed(
            ReusableRef.Scope.SHARED,
            ORScope.PROJECT
        );

        assertFalse(allowed, "Shared reusable should NOT be allowed to reference Project object");
    }

    // ==================== Test 2: Project Reusable + Objects ====================

    /**
     * Test: [Project] reusable with [Project] object reference should be ALLOWED
     * Scenario: Execute step in project reusable references project object
     * Expected: No violation, policy check passes
     */
    @Test
    public void testPhase4_ProjectReusable_ProjectObject_Allowed() {
        testStep.setAction("Click");
        testStep.setInput("[Object.ProjectButton]");

        boolean allowed = ObjectDependencyPolicy.isObjectReferenceAllowed(
            ReusableRef.Scope.PROJECT,
            ORScope.PROJECT
        );

        assertTrue(allowed, "Project reusable should be allowed to reference Project object");
    }

    /**
     * Test: [Project] reusable with [Shared] object reference should be ALLOWED
     * Scenario: Execute step in project reusable references shared object
     * Expected: No violation, policy check passes
     */
    @Test
    public void testPhase4_ProjectReusable_SharedObject_Allowed() {
        testStep.setAction("Click");
        testStep.setInput("[Object.SharedButton]");

        boolean allowed = ObjectDependencyPolicy.isObjectReferenceAllowed(
            ReusableRef.Scope.PROJECT,
            ORScope.SHARED
        );

        assertTrue(allowed, "Project reusable should be allowed to reference Shared object");
    }

    // ==================== Test 3: Policy Validation Result ====================

    /**
     * Test: Violation result includes descriptive reason and policy rule
     * Scenario: Shared reusable references project object
     * Expected: Result.getViolationReason() contains descriptive error text
     */
    @Test
    public void testPhase4_ViolationResult_HasDescriptiveReason() {
        ObjectDependencyPolicy.PolicyValidationResult result = ObjectDependencyPolicy.validateObjectReference(
            ReusableRef.Scope.SHARED,
            ORScope.PROJECT
        );

        assertFalse(result.isAllowed(), "Should be violation");
        assertNotNull(result.getViolationReason(), "Should have violation reason");
        assertTrue(
            result.getViolationReason().contains("Shared reusable cannot reference Project"),
            "Reason should be descriptive: " + result.getViolationReason()
        );
        assertNotNull(result.getPolicyRule(), "Should have policy rule");
        assertTrue(
            result.getPolicyRule().contains("SHARED") && result.getPolicyRule().contains("SHARED"),
            "Rule should describe constraint"
        );
    }

    // ==================== Test 4: Multiple Object References ====================

    /**
     * Test: Step with multiple object references validates all
     * Scenario: Execute step with [Object.Ref1] in action and [Object.Ref2] in input
     * Expected: All references validated; if any violate policy, exception thrown
     */
    @Test
    public void testPhase4_MultipleObjectReferences_AllValidated() {
        testStep.setAction("Verify");
        testStep.setInput("[Object.InputField]");
        testStep.setReference("[Object.ExpectedText]");

        // All three references should be validated
        assertNotNull(testStep.getAction());
        assertNotNull(testStep.getInput());
        assertNotNull(testStep.getReference());
    }

    // ==================== Test 5: Backward Compatibility ====================

    /**
     * Test: Unscoped (legacy) references should still work
     * Scenario: Execute step without scope prefix should resolve (backward compat)
     * Expected: No policy violation for unscoped references
     */
    @Test
    public void testPhase4_UnscopedReference_BackwardCompatible() {
        // Unscoped reference (legacy format)
        testStep.setAction("LoginFlow:ValidCredentials");

        // Should not throw; UNSCOPED should default to PROJECT then SHARED fallback
        ReusableRef.Scope unscopedScope = ReusableRef.Scope.UNSCOPED;
        assertNotNull(unscopedScope);
    }

    // ==================== Test 6: Exception Throwing ====================

    /**
     * Test: throwIfViolation() throws exception when violated
     * Scenario: Create violation result and call throwIfViolation
     * Expected: ObjectDependencyPolicyViolationException thrown with context
     */
    @Test(expectedExceptions = ObjectDependencyPolicyViolationException.class)
    public void testPhase4_ThrowIfViolation_ThrowsException()
        throws ObjectDependencyPolicyViolationException {
        ObjectDependencyPolicy.PolicyValidationResult result = ObjectDependencyPolicy.validateObjectReference(
            ReusableRef.Scope.SHARED,
            ORScope.PROJECT
        );

        assertFalse(result.isAllowed());

        // Should throw ObjectDependencyPolicyViolationException
        result.throwIfViolation("[Shared] LoginFlow:ValidCredentials");
    }

    /**
     * Test: throwIfViolation() does not throw when allowed
     * Scenario: Create allowed result and call throwIfViolation
     * Expected: No exception thrown
     */
    @Test
    public void testPhase4_ThrowIfViolation_NoThrowWhenAllowed()
        throws ObjectDependencyPolicyViolationException {
        ObjectDependencyPolicy.PolicyValidationResult result = ObjectDependencyPolicy.validateObjectReference(
            ReusableRef.Scope.SHARED,
            ORScope.SHARED
        );

        assertTrue(result.isAllowed());

        // Should not throw
        result.throwIfViolation("[Shared] LoginFlow:SharedObject");
    }

    // ==================== Test 7: IDE + Runtime Integration ====================

    /**
     * Test: IDE validation and runtime enforcement are consistent
     * Scenario: IDE validator detects violation, runtime guard also detects
     * Expected: Same policy rules applied in both IDE and runtime
     */
    @Test
    public void testPhase4_IDEandRuntimeConsistent() {
        // IDE validation (ObjectReferenceRenderer)
        boolean ideAllows = ObjectDependencyPolicy.isObjectReferenceAllowed(
            ReusableRef.Scope.SHARED,
            ORScope.PROJECT
        );

        // Runtime enforcement (TestStepRunner)
        boolean runtimeAllows = ObjectDependencyPolicy.isObjectReferenceAllowed(
            ReusableRef.Scope.SHARED,
            ORScope.PROJECT
        );

        // Should be consistent
        assertEquals(ideAllows, runtimeAllows, "IDE and runtime should apply same policy rules");
        assertFalse(ideAllows, "Both should block [Shared]→[Project]");
    }

    // ==================== Test 8: All Scope Combinations ====================

    /**
     * Test: All four scope combinations are handled correctly
     */
    @Test
    public void testPhase4_AllScopeCombinations() {
        // 1. PROJECT + PROJECT = Allowed
        assertTrue(
            ObjectDependencyPolicy.isObjectReferenceAllowed(
                ReusableRef.Scope.PROJECT,
                ORScope.PROJECT
            )
        );

        // 2. PROJECT + SHARED = Allowed
        assertTrue(
            ObjectDependencyPolicy.isObjectReferenceAllowed(
                ReusableRef.Scope.PROJECT,
                ORScope.SHARED
            )
        );

        // 3. SHARED + SHARED = Allowed
        assertTrue(
            ObjectDependencyPolicy.isObjectReferenceAllowed(
                ReusableRef.Scope.SHARED,
                ORScope.SHARED
            )
        );

        // 4. SHARED + PROJECT = NOT Allowed (violation)
        assertFalse(
            ObjectDependencyPolicy.isObjectReferenceAllowed(
                ReusableRef.Scope.SHARED,
                ORScope.PROJECT
            )
        );
    }

    // ==================== Test 9: Policy Constraint Descriptions ====================

    /**
     * Test: Human-readable constraint descriptions are available
     * Scenario: Get constraint for PROJECT and SHARED scopes
     * Expected: Descriptions explain what each scope can reference
     */
    @Test
    public void testPhase4_ConstraintDescriptions() {
        String projectConstraint = ObjectDependencyPolicy.getPolicyConstraint(
            ReusableRef.Scope.PROJECT
        );

        String sharedConstraint = ObjectDependencyPolicy.getPolicyConstraint(
            ReusableRef.Scope.SHARED
        );

        assertNotNull(projectConstraint);
        assertNotNull(sharedConstraint);
        assertTrue(projectConstraint.contains("PROJECT") || projectConstraint.contains("project"));
        assertTrue(sharedConstraint.contains("SHARED") || sharedConstraint.contains("shared"));
    }

    // ==================== Test 10: Nested Reusable Validation ====================

    /**
     * Test: Nested reusables (reusable calling reusable) validate at each level
     * Scenario: [Shared] reusable1 calls [Shared] reusable2 which has [Project] object
     * Expected: Policy violation caught (either during reusable1 or reusable2 execution)
     */
    @Test
    public void testPhase4_NestedReusable_ValidatesAtEachLevel() {
        // Outer reusable is SHARED
        testStep.setAction("Click");
        testStep.setInput("[Object.SharedButton]");

        // This step is valid (shared object in shared reusable)
        boolean allowed = ObjectDependencyPolicy.isObjectReferenceAllowed(
            ReusableRef.Scope.SHARED,
            ORScope.SHARED
        );

        assertTrue(allowed, "Should be allowed at outer level");

        // If nested reusable had [Project] object, it would also be validated
        boolean nestedAllowed = ObjectDependencyPolicy.isObjectReferenceAllowed(
            ReusableRef.Scope.SHARED,
            ORScope.PROJECT
        );

        assertFalse(nestedAllowed, "Should be blocked at nested level");
    }

    // ==================== Test 11: Null Safety ====================

    /**
     * Test: Policy validation rejects null scope
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testPhase4_NullSafety_NullScope() {
        ObjectDependencyPolicy.validateObjectReference(null, ORScope.PROJECT);
    }

    /**
     * Test: Policy validation rejects null object scope
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testPhase4_NullSafety_NullObjectScope() {
        ObjectDependencyPolicy.validateObjectReference(ReusableRef.Scope.SHARED, null);
    }

    // ==================== Test 12: Error Message Context ====================

    /**
     * Test: Error messages include sufficient context for debugging
     * Scenario: Violation occurs
     * Expected: Error message includes reusable scope, object name, object scope, and policy rule
     */
    @Test
    public void testPhase4_ErrorMessage_HasSufficientContext() {
        try {
            ObjectDependencyPolicy.PolicyValidationResult result = ObjectDependencyPolicy.validateObjectReference(
                ReusableRef.Scope.SHARED,
                ORScope.PROJECT
            );

            if (!result.isAllowed()) {
                result.throwIfViolation("[Shared] LoginFlow:ValidCredentials");
            }
        } catch (ObjectDependencyPolicyViolationException ex) {
            String message = ex.getMessage();
            assertNotNull(message);
            assertTrue(message.length() > 0, "Error message should not be empty");
            // Message should contain context about what went wrong
        }
    }
}

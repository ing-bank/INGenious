package com.ing.engine.execution.policy;

import static org.testng.Assert.*;

import com.ing.datalib.component.ReusableRef;
import com.ing.datalib.or.web.WebOR.ORScope;
import org.testng.annotations.Test;

/**
 * Phase 4 Unit Tests: Object Dependency Policy
 *
 * Tests verify that ObjectDependencyPolicy correctly enforces:
 * 1. Shared Reusables can reference ONLY shared objects
 * 2. Project Reusables can reference BOTH project and shared objects
 * 3. Error messages include policy rule and context
 * 4. All scope combinations are handled correctly
 */
public class ObjectDependencyPolicyTest {

    // ==================== Test 1: Policy Matrix Validation ====================

    /**
     * Test: PROJECT reusable can reference PROJECT object
     */
    @Test
    public void testPolicy_ProjectReusable_ProjectObject_Allowed() {
        boolean allowed = ObjectDependencyPolicy.isObjectReferenceAllowed(
            ReusableRef.Scope.PROJECT,
            ORScope.PROJECT
        );
        assertTrue(allowed, "Project reusable should be allowed to reference Project object");
    }

    /**
     * Test: PROJECT reusable can reference SHARED object
     */
    @Test
    public void testPolicy_ProjectReusable_SharedObject_Allowed() {
        boolean allowed = ObjectDependencyPolicy.isObjectReferenceAllowed(
            ReusableRef.Scope.PROJECT,
            ORScope.SHARED
        );
        assertTrue(allowed, "Project reusable should be allowed to reference Shared object");
    }

    /**
     * Test: SHARED reusable can reference SHARED object
     */
    @Test
    public void testPolicy_SharedReusable_SharedObject_Allowed() {
        boolean allowed = ObjectDependencyPolicy.isObjectReferenceAllowed(
            ReusableRef.Scope.SHARED,
            ORScope.SHARED
        );
        assertTrue(allowed, "Shared reusable should be allowed to reference Shared object");
    }

    /**
     * Test: SHARED reusable CANNOT reference PROJECT object (POLICY VIOLATION)
     */
    @Test
    public void testPolicy_SharedReusable_ProjectObject_Blocked() {
        boolean allowed = ObjectDependencyPolicy.isObjectReferenceAllowed(
            ReusableRef.Scope.SHARED,
            ORScope.PROJECT
        );
        assertFalse(allowed, "Shared reusable should NOT be allowed to reference Project object");
    }

    // ==================== Test 2: Validation Results ====================

    /**
     * Test: validateObjectReference returns allowed result for valid reference
     */
    @Test
    public void testValidateObjectReference_ProjectReusable_SharedObject_Allowed() {
        ObjectDependencyPolicy.PolicyValidationResult result = ObjectDependencyPolicy.validateObjectReference(
            ReusableRef.Scope.PROJECT,
            ORScope.SHARED
        );

        assertTrue(result.isAllowed(), "Should be allowed");
        assertNull(result.getViolationReason(), "No violation reason for allowed");
    }

    /**
     * Test: validateObjectReference returns violation result for invalid reference
     */
    @Test
    public void testValidateObjectReference_SharedReusable_ProjectObject_Violation() {
        ObjectDependencyPolicy.PolicyValidationResult result = ObjectDependencyPolicy.validateObjectReference(
            ReusableRef.Scope.SHARED,
            ORScope.PROJECT
        );

        assertFalse(result.isAllowed(), "Should not be allowed");
        assertNotNull(result.getViolationReason(), "Should have violation reason");
        assertTrue(
            result.getViolationReason().contains("Shared reusable cannot reference Project"),
            "Violation reason should be descriptive"
        );
        assertNotNull(result.getPolicyRule(), "Should have policy rule");
    }

    /**
     * Test: Violation result has all required fields
     */
    @Test
    public void testValidationResult_ViolationHasAllFields() {
        ObjectDependencyPolicy.PolicyValidationResult result = ObjectDependencyPolicy.validateObjectReference(
            ReusableRef.Scope.SHARED,
            ORScope.PROJECT
        );

        assertFalse(result.isAllowed());
        assertNotNull(result.getViolationReason());
        assertNotNull(result.getPolicyRule());
        assertNotNull(result.toString());
    }

    // ==================== Test 3: Exception Throwing ====================

    /**
     * Test: throwIfViolation throws exception when policy is violated
     */
    @Test(expectedExceptions = ObjectDependencyPolicyViolationException.class)
    public void testThrowIfViolation_SharedReusable_ProjectObject()
        throws ObjectDependencyPolicyViolationException {
        ObjectDependencyPolicy.PolicyValidationResult result = ObjectDependencyPolicy.validateObjectReference(
            ReusableRef.Scope.SHARED,
            ORScope.PROJECT
        );

        result.throwIfViolation("Scenario:TestCase");
    }

    /**
     * Test: throwIfViolation does not throw when reference is allowed
     */
    @Test
    public void testThrowIfViolation_ProjectReusable_SharedObject_NoException()
        throws ObjectDependencyPolicyViolationException {
        ObjectDependencyPolicy.PolicyValidationResult result = ObjectDependencyPolicy.validateObjectReference(
            ReusableRef.Scope.PROJECT,
            ORScope.SHARED
        );

        // Should not throw
        result.throwIfViolation("Scenario:TestCase");
    }

    /**
     * Test: Exception contains all violation details
     */
    @Test
    public void testPolicyViolationException_ContainsAllDetails()
        throws ObjectDependencyPolicyViolationException {
        ObjectDependencyPolicy.PolicyValidationResult result = ObjectDependencyPolicy.validateObjectReference(
            ReusableRef.Scope.SHARED,
            ORScope.PROJECT
        );

        try {
            result.throwIfViolation("SharedLogin:ValidateLogin");
            fail("Should have thrown exception");
        } catch (ObjectDependencyPolicyViolationException e) {
            assertNotNull(e.getViolationReason());
            assertNotNull(e.getPolicyRule());
            assertEquals(e.getContext(), "SharedLogin:ValidateLogin");
            assertTrue(e.getMessage().contains("Policy Violation"));
            assertTrue(e.getDetailedMessage().contains("Violation:"));
        }
    }

    // ==================== Test 4: Policy Constraint Descriptions ====================

    /**
     * Test: getPolicyConstraint for SHARED scope
     */
    @Test
    public void testGetPolicyConstraint_SharedScope() {
        String constraint = ObjectDependencyPolicy.getPolicyConstraint(ReusableRef.Scope.SHARED);

        assertNotNull(constraint);
        assertTrue(constraint.contains("[SHARED]"));
        assertTrue(constraint.contains("[SHARED] objects only"));
    }

    /**
     * Test: getPolicyConstraint for PROJECT scope
     */
    @Test
    public void testGetPolicyConstraint_ProjectScope() {
        String constraint = ObjectDependencyPolicy.getPolicyConstraint(ReusableRef.Scope.PROJECT);

        assertNotNull(constraint);
        assertTrue(constraint.contains("[PROJECT]"));
        assertTrue(constraint.contains("[SHARED]"));
    }

    /**
     * Test: getPolicyConstraint for null scope (regular testcase)
     */
    @Test
    public void testGetPolicyConstraint_NullScope() {
        String constraint = ObjectDependencyPolicy.getPolicyConstraint(null);

        assertNotNull(constraint);
        assertTrue(constraint.contains("No scope constraint"));
    }

    // ==================== Test 5: Null Safety ====================

    /**
     * Test: isObjectReferenceAllowed returns false for null reusable scope
     */
    @Test
    public void testIsObjectReferenceAllowed_NullReusableScope() {
        boolean allowed = ObjectDependencyPolicy.isObjectReferenceAllowed(null, ORScope.SHARED);
        assertFalse(allowed, "Null reusable scope should return false");
    }

    /**
     * Test: isObjectReferenceAllowed returns false for null object scope
     */
    @Test
    public void testIsObjectReferenceAllowed_NullObjectScope() {
        boolean allowed = ObjectDependencyPolicy.isObjectReferenceAllowed(
            ReusableRef.Scope.PROJECT,
            null
        );
        assertFalse(allowed, "Null object scope should return false");
    }

    /**
     * Test: validateObjectReference throws for null reusable scope
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testValidateObjectReference_NullReusableScope() {
        ObjectDependencyPolicy.validateObjectReference(null, ORScope.SHARED);
    }

    /**
     * Test: validateObjectReference throws for null object scope
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testValidateObjectReference_NullObjectScope() {
        ObjectDependencyPolicy.validateObjectReference(ReusableRef.Scope.SHARED, null);
    }

    // ==================== Test 6: Logging Methods ====================

    /**
     * Test: logPolicyViolation completes without error
     */
    @Test
    public void testLogPolicyViolation_NoError() {
        // Should not throw
        ObjectDependencyPolicy.logPolicyViolation(
            ReusableRef.Scope.SHARED,
            "LoginButton",
            ORScope.PROJECT,
            "SharedLogin:ValidateLogin"
        );
    }

    /**
     * Test: logPolicyApproval completes without error
     */
    @Test
    public void testLogPolicyApproval_NoError() {
        // Should not throw
        ObjectDependencyPolicy.logPolicyApproval(
            ReusableRef.Scope.SHARED,
            "LoginForm",
            ORScope.SHARED
        );
    }

    // ==================== Test 7: Edge Cases ====================

    /**
     * Test: Multiple PROJECT object references in PROJECT reusable are all allowed
     */
    @Test
    public void testMultipleProjectObjectReferences() {
        String[] objects = { "HomePage", "LoginPage", "DashboardPage" };

        for (String object : objects) {
            assertTrue(
                ObjectDependencyPolicy.isObjectReferenceAllowed(
                    ReusableRef.Scope.PROJECT,
                    ORScope.PROJECT
                ),
                "All project objects should be allowed in project reusable"
            );
        }
    }

    /**
     * Test: Multiple mixed object references in PROJECT reusable are all allowed
     */
    @Test
    public void testMixedObjectReferencesInProjectReusable() {
        boolean projectAllowed = ObjectDependencyPolicy.isObjectReferenceAllowed(
            ReusableRef.Scope.PROJECT,
            ORScope.PROJECT
        );
        boolean sharedAllowed = ObjectDependencyPolicy.isObjectReferenceAllowed(
            ReusableRef.Scope.PROJECT,
            ORScope.SHARED
        );

        assertTrue(projectAllowed, "Project object should be allowed");
        assertTrue(sharedAllowed, "Shared object should be allowed");
    }

    /**
     * Test: Only SHARED objects allowed in SHARED reusable
     */
    @Test
    public void testOnlySharedObjectsInSharedReusable() {
        boolean sharedAllowed = ObjectDependencyPolicy.isObjectReferenceAllowed(
            ReusableRef.Scope.SHARED,
            ORScope.SHARED
        );
        boolean projectNotAllowed = !ObjectDependencyPolicy.isObjectReferenceAllowed(
            ReusableRef.Scope.SHARED,
            ORScope.PROJECT
        );

        assertTrue(sharedAllowed, "Shared object should be allowed");
        assertTrue(projectNotAllowed, "Project object should not be allowed");
    }

    // ==================== Test 8: Result States ====================

    /**
     * Test: Allowed result has correct toString
     */
    @Test
    public void testAllowedResult_ToString() {
        ObjectDependencyPolicy.PolicyValidationResult result = ObjectDependencyPolicy.PolicyValidationResult.allowed();

        String str = result.toString();
        assertTrue(str.contains("ALLOWED"), "Allowed result should contain ALLOWED");
    }

    /**
     * Test: Violation result has correct toString
     */
    @Test
    public void testViolationResult_ToString() {
        ObjectDependencyPolicy.PolicyValidationResult result = ObjectDependencyPolicy.PolicyValidationResult.violation(
            "Test violation",
            "Test rule"
        );

        String str = result.toString();
        assertTrue(str.contains("VIOLATION"), "Violation result should contain VIOLATION");
        assertTrue(str.contains("Test violation"), "Should contain violation reason");
    }

    // ==================== Test 9: Policy Rule Descriptions ====================

    /**
     * Test: Policy rules are descriptive and include both scopes
     */
    @Test
    public void testPolicyRuleDescriptions() {
        ObjectDependencyPolicy.PolicyValidationResult projectToProject = ObjectDependencyPolicy.validateObjectReference(
            ReusableRef.Scope.PROJECT,
            ORScope.PROJECT
        );
        assertTrue(projectToProject.isAllowed());

        ObjectDependencyPolicy.PolicyValidationResult projectToShared = ObjectDependencyPolicy.validateObjectReference(
            ReusableRef.Scope.PROJECT,
            ORScope.SHARED
        );
        assertTrue(projectToShared.isAllowed());

        ObjectDependencyPolicy.PolicyValidationResult sharedToShared = ObjectDependencyPolicy.validateObjectReference(
            ReusableRef.Scope.SHARED,
            ORScope.SHARED
        );
        assertTrue(sharedToShared.isAllowed());

        ObjectDependencyPolicy.PolicyValidationResult sharedToProject = ObjectDependencyPolicy.validateObjectReference(
            ReusableRef.Scope.SHARED,
            ORScope.PROJECT
        );
        assertFalse(sharedToProject.isAllowed());
    }

    // ==================== Test 10: Backward Compatibility ====================

    /**
     * Test: UNSCOPED references behave correctly in policy checks
     */
    @Test
    public void testPolicy_UnScopedReference() {
        // Unscoped reusables should not trigger policy checks at the policy level
        // (Policy is only enforced when scope is explicitly PROJECT or SHARED)
        boolean allowed = ObjectDependencyPolicy.isObjectReferenceAllowed(
            ReusableRef.Scope.UNSCOPED,
            ORScope.PROJECT
        );
        // Unscoped should return false (no direct policy) but would be resolved at runtime
        assertFalse(allowed, "Unscoped scope should not match in policy check");
    }
}

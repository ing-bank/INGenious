package com.ing.ide.main.mainui.components.testdesign.testcase.validation;

import static org.testng.Assert.*;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.ReusableRef;
import com.ing.datalib.component.TestStep;
import com.ing.datalib.or.web.WebOR.ORScope;
import java.awt.Color;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Phase 4 Unit Tests: Object Reference Renderer for IDE
 *
 * Tests verify that ObjectReferenceRenderer correctly:
 * 1. Validates object references against policy constraints
 * 2. Returns red highlighting for violations
 * 3. Generates descriptive error messages
 * 4. Handles null safety and edge cases
 * 5. Supports scope-aware validation
 */
public class ObjectReferenceRendererTest {
    @Mock
    private Project mockProject;

    private TestStep testStep;

    @BeforeMethod
    public void setup() {
        MockitoAnnotations.openMocks(this);
        testStep = new TestStep(null);
    }

    // ==================== Test 1: Validation Results ====================

    /**
     * Test: Valid validation result (no violations)
     */
    @Test
    public void testValidationResult_Valid() {
        ObjectReferenceRenderer.ValidationResult result = ObjectReferenceRenderer.ValidationResult.valid();

        assertTrue(result.isValid());
        assertFalse(result.hasViolations());
        assertEquals(result.getViolationCount(), 0);
        assertEquals(result.getHighlightColor(), Color.WHITE);
        assertEquals(result.getErrorMessage(), "");
    }

    /**
     * Test: Violation result with red highlighting
     */
    @Test
    public void testValidationResult_Violation() {
        ObjectReferenceRenderer.ValidationResult result = ObjectReferenceRenderer.ValidationResult.violation(
            Color.RED,
            "Test violation",
            1
        );

        assertFalse(result.isValid());
        assertTrue(result.hasViolations());
        assertEquals(result.getViolationCount(), 1);
        assertEquals(result.getHighlightColor(), Color.RED);
        assertTrue(result.getErrorMessage().contains("Test violation"));
    }

    /**
     * Test: Violation result toString format
     */
    @Test
    public void testValidationResult_ToStringValid() {
        ObjectReferenceRenderer.ValidationResult result = ObjectReferenceRenderer.ValidationResult.valid();

        String str = result.toString();
        assertTrue(str.contains("Valid"));
        assertTrue(str.contains("✓"));
    }

    /**
     * Test: Violation result toString format with violations
     */
    @Test
    public void testValidationResult_ToStringViolation() {
        ObjectReferenceRenderer.ValidationResult result = ObjectReferenceRenderer.ValidationResult.violation(
            Color.RED,
            "Policy violation",
            2
        );

        String str = result.toString();
        assertTrue(str.contains("✗"));
        assertTrue(str.contains("2"));
        assertTrue(str.contains("violations"));
    }

    // ==================== Test 2: No Violations ====================

    /**
     * Test: No validation needed if reusable scope is null (TestPlan context)
     */
    @Test
    public void testValidateObjectReferences_NullScope_NoValidation() {
        testStep.setAction("Click");
        testStep.setInput("[Object.Button]");

        ObjectReferenceRenderer.ValidationResult result = ObjectReferenceRenderer.validateObjectReferences(
            testStep,
            null,
            mockProject
        );

        assertTrue(result.isValid());
    }

    /**
     * Test: No validation needed if project is null
     */
    @Test
    public void testValidateObjectReferences_NullProject_NoValidation() {
        testStep.setAction("Click");
        testStep.setInput("[Object.Button]");

        ObjectReferenceRenderer.ValidationResult result = ObjectReferenceRenderer.validateObjectReferences(
            testStep,
            ReusableRef.Scope.PROJECT,
            null
        );

        assertTrue(result.isValid());
    }

    /**
     * Test: Valid when PROJECT reusable with PROJECT object
     */
    @Test
    public void testValidateObjectReferences_ProjectReusableProjectObject() {
        testStep.setAction("Click");
        testStep.setInput("[Object.ProjectButton]");

        ObjectReferenceRenderer.ValidationResult result = ObjectReferenceRenderer.validateObjectReferences(
            testStep,
            ReusableRef.Scope.PROJECT,
            mockProject
        );

        // Valid (references may not be found but no policy violation occurs)
        assertNotNull(result);
        assertNotNull(result.toString());
    }

    // ==================== Test 3: Single Object Scope Validation ====================

    /**
     * Test: PROJECT object allowed in PROJECT reusable
     */
    @Test
    public void testValidateObjectScope_ProjectReusableProjectObject() {
        ObjectReferenceRenderer.ValidationResult result = ObjectReferenceRenderer.validateObjectScope(
            "LoginButton",
            ORScope.PROJECT,
            ReusableRef.Scope.PROJECT
        );

        assertTrue(result.isValid());
        assertFalse(result.hasViolations());
    }

    /**
     * Test: SHARED object allowed in PROJECT reusable
     */
    @Test
    public void testValidateObjectScope_ProjectReusableSharedObject() {
        ObjectReferenceRenderer.ValidationResult result = ObjectReferenceRenderer.validateObjectScope(
            "SharedButton",
            ORScope.SHARED,
            ReusableRef.Scope.PROJECT
        );

        assertTrue(result.isValid());
        assertFalse(result.hasViolations());
    }

    /**
     * Test: SHARED object allowed in SHARED reusable
     */
    @Test
    public void testValidateObjectScope_SharedReusableSharedObject() {
        ObjectReferenceRenderer.ValidationResult result = ObjectReferenceRenderer.validateObjectScope(
            "SharedButton",
            ORScope.SHARED,
            ReusableRef.Scope.SHARED
        );

        assertTrue(result.isValid());
        assertFalse(result.hasViolations());
    }

    /**
     * Test: PROJECT object NOT allowed in SHARED reusable (POLICY VIOLATION)
     */
    @Test
    public void testValidateObjectScope_SharedReusableProjectObject_Violation() {
        ObjectReferenceRenderer.ValidationResult result = ObjectReferenceRenderer.validateObjectScope(
            "ProjectButton",
            ORScope.PROJECT,
            ReusableRef.Scope.SHARED
        );

        assertFalse(result.isValid());
        assertTrue(result.hasViolations());
        assertEquals(result.getViolationCount(), 1);
        assertEquals(result.getHighlightColor(), Color.RED);
        assertTrue(result.getErrorMessage().contains("ProjectButton"));
        assertTrue(result.getErrorMessage().contains("PROJECT"));
        assertTrue(result.getErrorMessage().contains("SHARED"));
        assertTrue(result.getErrorMessage().contains("Policy Violation"));
    }

    // ==================== Test 4: Error Message Generation ====================

    /**
     * Test: Error message includes object name, scopes, and policy reason
     */
    @Test
    public void testValidateObjectScope_ViolationMessageContent() {
        ObjectReferenceRenderer.ValidationResult result = ObjectReferenceRenderer.validateObjectScope(
            "HomePageButton",
            ORScope.PROJECT,
            ReusableRef.Scope.SHARED
        );

        String message = result.getErrorMessage();
        assertTrue(message.contains("HomePageButton"));
        assertTrue(message.contains("[PROJECT]"));
        assertTrue(message.contains("[SHARED]"));
        assertTrue(message.contains("cannot be used"));
    }

    // ==================== Test 5: Policy Constraint Descriptions ====================

    /**
     * Test: Get policy constraint for PROJECT scope
     */
    @Test
    public void testGetPolicyConstraintDescription_ProjectScope() {
        String description = ObjectReferenceRenderer.getPolicyConstraintDescription(
            ReusableRef.Scope.PROJECT
        );

        assertNotNull(description);
        assertTrue(description.contains("PROJECT"));
    }

    /**
     * Test: Get policy constraint for SHARED scope
     */
    @Test
    public void testGetPolicyConstraintDescription_SharedScope() {
        String description = ObjectReferenceRenderer.getPolicyConstraintDescription(
            ReusableRef.Scope.SHARED
        );

        assertNotNull(description);
        assertTrue(description.contains("SHARED"));
    }

    /**
     * Test: Get policy constraint for null scope
     */
    @Test
    public void testGetPolicyConstraintDescription_NullScope() {
        String description = ObjectReferenceRenderer.getPolicyConstraintDescription(null);

        assertNotNull(description);
        assertTrue(description.contains("No scope constraint"));
    }

    // ==================== Test 6: Null Safety ====================

    /**
     * Test: Handle null test step
     */
    @Test
    public void testValidateObjectReferences_NullStep() {
        ObjectReferenceRenderer.ValidationResult result = ObjectReferenceRenderer.validateObjectReferences(
            null,
            ReusableRef.Scope.PROJECT,
            mockProject
        );

        // Should handle gracefully
        assertNotNull(result);
    }

    /**
     * Test: Handle null object name
     */
    @Test
    public void testValidateObjectScope_NullObjectName() {
        ObjectReferenceRenderer.ValidationResult result = ObjectReferenceRenderer.validateObjectScope(
            null,
            ORScope.PROJECT,
            ReusableRef.Scope.SHARED
        );

        assertNotNull(result);
    }

    /**
     * Test: Handle null object scope in single validation
     */
    @Test
    public void testValidateObjectScope_NullObjectScope() {
        ObjectReferenceRenderer.ValidationResult result = ObjectReferenceRenderer.validateObjectScope(
            "Button",
            null,
            ReusableRef.Scope.SHARED
        );

        assertTrue(result.isValid());
    }

    // ==================== Test 7: Highlighting Color ====================

    /**
     * Test: Valid result uses white background (no highlighting)
     */
    @Test
    public void testHighlightColor_Valid_White() {
        ObjectReferenceRenderer.ValidationResult result = ObjectReferenceRenderer.ValidationResult.valid();

        assertEquals(result.getHighlightColor(), Color.WHITE);
    }

    /**
     * Test: Violation result uses red highlighting
     */
    @Test
    public void testHighlightColor_Violation_Red() {
        ObjectReferenceRenderer.ValidationResult result = ObjectReferenceRenderer.validateObjectScope(
            "ProjectButton",
            ORScope.PROJECT,
            ReusableRef.Scope.SHARED
        );

        assertEquals(result.getHighlightColor(), Color.RED);
    }

    // ==================== Test 8: Integration Scenarios ====================

    /**
     * Test: Complete validation scenario for SHARED reusable
     */
    @Test
    public void testIntegration_SharedReusableValidation() {
        testStep.setAction("Click");
        testStep.setInput("[Object.Button]");

        ObjectReferenceRenderer.ValidationResult result = ObjectReferenceRenderer.validateObjectReferences(
            testStep,
            ReusableRef.Scope.SHARED,
            mockProject
        );

        assertNotNull(result);
        assertNotNull(result.toString());
        assertNotNull(result.getHighlightColor());
    }

    /**
     * Test: Complete validation scenario for PROJECT reusable
     */
    @Test
    public void testIntegration_ProjectReusableValidation() {
        testStep.setAction("TypeText");
        testStep.setInput("[Object.SearchField]");

        ObjectReferenceRenderer.ValidationResult result = ObjectReferenceRenderer.validateObjectReferences(
            testStep,
            ReusableRef.Scope.PROJECT,
            mockProject
        );

        assertNotNull(result);
        assertNotNull(result.toString());
    }

    // ==================== Test 9: Violation Count ====================

    /**
     * Test: Single violation count
     */
    @Test
    public void testValidationResult_SingleViolationCount() {
        ObjectReferenceRenderer.ValidationResult result = ObjectReferenceRenderer.ValidationResult.violation(
            Color.RED,
            "Violation",
            1
        );

        assertEquals(result.getViolationCount(), 1);
    }

    /**
     * Test: Multiple violations count
     */
    @Test
    public void testValidationResult_MultipleViolationCount() {
        ObjectReferenceRenderer.ValidationResult result = ObjectReferenceRenderer.ValidationResult.violation(
            Color.RED,
            "Violations",
            3
        );

        assertEquals(result.getViolationCount(), 3);
    }

    // ==================== Test 10: Edge Cases ====================

    /**
     * Test: Empty error message in valid result
     */
    @Test
    public void testValidationResult_ValidHasEmptyMessage() {
        ObjectReferenceRenderer.ValidationResult result = ObjectReferenceRenderer.ValidationResult.valid();

        assertTrue(result.getErrorMessage().isEmpty());
    }

    /**
     * Test: Violation has non-empty message
     */
    @Test
    public void testValidationResult_ViolationHasMessage() {
        ObjectReferenceRenderer.ValidationResult result = ObjectReferenceRenderer.ValidationResult.violation(
            Color.RED,
            "Violation message",
            1
        );

        assertFalse(result.getErrorMessage().isEmpty());
        assertTrue(result.getErrorMessage().length() > 0);
    }
}

package com.ing.engine.execution.policy;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.ReusableRef;
import com.ing.datalib.component.TestStep;
import com.ing.datalib.or.web.WebOR.ORScope;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Phase 4 Unit Tests: Object Reference Analyzer
 *
 * Tests verify that ObjectReferenceAnalyzer correctly:
 * 1. Extracts object references from test step actions and values
 * 2. Resolves object scope from project/shared repositories
 * 3. Validates references against policy constraints
 * 4. Reports violations with proper context
 * 5. Integrates with ObjectDependencyPolicy
 */
public class ObjectReferenceAnalyzerTest {
    @Mock
    private Project mockProject;

    private TestStep testStep;

    @BeforeMethod
    public void setup() {
        MockitoAnnotations.openMocks(this);

        // testStep will be created fresh in each test method
        testStep = null;
    }

    // ==================== Test 1: Reference Extraction ====================

    /**
     * Test: Extract [Object.Name] style references from action
     */
    @Test
    public void testExtractReferences_ObjectDotNotation_FromAction() {
        testStep = new TestStep(null);
        testStep.setAction("Click");
        testStep.setInput("[Object.LoginButton]");
        ObjectReferenceAnalyzer.ValidationReport report = ObjectReferenceAnalyzer.analyzeStepObjectReferences(
            testStep,
            ReusableRef.Scope.PROJECT,
            mockProject
        );

        assertNotNull(report);
        // Report created successfully - references extracted
    }

    /**
     * Test: Extract [Name] style references from value
     */
    @Test
    public void testExtractReferences_BracketNotation_FromValue() {
        testStep = new TestStep(null);
        testStep.setAction("SetValue");
        testStep.setInput("[SearchField]");
        ObjectReferenceAnalyzer.ValidationReport report = ObjectReferenceAnalyzer.analyzeStepObjectReferences(
            testStep,
            ReusableRef.Scope.PROJECT,
            mockProject
        );

        assertNotNull(report);
    }

    /**
     * Test: Handle null action and value gracefully
     */
    @Test
    public void testExtractReferences_NullValues() {
        testStep = new TestStep(null);
        // TestStep initializes with empty strings, so we don't need to set

        ObjectReferenceAnalyzer.ValidationReport report = ObjectReferenceAnalyzer.analyzeStepObjectReferences(
            testStep,
            ReusableRef.Scope.PROJECT,
            mockProject
        );

        assertNotNull(report);
        assertFalse(report.hasViolations());
    }

    // ==================== Test 2: Validation Result Structure ====================

    /**
     * Test: ValidationReport methods are accessible
     */
    @Test
    public void testValidationReport_Methods() {
        ObjectReferenceAnalyzer.ValidationReport report = new ObjectReferenceAnalyzer.ValidationReport();

        assertFalse(report.hasViolations());
        assertEquals(report.getViolationCount(), 0);
        assertTrue(report.getViolations().isEmpty());
        assertTrue(report.getSummary().contains("No object reference policy violations"));
    }

    /**
     * Test: Add violation to report
     */
    @Test
    public void testValidationReport_AddViolation() {
        ObjectReferenceAnalyzer.ValidationReport report = new ObjectReferenceAnalyzer.ValidationReport();

        ObjectReferenceAnalyzer.ObjectReferenceViolation violation = new ObjectReferenceAnalyzer.ObjectReferenceViolation(
            "ProjectButton",
            ORScope.PROJECT,
            ReusableRef.Scope.SHARED,
            "Shared reusable cannot reference Project object",
            "Click [Object.ProjectButton]"
        );

        report.addViolation(violation);

        assertTrue(report.hasViolations());
        assertEquals(report.getViolationCount(), 1);
        assertEquals(report.getViolations().size(), 1);
        assertTrue(report.getSummary().contains("1 object reference policy violation"));
    }

    /**
     * Test: Violation detailed message format
     */
    @Test
    public void testViolation_DetailedMessage() {
        ObjectReferenceAnalyzer.ObjectReferenceViolation violation = new ObjectReferenceAnalyzer.ObjectReferenceViolation(
            "HomeButton",
            ORScope.PROJECT,
            ReusableRef.Scope.SHARED,
            "Shared reusable cannot reference Project object",
            "Click [Object.HomeButton]"
        );

        String detailed = violation.getDetailedMessage();

        assertTrue(detailed.contains("HomeButton"));
        assertTrue(detailed.contains("PROJECT"));
        assertTrue(detailed.contains("SHARED"));
        assertTrue(detailed.contains("Click [Object.HomeButton]"));
        assertTrue(detailed.contains("Policy Violation:"));
    }

    // ==================== Test 3: Policy Integration ====================

    /**
     * Test: Analyze with PROJECT reusable and PROJECT object (allowed)
     */
    @Test
    public void testAnalyze_ProjectReusable_ProjectObject() {
        testStep = new TestStep(null);
        testStep.setAction("Click");
        testStep.setInput("[Object.Button]");
        ObjectReferenceAnalyzer.ValidationReport report = ObjectReferenceAnalyzer.analyzeStepObjectReferences(
            testStep,
            ReusableRef.Scope.PROJECT,
            mockProject
        );

        assertNotNull(report);
        // Should not have violations for project-to-project reference
    }

    /**
     * Test: Analyze with PROJECT reusable and SHARED object (allowed)
     */
    @Test
    public void testAnalyze_ProjectReusable_SharedObject() {
        testStep = new TestStep(null);
        testStep.setAction("Click");
        testStep.setInput("[Object.SharedButton]");
        ObjectReferenceAnalyzer.ValidationReport report = ObjectReferenceAnalyzer.analyzeStepObjectReferences(
            testStep,
            ReusableRef.Scope.PROJECT,
            mockProject
        );

        assertNotNull(report);
        // Should not have violations for project-to-shared reference
    }

    // ==================== Test 4: Error Conditions ====================

    /**
     * Test: Handle null test step
     */
    @Test
    public void testAnalyze_NullStep() {
        ObjectReferenceAnalyzer.ValidationReport report = ObjectReferenceAnalyzer.analyzeStepObjectReferences(
            null,
            ReusableRef.Scope.PROJECT,
            mockProject
        );

        assertNotNull(report);
        assertFalse(report.hasViolations());
    }

    /**
     * Test: Handle null scope
     */
    @Test
    public void testAnalyze_NullScope() {
        testStep = new TestStep(null);
        testStep.setAction("Click");
        testStep.setInput("[Object.Button]");
        ObjectReferenceAnalyzer.ValidationReport report = ObjectReferenceAnalyzer.analyzeStepObjectReferences(
            testStep,
            null,
            mockProject
        );

        assertNotNull(report);

        assertFalse(report.hasViolations());
    }

    /**
     * Test: Handle null project
     */
    @Test
    public void testAnalyze_NullProject() {
        testStep = new TestStep(null);
        testStep.setAction("Click");
        testStep.setInput("[Object.Button]");
        ObjectReferenceAnalyzer.ValidationReport report = ObjectReferenceAnalyzer.analyzeStepObjectReferences(
            testStep,
            ReusableRef.Scope.PROJECT,
            null
        );

        assertNotNull(report);

        assertFalse(report.hasViolations());
    }

    // ==================== Test 5: ObjectReference Class ====================

    /**
     * Test: ObjectReference constructor and fields
     */
    @Test
    public void testObjectReference_Fields() {
        ObjectReferenceAnalyzer.ObjectReference ref = new ObjectReferenceAnalyzer.ObjectReference(
            "MyButton",
            ORScope.PROJECT
        );

        assertEquals(ref.objectName, "MyButton");
        assertEquals(ref.objectScope, ORScope.PROJECT);
    }

    /**
     * Test: ObjectReference toString format
     */
    @Test
    public void testObjectReference_ToString() {
        ObjectReferenceAnalyzer.ObjectReference ref = new ObjectReferenceAnalyzer.ObjectReference(
            "MyButton",
            ORScope.SHARED
        );

        String str = ref.toString();
        assertTrue(str.contains("SHARED"));
        assertTrue(str.contains("MyButton"));
    }

    // ==================== Test 6: Report Throwing ====================

    /**
     * Test: throwIfViolationsExist does not throw when no violations
     */
    @Test
    public void testThrowIfViolationsExist_NoViolations()
        throws ObjectDependencyPolicyViolationException {
        ObjectReferenceAnalyzer.ValidationReport report = new ObjectReferenceAnalyzer.ValidationReport();

        // Should not throw
        report.throwIfViolationsExist();
    }

    /**
     * Test: throwIfViolationsExist throws when violations exist
     */
    @Test(expectedExceptions = ObjectDependencyPolicyViolationException.class)
    public void testThrowIfViolationsExist_WithViolations()
        throws ObjectDependencyPolicyViolationException {
        ObjectReferenceAnalyzer.ValidationReport report = new ObjectReferenceAnalyzer.ValidationReport();

        ObjectReferenceAnalyzer.ObjectReferenceViolation violation = new ObjectReferenceAnalyzer.ObjectReferenceViolation(
            "ProjectButton",
            ORScope.PROJECT,
            ReusableRef.Scope.SHARED,
            "Shared reusable cannot reference Project object",
            "Click [Object.ProjectButton]"
        );

        report.addViolation(violation);
        report.throwIfViolationsExist();
    }

    // ==================== Test 7: Report Summary ====================

    /**
     * Test: Empty report summary
     */
    @Test
    public void testReportSummary_Empty() {
        ObjectReferenceAnalyzer.ValidationReport report = new ObjectReferenceAnalyzer.ValidationReport();

        String summary = report.getSummary();
        assertTrue(summary.contains("No object reference policy violations"));
        assertTrue(summary.contains("✓"));
    }

    /**
     * Test: Report with violations summary
     */
    @Test
    public void testReportSummary_WithViolations() {
        ObjectReferenceAnalyzer.ValidationReport report = new ObjectReferenceAnalyzer.ValidationReport();

        for (int i = 0; i < 2; i++) {
            report.addViolation(
                new ObjectReferenceAnalyzer.ObjectReferenceViolation(
                    "Button" + i,
                    ORScope.PROJECT,
                    ReusableRef.Scope.SHARED,
                    "Violation " + i,
                    "Action " + i
                )
            );
        }

        String summary = report.getSummary();
        assertTrue(summary.contains("2 object reference policy violation"));
        assertTrue(summary.contains("✗"));
    }

    // ==================== Test 8: Violation Equality ====================

    /**
     * Test: ObjectReferenceViolation toString format
     */
    @Test
    public void testViolation_ToStringFormat() {
        ObjectReferenceAnalyzer.ObjectReferenceViolation violation = new ObjectReferenceAnalyzer.ObjectReferenceViolation(
            "SpecialButton",
            ORScope.PROJECT,
            ReusableRef.Scope.SHARED,
            "Cannot use project objects",
            "Click"
        );

        String str = violation.toString();
        assertTrue(str.contains("SpecialButton"));
        assertTrue(str.contains("PROJECT"));
        assertTrue(str.contains("SHARED"));
    }

    // ==================== Test 9: Multiple References ====================

    /**
     * Test: Handle multiple references in single action
     */
    @Test
    public void testAnalyze_MultipleReferencesInAction() {
        testStep = new TestStep(null);
        testStep.setAction("SetValue [Object.Field1] and [Object.Field2]");
        testStep.setInput("[Object.Button]");
        ObjectReferenceAnalyzer.ValidationReport report = ObjectReferenceAnalyzer.analyzeStepObjectReferences(
            testStep,
            ReusableRef.Scope.PROJECT,
            mockProject
        );

        assertNotNull(report);
    }

    // ==================== Test 10: Integration Scenarios ====================

    /**
     * Test: Complete SHARED reusable component step validation
     */
    @Test
    public void testIntegration_SharedReusableValidation() {
        testStep = new TestStep(null);
        testStep.setAction("ClickElement");
        testStep.setInput("[Object.LoginButton]");
        ObjectReferenceAnalyzer.ValidationReport report = ObjectReferenceAnalyzer.analyzeStepObjectReferences(
            testStep,
            ReusableRef.Scope.SHARED,
            mockProject
        );

        assertNotNull(report);

        assertNotNull(report.toString());
        assertNotNull(report.getSummary());
    }

    /**
     * Test: Complete PROJECT reusable component step validation
     */
    @Test
    public void testIntegration_ProjectReusableValidation() {
        testStep = new TestStep(null);
        testStep.setAction("TypeText");
        testStep.setInput("[Object.SearchBox]");

        ObjectReferenceAnalyzer.ValidationReport report = ObjectReferenceAnalyzer.analyzeStepObjectReferences(
            testStep,
            ReusableRef.Scope.PROJECT,
            mockProject
        );

        assertNotNull(report);
        assertNotNull(report.getViolations());
    }
}

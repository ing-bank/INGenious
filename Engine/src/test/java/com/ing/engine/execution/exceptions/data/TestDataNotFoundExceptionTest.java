package com.ing.engine.execution.exceptions.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import com.ing.engine.execution.exception.data.DataNotFoundException.Cause;
import com.ing.engine.execution.exception.data.TestDataNotFoundException;
import com.ing.engine.execution.run.ProjectRunner;
import com.ing.engine.execution.run.TestCaseRunner;
import org.testng.annotations.Test;

public class TestDataNotFoundExceptionTest {

    /**
     * Test of getTemplate method, of class TestDataNotFoundException.
     */
    @Test
    public void testGetTemplate() {
        System.out.println("getTemplate- TestData");
        String expResult =
            "{0} \n[Env : {1} | Scope : {2} | Sheet : {3} | Field : {4} | TestCase : {5}/{6} | Reusable : {7}/{8} ]";
        String result = TestDataNotFoundException.getTemplate(true);
        assertEquals(expResult, result);
        expResult =
            "{0} \n[Env : {1} | Scope : {2} | Sheet : {3} | Field : {4} | TestCase : {5}/{6} ]";
        result = TestDataNotFoundException.getTemplate(false);
        assertEquals(expResult, result);
    }

    private TestCaseRunner mockContext() {
        TestCaseRunner context = mock(TestCaseRunner.class);
        TestCaseRunner root = mock(TestCaseRunner.class);
        ProjectRunner executor = mock(ProjectRunner.class);
        when(context.executor()).thenReturn(executor);
        when(executor.runEnv()).thenReturn("Default");
        when(context.getRoot()).thenReturn(root);
        when(root.scenario()).thenReturn("MortgageCalculation-Browser");
        when(root.testcase()).thenReturn("High Income");
        when(context.scenario()).thenReturn("MortgageCalculation-Browser");
        when(context.testcase()).thenReturn("High Income");
        when(context.isReusable()).thenReturn(false);
        return context;
    }

    @Test
    public void testToStringShowsProjectScopeForUntaggedSheet() {
        TestCaseRunner context = mockContext();
        TestDataNotFoundException ex = new TestDataNotFoundException(
            context,
            "Basic",
            "URL",
            Cause.Iteration,
            "1"
        );
        assertThat(ex.toString()).contains("Scope : Project");
    }

    @Test
    public void testToStringShowsSharedScopeForSharedTaggedSheet() {
        TestCaseRunner context = mockContext();
        TestDataNotFoundException ex = new TestDataNotFoundException(
            context,
            "[Shared] TestData1",
            "Data1",
            Cause.Iteration,
            "1"
        );
        assertThat(ex.toString()).contains("Scope : Shared");
    }
}

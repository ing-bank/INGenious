package com.ing.engine.execution.exceptions.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;

import com.ing.engine.execution.exception.data.GlobalDataNotFoundException;
import com.ing.engine.execution.run.TestCaseRunner;
import org.testng.annotations.Test;

public class GlobalDataNotFoundExceptionTest {

    /**
     * Test of getTemplate method, of class GlobalDataNotFoundException.
     */
    @Test
    public void testGetTemplate() {
        System.out.println("getTemplate- GlobalData");
        String expResult =
            "{0} \n[Env : {1} | Field : {2} | GID : {3} | TestCase : {4}/{5} | Reusable : {6}/{7} ]";
        String result = GlobalDataNotFoundException.getTemplate(true);
        assertEquals(expResult, result);
        expResult = "{0} \n[Env : {1} | Field : {2} | GID : {3} | TestCase : {4}/{5} ]";
        result = GlobalDataNotFoundException.getTemplate(false);
        assertEquals(expResult, result);
    }

    /**
     * Regression test: Task.runIteration() unconditionally calls ex.cause.isEndData() on any
     * caught DataNotFoundException. A null cause field NPEs there and silently aborts the whole
     * test case run (steps after the failing one never execute). cause must never be null and
     * must never report itself as "end of data" (that would incorrectly suppress the failure
     * as if it were normal Param Loop termination).
     */
    @Test
    public void testCauseIsNeverNullAndNotMistakenForEndOfData() {
        TestCaseRunner context = mock(TestCaseRunner.class);
        GlobalDataNotFoundException ex = new GlobalDataNotFoundException(context, "#url", "URL");

        assertThat(ex.cause).isNotNull();
        assertThat(ex.cause.isEndData()).isFalse();
    }
}

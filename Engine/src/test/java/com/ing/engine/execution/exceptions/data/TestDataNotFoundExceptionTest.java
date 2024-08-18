
package com.ing.engine.execution.exceptions.data;

import com.ing.engine.execution.exception.data.TestDataNotFoundException;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;

public class TestDataNotFoundExceptionTest {

    /**
     * Test of getTemplate method, of class TestDataNotFoundException.
     */
    @Test
    public void testGetTemplate() {
        System.out.println("getTemplate- TestData");
        String expResult = "{0} \n[Env : {1} | Sheet : {2} | Field : {3} | TestCase : {4}/{5} | Reusabe : {6}/{7} ]";
        String result = TestDataNotFoundException.getTemplate(true);
        assertEquals(expResult, result);
        expResult = "{0} \n[Env : {1} | Sheet : {2} | Field : {3} | TestCase : {4}/{5} ]";
        result = TestDataNotFoundException.getTemplate(false);
        assertEquals(expResult, result);
    }

}

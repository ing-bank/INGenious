package com.ing.engine.execution.exceptions.data;

import static org.testng.Assert.assertEquals;

import com.ing.engine.execution.exception.data.GlobalDataNotFoundException;
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
}

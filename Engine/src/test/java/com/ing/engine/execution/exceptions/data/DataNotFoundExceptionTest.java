package com.ing.engine.execution.exceptions.data;

import static org.testng.Assert.assertEquals;

import com.ing.engine.execution.exception.data.DataNotFoundException;
import org.testng.annotations.Test;

public class DataNotFoundExceptionTest {

    /**
     * Test of getTemplate method, of class DataNotFoundException.
     */
    @Test
    public void testGetTemplate() {
        System.out.println("getTemplate");
        Boolean isReusable = true;
        String expResult =
            "{0} \n" + "[Env : {1} | Field : {2} | TestCase : {4}/{5} | Reusable : {6}/{7} ]";
        String result = DataNotFoundException.getTemplate(isReusable);
        assertEquals(expResult, result);
        isReusable = false;
        expResult = "{0} \n" + "[Env : {1} | Field : {2} | TestCase : {4}/{5} ]";
        result = DataNotFoundException.getTemplate(isReusable);
        assertEquals(expResult, result);
    }

    /**
     * Test of getFormatted method, of class DataNotFoundException.
     */
    @Test
    public void testGetFormatted() {
        System.out.println("getFormatted");
        String template = DataNotFoundException.getTemplate(true);
        Object[] args = { "msg", "env", "f", "if", "sc", "tc", "rsc", "rtc" };
        String expResult =
            "msg \n" + "[Env : env | Field : f | TestCase : sc/tc | Reusable : rsc/rtc ]";
        String result = DataNotFoundException.getFormatted(template, args);
        assertEquals(expResult, result);
        template = DataNotFoundException.getTemplate(false);
        expResult = "msg \n" + "[Env : env | Field : f | TestCase : sc/tc ]";
        result = DataNotFoundException.getFormatted(template, args);
        assertEquals(expResult, result);
    }
}

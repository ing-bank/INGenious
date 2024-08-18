
package com.ing.engine.util.data;

import com.ing.engine.util.data.KeyMap;
import java.util.HashMap;
import java.util.Map;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class KeyMapTest {

    private final Map vMap = new HashMap();

    public KeyMapTest() {
    }


    @BeforeMethod
    public void setUp() {
        System.setProperty("var.1", "x");
        System.setProperty("var.2", "y");
        System.setProperty("var.3", "z");
        System.setProperty("val.1", "1");
        System.setProperty("val.2", "b");
        System.setProperty("val.3", "c");

        vMap.put("scenario", "myscn");
        vMap.put("testcase", "tc");
        vMap.put("project", "pro");
        vMap.put("testset", "ts");
        vMap.put("release", "rel");
        vMap.put("rel", "L2");
    }

    @AfterMethod
    public void tearDown() {
        vMap.clear();
    }


    /**
     * Test of resolveContextVars method, of class KeyMap.
     */
    @Test
    public void testResolveContextVars() {
        System.out.println("resolveContextVars");
        String in = "this is a {scenario}/{testset} -> {testset}/{release} for {project}.{release}.{testcase} ";
        String expResult = "this is a myscn/ts -> ts/rel for pro.rel.tc ";
        String result = KeyMap.resolveContextVars(in, vMap);
        assertEquals(expResult, result);
    }

    /**
     * Test of resolveEnvVars method, of class KeyMap.
     */
    @Test
    public void testResolveEnvVars() {
        System.out.println("resolveEnvVars");
        String in = "${val.1}-${var.1}-${val.2}-${val.1}-${val.3}-${val.4}-${var.4}-${var.1}-${var.2}-";
        String expResult = "1-x-b-1-c-val.4-var.4-x-y-";
        String result = KeyMap.resolveEnvVars(in);
        assertEquals(expResult, result);

    }

    /**
     * Test of replaceKeys method, of class KeyMap.
     */
    @Test
    public void testReplaceKeysUserVariables() {

        System.out.println("replaceKeys");
        String in = "this is a %scenario%/%testset% -> %testset%/%release% for %project%.%testcase%/%%release%%. ";

        boolean preserveKeys = true;
        String result;
        String expResult = "this is a myscn/ts -> ts/rel for pro.tc/%rel%. ";
        result = KeyMap.replaceKeys(in, KeyMap.USER_VARS, preserveKeys, 1, vMap);
        assertEquals(expResult, result);
        expResult = "this is a myscn/ts -> ts/rel for pro.tc/L2. ";
        result = KeyMap.replaceKeys(in, KeyMap.USER_VARS, preserveKeys, 2, vMap);
        assertEquals(expResult, result);

    }

    /**
     * Test of replaceKeys method, of class KeyMap.
     */
    @Test
    public void testReplaceKeys_String_Pattern() {
        System.out.println("replaceKeys");
        String in = "${val.1}-${var.1}-${val.2}-${val.1}-${val.3}-${val.4}-${var.4}-${var.1}-${var.2}-";
        String expResult = "1-x-b-1-c-val.4-var.4-x-y-";
        String result = KeyMap.replaceKeys(in, KeyMap.ENV_VARS);
        assertEquals(expResult, result);

    }

}

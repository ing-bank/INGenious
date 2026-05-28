package com.ing.engine.commands.webservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.ing.engine.commands.browser.Command;
import com.ing.engine.core.CommandControl;
import com.ing.engine.execution.data.UserDataAccess;
import com.ing.engine.reporting.TestCaseReport;
import com.ing.ingenious.api.status.Status;

import java.lang.reflect.Field;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests for Webservice JSONPath-based assertion and store methods.
 * Uses reflection to set up the Command fields (Data, Condition, Input, etc.)
 * since the constructor requires a fully wired CommandControl.
 */
public class WebserviceJsonPathTest {

    @Mock private CommandControl cc;
    @Mock private TestCaseReport report;
    @Mock private UserDataAccess userData;

    private Webservice ws;
    private AutoCloseable mocks;
    private String key;

    @BeforeMethod
    public void setUp() throws Exception {
        mocks = MockitoAnnotations.openMocks(this);

        // Set up minimal CommandControl mock to satisfy constructor
        when(userData.getScenario()).thenReturn("TestScenario");
        when(userData.getTestCase()).thenReturn("TestCase");

        // Create Webservice via reflection bypass
        ws = createWebserviceInstance();
        key = "TestScenarioTestCase";

        // Clear static maps
        Command.responsebodies.clear();
        Command.responsecodes.clear();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        Command.responsebodies.clear();
        Command.responsecodes.clear();
        mocks.close();
    }

    private Webservice createWebserviceInstance() throws Exception {
        Webservice instance = mock(Webservice.class, CALLS_REAL_METHODS);

        // Set fields via reflection
        setField(instance, Command.class, "Report", report);
        setField(instance, Command.class, "userData", userData);
        setField(instance, Command.class, "key", key);
        setField(instance, Command.class, "Commander", cc);

        return instance;
    }

    private void setField(Object obj, Class<?> clazz, String name, Object value) throws Exception {
        Field f = clazz.getDeclaredField(name);
        f.setAccessible(true);
        f.set(obj, value);
    }

    private void setCommandFields(String data, String condition, String input, String action) throws Exception {
        setField(ws, Command.class, "Data", data);
        setField(ws, Command.class, "Condition", condition);
        setField(ws, Command.class, "Input", input);
        setField(ws, Command.class, "Action", action);
        setField(ws, Command.class, "key", key);
    }

    // ---- assertResponseCode ----

    @Test
    public void testAssertResponseCodePass() throws Exception {
        Command.responsecodes.put(key, "200");
        setCommandFields("200", null, null, "assertResponseCode");

        ws.assertResponseCode();
        verify(report).updateTestLog(eq("assertResponseCode"), contains("200"), eq(Status.PASSNS));
    }

    @Test
    public void testAssertResponseCodeFail() throws Exception {
        Command.responsecodes.put(key, "404");
        setCommandFields("200", null, null, "assertResponseCode");

        ws.assertResponseCode();
        verify(report).updateTestLog(eq("assertResponseCode"), contains("404"), eq(Status.FAILNS));
    }

    // ---- assertResponsebodycontains ----

    @Test
    public void testAssertResponseBodyContainsPass() throws Exception {
        Command.responsebodies.put(key, "{\"name\":\"John\",\"age\":30}");
        setCommandFields("John", null, null, "assertResponsebodycontains");

        ws.assertResponsebodycontains();
        verify(report).updateTestLog(eq("assertResponsebodycontains"), contains("John"), eq(Status.PASSNS));
    }

    @Test
    public void testAssertResponseBodyContainsFail() throws Exception {
        Command.responsebodies.put(key, "{\"name\":\"Jane\"}");
        setCommandFields("John", null, null, "assertResponsebodycontains");

        ws.assertResponsebodycontains();
        verify(report).updateTestLog(eq("assertResponsebodycontains"), contains("John"), eq(Status.FAILNS));
    }

    // ---- assertJSONelementEquals ----

    @Test
    public void testAssertJSONelementEqualsPass() throws Exception {
        Command.responsebodies.put(key, "{\"name\":\"John\",\"age\":30}");
        setCommandFields("John", "$.name", null, "assertJSONelementEquals");

        ws.assertJSONelementEquals();
        verify(report).updateTestLog(eq("assertJSONelementEquals"), contains("as expected"), eq(Status.PASSNS));
    }

    @Test
    public void testAssertJSONelementEqualsFail() throws Exception {
        Command.responsebodies.put(key, "{\"name\":\"Jane\",\"age\":30}");
        setCommandFields("John", "$.name", null, "assertJSONelementEquals");

        ws.assertJSONelementEquals();
        verify(report).updateTestLog(eq("assertJSONelementEquals"), contains("Jane"), eq(Status.FAILNS));
    }

    // ---- assertJSONelementContains ----

    @Test
    public void testAssertJSONelementContainsPass() throws Exception {
        Command.responsebodies.put(key, "{\"greeting\":\"Hello World\"}");
        setCommandFields("World", "$.greeting", null, "assertJSONelementContains");

        ws.assertJSONelementContains();
        verify(report).updateTestLog(eq("assertJSONelementContains"), contains("as expected"), eq(Status.PASSNS));
    }

    @Test
    public void testAssertJSONelementContainsFail() throws Exception {
        Command.responsebodies.put(key, "{\"greeting\":\"Good Morning\"}");
        setCommandFields("Evening", "$.greeting", null, "assertJSONelementContains");

        ws.assertJSONelementContains();
        verify(report).updateTestLog(eq("assertJSONelementContains"), contains("does not contain"), eq(Status.FAILNS));
    }

    // ---- storeJSONelement ----

    @Test
    public void testStoreJSONelementValid() throws Exception {
        Command.responsebodies.put(key, "{\"token\":\"abc123\"}");
        setCommandFields("$.token", "%myVar%", null, "storeJSONelement");

        ws.storeJSONelement();
        verify(report).updateTestLog(eq("storeJSONelement"), contains("stored"), eq(Status.DONE));
    }

    @Test
    public void testStoreJSONelementInvalidVarFormat() throws Exception {
        Command.responsebodies.put(key, "{\"token\":\"abc123\"}");
        setCommandFields("$.token", "myVar", null, "storeJSONelement");

        ws.storeJSONelement();
        verify(report).updateTestLog(eq("storeJSONelement"), contains("Variable format"), eq(Status.DEBUG));
    }
}

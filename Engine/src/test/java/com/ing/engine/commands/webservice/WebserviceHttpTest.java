package com.ing.engine.commands.webservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.ing.engine.commands.browser.Command;
import com.ing.engine.core.CommandControl;
import com.ing.engine.execution.data.UserDataAccess;
import com.ing.engine.reporting.TestCaseReport;
import com.ing.ingenious.api.status.Status;
import java.lang.reflect.Field;
import java.util.ArrayList;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests for Webservice store/response methods and URL param management.
 * Uses reflection to set Command fields.
 */
public class WebserviceHttpTest {
    @Mock
    private TestCaseReport report;

    @Mock
    private UserDataAccess userData;

    @Mock
    private CommandControl commander;

    private Webservice ws;
    private AutoCloseable mocks;
    private final String key = "TestScenarioTestCase";

    @BeforeMethod
    public void setUp() throws Exception {
        mocks = MockitoAnnotations.openMocks(this);

        when(userData.getScenario()).thenReturn("TestScenario");
        when(userData.getTestCase()).thenReturn("TestCase");
        when(userData.getSubIteration()).thenReturn("1");

        ws = mock(Webservice.class, CALLS_REAL_METHODS);
        setField(ws, Command.class, "Report", report);
        setField(ws, Command.class, "userData", userData);
        setField(ws, Command.class, "key", key);
        setField(ws, Command.class, "Commander", commander);

        Command.responsebodies.clear();
        Command.responsecodes.clear();
        Command.headers.clear();
        Command.urlParams.clear();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        Command.responsebodies.clear();
        Command.responsecodes.clear();
        Command.headers.clear();
        Command.urlParams.clear();
        mocks.close();
    }

    private void setField(Object obj, Class<?> clazz, String name, Object value) throws Exception {
        Field f = clazz.getDeclaredField(name);
        f.setAccessible(true);
        f.set(obj, value);
    }

    private void setCommandFields(String data, String condition, String input, String action)
        throws Exception {
        setField(ws, Command.class, "Data", data);
        setField(ws, Command.class, "Condition", condition);
        setField(ws, Command.class, "Input", input);
        setField(ws, Command.class, "Action", action);
    }

    // ---- storeResponseBodyInDataSheet ----

    @Test
    public void testStoreResponseBodyInDataSheetInvalidFormat() throws Exception {
        Command.responsebodies.put(key, "{\"result\":\"ok\"}");
        setCommandFields(null, null, "noColonHere", "storeResponseBodyInDataSheet");

        ws.storeResponseBodyInDataSheet();
        verify(report)
            .updateTestLog(
                eq("storeResponseBodyInDataSheet"),
                contains("format is invalid"),
                eq(Status.DEBUG)
            );
    }

    @Test
    public void testStoreResponseBodyInDataSheetValidFormat() throws Exception {
        Command.responsebodies.put(key, "{\"result\":\"ok\"}");
        setCommandFields(null, null, "Sheet1:Column1", "storeResponseBodyInDataSheet");

        ws.storeResponseBodyInDataSheet();
        verify(userData).putData("Sheet1", "Column1", "{\"result\":\"ok\"}");
        verify(report)
            .updateTestLog(eq("storeResponseBodyInDataSheet"), contains("stored"), eq(Status.DONE));
    }

    // ---- storeJSONelementInDataSheet ----

    @Test
    public void testStoreJSONelementInDataSheetInvalidFormat() throws Exception {
        Command.responsebodies.put(key, "{\"token\":\"xyz\"}");
        setCommandFields(null, "$.token", "invalidFormat", "storeJSONelementInDataSheet");

        ws.storeJSONelementInDataSheet();
        verify(report)
            .updateTestLog(
                eq("storeJSONelementInDataSheet"),
                contains("format is invalid"),
                eq(Status.DEBUG)
            );
    }

    @Test
    public void testStoreJSONelementInDataSheetValidFormat() throws Exception {
        Command.responsebodies.put(key, "{\"token\":\"xyz\"}");
        setCommandFields(null, "$.token", "Sheet1:Token", "storeJSONelementInDataSheet");

        ws.storeJSONelementInDataSheet();
        verify(userData).putData("Sheet1", "Token", "xyz");
        verify(report)
            .updateTestLog(eq("storeJSONelementInDataSheet"), contains("stored"), eq(Status.DONE));
    }

    // ---- addURLParam ----

    @Test
    public void testAddURLParamCreatesNewList() throws Exception {
        setCommandFields("key1=value1", null, null, "addURLParam");

        ws.addURLParam();
        assertThat(Command.urlParams).containsKey(key);
        assertThat(Command.urlParams.get(key)).containsExactly("key1=value1");
        verify(report).updateTestLog(eq("addURLParam"), contains("key1=value1"), eq(Status.DONE));
    }

    @Test
    public void testAddURLParamAppendsToExisting() throws Exception {
        ArrayList<String> existing = new ArrayList<>();
        existing.add("key1=value1");
        Command.urlParams.put(key, existing);

        setCommandFields("key2=value2", null, null, "addURLParam");

        ws.addURLParam();
        assertThat(Command.urlParams.get(key)).containsExactly("key1=value1", "key2=value2");
    }

    // ---- assertJSONelementCount ----

    @Test
    public void testAssertJSONelementCountPass() throws Exception {
        Command.responsebodies.put(key, "{\"items\":[{\"id\":1},{\"id\":2},{\"id\":3}]}");
        setCommandFields("3", "$.items", null, "assertJSONelementCount");

        ws.assertJSONelementCount();
        verify(report)
            .updateTestLog(
                eq("assertJSONelementCount"),
                contains("as expected"),
                eq(Status.PASSNS)
            );
    }

    @Test
    public void testAssertJSONelementCountFail() throws Exception {
        Command.responsebodies.put(key, "{\"items\":[{\"id\":1},{\"id\":2}]}");
        setCommandFields("5", "$.items", null, "assertJSONelementCount");

        ws.assertJSONelementCount();
        verify(report)
            .updateTestLog(
                eq("assertJSONelementCount"),
                contains("expected to be"),
                eq(Status.FAILNS)
            );
    }

    // ---- storeJsonElementCount ----

    @Test
    public void testStoreJsonElementCountValidVar() throws Exception {
        Command.responsebodies.put(key, "{\"items\":[1,2,3]}");
        setCommandFields("$.items", "%count%", null, "storeJsonElementCount");

        ws.storeJsonElementCount();
        verify(report)
            .updateTestLog(eq("storeJsonElementCount"), contains("stored"), eq(Status.DONE));
    }

    @Test
    public void testStoreJsonElementCountInvalidVarFormat() throws Exception {
        Command.responsebodies.put(key, "{\"items\":[1,2,3]}");
        setCommandFields("$.items", "count", null, "storeJsonElementCount");

        ws.storeJsonElementCount();
        verify(report)
            .updateTestLog(
                eq("storeJsonElementCount"),
                contains("format is invalid"),
                eq(Status.DEBUG)
            );
    }
}

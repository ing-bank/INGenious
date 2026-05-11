package com.ing.engine.commands.webservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.ing.engine.commands.browser.Command;
import com.ing.engine.core.CommandControl;
import com.ing.engine.execution.data.UserDataAccess;
import com.ing.engine.reporting.TestCaseReport;
import com.ing.ingenious.api.status.Status;

import java.lang.reflect.Field;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests for Webservice XPath-based assertion and store methods.
 * Uses reflection to set up the Command fields.
 */
public class WebserviceXPathTest {

    @Mock private TestCaseReport report;
    @Mock private UserDataAccess userData;
    @Mock private CommandControl commander;

    private Webservice ws;
    private AutoCloseable mocks;
    private final String key = "TestScenarioTestCase";

    private static final String XML_RESPONSE =
            "<root><item id=\"1\"><name>Widget</name><price>9.99</price></item>"
            + "<item id=\"2\"><name>Gadget</name><price>19.99</price></item></root>";

    @BeforeMethod
    public void setUp() throws Exception {
        mocks = MockitoAnnotations.openMocks(this);

        when(userData.getScenario()).thenReturn("TestScenario");
        when(userData.getTestCase()).thenReturn("TestCase");

        ws = mock(Webservice.class, CALLS_REAL_METHODS);
        setField(ws, Command.class, "Report", report);
        setField(ws, Command.class, "userData", userData);
        setField(ws, Command.class, "key", key);
        setField(ws, Command.class, "Commander", commander);

        Command.responsebodies.clear();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        Command.responsebodies.clear();
        mocks.close();
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
    }

    // ---- assertXMLelementEquals ----

    @Test
    public void testAssertXMLelementEqualsPass() throws Exception {
        Command.responsebodies.put(key, XML_RESPONSE);
        setCommandFields("Widget", "//item[@id='1']/name/text()", null, "assertXMLelementEquals");

        ws.assertXMLelementEquals();
        verify(report).updateTestLog(eq("assertXMLelementEquals"), contains("as expected"), eq(Status.PASSNS));
    }

    @Test
    public void testAssertXMLelementEqualsFail() throws Exception {
        Command.responsebodies.put(key, XML_RESPONSE);
        setCommandFields("Gizmo", "//item[@id='1']/name/text()", null, "assertXMLelementEquals");

        ws.assertXMLelementEquals();
        verify(report).updateTestLog(eq("assertXMLelementEquals"), contains("not as expected"), eq(Status.FAILNS));
    }

    // ---- assertXMLelementContains ----

    @Test
    public void testAssertXMLelementContainsPass() throws Exception {
        Command.responsebodies.put(key, XML_RESPONSE);
        setCommandFields("Wid", "//item[@id='1']/name/text()", null, "assertXMLelementContains");

        ws.assertXMLelementContains();
        verify(report).updateTestLog(eq("assertXMLelementContains"), contains("as expected"), eq(Status.PASSNS));
    }

    @Test
    public void testAssertXMLelementContainsFail() throws Exception {
        Command.responsebodies.put(key, XML_RESPONSE);
        setCommandFields("XYZ", "//item[@id='1']/name/text()", null, "assertXMLelementContains");

        ws.assertXMLelementContains();
        verify(report).updateTestLog(eq("assertXMLelementContains"), contains("does not contain"), eq(Status.FAILNS));
    }

    // ---- storeXMLelement ----

    @Test
    public void testStoreXMLelementValid() throws Exception {
        Command.responsebodies.put(key, XML_RESPONSE);
        setCommandFields("//item[@id='2']/name/text()", "%xmlVar%", null, "storeXMLelement");

        ws.storeXMLelement();
        verify(report).updateTestLog(eq("storeXMLelement"), contains("stored"), eq(Status.DONE));
    }

    @Test
    public void testStoreXMLelementInvalidVarFormat() throws Exception {
        Command.responsebodies.put(key, XML_RESPONSE);
        setCommandFields("//item[@id='1']/name/text()", "noPercent", null, "storeXMLelement");

        ws.storeXMLelement();
        verify(report).updateTestLog(eq("storeXMLelement"), contains("Variable format"), eq(Status.DEBUG));
    }

    // ---- storeXMLelementInDataSheet ----

    @Test
    public void testStoreXMLelementInDataSheetInvalidFormat() throws Exception {
        Command.responsebodies.put(key, XML_RESPONSE);
        setCommandFields(null, "//item[@id='1']/name/text()", "noColonFormat", "storeXMLelementInDataSheet");

        ws.storeXMLelementInDataSheet();
        verify(report).updateTestLog(eq("storeXMLelementInDataSheet"), contains("format is invalid"), eq(Status.DEBUG));
    }
}

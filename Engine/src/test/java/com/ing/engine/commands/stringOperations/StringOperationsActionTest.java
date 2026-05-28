package com.ing.engine.commands.stringOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.ing.engine.commands.browser.Command;
import com.ing.engine.core.CommandControl;
import com.ing.engine.execution.data.UserDataAccess;
import com.ing.engine.reporting.TestCaseReport;
import com.ing.ingenious.api.status.Status;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests for StringOperations @Action methods using reflection-based field
 * injection. Covers Concat, Trim, Substring, Replace, ToLower, ToUpper,
 * Split, GetOccurence, GetLength.
 */
public class StringOperationsActionTest {

    @Mock private CommandControl cc;
    @Mock private TestCaseReport report;
    @Mock private UserDataAccess userData;

    private StringOperations stringOps;
    private AutoCloseable mocks;

    // We need a map to simulate getVar/addVar
    private HashMap<String, String> runtimeVars;

    @BeforeMethod
    public void setUp() throws Exception {
        mocks = MockitoAnnotations.openMocks(this);
        runtimeVars = new HashMap<>();

        // Create StringOperations via Mockito spy with CALLS_REAL_METHODS
        stringOps = mock(StringOperations.class, CALLS_REAL_METHODS);
        setField(stringOps, Command.class, "Report", report);
        setField(stringOps, Command.class, "userData", userData);

        // Mock addVar/getVar behavior via the Commander delegate
        // Since addVar/getVar are on Command but delegate to Commander,
        // we'll use doAnswer to track them
        doAnswer(inv -> {
            runtimeVars.put(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(stringOps).addVar(anyString(), anyString());

        doAnswer(inv -> runtimeVars.get(inv.getArgument(0)))
                .when(stringOps).getVar(anyString());
    }

    @AfterMethod
    public void tearDown() throws Exception {
        mocks.close();
    }

    private void setField(Object obj, Class<?> clazz, String name, Object value) throws Exception {
        Field f = clazz.getDeclaredField(name);
        f.setAccessible(true);
        f.set(obj, value);
    }

    private void setCommandFields(String data, String condition, String input) throws Exception {
        setField(stringOps, Command.class, "Data", data);
        setField(stringOps, Command.class, "Condition", condition);
        setField(stringOps, Command.class, "Input", input);
    }

    // ---- Trim ----

    @Test
    public void testTrimSuccess() throws Exception {
        setCommandFields("\"  hello  \"", "%result%", "\"  hello  \"");
        stringOps.Trim();
        assertThat(runtimeVars.get("%result%")).isEqualTo("hello");
        verify(report).updateTestLog(eq("Trim"), contains("trimmed"), eq(Status.DONE));
    }

    @Test
    public void testTrimNoCondition() throws Exception {
        setCommandFields("\"hello\"", "", "\"hello\"");
        stringOps.Trim();
        verify(report).updateTestLog(eq("Trim"), contains("No variable name"), eq(Status.FAIL));
    }

    @Test
    public void testTrimInvalidInput() throws Exception {
        setCommandFields("invalidFormat", "%result%", "invalidFormat");
        stringOps.Trim();
        verify(report).updateTestLog(eq("Trim"), contains("invalid input format"), eq(Status.FAIL));
    }

    // ---- ToLower ----

    @Test
    public void testToLowerSuccess() throws Exception {
        setCommandFields("\"HELLO\"", "%result%", "\"HELLO\"");
        stringOps.ToLower();
        assertThat(runtimeVars.get("%result%")).isEqualTo("hello");
        verify(report).updateTestLog(eq("ToLower"), contains("lower case"), eq(Status.DONE));
    }

    @Test
    public void testToLowerNoCondition() throws Exception {
        setCommandFields("\"HELLO\"", "", "\"HELLO\"");
        stringOps.ToLower();
        verify(report).updateTestLog(eq("ToLower"), contains("No variable name"), eq(Status.FAIL));
    }

    // ---- ToUpper ----

    @Test
    public void testToUpperSuccess() throws Exception {
        setCommandFields("\"hello\"", "%result%", "\"hello\"");
        stringOps.ToUpper();
        assertThat(runtimeVars.get("%result%")).isEqualTo("HELLO");
        verify(report).updateTestLog(eq("ToUpper"), contains("upper case"), eq(Status.DONE));
    }

    // ---- GetLength ----

    @Test
    public void testGetLengthSuccess() throws Exception {
        setCommandFields("\"hello\"", "%len%", "\"hello\"");
        stringOps.GetLength();
        assertThat(runtimeVars.get("%len%")).isEqualTo("5");
        verify(report).updateTestLog(eq("getLength"), contains("5"), eq(Status.DONE));
    }

    @Test
    public void testGetLengthEmptyInput() throws Exception {
        setCommandFields("invalidFormat", "%len%", "invalidFormat");
        stringOps.GetLength();
        verify(report).updateTestLog(eq("getLength"), contains("invalid input format"), eq(Status.FAIL));
    }

    // ---- Concat ----

    @Test
    public void testConcatTwoStrings() throws Exception {
        String inputData = "\"Hello\",\" World\"";
        setCommandFields(inputData, "%result%", inputData);
        stringOps.Concat();
        assertThat(runtimeVars.get("%result%")).isEqualTo("Hello World");
        verify(report).updateTestLog(eq("Concat"), contains("concatenated"), eq(Status.DONE));
    }

    @Test
    public void testConcatNoCondition() throws Exception {
        String inputData = "\"A\",\"B\"";
        setCommandFields(inputData, "", inputData);
        stringOps.Concat();
        verify(report).updateTestLog(eq("Concat"), contains("No variable name"), eq(Status.FAIL));
    }

    @Test
    public void testConcatExceedsLimit() throws Exception {
        // Limit is 5, so 6 items should fail
        String inputData = "\"a\",\"b\",\"c\",\"d\",\"e\",\"f\"";
        setCommandFields(inputData, "%result%", inputData);
        stringOps.Concat();
        verify(report).updateTestLog(eq("Concat"), contains("exceeds expected limit"), eq(Status.FAIL));
    }

    // ---- Substring ----

    @Test
    public void testSubstringSuccess() throws Exception {
        String inputData = "\"Hello World\",\"0\",\"5\"";
        setCommandFields(inputData, "%result%", inputData);
        stringOps.Substring();
        assertThat(runtimeVars.get("%result%")).isEqualTo("Hello");
        verify(report).updateTestLog(eq("Substring"), contains("Hello"), eq(Status.DONE));
    }

    @Test
    public void testSubstringTwoArgs() throws Exception {
        // With only 2 args, second arg is start index, end = s.length()
        String inputData = "\"Hello\",\"1\"";
        setCommandFields(inputData, "%result%", inputData);
        stringOps.Substring();
        // s="Hello" (length=5), start=1, end=5, substring(1,5) = "ello"
        assertThat(runtimeVars.get("%result%")).isEqualTo("ello");
    }

    @Test
    public void testSubstringNoCondition() throws Exception {
        String inputData = "\"Hello\",\"0\",\"3\"";
        setCommandFields(inputData, "", inputData);
        stringOps.Substring();
        verify(report).updateTestLog(eq("Substring"), contains("No variable name"), eq(Status.FAIL));
    }

    // ---- Replace ----

    @Test
    public void testReplaceFirst() throws Exception {
        String inputData = "\"Hello World World\",\"World\",\"Earth\",\"first\"";
        setCommandFields(inputData, "%result%", inputData);
        stringOps.Replace();
        assertThat(runtimeVars.get("%result%")).isEqualTo("Hello Earth World");
        verify(report).updateTestLog(eq("Replace"), contains("First instance"), eq(Status.DONE));
    }

    @Test
    public void testReplaceAll() throws Exception {
        String inputData = "\"Hello World World\",\"World\",\"Earth\",\"all\"";
        setCommandFields(inputData, "%result%", inputData);
        stringOps.Replace();
        assertThat(runtimeVars.get("%result%")).isEqualTo("Hello Earth Earth");
        verify(report).updateTestLog(eq("Replace"), contains("All instances"), eq(Status.DONE));
    }

    @Test
    public void testReplaceInvalidType() throws Exception {
        String inputData = "\"Hello World\",\"World\",\"Earth\",\"invalid\"";
        setCommandFields(inputData, "%result%", inputData);
        stringOps.Replace();
        verify(report).updateTestLog(eq("Replace"), contains("'first' or 'all'"), eq(Status.FAIL));
    }

    // ---- Split ----

    @Test
    public void testSplitSuccess() throws Exception {
        String inputData = "\"a-b-c\",\"-\",\"1\"";
        setCommandFields(inputData, "%result%", inputData);
        stringOps.Split();
        assertThat(runtimeVars.get("%result%")).isEqualTo("b");
        verify(report).updateTestLog(eq("Split"), contains("split"), eq(Status.DONE));
    }

    @Test
    public void testSplitInvalidIndex() throws Exception {
        String inputData = "\"a-b\",\"-\",\"5\"";
        setCommandFields(inputData, "%result%", inputData);
        stringOps.Split();
        verify(report).updateTestLog(eq("Split"), contains("out of bound"), eq(Status.FAIL));
    }

    // ---- GetOccurence ----

    @Test
    public void testGetOccurenceSuccess() throws Exception {
        String inputData = "\"hello world\",\"l\"";
        setCommandFields(inputData, "%count%", inputData);
        stringOps.GetOccurence();
        assertThat(runtimeVars.get("%count%")).isEqualTo("3");
        verify(report).updateTestLog(eq("getOccurence"), contains("3"), eq(Status.DONE));
    }

    @Test
    public void testGetOccurenceMultiCharFails() throws Exception {
        String inputData = "\"hello\",\"ll\"";
        setCommandFields(inputData, "%count%", inputData);
        stringOps.GetOccurence();
        verify(report).updateTestLog(eq("getOccurence"), contains("single character"), eq(Status.FAIL));
    }
}

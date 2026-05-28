package com.ing.engine.commands.browser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.ing.engine.core.CommandControl;
import com.ing.engine.drivers.AutomationObject;
import com.ing.engine.reporting.TestCaseReport;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests for DynamicObject — setProperty map manipulation and
 * setglobalObjectProperty / setObjectProperty parsing.
 */
public class DynamicObjectTest {

    @Mock private TestCaseReport report;
    @Mock private CommandControl commander;

    private DynamicObject dynObj;
    private AutoCloseable mocks;

    @BeforeMethod
    public void setUp() throws Exception {
        mocks = MockitoAnnotations.openMocks(this);

        dynObj = mock(DynamicObject.class, CALLS_REAL_METHODS);
        setField(dynObj, Command.class, "Report", report);
        setField(dynObj, Command.class, "Commander", commander);

        // Clear static maps before each test
        AutomationObject.globalDynamicValue.clear();
        AutomationObject.dynamicValue.clear();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        AutomationObject.globalDynamicValue.clear();
        AutomationObject.dynamicValue.clear();
        mocks.close();
    }

    // ── setglobalObjectProperty ─────────────────────────────────────────

    @Test
    public void testSetGlobalObjectPropertyWithConditionAndData() throws Exception {
        setField(dynObj, Command.class, "Data", "myValue");
        setField(dynObj, Command.class, "Condition", "myKey");
        setField(dynObj, Command.class, "Action", "setglobalObjectProperty");

        dynObj.setglobalObjectProperty();

        assertThat(AutomationObject.globalDynamicValue).containsEntry("myKey", "myValue");
    }

    @Test
    public void testSetGlobalObjectPropertyParsesCommaSeparatedPairs() throws Exception {
        setField(dynObj, Command.class, "Data", "k1=v1,k2=v2,k3=v3");
        setField(dynObj, Command.class, "Condition", "");
        setField(dynObj, Command.class, "Action", "setglobalObjectProperty");

        dynObj.setglobalObjectProperty();

        assertThat(AutomationObject.globalDynamicValue)
                .containsEntry("k1", "v1")
                .containsEntry("k2", "v2")
                .containsEntry("k3", "v3");
    }

    @Test
    public void testSetGlobalObjectPropertyValueWithEquals() throws Exception {
        // value contains '=' — split limit 2 preserves it
        setField(dynObj, Command.class, "Data", "url=http://host?a=1");
        setField(dynObj, Command.class, "Condition", "");
        setField(dynObj, Command.class, "Action", "setglobalObjectProperty");

        dynObj.setglobalObjectProperty();

        assertThat(AutomationObject.globalDynamicValue)
                .containsEntry("url", "http://host?a=1");
    }

    @Test
    public void testSetGlobalObjectPropertyEmptyDataReportsFail() throws Exception {
        setField(dynObj, Command.class, "Data", "");
        setField(dynObj, Command.class, "Action", "setglobalObjectProperty");

        dynObj.setglobalObjectProperty();

        verify(report).updateTestLog(eq("setglobalObjectProperty"),
                eq("Input should not be empty"), any());
    }

    // ── setObjectProperty ───────────────────────────────────────────────

    @Test
    public void testSetObjectPropertyWithCondition() throws Exception {
        setField(dynObj, Command.class, "Data", "someVal");
        setField(dynObj, Command.class, "Condition", "propKey");
        setField(dynObj, Command.class, "Reference", "Page1");
        setField(dynObj, Command.class, "ObjectName", "Btn1");
        setField(dynObj, Command.class, "Action", "setObjectProperty");

        dynObj.setObjectProperty();

        assertThat(AutomationObject.dynamicValue)
                .containsKey("Page1");
        assertThat(AutomationObject.dynamicValue.get("Page1"))
                .containsKey("Btn1");
        assertThat(AutomationObject.dynamicValue.get("Page1").get("Btn1"))
                .containsEntry("propKey", "someVal");
    }

    @Test
    public void testSetObjectPropertyParsesCommaSeparatedPairs() throws Exception {
        setField(dynObj, Command.class, "Data", "a=1,b=2");
        setField(dynObj, Command.class, "Condition", "");
        setField(dynObj, Command.class, "Reference", "Ref1");
        setField(dynObj, Command.class, "ObjectName", "Obj1");
        setField(dynObj, Command.class, "Action", "setObjectProperty");

        dynObj.setObjectProperty();

        Map<String, String> props = AutomationObject.dynamicValue.get("Ref1").get("Obj1");
        assertThat(props).containsEntry("a", "1").containsEntry("b", "2");
    }

    @Test
    public void testSetObjectPropertyAddsToExistingReference() throws Exception {
        // Pre-populate for Ref1/ObjA
        Map<String, String> existingProp = new HashMap<>();
        existingProp.put("x", "0");
        Map<String, Map<String, String>> existingObj = new HashMap<>();
        existingObj.put("ObjA", existingProp);
        AutomationObject.dynamicValue.put("Ref1", existingObj);

        setField(dynObj, Command.class, "Data", "newKey=newVal");
        setField(dynObj, Command.class, "Condition", "");
        setField(dynObj, Command.class, "Reference", "Ref1");
        setField(dynObj, Command.class, "ObjectName", "ObjB");
        setField(dynObj, Command.class, "Action", "setObjectProperty");

        dynObj.setObjectProperty();

        // ObjA still present
        assertThat(AutomationObject.dynamicValue.get("Ref1")).containsKey("ObjA");
        // ObjB added
        assertThat(AutomationObject.dynamicValue.get("Ref1").get("ObjB"))
                .containsEntry("newKey", "newVal");
    }

    @Test
    public void testSetObjectPropertyUpdatesExistingObject() throws Exception {
        // Pre-populate Ref1/Obj1 with key "a"
        Map<String, String> prop = new HashMap<>();
        prop.put("a", "old");
        Map<String, Map<String, String>> objMap = new HashMap<>();
        objMap.put("Obj1", prop);
        AutomationObject.dynamicValue.put("Ref1", objMap);

        setField(dynObj, Command.class, "Data", "a=new");
        setField(dynObj, Command.class, "Condition", "");
        setField(dynObj, Command.class, "Reference", "Ref1");
        setField(dynObj, Command.class, "ObjectName", "Obj1");
        setField(dynObj, Command.class, "Action", "setObjectProperty");

        dynObj.setObjectProperty();

        assertThat(AutomationObject.dynamicValue.get("Ref1").get("Obj1"))
                .containsEntry("a", "new");
    }

    @Test
    public void testSetObjectPropertyEmptyDataReportsFail() throws Exception {
        setField(dynObj, Command.class, "Data", "");
        setField(dynObj, Command.class, "Action", "setObjectProperty");

        dynObj.setObjectProperty();

        verify(report).updateTestLog(eq("setObjectProperty"),
                eq("Input should not be empty"), any());
    }

    // ── Utility ─────────────────────────────────────────────────────────

    private static void setField(Object target, Class<?> clazz, String fieldName, Object value) throws Exception {
        Field f = clazz.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }
}

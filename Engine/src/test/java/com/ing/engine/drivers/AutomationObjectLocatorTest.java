package com.ing.engine.drivers;

import static org.assertj.core.api.Assertions.assertThat;

import com.ing.datalib.or.common.ORAttribute;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests for AutomationObject — static utility methods, FindType enum,
 * chainLocatorMapping, getRuntimeValue, storeElementDetailsinOR, getAttributeValue.
 */
public class AutomationObjectLocatorTest {

    private AutomationObject ao;

    @BeforeMethod
    public void setUp() {
        ao = new AutomationObject();
        AutomationObject.dynamicValue.clear();
        AutomationObject.globalDynamicValue.clear();
        AutomationObject.chainLocatorMaping.clear();
    }

    @AfterMethod
    public void tearDown() {
        AutomationObject.dynamicValue.clear();
        AutomationObject.globalDynamicValue.clear();
        AutomationObject.chainLocatorMaping.clear();
    }

    // ── FindType enum ───────────────────────────────────────────────────

    @Test
    public void testFindTypeFromStringGlobalObject() {
        assertThat(AutomationObject.FindType.fromString("globalobject"))
                .isEqualTo(AutomationObject.FindType.GLOBAL_OBJECT);
    }

    @Test
    public void testFindTypeFromStringGlobalObjectUpperCase() {
        assertThat(AutomationObject.FindType.fromString("GLOBALOBJECT"))
                .isEqualTo(AutomationObject.FindType.GLOBAL_OBJECT);
    }

    @Test
    public void testFindTypeFromStringDefault() {
        assertThat(AutomationObject.FindType.fromString("anything"))
                .isEqualTo(AutomationObject.FindType.DEFAULT);
    }

    @Test
    public void testFindTypeFromStringEmpty() {
        assertThat(AutomationObject.FindType.fromString(""))
                .isEqualTo(AutomationObject.FindType.DEFAULT);
    }

    @Test
    public void testFindTypeValues() {
        assertThat(AutomationObject.FindType.values()).hasSize(2);
    }

    // ── chainLocatorMapping ─────────────────────────────────────────────

    @Test
    public void testChainLocatorMappingGetByRole() {
        AutomationObject.chainLocatorMapping("getByRole(AriaRole.BUTTON, new GetByRoleOptions().setName(\"Submit\"))");

        assertThat(AutomationObject.chainLocatorMaping.get("roleType")).isEqualTo("BUTTON");
        assertThat(AutomationObject.chainLocatorMaping.get("setName")).isEqualTo("Submit");
    }

    @Test
    public void testChainLocatorMappingGetByRoleWithoutName() {
        AutomationObject.chainLocatorMapping("getByRole(AriaRole.LINK)");

        assertThat(AutomationObject.chainLocatorMaping.get("roleType")).isEqualTo("LINK");
        assertThat(AutomationObject.chainLocatorMaping).doesNotContainKey("setName");
    }

    @Test
    public void testChainLocatorMappingLocator() {
        AutomationObject.chainLocatorMapping("locator(\"div.container\")");

        assertThat(AutomationObject.chainLocatorMaping.get("locator")).isEqualTo("div.container");
    }

    @Test
    public void testChainLocatorMappingFilter() {
        AutomationObject.chainLocatorMapping("filter(new Locator.FilterOptions().setHasText(\"Hello\"))");

        assertThat(AutomationObject.chainLocatorMaping.get("setHasText")).isEqualTo("Hello");
    }

    @Test
    public void testChainLocatorMappingGetByPlaceholder() {
        AutomationObject.chainLocatorMapping("getByPlaceholder(\"Enter name\")");

        assertThat(AutomationObject.chainLocatorMaping.get("placeholder")).isEqualTo("Enter name");
    }

    @Test
    public void testChainLocatorMappingGetByLabel() {
        AutomationObject.chainLocatorMapping("getByLabel(\"Email\")");

        assertThat(AutomationObject.chainLocatorMaping.get("label")).isEqualTo("Email");
    }

    @Test
    public void testChainLocatorMappingGetByAltText() {
        AutomationObject.chainLocatorMapping("getByAltText(\"Logo\")");

        assertThat(AutomationObject.chainLocatorMaping.get("lable")).isEqualTo("Logo");
    }

    @Test
    public void testChainLocatorMappingGetByText() {
        AutomationObject.chainLocatorMapping("getByText(\"Click Me\")");

        assertThat(AutomationObject.chainLocatorMaping.get("text")).isEqualTo("Click Me");
    }

    @Test
    public void testChainLocatorMappingFrameLocator() {
        AutomationObject.chainLocatorMapping("frameLocator(\"#myFrame\")");

        assertThat(AutomationObject.chainLocatorMaping.get("frameLocator")).isEqualTo("#myFrame");
    }

    @Test
    public void testChainLocatorMappingSetExact() {
        AutomationObject.chainLocatorMapping("getByText(\"Save\").setExact(true)");

        assertThat(AutomationObject.chainLocatorMaping.get("setExact")).isEqualTo("true");
        assertThat(AutomationObject.chainLocatorMaping.get("text")).isEqualTo("Save");
    }

    // ── getRuntimeValue (private) ───────────────────────────────────────

    @Test
    public void testGetRuntimeValueWithGlobalDynamic() throws Exception {
        AutomationObject.globalDynamicValue.put("{env}", "production");
        ao.findType = AutomationObject.FindType.GLOBAL_OBJECT;

        String result = invokeGetRuntimeValue("https://{env}.example.com");
        assertThat(result).isEqualTo("https://production.example.com");
    }

    @Test
    public void testGetRuntimeValueWithObjectDynamic() throws Exception {
        Map<String, String> props = new HashMap<>();
        props.put("{id}", "42");
        Map<String, Map<String, String>> objMap = new HashMap<>();
        objMap.put("Btn1", props);
        AutomationObject.dynamicValue.put("Page1", objMap);

        ao.pageName = "Page1";
        ao.objectName = "Btn1";

        String result = invokeGetRuntimeValue("/users/{id}");
        assertThat(result).isEqualTo("/users/42");
    }

    @Test
    public void testGetRuntimeValueNoMatchReturnsSame() throws Exception {
        String result = invokeGetRuntimeValue("static-value");
        assertThat(result).isEqualTo("static-value");
    }

    @Test
    public void testGetRuntimeValueMultipleReplacements() throws Exception {
        AutomationObject.globalDynamicValue.put("{a}", "X");
        AutomationObject.globalDynamicValue.put("{b}", "Y");
        ao.findType = AutomationObject.FindType.GLOBAL_OBJECT;

        String result = invokeGetRuntimeValue("{a}-{b}");
        assertThat(result).isEqualTo("X-Y");
    }

    // ── storeElementDetailsinOR ─────────────────────────────────────────

    @Test
    public void testStoreElementDetailsUpdatesMatchingAttribute() {
        List<ORAttribute> attrs = new ArrayList<>();
        ORAttribute attr1 = createORAttribute("id", "old-id");
        ORAttribute attr2 = createORAttribute("name", "myName");
        attrs.add(attr1);
        attrs.add(attr2);

        ao.storeElementDetailsinOR(attrs, "id", "new-id");

        assertThat(attr1.getValue()).isEqualTo("new-id");
        assertThat(attr2.getValue()).isEqualTo("myName"); // unchanged
    }

    @Test
    public void testStoreElementDetailsNoMatchDoesNothing() {
        List<ORAttribute> attrs = new ArrayList<>();
        ORAttribute attr = createORAttribute("id", "old-id");
        attrs.add(attr);

        ao.storeElementDetailsinOR(attrs, "class", "myClass");

        assertThat(attr.getValue()).isEqualTo("old-id");
    }

    // ── getAttributeValue ───────────────────────────────────────────────

    @Test
    public void testGetAttributeValueFound() {
        List<ORAttribute> attrs = new ArrayList<>();
        attrs.add(createORAttribute("xpath", "//div[@id='main']"));
        attrs.add(createORAttribute("name", "mainDiv"));

        assertThat(ao.getAttributeValue(attrs, "xpath")).isEqualTo("//div[@id='main']");
    }

    @Test
    public void testGetAttributeValueNotFound() {
        List<ORAttribute> attrs = new ArrayList<>();
        attrs.add(createORAttribute("id", "123"));

        assertThat(ao.getAttributeValue(attrs, "class")).isNull();
    }

    @Test
    public void testGetAttributeValueEmptyList() {
        assertThat(ao.getAttributeValue(new ArrayList<>(), "id")).isNull();
    }

    /**
     * Helper: ORAttribute(String, int) sets value to ""; we set it afterward.
     */
    private static ORAttribute createORAttribute(String name, String value) {
        ORAttribute attr = new ORAttribute(name, 0);
        attr.setValue(value);
        return attr;
    }

    // ── waitTime management ─────────────────────────────────────────────

    @Test
    public void testSetAndResetWaitTime() throws Exception {
        ao.setWaitTime(Duration.ofSeconds(10));

        java.lang.reflect.Method getWaitTime = AutomationObject.class.getDeclaredMethod("getWaitTime");
        getWaitTime.setAccessible(true);
        Duration d = (Duration) getWaitTime.invoke(ao);
        assertThat(d).isEqualTo(Duration.ofSeconds(10));

        ao.resetWaitTime();
        // After reset, getWaitTime returns SystemDefaults.elementWaitTime
    }

    // ── Static map operations ───────────────────────────────────────────

    @Test
    public void testDynamicValueMapOperations() {
        AutomationObject.dynamicValue.put("Page1", new HashMap<>());
        assertThat(AutomationObject.dynamicValue).containsKey("Page1");
        AutomationObject.dynamicValue.clear();
        assertThat(AutomationObject.dynamicValue).isEmpty();
    }

    @Test
    public void testGlobalDynamicValueMapOperations() {
        AutomationObject.globalDynamicValue.put("key", "value");
        assertThat(AutomationObject.globalDynamicValue).containsEntry("key", "value");
    }

    // ── page/getPage/setPage ────────────────────────────────────────────

    @Test
    public void testPageGetterSetterNull() {
        assertThat(ao.getPage()).isNull();
    }

    @Test
    public void testPageGetterSetter() {
        com.microsoft.playwright.Page mockPage = org.mockito.Mockito.mock(com.microsoft.playwright.Page.class);
        ao.setPage(mockPage);
        assertThat(ao.getPage()).isSameAs(mockPage);
    }

    // ── Utility ─────────────────────────────────────────────────────────

    private String invokeGetRuntimeValue(String value) throws Exception {
        java.lang.reflect.Method m = AutomationObject.class.getDeclaredMethod("getRuntimeValue", String.class);
        m.setAccessible(true);
        return (String) m.invoke(ao, value);
    }
}

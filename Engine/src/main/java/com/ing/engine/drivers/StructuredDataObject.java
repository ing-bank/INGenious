package com.ing.engine.drivers;

import com.ing.datalib.or.ObjectRepository;
import com.ing.datalib.or.common.ORAttribute;
import com.ing.datalib.or.common.ObjectGroup;
import com.ing.datalib.or.mobile.MobileORObject;
import com.ing.datalib.or.mobile.MobileORPage;
import com.ing.datalib.or.mobile.ResolvedMobileObject;
import com.ing.datalib.or.structureddata.ResolvedStructuredDataObject;
import com.ing.datalib.or.structureddata.StructuredDataAttribute;
import com.ing.datalib.or.structureddata.StructuredDataORObject;
import com.ing.datalib.or.structureddata.StructuredDataORPage;
import com.ing.datalib.or.web.ResolvedWebObject;
import com.ing.datalib.or.web.WebORObject;
import com.ing.datalib.or.web.WebORPage;
import com.ing.engine.constants.SystemDefaults;
import com.ing.engine.core.CommandControl;
import com.ing.engine.core.Control;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.FrameLocator;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

public class StructuredDataObject {

    public StructuredDataObject(CommandControl cc) {
        super();
    }

    public Page page;
    BrowserContext browserContext;

    public Page getPage() {
        return page;
    }

    public void setPage(Page page) {
        this.page = page;
    }

    String pageName;
    String objectName;
    FindType findType;
    private Duration waitTime;

    public static HashMap<String, Map<String, Map<String, String>>> dynamicValue = new HashMap<>();
    public static HashMap<String, String> globalDynamicValue = new HashMap<>();
    public static String Action = "";
    static HashMap<String, String> chainLocatorMaping = new HashMap<String, String>();
    public static final Map<String, List<String>> locatorFiltersMap = new HashMap<>();

    public enum FindType {
        GLOBAL_OBJECT,
        DEFAULT;

        public static FindType fromString(String val) {
            switch (val.toLowerCase()) {
                case "globalobject":
                    return GLOBAL_OBJECT;
                default:
                    return DEFAULT;
            }
        }
    }

    public StructuredDataObject() {}

    public StructuredDataObject(Page Page) {
        this.page = Page;
    }

    public StructuredDataObject(BrowserContext BrowserContext) {
        this.browserContext = BrowserContext;
    }

    /**
     *
     * @param objectKey ObjectName in pageKey in OR
     * @param pageKey PageName in OR
     * @return
     */
    public String findElement(String objectKey, String pageKey) {
        String e = findElement(objectKey, pageKey, FindType.DEFAULT);
        return e;
    }

    /**
     *
     * @param element Driver or WebElement
     * @param objectKey ObjectName in pageKey in OR
     * @param Attribute
     * @param condition
     * @param pageKey PageName in OR
     * @return
     */
    public String findElement(String objectKey, String pageKey, String Attribute) {
        return findElement(objectKey, pageKey, Attribute, FindType.DEFAULT);
    }

    public String findElement(String objectKey, String pageKey, FindType condition) {
        return findElement(page, objectKey, pageKey, condition);
    }

    public String findElement(Page page, String objectKey, String pageKey, FindType condition) {
        pageName = pageKey;
        objectName = objectKey;
        findType = condition;
        return getElementFromList(findElements(getORObject(pageKey, objectKey), null));
    }

    public String findElement(
        String objectKey,
        String pageKey,
        String Attribute,
        FindType condition
    ) {
        pageName = pageKey;
        objectName = objectKey;
        findType = condition;

        return getElementFromList(findElements(getORObject(pageKey, objectKey), Attribute));
    }

    public List<String> findElements(String objectKey, String pageKey) {
        return findElements(objectKey, pageKey, FindType.DEFAULT);
    }

    public List<String> findElements(String objectKey, String pageKey, String Attribute) {
        return findElements(objectKey, pageKey, Attribute, FindType.DEFAULT);
    }

    public List<String> findElements(String objectKey, String pageKey, FindType condition) {
        //return findElements(objectKey, pageKey, condition);
        return findElements(objectKey, pageKey, null, condition);
    }

    public List<String> findElements(
        String objectKey,
        String pageKey,
        String Attribute,
        FindType condition
    ) {
        return findElements(objectKey, pageKey, Attribute, condition);
    }

    private String getElementFromList(List<String> elements) {
        return elements != null && !elements.isEmpty() ? elements.get(0) : null;
    }

    public ObjectGroup<?> getORObject(String page, String object) {
        ObjectRepository objRep = Control.getCurrentProject().getObjectRepository();
        try {
            ResolvedWebObject.PageRef wref = ResolvedWebObject.PageRef.parse(page);
            ResolvedWebObject wresolved = objRep.resolveWebObject(wref, object);
            if (wresolved != null && wresolved.getGroup() != null) {
                return wresolved.getGroup();
            }
        } catch (Exception ignore) {}
        try {
            ResolvedMobileObject.PageRef mref = ResolvedMobileObject.PageRef.parse(page);
            ResolvedMobileObject mresolved = objRep.resolveMobileObject(mref, object);
            if (mresolved != null && mresolved.getGroup() != null) {
                return mresolved.getGroup();
            }
        } catch (Exception ignore) {}
        try {
            ResolvedStructuredDataObject.PageRef aref = ResolvedStructuredDataObject.PageRef.parse(
                page
            );
            ResolvedStructuredDataObject aresolved = objRep.resolveStructuredDataObject(
                aref,
                object
            );
            if (aresolved != null && aresolved.getGroup() != null) {
                return aresolved.getGroup();
            }
        } catch (Exception ignore) {}
        if (objRep.getWebOR() != null && objRep.getWebOR().getPageByName(page) != null) {
            return objRep.getWebOR().getPageByName(page).getObjectGroupByName(object);
        } else if (
            objRep.getWebSharedOR() != null && objRep.getWebSharedOR().getPageByName(page) != null
        ) {
            return objRep.getWebSharedOR().getPageByName(page).getObjectGroupByName(object);
        } else if (
            objRep.getMobileOR() != null && objRep.getMobileOR().getPageByName(page) != null
        ) {
            return objRep.getMobileOR().getPageByName(page).getObjectGroupByName(object);
        } else if (
            objRep.getMobileSharedOR() != null &&
            objRep.getMobileSharedOR().getPageByName(page) != null
        ) {
            return objRep.getMobileSharedOR().getPageByName(page).getObjectGroupByName(object);
        } else if (
            objRep.getStructuredDataOR() != null &&
            objRep.getStructuredDataOR().getPageByName(page) != null
        ) {
            return objRep.getStructuredDataOR().getPageByName(page).getObjectGroupByName(object);
        } else if (
            objRep.getStructuredDataSharedOR() != null &&
            objRep.getStructuredDataSharedOR().getPageByName(page) != null
        ) {
            return objRep
                .getStructuredDataSharedOR()
                .getPageByName(page)
                .getObjectGroupByName(object);
        }
        return null;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private synchronized List<String> findElements(ObjectGroup objectGroup, String prop) {
        if (objectGroup != null && !objectGroup.getObjects().isEmpty()) {
            if (objectGroup.getObjects().get(0) instanceof StructuredDataORObject) {
                return getAPIElements(objectGroup, prop);
            }
        }
        return null;
    }

    private List<String> getAPIElements(
        ObjectGroup<StructuredDataORObject> objectGroup,
        String prop
    ) {
        long startTime = System.nanoTime();
        List<String> elements = null;
        for (StructuredDataORObject object : objectGroup.getObjects()) {
            elements = getElements(object.getAttributes());
            if (elements != null && !elements.isEmpty()) {
                break;
            }
        }
        //printStats(elements, objectGroup, startTime, System.nanoTime());
        return elements;
    }

    private void printStats(
        List<String> elements,
        ObjectGroup<?> objectGroup,
        long startTime,
        long stopTime
    ) {
        if (getElementFromList(elements) != null) {
            System.out.println(elements);
            System.out.println(foundElementIn(objectGroup, stopTime, startTime));
        } else {
            System.out.println(notFoundIn(objectGroup));
        }
    }

    private static String foundElementBy(String attr, String val) {
        return String.format("Object being identified with [%s] = [%s], ", attr, val);
    }

    private static String foundElementIn(
        ObjectGroup<?> objectGroup,
        long stopTime,
        long startTime
    ) {
        return String.format(
            "Object [%s] found in [%s] ms",
            objectGroup.getName(),
            (stopTime - startTime) / 1000000
        );
    }

    private String notFoundIn(ObjectGroup<?> objectGroup) {
        return String.format(
            "Couldn't find Object '%s' in stipulated Time '%s' Seconds",
            objectGroup.getName(),
            String.valueOf(getWaitTime().toSeconds())
        );
    }

    private List<String> getElements(final List<StructuredDataAttribute> attributes) {
        return getElementsInternal(attributes);
        //        return getElementsInternal(attributes -> {
        //            String el = null;
        ////            switch (tag) {
        ////                case "Text":
        ////                    locator = this.page.getByText(value, (Page.GetByTextOptions) options);
        ////                    break;
        ////                case "Label":
        ////                    locator = this.page.getByLabel(value, (Page.GetByLabelOptions) options);
        ////                    break;
        ////                case "Placeholder":
        ////                    locator = this.page.getByPlaceholder(value, (Page.GetByPlaceholderOptions) options);
        ////                    break;
        ////                case "AltText":
        ////                    locator = this.page.getByAltText(value, (Page.GetByAltTextOptions) options);
        ////                    break;
        ////                case "Title":
        ////                    locator = this.page.getByTitle(value, (Page.GetByTitleOptions) options);
        ////                    break;
        ////                case "TestId":
        ////                    locator = this.page.getByTestId(value);
        ////                    break;
        ////                case "css":
        ////                    locator = this.page.locator("css=" + value);
        ////                    break;
        ////                case "xpath":
        ////                    locator = this.page.locator("xpath=" + value);
        ////                    break;
        ////                case "Role":
        ////                    locator = createRoleLocator(value, this.page);
        ////                    break;
        ////                case "ChainedLocator":
        ////                    locator = createChainedLocator(value, this.page);
        ////                    break;
        ////                default:
        ////                    locator = null;
        ////            }
        ////            // Apply filter if required
        ////            if (locator != null) {
        ////                locator = setFilter(locator);
        ////            }
        //            return el;
        //        });
    }

    private String getRuntimeValue(String value) {
        if (findType != null && findType.equals(FindType.GLOBAL_OBJECT)) {
            for (String Key : globalDynamicValue.keySet()) {
                value = value.replace(Key, globalDynamicValue.get(Key));
            }
        }
        if (
            dynamicValue.containsKey(pageName) && dynamicValue.get(pageName).containsKey(objectName)
        ) {
            for (String Key : dynamicValue.get(pageName).get(objectName).keySet()) {
                value = value.replace(Key, dynamicValue.get(pageName).get(objectName).get(Key));
            }
        }

        return value;
    }

    public void setDriver(Page page) {
        this.page = page;
    }

    private String stripScope(String pageKey) {
        if (pageKey == null) return null;
        int at = pageKey.lastIndexOf('@');
        return (at > 0) ? pageKey.substring(0, at) : pageKey;
    }

    public List<String> getObjectList(String page, String regexObject) {
        if (page == null || page.trim().isEmpty()) {
            throw new RuntimeException("Page Name is empty please give a valid pageName");
        }
        ObjectRepository objRep = Control.getCurrentProject().getObjectRepository();
        WebORPage wPage = null;
        MobileORPage mPage = null;
        StructuredDataORPage aPage = null;
        try {
            ResolvedWebObject.PageRef ref = ResolvedWebObject.PageRef.parse(page);

            if (ref != null && ref.scope != null) {
                // Scoped: pick only the specified OR
                if (ref.scope.name().equals("SHARED")) {
                    wPage = objRep.getWebSharedOR().getPageByName(ref.name);
                } else {
                    wPage = objRep.getWebOR().getPageByName(ref.name);
                }
            } else {
                // Unscoped: project-first then shared
                wPage = objRep.getWebOR().getPageByName(ref.name);
                if (wPage == null) wPage = objRep.getWebSharedOR().getPageByName(ref.name);
            }
        } catch (Exception ignore) {
            // If parsing fails, treat as plain page name
            wPage = objRep.getWebOR().getPageByName(page);
            if (wPage == null) wPage = objRep.getWebSharedOR().getPageByName(page);
        }

        if (wPage == null && objRep.getMobileOR().getPageByName(stripScope(page)) != null) {
            mPage = objRep.getMobileOR().getPageByName(stripScope(page));
        }

        if (mPage == null && objRep.getStructuredDataOR().getPageByName(stripScope(page)) != null) {
            aPage = objRep.getStructuredDataOR().getPageByName(stripScope(page));
        }

        if (wPage == null && mPage == null && aPage == null) {
            throw new RuntimeException("Page [" + page + "] is not available in ObjectRepository");
        }
        List<String> elementList = new ArrayList<>();
        if (wPage != null) {
            for (ObjectGroup<WebORObject> objectgroup : wPage.getObjectGroups()) {
                if (objectgroup.getName().matches(regexObject)) {
                    elementList.add(regexObject);
                }
            }
        } else if (mPage != null) {
            for (ObjectGroup<MobileORObject> objectgroup : mPage.getObjectGroups()) {
                if (objectgroup.getName().matches(regexObject)) {
                    elementList.add(regexObject);
                }
            }
        } else if (aPage != null) {
            for (ObjectGroup<StructuredDataORObject> objectgroup : aPage.getObjectGroups()) {
                if (objectgroup.getName().matches(regexObject)) {
                    elementList.add(regexObject);
                }
            }
        }
        return elementList;
    }

    public void setWaitTime(Duration waitTime) {
        this.waitTime = waitTime;
    }

    public void resetWaitTime() {
        this.waitTime = null;
    }

    private Duration getWaitTime() {
        return this.waitTime != null ? this.waitTime : SystemDefaults.elementWaitTime;
    }

    public void storeElementDetailsinOR(
        List<ORAttribute> attributes,
        String attribute,
        String value
    ) {
        for (ORAttribute attr : attributes) {
            if (attr.getName().contentEquals(attribute)) {
                attr.setValue(value);
                break;
            }
        }
    }

    public String getAttributeValue(List<ORAttribute> attributes, String attribute) {
        for (ORAttribute attr : attributes) {
            if (attr.getName().contentEquals(attribute)) {
                return attr.getValue();
            }
        }
        return null;
    }

    private int getMinKey(Map<Integer, Integer> map, Object... object) {
        int minKey = 0;
        int minValue = Integer.MAX_VALUE;
        for (Object key : object) {
            int value = map.get(key);
            if (value < minValue) {
                minValue = value;
                minKey = (int) key;
            }
        }
        return minKey;
    }

    @FunctionalInterface
    private interface LocatorFactory {
        Locator create(String tag, String value, Object options);
    }

    private List<String> getElementsInternal(final List<StructuredDataAttribute> attributes) {
        if (attributes == null || attributes.isEmpty()) return null;
        List<String> elements = new ArrayList<>();
        for (StructuredDataAttribute attr : attributes) {
            String value = getRuntimeValue(attr.getValue() != null ? attr.getValue() : "");
            if (value.trim().isEmpty()) continue;
            elements.add(value);
            break; // Only first valid locator
            //            }
        }
        return elements.isEmpty() ? null : elements;
    }
}

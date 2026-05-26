package com.ing.engine.drivers;

import com.ing.datalib.or.ObjectRepository;
import com.ing.datalib.or.common.ORAttribute;
import com.ing.datalib.or.common.ObjectGroup;
import com.ing.datalib.or.mobile.MobileORObject;
import com.ing.datalib.or.mobile.MobileORPage;
import com.ing.datalib.or.mobile.ResolvedMobileObject;
import com.ing.datalib.or.structureddata.ResolvedStructuredDataObject;
import com.ing.datalib.or.structureddata.StructuredDataORObject;
import com.ing.datalib.or.web.WebORObject;
import com.ing.datalib.or.web.WebORPage;
import com.ing.datalib.or.web.ResolvedWebObject;
import com.ing.datalib.or.sap.ResolvedSapObject;
import com.ing.engine.constants.SystemDefaults;
import com.ing.engine.core.Control;
import com.ing.engine.core.CommandControl;
import com.ing.engine.reporting.intf.Report;
import com.ing.ingenious.api.contract.drivers.AutomationObjectApi;
import com.ing.ingenious.api.status.Status;
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

public class AutomationObject implements AutomationObjectApi {

    public AutomationObject(CommandControl cc) {
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

    public AutomationObject() {
    }

    public AutomationObject(Page Page) {
        this.page = Page;
    }

    public AutomationObject(BrowserContext BrowserContext) {
        this.browserContext = BrowserContext;
    }
    
    public enum LocatorType { LOCATOR, FRAMELOCATOR }

    /**
     *
     * @param objectKey ObjectName in pageKey in OR
     * @param pageKey PageName in OR
     * @return
     */
    public Locator findElement(String objectKey, String pageKey) {
        Locator e = findElement(objectKey, pageKey, FindType.DEFAULT);
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
    public Locator findElement(String objectKey, String pageKey, String Attribute) {
        return findElement(objectKey, pageKey, Attribute, FindType.DEFAULT);
    }

    public Locator findElement(String objectKey, String pageKey, FindType condition) {
        return findElement(page, objectKey, pageKey, condition);
    }

    public Locator findElement(Page page, String objectKey, String pageKey, FindType condition) {
        pageName = pageKey;
        objectName = objectKey;
        findType = condition;
        return getElementFromList(findElements(getORObject(pageKey, objectKey), null));
    }

    public Locator findElement(String objectKey, String pageKey, String Attribute, FindType condition) {
        pageName = pageKey;
        objectName = objectKey;
        findType = condition;

        return getElementFromList(findElements(getORObject(pageKey, objectKey), Attribute));
    }

    public List<Locator> findElements(String objectKey, String pageKey) {
        return findElements(objectKey, pageKey, FindType.DEFAULT);
    }

    public List<Locator> findElements(String objectKey, String pageKey, String Attribute) {
        return findElements(objectKey, pageKey, Attribute, FindType.DEFAULT);
    }

    public List<Locator> findElements(String objectKey, String pageKey, FindType condition) {
        //return findElements(objectKey, pageKey, condition);
        return findElements(objectKey, pageKey, null, condition);
    }

    public List<Locator> findElements(String objectKey, String pageKey, String Attribute, FindType condition) {
        return findElements(objectKey, pageKey, Attribute, condition);
    }

    private Locator getElementFromList(List<Locator> elements) {

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
        } catch (Exception ignore) { }
        try {
            ResolvedMobileObject.PageRef mref = ResolvedMobileObject.PageRef.parse(page);
            ResolvedMobileObject mresolved = objRep.resolveMobileObject(mref, object);
            if (mresolved != null && mresolved.getGroup() != null) {
                return mresolved.getGroup();
            }
        } catch (Exception ignore) { }
        try {
            ResolvedStructuredDataObject.PageRef sdref = ResolvedStructuredDataObject.PageRef.parse(page);
            ResolvedStructuredDataObject sdresolved = objRep.resolveStructuredDataObject(sdref, object);
            if (sdresolved != null && sdresolved.getGroup() != null) {
                return sdresolved.getGroup();
            }
        } catch (Exception ignore) { }
        try {
            ResolvedSapObject.PageRef sref = ResolvedSapObject.PageRef.parse(page);
            ResolvedSapObject sresolved = objRep.resolveSapObject(sref, object);
            if (sresolved != null && sresolved.getGroup() != null) {
                return sresolved.getGroup();
            }
        } catch (Exception ignore) { }
        if (objRep.getWebOR() != null && objRep.getWebOR().getPageByName(page) != null) {
            return objRep.getWebOR().getPageByName(page).getObjectGroupByName(object);
        } else if (objRep.getWebSharedOR() != null && objRep.getWebSharedOR().getPageByName(page) != null) {
            return objRep.getWebSharedOR().getPageByName(page).getObjectGroupByName(object);
        } else if (objRep.getMobileOR() != null && objRep.getMobileOR().getPageByName(page) != null) {
            return objRep.getMobileOR().getPageByName(page).getObjectGroupByName(object);
        } else if (objRep.getMobileSharedOR() != null && objRep.getMobileSharedOR().getPageByName(page) != null) {
            return objRep.getMobileSharedOR().getPageByName(page).getObjectGroupByName(object);
        } else if (objRep.getStructuredDataOR() != null && objRep.getStructuredDataOR().getPageByName(page) != null) {
            return objRep.getStructuredDataOR().getPageByName(page).getObjectGroupByName(object);
        } else if (objRep.getStructuredDataSharedOR() != null && objRep.getStructuredDataSharedOR().getPageByName(page) != null) {
            return objRep.getStructuredDataSharedOR().getPageByName(page).getObjectGroupByName(object);
        } else if (objRep.getSapOR() != null && objRep.getSapOR().getPageByName(page) != null) {
            return objRep.getSapOR().getPageByName(page).getObjectGroupByName(object);
        } else if (objRep.getSapSharedOR() != null && objRep.getSapSharedOR().getPageByName(page) != null) {
            return objRep.getSapSharedOR().getPageByName(page).getObjectGroupByName(object);
        }
        return null;
    }

    public String getObjectProperty(String pageName, String objectName, String propertyName) {
        return getWebObject(pageName, objectName).getAttributeByName(propertyName);
    }
    
    public ObjectGroup<WebORObject> getWebObjects(String page, String object) {
        ObjectRepository objRep = Control.getCurrentProject().getObjectRepository();

        try {
            ResolvedWebObject.PageRef ref = ResolvedWebObject.PageRef.parse(page);
            ResolvedWebObject resolved = objRep.resolveWebObject(ref, object);
            if (resolved != null && resolved.getGroup() != null) {
                return (ObjectGroup<WebORObject>) resolved.getGroup();
            }
        } catch (Exception ignore) { }
        if (objRep.getWebOR() != null && objRep.getWebOR().getPageByName(page) != null) {
            return objRep.getWebOR().getPageByName(page).getObjectGroupByName(object);
        } else if (objRep.getWebSharedOR() != null && objRep.getWebSharedOR().getPageByName(page) != null) {
            return objRep.getWebSharedOR().getPageByName(page).getObjectGroupByName(object);
        }
        return null;
    }

    public WebORObject getWebObject(String page, String object) {
        ObjectGroup<WebORObject> group = getWebObjects(page, object);
        if (group != null && group.getObjects() != null && !group.getObjects().isEmpty()) {
            return group.getObjects().get(0);
        }
        return null;
    }

    public ObjectGroup<MobileORObject> getMobileObjects(String page, String object) {
        ObjectRepository objRep = Control.getCurrentProject().getObjectRepository();
        if (objRep.getMobileOR() != null && objRep.getMobileOR().getPageByName(page) != null) {
            return objRep.getMobileOR().getPageByName(page).getObjectGroupByName(object);
        } else if (objRep.getMobileSharedOR() != null && objRep.getMobileSharedOR().getPageByName(page) != null) {
            return objRep.getMobileSharedOR().getPageByName(page).getObjectGroupByName(object);
        }
        return null;
    }

    public MobileORObject getMobileObject(String page, String object) {
        ObjectRepository objRep = Control.getCurrentProject().getObjectRepository();
        if (objRep.getMobileOR() != null && objRep.getMobileOR().getPageByName(page) != null) {
            return objRep.getMobileOR().getPageByName(page).getObjectGroupByName(object).getObjects().get(0);
        } else if (objRep.getMobileSharedOR() != null && objRep.getMobileSharedOR().getPageByName(page) != null) {
            return objRep.getMobileSharedOR().getPageByName(page).getObjectGroupByName(object).getObjects().get(0);
        }
        return null;
    }

    public ObjectGroup<StructuredDataORObject> getStructuredDataObjects(String page, String object) {
        ObjectRepository objRep = Control.getCurrentProject().getObjectRepository();
        if (objRep.getStructuredDataOR() != null && objRep.getStructuredDataOR().getPageByName(page) != null) {
            return objRep.getStructuredDataOR().getPageByName(page).getObjectGroupByName(object);
        } else if (objRep.getStructuredDataSharedOR() != null && objRep.getStructuredDataSharedOR().getPageByName(page) != null) {
            return objRep.getStructuredDataSharedOR().getPageByName(page).getObjectGroupByName(object);
        }
        return null;
    }

    public StructuredDataORObject getStructuredDataObject(String page, String object) {
        ObjectRepository objRep = Control.getCurrentProject().getObjectRepository();
        if (objRep.getMobileOR() != null && objRep.getMobileOR().getPageByName(page) != null) {
            return objRep.getStructuredDataOR().getPageByName(page).getObjectGroupByName(object).getObjects().get(0);
        } else if (objRep.getStructuredDataSharedOR() != null && objRep.getStructuredDataSharedOR().getPageByName(page) != null) {
            return objRep.getStructuredDataSharedOR().getPageByName(page).getObjectGroupByName(object).getObjects().get(0);
        }
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private synchronized List<Locator> findElements(ObjectGroup objectGroup, String prop) {

        if (objectGroup != null && !objectGroup.getObjects().isEmpty()) {
            if (objectGroup.getObjects().get(0) instanceof WebORObject) {

                return getWElements(objectGroup, prop);
            }
        }
        return null;
    }

    private List<Locator> getWElements(ObjectGroup<WebORObject> objectGroup, String prop) {
        long startTime = System.nanoTime();
        List<Locator> elements = null;
        for (WebORObject object : objectGroup.getObjects()) {
            FrameLocator framelocator = switchFrame(object.getFrame());
            if (framelocator != null) {
                elements = getElements(framelocator, object.getAttributes());
            } else {
                elements = getElements(object.getAttributes());
            }
            if (elements != null && !elements.isEmpty()) {
                break;
            }
        }
        //printStats(elements, objectGroup, startTime, System.nanoTime());
        return elements;
    }

    private void printStats(List<Locator> elements, ObjectGroup<?> objectGroup, long startTime, long stopTime) {
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

    private static String foundElementIn(ObjectGroup<?> objectGroup, long stopTime, long startTime) {
        return String.format("Object [%s] found in [%s] ms", objectGroup.getName(), (stopTime - startTime) / 1000000);
    }

    private String notFoundIn(ObjectGroup<?> objectGroup) {
        return String.format("Couldn't find Object '%s' in stipulated Time '%s' Seconds", objectGroup.getName(),
                String.valueOf(getWaitTime().toSeconds()));
    }

    private List<Locator> getElements(final List<ORAttribute> attributes) {
        return getElementsInternal(LocatorType.LOCATOR, attributes, (tag, value, options) -> {
            Locator locator = null;
            switch (tag) {
                case "Text":
                    locator = this.page.getByText(value, (Page.GetByTextOptions) options);
                    break;
                case "Label":
                    locator = this.page.getByLabel(value, (Page.GetByLabelOptions) options);
                    break;
                case "Placeholder":
                    locator = this.page.getByPlaceholder(value, (Page.GetByPlaceholderOptions) options);
                    break;
                case "AltText":
                    locator = this.page.getByAltText(value, (Page.GetByAltTextOptions) options);
                    break;
                case "Title":
                    locator = this.page.getByTitle(value, (Page.GetByTitleOptions) options);
                    break;
                case "TestId":
                    locator = this.page.getByTestId(value);
                    break;
                case "css":
                    locator = this.page.locator("css=" + value);
                    break;
                case "xpath":
                    locator = this.page.locator("xpath=" + value);
                    break;
                case "Role":
                    locator = createRoleLocator(value, this.page);
                    break;
                case "ChainedLocator":
                    locator = createChainedLocator(value, this.page);
                    break;
                default:
                    locator = null;
            }
            // Apply filter if required
            if (locator != null) {
                locator = setFilter(locator);
            }
            return locator;
        });
    }

    private List<Locator> getElements(FrameLocator framelocator, final List<ORAttribute> attributes) {
        return getElementsInternal(LocatorType.FRAMELOCATOR, attributes, (tag, value, options) -> {
            Locator locator = null;
            switch (tag) {
                case "Text":
                    locator = framelocator.getByText(value, (FrameLocator.GetByTextOptions) options);
                    break;
                case "Label":
                    locator = framelocator.getByLabel(value, (FrameLocator.GetByLabelOptions) options);
                    break;
                case "Placeholder":
                    locator = framelocator.getByPlaceholder(value, (FrameLocator.GetByPlaceholderOptions) options);
                    break;
                case "AltText":
                    locator = framelocator.getByAltText(value, (FrameLocator.GetByAltTextOptions) options);
                    break;
                case "Title":
                    locator = framelocator.getByTitle(value, (FrameLocator.GetByTitleOptions) options);
                    break;
                case "TestId":
                    locator = framelocator.getByTestId(value);
                    break;
                case "css":
                    locator = framelocator.locator("css=" + value).first();
                    break;
                case "xpath":
                    locator = framelocator.locator("xpath=" + value);
                    break;
                case "Role":
                    locator = createRoleLocator(value, framelocator);
                    break;
                case "ChainedLocator":
                    locator = createChainedLocator(value, framelocator);
                    break;
                default:
                    locator = null;
            }
            // Apply filter if required
            if (locator != null) {
                locator = setFilter(locator);
            }
            return locator;
        });
    }

    private static Locator chainLocators(String selector, int index, Page page, Locator locator) {
        Pattern pattern = null;
        Matcher matcher = null;
        chainLocatorMaping.clear();
        chainLocatorMapping(selector);
        String tag = selector.split("\\(")[0];

        switch (tag) {

            case "locator":
                Page.LocatorOptions pageLocatorOptions = new Page.LocatorOptions();
                Locator.LocatorOptions locatorLocatorOptions = new Locator.LocatorOptions();
                locator = (index == 0) ? page.locator(chainLocatorMaping.get("locator"), pageLocatorOptions)
                        : locator.locator(chainLocatorMaping.get("locator"), locatorLocatorOptions);
                break;

            case "getByRole":

                Page.GetByRoleOptions pageRoleOptions = new Page.GetByRoleOptions();
                Locator.GetByRoleOptions locatorRoleOptions = new Locator.GetByRoleOptions();

                if (chainLocatorMaping.containsKey("setName")) {
                    pageRoleOptions.setName(chainLocatorMaping.get("setName"));
                    locatorRoleOptions.setName(chainLocatorMaping.get("setName"));
                }
                if (chainLocatorMaping.containsKey("setExact")) {
                    pageRoleOptions.setExact(true);
                    locatorRoleOptions.setExact(true);
                }

                locator = (index == 0)
                        ? page.getByRole(AriaRole.valueOf(chainLocatorMaping.get("roleType")), pageRoleOptions)
                        : locator.getByRole(AriaRole.valueOf(chainLocatorMaping.get("roleType")), locatorRoleOptions);
                break;

            case "filter":

                Locator.FilterOptions locatorFilterOptions = new Locator.FilterOptions();

                if (chainLocatorMaping.containsKey("setHasText")) {
                    locatorFilterOptions.setHasText(chainLocatorMaping.get("setHasText"));
                }

                locator = locator.filter(locatorFilterOptions);

                break;

            case "getByPlaceholder":

                Page.GetByPlaceholderOptions pagePlaceHolderOptions = new Page.GetByPlaceholderOptions();
                Locator.GetByPlaceholderOptions locatorPlaceHolderOptions = new Locator.GetByPlaceholderOptions();

                if (chainLocatorMaping.containsKey("setExact")) {
                    pagePlaceHolderOptions.setExact(true);
                    locatorPlaceHolderOptions.setExact(true);
                }

                locator = (index == 0)
                        ? page.getByPlaceholder(chainLocatorMaping.get("placeholder"), pagePlaceHolderOptions)
                        : locator.getByPlaceholder(chainLocatorMaping.get("placeholder"), locatorPlaceHolderOptions);

                break;

            case "getByText":

                Page.GetByTextOptions pageTextOptions = new Page.GetByTextOptions();
                Locator.GetByTextOptions locatorTextOptions = new Locator.GetByTextOptions();

                if (chainLocatorMaping.containsKey("setExact")) {
                    pageTextOptions.setExact(true);
                    locatorTextOptions.setExact(true);
                }

                locator = (index == 0) ? page.getByText(chainLocatorMaping.get("text"), pageTextOptions)
                        : locator.getByText(chainLocatorMaping.get("text"), locatorTextOptions);
                break;

            case "getByAltText":

                Page.GetByAltTextOptions pageAltTextOptions = new Page.GetByAltTextOptions();
                Locator.GetByAltTextOptions locatorAltTextOptions = new Locator.GetByAltTextOptions();

                if (chainLocatorMaping.containsKey("setExact")) {
                    pageAltTextOptions.setExact(true);
                    locatorAltTextOptions.setExact(true);
                }

                locator = (index == 0) ? page.getByAltText(chainLocatorMaping.get("altText"), pageAltTextOptions)
                        : locator.getByAltText(chainLocatorMaping.get("altText"), locatorAltTextOptions);
                break;

            case "getByLabel":

                Page.GetByLabelOptions pageLableOptions = new Page.GetByLabelOptions();
                Locator.GetByLabelOptions locatorLableOptions = new Locator.GetByLabelOptions();

                if (chainLocatorMaping.containsKey("setExact")) {
                    pageLableOptions.setExact(true);
                    locatorLableOptions.setExact(true);
                }

                locator = (index == 0) ? page.getByLabel(chainLocatorMaping.get("label"), pageLableOptions)
                        : locator.getByLabel(chainLocatorMaping.get("label"), locatorLableOptions);
                break;

        }

        if (selector.matches("first\\(\\)")) {
            locator = locator.first();
        }
        if (selector.matches("last\\(\\)")) {
            locator = locator.last();
        }
        if (selector.matches("nth\\((\\d+)\\)")) {
            pattern = Pattern.compile("nth\\((\\d+)\\)");
            matcher = pattern.matcher(selector);
            //System.out.println("NTH");
            if (matcher.find()) {
                locator = locator.nth(Integer.parseInt(matcher.group(1)));
            }
        }

        return locator;
    }

    private static Locator chainLocators(String selector, int index, FrameLocator framelocator, Locator locator) {
        Pattern pattern = null;
        Matcher matcher = null;
        chainLocatorMaping.clear();
        chainLocatorMapping(selector);
        String tag = selector.split("\\(")[0];

        switch (tag) {

            case "locator":
                FrameLocator.LocatorOptions pageLocatorOptions = new FrameLocator.LocatorOptions();
                Locator.LocatorOptions locatorLocatorOptions = new Locator.LocatorOptions();
                locator = (index == 0) ? framelocator.locator(chainLocatorMaping.get("locator"), pageLocatorOptions)
                        : locator.locator(chainLocatorMaping.get("locator"), locatorLocatorOptions);
                break;

            case "getByRole":

                FrameLocator.GetByRoleOptions pageRoleOptions = new FrameLocator.GetByRoleOptions();
                Locator.GetByRoleOptions locatorRoleOptions = new Locator.GetByRoleOptions();

                if (chainLocatorMaping.containsKey("setName")) {
                    pageRoleOptions.setName(chainLocatorMaping.get("setName"));
                    locatorRoleOptions.setName(chainLocatorMaping.get("setName"));
                }
                if (chainLocatorMaping.containsKey("setExact")) {
                    pageRoleOptions.setExact(true);
                    locatorRoleOptions.setExact(true);
                }

                locator = (index == 0)
                        ? framelocator.getByRole(AriaRole.valueOf(chainLocatorMaping.get("roleType")), pageRoleOptions)
                        : locator.getByRole(AriaRole.valueOf(chainLocatorMaping.get("roleType")), locatorRoleOptions);
                break;

            case "filter":

                Locator.FilterOptions locatorFilterOptions = new Locator.FilterOptions();

                if (chainLocatorMaping.containsKey("setHasText")) {
                    locatorFilterOptions.setHasText(chainLocatorMaping.get("setHasText"));
                }

                locator = locator.filter(locatorFilterOptions);

                break;

            case "getByPlaceholder":

                FrameLocator.GetByPlaceholderOptions pagePlaceHolderOptions = new FrameLocator.GetByPlaceholderOptions();
                Locator.GetByPlaceholderOptions locatorPlaceHolderOptions = new Locator.GetByPlaceholderOptions();

                if (chainLocatorMaping.containsKey("setExact")) {
                    pagePlaceHolderOptions.setExact(true);
                    locatorPlaceHolderOptions.setExact(true);
                }

                locator = (index == 0)
                        ? framelocator.getByPlaceholder(chainLocatorMaping.get("placeholder"), pagePlaceHolderOptions)
                        : locator.getByPlaceholder(chainLocatorMaping.get("placeholder"), locatorPlaceHolderOptions);

                break;

            case "getByText":

                FrameLocator.GetByTextOptions pageTextOptions = new FrameLocator.GetByTextOptions();
                Locator.GetByTextOptions locatorTextOptions = new Locator.GetByTextOptions();

                if (chainLocatorMaping.containsKey("setExact")) {
                    pageTextOptions.setExact(true);
                    locatorTextOptions.setExact(true);
                }

                locator = (index == 0) ? framelocator.getByText(chainLocatorMaping.get("text"), pageTextOptions)
                        : locator.getByText(chainLocatorMaping.get("text"), locatorTextOptions);
                break;

            case "getByAltText":

                FrameLocator.GetByAltTextOptions pageAltTextOptions = new FrameLocator.GetByAltTextOptions();
                Locator.GetByAltTextOptions locatorAltTextOptions = new Locator.GetByAltTextOptions();

                if (chainLocatorMaping.containsKey("setExact")) {
                    pageAltTextOptions.setExact(true);
                    locatorAltTextOptions.setExact(true);
                }

                locator = (index == 0) ? framelocator.getByAltText(chainLocatorMaping.get("altText"), pageAltTextOptions)
                        : locator.getByAltText(chainLocatorMaping.get("altText"), locatorAltTextOptions);
                break;

            case "getByLabel":

                FrameLocator.GetByLabelOptions pageLableOptions = new FrameLocator.GetByLabelOptions();
                Locator.GetByLabelOptions locatorLableOptions = new Locator.GetByLabelOptions();

                if (chainLocatorMaping.containsKey("setExact")) {
                    pageLableOptions.setExact(true);
                    locatorLableOptions.setExact(true);
                }

                locator = (index == 0) ? framelocator.getByLabel(chainLocatorMaping.get("label"), pageLableOptions)
                        : locator.getByLabel(chainLocatorMaping.get("label"), locatorLableOptions);
                break;

        }

        if (selector.matches("first\\(\\)")) {
            locator = locator.first();
        }
        if (selector.matches("last\\(\\)")) {
            locator = locator.last();
        }
        if (selector.matches("nth\\((\\d+)\\)")) {
            pattern = Pattern.compile("nth\\((\\d+)\\)");
            matcher = pattern.matcher(selector);
            //System.out.println("NTH");
            if (matcher.find()) {
                locator = locator.nth(Integer.parseInt(matcher.group(1)));
            }
        }

        return locator;
    }

    public static void chainLocatorMapping(String selector) {
        if (selector.contains("setExact(true)")) {
            chainLocatorMaping.put("setExact", "true");
        }
        String tag = selector.split("\\(")[0];
        switch (tag) {

            case "getByRole":
                String roleType = selector.split("AriaRole.")[1].split(",")[0];
                if (roleType.contains(")")) {
                    roleType = roleType.replace(")", "");
                }
                chainLocatorMaping.put("roleType", roleType.toUpperCase().trim());
                // System.out.println("****** : Role type : " + chainLocatorMaping.get("roleType"));
                if (selector.contains(".setName(")) {
                    String setName = selector.split("setName\\(\"")[1].split("\"")[0];
                    chainLocatorMaping.put("setName", setName);
                }
                break;

            case "locator":
                String locator = selector.split("locator\\(\"")[1].split("\"\\)")[0].replace("\\", "").trim();
                chainLocatorMaping.put("locator", locator);
                //  System.out.println("locator in chainMapping : " + chainLocatorMaping.get("locator"));

                break;

            case "filter":

                if (selector.contains("setHasText(\"")) {
                    String setHasText = selector.split("setHasText\\(\"")[1].split("\"")[0];
                    chainLocatorMaping.put("setHasText", setHasText);

                }

                break;

            case "getByPlaceholder":

                String placeholder = selector.split("getByPlaceholder\\(\"")[1].split("\"")[0];
                chainLocatorMaping.put("placeholder", placeholder);
                break;

            case "getByLabel":

                String label = selector.split("getByLabel\\(\"")[1].split("\"")[0];
                chainLocatorMaping.put("label", label);

                break;

            case "getByAltText":

                String altText = selector.split("getByAltText\\(\"")[1].split("\"")[0];
                chainLocatorMaping.put("lable", altText);

                break;

            case "getByText":
                String text = selector.split("getByText\\(\"")[1].split("\"")[0];
                chainLocatorMaping.put("text", text);
                break;

            case "frameLocator":
                String frameLocator = selector.split("frameLocator\\(\"")[1].split("\"\\)")[0];
                chainLocatorMaping.put("frameLocator", frameLocator);
                break;

        }

    }

    private FrameLocator switchFrame(String frameData) {

        FrameLocator frameLocator = null;
        try {
            if (frameData != null && !frameData.trim().isEmpty()) {
                if (!frameData.contains(";")) {
                    frameLocator = page.frameLocator(frameData);
                    return frameLocator;
                } else {
                    String[] frameLocatorStrings = frameData.split(";");
                    frameLocator = page.frameLocator(frameLocatorStrings[0]);
                    for (int i = 1; i < frameLocatorStrings.length; i++) {
                        frameLocator = frameLocator.frameLocator(frameLocatorStrings[i]);
                    }
                    return frameLocator;
                }
            }
        } catch (Exception ex) {
            // Error while switching to frame
        }
        return null;
    }

    private String getRuntimeValue(String value) {
        if (findType != null && findType.equals(FindType.GLOBAL_OBJECT)) {
            for (String Key : globalDynamicValue.keySet()) {
                value = value.replace(Key, globalDynamicValue.get(Key));
            }
        }
        if (dynamicValue.containsKey(pageName) && dynamicValue.get(pageName).containsKey(objectName)) {
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

        if (wPage == null && mPage == null) {
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

    public void storeElementDetailsinOR(List<ORAttribute> attributes, String attribute, String value) {
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

    private List<Locator> getElementsInternal(LocatorType locatorType, final List<ORAttribute> attributes, LocatorFactory factory) {
        if (attributes == null || attributes.isEmpty()) return null;
        List<Locator> elements = new ArrayList<Locator>();
        for (ORAttribute attr : attributes) {
            String value = getRuntimeValue(attr.getValue() != null ? attr.getValue() : "");
            if (value.trim().isEmpty()) continue;
            String tag = attr.getName();
            Object options = getOptions(locatorType, tag, value, attr.isExact());
            Locator locator = factory.create(tag, value, options);
            if (locator != null) {
                elements.add(locator);
                break; // Only first valid locator
            }
        }
        return elements.isEmpty() ? null : elements;
    }

    private Object getOptions(LocatorType locatorType, String tag, String value, Boolean exact) {
        switch (tag) {
            case "Text":
                Page.GetByTextOptions textOptions = new Page.GetByTextOptions();
                FrameLocator.GetByTextOptions textFrameLocatorOptions = new FrameLocator.GetByTextOptions();
                
                if (locatorType.equals(LocatorType.LOCATOR)){
                    textOptions.setExact(exact);
                    System.out.println("textOptions : " + textOptions);
                    return textOptions;
                } else if (locatorType.equals(LocatorType.FRAMELOCATOR)){
                    textFrameLocatorOptions.setExact(exact);
                    System.out.println("textFrameLocatorOptions : " + textFrameLocatorOptions);
                    return textFrameLocatorOptions;
                }
                break;
            case "Label":
                Page.GetByLabelOptions labelOptions = new Page.GetByLabelOptions();
                FrameLocator.GetByLabelOptions labelFrameLocatorOptions = new FrameLocator.GetByLabelOptions();

                if (locatorType.equals(LocatorType.LOCATOR)){
                    labelOptions.setExact(exact);
                    System.out.println("labelOptions : " + labelOptions);
                    return labelOptions;
                } else if (locatorType.equals(LocatorType.FRAMELOCATOR)){
                    labelFrameLocatorOptions.setExact(exact);
                    System.out.println("labelFrameLocatorOptions : " + labelFrameLocatorOptions);
                    return labelFrameLocatorOptions;
                }
                break;
            case "Placeholder":
                Page.GetByPlaceholderOptions placeholderOptions = new Page.GetByPlaceholderOptions();
                FrameLocator.GetByPlaceholderOptions placeholderFrameLocatorOptions = new FrameLocator.GetByPlaceholderOptions();

                if (locatorType.equals(LocatorType.LOCATOR)){
                    placeholderOptions.setExact(exact);
                    System.out.println("placeholderOptions : " + placeholderOptions);
                    return placeholderOptions;
                } else if (locatorType.equals(LocatorType.FRAMELOCATOR)){
                    placeholderFrameLocatorOptions.setExact(exact);
                    System.out.println("placeholderFrameLocatorOptions : " + placeholderFrameLocatorOptions);
                    return placeholderFrameLocatorOptions;
                }
                break;
            case "AltText":
                Page.GetByAltTextOptions altTextOptions = new Page.GetByAltTextOptions();
                FrameLocator.GetByAltTextOptions altTextFrameLocatorOptions = new FrameLocator.GetByAltTextOptions();

                if (locatorType.equals(LocatorType.LOCATOR)){
                    altTextOptions.setExact(exact);
                    System.out.println("altTextOptions : " + altTextOptions);
                    return altTextOptions;
                } else if (locatorType.equals(LocatorType.FRAMELOCATOR)){
                    altTextFrameLocatorOptions.setExact(exact);
                    System.out.println("altTextFrameLocatorOptions : " + altTextFrameLocatorOptions);
                    return altTextFrameLocatorOptions;
                }
                break;
            case "Title":
                Page.GetByTitleOptions titleOptions = new Page.GetByTitleOptions();
                FrameLocator.GetByTitleOptions titleFrameLocatorOptions = new FrameLocator.GetByTitleOptions();

                if (locatorType.equals(LocatorType.LOCATOR)){
                    titleOptions.setExact(exact);
                    System.out.println("titleOptions : " + titleOptions);
                    return titleOptions;
                } else if (locatorType.equals(LocatorType.FRAMELOCATOR)){
                    titleFrameLocatorOptions.setExact(exact);
                    System.out.println("titleFrameLocatorOptions : " + titleFrameLocatorOptions);
                    return titleFrameLocatorOptions;
                }
                break;
        }
        return null;
    }

    private Locator createRoleLocator(String value, Page page) {
        if (value.contains(";")) {
            String[] parts = value.split(";");
            String roleType = parts[0].toUpperCase();
            Page.GetByRoleOptions roleOptions = new Page.GetByRoleOptions();
            if (value.toLowerCase().contains(";exact")) {
                roleOptions.setExact(true);
            }
            if (parts.length > 1) {
                roleOptions.setName(parts[1]);
            }
            return page.getByRole(AriaRole.valueOf(roleType), roleOptions);
        } else {
            return page.getByRole(AriaRole.valueOf(value.toUpperCase()));
        }
    }

    private Locator createRoleLocator(String value, FrameLocator framelocator) {
        if (value.contains(";")) {
            String[] parts = value.split(";");
            String roleType = parts[0].toUpperCase();
            FrameLocator.GetByRoleOptions roleOptions = new FrameLocator.GetByRoleOptions();
            if (value.toLowerCase().contains(";exact")) {
                roleOptions.setExact(true);
            }
            if (parts.length > 1) {
                roleOptions.setName(parts[1]);
            }
            return framelocator.getByRole(AriaRole.valueOf(roleType), roleOptions);
        } else {
            return framelocator.getByRole(AriaRole.valueOf(value.toUpperCase()));
        }
    }

    private Locator createChainedLocator(String value, Page page) {
        List<String> selectors = Arrays.asList(value.split(";"));
        Locator locator = null;
        for (int i = 0; i < selectors.size(); i++) {
            locator = chainLocators(selectors.get(i), i, page, locator);
        }
        return locator;
    }

    private Locator createChainedLocator(String value, FrameLocator framelocator) {
        List<String> selectors = Arrays.asList(value.split(";"));
        Locator locator = null;
        for (int i = 0; i < selectors.size(); i++) {
            locator = chainLocators(selectors.get(i), i, framelocator, locator);
        }
        return locator;
    }

    public void addFilter(String locatorKey, String filter) {
        locatorFiltersMap.computeIfAbsent(locatorKey, k -> new ArrayList<>()).add(filter);
    }

    private Locator setFilter(Locator locator) {
        List<String> filters = locatorFiltersMap.get(pageName + objectName);
        if (filters != null) {
            for (String value : filters) {
                Locator.FilterOptions options = new Locator.FilterOptions();
                if (value.startsWith("setHasText: ")) {
                    options.setHasText(value.replace("setHasText: ", ""));
                } else if (value.startsWith("setHasNotText: ")) {
                    options.setHasNotText(value.replace("setHasNotText: ", ""));
                } else if (value.startsWith("setVisible: ")) {
                    options.setVisible(Boolean.parseBoolean(value.replace("setVisible: ", "")));
                }
                locator = locator.filter(options);
                if (value.startsWith("setIndex: ")) {
                   locator = locator.nth(Integer.parseInt(value.replace("setIndex: ", "")));
                }
            }
               if(!Action.contains("setFilter")) {
                   locatorFiltersMap.remove(pageName + objectName); // Clear only for this locator
               }

        }

        return locator;
    }

    // ===== API Interface Implementations (Object type wrappers) =====
    

    /**
     * Sets the Playwright Page instance for this automation object.
     * <p>
     * <b>API-Plugin Contract:</b> Required by {@link AutomationObjectApi}. The argument is provided as Object for type erasure; cast to {@link Page} when calling framework methods.
     * </p>
     * @param page the Playwright Page instance (as Object, must be cast to {@link Page})
     */
    @Override
    public void setPage(Object page) {
        setPage((Page) page);
    }


    /**
     * Sets the Playwright Page instance as the driver for this automation object.
     * <p>
     * <b>API-Plugin Contract:</b> Required by {@link AutomationObjectApi}. The argument is provided as Object for type erasure; cast to {@link Page} when calling framework methods.
     * </p>
     * @param page the Playwright Page instance (as Object, must be cast to {@link Page})
     */
    @Override
    public void setDriver(Object page) {
        setDriver((Page) page);
    }


    /**
     * Finds a single element on the page using the provided keys and condition.
     * <p>
     * <b>API-Plugin Contract:</b> Required by {@link AutomationObjectApi}. The returned Object should be cast to {@link Locator} for Playwright operations.
     * </p>
     * @param page the Playwright Page instance (as Object, must be cast to {@link Page})
     * @param objectKey the object key in the object repository
     * @param pageKey the page key in the object repository
     * @param condition the find condition
     * @return the found element as Object (cast to {@link Locator})
     */
    @Override
    public Object findElement(Object page, String objectKey, String pageKey, FindType condition) {
        return findElement((Page) page, objectKey, pageKey, condition);
    }
    

    /**
     * Finds all elements matching the given keys and returns them as a list of Objects.
     * <p>
     * <b>API-Plugin Contract:</b> Required by {@link AutomationObjectApi}. Each Object in the returned list should be cast to {@link Locator} for Playwright operations.
     * </p>
     * @param objectKey the object key in the object repository
     * @param pageKey the page key in the object repository
     * @return a list of Objects (cast each to {@link Locator}), or null if none found
     */
    @Override
    public List<Object> findElementsList(String objectKey, String pageKey) {
        List<Locator> locators = findElements(objectKey, pageKey);
        return locators != null ? new ArrayList<>(locators) : null;
    }
    

    /**
     * Finds all elements matching the given keys and attribute, returning them as a list of Objects.
     * <p>
     * <b>API-Plugin Contract:</b> Required by {@link AutomationObjectApi}. Each Object in the returned list should be cast to {@link Locator} for Playwright operations.
     * </p>
     * @param objectKey the object key in the object repository
     * @param pageKey the page key in the object repository
     * @param attribute the attribute to match
     * @return a list of Objects (cast each to {@link Locator}), or null if none found
     */
    @Override
    public List<Object> findElementsList(String objectKey, String pageKey, String attribute) {
        List<Locator> locators = findElements(objectKey, pageKey, attribute);
        return locators != null ? new ArrayList<>(locators) : null;
    }
    

    /**
     * Finds all elements matching the given keys and condition, returning them as a list of Objects.
     * <p>
     * <b>API-Plugin Contract:</b> Required by {@link AutomationObjectApi}. Each Object in the returned list should be cast to {@link Locator} for Playwright operations.
     * </p>
     * @param objectKey the object key in the object repository
     * @param pageKey the page key in the object repository
     * @param condition the find condition
     * @return a list of Objects (cast each to {@link Locator}), or null if none found
     */
    @Override
    public List<Object> findElementsList(String objectKey, String pageKey, FindType condition) {
        List<Locator> locators = findElements(objectKey, pageKey, condition);
        return locators != null ? new ArrayList<>(locators) : null;
    }
    

    /**
     * Finds all elements matching the given keys, attribute, and condition, returning them as a list of Objects.
     * <p>
     * <b>API-Plugin Contract:</b> Required by {@link AutomationObjectApi}. Each Object in the returned list should be cast to {@link Locator} for Playwright operations.
     * </p>
     * @param objectKey the object key in the object repository
     * @param pageKey the page key in the object repository
     * @param attribute the attribute to match
     * @param condition the find condition
     * @return a list of Objects (cast each to {@link Locator}), or null if none found
     */
    @Override
    public List<Object> findElementsList(String objectKey, String pageKey, String attribute, FindType condition) {
        List<Locator> locators = findElements(objectKey, pageKey, attribute, condition);
        return locators != null ? new ArrayList<>(locators) : null;
    }


    /**
     * Stores the given attribute value in the object repository for the specified element.
     * <p>
     * <b>API-Plugin Contract:</b> Required by {@link AutomationObjectApi}. The attributes argument is provided as Object for type erasure; cast to {@code List<ORAttribute>} when calling framework methods.
     * </p>
     * @param attributes the attributes list (as Object, must be cast to {@code List<ORAttribute>})
     * @param attribute the attribute name
     * @param value the value to store
     */
    @Override
    public void storeElementDetailsinOR(Object attributes, String attribute, String value) {
        storeElementDetailsinOR((List<ORAttribute>) attributes, attribute, value);
    }


    /**
     * Retrieves the value of the specified attribute from the given attributes list.
     * <p>
     * <b>API-Plugin Contract:</b> Required by {@link AutomationObjectApi}. The attributes argument is provided as Object for type erasure; cast to {@code List<ORAttribute>} when calling framework methods.
     * </p>
     * @param attributes the attributes list (as Object, must be cast to {@code List<ORAttribute>})
     * @param attribute the attribute name
     * @return the value of the attribute, or null if not found
     */
    @Override
    public String getAttributeValue(Object attributes, String attribute) {
        return getAttributeValue((List<ORAttribute>) attributes, attribute);
    }

}
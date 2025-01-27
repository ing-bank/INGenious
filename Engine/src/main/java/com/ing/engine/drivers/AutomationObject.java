package com.ing.engine.drivers;

import com.ing.datalib.or.ObjectRepository;
import com.ing.datalib.or.common.ORAttribute;
import com.ing.datalib.or.common.ObjectGroup;
import com.ing.datalib.or.image.ImageORObject;
import com.ing.datalib.or.mobile.MobileORObject;
import com.ing.datalib.or.mobile.MobileORPage;
import com.ing.datalib.or.web.WebORObject;
import com.ing.datalib.or.web.WebORPage;
import com.ing.engine.constants.SystemDefaults;
import com.ing.engine.core.Control;
import com.ing.engine.core.CommandControl;
import com.ing.engine.reporting.intf.Report;
import com.ing.engine.support.Status;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.FrameLocator;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Page.GetByAltTextOptions;
import com.microsoft.playwright.Page.GetByLabelOptions;
import com.microsoft.playwright.Page.GetByPlaceholderOptions;
import com.microsoft.playwright.Page.GetByRoleOptions;
import com.microsoft.playwright.Page.GetByTextOptions;
import com.microsoft.playwright.Page.GetByTitleOptions;
import com.microsoft.playwright.options.AriaRole;
import java.time.Duration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static org.apache.http.client.methods.RequestBuilder.options;

public class AutomationObject {

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

    public enum FindType {
        GLOBAL_OBJECT, DEFAULT;

        public static FindType fromString(String val) {
            switch (val.toLowerCase()) {
                case "globalobject":
                    return GLOBAL_OBJECT;
                default:
                    return DEFAULT;
            }
        }
    }

    public AutomationObject() {
    }

    public AutomationObject(Page Page) {
        this.page = Page;
    }

    public AutomationObject(BrowserContext BrowserContext) {
        this.browserContext = BrowserContext;
    }

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
        return findElements(objectKey, pageKey, condition);
    }

    public List<Locator> findElements(String objectKey, String pageKey, String Attribute, FindType condition) {
        return findElements(objectKey, pageKey, Attribute, condition);
    }

    private Locator getElementFromList(List<Locator> elements) {

        return elements != null && !elements.isEmpty() ? elements.get(0) : null;
    }

    public ObjectGroup<?> getORObject(String page, String object) {
        ObjectRepository objRep = Control.getCurrentProject().getObjectRepository();
        if (objRep.getWebOR().getPageByName(page) != null) {
            return objRep.getWebOR().getPageByName(page).getObjectGroupByName(object);
        } else if (objRep.getMobileOR().getPageByName(page) != null) {
            return objRep.getMobileOR().getPageByName(page).getObjectGroupByName(object);
        }
        return null;
    }

    public String getObjectProperty(String pageName, String objectName, String propertyName) {
        return getWebObject(pageName, objectName).getAttributeByName(propertyName);
    }

    public ObjectGroup<WebORObject> getWebObjects(String page, String object) {
        ObjectRepository objRep = Control.getCurrentProject().getObjectRepository();
        if (objRep.getWebOR().getPageByName(page) != null) {
            return objRep.getWebOR().getPageByName(page).getObjectGroupByName(object);
        }
        return null;
    }

    public WebORObject getWebObject(String page, String object) {
        ObjectRepository objRep = Control.getCurrentProject().getObjectRepository();
        if (objRep.getWebOR().getPageByName(page) != null) {
            return objRep.getWebOR().getPageByName(page).getObjectGroupByName(object).getObjects().get(0);
        }
        return null;
    }

    public ObjectGroup<MobileORObject> getMobileObjects(String page, String object) {
        ObjectRepository objRep = Control.getCurrentProject().getObjectRepository();
        if (objRep.getMobileOR().getPageByName(page) != null) {
            return objRep.getMobileOR().getPageByName(page).getObjectGroupByName(object);
        }
        return null;
    }

    public MobileORObject getMobileObject(String page, String object) {
        ObjectRepository objRep = Control.getCurrentProject().getObjectRepository();
        if (objRep.getMobileOR().getPageByName(page) != null) {
            return objRep.getMobileOR().getPageByName(page).getObjectGroupByName(object).getObjects().get(0);
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
        try {

            String tag = "";
            String value = "";
            List<Locator> elements = new ArrayList<Locator>();
            Locator e = null;
            //elements = null;
            for (ORAttribute attr : attributes) {
                if (!attr.getValue().trim().isEmpty()) {
                    tag = attr.getName();
                    value = getRuntimeValue(attr.getValue());

                    switch (tag) {
                        case "Text":
                            System.out.println(foundElementBy("Text", value));
                            GetByTextOptions textOptions = new Page.GetByTextOptions();
                            if (value.toLowerCase().contains(";exact")) {
                                textOptions.setExact(true);
                                value = value.replace(";exact", "").trim();
                            }
                            elements.add(this.getPage().getByText(value, textOptions));
                            break;
                        case "Label":
                            System.out.println(foundElementBy("Label", value));
                            GetByLabelOptions labelOptions = new Page.GetByLabelOptions();
                            if (value.toLowerCase().contains(";exact")) {
                                labelOptions.setExact(true);
                                value = value.replace(";exact", "").trim();
                            }
                            elements.add(this.getPage().getByLabel(value, labelOptions));
                            break;
                        case "Placeholder":
                            System.out.println(foundElementBy("Placeholder", value));
                            GetByPlaceholderOptions placeholderOptions = new Page.GetByPlaceholderOptions();
                            if (value.toLowerCase().contains(";exact")) {
                                placeholderOptions.setExact(true);
                                value = value.replace(";exact", "").trim();
                            }
                            elements.add(this.getPage().getByPlaceholder(value, placeholderOptions));
                            break;
                        case "AltText":
                            System.out.println(foundElementBy("AltText", value));
                            GetByAltTextOptions altTextOptions = new Page.GetByAltTextOptions();
                            if (value.toLowerCase().contains(";exact")) {
                                altTextOptions.setExact(true);
                                value = value.replace(";exact", "").trim();
                            }
                            elements.add(this.getPage().getByAltText(value, altTextOptions));
                            break;
                        case "Title":
                            System.out.println(foundElementBy("Title", value));
                            GetByTitleOptions titleOptions = new Page.GetByTitleOptions();
                            if (value.toLowerCase().contains(";exact")) {
                                titleOptions.setExact(true);
                                value = value.replace(";exact", "").trim();
                            }
                            elements.add(this.getPage().getByTitle(value, titleOptions));
                            break;
                        case "TestId":
                            System.out.println(foundElementBy("TestId", value));
                            elements.add(this.getPage().getByTestId(value));
                            break;
                        case "css":
                            System.out.println(foundElementBy("CSS", value));
                            elements.add(this.getPage().locator("css=" + value).first());
                            break;
                        case "xpath":
                            System.out.println(foundElementBy("Xpath", value));
                            elements.add(this.getPage().locator("xpath=" + value));
                            break;
                        case "Role":
                            System.out.println(foundElementBy("Role", value));
                            String roleType;
                            String name;
                            if (value.contains(";")) {
                                roleType = value.split(";")[0].toUpperCase();
                                GetByRoleOptions roleOptions = new Page.GetByRoleOptions();
                                if (value.toLowerCase().contains(";exact")) {
                                    roleOptions.setExact(true);
                                    value = value.replace(";exact", "").trim();
                                }
                                name = value.split(";")[1];
                                roleOptions.setName(name);

                                elements.add(this.getPage().getByRole(AriaRole.valueOf(roleType), roleOptions));
                            } else {
                                elements.add(this.getPage().getByRole(AriaRole.valueOf(value.toUpperCase())));
                            }
                        case "ChainedLocator":

                            List<String> selectors = new ArrayList<>();
                            selectors = Arrays.asList(value.split(";"));
                            Locator locator = null;
                            for (int i = 0; i < selectors.size(); i++) {
                                locator = chainLocators(selectors.get(i), i, this.getPage(), locator);
                            }
                            //System.out.println(foundElementBy("Chained Locators", value));
                            elements.add(locator);
                            break;
                    }

                    return elements;
                }

            }
            return null;
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            return null;
        }
    }

    private List<Locator> getElements(FrameLocator framelocator, final List<ORAttribute> attributes) {
        try {
            String tag = "";
            String value = "";
            List<Locator> elements = new ArrayList<Locator>();
            Locator e = null;
            //elements = null;
            for (ORAttribute attr : attributes) {
                if (!attr.getValue().trim().isEmpty()) {
                    tag = attr.getName();
                    value = getRuntimeValue(attr.getValue());
                    //value = attr.getValue();

                    switch (tag) {
                        case "Text":
                            System.out.println(foundElementBy("Text", value));
                            FrameLocator.GetByTextOptions textOptions = new FrameLocator.GetByTextOptions();
                            if (value.toLowerCase().contains(";exact")) {
                                textOptions.setExact(true);
                                value = value.replace(";exact", "").trim();
                            }
                            elements.add(framelocator.getByText(value, textOptions));
                            break;
                        case "Label":
                            System.out.println(foundElementBy("Label", value));
                            FrameLocator.GetByLabelOptions labelOptions = new FrameLocator.GetByLabelOptions();
                            if (value.toLowerCase().contains(";exact")) {
                                labelOptions.setExact(true);
                                value = value.replace(";exact", "").trim();
                            }
                            elements.add(framelocator.getByLabel(value, labelOptions));
                            break;
                        case "Placeholder":
                            System.out.println(foundElementBy("Placeholder", value));
                            FrameLocator.GetByPlaceholderOptions placeholderOptions = new FrameLocator.GetByPlaceholderOptions();
                            if (value.toLowerCase().contains(";exact")) {
                                placeholderOptions.setExact(true);
                                value = value.replace(";exact", "").trim();
                            }
                            elements.add(framelocator.getByPlaceholder(value, placeholderOptions));
                            break;
                        case "AltText":
                            System.out.println(foundElementBy("AltText", value));
                            FrameLocator.GetByAltTextOptions altTextOptions = new FrameLocator.GetByAltTextOptions();
                            if (value.toLowerCase().contains(";exact")) {
                                altTextOptions.setExact(true);
                                value = value.replace(";exact", "").trim();
                            }
                            elements.add(framelocator.getByAltText(value, altTextOptions));
                            break;
                        case "Title":
                            System.out.println(foundElementBy("Title", value));
                            FrameLocator.GetByTitleOptions titleOptions = new FrameLocator.GetByTitleOptions();
                            if (value.toLowerCase().contains(";exact")) {
                                titleOptions.setExact(true);
                                value = value.replace(";exact", "").trim();
                            }
                            elements.add(framelocator.getByTitle(value, titleOptions));
                            break;
                        case "TestId":
                            System.out.println(foundElementBy("TestId", value));
                            elements.add(framelocator.getByTestId(value));
                            break;
                        case "css":
                            System.out.println(foundElementBy("CSS", value));
                            elements.add(framelocator.locator("css=" + value).first());
                            break;
                        case "xpath":
                            System.out.println(foundElementBy("Xpath", value));
                            elements.add(framelocator.locator("xpath=" + value));
                            break;
                        case "Role":
                            System.out.println(foundElementBy("Role", value));
                            String roleType;
                            String name;
                            if (value.contains(";")) {
                                roleType = value.split(";")[0].toUpperCase();
                                FrameLocator.GetByRoleOptions roleOptions = new FrameLocator.GetByRoleOptions();
                                if (value.toLowerCase().contains(";exact")) {
                                    roleOptions.setExact(true);
                                    value = value.replace(";exact", "").trim();
                                }
                                name = value.split(";")[1];
                                roleOptions.setName(name);

                                elements.add(framelocator.getByRole(AriaRole.valueOf(roleType), roleOptions));
                            } else {
                                elements.add(framelocator.getByRole(AriaRole.valueOf(value.toUpperCase())));
                            }
                        case "ChainedLocator":

                            List<String> selectors = new ArrayList<>();
                            selectors = Arrays.asList(value.split(";"));
                            Locator locator = null;
                            for (int i = 0; i < selectors.size(); i++) {
                                locator = chainLocators(selectors.get(i), i, framelocator, locator);
                            }
                            //System.out.println(foundElementBy("Chained Locators", value));
                            elements.add(locator);
                            break;
                    }

                    return elements;
                }

            }
            return null;
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            return null;
        }
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

        if (selector.matches("first()")) {
            locator = locator.first();
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

        if (selector.matches("first()")) {
            locator = locator.first();
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

    public List<String> getObjectList(String page, String regexObject) {
        if (page == null || page.trim().isEmpty()) {
            throw new RuntimeException("Page Name is empty please give a valid pageName");
        }
        ObjectRepository objRep = Control.getCurrentProject().getObjectRepository();
        WebORPage wPage = null;
        MobileORPage mPage = null;
        if (objRep.getWebOR().getPageByName(page) != null) {
            wPage = objRep.getWebOR().getPageByName(page);
        } else if (objRep.getMobileOR().getPageByName(page) != null) {
            mPage = objRep.getMobileOR().getPageByName(page);
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

}

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
import com.ing.engine.drivers.findObjectBy.support.ByObjectProp;
import com.ing.engine.core.CommandControl;
import io.appium.java_client.android.AndroidDriver;
import java.time.Duration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
//import org.openqa.selenium.support.ui.WebDriverWait;
//import org.jsoup.Jsoup;
//import org.jsoup.nodes.Document;
//import org.jsoup.nodes.Element;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.support.ui.WebDriverWait;

public class MobileObject {

    public MobileObject(CommandControl cc) {
        super();
    }

    WebDriver driver;

    String pageName;
    String objectName;
    FindmType findType;
    private Duration waitTime;

    public static HashMap<String, Map<String, Map<String, String>>> dynamicValue = new HashMap<>();
    public static HashMap<String, String> globalDynamicValue = new HashMap<>();
    public static String Action = "";

    public enum FindmType {
        GLOBAL_OBJECT, DEFAULT;

        public static FindmType fromString(String val) {
            switch (val.toLowerCase()) {
                case "globalobject":
                    return GLOBAL_OBJECT;
                default:
                    return DEFAULT;
            }
        }
    }

    public MobileObject() {
    }

    public MobileObject(WebDriver Driver) {
        this.driver = Driver;
    }

    /**
     *
     * @param objectKey ObjectName in pageKey in OR
     * @param pageKey PageName in OR
     * @return
     */
    public WebElement findElement(String objectKey, String pageKey) {
        WebElement e = findElement(objectKey, pageKey, FindmType.DEFAULT);
        return e;
    }

    /**
     *
     * @param element Driver or WebElement
     * @param objectKey ObjectName in pageKey in OR
     * @param pageKey PageName in OR
     * @return
     */
    public boolean isAutoHealEnabled() {
        return Control.getCurrentProject().getProjectSettings().getExecSettings().getRunSettings().isAutoHealEnabled();
    }

    public WebElement findElement(SearchContext element, String objectKey, String pageKey) {
        return findElement(element, objectKey, pageKey, FindmType.DEFAULT);
    }

    public WebElement findElement(String objectKey, String pageKey, String Attribute) {
        return findElement(objectKey, pageKey, Attribute, FindmType.DEFAULT);
    }

    public WebElement findElement(SearchContext element, String objectKey, String pageKey, String Attribute) {
        return findElement(element, objectKey, pageKey, Attribute, FindmType.DEFAULT);
    }

    public WebElement findElement(String objectKey, String pageKey, FindmType condition) {
        return findElement(driver, objectKey, pageKey, condition);
    }

    public WebElement findElement(SearchContext element, String objectKey, String pageKey, FindmType condition) {
        pageName = pageKey;
        objectName = objectKey;
        findType = condition;
        return getElementFromList(findElements(element, getORObject(pageKey, objectKey), null));
    }

    public WebElement findElement(String objectKey, String pageKey, String Attribute, FindmType condition) {
        return findElement(driver, objectKey, pageKey, Attribute, condition);
    }

    public WebElement findElement(SearchContext element, String objectKey, String pageKey, String Attribute,
            FindmType condition) {
        pageName = pageKey;
        objectName = objectKey;
        findType = condition;
        return getElementFromList(findElements(element, getORObject(pageKey, objectKey), Attribute));
    }

    public List<WebElement> findElements(String objectKey, String pageKey) {
        return findElements(objectKey, pageKey, FindmType.DEFAULT);
    }

    public List<WebElement> findElements(String objectKey, String pageKey, String Attribute) {
        return findElements(objectKey, pageKey, Attribute, FindmType.DEFAULT);
    }

    public List<WebElement> findElements(String objectKey, String pageKey, FindmType condition) {
        return findElements(driver, objectKey, pageKey, condition);
    }

    public List<WebElement> findElements(String objectKey, String pageKey, String Attribute, FindmType condition) {
        return findElements(driver, objectKey, pageKey, Attribute, condition);
    }

    public List<WebElement> findElements(SearchContext element, String objectKey, String pageKey) {
        return findElements(element, objectKey, pageKey, FindmType.DEFAULT);
    }

    public List<WebElement> findElements(SearchContext element, String objectKey, String pageKey, String Attribute) {
        return findElements(element, objectKey, pageKey, Attribute, FindmType.DEFAULT);
    }

    public List<WebElement> findElements(SearchContext element, String objectKey, String pageKey, FindmType condition) {
        pageName = pageKey;
        objectName = objectKey;
        findType = condition;
        return findElements(element, getORObject(pageKey, objectKey), null);
    }

    public List<WebElement> findElements(SearchContext element, String objectKey, String pageKey, String Attribute,
            FindmType condition) {
        pageName = pageKey;
        objectName = objectKey;
        findType = condition;
        return findElements(element, getORObject(pageKey, objectKey), Attribute);
    }

    private WebElement getElementFromList(List<WebElement> elements) {
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
    private synchronized List<WebElement> findElements(SearchContext context, ObjectGroup objectGroup, String prop) {
        if (objectGroup != null && !objectGroup.getObjects().isEmpty()) {
            if (objectGroup.getObjects().get(0) instanceof WebORObject) {
                return getWElements(context, objectGroup, prop);
            } else if (objectGroup.getObjects().get(0) instanceof MobileORObject) {
                return getMElements(context, objectGroup, prop);
            }
        }
        return null;
    }

    private List<WebElement> getWElements(SearchContext context, ObjectGroup<WebORObject> objectGroup, String prop) {
        long startTime = System.nanoTime();
        List<WebElement> elements = null;
        for (WebORObject object : objectGroup.getObjects()) {
            switchFrame(object.getFrame());
            elements = getElements(context, object.getAttributes(), prop);
            if (elements != null && !elements.isEmpty()) {
                break;
            }
        }
        printStats(elements, objectGroup, startTime, System.nanoTime());
        return elements;
    }

    private List<WebElement> getMElements(SearchContext context, ObjectGroup<MobileORObject> objectGroup, String prop) {
        long startTime = System.nanoTime();
        List<WebElement> elements = null;
        for (MobileORObject object : objectGroup.getObjects()) {
            elements = getElements(context, object.getAttributes(), prop);
            if (elements != null && !elements.isEmpty()) {
                break;
            }
        }
        printStats(elements, objectGroup, startTime, System.nanoTime());
        return elements;
    }

    private void printStats(List<?> elements, ObjectGroup<?> objectGroup, long startTime, long stopTime) {
        if (elements != null) {
            System.out.println(foundElementIn(objectGroup, stopTime, startTime));
        } else {
            System.out.println(notFoundIn(objectGroup));
        }
    }

    private static String foundElementBy(String attr, String val) {
        return String.format("Using @%s [%s], ", attr, val);
    }

    private static String foundElementIn(ObjectGroup<?> objectGroup, long stopTime, long startTime) {
        return String.format("Object '%s' Found in %s ms", objectGroup.getName(), (stopTime - startTime) / 1000000);
    }

    private String notFoundIn(ObjectGroup<?> objectGroup) {
        return String.format("Couldn't find Object '%s' in stipulated Time '%s' Seconds", objectGroup.getName(),
                String.valueOf(getWaitTime().toSeconds()));
    }

    private List<WebElement> getElements(final SearchContext context, final List<ORAttribute> attributes,
            final String prop) {
        WebDriverWait wait = new WebDriverWait(driver, getWaitTime());

        try {
            return wait.until((ExpectedCondition<List<WebElement>>) (WebDriver driver) -> {
                String found = "no";
                String elementString = "";
                String tagName = "";
                String tag = "";
                String value = "";
                List<WebElement> elements = new ArrayList<WebElement>();
                elements = null;
                for (ORAttribute attr : attributes) {
                    if (!attr.getValue().trim().isEmpty()) {
                        tag = attr.getName();
                        value = getRuntimeValue(attr.getValue());
                        if (tag.equals("NLP_locator")) {
                            elements = NLP_located_element(attributes, Action, value);
                            if (elements != null) {
                                storeElementDetailsinOR(attributes, "tagName", elements.get(0).getTagName());
                                storeElementDetailsinOR(attributes, "outerHTML",
                                        elements.get(0).getAttribute("outerHTML"));
                            }
                        }
                        if (tag.equals("outerHTML")) {
                            elementString = attr.getValue();
                            continue;
                        }
                        if (tag.equals("tagName")) {
                            tagName = attr.getValue();
                            continue;
                        }
                        //JSPath********'
                        if (tag.equals("JSPath")) {
                            if (!attr.getValue().trim().isEmpty()) {
                                JavascriptExecutor js = (JavascriptExecutor) driver;
                                elements = new ArrayList<WebElement>();
                                elements.add((WebElement) js.executeScript("return " + attr.getValue()));
                            }
                        }

                        if (elements != null) {
                            return elements;
                        } else if (elements == null) {
                            By by = ByObjectProp.get().getBy(tag, value);
                            if (by != null) {

                                try {
                                    elements = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(by));
                                } catch (TimeoutException ex) {
                                    System.out.println("Element could not be identified with [" + tag + "] : [" + value + "]");
                                    System.out.println(ex.getLocalizedMessage().substring(0, ex.getLocalizedMessage().indexOf("Build info")));
                                    found = "no";
                                    continue;
                                }

                                if (elements.size() == 1) {
                                    System.out.print(foundElementBy(tag, value));
                                    found = "yes";
                                    if (!attributes.toString().contains("UiAutomator")) {
                                        storeElementDetailsinOR(attributes, "tagName", elements.get(0).getTagName());
                                        storeElementDetailsinOR(attributes, "outerHTML",
                                                elements.get(0).getAttribute("outerHTML"));
                                    }
                                    return elements;
                                } else if (elements.size() > 1 || elements.size() == 0) {
                                    found = "no";
                                }
                            }
                        }
                    }
                }
                return null;
            });
        } catch (Exception ex) {
            //Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            return null;
        }
    }

    private void switchFrame(String frameData) {
        try {
            if (frameData != null && !frameData.trim().isEmpty()) {
                driver.switchTo().defaultContent();
                if (frameData.trim().matches("[0-9]+")) {
                    driver.switchTo().frame(Integer.parseInt(frameData.trim()));
                } else {
                    WebDriverWait wait = new WebDriverWait(driver, SystemDefaults.waitTime);
                    wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(frameData));
                }

            }
        } catch (Exception ex) {
            // Error while switching to frame
        }
    }

    private String getRuntimeValue(String value) {
        if (findType != null && findType.equals(FindmType.GLOBAL_OBJECT)) {
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

    public void setDriver(AndroidDriver driver) {
        this.driver = (AndroidDriver) driver;

    }

    /**
     * Get Object Details
     *
     * @param page
     * @return
     */
    public Map<String, WebElement> findAllElementsFromPage(String page) {
        return findElementsByRegex("*", page);
    }

    public Map<String, WebElement> findElementsByRegex(String regexObject, String page) {
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
        Map<String, WebElement> elementList = new HashMap<>();
        if (wPage != null) {
            for (ObjectGroup<WebORObject> objectgroup : wPage.getObjectGroups()) {
                if (objectgroup.getName().matches(regexObject)) {
                    WebElement element = getElementFromList(getWElements(driver, objectgroup, null));
                    if (element != null) {
                        elementList.put(objectgroup.getName(), element);
                    }
                }
            }
        } else if (mPage != null) {
            for (ObjectGroup<MobileORObject> objectgroup : mPage.getObjectGroups()) {
                if (objectgroup.getName().matches(regexObject)) {
                    WebElement element = getElementFromList(getMElements(driver, objectgroup, null));
                    if (element != null) {
                        elementList.put(objectgroup.getName(), element);
                    }
                }
            }
        }
        return elementList;
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

    public List<WebElement> NLP_located_element(List<ORAttribute> attributes, String Action, String text) {
        WebDriverWait wait = new WebDriverWait(driver, getWaitTime());
        boolean isSetOperation = Pattern.compile("set", Pattern.CASE_INSENSITIVE + Pattern.LITERAL).matcher(Action)
                .find();
        boolean isSelectOperation = Pattern.compile("select", Pattern.CASE_INSENSITIVE + Pattern.LITERAL)
                .matcher(Action).find();
        boolean isclickOperation = Pattern.compile("click", Pattern.CASE_INSENSITIVE + Pattern.LITERAL).matcher(Action)
                .find();
        boolean isRelativeObject = Pattern.compile(".* for .*").matcher(text).find() || Pattern.compile(".* next to .*").matcher(text).find() || Pattern.compile(".* after .*").matcher(text).find();
        boolean isBeforeObject = Pattern.compile(".* before .*").matcher(text).find();

        try {

            return wait.until((ExpectedCondition<List<WebElement>>) (WebDriver driver) -> {

                if (isSetOperation) {
                    String parentXpath = "//*[contains(text(),'" + text + "')]";
                    String labelxpath = "./following::input[1]";
                    String placeholderXpath = "//*[@placeholder='" + text + "']";
                    if (getAttributeValue(attributes, "tagName").contains("textarea")) {
                        labelxpath = "./following::textarea[1]";
                    }

                    if (isRelativeObject) {

                        String targetElement = "";
                        String parentElement = "";
                        String splitPhrase = "";

                        if (text.contains(" for ")) {
                            splitPhrase = " for ";
                        }
                        if (text.contains(" next to ")) {
                            splitPhrase = " next to ";
                        }
                        if (text.contains(" after ")) {
                            splitPhrase = " after ";
                        }

                        targetElement = text.split(splitPhrase)[0].replace("\"", "");
                        parentElement = text.split(splitPhrase)[1].replace("\"", "");

                        parentXpath = "//*[contains(text(),'" + parentElement + "')]";
                        labelxpath = "./following::input[contains(text(),'" + targetElement + "')][1]";
                        placeholderXpath = "//*[contains(text(),'" + parentElement + "')]/following::*[@placeholder='"
                                + targetElement + "']";
                        if (getAttributeValue(attributes, "tagName").contains("textarea")) {
                            labelxpath = "./following::textarea[contains(text(),'" + targetElement + "')][1]";
                        }
                    }

                    if (isBeforeObject) {

                        String targetElement = text.split(" before ")[0].replace("\"", "");
                        String parentElement = text.split(" before ")[1].replace("\"", "");
                        parentXpath = "//*[contains(text(),'" + parentElement + "')]";
                        labelxpath = "./preceding::input[contains(text(),'" + targetElement + "')][1]";
                        placeholderXpath = "//*[contains(text(),'" + parentElement + "')]/preceding::*[@placeholder='"
                                + targetElement + "']";
                        if (getAttributeValue(attributes, "tagName").contains("textarea")) {
                            labelxpath = "./preceding::textarea[contains(text(),'" + targetElement + "')][1]";
                        }
                    }

                    if (driver.findElements(By.xpath(placeholderXpath)).size() != 0) {
                        for (WebElement element : driver.findElements(By.xpath(placeholderXpath))) {
                            if (element.isDisplayed()) {
                                System.out.print(foundElementBy("xpath", placeholderXpath));
                                return driver.findElements(By.xpath(placeholderXpath));
                            }
                        }
                    }

                    if (driver.findElements(By.xpath(parentXpath)).size() != 0) {
                        for (WebElement element : driver.findElements(By.xpath(parentXpath))) {
                            if (element.isDisplayed()) {
                                System.out.print(foundElementBy("xpath", parentXpath + labelxpath.substring(1)));
                                return element.findElements(By.xpath(labelxpath));
                            }
                        }
                    }

                } else if (isSelectOperation) {
                    String parentXpath = "//*[contains(text(),'" + text + "')]";
                    String labelSelectXpath = "./following::select[1]";
                    if (isRelativeObject) {

                        String targetElement = "";
                        String parentElement = "";
                        String splitPhrase = "";

                        if (text.contains(" for ")) {
                            splitPhrase = " for ";
                        }
                        if (text.contains(" next to ")) {
                            splitPhrase = " next to ";
                        }
                        if (text.contains(" after ")) {
                            splitPhrase = " after ";
                        }

                        targetElement = text.split(splitPhrase)[0].replace("\"", "");
                        parentElement = text.split(splitPhrase)[1].replace("\"", "");

                        parentXpath = "//*[contains(text(),'" + parentElement + "')]";
                        labelSelectXpath = "./following::select[contains(text(),'" + targetElement + "')][1]";
                    }
                    if (isBeforeObject) {

                        String targetElement = text.split(" before ")[0].replace("\"", "");
                        String parentElement = text.split(" before ")[1].replace("\"", "");
                        parentXpath = "//*[contains(text(),'" + parentElement + "')]";
                        labelSelectXpath = "./preceding::select[contains(text(),'" + targetElement + "')][1]";
                    }

                    if (driver.findElements(By.xpath(parentXpath)).size() != 0) {
                        for (WebElement element : driver.findElements(By.xpath(parentXpath))) {
                            if (element.isDisplayed()) {
                                System.out.print(foundElementBy("xpath", parentXpath + labelSelectXpath.substring(1)));
                                return element.findElements(By.xpath(labelSelectXpath));
                            }
                        }
                    }
                } else {

                    String labelClickXpath = "//*[contains(text(),'" + text + "')][1]";
                    String valueClickXpath = "//*[contains(@value,'" + text + "')][1]";
                    if (isRelativeObject) {

                        String targetElement = "";
                        String parentElement = "";
                        String splitPhrase = "";

                        if (text.contains(" for ")) {
                            splitPhrase = " for ";
                        }
                        if (text.contains(" next to ")) {
                            splitPhrase = " next to ";
                        }
                        if (text.contains(" after ")) {
                            splitPhrase = " after ";
                        }

                        targetElement = text.split(splitPhrase)[0].replace("\"", "");
                        parentElement = text.split(splitPhrase)[1].replace("\"", "");

                        labelClickXpath = "//*[contains(text(),'" + parentElement + "')]/following::*[contains(text(),'"
                                + targetElement + "')][1]";
                        valueClickXpath = "//*[contains(@value,'" + parentElement + "')]/following::*[contains(text(),'"
                                + targetElement + "')][1]";
                    }

                    if (isBeforeObject) {

                        String targetElement = text.split(" before ")[0].replace("\"", "");
                        String parentElement = text.split(" before ")[1].replace("\"", "");

                        labelClickXpath = "//*[contains(text(),'" + parentElement + "')]/preceding::*[contains(text(),'"
                                + targetElement + "')][1]";
                        valueClickXpath = "//*[contains(@value,'" + parentElement + "')]/preceding::*[contains(text(),'"
                                + targetElement + "')][1]";
                    }

                    if (driver.findElements(By.xpath(labelClickXpath)).size() != 0) {
                        for (WebElement element : driver.findElements(By.xpath(labelClickXpath))) {
                            if (element.isDisplayed()) {
                                System.out.print(foundElementBy("xpath", labelClickXpath));
                                return driver.findElements(By.xpath(labelClickXpath));
                            }
                        }
                    }

                    if (driver.findElements(By.xpath(valueClickXpath)).size() != 0) {
                        for (WebElement element : driver.findElements(By.xpath(valueClickXpath))) {
                            if (element.isDisplayed()) {
                                System.out.print(foundElementBy("xpath", valueClickXpath));
                                return driver.findElements(By.xpath(valueClickXpath));
                            }
                        }
                    }
                }
                return null;
            });
        } catch (TimeoutException ex) {
            System.out.println("\n------------------------------------------------------------------------------------");
            System.out.println("Element could not be identified with NLP Locator : [" + text + "]" + "\n");
            //Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            return null;
        }

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

package com.ing.ingenious.api.contract.drivers;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * API interface for MobileObject - provides access to mobile and web element finding 
 * using Selenium WebDriver for plugins.
 * 
 * This interface serves as the bridge between the main application and 
 * user plugins. Selenium types (WebDriver, WebElement, SearchContext) are 
 * represented as Object to avoid direct dependency on Selenium versions.
 * 
 * At runtime, with parent-first classloader delegation for Selenium packages,
 * all parties use the same Selenium classes loaded from the main application.
 */
public interface MobileObjectApi {
    
    /**
     * Enum to specify the type of element finding operation
     */
    enum FindmType {
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
    
    // ===== Driver Management =====
    
    /**
     * Set the driver (WebDriver/AndroidDriver object)
     * @param driver WebDriver instance (org.openqa.selenium.WebDriver or io.appium.java_client.android.AndroidDriver)
     */
    void setDriver(Object driver);
    
    // ===== Element Finding Operations =====
    
    /**
     * Find a single element using object and page keys from OR
     * @param objectKey Object name in the Object Repository
     * @param pageKey Page name in the Object Repository
     * @return WebElement instance (org.openqa.selenium.WebElement)
     */
    Object findElement(String objectKey, String pageKey);
    
    /**
     * Find element with SearchContext
     * @param element SearchContext (WebDriver or WebElement)
     * @param objectKey Object name in OR
     * @param pageKey Page name in OR
     * @return WebElement instance (org.openqa.selenium.WebElement)
     */
    Object findElement(Object element, String objectKey, String pageKey);
    
    /**
     * Find element with specific attribute
     * @param objectKey Object name in OR
     * @param pageKey Page name in OR
     * @param attribute Specific attribute to use
     * @return WebElement instance (org.openqa.selenium.WebElement)
     */
    Object findElement(String objectKey, String pageKey, String attribute);
    
    /**
     * Find element with SearchContext and attribute
     * @param element SearchContext (WebDriver or WebElement)
     * @param objectKey Object name in OR
     * @param pageKey Page name in OR
     * @param attribute Specific attribute
     * @return WebElement instance (org.openqa.selenium.WebElement)
     */
    Object findElement(Object element, String objectKey, String pageKey, String attribute);
    
    /**
     * Find element with specific find type condition
     * @param objectKey Object name in OR
     * @param pageKey Page name in OR
     * @param condition FindmType condition
     * @return WebElement instance (org.openqa.selenium.WebElement)
     */
    Object findElement(String objectKey, String pageKey, FindmType condition);
    
    /**
     * Find element with SearchContext and condition
     * @param element SearchContext (WebDriver or WebElement)
     * @param objectKey Object name in OR
     * @param pageKey Page name in OR
     * @param condition FindmType condition
     * @return WebElement instance (org.openqa.selenium.WebElement)
     */
    Object findElement(Object element, String objectKey, String pageKey, FindmType condition);
    
    /**
     * Find element with attribute and condition
     * @param objectKey Object name in OR
     * @param pageKey Page name in OR
     * @param attribute Specific attribute
     * @param condition FindmType condition
     * @return WebElement instance (org.openqa.selenium.WebElement)
     */
    Object findElement(String objectKey, String pageKey, String attribute, FindmType condition);
    
    /**
     * Find element with SearchContext, attribute and condition
     * @param element SearchContext (WebDriver or WebElement)
     * @param objectKey Object name in OR
     * @param pageKey Page name in OR
     * @param attribute Specific attribute
     * @param condition FindmType condition
     * @return WebElement instance (org.openqa.selenium.WebElement)
     */
    Object findElement(Object element, String objectKey, String pageKey, String attribute, FindmType condition);
    
    /**
     * Find multiple elements
     * @param objectKey Object name in OR
     * @param pageKey Page name in OR
     * @return List of WebElement instances (org.openqa.selenium.WebElement)
     */
    List<Object> findElementsList(String objectKey, String pageKey);
    
    /**
     * Find multiple elements with attribute
     * @param objectKey Object name in OR
     * @param pageKey Page name in OR
     * @param attribute Specific attribute
     * @return List of WebElement instances (org.openqa.selenium.WebElement)
     */
    List<Object> findElementsList(String objectKey, String pageKey, String attribute);
    
    /**
     * Find multiple elements with condition
     * @param objectKey Object name in OR
     * @param pageKey Page name in OR
     * @param condition FindmType condition
     * @return List of WebElement instances (org.openqa.selenium.WebElement)
     */
    List<Object> findElementsList(String objectKey, String pageKey, FindmType condition);
    
    /**
     * Find multiple elements with attribute and condition
     * @param objectKey Object name in OR
     * @param pageKey Page name in OR
     * @param attribute Specific attribute
     * @param condition FindmType condition
     * @return List of WebElement instances (org.openqa.selenium.WebElement)
     */
    List<Object> findElementsList(String objectKey, String pageKey, String attribute, FindmType condition);
    
    /**
     * Find multiple elements with SearchContext
     * @param element SearchContext (WebDriver or WebElement)
     * @param objectKey Object name in OR
     * @param pageKey Page name in OR
     * @return List of WebElement instances (org.openqa.selenium.WebElement)
     */
    List<Object> findElementsList(Object element, String objectKey, String pageKey);
    
    /**
     * Find multiple elements with SearchContext and attribute
     * @param element SearchContext (WebDriver or WebElement)
     * @param objectKey Object name in OR
     * @param pageKey Page name in OR
     * @param attribute Specific attribute
     * @return List of WebElement instances (org.openqa.selenium.WebElement)
     */
    List<Object> findElementsList(Object element, String objectKey, String pageKey, String attribute);
    
    /**
     * Find multiple elements with SearchContext and condition
     * @param element SearchContext (WebDriver or WebElement)
     * @param objectKey Object name in OR
     * @param pageKey Page name in OR
     * @param condition FindmType condition
     * @return List of WebElement instances (org.openqa.selenium.WebElement)
     */
    List<Object> findElementsList(Object element, String objectKey, String pageKey, FindmType condition);
    
    /**
     * Find multiple elements with SearchContext, attribute and condition
     * @param element SearchContext (WebDriver or WebElement)
     * @param objectKey Object name in OR
     * @param pageKey Page name in OR
     * @param attribute Specific attribute
     * @param condition FindmType condition
     * @return List of WebElement instances (org.openqa.selenium.WebElement)
     */
    List<Object> findElementsList(Object element, String objectKey, String pageKey, String attribute, FindmType condition);
    
    // ===== Object Repository Access =====
    
    /**
     * Get OR object group
     * @param page Page name in OR
     * @param object Object name in OR
     * @return ObjectGroup<?> from com.ing.datalib.or.common
     */
    Object getORObject(String page, String object);
    
    /**
     * Get object property value from OR
     * @param pageName Page name in OR
     * @param objectName Object name in OR
     * @param propertyName Property name to retrieve
     * @return Property value as String
     */
    String getObjectProperty(String pageName, String objectName, String propertyName);
    
    /**
     * Get web objects group
     * @param page Page name
     * @param object Object name
     * @return ObjectGroup<WebORObject> from com.ing.datalib.or.web
     */
    Object getWebObjects(String page, String object);
    
    /**
     * Get single web object
     * @param page Page name
     * @param object Object name
     * @return WebORObject from com.ing.datalib.or.web
     */
    Object getWebObject(String page, String object);
    
    /**
     * Get mobile objects group
     * @param page Page name
     * @param object Object name
     * @return ObjectGroup<MobileORObject> from com.ing.datalib.or.mobile
     */
    Object getMobileObjects(String page, String object);
    
    /**
     * Get single mobile object
     * @param page Page name
     * @param object Object name
     * @return MobileORObject from com.ing.datalib.or.mobile
     */
    Object getMobileObject(String page, String object);
    
    /**
     * Get list of objects matching regex pattern
     * @param page Page name
     * @param regexObject Regex pattern for object names
     * @return List of matching object names
     */
    List<String> getObjectList(String page, String regexObject);
    
    // ===== Element Collection Operations =====
    
    /**
     * Find all elements from a page
     * @param page Page name in OR
     * @return Map of object names to WebElement instances
     */
    Map<String, Object> findAllElementsFromPageMap(String page);
    
    /**
     * Find elements matching regex pattern
     * @param regexObject Regex pattern for object names
     * @param page Page name in OR
     * @return Map of object names to WebElement instances
     */
    Map<String, Object> findElementsByRegexMap(String regexObject, String page);
    
    // ===== Wait Time Configuration =====
    
    /**
     * Set custom wait time for element operations
     * @param waitTime Duration to wait
     */
    void setWaitTime(Duration waitTime);
    
    /**
     * Reset wait time to default
     */
    void resetWaitTime();
    
    // ===== Auto Heal =====
    
    /**
     * Check if auto heal is enabled
     * @return true if auto heal is enabled
     */
    boolean isAutoHealEnabled();
    
    // ===== OR Attribute Management =====
    
    /**
     * Store/update element details in OR attributes list
     * @param attributes List of ORAttribute (List<com.ing.datalib.or.common.ORAttribute>)
     * @param attribute Attribute name
     * @param value New value
     */
    void storeElementDetailsinOR(Object attributes, String attribute, String value);
    
    /**
     * Get attribute value from attributes list
     * @param attributes List of ORAttribute (List<com.ing.datalib.or.common.ORAttribute>)
     * @param attribute Attribute name to retrieve
     * @return Attribute value
     */
    String getAttributeValue(Object attributes, String attribute);
    
    // ===== NLP Locator =====
    
    /**
     * Locate element using Natural Language Processing
     * @param attributes List of ORAttribute (List<com.ing.datalib.or.common.ORAttribute>)
     * @param action Action type (e.g., "set", "select", "click")
     * @param text NLP text description
     * @return List of WebElement instances (org.openqa.selenium.WebElement)
     */
    List<Object> NLP_located_elementList(Object attributes, String action, String text);
}

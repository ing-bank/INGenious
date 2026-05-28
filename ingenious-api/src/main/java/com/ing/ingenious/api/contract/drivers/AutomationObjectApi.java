package com.ing.ingenious.api.contract.drivers;

import java.time.Duration;
import java.util.List;

/**
 * API interface for AutomationObject - provides access to element finding 
 * and object repository operations for plugins.
 * 
 * This interface serves as the bridge between the main application and 
 * user plugins. Playwright types are represented as Object to avoid
 * direct dependency on Playwright versions.
 * 
 * At runtime, with parent-first classloader delegation for Playwright packages,
 * all parties use the same Playwright classes loaded from the main application.
 */
public interface AutomationObjectApi {
    
    /**
     * Enum to specify the type of element finding operation
     */
    enum FindType {
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
    
    // ===== Page Management =====
    
    /**
     * Get the current Playwright Page
     * @return Page instance (com.microsoft.playwright.Page)
     */
    Object getPage();
    
    /**
     * Set the Playwright Page
     * @param page Page instance (com.microsoft.playwright.Page)
     */
    void setPage(Object page);
    
    /**
     * Set the driver (Page object)
     * @param page Page instance (com.microsoft.playwright.Page)
     */
    void setDriver(Object page);
    
    // ===== Element Finding Operations =====
    
    /**
     * Find a single element using object and page keys from OR
     * @param objectKey Object name in the Object Repository
     * @param pageKey Page name in the Object Repository
     * @return Locator instance (com.microsoft.playwright.Locator)
     */
    Object findElement(String objectKey, String pageKey);
    
    /**
     * Find element with specific attribute
     * @param objectKey Object name in OR
     * @param pageKey Page name in OR
     * @param attribute Specific attribute to use
     * @return Locator instance (com.microsoft.playwright.Locator)
     */
    Object findElement(String objectKey, String pageKey, String attribute);
    
    /**
     * Find element with specific find type condition
     * @param objectKey Object name in OR
     * @param pageKey Page name in OR
     * @param condition FindType condition
     * @return Locator instance (com.microsoft.playwright.Locator)
     */
    Object findElement(String objectKey, String pageKey, FindType condition);
    
    /**
     * Find element on specific page with condition
     * @param page Page instance (com.microsoft.playwright.Page)
     * @param objectKey Object name in OR
     * @param pageKey Page name in OR
     * @param condition FindType condition
     * @return Locator instance (com.microsoft.playwright.Locator)
     */
    Object findElement(Object page, String objectKey, String pageKey, FindType condition);
    
    /**
     * Find element with attribute and condition
     * @param objectKey Object name in OR
     * @param pageKey Page name in OR
     * @param attribute Specific attribute
     * @param condition FindType condition
     * @return Locator instance (com.microsoft.playwright.Locator)
     */
    Object findElement(String objectKey, String pageKey, String attribute, FindType condition);
    
    /**
     * Find multiple elements
     * @param objectKey Object name in OR
     * @param pageKey Page name in OR
     * @return List of Locator instances (com.microsoft.playwright.Locator)
     */
    List<Object> findElementsList(String objectKey, String pageKey);
    
    /**
     * Find multiple elements with attribute
     * @param objectKey Object name in OR
     * @param pageKey Page name in OR
     * @param attribute Specific attribute
     * @return List of Locator instances (com.microsoft.playwright.Locator)
     */
    List<Object> findElementsList(String objectKey, String pageKey, String attribute);
    
    /**
     * Find multiple elements with condition
     * @param objectKey Object name in OR
     * @param pageKey Page name in OR
     * @param condition FindType condition
     * @return List of Locator instances (com.microsoft.playwright.Locator)
     */
    List<Object> findElementsList(String objectKey, String pageKey, FindType condition);
    
    /**
     * Find multiple elements with attribute and condition
     * @param objectKey Object name in OR
     * @param pageKey Page name in OR
     * @param attribute Specific attribute
     * @param condition FindType condition
     * @return List of Locator instances (com.microsoft.playwright.Locator)
     */
    List<Object> findElementsList(String objectKey, String pageKey, String attribute, FindType condition);
    
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
}
package com.ing.ingenious.api.contract;

import com.ing.ingenious.api.contract.reports.TestCaseReportApi;
import com.ing.ingenious.api.contract.data.UserDataAccessApi;
import com.ing.ingenious.api.contract.drivers.AutomationObjectApi;
import com.ing.ingenious.api.contract.drivers.PlaywrightDriverCreationApi;
import java.io.File;
import java.util.Properties;
import java.util.Stack;

//import com.microsoft.playwright.Locator;
//import com.microsoft.playwright.Page;
//import com.microsoft.playwright.Playwright;
//import com.microsoft.playwright.BrowserContext;

/**
 * Main command API interface for INGenious plugin development.
 * Provides access to test data, Playwright objects, reporting, variables, and framework utilities.
 * This interface is injected into plugin entry classes via constructor.
 */
public interface CommandApi {

    /**
     * Gets the user data access API for test data operations.
     * @return the UserDataAccessApi instance for accessing test data
     */
    UserDataAccessApi getUserData();


    //Playwright / Browser related getters
    /**
     * Retrieves the current Playwright Page instance.
     * @return the Page object that needs to be cast as {@code com.microsoft.playwright.Page}
     */
    Object getPage();
    
    /**
     * Retrieves the Playwright instance.
     * @return the Playwright object that needs to be cast as {@code com.microsoft.playwright.Playwright}
     */
    Object getPlaywright();
    
    /**
     * Retrieves the current browser context.
     * @return the BrowserContext object that needs to be cast as {@code com.microsoft.playwright.BrowserContext}
     */
    Object getBrowserContext();
    
    /**
     * Retrieves the current Locator instance.
     * @return the Locator object that needs to be cast as {@code com.microsoft.playwright.Locator}
     */
    Object getLocator();
    
    /**
     * Retrieves the automation object API for interacting with web elements.
     * @return the AutomationObjectApi instance
     */
    AutomationObjectApi getAObject();
    
    /**
     * Retrieves the Playwright driver creation API.
     * @return the PlaywrightDriverCreationApi instance for driver management
     */
    PlaywrightDriverCreationApi getDriver();


   /**
    * Gets the data input parameter from the action annotation.
    * @return the data input string
    */
   String getData();
   
   /**
    * Gets the object name for the current action.
    * @return the object name
    */
   String getObjectName();
   
   /**
    * Gets the description of the current action.
    * @return the action description
    */
   String getDescription();
   
   /**
    * Gets the condition parameter from the action annotation.
    * @return the condition string
    */
   String getCondition();
   
   /**
    * Gets the input parameter from the action annotation.
    * @return the input string
    */
   String getInput();
   
   /**
    * Gets the action name for the current command.
    * @return the action name
    */
   String getAction();
   
   /**
    * Gets the reference parameter from the action annotation.
    * @return the reference string
    */
   String getReference();

   /**
    * Gets the test case report API for logging test results.
    * @return the TestCaseReportApi instance for test reporting
    */
   TestCaseReportApi getReport();

    /**
     * Adds a runtime variable with the specified key and value.
     * @param key the variable key
     * @param val the variable value
     */
    void addVar(String key, String val);
    
    /**
     * Retrieves a runtime variable value by key.
     * @param key the variable key
     * @return the variable value, or null if not found
     */
    String getRuntimeVar(String key);
    
    /**
     * Retrieves a variable value by key, checking both runtime and global variables.
     * @param key the variable key
     * @return the variable value, or null if not found
     */
    String getVar(String key);
    
    /**
     * Adds a global variable with the specified key and value.
     * @param key the variable key
     * @param val the variable value
     */
    void addGlobalVar(String key, String val);
    
    /**
     * Retrieves a user-defined data value by key.
     * @param key the data key
     * @return the user-defined data value
     */
    String getUserDefinedData(String key);
    
    /**
     * Retrieves a datasheet value by key.
     * @param key the datasheet key
     * @return the datasheet value
     */
    String getDatasheet(String key);

    /**
     * Retrieves the Playwright driver control instance.
     * This returns the same instance as {@link #getDriver()}.
     * @return the PlaywrightDriverCreationApi instance for driver control and management
     */
    PlaywrightDriverCreationApi getDriverControl();
    
    /**
     * Checks if the browser driver is alive.
     * @return true if the driver is alive, false otherwise
     */
    Boolean isDriverAlive();
    
    /**
     * Executes a browser action.
     * @return true if the action was successful, false otherwise
     */
    boolean browserAction();

    /**
     * Resolves all runtime variables in the given string.
     * @param str the string containing runtime variable references
     * @return the string with all runtime variables resolved
     */
    String resolveAllRuntimeVars(String str);
    
    /**
     * Gets the endpoint URL for HTTP/API operations.
     * @return the endpoint URL
     */
    String Endpoint();
    
    /**
     * Gets the HTTP response code.
     * @return the response code as a string
     */
    String ResponseCode();
    
    /**
     * Gets the HTTP response message.
     * @return the response message
     */
    String ResponseMessage();
    
    /**
     * Gets the HTTP response body.
     * @return the response body as a string
     */
    String ResponseBody();
    
    /**
     * Gets the HTTP connection object.
     * @return the connection object (needs to be cast to appropriate type)
     */
    Object Connection();
    
    /**
     * Gets the HTTP user agent string.
     * @return the HTTP user agent
     */
    String HttpAgent();

    //Mobile
    // Object getMobileDriverControl();
    //    com.ing.engine.drivers.MobileObject getMObject();
    // Object getCommander();
    //    org.openqa.selenium.WebDriver getMDriver();
//    org.openqa.selenium.WebElement getElement();
//    com.ing.engine.drivers.MobileObject getMObjectField();
}

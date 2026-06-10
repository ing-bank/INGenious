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
public interface CommandPluginApi {

    /**
     * Gets the user data access API for test data operations.
     * @return the UserDataAccessApi instance for accessing test data
     */
    UserDataAccessApi getUserData();

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
     * Resolves all runtime variables in the given string.
     * @param str the string containing runtime variable references
     * @return the string with all runtime variables resolved
     */
    String resolveAllRuntimeVars(String str);
    
    /**
     * The key is typically constructed as: scenario + testCase
     * This key is used to store and retrieve request/response data in the shared maps.
     * 
     * @return the context key for the current test execution
     */
    String getKey();

}

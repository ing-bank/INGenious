package com.ing.ingenious.api.contract.drivers;

import java.io.File;

/**
 * API interface for WebDriver/Mobile driver creation and management.
 * Supports Selenium WebDriver and Appium driver operations.
 * 
 * All Selenium/Appium types (WebDriver, AndroidDriver, IOSDriver) are returned as Object
 * to maintain version independence from the specific driver implementations.
 * 
 * At runtime, with parent-first classloader delegation for Selenium/Appium packages,
 * all parties use the same driver classes loaded from the main application.
 */
public interface MobileDriverControlApi {
    
    /**
     * Get the current WebDriver instance
     * @return WebDriver instance as Object (cast to org.openqa.selenium.WebDriver or io.appium.java_client.AppiumDriver)
     */
    Object getDriver();
    
    /**
     * Launch the driver with specified context
     * Note: RunContext is a framework class containing browser/platform configuration
     * @param context RunContext containing execution configuration
     * @throws Exception if driver initialization fails
     */
    void launchDriver(Object context) throws Exception;
    
    /**
     * Check if current execution is browser-based (Chromium, WebKit, Firefox)
     * @return true if browser execution, false otherwise
     */
    boolean isBrowserExecution();
    
    /**
     * Check if current execution is "No Browser" mode
     * @return true if no browser execution, false otherwise
     */
    boolean isNoBrowserExecution();
    
    /**
     * Check if current execution is mobile (neither browser nor no-browser)
     * @return true if mobile execution, false otherwise
     */
    boolean isMobileExecution();
    
    /**
     * Get the driver name for a browser configuration
     * @param browserName Browser/emulator name from configuration
     * @return Actual driver name to use
     */
    String getDriverName(String browserName);
    
    /**
     * Check if the driver is alive and responding
     * @return true if driver is alive, throws DriverClosedException otherwise
     */
    Boolean isAlive();
    
    /**
     * Get the current browser or device name
     * @return Browser name or device name from capabilities
     */
    String getCurrentBrowser();
    
    /**
     * Get the current browser or platform version
     * @return Version string, or "NA" if not applicable
     */
    String getCurrentBrowserVersion();
    
    /**
     * Get the platform information
     * @return Platform name and version (e.g., "Android 11", "Windows 10")
     */
    String getPlatform();
    
    /**
     * Create a screenshot of the current state
     * @return File object containing the screenshot, or null if screenshot failed
     */
    File createScreenShot();
    
    /**
     * Stop and quit the browser/driver
     */
    void StopBrowser();
    
    /**
     * Check if execution is running on LambdaTest platform
     * @return true if using LambdaTest remote execution, false otherwise
     */
    boolean isLambdaTestExecutionPlatform();
}

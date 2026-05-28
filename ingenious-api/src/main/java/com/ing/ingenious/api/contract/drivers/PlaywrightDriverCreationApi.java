package com.ing.ingenious.api.contract.drivers;

import java.io.File;
import java.io.IOException;

/**
 * API interface for Playwright driver creation and management.
 * All Playwright types (Playwright, Page, BrowserContext) are returned as Object
 * to maintain version independence.
 */
public interface PlaywrightDriverCreationApi {
    
    /**
     * Get the current Page object
     * @return Playwright Page as Object (cast to com.microsoft.playwright.Page in plugin)
     */
    Object getPage();
    
    /**
     * Set the Page object
     * @param page Playwright Page as Object
     */
    void setPage(Object page);
    
    /**
     * Get the current BrowserContext object
     * @return Playwright BrowserContext as Object (cast to com.microsoft.playwright.BrowserContext in plugin)
     */
    Object getBrowserContext();
    
    /**
     * Get the Playwright instance
     * @return Playwright instance as Object (cast to com.microsoft.playwright.Playwright in plugin)
     */
    Object getPlaywright();
    
    /**
     * Get the current browser name
     * @return Browser name (e.g., "chromium", "firefox", "webkit")
     */
    String getCurrentBrowser();
    
    /**
     * Check if the browser/driver is alive
     * @return true if browser is alive, throws DriverClosedException otherwise
     */
    Boolean isAlive();
    
    /**
     * Create a screenshot of the current page
     * @return File object containing the screenshot
     * @throws IOException if screenshot creation fails
     */
    File createScreenShot() throws IOException;
    
    /**
     * Get the browser version
     * @return Browser version string
     */
    String getBrowserVersion();
    
    /**
     * Close the browser
     */
    void closeBrowser();
    
    /**
     * Stop the browser (closes page and context)
     */
    void StopBrowser();
}

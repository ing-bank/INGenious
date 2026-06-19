package com.ing.ingenious.api.contract;

import com.ing.ingenious.api.contract.drivers.AutomationObjectApi;
import com.ing.ingenious.api.contract.drivers.PlaywrightDriverCreationApi;

/**
 * Interface for browser general API contract in INGenious.
 * Provides methods to check browser and element state, and scroll bar presence.
 */
public interface BrowserPluginApi extends CommandPluginApi {

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
     * Retrieves the Playwright driver control instance.
     * This returns the same instance as {@link #getDriver()}.
     * @return the PlaywrightDriverCreationApi instance for driver control and management
     */
    PlaywrightDriverCreationApi getDriverControl();

    //Browser related utility methods

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
     * Checks if the browser driver is alive and responsive.
     * @return true if the driver is alive, false otherwise
     */
    Boolean checkIfDriverIsAlive();

    /**
     * Checks if the target element is present in the DOM.
     * @return true if the element is present, false otherwise
     */
    Boolean elementPresent();

    /**
     * Checks if the target element is selected (e.g., checkbox, radio button).
     * @return true if the element is selected, false otherwise
     */
    Boolean elementSelected();

    /**
     * Checks if the target element is displayed (visible to the user).
     * @return true if the element is displayed, false otherwise
     */
    Boolean elementDisplayed();

    /**
     * Checks if the target element is enabled (interactable).
     * @return true if the element is enabled, false otherwise
     */
    Boolean elementEnabled();

    /**
     * Checks if a horizontal scroll bar is present on the page or element.
     * @return true if a horizontal scroll bar is present, false otherwise
     */
    boolean isHScrollBarPresent();

    /**
     * Checks if a vertical scroll bar is present on the page or element.
     * @return true if a vertical scroll bar is present, false otherwise
     */
    boolean isvScrollBarPresent();

}

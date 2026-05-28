package com.ing.ingenious.api.contract;

import com.ing.ingenious.api.contract.CommandPluginApi;
import com.ing.ingenious.api.contract.drivers.MobileObjectApi;
import com.ing.ingenious.api.contract.drivers.MobileDriverControlApi;

/**
 * Interface for mobile general API contract in INGenious.
 * Provides methods to check mobile driver and element state, scroll bars, and alerts.
 * 
 * This interface extends CommandApi to provide mobile-specific general operations
 * for plugin development with Appium/Selenium WebDriver.
 */
public interface MobilePluginApi extends CommandPluginApi {

    // ===== Mobile / Selenium Related Getters =====
    
    /**
     * Retrieves the mobile object API for interacting with mobile/web elements using Selenium.
     * @return the MobileObjectApi instance for element finding operations
     */
    MobileObjectApi getMObject();
    
    /**
     * Retrieves the mobile driver control API for driver management.
     * @return the MobileDriverControlApi instance for driver control and management
     */
    MobileDriverControlApi getMobileDriverControl();
    
    /**
     * Retrieves the current WebDriver/AppiumDriver instance.
     * @return the WebDriver object that needs to be cast as {@code org.openqa.selenium.WebDriver}, 
     *         {@code io.appium.java_client.android.AndroidDriver}, or {@code io.appium.java_client.ios.IOSDriver}
     */
    Object getMDriver();
    
    /**
     * Retrieves the current WebElement instance.
     * @return the WebElement object that needs to be cast as {@code org.openqa.selenium.WebElement}
     */
    Object getElement();

    // ===== Mobile / Selenium Related Utility Methods =====

    /**
     * Checks if the mobile driver is alive and responsive.
     * @return true if the driver is alive, false otherwise
     * @throws RuntimeException if connection with the driver is lost/driver is closed
     */
    Boolean checkIfDriverIsAlive();

    /**
     * Checks if the target mobile element is present in the DOM.
     * @return true if the element is present, false otherwise
     */
    Boolean elementPresent();

    /**
     * Checks if the target mobile element is selected (e.g., checkbox, radio button, toggle).
     * @return true if the element is selected, false otherwise
     * @throws com.ing.ingenious.api.exception.mobile.ElementException if element is not visible
     */
    Boolean elementSelected();

    /**
     * Checks if the target mobile element is displayed (visible to the user).
     * @return true if the element is displayed, false otherwise
     * @throws com.ing.ingenious.api.exception.mobile.ElementException if element is not found
     */
    Boolean elementDisplayed();

    /**
     * Checks if the target mobile element is enabled (interactable).
     * @return true if the element is enabled, false otherwise
     * @throws com.ing.ingenious.api.exception.mobile.ElementException if element is not visible
     */
    Boolean elementEnabled();

    /**
     * Checks if a horizontal scroll bar is present on the mobile page or element.
     * Uses JavaScript execution to detect scroll width vs client width.
     * @return true if a horizontal scroll bar is present, false otherwise
     */
    boolean isHScrollBarPresent();

    /**
     * Checks if a vertical scroll bar is present on the mobile page or element.
     * Uses JavaScript execution to detect scroll height vs client height.
     * @return true if a vertical scroll bar is present, false otherwise
     */
    boolean isvScrollBarPresent();

    /**
     * Checks if an alert dialog is present in the mobile application.
     * This includes native alerts, popups, and dialogs.
     * @return true if an alert is present, false otherwise
     */
    boolean isAlertPresent();
}

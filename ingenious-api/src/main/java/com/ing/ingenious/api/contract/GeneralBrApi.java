package com.ing.ingenious.api.contract;

/**
 * Interface for browser general API contract in INGenious.
 * Provides methods to check browser and element state, and scroll bar presence.
 */
public interface GeneralBrApi extends CommandApi {

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

package com.ing.engine.commands.browser;

import com.ing.engine.core.CommandControl;
import com.ing.ingenious.api.contract.BrowserPluginApi;
import com.ing.ingenious.api.contract.CommandPluginApi;
import com.ing.ingenious.api.contract.data.UserDataAccessApi;
import com.ing.ingenious.api.contract.drivers.AutomationObjectApi;
import com.ing.ingenious.api.contract.drivers.PlaywrightDriverCreationApi;
import com.ing.ingenious.api.exception.mobile.ElementException;
import com.ing.ingenious.api.exception.mobile.ElementException.ExceptionType;

/**
 *
 *
 */
public class General extends Command implements BrowserPluginApi {

    public General(CommandControl cc) {
        super(cc);
    }

    private static final ThreadLocal<Double> defaultAssertionTimeout = ThreadLocal.withInitial(
        () -> 5000.0
    );

    protected double getDefaultAssertionTimeout() {
        return defaultAssertionTimeout.get();
    }

    protected void setDefaultAssertionTimeout(double timeout) {
        defaultAssertionTimeout.set(timeout);
    }

    public Boolean checkIfDriverIsAlive() {
        if (isDriverAlive()) {
            return true;
        } else {
            throw new RuntimeException(
                "Seems like Connection with the driver is lost/driver is closed"
            );
        }
    }

    public Boolean elementPresent() {
        return checkIfDriverIsAlive() && Locator != null;
    }

    public Boolean elementSelected() {
        if (!elementDisplayed()) {
            throw new ElementException(ExceptionType.Element_Not_Visible, ObjectName);
        }
        return Locator.isChecked();
    }

    public Boolean elementDisplayed() {
        if (!elementPresent()) {
            throw new ElementException(ExceptionType.Element_Not_Found, ObjectName);
        }
        return Locator.isVisible();
    }

    public Boolean elementEnabled() {
        if (!elementDisplayed()) {
            throw new ElementException(ExceptionType.Element_Not_Visible, ObjectName);
        }
        return Locator.isEnabled();
    }

    public boolean isHScrollBarPresent() {
        return (boolean) (
            Page.evaluate(
                "document.documentElement.scrollWidth>document.documentElement.clientWidth;"
            )
        );
    }

    public boolean isvScrollBarPresent() {
        return (boolean) (
            Page.evaluate(
                "document.documentElement.scrollHeight>document.documentElement.clientHeight;"
            )
        );
    }

    /**
     * Implementation of {@link CommandPluginApi#getPage()} for the API-plugin contract.
     * @return the Page object that should be cast to {@link com.microsoft.playwright.Page}
     */
    @Override
    public Object getPage() {
        return Page;
    }

    /**
     * Implementation of {@link CommandPluginApi#getPlaywright()} for the API-plugin contract.
     * @return the Playwright object that should be cast to {@link com.microsoft.playwright.Playwright}
     */
    @Override
    public Object getPlaywright() {
        return Playwright;
    }

    /**
     * Implementation of {@link CommandPluginApi#getBrowserContext()} for the API-plugin contract.
     * @return the BrowserContext object that should be cast to {@link com.microsoft.playwright.BrowserContext}
     */
    @Override
    public Object getBrowserContext() {
        return BrowserContext;
    }

    /**
     * Implementation of {@link CommandPluginApi#getLocator()} for the API-plugin contract.
     * @return the Locator object that should be cast to {@link com.microsoft.playwright.Locator}
     */
    @Override
    public Object getLocator() {
        return Locator;
    }

    /**
     * Implementation of {@link CommandPluginApi#getAObject()} for the API-plugin contract.
     * @return the AutomationObjectApi instance for web element interactions
     */
    @Override
    public AutomationObjectApi getAObject() {
        return AObject;
    }

    /**
     * Implementation of {@link CommandPluginApi#getDriver()} for the API-plugin contract.
     * @return the PlaywrightDriverCreationApi instance for driver management
     */
    @Override
    public PlaywrightDriverCreationApi getDriver() {
        return Driver;
    }
}

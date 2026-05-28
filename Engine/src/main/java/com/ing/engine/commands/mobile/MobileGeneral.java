package com.ing.engine.commands.mobile;

import com.ing.engine.commands.browser.Command;
import com.ing.engine.core.CommandControl;
import com.ing.ingenious.api.exception.mobile.ElementException;
import com.ing.ingenious.api.exception.mobile.ElementException.ExceptionType;
import com.ing.ingenious.api.contract.MobilePluginApi;
import com.ing.ingenious.api.contract.drivers.MobileObjectApi;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoAlertPresentException;

public class MobileGeneral extends Command implements MobilePluginApi {

    public MobileGeneral(CommandControl cc) {
        super(cc);
    }

    /**
     * Implementation of {@link GeneralMobileApi#checkIfDriverIsAlive()} for mobile operations.
     * Checks if the mobile driver is alive and responsive.
     * @return true if the driver is alive, false otherwise
     * @throws RuntimeException if connection with the driver is lost/driver is closed
     */
    @Override
    public Boolean checkIfDriverIsAlive() {
        if (mDriver != null) {
            return getMobileDriverControl().isAlive();
        } else {
            throw new RuntimeException("Seems like connection with the driver is lost/driver is closed");
        }
    }

    /**
     * Implementation of {@link GeneralMobileApi#elementPresent()} for mobile operations.
     * Checks if the target mobile element is present in the DOM.
     * @return true if the element is present, false otherwise
     */
    @Override
    public Boolean elementPresent() {
        return checkIfDriverIsAlive() && Element != null;
    }

    /**
     * Implementation of {@link GeneralMobileApi#elementSelected()} for mobile operations.
     * Checks if the target mobile element is selected (e.g., checkbox, radio button, toggle).
     * @return true if the element is selected, false otherwise
     * @throws ElementException if element is not visible
     */
    @Override
    public Boolean elementSelected() {
        if (!elementDisplayed()) {
            throw new ElementException(ExceptionType.Element_Not_Visible, ObjectName);
        }
        return Element.isSelected();
    }

    /**
     * Implementation of {@link GeneralMobileApi#elementDisplayed()} for mobile operations.
     * Checks if the target mobile element is displayed (visible to the user).
     * @return true if the element is displayed, false otherwise
     * @throws ElementException if element is not found
     */
    @Override
    public Boolean elementDisplayed() {
        if (!elementPresent()) {
            throw new ElementException(ExceptionType.Element_Not_Found, ObjectName);
        }
        return Element.isDisplayed();
    }

    /**
     * Implementation of {@link GeneralMobileApi#elementEnabled()} for mobile operations.
     * Checks if the target mobile element is enabled (interactable).
     * @return true if the element is enabled, false otherwise
     * @throws ElementException if element is not visible
     */
    @Override
    public Boolean elementEnabled() {
        if (!elementDisplayed()) {
            throw new ElementException(ExceptionType.Element_Not_Visible, ObjectName);
        }
        return Element.isEnabled();
    }

    /**
     * Implementation of {@link GeneralMobileApi#isHScrollBarPresent()} for mobile operations.
     * Checks if a horizontal scroll bar is present on the mobile page or element.
     * @return true if a horizontal scroll bar is present, false otherwise
     */
    @Override
    public boolean isHScrollBarPresent() {
        return (boolean) ((JavascriptExecutor) mDriver)
                .executeScript("return document.documentElement.scrollWidth>document.documentElement.clientWidth;");
    }

    /**
     * Implementation of {@link GeneralMobileApi#isvScrollBarPresent()} for mobile operations.
     * Checks if a vertical scroll bar is present on the mobile page or element.
     * @return true if a vertical scroll bar is present, false otherwise
     */
    @Override
    public boolean isvScrollBarPresent() {
        return (boolean) ((JavascriptExecutor) mDriver)
                .executeScript("return document.documentElement.scrollHeight>document.documentElement.clientHeight;");
    }

    /**
     * Implementation of {@link GeneralMobileApi#isAlertPresent()} for mobile operations.
     * Checks if an alert dialog is present in the mobile application.
     * @return true if an alert is present, false otherwise
     */
    @Override
    public boolean isAlertPresent() {
        try {
            mDriver.switchTo().alert();
            return true;
        } catch (NoAlertPresentException e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            return false;
        }
    }
    
    @Override
    public MobileObjectApi getMObject() {
        return mObject;
    }

    @Override
    public Object getMDriver() {
        return mDriver;
    }

    @Override
    public Object getElement() {
        return Element;
    }

    
}

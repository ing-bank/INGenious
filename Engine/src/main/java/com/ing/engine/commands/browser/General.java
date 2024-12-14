
package com.ing.engine.commands.browser;

import com.ing.engine.core.CommandControl;
import com.ing.engine.execution.exception.element.ElementException;
import com.ing.engine.execution.exception.element.ElementException.ExceptionType;

/**
 *
 * 
 */
public class General extends Command {

    public General(CommandControl cc) {
        super(cc);
    }

    public Boolean checkIfDriverIsAlive() {
        if (isDriverAlive()) {
            return true;
        } else {
            throw new RuntimeException("Seems like Connection with the driver is lost/driver is closed");
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
        return (boolean) (Page
                .evaluate("document.documentElement.scrollWidth>document.documentElement.clientWidth;"));
    }

    public boolean isvScrollBarPresent() {
        return (boolean) (Page
                .evaluate("document.documentElement.scrollHeight>document.documentElement.clientHeight;"));
    }

    

}

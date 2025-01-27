package com.ing.engine.commands.mobile;

import com.ing.engine.commands.browser.Command;
import com.ing.engine.core.CommandControl;
import com.ing.engine.execution.exception.element.ElementException;
import com.ing.engine.execution.exception.element.ElementException.ExceptionType;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoAlertPresentException;

public class MobileGeneral extends Command {

    public MobileGeneral(CommandControl cc) {
        super(cc);
    }

    public Boolean checkIfDriverIsAlive() {
        if (mDriver != null) {
            return getMobileDriverControl().isAlive();
        } else {
            throw new RuntimeException("Seems like connection with the driver is lost/driver is closed");
        }
    }

    public Boolean elementPresent() {
        return checkIfDriverIsAlive() && Element != null;
    }

    public Boolean elementSelected() {
        if (!elementDisplayed()) {
            throw new ElementException(ExceptionType.Element_Not_Visible, ObjectName);
        }
        return Element.isSelected();
    }

    public Boolean elementDisplayed() {
        if (!elementPresent()) {
            throw new ElementException(ExceptionType.Element_Not_Found, ObjectName);
        }
        return Element.isDisplayed();
    }

    public Boolean elementEnabled() {
        if (!elementDisplayed()) {
            throw new ElementException(ExceptionType.Element_Not_Visible, ObjectName);
        }
        return Element.isEnabled();
    }

    public boolean isHScrollBarPresent() {
        return (boolean) ((JavascriptExecutor) mDriver)
                .executeScript("return document.documentElement.scrollWidth>document.documentElement.clientWidth;");
    }

    public boolean isvScrollBarPresent() {
        return (boolean) ((JavascriptExecutor) mDriver)
                .executeScript("return document.documentElement.scrollHeight>document.documentElement.clientHeight;");
    }

    public boolean isAlertPresent() {
        try {
            mDriver.switchTo().alert();
            return true;
        } catch (NoAlertPresentException e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            return false;
        }
    }
}

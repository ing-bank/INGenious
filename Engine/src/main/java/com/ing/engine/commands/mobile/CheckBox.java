package com.ing.engine.commands.mobile;

import com.ing.engine.commands.browser.Command;
import com.ing.engine.core.CommandControl;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.ObjectType;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class CheckBox extends Command {

    public CheckBox(CommandControl cc) {
        super(cc);
    }
/*
    @Action(object = ObjectType.APP, desc = "Check the [<Object>] element")
    public void check() {
        if (Element != null) {
            if (Element.isEnabled()) {
                if (!Element.isSelected()) {
                    Element.click();
                }
                if (Element.isSelected()) {
                    Report.updateTestLog("check", "Checkbox '" + Element
                            + "'  has been selected/checked successfully",
                            Status.DONE);
                } else {
                    Report.updateTestLog("check", "Checkbox '" + Element
                            + "' couldn't be selected/checked", Status.FAIL);
                }
            } else {
                Report.updateTestLog("check", "Checkbox '" + Element
                        + "' is not enabled", Status.FAIL);
            }
        } else {
            Report.updateTestLog(Action, "Object [" + ObjectName + "] not found", Status.FAIL);
        }
    }

    @Action(object = ObjectType.APP, desc = "Uncheck the [<Object>] element")

    public void uncheck() {
        if (Element != null) {
            if (Element.isEnabled()) {
                if (Element.isSelected()) {
                    Element.click();
                }
                if (!Element.isSelected()) {
                    Report.updateTestLog("uncheck", "Checkbox '" + Element
                            + "'  has been un-checked successfully",
                            Status.DONE);
                } else {
                    Report.updateTestLog("uncheck", "Checkbox '" + Element
                            + "' couldn't be un-checked", Status.FAIL);
                }
            } else {
                Report.updateTestLog("uncheck", "Checkbox '" + Element
                        + "' is not enabled", Status.FAIL);
            }
        } else {
            Report.updateTestLog(Action, "Object[" + ObjectName + "] not found", Status.FAIL);
        }
    }

    @Action(object = ObjectType.MOBILE, desc = "Check all the check boxes in the context")
    public void checkAllCheckBoxes() {
        try {
            List<WebElement> checkboxes = mDriver.findElements(By.cssSelector("input[type=checkbox]"));
            if (checkboxes.isEmpty()) {
                Report.updateTestLog(Action, "No Checkbox present in the page", Status.WARNING);
            } else {
                for (WebElement checkbox : checkboxes) {
                    if (checkbox.isDisplayed() && !checkbox.isSelected()) {
                        checkbox.click();
                    }
                }
                Report.updateTestLog(Action, "All checkboxes are checked", Status.PASS);
            }
        } catch (Exception ex) {
            Report.updateTestLog(Action, "Error while checking checkboxes - " + ex, Status.FAIL);
            Logger.getLogger(CheckBox.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
*/
}

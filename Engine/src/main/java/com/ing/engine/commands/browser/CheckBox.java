package com.ing.engine.commands.browser;

import com.ing.engine.core.CommandControl;
import com.ing.engine.execution.exception.ActionException;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import com.microsoft.playwright.PlaywrightException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CheckBox extends Command {

    public CheckBox(CommandControl cc) {
        super(cc);
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Check the [<Object>] element")
    public void Check() {
        if (Locator != null) {
            if (Locator.isEnabled()) {
                if (!Locator.isChecked()) {
                    Locator.check();
                }
                if (Locator.isChecked()) {
                    Report.updateTestLog("check", "Checkbox '" + Locator
                            + "'  has been selected/checked successfully",
                            Status.DONE);
                } else {
                    Report.updateTestLog("check", "Checkbox '" + Locator
                            + "' couldn't be selected/checked", Status.FAIL);
                }
            } else {
                Report.updateTestLog("check", "Checkbox '" + Locator
                        + "' is not enabled", Status.FAIL);
            }
        } else {
            Report.updateTestLog(Action, "Object [" + ObjectName + "] not found", Status.FAIL);
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Uncheck the [<Object>] element")
    public void Uncheck() {
        if (Locator != null) {
            if (Locator.isEnabled()) {
                if (Locator.isChecked()) {
                    Locator.uncheck();
                }
                if (!Locator.isChecked()) {
                    Report.updateTestLog("uncheck", "Checkbox '" + Locator
                            + "'  has been un-checked successfully",
                            Status.DONE);
                } else {
                    Report.updateTestLog("uncheck", "Checkbox '" + Locator
                            + "' couldn't be un-checked", Status.FAIL);
                }
            } else {
                Report.updateTestLog("uncheck", "Checkbox '" + Locator
                        + "' is not enabled", Status.FAIL);
            }
        } else {
            Report.updateTestLog(Action, "Object[" + ObjectName + "] not found", Status.FAIL);
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Check/Uncheck the [<Object>] element based on Data", input = InputType.YES)
    public void SetChecked() {
        try {
            Locator.setChecked(Boolean.parseBoolean(Data));
            Report.updateTestLog(Action, "Setting checked status of" + "[" + ObjectName + "] as [" + Data + "]", Status.DONE);
        } catch (PlaywrightException e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog("Could not perfom [" + Action + "] action", "Error: " + e.getMessage(), Status.FAIL);
            throw new ActionException(e);
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Check [<Object>] if visible", input = InputType.YES)
    public void CheckifVisible() {
        try
        {
        Page.waitForLoadState();
        if (Locator.isVisible()) {
            Check();
        } else {
            Report.updateTestLog(Action, "[" + ObjectName + "]" + " is not visible", Status.DONE);
        }
        }
        catch (PlaywrightException e) {
        Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
        Report.updateTestLog(Action, "Unique Element [" + ObjectName + "] not found on Page. Error :" + e.getMessage(), Status.FAIL);
        throw new ActionException(e);
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Check/Uncheck the [<Object>] element based on Data", input = InputType.YES)
    public void SetCheckedifDataExists() {
        if (!Data.isEmpty()) {
            SetChecked();
        } else {
            Report.updateTestLog(Action, "Data not present", Status.DONE);
        }
    }

}

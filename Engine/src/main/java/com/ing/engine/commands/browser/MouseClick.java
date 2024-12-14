package com.ing.engine.commands.browser;

import com.ing.engine.core.CommandControl;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import com.microsoft.playwright.Locator;
import com.ing.engine.execution.exception.ActionException;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.KeyboardModifier;
import com.microsoft.playwright.options.MouseButton;
import java.util.Arrays;

import java.util.logging.Level;
import java.util.logging.Logger;

public class MouseClick extends General {

    public MouseClick(CommandControl cc) {
        super(cc);
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Click the [<Object>] ")
    public void Click() {
        try {
            Locator.click();
            Report.updateTestLog(Action, "Clicking on " + "[" + ObjectName + "]", Status.DONE);
        } catch (PlaywrightException e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog("Could not perfom [" + Action + "] action", "Error: " + e.getMessage(), Status.FAIL);
            throw new ActionException(e);
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Click the [<Object>] if it is displayed")
    public void ClickIfVisible() {
        Page.waitForLoadState();
        if (Locator != null) {
            if (Locator.isVisible()) {
                Click();
            } else {
                Report.updateTestLog(Action, "Element [" + ObjectName + "] not Visible", Status.DONE);
            }
        } else {
            Report.updateTestLog(Action, "Element [" + ObjectName + "] not Exists", Status.DONE);
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Click the [<Object>] if Data Exists", input = InputType.YES)
    public void ClickIfDataExists() {
        Page.waitForLoadState();
        if (!Data.isEmpty()) {
            Click();
        } else {
            Report.updateTestLog(Action, "Data not present", Status.DONE);
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Double Click the [<Object>]")
    public void DoubleClick() {
        try {
            Locator.dblclick();
            Report.updateTestLog(Action, "Double Clicking on " + "[" + ObjectName + "]", Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog("Could not perfom [" + Action + "] action", "Error: " + e.getMessage(), Status.FAIL);
            throw new ActionException(e);
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Right Click the [<Object>]")
    public void RightClick() {
        try {
            Locator.click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            Report.updateTestLog(Action, "Right Clicking on " + "[" + ObjectName + "]", Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog("Could not perfom [" + Action + "] action", "Error: " + e.getMessage(), Status.FAIL);
            throw new ActionException(e);
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Shift Click the [<Object>]")
    public void ShiftClick() {
        try {
            Locator.click(new Locator.ClickOptions().setModifiers(Arrays.asList(KeyboardModifier.SHIFT)));
            Report.updateTestLog(Action, "Shift Clicking on " + "[" + ObjectName + "]", Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog("Could not perfom [" + Action + "] action", "Error: " + e.getMessage(), Status.FAIL);
            throw new ActionException(e);
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Click the [<Object>] ")
    public void MouseHover() {
        try {
            Locator.hover();
            Report.updateTestLog(Action, "Hovering on " + "[" + ObjectName + "]", Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog("Could not perfom [" + Action + "] action", "Error: " + e.getMessage(), Status.FAIL);
            throw new ActionException(e);
        }
    }

    @Action(object = ObjectType.BROWSER, desc = "Press Mouse Up ")
    public void MouseUp() {
        try {
            Page.mouse().up();
            Report.updateTestLog(Action, "Pressed Mouse Up", Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog("Could not perfom [" + Action + "] action", "Error: " + e.getMessage(), Status.FAIL);
            throw new ActionException(e);
        }
    }

    @Action(object = ObjectType.BROWSER, desc = "Press Mouse Down ")
    public void MouseDown() {
        try {
            Page.mouse().down();
            Report.updateTestLog(Action, "Pressed Mouse Down", Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog("Could not perfom [" + Action + "] action", "Element not Found. Error: " + e.getMessage(), Status.FAIL);
            throw new ActionException(e);
        }
    }

}

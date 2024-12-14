package com.ing.engine.commands.browser;

import com.ing.engine.core.CommandControl;
import com.ing.engine.execution.exception.ActionException;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SelectOptions extends General {

    public SelectOptions(CommandControl cc) {
        super(cc);
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Select item in [<Object>] which has text: [<Data>]", input = InputType.YES)
    public void SelectSingleByText() {
        try {
            Locator.selectOption(Data);
            Report.updateTestLog(Action, "Item '" + Data
                    + "' is selected" + " from list [" + ObjectName + "]", Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog("Could not perfom [" + Action + "] action", "Error: " + e.getMessage(), Status.FAIL);
            throw new ActionException(e);
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Select items [<Data>] of [<Object>] by visible Text", input = InputType.YES)
    public void SelectMultipleByText() {
        try {
            String options[] = Data.split("|");
            Locator.selectOption(options);
            Report.updateTestLog(Action, "Items '" + Data
                    + "' are selected" + " from list [" + ObjectName + "]", Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog("Could not perfom [" + Action + "] action", "Error: " + e.getMessage(), Status.FAIL);
            throw new ActionException(e);
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Select item in [<Object>] which has text: [<Data>] if it Data exists", input = InputType.YES)
    public void SelectSingleByTextIfDataExists() {
        Page.waitForLoadState();
        if (!Data.isEmpty()) {
            SelectSingleByText();
        } else {
            Report.updateTestLog(Action, "Data not present", Status.DONE);
        }
    }

     @Action(object = ObjectType.PLAYWRIGHT, desc = "Select item in [<Object>] if visible which has text: [<Data>]", input = InputType.YES)
    public void SelectSingleByTextIfVisible() {
        Page.waitForLoadState();
        if (Locator.isVisible()) {
            SelectSingleByText();
        } else {
            Report.updateTestLog(Action, "[" + ObjectName + "]" + " is not visible", Status.DONE);
        }
    }
}

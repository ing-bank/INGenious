package com.ing.engine.commands.browser;

import com.ing.engine.core.CommandControl;
import com.ing.engine.execution.exception.ActionException;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import com.microsoft.playwright.Locator.WaitForOptions;
import com.microsoft.playwright.options.WaitForSelectorState;

import java.util.logging.Level;
import java.util.logging.Logger;

public class WaitFor extends Command {

    public WaitFor(CommandControl cc) {
        super(cc);
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Wait for [<Object>] to be visible ", condition = InputType.OPTIONAL)
    public void waitForElementToBeVisible() {
        waitForElement("VISIBLE", "Successfully waited for [" + ObjectName + "] to be visible");
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Wait for [<Object>] to be hidden ", condition = InputType.OPTIONAL)
    public void waitForElementToBeHidden() {
        waitForElement("HIDDEN", "Successfully waited for [" + ObjectName + "] to be hidden");
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Wait for [<Object>] to be attached to the DOM ", condition = InputType.OPTIONAL)
    public void waitForElementToBeAttached() {
        waitForElement("ATTACHED", "Successfully waited for [" + ObjectName + "] to be attached to the DOM");
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Wait for [<Object>] to be detached from the DOM ", condition = InputType.OPTIONAL)
    public void waitForElementToBeDetached() {
        waitForElement("DETACHED", "Successfully waited for [" + ObjectName + "] to be detached from the DOM");
    }
    
    @Action(object = ObjectType.BROWSER, desc = "Wait for required load state has been reached", condition = InputType.OPTIONAL)
    public void waitForLoadState() {
        try
        {
            Page.waitForLoadState();
            Report.updateTestLog(Action, "Successfully waited for required load state has been reached", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            Report.updateTestLog(Action, "Wait Action Failed", Status.DEBUG);
            throw new ActionException(ex);
        }
    }
    
    private void waitForElement(String command, String message) {
        try {
            WaitForOptions waitOptions = new WaitForOptions();
            waitOptions.setState(WaitForSelectorState.valueOf(command.toUpperCase()));
            if (Condition != null && Condition.matches("[0-9]+")) {
                System.out.println("\nTimeout set to :[" + Condition + "] milliseconds\n");
                waitOptions.setTimeout(Double.parseDouble(Condition));
            }
            
            Locator.waitFor(waitOptions);
            Report.updateTestLog(Action, message, Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            Report.updateTestLog(Action, "Wait Action Failed", Status.DEBUG);
            throw new ActionException(ex);
        }
    }

}

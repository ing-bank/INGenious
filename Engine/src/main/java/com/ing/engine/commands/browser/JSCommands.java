
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

/**
 *
 * 
 */
public class JSCommands extends General {

    public JSCommands(CommandControl cc) {
        super(cc);
    }

    @Action(object = ObjectType.BROWSER, desc = "To execute the JavaScript commands", input = InputType.YES)
    public void BrowserExecuteEval() {
        try {
            Page.evaluate(Data);
            Report.updateTestLog(Action, "Javascript executed", Status.DONE);

        } catch (Exception ex) {
            Logger.getLogger(JSCommands.class.getName()).log(Level.SEVERE, null, ex);
            Report.updateTestLog(Action, "Javascript execution failed", Status.DEBUG);
            throw new ActionException(ex);

        }
    }
    
    @Action(object = ObjectType.PLAYWRIGHT, desc = "To execute the JavaScript commands", input = InputType.YES)
    public void LocatorExecuteEval() {
        try {
            Locator.evaluate(Data);
            Report.updateTestLog(Action, "Javascript executed", Status.DONE);

        } catch (Exception ex) {
            Logger.getLogger(JSCommands.class.getName()).log(Level.SEVERE, null, ex);
            Report.updateTestLog(Action, "Javascript execution failed", Status.DEBUG);
            throw new ActionException(ex);
        }
    }
    
    @Action(object = ObjectType.BROWSER, desc = "To Store value from the JavaScript command", input = InputType.YES, condition=InputType.YES)
    public void BrowserStoreEval() {
        try {
            String variableName = Condition;
            String value = "";
            if (variableName.matches("%.*%")) {
                value = (String) Page.evaluate(Data);
                addVar(variableName, value);
                Report.updateTestLog(Action, "JS evaluated value stored", Status.DONE);
            } else {
                Report.updateTestLog(Action, "Variable format is not correct", Status.DEBUG);
            }

        } catch (Exception ex) {
            Logger.getLogger(JSCommands.class.getName()).log(Level.SEVERE, null, ex);
            Report.updateTestLog(Action, "Javascript execution failed", Status.DEBUG);
            throw new ActionException(ex);

        }
    }
    
    @Action(object = ObjectType.PLAYWRIGHT, desc = "To Store value from the JavaScript command on a Locator", input = InputType.YES, condition=InputType.YES)
    public void LocatorStoreEval() {
        try {
            String variableName = Condition;
            String value = "";
            if (variableName.matches("%.*%")) {
                value = (String) Locator.evaluate(Data);
                addVar(variableName, value);
                Report.updateTestLog(Action, "JS evaluated value stored", Status.DONE);
            } else {
                Report.updateTestLog(Action, "Variable format is not correct", Status.DEBUG);
            }

        } catch (Exception ex) {
            Logger.getLogger(JSCommands.class.getName()).log(Level.SEVERE, null, ex);
            Report.updateTestLog(Action, "Javascript execution failed", Status.DEBUG);

        }
    }
    
    @Action(object = ObjectType.PLAYWRIGHT, desc = "Click the [<Object>] using JavaScript ")
    public void clickByJS() {
      try {
            Locator.evaluate("element => element.click()");
            Report.updateTestLog(Action, "Clicking on " + "["+ObjectName+"]", Status.DONE);
         } catch(PlaywrightException e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog("Could not perfom ["+Action+"] action", "Error: " + e.getMessage(),Status.FAIL);
            throw new ActionException(e);
        }
    }


    @Action(object = ObjectType.PLAYWRIGHT, desc = "Click the [<Object>] if it is displayed")
    public void clickByJSifVisible() {
        Page.waitForLoadState();
        if (Locator != null) {
            if (Locator.isVisible()) {
                clickByJS();
            } else {
                Report.updateTestLog(Action, "[" + ObjectName + "] is not Visible", Status.DONE);
            }
        } else {
            Report.updateTestLog(Action, "[" + ObjectName + "] does not Exist", Status.DONE);
        }
    }
    
    @Action(object = ObjectType.PLAYWRIGHT, desc = "Enter the value [<Data>] in the Field [<Object>]", input = InputType.YES)
    public void fillByJS() {
       try {
            Locator.clear();
            Locator.evaluate("element => element.value='"+Data+"'");
            Report.updateTestLog(Action, "Entered Text '" + Data + "' on '"
                    + "["+ObjectName+"]" + "'", Status.DONE);
        } catch(Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog("Could not perfom ["+Action+"] action", "Error: " + e.getMessage(),Status.FAIL);
            throw new ActionException(e);
        }
    }

}

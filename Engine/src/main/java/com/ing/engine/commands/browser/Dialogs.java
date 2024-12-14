
package com.ing.engine.commands.browser;

import com.ing.engine.core.CommandControl;
import com.ing.engine.execution.exception.ActionException;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Dialogs extends General {

    public Dialogs(CommandControl cc) {
        super(cc);
    }

    
    @Action(object = ObjectType.BROWSER, desc = "Answer the next alert with [<Data>]", input = InputType.YES)
    public void answerNextAlert() {
        String setAlertText = Data;
        try {
        	Page.onceDialog(dialog -> {
                dialog.accept(setAlertText);
              });
            Report.updateTestLog(Action, "Message '" + setAlertText
                    + "' will be set in the next alert window", Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(Action, e.getMessage(), Status.FAILNS);
            Logger.getLogger(CommonMethods.class.getName()).log(Level.SEVERE, null, e);
            throw new ActionException(e);
        }
    }
    
    @Action(object = ObjectType.BROWSER, desc = "Answer all the alerts with [<Data>]", input = InputType.YES)
    public void answerAllAlerts() {
        String setAlertText = Data;
        try {
        	Page.onDialog(dialog -> {
                dialog.accept(setAlertText);
              });
            Report.updateTestLog(Action, "Message '" + setAlertText
                    + "' will be set in all the alert windows", Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(Action, e.getMessage(), Status.FAILNS);
            Logger.getLogger(CommonMethods.class.getName()).log(Level.SEVERE, null, e);
            throw new ActionException(e);
        }
    }

    @Action(object = ObjectType.BROWSER, desc = "Accept the next alert")
    public void acceptNextAlert() {
        try {
        	Page.onceDialog(dialog -> {
                dialog.accept();
              });
            Report.updateTestLog(Action, "The next alert will be accepted", Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(Action, e.getMessage(), Status.FAILNS);
            Logger.getLogger(CommonMethods.class.getName()).log(Level.SEVERE, null, e);
            throw new ActionException(e);
        }
    }
    
    @Action(object = ObjectType.BROWSER, desc = "Accept all the alerts")
    public void acceptAllAlerts() {
        try {
        	Page.onDialog(dialog -> {
                dialog.accept();
              });
            Report.updateTestLog(Action, "All alerts will be accepted", Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(Action, e.getMessage(), Status.FAILNS);
            Logger.getLogger(CommonMethods.class.getName()).log(Level.SEVERE, null, e);
            throw new ActionException(e);
        }
    }

    @Action(object = ObjectType.BROWSER, desc = "Dismiss all alerts")
    public void dismissAllAlerts() {
        try {
        	Page.onDialog(dialog -> {
                dialog.dismiss();
              });
            Report.updateTestLog(Action, "All alerts will be dismissed",
                    Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(Action, e.getMessage(), Status.FAILNS);
            Logger.getLogger(CommonMethods.class.getName()).log(Level.SEVERE, null, e);
            throw new ActionException(e);
        }
    }
    
    @Action(object = ObjectType.BROWSER, desc = "Dismiss the next alert")
    public void dismissNextAlert() {
        try {
        	Page.onceDialog(dialog -> {
                dialog.dismiss();
              });
            Report.updateTestLog(Action, "Next alert will be dismissed",
                    Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(Action, e.getMessage(), Status.FAILNS);
            Logger.getLogger(CommonMethods.class.getName()).log(Level.SEVERE, null, e);
            throw new ActionException(e);
        }
    }

    @Action(object = ObjectType.BROWSER, desc = "Store next Alert message into the Runtime variable: [<Data>]", input = InputType.YES)
    public void storeAlertMessageinVariable() {
        String strObj = Input;
        try {
        	Page.onceDialog(dialog -> {           
            if (strObj.startsWith("%") && strObj.endsWith("%")) {
                addVar(strObj, dialog.message());
                Report.updateTestLog(Action, "Alert Message " + dialog.message() + " is stored in variable " + strObj, Status.DONE);
            } else {
                Report.updateTestLog(Action, "Invalid variable format", Status.DEBUG);
            }
        });
        } catch (Exception e) {
            Report.updateTestLog(Action, e.getMessage(), Status.FAILNS);
            Logger.getLogger(CommonMethods.class.getName()).log(Level.SEVERE, null, e);
            throw new ActionException(e);
        }
    }
    
    @Action(object = ObjectType.BROWSER, desc = "Store Alert type into the Runtime variable: [<Data>]", input = InputType.YES)
    public void storeAlertTypeinVariable() {
        String strObj = Input;
        try {
        	Page.onceDialog(dialog -> {           
            if (strObj.startsWith("%") && strObj.endsWith("%")) {
                addVar(strObj, dialog.type());
                Report.updateTestLog(Action, "Alert Type " + dialog.type() + " is stored in variable " + strObj, Status.DONE);
            } else {
                Report.updateTestLog(Action, "Invalid variable format", Status.DEBUG);
            }
        });
        } catch (Exception e) {
            Report.updateTestLog(Action, e.getMessage(), Status.FAILNS);
            Logger.getLogger(CommonMethods.class.getName()).log(Level.SEVERE, null, e);
            throw new ActionException(e);
        }
    }
    
    @Action(object = ObjectType.BROWSER, desc = "Store default Alert value into the Runtime variable: [<Data>]", input = InputType.YES)
    public void storeDefaultAlertValueinVariable() {
        String strObj = Input;
        try {
        	Page.onceDialog(dialog -> {           
            if (strObj.startsWith("%") && strObj.endsWith("%")) {
                addVar(strObj, dialog.defaultValue());
                Report.updateTestLog(Action, "Default Alert Value " + dialog.defaultValue() + " is stored in variable " + strObj, Status.DONE);
            } else {
                Report.updateTestLog(Action, "Invalid variable format", Status.DEBUG);
            }
        });
        } catch (Exception e) {
            Report.updateTestLog(Action, e.getMessage(), Status.FAILNS);
            Logger.getLogger(CommonMethods.class.getName()).log(Level.SEVERE, null, e);
            throw new ActionException(e);
        }
    }
}
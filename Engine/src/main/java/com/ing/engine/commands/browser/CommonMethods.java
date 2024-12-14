
package com.ing.engine.commands.browser;

import com.ing.engine.core.CommandControl;
import com.ing.engine.execution.exception.ActionException;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CommonMethods extends General {

    public CommonMethods(CommandControl cc) {
        super(cc);
    }

    @Action(object = ObjectType.BROWSER, desc = "Take a Screen Shot ")
    public void TakePageScreenshot() {
        try {
            Report.updateTestLog(Action, "Screenshot is taken", Status.PASS);
        } catch (Exception e) {
            Report.updateTestLog(Action, e.getMessage(), Status.DEBUG);
            Logger.getLogger(CommonMethods.class.getName()).log(Level.SEVERE, null, e);
        }
    }
    
    @Action(object = ObjectType.PLAYWRIGHT, desc = "Take a Screen Shot of [<Object>]")
    public void TakeElementScreenshot() {
        try {
            Locator.screenshot();
            Report.updateTestLog(Action, "Element Screenshot is taken", Status.PASS);
        } catch (Exception e) {
            Report.updateTestLog(Action, e.getMessage(), Status.DEBUG);
            Logger.getLogger(CommonMethods.class.getName()).log(Level.SEVERE, null, e);
            throw new ActionException(e);
        }
    }
    
    @Action(object = ObjectType.PLAYWRIGHT, desc = "Store Element count in Variable", input = InputType.YES)
    public void StoreElementCount() {
        try {
            String variableName = Data;
            String count = String.valueOf(Locator.count());
            if (variableName.matches("%.*%")) {
                addVar(variableName, count);
                Report.updateTestLog(Action, "Element count ["+count+"] stored in variable ["+variableName+"]", Status.DONE);
            } else {
                Report.updateTestLog(Action, "Variable format is not correct", Status.DEBUG);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error Storing Element count:" + "\n" + ex.getMessage(), Status.DEBUG);
            throw new ActionException(ex);
        }
    }
    
    @Action(object = ObjectType.PLAYWRIGHT, desc = "Highlight the element [<Object>]", input = InputType.OPTIONAL)
    public void Highlight() {        
        try {
            Locator.highlight();
            Report.updateTestLog(Action, "Element ["+ ObjectName +"] Highlighted",Status.PASS);
        } catch(Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog("Could not perfom ["+Action+"] action", "Error: " + e.getMessage(),Status.FAIL);
            throw new ActionException(e);
        }
    }
}

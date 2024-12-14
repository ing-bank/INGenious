
package com.ing.engine.commands.browser;

import com.ing.engine.core.CommandControl;
import com.ing.engine.execution.exception.ActionException;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Focus extends General {

    public Focus(CommandControl cc) {
        super(cc);
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Focus on the [<Object>] ")
    public void Focus() {
      try {
            Locator.focus();
            Report.updateTestLog(Action, "Focussing on " + "["+ObjectName+"]", Status.DONE);
         } catch(Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog("Could not perfom ["+Action+"] action", "Element not Found. Error: " + e.getMessage(),Status.FAIL);
            throw new ActionException(e);
        }
    }
    
    @Action(object = ObjectType.PLAYWRIGHT, desc = "Remove focus from [<Object>] ",input = InputType.YES)
    public void Blur() {
      try {
            Locator.blur();
            Report.updateTestLog(Action, "Removing focus from " + "["+ObjectName+"]", Status.DONE);
         } catch(Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog("Could not perfom ["+Action+"] action", "Element not Found. Error: " + e.getMessage(),Status.FAIL);
            throw new ActionException(e);
        }
    }
}

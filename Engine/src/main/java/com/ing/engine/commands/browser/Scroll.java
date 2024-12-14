
package com.ing.engine.commands.browser;

import com.ing.engine.core.CommandControl;
import com.ing.engine.execution.exception.ActionException;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.ObjectType;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Scroll extends General {

   

    public Scroll(CommandControl cc) {
        super(cc);
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc ="Scroll in to view the [<Object>]")
    public void ScrollIntoViewIfNeeded() {
        try{
            Locator.scrollIntoViewIfNeeded();
            Report.updateTestLog(Action, "Scrolled to view for " + "["+ObjectName+"]", Status.DONE);
         } catch(Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog("Could not perfom ["+Action+"] action", "Error: " + e.getMessage(),Status.FAIL);
            throw new ActionException(e);
        }
    }

    
}

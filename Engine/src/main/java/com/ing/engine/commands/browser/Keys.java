
package com.ing.engine.commands.browser;

import com.ing.engine.core.CommandControl;
import com.ing.engine.execution.exception.ActionException;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Keys extends General {

    public Keys(CommandControl cc) {
        super(cc);
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Press the [<Object>] ",input = InputType.YES)
    public void KeyPressOnElement() {
      try {
            Locator.press(Data);
            Report.updateTestLog(Action, "Pressed key ["+ Data + "] on " + "["+ObjectName+"]", Status.DONE);
         } catch(Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog("Could not perfom ["+Action+"] action", "Error: " + e.getMessage(),Status.FAIL);
            throw new ActionException(e);
        }
    }
    
    @Action(object = ObjectType.BROWSER, desc = "Press the [<Object>] ",input = InputType.YES)
    public void KeyPress() {
      try {
            Page.keyboard().press(Data);
            Report.updateTestLog(Action, "Pressed key ["+ Data + "]", Status.DONE);
         } catch(Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog("Could not perfom ["+Action+"] action", "Error: " + e.getMessage(),Status.FAIL);
            throw new ActionException(e);
        }
    }
    
    @Action(object = ObjectType.BROWSER, desc = "Press Key Up ",input = InputType.YES)
    public void KeyUp() {
      try {
            Page.keyboard().up(Data);
            Report.updateTestLog(Action, "Pressed key ["+ Data + "] Up", Status.DONE);
         } catch(Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog("Could not perfom ["+Action+"] action", "Error: " + e.getMessage(),Status.FAIL);
            throw new ActionException(e);
        }
    }
    
    @Action(object = ObjectType.BROWSER, desc = "Press Key Down ",input = InputType.YES)
    public void KeyDown() {
      try {
            Page.keyboard().down(Data);
            Report.updateTestLog(Action, "Pressed key ["+ Data + "] Down", Status.DONE);
         } catch(Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog("Could not perfom ["+Action+"] action", "Error: " + e.getMessage(),Status.FAIL);
            throw new ActionException(e);
        }
    }
    
    @Action(object = ObjectType.BROWSER, desc = "Insert Text via Keyboard",input = InputType.YES)
    public void KeyInsertText() {
      try {
            Page.keyboard().insertText(Data);
            Report.updateTestLog(Action, "Inserted Text ["+ Data + "]", Status.DONE);
         } catch(Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog("Could not perfom ["+Action+"] action", "Error: " + e.getMessage(),Status.FAIL);
            throw new ActionException(e);
        }
    }
    

}

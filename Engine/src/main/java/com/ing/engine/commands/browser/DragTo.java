package com.ing.engine.commands.browser;

import com.ing.engine.core.CommandControl;
import com.ing.engine.execution.exception.ActionException;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;

import java.util.logging.Level;
import java.util.logging.Logger;

public class DragTo extends General {

    public DragTo(CommandControl cc) {
        super(cc);
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Drag Source Object to Target", input = InputType.YES)
    public void DragElementTo() {
        try {
            com.microsoft.playwright.Locator source = Locator;
            String pageName = Data.split(",")[0];
            String targetObject = Data.split(",")[1];
            com.microsoft.playwright.Locator target = AObject.findElement(targetObject, pageName);
            source.dragTo(target);
            Report.updateTestLog(Action, "[" + ObjectName + "] dragged and dropped to object referred in Page [" + pageName + "] and ObjectName [" + targetObject + "]", Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(Action, e.getMessage(), Status.FAILNS);
            Logger.getLogger(CommonMethods.class.getName()).log(Level.SEVERE, null, e);
            throw new ActionException(e);
        }
    }

}

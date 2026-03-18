package com.ing.engine.commands.browser;

import com.ing.engine.core.CommandControl;
import com.ing.ingenious.api.exception.ActionException;
import com.ing.ingenious.api.status.Status;
import com.ing.ingenious.api.annotation.Action;
import com.ing.ingenious.api.types.InputType;
import com.ing.ingenious.api.types.ObjectType;

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

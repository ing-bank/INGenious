package com.ing.engine.commands.browser;

import com.ing.engine.core.CommandControl;
import com.ing.ingenious.api.annotation.Action;
import com.ing.ingenious.api.exception.ActionException;
import com.ing.ingenious.api.status.Status;
import com.ing.ingenious.api.types.InputType;
import com.ing.ingenious.api.types.ObjectType;
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
            Report.updateTestLog(Action, "Focussing on " + "[" + ObjectName + "]", Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(
                "Could not perfom [" + Action + "] action",
                "Element not Found. Error: " + e.getMessage(),
                Status.FAIL
            );
            throw new ActionException(e);
        }
    }

    @Action(
        object = ObjectType.PLAYWRIGHT,
        desc = "Remove focus from [<Object>] ",
        input = InputType.YES
    )
    public void Blur() {
        try {
            Locator.blur();
            Report.updateTestLog(
                Action,
                "Removing focus from " + "[" + ObjectName + "]",
                Status.DONE
            );
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(
                "Could not perfom [" + Action + "] action",
                "Element not Found. Error: " + e.getMessage(),
                Status.FAIL
            );
            throw new ActionException(e);
        }
    }
}

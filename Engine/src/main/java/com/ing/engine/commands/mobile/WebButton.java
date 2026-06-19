package com.ing.engine.commands.mobile;

import com.ing.engine.core.CommandControl;
import com.ing.ingenious.api.annotation.Action;
import com.ing.ingenious.api.exception.mobile.ElementException;
import com.ing.ingenious.api.status.Status;
import com.ing.ingenious.api.types.ObjectType;

final class WebButton extends MobileGeneral {

    public WebButton(CommandControl cc) {
        super(cc);
    }

    @Action(object = ObjectType.APP, desc = "Object [<Object> is enabled]")
    public void isEnabled() {
        if (elementEnabled()) {
            Report.updateTestLog(Action, "Web Element is enabled", Status.PASS);
        } else {
            throw new ElementException(
                ElementException.ExceptionType.Element_Not_Enabled,
                Condition
            );
        }
    }
}

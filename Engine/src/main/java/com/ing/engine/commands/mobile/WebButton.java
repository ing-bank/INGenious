package com.ing.engine.commands.mobile;

import com.ing.engine.core.CommandControl;
import com.ing.engine.execution.exception.element.ElementException;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.ObjectType;

final class WebButton extends MobileGeneral {

    public WebButton(CommandControl cc) {
        super(cc);
    }

    @Action(object = ObjectType.APP, desc = "Object [<Object> is enabled]")
    public void isEnabled() {
        if (elementEnabled()) {
            Report.updateTestLog(Action, "Web Element is enabled", Status.PASS);
        } else {
            throw new ElementException(ElementException.ExceptionType.Element_Not_Enabled, Condition);
        }
    }

}

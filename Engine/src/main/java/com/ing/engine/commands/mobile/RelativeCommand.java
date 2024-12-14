package com.ing.engine.commands.mobile;

import com.ing.engine.commands.browser.Command;
import com.ing.engine.core.CommandControl;
import com.ing.engine.execution.exception.element.ElementException;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import org.openqa.selenium.WebElement;

public class RelativeCommand extends Command {

    private enum RelativeAction {

        TAP, SET
    };

    public RelativeCommand(CommandControl cc) {
        super(cc);
    }

    private Boolean isConditionValid() {
        return !Condition.matches("((Start|End) (Loop|Param)(:\\s)*)|Global Object|Continue");
    }

    private void doRelative(RelativeAction action) {
        if (isConditionValid()) {
            WebElement parent = mObject.findElement(Condition, Reference);
            if (parent != null) {
                Element = mObject.findElement(parent, ObjectName, Reference);
                if (Element != null) {
                    getCommander().Element = Element;
                    switch (action) {
                        case TAP:
                            new Basic(getCommander()).Tap();
                            break;
                        case SET:
                            new Basic(getCommander()).Set();
                            break;
                    }
                } else {
                    throw new ElementException(ElementException.ExceptionType.Element_Not_Found, ObjectName);
                }
            } else {
                throw new ElementException(ElementException.ExceptionType.Element_Not_Found, Condition);
            }
        } else {
            Report.updateTestLog(Action, "No Relative Element Found in Condition Column", Status.DEBUG);
        }
    }

    @Action(object = ObjectType.APP, desc = "Tap on element based on parent [<Object>]", condition = InputType.YES)
    public void Tap_Relative() {
        doRelative(RelativeAction.TAP);
    }

    @Action(object = ObjectType.APP, desc = "Set [<Data>] on element based on parent [<Object>]", input = InputType.YES, condition = InputType.YES)
    public void set_Relative() {
        doRelative(RelativeAction.SET);
    }

}

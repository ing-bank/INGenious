package com.ing.engine.commands.mobile;

import com.ing.engine.commands.browser.Command;
import com.ing.engine.core.CommandControl;
import com.ing.engine.drivers.MobileObject;
import com.ing.ingenious.api.annotation.Action;
import com.ing.ingenious.api.annotation.Args;
import com.ing.ingenious.api.status.Status;
import com.ing.ingenious.api.types.ArgType;
import com.ing.ingenious.api.types.ConditionKind;
import com.ing.ingenious.api.types.InputType;
import com.ing.ingenious.api.types.ObjectType;

public class DynamicObject extends Command {

    public DynamicObject(CommandControl cc) {
        super(cc);
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Set  all objects property to [<Data>] at runtime.",
        input = InputType.YES,
        condition = InputType.YES
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "@value",
        inputHelp = "input value (e.g. @value)",
        condition = ConditionKind.TEXT,
        conditionExample = "@value",
        conditionHelp = "condition value (e.g. @value)"
    )
    public void setMobileglobalObjectProperty() {
        if (!Data.isEmpty()) {
            if (Condition.isEmpty()) {
                String[] groups = Data.split(",");
                for (String group : groups) {
                    String[] vals = group.split("=", 2);
                    MobileObject.globalDynamicValue.put(vals[0], vals[1]);
                }
            } else {
                MobileObject.globalDynamicValue.put(Condition, Data);
            }
            String text = String.format(
                "Setting Global Object Property for %s with %s",
                Condition,
                Data
            );
            Report.updateTestLog(Action, text, Status.DONE);
        } else {
            Report.updateTestLog(Action, "Input should not be empty", Status.FAILNS);
        }
    }

    @Action(
        object = ObjectType.APP,
        desc = "Set object [<Object>] property  as [<Data>] at runtime",
        input = InputType.YES,
        condition = InputType.YES
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "@value",
        inputHelp = "input value (e.g. @value)",
        condition = ConditionKind.TEXT,
        conditionExample = "@value",
        conditionHelp = "condition value (e.g. @value)"
    )
    public void setMobileObjectProperty() {
        if (!Data.isEmpty()) {
            if (Condition.isEmpty()) {
                String[] groups = Data.split(",");
                for (String group : groups) {
                    String[] vals = group.split("=", 2);
                    setProperty(vals[0], vals[1]);
                }
            } else {
                setProperty(Condition, Data);
            }
            String text = String.format(
                "Setting Object Property for %s with %s for Object [%s - %s]",
                Condition,
                Data,
                Reference,
                ObjectName
            );
            Report.updateTestLog(Action, text, Status.DONE);
        } else {
            Report.updateTestLog(Action, "Input should not be empty", Status.FAILNS);
        }
    }

    private void setProperty(String key, String value) {
        com.ing.engine.core.InlineObjectProperty.putObjectProperty(
            MobileObject.dynamicValue,
            Reference,
            ObjectName,
            key,
            value
        );
    }
}

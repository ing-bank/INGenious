package com.ing.engine.commands.browser;

import com.ing.engine.core.CommandControl;
import com.ing.ingenious.api.annotation.Action;
import com.ing.ingenious.api.annotation.Args;
import com.ing.ingenious.api.exception.ActionException;
import com.ing.ingenious.api.status.Status;
import com.ing.ingenious.api.types.ArgType;
import com.ing.ingenious.api.types.ConditionKind;
import com.ing.ingenious.api.types.InputType;
import com.ing.ingenious.api.types.ObjectType;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SelectOptions extends General {

    public SelectOptions(CommandControl cc) {
        super(cc);
    }

    @Action(
        object = ObjectType.PLAYWRIGHT,
        desc = "Select item in [<Object>] which has text: [<Data>]",
        input = InputType.YES
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "@Option 1",
        inputHelp = "visible text/value (e.g. @Option 1) of a single option",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required"
    )
    public void SelectSingleByText() {
        try {
            Locator.selectOption(Data);
            Report.updateTestLog(
                Action,
                "Item '" + Data + "' is selected" + " from list [" + ObjectName + "]",
                Status.DONE
            );
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(
                "Could not perfom [" + Action + "] action",
                "Error: " + e.getMessage(),
                Status.FAIL
            );
            throw new ActionException(e);
        }
    }

    @Action(
        object = ObjectType.PLAYWRIGHT,
        desc = "Select items [<Data>] of [<Object>] by visible Text",
        input = InputType.YES
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "@Option 1|Option 2",
        inputHelp = "multiple option texts/values (e.g. @Option 1|Option 2) separated by '|'",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required"
    )
    public void SelectMultipleByText() {
        try {
            String options[] = Data.split("\\|");
            Locator.selectOption(options);
            Report.updateTestLog(
                Action,
                "Items '" + Data + "' are selected" + " from list [" + ObjectName + "]",
                Status.DONE
            );
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(
                "Could not perfom [" + Action + "] action",
                "Error: " + e.getMessage(),
                Status.FAIL
            );
            throw new ActionException(e);
        }
    }

    @Action(
        object = ObjectType.PLAYWRIGHT,
        desc = "Select item in [<Object>] which has text: [<Data>] if it Data exists",
        input = InputType.YES
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "@Option 1",
        inputHelp = "single option text/value; action is skipped when input is empty",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required"
    )
    public void SelectSingleByTextIfDataExists() {
        Page.waitForLoadState();
        if (!Data.isEmpty()) {
            SelectSingleByText();
        } else {
            Report.updateTestLog(Action, "Data not present", Status.DONE);
        }
    }

    @Action(
        object = ObjectType.PLAYWRIGHT,
        desc = "Select item in [<Object>] if visible which has text: [<Data>]",
        input = InputType.YES
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "@Option 1",
        inputHelp = "single option text/value; action runs only when locator is visible",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required"
    )
    public void SelectSingleByTextIfVisible() {
        Page.waitForLoadState();
        if (Locator.isVisible()) {
            SelectSingleByText();
        } else {
            Report.updateTestLog(Action, "[" + ObjectName + "]" + " is not visible", Status.DONE);
        }
    }
}

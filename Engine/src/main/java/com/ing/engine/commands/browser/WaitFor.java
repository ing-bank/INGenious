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
import com.microsoft.playwright.Locator.WaitForOptions;
import com.microsoft.playwright.Page.WaitForLoadStateOptions;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WaitFor extends Command {

    public WaitFor(CommandControl cc) {
        super(cc);
    }

    @Action(
        object = ObjectType.PLAYWRIGHT,
        desc = "Wait for [<Object>] to be visible ",
        condition = InputType.OPTIONAL
    )
    @Args(
        inputHelp = "no input required",
        condition = ConditionKind.TEXT,
        conditionExample = "5000",
        conditionHelp = "optional timeout in ms (e.g. 5000)"
    )
    public void waitForElementToBeVisible() {
        waitForElement("VISIBLE", "Successfully waited for [" + ObjectName + "] to be visible");
    }

    @Action(
        object = ObjectType.PLAYWRIGHT,
        desc = "Wait for [<Object>] to be hidden ",
        condition = InputType.OPTIONAL
    )
    @Args(
        inputHelp = "no input required",
        condition = ConditionKind.TEXT,
        conditionExample = "5000",
        conditionHelp = "optional timeout in ms (e.g. 5000)"
    )
    public void waitForElementToBeHidden() {
        waitForElement("HIDDEN", "Successfully waited for [" + ObjectName + "] to be hidden");
    }

    @Action(
        object = ObjectType.PLAYWRIGHT,
        desc = "Wait for [<Object>] to be attached to the DOM ",
        condition = InputType.OPTIONAL
    )
    @Args(
        inputHelp = "no input required",
        condition = ConditionKind.TEXT,
        conditionExample = "5000",
        conditionHelp = "optional timeout in ms (e.g. 5000)"
    )
    public void waitForElementToBeAttached() {
        waitForElement(
            "ATTACHED",
            "Successfully waited for [" + ObjectName + "] to be attached to the DOM"
        );
    }

    @Action(
        object = ObjectType.PLAYWRIGHT,
        desc = "Wait for [<Object>] to be detached from the DOM ",
        condition = InputType.OPTIONAL
    )
    @Args(
        inputHelp = "no input required",
        condition = ConditionKind.TEXT,
        conditionExample = "5000",
        conditionHelp = "optional timeout in ms (e.g. 5000)"
    )
    public void waitForElementToBeDetached() {
        waitForElement(
            "DETACHED",
            "Successfully waited for [" + ObjectName + "] to be detached from the DOM"
        );
    }

    @Action(
        object = ObjectType.BROWSER,
        desc = "Wait for required load state has been reached",
        condition = InputType.OPTIONAL
    )
    @Args(
        input = ArgType.ENUM,
        inputExample = "@load",
        inputHelp = "optional load state: @load, @domcontentloaded, or @networkidle; leave blank for default load",
        condition = ConditionKind.TEXT,
        conditionExample = "5000",
        conditionHelp = "optional timeout in ms (e.g. 5000)"
    )
    public void waitForLoadState() {
        try {
            WaitForLoadStateOptions loadStateOptions = new WaitForLoadStateOptions();
            boolean hasTimeout = Condition != null && Condition.matches("[0-9]+");
            if (hasTimeout) {
                loadStateOptions.setTimeout(Double.parseDouble(Condition));
            }

            if (Data != null && !Data.isBlank()) {
                LoadState loadState = LoadState.valueOf(Data.trim().toUpperCase());
                if (hasTimeout) {
                    Page.waitForLoadState(loadState, loadStateOptions);
                } else {
                    Page.waitForLoadState(loadState);
                }
            } else if (hasTimeout) {
                Page.waitForLoadState(LoadState.LOAD, loadStateOptions);
            } else {
                Page.waitForLoadState();
            }
            Report.updateTestLog(
                Action,
                "Successfully waited for required load state has been reached",
                Status.DONE
            );
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            Report.updateTestLog(Action, "Wait Action Failed", Status.DEBUG);
            throw new ActionException(ex);
        }
    }

    private void waitForElement(String command, String message) {
        try {
            WaitForOptions waitOptions = new WaitForOptions();
            waitOptions.setState(WaitForSelectorState.valueOf(command.toUpperCase()));
            if (Condition != null && Condition.matches("[0-9]+")) {
                System.out.println("\nTimeout set to :[" + Condition + "] milliseconds\n");
                waitOptions.setTimeout(Double.parseDouble(Condition));
            }

            Locator.waitFor(waitOptions);
            Report.updateTestLog(Action, message, Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            Report.updateTestLog(Action, "Wait Action Failed", Status.DEBUG);
            throw new ActionException(ex);
        }
    }
}

package com.ing.engine.commands.mobile;

import com.galenframework.specs.SpecText;
import com.ing.engine.commands.browser.Command;
import com.ing.engine.core.CommandControl;
import com.ing.engine.core.RunContext;
import com.ing.engine.drivers.MobileObject;
import com.ing.engine.drivers.WebDriverCreation;
import com.ing.ingenious.api.annotation.Action;
import com.ing.ingenious.api.annotation.Args;
import com.ing.ingenious.api.exception.ForcedException;
import com.ing.ingenious.api.status.Status;
import com.ing.ingenious.api.types.ArgType;
import com.ing.ingenious.api.types.ConditionKind;
import com.ing.ingenious.api.types.InputType;
import com.ing.ingenious.api.types.ObjectType;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

public class SwitchTo extends Command {

    public SwitchTo(CommandControl cc) {
        super(cc);
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Launch a new device session and switch to it",
        input = InputType.NO,
        condition = InputType.YES
    )
    @Args(
        inputHelp = "no input required (e.g. leave empty)",
        condition = ConditionKind.TEXT,
        conditionHelp = "device alias prefixed with # (e.g. #Pixel9Pro)"
    )
    public void launchAndSwitchToDevice() {
        try {
            if (!Condition.startsWith("#")) {
                Report.updateTestLog(
                    Action,
                    "Device alias must be prefixed with '#' (e.g. #Pixel9Pro)",
                    Status.FAIL
                );
                return;
            }
            String alias = Condition.substring(1);

            // Primary driver is registered in Task.launchWebDriver; fallback check for non-standard setups
            if (!Command.deviceSessions.containsKey("default") && mDriver != null) {
                Command.deviceSessions.put("default", mDriver);
            }

            RunContext ctx = new RunContext();
            ctx.BrowserName = alias;
            ctx.PlatformValue = System.getProperty("os.name");
            ctx.BrowserVersion = "default";
            ctx.Scenario = "DeviceSession";
            ctx.TestCase = alias;
            ctx.Iteration = "Single";

            WebDriverCreation newDriverCreation = new WebDriverCreation();
            newDriverCreation.launchDriver(ctx);
            WebDriver newDriver = newDriverCreation.driver;

            Command.deviceSessions.put(alias, newDriver);
            switchActiveWebDriver(newDriver);

            Report.updateTestLog(
                Action,
                "Successfully launched and switched to device session [" + alias + "]",
                Status.DONE
            );
        } catch (Exception e) {
            Report.updateTestLog(Action, "Something went wrong: " + e.getMessage(), Status.DEBUG);
            throw new com.ing.ingenious.api.exception.ActionException(e);
        }
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Switch to an already-launched device session",
        input = InputType.NO,
        condition = InputType.YES
    )
    @Args(
        inputHelp = "no input required (e.g. leave empty)",
        condition = ConditionKind.TEXT,
        conditionHelp = "device alias prefixed with # (e.g. #Pixel9Pro or #default)"
    )
    public void switchToDevice() {
        try {
            if (!Condition.startsWith("#")) {
                Report.updateTestLog(
                    Action,
                    "Device alias must be prefixed with '#' (e.g. #Pixel9Pro)",
                    Status.FAIL
                );
                return;
            }
            String alias = Condition.substring(1);
            WebDriver target = Command.deviceSessions.get(alias);
            if (target == null) {
                Report.updateTestLog(
                    Action,
                    "No device session found for alias [" +
                    alias +
                    "]. " +
                    "Use launchAndSwitchToDevice first.",
                    Status.FAIL
                );
                return;
            }
            switchActiveWebDriver(target);
            Report.updateTestLog(
                Action,
                "Successfully switched to device session [" + alias + "]",
                Status.DONE
            );
        } catch (Exception e) {
            Report.updateTestLog(Action, "Something went wrong: " + e.getMessage(), Status.DEBUG);
            throw new com.ing.ingenious.api.exception.ActionException(e);
        }
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Close a device session by alias",
        input = InputType.YES,
        condition = InputType.OPTIONAL
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "#Pixel9Pro",
        inputHelp = "device alias prefixed with # (e.g. #Pixel9Pro)",
        condition = ConditionKind.TEXT,
        conditionExample = "#default",
        conditionHelp = "optional alias to switch to after closing (e.g. #default)"
    )
    public void closeDeviceSession() {
        try {
            if (!Data.startsWith("#")) {
                Report.updateTestLog(
                    Action,
                    "Device alias must be prefixed with '#' (e.g. #Pixel9Pro)",
                    Status.FAIL
                );
                return;
            }
            String alias = Data.substring(1);
            WebDriver target = Command.deviceSessions.remove(alias);
            if (target != null) {
                try {
                    target.quit();
                } catch (Exception ignore) {}
            }
            // Optionally switch active driver to another session
            if (Condition != null && !Condition.isEmpty() && Condition.startsWith("#")) {
                String switchAlias = Condition.substring(1);
                WebDriver switchTarget = Command.deviceSessions.get(switchAlias);
                if (switchTarget != null) {
                    switchActiveWebDriver(switchTarget);
                    Report.updateTestLog(
                        Action,
                        "Closed device session [" +
                        alias +
                        "] and switched to [" +
                        switchAlias +
                        "]",
                        Status.DONE
                    );
                    return;
                }
            }
            Report.updateTestLog(
                Action,
                "Successfully closed device session [" + alias + "]",
                Status.DONE
            );
        } catch (Exception e) {
            Report.updateTestLog(Action, "Something went wrong: " + e.getMessage(), Status.DEBUG);
            throw new com.ing.ingenious.api.exception.ActionException(e);
        }
    }
}

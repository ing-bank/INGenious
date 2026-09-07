package com.ing.engine.commands.mobile;

import com.ing.engine.core.CommandControl;
import com.ing.ingenious.api.annotation.Action;
import com.ing.ingenious.api.annotation.Args;
import com.ing.ingenious.api.status.Status;
import com.ing.ingenious.api.types.ArgType;
import com.ing.ingenious.api.types.ConditionKind;
import com.ing.ingenious.api.types.InputType;
import com.ing.ingenious.api.types.ObjectType;
import io.appium.java_client.android.AndroidDriver;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AdbCommands extends MobileGeneral {
    private static final Logger LOG = Logger.getLogger(AdbCommands.class.getName());

    public AdbCommands(CommandControl cc) {
        super(cc);
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Execute adb shell <Input> command and store output in [Condition] variable",
        input = InputType.YES,
        condition = InputType.OPTIONAL
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "@value",
        inputHelp = "input value (e.g. @value)",
        condition = ConditionKind.TEXT,
        conditionExample = "@value",
        conditionHelp = "condition value (e.g. @value)"
    )
    public void executeAdbShellCommand() {
        if (!isAndroid()) return;
        try {
            String output = runShell(Data);
            if (!Condition.isEmpty()) {
                addVar(Condition, output);
            }
            Report.updateTestLog(Action, "adb shell output: " + output, Status.DONE);
        } catch (Exception e) {
            LOG.log(Level.OFF, null, e);
            Report.updateTestLog(Action, "adb shell failed: " + e.getMessage(), Status.FAIL);
        }
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Execute adb shell <Input> command and assert output contains [Condition] text",
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
    public void assertAdbShellOutput() {
        if (!isAndroid()) return;
        try {
            String output = runShell(Data);
            if (output.contains(Condition)) {
                Report.updateTestLog(
                    Action,
                    "Output contained '" + Condition + "': " + output,
                    Status.PASS
                );
            } else {
                Report.updateTestLog(
                    Action,
                    "Expected output to contain '" + Condition + "' but got: " + output,
                    Status.FAIL
                );
            }
        } catch (Exception e) {
            LOG.log(Level.OFF, null, e);
            Report.updateTestLog(Action, "adb shell failed: " + e.getMessage(), Status.FAIL);
        }
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Clear data for app with package name <Input>",
        input = InputType.YES,
        condition = InputType.NO
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "@value",
        inputHelp = "input value (e.g. @value)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void clearAppData() {
        if (!isAndroid()) return;
        try {
            String output = runShell("pm clear " + Data);
            Report.updateTestLog(Action, "pm clear " + Data + ": " + output, Status.DONE);
        } catch (Exception e) {
            LOG.log(Level.OFF, null, e);
            Report.updateTestLog(Action, "clearAppData failed: " + e.getMessage(), Status.FAIL);
        }
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Force-stop app with package name <Input>",
        input = InputType.YES,
        condition = InputType.NO
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "@value",
        inputHelp = "input value (e.g. @value)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void forceStopApp() {
        if (!isAndroid()) return;
        try {
            runShell("am force-stop " + Data);
            Report.updateTestLog(Action, "Force-stopped " + Data, Status.DONE);
        } catch (Exception e) {
            LOG.log(Level.OFF, null, e);
            Report.updateTestLog(Action, "forceStopApp failed: " + e.getMessage(), Status.FAIL);
        }
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Get Android system property <Input> and store in [Condition] variable",
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
    public void getDeviceProperty() {
        if (!isAndroid()) return;
        try {
            String value = runShell("getprop " + Data);
            addVar(Condition, value);
            Report.updateTestLog(Action, Data + " = " + value, Status.DONE);
        } catch (Exception e) {
            LOG.log(Level.OFF, null, e);
            Report.updateTestLog(
                Action,
                "getDeviceProperty failed: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Push local file <Input> to device path [Condition]",
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
    public void pushFileToDevice() {
        if (!isAndroid()) return;
        try {
            byte[] fileBytes = Files.readAllBytes(Path.of(Data));
            ((AndroidDriver) mDriver).pushFile(Condition, fileBytes);
            Report.updateTestLog(Action, "Pushed " + Data + " to " + Condition, Status.DONE);
        } catch (IOException e) {
            LOG.log(Level.OFF, null, e);
            Report.updateTestLog(Action, "pushFileToDevice failed: " + e.getMessage(), Status.FAIL);
        } catch (Exception e) {
            LOG.log(Level.OFF, null, e);
            Report.updateTestLog(Action, "pushFileToDevice failed: " + e.getMessage(), Status.FAIL);
        }
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Pull file from device path <Input> to local path [Condition]",
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
    public void pullFileFromDevice() {
        if (!isAndroid()) return;
        try {
            byte[] fileBytes = ((AndroidDriver) mDriver).pullFile(Data);
            Files.write(Path.of(Condition), fileBytes);
            Report.updateTestLog(Action, "Pulled " + Data + " to " + Condition, Status.DONE);
        } catch (IOException e) {
            LOG.log(Level.OFF, null, e);
            Report.updateTestLog(
                Action,
                "pullFileFromDevice failed: " + e.getMessage(),
                Status.FAIL
            );
        } catch (Exception e) {
            LOG.log(Level.OFF, null, e);
            Report.updateTestLog(
                Action,
                "pullFileFromDevice failed: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    // --- helpers ---

    private boolean isAndroid() {
        if (mDriver instanceof AndroidDriver) {
            return true;
        }
        Report.updateTestLog(
            Action,
            "ADB commands are Android-only — skipping on this platform",
            Status.DEBUG
        );
        return false;
    }

    private String runShell(String command) {
        String[] parts = command.trim().split("\\s+", 2);
        List<String> args = parts.length > 1
            ? Arrays.asList(parts[1].split("\\s+"))
            : Collections.emptyList();
        Object result =
            ((AndroidDriver) mDriver).executeScript(
                    "mobile: shell",
                    Map.of("command", parts[0], "args", args)
                );
        return result != null ? result.toString().trim() : "";
    }
}

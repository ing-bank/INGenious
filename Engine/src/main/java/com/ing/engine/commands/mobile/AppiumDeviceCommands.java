package com.ing.engine.commands.mobile;

import com.ing.engine.core.CommandControl;
import com.ing.ingenious.api.annotation.Action;
import com.ing.ingenious.api.annotation.Args;
import com.ing.ingenious.api.status.Status;
import com.ing.ingenious.api.types.ArgType;
import com.ing.ingenious.api.types.ConditionKind;
import com.ing.ingenious.api.types.InputType;
import com.ing.ingenious.api.types.ObjectType;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.remote.SupportsContextSwitching;
import io.appium.java_client.remote.SupportsRotation;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Pause;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.remote.RemoteWebDriver;

public class AppiumDeviceCommands extends MobileGeneral {

    public AppiumDeviceCommands(CommandControl cc) {
        super(cc);
        // TODO Auto-generated constructor stub
    }

    @Action(
        object = ObjectType.APP,
        desc = "Swipe Element to <Input> position",
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
    public void swipeElement() {
        try {
            int duration = 2000;
            Dimension size = mDriver.manage().window().getSize();
            Rectangle rectangle = Element.getRect();
            Point point = new Point(
                rectangle.x + (rectangle.width / 2),
                rectangle.y + (rectangle.height / 2)
            );
            int startX = point.x;
            int startY = point.y;
            int endX = 0;
            int endY = 0;

            switch (Data) {
                case "Left":
                    endY = startY;
                    break;
                case "Right":
                    endX = (int) (0.9 * size.getWidth());
                    endY = startY;
                    break;
                case "Up":
                    endX = startX;
                    break;
                case "Down":
                    endX = startX;
                    endY = size.getHeight();
            }
            if (!Condition.equals("")) {
                duration = Integer.parseInt(Condition);
            }
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence seq = new Sequence(finger, 1);
            seq.addAction(
                finger.createPointerMove(
                    Duration.ofMillis(0),
                    PointerInput.Origin.viewport(),
                    startX,
                    startY
                )
            );
            seq.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            seq.addAction(
                finger.createPointerMove(
                    Duration.ofMillis(duration),
                    PointerInput.Origin.viewport(),
                    endX,
                    endY
                )
            );
            seq.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            ((RemoteWebDriver) mDriver).perform(Arrays.asList(seq));
            Report.updateTestLog(Action, "Element Swiped to " + Data, Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(
                Action,
                "Unable to Swipe the Element to" + Data + ", Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Swipe Screen to <Input> position",
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
    public void swipeMobileScreen() {
        try {
            int duration = 2000;
            Dimension size = mDriver.manage().window().getSize();
            int startX = 0;
            int startY = 0;
            int endX = 0;
            int endY = 0;

            switch (Data) {
                case "Left":
                    startX = (int) (0.8 * size.getWidth());
                    startY = (int) size.getHeight() / 2;
                    endX = (int) (0.2 * size.getWidth());
                    endY = startY;
                    break;
                case "Right":
                    startX = (int) (0.2 * size.getWidth());
                    startY = (int) size.getHeight() / 2;
                    endX = (int) (0.8 * size.getWidth());
                    endY = startY;
                    break;
                case "Up":
                    startX = (int) (size.getWidth() / 2);
                    startY = (int) (0.8 * size.getHeight());
                    endX = startX;
                    endY = (int) (0.2 * size.getHeight());
                    break;
                case "Down":
                    startX = (int) (size.getWidth() / 2);
                    startY = (int) (0.2 * size.getHeight());
                    endX = startX;
                    endY = (int) (0.8 * size.getHeight());
                    break;
            }
            if (!Condition.equals("")) {
                duration = Integer.parseInt(Condition);
            }
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence seq = new Sequence(finger, 1);
            seq.addAction(
                finger.createPointerMove(
                    Duration.ofMillis(0),
                    PointerInput.Origin.viewport(),
                    startX,
                    startY
                )
            );
            seq.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            seq.addAction(
                finger.createPointerMove(
                    Duration.ofMillis(duration),
                    PointerInput.Origin.viewport(),
                    endX,
                    endY
                )
            );
            seq.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            ((RemoteWebDriver) mDriver).perform(Arrays.asList(seq));
            Report.updateTestLog(Action, "Screen swiped to " + Data, Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(
                Action,
                "Unable to Swipe the Screen to" + Data + ", Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Rotate Screen orientation to Landscape",
        input = InputType.NO,
        condition = InputType.NO
    )
    @Args(
        inputHelp = "no input required (e.g. leave empty)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void rotateLandscape() {
        try {
            ((SupportsRotation) mDriver).rotate(org.openqa.selenium.ScreenOrientation.LANDSCAPE);
            Report.updateTestLog(Action, "Screen orientation changed to Landscape ", Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(
                Action,
                "Unable to change the Screen orientation to Landscape, Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Rotate Screen orientation to Portrait",
        input = InputType.NO,
        condition = InputType.NO
    )
    @Args(
        inputHelp = "no input required (e.g. leave empty)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void rotatePortrait() {
        try {
            ((SupportsRotation) mDriver).rotate(org.openqa.selenium.ScreenOrientation.PORTRAIT);
            Report.updateTestLog(Action, "Screen orientation changed to Portrait ", Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(
                Action,
                "Unable to change the Screen orientation to Portrait, Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    @Action(
        object = ObjectType.APP,
        desc = "Long press the [<Object>]",
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
    public void longPress() {
        try {
            int holdDuration = Integer.parseInt(Data);
            Rectangle rectangle = Element.getRect();
            Point point = new Point(
                rectangle.x + (rectangle.width / 2),
                rectangle.y + (rectangle.height / 2)
            );
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence seq = new Sequence(finger, 1);
            seq.addAction(
                finger.createPointerMove(
                    Duration.ofMillis(0),
                    PointerInput.Origin.viewport(),
                    point.x,
                    point.y
                )
            );
            seq.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            seq.addAction(
                finger.createPointerMove(
                    Duration.ofMillis(50),
                    PointerInput.Origin.viewport(),
                    point.x,
                    point.y
                )
            );
            seq.addAction(new Pause(finger, Duration.ofMillis(holdDuration)));
            seq.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            ((RemoteWebDriver) mDriver).perform(Arrays.asList(seq));
            Report.updateTestLog(Action, "Long press on " + "[" + ObjectName + "]", Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(
                Action,
                "Unable to perform Long Press action, Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    @Action(
        object = ObjectType.APP,
        desc = "Double tap the [<Object>]",
        input = InputType.NO,
        condition = InputType.NO
    )
    @Args(
        inputHelp = "no input required (e.g. leave empty)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void doubleTap() {
        try {
            Rectangle rectangle = Element.getRect();
            Point point = new Point(
                rectangle.x + (rectangle.width / 2),
                rectangle.y + (rectangle.height / 2)
            );
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence seq = new Sequence(finger, 1);
            seq.addAction(
                finger.createPointerMove(
                    Duration.ofMillis(0),
                    PointerInput.Origin.viewport(),
                    point.x,
                    point.y
                )
            );
            seq.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            seq.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            seq.addAction(new Pause(finger, Duration.ofMillis(100)));
            seq.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            seq.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            ((RemoteWebDriver) mDriver).perform(Arrays.asList(seq));
            Report.updateTestLog(
                Action,
                "Double tap performed on [" + ObjectName + "]",
                Status.DONE
            );
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(
                Action,
                "Unable to perform double tap, Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Activate app by package/bundle id [<Data>]",
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
    public void activateApp() {
        try {
            if (Data == null || Data.trim().isEmpty()) {
                Report.updateTestLog(Action, "App id input is empty", Status.FAIL);
                return;
            }
            String appId = Data.trim();
            if (mDriver instanceof AndroidDriver) {
                ((AndroidDriver) mDriver).activateApp(appId);
            } else if (mDriver instanceof IOSDriver) {
                ((IOSDriver) mDriver).activateApp(appId);
            } else {
                Report.updateTestLog(Action, "Driver does not support activateApp", Status.FAIL);
                return;
            }
            Report.updateTestLog(Action, "Activated app: " + appId, Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(
                Action,
                "Unable to activate app, Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Terminate app by package/bundle id [<Data>]",
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
    public void terminateApp() {
        try {
            if (Data == null || Data.trim().isEmpty()) {
                Report.updateTestLog(Action, "App id input is empty", Status.FAIL);
                return;
            }
            String appId = Data.trim();
            boolean terminated;
            if (mDriver instanceof AndroidDriver) {
                terminated = ((AndroidDriver) mDriver).terminateApp(appId);
            } else if (mDriver instanceof IOSDriver) {
                terminated = ((IOSDriver) mDriver).terminateApp(appId);
            } else {
                Report.updateTestLog(Action, "Driver does not support terminateApp", Status.FAIL);
                return;
            }
            Report.updateTestLog(
                Action,
                "Terminate app " + appId + " result: " + terminated,
                terminated ? Status.DONE : Status.DEBUG
            );
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(
                Action,
                "Unable to terminate app, Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Run app in background for [<Data>] seconds (default 5)",
        input = InputType.OPTIONAL,
        condition = InputType.NO
    )
    @Args(
        input = ArgType.INTEGER,
        inputExample = "@5",
        inputHelp = "optional duration in seconds before app resumes (e.g. @5)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void runAppInBackground() {
        try {
            int seconds = 5;
            if (Data != null && !Data.trim().isEmpty()) {
                seconds = Integer.parseInt(Data.trim());
            }
            Duration duration = Duration.ofSeconds(seconds);
            if (mDriver instanceof AndroidDriver) {
                ((AndroidDriver) mDriver).runAppInBackground(duration);
            } else if (mDriver instanceof IOSDriver) {
                ((IOSDriver) mDriver).runAppInBackground(duration);
            } else {
                Report.updateTestLog(
                    Action,
                    "Driver does not support runAppInBackground",
                    Status.FAIL
                );
                return;
            }
            Report.updateTestLog(
                Action,
                "App sent to background for " + seconds + " second(s)",
                Status.DONE
            );
        } catch (NumberFormatException e) {
            Report.updateTestLog(Action, "Invalid seconds input: " + Data, Status.FAIL);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(
                Action,
                "Unable to run app in background, Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Query app state for package/bundle id [<Data>] and optionally store in [Condition]",
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
    public void queryAppState() {
        try {
            if (Data == null || Data.trim().isEmpty()) {
                Report.updateTestLog(Action, "App id input is empty", Status.FAIL);
                return;
            }
            String appId = Data.trim();
            String state;
            if (mDriver instanceof AndroidDriver) {
                state = ((AndroidDriver) mDriver).queryAppState(appId).name();
            } else if (mDriver instanceof IOSDriver) {
                state = ((IOSDriver) mDriver).queryAppState(appId).name();
            } else {
                Report.updateTestLog(Action, "Driver does not support queryAppState", Status.FAIL);
                return;
            }
            if (Condition != null && !Condition.trim().isEmpty()) {
                addVar(Condition, state);
            }
            Report.updateTestLog(Action, "App state for " + appId + " is " + state, Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(
                Action,
                "Unable to query app state, Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Install app from local path [<Data>]",
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
    public void installMobileApp() {
        try {
            if (Data == null || Data.trim().isEmpty()) {
                Report.updateTestLog(Action, "App path input is empty", Status.FAIL);
                return;
            }
            invokeDriverMethod(
                "installApp",
                new Class[] { String.class },
                new Object[] { Data.trim() }
            );
            Report.updateTestLog(Action, "App installed from: " + Data, Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(Action, "Failed to install app: " + e.getMessage(), Status.FAIL);
        }
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Uninstall app by package/bundle id [<Data>]",
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
    public void removeMobileApp() {
        try {
            if (Data == null || Data.trim().isEmpty()) {
                Report.updateTestLog(Action, "App id input is empty", Status.FAIL);
                return;
            }
            Object removed = invokeDriverMethod(
                "removeApp",
                new Class[] { String.class },
                new Object[] { Data.trim() }
            );
            Report.updateTestLog(
                Action,
                "Remove app result for " + Data + ": " + String.valueOf(removed),
                Status.DONE
            );
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(Action, "Failed to remove app: " + e.getMessage(), Status.FAIL);
        }
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Store whether app [<Data>] is installed into runtime variable [<Condition>]",
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
    public void storeAppInstalledState() {
        try {
            if (
                Data == null ||
                Data.trim().isEmpty() ||
                Condition == null ||
                Condition.trim().isEmpty()
            ) {
                Report.updateTestLog(Action, "Input/Condition cannot be empty", Status.FAIL);
                return;
            }
            Object installed = invokeDriverMethod(
                "isAppInstalled",
                new Class[] { String.class },
                new Object[] { Data.trim() }
            );
            addVar(Condition, String.valueOf(installed));
            Report.updateTestLog(
                Action,
                "Stored app installed state for " + Data + " in variable " + Condition,
                Status.DONE
            );
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(
                Action,
                "Failed to read app installed state: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Simulate biometric match with [<Data>] (iOS: true/false, Android: fingerprint id)",
        input = InputType.YES,
        condition = InputType.NO
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "@true",
        inputHelp = "iOS uses true/false; Android uses fingerprint id (e.g. @true or @1)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void simulateBiometricMatch() {
        try {
            if (Data == null || Data.trim().isEmpty()) {
                Report.updateTestLog(Action, "Biometric input is empty", Status.FAIL);
                return;
            }
            String input = Data.trim().toLowerCase();
            if (mDriver instanceof IOSDriver) {
                boolean match = Boolean.parseBoolean(input);
                ((IOSDriver) mDriver).executeScript(
                        "mobile: sendBiometricMatch",
                        Map.of("type", "touchId", "match", match)
                    );
                Report.updateTestLog(
                    Action,
                    "iOS biometric simulated with match=" + match,
                    Status.DONE
                );
            } else if (mDriver instanceof AndroidDriver) {
                int fingerId = Integer.parseInt(input);
                ((AndroidDriver) mDriver).executeScript(
                        "mobile: fingerPrint",
                        Map.of("fingerprintId", fingerId)
                    );
                Report.updateTestLog(
                    Action,
                    "Android fingerprint simulated with id=" + fingerId,
                    Status.DONE
                );
            } else {
                Report.updateTestLog(Action, "Unsupported driver/platform", Status.FAIL);
            }
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(
                Action,
                "Failed to simulate biometric: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Set Android airplane mode with [<Data>] as on|off",
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
    public void setAirplaneMode() {
        try {
            if (!(mDriver instanceof AndroidDriver)) {
                Report.updateTestLog(Action, "Airplane mode action is Android-only", Status.DEBUG);
                return;
            }
            if (Data == null || Data.trim().isEmpty()) {
                Report.updateTestLog(Action, "Input cannot be empty. Use on|off", Status.FAIL);
                return;
            }
            String mode = Data.trim().toLowerCase();
            String value;
            if ("on".equals(mode)) {
                value = "1";
            } else if ("off".equals(mode)) {
                value = "0";
            } else {
                Report.updateTestLog(Action, "Invalid input. Use on|off", Status.FAIL);
                return;
            }
            ((AndroidDriver) mDriver).executeScript(
                    "mobile: shell",
                    Map.of(
                        "command",
                        "settings",
                        "args",
                        List.of("put", "global", "airplane_mode_on", value)
                    )
                );
            ((AndroidDriver) mDriver).executeScript(
                    "mobile: shell",
                    Map.of(
                        "command",
                        "am",
                        "args",
                        List.of(
                            "broadcast",
                            "-a",
                            "android.intent.action.AIRPLANE_MODE",
                            "--ez",
                            "state",
                            "on".equals(mode) ? "true" : "false"
                        )
                    )
                );
            Report.updateTestLog(Action, "Airplane mode set to " + mode, Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(
                Action,
                "Failed to set airplane mode: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Toggle Internet Data ",
        input = InputType.NO,
        condition = InputType.NO
    )
    @Args(
        inputHelp = "no input required (e.g. leave empty)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void toggleInternetData() {
        try {
            if (mDriver instanceof AndroidDriver) {
                ((AndroidDriver) mDriver).toggleData();
            } else if (mDriver instanceof IOSDriver) {}
            Report.updateTestLog(Action, "Toggle Data is done ", Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(
                Action,
                "Unable to toggle Data, Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Toggle Wifi",
        input = InputType.NO,
        condition = InputType.NO
    )
    @Args(
        inputHelp = "no input required (e.g. leave empty)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void toggleWifi() {
        try {
            if (mDriver instanceof AndroidDriver) {
                ((AndroidDriver) mDriver).toggleWifi();
            } else if (mDriver instanceof IOSDriver) {}
            Report.updateTestLog(Action, "Toggle Wifi is done ", Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(
                Action,
                "Unable to toggle Wifi, Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Toggle Location Services",
        input = InputType.NO,
        condition = InputType.NO
    )
    @Args(
        inputHelp = "no input required (e.g. leave empty)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void toggleLocationServices() {
        try {
            if (mDriver instanceof AndroidDriver) {
                ((AndroidDriver) mDriver).toggleLocationServices();
            } else if (mDriver instanceof IOSDriver) {}
            Report.updateTestLog(Action, "Toggle Location Services is done ", Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(
                Action,
                "Unable to toggle Location Services, Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Lock Mobile Device",
        input = InputType.NO,
        condition = InputType.NO
    )
    @Args(
        inputHelp = "no input required (e.g. leave empty)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void lockMobileDevice() {
        try {
            if (mDriver instanceof AndroidDriver) {
                boolean lockAndroid = ((AndroidDriver) mDriver).isDeviceLocked();
                if (!lockAndroid) {
                    ((AndroidDriver) mDriver).lockDevice();
                    Report.updateTestLog(Action, "Device is locked successfully ", Status.DONE);
                } else {
                    Report.updateTestLog(Action, "Device is locked already ", Status.DONE);
                }
            } else if (mDriver instanceof IOSDriver) {
                boolean lockIOS = ((IOSDriver) mDriver).isDeviceLocked();
                if (!lockIOS) {
                    ((IOSDriver) mDriver).lockDevice();
                    Report.updateTestLog(Action, "Device is locked successfully ", Status.DONE);
                } else {
                    Report.updateTestLog(Action, "Device is locked already ", Status.DONE);
                }
            } else {
                Report.updateTestLog(Action, "Driver does not support lock operation", Status.FAIL);
            }
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(
                Action,
                "Unable to Lock device, Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Unlock Mobile Device",
        input = InputType.NO,
        condition = InputType.NO
    )
    @Args(
        inputHelp = "no input required (e.g. leave empty)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void unlockMobileDevice() {
        try {
            if (mDriver instanceof AndroidDriver) {
                boolean lockAndroid = ((AndroidDriver) mDriver).isDeviceLocked();
                if (lockAndroid) {
                    ((AndroidDriver) mDriver).unlockDevice();
                    Report.updateTestLog(Action, "Device is unlocked successfully ", Status.DONE);
                } else {
                    Report.updateTestLog(Action, "Device is unlocked already ", Status.DONE);
                }
            } else if (mDriver instanceof IOSDriver) {
                boolean lockIOS = ((IOSDriver) mDriver).isDeviceLocked();
                if (lockIOS) {
                    ((IOSDriver) mDriver).unlockDevice();
                    Report.updateTestLog(Action, "Device is unlocked successfully ", Status.DONE);
                } else {
                    Report.updateTestLog(Action, "Device is unlocked already ", Status.DONE);
                }
            } else {
                Report.updateTestLog(
                    Action,
                    "Driver does not support unlock operation",
                    Status.FAIL
                );
            }
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(
                Action,
                "Unable to Unlock device, Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Get device time in variable",
        input = InputType.NO,
        condition = InputType.YES
    )
    @Args(
        inputHelp = "no input required (e.g. leave empty)",
        condition = ConditionKind.TEXT,
        conditionExample = "@value",
        conditionHelp = "condition value (e.g. @value)"
    )
    public void getDeviceTime() {
        try {
            String deviceTime = "";
            if (mDriver instanceof AndroidDriver) {
                deviceTime = ((AndroidDriver) mDriver).getDeviceTime();
                addVar(Condition, deviceTime);
            } else if (mDriver instanceof IOSDriver) {
                deviceTime = ((IOSDriver) mDriver).getDeviceTime();
                addVar(Condition, deviceTime);
            }
            Report.updateTestLog(Action, "Device time is " + deviceTime, Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(
                Action,
                "Unable to get device time, Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Open Notifications",
        input = InputType.NO,
        condition = InputType.NO
    )
    @Args(
        inputHelp = "no input required (e.g. leave empty)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void openNotifications() {
        try {
            if (mDriver instanceof AndroidDriver) {
                ((AndroidDriver) mDriver).openNotifications();
            } else if (mDriver instanceof IOSDriver) {
                Report.updateTestLog(Action, "openNotifications is Android-only", Status.DEBUG);
                return;
            }
            Report.updateTestLog(Action, "Notification opened ", Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(
                Action,
                "Unable to open Notifications, Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    @Action(object = ObjectType.MOBILE, desc = "Shake Device")
    @Args(
        inputHelp = "no input required (e.g. leave empty)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void shake() {
        try {
            String executionMode = executeShakeGesture();
            String normalizedMode = executionMode.toLowerCase();
            if (normalizedMode.contains("skipped") || normalizedMode.contains("unsupported")) {
                Report.updateTestLog(
                    Action,
                    "Unable to perform Shake operation, " + executionMode,
                    Status.FAIL
                );
                return;
            }
            Report.updateTestLog(Action, "Performed Shake Operation " + executionMode, Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(
                Action,
                "Unable to perform Shake operation, Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Hide Keyboard",
        input = InputType.NO,
        condition = InputType.NO
    )
    @Args(
        inputHelp = "no input required (e.g. leave empty)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void hideKeyboard() {
        try {
            if (mDriver instanceof AndroidDriver) {
                if (((AndroidDriver) mDriver).isKeyboardShown()) {
                    ((AndroidDriver) mDriver).hideKeyboard();
                    Report.updateTestLog(Action, "Keyboard hidden successfully ", Status.DONE);
                } else {
                    Report.updateTestLog(Action, "Keyboard is hidden already ", Status.DEBUG);
                }
            } else if (mDriver instanceof IOSDriver) {
                ((IOSDriver) mDriver).hideKeyboard();
                Report.updateTestLog(Action, "Keyboard hidden successfully ", Status.DONE);
            } else {
                Report.updateTestLog(Action, "Driver does not support hideKeyboard", Status.FAIL);
            }
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(
                Action,
                "Unable to hide keyboard, Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Go to homescreen",
        input = InputType.NO,
        condition = InputType.NO
    )
    @Args(
        inputHelp = "no input required (e.g. leave empty)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void goToHomescreen() {
        try {
            if (mDriver instanceof AndroidDriver) {
                ((AndroidDriver) mDriver).executeScript("mobile: pressKey", Map.of("keycode", 3));
            } else if (mDriver instanceof IOSDriver) {
                ((IOSDriver) mDriver).executeScript("mobile: pressButton", Map.of("name", "home"));
            }
            Report.updateTestLog(Action, "Performed go to Homescreen operation", Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(
                Action,
                "Unable to perform homescreen operation, Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Press mobile key/button [<Data>] (Android: back/home/recent/menu/enter or keycode, iOS: home)",
        input = InputType.YES,
        condition = InputType.NO
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "@back",
        inputHelp = "mobile key/button name or Android keycode (e.g. @back or @66)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void pressMobileKeyOrButton() {
        try {
            if (Data == null || Data.trim().isEmpty()) {
                Report.updateTestLog(Action, "Key/button input is empty", Status.FAIL);
                return;
            }
            String key = Data.trim().toLowerCase();
            if (mDriver instanceof AndroidDriver) {
                Map<String, Object> args;
                if (key.matches("[0-9]+")) {
                    args = Map.of("keycode", Integer.parseInt(key));
                } else {
                    int keycode;
                    switch (key) {
                        case "home":
                            keycode = 3;
                            break;
                        case "back":
                            keycode = 4;
                            break;
                        case "enter":
                            keycode = 66;
                            break;
                        case "menu":
                            keycode = 82;
                            break;
                        case "recent":
                        case "appswitch":
                            keycode = 187;
                            break;
                        default:
                            Report.updateTestLog(
                                Action,
                                "Unsupported Android key: " + Data,
                                Status.FAIL
                            );
                            return;
                    }
                    args = Map.of("keycode", keycode);
                }
                ((AndroidDriver) mDriver).executeScript("mobile: pressKey", args);
                Report.updateTestLog(Action, "Pressed Android key: " + Data, Status.DONE);
            } else if (mDriver instanceof IOSDriver) {
                if (!"home".equals(key)) {
                    Report.updateTestLog(
                        Action,
                        "Unsupported iOS button: " + Data + " (currently supports home)",
                        Status.FAIL
                    );
                    return;
                }
                ((IOSDriver) mDriver).executeScript("mobile: pressButton", Map.of("name", "home"));
                Report.updateTestLog(Action, "Pressed iOS button: home", Status.DONE);
            } else {
                Report.updateTestLog(Action, "Unsupported driver/platform", Status.FAIL);
            }
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(
                Action,
                "Unable to press key/button, Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Pinch and Zoom",
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
    public void pinchAndZoomScreen() throws InterruptedException {
        try {
            if (Data == null || !Data.trim().matches("-?[0-9]+(\\.[0-9]+)?")) {
                Report.updateTestLog(
                    Action,
                    "Invalid input [" + Data + "]. Expected numeric zoom percentage",
                    Status.FAIL
                );
                return;
            }
            Dimension size = mDriver.manage().window().getSize();
            Point source = new Point(size.getWidth(), size.getHeight());
            float halfY = source.y / 2;
            float halfX = source.x / 2;
            double angle = Math.atan2(halfY, halfX);
            int halfDigonal = (int) Math.sqrt((Math.pow(halfX, 2) + (Math.pow(halfY, 2))));
            float percentageZoom = Float.parseFloat(Data) / 100;
            int xExtension = (int) (Math.cos(angle) * halfDigonal * percentageZoom);
            int yExtension = (int) (Math.sin(angle) * halfDigonal * percentageZoom);
            int startXFingure1 = source.x / 2;
            int endXFingure1 = (int) (source.x / 2 + xExtension);
            int startXFingure2 = source.x / 2;
            int endXFingure2 = (int) (source.x / 2 - xExtension);
            int startYFingure1 = source.y / 2;
            int endYFingure1 = (int) (source.y / 2 - yExtension);
            int startYFingure2 = source.y / 2;
            int endYFingure2 = (int) (source.y / 2 + yExtension);
            PointerInput finger1 = new PointerInput(PointerInput.Kind.TOUCH, "finger1");
            PointerInput finger2 = new PointerInput(PointerInput.Kind.TOUCH, "finger2");
            Sequence pinchAndZoom1 = new Sequence(finger1, 1);
            pinchAndZoom1.addAction(
                finger1.createPointerMove(
                    Duration.ZERO,
                    PointerInput.Origin.viewport(),
                    startXFingure1,
                    startYFingure1
                )
            );
            pinchAndZoom1.addAction(
                finger1.createPointerDown(PointerInput.MouseButton.LEFT.asArg())
            );
            pinchAndZoom1.addAction(new Pause(finger1, Duration.ofMillis(200)));
            pinchAndZoom1.addAction(
                finger1.createPointerMove(
                    Duration.ofMillis(200),
                    PointerInput.Origin.viewport(),
                    endXFingure1,
                    endYFingure1
                )
            );
            pinchAndZoom1.addAction(finger1.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            Sequence pinchAndZoom2 = new Sequence(finger2, 1);
            pinchAndZoom2.addAction(
                finger2.createPointerMove(
                    Duration.ZERO,
                    PointerInput.Origin.viewport(),
                    startXFingure2,
                    startYFingure2
                )
            );
            pinchAndZoom2.addAction(
                finger2.createPointerDown(PointerInput.MouseButton.LEFT.asArg())
            );
            pinchAndZoom2.addAction(new Pause(finger2, Duration.ofMillis(200)));
            pinchAndZoom2.addAction(
                finger2.createPointerMove(
                    Duration.ofMillis(200),
                    PointerInput.Origin.viewport(),
                    endXFingure2,
                    endYFingure2
                )
            );
            pinchAndZoom2.addAction(finger2.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

            ((RemoteWebDriver) mDriver).perform(Arrays.asList(pinchAndZoom1, pinchAndZoom2));
            Report.updateTestLog(Action, "Pinch/Zoom performed on screen", Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(
                Action,
                "Unable to perform pinch/zoom on screen, Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    @Action(
        object = ObjectType.APP,
        desc = "Pinch and Zoom",
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
    public void pinchAndZoomElement() throws InterruptedException {
        try {
            if (Data == null || !Data.trim().matches("-?[0-9]+(\\.[0-9]+)?")) {
                Report.updateTestLog(
                    Action,
                    "Invalid input [" + Data + "]. Expected numeric zoom percentage",
                    Status.FAIL
                );
                return;
            }
            Dimension size = mDriver.manage().window().getSize();
            Point SreenSource = new Point(size.getWidth(), size.getHeight());
            Rectangle rectangle = Element.getRect();
            Point elementCentre = new Point(
                rectangle.x + (rectangle.width / 2),
                rectangle.y + (rectangle.height / 2)
            );
            int rightSideWidth = SreenSource.x - elementCentre.x;
            int leftSideWidth = elementCentre.x;
            int longestDigonal = 0;
            double angle = 0;
            float percentageZoom = Float.parseFloat(Data) / 100;
            int xExtension = 0;
            int yExtension = 0;
            int startXFingure1 = elementCentre.x;
            int startXFingure2 = elementCentre.x;
            int startYFingure1 = elementCentre.y;
            int startYFingure2 = elementCentre.y;
            int endXFingure1 = 0;
            int endYFingure1 = 0;
            int endYFingure2 = 0;
            int endXFingure2 = 0;
            if (rightSideWidth > leftSideWidth) {
                longestDigonal =
                    (int) Math.sqrt((Math.pow(rightSideWidth, 2) + (Math.pow(elementCentre.y, 2))));
                angle = Math.atan2(elementCentre.y, rightSideWidth);
                xExtension = (int) (Math.cos(angle) * longestDigonal * percentageZoom);
                yExtension = (int) (Math.sin(angle) * longestDigonal * percentageZoom);
                endXFingure1 = (int) (elementCentre.x + xExtension);
                endXFingure2 = (int) (elementCentre.x - xExtension);
            } else {
                longestDigonal =
                    (int) Math.sqrt((Math.pow(leftSideWidth, 2) + (Math.pow(elementCentre.y, 2))));
                angle = Math.atan2(elementCentre.y, leftSideWidth);
                xExtension = (int) (Math.cos(angle) * longestDigonal * percentageZoom);
                yExtension = (int) (Math.sin(angle) * longestDigonal * percentageZoom);
                endXFingure1 = (int) (elementCentre.x - xExtension);
                endXFingure2 = (int) (elementCentre.x + xExtension);
            }
            endYFingure1 = (int) (elementCentre.y - yExtension);
            endYFingure2 = (int) (elementCentre.y + yExtension);
            PointerInput finger1 = new PointerInput(PointerInput.Kind.TOUCH, "finger1");
            PointerInput finger2 = new PointerInput(PointerInput.Kind.TOUCH, "finger2");
            Sequence pinchAndZoom1 = new Sequence(finger1, 1);
            pinchAndZoom1.addAction(
                finger1.createPointerMove(
                    Duration.ZERO,
                    PointerInput.Origin.viewport(),
                    startXFingure1,
                    startYFingure1
                )
            );
            pinchAndZoom1.addAction(
                finger1.createPointerDown(PointerInput.MouseButton.LEFT.asArg())
            );
            pinchAndZoom1.addAction(new Pause(finger1, Duration.ofMillis(200)));
            pinchAndZoom1.addAction(
                finger1.createPointerMove(
                    Duration.ofMillis(200),
                    PointerInput.Origin.viewport(),
                    endXFingure1,
                    endYFingure1
                )
            );
            pinchAndZoom1.addAction(finger1.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            Sequence pinchAndZoom2 = new Sequence(finger2, 1);
            pinchAndZoom2.addAction(
                finger2.createPointerMove(
                    Duration.ZERO,
                    PointerInput.Origin.viewport(),
                    startXFingure2,
                    startYFingure2
                )
            );
            pinchAndZoom2.addAction(
                finger2.createPointerDown(PointerInput.MouseButton.LEFT.asArg())
            );
            pinchAndZoom2.addAction(new Pause(finger2, Duration.ofMillis(200)));
            pinchAndZoom2.addAction(
                finger2.createPointerMove(
                    Duration.ofMillis(200),
                    PointerInput.Origin.viewport(),
                    endXFingure2,
                    endYFingure2
                )
            );
            pinchAndZoom2.addAction(finger2.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

            ((RemoteWebDriver) mDriver).perform(Arrays.asList(pinchAndZoom1, pinchAndZoom2));
            Report.updateTestLog(Action, "Pinch/Zoom performed on element", Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(
                Action,
                "Unable to perform pinch/zoom on element, Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Switch to first available WebView context",
        input = InputType.NO,
        condition = InputType.NO
    )
    @Args(
        inputHelp = "no input required (e.g. leave empty)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void switchToWebView() {
        try {
            Set<String> contextNames = ((SupportsContextSwitching) mDriver).getContextHandles();
            String webViewContext = contextNames
                .stream()
                .filter(c -> c.startsWith("WEBVIEW"))
                .findFirst()
                .orElse(null);
            if (webViewContext != null) {
                ((SupportsContextSwitching) mDriver).context(webViewContext);
                Report.updateTestLog(
                    Action,
                    "Switched to WebView context: " + webViewContext,
                    Status.DONE
                );
            } else {
                Report.updateTestLog(
                    Action,
                    "No WebView context found. Available: " + contextNames,
                    Status.FAIL
                );
            }
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(
                Action,
                "Failed to switch to WebView: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Switch to Native App context",
        input = InputType.NO,
        condition = InputType.NO
    )
    @Args(
        inputHelp = "no input required (e.g. leave empty)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void switchToNativeApp() {
        try {
            SupportsContextSwitching contextDriver = (SupportsContextSwitching) mDriver;
            String startContext = contextDriver.getContext();
            Set<String> contextNames = contextDriver.getContextHandles();

            if (startContext != null && startContext.startsWith("WEBVIEW")) {
                try {
                    Set<String> windowHandles = mDriver.getWindowHandles();
                    if (windowHandles.size() > 1) {
                        String currentWindowHandle = mDriver.getWindowHandle();
                        mDriver.close();
                        Set<String> remainingHandles = mDriver.getWindowHandles();
                        if (!remainingHandles.isEmpty()) {
                            mDriver.switchTo().window(remainingHandles.iterator().next());
                        }
                        Report.updateTestLog(
                            Action,
                            "Closed WebView window " +
                            currentWindowHandle +
                            " before switching to native",
                            Status.DEBUG
                        );
                    }
                } catch (Exception windowEx) {
                    Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, windowEx);
                    Report.updateTestLog(
                        Action,
                        "Could not close/switch WebView window before native switch: " +
                        windowEx.getMessage(),
                        Status.DEBUG
                    );
                }
            }

            contextDriver.context("NATIVE_APP");
            String currentContext = contextDriver.getContext();
            if ("NATIVE_APP".equals(currentContext)) {
                Element = null;
                Report.updateTestLog(Action, "Switched to NATIVE_APP context", Status.DONE);
                return;
            }

            mDriver.navigate().back();
            currentContext = contextDriver.getContext();
            if ("NATIVE_APP".equals(currentContext)) {
                Element = null;
                Report.updateTestLog(
                    Action,
                    "Context switched to NATIVE_APP after back navigation",
                    Status.DONE
                );
            } else {
                Report.updateTestLog(
                    Action,
                    "Switch to NATIVE_APP failed even after back. Current context: " +
                    currentContext +
                    ", Start context: " +
                    startContext +
                    ", Available contexts: " +
                    contextNames,
                    Status.FAIL
                );
            }
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(
                Action,
                "Failed to switch to NATIVE_APP: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Switch to context by index [<Data>] (0-based)",
        input = InputType.YES,
        condition = InputType.NO
    )
    @Args(
        input = ArgType.INTEGER,
        inputExample = "@0",
        inputHelp = "0-based context index from available contexts list (e.g. @0)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void switchContextByIndex() {
        try {
            if (Data == null || Data.trim().isEmpty()) {
                Report.updateTestLog(Action, "Context index input is empty", Status.FAIL);
                return;
            }
            List<String> contexts = new ArrayList<>(
                ((SupportsContextSwitching) mDriver).getContextHandles()
            );
            int index = Integer.parseInt(Data.trim());
            if (index < 0 || index >= contexts.size()) {
                Report.updateTestLog(
                    Action,
                    "Context index " +
                    index +
                    " is out of range. Available contexts (" +
                    contexts.size() +
                    "): " +
                    contexts,
                    Status.FAIL
                );
                return;
            }
            String target = contexts.get(index);
            ((SupportsContextSwitching) mDriver).context(target);
            Report.updateTestLog(
                Action,
                "Switched to context[" + index + "]: " + target,
                Status.DONE
            );
        } catch (NumberFormatException e) {
            Report.updateTestLog(
                Action,
                "Invalid index '" + Data + "': must be a number",
                Status.FAIL
            );
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(
                Action,
                "Failed to switch context by index: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Switch to context whose name contains [<Data>]",
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
    public void switchContextWhenNameContains() {
        try {
            if (Data == null || Data.trim().isEmpty()) {
                Report.updateTestLog(Action, "Context name input is empty", Status.FAIL);
                return;
            }
            Set<String> contextNames = ((SupportsContextSwitching) mDriver).getContextHandles();
            String match = contextNames
                .stream()
                .filter(c -> c.contains(Data))
                .findFirst()
                .orElse(null);
            if (match != null) {
                ((SupportsContextSwitching) mDriver).context(match);
                Report.updateTestLog(Action, "Switched to context: " + match, Status.DONE);
            } else {
                Report.updateTestLog(
                    Action,
                    "No context found containing '" + Data + "'. Available: " + contextNames,
                    Status.FAIL
                );
            }
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(
                Action,
                "Failed to switch context: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Switch to context whose name equals [<Data>]",
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
    public void switchContextWhenNameEquals() {
        try {
            if (Data == null || Data.trim().isEmpty()) {
                Report.updateTestLog(Action, "Context name input is empty", Status.FAIL);
                return;
            }
            Set<String> contextNames = ((SupportsContextSwitching) mDriver).getContextHandles();
            if (contextNames.contains(Data)) {
                ((SupportsContextSwitching) mDriver).context(Data);
                Report.updateTestLog(Action, "Switched to context: " + Data, Status.DONE);
            } else {
                Report.updateTestLog(
                    Action,
                    "Context '" + Data + "' not found. Available: " + contextNames,
                    Status.FAIL
                );
            }
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(
                Action,
                "Failed to switch context: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "List all available contexts (logged to report)",
        input = InputType.NO,
        condition = InputType.NO
    )
    @Args(
        inputHelp = "no input required (e.g. leave empty)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void listAvailableContexts() {
        try {
            Set<String> contextNames = ((SupportsContextSwitching) mDriver).getContextHandles();
            String current = ((SupportsContextSwitching) mDriver).getContext();
            StringBuilder sb = new StringBuilder("Available contexts: ");
            int i = 0;
            for (String ctx : contextNames) {
                sb.append("[").append(i++).append("] ").append(ctx);
                if (ctx.equals(current)) sb.append(" (current)");
                sb.append("  ");
            }
            Report.updateTestLog(Action, sb.toString().trim(), Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(Action, "Failed to list contexts: " + e.getMessage(), Status.FAIL);
        }
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Store all available contexts into runtime variable [<Data>] (comma-separated)",
        input = InputType.YES,
        condition = InputType.NO
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "%contexts%",
        inputHelp = "destination runtime variable in %var% format (e.g. %contexts%)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void storeAvailableContexts() {
        try {
            if (Data == null || Data.trim().isEmpty()) {
                Report.updateTestLog(Action, "Variable name input is empty", Status.FAIL);
                return;
            }
            Set<String> contextNames = ((SupportsContextSwitching) mDriver).getContextHandles();
            String value = String.join(",", contextNames);
            addVar(Data, value);
            Report.updateTestLog(Action, "Stored contexts in variable " + Data, Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(
                Action,
                "Failed to store contexts: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Scroll to element using [<Data>] as strategy=value and optional [Condition] direction:attempts",
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
    public void scrollToElement() {
        try {
            if (Data == null || !Data.contains("=")) {
                Report.updateTestLog(
                    Action,
                    "Invalid input format. Use strategy=value, for example id=my.element",
                    Status.FAIL
                );
                return;
            }
            String[] parts = Data.split("=", 2);
            String strategy = parts[0].trim().toLowerCase();
            String selector = parts[1].trim();
            String direction = "down";
            int attempts = 10;
            if (Condition != null && !Condition.trim().isEmpty()) {
                String[] opts = Condition.split(":", 2);
                direction = opts[0].trim().toLowerCase();
                if (opts.length > 1 && opts[1].trim().matches("[0-9]+")) {
                    attempts = Integer.parseInt(opts[1].trim());
                }
            }
            if (!direction.matches("up|down|left|right")) {
                Report.updateTestLog(
                    Action,
                    "Invalid direction in Condition: " + direction + ". Use up|down|left|right",
                    Status.FAIL
                );
                return;
            }

            WebElement target = null;
            for (int i = 0; i <= attempts; i++) {
                try {
                    target = findElementByStrategy(strategy, selector);
                    if (target != null && target.isDisplayed()) {
                        break;
                    }
                } catch (Exception ignored) {}

                if (i == attempts) break;

                if (mDriver instanceof AndroidDriver) {
                    boolean vertical = direction.equals("up") || direction.equals("down");
                    boolean forward = direction.equals("down") || direction.equals("right");
                    String scrollable =
                        "new UiScrollable(new UiSelector().scrollable(true))" +
                        (vertical ? ".setAsVerticalList()" : ".setAsHorizontalList()");
                    String uia = scrollable + (forward ? ".scrollForward()" : ".scrollBackward()");
                    mDriver.findElement(AppiumBy.androidUIAutomator(uia));
                } else if (mDriver instanceof IOSDriver) {
                    ((IOSDriver) mDriver).executeScript(
                            "mobile:scroll",
                            Map.of("direction", direction)
                        );
                } else {
                    Report.updateTestLog(Action, "Unsupported driver/platform", Status.FAIL);
                    return;
                }
            }

            if (target != null && target.isDisplayed()) {
                Report.updateTestLog(
                    Action,
                    "Element found using " + strategy + "=" + selector,
                    Status.DONE
                );
            } else {
                Report.updateTestLog(
                    Action,
                    "Element not found after " + attempts + " scroll attempts",
                    Status.FAIL
                );
            }
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(Action, "scrollToElement failed: " + e.getMessage(), Status.FAIL);
        }
    }

    private WebElement findElementByStrategy(String strategy, String selector) {
        switch (strategy) {
            case "id":
                return mDriver.findElement(AppiumBy.id(selector));
            case "accessibilityid":
            case "accessibility id":
            case "a11y":
                return mDriver.findElement(AppiumBy.accessibilityId(selector));
            case "xpath":
                return mDriver.findElement(AppiumBy.xpath(selector));
            case "name":
                return mDriver.findElement(AppiumBy.name(selector));
            default:
                throw new IllegalArgumentException(
                    "Unsupported strategy: " +
                    strategy +
                    " (supported: id, accessibility id, xpath, name)"
                );
        }
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Store current context name into runtime variable [<Data>]",
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
    public void storeCurrentContext() {
        try {
            if (Data == null || Data.trim().isEmpty()) {
                Report.updateTestLog(Action, "Variable name input is empty", Status.FAIL);
                return;
            }
            String current = ((SupportsContextSwitching) mDriver).getContext();
            addVar(Data, current);
            Report.updateTestLog(Action, "Stored current context in variable " + Data, Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(
                Action,
                "Failed to store current context: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Update Android runtime permission using [<Data>] format package:permission:grant|revoke",
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
    public void updateRuntimePermission() {
        try {
            if (!(mDriver instanceof AndroidDriver)) {
                Report.updateTestLog(
                    Action,
                    "Runtime permission update is Android-only",
                    Status.DEBUG
                );
                return;
            }
            if (Data == null || !Data.contains(":")) {
                Report.updateTestLog(
                    Action,
                    "Invalid input. Expected package:permission:grant|revoke",
                    Status.FAIL
                );
                return;
            }
            String[] parts = Data.split(":", 3);
            if (parts.length < 3) {
                Report.updateTestLog(
                    Action,
                    "Invalid input. Expected package:permission:grant|revoke",
                    Status.FAIL
                );
                return;
            }
            String appId = parts[0].trim();
            String permission = parts[1].trim();
            String operation = parts[2].trim().toLowerCase();
            if (!operation.matches("grant|revoke")) {
                Report.updateTestLog(Action, "Operation must be grant or revoke", Status.FAIL);
                return;
            }
            ((AndroidDriver) mDriver).executeScript(
                    "mobile: shell",
                    Map.of("command", "pm", "args", List.of(operation, appId, permission))
                );
            Report.updateTestLog(
                Action,
                "Permission update executed: " + operation + " " + permission + " for " + appId,
                Status.DONE
            );
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(
                Action,
                "Failed to update runtime permission: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Set clipboard text to [<Data>]",
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
    public void setClipboardText() {
        try {
            if (Data == null) {
                Report.updateTestLog(Action, "Clipboard text input is null", Status.FAIL);
                return;
            }
            invokeDriverMethod(
                "setClipboardText",
                new Class[] { String.class },
                new Object[] { Data }
            );
            Report.updateTestLog(Action, "Clipboard text updated", Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(
                Action,
                "Failed to set clipboard text: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Store clipboard text into runtime variable [<Data>]",
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
    public void storeClipboardText() {
        try {
            if (Data == null || Data.trim().isEmpty()) {
                Report.updateTestLog(Action, "Variable name input is empty", Status.FAIL);
                return;
            }
            Object value = invokeDriverMethod("getClipboardText", new Class[] {}, new Object[] {});
            addVar(Data, value == null ? "" : value.toString());
            Report.updateTestLog(Action, "Clipboard text stored in variable " + Data, Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(
                Action,
                "Failed to read clipboard text: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Start screen recording",
        input = InputType.NO,
        condition = InputType.NO
    )
    @Args(
        inputHelp = "no input required (e.g. leave empty)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void startScreenRecording() {
        try {
            invokeDriverMethod("startRecordingScreen", new Class[] {}, new Object[] {});
            Report.updateTestLog(Action, "Screen recording started", Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(
                Action,
                "Failed to start screen recording: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Stop screen recording and save to [<Data>] path (mp4)",
        input = InputType.YES,
        condition = InputType.OPTIONAL
    )
    @Args(
        input = ArgType.FILE_PATH,
        inputExample = "@/tmp/recording.mp4",
        inputHelp = "destination file path for recording output (e.g. @/tmp/recording.mp4)",
        condition = ConditionKind.TEXT,
        conditionExample = "%recordingBase64%",
        conditionHelp = "optional variable to store base64 output when no file path is provided (e.g. %recordingBase64%)"
    )
    public void stopScreenRecording() {
        try {
            Object result = invokeDriverMethod(
                "stopRecordingScreen",
                new Class[] {},
                new Object[] {}
            );
            String base64 = result == null ? "" : result.toString();
            if (Data == null || Data.trim().isEmpty()) {
                if (Condition != null && !Condition.trim().isEmpty()) {
                    addVar(Condition, base64);
                    Report.updateTestLog(
                        Action,
                        "Screen recording stopped and stored in variable " + Condition,
                        Status.DONE
                    );
                } else {
                    Report.updateTestLog(
                        Action,
                        "Screen recording stopped. No file path provided; output discarded",
                        Status.DEBUG
                    );
                }
                return;
            }
            byte[] bytes = Base64.getDecoder().decode(base64);
            Path target = Path.of(Data.trim());
            Files.createDirectories(target.getParent() == null ? Path.of(".") : target.getParent());
            Files.write(target, bytes);
            Report.updateTestLog(Action, "Screen recording saved to " + target, Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(
                Action,
                "Failed to stop/save screen recording: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Set device location using [<Data>] as latitude,longitude[,altitude]",
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
    public void setDeviceLocation() {
        try {
            if (Data == null || !Data.contains(",")) {
                Report.updateTestLog(
                    Action,
                    "Invalid input. Expected latitude,longitude[,altitude]",
                    Status.FAIL
                );
                return;
            }
            String[] p = Data.split(",");
            double latitude = Double.parseDouble(p[0].trim());
            double longitude = Double.parseDouble(p[1].trim());
            double altitude = p.length > 2 ? Double.parseDouble(p[2].trim()) : 0.0;
            Map<String, Double> args = Map.of(
                "latitude",
                latitude,
                "longitude",
                longitude,
                "altitude",
                altitude
            );
            if (mDriver instanceof AndroidDriver) {
                ((AndroidDriver) mDriver).executeScript("mobile: setLocation", args);
            } else if (mDriver instanceof IOSDriver) {
                ((IOSDriver) mDriver).executeScript("mobile: setLocation", args);
            } else {
                Report.updateTestLog(Action, "Unsupported driver/platform", Status.FAIL);
                return;
            }
            Report.updateTestLog(
                Action,
                "Location set to lat=" + latitude + ", lon=" + longitude + ", alt=" + altitude,
                Status.DONE
            );
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(
                Action,
                "Failed to set device location: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Store device location into runtime variable [<Data>]",
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
    public void storeDeviceLocation() {
        try {
            if (Data == null || Data.trim().isEmpty()) {
                Report.updateTestLog(Action, "Variable name input is empty", Status.FAIL);
                return;
            }
            Object location = invokeDriverMethod("getLocation", new Class[] {}, new Object[] {});
            addVar(Data, location == null ? "" : location.toString());
            Report.updateTestLog(Action, "Device location stored in variable " + Data, Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(
                Action,
                "Failed to get device location: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    @Action(
        object = ObjectType.APP,
        desc = "Drag [<Object>] to target object [<Condition>]",
        input = InputType.NO,
        condition = InputType.YES
    )
    @Args(
        inputHelp = "no input required (e.g. leave empty)",
        condition = ConditionKind.TEXT,
        conditionExample = "@value",
        conditionHelp = "condition value (e.g. @value)"
    )
    public void dragElementToObject() {
        try {
            if (Condition == null || Condition.trim().isEmpty()) {
                Report.updateTestLog(
                    Action,
                    "Target object name in Condition is empty",
                    Status.FAIL
                );
                return;
            }
            WebElement target = mObject.findElement(Condition, Reference);
            if (Element == null || target == null) {
                Report.updateTestLog(Action, "Source/target element not found", Status.FAIL);
                return;
            }
            Rectangle src = Element.getRect();
            Rectangle dst = target.getRect();
            int startX = src.x + (src.width / 2);
            int startY = src.y + (src.height / 2);
            int endX = dst.x + (dst.width / 2);
            int endY = dst.y + (dst.height / 2);
            performTouchDrag(startX, startY, endX, endY, 600);
            Report.updateTestLog(
                Action,
                "Dragged [" + ObjectName + "] to [" + Condition + "]",
                Status.DONE
            );
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(
                Action,
                "Failed to drag to target object: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Drag using coordinates from [<Data>] to [<Condition>] where each is x,y",
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
    public void dragByCoordinates() {
        try {
            if (Data == null || Condition == null) {
                Report.updateTestLog(Action, "Input/Condition cannot be empty", Status.FAIL);
                return;
            }
            int[] from = parseCoordinates(Data);
            int[] to = parseCoordinates(Condition);
            performTouchDrag(from[0], from[1], to[0], to[1], 600);
            Report.updateTestLog(
                Action,
                "Dragged from (" + from[0] + "," + from[1] + ") to (" + to[0] + "," + to[1] + ")",
                Status.DONE
            );
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(
                Action,
                "Failed to drag by coordinates: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Wait up to [<Condition>] seconds for a WebView to appear then switch to it",
        input = InputType.NO,
        condition = InputType.OPTIONAL
    )
    @Args(
        inputHelp = "no input required (e.g. leave empty)",
        condition = ConditionKind.TEXT,
        conditionExample = "@value",
        conditionHelp = "condition value (e.g. @value)"
    )
    public void waitForWebViewAndSwitch() {
        try {
            int timeoutSecs = 30;
            if (
                Condition != null &&
                !Condition.trim().isEmpty() &&
                Condition.trim().matches("[0-9]+")
            ) {
                timeoutSecs = Integer.parseInt(Condition.trim());
            }
            long deadline = System.currentTimeMillis() + timeoutSecs * 1000L;
            String webViewContext = null;
            while (System.currentTimeMillis() < deadline) {
                Set<String> contextNames = ((SupportsContextSwitching) mDriver).getContextHandles();
                webViewContext =
                    contextNames
                        .stream()
                        .filter(c -> c.startsWith("WEBVIEW"))
                        .findFirst()
                        .orElse(null);
                if (webViewContext != null) break;
                Thread.sleep(1000);
            }
            if (webViewContext != null) {
                ((SupportsContextSwitching) mDriver).context(webViewContext);
                Report.updateTestLog(
                    Action,
                    "Switched to WebView context: " + webViewContext,
                    Status.DONE
                );
            } else {
                Report.updateTestLog(
                    Action,
                    "WebView context did not appear within " + timeoutSecs + " seconds",
                    Status.FAIL
                );
            }
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(
                Action,
                "Error waiting for WebView: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    private Object invokeDriverMethod(String name, Class<?>[] types, Object[] args)
        throws Exception {
        Method m = mDriver.getClass().getMethod(name, types);
        return m.invoke(mDriver, args);
    }

    private int[] parseCoordinates(String value) {
        String[] parts = value.split(",");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid coordinates: " + value + " (expected x,y)");
        }
        return new int[] { Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()) };
    }

    private void performTouchDrag(int startX, int startY, int endX, int endY, int durationMs) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence seq = new Sequence(finger, 1);
        seq.addAction(
            finger.createPointerMove(
                Duration.ofMillis(0),
                PointerInput.Origin.viewport(),
                startX,
                startY
            )
        );
        seq.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        seq.addAction(
            finger.createPointerMove(
                Duration.ofMillis(durationMs),
                PointerInput.Origin.viewport(),
                endX,
                endY
            )
        );
        seq.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        ((RemoteWebDriver) mDriver).perform(Arrays.asList(seq));
    }
}

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
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openqa.selenium.JavascriptExecutor;

public class Scroll extends MobileGeneral {

    public Scroll(CommandControl cc) {
        super(cc);
    }

    /**
     * Normalized scroll action that works on both Android and iOS.
     *
     * Input (Data): direction - one of [up | down | left | right].
     * Condition (optional): target element identifier.
     *   - On Android: visible text to scroll to (uses UiScrollable.scrollIntoView).
     *   - On iOS: attribute=value pair (e.g. name=submitBtn) passed to mobile:scroll.
     *
     * If no Condition is supplied, a directional scroll is performed:
     *   - Android: UiScrollable.scrollForward()/scrollBackward() on a vertical or horizontal list.
     *   - iOS: mobile:scroll with the given direction.
     */
    @Action(
        object = ObjectType.MOBILE,
        desc = "Scroll [<Data>] (up|down|left|right) - platform aware",
        input = InputType.YES,
        condition = InputType.OPTIONAL
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "@down",
        inputHelp = "scroll direction: up, down, left, or right (e.g. @down)",
        condition = ConditionKind.TEXT,
        conditionExample = "@submitBtn",
        conditionHelp = "optional target: Android visible text or iOS attribute=value (e.g. @name=submitBtn)"
    )
    public void scroll() {
        try {
            String direction = (Data == null) ? "" : Data.trim().toLowerCase();
            if (!direction.matches("up|down|left|right")) {
                Report.updateTestLog(
                    Action,
                    "Invalid input [" + Data + "]. Expected one of [up|down|left|right]",
                    Status.DEBUG
                );
                return;
            }
            if (mDriver instanceof AndroidDriver) {
                scrollOnAndroid(direction);
            } else if (mDriver instanceof IOSDriver) {
                scrollOnIOS(direction);
            } else {
                Report.updateTestLog(
                    Action,
                    "Unsupported driver/platform for scroll action",
                    Status.FAIL
                );
            }
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(
                "Could not perfom [" + Action + "] action",
                "Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    private void scrollOnAndroid(String direction) {
        boolean vertical = direction.equals("up") || direction.equals("down");
        boolean forward = direction.equals("down") || direction.equals("right");
        String scrollable =
            "new UiScrollable(new UiSelector().scrollable(true))" +
            (vertical ? ".setAsVerticalList()" : ".setAsHorizontalList()");
        String uia;
        boolean hasTarget = Condition != null && !Condition.trim().isEmpty();
        if (hasTarget) {
            // scrollIntoView searches by scrolling forward; orientation is honored above.
            uia =
                scrollable +
                ".scrollIntoView(new UiSelector().text(\"" +
                Condition +
                "\").instance(0))";
        } else {
            uia = scrollable + (forward ? ".scrollForward()" : ".scrollBackward()");
        }
        mDriver.findElement(AppiumBy.androidUIAutomator(uia));
        Report.updateTestLog(
            Action,
            "Scrolled " + direction + (hasTarget ? " to '" + Condition + "'" : ""),
            Status.DONE
        );
    }

    private void scrollOnIOS(String direction) {
        HashMap<String, Object> scrollObject = new HashMap<>();
        scrollObject.put("direction", direction);
        boolean hasTarget = Condition != null && Condition.contains("=");
        if (hasTarget) {
            String[] parts = Condition.split("=", 2);
            scrollObject.put(parts[0].trim(), parts[1].trim());
        }
        ((IOSDriver) mDriver).executeScript("mobile:scroll", scrollObject);
        Report.updateTestLog(
            Action,
            "Scrolled " + direction + (hasTarget ? " to '" + Condition + "'" : ""),
            Status.DONE
        );
    }

    @Action(object = ObjectType.MOBILE, desc = "Scroll to Text in Android", input = InputType.YES)
    @Args(
        input = ArgType.TEXT,
        inputExample = "@value",
        inputHelp = "input value (e.g. @value)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void scrollInAndroid() {
        try {
            mDriver.findElement(
                AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().text(\"" +
                    Data +
                    "\").instance(0))"
                )
            );
            Report.updateTestLog(Action, "Scrolled to '" + Data + "'", Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(
                "Could not perfom [" + Action + "] action",
                "Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Scroll to Element in iOS",
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
    public void scrollInIOS() {
        try {
            HashMap<String, Object> scrollObject = new HashMap<>();
            scrollObject.put("direction", Condition.toLowerCase());
            String attribute = Data.split("=")[0];
            String value = Data.split("=")[1];
            scrollObject.put(attribute, value);
            IOSDriver driver = (IOSDriver) mDriver;
            driver.executeScript("mobile:scroll", scrollObject);
            Report.updateTestLog(Action, "Scrolled to '" + Data + "'", Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(
                "Could not perfom [" + Action + "] action",
                "Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }
}

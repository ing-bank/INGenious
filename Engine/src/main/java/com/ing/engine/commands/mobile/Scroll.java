package com.ing.engine.commands.mobile;

import com.ing.engine.core.CommandControl;
import com.ing.ingenious.api.annotation.Action;
import com.ing.ingenious.api.status.Status;
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

    /*
    private final String currentHScrollPos = "Math.max("
            + "document.body.scrollLeft, document.documentElement.scrollLeft,"
            + "document.body.offsetLeft, document.documentElement.offsetLeft,"
            + "document.body.clientLeft, document.documentElement.clientLeft)";
    private final String currentVScrollPos = "Math.max("
            + "document.body.scrollTop, document.documentElement.scrollTop,"
            + "document.body.offsetTop, document.documentElement.offsetTop,"
            + "document.body.clientTop, document.documentElement.clientTop)";
    private final String docScrollHeight = "Math.max("
            + "document.body.scrollHeight, document.documentElement.scrollHeight,"
            + "document.body.offsetHeight, document.documentElement.offsetHeight,"
            + "document.body.clientHeight, document.documentElement.clientHeight)";
    private final String docScrollWidth = "Math.max("
            + "document.body.scrollWidth, document.documentElement.scrollWidth,"
            + "document.body.offsetWidth, document.documentElement.offsetWidth,"
            + "document.body.clientWidth, document.documentElement.clientWidth)";
*/
    public Scroll(CommandControl cc) {
        super(cc);
    }

    /*
    @Action(object = ObjectType.MOBILE, desc = "Scroll horizondally to [<Data>]", input = InputType.YES)
    public void scrollHorizontallyTo() {
        if (Data != null && Data.trim().toLowerCase().matches("(left|right|\\d*)")) {
            scrollTo(getScrollData(Data), currentVScrollPos);
        } else {
            Report.updateTestLog(Action, "Invalid input[" + Data + "] It should be [(left|right|number)] ", Status.DEBUG);
        }
    }

    @Action(object = ObjectType.MOBILE, desc = "Scroll vertically to [<Data>]", input = InputType.YES)
    public void scrollVerticallyTo() {
        if (Data != null && Data.trim().toLowerCase().matches("(top|bottom|\\d*)")) {
            scrollTo(currentHScrollPos, getScrollData(Data));
        } else {
            Report.updateTestLog(Action, "Invalid input[" + Data + "] It should be [(top|bottom|number)] ", Status.DEBUG);
        }
    }

    @Action(object = ObjectType.MOBILE, desc = "Scroll to [<Data>]", input = InputType.YES)
    public void scrollTo() {
        if (Data != null && Data.trim().toLowerCase().matches("(left|right|\\d*),(top|bottom|\\d*)")) {
            scrollTo(getScrollData(Data.split(",")[0]), getScrollData(Data.split(",")[1]));
        } else {
            Report.updateTestLog(Action, "Invalid input[" + Data + "] It should be [(left|right|number),(top|bottom|number)] ", Status.DEBUG);
        }
    }

    @Action(object = ObjectType.MOBILE, desc = "Scroll to top")
    public void scrollToTop() {
        scrollTo(currentHScrollPos, "0");
    }

    @Action(object = ObjectType.MOBILE, desc = "Scroll to bottom")
    public void scrollToBottom() {
        scrollTo(currentHScrollPos, docScrollHeight);
    }

    @Action(object = ObjectType.MOBILE, desc = "Scroll to left")
    public void scrollToLeft() {
        scrollTo("0", currentVScrollPos);
    }

    @Action(object = ObjectType.MOBILE, desc = "Scroll to page")
    public void scrollToRight() {
        scrollTo(docScrollWidth, currentVScrollPos);
    }

    private String getScrollData(String val) {
        try {
            switch (val.trim().toLowerCase()) {
                case "top":
                    return "0";
                case "bottom":
                    return docScrollHeight;
                case "left":
                    return "0";
                case "right":
                    return docScrollWidth;
                default:
                    return Integer.valueOf(val).toString();
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            System.out.println("Invalid value" + val);
        }
        return null;
    }

    private void scrollTo(String x, String y) {
        if (checkIfDriverIsAlive()) {
            ((JavascriptExecutor) mDriver).executeScript("window.scrollTo(" + x + ", " + y + ");");
            Report.updateTestLog(Action, "Browser Scrolled to [" + Data + "]", Status.DONE);
        }

    }
*/
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

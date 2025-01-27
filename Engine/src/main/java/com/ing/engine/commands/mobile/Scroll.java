package com.ing.engine.commands.mobile;

import com.ing.engine.core.CommandControl;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import io.appium.java_client.AppiumBy;
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
    @Action(object = ObjectType.MOBILE, desc ="Scroll to Text in Android", input = InputType.YES)
    public void scrollInAndroid() {
        try {
            mDriver.findElement(AppiumBy.androidUIAutomator("new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().text(\"" + Data + "\"))"));
            Report.updateTestLog(Action, "Scrolled to '" + Data + "'", Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog("Could not perfom [" + Action + "] action", "Error: " + e.getMessage(), Status.FAIL);
        }
    }

    @Action(object = ObjectType.MOBILE, desc ="Scroll to Element in iOS", input = InputType.YES, condition = InputType.YES)
    public void scrollInIOS() {
        try {
            HashMap<String, Object> scrollObject = new HashMap<>();
            scrollObject.put("direction", Condition.toLowerCase());
            String attribute = Data.split("=")[0];
            String value = Data.split("=")[1];
            scrollObject.put(attribute,value);
            IOSDriver driver = (IOSDriver) mDriver;
            driver.executeScript("mobile:scroll",scrollObject);
            Report.updateTestLog(Action, "Scrolled to '" + Data + "'", Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog("Could not perfom [" + Action + "] action", "Error: " + e.getMessage(), Status.FAIL);
        }
    }

}

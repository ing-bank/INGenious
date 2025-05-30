package com.ing.engine.commands.mobile;

import com.ing.engine.constants.SystemDefaults;
import com.ing.engine.core.CommandControl;
import com.ing.engine.execution.exception.ForcedException;
import com.ing.engine.execution.exception.element.ElementException;
import com.ing.engine.execution.exception.element.ElementException.ExceptionType;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import com.ing.util.encryption.Encryption;
import io.appium.java_client.remote.SupportsContextSwitching;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

public class Basic extends MobileGeneral {

    public Basic(CommandControl cc) {
        super(cc);
    }

    @Action(object = ObjectType.APP, desc = "Tap the [<Object>] ")
    public void Tap() {
        if (elementEnabled()) {
            Element.click();
            Report.updateTestLog(Action, "Taping on " + ObjectName, Status.DONE);

        } else {
            throw new ElementException(ExceptionType.Element_Not_Enabled, ObjectName);
        }
    }

    @Action(object = ObjectType.APP, desc = "Tap the [<Object>] if it exists")
    public void TapIfExists() {
        if (Element != null) {
            Tap();
        } else {
            Report.updateTestLog(Action, "Element [" + ObjectName + "] not Exists", Status.DONE);
        }
    }

    @Action(object = ObjectType.APP, desc = "Tap the [<Object>] if it is displayed")
    public void TapIfVisible() {
        if (Element != null) {
            if (Element.isDisplayed()) {
                Tap();
            } else {
                Report.updateTestLog(Action, "Element [" + ObjectName + "] not Visible", Status.DONE);
            }
        } else {
            Report.updateTestLog(Action, "Element [" + ObjectName + "] not Exists", Status.DONE);
        }
    }

    @Action(object = ObjectType.APP, desc = "Submit action on the browser")
    public void Submit() {
        if (elementEnabled()) {
            Element.submit();
            Report.updateTestLog(Action, "[" + ObjectName + "] Submitted successfully ", Status.DONE);

        } else {
            throw new ElementException(ExceptionType.Element_Not_Enabled, ObjectName);
        }
    }

    @Action(object = ObjectType.APP, desc = "Submit the [<Object>] if it exists")
    public void SubmitIfExists() {
        if (Element != null) {
            Submit();
        } else {
            Report.updateTestLog(Action, "Element [" + ObjectName + "] not Exists", Status.DONE);
        }
    }

    @Action(object = ObjectType.APP, desc = "Enter the value [<Data>] in the Field [<Object>]", input = InputType.YES)
    public void Set() {
        if (elementEnabled()) {
            Element.clear();
            Element.sendKeys(Data);
            Report.updateTestLog(Action, "Entered Text '" + Data + "' on '"
                    + ObjectName + "'", Status.DONE);
        } else {
            throw new ElementException(ExceptionType.Element_Not_Enabled, ObjectName);
        }
    }

    @Action(object = ObjectType.APP, desc = "Enter the value [<Data>] in the [<Object>] if it exists", input = InputType.YES)
    public void SetIfExists() {
        if (Element != null) {
            Set();
        } else {
            Report.updateTestLog(Action, "Element [" + ObjectName + "] not Exists", Status.DONE);
        }
    }

    @Action(object = ObjectType.APP, desc = "Enter the value [<Data>] in the Field [<Object>] and check [<Data>] matches with [<Object>] value", input = InputType.YES)
    public void SetAndCheck() {
        if (elementEnabled()) {
            Element.clear();
            Element.sendKeys(Data);
            if (Element.getAttribute("value").equals(Data)) {
                Report.updateTestLog("Set", "Entered Text '" + Data + "' on '"
                        + ObjectName + "'", Status.DONE);
            } else {
                Report.updateTestLog("Set", "Unable Enter Text '" + Data
                        + "' on '" + ObjectName + "'", Status.FAIL);
            }
        } else {
            throw new ElementException(ExceptionType.Element_Not_Enabled, ObjectName);
        }
    }

    @Action(object = ObjectType.APP, desc = "Clear text [<Data>] from object [<Object>].")
    public void clear() {
        if (elementEnabled()) {
            Element.clear();
            Report.updateTestLog("Clear", "Cleared Text on '" + ObjectName + "'", Status.DONE);
        } else {
            throw new ElementException(ExceptionType.Element_Not_Enabled, ObjectName);
        }
    }

    @Action(object = ObjectType.APP, desc = "Enter the Decrypted value [<Data>] in the Field [<Object>]", input = InputType.YES)
    public void setEncrypted() {
        if (Data != null && Data.matches(".* Enc")) {
            if (elementEnabled()) {
                try {
                    Element.clear();
                    Data = Data.substring(0, Data.lastIndexOf(" Enc"));
                    byte[] valueDecoded = Encryption.getInstance().decrypt(Data).getBytes();
                    Element.sendKeys(new String(valueDecoded));
                    Report.updateTestLog(Action, "Entered Encrypted Text " + Data + " on " + ObjectName, Status.DONE);
                } catch (Exception ex) {
                    Report.updateTestLog("setEncrypted", ex.getMessage(), Status.FAIL);
                    Logger.getLogger(Basic.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                throw new ElementException(ExceptionType.Element_Not_Enabled, ObjectName);
            }
        } else {
            Report.updateTestLog(Action, "Data not encrypted '" + Data + "'", Status.DEBUG);
        }
    }

    @Action(object = ObjectType.APP,
            desc = "Move the Browser View to the specified element [<Object>]")
    public void moveTo() {
        if (elementDisplayed()) {
            if (Data != null && Data.matches("(\\d)+,(\\d)+")) {
                int x = Integer.valueOf(Data.split(",")[0]);
                int y = Integer.valueOf(Data.split(",")[1]);
                new Actions(mDriver).moveToElement(Element, x, y).build().perform();
            } else {
                new Actions(mDriver).moveToElement(Element).build().perform();
            }
            Report.updateTestLog(Action, "Viewport moved to" + ObjectName, Status.DONE);
        } else {
            throw new ElementException(ExceptionType.Element_Not_Visible, ObjectName);
        }
    }

    private void setPageTimeOut(int sec) {
        try {
            mDriver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(sec));
        } catch (Exception ex) {
            System.out.println("Couldn't set PageTimeOut to " + sec);
        }
    }

    @Action(object = ObjectType.MOBILE, desc = "changing wait time by [<Data>] seconds", input = InputType.YES)
    public void changeWaitTime() {
        try {
            Duration t = Duration.ofSeconds(Integer.parseInt(Data));
            if (Integer.parseInt(Data) > 0) {
                SystemDefaults.waitTime = t;
                Report.updateTestLog("changeWaitTime", "Wait time changed to "
                        + Data + " second/s", Status.DONE);
            } else {
                Report.updateTestLog("changeWaitTime",
                        "Couldn't change Wait time (invalid input)",
                        Status.DEBUG);
            }

        } catch (NumberFormatException ex) {
            Report.updateTestLog("changeWaitTime",
                    "Couldn't change Wait time ", Status.DEBUG);
            Logger.getLogger(Basic.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Action(object = ObjectType.MOBILE,
            desc = "Change Default Element finding wait time by [<Data>] seconds",
            input = InputType.YES)
    public void setElementTimeOut() {
        if (Data != null && Data.matches("[0-9]+")) {
            SystemDefaults.elementWaitTime = Duration.ofSeconds(Integer.valueOf(Data));
            Report.updateTestLog(Action, "Element Wait time changed to "
                    + Data + " second/s", Status.DONE);
        } else {
            Report.updateTestLog(Action,
                    "Couldn't change Element Wait time (invalid input) " + Data,
                    Status.DEBUG);
        }

    }
    /*

    @Action(object = ObjectType.BROWSER, desc = "Open the Url [<Data>] in the Browser", input = InputType.YES)
    public void Open() {
        Boolean pageTimeOut = false;
        try {
            if (Condition.matches("[0-9]+")) {
                setPageTimeOut(Integer.valueOf(Condition));
                pageTimeOut = true;
            }
            mDriver.get(Data);
            Report.updateTestLog("Open", "Opened Url: " + Data, Status.DONE);
        } catch (TimeoutException e) {
            Report.updateTestLog("Open",
                    "Opened Url: " + Data + " and cancelled page load after " + Condition + " seconds",
                    Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog("Open", e.getMessage(), Status.FAIL);
            throw new ForcedException("Open", e.getMessage());
        }
        if (pageTimeOut) {
            setPageTimeOut(300);
        }
    }

    @Action(object = ObjectType.BROWSER, desc = "Start a specified browser", input = InputType.YES)
    public void StartBrowser() {
        try {
            getDriverControl().StartBrowser(Data);
            Report.setWebDriver(getMobileDriverControl());
            Report.updateTestLog("StartBrowser", "Browser Started: " + Data,
                    Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog("StartBrowser", "Error: " + e.getMessage(),
                    Status.FAIL);
        }
    }

    @Action(object = ObjectType.BROWSER, desc = "Restarts the Browser")
    public void RestartBrowser() {
        try {
            getDriverControl().RestartBrowser();
            Report.setWebDriver(getMobileDriverControl());
            Report.updateTestLog("RestartBrowser", "Restarted Browser", Status.DONE);
        } catch (Exception ex) {
            Report.updateTestLog("RestartBrowser", "Unable Restart Browser",
                    Status.FAIL);
            Logger.getLogger(Basic.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    @Action(object = ObjectType.BROWSER, desc = "Stop the current browser")
    public void StopBrowser() {
        getMobileDriverControl().StopBrowser();
        Report.updateTestLog("StopBrowser", "Browser Stopped: ", Status.DONE);
    }

    private void highlightElement(WebElement element, String color) {
        JavascriptExecutor js = (JavascriptExecutor) mDriver;
        js.executeScript("arguments[0].setAttribute('style', arguments[1]);", element, " outline:" + color + " solid 2px;");
    }

    public void highlightElement(WebElement element) {
        highlightElement(element, "#f00");
    }

    @Action(object = ObjectType.APP, desc = "Highlight the element [<Object>]", input = InputType.OPTIONAL)
    public void highlight() {
        if (elementDisplayed()) {
            if (Data != null && !Data.trim().isEmpty()) {
                highlightElement(Element, Data);
            } else {
                highlightElement(Element);
            }
            Report.updateTestLog(Action, "Element Highlighted",
                    Status.PASS);
        }
    }
     */
}

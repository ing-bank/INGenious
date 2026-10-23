package com.ing.plugin.mobile;

import com.ing.ingenious.api.annotation.Action;
import com.ing.ingenious.api.contract.MobilePluginApi;
import com.ing.ingenious.api.contract.data.UserDataAccessApi;
import com.ing.ingenious.api.contract.drivers.MobileObjectApi;
import com.ing.ingenious.api.contract.reports.TestCaseReportApi;
import com.ing.ingenious.api.types.ObjectType;
import com.ing.ingenious.api.types.InputType;
import com.ing.ingenious.api.status.Status;
import com.ing.ingenious.api.exception.mobile.ElementException;
import com.ing.ingenious.api.exception.mobile.ElementException.ExceptionType;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.ios.IOSDriver;

/**
 * Mobile testing plugin for Appium-based mobile automation
 * 
 * @author Your Name
 */
public class MobileTestPlugin {
    
    MobilePluginApi gen;
    
    public String Data;
    public String Action;
    public String Input;
    public String Condition;
    public TestCaseReportApi Report;
    public UserDataAccessApi userData;
    public String ObjectName;
    
    public WebDriver mDriver;
    public WebElement Element;
    public MobileObjectApi mObject;

    public MobileTestPlugin(MobilePluginApi gen) {
        System.out.println("MobileTestPlugin initialized with MobilePluginApi: " + gen);
        this.gen = gen;
        this.Data = gen.getData();
        this.Action = gen.getAction();
        this.Input = gen.getInput();
        this.Condition = gen.getCondition();
        this.Report = gen.getReport();
        this.userData = gen.getUserData();
        this.ObjectName = gen.getObjectName();
        this.Element = (WebElement) gen.getElement();
        this.mDriver = (WebDriver) gen.getMDriver();
        this.mObject = gen.getMObject();
    }

    /**
     * Tap on mobile element
     */
    @Action(object = ObjectType.APP, desc = "Tap on [<Object>]")
    public void Tap() {
        if (gen.elementEnabled()) {
            Element.click();
            Report.updateTestLog(Action, "Tapped on " + ObjectName, Status.DONE);
        } else {
            throw new ElementException(ExceptionType.Element_Not_Enabled, ObjectName);
        }
    }

    /**
     * Set value in mobile element
     */
    @Action(object = ObjectType.APP, desc = "Enter the value [<Data>] in [<Object>]", input = InputType.YES)
    public void Set() {
        if (gen.elementEnabled()) {
            Element.sendKeys(Data);
            Report.updateTestLog(Action, "Entered '" + Data + "' in '" + ObjectName + "'", Status.DONE);
        } else {
            throw new ElementException(ExceptionType.Element_Not_Enabled, ObjectName);
        }
    }

    /**
     * Scroll to text in Android
     */
    @Action(object = ObjectType.MOBILE, desc = "Scroll to Text [<Data>] in Android", input = InputType.YES)
    public void scrollInAndroid() {
        try {
            mDriver.findElement(AppiumBy.androidUIAutomator(
                "new UiScrollable(new UiSelector().scrollable(true))" +
                ".scrollIntoView(new UiSelector().text(\"" + Data + "\").instance(0))"
            ));
            Report.updateTestLog(Action, "Scrolled to '" + Data + "'", Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog("Could not perform [" + Action + "] action", "Error: " + e.getMessage(), Status.FAIL);
        }
    }

    /**
     * Scroll to element in iOS
     */
    @Action(object = ObjectType.MOBILE, desc = "Scroll to Text [<Data>] in IOS", input = InputType.YES)
    public void scrollInIOS() {
        try {
            HashMap<String, String> scrollObject = new HashMap<>();
            scrollObject.put("direction", "down");
            scrollObject.put("name", Data);
            ((IOSDriver) mDriver).executeScript("mobile: scroll", scrollObject);
            Report.updateTestLog(Action, "Scrolled to '" + Data + "'", Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog("Could not perform [" + Action + "] action", "Error: " + e.getMessage(), Status.FAIL);
        }
    }

    /**
     * Assert element is displayed
     */
    @Action(object = ObjectType.APP, desc = "Assert if [<Object>] is displayed", condition = InputType.OPTIONAL)
    public void assertElementDisplayed() {
        try {
            if (Element.isDisplayed()) {
                Report.updateTestLog(Action, "Element [" + ObjectName + "] is displayed", Status.PASSNS);
            } else {
                Report.updateTestLog(Action, "Element [" + ObjectName + "] is not displayed", Status.FAILNS);
            }
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(Action, "Element [" + ObjectName + "] not found. Error: " + e.getMessage(), Status.FAILNS);
        }
    }

    // ========== Helper Methods ==========

    public boolean elementPresent() {
        return gen.checkIfDriverIsAlive() && Element != null;
    }
}

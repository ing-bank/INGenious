package com.ing.engine.commands.mobile;

import com.ing.engine.commands.browser.Command;
import com.ing.engine.constants.SystemDefaults;
import com.ing.engine.core.CommandControl;
import com.ing.engine.execution.exception.ForcedException;
import com.ing.engine.execution.exception.element.ElementException;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class WaitFor extends Command {

    enum WaitType {

        VISIBLE,
        INVISIBLE,
        CLICKABLE,
        SELECTED,
        TEXT_CONTAINS,
        VALUE_CONTAINS,
        TITLE_IS,
        TITLE_CONTAINS,
        EL_SELECT_TRUE,
        EL_SELECT_FALSE,
        ALERT_PRESENT,
        FRAME_EL,
        FRAME_STR,
        FRAME_IND,
        CUSTOM_SCRIPT
    };

    public WaitFor(CommandControl cc) {
        super(cc);
    }

    /*
    @Action(object = ObjectType.APP, desc = "Tap the [<Object>] and Wait for Page to be loaded", condition = InputType.OPTIONAL)
    public void TapAndWait() {
        if (Element != null) {
            Element.click();
            waitForPageLoaded();
            Report.updateTestLog(Action, "Tap and wait for page load is done",
                    Status.DONE);
        } else {
            throw new ElementException(ElementException.ExceptionType.Element_Not_Found, Condition);
        }
    }

    @Action(object = ObjectType.MOBILE, desc = "Wait for Page to be loaded", condition = InputType.OPTIONAL)
    public void waitForPageLoaded() {
        waitFor(WaitType.CUSTOM_SCRIPT,
                "Page load completed in stipulated time",
                "return document.readyState==='complete'");
    }
   */
    
    @Action(object = ObjectType.MOBILE, desc = "Wait for alert to be present ", condition = InputType.OPTIONAL)
    public void waitForAlertPresent() {
        waitFor(WaitType.ALERT_PRESENT,
                "Alert popped up in stipulated time");
    }

    @Action(object = ObjectType.APP, desc = "Wait for [<Object>] to be visible ", condition = InputType.OPTIONAL)
    public void waitForAppElementToBeVisible() {
        waitForElement(WaitType.VISIBLE, "'"
                + this.ObjectName
                + "' Element becomes visible in stipulated time");
    }

    @Action(object = ObjectType.APP, desc = "Wait for [<Object>] to be invisible ", condition = InputType.OPTIONAL)
    public void waitForElementToBeInVisible() {
        waitForElement(WaitType.INVISIBLE, "'"
                + this.ObjectName
                + "' Element becomes invisible in stipulated time");
    }

    @Action(object = ObjectType.APP, desc = "Wait for [<Object>] to be Tapable ", condition = InputType.OPTIONAL)
    public void waitForElementToBeTapable() {
        waitForElement(WaitType.CLICKABLE, "'"
                + this.ObjectName
                + "' Element becomes Tapable in stipulated time");
    }

    @Action(object = ObjectType.APP, desc = "Wait for [<Object>] to be selected ", condition = InputType.OPTIONAL)
    public void waitForElementToBeSelected() {
        waitForElement(WaitType.SELECTED, "'"
                + this.ObjectName
                + "' Element Selected in stipulated time");
    }

    @Action(object = ObjectType.APP, desc = "Wait for element: [<Object>] to contain text [<Data>]", condition = InputType.OPTIONAL, input = InputType.YES)
    public void waitForElementToContainText() {
        waitForElement(WaitType.TEXT_CONTAINS, "'"
                + this.ObjectName + "' Element contained the text: "
                + Data + " in stipulated Time");
    }

    @Action(object = ObjectType.APP, desc = "Wait for [<Object>] element to contain value: [<Data>]", condition = InputType.OPTIONAL, input = InputType.YES)
    public void waitForElementToContainValue() {
        waitForElement(WaitType.VALUE_CONTAINS, "'"
                + this.ObjectName + "' Element contained the value: "
                + Data + " in stipulated Time");
    }

    @Action(object = ObjectType.APP, desc = "Wait for [<Object>] element to be selected: [<Data>]", condition = InputType.OPTIONAL)
    public void waitForElementSelectionToBeTrue() {
        waitForElement(WaitType.EL_SELECT_TRUE, "'"
                + this.ObjectName
                + "' Element got Selected in the stipulated time");
    }

    @Action(object = ObjectType.APP, desc = "Wait for [<Object>] element to be deselected", condition = InputType.OPTIONAL)
    public void waitForElementSelectionToBeFalse() {
        waitForElement(WaitType.EL_SELECT_FALSE, "'"
                + this.ObjectName
                + "' Element got Deselected in the stipulated time");
    }

    @Action(object = ObjectType.MOBILE, desc = "Wait for page's title to be [<Data>]", input = InputType.YES, condition = InputType.OPTIONAL)
    public void waitForTitleToBe() {
        waitFor(WaitType.TITLE_IS,
                "Title Equals '"
                + Data + "' in stipulated Time");
    }

    @Action(object = ObjectType.MOBILE, desc = "Wait for page's title to contain [<Data>]", condition = InputType.OPTIONAL, input = InputType.YES)
    public void waitForTitleToContain() {
        waitFor(WaitType.TITLE_CONTAINS,
                "Title Contains the value '"
                + Data + "' in stipulated Time");
    }

    /*
    @Action(object = ObjectType.MOBILE, desc = "Wait till the given javascript condition [<Data>] returns true", input = InputType.YES, condition = InputType.OPTIONAL)
    public void waitTillCustomScript() {
        if (Data != null && !Data.trim().isEmpty()) {
            if (Data.contains("return")) {
                waitFor(WaitType.CUSTOM_SCRIPT,
                        "Condition passed in stipulated time",
                        Data);
            } else {
                Report.updateTestLog(Action, "Javascript condition should have atleast one return and the condtion should return Boolean value", Status.DEBUG);
            }
        } else {
            Report.updateTestLog(Action, "Include a proper javascript condition to check", Status.DEBUG);
        }
    }
    */
    
    @Action(object = ObjectType.APP, desc = "Wait  for the element [<Object>] to be present", condition = InputType.OPTIONAL)
    public void waitForElementToBePresent() {
        MObject.setWaitTime(getWaitTime());
        try {
            Element = mObject.findElement(ObjectName, Reference);
            MObject.resetWaitTime();
            if (Element != null) {
                Report.updateTestLog(Action, "'" + this.ObjectName
                        + "' Element Present in the stipulated time", Status.PASS);
            } else {
                throw new ElementException(ElementException.ExceptionType.Element_Not_Found, ObjectName);
            }

        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            throw new ForcedException(Action,
                    ex.getMessage());
        }
    }

    @Action(object = ObjectType.APP, desc = "Wait for Frame To Be Available and Switch to it", input = InputType.OPTIONAL,
            condition = InputType.OPTIONAL)
    public void waitForFrameAndSwitch() {
        if (Element != null) {
            waitFor(WaitType.FRAME_EL, "Switched to Frame By Object '"
                    + ObjectName + "' in stipulated Time");
        } else if (Data != null) {
            if (Data.matches("[0-9]+")) {
                waitFor(WaitType.FRAME_IND, "Switched to Frame By Index '"
                        + Data + "' in stipulated Time");
            } else {
                waitFor(WaitType.FRAME_STR, "Switched to Frame By Value '"
                        + Data + "' in stipulated Time");
            }
        }
    }

    private void waitForElement(WaitType command, String message) {
        if (Element != null) {
            waitFor(command, message);
        } else {
            throw new ElementException(ElementException.ExceptionType.Element_Not_Found, Condition);
        }
    }

    private void waitFor(WaitType command, String message) {
        waitFor(command, message, null);
    }

    private void waitFor(WaitType command, String message, String customScript) {
        Duration time = getWaitTime();
        WebDriverWait wait = new WebDriverWait(mDriver, time);
        try {
            waitFor(wait, command, customScript);
            Report.updateTestLog(Action, message, Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Couldn't wait for action to complete in given time - " + time + " seconds", Status.DEBUG);
        }
    }

    private Duration getWaitTime() {
        if (Condition != null && Condition.matches("[0-9]+")) {
            return Duration.ofSeconds(Integer.valueOf(Condition));
        } else {
            return SystemDefaults.waitTime;
        }
    }

    private void waitFor(WebDriverWait wait, WaitType command, String customScript) {
        switch (command) {
            case VISIBLE:
                wait.until(ExpectedConditions.visibilityOf(Element));
                break;
            case INVISIBLE:
                wait.until(ExpectedConditions.not(ExpectedConditions.visibilityOf(Element)));
                break;
            case CLICKABLE:
                wait.until(ExpectedConditions.elementToBeClickable(Element));
                break;
            case SELECTED:
                wait.until(ExpectedConditions.elementToBeSelected(Element));
                break;
            case TITLE_IS:
                wait.until(ExpectedConditions.titleIs(Data));
                break;
            case TITLE_CONTAINS:
                wait.until(ExpectedConditions.titleContains(Data));
                break;
            case TEXT_CONTAINS:
                wait.until(ExpectedConditions.textToBePresentInElement(Element, Data));
                break;
            case VALUE_CONTAINS:
                wait.until(ExpectedConditions.textToBePresentInElementValue(Element, Data));
                break;
            case EL_SELECT_TRUE:
                wait.until(ExpectedConditions.elementSelectionStateToBe(Element, true));
                break;
            case EL_SELECT_FALSE:
                wait.until(ExpectedConditions.elementSelectionStateToBe(Element, false));
                break;
            case ALERT_PRESENT:
                wait.until(ExpectedConditions.alertIsPresent());
                break;
            case CUSTOM_SCRIPT:
                wait.until(getCustomCondition(customScript));
                break;
            case FRAME_EL:
                wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(Element));
                break;
            case FRAME_IND:
                wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(Integer.valueOf(Data, 0)));
                break;
            case FRAME_STR:
                wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(Data));
                break;
            default:
                throw new UnsupportedOperationException();
        }
    }
 

    private ExpectedCondition<?> getCustomCondition(final String javascript) {
        return new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver driver) {
                return (Boolean) ((JavascriptExecutor) driver).executeScript(javascript);
            }
        };
    }

}

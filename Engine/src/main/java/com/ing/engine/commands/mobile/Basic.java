package com.ing.engine.commands.mobile;

import com.ing.engine.constants.SystemDefaults;
import com.ing.engine.core.CommandControl;
import com.ing.ingenious.api.annotation.Action;
import com.ing.ingenious.api.annotation.Args;
import com.ing.ingenious.api.exception.ForcedException;
import com.ing.ingenious.api.exception.mobile.ElementException;
import com.ing.ingenious.api.exception.mobile.ElementException.ExceptionType;
import com.ing.ingenious.api.status.Status;
import com.ing.ingenious.api.types.ArgType;
import com.ing.ingenious.api.types.ConditionKind;
import com.ing.ingenious.api.types.InputType;
import com.ing.ingenious.api.types.ObjectType;
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
    @Args(
        inputHelp = "no input required (e.g. leave empty)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void Tap() {
        if (elementEnabled()) {
            Element.click();
            Report.updateTestLog(Action, "Taping on " + ObjectName, Status.DONE);
        } else {
            throw new ElementException(ExceptionType.Element_Not_Enabled, ObjectName);
        }
    }

    @Action(object = ObjectType.APP, desc = "Tap the [<Object>] if it exists")
    @Args(
        inputHelp = "no input required (e.g. leave empty)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void TapIfExists() {
        if (Element != null) {
            Tap();
        } else {
            Report.updateTestLog(Action, "Element [" + ObjectName + "] not Exists", Status.DONE);
        }
    }

    @Action(object = ObjectType.APP, desc = "Tap the [<Object>] if it is displayed")
    @Args(
        inputHelp = "no input required (e.g. leave empty)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void TapIfVisible() {
        if (Element != null) {
            if (Element.isDisplayed()) {
                Tap();
            } else {
                Report.updateTestLog(
                    Action,
                    "Element [" + ObjectName + "] not Visible",
                    Status.DONE
                );
            }
        } else {
            Report.updateTestLog(Action, "Element [" + ObjectName + "] not Exists", Status.DONE);
        }
    }

    @Action(object = ObjectType.APP, desc = "Submit action on the browser")
    @Args(
        inputHelp = "no input required (e.g. leave empty)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void Submit() {
        if (elementEnabled()) {
            Element.submit();
            Report.updateTestLog(
                Action,
                "[" + ObjectName + "] Submitted successfully ",
                Status.DONE
            );
        } else {
            throw new ElementException(ExceptionType.Element_Not_Enabled, ObjectName);
        }
    }

    @Action(object = ObjectType.APP, desc = "Submit the [<Object>] if it exists")
    @Args(
        inputHelp = "no input required (e.g. leave empty)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void SubmitIfExists() {
        if (Element != null) {
            Submit();
        } else {
            Report.updateTestLog(Action, "Element [" + ObjectName + "] not Exists", Status.DONE);
        }
    }

    @Action(
        object = ObjectType.APP,
        desc = "Enter the value [<Data>] in the Field [<Object>]",
        input = InputType.YES
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "@value",
        inputHelp = "input value (e.g. @value)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void Set() {
        if (elementEnabled()) {
            Element.clear();
            Element.sendKeys(Data);
            Report.updateTestLog(
                Action,
                "Entered Text '" + Data + "' on '" + ObjectName + "'",
                Status.DONE
            );
        } else {
            throw new ElementException(ExceptionType.Element_Not_Enabled, ObjectName);
        }
    }

    @Action(
        object = ObjectType.APP,
        desc = "Enter the value [<Data>] in the [<Object>] if it exists",
        input = InputType.YES
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "@value",
        inputHelp = "input value (e.g. @value)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void SetIfExists() {
        if (Element != null) {
            Set();
        } else {
            Report.updateTestLog(Action, "Element [" + ObjectName + "] not Exists", Status.DONE);
        }
    }

    @Action(
        object = ObjectType.APP,
        desc = "Enter the value [<Data>] in the Field [<Object>] and check [<Data>] matches with [<Object>] value",
        input = InputType.YES
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "@value",
        inputHelp = "input value (e.g. @value)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void SetAndCheck() {
        if (elementEnabled()) {
            Element.clear();
            Element.sendKeys(Data);
            if (Element.getAttribute("value").equals(Data)) {
                Report.updateTestLog(
                    "Set",
                    "Entered Text '" + Data + "' on '" + ObjectName + "'",
                    Status.DONE
                );
            } else {
                Report.updateTestLog(
                    "Set",
                    "Unable Enter Text '" + Data + "' on '" + ObjectName + "'",
                    Status.FAIL
                );
            }
        } else {
            throw new ElementException(ExceptionType.Element_Not_Enabled, ObjectName);
        }
    }

    @Action(object = ObjectType.APP, desc = "Clear text [<Data>] from object [<Object>].")
    @Args(
        inputHelp = "no input required (e.g. leave empty)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void clear() {
        if (elementEnabled()) {
            Element.clear();
            Report.updateTestLog("Clear", "Cleared Text on '" + ObjectName + "'", Status.DONE);
        } else {
            throw new ElementException(ExceptionType.Element_Not_Enabled, ObjectName);
        }
    }

    @Action(
        object = ObjectType.APP,
        desc = "Enter the Decrypted value [<Data>] in the Field [<Object>]",
        input = InputType.YES
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "@value",
        inputHelp = "input value (e.g. @value)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void setEncrypted() {
        if (Data != null && Data.matches(".* Enc")) {
            if (elementEnabled()) {
                try {
                    Element.clear();
                    Data = Data.substring(0, Data.lastIndexOf(" Enc"));
                    byte[] valueDecoded = Encryption.getInstance().decrypt(Data).getBytes();
                    Element.sendKeys(new String(valueDecoded));
                    Report.updateTestLog(
                        Action,
                        "Entered Encrypted Text " + Data + " on " + ObjectName,
                        Status.DONE
                    );
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

    @Action(
        object = ObjectType.APP,
        desc = "Move the Browser View to the specified element [<Object>]"
    )
    @Args(
        inputHelp = "no input required (e.g. leave empty)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
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

    @Action(
        object = ObjectType.MOBILE,
        desc = "changing wait time by [<Data>] seconds",
        input = InputType.YES
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "@value",
        inputHelp = "input value (e.g. @value)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void changeWaitTime() {
        try {
            Duration t = Duration.ofSeconds(Integer.parseInt(Data));
            if (Integer.parseInt(Data) > 0) {
                SystemDefaults.waitTime = t;
                Report.updateTestLog(
                    "changeWaitTime",
                    "Wait time changed to " + Data + " second/s",
                    Status.DONE
                );
            } else {
                Report.updateTestLog(
                    "changeWaitTime",
                    "Couldn't change Wait time (invalid input)",
                    Status.DEBUG
                );
            }
        } catch (NumberFormatException ex) {
            Report.updateTestLog("changeWaitTime", "Couldn't change Wait time ", Status.DEBUG);
            Logger.getLogger(Basic.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Change Default Element finding wait time by [<Data>] seconds",
        input = InputType.YES
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "@value",
        inputHelp = "input value (e.g. @value)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void setElementTimeOut() {
        if (Data != null && Data.matches("[0-9]+")) {
            SystemDefaults.elementWaitTime = Duration.ofSeconds(Integer.valueOf(Data));
            Report.updateTestLog(
                Action,
                "Element Wait time changed to " + Data + " second/s",
                Status.DONE
            );
        } else {
            Report.updateTestLog(
                Action,
                "Couldn't change Element Wait time (invalid input) " + Data,
                Status.DEBUG
            );
        }
    }
}

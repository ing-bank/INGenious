package com.ing.engine.mobileCommands;

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
import java.time.Duration;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.codec.binary.Base64;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

public class Basic extends MobileGeneral {

    public Basic(CommandControl cc) {
        super(cc);
    }

    @Action(object = ObjectType.MOBILE, desc = "Click the [<Object>] ")
    public void Tap() {
        if (elementEnabled()) {
            Element.click();
            Report.updateTestLog(Action, "Clicking on " + ObjectName, Status.DONE);
        } else{
            throw new ElementException(ExceptionType.Element_Not_Enabled, ObjectName);
        }
    }

    @Action(object = ObjectType.MOBILE, desc = "Click the [<Object>] if it exists")
    public void TapIfExists() {
        if (Element != null) {
            Tap();
        } else {
            Report.updateTestLog(Action, "Element [" + ObjectName + "] not Exists", Status.DONE);
        }
    }

    @Action(object = ObjectType.MOBILE, desc = "Click the [<Object>] if it is displayed")
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

    @Action(object = ObjectType.MOBILE, desc = "Submit action on the browser")
    public void Submit() {
        if (elementEnabled()) {
            Element.submit();
            Report.updateTestLog(Action, "[" + ObjectName + "] Submitted successfully ", Status.DONE);

        } else {
            throw new ElementException(ExceptionType.Element_Not_Enabled, ObjectName);
        }
    }

    @Action(object = ObjectType.MOBILE, desc = "Submit the [<Object>] if it exists")
    public void SubmitIfExists() {
        if (Element != null) {
            Submit();
        } else {
            Report.updateTestLog(Action, "Element [" + ObjectName + "] not Exists", Status.DONE);
        }
    }

    @Action(object = ObjectType.MOBILE, desc = "Enter the value [<Data>] in the Field [<Object>]", input = InputType.YES)
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

    @Action(object = ObjectType.MOBILE, desc = "Enter the value [<Data>] in the [<Object>] if it exists", input = InputType.YES)
    public void SetIfExists() {
        if (Element != null) {
            Set();
        } else {
            Report.updateTestLog(Action, "Element [" + ObjectName + "] not Exists", Status.DONE);
        }
    }

    @Action(object = ObjectType.MOBILE, desc = "Enter the value [<Data>] in the Field [<Object>] and check [<Data>] matches with [<Object>] value", input = InputType.YES)
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

    @Action(object = ObjectType.MOBILE, desc = "Clear text [<Data>] from object [<Object>].")
    public void clear() {
        if (elementEnabled()) {
            Element.clear();
            Report.updateTestLog("Clear", "Cleared Text on '" + ObjectName + "'", Status.DONE);
        } else {
            throw new ElementException(ExceptionType.Element_Not_Enabled, ObjectName);
        }
    }

    @Action(object = ObjectType.MOBILE, desc = "Enter the Decrypted value [<Data>] in the Field [<Object>]", input = InputType.YES)
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

    @Action(object = ObjectType.MOBILE,
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

    @Action(object = ObjectType.BROWSER, desc = "This a dummy function helpful with testing.")
    public void filler() {

    }


    private void setPageTimeOut(int sec) {
        try {
            mDriver.manage().timeouts().pageLoadTimeout(sec, TimeUnit.SECONDS);
        } catch (Exception ex) {
            System.out.println("Couldn't set PageTimeOut to " + sec);
        }
    }
    
    @Action(object = ObjectType.BROWSER, desc = "Add a variable to access within testcase", input = InputType.YES, condition = InputType.YES)
    public void AddVar() {
        if (Input.startsWith("=Replace(")) {
            replaceFunction();
        } else if (Input.startsWith("=Split(")) {
            splitFunction();
        } else if (Input.startsWith("=Substring(")) {
            subStringFunction();
        } else {
            addVar(Condition, Data);
        }

        if (getVar(Condition) != null) {
            Report.updateTestLog("addVar", "Variable " + Condition + " added with value " + Data, Status.DONE);
        } else {
            Report.updateTestLog("addVar", "Variable " + Condition + " not added ", Status.DEBUG);
        }
    }

    @Action(object = ObjectType.BROWSER, desc = "Add a Global variable to access across test set", input = InputType.YES, condition = InputType.YES)
    public void AddGlobalVar() {
        addGlobalVar(Condition, Data);
        if (getVar(Condition) != null) {
            Report.updateTestLog(Action, "Variable " + Condition
                    + " added with value " + Data, Status.DONE);
        } else {
            Report.updateTestLog(Action, "Variable " + Condition
                    + " not added ", Status.DEBUG);
        }
    }

    @Action(object = ObjectType.BROWSER, desc = "changing wait time by [<Data>] seconds", input = InputType.YES)
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

    @Action(object = ObjectType.BROWSER,
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

    @Action(object = ObjectType.MOBILE, desc = "Highlight the element [<Object>]", input = InputType.OPTIONAL)
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

    private void highlightElement(WebElement element, String color) {
        JavascriptExecutor js = (JavascriptExecutor) mDriver;
        js.executeScript("arguments[0].setAttribute('style', arguments[1]);", element, " outline:" + color + " solid 2px;");
    }

    public void highlightElement(WebElement element) {
        highlightElement(element, "#f00");
    }

    public void replaceFunction() {
        String op = "";
        String original = "";
        String targetString = "";
        String replaceString = "";
        String occurance = "";
        String[] args2 = null;
        String args1 = Input.split("Replace\\(")[1];
        if (args1.substring(args1.length() - 1).equals(":")) {
            args2 = args1.substring(0, args1.length() - 2).split(",'");
        } else {
            args2 = args1.substring(0, args1.length() - 1).split(",'");
        }
        targetString = args2[1].substring(0, args2[1].length() - 1);
        replaceString = args2[2].split("',")[0];
        occurance = args2[2].split("',")[1];
        Pattern pattern = Pattern.compile("%.*%");
        Matcher matcher = pattern.matcher(args2[0]);
        if (matcher.find()) {
            original = getVar(args2[0]);
        } else {
            original = args2[0].substring(1, args2[0].length() - 1);
        }
        try {
            if (args2.length > 0) {
                if (occurance.toLowerCase().equals("first")) {
                    System.out.println("original " + original);
                    System.out.println("targetString " + targetString);
                    System.out.println("replaceString " + replaceString);
                    System.out.println("occurance " + occurance);

                    op = original.replaceFirst(targetString, replaceString);
                } else {
                    System.out.println("original " + original);
                    System.out.println("targetString " + targetString);
                    System.out.println("replaceString " + replaceString);
                    System.out.println("occurance " + occurance);
                    op = original.replace(targetString, replaceString);
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        addVar(Condition, op);
    }

    public void splitFunction() {
        try {
            String op = "";
            String original = "";
            String regex = "";
            String stringIndex = "";
            String splitLength = "";
            String[] args2 = null;
            String[] stringSplit = null;
            String args1 = Input.split("Split\\(")[1];

            if (args1.substring(args1.length() - 1).equals(":")) {
                args2 = args1.substring(0, args1.length() - 2).split(",'");
            } else {
                args2 = args1.substring(0, args1.length() - 1).split(",'");
            }
            regex = args2[1].split("',")[0];
            int arrayLength = args2.length;
            Pattern pattern = Pattern.compile("%.*%");
            Matcher matcher = pattern.matcher(args2[0]);
            if (matcher.find()) {
                original = getVar(args2[0]);
            } else {
                original = args2[0].substring(1, args2[0].length() - 1);
            }
            if (!(args2[1].split("',")[1]).contains(",")) {
                stringIndex = args2[1].split("',")[1];
                stringSplit = original.split(regex);
            } else {
                stringIndex = args2[1].split("',")[1].split(",")[1];
                splitLength = args2[1].split("',")[1].split(",")[0];
                stringSplit = original.split(regex, Integer.parseInt(splitLength));
            }
            op = stringSplit[Integer.parseInt(stringIndex)];
            addVar(Condition, op);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void subStringFunction() {
        try {
            String op = "";
            String original = "";
            String startIndex = "";
            String endIndex = "";
            String[] args2 = null;
            String args1 = Input.split("Substring\\(")[1];

            if (args1.substring(args1.length() - 1).equals(":")) {
                args2 = args1.substring(0, args1.length() - 2).split(",");
            } else {
                args2 = args1.substring(0, args1.length() - 1).split(",");
            }
            Pattern pattern = Pattern.compile("%.*%");
            Matcher matcher = pattern.matcher(args2[0]);
            if (matcher.find()) {
                original = getVar(args2[0]);
                startIndex = args2[1];
                if (args2.length == 3) {
                    endIndex = args2[2];
                }
            } else {
                String[] args3 = args1.substring(0, args1.length() - 2).split("',");
                original = args3[0].substring(1, args2[0].length() - 1);
                if (args3[1].contains(",")) {
                    startIndex = args3[1].split(",")[0];
                    endIndex = args3[1].split(",")[1];
                } else {
                    startIndex = args3[1];
                }
            }

            if (endIndex.equals("")) {
                op = original.substring(Integer.parseInt(startIndex));
            } else {
                op = original.substring(Integer.parseInt(startIndex), Integer.parseInt(endIndex));
            }
            addVar(Condition, op);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


package com.ing.engine.mobileCommands;

import com.ing.engine.core.CommandControl;
import com.ing.engine.drivers.MobileObject;
import com.ing.engine.execution.exception.ForcedException;
import com.ing.engine.execution.exception.element.ElementException;
import com.ing.engine.execution.exception.element.ElementException.ExceptionType;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.Select;

public class CommonMethods extends MobileGeneral {

    public CommonMethods(CommandControl cc) {
        super(cc);
    }

    @Action(object = ObjectType.BROWSER, desc = "Refresh current page ")
    public void refreshDriver() {
        try {
            mDriver.navigate().refresh();
            Report.updateTestLog("refreshDriver", "Page is refreshed",
                    Status.DONE);
        } catch (WebDriverException e) {
            Report.updateTestLog("refreshDriver", e.getMessage(), Status.FAIL);
            Logger.getLogger(CommonMethods.class.getName()).log(Level.SEVERE, null, e);
        }

    }

    @Action(object = ObjectType.BROWSER, desc = "Navigate to previous page")
    public void back() {
        try {
            mDriver.navigate().back();
            Report.updateTestLog("back", "Navigate page back is success",
                    Status.DONE);
        } catch (WebDriverException e) {
            Report.updateTestLog("back", e.getMessage(), Status.FAIL);
            Logger.getLogger(CommonMethods.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    @Action(object = ObjectType.BROWSER, desc = "browser navigates to next page.")
    public void forward() {
        try {
            mDriver.navigate().forward();
            Report.updateTestLog("forward", "Navigate page forward is success",
                    Status.DONE);
        } catch (WebDriverException e) {
            Report.updateTestLog("forward", e.getMessage(), Status.FAIL);
            Logger.getLogger(CommonMethods.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    @Action(object = ObjectType.BROWSER, desc = "Close the current browser session")
    public void close() {
        try {
            mDriver.close();
            Report.updateTestLog("close", "Selenium Webdriver is closed",
                    Status.DONE);
        } catch (WebDriverException e) {
            Report.updateTestLog("close", e.getMessage(), Status.FAIL);
            Logger.getLogger(CommonMethods.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    @Action(desc = "To Perform Right Click action on WebPage/Element")
    public void rightClick() {
        String desc = "Right click action performed on ";
        Actions action = new Actions(mDriver);
        if (Element != null) {
            action.contextClick(Element).build().perform();
            desc += "Element - " + ObjectName;
        } else {
            action.contextClick().build().perform();
            desc += "Webpage";
        }
        Report.updateTestLog(Action, desc, Status.DONE);
    }

    @Action(object = ObjectType.MOBILE, desc = "Double click [<Object>] element")
    public void doubleClickElement() {
        if (elementEnabled()) {
            new Actions(mDriver).doubleClick(Element).build().perform();
            Report.updateTestLog("doubleClickElement", "'" + Element
                    + "' is doubleClicked", Status.DONE);
        } else {
            throw new ElementException(ExceptionType.Element_Not_Enabled, ObjectName);
        }
    }

    @Action(object = ObjectType.MOBILE, desc = "Hover over the [<Object>] element")
    public void mouseOverElement() {
        if (elementPresent()) {
            new Actions(mDriver).moveToElement(Element).build().perform();
            Report.updateTestLog(Action, "Mouse Over to Element '" + ObjectName,
                    Status.DONE);
        } else {
            throw new ElementException(ElementException.ExceptionType.Element_Not_Found, ObjectName);
        }
    }
/* check later
    
    @Action(object = ObjectType.MOBILE, desc = "Drags the [<Object>]")
    public void dragElement() {
        if (elementPresent()) {
            getRunTimeElement().push(Element);
            Report.updateTestLog(Action, "'" + ObjectName
                    + "' dragged", Status.DONE);
        } else {
            throw new ElementException(ElementException.ExceptionType.Element_Not_Found, ObjectName);
        }
    }

    @Action(object = ObjectType.MOBILE, desc = "Drops the Dragged Object to [<Object>]")
    public void dropElement() {
        if (elementPresent()) {
            if (!getRunTimeElement().empty()) {
                new Actions(mDriver)
                        .dragAndDrop(getRunTimeElement().pop(), Element)
                        .build().perform();
                Report.updateTestLog(Action, "Element  dropped to '"
                        + ObjectName + "' ", Status.DONE);
            } else {
                throw new ElementException(ElementException.ExceptionType.Element_Not_Found, "Drop Target");
            }
        } else {
            throw new ElementException(ElementException.ExceptionType.Element_Not_Found, ObjectName);
        }
    }
    
    */

    @Action(object = ObjectType.MOBILE, desc = "Drad and drop the element [<Object>] at location [<Data>]", input = InputType.YES)
    public void dragAndDropBy() {
        try {
            if (elementPresent()) {
                String[] coords = Data.split(",", 2);
                new Actions(mDriver).dragAndDropBy(Element, Integer.parseInt(coords[0]), Integer.parseInt(coords[1]))
                        .build().perform();
                Report.updateTestLog(Action, "Element [" + ObjectName + "] dropped at '"
                        + Data + "' ", Status.PASS);

            } else {
                throw new ElementException(ElementException.ExceptionType.Element_Not_Found, ObjectName);
            }
        } catch (Exception e) {
            Report.updateTestLog(Action, e.getMessage(), Status.DEBUG);
            Logger.getLogger(CommonMethods.class.getName()).log(Level.SEVERE, null, e);
        }

    }

    @Action(object = ObjectType.MOBILE, desc = "drag and drop operation of ", input = InputType.YES)
    public void dragToAndDropElement() {
        try {
            String Page = Data.split(":", 2)[0];
            String Object = Data.split(":", 2)[1];
            if (elementPresent()) {
                WebElement DropElement = mObject.findElement(Object, Page);
                if (DropElement != null) {
                    new Actions(mDriver).dragAndDrop(Element, DropElement)
                            .build().perform();
                    Report.updateTestLog(Action, "'" + ObjectName
                            + "' has been dragged and dropped to '" + Object + "'",
                            Status.PASS);
                } else {
                    throw new ElementException(ElementException.ExceptionType.Element_Not_Found, Object);
                }
            } else {
                throw new ElementException(ElementException.ExceptionType.Element_Not_Found, ObjectName);
            }
        } catch (Exception e) {
            Report.updateTestLog(Action, e.getMessage(), Status.FAIL);
            Logger.getLogger(CommonMethods.class.getName()).log(Level.SEVERE, e.getMessage(), e);
        }
    }

    @Action(object = ObjectType.MOBILE, desc = "Click and hold the [<Object>] element ")
    public void clickAndHoldElement() {
        if (elementEnabled()) {
            new Actions(mDriver).clickAndHold(Element).build().perform();
            Report.updateTestLog(Action, "Click and hold done", Status.DONE);
        } else {
            throw new ElementException(ElementException.ExceptionType.Element_Not_Enabled, ObjectName);
        }
    }

    @Action(object = ObjectType.MOBILE,
            desc = "Release the dragged element over the [<Object>] element ")
    public void releaseElement() {
        if (elementEnabled()) {
            new Actions(mDriver).release(Element).build().perform();
            Report.updateTestLog(Action, "releaseElement action is done", Status.DONE);
        } else {
            throw new ElementException(ElementException.ExceptionType.Element_Not_Enabled, ObjectName);
        }
    }

    @Action(object = ObjectType.BROWSER, desc = "Add the cookie of name with value [<Data>].", input = InputType.YES)
    public void addCookie() {

        try {
            String strCookieName = Data.split(":", 2)[0];
            String strCookieValue = Data.split(":", 2)[1];
            Cookie oCookie = new Cookie.Builder(strCookieName, strCookieValue)
                    .build();
            mDriver.manage().addCookie(oCookie);
            Report.updateTestLog(Action, "Cookie Name- '" + strCookieName
                    + "' with value '" + strCookieValue + "' is added",
                    Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(Action, e.getMessage(), Status.FAIL);
            Logger.getLogger(CommonMethods.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    @Action(object = ObjectType.BROWSER, desc = "delete the cookie having name [<Data>].", input = InputType.YES)
    public void deleteCookie() {
        try {
            String strCookieName = Data;
            Cookie oCookie = mDriver.manage().getCookieNamed(strCookieName);
            if (oCookie != null) {
                mDriver.manage().deleteCookie(oCookie);
                Report.updateTestLog(Action, "Cookie Name- '"
                        + strCookieName + "' is deleted", Status.DONE);
            } else {
                Report.updateTestLog(Action, "Cookie doesn't exist",
                        Status.FAIL);
            }
        } catch (Exception e) {
            Report.updateTestLog(Action, e.getMessage(), Status.FAIL);
            Logger.getLogger(CommonMethods.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    @Action(object = ObjectType.BROWSER, desc = "Take screenshot of the current page and store it in the location [<Input>]", input = InputType.YES)
    public void saveScreenshot() {
        try {
            String strFullpath = Data;
            File scrFile = getDriverControl().createScreenShot();
            FileUtils.copyFile(scrFile, new File(strFullpath));
            Report.updateTestLog(Action, "Screenshot is taken and saved in this path -'"
                    + strFullpath + "'", Status.PASS);
        } catch (IOException e) {
            Report.updateTestLog(Action, e.getMessage(), Status.FAIL);
            Logger.getLogger(CommonMethods.class.getName()).log(Level.SEVERE, null, e);
        }

    }

    @Action(object = ObjectType.BROWSER, desc = "Take a Screen Shot ")
    public void takeScreenshot() {
        try {
            Report.updateTestLog(Action, "Screenshot is taken", Status.PASS);
        } catch (Exception e) {
            Report.updateTestLog(Action, e.getMessage(), Status.DEBUG);
            Logger.getLogger(CommonMethods.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    @Action(object = ObjectType.BROWSER, desc = "print the data [<Data>]", input = InputType.YES)
    public void print() {
        System.out.println(Data);
        Report.updateTestLog("print", String.format("printed %s", Data), Status.DONE);
    }

    @Action(object = ObjectType.BROWSER, desc = "Wait for [<Data>] milli seconds", input = InputType.YES)
    public void pause() {
        try {
            Thread.sleep(Long.parseLong(Data));
            Report.updateTestLog(Action,
                    "Thread sleep for '" + Long.parseLong(Data), Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(Action, e.getMessage(), Status.FAIL);
            Logger.getLogger(CommonMethods.class.getName()).log(Level.SEVERE, null, e);
        }

    }

    @Action(object = ObjectType.BROWSER, desc = "Answer the alert present with [<Data>]", input = InputType.YES)
    public void answerAlert() {
        String setAlertText = Data;
        try {
            mDriver.switchTo().alert().sendKeys(setAlertText);
            Report.updateTestLog(Action, "Message '" + setAlertText
                    + "' is set in the alert window", Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(Action, e.getMessage(), Status.FAILNS);
            Logger.getLogger(CommonMethods.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    @Action(object = ObjectType.BROWSER, desc = "Accept the alert present")
    public void acceptAlert() {
        try {
            mDriver.switchTo().alert().accept();
            Report.updateTestLog(Action, "Alert is accepted", Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(Action, e.getMessage(), Status.FAILNS);
            Logger.getLogger(CommonMethods.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    @Action(object = ObjectType.BROWSER, desc = "Dismiss the alert present")
    public void dismissAlert() {
        try {
            mDriver.switchTo().alert().dismiss();
            Report.updateTestLog(Action, "Alert is dismissed",
                    Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(Action, e.getMessage(), Status.FAILNS);
            Logger.getLogger(CommonMethods.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    public HashMap<String, String> getCellValue(WebElement Element, int tr,
            int td) {
        int rowCounter = 0;
        int colCounter = 0;
        String rowKey = null;
        String colKey = null;
        HashMap<String, String> HashTable = new HashMap<>();

        String strObj = Data;
        List<WebElement> tableList = Element.findElements(By
                .cssSelector("div[class='" + strObj + "'] tr td"));
        for (WebElement listIterator : tableList) {
            String TagName = listIterator.getTagName();
            if (TagName.equals("tr")) {
                rowKey = "R" + rowCounter++;
            }
            if (TagName.equals("td")) {
                colKey = "C" + colCounter++;
            }
            HashTable.put(rowKey + colKey, listIterator.getText());
        }
        return HashTable;
    }

    @Action(object = ObjectType.BROWSER, desc = "Store the current page url into the Runtime variable: [<Data>]", input = InputType.YES)
    public void storeCurrentUrl() {
        String strObj = Input;
        if (strObj.startsWith("%") && strObj.endsWith("%")) {
            addVar(strObj, mDriver.getCurrentUrl());
            Report.updateTestLog(Action, "Current URL '" + mDriver.getCurrentUrl()
                    + "' is stored in variable '" + strObj + "'", Status.PASS);
        } else {
            Report.updateTestLog(Action, "Variable format is not correct", Status.FAIL);
        }
    }

    @Action(object = ObjectType.BROWSER, desc = "store the webpage title in variable named [<Data>].", input = InputType.YES)
    public void storeTitle() {
        String strObj = Input;
        if (strObj.startsWith("%") && strObj.endsWith("%")) {
            addVar(strObj, mDriver.getTitle());
            Report.updateTestLog(Action, "Page title '" + mDriver.getTitle() + "' is stored in '"
                    + strObj + "'", Status.PASS);
        } else {
            Report.updateTestLog(Action, "Variable format is not correct", Status.FAIL);
        }
    }

    @Action(object = ObjectType.MOBILE, desc = "Store the [<Object>] element's text into the Runtime variable: [<Data>]", input = InputType.YES)
    public void storeText() {
        if (elementPresent()) {
            String strObj = Input;
            if (strObj.startsWith("%") && strObj.endsWith("%")) {
                addVar(strObj, getElementText());
                Report.updateTestLog(Action, "Element text " + getElementText()
                        + " is stored in variable " + strObj, Status.PASS);
            } else {
                Report.updateTestLog(Action, "Invalid variable format", Status.DEBUG);
            }
        } else {
            throw new ElementException(ExceptionType.Element_Not_Found, ObjectName);
        }
    }

    @Action(object = ObjectType.MOBILE, desc = "Store the [<Object>] element's text into datasheet:columname [<Data>]", input = InputType.YES)
    public void storeTextinDataSheet() {
        if (elementPresent()) {
            String strObj = Input;
            if (strObj.matches(".*:.*")) {
                try {
                    System.out.println("Updating value in SubIteration " + userData.getSubIteration());
                    String sheetName = strObj.split(":", 2)[0];
                    String columnName = strObj.split(":", 2)[1];
                    String elText = getElementText();
                    userData.putData(sheetName, columnName, elText.trim());
                    Report.updateTestLog(Action, "Element text [" + elText
                            + "] is stored in " + strObj, Status.DONE);
                } catch (Exception ex) {
                    Logger.getLogger(this.getClass().getName()).log(Level.OFF, ex.getMessage(), ex);
                    Report.updateTestLog(Action, "Error Storing text in datasheet " + ex.getMessage(), Status.DEBUG);
                }

            } else {
                Report.updateTestLog(Action,
                        "Given input [" + Input + "] format is invalid. It should be [sheetName:ColumnName]", Status.DEBUG);
            }
        } else {
            throw new ElementException(ExceptionType.Element_Not_Found, ObjectName);
        }
    }

    @Action(object = ObjectType.MOBILE, desc = "Store in variable true or false based on presence of text in [<Object>] element -> [<Data>]", input = InputType.YES, condition = InputType.YES)
    public void storeTextPresent() {
        try {
            if (elementPresent()) {
                if (getElementText().contains(Data)) {
                    addVar(Condition, "true");
                } else {
                    addVar(Condition, "false");
                }
                Report.updateTestLog(Action, "Element presence flag has been stored into variable", Status.DONE);
            } else {
                throw new ElementException(ExceptionType.Element_Not_Found, ObjectName);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, ex.getMessage(), ex);
            Report.updateTestLog(Action, ex.getMessage(), Status.FAIL);
        }
    }

    private String getElementText() {
        if (Element.getTagName().equalsIgnoreCase("input")) {
            return Element.getAttribute("value");
        } else if (Element.getTagName().equalsIgnoreCase("select")) {
            return new Select(Element).getFirstSelectedOption().getText();
        } else {
            return Element.getText();
        }
    }

    @Action(object = ObjectType.BROWSER, desc = "Store the [<Object>] page source into the Runtime variable: -> [<Data>]", input = InputType.YES)
    public void storePageSource() {
        String strObj = Input;
        if (strObj.startsWith("%") && strObj.endsWith("%")) {
            addVar(strObj, mDriver.getPageSource());
            Report.updateTestLog(Action,
                    "Page source is stored in variable '" + strObj + "'",
                    Status.DONE);
        } else {
            Report.updateTestLog(Action,
                    "Variable format is not correct", Status.FAIL);
        }
    }

    @Action(object = ObjectType.MOBILE, desc = "Store [<Object>] element  selection state into Runtime variable: -> [<Data>]", input = InputType.YES)
    public void storeElementSelected() {
        if (elementPresent()) {
            String strObj = Input;
            if (strObj.startsWith("%") && strObj.endsWith("%")) {

                if (Element.isSelected()) {
                    addVar(strObj, "true");
                } else {
                    addVar(strObj, "false");
                }
                Report.updateTestLog(Action,
                        "Element selected flag has been stored into variable '"
                        + strObj + "'", Status.DONE);
            } else {
                Report.updateTestLog(Action,
                        "Variable format is not correct", Status.DEBUG);
            }
        } else {
            throw new ElementException(ExceptionType.Element_Not_Found, ObjectName);
        }
    }

    @Action(object = ObjectType.MOBILE, desc = "Store [<Object>] element's  attribute into Runtime variable ->  [<Data>]", input = InputType.YES, condition = InputType.YES)
    public void storeElementAttribute() {
        if (elementPresent()) {
            addVar(Condition, Element.getAttribute(Data));
            Report.updateTestLog(Action, "Element's attribute value is stored in variable", Status.PASS);
        } else {
            throw new ElementException(ExceptionType.Element_Not_Found, ObjectName);
        }
    }

    @Action(object = ObjectType.MOBILE, desc = "Store [<Object>] element's  value  into Runtime variable: -> [<Data>]", input = InputType.YES)
    public void storeElementValue() {
        if (elementPresent()) {
            String strObj = Input;
            if (strObj.startsWith("%") && strObj.endsWith("%")) {
                addVar(strObj, Element.getAttribute("value"));
                Report.updateTestLog(Action, "Element's value " + Element.getAttribute("value")
                        + " is stored in variable '" + strObj + "'", Status.DONE);
            } else {
                Report.updateTestLog(Action, "Variable format is not correct", Status.DEBUG);
            }
        } else {
            throw new ElementException(ExceptionType.Element_Not_Found, ObjectName);
        }
    }

    @Action(object = ObjectType.BROWSER, desc = "Store in Runtime variable Exist/Not Exist based on the  presence of cookie ->[<Data>]", input = InputType.YES, condition = InputType.YES)
    public void storeCookiePresent() {
        String variableName = Condition;
        String cookieName = Data;
        if (variableName.matches("%.*%")) {
            if (mDriver.manage().getCookieNamed(cookieName) != null) {
                addVar(variableName, "Exist");
            } else {
                addVar(variableName, "Not Exist");
            }
            Report.updateTestLog(Action,
                    "Cookie presense flag is stored in variable " + variableName + "",
                    Status.DONE);
        } else {
            Report.updateTestLog(Action, "Variable format is not correct", Status.DEBUG);
        }
    }

    @Action(object = ObjectType.BROWSER, desc = "Store value of cookie into Runtime variable -> [<Data>]", input = InputType.YES, condition = InputType.YES)
    public void storeCookieByName() {
        String variableName = Condition;
        String cookieName = Data;
        if (variableName.matches("%.*%")) {
            addVar(variableName, mDriver.manage().getCookieNamed(cookieName)
                    .getValue());
            Report.updateTestLog(Action, "Cookie Stored", Status.DONE);
        } else {
            Report.updateTestLog(Action, "Variable format is not correct", Status.DEBUG);
        }
    }

    @Action(object = ObjectType.BROWSER, desc = "Store the text of alert present into -> [<Data>] Runtime variable", input = InputType.YES)
    public void storeAlertText() {
        String strObj = Input;
        if (strObj.startsWith("%") && strObj.endsWith("%")) {
            addVar(strObj, mDriver.switchTo().alert().getText());
            Report.updateTestLog(Action, "Alert Text "
                    + mDriver.switchTo().alert().getText()
                    + " is Stored in variable " + strObj + "", Status.DONE);
        } else {
            Report.updateTestLog(Action, "Variable format is not correct", Status.DEBUG);
        }
    }

    @Action(object = ObjectType.BROWSER, desc = "Store \"Exist\" or \"Not Exist\" based on the alert presence into -> [<Data>] Runtime variable", input = InputType.YES)
    public void storeAlertPresent() {
        String strObj = Input;
        if (strObj.startsWith("%") && strObj.endsWith("%")) {
            if (isAlertPresent(mDriver)) {
                addVar(strObj, "Exist");
            } else {
                addVar(strObj, "Not Exist");
            }
            Report.updateTestLog(Action, "Alert Text Status Stored", Status.DONE);
        } else {
            Report.updateTestLog(Action, "Variable format is not correct", Status.DEBUG);
        }
    }

    @Action(object = ObjectType.BROWSER, desc = "store variable value [<Condition>] in data sheet[<Data>]", input = InputType.YES, condition = InputType.YES)
    public void storeVariableInDataSheet() {
        if (Input != null && Condition != null) {
            if (!getVar(Condition).isEmpty()) {
                System.out.println(Condition);
                String[] sheetDetail = Input.split(":");
                String sheetName = sheetDetail[0];
                String columnName = sheetDetail[1];
                userData.putData(sheetName, columnName, getVar(Condition));
                Report.updateTestLog(Action, "Value of variable " + Condition + " has been stored into " + "the data sheet", Status.DONE);
            } else {
                Report.updateTestLog(Action, "The variable " + Condition + " does not contain any value", Status.FAIL);
            }
        } else {
            Report.updateTestLog(Action, "Incorrect input format", Status.DEBUG);
            System.out.println("Incorrect input format " + Condition);
        }
    }

    @Action(object = ObjectType.BROWSER, desc = "store  value [<Data>] in Variable [<Condition>]", input = InputType.YES, condition = InputType.YES)
    public void storeVariable() {
        if (Condition.startsWith("%") && Condition.endsWith("%")) {
            addVar(Condition, Data);
            Report.updateTestLog(Action, "Value" + Data
                    + "' is stored in Variable '" + Condition + "'",
                    Status.DONE);
        } else {
            Report.updateTestLog(Action, "Variable format is not correct", Status.DEBUG);
        }
    }

    @Action(object = ObjectType.BROWSER, desc = "Store the result of Javascript expression value in a variable", input = InputType.YES, condition = InputType.YES)
    public void storeEval() {
        String javaScript = Data;
        String variableName = Condition;
        if (variableName.matches("%.*%")) {
            JavascriptExecutor js = (JavascriptExecutor) Driver;
            addVar(variableName, js.executeScript(javaScript).toString());
            Report.updateTestLog(Action, "Eval Stored", Status.DONE);
        } else {
            Report.updateTestLog(Action, "Variable format is not correct", Status.FAIL);
        }
    }

    private boolean isAlertPresent(WebDriver Driver) {
        try {
            mDriver.switchTo().alert();
            return true;
        } catch (NoAlertPresentException e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, e.getMessage(), e);
            return false;
        }
    }

    @Action(object = ObjectType.MOBILE, desc = "Send Keys [<Data>]  to object [<Object>].", input = InputType.YES)
    public void sendKeysToElement() {
        if (elementPresent()) {
            String[] Values = Data.toLowerCase().split("\\+");
            if (Values.length == 4) {
                Element.sendKeys(Keys
                        .chord(getKeyCode(Values[0]),
                                getKeyCode(Values[1]),
                                getKeyCode(Values[2]),
                                getKeyCode(Values[3]) != null ? getKeyCode(Values[3])
                                : Values[3]));
                Report.updateTestLog(Action, "Keys Submitted", Status.DONE);
            } else if (Values.length == 3) {
                Element.sendKeys(Keys
                        .chord(getKeyCode(Values[0]),
                                getKeyCode(Values[1]),
                                getKeyCode(Values[2]) != null ? getKeyCode(Values[2])
                                : Values[2]));
                Report.updateTestLog(Action, "Keys Submitted", Status.DONE);
            } else if (Values.length == 2) {
                Element.sendKeys(Keys
                        .chord(getKeyCode(Values[0]),
                                getKeyCode(Values[1]) != null ? getKeyCode(Values[1])
                                : Values[1]));
                Report.updateTestLog(Action, "Keys Submitted", Status.DONE);
            } else if (Values.length == 1) {
                Element.sendKeys(Keys.chord(getKeyCode(Values[0])));
                Report.updateTestLog(Action, "Keys Submitted", Status.DONE);
            }
        } else {
            throw new ElementException(ExceptionType.Element_Not_Found, ObjectName);
        }
    }

    @Action(object = ObjectType.BROWSER, desc = "Send Keys [<Data>]  to Window.", input = InputType.YES)
    public void sendKeysToWindow() {
        Actions builder = new Actions(mDriver);
        String[] Values = Data.toLowerCase().split("\\+");
        switch (Values.length) {
            case 4:
                builder.sendKeys(
                        Keys.chord(
                                getKeyCode(Values[0]),
                                getKeyCode(Values[1]),
                                getKeyCode(Values[2]),
                                getKeyCode(Values[3]) != null ? getKeyCode(Values[3])
                                : Values[3])).perform();
                Report.updateTestLog(Action, "Keys Submitted", Status.DONE);
                break;
            case 3:
                builder.sendKeys(
                        Keys.chord(
                                getKeyCode(Values[0]),
                                getKeyCode(Values[1]),
                                getKeyCode(Values[2]) != null ? getKeyCode(Values[2])
                                : Values[2])).perform();
                Report.updateTestLog(Action, "Keys Submitted", Status.DONE);
                break;
            case 2:
                builder.sendKeys(
                        Keys.chord(
                                getKeyCode(Values[0]),
                                getKeyCode(Values[1]) != null ? getKeyCode(Values[1])
                                : Values[1])).build().perform();
                Report.updateTestLog(Action, "Keys Submitted", Status.DONE);
                break;
            case 1:
                builder.sendKeys(Keys.chord(getKeyCode(Values[0]))).build()
                        .perform();
                Report.updateTestLog(Action, "Keys Submitted", Status.DONE);
                break;
            default:
                Report.updateTestLog(Action, "Input format is not correct",
                        Status.DEBUG);
                break;
        }
    }

    Keys getKeyCode(String data) {
        switch (data) {
            case "tab":
                return Keys.TAB;
            case "enter":
                return Keys.ENTER;
            case "shift":
                return Keys.SHIFT;
            case "ctrl":
                return Keys.CONTROL;
            case "alt":
                return Keys.ALT;
            case "esc":
                return Keys.ESCAPE;
            case "delete":
                return Keys.DELETE;
            case "backspace":
                return Keys.BACK_SPACE;
            case "home":
                return Keys.HOME;
            default:
                try {
                    return Keys.valueOf(data.toUpperCase());
                } catch (Exception ex) {
                    return null;
                }
        }
    }

}

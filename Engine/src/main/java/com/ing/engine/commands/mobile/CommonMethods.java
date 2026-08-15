package com.ing.engine.commands.mobile;

import com.ing.engine.core.CommandControl;
import com.ing.ingenious.api.annotation.Action;
import com.ing.ingenious.api.annotation.Args;
import com.ing.ingenious.api.exception.mobile.ElementException;
import com.ing.ingenious.api.exception.mobile.ElementException.ExceptionType;
import com.ing.ingenious.api.status.Status;
import com.ing.ingenious.api.types.ArgType;
import com.ing.ingenious.api.types.ConditionKind;
import com.ing.ingenious.api.types.InputType;
import com.ing.ingenious.api.types.ObjectType;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.By;
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

    @Action(object = ObjectType.MOBILE, desc = "Navigate to previous page")
    @Args(
        inputHelp = "no input required (e.g. leave empty)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void back() {
        try {
            mDriver.navigate().back();
            Report.updateTestLog("back", "Navigate page back is success", Status.DONE);
        } catch (WebDriverException e) {
            Report.updateTestLog("back", e.getMessage(), Status.FAIL);
            Logger.getLogger(CommonMethods.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    @Action(object = ObjectType.APP, desc = "Hover over the [<Object>] element")
    @Args(
        inputHelp = "no input required (e.g. leave empty)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void mouseOverElement() {
        if (elementPresent()) {
            new Actions(mDriver).moveToElement(Element).build().perform();
            Report.updateTestLog(Action, "Mouse Over to Element '" + ObjectName, Status.DONE);
        } else {
            throw new ElementException(
                ElementException.ExceptionType.Element_Not_Found,
                ObjectName
            );
        }
    }

    @Action(object = ObjectType.APP, desc = "drag and drop operation of ", input = InputType.YES)
    @Args(
        input = ArgType.TEXT,
        inputExample = "@value",
        inputHelp = "input value (e.g. @value)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void dragToAndDropElement() {
        try {
            String Page = Data.split(":", 2)[0];
            String Object = Data.split(":", 2)[1];
            if (elementPresent()) {
                WebElement DropElement = mObject.findElement(Object, Page);
                if (DropElement != null) {
                    new Actions(mDriver).dragAndDrop(Element, DropElement).build().perform();
                    Report.updateTestLog(
                        Action,
                        "'" + ObjectName + "' has been dragged and dropped to '" + Object + "'",
                        Status.PASS
                    );
                } else {
                    throw new ElementException(
                        ElementException.ExceptionType.Element_Not_Found,
                        Object
                    );
                }
            } else {
                throw new ElementException(
                    ElementException.ExceptionType.Element_Not_Found,
                    ObjectName
                );
            }
        } catch (Exception e) {
            Report.updateTestLog(Action, e.getMessage(), Status.FAIL);
            Logger.getLogger(CommonMethods.class.getName()).log(Level.SEVERE, e.getMessage(), e);
        }
    }

    @Action(object = ObjectType.APP, desc = "Tap and hold the [<Object>] element ")
    @Args(
        inputHelp = "no input required (e.g. leave empty)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void TapAndHoldElement() {
        if (elementEnabled()) {
            new Actions(mDriver).clickAndHold(Element).build().perform();
            Report.updateTestLog(Action, "Tap and hold done", Status.DONE);
        } else {
            throw new ElementException(
                ElementException.ExceptionType.Element_Not_Enabled,
                ObjectName
            );
        }
    }

    @Action(
        object = ObjectType.APP,
        desc = "Release the dragged element over the [<Object>] element "
    )
    @Args(
        inputHelp = "no input required (e.g. leave empty)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void releaseElement() {
        if (elementEnabled()) {
            new Actions(mDriver).release(Element).build().perform();
            Report.updateTestLog(Action, "releaseElement action is done", Status.DONE);
        } else {
            throw new ElementException(
                ElementException.ExceptionType.Element_Not_Enabled,
                ObjectName
            );
        }
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Take screenshot of the current page and store it in the location [<Input>]",
        input = InputType.YES
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "@value",
        inputHelp = "input value (e.g. @value)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void saveScreenshot() {
        try {
            String strFullpath = Data;
            File scrFile = getDriverControl().createScreenShot();
            FileUtils.copyFile(scrFile, new File(strFullpath));
            Report.updateTestLog(
                Action,
                "Screenshot is taken and saved in this path -'" + strFullpath + "'",
                Status.PASS
            );
        } catch (IOException e) {
            Report.updateTestLog(Action, e.getMessage(), Status.FAIL);
            Logger.getLogger(CommonMethods.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    @Action(object = ObjectType.MOBILE, desc = "Take a Screen Shot ")
    @Args(
        inputHelp = "no input required (e.g. leave empty)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void takeScreenshot() {
        try {
            Report.updateTestLog(Action, "Screenshot is taken", Status.PASS);
        } catch (Exception e) {
            Report.updateTestLog(Action, e.getMessage(), Status.DEBUG);
            Logger.getLogger(CommonMethods.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Answer the alert present with [<Data>]",
        input = InputType.YES
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "@value",
        inputHelp = "input value (e.g. @value)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void answerAlert() {
        String setAlertText = Data;
        try {
            mDriver.switchTo().alert().sendKeys(setAlertText);
            Report.updateTestLog(
                Action,
                "Message '" + setAlertText + "' is set in the alert window",
                Status.DONE
            );
        } catch (Exception e) {
            Report.updateTestLog(Action, e.getMessage(), Status.FAILNS);
            Logger.getLogger(CommonMethods.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    @Action(object = ObjectType.MOBILE, desc = "Accept the alert present")
    @Args(
        inputHelp = "no input required (e.g. leave empty)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void acceptAlert() {
        try {
            mDriver.switchTo().alert().accept();
            Report.updateTestLog(Action, "Alert is accepted", Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(Action, e.getMessage(), Status.FAILNS);
            Logger.getLogger(CommonMethods.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    @Action(object = ObjectType.MOBILE, desc = "Dismiss the alert present")
    @Args(
        inputHelp = "no input required (e.g. leave empty)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void dismissAlert() {
        try {
            mDriver.switchTo().alert().dismiss();
            Report.updateTestLog(Action, "Alert is dismissed", Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(Action, e.getMessage(), Status.FAILNS);
            Logger.getLogger(CommonMethods.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    public HashMap<String, String> getCellValue(WebElement Element, int tr, int td) {
        int rowCounter = 0;
        int colCounter = 0;
        String rowKey = null;
        String colKey = null;
        HashMap<String, String> HashTable = new HashMap<>();

        String strObj = Data;
        List<WebElement> tableList = Element.findElements(
            By.cssSelector("div[class='" + strObj + "'] tr td")
        );
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

    @Action(
        object = ObjectType.APP,
        desc = "Store the [<Object>] element's text into the Runtime variable: [<Data>]",
        input = InputType.YES
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "@value",
        inputHelp = "input value (e.g. @value)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void storeText() {
        if (elementPresent()) {
            String strObj = Input;
            if (strObj.startsWith("%") && strObj.endsWith("%")) {
                addVar(strObj, getElementText());
                Report.updateTestLog(
                    Action,
                    "Element text " + getElementText() + " is stored in variable " + strObj,
                    Status.PASS
                );
            } else {
                Report.updateTestLog(Action, "Invalid variable format", Status.DEBUG);
            }
        } else {
            throw new ElementException(ExceptionType.Element_Not_Found, ObjectName);
        }
    }

    @Action(
        object = ObjectType.APP,
        desc = "Store the [<Object>] element's text into datasheet:columname [<Data>]",
        input = InputType.YES
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "@value",
        inputHelp = "input value (e.g. @value)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void storeTextinDataSheet() {
        if (elementPresent()) {
            String strObj = Input;
            if (strObj.matches(".*:.*")) {
                try {
                    System.out.println(
                        "Updating value in SubIteration " + userData.getSubIteration()
                    );
                    String sheetName = strObj.split(":", 2)[0];
                    String columnName = strObj.split(":", 2)[1];
                    String elText = getElementText();
                    userData.putData(sheetName, columnName, elText.trim());
                    Report.updateTestLog(
                        Action,
                        "Element text [" + elText + "] is stored in " + strObj,
                        Status.DONE
                    );
                } catch (Exception ex) {
                    Logger.getLogger(this.getClass().getName()).log(Level.OFF, ex.getMessage(), ex);
                    Report.updateTestLog(
                        Action,
                        "Error Storing text in datasheet " + ex.getMessage(),
                        Status.DEBUG
                    );
                }
            } else {
                Report.updateTestLog(
                    Action,
                    "Given input [" +
                    Input +
                    "] format is invalid. It should be [sheetName:ColumnName]",
                    Status.DEBUG
                );
            }
        } else {
            throw new ElementException(ExceptionType.Element_Not_Found, ObjectName);
        }
    }

    @Action(
        object = ObjectType.APP,
        desc = "Store in variable true or false based on presence of text in [<Object>] element -> [<Data>]",
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
    public void storeTextPresent() {
        try {
            if (elementPresent()) {
                if (getElementText().contains(Data)) {
                    addVar(Condition, "true");
                } else {
                    addVar(Condition, "false");
                }
                Report.updateTestLog(
                    Action,
                    "Element presence flag has been stored into variable",
                    Status.DONE
                );
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

    @Action(
        object = ObjectType.APP,
        desc = "Store [<Object>] element  selection state into Runtime variable: -> [<Data>]",
        input = InputType.YES
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "@value",
        inputHelp = "input value (e.g. @value)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void storeElementSelected() {
        if (elementPresent()) {
            String strObj = Input;
            if (strObj.startsWith("%") && strObj.endsWith("%")) {
                if (Element.isSelected()) {
                    addVar(strObj, "true");
                } else {
                    addVar(strObj, "false");
                }
                Report.updateTestLog(
                    Action,
                    "Element selected flag has been stored into variable '" + strObj + "'",
                    Status.DONE
                );
            } else {
                Report.updateTestLog(Action, "Variable format is not correct", Status.DEBUG);
            }
        } else {
            throw new ElementException(ExceptionType.Element_Not_Found, ObjectName);
        }
    }

    @Action(
        object = ObjectType.APP,
        desc = "Store [<Object>] element's  attribute into Runtime variable ->  [<Data>]",
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
    public void storeElementAttribute() {
        if (elementPresent()) {
            addVar(Condition, Element.getAttribute(Data));
            Report.updateTestLog(
                Action,
                "Element's attribute value is stored in variable",
                Status.PASS
            );
        } else {
            throw new ElementException(ExceptionType.Element_Not_Found, ObjectName);
        }
    }

    @Action(
        object = ObjectType.APP,
        desc = "Store [<Object>] element's  value  into Runtime variable: -> [<Data>]",
        input = InputType.YES
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "@value",
        inputHelp = "input value (e.g. @value)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void storeElementValue() {
        if (elementPresent()) {
            String strObj = Input;
            if (strObj.startsWith("%") && strObj.endsWith("%")) {
                addVar(strObj, Element.getAttribute("value"));
                Report.updateTestLog(
                    Action,
                    "Element's value " +
                    Element.getAttribute("value") +
                    " is stored in variable '" +
                    strObj +
                    "'",
                    Status.DONE
                );
            } else {
                Report.updateTestLog(Action, "Variable format is not correct", Status.DEBUG);
            }
        } else {
            throw new ElementException(ExceptionType.Element_Not_Found, ObjectName);
        }
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Store \"Exist\" or \"Not Exist\" based on the alert presence into -> [<Data>] Runtime variable",
        input = InputType.YES
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "@value",
        inputHelp = "input value (e.g. @value)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
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

    private boolean isAlertPresent(WebDriver mDriver) {
        try {
            mDriver.switchTo().alert();
            return true;
        } catch (NoAlertPresentException e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, e.getMessage(), e);
            return false;
        }
    }

    @Action(
        object = ObjectType.APP,
        desc = "Send Keys [<Data>]  to object [<Object>].",
        input = InputType.YES
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "@value",
        inputHelp = "input value (e.g. @value)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void sendKeysToElement() {
        if (elementPresent()) {
            String[] Values = Data.toLowerCase().split("\\+");
            if (Values.length == 4) {
                Element.sendKeys(
                    Keys.chord(
                        getKeyCode(Values[0]),
                        getKeyCode(Values[1]),
                        getKeyCode(Values[2]),
                        getKeyCode(Values[3]) != null ? getKeyCode(Values[3]) : Values[3]
                    )
                );
                Report.updateTestLog(Action, "Keys Submitted", Status.DONE);
            } else if (Values.length == 3) {
                Element.sendKeys(
                    Keys.chord(
                        getKeyCode(Values[0]),
                        getKeyCode(Values[1]),
                        getKeyCode(Values[2]) != null ? getKeyCode(Values[2]) : Values[2]
                    )
                );
                Report.updateTestLog(Action, "Keys Submitted", Status.DONE);
            } else if (Values.length == 2) {
                Element.sendKeys(
                    Keys.chord(
                        getKeyCode(Values[0]),
                        getKeyCode(Values[1]) != null ? getKeyCode(Values[1]) : Values[1]
                    )
                );
                Report.updateTestLog(Action, "Keys Submitted", Status.DONE);
            } else if (Values.length == 1) {
                Element.sendKeys(Keys.chord(getKeyCode(Values[0])));
                Report.updateTestLog(Action, "Keys Submitted", Status.DONE);
            }
        } else {
            throw new ElementException(ExceptionType.Element_Not_Found, ObjectName);
        }
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Send Keys [<Data>]  to Window.",
        input = InputType.YES
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "@value",
        inputHelp = "input value (e.g. @value)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void sendKeysToWindow() {
        Actions builder = new Actions(mDriver);
        String[] Values = Data.toLowerCase().split("\\+");
        switch (Values.length) {
            case 4:
                builder
                    .sendKeys(
                        Keys.chord(
                            getKeyCode(Values[0]),
                            getKeyCode(Values[1]),
                            getKeyCode(Values[2]),
                            getKeyCode(Values[3]) != null ? getKeyCode(Values[3]) : Values[3]
                        )
                    )
                    .perform();
                Report.updateTestLog(Action, "Keys Submitted", Status.DONE);
                break;
            case 3:
                builder
                    .sendKeys(
                        Keys.chord(
                            getKeyCode(Values[0]),
                            getKeyCode(Values[1]),
                            getKeyCode(Values[2]) != null ? getKeyCode(Values[2]) : Values[2]
                        )
                    )
                    .perform();
                Report.updateTestLog(Action, "Keys Submitted", Status.DONE);
                break;
            case 2:
                builder
                    .sendKeys(
                        Keys.chord(
                            getKeyCode(Values[0]),
                            getKeyCode(Values[1]) != null ? getKeyCode(Values[1]) : Values[1]
                        )
                    )
                    .build()
                    .perform();
                Report.updateTestLog(Action, "Keys Submitted", Status.DONE);
                break;
            case 1:
                builder.sendKeys(Keys.chord(getKeyCode(Values[0]))).build().perform();
                Report.updateTestLog(Action, "Keys Submitted", Status.DONE);
                break;
            default:
                Report.updateTestLog(Action, "Input format is not correct", Status.DEBUG);
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

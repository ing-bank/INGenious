package com.ing.engine.commands.browser;

import com.ing.engine.core.CommandControl;
import com.ing.engine.execution.exception.ActionException;
import com.ing.engine.execution.exception.ForcedException;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import com.microsoft.playwright.Page.GoBackOptions;
import com.microsoft.playwright.Page.GoForwardOptions;
import com.microsoft.playwright.Page.NavigateOptions;
import com.microsoft.playwright.Page.ReloadOptions;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.TimeoutError;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Basic extends General {

    public Basic(CommandControl cc) {
        super(cc);
    }


    @Action(object = ObjectType.BROWSER, desc = "Open the Url [<Data>] in the Browser", input = InputType.YES, condition = InputType.OPTIONAL)
    public void Open() {
        
        Boolean pageTimeOut = false;
        NavigateOptions navigateOptions = new NavigateOptions();
        try {
            if (Condition.matches("[0-9]+")) {
                navigateOptions.setTimeout(Double.parseDouble(Condition));
            }
            Page.navigate(Data, navigateOptions);
            Report.updateTestLog("Open", "Opened Url: " + Data, Status.DONE);
        } catch (TimeoutError e) {
            Report.updateTestLog("Open",
                    "Opened Url: " + Data + " and cancelled page load after " + Condition + " seconds", Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog("Open", e.getMessage(), Status.FAIL);
            throw new ForcedException("Open", e.getMessage());
        }
        if (pageTimeOut) {
            setPageTimeOut(300);
        }
    }

    private void setPageTimeOut(double sec) {
        try {
            Page.setDefaultNavigationTimeout(sec);
        } catch (Exception ex) {
            System.out.println("Couldn't set PageTimeOut to " + sec);
        }
    }

    @Action(object = ObjectType.BROWSER, desc = "Navigate to the next page in history", input = InputType.NO, condition = InputType.OPTIONAL)
    public void GoForward() {
        GoForwardOptions goForwardOptions = new GoForwardOptions();
        try {
            if (Condition.matches("[0-9]+")) {
                goForwardOptions.setTimeout(Double.parseDouble(Condition));
            }
            Page.goForward(goForwardOptions);
            Report.updateTestLog(Action, "Successfully navigated to the next page", Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(Action, e.getMessage(), Status.FAIL);
            throw new ActionException(e);
        }

    }

    @Action(object = ObjectType.BROWSER, desc = "Navigate to the previous page in history", input = InputType.NO, condition = InputType.OPTIONAL)
    public void GoBack() {
        GoBackOptions goBackOptions = new GoBackOptions();
        try {
            if (Condition.matches("[0-9]+")) {
                goBackOptions.setTimeout(Double.parseDouble(Condition));
            }
            Page.goBack(goBackOptions);
            Report.updateTestLog(Action, "Successfully navigated to the previous page", Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(Action, e.getMessage(), Status.FAIL);
            throw new ActionException(e);
        }

    }

    @Action(object = ObjectType.BROWSER, desc = "Reload the current page", input = InputType.NO, condition = InputType.OPTIONAL)
    public void Reload() {
        ReloadOptions reloadOptions = new ReloadOptions();
        try {
            if (Condition.matches("[0-9]+")) {
                reloadOptions.setTimeout(Double.parseDouble(Condition));
            }
            Page.reload(reloadOptions);
            Report.updateTestLog(Action, "Successfully reloaded to the current page", Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(Action, e.getMessage(), Status.FAIL);
            throw new ActionException(e);
        }

    }

    @Action(object = ObjectType.BROWSER, desc = "Start Recorder from the current page", input = InputType.NO, condition = InputType.NO)
    public void RecordFromHere() {
        try {
            Page.pause();
            Report.updateTestLog(Action, "Successfully started Playwright recorder", Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(Action, e.getMessage(), Status.FAIL);
            throw new ActionException(e);
        }
    }

    @Action(object = ObjectType.BROWSER, desc = "Close the Page in the Browser", input = InputType.NO, condition = InputType.NO)
    public void ClosePage() {
        try {
            Page.close();
            Report.updateTestLog(Action, "Successfully closed the current Page", Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(Action, e.getMessage(), Status.FAIL);
            throw new ActionException(e);
        }
    }

    @Action(object = ObjectType.BROWSER, desc = "Close the Browser Context", input = InputType.NO, condition = InputType.NO)
    public void CloseBrowserContext() {
        try {
            BrowserContext.close();
            Report.updateTestLog(Action, "Successfully closed the the Browser Context", Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(Action, e.getMessage(), Status.FAIL);
            throw new ActionException(e);
        }
    }

    @Action(object = ObjectType.BROWSER, desc = "Set Default Timeout (in milliseconds)", input = InputType.YES)
    public void setDefaultTimeout() {
        if (Data != null && Data.matches("[0-9]+")) {
            Double timeout = Double.valueOf(Data);
            Page.setDefaultTimeout(timeout);
            Report.updateTestLog(Action, "Default timeout changed to [" + Data + "] millisecond/s", Status.DONE);
        } else {
            Report.updateTestLog(Action, "Couldn't change default timeout (invalid input) " + Data, Status.DEBUG);            
        }
    }

    @Action(object = ObjectType.BROWSER, desc = "Add a variable to access within testcase", input = InputType.YES, condition = InputType.YES)
    public void AddVar() {
        String stringOpration = Input.split("\\(", 2)[0].replace("=", "").trim();
        switch (stringOpration) {
            case "Replace":
                replaceFunction();
                break;
            case "Split":
                splitFunction();
                break;
            case "Substring":
                subStringFunction();
                break;
            default:
                addVar(Condition, Data);
                break;
        }
        if (getVar(Condition) != null) {
            Report.updateTestLog("addVar", "Variable " + Condition + " added with value " + Data, Status.DONE);
        } else {
            Report.updateTestLog("addVar", "Variable " + Condition + " not added ", Status.DEBUG);
        }
    }

    @Action(object = ObjectType.BROWSER, desc = "Add a Global variable to access across test set", input = InputType.YES, condition = InputType.YES)
    public void AddGlobalVar() {
        //Added for  Replace, Split and Substring functions to work with AddGlobalVar
        String stringOpration = Input.split("\\(", 2)[0].replace("=", "").trim();
        switch (stringOpration) {
            case "Replace":
                replaceFunction();
                break;
            case "Split":
                splitFunction();
                break;
            case "Substring":
                subStringFunction();
                break;
            default:
                addGlobalVar(Condition, Data);
                break;
        }
        if (getVar(Condition) != null) {
            Report.updateTestLog(Action, "Variable " + Condition + " added with value " + Data, Status.DONE);
        } else {
            Report.updateTestLog(Action, "Variable " + Condition + " not added ", Status.DEBUG);
        }
    }

    @Action(object = ObjectType.BROWSER, desc = "Changes the browser size into [<Data>]", input = InputType.YES)
    public void setViewPortSize() {
        try {
            if (Data.matches("\\d+,\\d+")) {
                int width = Integer.parseInt(Data.split(",")[0]);
                int height = Integer.parseInt(Data.split(",")[1]);
                Page.setViewportSize(width, height);
                Report.updateTestLog(Action, " Viewport size is set to " + Data, Status.DONE);
            } else {
                Report.updateTestLog(Action, " Invalid viewport size [" + Data + "]", Status.DEBUG);
            }
        } catch (Exception ex) {
            Report.updateTestLog(Action, "Unable to change viewport size ", Status.FAIL);
            Logger.getLogger(Basic.class.getName()).log(Level.SEVERE, null, ex);
            throw new ActionException(ex);
        }
    }

    

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Store the [<Object>] element's text into the Runtime variable: [<Data>]", input = InputType.YES)
    public void storeElementTextinVariable() {
        String text = "";
        String strObj = Input;
        try {
            text = Locator.textContent();

            if (strObj.startsWith("%") && strObj.endsWith("%")) {
                addVar(strObj, text);
                Report.updateTestLog(Action, "Element text " + text + " is stored in variable " + strObj, Status.DONE);
            } else {
                Report.updateTestLog(Action, "Invalid variable format", Status.DEBUG);
            }
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Store the [<Object>] element's text into datasheet:columname [<Data>]", input = InputType.YES)
    public void storeElementTextinDataSheet() {
        String text = "";
        String strObj = Input;
        try {
            text = Locator.textContent();

            if (strObj.matches(".*:.*")) {
                String sheetName = strObj.split(":", 2)[0];
                String columnName = strObj.split(":", 2)[1];
                userData.putData(sheetName, columnName, text);
                Report.updateTestLog(Action, "Element text [" + text + "] is stored in " + strObj, Status.DONE);
            } else {
                Report.updateTestLog(Action,
                        "Given input [" + Input + "] format is invalid. It should be [sheetName:ColumnName]",
                        Status.DEBUG);
            }
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Store the [<Object>] element's inner HTML into the Runtime variable: [<Data>]", input = InputType.YES)
    public void storeElementInnerHTMLinVariable() {
        String text = "";
        String strObj = Input;
        try {
            text = Locator.innerHTML();

            if (strObj.startsWith("%") && strObj.endsWith("%")) {
                addVar(strObj, text);
                Report.updateTestLog(Action, "Element's inner HTML " + text + " is stored in variable " + strObj, Status.DONE);
            } else {
                Report.updateTestLog(Action, "Invalid variable format", Status.DEBUG);
            }
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Store the [<Object>] element's inner HTML into datasheet:columname [<Data>]", input = InputType.YES)
    public void storeElementInnerHTMLinDataSheet() {
        String text = "";
        String strObj = Input;
        try {
            text = Locator.innerHTML();

            if (strObj.matches(".*:.*")) {
                String sheetName = strObj.split(":", 2)[0];
                String columnName = strObj.split(":", 2)[1];
                userData.putData(sheetName, columnName, text);
                Report.updateTestLog(Action, "Element's inner HTML [" + text + "] is stored in " + strObj, Status.DONE);
            } else {
                Report.updateTestLog(Action,
                        "Given input [" + Input + "] format is invalid. It should be [sheetName:ColumnName]",
                        Status.DEBUG);
            }
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Store the [<Object>] element's inner Text into the Runtime variable: [<Data>]", input = InputType.YES)
    public void storeElementInnerTextinVariable() {
        String text = "";
        String strObj = Input;
        try {
            text = Locator.innerText();

            if (strObj.startsWith("%") && strObj.endsWith("%")) {
                addVar(strObj, text);
                Report.updateTestLog(Action, "Element's inner Text " + text + " is stored in variable " + strObj, Status.DONE);
            } else {
                Report.updateTestLog(Action, "Invalid variable format", Status.DEBUG);
            }
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Store the [<Object>] element's inner Text into datasheet:columname [<Data>]", input = InputType.YES)
    public void storeElementInnerTextinDataSheet() {
        String text = "";
        String strObj = Input;
        try {
            text = Locator.innerText();

            if (strObj.matches(".*:.*")) {
                String sheetName = strObj.split(":", 2)[0];
                String columnName = strObj.split(":", 2)[1];
                userData.putData(sheetName, columnName, text);
                Report.updateTestLog(Action, "Element's inner Text [" + text + "] is stored in " + strObj, Status.DONE);
            } else {
                Report.updateTestLog(Action,
                        "Given input [" + Input + "] format is invalid. It should be [sheetName:ColumnName]",
                        Status.DEBUG);
            }
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Store the [<Object>] element's input Value into the Runtime variable: [<Data>]", input = InputType.YES)
    public void storeElementInputValueinVariable() {
        String text = "";
        String strObj = Input;
        try {
            text = Locator.inputValue();

            if (strObj.startsWith("%") && strObj.endsWith("%")) {
                addVar(strObj, text);
                Report.updateTestLog(Action, "Element's input Value " + text + " is stored in variable " + strObj, Status.DONE);
            } else {
                Report.updateTestLog(Action, "Invalid variable format", Status.DEBUG);
            }
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Store the [<Object>] element's input Value into datasheet:columname [<Data>]", input = InputType.YES)
    public void storeElementInputValueinDataSheet() {
        String text = "";
        String strObj = Input;
        try {
            text = Locator.inputValue();

            if (strObj.matches(".*:.*")) {
                String sheetName = strObj.split(":", 2)[0];
                String columnName = strObj.split(":", 2)[1];
                userData.putData(sheetName, columnName, text);
                Report.updateTestLog(Action, "Element's input Value [" + text + "] is stored in " + strObj, Status.DONE);
            } else {
                Report.updateTestLog(Action,
                        "Given input [" + Input + "] format is invalid. It should be [sheetName:ColumnName]",
                        Status.DEBUG);
            }
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Store [<Object>] element's  attribute into Runtime variable ->  [<Data>]", input = InputType.YES, condition = InputType.YES)
    public void storeElementAttributeinVariable() {
        try {
            addVar(Condition, Locator.getAttribute(Data));
            Report.updateTestLog(Action, "Element's attribute value is stored in variable", Status.PASS);
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        }
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Store [<Object>] element's  value  into Runtime variable: -> [<Data>]", input = InputType.YES)
    public void storeElementValueinVariable() {
        try {
            String strObj = Input;
            if (strObj.startsWith("%") && strObj.endsWith("%")) {
                addVar(strObj, Locator.getAttribute("value"));
                Report.updateTestLog(Action,
                        "Element's value " + Locator.getAttribute("value") + " is stored in variable '" + strObj + "'",
                        Status.DONE);
            } else {
                Report.updateTestLog(Action, "Variable format is not correct", Status.DEBUG);
            }
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        }
    }
    
    @Action(object = ObjectType.PLAYWRIGHT, desc = "Store [<Object>] element's  CSS value  into Runtime variable: -> [<Data>]", input = InputType.YES, condition=InputType.YES)
    public void storeElementCSSValueinVariable() {
        
        String cssValue = "";
        String strObj = Input;
        try {
            cssValue = (String) Locator.evaluate("(element) => window.getComputetStyle(element).getPropertyValue("+Condition+")");
            if (strObj.matches(".*:.*")) {
                String sheetName = strObj.split(":", 2)[0];
                String columnName = strObj.split(":", 2)[1];
                userData.putData(sheetName, columnName, cssValue);
                Report.updateTestLog(Action, "Element's '"+Condition+"' value [" + cssValue + "] is stored in " + strObj, Status.DONE);
            } else {
                Report.updateTestLog(Action,
                        "Given input [" + Input + "] format is invalid. It should be [sheetName:ColumnName]",
                        Status.DEBUG);
            }

        } catch (Exception ex) {
            Logger.getLogger(JSCommands.class.getName()).log(Level.SEVERE, null, ex);
            Report.updateTestLog(Action, "Javascript execution failed", Status.DEBUG);

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
                Report.updateTestLog(Action,
                        "Value of variable " + Condition + " has been stored into " + "the data sheet", Status.DONE);
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
            Report.updateTestLog(Action, "Value" + Data + "' is stored in Variable '" + Condition + "'", Status.DONE);
        } else {
            Report.updateTestLog(Action, "Variable format is not correct", Status.DEBUG);
        }
    }

    private void PlaywrightExceptionLogging(PlaywrightException e) {
        Report.updateTestLog(Action, "Unique Element [" + ObjectName + "] not found on Page. Error :" + e.getMessage(),
                Status.FAIL);
        Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
        throw new ActionException(e);
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
        switch (Action) {
            case "AddGlobalVar":
                addGlobalVar(Condition, op);
                break;
            case "AddVar":
                addVar(Condition, op);
                break;
        }
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

            switch (Action) {
                case "AddGlobalVar":
                    addGlobalVar(Condition, op);
                    break;
                case "AddVar":
                    addVar(Condition, op);
                    break;
            }

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

            switch (Action) {
                case "AddGlobalVar":
                    addGlobalVar(Condition, op);
                    break;
                case "AddVar":
                    addVar(Condition, op);
                    break;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

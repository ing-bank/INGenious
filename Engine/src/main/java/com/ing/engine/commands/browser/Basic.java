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


    private void PlaywrightExceptionLogging(PlaywrightException e) {
        Report.updateTestLog(Action, "Unique Element [" + ObjectName + "] not found on Page. Error :" + e.getMessage(),
                Status.FAIL);
        Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
        throw new ActionException(e);
    }

}

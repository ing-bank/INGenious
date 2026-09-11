package com.ing.plugin.browser;

import com.ing.ingenious.api.annotation.Action;
import com.ing.ingenious.api.contract.BrowserPluginApi;
import com.ing.ingenious.api.contract.data.UserDataAccessApi;
import com.ing.ingenious.api.contract.reports.TestCaseReportApi;
import com.ing.ingenious.api.exception.ForcedException;
import com.ing.ingenious.api.types.ObjectType;
import com.ing.ingenious.api.types.InputType;
import com.ing.ingenious.api.status.Status;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.assertions.LocatorAssertions;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.opentest4j.AssertionFailedError;

/**
 * Browser automation plugin for Playwright-based actions
 * 
 * @author Your Name
 */
public class BrowserTestPlugin {
    
    BrowserPluginApi gen;
    
    public String Data;
    public String Action;
    public String Input;
    public String Condition;
    public TestCaseReportApi Report;
    public UserDataAccessApi userData;
    public String ObjectName;
    
    public Page Page;
    public Locator Locator;

    public BrowserTestPlugin(BrowserPluginApi gen) {
        System.out.println("BrowserTestPlugin initialized with BrowserPluginApi: " + gen);
        this.gen = gen;
        this.Data = gen.getData();
        this.Action = gen.getAction();
        this.Input = gen.getInput();
        this.Condition = gen.getCondition();
        this.Report = gen.getReport();
        this.userData = gen.getUserData();
        this.ObjectName = gen.getObjectName();
        this.Page = (Page) gen.getPage();
        this.Locator = (Locator) gen.getLocator();
    }

    /**
     * Navigate to URL with timeout support
     */
    @Action(object = ObjectType.BROWSER, desc = "Open the Url [<Data>] in the Browser", input = InputType.YES, condition = InputType.OPTIONAL)
    public void Open() {
        try {
            Page.NavigateOptions options = new Page.NavigateOptions();
            if (Condition != null && !Condition.isEmpty() && Condition.matches("[0-9]+")) {
                options.setTimeout(Double.parseDouble(Condition) * 1000);
            }
            Page.navigate(Data, options);
            Report.updateTestLog(Action, "Opened " + Data + " in the Browser", Status.DONE);
        } catch (TimeoutError e) {
            if (Condition != null && !Condition.isEmpty()) {
                Report.updateTestLog(Action, 
                    "Opened URL: " + Data + " and cancelled page load after " + Condition + " seconds", 
                    Status.DONE);
            } else {
                Report.updateTestLog(Action, "Page load timed out for URL: " + Data, Status.FAIL);
            }
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(Action, e.getMessage(), Status.FAIL);
            throw new ForcedException(Action, e.getMessage());
        }
    }

    /**
     * Assert element contains text with visual feedback
     */
    @Action(object = ObjectType.PLAYWRIGHT, desc = "Assert if [<Object>] contains the text [<Data>]", input = InputType.YES)
    public void assertElementContains() {
        String actualText = "";
        try {
            LocatorAssertions.ContainsTextOptions options = new LocatorAssertions.ContainsTextOptions();
            options.setTimeout(getTimeoutValue());
            actualText = Locator.innerHTML();
            highlightElement();
            assertThat(Locator).containsText(Data, options);
            Report.updateTestLog(Action, "Element [" + ObjectName + "] Contains text '" + Data + "'", Status.PASS);
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        } catch (AssertionFailedError err) {
            assertionLogging(err, actualText);
        } finally {
            removeHighlightFromElement();
        }
    }

    /**
     * Click on element
     */
    @Action(object = ObjectType.PLAYWRIGHT, desc = "Click on [<Object>]")
    public void Click() {
        try {
            highlightElement();
            Locator.click();
            Report.updateTestLog(Action, "Clicked on '" + ObjectName + "'", Status.DONE);
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        } finally {
            removeHighlightFromElement();
        }
    }

    /**
     * Fill element with data
     */
    @Action(object = ObjectType.PLAYWRIGHT, desc = "Enter the value [<Data>] in [<Object>]", input = InputType.YES)
    public void Fill() {
        try {
            highlightElement();
            Locator.fill(Data);
            Report.updateTestLog(Action, "Entered '" + Data + "' in '" + ObjectName + "'", Status.DONE);
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        } finally {
            removeHighlightFromElement();
        }
    }

    /**
     * Store element text in variable
     */
    @Action(object = ObjectType.PLAYWRIGHT, desc = "Store [<Object>] element's text into variable [<Data>]", input = InputType.YES)
    public void storeElementTextinVariable() {
        try {
            String text = Locator.textContent();
            String variableName = Data;
            if (!variableName.matches("%.*%")) {
                Report.updateTestLog(Action, "Variable format incorrect. Expected: %variableName%", Status.FAIL);
                return;
            }
            gen.addVar(variableName, text);
            Report.updateTestLog(Action, "Stored text '" + text + "' in variable " + variableName, Status.DONE);
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        }
    }

    /**
     * Store element text in data sheet
     */
    @Action(object = ObjectType.PLAYWRIGHT, desc = "Store [<Object>] element's text into data sheet [<Data>] under column [<Input>]", input = InputType.YES)
    public void storeElementTextinDataSheet() {
        try {
            String text = Locator.textContent();
            String sheetName = Data;
            String columnName = Input;
            userData.putData(sheetName, columnName, text);
            Report.updateTestLog(Action, "Stored text '" + text + "' in data sheet '" + sheetName + "' under column '" + columnName + "'", Status.DONE);
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(Action, "Error storing text in data sheet: " + e.getMessage(), Status.FAIL);
        }
    }

    // ========== Helper Methods ==========

    private void highlightElement() {
        Locator.scrollIntoViewIfNeeded();
        Locator.evaluate("element => element.style.outline = '2px solid red'");
    }

    private void removeHighlightFromElement() {
        Locator.evaluate("element => element.style.outline = ''");
    }

    private double getTimeoutValue() {
        double timeout = 5000;
        if (StringUtils.isNotBlank(Condition)) {
            try {
                timeout = Double.parseDouble(Condition) * 1000;
            } catch (NumberFormatException e) {
                // Use default timeout
            }
        }
        return timeout;
    }

    private void PlaywrightExceptionLogging(PlaywrightException e) {
        Report.updateTestLog(Action, "Element [" + ObjectName + "] not found. Error: " + e.getMessage(), Status.FAIL);
        Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
    }

    private void assertionLogging(AssertionFailedError err, String actualText) {
        if (err.getMessage().contains("locator resolved to")) {
            Report.updateTestLog(Action, "[" + ObjectName + "] does not contain text '" + Data + "'. Actual text is '" + actualText + "'", Status.FAIL);
        } else {
            Report.updateTestLog(Action, "Element [" + ObjectName + "] not found on Page", Status.FAIL);
        }
        Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, err);
    }
}

package com.ing.engine.commands;

import com.ing.datalib.or.common.ObjectGroup;
import com.ing.datalib.or.image.ImageORObject;
import com.ing.engine.core.CommandControl;
import com.ing.engine.drivers.AutomationObject;
import com.ing.engine.drivers.PlaywrightDriver;
import com.ing.engine.execution.data.UserDataAccess;
import com.ing.engine.reporting.TestCaseReport;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import javax.net.ssl.HttpsURLConnection;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

//Added for Mobile
import com.ing.engine.drivers.MobileDriver;
import com.ing.engine.drivers.MobileObject;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
public class Command {
    
    public Page Page;
    public Playwright Playwright;
    public BrowserContext BrowserContext;
    public AutomationObject AObject;
    public PlaywrightDriver Driver;
    public String Data;
    public String ObjectName;
    public Locator Locator;
    public ObjectGroup<ImageORObject> imageObjectGroup;
    public String Description;
    public String Condition;
    public String Input;
    public String Action;
    public TestCaseReport Report;
    public String Reference;
    private final CommandControl Commander;
    public UserDataAccess userData;
    
    //Added for Mobile
    public WebDriver mDriver;
    public WebElement Element;
    public MobileObject mObject;

    /**
     * ******API*******
     */
    static public Map<String, String> endPoints = new HashMap<>();
    static public Map<String, ArrayList<String>> headers = new HashMap<>();
    static public Map<String, ArrayList<String>> urlParams = new HashMap<>();
    static public Map<String, String> responsebodies = new HashMap<>();
    static public Map<String, String> responsecodes = new HashMap<>();
    static public Map<String, String> responsemessages = new HashMap<>();
    static public Map<String, APIRequestContext> requests = new HashMap<>();
    static public Map<String, APIResponse> responses = new HashMap<>();
    static public Map<String, java.net.http.HttpRequest.Builder> httpRequestBuilder = new HashMap<>();
    static public Map<String, java.net.http.HttpRequest> httpRequest = new HashMap<>();
    static public Map<String, java.net.http.HttpClient.Builder> httpClientBuilder = new HashMap<>();
    static public Map<String, java.net.http.HttpClient> httpClient = new HashMap<>();
    static public Map<String, java.net.http.HttpResponse> response = new HashMap<>();
    static public Map<String, String> httpagents = new HashMap<>();
    static public Map<String, Instant> before = new HashMap<>();
    static public Map<String, Instant> after = new HashMap<>();
    static public Map<String, Long> duration = new HashMap<>();
    public String key;
    static public String basicAuthorization;
    /**
     * ************************
     */

    /**
     * Playwright Mocking *
     */
    static public Map<String, String> mockEndPoints = new HashMap<>();

    /**
     * ************************
     */
    public Command(CommandControl cc) {
        Commander = cc;
        if(Commander.mobileDriver.driver!=null)
        {
        mDriver = Commander.mobileDriver.driver;
        mObject = Commander.MObject;
        Data = Commander.Data;
        ObjectName = Commander.ObjectName;
        Element = Commander.Element;
        imageObjectGroup = Commander.imageObjectGroup;
        Description = Commander.Description;
        Condition = Commander.Condition;
        Input = Commander.Input;
        Report = Commander.Report;
        Reference = Commander.Reference;
        Action = Commander.Action;
        userData = Commander.userData;
        }
        else
        {
        Page = Commander.Page.page;
        Playwright = Commander.Playwright.playwright;
        BrowserContext = Commander.BrowserContext.browserContext;
        AObject = Commander.AObject;
        Driver=Commander.Page;
        Data = Commander.Data;
        ObjectName = Commander.ObjectName;
        Locator = Commander.Locator;
        imageObjectGroup = Commander.imageObjectGroup;
        Description = Commander.Description;
        Condition = Commander.Condition;
        Input = Commander.Input;
        Report = Commander.Report;
        Reference = Commander.Reference;
        Action = Commander.Action;
        userData = Commander.userData;
        }
        /**
         * ******Webservice*******
         */
        key = userData.getScenario() + userData.getTestCase();
        /**
         * ***********************
         */
    }

    public void addVar(String key, String val) {
        Commander.addVar(key, val);
    }

    public String getVar(String key) {
        return Commander.getVar(key);
    }

    public void addGlobalVar(String key, String val) {
        if (key.matches("%.*%")) {
            key = key.substring(1, key.length() - 1);
        }
        Commander.putUserDefinedData(key, val);
    }

    public String getUserDefinedData(String key) {
        return Commander.getUserDefinedData(key);
    }

    public String getDataBaseData(String key) {
        String data = Commander.getDataBaseProperty(key);

        return data;
    }

    public Stack<Locator> getRunTimeElement() {
        return Commander.getRunTimeElement();
    }
    
    public void executeMethod(String Action) {
        Commander.executeAction(Action);
    }

    public void executeMethod(Locator Locator, String Action, String Input) {
        setElement(Locator);
        setInput(Input);
        executeMethod(Action);
    }

    public void executeMethod(String Action, String Input) {
        setInput(Input);
        executeMethod(Action);
    }

    public void executeMethod(Locator Locator, String Action) {
        setElement(Locator);
        executeMethod(Action);
    }

    public PlaywrightDriver getDriverControl() {
        return Commander.Page;
    }
    
    public MobileDriver getMobileDriverControl()
    {
        return Commander.mobileDriver;
    }

    public Boolean isDriverAlive() {
        if(mDriver!=null)
        {
           return getMobileDriverControl().isAlive();
        }
        else
        {
        return getDriverControl().isAlive();
        }
    }

    private void setElement(Locator Locator) {
        Commander.Locator = Locator;
    }

    private void setInput(String input) {
        Commander.Data = input;
    }

    public String getCurrentBrowserName() {
        return Commander.Page.getCurrentBrowser();
    }

    public CommandControl getCommander() {
        return Commander;
    }

    public void executeTestCase(String scenarioName, String testCaseName, int subIteration) {
        Commander.execute(scenarioName + ":" + testCaseName, subIteration);
    }

    public void executeTestCase(String scenarioName, String testCaseName) {
        executeTestCase(scenarioName, testCaseName, userData.getSubIterationAsNumber());
    }

    public boolean browserAction() {
        return "browser".equalsIgnoreCase(ObjectName);
    }

    /**
     * ******Webservice**************
     */
    public String Endpoint() {
        return endPoints.get(key);
    }

    public String ResponseCode() {
        return responsecodes.get(key);
    }

    public String ResponseMessage() {
        return responsemessages.get(key);
    }

    public String ResponseBody() {
        return responsebodies.get(key);
    }

    public APIRequestContext Connection() {
        return requests.get(key);
    }

    public String HttpAgent() {
        return httpagents.get(key);
    }

    /**
     * ******************************
     */
}

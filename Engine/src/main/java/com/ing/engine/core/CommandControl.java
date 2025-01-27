
package com.ing.engine.core;

import com.ing.datalib.or.common.ObjectGroup;
import com.ing.datalib.or.image.ImageORObject;
import com.ing.datalib.settings.DriverSettings;
import com.ing.datalib.util.data.LinkedProperties;
import com.ing.engine.drivers.AutomationObject;
import com.ing.engine.drivers.AutomationObject.FindType;
import com.ing.engine.drivers.PlaywrightDriverCreation;
import com.ing.engine.execution.data.DataProcessor;
import com.ing.engine.execution.data.UserDataAccess;
import com.ing.engine.execution.exception.UnCaughtException;
import com.ing.engine.execution.run.TestCaseRunner;
import com.ing.engine.reporting.TestCaseReport;
import com.ing.engine.support.Status;
import com.ing.engine.support.Step;
import com.microsoft.playwright.Locator;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

//Added For Mobile
import com.ing.engine.drivers.WebDriverCreation;
import com.ing.engine.drivers.MobileObject;
import com.ing.engine.drivers.MobileObject.FindmType;
import java.util.Properties;
import org.openqa.selenium.WebElement;

public abstract class CommandControl {

    public PlaywrightDriverCreation Playwright;
    public PlaywrightDriverCreation Page;
    public PlaywrightDriverCreation BrowserContext;
    public AutomationObject AObject;
    public String Data;
    public String Action;
    public String ObjectName;
    public String Reference;
    public Locator Locator;

    public ObjectGroup<ImageORObject> imageObjectGroup;

    public String Condition;
    public String Description;
    public String Input;
    public TestCaseReport Report;
    public UserDataAccess userData;
    private HashMap<String, String> runTimeVars = new HashMap<>();
    private Stack<Locator> runTimeElement = new Stack<>();
    
    public MobileObject MObject;
    public WebDriverCreation webDriver;
    public WebElement Element;

    public CommandControl(PlaywrightDriverCreation playwright, PlaywrightDriverCreation page, PlaywrightDriverCreation browserContext ,WebDriverCreation driver,TestCaseReport report) {
        Playwright = playwright;
        BrowserContext = browserContext;
        Page = page;
        webDriver = driver;
        userData = new UserDataAccess() {
            @Override
            public TestCaseRunner context() {
                return (TestCaseRunner) CommandControl.this.context();
            }
        };
        if(webDriver==null)
        {
           AObject = new AutomationObject(Page.page); 
        }
        else if(webDriver!=null)
        {
           MObject=new MobileObject(webDriver.driver); 
        }
        Report = (TestCaseReport) report;

    }

    public void refresh() {
        Data = ObjectName = Condition = Description = Input = Reference = Action = "";
        Locator = null;
        imageObjectGroup = null;
    }

    public void sync(Step curr) throws UnCaughtException {
        if(webDriver==null)
        {
        refresh();
        //AObject.setDriver(seDriver.driver);
        this.Description = curr.Description;
        this.Action = curr.Action;
        this.Input = curr.Input;
        this.Data = curr.Data;

        /********** Updates the Action for NLP_locator****************/
        AutomationObject.Action = this.Action;
        /**************************************************************/
        
        if (curr.Condition != null && curr.Condition.length() > 0) {
            this.Condition = curr.Condition;
        }

        if (curr.ObjectName != null && curr.ObjectName.length() > 0) {
            this.ObjectName = curr.ObjectName.trim();

            if (!(ObjectName.matches("(?i:app|browser|execute|executeclass)"))) {
                this.Reference = curr.Reference;
                if (!curr.Action.startsWith("img")) {
                    if (canIFindElement()) {
                        
                        Locator = AObject.findElement(ObjectName, Reference, FindType.fromString(Condition));
                        
                    }
                }
            }
        }
    }
         else
    { 
       refresh();
//        mobileObject.setDriver(mobileDriver.driver);
        this.Description = curr.Description;
        this.Action = curr.Action;
        this.Input = curr.Input;
        this.Data = curr.Data;

        /********** Updates the Action for NLP_locator****************/
        MobileObject.Action = this.Action;
        /**************************************************************/

        if (curr.Condition != null && curr.Condition.length() > 0) {
            this.Condition = curr.Condition;
        }

        if (curr.ObjectName != null && curr.ObjectName.length() > 0) {
            this.ObjectName = curr.ObjectName.trim();

            if (!(ObjectName.matches("(?i:app|browser|execute|executeclass)"))) {
                this.Reference = curr.Reference;
                if (!curr.Action.startsWith("img")) {
                    if (canIFindElement()) {
                        Element = MObject.findElement(ObjectName, Reference, FindmType.fromString(Condition));


                    }
                }
            }
        } 
    }
    }

    private Boolean canIFindElement() {
        if(webDriver!=null)
        {
        if(webDriver.isAlive())
        {
            if (webDriver.getCurrentBrowser().equalsIgnoreCase("ProtractorJS")) {
                return false;
            } else {
                switch (Action) {
                    case "waitForElementToBePresent":
                    case "setObjectProperty":
                        return false;
                    default:
                        return true;
                }
            }
        }
        }
        else
        {
        if (Page.isAlive()) {
                switch (Action) {
                    case "waitForElementToBePresent":
                    case "setObjectProperty":
                        return false;
                    default:
                        return true;
                }
            
        }
        }
        return false;
    }

    abstract public void execute(String com, int sub);

    abstract public void executeAction(String Action);

    abstract public Object context();

    public void addVar(String key, String val) {

        if (runTimeVars.containsKey(key)) {
            System.err.println("runTimeVars already contains " + key + ".Forcing change to " + val);
            System.out.println("Already contains " + key);

        }
        System.out.println("Adding to runTimeVars " + key + ":" + val);
        runTimeVars.put(key, val);

    }

    public String getVar(String key) {

        System.out.println("Getting runTimeVar " + key);
        String val = getDynamicValue(key);
        if (val == null) {
            System.err.println("runTimeVars does not contain " + key + ".Returning Empty");
            Report.updateTestLog("Get Var", "Getting From runTimeVars " + key + " Failed", Status.WARNING);
            return "";
        } else {
            return val;
        }

    }

    public String getDynamicValue(String key) {
        if (!runTimeVars.containsKey(key)) {
            key = key.matches("\\%(\\S)+\\%") ? key.substring(1, key.length() - 1) : key;
            return getUserDefinedData(key);
        }
        return runTimeVars.get(key);
    }

    public String getUserDefinedData(String key) {
        return Control.getCurrentProject().getProjectSettings().getUserDefinedSettings().getProperty(key);
    }

    public void putUserDefinedData(String key, String value) {
        Control.getCurrentProject().getProjectSettings().getUserDefinedSettings().put(key, value);
    }

    public Stack<Locator> getRunTimeElement() {
        return runTimeElement;
    }

    public void sync(Step curr, String subIter) throws Exception {
        curr.Data = DataProcessor.resolve(curr.Input, (TestCaseRunner) context(), subIter);
        sync(curr);
    }

    public Map<String, String> getRunTimeVars() {
        return runTimeVars;
    }

    public String getDBFile(String value){
        return Control.getCurrentProject().getProjectSettings().getDatabaseSettings().getDBLocation(value);
    }

    public Properties getDataBaseProperty(String key) {
        return Control.getCurrentProject().getProjectSettings().getDatabaseSettings().getDBPropertiesFor(key);
    }
    
    public Map<String, String> getProxySettings() {
        Map<String, String> systemSettings = new HashMap<>();
        DriverSettings settings = Control.getCurrentProject().getProjectSettings().getDriverSettings();
        systemSettings.put("proxySet", "true");            
        systemSettings.put("http.proxyHost", settings.getProperty("proxyHost"));            
        systemSettings.put("http.proxyPort", settings.getProperty("proxyPort"));
        systemSettings.put("http.proxyUser", settings.getProperty("proxyUser"));
        systemSettings.put("http.proxyPassword", settings.getProperty("proxyPassword"));
        return systemSettings;
    }
}

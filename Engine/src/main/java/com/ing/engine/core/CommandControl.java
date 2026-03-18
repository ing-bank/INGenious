
package com.ing.engine.core;

import com.ing.ingenious.api.contract.drivers.AutomationObjectApi;
import com.ing.datalib.or.common.ObjectGroup;
import com.ing.datalib.or.image.ImageORObject;
import com.ing.datalib.settings.DriverSettings;
import com.ing.datalib.util.data.LinkedProperties;
import com.ing.datalib.settings.DriverProperties;
import com.ing.engine.drivers.AutomationObject;
import com.ing.engine.drivers.PlaywrightDriverCreation;
import com.ing.engine.execution.data.DataProcessor;
import com.ing.engine.execution.data.UserDataAccess;
import com.ing.engine.execution.exception.UnCaughtException;
import com.ing.engine.execution.run.TestCaseRunner;
import com.ing.engine.reporting.TestCaseReport;
import com.ing.ingenious.api.status.Status;
import com.ing.engine.support.Step;
import com.microsoft.playwright.Locator;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

//Added For Mobile
import com.ing.engine.drivers.WebDriverCreation;
import com.ing.engine.drivers.MobileObject;
import com.ing.ingenious.api.contract.drivers.MobileObjectApi;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
                        
                        Locator = AObject.findElement(ObjectName, Reference, AutomationObjectApi.FindType.fromString(Condition));
                        
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
                        Element = MObject.findElement(ObjectName, Reference, MobileObjectApi.FindmType.fromString(Condition));


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
                    case "setMobileObjectProperty":
                    case "setMobileGlobalProperty":
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
                    case "setMobileObjectProperty":
                    case "setMobileGlobalProperty":
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
    
    public String getRuntimeVar(String key) {

        if (runTimeVars.containsKey(key)) {
             return getDynamicValue(key);
        }
        
        return null;
    }
    
    public String getVar(String key) {

        System.out.println("Getting runTimeVar " + key);
        String val = getDynamicValue(key);
        if (val == null) {
            System.err.println("runTimeVars does not contain " + key + ". Returning Empty");
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
    
    public String getDatasheet(String key) {

        System.out.println("Getting Datasheet " + key);
        String val = getDataSheetValue(key);
        if (val == null) {
            System.err.println("Datasheet does not contain " + key + ". Returning Empty");
            Report.updateTestLog("Get Datasheet", "Getting From Datasheet " + key + " Failed", Status.WARNING);
            return "";
        } else {
            return val;
        }
    }
    
    public String getDataSheetValue(String key){
        String val = null;
        key = key.matches("\\{(\\S)+\\}") ? key.substring(1, key.length() - 1) : key;
        List<String> sheetlist = Control.getCurrentProject().getTestData().getTestDataFor(Control.exe.runEnv())
                .getTestDataNames();
        for (int sheet = 0; sheet < sheetlist.size(); sheet++) {
            if (key.contains(sheetlist.get(sheet) + ":")) {
                com.ing.datalib.testdata.model.TestDataModel tdModel = Control.getCurrentProject()
                        .getTestData().getTestDataByName(sheetlist.get(sheet));
                List<String> columns = tdModel.getColumns();
                for (int col = 0; col < columns.size(); col++) {
                    if (key.contains(sheetlist.get(sheet) + ":" + columns.get(col))) {
                    	val = userData.getData(sheetlist.get(sheet), columns.get(col));
                    }
                }
            }
        }
        return val;
    }

    public String getUserDefinedData(String key) {
        return Control.getCurrentProject().getProjectSettings().getUserDefinedSettings().getProperty(key);
    }

    public void putUserDefinedData(String key, String value) {
        Control.getCurrentProject().getProjectSettings().getUserDefinedSettings().put(key, value);
        Control.getCurrentProject().getProjectSettings().getUserDefinedSettings().save();
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
        // DriverSettings settings = Control.getCurrentProject().getProjectSettings().getDriverSettings();
        DriverProperties settings = Control.getCurrentProject().getProjectSettings().getDriverSettings();
        systemSettings.put("proxySet", "true");            
        systemSettings.put("http.proxyHost", settings.getProperty("proxyHost"));            
        systemSettings.put("http.proxyPort", settings.getProperty("proxyPort"));
        systemSettings.put("http.proxyUser", settings.getProperty("proxyUser"));
        systemSettings.put("http.proxyPassword", settings.getProperty("proxyPassword"));
        return systemSettings;
    }
    
    public static List<String> smartCommaSplitter(String strInput){
        List<String> result = new ArrayList();
        StringBuilder currentStr = new StringBuilder();
        
        boolean inQuotes = false;
        boolean inBraces = false;
        boolean inPercent = false;
        
        for(int i = 0; i < strInput.length(); i++){
            char c = strInput.charAt(i);
            
            if(c == '%' && !inQuotes && !inBraces){
                inPercent = !inPercent;
            }
            
            if(c == '"' && !inPercent && !inBraces){
                inQuotes = !inQuotes;
            }
            
            if(c == '{' && !inQuotes && !inPercent){
                inBraces = true;
            } else if(c == '}' && !inQuotes && !inPercent){
                inBraces = false;
            }
            
            if(c == ',' && !inQuotes && !inPercent&& !inBraces){
                result.add(currentStr.toString());
                currentStr.setLength(0);
            } else {
                currentStr.append(c);
            }
        }
        
        if (currentStr.length() > 0) {
            result.add(currentStr.toString());
        }
        
        return result;
    }
    
    
    /**
     * Detects all runtime variable keys marked with percent signs (%) in the input string
     * and returns them as a set.
     *
     * <p>Runtime variable keys are identified by surrounding percent signs (e.g., %KEY%).</p>
     *
     * @param str the input string to be evaluated
     * @return a set containing all detected runtime variable keys, including the percent signs
     */
    public static HashSet<String> getAllRuntimeNameVars(String str){
        Pattern pattern = Pattern.compile("%(\\S+?)%");
        Matcher matcher = pattern.matcher(str);
        HashSet<String> runtimeVars = new HashSet<>();
         
        int searchStart = 0;

        while (searchStart < str.length()) {
            matcher.region(searchStart, str.length());
            if (matcher.find()) {
                int startIndex = matcher.start();
                int endIndex = matcher.end();
                
                // Move searchStart past the current match
                searchStart = matcher.end();
                runtimeVars.add(str.substring(startIndex, endIndex));
            } else {
                break;
            }
        }
        
        return runtimeVars;
    }
    
    
    /**
     * Resolves all runtime variables marked with percent signs (%) in the input string,
     * including user-defined variables.
     *
     * <p>If no runtime variables are present, the original string is returned unchanged.</p>
     *
     * @param str the input string to evaluate; may or may not contain runtime variables
     * @return a string with all detected runtime variables replaced by their resolved values,
     *         or the original string if none are found
     */ 
    public String resolveAllRuntimeVars(String str) {
        HashSet<String> keys = getAllRuntimeNameVars(str);
        for (String key : keys) {
            String runtimeValue = getVar(key);
            str=str.replace(key, runtimeValue);
        }
        return str;
    }

    /**
     * Checks if a runtime or user-defined variable exists.
     * 
     * <p>This method verifies whether a variable is defined in either the runtime variables map
     * or the user-defined settings. The key can be provided with or without percent signs.</p>
     * 
     * <p>Variable resolution order:
     * <ol>
     *   <li>Runtime variables (set during test execution via addVar)</li>
     *   <li>User-defined variables (configured in project settings)</li>
     * </ol>
     * 
     * <p>Example usage:
     * <ul>
     *   <li>isVarExist("%filePath%") - checks if filePath variable exists</li>
     *   <li>isVarExist("filePath") - equivalent to above</li>
     * </ul>
     * 
     * @param key the variable key to check, with or without percent signs (e.g., "%varName%" or "varName")
     * @return true if the variable exists and has a non-null value, false otherwise
     */
    public boolean isVarExist(String key) {
        String val = getDynamicValue(key);
        if (val == null) { return false; } return true;
    }
}

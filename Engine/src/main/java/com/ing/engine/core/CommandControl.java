package com.ing.engine.core;

import com.ing.datalib.or.common.ObjectGroup;
import com.ing.datalib.or.image.ImageORObject;
import com.ing.datalib.sap.SapCompatibility;
import com.ing.datalib.settings.DriverProperties;
import com.ing.datalib.settings.DriverSettings;
import com.ing.datalib.util.data.LinkedProperties;
import com.ing.engine.drivers.AutomationObject;
import com.ing.engine.drivers.MobileObject;
import com.ing.engine.drivers.PlaywrightDriverCreation;
import com.ing.engine.drivers.SAPObject;
import com.ing.engine.drivers.SAPObject.SAPFindType;
import com.ing.engine.drivers.StructuredDataObject;
//Added For Mobile
import com.ing.engine.drivers.WebDriverCreation;
import com.ing.engine.drivers.sap.SapGuiSession;
import com.ing.engine.drivers.sap.SapSessionManager;
import com.ing.engine.execution.data.DataProcessor;
import com.ing.engine.execution.data.UserDataAccess;
import com.ing.engine.execution.exception.UnCaughtException;
import com.ing.engine.execution.run.TestCaseRunner;
import com.ing.engine.reporting.TestCaseReport;
import com.ing.engine.support.Step;
import com.ing.engine.support.methodInf.MethodInfoManager;
import com.ing.ingenious.api.contract.drivers.AutomationObjectApi;
import com.ing.ingenious.api.contract.drivers.MobileObjectApi;
import com.ing.ingenious.api.status.Status;
import com.jacob.com.Dispatch;
import com.microsoft.playwright.Locator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;
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
    public StructuredDataObject SObject;
    public WebDriverCreation webDriver;
    public WebElement Element;
    public String structuredData;

    //For SAPTesting
    public SAPObject SAPObject;
    public Dispatch SAPElement;
    public Process SAPProcess;

    /** The four SAP connection-lifecycle actions, exempt from the "no SAP connection" fail-fast. */
    private static final java.util.Set<String> SAP_CONNECTION_ACTIONS = new HashSet<>(
        Arrays.asList(
            "sapInitConnection",
            "sapSwitchConnection",
            "sapCloseConnection",
            "sapCloseAllConnection"
        )
    );

    public CommandControl(
        PlaywrightDriverCreation playwright,
        PlaywrightDriverCreation page,
        PlaywrightDriverCreation browserContext,
        WebDriverCreation driver,
        TestCaseReport report
    ) {
        Playwright = playwright;
        BrowserContext = browserContext;
        Page = page;
        webDriver = driver;
        userData =
            new UserDataAccess() {

                @Override
                public TestCaseRunner context() {
                    return (TestCaseRunner) CommandControl.this.context();
                }
            };

        if (webDriver == null) {
            if (Page != null && Page.page != null) {
                AObject = new AutomationObject(Page.page);
                SObject = new StructuredDataObject(Page.page);
            }
        } else if (webDriver.driver != null) {
            MObject = new MobileObject(webDriver.driver);
        }
        // SAP objects are bound lazily once SAP.initConnection has run (driverless model).
        if (isSapMode()) {
            bindSapSession();
        }
        // STRUCTUREDDATA actions (JSON/XML path assertions on Webservice responses)
        // are driver-agnostic. Ensure SObject is always initialized so OR references
        // can be resolved regardless of execution mode (Playwright, WebDriver, SAP,
        // or API-only where a WebDriverCreation wrapper exists but no browser was
        // launched).
        if (SObject == null) {
            SObject = new StructuredDataObject();
        }
        Report = (TestCaseReport) report;
    }

    /** @return true when a SAP connection is open for this runner thread. */
    public boolean isSapMode() {
        return SapSessionManager.INSTANCE.hasConnection();
    }

    private SapGuiSession sapGuiSession() {
        return SapSessionManager.INSTANCE.current();
    }

    /** The raw {@code ActiveXComponent} of the current SAP session, or {@code null}. */
    public Object currentSapRaw() {
        SapGuiSession s = sapGuiSession();
        return s == null ? null : s.raw();
    }

    /** (Re)build {@link #SAPObject} from the current SAP session; clears it when not in SAP mode. */
    public void bindSapSession() {
        if (isSapMode()) {
            SAPObject = new SAPObject(sapGuiSession());
            // null when the connection was adopted, or opened on someone else's engine -
            // there is no process this run launched, so nothing sapCloseLogonScreen should kill.
            SAPProcess = SapSessionManager.INSTANCE.currentProcess();
        } else {
            SAPObject = null;
            SAPProcess = null;
        }
    }

    public void refresh() {
        Data = ObjectName = Condition = Description = Input = Reference = Action = "";
        Locator = null;
        imageObjectGroup = null;
        //For SAPTesting
        SAPElement = null;
    }

    public void sync(Step curr) throws UnCaughtException {
        refresh();
        this.Description = curr.Description;
        this.Action = curr.Action;
        this.Input = curr.Input;
        this.Data = curr.Data;

        // A SAP operation with no connection open: fail fast, don't NPE later.
        if (
            curr.Action != null &&
            curr.Action.startsWith("sap") &&
            !SAP_CONNECTION_ACTIONS.contains(curr.Action) &&
            !isSapMode()
        ) {
            Report.updateTestLog(
                curr.Action,
                "No SAP connection - add a SAP.initConnection step.",
                Status.FAILNS
            );
            return;
        }

        // Guardrail: an archetype needing its own live device/broker/remote driver can't share
        // a test case with an open SAP connection - fail fast with the fix instead of NPEing.
        if (isSapMode() && curr.Action != null) {
            com.ing.ingenious.api.annotation.Action actionMeta = MethodInfoManager.getActionFor(
                curr.Action
            );
            String actionObjectType = actionMeta != null ? actionMeta.object() : null;
            if (SapCompatibility.isBlockedWithSap(actionObjectType)) {
                Report.updateTestLog(
                    curr.Action,
                    SapCompatibility.blockedReasonMessage(actionObjectType),
                    Status.FAILNS
                );
                return;
            }
        }

        if (curr.Condition != null && curr.Condition.length() > 0) {
            this.Condition = curr.Condition;
        }

        if (curr.ObjectName != null && curr.ObjectName.length() > 0) {
            this.ObjectName = curr.ObjectName.trim();

            if (!(ObjectName.matches("(?i:app|browser|execute|executeclass)"))) {
                this.Reference = curr.Reference;
                if (!curr.Action.startsWith("img")) {
                    // While a SAP connection is open, non-SAP steps (General, DB, ...)
                    // resolve nothing against SAP - the handler proceeds without an object.
                    if (isSapMode() && !isSAPAction()) {
                        return;
                    }

                    // SAP element finding is hoisted ahead of the web/mobile branches
                    // because a "No Browser" run still carries a (non-driving) webDriver.
                    if (isSapMode() && isSAPAction()) {
                        if (SAPObject == null) {
                            bindSapSession();
                        }
                        SAPObject.Action = this.Action;
                        SAPElement =
                            SAPObject.findSAPElement(
                                ObjectName,
                                Reference,
                                SAPFindType.fromString(Condition)
                            );
                        return;
                    }

                    if (canIFindElement()) {
                        if (SObject != null) {
                            structuredData = SObject.findElement(ObjectName, Reference);
                        }
                        if (structuredData != null) {
                            StructuredDataObject.Action = this.Action;

                            Data = structuredData;
                        } else if (webDriver == null && AObject != null) {
                            /********** Updates the Action for NLP_locator****************/
                            AutomationObject.Action = this.Action;
                            /**************************************************************/

                            Locator =
                                AObject.findElement(
                                    ObjectName,
                                    Reference,
                                    AutomationObjectApi.FindType.fromString(Condition)
                                );
                        } else if (webDriver != null && MObject != null) {
                            /********** Updates the Action for NLP_locator****************/
                            MobileObject.Action = this.Action;
                            /**************************************************************/

                            Element =
                                MObject.findElement(
                                    ObjectName,
                                    Reference,
                                    MobileObjectApi.FindmType.fromString(Condition)
                                );
                        }
                    }
                }
            }
        }
    }

    /**
     * Checks if the current step requires SAP object finding.
     * A step is SAP-specific if the ObjectName exists in the SAP Object Repository.
     *
     * <p>This is used when in SAP mode (SAPsession != null) to determine whether
     * to attempt finding a SAP object. If the object doesn't exist in SAP OR,
     * it's a non-SAP action (General, Database, etc.) and should skip object finding.</p>
     *
     * @return true if ObjectName exists in SAP OR, false otherwise
     */
    private boolean isSAPAction() {
        // If no ObjectName, it's not a SAP action
        if (ObjectName == null || ObjectName.isEmpty()) {
            return false;
        }

        // If no Reference (page name), it's not a SAP action
        if (Reference == null || Reference.isEmpty()) {
            return false;
        }

        if (SAPObject == null && isSapMode()) {
            bindSapSession();
        }

        // Check if this object exists in the SAP Object Repository
        if (SAPObject != null) {
            return SAPObject.getSapObject(Reference, ObjectName) != null;
        }

        return false;
    }

    private Boolean canIFindElement() {
        // SAP is checked first: a "No Browser" run still carries a non-driving webDriver.
        if (isSapMode()) {
            if (!isSAPAction()) {
                return false;
            }
            return ObjectName != null && !ObjectName.isEmpty();
        }
        if (webDriver != null) {
            if (webDriver.isAlive()) {
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
        } else {
            if (Page != null && Page.isAlive()) {
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
            // API / Structured-Data flows have no browser, no SAP session and no
            // Playwright page, but the test step may still reference a Structured
            // Data OR object (e.g. JsonPath / XPath assertions on a REST response).
            // Allow the OR lookup when an SD reference is present so the engine
            // resolves Object/Reference -> JsonPath/Xpath into `Data` for the
            // STRUCTUREDDATA actions.
            if (
                SObject != null &&
                ObjectName != null &&
                !ObjectName.isEmpty() &&
                Reference != null &&
                !Reference.isEmpty()
            ) {
                return true;
            }
        }
        return false;
    }

    public abstract void execute(String com, int sub);

    public abstract void executeAction(String Action);

    public abstract Object context();

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
            Report.updateTestLog(
                "Get Var",
                "Getting From runTimeVars " + key + " Failed",
                Status.WARNING
            );
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
            Report.updateTestLog(
                "Get Datasheet",
                "Getting From Datasheet " + key + " Failed",
                Status.WARNING
            );
            return "";
        } else {
            return val;
        }
    }

    public String getDataSheetValue(String key) {
        String val = null;
        key = key.matches("\\{(\\S)+\\}") ? key.substring(1, key.length() - 1) : key;
        List<String> sheetlist = Control
            .getCurrentProject()
            .getTestData()
            .getTestDataFor(Control.exe.runEnv())
            .getTestDataNames();
        for (int sheet = 0; sheet < sheetlist.size(); sheet++) {
            if (key.contains(sheetlist.get(sheet) + ":")) {
                com.ing.datalib.testdata.model.TestDataModel tdModel = Control
                    .getCurrentProject()
                    .getTestData()
                    .getTestDataByName(sheetlist.get(sheet));
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
        return Control
            .getCurrentProject()
            .getProjectSettings()
            .getUserDefinedSettings()
            .getProperty(key);
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
        applyInlineObjectProperties(curr, subIter);
        sync(curr);
    }

    /**
     * Applies an <b>inline object-property override</b> carried in a locator step's
     * Condition column (see {@link InlineObjectProperty}). Each {@code #token=value}
     * pair's value is resolved through the normal data pipeline and written into the
     * active driver's dynamic-value map <em>before</em> the element is found, so the
     * locator placeholders are substituted for this step without a separate
     * {@code setObjectProperty} step.
     *
     * <p>The Condition value is then "consumed": for the object-scoped variant it is
     * cleared (find type {@code DEFAULT}); for the global variant it is replaced with
     * {@code GlobalObject} so the object resolves against the global map.</p>
     */
    private void applyInlineObjectProperties(Step curr, String subIter) {
        String condition = curr.Condition;
        if (!InlineObjectProperty.isInline(condition)) {
            return;
        }
        boolean global = InlineObjectProperty.isGlobal(condition);
        // Log under a dedicated action label so the report does not repeat the real
        // action (e.g. "Fill") for the property-setting sub-step.
        String logAction = global ? "setGlobalObjectProperty" : "setObjectProperty";
        List<String[]> pairs = InlineObjectProperty.parsePairs(
            InlineObjectProperty.stripMarker(condition)
        );

        // Consume the marker so downstream find-type parsing is not confused by it.
        curr.Condition = global ? InlineObjectProperty.GLOBAL_FIND_TYPE : "";

        if (pairs.isEmpty()) {
            Report.updateTestLog(
                logAction,
                "Inline property override has no valid '#token=value' pairs; skipped",
                Status.DEBUG
            );
            return;
        }
        if (!global && (isBlank(curr.ObjectName) || isBlank(curr.Reference))) {
            Report.updateTestLog(
                logAction,
                "Inline object property requires an Object Repository element; skipped",
                Status.DEBUG
            );
            return;
        }

        StringBuilder applied = new StringBuilder();
        for (String[] pair : pairs) {
            String token = pair[0];
            // A per-token |subiter=N overrides the step's own sub-iteration for
            // data-sheet lookups; otherwise fall back to the step sub-iteration.
            String pairSubIter = (pair.length > 2 && pair[2] != null && !pair[2].isEmpty())
                ? pair[2]
                : subIter;
            String resolved;
            try {
                resolved = DataProcessor.resolve(pair[1], (TestCaseRunner) context(), pairSubIter);
            } catch (Exception ex) {
                resolved = pair[1];
            }
            applyInlineProperty(global, curr.Reference, curr.ObjectName, token, resolved);
            if (applied.length() > 0) {
                applied.append("; ");
            }
            applied.append(token).append('=').append(resolved);
        }
        Report.updateTestLog(
            logAction,
            String.format(
                "Inline %s applied [%s]",
                global ? "global property" : "object property",
                applied
            ),
            Status.DONE
        );
    }

    /** Writes a resolved token/value into the active driver's dynamic-value map(s). */
    private void applyInlineProperty(
        boolean global,
        String reference,
        String objectName,
        String key,
        String value
    ) {
        if (global) {
            if (isSapMode() && SAPObject != null) {
                SAPObject.globalDynamicValue.put(key, value);
            } else if (webDriver != null && MObject != null) {
                MobileObject.globalDynamicValue.put(key, value);
            } else {
                AutomationObject.globalDynamicValue.put(key, value);
                StructuredDataObject.globalDynamicValue.put(key, value);
            }
        } else {
            if (isSapMode() && SAPObject != null) {
                InlineObjectProperty.putObjectProperty(
                    SAPObject.dynamicValue,
                    reference,
                    objectName,
                    key,
                    value
                );
            } else if (webDriver != null && MObject != null) {
                InlineObjectProperty.putObjectProperty(
                    MobileObject.dynamicValue,
                    reference,
                    objectName,
                    key,
                    value
                );
            } else {
                InlineObjectProperty.putObjectProperty(
                    AutomationObject.dynamicValue,
                    reference,
                    objectName,
                    key,
                    value
                );
                InlineObjectProperty.putObjectProperty(
                    StructuredDataObject.dynamicValue,
                    reference,
                    objectName,
                    key,
                    value
                );
            }
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    public Map<String, String> getRunTimeVars() {
        return runTimeVars;
    }

    public String getDBFile(String value) {
        return Control
            .getCurrentProject()
            .getProjectSettings()
            .getDatabaseSettings()
            .getDBLocation(value);
    }

    public Properties getDataBaseProperty(String key) {
        return Control
            .getCurrentProject()
            .getProjectSettings()
            .getDatabaseSettings()
            .getDBPropertiesFor(key);
    }

    public Map<String, String> getProxySettings() {
        Map<String, String> systemSettings = new HashMap<>();
        // DriverSettings settings = Control.getCurrentProject().getProjectSettings().getDriverSettings();
        DriverProperties settings = Control
            .getCurrentProject()
            .getProjectSettings()
            .getDriverSettings();
        systemSettings.put("proxySet", "true");
        systemSettings.put("http.proxyHost", settings.getProperty("proxyHost"));
        systemSettings.put("http.proxyPort", settings.getProperty("proxyPort"));
        systemSettings.put("http.proxyUser", settings.getProperty("proxyUser"));
        systemSettings.put("http.proxyPassword", settings.getProperty("proxyPassword"));
        return systemSettings;
    }

    public static List<String> smartCommaSplitter(String strInput) {
        List<String> result = new ArrayList();
        StringBuilder currentStr = new StringBuilder();

        boolean inQuotes = false;
        boolean inBraces = false;
        boolean inPercent = false;

        for (int i = 0; i < strInput.length(); i++) {
            char c = strInput.charAt(i);

            if (c == '%' && !inQuotes && !inBraces) {
                inPercent = !inPercent;
            }

            if (c == '"' && !inPercent && !inBraces) {
                inQuotes = !inQuotes;
            }

            if (c == '{' && !inQuotes && !inPercent) {
                inBraces = true;
            } else if (c == '}' && !inQuotes && !inPercent) {
                inBraces = false;
            }

            if (c == ',' && !inQuotes && !inPercent && !inBraces) {
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
    public static HashSet<String> getAllRuntimeNameVars(String str) {
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
            str = str.replace(key, runtimeValue);
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
        if (val == null) {
            return false;
        }
        return true;
    }
}

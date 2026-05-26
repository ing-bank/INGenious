package com.ing.ide.main.sapscript.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Abstract base class for language-specific SAP GUI Script parsers.
 * Each supported language (VBScript, JavaScript, PowerShell, Python, AutoIt) 
 * extends this class to implement language-specific parsing rules.
 */
public abstract class SapLanguageParser {
    
    protected static final Logger LOGGER = Logger.getLogger(SapLanguageParser.class.getName());
    
    protected Map<String, SapObject> sapObjects = new LinkedHashMap<>();
    protected List<SapAction> sapActions = new ArrayList<>();
    
    // Statistics tracking
    protected int linesProcessed = 0;
    protected int linesParsed = 0;
    protected List<String> warnings = new ArrayList<>();
    
    /**
     * Get the language name for this parser.
     */
    public abstract String getLanguageName();
    
    /**
     * Get the file extensions supported by this parser.
     */
    public abstract String[] getSupportedExtensions();
    
    /**
     * Check if a line is a comment in this language.
     */
    protected abstract boolean isComment(String line);
    
    /**
     * Get the session variable prefix for this language (e.g., "$" for PowerShell).
     */
    protected abstract String getSessionPrefix();
    
    /**
     * Parse a SAP script file and extract objects and actions.
     */
    public void parse(File file) throws IOException {
        LOGGER.info("Parsing SAP Script file with " + getLanguageName() + " parser: " + file.getAbsolutePath());
        
        // Reset statistics
        linesProcessed = 0;
        linesParsed = 0;
        warnings.clear();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNumber = 0;
            String currentTransaction = null;
            int actionCountBefore;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                linesProcessed++;
                String trimmedLine = line.trim();
                
                // Skip empty lines and comments
                if (trimmedLine.isEmpty() || isComment(trimmedLine)) {
                    continue;
                }
                
                actionCountBefore = sapActions.size();
                
                // Extract transaction
                String transaction = extractTransaction(trimmedLine);
                if (transaction != null) {
                    currentTransaction = transaction;
                    LOGGER.fine("Found transaction: " + currentTransaction);
                    sapActions.add(new SapAction("Transaction", "SAP_SYSTEM", currentTransaction, lineNumber));
                    linesParsed++;
                    continue;
                }
                
                // Parse SAP GUI actions
                parseSapAction(trimmedLine, lineNumber, currentTransaction);
                
                // If an action was added, count this line as parsed
                if (sapActions.size() > actionCountBefore) {
                    linesParsed++;
                }
            }
        }
        
        LOGGER.info(String.format("Parsed %d SAP objects and %d actions from %s script", 
            sapObjects.size(), sapActions.size(), getLanguageName()));
    }
    
    /**
     * Extract transaction code from a line (e.g., session.startTransaction("VA03")).
     */
    protected String extractTransaction(String line) {
        String prefix = getSessionPrefix();
        Pattern pattern = Pattern.compile(
            Pattern.quote(prefix) + "session\\.startTransaction\\s*\\(?\\s*\"([^\"]+)\"\\s*\\)?", 
            Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    /**
     * Parse a single line for SAP actions.
     * This method should be overridden by language-specific parsers if needed.
     */
    protected void parseSapAction(String line, int lineNumber, String transaction) {
        String prefix = getSessionPrefix();
        
        // Try to match setText action
        if (parseSetTextAction(line, lineNumber, transaction, prefix)) return;
        
        // Try to match press action
        if (parsePressAction(line, lineNumber, transaction, prefix)) return;
        
        // Try to match dropdown key selection
        if (parseDropdownKeyAction(line, lineNumber, transaction, prefix)) return;
        
        // Try to match dropdown select by index
        if (parseDropdownSelectAction(line, lineNumber, transaction, prefix)) return;
        
        // Try to match combo value action
        if (parseComboValueAction(line, lineNumber, transaction, prefix)) return;
        
        // Try to match select action
        if (parseSelectAction(line, lineNumber, transaction, prefix)) return;
        
        // Try to match tab select action
        if (parseTabSelectAction(line, lineNumber, transaction, prefix)) return;
        
        // Try to match setFocus action
        if (parseSetFocusAction(line, lineNumber, transaction, prefix)) return;
        
        // Try to match sendVKey action
        if (parseSendVKeyAction(line, lineNumber, transaction, prefix)) return;
        
        // Try to match double click on current cell
        if (parseDoubleClickCellAction(line, lineNumber, transaction, prefix)) return;
        
        // Try to match general doubleClick action
        if (parseDoubleClickAction(line, lineNumber, transaction, prefix)) return;
        
        // Try to match modifyCell action
        if (parseModifyCellAction(line, lineNumber, transaction, prefix)) return;
        
        // Try to match setCurrentCell action
        if (parseSetCurrentCellAction(line, lineNumber, transaction, prefix)) return;
        
        // Generic findById for objects not yet handled
        if (parseFindByIdAction(line, transaction, prefix)) return;
        
        // Try to capture general property assignments
        parsePropertyAssignment(line, prefix);
    }
    
    protected boolean parseSetTextAction(String line, int lineNumber, String transaction, String prefix) {
        Pattern pattern = Pattern.compile(
            Pattern.quote(prefix) + "session\\.findById\\(\"([^\"]+)\"\\)\\.(?:text|Text)\\s*=\\s*\"([^\"]*)\"", 
            Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            String id = matcher.group(1);
            String value = matcher.group(2);
            String objType = determineObjectType(id);
            addSapObject(id, objType, transaction);
            // Text value should NOT be stored in SAP Object - it goes in test case Input column only
            // addPropertyToSapObject(id, "text", value); // REMOVED
            sapActions.add(new SapAction("Set", id, value, lineNumber));
            return true;
        }
        return false;
    }
    
    protected boolean parsePressAction(String line, int lineNumber, String transaction, String prefix) {
        Pattern pattern = Pattern.compile(
            Pattern.quote(prefix) + "session\\.findById\\(\"([^\"]+)\"\\)\\.press\\(\\)", 
            Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            String id = matcher.group(1);
            addSapObject(id, "Button", transaction);
            sapActions.add(new SapAction("Click", id, "", lineNumber));
            return true;
        }
        return false;
    }
    
    protected boolean parseDropdownKeyAction(String line, int lineNumber, String transaction, String prefix) {
        Pattern pattern = Pattern.compile(
            Pattern.quote(prefix) + "session\\.findById\\(\"([^\"]+)\"\\)\\.Key\\s*=\\s*\"([^\"]+)\"", 
            Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            String id = matcher.group(1);
            String key = matcher.group(2);
            addSapObject(id, "ComboBox", transaction);
            sapActions.add(new SapAction("SelectDropDownByKey", id, key, lineNumber));
            return true;
        }
        return false;
    }
    
    protected boolean parseDropdownSelectAction(String line, int lineNumber, String transaction, String prefix) {
        Pattern pattern = Pattern.compile(
            Pattern.quote(prefix) + "session\\.findById\\(\"([^\"]+)\"\\)\\.Select\\s*\\(?\\s*(\\d+)\\s*\\)?", 
            Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            String id = matcher.group(1);
            String index = matcher.group(2);
            addSapObject(id, "ComboBox", transaction);
            sapActions.add(new SapAction("SelectDropDownByIndex", id, index, lineNumber));
            return true;
        }
        return false;
    }
    
    protected boolean parseComboValueAction(String line, int lineNumber, String transaction, String prefix) {
        Pattern pattern = Pattern.compile(
            Pattern.quote(prefix) + "session\\.findById\\(\"([^\"]+)\"\\)\\.value\\s*=\\s*\"([^\"]*)\"", 
            Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            String id = matcher.group(1);
            String value = matcher.group(2);
            String objType = determineObjectType(id);
            if (objType.equals("ComboBox")) {
                addSapObject(id, "ComboBox", transaction);
                sapActions.add(new SapAction("SelectDropDownByText", id, value, lineNumber));
                return true;
            }
        }
        return false;
    }
    
    protected boolean parseSelectAction(String line, int lineNumber, String transaction, String prefix) {
        Pattern pattern = Pattern.compile(
            Pattern.quote(prefix) + "session\\.findById\\(\"([^\"]+)\"\\)\\.selected\\s*=\\s*(true|false|-?\\d+|\\$true|\\$false)", 
            Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            String id = matcher.group(1);
            String selected = matcher.group(2);
            String objType = determineObjectType(id);
            
            if (objType.equals("Checkbox")) {
                addSapObject(id, "Checkbox", transaction);
                sapActions.add(new SapAction("SelectCheckBox", id, selected, lineNumber));
            } else if (objType.equals("RadioButton")) {
                addSapObject(id, "RadioButton", transaction);
                sapActions.add(new SapAction("SelectRadioButton", id, selected, lineNumber));
            } else if (objType.equals("Tab")) {
                addSapObject(id, "Tab", transaction);
                sapActions.add(new SapAction("SelectTab", id, selected, lineNumber));
            } else {
                addSapObject(id, objType, transaction);
                sapActions.add(new SapAction("Select", id, selected, lineNumber));
            }
            return true;
        }
        return false;
    }
    
    protected boolean parseTabSelectAction(String line, int lineNumber, String transaction, String prefix) {
        Pattern pattern = Pattern.compile(
            Pattern.quote(prefix) + "session\\.findById\\(\"([^\"]+)\"\\)\\.select\\(\\)", 
            Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            String id = matcher.group(1);
            addSapObject(id, "Tab", transaction);
            sapActions.add(new SapAction("SelectTab", id, "", lineNumber));
            return true;
        }
        return false;
    }
    
    protected boolean parseSetFocusAction(String line, int lineNumber, String transaction, String prefix) {
        Pattern pattern = Pattern.compile(
            Pattern.quote(prefix) + "session\\.findById\\(\"([^\"]+)\"\\)\\.setFocus\\(\\)", 
            Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            String id = matcher.group(1);
            addSapObject(id, "Element", transaction);
            sapActions.add(new SapAction("SetFocus", id, "", lineNumber));
            return true;
        }
        return false;
    }
    
    protected boolean parseSendVKeyAction(String line, int lineNumber, String transaction, String prefix) {
        Pattern pattern = Pattern.compile(
            Pattern.quote(prefix) + "session\\.findById\\(\"([^\"]+)\"\\)\\.sendVKey\\s*\\(?\\s*(\\d+)\\s*\\)?", 
            Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            String id = matcher.group(1);
            String vkey = matcher.group(2);
            addSapObject(id, "Window", transaction);
            sapActions.add(new SapAction("SendVKey", id, vkey, lineNumber));
            return true;
        }
        return false;
    }
    
    protected boolean parseDoubleClickCellAction(String line, int lineNumber, String transaction, String prefix) {
        Pattern pattern = Pattern.compile(
            Pattern.quote(prefix) + "session\\.findById\\(\"([^\"]+)\"\\)\\.doubleClickCurrentCell", 
            Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            String id = matcher.group(1);
            addSapObject(id, "Table", transaction);
            sapActions.add(new SapAction("DoubleClickCell", id, "", lineNumber));
            return true;
        }
        return false;
    }
    
    protected boolean parseDoubleClickAction(String line, int lineNumber, String transaction, String prefix) {
        Pattern pattern = Pattern.compile(
            Pattern.quote(prefix) + "session\\.findById\\(\"([^\"]+)\"\\)\\.doubleClick\\(\\)", 
            Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            String id = matcher.group(1);
            addSapObject(id, "Element", transaction);
            sapActions.add(new SapAction("DoubleClick", id, "", lineNumber));
            return true;
        }
        return false;
    }
    
    protected boolean parseModifyCellAction(String line, int lineNumber, String transaction, String prefix) {
        Pattern pattern = Pattern.compile(
            Pattern.quote(prefix) + "session\\.findById\\(\"([^\"]+)\"\\)\\.modifyCell\\s*\\(\\s*(\\d+)\\s*,\\s*\"([^\"]+)\"\\s*,\\s*\"([^\"]*)\"\\s*\\)", 
            Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            String id = matcher.group(1);
            String row = matcher.group(2);
            String column = matcher.group(3);
            String value = matcher.group(4);
            addSapObject(id, "Table", transaction);
            sapActions.add(new SapAction("ModifyCell", id, row + "," + column + "," + value, lineNumber));
            return true;
        }
        return false;
    }
    
    protected boolean parseSetCurrentCellAction(String line, int lineNumber, String transaction, String prefix) {
        Pattern pattern = Pattern.compile(
            Pattern.quote(prefix) + "session\\.findById\\(\"([^\"]+)\"\\)\\.currentCellRow\\s*=\\s*(\\d+)", 
            Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            String id = matcher.group(1);
            String row = matcher.group(2);
            addSapObject(id, "Table", transaction);
            sapActions.add(new SapAction("SetCurrentCell", id, row, lineNumber));
            return true;
        }
        return false;
    }
    
    protected boolean parseFindByIdAction(String line, String transaction, String prefix) {
        Pattern pattern = Pattern.compile(
            Pattern.quote(prefix) + "session\\.findById\\(\"([^\"]+)\"\\)", 
            Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            String id = matcher.group(1);
            addSapObject(id, "Element", transaction);
            return true;
        }
        return false;
    }
    
    protected void parsePropertyAssignment(String line, String prefix) {
        Pattern pattern = Pattern.compile(
            Pattern.quote(prefix) + "session\\.findById\\(\"([^\"]+)\"\\)\\.([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*\"([^\"]*)\"", 
            Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            String id = matcher.group(1);
            String propertyName = matcher.group(2);
            String propertyValue = matcher.group(3);
            
            // Skip if this is an action property
            String propLower = propertyName.toLowerCase();
            if (!propLower.equals("text") && !propLower.equals("selected") && !propLower.equals("key") 
                && !propLower.equals("value") && !propLower.equals("caretposition") 
                && !propLower.equals("currentcellrow")) {
                addPropertyToSapObject(id, propertyName, propertyValue);
                LOGGER.fine(String.format("Captured property: %s.%s = %s", id, propertyName, propertyValue));
            }
        }
    }
    
    protected void addSapObject(String id, String type, String transaction) {
        if (!sapObjects.containsKey(id)) {
            SapObject obj = new SapObject(id, type, transaction);
            // Text should only be set when explicitly captured from the script
            sapObjects.put(id, obj);
            LOGGER.fine(String.format("Added SAP object: id=%s, type=%s", id, type));
        }
    }
    
    protected void addPropertyToSapObject(String id, String propertyName, String propertyValue) {
        if (sapObjects.containsKey(id)) {
            SapObject obj = sapObjects.get(id);
            obj.setProperty(propertyName, propertyValue);
        } else {
            SapObject obj = new SapObject(id, determineObjectType(id), null);
            obj.setProperty(propertyName, propertyValue);
            sapObjects.put(id, obj);
        }
    }
    
    protected String determineObjectType(String sapId) {
        String lastSegment = sapId;
        int lastSlash = sapId.lastIndexOf('/');
        if (lastSlash >= 0) {
            lastSegment = sapId.substring(lastSlash + 1);
        }
        
        if (lastSegment.startsWith("txt") || lastSegment.startsWith("ctxt")) {
            return "TextField";
        }
        if (lastSegment.startsWith("pwd")) {
            return "PasswordField";
        }
        if (lastSegment.startsWith("btn")) {
            return "Button";
        }
        if (lastSegment.startsWith("chk")) {
            return "Checkbox";
        }
        if (lastSegment.startsWith("rad")) {
            return "RadioButton";
        }
        if (lastSegment.startsWith("cmb") || lastSegment.startsWith("cbo")) {
            return "ComboBox";
        }
        if (lastSegment.startsWith("tbl")) {
            return "Table";
        }
        if (lastSegment.startsWith("tab")) {
            return "Tab";
        }
        if (lastSegment.startsWith("wnd")) {
            return "Window";
        }
        if (lastSegment.startsWith("usr") || lastSegment.startsWith("sub")) {
            return "Container";
        }
        
        return "Element";
    }
    
    protected String extractTextFromId(String id) {
        String[] parts = id.split("/");
        if (parts.length > 0) {
            String lastPart = parts[parts.length - 1];
            return lastPart.replaceAll("^(txt|btn|cbo|chk|tbl|tab|ctxt|cmbBox|rad)", "");
        }
        return "";
    }
    
    public Map<String, SapObject> getSapObjects() {
        return sapObjects;
    }
    
    public List<SapAction> getSapActions() {
        return sapActions;
    }
    
    // -------- Inner Classes --------
    
    public static class SapObject {
        public String id;
        public String type;
        public String text;
        public String name;
        public String transaction;
        public Map<String, String> additionalProperties;

        public SapObject(String id, String type, String transaction) {
            this.id = id;
            this.type = type;
            this.transaction = transaction;
            this.text = "";
            this.name = "";
            this.additionalProperties = new LinkedHashMap<>();
        }
        
        public void setProperty(String propertyName, String propertyValue) {
            switch (propertyName.toLowerCase()) {
                case "text":
                    this.text = propertyValue;
                    break;
                case "name":
                    this.name = propertyValue;
                    break;
                default:
                    additionalProperties.put(propertyName, propertyValue);
                    break;
            }
        }
    }
    
    /**
     * Parse a SAP script file with statistics tracking.
     * Returns a SapParseResult with detailed metrics about the parse operation.
     */
    public SapParseResult parseWithStats(File file) throws IOException {
        long startTime = System.currentTimeMillis();
        parse(file);
        long parseTime = System.currentTimeMillis() - startTime;
        
        return new SapParseResult(
            sapObjects.size(),
            sapActions.size(),
            linesProcessed,
            linesParsed,
            parseTime,
            warnings,
            calculateActionTypeCounts()
        );
    }
    
    /**
     * Calculate count of each action type from parsed actions.
     */
    protected Map<String, Integer> calculateActionTypeCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (SapAction action : sapActions) {
            counts.merge(action.actionType, 1, Integer::sum);
        }
        return counts;
    }

    public static class SapAction {
        public String actionType;
        public String objectId;
        public String value;
        public int lineNumber;

        public SapAction(String actionType, String objectId, String value, int lineNumber) {
            this.actionType = actionType;
            this.objectId = objectId;
            this.value = value;
            this.lineNumber = lineNumber;
        }
    }
}

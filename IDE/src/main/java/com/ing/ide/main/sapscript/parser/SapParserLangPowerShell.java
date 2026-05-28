package com.ing.ide.main.sapscript.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SAP GUI Script parser for PowerShell (.ps1) files.
 * PowerShell SAP scripts from Script Tracker use helper functions from COM.ps1:
 * - Invoke-Method for method calls
 * - Set-Property for property assignments
 * - Get-Property for property reads
 * 
 * Language features:
 * - Comments: #
 * - Session variable prefix: $
 * - Case insensitive
 * - Helper functions: Invoke-Method, Set-Property, Get-Property
 * 
 * Example patterns:
 * $ID = Invoke-Method -object $session -methodName "findById" -methodParameter @("wnd[0]/usr/txtRSYST-BNAME")
 * Set-Property -object $ID -propertyName "text" -propertyValue @("TESTUSER")
 * Invoke-Method -object $ID -methodName "sendVKey" -methodParameter @(0)
 */
public class SapParserLangPowerShell extends SapLanguageParser {
    
    // Track variable assignments: $ID -> SAP object ID
    private Map<String, String> variableMap = new HashMap<>();
    private String currentTransaction = null;
    
    // Patterns for PowerShell helper functions
    private static final Pattern INVOKE_METHOD_PATTERN = Pattern.compile(
        "Invoke-Method\\s+-object\\s+(\\$\\w+)\\s+-methodName\\s+\"([^\"]+)\"(?:\\s+-methodParameter\\s+@\\(([^)]+)\\))?",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern SET_PROPERTY_PATTERN = Pattern.compile(
        "Set-Property\\s+-object\\s+(\\$\\w+)\\s+-propertyName\\s+\"([^\"]+)\"\\s+-propertyValue\\s+@\\(([^)]+)\\)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern VARIABLE_ASSIGNMENT_PATTERN = Pattern.compile(
        "(\\$\\w+)\\s*=\\s*Invoke-Method\\s+-object\\s+\\$\\w+\\s+-methodName\\s+\"findById\"\\s+-methodParameter\\s+@\\(\"([^\"]+)\"\\)",
        Pattern.CASE_INSENSITIVE
    );
    
    @Override
    public String getLanguageName() {
        return "PowerShell";
    }
    
    @Override
    public String[] getSupportedExtensions() {
        return new String[]{"ps1"};
    }
    
    @Override
    protected boolean isComment(String line) {
        return line.startsWith("#");
    }
    
    @Override
    protected String getSessionPrefix() {
        return "$"; // PowerShell uses $ prefix for variables (literal, will be quoted by Pattern.quote())
    }
    
    @Override
    public void parse(File file) throws IOException {
        LOGGER.info("Parsing SAP Script file with " + getLanguageName() + " parser: " + file.getAbsolutePath());
        
        variableMap.clear();
        currentTransaction = null;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNumber = 0;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String trimmedLine = line.trim();
                
                // Skip empty lines and comments
                if (trimmedLine.isEmpty() || isComment(trimmedLine)) {
                    continue;
                }
                
                // Extract transaction using base class method
                String transaction = extractTransaction(trimmedLine);
                if (transaction != null) {
                    currentTransaction = transaction;
                    LOGGER.fine("Found transaction: " + currentTransaction);
                    sapActions.add(new SapAction("Transaction", "SAP_SYSTEM", currentTransaction, lineNumber));
                    continue;
                }
                
                // Parse PowerShell-specific patterns
                parsePowerShellLine(trimmedLine, lineNumber);
            }
        }
        
        LOGGER.info(String.format("Parsed %d SAP objects and %d actions from %s script", 
            sapObjects.size(), sapActions.size(), getLanguageName()));
    }
    
    private void parsePowerShellLine(String line, int lineNumber) {
        // Check for variable assignment with findById
        Matcher varAssignMatcher = VARIABLE_ASSIGNMENT_PATTERN.matcher(line);
        if (varAssignMatcher.find()) {
            String varName = varAssignMatcher.group(1);
            String objectId = varAssignMatcher.group(2);
            variableMap.put(varName, objectId);
            
            // Extract transaction from object ID
            extractTransactionFromId(objectId);
            
            // Store object
            storeObject(objectId, lineNumber);
            return;
        }
        
        // Check for Invoke-Method calls
        Matcher invokeMethodMatcher = INVOKE_METHOD_PATTERN.matcher(line);
        if (invokeMethodMatcher.find()) {
            String objectVar = invokeMethodMatcher.group(1);
            String methodName = invokeMethodMatcher.group(2);
            String parameters = invokeMethodMatcher.group(3);
            
            String objectId = variableMap.get(objectVar);
            if (objectId != null) {
                parseInvokeMethod(objectId, methodName, parameters, lineNumber);
            }
            return;
        }
        
        // Check for Set-Property calls
        Matcher setPropMatcher = SET_PROPERTY_PATTERN.matcher(line);
        if (setPropMatcher.find()) {
            String objectVar = setPropMatcher.group(1);
            String propertyName = setPropMatcher.group(2);
            String propertyValue = setPropMatcher.group(3);
            
            String objectId = variableMap.get(objectVar);
            if (objectId != null) {
                parseSetProperty(objectId, propertyName, propertyValue, lineNumber);
            }
        }
    }
    
    private void parseInvokeMethod(String objectId, String methodName, String parameters, int lineNumber) {
        switch (methodName.toLowerCase()) {
            case "sendvkey":
                if (parameters != null && !parameters.isEmpty()) {
                    String key = parameters.trim();
                    addAction("SendVKey", objectId, key, lineNumber);
                }
                break;
            
            case "press":
                addAction("Click", objectId, "", lineNumber);
                break;
            
            case "setfocus":
                addAction("SetFocus", objectId, "", lineNumber);
                break;
            
            case "select":
                if (parameters != null && !parameters.isEmpty()) {
                    String value = extractQuotedValue(parameters);
                    addAction("Select", objectId, value, lineNumber);
                }
                break;
            
            case "doubleclick":
                addAction("DoubleClick", objectId, "", lineNumber);
                break;
            
            case "selectcontextmenuitem":
                if (parameters != null && !parameters.isEmpty()) {
                    String menuItem = extractQuotedValue(parameters);
                    addAction("SelectContextMenuItem", objectId, menuItem, lineNumber);
                }
                break;
            
            case "presscontextbutton":
                if (parameters != null && !parameters.isEmpty()) {
                    String value = extractQuotedValue(parameters);
                    addAction("PressContextButton", objectId, value, lineNumber);
                } else {
                    addAction("PressContextButton", objectId, "", lineNumber);
                }
                break;
            
            case "doubleclickcurrentcell":
                addAction("DoubleClickCurrentCell", objectId, "", lineNumber);
                break;
            
            case "clearselection":
                addAction("ClearSelection", objectId, "", lineNumber);
                break;
            
            case "resizeworkingpane":
                if (parameters != null && !parameters.isEmpty()) {
                    addAction("ResizeWorkingPane", objectId, parameters.trim(), lineNumber);
                } else {
                    addAction("ResizeWorkingPane", objectId, "", lineNumber);
                }
                break;
            
            case "expandnode":
                if (parameters != null && !parameters.isEmpty()) {
                    String value = extractQuotedValue(parameters);
                    addAction("ExpandNode", objectId, value, lineNumber);
                } else {
                    addAction("ExpandNode", objectId, "", lineNumber);
                }
                break;
            
            case "setcolumnwidth":
                if (parameters != null && !parameters.isEmpty()) {
                    addAction("SetColumnWidth", objectId, parameters.trim(), lineNumber);
                }
                break;
            
            case "setcurrentcell":
                if (parameters != null && !parameters.isEmpty()) {
                    addAction("SetCurrentCell", objectId, parameters.trim(), lineNumber);
                }
                break;
            
            default:
                // For unrecognized methods, still create object and action with methodName as actionType
                LOGGER.fine("Found unrecognized method call: " + methodName + " on " + objectId);
                String value = "";
                if (parameters != null && !parameters.isEmpty()) {
                    value = extractQuotedValue(parameters);
                    if (value.isEmpty()) {
                        value = parameters.trim();
                    }
                }
                // Store object and create action with the method name
                storeObject(objectId, lineNumber);
                addAction(methodName, objectId, value, lineNumber);
                break;
        }
    }
    
    private void parseSetProperty(String objectId, String propertyName, String propertyValue, int lineNumber) {
        String value = extractQuotedValue(propertyValue);
        
        switch (propertyName.toLowerCase()) {
            case "text":
                addAction("Set", objectId, value, lineNumber);
                
                // Also store the text property in the object
                SapObject obj = sapObjects.get(objectId);
                if (obj != null) {
                    obj.text = value;
                }
                break;
            
            case "selected":
                addAction("Check", objectId, value, lineNumber);
                break;
            
            case "key":
                addAction("DropdownKey", objectId, value, lineNumber);
                break;
            
            case "caretposition":
            case "cursorposition":
                addAction("CaretPosition", objectId, value, lineNumber);
                break;
            
            case "modified":
                addAction("Modified", objectId, value, lineNumber);
                break;
            
            case "selectedrows":
                value = extractQuotedValue(value);
                addAction("SelectedRows", objectId, value, lineNumber);
                break;
            
            case "currentcellrow":
                value = extractQuotedValue(value);
                addAction("CurrentCellRow", objectId, value, lineNumber);
                break;
            
            case "topnode":
                value = extractQuotedValue(value);
                addAction("TopNode", objectId, value, lineNumber);
                break;
            
            case "firstvisiblecolumn":
                value = extractQuotedValue(value);
                addAction("FirstVisibleColumn", objectId, value, lineNumber);
                break;
            
            default:
                // For unrecognized properties, still create object and action
                LOGGER.fine("Found unrecognized property assignment: " + propertyName + " = " + value + " on " + objectId);
                value = extractQuotedValue(value);
                if (value.isEmpty()) {
                    value = propertyValue.trim();
                }
                storeObject(objectId, lineNumber);
                addAction("SetProperty_" + propertyName, objectId, value, lineNumber);
                break;
        }
    }
    
    private String extractQuotedValue(String input) {
        if (input == null) {
            return "";
        }
        
        // Extract value from quotes: @("value") or @('value') or just "value"
        Pattern quotedPattern = Pattern.compile("\"([^\"]*)\"");
        Matcher matcher = quotedPattern.matcher(input);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // If no quotes, return trimmed value
        return input.trim().replaceAll("^@\\(|\\)$", "").trim();
    }
    
    private void storeObject(String objectId, int lineNumber) {
        if (!sapObjects.containsKey(objectId)) {
            String objectType = determineObjectType(objectId);
            SapObject obj = new SapObject(objectId, objectType, currentTransaction);
            sapObjects.put(objectId, obj);
            
            LOGGER.fine("Stored SAP object: " + objectId + " (type: " + objectType + ")");
        }
    }
    
    private void addAction(String actionType, String objectId, String value, int lineNumber) {
        SapAction action = new SapAction(actionType, objectId, value, lineNumber);
        sapActions.add(action);
        
        LOGGER.fine(String.format("Added action: %s on %s (line %d)", actionType, objectId, lineNumber));
    }
    
    private void extractTransactionFromId(String objectId) {
        Pattern transactionPattern = Pattern.compile("/n([A-Z0-9_/]+)");
        Matcher matcher = transactionPattern.matcher(objectId);
        if (matcher.find()) {
            currentTransaction = matcher.group(1);
            LOGGER.fine("Found transaction from object ID: " + currentTransaction);
        }
    }
}

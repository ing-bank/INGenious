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
 * SAP GUI Script parser for Java (.java, .jsh) files.
 * Java accesses SAP GUI Scripting COM API via JACOB library with ActiveXComponent.
 * 
 * Language features:
 * - Comments: // or block comments
 * - Uses JACOB: new ActiveXComponent(...), obj.invoke(), obj.setProperty()
 * - Variable tracking for obj references
 * 
 * Example patterns:
 * obj = new ActiveXComponent(session.invoke("findById", "wnd[0]").toDispatch());
 * obj.invoke("setFocus");
 * obj.setProperty("caretPosition", 2);
 */
public class SapParserLangJava extends SapLanguageParser {
    
    private boolean inBlockComment = false;
    private Map<String, String> variableMap = new HashMap<>();
    private Map<String, Map<Integer, String>> arrayMap = new HashMap<>();
    private String currentTransaction = null;
    
    private static final Pattern ACTIVEX_FINDBYID_PATTERN = Pattern.compile(
        "(\\w+)\\s*=\\s*new\\s+ActiveXComponent\\(\\w+\\.invoke\\(\\s*\"findById\"\\s*,\\s*\"([^\"]+)\"",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern INVOKE_METHOD_PATTERN = Pattern.compile(
        "(\\w+)\\.invoke\\(\\s*\"([^\"]+)\"(?:\\s*,\\s*(.+))?\\s*\\)(?:\\s*[);])*\\s*$",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern SET_PROPERTY_PATTERN = Pattern.compile(
        "(\\w+)\\.setProperty\\(\\s*\"([^\"]+)\"\\s*,\\s*(.+?)\\s*\\)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern VARIANT_ARRAY_INIT_PATTERN = Pattern.compile(
        "(\\w+)\\s*=\\s*new\\s+Variant\\[\\d+\\]\\s*;?",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern ARRAY_ASSIGNMENT_PATTERN = Pattern.compile(
        "(\\w+)\\[(\\d+)\\]\\s*=\\s*(.+?)\\s*;?\\s*$",
        Pattern.CASE_INSENSITIVE
    );
    
    @Override
    public String getLanguageName() {
        return "Java";
    }
    
    @Override
    public String[] getSupportedExtensions() {
        return new String[]{"java", "jsh"};
    }
    
    @Override
    protected boolean isComment(String line) {
        if (inBlockComment) {
            if (line.contains("*/")) {
                inBlockComment = false;
            }
            return true;
        }
        
        if (line.startsWith("/*")) {
            if (!line.contains("*/")) {
                inBlockComment = true;
            }
            return true;
        }
        
        return line.startsWith("//") || line.startsWith("*");
    }
    
    @Override
    protected String getSessionPrefix() {
        return "";
    }
    
    @Override
    public void parse(File file) throws IOException {
        LOGGER.info("Parsing SAP Script file with " + getLanguageName() + " parser: " + file.getAbsolutePath());
        
        variableMap.clear();
        arrayMap.clear();
        currentTransaction = null;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNumber = 0;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String trimmedLine = line.trim();
                
                if (trimmedLine.isEmpty() || isComment(trimmedLine)) {
                    continue;
                }
                
                String transaction = extractTransaction(trimmedLine);
                if (transaction != null) {
                    currentTransaction = transaction;
                    LOGGER.fine("Found transaction: " + currentTransaction);
                    sapActions.add(new SapAction("Transaction", "SAP_SYSTEM", currentTransaction, lineNumber));
                    continue;
                }
                
                parseJavaLine(trimmedLine, lineNumber);
            }
        }
        
        LOGGER.info(String.format("Parsed %d SAP objects and %d actions from %s script", 
            sapObjects.size(), sapActions.size(), getLanguageName()));
    }
    
    private void parseJavaLine(String line, int lineNumber) {
        // Check for Variant array initialization
        Matcher arrayInitMatcher = VARIANT_ARRAY_INIT_PATTERN.matcher(line);
        if (arrayInitMatcher.find()) {
            String arrayName = arrayInitMatcher.group(1);
            arrayMap.put(arrayName, new HashMap<>());
            return;
        }
        
        // Check for array element assignment
        Matcher arrayAssignMatcher = ARRAY_ASSIGNMENT_PATTERN.matcher(line);
        if (arrayAssignMatcher.find()) {
            String arrayName = arrayAssignMatcher.group(1);
            int index = Integer.parseInt(arrayAssignMatcher.group(2));
            String value = arrayAssignMatcher.group(3);
            
            if (arrayMap.containsKey(arrayName)) {
                String extractedValue = extractParameterValue(value);
                arrayMap.get(arrayName).put(index, extractedValue);
            }
            return;
        }
        
        Matcher activeXMatcher = ACTIVEX_FINDBYID_PATTERN.matcher(line);
        if (activeXMatcher.find()) {
            String varName = activeXMatcher.group(1);
            String objectId = activeXMatcher.group(2);
            variableMap.put(varName, objectId);
            storeObject(objectId, lineNumber);
            return;
        }
        
        Matcher invokeMatcher = INVOKE_METHOD_PATTERN.matcher(line);
        if (invokeMatcher.find()) {
            String varName = invokeMatcher.group(1);
            String methodName = invokeMatcher.group(2);
            String parameters = invokeMatcher.group(3);
            
            String objectId = variableMap.get(varName);
            if (objectId != null) {
                parseInvokeMethod(objectId, methodName, parameters, lineNumber);
            }
            return;
        }
        
        Matcher setPropMatcher = SET_PROPERTY_PATTERN.matcher(line);
        if (setPropMatcher.find()) {
            String varName = setPropMatcher.group(1);
            String propertyName = setPropMatcher.group(2);
            String propertyValue = setPropMatcher.group(3);
            
            String objectId = variableMap.get(varName);
            if (objectId != null) {
                parseSetProperty(objectId, propertyName, propertyValue, lineNumber);
            }
        }
    }
    
    private void parseInvokeMethod(String objectId, String methodName, String parameters, int lineNumber) {
        switch (methodName.toLowerCase()) {
            case "sendvkey":
                if (parameters != null && !parameters.isEmpty()) {
                    addAction("SendVKey", objectId, parameters.trim(), lineNumber);
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
                    String value = extractParameterValue(parameters);
                    addAction("Select", objectId, value, lineNumber);
                }
                break;
            
            case "doubleclick":
                addAction("DoubleClick", objectId, "", lineNumber);
                break;
            
            case "presscontextbutton":
                if (parameters != null && !parameters.isEmpty()) {
                    String value = extractQuotedValue(parameters);
                    addAction("PressContextButton", objectId, value, lineNumber);
                } else {
                    addAction("PressContextButton", objectId, "", lineNumber);
                }
                break;
            
            case "selectcontextmenuitem":
                if (parameters != null && !parameters.isEmpty()) {
                    String value = extractQuotedValue(parameters);
                    addAction("SelectContextMenuItem", objectId, value, lineNumber);
                } else {
                    addAction("SelectContextMenuItem", objectId, "", lineNumber);
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
                    String value = resolveArrayParameter(parameters.trim());
                    addAction("ResizeWorkingPane", objectId, value, lineNumber);
                } else {
                    addAction("ResizeWorkingPane", objectId, "", lineNumber);
                }
                break;
            
            // ========== NEW: WINDOW MANAGEMENT ==========
            case "maximize":
                addAction("Maximize", objectId, "", lineNumber);
                break;
            
            case "minimize":
                addAction("Minimize", objectId, "", lineNumber);
                break;
            
            case "restore":
                addAction("Restore", objectId, "", lineNumber);
                break;
            
            case "close":
                addAction("Close", objectId, "", lineNumber);
                break;
            
            case "iconify":
                addAction("Iconify", objectId, "", lineNumber);
                break;
            
            // ========== NEW: TEXT FIELD ACTIONS ==========
            case "selectall":
                addAction("SelectAll", objectId, "", lineNumber);
                break;
            
            // ========== NEW: COMBOBOX ACTIONS ==========
            case "open":
                addAction("OpenComboBox", objectId, "", lineNumber);
                break;
            
            // ========== NEW: TABLE ACTIONS ==========
            case "selectrow":
                if (parameters != null && !parameters.isEmpty()) {
                    addAction("SelectRow", objectId, parameters.trim(), lineNumber);
                }
                break;
            
            case "setcurrentcell":
                if (parameters != null && !parameters.isEmpty()) {
                    addAction("SetCurrentCell", objectId, parameters.trim(), lineNumber);
                }
                break;
            
            case "getcellvalue":
                if (parameters != null && !parameters.isEmpty()) {
                    addAction("GetCellValue", objectId, parameters.trim(), lineNumber);
                }
                break;
            
            case "clickcurrentcell":
                addAction("ClickCurrentCell", objectId, "", lineNumber);
                break;
            
            case "modifycell":
                if (parameters != null && !parameters.isEmpty()) {
                    addAction("ModifyCell", objectId, parameters.trim(), lineNumber);
                }
                break;
            
            // ========== NEW: GRID/ALV ACTIONS ==========
            case "selectcolumn":
                if (parameters != null && !parameters.isEmpty()) {
                    String value = extractQuotedValue(parameters);
                    addAction("GridSelectColumn", objectId, value, lineNumber);
                }
                break;
            
            case "presstoolbarbutton":
                if (parameters != null && !parameters.isEmpty()) {
                    String value = extractQuotedValue(parameters);
                    addAction("PressToolbarButton", objectId, value, lineNumber);
                }
                break;
            
            case "presstoolbarcontextbutton":
                if (parameters != null && !parameters.isEmpty()) {
                    String value = extractQuotedValue(parameters);
                    addAction("PressToolbarContextButton", objectId, value, lineNumber);
                }
                break;
            
            case "pressbutton":
                if (parameters != null && !parameters.isEmpty()) {
                    String value = extractQuotedValue(parameters);
                    addAction("PressButton", objectId, value, lineNumber);
                }
                break;
            
            case "gridselectall":
                addAction("GridSelectAll", objectId, "", lineNumber);
                break;
            
            case "deselectrow":
                if (parameters != null && !parameters.isEmpty()) {
                    addAction("DeselectRow", objectId, parameters.trim(), lineNumber);
                }
                break;
            
            case "setfilter":
                if (parameters != null && !parameters.isEmpty()) {
                    addAction("SetFilter", objectId, parameters.trim(), lineNumber);
                }
                break;
            
            case "clearfilter":
                addAction("ClearFilter", objectId, "", lineNumber);
                break;
            
            // ========== NEW: TREE ACTIONS ==========
            case "expandnode":
                if (parameters != null && !parameters.isEmpty()) {
                    String value = extractParameterValue(parameters);
                    addAction("ExpandNode", objectId, value, lineNumber);
                }
                break;
            
            case "collapsenode":
                if (parameters != null && !parameters.isEmpty()) {
                    String value = extractParameterValue(parameters);
                    addAction("CollapseNode", objectId, value, lineNumber);
                }
                break;
            
            case "selectnode":
                if (parameters != null && !parameters.isEmpty()) {
                    String value = extractParameterValue(parameters);
                    addAction("SelectNode", objectId, value, lineNumber);
                }
                break;
            
            case "doubleclicknode":
                if (parameters != null && !parameters.isEmpty()) {
                    String value = extractParameterValue(parameters);
                    addAction("DoubleClickNode", objectId, value, lineNumber);
                }
                break;
            
            // ========== NEW: SESSION MANAGEMENT ==========
            case "endtransaction":
                addAction("EndTransaction", objectId, "", lineNumber);
                break;
            
            case "refresh":
                addAction("Refresh", objectId, "", lineNumber);
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
        String value = propertyValue.trim();
        
        switch (propertyName.toLowerCase()) {
            case "text":
                value = extractQuotedValue(value);
                addAction("Set", objectId, value, lineNumber);
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
            
            // ========== NEW: ADDITIONAL GRID/TABLE PROPERTIES ==========
            case "firstvisiblerow":
                value = extractQuotedValue(value);
                addAction("FirstVisibleRow", objectId, value, lineNumber);
                break;
            
            case "currentcellcolumn":
                value = extractQuotedValue(value);
                addAction("CurrentCellColumn", objectId, value, lineNumber);
                break;
            
            case "verticalscrollbar.position":
            case "verticalscrollbarposition":
                value = extractQuotedValue(value);
                addAction("VerticalScrollbarPosition", objectId, value, lineNumber);
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
        
        Pattern quotedPattern = Pattern.compile("\"([^\"]*)\"");
        Matcher matcher = quotedPattern.matcher(input);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return input.trim();
    }
    
    /**
     * Extract actual value from parameter, handling Variant wrappers and other Java constructs.
     * Examples:
     * - "new Variant(2)" -> "2"
     * - "new Variant(Integer.parseInt(Data))" -> "Integer.parseInt(Data)"
     * - "true" -> "true"
     * - "123" -> "123"
     */
    private String extractParameterValue(String parameter) {
        if (parameter == null || parameter.isEmpty()) {
            return "";
        }
        
        String trimmed = parameter.trim();
        
        // Remove trailing semicolon if present
        if (trimmed.endsWith(";")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
        }
        
        // Handle new Variant(...) wrapper
        Pattern variantPattern = Pattern.compile("new\\s+Variant\\((.+)\\)$", Pattern.CASE_INSENSITIVE);
        Matcher variantMatcher = variantPattern.matcher(trimmed);
        if (variantMatcher.find()) {
            String innerValue = variantMatcher.group(1).trim();
            // Handle Integer.parseInt(...)
            Pattern parseIntPattern = Pattern.compile("Integer\\.parseInt\\((.+)\\)$", Pattern.CASE_INSENSITIVE);
            Matcher parseIntMatcher = parseIntPattern.matcher(innerValue);
            if (parseIntMatcher.find()) {
                return parseIntMatcher.group(1).trim();
            }
            return innerValue;
        }
        
        // Try to extract quoted string
        String quoted = extractQuotedValue(trimmed);
        if (!quoted.equals(trimmed)) {
            return quoted.trim();
        }
        
        return trimmed;
    }
    
    /**
     * Resolve parameter that might be an array variable reference.
     * If it's an array variable, concatenate all array values with commas.
     * Otherwise, return the parameter as-is.
     * 
     * Examples:
     * - "arg" with array values [0=87, 1=20, 2=false] -> "87,20,false"
     * - "someValue" -> "someValue"
     */
    private String resolveArrayParameter(String parameter) {
        if (parameter == null || parameter.isEmpty()) {
            return "";
        }
        
        String trimmed = parameter.trim();
        
        // Check if the parameter is a known array variable
        if (arrayMap.containsKey(trimmed)) {
            Map<Integer, String> arrayValues = arrayMap.get(trimmed);
            if (arrayValues != null && !arrayValues.isEmpty()) {
                // Sort by index and concatenate values
                StringBuilder result = new StringBuilder();
                int maxIndex = arrayValues.keySet().stream().max(Integer::compareTo).orElse(-1);
                
                for (int i = 0; i <= maxIndex; i++) {
                    if (i > 0) {
                        result.append(",");
                    }
                    String value = arrayValues.getOrDefault(i, "");
                    result.append(value);
                }
                
                return result.toString();
            }
        }
        
        // Not an array variable, return as-is
        return extractParameterValue(trimmed);
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
}

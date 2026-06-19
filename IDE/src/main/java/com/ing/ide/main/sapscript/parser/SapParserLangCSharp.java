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
 * SAP GUI Script parser for C# (.cs) files.
 * C# accesses SAP GUI Scripting COM API via Reflection with InvokeMember.
 *
 * Language features:
 * - Comments: // or block comments
 * - Uses reflection helper functions: InvokeMethod, GetProperty, SetProperty
 * - Variable tracking for dynamic object references
 *
 * Example patterns:
 * ID = InvokeMethod(session, "findById", new object[]{});
 * InvokeMethod(ID, "setFocus", new object[]{});
 * SetProperty(ID, "caretPosition", new object[]{2});
 */
public class SapParserLangCSharp extends SapLanguageParser {
    private boolean inBlockComment = false;
    private Map<String, String> variableMap = new HashMap<>();
    private String currentTransaction = null;

    private static final Pattern INVOKEMETHOD_FINDBYID_PATTERN = Pattern.compile(
        "(\\w+)\\s*=\\s*InvokeMethod\\(\\w+,\\s*\"findById\"\\s*,\\s*new\\s+object\\[\\d+\\]\\s*\\{\\s*\"([^\"]+)\"",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern INVOKEMETHOD_PATTERN = Pattern.compile(
        "InvokeMethod\\((\\w+),\\s*\"([^\"]+)\"(?:,\\s*new\\s+object\\[\\d+\\]\\s*\\{([^}]*)\\})?",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern SETPROPERTY_PATTERN = Pattern.compile(
        "SetProperty\\((\\w+),\\s*\"([^\"]+)\"\\s*,\\s*new\\s+object\\[\\d+\\]\\s*\\{(.+?)\\}",
        Pattern.CASE_INSENSITIVE
    );

    @Override
    public String getLanguageName() {
        return "C#";
    }

    @Override
    public String[] getSupportedExtensions() {
        return new String[] { "cs" };
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
        LOGGER.info(
            "Parsing SAP Script file with " +
            getLanguageName() +
            " parser: " +
            file.getAbsolutePath()
        );

        variableMap.clear();
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
                    sapActions.add(
                        new SapAction("Transaction", "SAP_SYSTEM", currentTransaction, lineNumber)
                    );
                    continue;
                }

                parseCSharpLine(trimmedLine, lineNumber);
            }
        }

        LOGGER.info(
            String.format(
                "Parsed %d SAP objects and %d actions from %s script",
                sapObjects.size(),
                sapActions.size(),
                getLanguageName()
            )
        );
    }

    private void parseCSharpLine(String line, int lineNumber) {
        Matcher invokeMethodFindByIdMatcher = INVOKEMETHOD_FINDBYID_PATTERN.matcher(line);
        if (invokeMethodFindByIdMatcher.find()) {
            String varName = invokeMethodFindByIdMatcher.group(1);
            String objectId = invokeMethodFindByIdMatcher.group(2);
            variableMap.put(varName, objectId);
            storeObject(objectId, lineNumber);
            return;
        }

        Matcher invokeMethodMatcher = INVOKEMETHOD_PATTERN.matcher(line);
        if (invokeMethodMatcher.find()) {
            String varName = invokeMethodMatcher.group(1);
            String methodName = invokeMethodMatcher.group(2);
            String parameters = invokeMethodMatcher.group(3);

            String objectId = variableMap.get(varName);
            if (objectId != null) {
                parseInvokeMethod(objectId, methodName, parameters, lineNumber);
            }
            return;
        }

        Matcher setPropMatcher = SETPROPERTY_PATTERN.matcher(line);
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

    private void parseInvokeMethod(
        String objectId,
        String methodName,
        String parameters,
        int lineNumber
    ) {
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
            default:
                LOGGER.fine("Found method call: " + methodName + " on " + objectId);
                break;
        }
    }

    private void parseSetProperty(
        String objectId,
        String propertyName,
        String propertyValue,
        int lineNumber
    ) {
        String value = extractQuotedValue(propertyValue);

        switch (propertyName.toLowerCase()) {
            case "text":
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
            default:
                LOGGER.fine(
                    "Found property assignment: " + propertyName + " = " + value + " on " + objectId
                );
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
        LOGGER.fine(
            String.format("Added action: %s on %s (line %d)", actionType, objectId, lineNumber)
        );
    }
}

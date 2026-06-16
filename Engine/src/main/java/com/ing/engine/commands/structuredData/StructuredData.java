package com.ing.engine.commands.structuredData;

import com.ing.engine.commands.browser.General;
import com.ing.engine.core.CommandControl;
import com.ing.ingenious.api.annotation.Action;
import com.ing.ingenious.api.status.Status;
import com.ing.ingenious.api.types.InputType;
import com.ing.ingenious.api.types.ObjectType;
import com.jayway.jsonpath.*;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Provides comprehensive webservice testing actions for REST and SOAP API automation.
 * <p>
 * This class extends {@link General} and offers a complete suite of HTTP/HTTPS operations
 * for testing web services, including request execution, response validation, data extraction,
 * and assertion capabilities for both JSON and XML responses.
 * </p>
 *
 * <h2>Configuration</h2>
 * <p>
 * API-specific configurations can be loaded using aliases in the Condition field of {@link #setEndPoint()}.
 * Settings include SSL verification, proxy configuration, redirect policies, and custom HTTP agents.
 * </p>
 */
public class StructuredData extends General {

    public StructuredData(CommandControl cc) {
        super(cc);
    }

    public enum PathType {
        DEFAULT,
        JSONPATH,
        XMLPATH
    }

    /****** JsonPath Actions ******/

    /**
     * Asserts that a JsonPath query result contains the expected substring.
     * <p>
     * Uses JsonPath to extract a value from the last Webservice JSON response and verifies it contains
     * the specified substring.
     * <ul>
     *   <li>Input: Expected substring</li>
     * </ul>
     */
    @Action(
        object = ObjectType.STRUCTUREDDATA,
        desc = "Assert JsonPath Result Contains ",
        input = InputType.YES
    )
    public void assertJsonPathResultContains() {
        try {
            String response = responsebodies.get(key);
            String jsonpath = resolveStructuredDataPath();
            logJsonPathContext("assertJsonPathResultContains", response, jsonpath);
            String value = JsonPath.read(response, jsonpath).toString();
            String strObj = getInputValue(Input);
            if (value.contains(strObj)) {
                Report.updateTestLog(
                    Action,
                    "Element text contains [" + strObj + "] is as expected",
                    Status.PASSNS
                );
            } else {
                Report.updateTestLog(
                    Action,
                    "Element text [" + value + "] does not contain [" + strObj + "]",
                    Status.FAILNS
                );
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(
                Action,
                "Error in validating JSON element :" + "\n" + ex.getMessage(),
                Status.DEBUG
            );
        }
    }

    /**
     * Asserts that a JsonPath query result does NOT contain the specified text.
     * <p>
     * Uses JsonPath to extract a value from the last Webservice JSON response and verifies it does NOT
     * contain the specified substring. This is the negative assertion counterpart to
     * {@link #assertJsonPathResultContains()}.
     * <ul>
     *   <li>Input: Substring that should NOT be present</li>
     * </ul>
     */
    @Action(
        object = ObjectType.STRUCTUREDDATA,
        desc = "Assert JsonPath Result Not Contains ",
        input = InputType.YES
    )
    public void assertJsonPathResultNotContains() {
        try {
            String response = responsebodies.get(key);
            String jsonpath = resolveStructuredDataPath();
            logJsonPathContext("assertJsonPathResultNotContains", response, jsonpath);
            String value = JsonPath.read(response, jsonpath).toString();
            String strObj = getInputValue(Input);
            if (!value.contains(strObj)) {
                Report.updateTestLog(
                    Action,
                    "Element text [" + value + "] does not contain [" + strObj + "] as expected",
                    Status.PASSNS
                );
            } else {
                Report.updateTestLog(
                    Action,
                    "Element text [" + value + "] contains [" + strObj + "] but should not",
                    Status.FAILNS
                );
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(
                Action,
                "Error in validating JSON element :" + "\n" + ex.getMessage(),
                Status.DEBUG
            );
        }
    }

    /**
     * Asserts that a JsonPath query result contains the expected substring.
     * <p>
     * Uses JsonPath to extract a value from the last Webservice JSON response and verifies it matches
     * the expected value exactly.
     * <ul>
     *   <li>Input: Expected value</li>
     * </ul>
     */
    @Action(
        object = ObjectType.STRUCTUREDDATA,
        desc = "Assert JsonPath Result Equals ",
        input = InputType.YES
    )
    public void assertJsonPathResultEquals() {
        try {
            String response = responsebodies.get(key);
            String jsonpath = resolveStructuredDataPath();
            logJsonPathContext("assertJsonPathResultEquals", response, jsonpath);
            String value = JsonPath.read(response, jsonpath).toString();
            String strObj = getInputValue(Input);
            if (value.equals(strObj)) {
                Report.updateTestLog(
                    Action,
                    "Element text [" + value + "] is as expected",
                    Status.PASSNS
                );
            } else {
                Report.updateTestLog(
                    Action,
                    "Element text is [" + value + "] but is expected to be [" + strObj + "]",
                    Status.FAILNS
                );
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(
                Action,
                "Error in validating JSON element :" + "\n" + ex.getMessage(),
                Status.DEBUG
            );
        }
    }

    /**
     * Asserts that a JsonPath query result does NOT equal the specified value.
     * <p>
     * Uses JsonPath to extract a value from the last Webservice JSON response and verifies it does NOT
     * match the specified value exactly. This is the negative assertion counterpart to
     * {@link #assertJsonPathResultEquals()}.
     * <ul>
     *   <li>Input: Value that should NOT match</li>
     * </ul>
     */
    @Action(
        object = ObjectType.STRUCTUREDDATA,
        desc = "Assert JsonPath Result Not Equals ",
        input = InputType.YES
    )
    public void assertJsonPathResultNotEquals() {
        try {
            String response = responsebodies.get(key);
            String jsonpath = resolveStructuredDataPath();
            logJsonPathContext("assertJsonPathResultNotEquals", response, jsonpath);
            String value = JsonPath.read(response, jsonpath).toString();
            String strObj = getInputValue(Input);
            if (!value.equals(strObj)) {
                Report.updateTestLog(
                    Action,
                    "Element text [" + value + "] is not equal to [" + strObj + "] as expected",
                    Status.PASSNS
                );
            } else {
                Report.updateTestLog(
                    Action,
                    "Element text is [" + value + "] but should not be equal to [" + strObj + "]",
                    Status.FAILNS
                );
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(
                Action,
                "Error in validating JSON element :" + "\n" + ex.getMessage(),
                Status.DEBUG
            );
        }
    }

    /**
     * Asserts that the count of JSON elements from a JsonPath query result matches the expected number.
     * <p>
     * Uses JsonPath to select elements and counts them, then verifies the count
     * matches the expected value. Works with arrays and objects.
     * <ul>
     *   <li>Input: Expected count (integer)</li>
     * </ul>
     */
    @Action(
        object = ObjectType.STRUCTUREDDATA,
        desc = "Assert JsonPath Result Count ",
        input = InputType.YES
    )
    public void assertJsonPathResultCount() {
        try {
            String response = responsebodies.get(key);
            int actualObjectCount = 0;
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(response);
            String strObj = getInputValue(Input);
            String jsonpath = resolveStructuredDataPath();
            logJsonPathContext("assertJsonPathResultCount", response, jsonpath);
            try {
                Map<String, String> objectMap = JsonPath.read(json, jsonpath);
                actualObjectCount = objectMap.keySet().size();
            } catch (Exception ex) {
                try {
                    JSONArray objectMap = JsonPath.read(json, jsonpath);
                    actualObjectCount = objectMap.size();
                } catch (Exception ex1) {
                    try {
                        net.minidev.json.JSONArray objectMap = JsonPath.read(json, jsonpath);
                        actualObjectCount = objectMap.size();
                    } catch (Exception ex2) {
                        String objectMap = JsonPath.read(json, jsonpath);
                        actualObjectCount = 1;
                    }
                }
            }

            int expectedObjectCount = Integer.parseInt(strObj);
            if (actualObjectCount == expectedObjectCount) {
                Report.updateTestLog(
                    Action,
                    "Element count [" + expectedObjectCount + "] is as expected",
                    Status.PASSNS
                );
            } else {
                Report.updateTestLog(
                    Action,
                    "Element count is [" +
                    actualObjectCount +
                    "] but is expected to be [" +
                    expectedObjectCount +
                    "]",
                    Status.FAILNS
                );
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(
                Action,
                "Error in validating JSON element :" + "\n" + ex.getMessage(),
                Status.DEBUG
            );
        }
    }

    /**
     * Asserts that a JsonPath query result starts with the expected prefix.
     * <p>
     * Uses JsonPath to extract a value from the last Webservice JSON response and verifies it
     * starts with the specified prefix.
     * <ul>
     *   <li>Input: Expected prefix</li>
     * </ul>
     */
    @Action(
        object = ObjectType.STRUCTUREDDATA,
        desc = "Assert JsonPath Result Starts With ",
        input = InputType.YES
    )
    public void assertJsonPathResultStartsWith() {
        try {
            String response = responsebodies.get(key);
            String jsonpath = resolveStructuredDataPath();
            logJsonPathContext("assertJsonPathResultStartsWith", response, jsonpath);
            String value = String.valueOf(JsonPath.read(response, jsonpath));
            String strObj = getInputValue(Input);
            if (value.startsWith(strObj)) {
                Report.updateTestLog(
                    Action,
                    "Element text [" + value + "] starts with [" + strObj + "] as expected",
                    Status.PASSNS
                );
            } else {
                Report.updateTestLog(
                    Action,
                    "Element text [" + value + "] does not start with [" + strObj + "]",
                    Status.FAILNS
                );
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(
                Action,
                "Error in validating JSON element :" + "\n" + ex.getMessage(),
                Status.DEBUG
            );
        }
    }

    /**
     * Asserts that a JsonPath query result ends with the expected suffix.
     * <p>
     * Uses JsonPath to extract a value from the last Webservice JSON response and verifies it
     * ends with the specified suffix.
     * <ul>
     *   <li>Input: Expected suffix</li>
     * </ul>
     */
    @Action(
        object = ObjectType.STRUCTUREDDATA,
        desc = "Assert JsonPath Result Ends With ",
        input = InputType.YES
    )
    public void assertJsonPathResultEndsWith() {
        try {
            String response = responsebodies.get(key);
            String jsonpath = resolveStructuredDataPath();
            logJsonPathContext("assertJsonPathResultEndsWith", response, jsonpath);
            String value = String.valueOf(JsonPath.read(response, jsonpath));
            String strObj = getInputValue(Input);
            if (value.endsWith(strObj)) {
                Report.updateTestLog(
                    Action,
                    "Element text [" + value + "] ends with [" + strObj + "] as expected",
                    Status.PASSNS
                );
            } else {
                Report.updateTestLog(
                    Action,
                    "Element text [" + value + "] does not end with [" + strObj + "]",
                    Status.FAILNS
                );
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(
                Action,
                "Error in validating JSON element :" + "\n" + ex.getMessage(),
                Status.DEBUG
            );
        }
    }

    /**
     * Asserts that a JsonPath query result matches the supplied regular expression.
     * <p>
     * Uses JsonPath to extract a value from the last Webservice JSON response and verifies it
     * matches (fully) the given Java regular expression.
     * <ul>
     *   <li>Input: Java regular expression</li>
     * </ul>
     */
    @Action(
        object = ObjectType.STRUCTUREDDATA,
        desc = "Assert JsonPath Result Matches Regex ",
        input = InputType.YES
    )
    public void assertJsonPathResultMatchesRegex() {
        try {
            String response = responsebodies.get(key);
            String jsonpath = resolveStructuredDataPath();
            logJsonPathContext("assertJsonPathResultMatchesRegex", response, jsonpath);
            String value = String.valueOf(JsonPath.read(response, jsonpath));
            String regex = getInputValue(Input);
            if (value.matches(regex)) {
                Report.updateTestLog(
                    Action,
                    "Element text [" + value + "] matches regex [" + regex + "] as expected",
                    Status.PASSNS
                );
            } else {
                Report.updateTestLog(
                    Action,
                    "Element text [" + value + "] does not match regex [" + regex + "]",
                    Status.FAILNS
                );
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(
                Action,
                "Error in validating JSON element :" + "\n" + ex.getMessage(),
                Status.DEBUG
            );
        }
    }

    /**
     * Asserts that a JsonPath query result is numerically greater than the expected value.
     * <p>
     * Uses JsonPath to extract a value from the last Webservice JSON response, parses it as a
     * number and verifies it is strictly greater than the expected number.
     * <ul>
     *   <li>Input: Expected numeric threshold</li>
     * </ul>
     */
    @Action(
        object = ObjectType.STRUCTUREDDATA,
        desc = "Assert JsonPath Result Greater Than ",
        input = InputType.YES
    )
    public void assertJsonPathResultGreaterThan() {
        try {
            String response = responsebodies.get(key);
            String jsonpath = resolveStructuredDataPath();
            logJsonPathContext("assertJsonPathResultGreaterThan", response, jsonpath);
            String value = String.valueOf(JsonPath.read(response, jsonpath));
            String strObj = getInputValue(Input);
            try {
                double actual = Double.parseDouble(value.trim());
                double expected = Double.parseDouble(strObj.trim());
                if (actual > expected) {
                    Report.updateTestLog(
                        Action,
                        "Element value [" +
                        actual +
                        "] is greater than [" +
                        expected +
                        "] as expected",
                        Status.PASSNS
                    );
                } else {
                    Report.updateTestLog(
                        Action,
                        "Element value [" + actual + "] is not greater than [" + expected + "]",
                        Status.FAILNS
                    );
                }
            } catch (NumberFormatException nfe) {
                Report.updateTestLog(
                    Action,
                    "Cannot compare non-numeric values: actual=[" +
                    value +
                    "], expected=[" +
                    strObj +
                    "]",
                    Status.FAILNS
                );
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(
                Action,
                "Error in validating JSON element :" + "\n" + ex.getMessage(),
                Status.DEBUG
            );
        }
    }

    /**
     * Asserts that a JsonPath query result is numerically less than the expected value.
     * <p>
     * Uses JsonPath to extract a value from the last Webservice JSON response, parses it as a
     * number and verifies it is strictly less than the expected number.
     * <ul>
     *   <li>Input: Expected numeric threshold</li>
     * </ul>
     */
    @Action(
        object = ObjectType.STRUCTUREDDATA,
        desc = "Assert JsonPath Result Less Than ",
        input = InputType.YES
    )
    public void assertJsonPathResultLessThan() {
        try {
            String response = responsebodies.get(key);
            String jsonpath = resolveStructuredDataPath();
            logJsonPathContext("assertJsonPathResultLessThan", response, jsonpath);
            String value = String.valueOf(JsonPath.read(response, jsonpath));
            String strObj = getInputValue(Input);
            try {
                double actual = Double.parseDouble(value.trim());
                double expected = Double.parseDouble(strObj.trim());
                if (actual < expected) {
                    Report.updateTestLog(
                        Action,
                        "Element value [" +
                        actual +
                        "] is less than [" +
                        expected +
                        "] as expected",
                        Status.PASSNS
                    );
                } else {
                    Report.updateTestLog(
                        Action,
                        "Element value [" + actual + "] is not less than [" + expected + "]",
                        Status.FAILNS
                    );
                }
            } catch (NumberFormatException nfe) {
                Report.updateTestLog(
                    Action,
                    "Cannot compare non-numeric values: actual=[" +
                    value +
                    "], expected=[" +
                    strObj +
                    "]",
                    Status.FAILNS
                );
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(
                Action,
                "Error in validating JSON element :" + "\n" + ex.getMessage(),
                Status.DEBUG
            );
        }
    }

    /**
     * Asserts that a JsonPath expression exists (resolves to a value) in the JSON response.
     * <p>
     * Uses JsonPath to evaluate the expression against the last Webservice JSON response. Passes
     * when the path resolves to any value (including {@code null}); fails when the path cannot
     * be resolved. The Input column is ignored for this assertion.
     */
    @Action(
        object = ObjectType.STRUCTUREDDATA,
        desc = "Assert JsonPath Exists ",
        input = InputType.NO
    )
    public void assertJsonPathExists() {
        try {
            String response = responsebodies.get(key);
            String jsonpath = resolveStructuredDataPath();
            logJsonPathContext("assertJsonPathExists", response, jsonpath);
            Object result;
            try {
                result = JsonPath.read(response, jsonpath);
            } catch (PathNotFoundException pnf) {
                Report.updateTestLog(
                    Action,
                    "JSON path [" + jsonpath + "] does not exist",
                    Status.FAILNS
                );
                return;
            }
            if (result == null) {
                Report.updateTestLog(
                    Action,
                    "JSON path [" + jsonpath + "] exists (value is null)",
                    Status.PASSNS
                );
            } else if (
                result instanceof java.util.Collection &&
                ((java.util.Collection<?>) result).isEmpty()
            ) {
                Report.updateTestLog(
                    Action,
                    "JSON path [" + jsonpath + "] resolved to an empty result",
                    Status.FAILNS
                );
            } else {
                Report.updateTestLog(
                    Action,
                    "JSON path [" + jsonpath + "] exists as expected",
                    Status.PASSNS
                );
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(
                Action,
                "Error in validating JSON path existence :" + "\n" + ex.getMessage(),
                Status.DEBUG
            );
        }
    }

    /**
     * Asserts that a JsonPath expression does NOT exist in the JSON response.
     * <p>
     * Uses JsonPath to evaluate the expression against the last Webservice JSON response. Passes
     * when the path cannot be resolved (or resolves to an empty result); fails otherwise. The
     * Input column is ignored for this assertion.
     */
    @Action(
        object = ObjectType.STRUCTUREDDATA,
        desc = "Assert JsonPath Not Exists ",
        input = InputType.NO
    )
    public void assertJsonPathNotExists() {
        try {
            String response = responsebodies.get(key);
            String jsonpath = resolveStructuredDataPath();
            logJsonPathContext("assertJsonPathNotExists", response, jsonpath);
            Object result;
            try {
                result = JsonPath.read(response, jsonpath);
            } catch (PathNotFoundException pnf) {
                Report.updateTestLog(
                    Action,
                    "JSON path [" + jsonpath + "] does not exist as expected",
                    Status.PASSNS
                );
                return;
            }
            if (
                result instanceof java.util.Collection &&
                ((java.util.Collection<?>) result).isEmpty()
            ) {
                Report.updateTestLog(
                    Action,
                    "JSON path [" +
                    jsonpath +
                    "] resolves to an empty result (treated as not present)",
                    Status.PASSNS
                );
            } else {
                Report.updateTestLog(
                    Action,
                    "JSON path [" + jsonpath + "] exists but should not (value=[" + result + "])",
                    Status.FAILNS
                );
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(
                Action,
                "Error in validating JSON path absence :" + "\n" + ex.getMessage(),
                Status.DEBUG
            );
        }
    }

    /**
     * Stores the count of JSON elements from a JsonPath query result in a datasheet column.
     * <p>
     * Uses JsonPath to select elements, counts them, and stores the count in
     * the specified datasheet column.
     * <ul>
     *   <li>Input: sheetName:ColumnName</li>
     * </ul>
     */
    @Action(
        object = ObjectType.STRUCTUREDDATA,
        desc = "Store JsonPath Result count in Datasheet ",
        input = InputType.YES
    )
    public void storeJsonPathResultCountInDataSheet() {
        try {
            String dataSheetReference = Input;
            if (dataSheetReference.matches(".*:.*")) {
                try {
                    System.out.println(
                        "Updating value in SubIteration " + userData.getSubIteration()
                    );
                    String sheetName = dataSheetReference.split(":", 2)[0];
                    String columnName = dataSheetReference.split(":", 2)[1];
                    String actualObjectCount = Integer.toString(getJsonElementCount());
                    userData.putData(sheetName, columnName, actualObjectCount);
                    Report.updateTestLog(
                        Action,
                        "Element count [" +
                        actualObjectCount +
                        "] is stored in " +
                        dataSheetReference,
                        Status.DONE
                    );
                } catch (Exception ex) {
                    Logger.getLogger(this.getClass().getName()).log(Level.OFF, ex.getMessage(), ex);
                    Report.updateTestLog(
                        Action,
                        "Error Storing JSON element in datasheet :" + "\n" + ex.getMessage(),
                        Status.DEBUG
                    );
                }
            } else {
                Report.updateTestLog(
                    Action,
                    "Given input [" +
                    Input +
                    "] format is invalid. It should be [sheetName:ColumnName]",
                    Status.DEBUG
                );
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(
                Action,
                "Error Storing JSON element in datasheet :" + "\n" + ex.getMessage(),
                Status.DEBUG
            );
        }
    }

    /**
     * Stores the count of JSON elements from a JsonPath query result in a variable.
     * <p>
     * Uses JsonPath to select elements, counts them, and stores the count in a variable.
     * <ul>
     *   <li>Input: JsonPath expression (e.g., $.items[*])</li>
     * </ul>
     */
    @Action(
        object = ObjectType.STRUCTUREDDATA,
        desc = "Store JsonPath Result count in variable ",
        input = InputType.YES
    )
    public void storeJsonPathResultCountInVariable() {
        try {
            String varName = Input;
            if (varName.matches("%.*%")) {
                try {
                    System.out.println(
                        "Updating value in SubIteration " + userData.getSubIteration()
                    );
                    String actualObjectCount = Integer.toString(getJsonElementCount());
                    addVar(varName, actualObjectCount);
                    Report.updateTestLog(
                        Action,
                        "Element count [" + actualObjectCount + "] is stored in " + varName,
                        Status.DONE
                    );
                } catch (Exception ex) {
                    Logger.getLogger(this.getClass().getName()).log(Level.OFF, ex.getMessage(), ex);
                    Report.updateTestLog(
                        Action,
                        "Error Storing JSON element in Variable :" + "\n" + ex.getMessage(),
                        Status.DEBUG
                    );
                }
            } else {
                Report.updateTestLog(
                    Action,
                    "Given condition [" + Condition + "] format is invalid. It should be [%Var%]",
                    Status.DEBUG
                );
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(
                Action,
                "Error Storing JSON element in Variable :" + "\n" + ex.getMessage(),
                Status.DEBUG
            );
        }
    }

    /**
     * Stores a JsonPath query result value in a datasheet column.
     * <p>
     * Extracts a value from the JSON response using JsonPath and stores it in the
     * specified datasheet column.
     * <ul>
     *   <li>Input: sheetName:ColumnName</li>
     * </ul>
     */
    @Action(
        object = ObjectType.STRUCTUREDDATA,
        desc = "Store JsonPath Result In DataSheet ",
        input = InputType.YES
    )
    public void storeJsonPathResultInDataSheet() {
        try {
            String dataSheetReference = Input;
            if (dataSheetReference.matches(".*:.*")) {
                try {
                    System.out.println(
                        "Updating value in SubIteration " + userData.getSubIteration()
                    );
                    String sheetName = dataSheetReference.split(":", 2)[0];
                    String columnName = dataSheetReference.split(":", 2)[1];
                    String response = responsebodies.get(key);
                    String jsonpath = resolveStructuredDataPath();
                    String value = JsonPath.read(response, jsonpath).toString();
                    userData.putData(sheetName, columnName, value);
                    Report.updateTestLog(
                        Action,
                        "Element text [" + value + "] is stored in " + dataSheetReference,
                        Status.DONE
                    );
                } catch (Exception ex) {
                    Logger.getLogger(this.getClass().getName()).log(Level.OFF, ex.getMessage(), ex);
                    Report.updateTestLog(
                        Action,
                        "Error Storing JSON element in datasheet :" + "\n" + ex.getMessage(),
                        Status.DEBUG
                    );
                }
            } else {
                Report.updateTestLog(
                    Action,
                    "Given input [" +
                    Input +
                    "] format is invalid. It should be [sheetName:ColumnName]",
                    Status.DEBUG
                );
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(
                Action,
                "Error Storing JSON element in datasheet :" + "\n" + ex.getMessage(),
                Status.DEBUG
            );
        }
    }

    /**
     * Stores a JsonPath query result value in a variable.
     * <p>
     * Extracts a value from the JSON response using JsonPath and stores it in a variable.
     * <ul>
     *   <li>Input: JsonPath expression (e.g., $.data.token)</li>
     * </ul>
     */
    @Action(
        object = ObjectType.STRUCTUREDDATA,
        desc = "Store JsonPath Result",
        input = InputType.YES
    )
    public void storeJsonPathResultInVariable() {
        try {
            String variableName = Input;
            String jsonpath = resolveStructuredDataPath();
            if (variableName.matches("%.*%")) {
                addVar(variableName, JsonPath.read(responsebodies.get(key), jsonpath).toString());
                Report.updateTestLog(Action, "JSON element value stored", Status.DONE);
            } else {
                Report.updateTestLog(Action, "Variable format is not correct", Status.DEBUG);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(
                Action,
                "Error Storing JSON element :" + "\n" + ex.getMessage(),
                Status.DEBUG
            );
        }
    }

    /****** XmlPath Actions ******/

    /**
     * Asserts that an XmlPath query result value contains the expected substring.
     * <p>
     * Uses XPath to extract a value from the XML response and verifies it contains
     * the specified substring.
     * <ul>
     *   <li>Input: Expected substring</li>
     * </ul>
     */
    @Action(
        object = ObjectType.STRUCTUREDDATA,
        desc = "Assert XmlPath Result Contains ",
        input = InputType.YES
    )
    public void assertXmlPathResultContains() {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder;
            InputSource inputSource = new InputSource();
            inputSource.setCharacterStream(new StringReader(responsebodies.get(key)));
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputSource);
            doc.getDocumentElement().normalize();
            XPath xPath = XPathFactory.newInstance().newXPath();
            String expression = resolveStructuredDataPath();
            NodeList nodeList = (NodeList) xPath
                .compile(expression)
                .evaluate(doc, XPathConstants.NODESET);
            Node nNode = nodeList.item(0);
            String value = extractXmlNodeText(nNode);
            String inputValue = getInputValue(Input);
            if (value.contains(inputValue)) {
                Report.updateTestLog(
                    Action,
                    "Element text contains [" + inputValue + "] is as expected",
                    Status.PASSNS
                );
            } else {
                Report.updateTestLog(
                    Action,
                    "Element text [" + value + "] does not contain [" + inputValue + "]",
                    Status.FAILNS
                );
            }
        } catch (
            IOException
            | ParserConfigurationException
            | XPathExpressionException
            | DOMException
            | SAXException ex
        ) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(
                Action,
                "Error validating XML element :" + "\n" + ex.getMessage(),
                Status.DEBUG
            );
        }
    }

    /**
     * Asserts that an XmlPath query result does NOT contain the specified text.
     * <p>
     * Uses XPath to extract a value from the XML response and verifies it does NOT
     * contain the specified substring. This is the negative assertion counterpart to
     * {@link #assertXmlPathResultContains()}.
     * <ul>
     *   <li>Input: Substring that should NOT be present</li>
     * </ul>
     */
    @Action(
        object = ObjectType.STRUCTUREDDATA,
        desc = "Assert XmlPath Result Not Contains ",
        input = InputType.YES
    )
    public void assertXmlPathResultNotContains() {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder;
            InputSource inputSource = new InputSource();
            inputSource.setCharacterStream(new StringReader(responsebodies.get(key)));
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputSource);
            doc.getDocumentElement().normalize();
            XPath xPath = XPathFactory.newInstance().newXPath();
            String expression = resolveStructuredDataPath();
            NodeList nodeList = (NodeList) xPath
                .compile(expression)
                .evaluate(doc, XPathConstants.NODESET);
            Node nNode = nodeList.item(0);
            String value = extractXmlNodeText(nNode);
            String inputValue = getInputValue(Input);
            if (!value.contains(inputValue)) {
                Report.updateTestLog(
                    Action,
                    "Element text [" +
                    value +
                    "] does not contain [" +
                    inputValue +
                    "] as expected",
                    Status.PASSNS
                );
            } else {
                Report.updateTestLog(
                    Action,
                    "Element text [" + value + "] contains [" + inputValue + "] but should not",
                    Status.FAILNS
                );
            }
        } catch (
            IOException
            | ParserConfigurationException
            | XPathExpressionException
            | DOMException
            | SAXException ex
        ) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(
                Action,
                "Error validating XML element :" + "\n" + ex.getMessage(),
                Status.DEBUG
            );
        }
    }

    /**
     * Asserts that an XmlPath query result equals the expected value.
     * <p>
     * Uses XPath to extract a value from the XML response and verifies it matches
     * the expected value exactly.
     * <ul>
     *   <li>Input: Expected value</li>
     * </ul>
     */
    @Action(
        object = ObjectType.STRUCTUREDDATA,
        desc = "Assert XmlPath Result Equals ",
        input = InputType.YES
    )
    public void assertXmlPathResultEquals() {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder;
            InputSource inputSource = new InputSource();
            inputSource.setCharacterStream(new StringReader(responsebodies.get(key)));
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputSource);
            doc.getDocumentElement().normalize();
            XPath xPath = XPathFactory.newInstance().newXPath();
            String expression = resolveStructuredDataPath();
            NodeList nodeList = (NodeList) xPath
                .compile(expression)
                .evaluate(doc, XPathConstants.NODESET);
            Node nNode = nodeList.item(0);
            String value = extractXmlNodeText(nNode);
            String inputValue = getInputValue(Input);
            if (value.equals(inputValue)) {
                Report.updateTestLog(
                    Action,
                    "Element text [" + value + "] is as expected",
                    Status.PASSNS
                );
            } else {
                Report.updateTestLog(
                    Action,
                    "Element text is [" + value + "] but is expected to be [" + inputValue + "]",
                    Status.FAILNS
                );
            }
        } catch (
            IOException
            | ParserConfigurationException
            | XPathExpressionException
            | DOMException
            | SAXException ex
        ) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(
                Action,
                "Error validating XML element :" + "\n" + ex.getMessage(),
                Status.DEBUG
            );
        }
    }

    /**
     * Asserts that an XmlPath query result does NOT equal the specified value.
     * <p>
     * Uses XPath to extract a value from the XML response and verifies it does NOT
     * match the specified value exactly. This is the negative assertion counterpart to
     * {@link #assertXmlPathResultEquals()}.
     * <ul>
     *   <li>Input: Value that should NOT match</li>
     * </ul>
     */
    @Action(
        object = ObjectType.STRUCTUREDDATA,
        desc = "Assert XmlPath Result Not Equals ",
        input = InputType.YES
    )
    public void assertXmlPathResultNotEquals() {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder;
            InputSource inputSource = new InputSource();
            inputSource.setCharacterStream(new StringReader(responsebodies.get(key)));
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputSource);
            doc.getDocumentElement().normalize();
            XPath xPath = XPathFactory.newInstance().newXPath();
            String expression = resolveStructuredDataPath();
            NodeList nodeList = (NodeList) xPath
                .compile(expression)
                .evaluate(doc, XPathConstants.NODESET);
            Node nNode = nodeList.item(0);
            String value = extractXmlNodeText(nNode);
            String inputValue = getInputValue(Input);
            if (!value.equals(inputValue)) {
                Report.updateTestLog(
                    Action,
                    "Element text [" + value + "] is not equal to [" + inputValue + "] as expected",
                    Status.PASSNS
                );
            } else {
                Report.updateTestLog(
                    Action,
                    "Element text [" + value + "] should not be equal to [" + inputValue + "]",
                    Status.FAILNS
                );
            }
        } catch (
            IOException
            | ParserConfigurationException
            | XPathExpressionException
            | DOMException
            | SAXException ex
        ) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(
                Action,
                "Error validating XML element :" + "\n" + ex.getMessage(),
                Status.DEBUG
            );
        }
    }

    /**
     * Asserts that an XmlPath query result starts with the expected prefix.
     * <p>
     * Uses XPath to extract a value from the XML response and verifies it starts with the
     * specified prefix.
     * <ul>
     *   <li>Input: Expected prefix</li>
     * </ul>
     */
    @Action(
        object = ObjectType.STRUCTUREDDATA,
        desc = "Assert XmlPath Result Starts With ",
        input = InputType.YES
    )
    public void assertXmlPathResultStartsWith() {
        try {
            String value = readXmlPathValue(resolveStructuredDataPath());
            String inputValue = getInputValue(Input);
            if (value.startsWith(inputValue)) {
                Report.updateTestLog(
                    Action,
                    "Element text [" + value + "] starts with [" + inputValue + "] as expected",
                    Status.PASSNS
                );
            } else {
                Report.updateTestLog(
                    Action,
                    "Element text [" + value + "] does not start with [" + inputValue + "]",
                    Status.FAILNS
                );
            }
        } catch (
            IOException
            | ParserConfigurationException
            | XPathExpressionException
            | DOMException
            | SAXException ex
        ) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(
                Action,
                "Error validating XML element :" + "\n" + ex.getMessage(),
                Status.DEBUG
            );
        }
    }

    /**
     * Asserts that an XmlPath query result ends with the expected suffix.
     * <p>
     * Uses XPath to extract a value from the XML response and verifies it ends with the
     * specified suffix.
     * <ul>
     *   <li>Input: Expected suffix</li>
     * </ul>
     */
    @Action(
        object = ObjectType.STRUCTUREDDATA,
        desc = "Assert XmlPath Result Ends With ",
        input = InputType.YES
    )
    public void assertXmlPathResultEndsWith() {
        try {
            String value = readXmlPathValue(resolveStructuredDataPath());
            String inputValue = getInputValue(Input);
            if (value.endsWith(inputValue)) {
                Report.updateTestLog(
                    Action,
                    "Element text [" + value + "] ends with [" + inputValue + "] as expected",
                    Status.PASSNS
                );
            } else {
                Report.updateTestLog(
                    Action,
                    "Element text [" + value + "] does not end with [" + inputValue + "]",
                    Status.FAILNS
                );
            }
        } catch (
            IOException
            | ParserConfigurationException
            | XPathExpressionException
            | DOMException
            | SAXException ex
        ) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(
                Action,
                "Error validating XML element :" + "\n" + ex.getMessage(),
                Status.DEBUG
            );
        }
    }

    /**
     * Asserts that an XmlPath query result matches the supplied regular expression.
     * <p>
     * Uses XPath to extract a value from the XML response and verifies it matches (fully) the
     * given Java regular expression.
     * <ul>
     *   <li>Input: Java regular expression</li>
     * </ul>
     */
    @Action(
        object = ObjectType.STRUCTUREDDATA,
        desc = "Assert XmlPath Result Matches Regex ",
        input = InputType.YES
    )
    public void assertXmlPathResultMatchesRegex() {
        try {
            String value = readXmlPathValue(resolveStructuredDataPath());
            String regex = getInputValue(Input);
            if (value.matches(regex)) {
                Report.updateTestLog(
                    Action,
                    "Element text [" + value + "] matches regex [" + regex + "] as expected",
                    Status.PASSNS
                );
            } else {
                Report.updateTestLog(
                    Action,
                    "Element text [" + value + "] does not match regex [" + regex + "]",
                    Status.FAILNS
                );
            }
        } catch (
            IOException
            | ParserConfigurationException
            | XPathExpressionException
            | DOMException
            | SAXException ex
        ) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(
                Action,
                "Error validating XML element :" + "\n" + ex.getMessage(),
                Status.DEBUG
            );
        }
    }

    /**
     * Asserts that an XmlPath query result is numerically greater than the expected value.
     * <ul>
     *   <li>Input: Expected numeric threshold</li>
     * </ul>
     */
    @Action(
        object = ObjectType.STRUCTUREDDATA,
        desc = "Assert XmlPath Result Greater Than ",
        input = InputType.YES
    )
    public void assertXmlPathResultGreaterThan() {
        try {
            String value = readXmlPathValue(resolveStructuredDataPath());
            String inputValue = getInputValue(Input);
            try {
                double actual = Double.parseDouble(value.trim());
                double expected = Double.parseDouble(inputValue.trim());
                if (actual > expected) {
                    Report.updateTestLog(
                        Action,
                        "Element value [" +
                        actual +
                        "] is greater than [" +
                        expected +
                        "] as expected",
                        Status.PASSNS
                    );
                } else {
                    Report.updateTestLog(
                        Action,
                        "Element value [" + actual + "] is not greater than [" + expected + "]",
                        Status.FAILNS
                    );
                }
            } catch (NumberFormatException nfe) {
                Report.updateTestLog(
                    Action,
                    "Cannot compare non-numeric values: actual=[" +
                    value +
                    "], expected=[" +
                    inputValue +
                    "]",
                    Status.FAILNS
                );
            }
        } catch (
            IOException
            | ParserConfigurationException
            | XPathExpressionException
            | DOMException
            | SAXException ex
        ) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(
                Action,
                "Error validating XML element :" + "\n" + ex.getMessage(),
                Status.DEBUG
            );
        }
    }

    /**
     * Asserts that an XmlPath query result is numerically less than the expected value.
     * <ul>
     *   <li>Input: Expected numeric threshold</li>
     * </ul>
     */
    @Action(
        object = ObjectType.STRUCTUREDDATA,
        desc = "Assert XmlPath Result Less Than ",
        input = InputType.YES
    )
    public void assertXmlPathResultLessThan() {
        try {
            String value = readXmlPathValue(resolveStructuredDataPath());
            String inputValue = getInputValue(Input);
            try {
                double actual = Double.parseDouble(value.trim());
                double expected = Double.parseDouble(inputValue.trim());
                if (actual < expected) {
                    Report.updateTestLog(
                        Action,
                        "Element value [" +
                        actual +
                        "] is less than [" +
                        expected +
                        "] as expected",
                        Status.PASSNS
                    );
                } else {
                    Report.updateTestLog(
                        Action,
                        "Element value [" + actual + "] is not less than [" + expected + "]",
                        Status.FAILNS
                    );
                }
            } catch (NumberFormatException nfe) {
                Report.updateTestLog(
                    Action,
                    "Cannot compare non-numeric values: actual=[" +
                    value +
                    "], expected=[" +
                    inputValue +
                    "]",
                    Status.FAILNS
                );
            }
        } catch (
            IOException
            | ParserConfigurationException
            | XPathExpressionException
            | DOMException
            | SAXException ex
        ) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(
                Action,
                "Error validating XML element :" + "\n" + ex.getMessage(),
                Status.DEBUG
            );
        }
    }

    /**
     * Asserts that an XPath expression exists (matches at least one node) in the XML response.
     * <p>
     * The Input column is ignored for this assertion.
     */
    @Action(
        object = ObjectType.STRUCTUREDDATA,
        desc = "Assert XmlPath Exists ",
        input = InputType.NO
    )
    public void assertXmlPathExists() {
        try {
            String expression = resolveStructuredDataPath();
            NodeList nodeList = readXmlPathNodes(expression);
            if (nodeList != null && nodeList.getLength() > 0) {
                Report.updateTestLog(
                    Action,
                    "XPath [" +
                    expression +
                    "] exists (" +
                    nodeList.getLength() +
                    " node(s)) as expected",
                    Status.PASSNS
                );
            } else {
                Report.updateTestLog(
                    Action,
                    "XPath [" + expression + "] does not match any nodes",
                    Status.FAILNS
                );
            }
        } catch (
            IOException
            | ParserConfigurationException
            | XPathExpressionException
            | DOMException
            | SAXException ex
        ) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(
                Action,
                "Error validating XML path existence :" + "\n" + ex.getMessage(),
                Status.DEBUG
            );
        }
    }

    /**
     * Asserts that an XPath expression does NOT match any nodes in the XML response.
     * <p>
     * The Input column is ignored for this assertion.
     */
    @Action(
        object = ObjectType.STRUCTUREDDATA,
        desc = "Assert XmlPath Not Exists ",
        input = InputType.NO
    )
    public void assertXmlPathNotExists() {
        try {
            String expression = resolveStructuredDataPath();
            NodeList nodeList = readXmlPathNodes(expression);
            if (nodeList == null || nodeList.getLength() == 0) {
                Report.updateTestLog(
                    Action,
                    "XPath [" + expression + "] does not match any nodes as expected",
                    Status.PASSNS
                );
            } else {
                Report.updateTestLog(
                    Action,
                    "XPath [" +
                    expression +
                    "] matches " +
                    nodeList.getLength() +
                    " node(s) but should not",
                    Status.FAILNS
                );
            }
        } catch (
            IOException
            | ParserConfigurationException
            | XPathExpressionException
            | DOMException
            | SAXException ex
        ) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(
                Action,
                "Error validating XML path absence :" + "\n" + ex.getMessage(),
                Status.DEBUG
            );
        }
    }

    /**
     * Evaluates an XPath expression against the last Webservice XML response and returns the
     * string value of the first matching node.
     */
    private String readXmlPathValue(String expression)
        throws IOException, ParserConfigurationException, XPathExpressionException, SAXException {
        NodeList nodeList = readXmlPathNodes(expression);
        if (nodeList == null || nodeList.getLength() == 0) {
            return "";
        }
        Node node = nodeList.item(0);
        String value = node.getNodeValue();
        if (value == null) {
            value = node.getTextContent();
        }
        return value == null ? "" : value;
    }

    /**
     * Evaluates an XPath expression against the last Webservice XML response and returns the
     * matching node list.
     */
    private NodeList readXmlPathNodes(String expression)
        throws IOException, ParserConfigurationException, XPathExpressionException, SAXException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        InputSource inputSource = new InputSource();
        inputSource.setCharacterStream(new StringReader(responsebodies.get(key)));
        Document doc = dBuilder.parse(inputSource);
        doc.getDocumentElement().normalize();
        XPath xPath = XPathFactory.newInstance().newXPath();
        return (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
    }

    /**
     * Stores an XmlPath query result in a datasheet column.
     * <p>
     * Extracts a value from the XML response using XPath and stores it in the
     * specified datasheet column.
     * <ul>
     *   <li>Input: sheetName:ColumnName</li>
     * </ul>
     */
    @Action(
        object = ObjectType.STRUCTUREDDATA,
        desc = "Store XmlPath Result In DataSheet ",
        input = InputType.YES
    )
    public void storeXmlPathResultInDataSheet() {
        try {
            String strObj = Input;
            if (strObj.matches(".*:.*")) {
                try {
                    System.out.println(
                        "Updating value in SubIteration " + userData.getSubIteration()
                    );
                    String sheetName = strObj.split(":", 2)[0];
                    String columnName = strObj.split(":", 2)[1];
                    String xmlText = responsebodies.get(key);
                    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder dBuilder;
                    InputSource inputSource = new InputSource();
                    inputSource.setCharacterStream(new StringReader(xmlText));
                    dBuilder = dbFactory.newDocumentBuilder();
                    Document doc = dBuilder.parse(inputSource);
                    doc.getDocumentElement().normalize();
                    XPath xPath = XPathFactory.newInstance().newXPath();
                    String expression = resolveStructuredDataPath();
                    NodeList nodeList = (NodeList) xPath
                        .compile(expression)
                        .evaluate(doc, XPathConstants.NODESET);
                    Node nNode = nodeList.item(0);
                    String value = extractXmlNodeText(nNode);
                    userData.putData(sheetName, columnName, value);
                    Report.updateTestLog(
                        Action,
                        "Element text [" + value + "] is stored in " + strObj,
                        Status.DONE
                    );
                } catch (
                    IOException
                    | ParserConfigurationException
                    | XPathExpressionException
                    | DOMException
                    | SAXException ex
                ) {
                    Logger.getLogger(this.getClass().getName()).log(Level.OFF, ex.getMessage(), ex);
                    Report.updateTestLog(
                        Action,
                        "Error Storing XML element in datasheet :" + "\n" + ex.getMessage(),
                        Status.DEBUG
                    );
                }
            } else {
                Report.updateTestLog(
                    Action,
                    "Given input [" +
                    Input +
                    "] format is invalid. It should be [sheetName:ColumnName]",
                    Status.DEBUG
                );
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(
                Action,
                "Error Storing XML element in datasheet :" + "\n" + ex.getMessage(),
                Status.DEBUG
            );
        }
    }

    /**
     * Stores an XmlPath query result in a variable.
     * <p>
     * Extracts a value from the XML response using XPath and stores it in a variable.
     * <ul>
     *   <li>Input: XPath expression (e.g., //response/token)</li>
     * </ul>
     */
    @Action(
        object = ObjectType.STRUCTUREDDATA,
        desc = "Store XmlPath Result",
        input = InputType.YES
    )
    public void storeXmlPathResultInVariable() {
        try {
            String variableName = Input;
            String expression = resolveStructuredDataPath();
            if (variableName.matches("%.*%")) {
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder;
                InputSource inputSource = new InputSource();
                inputSource.setCharacterStream(new StringReader(responsebodies.get(key)));
                dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(inputSource);
                doc.getDocumentElement().normalize();
                XPath xPath = XPathFactory.newInstance().newXPath();
                NodeList nodeList = (NodeList) xPath
                    .compile(expression)
                    .evaluate(doc, XPathConstants.NODESET);
                Node nNode = nodeList.item(0);
                String value = extractXmlNodeText(nNode);
                addVar(variableName, value);
                Report.updateTestLog(Action, "XML element value stored", Status.DONE);
            } else {
                Report.updateTestLog(Action, "Variable format is not correct", Status.DEBUG);
            }
        } catch (
            IOException
            | ParserConfigurationException
            | XPathExpressionException
            | DOMException
            | SAXException ex
        ) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(
                Action,
                "Error Storing XML element :" + "\n" + ex.getMessage(),
                Status.DEBUG
            );
        }
    }

    /****** Helper Methods ******/

    /*
     * Calculates the count of JSON elements matched by the JsonPath expression.
     * <p>
     * Internal method that handles different JSON types (objects, arrays, primitives)
     * and returns the appropriate count.
     *
     * @return the count of elements matched by the JsonPath expression
     * @throws org.json.simple.parser.ParseException if JSON parsing fails
     */
    public int getJsonElementCount() throws org.json.simple.parser.ParseException {
        int actualObjectCount = 0;
        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject) parser.parse(responsebodies.get(key));
        String jsonpath = resolveStructuredDataPath();

        try {
            Map<String, String> objectMap = JsonPath.read(json, jsonpath);
            actualObjectCount = objectMap.keySet().size();
        } catch (Exception ex) {
            try {
                JSONArray objectMap = JsonPath.read(json, jsonpath);
                actualObjectCount = objectMap.size();
            } catch (Exception ex1) {
                try {
                    net.minidev.json.JSONArray objectMap = JsonPath.read(json, jsonpath);
                    actualObjectCount = objectMap.size();
                } catch (Exception ex2) {
                    String objectMap = JsonPath.read(json, jsonpath);
                    actualObjectCount = 1;
                }
            }
        }
        return actualObjectCount;
    }

    public String getInputValue(String strObj) {
        if (strObj != null && strObj.length() != 0) {
            if (strObj.startsWith("@")) {
                return strObj.substring(1);
            } else if (strObj.matches(("%.*%"))) {
                return getVar(strObj);
            } else if (strObj.matches(".*:.*")) {
                return getDatasheet(strObj);
            }
        }
        return strObj;
    }

    /**
     * Emits diagnostic information about the response body and the resolved path used by
     * a STRUCTUREDDATA action. Helps confirm whether the OR lookup yielded the expected
     * JSON / XML path expression and whether the response body cache is populated for the
     * current key.
     */
    private void logJsonPathContext(String actionName, String response, String path) {
        System.out.println(
            "[StructuredData] " +
            actionName +
            " key=" +
            key +
            " response=" +
            (response == null ? "<null>" : response) +
            " path=" +
            path
        );
    }

    /**
     * Resolves the JSON/XML path used by a STRUCTUREDDATA action.
     * <p>
     * The path is the value of the OR object's attribute (e.g. the {@code JsonPath} or
     * {@code Xpath} attribute on a Structured Data OR object). When the step references
     * an OR object via the Object/Reference columns, this method looks up the attribute
     * value via {@code SObject.findElement(ObjectName, Reference)}. When the OR
     * lookup yields no value (legacy callers that pass the path directly through the
     * Data column), this method falls back to the resolved {@code Data} value.
     *
     * <p>Emits diagnostic information to {@code System.out} so that the resolved path
     * (and the source it came from) can be inspected from the test execution console.
     *
     * @return the path string to use with JsonPath / XPath, never {@code null}
     */
    private String resolveStructuredDataPath() {
        try {
            if (
                SObject != null &&
                ObjectName != null &&
                !ObjectName.isEmpty() &&
                Reference != null &&
                !Reference.isEmpty()
            ) {
                String path = SObject.findElement(ObjectName, Reference);
                System.out.println(
                    "[StructuredData] resolveStructuredDataPath: SObject.findElement(" +
                    "ObjectName=" +
                    ObjectName +
                    ", Reference=" +
                    Reference +
                    ") -> " +
                    path
                );
                if (path != null && !path.isEmpty()) {
                    return path;
                }
                System.out.println(
                    "[StructuredData] resolveStructuredDataPath: OR lookup returned null/empty - " +
                    "falling back to Data=" +
                    Data
                );
            } else {
                System.out.println(
                    "[StructuredData] resolveStructuredDataPath: OR lookup skipped" +
                    " (SObject=" +
                    (SObject == null ? "null" : "set") +
                    ", ObjectName=" +
                    ObjectName +
                    ", Reference=" +
                    Reference +
                    ") - using Data=" +
                    Data
                );
            }
        } catch (Exception ex) {
            System.out.println(
                "[StructuredData] resolveStructuredDataPath: OR lookup threw " +
                ex.getClass().getSimpleName() +
                ": " +
                ex.getMessage() +
                " - falling back to Data=" +
                Data
            );
        }
        return Data == null ? "" : Data;
    }

    /**
     * Extracts the text content of a DOM node selected by an XPath expression.
     * <p>
     * {@link Node#getNodeValue()} returns {@code null} for element nodes (it is only
     * meaningful for attribute, text, CDATA, PI and comment nodes), so it cannot be
     * used unconditionally on the result of an XPath that targets an element. This
     * helper falls back to {@link Node#getTextContent()} when {@code getNodeValue()}
     * is null and returns an empty string when the node itself is null (e.g. the
     * XPath matched no nodes).
     *
     * @param node the DOM node to read text from; may be {@code null}
     * @return the node's text content, never {@code null}
     */
    private String extractXmlNodeText(Node node) {
        if (node == null) {
            return "";
        }
        String value = node.getNodeValue();
        if (value == null) {
            value = node.getTextContent();
        }
        return value == null ? "" : value;
    }
}

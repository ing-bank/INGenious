package com.ing.engine.commands.structuredData;

import com.ing.engine.commands.browser.General;
import com.ing.engine.core.CommandControl;
import com.ing.ingenious.api.status.Status;
import com.ing.ingenious.api.annotation.Action;
import com.ing.ingenious.api.types.InputType;
import com.ing.ingenious.api.types.ObjectType;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import com.jayway.jsonpath.*;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import java.io.StringReader;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.w3c.dom.DOMException;
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
    @Action(object = ObjectType.STRUCTUREDDATA, desc = "Assert JsonPath Result Contains ", input = InputType.YES)
    public void assertJsonPathResultContains() {
        try {
            String response = responsebodies.get(key);
            String jsonpath = Data;
            String value = JsonPath.read(response, jsonpath).toString();
            String strObj = getInputValue(Input);
            if (value.contains(strObj)) {
                Report.updateTestLog(Action, "Element text contains [" + strObj + "] is as expected", Status.PASSNS);
            } else {
                Report.updateTestLog(Action, "Element text [" + value + "] does not contain [" + strObj + "]",
                        Status.FAILNS);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error in validating JSON element :" + "\n" + ex.getMessage(), Status.DEBUG);
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
    @Action(object = ObjectType.STRUCTUREDDATA, desc = "Assert JsonPath Result Not Contains ", input = InputType.YES)
    public void assertJsonPathResultNotContains() {
        try {
            String response = responsebodies.get(key);
            String jsonpath = Data;
            String value = JsonPath.read(response, jsonpath).toString();
            String strObj = getInputValue(Input);
            if (!value.contains(strObj)) {
                Report.updateTestLog(Action, "Element text [" + value + "] does not contain [" + strObj + "] as expected", Status.PASSNS);
            } else {
                Report.updateTestLog(Action, "Element text [" + value + "] contains [" + strObj + "] but should not",
                        Status.FAILNS);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error in validating JSON element :" + "\n" + ex.getMessage(), Status.DEBUG);
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
    @Action(object = ObjectType.STRUCTUREDDATA, desc = "Assert JsonPath Result Equals ", input = InputType.YES)
    public void assertJsonPathResultEquals() {
        try {
            String response = responsebodies.get(key);
            String jsonpath = Data;
            String value = JsonPath.read(response, jsonpath).toString();
            String strObj = getInputValue(Input);
            if (value.equals(strObj)) {
                Report.updateTestLog(Action, "Element text [" + value + "] is as expected", Status.PASSNS);
            } else {
                Report.updateTestLog(Action, "Element text is [" + value + "] but is expected to be [" + strObj + "]",
                        Status.FAILNS);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error in validating JSON element :" + "\n" + ex.getMessage(), Status.DEBUG);
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
    @Action(object = ObjectType.STRUCTUREDDATA, desc = "Assert JsonPath Result Not Equals ", input = InputType.YES)
    public void assertJsonPathResultNotEquals() {
        try {
            String response = responsebodies.get(key);
            String jsonpath = Data;
            String value = JsonPath.read(response, jsonpath).toString();
            String strObj = getInputValue(Input);
            if (!value.equals(strObj)) {
                Report.updateTestLog(Action, "Element text [" + value + "] is not equal to [" + strObj + "] as expected", Status.PASSNS);
            } else {
                Report.updateTestLog(Action, "Element text is [" + value + "] but should not be equal to [" + strObj + "]",
                        Status.FAILNS);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error in validating JSON element :" + "\n" + ex.getMessage(), Status.DEBUG);
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
    @Action(object = ObjectType.STRUCTUREDDATA, desc = "Assert JsonPath Result Count ", input = InputType.YES)
    public void assertJsonPathResultCount() {
        try {
            String response = responsebodies.get(key);
            int actualObjectCount = 0;
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(response);
            String strObj = getInputValue(Input);
            try {
                Map<String, String> objectMap = JsonPath.read(json, Data);
                actualObjectCount = objectMap.keySet().size();
            } catch (Exception ex) {
                try {
                    JSONArray objectMap = JsonPath.read(json, Data);
                    actualObjectCount = objectMap.size();
                } catch (Exception ex1) {
                    try {
                        net.minidev.json.JSONArray objectMap = JsonPath.read(json, Data);
                        actualObjectCount = objectMap.size();
                    } catch (Exception ex2) {
                        String objectMap = JsonPath.read(json, Data);
                        actualObjectCount = 1;
                    }
                }
            }

            int expectedObjectCount = Integer.parseInt(strObj);
            if (actualObjectCount == expectedObjectCount) {
                Report.updateTestLog(Action, "Element count [" + expectedObjectCount + "] is as expected", Status.PASSNS);
            } else {
                Report.updateTestLog(Action, "Element count is [" + actualObjectCount + "] but is expected to be [" + expectedObjectCount + "]", Status.FAILNS);
            }

        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error in validating JSON element :" + "\n" + ex.getMessage(), Status.DEBUG);
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
    @Action(object = ObjectType.STRUCTUREDDATA, desc = "Store JsonPath Result count in Datasheet ", input = InputType.YES)
    public void storeJsonPathResultCountInDataSheet() {
        try {
            String dataSheetReference = Input;
            if (dataSheetReference.matches(".*:.*")) {
                try {
                    System.out.println("Updating value in SubIteration " + userData.getSubIteration());
                    String sheetName = dataSheetReference.split(":", 2)[0];
                    String columnName = dataSheetReference.split(":", 2)[1];
                    String actualObjectCount = Integer.toString(getJsonElementCount());
                    userData.putData(sheetName, columnName, actualObjectCount);
                    Report.updateTestLog(Action, "Element count [" + actualObjectCount + "] is stored in " + dataSheetReference,
                            Status.DONE);
                } catch (Exception ex) {
                    Logger.getLogger(this.getClass().getName()).log(Level.OFF, ex.getMessage(), ex);
                    Report.updateTestLog(Action, "Error Storing JSON element in datasheet :" + "\n" + ex.getMessage(),
                            Status.DEBUG);
                }
            } else {
                Report.updateTestLog(Action,
                        "Given input [" + Input + "] format is invalid. It should be [sheetName:ColumnName]",
                        Status.DEBUG);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error Storing JSON element in datasheet :" + "\n" + ex.getMessage(),
                    Status.DEBUG);
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
    @Action(object = ObjectType.STRUCTUREDDATA, desc = "Store JsonPath Result count in variable ", input = InputType.YES)
    public void storeJsonPathResultCountInVariable() {
        try {
            String varName = Input;
            if (varName.matches("%.*%")) {
                try {
                    System.out.println("Updating value in SubIteration " + userData.getSubIteration());
                    String actualObjectCount = Integer.toString(getJsonElementCount());
                    addVar(varName, actualObjectCount);
                    Report.updateTestLog(Action, "Element count [" + actualObjectCount + "] is stored in " + varName,
                            Status.DONE);
                } catch (Exception ex) {
                    Logger.getLogger(this.getClass().getName()).log(Level.OFF, ex.getMessage(), ex);
                    Report.updateTestLog(Action, "Error Storing JSON element in Variable :" + "\n" + ex.getMessage(),
                            Status.DEBUG);
                }
            } else {
                Report.updateTestLog(Action,
                        "Given condition [" + Condition + "] format is invalid. It should be [%Var%]",
                        Status.DEBUG);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error Storing JSON element in Variable :" + "\n" + ex.getMessage(),
                    Status.DEBUG);
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
    @Action(object = ObjectType.STRUCTUREDDATA, desc = "Store JsonPath Result In DataSheet ", input = InputType.YES)
    public void storeJsonPathResultInDataSheet() {
        try {
            String dataSheetReference = Input;
            if (dataSheetReference.matches(".*:.*")) {
                try {
                    System.out.println("Updating value in SubIteration " + userData.getSubIteration());
                    String sheetName = dataSheetReference.split(":", 2)[0];
                    String columnName = dataSheetReference.split(":", 2)[1];
                    String response = responsebodies.get(key);
                    String jsonpath = Data;
                    String value = JsonPath.read(response, jsonpath).toString();
                    userData.putData(sheetName, columnName, value);
                    Report.updateTestLog(Action, "Element text [" + value + "] is stored in " + dataSheetReference, Status.DONE);
                } catch (Exception ex) {
                    Logger.getLogger(this.getClass().getName()).log(Level.OFF, ex.getMessage(), ex);
                    Report.updateTestLog(Action, "Error Storing JSON element in datasheet :" + "\n" + ex.getMessage(),
                            Status.DEBUG);
                }
            } else {
                Report.updateTestLog(Action,
                        "Given input [" + Input + "] format is invalid. It should be [sheetName:ColumnName]",
                        Status.DEBUG);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error Storing JSON element in datasheet :" + "\n" + ex.getMessage(),
                    Status.DEBUG);
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
    @Action(object = ObjectType.STRUCTUREDDATA, desc = "Store JsonPath Result", input = InputType.YES)
    public void storeJsonPathResultInVariable() {
        try {
            String variableName = Input;
            String jsonpath = Data;
            if (variableName.matches("%.*%")) {
                addVar(variableName, JsonPath.read(responsebodies.get(key), jsonpath).toString());
                Report.updateTestLog(Action, "JSON element value stored", Status.DONE);
            } else {
                Report.updateTestLog(Action, "Variable format is not correct", Status.DEBUG);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error Storing JSON element :" + "\n" + ex.getMessage(), Status.DEBUG);
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
    @Action(object = ObjectType.STRUCTUREDDATA, desc = "Assert XmlPath Result Contains ", input = InputType.YES)
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
            String expression = Data;
            NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
            Node nNode = nodeList.item(0);
            String value = nNode.getNodeValue();
            String inputValue = getInputValue(Input);
            if (value.contains(inputValue)) {
                Report.updateTestLog(Action, "Element text contains [" + inputValue + "] is as expected", Status.PASSNS);
            } else {
                Report.updateTestLog(Action, "Element text [" + value + "] does not contain [" + inputValue + "]",
                        Status.FAILNS);
            }
        } catch (IOException | ParserConfigurationException | XPathExpressionException | DOMException
                | SAXException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error validating XML element :" + "\n" + ex.getMessage(), Status.DEBUG);
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
    @Action(object = ObjectType.STRUCTUREDDATA, desc = "Assert XmlPath Result Not Contains ", input = InputType.YES)
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
            String expression = Data;
            NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
            Node nNode = nodeList.item(0);
            String value = nNode.getNodeValue();
            if (!value.contains(Input)) {
                Report.updateTestLog(Action, "Element text [" + value + "] does not contain [" + Input + "] as expected", Status.PASSNS);
            } else {
                Report.updateTestLog(Action, "Element text [" + value + "] contains [" + Input + "] but should not",
                        Status.FAILNS);
            }
        } catch (IOException | ParserConfigurationException | XPathExpressionException | DOMException
                | SAXException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error validating XML element :" + "\n" + ex.getMessage(), Status.DEBUG);
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
    @Action(object = ObjectType.STRUCTUREDDATA, desc = "Assert XmlPath Result Equals ", input = InputType.YES)
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
            String expression = Data;
            NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
            Node nNode = nodeList.item(0);
            String value = nNode.getNodeValue();
            String inputValue = getInputValue(Input);
            if (value.equals(inputValue)) {
                Report.updateTestLog(Action, "Element text [" + value + "] is as expected", Status.PASSNS);
            } else {
                Report.updateTestLog(Action, "Element text [" + value + "] is not as expected. " + Data, Status.FAILNS);
            }
        } catch (IOException | ParserConfigurationException | XPathExpressionException | DOMException
                | SAXException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error validating XML element :" + "\n" + ex.getMessage(), Status.DEBUG);
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
    @Action(object = ObjectType.STRUCTUREDDATA, desc = "Assert XmlPath Result Not Equals ", input = InputType.YES)
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
            String expression = Data;
            NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
            Node nNode = nodeList.item(0);
            String value = nNode.getNodeValue();
            String inputValue = getInputValue(Input);
            if (!value.equals(inputValue)) {
                Report.updateTestLog(Action, "Element text [" + value + "] is not equal to [" + inputValue + "] as expected", Status.PASSNS);
            } else {
                Report.updateTestLog(Action, "Element text [" + value + "] should not be equal to [" + inputValue + "]", Status.FAILNS);
            }
        } catch (IOException | ParserConfigurationException | XPathExpressionException | DOMException
                | SAXException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error validating XML element :" + "\n" + ex.getMessage(), Status.DEBUG);
        }
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
    @Action(object = ObjectType.STRUCTUREDDATA, desc = "Store XmlPath Result In DataSheet ", input = InputType.YES)
    public void storeXmlPathResultInDataSheet() {
        try {
            String strObj = Input;
            if (strObj.matches(".*:.*")) {
                try {
                    System.out.println("Updating value in SubIteration " + userData.getSubIteration());
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
                    String expression = Data;
                    NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
                    Node nNode = nodeList.item(0);
                    String value = nNode.getNodeValue();
                    userData.putData(sheetName, columnName, value);
                    Report.updateTestLog(Action, "Element text [" + value + "] is stored in " + strObj, Status.DONE);
                } catch (IOException | ParserConfigurationException | XPathExpressionException | DOMException
                        | SAXException ex) {
                    Logger.getLogger(this.getClass().getName()).log(Level.OFF, ex.getMessage(), ex);
                    Report.updateTestLog(Action, "Error Storing XML element in datasheet :" + "\n" + ex.getMessage(),
                            Status.DEBUG);
                }
            } else {
                Report.updateTestLog(Action,
                        "Given input [" + Input + "] format is invalid. It should be [sheetName:ColumnName]",
                        Status.DEBUG);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error Storing XML element in datasheet :" + "\n" + ex.getMessage(),
                    Status.DEBUG);
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
    @Action(object = ObjectType.STRUCTUREDDATA, desc = "Store XmlPath Result", input = InputType.YES)
    public void storeXmlPathResultInVariable() {
        try {
            String variableName = Input;
            String expression = Data;
            if (variableName.matches("%.*%")) {
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder;
                InputSource inputSource = new InputSource();
                inputSource.setCharacterStream(new StringReader(responsebodies.get(key)));
                dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(inputSource);
                doc.getDocumentElement().normalize();
                XPath xPath = XPathFactory.newInstance().newXPath();
                NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
                Node nNode = nodeList.item(0);
                String value = nNode.getNodeValue();
                addVar(variableName, value);
                Report.updateTestLog(Action, "XML element value stored", Status.DONE);
            } else {
                Report.updateTestLog(Action, "Variable format is not correct", Status.DEBUG);
            }
        } catch (IOException | ParserConfigurationException | XPathExpressionException | DOMException
                | SAXException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error Storing XML element :" + "\n" + ex.getMessage(), Status.DEBUG);
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

        try {
            Map<String, String> objectMap = JsonPath.read(json, Data);
            actualObjectCount = objectMap.keySet().size();
        } catch (Exception ex) {
            try {
                JSONArray objectMap = JsonPath.read(json, Data);
                actualObjectCount = objectMap.size();
            } catch (Exception ex1) {
                try {
                    net.minidev.json.JSONArray objectMap = JsonPath.read(json, Data);
                    actualObjectCount = objectMap.size();
                } catch (Exception ex2) {
                    String objectMap = JsonPath.read(json, Data);
                    actualObjectCount = 1;
                }
            }
        }
        return actualObjectCount;
    }
    
    public String getInputValue(String strObj){
        if (strObj!=null && strObj.length()!=0){
            if (strObj.startsWith("@")){
                return strObj.substring(1);
            } else if (strObj.matches(("%.*%"))){
                return getVar(strObj);
            } else if (strObj.matches(".*:.*")){
                return getDatasheet(strObj);
            }
        }
        return strObj;
    }

}

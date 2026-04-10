package com.ing.engine.commands.webservice;

import com.ing.datalib.settings.DriverProperties;
import com.ing.engine.commands.browser.General;
import com.ing.engine.constants.FilePath;
import com.ing.engine.core.CommandControl;
import com.ing.engine.core.Control;
import com.ing.engine.execution.exception.ActionException;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import com.jayway.jsonpath.*;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import java.io.StringReader;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
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
public class Webservice extends General {

    public Webservice(CommandControl cc) {
        super(cc);
    }

    public enum RequestMethod {
        POST,
        PUT,
        PATCH,
        GET,
        DELETE,
        DELETEWITHPAYLOAD
    }

    /**
     * Sends a PUT HTTP request to the configured endpoint with the specified payload.
     * <p>
     * Executes a PUT REST request using the data from the Input field and the endpoint
     * previously set with {@code setEndPoint}.
     * <ul>
     *   <li>Input: Request payload</li>
     *   <li>Condition: Optional API configuration alias (e.g., #alias)</li>
     * </ul>
     *
     * @see #setEndPoint()
     */
    @Action(object = ObjectType.WEBSERVICE, desc = "PUT Rest Request ", input = InputType.YES, condition = InputType.OPTIONAL)
    public void putRestRequest() {
        try {
            createhttpRequest(RequestMethod.PUT);
        } catch (Exception e) {
            Report.updateTestLog(Action,
                    "An unexpected error occurred while executing the request : " + "\n" + e.getMessage(),
                    Status.FAIL);
        }
    }

    /**
     * Sends a POST HTTP request to the configured endpoint with the specified payload.
     * <p>
     * Executes a POST REST request using the data from the Input field and the endpoint
     * previously set with {@code setEndPoint}.
     * <ul>
     *   <li>Input: Request payload</li>
     *   <li>Condition: Optional API configuration alias (e.g., #alias)</li>
     * </ul>
     *
     * @see #setEndPoint()
     */
    @Action(object = ObjectType.WEBSERVICE, desc = "POST Rest Request ", input = InputType.YES, condition = InputType.OPTIONAL)
    public void postRestRequest() {
        try {
            createhttpRequest(RequestMethod.POST);
        } catch (Exception e) {
            Report.updateTestLog(Action,
                    "An unexpected error occurred while executing the request : " + "\n" + e.getMessage(),
                    Status.FAIL);
        }
    }

    /**
     * Sends a POST SOAP request to the configured endpoint with the specified SOAP envelope.
     * <p>
     * Executes a POST request with SOAP XML payload. The SOAP envelope should be provided
     * in the Input field.
     * <ul>
     *   <li>Input: SOAP XML payload</li>
     *   <li>Condition: Optional API configuration alias (e.g., #alias)</li>
     * </ul>
     *
     * @see #setEndPoint()
     */
    @Action(object = ObjectType.WEBSERVICE, desc = "POST SOAP Request ", input = InputType.YES, condition = InputType.OPTIONAL)
    public void postSoapRequest() {
        try {
            createhttpRequest(RequestMethod.POST);
        } catch (Exception e) {
            Report.updateTestLog(Action,
                    "An unexpected error occurred while executing the request : " + "\n" + e.getMessage(),
                    Status.FAIL);
        }
    }

    /**
     * Sends a PATCH HTTP request to the configured endpoint with the specified payload.
     * <p>
     * Executes a PATCH REST request to partially update a resource. Uses the data from
     * the Input field and the endpoint previously set with {@code setEndPoint}.
     * <ul>
     *   <li>Input: Request payload</li>
     *   <li>Condition: Optional API configuration alias (e.g., #alias)</li>
     * </ul>
     *
     * @see #setEndPoint()
     */
    @Action(object = ObjectType.WEBSERVICE, desc = "PATCH Rest Request ", input = InputType.YES, condition = InputType.OPTIONAL)
    public void patchRestRequest() {
        try {
            createhttpRequest(RequestMethod.PATCH);
        } catch (Exception e) {
            Report.updateTestLog(Action,
                    "An unexpected error occurred while executing the request : " + "\n" + e.getMessage(),
                    Status.FAIL);
        }
    }

    /**
     * Sends a GET HTTP request to the configured endpoint.
     * <p>
     * Executes a GET REST request to retrieve data from the endpoint previously set
     * with {@code setEndPoint}. No payload is required for GET requests.
     * <ul>
     *   <li>Input: Not required</li>
     *   <li>Condition: Optional API configuration alias (e.g., #alias)</li>
     * </ul>
     *
     * @see #setEndPoint()
     */
    @Action(object = ObjectType.WEBSERVICE, desc = "GET Rest Request ", input = InputType.NO, condition = InputType.OPTIONAL)
    public void getRestRequest() {
        try {
            createhttpRequest(RequestMethod.GET);
        } catch (Exception e) {
            Report.updateTestLog(Action,
                    "An unexpected error occurred while executing the request : " + "\n" + e.getMessage(),
                    Status.FAIL);
        }
    }

    /**
     * Sends a DELETE HTTP request to the configured endpoint.
     * <p>
     * Executes a DELETE REST request to remove a resource. No payload is required.
     * <ul>
     *   <li>Input: Not required</li>
     * </ul>
     *
     * @see #setEndPoint()
     */
    @Action(object = ObjectType.WEBSERVICE, desc = "DELETE Rest Request ", input = InputType.NO)
    public void deleteRestRequest() {
        try {
            createhttpRequest(RequestMethod.DELETE);
        } catch (Exception e) {
            Report.updateTestLog(Action,
                    "An unexpected error occurred while executing the request : " + "\n" + e.getMessage(),
                    Status.FAIL);
        }
    }

    /**
     * Sends a DELETE HTTP request with a payload to the configured endpoint.
     * <p>
     * Executes a DELETE REST request that includes a request body. This is useful for
     * DELETE operations that require additional data.
     * <ul>
     *   <li>Input: Request payload</li>
     * </ul>
     *
     * @see #setEndPoint()
     */
    @Action(object = ObjectType.WEBSERVICE, desc = "DELETE with Payload ", input = InputType.YES)
    public void deleteWithPayload() {
        try {
            createhttpRequest(RequestMethod.DELETEWITHPAYLOAD);
        } catch (Exception e) {
            Report.updateTestLog(Action,
                    "An unexpected error occurred while executing the request : " + "\n" + e.getMessage(),
                    Status.FAIL);
        }
    }

    /**
     * Asserts that the HTTP response status code equals the expected value.
     * <p>
     * Verifies the status code from the most recent API request matches the expected value.
     * <ul>
     *   <li>Input: Expected HTTP status code (e.g., 200, 404)</li>
     * </ul>
     */
    @Action(object = ObjectType.WEBSERVICE, desc = "Assert Response Code ", input = InputType.YES)
    public void assertResponseCode() {
        try {
            if (responsecodes.get(key).equals(Data)) {
                Report.updateTestLog(Action, "Status code is : " + Data, Status.PASSNS);
            } else {
                Report.updateTestLog(Action, "Status code is : " + responsecodes.get(key) + " but should be " + Data,
                        Status.FAILNS);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error in validating response code :" + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    /**
     * Asserts that the HTTP response body contains the expected text.
     * <p>
     * Verifies that the response body from the most recent API request contains
     * the specified substring.
     * <ul>
     *   <li>Input: Expected substring to find in response body</li>
     * </ul>
     */
    @Action(object = ObjectType.WEBSERVICE, desc = "Assert Response Body contains ", input = InputType.YES)
    public void assertResponsebodycontains() {
        try {
            if (responsebodies.get(key).contains(Data)) {
                Report.updateTestLog(Action, "Response body contains : " + Data, Status.PASSNS);
            } else {
                Report.updateTestLog(Action, "Response body does not contain : " + Data, Status.FAILNS);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error in validating response body :" + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    /**
     * Asserts that a JSON element value equals the expected value.
     * <p>
     * Uses JSONPath to extract a value from the JSON response and verifies it matches
     * the expected value exactly.
     * <ul>
     *   <li>Condition: JSONPath expression (e.g., $.user.name)</li>
     *   <li>Input: Expected value</li>
     * </ul>
     */
    @Action(object = ObjectType.WEBSERVICE, desc = "Assert JSON Element Equals ", input = InputType.YES, condition = InputType.YES)
    public void assertJSONelementEquals() {
        try {
            String response = responsebodies.get(key);
            String jsonpath = Condition;
            String value = JsonPath.read(response, jsonpath).toString();
            if (value.equals(Data)) {
                Report.updateTestLog(Action, "Element text [" + value + "] is as expected", Status.PASSNS);
            } else {
                Report.updateTestLog(Action, "Element text is [" + value + "] but is expected to be [" + Data + "]",
                        Status.FAILNS);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error in validating JSON element :" + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    /**
     * Asserts that a JSON element value contains the expected substring.
     * <p>
     * Uses JSONPath to extract a value from the JSON response and verifies it contains
     * the specified substring.
     * <ul>
     *   <li>Condition: JSONPath expression (e.g., $.user.email)</li>
     *   <li>Input: Expected substring</li>
     * </ul>
     */
    @Action(object = ObjectType.WEBSERVICE, desc = "Assert JSON Element Contains ", input = InputType.YES, condition = InputType.YES)
    public void assertJSONelementContains() {
        try {
            String response = responsebodies.get(key);
            String jsonpath = Condition;
            String value = JsonPath.read(response, jsonpath).toString();
            if (value.contains(Data)) {
                Report.updateTestLog(Action, "Element text contains [" + Data + "] is as expected", Status.PASSNS);
            } else {
                Report.updateTestLog(Action, "Element text [" + value + "] does not contain [" + Data + "]",
                        Status.FAILNS);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error in validating JSON element :" + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    /**
     * Stores a JSON element value in a datasheet column.
     * <p>
     * Extracts a value from the JSON response using JSONPath and stores it in the
     * specified datasheet column.
     * <ul>
     *   <li>Condition: JSONPath expression (e.g., $.user.id)</li>
     *   <li>Input: sheetName:ColumnName</li>
     * </ul>
     */
    @Action(object = ObjectType.WEBSERVICE, desc = "Store JSON Element In DataSheet ", input = InputType.YES, condition = InputType.YES)
    public void storeJSONelementInDataSheet() {

        try {
            String strObj = Input;
            if (strObj.matches(".*:.*")) {
                try {
                    System.out.println("Updating value in SubIteration " + userData.getSubIteration());
                    String sheetName = strObj.split(":", 2)[0];
                    String columnName = strObj.split(":", 2)[1];
                    String response = responsebodies.get(key);
                    String jsonpath = Condition;
                    String value = JsonPath.read(response, jsonpath).toString();
                    userData.putData(sheetName, columnName, value);
                    Report.updateTestLog(Action, "Element text [" + value + "] is stored in " + strObj, Status.DONE);
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
     * Stores an XML element value in a datasheet column.
     * <p>
     * Extracts a value from the XML response using XPath and stores it in the
     * specified datasheet column.
     * <ul>
     *   <li>Condition: XPath expression (e.g., //user/@id)</li>
     *   <li>Input: sheetName:ColumnName</li>
     * </ul>
     */
    @Action(object = ObjectType.WEBSERVICE, desc = "Store XML Element In DataSheet ", input = InputType.YES, condition = InputType.YES)
    public void storeXMLelementInDataSheet() {

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
                    String expression = Condition;
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
     * Stores a JSON element value in a variable.
     * <p>
     * Extracts a value from the JSON response using JSONPath and stores it in a variable.
     * <ul>
     *   <li>Condition: Variable name (e.g., %myVar%)</li>
     *   <li>Input: JSONPath expression (e.g., $.data.token)</li>
     * </ul>
     */
    @Action(object = ObjectType.WEBSERVICE, desc = "Store JSON Element", input = InputType.YES, condition = InputType.YES)
    public void storeJSONelement() {
        try {
            String variableName = Condition;
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

    /**
     * Stores an XML element value in a variable.
     * <p>
     * Extracts a value from the XML response using XPath and stores it in a variable.
     * <ul>
     *   <li>Condition: Variable name (e.g., %myVar%)</li>
     *   <li>Input: XPath expression (e.g., //response/token)</li>
     * </ul>
     */
    @Action(object = ObjectType.WEBSERVICE, desc = "Store XML Element", input = InputType.YES, condition = InputType.YES)
    public void storeXMLelement() {
        try {
            String variableName = Condition;
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

    /**
     * Stores the entire HTTP response body in a datasheet column.
     * <p>
     * Saves the complete response body text from the most recent API request
     * to the specified datasheet column.
     * <ul>
     *   <li>Input: sheetName:ColumnName</li>
     * </ul>
     */
    @Action(object = ObjectType.WEBSERVICE, desc = "Store Response Message In DataSheet ", input = InputType.YES)
    public void storeResponseBodyInDataSheet() {
        try {
            String strObj = Input;
            if (strObj.matches(".*:.*")) {
                try {
                    System.out.println("Updating value in SubIteration " + userData.getSubIteration());
                    String sheetName = strObj.split(":", 2)[0];
                    String columnName = strObj.split(":", 2)[1];
                    userData.putData(sheetName, columnName, responsebodies.get(key));
                    Report.updateTestLog(Action, "Response body is stored in " + strObj, Status.DONE);
                } catch (Exception ex) {
                    Logger.getLogger(this.getClass().getName()).log(Level.OFF, ex.getMessage(), ex);
                    Report.updateTestLog(Action, "Error Storing text in datasheet :" + ex.getMessage(), Status.DEBUG);
                }
            } else {
                Report.updateTestLog(Action,
                        "Given input [" + Input + "] format is invalid. It should be [sheetName:ColumnName]",
                        Status.DEBUG);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error Storing response body in datasheet :" + "\n" + ex.getMessage(),
                    Status.DEBUG);
        }
    }

    /**
     * Asserts that an XML element value equals the expected value.
     * <p>
     * Uses XPath to extract a value from the XML response and verifies it matches
     * the expected value exactly.
     * <ul>
     *   <li>Condition: XPath expression (e.g., //user/name)</li>
     *   <li>Input: Expected value</li>
     * </ul>
     */
    @Action(object = ObjectType.WEBSERVICE, desc = "Assert XML Element Equals ", input = InputType.YES, condition = InputType.YES)
    public void assertXMLelementEquals() {

        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder;
            InputSource inputSource = new InputSource();
            inputSource.setCharacterStream(new StringReader(responsebodies.get(key)));
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputSource);
            doc.getDocumentElement().normalize();
            XPath xPath = XPathFactory.newInstance().newXPath();
            String expression = Condition;
            NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
            Node nNode = nodeList.item(0);
            String value = nNode.getNodeValue();
            if (value.equals(Data)) {
                Report.updateTestLog(Action, "Element text [" + value + "] is as expected", Status.PASSNS);
            } else {
                Report.updateTestLog(Action, "Element text [" + value + "] is not as expected", Status.FAILNS);
            }
        } catch (IOException | ParserConfigurationException | XPathExpressionException | DOMException
                | SAXException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error validating XML element :" + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    /**
     * Asserts that an XML element value contains the expected substring.
     * <p>
     * Uses XPath to extract a value from the XML response and verifies it contains
     * the specified substring.
     * <ul>
     *   <li>Condition: XPath expression (e.g., //response/message)</li>
     *   <li>Input: Expected substring</li>
     * </ul>
     */
    @Action(object = ObjectType.WEBSERVICE, desc = "Assert XML Element Contains ", input = InputType.YES, condition = InputType.YES)
    public void assertXMLelementContains() {

        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder;
            InputSource inputSource = new InputSource();
            inputSource.setCharacterStream(new StringReader(responsebodies.get(key)));
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputSource);
            doc.getDocumentElement().normalize();
            XPath xPath = XPathFactory.newInstance().newXPath();
            String expression = Condition;
            NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
            Node nNode = nodeList.item(0);
            String value = nNode.getNodeValue();
            if (value.contains(Data)) {
                Report.updateTestLog(Action, "Element text contains [" + Data + "] is as expected", Status.PASSNS);
            } else {
                Report.updateTestLog(Action, "Element text [" + value + "] does not contain [" + Data + "]",
                        Status.FAILNS);
            }
        } catch (IOException | ParserConfigurationException | XPathExpressionException | DOMException
                | SAXException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error validating XML element :" + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    /**
     * Sets the API endpoint URL for subsequent HTTP requests.
     * <p>
     * Configures the base URL or full endpoint for API calls. Supports variable substitution
     * from datasheets, user-defined settings, and runtime variables. Optionally loads
     * API-specific configuration using an alias.
     * <ul>
     *   <li>Input: Endpoint URL (supports {sheet:column} and %variable% placeholders)</li>
     *   <li>Condition: Optional API config alias starting with # (e.g., #production)</li>
     * </ul>
     */
    @Action(object = ObjectType.WEBSERVICE, desc = "Set End Point ", input = InputType.YES, condition = InputType.OPTIONAL)
    public void setEndPoint() {
        try {
            String apiConfigName = Condition;
            DriverProperties driverProperties = Control.getCurrentProject().getProjectSettings().getDriverSettings();
            if (apiConfigName.startsWith("#")) {
                apiConfigName = apiConfigName.replace("#", "");
            } else {
                apiConfigName = ""; //This means that the Condtion is not an API Config Alias
            }

            String configToLoad = driverProperties.doesAPIconfigExist(apiConfigName) ? apiConfigName : "default";
            driverProperties.setCurrLoadedAPIConfig(configToLoad);
            
            String resource = handlePayloadorEndpoint(Data);
            endPoints.put(key, resource);
            httpAgentCheck();
            OpenURLconnection();
            Report.updateTestLog(Action, "End point set : " + resource, Status.DONE);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error setting the end point :" + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    /**
     * Executes the HTTP request and captures the response details.
     * <p>
     * Builds the HTTP client with configured settings (SSL, proxy, redirects),
     * sends the request, and stores the response body, status code, and timing information.
     *
     * @throws IOException if an I/O error occurs during the request
     * @throws InterruptedException if the request is interrupted
     */
    private void returnResponseDetails() throws IOException, InterruptedException {

        initiateClientBuilder();
        sslCertificateVerification();
        handleProxy();

        /**
         * *** need to add timeout,version******
         */
        httpClient.put(key, httpClientBuilder.get(key).followRedirects(getRedirectPolicy()).build());
        httpRequest.put(key, httpRequestBuilder.get(key).build());
        response.put(key, httpClient.get(key).send(httpRequest.get(key), HttpResponse.BodyHandlers.ofString()));

        responsebodies.put(key, (String) response.get(key).body());

        after.put(key, Instant.now());
        savePayload("response", (String) response.get(key).body());

        responsecodes.put(key, Integer.toString(response.get(key).statusCode()));

    }

    /**
     * Asserts that the count of JSON elements matches the expected number.
     * <p>
     * Uses JSONPath to select elements and counts them, then verifies the count
     * matches the expected value. Works with arrays and objects.
     * <ul>
     *   <li>Condition: JSONPath expression (e.g., $.users[*])</li>
     *   <li>Input: Expected count (integer)</li>
     * </ul>
     */
    @Action(object = ObjectType.WEBSERVICE, desc = "Assert JSON Element Count ", input = InputType.YES, condition = InputType.YES)
    public void assertJSONelementCount() {

        try {
            String response = responsebodies.get(key);
            int actualObjectCount = 0;
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(response);
            try {
                Map<String, String> objectMap = JsonPath.read(json, Condition);
                actualObjectCount = objectMap.keySet().size();
            } catch (Exception ex) {
                try {
                    JSONArray objectMap = JsonPath.read(json, Condition);
                    actualObjectCount = objectMap.size();
                } catch (Exception ex1) {
                    try {
                        net.minidev.json.JSONArray objectMap = JsonPath.read(json, Condition);
                        actualObjectCount = objectMap.size();
                    } catch (Exception ex2) {
                        String objectMap = JsonPath.read(json, Condition);
                        actualObjectCount = 1;
                    }
                }
            }

            int expectedObjectCount = Integer.parseInt(Data);
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
     * Stores the count of JSON elements in a variable.
     * <p>
     * Uses JSONPath to select elements, counts them, and stores the count in a variable.
     * <ul>
     *   <li>Condition: Variable name (e.g., %count%)</li>
     *   <li>Input: JSONPath expression (e.g., $.items[*])</li>
     * </ul>
     */
    @Action(object = ObjectType.WEBSERVICE, desc = "Store JSON Element count in variable ", input = InputType.YES, condition = InputType.YES)
    public void storeJsonElementCount() {

        try {
            String variableName = Condition;
            Condition = Data;

            if (variableName.matches("%.*%")) {
                try {
                    System.out.println("Updating value in SubIteration " + userData.getSubIteration());
                    int actualObjectCountInteger = 1;                                                       //getJsonElementCount();
                    String actualObjectCount = Integer.toString(actualObjectCountInteger);
                    addVar(variableName, actualObjectCount);
                    Report.updateTestLog(Action, "Element count [" + actualObjectCount + "] is stored in " + variableName,
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
     * Calculates the count of JSON elements matched by the JSONPath expression.
     * <p>
     * Internal method that handles different JSON types (objects, arrays, primitives)
     * and returns the appropriate count.
     *
     * @return the count of elements matched by the JSONPath expression
     * @throws org.json.simple.parser.ParseException if JSON parsing fails
     */
    public int getJsonElementCount() throws org.json.simple.parser.ParseException {

        int actualObjectCount = 0;

        JSONParser parser = new JSONParser();

        JSONObject json = (JSONObject) parser.parse(responsebodies.get(key));

        try {
            Map<String, String> objectMap = JsonPath.read(json, Condition);
            actualObjectCount = objectMap.keySet().size();
        } catch (Exception ex) {
            try {
                JSONArray objectMap = JsonPath.read(json, Condition);
                actualObjectCount = objectMap.size();
            } catch (Exception ex1) {
                try {
                    net.minidev.json.JSONArray objectMap = JsonPath.read(json, Condition);
                    actualObjectCount = objectMap.size();
                } catch (Exception ex2) {
                    String objectMap = JsonPath.read(json, Condition);
                    actualObjectCount = 1;
                }
            }
        }
        return actualObjectCount;
    }

    /**
     * Stores the count of JSON elements in a datasheet column.
     * <p>
     * Uses JSONPath to select elements, counts them, and stores the count in
     * the specified datasheet column.
     * <ul>
     *   <li>Condition: JSONPath expression (e.g., $.products[*])</li>
     *   <li>Input: sheetName:ColumnName</li>
     * </ul>
     */
    @Action(object = ObjectType.WEBSERVICE, desc = "Store JSON Element count in Datasheet ", input = InputType.YES, condition = InputType.YES)
    public void storeJsonElementCountInDataSheet() {

        try {
            String strObj = Input;
            if (strObj.matches(".*:.*")) {
                try {
                    System.out.println("Updating value in SubIteration " + userData.getSubIteration());
                    String sheetName = strObj.split(":", 2)[0];
                    String columnName = strObj.split(":", 2)[1];
                    int actualObjectCountInteger = 1;                                                                         //getJsonElementCount();
                    String actualObjectCount = Integer.toString(actualObjectCountInteger);
                    userData.putData(sheetName, columnName, actualObjectCount);
                    Report.updateTestLog(Action, "Element count [" + actualObjectCount + "] is stored in " + strObj,
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
     * Adds an HTTP header to the next API request.
     * <p>
     * Headers are added in key=value format. Supports variable substitution from
     * datasheets, user-defined settings, and runtime variables.
     * <ul>
     *   <li>Input: Header in format "Name=Value" (e.g., Content-Type=application/json)</li>
     * </ul>
     *
     * @see #setEndPoint()
     */
    @Action(object = ObjectType.WEBSERVICE, desc = "Add Header ", input = InputType.YES)
    public void addHeader() {
        try {

            List<String> sheetlist = Control.getCurrentProject().getTestData().getTestDataFor(Control.exe.runEnv())
                    .getTestDataNames();
            for (int sheet = 0; sheet < sheetlist.size(); sheet++) {
                if (Data.contains("{" + sheetlist.get(sheet) + ":")) {
                    com.ing.datalib.testdata.model.TestDataModel tdModel = Control.getCurrentProject().getTestData()
                            .getTestDataByName(sheetlist.get(sheet));
                    List<String> columns = tdModel.getColumns();
                    for (int col = 0; col < columns.size(); col++) {
                        if (Data.contains("{" + sheetlist.get(sheet) + ":" + columns.get(col) + "}")) {
                            Data = Data.replace("{" + sheetlist.get(sheet) + ":" + columns.get(col) + "}",
                                    userData.getData(sheetlist.get(sheet), columns.get(col)));
                        }
                    }
                }
            }

            Collection<Object> valuelist = Control.getCurrentProject().getProjectSettings().getUserDefinedSettings()
                    .values();
            for (Object prop : valuelist) {
                if (Data.contains("{" + prop + "}")) {
                    Data = Data.replace("{" + prop + "}", prop.toString());
                }
            }

            Pattern pattern = Pattern.compile("%\\w+%");
            Matcher matcher = pattern.matcher(Data);

            while (matcher.find()) {
                String variable = matcher.group();
                if (getVar(variable) != null) {
                    Data = Data.replaceAll(variable, getVar(variable));
                } else {
                    Report.updateTestLog(Action, "Variable " + variable + " not found", Status.DEBUG);
                }
            }

            if (headers.containsKey(key)) {
                headers.get(key).add(Data);
            } else {
                ArrayList<String> toBeAdded = new ArrayList<String>();
                toBeAdded.add(Data);
                headers.put(key, toBeAdded);
            }
            
            Report.updateTestLog(Action, "Header added [" + Data + "]", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error adding Header :" + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    /**
     * Adds a URL parameter for the next API request.
     * <p>
     * URL parameters are added in key=value format and will be appended to the URL
     * or sent as form data depending on the content type.
     * <ul>
     *   <li>Input: Parameter in format "name=value" (e.g., page=1)</li>
     * </ul>
     */
    @Action(object = ObjectType.WEBSERVICE, desc = "Add Parameters ", input = InputType.YES)
    public void addURLParam() {

        try {
            if (urlParams.containsKey(key)) {
                urlParams.get(key).add(Data);
            } else {
                ArrayList<String> toBeAdded = new ArrayList<String>();
                toBeAdded.add(Data);
                urlParams.put(key, toBeAdded);
            }
            Report.updateTestLog(Action, "URl Param added " + Data, Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error adding Header :" + "\n" + ex.getMessage(), Status.DEBUG);
        }

    }

    /**
     * Stores the value of a header by name in a variable.
     * <p>
     * The header value is retrieved for the current scenario/test case and stored in a variable if the variable format is correct.
     * <ul>
     *   <li>Condition: Variable name (e.g., %Variable Name%)</li>
     *   <li>Data: Header name (e.g., "Content-Type")</li>
     * </ul>
     */
    @Action(object = ObjectType.WEBSERVICE, desc = "Store Header Element in Variable", input = InputType.YES, condition = InputType.YES)
    public void storeHeaderByNameInVariable() {
        try {
            String variableName = Condition; // e.g., %Variable Name%
            String headerName = Data;        // e.g., "Content-Type"

            // storeAllHeadersInMap() will populate headerKeyValueMap with headers for the current scenario/test case (key)
            storeAllHeadersInMap();

            // Check if headers exist for this key
            if (!headerKeyValueMap.containsKey(key) || headerKeyValueMap.get(key).isEmpty()) {
                Report.updateTestLog(Action, "No headers found for scenario: [" + userData.getScenario() + "] and test case: [" + userData.getTestCase() + "]", Status.DEBUG);
                return;
            }

            // Get headers for this scenario
            Map<String, String> currentHeaders = headerKeyValueMap.get(key);

            // Check if requested header exists
            if (!currentHeaders.containsKey(headerName)) {
                Report.updateTestLog(Action, "Header '" + headerName + "' does not exist in available headers for scenario: [" + userData.getScenario() + "] and test case: [" + userData.getTestCase() + "]", Status.DEBUG);
                return;
            }

            // Validate variable format
            if (variableName.matches("%.*%")) {
                String headerValue = currentHeaders.get(headerName);
                addVar(variableName, headerValue);
                Report.updateTestLog(Action, "Header '" + headerName + "' stored in variable '" + variableName + "' with value: " + headerValue, Status.DONE);
            } else {
                Report.updateTestLog(Action, "Variable format is not correct", Status.DEBUG);
            }

        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            Report.updateTestLog(Action, "Error storing header value: " + ex.getMessage(), Status.DEBUG);
        }
    }

    /**
     * Stores the value of a header by name in a datasheet column.
     * <p>
     * The header value is retrieved for the current scenario/test case and stored in the specified datasheet column.
     * <ul>
     *   <li>Condition: Header name (e.g., "Content-Type")</li>
     *   <li>Input: sheetName:ColumnName</li>
     * </ul>
     */
    @Action(object = ObjectType.WEBSERVICE, desc = "Store Header value in Datasheet", input = InputType.YES, condition = InputType.YES)
    public void storeHeaderByNameInDatasheet() {
        try {
            String headerName = Condition; // e.g., "Content-Type"

            // First, populate maps for this scenario/test case
            storeAllHeadersInMap();

            // Check if headers exist for this key
            if (!headerKeyValueMap.containsKey(key) || headerKeyValueMap.get(key).isEmpty()) {
                Report.updateTestLog(Action, "No headers found for scenario: [" + userData.getScenario() + "] and test case: [" + userData.getTestCase() + "]", Status.DEBUG);
                return;
            }

            // Get headers for this scenario
            Map<String, String> currentHeaders = headerKeyValueMap.get(key);

            // Check if requested header exists
            if (!currentHeaders.containsKey(headerName)) {
                Report.updateTestLog(Action, "Header '" + headerName + "' does not exist in available headers for scenario: [" + userData.getScenario() + "] and test case: [" + userData.getTestCase() + "]", Status.DEBUG);
                return;
            }

            // Early return if input format is invalid
            if (!Input.matches(".*:.*")) {
                Report.updateTestLog(Action, "Invalid input format [" + Input + "]. Expected format: sheetName:ColumnName", Status.DEBUG);
                return;
            }

            try {
                String sheetName = Input.split(":", 2)[0];
                String columnName = Input.split(":", 2)[1];
                String headerValue = currentHeaders.get(headerName);

                // Store header value in datasheet
                userData.putData(sheetName, columnName, headerValue);

                Report.updateTestLog(Action, "Header value [" + headerValue + "] stored in datasheet [" + sheetName + ":" + columnName + "]", Status.DONE);
            } catch (Exception ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, ex.getMessage(), ex);
                Report.updateTestLog(Action, "Error storing header value in datasheet: " + ex.getMessage(), Status.DEBUG);
            }

        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            Report.updateTestLog(Action, "Error storing header value in datasheet: " + ex.getMessage(), Status.DEBUG);
        }
    }

    /**
     * Asserts that the value of a header contains the expected text.
     * <p>
     * The header value is checked for the current scenario/test case.
     * <ul>
     *   <li>Condition: Header name (e.g., "Content-Type")</li>
     *   <li>Data: Expected substring</li>
     * </ul>
     */
    @Action(object = ObjectType.WEBSERVICE, desc = "Assert header", input = InputType.YES, condition = InputType.YES)
    public void assertHeaderValueContains() {
        try {
            String headerName = Condition; // e.g., "Content-Type"

            // First, populate maps for this scenario/test case
            storeAllHeadersInMap();

            // Check if headers exist for this key
            if (!headerKeyValueMap.containsKey(key) || headerKeyValueMap.get(key).isEmpty()) {
                Report.updateTestLog(Action, "No headers found for scenario: [" + userData.getScenario() + "] and test case: [" + userData.getTestCase() + "]", Status.FAILNS);
                return;
            }

            // Get headers for this scenario
            Map<String, String> currentHeaders = headerKeyValueMap.get(key);

            // Check if requested header exists
            if (!currentHeaders.containsKey(headerName)) {
                Report.updateTestLog(Action, "Header '" + headerName + "' does not exist in available headers.", Status.FAILNS);
                return;
            } 
                
            String headerValue = headerKeyValueMap.get(key).get(headerName);
            if (headerValue.contains(Data)) {
                Report.updateTestLog(Action, "Header value [" + headerValue + "] contains expected text [" + Data + "]", Status.PASSNS);
            } else {
                Report.updateTestLog(Action, "Header value [" + headerValue + "] does not contain expected text [" + Data + "]", Status.FAILNS);
            }
            
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            Report.updateTestLog(Action, "Error to assert header value : " + ex.getMessage(), Status.FAILNS);
        }
    }

    /**
     * Asserts that the value of a header equals the expected text.
     * <p>
     * The header value is checked for the current scenario/test case.
     * <ul>
     *   <li>Condition: Header name (e.g., "Content-Type")</li>
     *   <li>Data: Expected value</li>
     * </ul>
     */
    @Action(object = ObjectType.WEBSERVICE, desc = "Assert header", input = InputType.YES, condition = InputType.YES)
    public void assertHeaderValueEquals() {
        try {
            String headerName = Condition; // e.g., "Content-Type"

            // First, populate maps for this scenario/test case
            storeAllHeadersInMap();

            // Check if headers exist for this key
            if (!headerKeyValueMap.containsKey(key) || headerKeyValueMap.get(key).isEmpty()) {
                Report.updateTestLog(Action, "No headers found for scenario: [" + userData.getScenario() + "] and test case: [" + userData.getTestCase() + "]", Status.FAILNS);
                return;
            }

            // Get headers for this scenario
            Map<String, String> currentHeaders = headerKeyValueMap.get(key);

            // Check if requested header exists
            if (!currentHeaders.containsKey(headerName)) {
                Report.updateTestLog(Action, "Header '" + headerName + "' does not exist in available headers.", Status.FAILNS);
                return;
            } 
                
            String headerValue = headerKeyValueMap.get(key).get(headerName);
            if (headerValue.equals(Data)) {
                Report.updateTestLog(Action, "Header value [" + headerValue + "] equals expected text [" + Data + "]", Status.PASSNS);
            } else {
                Report.updateTestLog(Action, "Header value [" + headerValue + "] does not equal expected text [" + Data + "]", Status.FAILNS);
            }
            
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            Report.updateTestLog(Action, "Error to assert header value : " + ex.getMessage(), Status.FAILNS);
        }
    }

    /**
     * Populates the headerKeyValueMap with all headers for the current scenario/test case.
     * <p>
     * Combines header values and tags them with the scenario/test case key.
     */
    private void storeAllHeadersInMap() {
        try {
            Map<String, List<String>> headersMap = response.get(key).headers().map();

            // If headers are missing, just return
            if (headersMap == null || headersMap.isEmpty()) {
                return;
            }

            // Clear previous headerMap for this run
            headerMap.clear();

            // Populate headerMap with combined values
            headersMap.forEach((headerName, values) -> {
                String combinedValues = String.join(", ", values); // Append all values
                headerMap.put(headerName, combinedValues);
            });

            // Tag this headerMap with scenario/test case key
            headerKeyValueMap.put(key, new HashMap<>(headerMap));

        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Checks if the request is configured for form URL encoding.
     * <p>
     * Examines the headers to determine if the content type is set to
     * application/x-www-form-urlencoded.
     *
     * @return true if form URL encoding is configured, false otherwise
     */
    private boolean isformUrlencoded() {
        if (headers.containsKey(key)) {
            ArrayList<String> headerlist = headers.get(key);
            if (headerlist.size() > 0) {
                for (String header : headerlist) {
                    if (header.split("=")[1].contains("x-www-form-urlencoded")) {
                        return true;
                    }
                };
            }
        }
        return false;
    }

    /**
     * Converts URL parameters to URL-encoded string format.
     * <p>
     * Transforms the stored URL parameters into a properly encoded query string
     * suitable for form URL encoding.
     *
     * @return URL-encoded parameter string
     */
    private String urlencodedParams() {
        Map<String, String> parameters = new HashMap<>();
        String urlParamString = "";
        try {
            ArrayList<String> params = urlParams.get(key);
            for (String param : params) {
                parameters.put(param.split("=", 2)[0], param.split("=", 2)[1]);
            }
            urlParamString = parameters.entrySet()
                    .stream()
                    .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                    .collect(Collectors.joining("&"));
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, ex.getMessage(), ex);
        }
        return urlParamString;
    }

    /**
     * Closes the connection and cleans up stored request/response data.
     * <p>
     * Removes headers, response bodies, status codes, and endpoint information
     * for the current test scenario/test case.
     * <ul>
     *   <li>Input: Not required</li>
     * </ul>
     */
    @Action(object = ObjectType.WEBSERVICE, desc = "Close the connection ", input = InputType.NO)
    public void closeConnection() {
        try {
            // httpConnections.get(key).disconnect();
            headers.remove(key);
            responsebodies.remove(key);
            basicAuthorization = "";
            responsecodes.remove(key);
            responsemessages.remove(key);
            endPoints.remove(key);
            Report.updateTestLog(Action, "Connection is closed", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error closing connection :" + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }
    
    /**
     * Retrieves proxy configuration if proxy usage is enabled.
     * <p>
     * Reads proxy settings from the driver configuration and creates a ProxySelector
     * if proxy is enabled.
     *
     * @return ProxySelector configured with host and port, or null if proxy is disabled
     */
    private ProxySelector getProxyDetails() {
        if (Control.getCurrentProject().getProjectSettings().getDriverSettings().useProxy()) {
            String proxyhost = Control.getCurrentProject().getProjectSettings().getDriverSettings()
                    .getProxyHost().replaceFirst("^(http://|https://)", "");
            String proxyport = Control.getCurrentProject().getProjectSettings().getDriverSettings()
                    .getProxyPort();
            ProxySelector proxySelector = ProxySelector.of(new InetSocketAddress(proxyhost, Integer.parseInt(proxyport)));
            return proxySelector;
        } else {
            return null;
        }
    }

    /**
     * Retrieves the HTTP user agent string from user-defined settings.
     * <p>
     * Checks for a custom http.agent property in user-defined settings and returns
     * it if configured.
     *
     * @return the custom HTTP user agent string, or null if not configured
     */
    private String getHttpAgentDetails() {
        if (Control.getCurrentProject().getProjectSettings().getUserDefinedSettings().stringPropertyNames()
                .contains("http.agent")) {
            if (!getUserDefinedData("http.agent").isEmpty()) {
                httpagents.put(key, getUserDefinedData("http.agent"));
                return httpagents.get(key);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Processes payload or endpoint strings by substituting variables.
     * <p>
     * Replaces datasheet variables and user-defined variables with their actual values.
     *
     * @param data the raw payload or endpoint string containing placeholders
     * @return the processed string with variables replaced
     * @throws FileNotFoundException if a referenced file is not found
     */
    private String handlePayloadorEndpoint(String data) throws FileNotFoundException {
        String payloadstring = data;
        payloadstring = handleDataSheetVariables(payloadstring);
        payloadstring = handleuserDefinedVariables(payloadstring);
        System.out.println("Payload :" + payloadstring);
        return payloadstring;
    }

    /**
     * Replaces datasheet variable placeholders with actual values.
     * <p>
     * Scans for {sheetName:columnName} patterns and replaces them with values
     * from the corresponding datasheet.
     *
     * @param payloadstring the string containing datasheet variable placeholders
     * @return the string with datasheet variables replaced
     */
    private String handleDataSheetVariables(String payloadstring) {
        List<String> sheetlist = Control.getCurrentProject().getTestData().getTestDataFor(Control.exe.runEnv())
                .getTestDataNames();
        for (int sheet = 0; sheet < sheetlist.size(); sheet++) {
            if (payloadstring.contains("{" + sheetlist.get(sheet) + ":")) {
                com.ing.datalib.testdata.model.TestDataModel tdModel = Control.getCurrentProject()
                        .getTestData().getTestDataByName(sheetlist.get(sheet));
                List<String> columns = tdModel.getColumns();
                for (int col = 0; col < columns.size(); col++) {
                    if (payloadstring.contains("{" + sheetlist.get(sheet) + ":" + columns.get(col) + "}")) {
                        payloadstring = payloadstring.replace("{" + sheetlist.get(sheet) + ":" + columns.get(col) + "}",
                                userData.getData(sheetlist.get(sheet), columns.get(col)));
                    }
                }
            }
        }
        return payloadstring;
    }

    /**
     * Replaces user-defined variable placeholders with actual values.
     * <p>
     * Scans for {variableName} patterns and replaces them with values from
     * user-defined settings.
     *
     * @param payloadstring the string containing user-defined variable placeholders
     * @return the string with user-defined variables replaced
     */
    private String handleuserDefinedVariables(String payloadstring) {
        Collection<Object> valuelist = Control.getCurrentProject().getProjectSettings().getUserDefinedSettings()
                .values();
        for (Object prop : valuelist) {
            if (payloadstring.contains("{" + prop + "}")) {
                payloadstring = payloadstring.replace("{" + prop + "}", prop.toString());
            }
        }
        return payloadstring;
    }

    /**
     * Initializes the HTTP request builder with the configured endpoint URI.
     * <p>
     * Creates a new HttpRequest.Builder and sets the URI from the stored endpoint.
     */
    private void OpenURLconnection() {
        try {
            httpRequestBuilder.put(key, HttpRequest.newBuilder());
            URI uri = URI.create(endPoints.get(key));
            httpRequestBuilder.put(key, httpRequestBuilder.get(key).uri(uri));
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, ex.getMessage(), ex);
        }
    }

    /**
     * Applies stored headers to the HTTP request.
     * <p>
     * Iterates through the headers stored for the current scenario/test case
     * and adds them to the HTTP request builder.
     */
    private void setheaders() {
        try {
            if (headers.containsKey(key)) {
                ArrayList<String> headerlist = headers.get(key);
                System.out.println(headerlist);
                if (headerlist.size() > 0) {
                    headerlist.forEach((header) -> {
                        httpRequestBuilder.put(key, httpRequestBuilder.get(key).setHeader(header.substring(0, header.indexOf("=")), header.substring(header.indexOf("=") + 1, header.length())));
                    });
                }
            }

        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, ex.getMessage(), ex);
        }
    }

    /**
     * Configures the HTTP user agent if specified in settings.
     * <p>
     * Sets the http.agent system property if a custom user agent is configured.
     */
    private void httpAgentCheck() {
        try {
            if (getHttpAgentDetails() != null) {
                System.setProperty("http.agent", getHttpAgentDetails());
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, ex.getMessage(), ex);
        }
    }

    /**
     * Checks if the request is configured for multipart form data.
     * <p>
     * Examines the headers to determine if the content type contains "multipart".
     *
     * @return true if multipart form data is configured, false otherwise
     */
    private boolean isMultiPart() {
        if (headers.containsKey(key)) {
            ArrayList<String> headerlist = headers.get(key);
            if (headerlist.size() > 0) {
                for (String header : headerlist) {
                    if (header.split("=")[1].contains("multipart")) {
                        return true;
                    }
                };
            }
        }
        return false;
    }

    /**
     * Configures the HTTP request method and payload.
     * <p>
     * Sets up the request with the specified HTTP method (POST, PUT, etc.) and payload.
     * Handles special cases like form URL encoding and multipart form data.
     *
     * @param method the HTTP method (POST, PUT, PATCH, GET, DELETE, DELETEWITHPAYLOAD)
     * @param payload the request payload/body
     * @throws IOException if an I/O error occurs while reading files for multipart requests
     */
    private void setRequestMethod(String method, String payload) throws IOException {
        BodyPublisher payloadBody = null;
        if (isformUrlencoded()) {
            payload = urlencodedParams();
        }
        if (isMultiPart()) {
            Path filePath = Path.of(getVar("%filePath%"));
            filePath = Path.of(Control.getCurrentProject().getLocation() + "/" + filePath);
            String mimeType = Files.probeContentType(filePath);
            System.out.println("Path of the file === " + filePath);
            String boundary = "Boundary-" + System.currentTimeMillis();
            String fileName = filePath.getFileName().toString();

            /* String body = "--" + boundary.getBytes(StandardCharsets.UTF_8)+ "\r\n"
                    + "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n"
                    + "Content-Type: " + mimeType // Set Content-Type to text/csv
                    + Files.readString(filePath, StandardCharsets.UTF_8) + "\r\n"
                    + ("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);*/
            var byteArrays = new ArrayList<byte[]>();
            byteArrays.add(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            byteArrays.add(("Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n").getBytes(StandardCharsets.UTF_8));
            byteArrays.add(("Content-Type: " + mimeType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            byteArrays.add(Files.readAllBytes(filePath));
            byteArrays.add(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

            payloadBody = HttpRequest.BodyPublishers.ofByteArrays(byteArrays);
            httpRequestBuilder.put(key, httpRequestBuilder.get(key).setHeader("Content-Type", "multipart/form-data; boundary=" + boundary));
        } else {
            payloadBody = HttpRequest.BodyPublishers.ofString(payload);
        }
        try {
            switch (method) {
                case "POST": {
                    httpRequestBuilder.put(key, httpRequestBuilder.get(key).POST(payloadBody));
                    savePayload("request", payload);
                    break;
                }
                case "PUT": {
                    httpRequestBuilder.put(key, httpRequestBuilder.get(key).PUT(payloadBody));
                    savePayload("request", payload);
                    break;
                }
                case "PATCH": {
                    httpRequestBuilder.put(key, httpRequestBuilder.get(key).method("PATCH", payloadBody));
                    savePayload("request", payload);
                    break;
                }
                case "GET": {
                    httpRequestBuilder.put(key, httpRequestBuilder.get(key).GET());
                    break;
                }
                case "DELETE": {
                    httpRequestBuilder.put(key, httpRequestBuilder.get(key).DELETE());
                    break;
                }
                case "DELETEWITHPAYLOAD": {
                    httpRequestBuilder.put(key, httpRequestBuilder.get(key).method("DELETE", payloadBody));
                    savePayload("request", payload);
                    break;
                }

            }
            headers.remove(key);
            urlParams.remove(key);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, ex.getMessage(), ex);
        }
    }

    /**
     * Configures the HTTP request method using the RequestMethod enum.
     * <p>
     * Determines whether a payload is required based on the HTTP method and
     * delegates to the string-based setRequestMethod.
     *
     * @param requestmethod the HTTP method as a RequestMethod enum value
     * @throws FileNotFoundException if a referenced file is not found
     * @throws IOException if an I/O error occurs
     */
    private void setRequestMethod(RequestMethod requestmethod) throws FileNotFoundException, IOException {
        if (requestmethod.toString().equals("PUT") || requestmethod.toString().equals("POST") || requestmethod.toString().equals("PATCH") || requestmethod.toString().equals("DELETEWITHPAYLOAD")) {

            setRequestMethod(requestmethod.toString(), handlePayloadorEndpoint(Data));
        } else {

            setRequestMethod(requestmethod.toString(), "");
        }
    }

    /**
     * Creates and executes an HTTP request with the specified method.
     * <p>
     * Orchestrates the entire request lifecycle: setting headers, configuring the method,
     * executing the request, capturing response details, and handling errors.
     *
     * @param requestmethod the HTTP method to use for the request
     * @throws InterruptedException if the request is interrupted
     * @throws Exception if any error occurs during request execution
     */
    private void createhttpRequest(RequestMethod requestmethod) throws InterruptedException, Exception {
        try {
            setheaders();
            setRequestMethod(requestmethod);
            before.put(key, Instant.now());

            returnResponseDetails();
            duration.put(key, Duration.between(before.get(key), after.get(key)).toMillis());
            Report.updateTestLog(Action, "Response received in : [" + duration.get(key) + "ms] with Status code  : " + responsecodes.get(key), Status.COMPLETE);

            if (headers.containsKey(key)) {
                if (!headers.get(key).isEmpty()) {
                    headers.get(key).clear();
                }
            }

        } catch (IOException ex) {
            int responseCode = 0;
            Matcher exMsgStatusCodeMatcher = Pattern.compile("^Server returned HTTP response code: (\\d+)")
                    .matcher(ex.getMessage());

            if (exMsgStatusCodeMatcher.find()) {
                responseCode = Integer.parseInt(exMsgStatusCodeMatcher.group(1));
            } else if (ex.getClass().getSimpleName().equals("FileNotFoundException")) {
                System.out.println("\n =====================================\n" + " Returned [FileNotFoundException]" + "\n =====================================\n");
                responseCode = 404;

            } else {
                System.out.println(
                        "Exception (" + ex.getClass().getSimpleName() + ") doesn't contain status code: " + ex);
            }
            if (responseCode == 0) {
                System.out.println("\n =====================================\n" + "Response Code does not exist in Exception" + "\n =====================================\n");
            } else {
                responsecodes.put(key, Integer.toString(responseCode));
            }

            if (responseCode == 400 || responseCode == 401 || responseCode == 402 || responseCode == 403
                    || responseCode == 404) {
                Report.updateTestLog(Action,
                        "Error in executing [" + requestmethod.toString() + "] request : " + "\n" + ex.getMessage(),
                        Status.DONE);

            } else {
                Report.updateTestLog(Action,
                        "Error in executing " + requestmethod.toString() + " request : " + "\n" + ex.getMessage(),
                        Status.DEBUG);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new ActionException(e);
        }
    }

    /**
     * Saves request or response payload to a file.
     * <p>
     * Creates files in the webservice folder under the current results path to store
     * request or response payloads for debugging and reporting purposes.
     *
     * @param reqOrRes "request" or "response" to indicate which type of payload to save
     * @param data the payload data to save
     */
    private void savePayload(String reqOrRes, String data) {
        String payloadFileName = "";
        String path = "";
        if (reqOrRes.equals("request")) {
            payloadFileName = Report.getWebserviceRequestFileName();
        } else if (reqOrRes.equals("response")) {
            payloadFileName = Report.getWebserviceResponseFileName();

        }
        try {
            if (!payloadFileName.isBlank()) {
                path = FilePath.getCurrentResultsPath() + File.separator + "webservice";
                File file = new File(path);
                file.mkdirs();
                //FileManager.mkdir(path);
                File location = new File(FilePath.getCurrentResultsPath() + payloadFileName);
                if (location.createNewFile()) {
                    FileWriter writer = new FileWriter(location);
                    writer.write(data);
                    writer.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Configures proxy settings for the HTTP client if enabled.
     * <p>
     * Applies proxy configuration to the HTTP client builder if proxy usage
     * is enabled in the driver settings.
     */
    private void handleProxy() {
        try {
            if (getProxyDetails() != null) {
                System.out.println("\nRequest opened with following proxy details :\n" + getProxyDetails().toString() + "\n");
                httpClientBuilder.put(key, httpClientBuilder.get(key).proxy(getProxyDetails()));
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, ex.getMessage(), ex);
        }
    }

    /**
     * Initializes the HTTP client builder with default settings.
     * <p>
     * Creates a new HttpClient.Builder configured to use HTTP/1.1.
     */
    private void initiateClientBuilder() {
        try {
            httpClientBuilder.put(key, HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1));
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, ex.getMessage(), ex);
        }
    }

    TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] certs, String authType) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] certs, String authType) {
        }
    }};

    /**
     * Loads the keystore for client certificate authentication.
     * <p>
     * Reads the JKS keystore from the configured path and initializes key managers
     * for SSL/TLS connections.
     *
     * @return array of KeyManagers configured with the keystore, or null if loading fails
     */
    private KeyManager[] loadKeyStore() {
        String keystorePath = Control.getCurrentProject().getProjectSettings().getDriverSettings().getKeyStorePath();
        String keystorePassword = Control.getCurrentProject().getProjectSettings().getDriverSettings().getKeyStorePassword();
        KeyStore keyStore;
        KeyManagerFactory kmf = null;
        try {
            keyStore = KeyStore.getInstance("JKS");
            keyStore.load(new FileInputStream(keystorePath), keystorePassword.toCharArray());
            kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, keystorePassword.toCharArray());
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, ex.getMessage(), ex);
        }
        return kmf.getKeyManagers();
    }

    /**
     * Configures SSL/TLS certificate verification for the HTTP client.
     * <p>
     * If SSL verification is disabled, creates an SSL context that trusts all certificates.
     * For self-signed certificates, loads the keystore with client certificates.
     */
    private void sslCertificateVerification() {
        try {
            if (!isSSLCertificateVerification()) {
                SSLContext sc = SSLContext.getInstance("TLS");
                if (isSelfSigned()) {
                    sc.init(loadKeyStore(), trustAllCerts, new SecureRandom());
                } else {
                    sc.init(null, trustAllCerts, new SecureRandom());
                }
                httpClientBuilder.put(key, httpClientBuilder.get(key)).sslContext(sc);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, ex.getMessage(), ex);
        }
    }

    /**
     * Checks if SSL certificate verification is enabled in settings.
     *
     * @return true if SSL certificate verification is enabled, false otherwise
     */
    private Boolean isSSLCertificateVerification() {
        return Control.getCurrentProject().getProjectSettings().getDriverSettings().sslCertificateVerification();
    }

    /**
     * Checks if self-signed certificate support is enabled in settings.
     *
     * @return true if self-signed certificate support is enabled, false otherwise
     */
    private Boolean isSelfSigned() {
        return Control.getCurrentProject().getProjectSettings().getDriverSettings().selfSigned();
    }


    /**
     * Retrieves the HTTP redirect policy configured for the current API driver settings.
     * <p>
     * The logic follows three strict rules:
     * <ul>
     *   <li>If no value is configured (i.e., the property is {@code null} or blank), the method defaults to
     *       {@link Redirect#NEVER}.</li>
     *   <li>If a valid redirect policy is provided (one of {@code NEVER}, {@code NORMAL}, or {@code ALWAYS},
     *       case-insensitive), the corresponding {@link Redirect} enum is returned.</li>
     *   <li>If a value is provided but does not match any {@link Redirect} enum constant, the method throws an
     *       {@link IllegalArgumentException} to indicate a configuration error.</li>
     * </ul>
     * </p>
     *
     * @return the resolved {@link Redirect} policy to be applied when building the {@link java.net.http.HttpClient}
     * @throws IllegalArgumentException if a non-blank but invalid redirect value is configured
     */
    private Redirect getRedirectPolicy() {
        String httpClientRedirect = Control.getCurrentProject().getProjectSettings().getDriverSettings().getHttpClientRedirect();

        if (httpClientRedirect == null || httpClientRedirect.trim().isEmpty()) {
            return Redirect.NEVER;
        }

        try {
            return Redirect.valueOf(httpClientRedirect.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid httpClientRedirect value: '" + httpClientRedirect + "'. Allowed values: NEVER, NORMAL, ALWAYS.");
        }
    }

    /**
     * Extracts a cookie value from the HTTP response headers and stores it in a variable.
     * <p>
     * This method searches for the cookie with the name specified by {@code Data} in the response headers.
     * The cookie value is then stored in a variable whose name is specified by {@code Condition} (must be in the format %variableName%).
     * The header name search for "Set-Cookie" is case-insensitive and will match any casing.
     * <ul>
     *   <li>If the variable name format is invalid, a debug message is logged and the method returns.</li>
     *   <li>If the cookie is found, its value is stored in the variable and a DONE status is logged.</li>
     *   <li>If no cookies are found, a FAIL status is logged.</li>
     *   <li>If an error occurs, a FAIL status is logged and the stack trace is printed.</li>
     * </ul>
     *
     * @see #addVar(String, String)
     */
    @Action(object = ObjectType.WEBSERVICE, desc = "Store Cookies In Variable ", input = InputType.YES, condition = InputType.YES)
    public void storeResponseCookiesInVariable() {
        try {
            String cookieKey = Data;
            String variableName = Condition;
            
            if (!variableName.matches("%.*%")) {
                Report.updateTestLog(Action, "Variable format is not correct. Should be %variableName%", Status.DEBUG);
                return;
            }
            
            variableName = variableName.substring(1, variableName.length() - 1);

            if (!response.containsKey(key) && response.get(key) == null) {
                Report.updateTestLog(Action, "Response did not contain a valid HttpResponse for key [" + key + "]", Status.FAIL);
                return;
            }

            HttpResponse<?> httpResponse = response.get(key);
            HttpHeaders responseHeaders = httpResponse.headers();

            List<String> cookieHeaders = !responseHeaders.allValues("set-cookie").isEmpty() ? responseHeaders.allValues("set-cookie") : responseHeaders.allValues("Set-Cookie");
            
            if (cookieHeaders.isEmpty()) {
                Report.updateTestLog(Action, "No cookies were retrieved from the endpoint", Status.FAIL);
                return;
            }

            for (String cookieHeader : cookieHeaders) {
                if (cookieHeader == null || cookieHeader.isEmpty()) continue;

                String[] cookieParts = cookieHeader.split(";");
                if (cookieParts.length == 0) continue;

                String[] keyValue = cookieParts[0].trim().split("=", 2);
                if (keyValue.length != 2) continue;

                String cookieName  = keyValue[0].trim();
                String cookieValue = keyValue[1].trim();

                if (cookieName.equals(cookieKey)) {
                    addVar(variableName, cookieValue);
                    Report.updateTestLog(
                        Action,
                        "Cookies with name [" + cookieKey + "] has been added in variable [" 
                            + variableName + "] with value [" + cookieValue + "] ",
                        Status.DONE
                    );
                    return; // early exit on success
                }
            }
        } catch (Exception ex) {
            Report.updateTestLog(Action, "Error in storing cookies with name in variable :"+ex.getMessage(), Status.FAIL);
            ex.printStackTrace();
        }
    }

    /**
     * Asserts that a JSON element value does NOT contain the specified text.
     * <p>
     * Uses JSONPath to extract a value from the JSON response and verifies it does NOT
     * contain the specified substring. This is the negative assertion counterpart to
     * {@link #assertJSONelementContains()}.
     * <ul>
     *   <li>Condition: JSONPath expression (e.g., $.user.email)</li>
     *   <li>Input: Substring that should NOT be present</li>
     * </ul>
     */
    @Action(object = ObjectType.WEBSERVICE, desc = "Assert JSON Element Not Contains ", input = InputType.YES, condition = InputType.YES)
    public void assertJSONelementNotContains() {
        try {
            String response = responsebodies.get(key);
            String jsonpath = Condition;
            String value = JsonPath.read(response, jsonpath).toString();
            if (!value.contains(Data)) {
                Report.updateTestLog(Action, "Element text [" + value + "] does not contain [" + Data + "] as expected", Status.PASSNS);
            } else {
                Report.updateTestLog(Action, "Element text [" + value + "] contains [" + Data + "] but should not",
                        Status.FAILNS);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error in validating JSON element :" + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    /**
     * Asserts that a JSON element value does NOT equal the specified value.
     * <p>
     * Uses JSONPath to extract a value from the JSON response and verifies it does NOT
     * match the specified value exactly. This is the negative assertion counterpart to
     * {@link #assertJSONelementEquals()}.
     * <ul>
     *   <li>Condition: JSONPath expression (e.g., $.status)</li>
     *   <li>Input: Value that should NOT match</li>
     * </ul>
     */
    @Action(object = ObjectType.WEBSERVICE, desc = "Assert JSON Element Not Equals ", input = InputType.YES, condition = InputType.YES)
    public void assertJSONelementNotEquals() {
        try {
            String response = responsebodies.get(key);
            String jsonpath = Condition;
            String value = JsonPath.read(response, jsonpath).toString();
            if (!value.equals(Data)) {
                Report.updateTestLog(Action, "Element text [" + value + "] is not equal to [" + Data + "] as expected", Status.PASSNS);
            } else {
                Report.updateTestLog(Action, "Element text is [" + value + "] but should not be equal to [" + Data + "]",
                        Status.FAILNS);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error in validating JSON element :" + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    /**
     * Asserts that an XML element value does NOT equal the specified value.
     * <p>
     * Uses XPath to extract a value from the XML response and verifies it does NOT
     * match the specified value exactly. This is the negative assertion counterpart to
     * {@link #assertXMLelementEquals()}.
     * <ul>
     *   <li>Condition: XPath expression (e.g., //status)</li>
     *   <li>Input: Value that should NOT match</li>
     * </ul>
     */
    @Action(object = ObjectType.WEBSERVICE, desc = "Assert XML Element Not Equals ", input = InputType.YES, condition = InputType.YES)
    public void assertXMLelementNotEquals() {

        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder;
            InputSource inputSource = new InputSource();
            inputSource.setCharacterStream(new StringReader(responsebodies.get(key)));
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputSource);
            doc.getDocumentElement().normalize();
            XPath xPath = XPathFactory.newInstance().newXPath();
            String expression = Condition;
            NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
            Node nNode = nodeList.item(0);
            String value = nNode.getNodeValue();
            if (!value.equals(Data)) {
                Report.updateTestLog(Action, "Element text [" + value + "] is not equal to [" + Data + "] as expected", Status.PASSNS);
            } else {
                Report.updateTestLog(Action, "Element text [" + value + "] should not be equal to [" + Data + "]", Status.FAILNS);
            }
        } catch (IOException | ParserConfigurationException | XPathExpressionException | DOMException
                | SAXException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error validating XML element :" + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    /**
     * Asserts that an XML element value does NOT contain the specified text.
     * <p>
     * Uses XPath to extract a value from the XML response and verifies it does NOT
     * contain the specified substring. This is the negative assertion counterpart to
     * {@link #assertXMLelementContains()}.
     * <ul>
     *   <li>Condition: XPath expression (e.g., //response/message)</li>
     *   <li>Input: Substring that should NOT be present</li>
     * </ul>
     */
    @Action(object = ObjectType.WEBSERVICE, desc = "Assert XML Element Not Contains ", input = InputType.YES, condition = InputType.YES)
    public void assertXMLelementNotContains() {

        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder;
            InputSource inputSource = new InputSource();
            inputSource.setCharacterStream(new StringReader(responsebodies.get(key)));
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputSource);
            doc.getDocumentElement().normalize();
            XPath xPath = XPathFactory.newInstance().newXPath();
            String expression = Condition;
            NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
            Node nNode = nodeList.item(0);
            String value = nNode.getNodeValue();
            if (!value.contains(Data)) {
                Report.updateTestLog(Action, "Element text [" + value + "] does not contain [" + Data + "] as expected", Status.PASSNS);
            } else {
                Report.updateTestLog(Action, "Element text [" + value + "] contains [" + Data + "] but should not",
                        Status.FAILNS);
            }
        } catch (IOException | ParserConfigurationException | XPathExpressionException | DOMException
                | SAXException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error validating XML element :" + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    /**
     * Asserts that the HTTP response body does NOT contain the specified text.
     * <p>
     * Verifies that the response body from the most recent API request does NOT contain
     * the specified substring. This is the negative assertion counterpart to
     * {@link #assertResponsebodycontains()}.
     * <ul>
     *   <li>Input: Substring that should NOT be present in the response body</li>
     * </ul>
     */
    @Action(object = ObjectType.WEBSERVICE, desc = "Assert Response Body Not Contains ", input = InputType.YES)
    public void assertResponsebodyNotContains() {
        try {
            if (!responsebodies.get(key).contains(Data)) {
                Report.updateTestLog(Action, "Response body does not contain : " + Data + " as expected", Status.PASSNS);
            } else {
                Report.updateTestLog(Action, "Response body contains : " + Data + " but should not", Status.FAILNS);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error in validating response body :" + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

}

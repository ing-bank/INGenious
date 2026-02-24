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

    private String handlePayloadorEndpoint(String data) throws FileNotFoundException {
        String payloadstring = data;
        payloadstring = handleDataSheetVariables(payloadstring);
        payloadstring = handleuserDefinedVariables(payloadstring);
        System.out.println("Payload :" + payloadstring);
        return payloadstring;
    }

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

    private void OpenURLconnection() {
        try {
            httpRequestBuilder.put(key, HttpRequest.newBuilder());
            URI uri = URI.create(endPoints.get(key));
            httpRequestBuilder.put(key, httpRequestBuilder.get(key).uri(uri));
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, ex.getMessage(), ex);
        }
    }

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

    private void httpAgentCheck() {
        try {
            if (getHttpAgentDetails() != null) {
                System.setProperty("http.agent", getHttpAgentDetails());
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, ex.getMessage(), ex);
        }
    }

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

    private void setRequestMethod(RequestMethod requestmethod) throws FileNotFoundException, IOException {
        if (requestmethod.toString().equals("PUT") || requestmethod.toString().equals("POST") || requestmethod.toString().equals("PATCH") || requestmethod.toString().equals("DELETEWITHPAYLOAD")) {

            setRequestMethod(requestmethod.toString(), handlePayloadorEndpoint(Data));
        } else {

            setRequestMethod(requestmethod.toString(), "");
        }
    }

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

    private KeyManager[] loadKeyStore() {
        String keystorePath = Control.getCurrentProject().getProjectSettings().getDriverSettings().getProperty("keyStorePath");
        String keystorePassword = Control.getCurrentProject().getProjectSettings().getDriverSettings().getProperty("keyStorePassword");
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

    private Boolean isSSLCertificateVerification() {
        return Control.getCurrentProject().getProjectSettings().getDriverSettings().sslCertificateVerification();
    }

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

}

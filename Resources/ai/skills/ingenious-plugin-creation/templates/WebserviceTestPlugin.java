package com.ing.plugin.webservice;

import com.ing.ingenious.api.annotation.Action;
import com.ing.ingenious.api.contract.WebservicePluginApi;
import com.ing.ingenious.api.contract.data.UserDataAccessApi;
import com.ing.ingenious.api.contract.reports.TestCaseReportApi;
import com.ing.ingenious.api.types.ObjectType;
import com.ing.ingenious.api.types.InputType;
import com.ing.ingenious.api.types.RequestMethod;
import com.ing.ingenious.api.status.Status;

import com.jayway.jsonpath.JsonPath;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Webservice Test Plugin for REST API testing
 * 
 * @author Your Name
 */
public class WebserviceTestPlugin {

    WebservicePluginApi gen;

    public String Data;
    public String Action;
    public String Input;
    public String Condition;
    public TestCaseReportApi Report;
    public UserDataAccessApi userData;
    public String ObjectName;

    // Webservice-specific fields
    public String endpoint;
    public String responseCode;
    public String responseMessage;
    public String responseBody;
    public Object connection;
    public String httpAgent;

    // Shared storage - direct access to framework's static maps
    private String key;
    private Map<String, String> endPoints;
    private Map<String, ArrayList<String>> headers;
    private Map<String, ArrayList<String>> urlParams;
    private Map<String, String> responseBodies;
    private Map<String, String> responseCodes;
    private Map<String, String> responseMessages;

    public WebserviceTestPlugin(WebservicePluginApi gen) {
        System.out.println("WebserviceTestPlugin initialized with WebservicePluginApi: " + gen);
        this.gen = gen;
        this.Data = gen.getData();
        this.Action = gen.getAction();
        this.Input = gen.getInput();
        this.Condition = gen.getCondition();
        this.Report = gen.getReport();
        this.userData = gen.getUserData();
        this.ObjectName = gen.getObjectName();
        
        // Initialize webservice-specific fields from API
        this.endpoint = gen.Endpoint();
        this.responseCode = gen.ResponseCode();
        this.responseMessage = gen.ResponseMessage();
        this.responseBody = gen.ResponseBody();
        this.connection = gen.Connection();
        this.httpAgent = gen.HttpAgent();
        
        // Get references to shared maps from framework
        this.key = gen.getKey();
        this.endPoints = gen.getEndPointsMap();
        this.headers = gen.getHeadersMap();
        this.urlParams = gen.getUrlParamsMap();
        this.responseBodies = gen.getResponseBodiesMap();
        this.responseCodes = gen.getResponseCodesMap();
        this.responseMessages = gen.getResponseMessagesMap();
    }

    /**
     * Execute GET REST request
     */
    @Action(object = ObjectType.WEBSERVICE, desc = "GET Rest Request ", input = InputType.NO, condition = InputType.OPTIONAL)
    public void getRestRequest() {
        try {
            gen.createHttpRequest(RequestMethod.GET);
            
            // Update local fields with response
            this.responseCode = gen.ResponseCode();
            this.responseBody = gen.ResponseBody();
            this.responseMessage = gen.ResponseMessage();
            
            /** 
             * Report here is optional. createHttpRequest already updates the report with request and response details. 
             * This is just an additional log entry if you want to explicitly log the successful execution of the GET request
             */
            // Report.updateTestLog(Action, "GET request executed successfully. Response code: " + responseCode, Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(Action,
                    "An unexpected error occurred while executing the GET request : " + "\n" + e.getMessage(),
                    Status.FAIL);
        }
    }

    /**
     * Store JSON element value in runtime variable
     */
    @Action(object = ObjectType.WEBSERVICE, desc = "Store JSON Element", input = InputType.YES, condition = InputType.YES)
    public void storeJSONelement() {
        try {
            String variableName = Condition;
            String jsonpath = Data;
            
            if (variableName.matches("%.*%")) {
                // Get the current response body
                String currentResponseBody = gen.ResponseBody();
                
                if (currentResponseBody != null && !currentResponseBody.isEmpty()) {
                    String value = JsonPath.read(currentResponseBody, jsonpath).toString();
                    gen.addVar(variableName, value);
                    Report.updateTestLog(Action, "JSON element value [" + value + "] stored in variable " + variableName, Status.DONE);
                } else {
                    Report.updateTestLog(Action, "Response body is empty or null", Status.DEBUG);
                }
            } else {
                Report.updateTestLog(Action, "Variable format is not correct. Expected format: %variableName%", Status.DEBUG);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error Storing JSON element :" + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    /**
     * Add HTTP header to request
     */
    @Action(object = ObjectType.WEBSERVICE, desc = "Add Header ", input = InputType.YES)
    public void addHeader() {
        try {
            String headerData = Data;

            // Handle datasheet variable substitution: {sheetName:columnName}
            Pattern datasheetPattern = Pattern.compile("\\{([^:]+):([^}]+)\\}");
            Matcher datasheetMatcher = datasheetPattern.matcher(headerData);
            
            while (datasheetMatcher.find()) {
                String sheetName = datasheetMatcher.group(1);
                String columnName = datasheetMatcher.group(2);
                try {
                    String value = userData.getData(sheetName, columnName);
                    if (value != null) {
                        headerData = headerData.replace("{" + sheetName + ":" + columnName + "}", value);
                    }
                } catch (Exception e) {
                    Report.updateTestLog(Action, "Could not find data for {" + sheetName + ":" + columnName + "}", Status.DEBUG);
                }
            }

            // Handle runtime variable substitution: %variable%
            Pattern variablePattern = Pattern.compile("%\\w+%");
            Matcher variableMatcher = variablePattern.matcher(headerData);

            while (variableMatcher.find()) {
                String variable = variableMatcher.group();
                String value = gen.getVar(variable);
                if (value != null) {
                    headerData = headerData.replaceAll(Pattern.quote(variable), value);
                } else {
                    Report.updateTestLog(Action, "Variable " + variable + " not found", Status.DEBUG);
                }
            }

            // Store the header in shared map
            if (headers.containsKey(key)) {
                headers.get(key).add(headerData);
            } else {
                ArrayList<String> toBeAdded = new ArrayList<>();
                toBeAdded.add(headerData);
                headers.put(key, toBeAdded);
            }
            
            Report.updateTestLog(Action, "Header added [" + headerData + "]", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error adding Header :" + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    /**
     * POST REST request
     */
    @Action(object = ObjectType.WEBSERVICE, desc = "POST Rest Request ", input = InputType.YES, condition = InputType.OPTIONAL)
    public void postRestRequest() {
        try {
            gen.createHttpRequest(RequestMethod.POST);
            
            // Update local fields with response
            this.responseCode = gen.ResponseCode();
            this.responseBody = gen.ResponseBody();
            this.responseMessage = gen.ResponseMessage();
            
            /** 
             * Report here is optional. createHttpRequest already updates the report with request and response details. 
             * This is just an additional log entry if you want to explicitly log the successful execution of the POST request
             */
            // Report.updateTestLog(Action, "POST request executed successfully. Response code: " + responseCode, Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(Action,
                    "An unexpected error occurred while executing the POST request : " + "\n" + e.getMessage(),
                    Status.FAIL);
        }
    }

    /**
     * PUT REST request
     */
    @Action(object = ObjectType.WEBSERVICE, desc = "PUT Rest Request ", input = InputType.YES, condition = InputType.OPTIONAL)
    public void putRestRequest() {
        try {
            System.out.println("Executing PUT request with key: " + key);
            gen.createHttpRequest(RequestMethod.PUT);
            
            // Update local fields with response
            this.responseCode = gen.ResponseCode();
            this.responseBody = gen.ResponseBody();
            this.responseMessage = gen.ResponseMessage();
            
            /** 
             * Report here is optional. createHttpRequest already updates the report with request and response details. 
             * This is just an additional log entry if you want to explicitly log the successful execution of the PUT request
             */
            // Report.updateTestLog(Action, "PUT request executed successfully. Response code: " + responseCode, Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(Action,
                    "An unexpected error occurred while executing the PUT request : " + "\n" + e.getMessage(),
                    Status.FAIL);
        }
    }

    /**
     * Assert response code matches expected value
     */
    @Action(object = ObjectType.WEBSERVICE, desc = "Assert Response Code ", input = InputType.YES)
    public void assertResponseCode() {
        try {
            String currentResponseCode = gen.ResponseCode();
            
            if (currentResponseCode != null && currentResponseCode.equals(Data)) {
                Report.updateTestLog(Action, "Status code is : " + Data, Status.PASSNS);
            } else {
                Report.updateTestLog(Action, "Status code is : " + currentResponseCode + " but should be " + Data,
                        Status.FAILNS);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error in validating response code :" + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    /**
     * Assert response body contains specific text
     */
    @Action(object = ObjectType.WEBSERVICE, desc = "Assert Response Body contains ", input = InputType.YES)
    public void assertResponseBodyContains() {
        try {
            String currentResponseBody = gen.ResponseBody();
            
            if (currentResponseBody != null && currentResponseBody.contains(Data)) {
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
     * Assert JSON element equals expected value
     */
    @Action(object = ObjectType.WEBSERVICE, desc = "Assert JSON Element Equals ", input = InputType.YES, condition = InputType.YES)
    public void assertJSONelementEquals() {
        try {
            String currentResponseBody = gen.ResponseBody();
            String jsonpath = Condition;
            String value = JsonPath.read(currentResponseBody, jsonpath).toString();
            
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
     * Get stored headers for current context
     */
    public List<String> getHeaders() {
        return headers.getOrDefault(key, new ArrayList<>());
    }
}

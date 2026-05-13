package com.ing.ingenious.api.contract;

import com.ing.ingenious.api.contract.CommandPluginApi;
import com.ing.ingenious.api.types.RequestMethodType;
import java.util.ArrayList;
import java.util.Map;

public interface WebservicePluginApi extends CommandPluginApi  {

    /**
     * Gets the endpoint URL for HTTP/API operations.
     * @return the endpoint URL
     */
    String Endpoint();
    
    /**
     * Gets the HTTP response code.
     * @return the response code as a string
     */
    String ResponseCode();
    
    /**
     * Gets the HTTP response message.
     * @return the response message
     */
    String ResponseMessage();
    
    /**
     * Gets the HTTP response body.
     * @return the response body as a string
     */
    String ResponseBody();
    
    /**
     * Gets the HTTP connection object.
     * @return the connection object (needs to be cast to appropriate type)
     */
    Object Connection();
    
    /**
     * Gets the HTTP user agent string.
     * @return the HTTP user agent
     */
    String HttpAgent();
    
    /**
     * Creates and executes an HTTP request with the specified request method.
     * This method handles the complete lifecycle of an HTTP request including
     * setting headers, configuring the request method, executing the request,
     * and capturing response details.
     * 
     * @param requestMethod the HTTP request method enum value
     * @throws InterruptedException if the request is interrupted
     * @throws Exception if an error occurs during request execution
     * @see RequestMethodType
     */
    void createHttpRequest(RequestMethodType requestMethod) throws InterruptedException, Exception;

    /**
     * Gets the context key used to index all webservice-related maps.
     * The key is typically constructed as: scenario + testCase
     * This key is used to store and retrieve request/response data in the shared maps.
     * 
     * @return the context key for the current test execution
     */
    String getKey();
    
    /**
     * Gets direct access to the shared endpoints map.
     * This map stores endpoint URLs keyed by context (scenario + testCase).
     * 
     * @return the shared static map of endpoints
     */
    Map<String, String> getEndPointsMap();
    
    /**
     * Gets direct access to the shared headers map.
     * This map stores HTTP headers keyed by context (scenario + testCase).
     * Each context can have multiple headers stored as an ArrayList.
     * 
     * @return the shared static map of headers
     */
    Map<String, ArrayList<String>> getHeadersMap();
    
    /**
     * Gets direct access to the shared URL parameters map.
     * This map stores URL parameters keyed by context (scenario + testCase).
     * Each context can have multiple parameters stored as an ArrayList.
     * 
     * @return the shared static map of URL parameters
     */
    Map<String, ArrayList<String>> getUrlParamsMap();
    
    /**
     * Gets direct access to the shared response bodies map.
     * This map stores HTTP response bodies keyed by context (scenario + testCase).
     * 
     * @return the shared static map of response bodies
     */
    Map<String, String> getResponseBodiesMap();
    
    /**
     * Gets direct access to the shared response codes map.
     * This map stores HTTP response status codes keyed by context (scenario + testCase).
     * 
     * @return the shared static map of response codes
     */
    Map<String, String> getResponseCodesMap();
    
    /**
     * Gets direct access to the shared response messages map.
     * This map stores HTTP response messages keyed by context (scenario + testCase).
     * 
     * @return the shared static map of response messages
     */
    Map<String, String> getResponseMessagesMap();
    
    /**
     * Gets a driver property value from the current project settings.
     * This method respects the current API config context that was set by setEndPoint.
     * The API config context is maintained by the framework after setEndPoint is called.
     * 
     * @param propertyKey the property key to retrieve (e.g., "keyStorePath", "keyStorePassword")
     * @return the property value, or null if not found
     */
    String getDriverProperty(String propertyKey);

}

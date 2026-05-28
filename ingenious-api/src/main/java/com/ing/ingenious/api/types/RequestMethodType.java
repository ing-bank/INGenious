package com.ing.ingenious.api.types;

/**
 * Defines HTTP request methods for webservice/API testing operations.
 * <p>
 * This enum is used by both the framework and plugins to specify which HTTP method
 * to use when creating HTTP requests. Being in the API module ensures that the same
 * enum class is shared across all classloaders, avoiding ClassCastException issues
 * in plugin architectures with child-first classloading strategies.
 * </p>
 * 
 * @see com.ing.ingenious.api.contract.WebservicePluginApi#createHttpRequest(RequestMethodType)
 */
public enum RequestMethodType {
    /** HTTP POST method - typically used to create resources */
    POST,
    
    /** HTTP PUT method - typically used to update/replace resources */
    PUT,
    
    /** HTTP PATCH method - typically used to partially update resources */
    PATCH,
    
    /** HTTP GET method - used to retrieve resources */
    GET,
    
    /** HTTP DELETE method - used to delete resources (without payload) */
    DELETE,
    
    /** HTTP DELETE method with payload - used to delete resources with a request body */
    DELETEWITHPAYLOAD
}

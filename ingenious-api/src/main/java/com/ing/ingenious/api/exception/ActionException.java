package com.ing.ingenious.api.exception;

/**
 * Exception thrown when an action fails during test execution.
 * This is a framework exception with zero vendor dependencies, making it safe
 * for use in the API module and by plugin developers.
 * 
 * <p>ActionException is typically thrown when:
 * <ul>
 *   <li>Browser commands encounter errors (clicks, navigation, etc.)</li>
 *   <li>Web service calls fail</li>
 *   <li>Element operations cannot be completed</li>
 *   <li>Any test action encounters an unexpected exception</li>
 * </ul>
 * 
 * @since 3.0
 */
public class ActionException extends RuntimeException {
    
    public String ErrorName;
    public String ErrorDescription;
    
    /**
     * Creates a new ActionException wrapping an underlying exception.
     * The error description will be taken from the exception's message.
     * 
     * @param ex the underlying exception that caused this action to fail
     */
    public ActionException(Throwable ex) {
        super(ex);
        this.ErrorDescription = ex.getMessage();
    }
}

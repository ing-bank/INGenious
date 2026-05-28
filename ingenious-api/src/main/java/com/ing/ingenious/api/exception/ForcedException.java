package com.ing.ingenious.api.exception;

/**
 * Exception thrown when a test step is forced to fail with a custom error message.
 * This is a framework exception with zero vendor dependencies, making it safe
 * for use in the API module and by plugin developers.
 * 
 * @since 3.0
 */
public class ForcedException extends RuntimeException {

    public String ErrorName;
    public String ErrorDescription;

    /**
     * Creates a new ForcedException with custom error name and description.
     * 
     * @param errorName the name/title of the error
     * @param errorDescription detailed description of the error
     */
    public ForcedException(String errorName, String errorDescription) {
        this.ErrorName = errorName;
        this.ErrorDescription = errorDescription;
    }

    /**
     * Creates a new ForcedException with custom error name and an underlying exception.
     * 
     * @param errorName the name/title of the error  
     * @param ex the underlying exception that caused this forced exception
     */
    public ForcedException(String errorName, Throwable ex) {
        super(ex);
        this.ErrorName = errorName;
        this.ErrorDescription = ex.getMessage();
    }

    /**
     * Creates a new ForcedException with just an error description.
     * The error name defaults to "ForcedException".
     * 
     * @param errorDescription detailed description of the error
     */
    public ForcedException(String errorDescription) {
        this.ErrorName = "ForcedException";
        this.ErrorDescription = errorDescription;
    }

    @Override
    public String getMessage() {
        return ErrorDescription;
    }
}

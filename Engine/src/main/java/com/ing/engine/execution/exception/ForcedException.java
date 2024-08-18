
package com.ing.engine.execution.exception;

public class ForcedException extends RuntimeException {

    public String ErrorName;
    public String ErrorDescription;

    public ForcedException(String errorName, String errorDescription) {
        this.ErrorName = errorName;
        this.ErrorDescription = errorDescription;

    }

    public ForcedException(String errorName, Throwable ex) {
        super(ex);
        this.ErrorName = errorName;
        this.ErrorDescription = ex.getMessage();
    }

    public ForcedException(String errorDescription) {
        this.ErrorName = "ForcedException";
        this.ErrorDescription = errorDescription;
    }

    @Override
    public String getMessage() {
        return ErrorDescription;
    }

}

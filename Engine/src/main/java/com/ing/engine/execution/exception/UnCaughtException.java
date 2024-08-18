
package com.ing.engine.execution.exception;

public class UnCaughtException extends RuntimeException {

    public String errorName = "UnCaughtException";

    public UnCaughtException(String errorDescription) {
        super(errorDescription);
    }

    public UnCaughtException(Throwable ex) {
        super(ex);
    }

    public UnCaughtException(String errorName, String errorDescription) {
        super(errorDescription);
        this.errorName = errorName;
    }

    public String getName() {
        return errorName;
    }
}

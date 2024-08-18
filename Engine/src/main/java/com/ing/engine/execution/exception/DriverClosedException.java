
package com.ing.engine.execution.exception;

public class DriverClosedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DriverClosedException(String browserName) {
        super("Driver " + browserName + " Closed / Could not be reached");
    }

}

package com.ing.engine.reporting.sync.testmanager;

/** Raised for any non-recoverable Test Manager API failure. */
public class TestManagerApiException extends Exception {

    private static final long serialVersionUID = 1L;

    public TestManagerApiException(String message) {
        super(message);
    }

    public TestManagerApiException(String message, Throwable cause) {
        super(message, cause);
    }
}

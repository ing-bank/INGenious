package com.ing.datalib.exception;

/**
 * Exception thrown when a test case conversion operation fails.
 * This includes operations such as moving test cases between Test Plan and Reusable Components.
 */
public class TestCaseConversionException extends Exception {

    /**
     * Constructs a new TestCaseConversionException with the specified detail message.
     * @param message the detail message explaining the reason for the exception
     */
    public TestCaseConversionException(String message) {
        super(message);
    }

    /**
     * Constructs a new TestCaseConversionException with the specified detail message and cause.
     * @param message the detail message explaining the reason for the exception
     * @param cause the cause of the exception
     */
    public TestCaseConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}


package com.ing.engine.execution.exception;

public class TestFailedException extends RuntimeException {

    private String testName;

    public TestFailedException(String scenario, String testcase, Throwable ex) {
        super(ex);
        this.testName = String.format("//%s/%s", scenario, testcase);
    }

    public TestFailedException(String errorDescription, Throwable ex) {
        super(errorDescription, ex);
    }

    @Override
    public String getMessage() {
        return String.format("Error in testcase [%s]", testName);
    }

}

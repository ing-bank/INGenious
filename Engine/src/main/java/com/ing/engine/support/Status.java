
package com.ing.engine.support;

/**
 * Enumeration to represent the status of the current test step
 *
 * 
 */
public enum Status {

    /**
     * Indicates that the outcome of a verification was not successful
     */
    FAIL,
    /**
     * Indicates a warning message
     */
    WARNING,
    /**
     * Indicates that the outcome of a verification was successful
     */
    PASS,
    /**
     * Indicates a step that is logged into the results for informational
     * purposes, along with an attached screen shot for reference
     */
    SCREENSHOT,
    /**
     * Indicates a message that is logged into the results for informational
     * purposes
     */
    DONE,
    /**
     * Indicates a debug-level message, typically used by automation developers
     * to troubleshoot any errors that may occur
     */
    DEBUG,
    /**
     * Pass without Screenshot
     */
    PASSNS,
    /**
     * Fail without screenshot
     */
    FAILNS,
    /**
     * Indicates submission of API Request and generation of Response
     */
    COMPLETE;

    @Override
    public String toString() {
        switch (this) {
            case DONE:
                return "DONE";
            case PASS:
            case PASSNS:
                return "PASS";
            case FAIL:
            case FAILNS:
                return "FAIL";
            case SCREENSHOT:
                return "SCREENSHOT";
            case DEBUG:
                return "DEBUG";
            case WARNING:
                return "WARNING";
            case COMPLETE:
                return "COMPLETE";
        }
        return null;
    }

    public static Status getValue(Boolean value) {
        return value ? PASS : FAIL;
    }

}

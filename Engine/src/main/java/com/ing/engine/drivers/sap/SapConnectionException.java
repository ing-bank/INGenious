package com.ing.engine.drivers.sap;

/**
 * Raised when a SAP connection cannot be established: scripting disabled, the
 * engine never came up, or an unknown / ambiguous alias. Carries a
 * user-actionable message.
 */
public class SapConnectionException extends RuntimeException {

    public SapConnectionException(String message) {
        super(message);
    }

    public SapConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}

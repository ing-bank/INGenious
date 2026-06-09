package com.ing.datalib.api.importer;

/**
 * Raised when a collection file cannot be parsed at all.
 */
public class ImportException extends Exception {
    private static final long serialVersionUID = 1L;

    public ImportException(String message) {
        super(message);
    }

    public ImportException(String message, Throwable cause) {
        super(message, cause);
    }
}

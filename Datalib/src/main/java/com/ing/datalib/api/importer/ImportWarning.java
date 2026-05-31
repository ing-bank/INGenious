package com.ing.datalib.api.importer;

import java.io.Serializable;

/**
 * A non-fatal issue encountered while parsing or mapping an imported collection.
 */
public class ImportWarning implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Severity { INFO, WARN, ERROR }

    private final Severity severity;
    private final String location;
    private final String message;

    public ImportWarning(Severity severity, String location, String message) {
        this.severity = severity;
        this.location = location;
        this.message = message;
    }

    public static ImportWarning warn(String location, String message) {
        return new ImportWarning(Severity.WARN, location, message);
    }

    public static ImportWarning info(String location, String message) {
        return new ImportWarning(Severity.INFO, location, message);
    }

    public static ImportWarning error(String location, String message) {
        return new ImportWarning(Severity.ERROR, location, message);
    }

    public Severity getSeverity() { return severity; }
    public String getLocation() { return location; }
    public String getMessage() { return message; }

    @Override
    public String toString() {
        return "[" + severity + "] " + (location != null ? location + ": " : "") + message;
    }
}

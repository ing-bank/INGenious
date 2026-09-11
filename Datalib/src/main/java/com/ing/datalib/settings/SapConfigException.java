package com.ing.datalib.settings;

/** Thrown when a SAP connection alias cannot be resolved (unknown / ambiguous default). */
public class SapConfigException extends RuntimeException {

    public SapConfigException(String message) {
        super(message);
    }
}

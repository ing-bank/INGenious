package com.ing.datalib.api.importer;

import java.io.Serializable;

/**
 * A collection / environment variable in the normalized import model.
 */
public class NormalizedVariable implements Serializable {
    private static final long serialVersionUID = 1L;

    private String key;
    private String value;
    private boolean secret;

    public NormalizedVariable() {}

    public NormalizedVariable(String key, String value, boolean secret) {
        this.key = key;
        this.value = value;
        this.secret = secret;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isSecret() {
        return secret;
    }

    public void setSecret(boolean secret) {
        this.secret = secret;
    }
}

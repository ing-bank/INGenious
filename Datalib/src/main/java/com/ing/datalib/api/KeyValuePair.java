package com.ing.datalib.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a key-value pair for headers, query params, form data, etc.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeyValuePair implements Serializable {

    private static final long serialVersionUID = 1L;

    private String key;
    private String value;
    private String description;
    private boolean enabled;

    public KeyValuePair() {
        this.enabled = true;
    }

    public KeyValuePair(String key, String value) {
        this.key = key;
        this.value = value;
        this.enabled = true;
    }

    public KeyValuePair(String key, String value, boolean enabled) {
        this.key = key;
        this.value = value;
        this.enabled = enabled;
    }

    public KeyValuePair(String key, String value, String description, boolean enabled) {
        this.key = key;
        this.value = value;
        this.description = description;
        this.enabled = enabled;
    }

    // Getters and Setters
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KeyValuePair that = (KeyValuePair) o;
        return enabled == that.enabled &&
                Objects.equals(key, that.key) &&
                Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value, enabled);
    }

    @Override
    public String toString() {
        return key + ": " + value + (enabled ? "" : " (disabled)");
    }

    /**
     * Creates a copy of this key-value pair.
     */
    public KeyValuePair copy() {
        return new KeyValuePair(key, value, description, enabled);
    }
}

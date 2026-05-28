package com.ing.datalib.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents an environment with variables for API testing.
 * Variables can be referenced in requests using {{variableName}} syntax.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class APIEnvironment implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");

    private String id;
    private String name;
    private String description;
    private Map<String, String> variables;
    private Map<String, String> secrets; // Sensitive values (not exported)
    private boolean active;
    private long createdAt;
    private long updatedAt;

    public APIEnvironment() {
        this.id = UUID.randomUUID().toString();
        this.variables = new HashMap<>();
        this.secrets = new HashMap<>();
        this.active = false;
        this.createdAt = Instant.now().toEpochMilli();
        this.updatedAt = this.createdAt;
    }

    public APIEnvironment(String name) {
        this();
        this.name = name;
    }

    // Static factory methods
    public static APIEnvironment create(String name) {
        return new APIEnvironment(name);
    }

    public static APIEnvironment development() {
        APIEnvironment env = new APIEnvironment("Development");
        env.setVariable("baseUrl", "http://localhost:8080");
        env.setVariable("apiVersion", "v1");
        return env;
    }

    public static APIEnvironment staging() {
        APIEnvironment env = new APIEnvironment("Staging");
        env.setVariable("baseUrl", "https://staging-api.example.com");
        env.setVariable("apiVersion", "v1");
        return env;
    }

    public static APIEnvironment production() {
        APIEnvironment env = new APIEnvironment("Production");
        env.setVariable("baseUrl", "https://api.example.com");
        env.setVariable("apiVersion", "v1");
        return env;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.updatedAt = Instant.now().toEpochMilli();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = Instant.now().toEpochMilli();
    }

    public Map<String, String> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, String> variables) {
        this.variables = variables;
        this.updatedAt = Instant.now().toEpochMilli();
    }

    public Map<String, String> getSecrets() {
        return secrets;
    }

    public void setSecrets(Map<String, String> secrets) {
        this.secrets = secrets;
        this.updatedAt = Instant.now().toEpochMilli();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        this.updatedAt = Instant.now().toEpochMilli();
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Variable management
    public void setVariable(String key, String value) {
        if (variables == null) {
            variables = new HashMap<>();
        }
        variables.put(key, value);
        this.updatedAt = Instant.now().toEpochMilli();
    }

    public String getVariable(String key) {
        // Check variables first
        if (variables != null && variables.containsKey(key)) {
            return variables.get(key);
        }
        // Then check secrets
        if (secrets != null && secrets.containsKey(key)) {
            return secrets.get(key);
        }
        return null;
    }

    public void removeVariable(String key) {
        if (variables != null) {
            variables.remove(key);
            this.updatedAt = Instant.now().toEpochMilli();
        }
    }

    public void setSecret(String key, String value) {
        if (secrets == null) {
            secrets = new HashMap<>();
        }
        secrets.put(key, value);
        this.updatedAt = Instant.now().toEpochMilli();
    }

    public void removeSecret(String key) {
        if (secrets != null) {
            secrets.remove(key);
            this.updatedAt = Instant.now().toEpochMilli();
        }
    }

    /**
     * Resolves all {{variableName}} placeholders in the given string.
     * 
     * @param input The input string with placeholders
     * @return The resolved string with placeholders replaced by variable values
     */
    public String resolve(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        StringBuffer result = new StringBuffer();
        Matcher matcher = VARIABLE_PATTERN.matcher(input);
        
        while (matcher.find()) {
            String variableName = matcher.group(1).trim();
            String value = getVariable(variableName);
            if (value != null) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(value));
            } else {
                // Keep original placeholder if variable not found
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(result);
        
        return result.toString();
    }

    /**
     * Checks if the input contains any variable placeholders.
     */
    public static boolean hasVariables(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        return VARIABLE_PATTERN.matcher(input).find();
    }

    /**
     * Extracts all variable names from the input string.
     */
    public static java.util.List<String> extractVariableNames(String input) {
        java.util.List<String> names = new java.util.ArrayList<>();
        if (input == null || input.isEmpty()) {
            return names;
        }
        Matcher matcher = VARIABLE_PATTERN.matcher(input);
        while (matcher.find()) {
            names.add(matcher.group(1).trim());
        }
        return names;
    }

    /**
     * Gets all combined variables and secrets (for resolution).
     */
    public Map<String, String> getAllVariables() {
        Map<String, String> all = new HashMap<>();
        if (variables != null) {
            all.putAll(variables);
        }
        if (secrets != null) {
            all.putAll(secrets);
        }
        return all;
    }

    /**
     * Creates a copy of this environment.
     */
    public APIEnvironment copy() {
        APIEnvironment copy = new APIEnvironment();
        copy.setId(UUID.randomUUID().toString());
        copy.setName(this.name + " (Copy)");
        copy.setDescription(this.description);
        copy.setActive(false);
        if (this.variables != null) {
            copy.setVariables(new HashMap<>(this.variables));
        }
        if (this.secrets != null) {
            copy.setSecrets(new HashMap<>(this.secrets));
        }
        return copy;
    }

    /**
     * Creates an export-safe copy (without secrets).
     */
    public APIEnvironment copyForExport() {
        APIEnvironment copy = new APIEnvironment();
        copy.setId(this.id);
        copy.setName(this.name);
        copy.setDescription(this.description);
        copy.setActive(this.active);
        if (this.variables != null) {
            copy.setVariables(new HashMap<>(this.variables));
        }
        // Secrets are NOT copied for export
        return copy;
    }

    @Override
    public String toString() {
        return name != null ? name : "Environment";
    }
}

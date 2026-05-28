package com.ing.datalib.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;

/**
 * Represents an assertion/validation rule for API responses.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class APIAssertion implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Type of assertion.
     */
    public enum AssertionType {
        STATUS_CODE,
        JSON_PATH,
        XPATH,
        HEADER,
        RESPONSE_TIME,
        BODY_CONTAINS,
        BODY_EQUALS,
        BODY_MATCHES_REGEX,
        CONTENT_TYPE
    }

    /**
     * Comparison operator.
     */
    public enum Operator {
        EQUALS,
        NOT_EQUALS,
        CONTAINS,
        NOT_CONTAINS,
        STARTS_WITH,
        ENDS_WITH,
        EXISTS,
        NOT_EXISTS,
        GREATER_THAN,
        LESS_THAN,
        GREATER_THAN_OR_EQUALS,
        LESS_THAN_OR_EQUALS,
        MATCHES_REGEX,
        IS_NULL,
        IS_NOT_NULL,
        IS_ARRAY,
        IS_OBJECT,
        IS_STRING,
        IS_NUMBER,
        IS_BOOLEAN
    }

    private String id;
    private String name;
    private AssertionType type;
    private String target;  // e.g., "$.user.id" for JSON_PATH, "Content-Type" for HEADER
    private Operator operator;
    private String expectedValue;
    private boolean enabled;
    private String description;

    public APIAssertion() {
        this.id = java.util.UUID.randomUUID().toString();
        this.type = AssertionType.STATUS_CODE;
        this.operator = Operator.EQUALS;
        this.enabled = true;
    }

    public APIAssertion(AssertionType type, String target, Operator operator, String expectedValue) {
        this();
        this.type = type;
        this.target = target;
        this.operator = operator;
        this.expectedValue = expectedValue;
    }

    // Static factory methods for common assertions
    public static APIAssertion statusCode(int code) {
        APIAssertion assertion = new APIAssertion();
        assertion.setType(AssertionType.STATUS_CODE);
        assertion.setOperator(Operator.EQUALS);
        assertion.setExpectedValue(String.valueOf(code));
        assertion.setName("Status code is " + code);
        return assertion;
    }

    public static APIAssertion statusCodeSuccess() {
        APIAssertion assertion = new APIAssertion();
        assertion.setType(AssertionType.STATUS_CODE);
        assertion.setOperator(Operator.LESS_THAN);
        assertion.setExpectedValue("300");
        assertion.setName("Status code is success (2xx)");
        return assertion;
    }

    public static APIAssertion jsonPath(String path, Operator operator, String expectedValue) {
        APIAssertion assertion = new APIAssertion();
        assertion.setType(AssertionType.JSON_PATH);
        assertion.setTarget(path);
        assertion.setOperator(operator);
        assertion.setExpectedValue(expectedValue);
        assertion.setName("JSON path " + path + " " + operator + " " + expectedValue);
        return assertion;
    }

    public static APIAssertion jsonPathExists(String path) {
        APIAssertion assertion = new APIAssertion();
        assertion.setType(AssertionType.JSON_PATH);
        assertion.setTarget(path);
        assertion.setOperator(Operator.EXISTS);
        assertion.setName("JSON path " + path + " exists");
        return assertion;
    }

    public static APIAssertion header(String headerName, Operator operator, String expectedValue) {
        APIAssertion assertion = new APIAssertion();
        assertion.setType(AssertionType.HEADER);
        assertion.setTarget(headerName);
        assertion.setOperator(operator);
        assertion.setExpectedValue(expectedValue);
        assertion.setName("Header " + headerName + " " + operator + " " + expectedValue);
        return assertion;
    }

    public static APIAssertion responseTimeLessThan(long milliseconds) {
        APIAssertion assertion = new APIAssertion();
        assertion.setType(AssertionType.RESPONSE_TIME);
        assertion.setOperator(Operator.LESS_THAN);
        assertion.setExpectedValue(String.valueOf(milliseconds));
        assertion.setName("Response time < " + milliseconds + "ms");
        return assertion;
    }

    public static APIAssertion bodyContains(String substring) {
        APIAssertion assertion = new APIAssertion();
        assertion.setType(AssertionType.BODY_CONTAINS);
        assertion.setOperator(Operator.CONTAINS);
        assertion.setExpectedValue(substring);
        assertion.setName("Body contains '" + substring + "'");
        return assertion;
    }

    public static APIAssertion contentType(String contentType) {
        APIAssertion assertion = new APIAssertion();
        assertion.setType(AssertionType.CONTENT_TYPE);
        assertion.setOperator(Operator.CONTAINS);
        assertion.setExpectedValue(contentType);
        assertion.setName("Content-Type is " + contentType);
        return assertion;
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
    }

    public AssertionType getType() {
        return type;
    }

    public void setType(AssertionType type) {
        this.type = type;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public Operator getOperator() {
        return operator;
    }

    public void setOperator(Operator operator) {
        this.operator = operator;
    }

    public String getExpectedValue() {
        return expectedValue;
    }

    public void setExpectedValue(String expectedValue) {
        this.expectedValue = expectedValue;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Creates a copy of this assertion.
     */
    public APIAssertion copy() {
        APIAssertion copy = new APIAssertion();
        copy.setId(java.util.UUID.randomUUID().toString());
        copy.setName(this.name);
        copy.setType(this.type);
        copy.setTarget(this.target);
        copy.setOperator(this.operator);
        copy.setExpectedValue(this.expectedValue);
        copy.setEnabled(this.enabled);
        copy.setDescription(this.description);
        return copy;
    }

    @Override
    public String toString() {
        return (name != null && !name.isEmpty()) ? name : type + " " + operator + " " + expectedValue;
    }
}

package com.ing.datalib.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;

/**
 * Represents the body content of an API request.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RequestBody implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Type of body content.
     */
    public enum BodyType {
        NONE,
        RAW,
        FORM_DATA,
        URL_ENCODED,
        BINARY,
        GRAPHQL
    }

    /**
     * Raw body content format.
     */
    public enum RawFormat {
        JSON,
        XML,
        TEXT,
        HTML,
        JAVASCRIPT
    }

    private BodyType bodyType;
    private RawFormat rawFormat;
    private String rawContent;
    private java.util.List<KeyValuePair> formData;
    private java.util.List<KeyValuePair> urlEncodedData;
    private String binaryFilePath;
    private String graphqlQuery;
    private String graphqlVariables;

    public RequestBody() {
        this.bodyType = BodyType.NONE;
        this.rawFormat = RawFormat.JSON;
        this.formData = new java.util.ArrayList<>();
        this.urlEncodedData = new java.util.ArrayList<>();
    }

    public static RequestBody none() {
        return new RequestBody();
    }

    public static RequestBody json(String content) {
        RequestBody body = new RequestBody();
        body.setBodyType(BodyType.RAW);
        body.setRawFormat(RawFormat.JSON);
        body.setRawContent(content);
        return body;
    }

    public static RequestBody xml(String content) {
        RequestBody body = new RequestBody();
        body.setBodyType(BodyType.RAW);
        body.setRawFormat(RawFormat.XML);
        body.setRawContent(content);
        return body;
    }

    public static RequestBody text(String content) {
        RequestBody body = new RequestBody();
        body.setBodyType(BodyType.RAW);
        body.setRawFormat(RawFormat.TEXT);
        body.setRawContent(content);
        return body;
    }

    // Getters and Setters
    public BodyType getBodyType() {
        return bodyType;
    }

    public void setBodyType(BodyType bodyType) {
        this.bodyType = bodyType;
    }

    public RawFormat getRawFormat() {
        return rawFormat;
    }

    public void setRawFormat(RawFormat rawFormat) {
        this.rawFormat = rawFormat;
    }

    public String getRawContent() {
        return rawContent;
    }

    public void setRawContent(String rawContent) {
        this.rawContent = rawContent;
    }

    public java.util.List<KeyValuePair> getFormData() {
        return formData;
    }

    public void setFormData(java.util.List<KeyValuePair> formData) {
        this.formData = formData;
    }

    public java.util.List<KeyValuePair> getUrlEncodedData() {
        return urlEncodedData;
    }

    public void setUrlEncodedData(java.util.List<KeyValuePair> urlEncodedData) {
        this.urlEncodedData = urlEncodedData;
    }

    public String getBinaryFilePath() {
        return binaryFilePath;
    }

    public void setBinaryFilePath(String binaryFilePath) {
        this.binaryFilePath = binaryFilePath;
    }

    public String getGraphqlQuery() {
        return graphqlQuery;
    }

    public void setGraphqlQuery(String graphqlQuery) {
        this.graphqlQuery = graphqlQuery;
    }

    public String getGraphqlVariables() {
        return graphqlVariables;
    }

    public void setGraphqlVariables(String graphqlVariables) {
        this.graphqlVariables = graphqlVariables;
    }

    /**
     * Returns the content type header value for this body.
     */
    public String getContentType() {
        if (bodyType == null || bodyType == BodyType.NONE) {
            return null;
        }
        switch (bodyType) {
            case RAW:
                if (rawFormat == null) return "text/plain";
                switch (rawFormat) {
                    case JSON: return "application/json";
                    case XML: return "application/xml";
                    case HTML: return "text/html";
                    case JAVASCRIPT: return "application/javascript";
                    default: return "text/plain";
                }
            case FORM_DATA:
                return "multipart/form-data";
            case URL_ENCODED:
                return "application/x-www-form-urlencoded";
            case GRAPHQL:
                return "application/json";
            default:
                return null;
        }
    }

    /**
     * Returns true if this body has content.
     */
    public boolean hasContent() {
        if (bodyType == null || bodyType == BodyType.NONE) {
            return false;
        }
        switch (bodyType) {
            case RAW:
                return rawContent != null && !rawContent.isEmpty();
            case FORM_DATA:
                return formData != null && !formData.isEmpty();
            case URL_ENCODED:
                return urlEncodedData != null && !urlEncodedData.isEmpty();
            case BINARY:
                return binaryFilePath != null && !binaryFilePath.isEmpty();
            case GRAPHQL:
                return graphqlQuery != null && !graphqlQuery.isEmpty();
            default:
                return false;
        }
    }

    /**
     * Creates a copy of this request body.
     */
    public RequestBody copy() {
        RequestBody copy = new RequestBody();
        copy.setBodyType(this.bodyType);
        copy.setRawFormat(this.rawFormat);
        copy.setRawContent(this.rawContent);
        copy.setBinaryFilePath(this.binaryFilePath);
        copy.setGraphqlQuery(this.graphqlQuery);
        copy.setGraphqlVariables(this.graphqlVariables);
        if (this.formData != null) {
            copy.setFormData(new java.util.ArrayList<>());
            for (KeyValuePair kvp : this.formData) {
                copy.getFormData().add(kvp.copy());
            }
        }
        if (this.urlEncodedData != null) {
            copy.setUrlEncodedData(new java.util.ArrayList<>());
            for (KeyValuePair kvp : this.urlEncodedData) {
                copy.getUrlEncodedData().add(kvp.copy());
            }
        }
        return copy;
    }
}

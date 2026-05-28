package com.ing.datalib.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents an API response.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class APIResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private int statusCode;
    private String statusText;
    private String body;
    private Map<String, List<String>> headers;
    private long responseTimeMs;
    private long responseSizeBytes;
    private long timestamp;
    private String requestId;
    private String contentType;
    private List<AssertionResult> assertionResults;
    private String errorMessage;
    private boolean isError;

    public APIResponse() {
        this.headers = new HashMap<>();
        this.assertionResults = new ArrayList<>();
        this.timestamp = Instant.now().toEpochMilli();
    }

    public APIResponse(int statusCode, String body, Map<String, List<String>> headers, long responseTimeMs) {
        this();
        this.statusCode = statusCode;
        this.body = body;
        this.headers = headers != null ? headers : new HashMap<>();
        this.responseTimeMs = responseTimeMs;
        this.statusText = getStatusTextForCode(statusCode);
        this.responseSizeBytes = body != null ? body.getBytes().length : 0;
        
        // Extract content type from headers
        if (this.headers.containsKey("content-type")) {
            List<String> ct = this.headers.get("content-type");
            if (ct != null && !ct.isEmpty()) {
                this.contentType = ct.get(0);
            }
        } else if (this.headers.containsKey("Content-Type")) {
            List<String> ct = this.headers.get("Content-Type");
            if (ct != null && !ct.isEmpty()) {
                this.contentType = ct.get(0);
            }
        }
    }

    public static APIResponse error(String errorMessage) {
        APIResponse response = new APIResponse();
        response.setError(true);
        response.setErrorMessage(errorMessage);
        response.setStatusCode(0);
        return response;
    }

    // Getters and Setters
    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
        this.statusText = getStatusTextForCode(statusCode);
    }

    public String getStatusText() {
        return statusText;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
        this.responseSizeBytes = body != null ? body.getBytes().length : 0;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, List<String>> headers) {
        this.headers = headers;
    }

    public long getResponseTimeMs() {
        return responseTimeMs;
    }

    public void setResponseTimeMs(long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }

    public long getResponseSizeBytes() {
        return responseSizeBytes;
    }

    public void setResponseSizeBytes(long responseSizeBytes) {
        this.responseSizeBytes = responseSizeBytes;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public List<AssertionResult> getAssertionResults() {
        return assertionResults;
    }

    public void setAssertionResults(List<AssertionResult> assertionResults) {
        this.assertionResults = assertionResults;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public boolean isError() {
        return isError;
    }

    public void setError(boolean error) {
        isError = error;
    }

    /**
     * Returns true if status code is 2xx.
     */
    public boolean isSuccess() {
        return statusCode >= 200 && statusCode < 300;
    }

    /**
     * Returns true if status code indicates a client error (4xx).
     */
    public boolean isClientError() {
        return statusCode >= 400 && statusCode < 500;
    }

    /**
     * Returns true if status code indicates a server error (5xx).
     */
    public boolean isServerError() {
        return statusCode >= 500 && statusCode < 600;
    }

    /**
     * Returns true if body appears to be JSON.
     */
    public boolean isJsonBody() {
        if (contentType != null && contentType.contains("application/json")) {
            return true;
        }
        if (body != null) {
            String trimmed = body.trim();
            return (trimmed.startsWith("{") && trimmed.endsWith("}")) ||
                   (trimmed.startsWith("[") && trimmed.endsWith("]"));
        }
        return false;
    }

    /**
     * Returns true if body appears to be XML.
     */
    public boolean isXmlBody() {
        if (contentType != null && (contentType.contains("application/xml") || contentType.contains("text/xml"))) {
            return true;
        }
        if (body != null) {
            String trimmed = body.trim();
            return trimmed.startsWith("<?xml") || trimmed.startsWith("<");
        }
        return false;
    }

    /**
     * Returns true if body appears to be HTML.
     */
    public boolean isHtmlBody() {
        if (contentType != null && contentType.contains("text/html")) {
            return true;
        }
        if (body != null) {
            String lower = body.toLowerCase().trim();
            return lower.startsWith("<!doctype html") || lower.startsWith("<html");
        }
        return false;
    }

    /**
     * Gets a specific header value (first value if multiple).
     */
    public String getHeader(String name) {
        if (headers == null) return null;
        
        // Try exact match
        List<String> values = headers.get(name);
        if (values != null && !values.isEmpty()) {
            return values.get(0);
        }
        
        // Try case-insensitive match
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                List<String> v = entry.getValue();
                if (v != null && !v.isEmpty()) {
                    return v.get(0);
                }
            }
        }
        return null;
    }

    /**
     * Returns a human-readable size string.
     */
    public String getFormattedSize() {
        if (responseSizeBytes < 1024) {
            return responseSizeBytes + " B";
        } else if (responseSizeBytes < 1024 * 1024) {
            return String.format("%.2f KB", responseSizeBytes / 1024.0);
        } else {
            return String.format("%.2f MB", responseSizeBytes / (1024.0 * 1024.0));
        }
    }

    /**
     * Returns a human-readable time string.
     */
    public String getFormattedTime() {
        if (responseTimeMs < 1000) {
            return responseTimeMs + " ms";
        } else {
            return String.format("%.2f s", responseTimeMs / 1000.0);
        }
    }

    /**
     * Returns the count of passed assertions.
     */
    public int getPassedAssertionsCount() {
        int count = 0;
        if (assertionResults != null) {
            for (AssertionResult r : assertionResults) {
                if (r.isPassed()) count++;
            }
        }
        return count;
    }

    /**
     * Returns the count of failed assertions.
     */
    public int getFailedAssertionsCount() {
        int count = 0;
        if (assertionResults != null) {
            for (AssertionResult r : assertionResults) {
                if (!r.isPassed()) count++;
            }
        }
        return count;
    }

    private static String getStatusTextForCode(int code) {
        switch (code) {
            case 100: return "Continue";
            case 101: return "Switching Protocols";
            case 200: return "OK";
            case 201: return "Created";
            case 202: return "Accepted";
            case 204: return "No Content";
            case 301: return "Moved Permanently";
            case 302: return "Found";
            case 304: return "Not Modified";
            case 307: return "Temporary Redirect";
            case 308: return "Permanent Redirect";
            case 400: return "Bad Request";
            case 401: return "Unauthorized";
            case 403: return "Forbidden";
            case 404: return "Not Found";
            case 405: return "Method Not Allowed";
            case 408: return "Request Timeout";
            case 409: return "Conflict";
            case 410: return "Gone";
            case 415: return "Unsupported Media Type";
            case 422: return "Unprocessable Entity";
            case 429: return "Too Many Requests";
            case 500: return "Internal Server Error";
            case 501: return "Not Implemented";
            case 502: return "Bad Gateway";
            case 503: return "Service Unavailable";
            case 504: return "Gateway Timeout";
            default: return "";
        }
    }

    /**
     * Represents the result of a single assertion.
     */
    public static class AssertionResult implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private String assertionId;
        private String assertionName;
        private boolean passed;
        private String actualValue;
        private String expectedValue;
        private String message;

        public AssertionResult() {}

        public AssertionResult(String assertionId, String assertionName, boolean passed, String message) {
            this.assertionId = assertionId;
            this.assertionName = assertionName;
            this.passed = passed;
            this.message = message;
        }

        public String getAssertionId() {
            return assertionId;
        }

        public void setAssertionId(String assertionId) {
            this.assertionId = assertionId;
        }

        public String getAssertionName() {
            return assertionName;
        }

        public void setAssertionName(String assertionName) {
            this.assertionName = assertionName;
        }

        public boolean isPassed() {
            return passed;
        }

        public void setPassed(boolean passed) {
            this.passed = passed;
        }

        public String getActualValue() {
            return actualValue;
        }

        public void setActualValue(String actualValue) {
            this.actualValue = actualValue;
        }

        public String getExpectedValue() {
            return expectedValue;
        }

        public void setExpectedValue(String expectedValue) {
            this.expectedValue = expectedValue;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}

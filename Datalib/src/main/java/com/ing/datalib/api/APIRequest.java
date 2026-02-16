package com.ing.datalib.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a single API request definition.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class APIRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * HTTP methods supported.
     */
    public enum HttpMethod {
        GET,
        POST,
        PUT,
        PATCH,
        DELETE,
        HEAD,
        OPTIONS
    }

    private String id;
    private String name;
    private String description;
    private HttpMethod method;
    private String url;
    private List<KeyValuePair> queryParams;
    private List<KeyValuePair> headers;
    private List<KeyValuePair> pathVariables;
    private RequestBody body;
    private AuthConfig auth;
    private String preRequestScript;
    private String testScript;
    private List<APIAssertion> assertions;
    private long createdAt;
    private long updatedAt;
    private int timeout; // in milliseconds
    private boolean followRedirects;

    public APIRequest() {
        this.id = UUID.randomUUID().toString();
        this.method = HttpMethod.GET;
        this.queryParams = new ArrayList<>();
        this.headers = new ArrayList<>();
        this.pathVariables = new ArrayList<>();
        this.body = new RequestBody();
        this.auth = new AuthConfig();
        this.assertions = new ArrayList<>();
        this.createdAt = Instant.now().toEpochMilli();
        this.updatedAt = this.createdAt;
        this.timeout = 30000; // 30 seconds default
        this.followRedirects = true;
    }

    public APIRequest(String name, HttpMethod method, String url) {
        this();
        this.name = name;
        this.method = method;
        this.url = url;
    }

    public APIRequest(String name) {
        this();
        this.name = name;
    }

    // Static factory methods
    public static APIRequest get(String name, String url) {
        return new APIRequest(name, HttpMethod.GET, url);
    }

    public static APIRequest post(String name, String url) {
        return new APIRequest(name, HttpMethod.POST, url);
    }

    public static APIRequest put(String name, String url) {
        return new APIRequest(name, HttpMethod.PUT, url);
    }

    public static APIRequest patch(String name, String url) {
        return new APIRequest(name, HttpMethod.PATCH, url);
    }

    public static APIRequest delete(String name, String url) {
        return new APIRequest(name, HttpMethod.DELETE, url);
    }

    // Fluent builder methods
    public APIRequest withHeader(String key, String value) {
        this.headers.add(new KeyValuePair(key, value));
        return this;
    }

    public APIRequest withQueryParam(String key, String value) {
        this.queryParams.add(new KeyValuePair(key, value));
        return this;
    }

    public APIRequest withJsonBody(String json) {
        this.body = RequestBody.json(json);
        return this;
    }

    public APIRequest withBasicAuth(String username, String password) {
        this.auth = AuthConfig.basic(username, password);
        return this;
    }

    public APIRequest withBearerToken(String token) {
        this.auth = AuthConfig.bearer(token);
        return this;
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

    public HttpMethod getMethod() {
        return method;
    }

    public void setMethod(HttpMethod method) {
        this.method = method;
        this.updatedAt = Instant.now().toEpochMilli();
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
        this.updatedAt = Instant.now().toEpochMilli();
    }

    public List<KeyValuePair> getQueryParams() {
        return queryParams;
    }

    public void setQueryParams(List<KeyValuePair> queryParams) {
        this.queryParams = queryParams;
        this.updatedAt = Instant.now().toEpochMilli();
    }

    public List<KeyValuePair> getHeaders() {
        return headers;
    }

    public void setHeaders(List<KeyValuePair> headers) {
        this.headers = headers;
        this.updatedAt = Instant.now().toEpochMilli();
    }

    public List<KeyValuePair> getPathVariables() {
        return pathVariables;
    }

    public void setPathVariables(List<KeyValuePair> pathVariables) {
        this.pathVariables = pathVariables;
        this.updatedAt = Instant.now().toEpochMilli();
    }

    public RequestBody getBody() {
        return body;
    }

    public void setBody(RequestBody body) {
        this.body = body;
        this.updatedAt = Instant.now().toEpochMilli();
    }

    public AuthConfig getAuth() {
        return auth;
    }

    public void setAuth(AuthConfig auth) {
        this.auth = auth;
        this.updatedAt = Instant.now().toEpochMilli();
    }

    public String getPreRequestScript() {
        return preRequestScript;
    }

    public void setPreRequestScript(String preRequestScript) {
        this.preRequestScript = preRequestScript;
        this.updatedAt = Instant.now().toEpochMilli();
    }

    public String getTestScript() {
        return testScript;
    }

    public void setTestScript(String testScript) {
        this.testScript = testScript;
        this.updatedAt = Instant.now().toEpochMilli();
    }

    public List<APIAssertion> getAssertions() {
        return assertions;
    }

    public void setAssertions(List<APIAssertion> assertions) {
        this.assertions = assertions;
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

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
        this.updatedAt = Instant.now().toEpochMilli();
    }

    public boolean isFollowRedirects() {
        return followRedirects;
    }

    public void setFollowRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
        this.updatedAt = Instant.now().toEpochMilli();
    }

    /**
     * Returns enabled headers only.
     */
    public List<KeyValuePair> getEnabledHeaders() {
        List<KeyValuePair> enabled = new ArrayList<>();
        if (headers != null) {
            for (KeyValuePair h : headers) {
                if (h.isEnabled()) {
                    enabled.add(h);
                }
            }
        }
        return enabled;
    }

    /**
     * Returns enabled query params only.
     */
    public List<KeyValuePair> getEnabledQueryParams() {
        List<KeyValuePair> enabled = new ArrayList<>();
        if (queryParams != null) {
            for (KeyValuePair p : queryParams) {
                if (p.isEnabled()) {
                    enabled.add(p);
                }
            }
        }
        return enabled;
    }

    /**
     * Creates a deep copy of this request.
     */
    public APIRequest copy() {
        APIRequest copy = new APIRequest();
        copy.setId(UUID.randomUUID().toString()); // New ID for copy
        copy.setName(this.name != null ? this.name + " (Copy)" : "Copy");
        copy.setDescription(this.description);
        copy.setMethod(this.method);
        copy.setUrl(this.url);
        copy.setTimeout(this.timeout);
        copy.setFollowRedirects(this.followRedirects);
        copy.setPreRequestScript(this.preRequestScript);
        copy.setTestScript(this.testScript);
        
        // Deep copy collections
        if (this.queryParams != null) {
            copy.setQueryParams(new ArrayList<>());
            for (KeyValuePair kvp : this.queryParams) {
                copy.getQueryParams().add(kvp.copy());
            }
        }
        if (this.headers != null) {
            copy.setHeaders(new ArrayList<>());
            for (KeyValuePair kvp : this.headers) {
                copy.getHeaders().add(kvp.copy());
            }
        }
        if (this.pathVariables != null) {
            copy.setPathVariables(new ArrayList<>());
            for (KeyValuePair kvp : this.pathVariables) {
                copy.getPathVariables().add(kvp.copy());
            }
        }
        if (this.body != null) {
            copy.setBody(this.body.copy());
        }
        if (this.auth != null) {
            copy.setAuth(this.auth.copy());
        }
        if (this.assertions != null) {
            copy.setAssertions(new ArrayList<>());
            for (APIAssertion a : this.assertions) {
                copy.getAssertions().add(a.copy());
            }
        }
        
        return copy;
    }

    @Override
    public String toString() {
        return method + " " + (name != null ? name : url);
    }
}

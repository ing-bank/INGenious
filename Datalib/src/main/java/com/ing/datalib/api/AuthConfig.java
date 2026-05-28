package com.ing.datalib.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.util.Base64;

/**
 * Represents authentication configuration for an API request.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Authentication type.
     */
    public enum AuthType {
        NONE,
        BASIC,
        BEARER,
        API_KEY,
        OAUTH2
    }

    /**
     * Where to add API key.
     */
    public enum ApiKeyLocation {
        HEADER,
        QUERY_PARAM
    }

    private AuthType authType;
    
    // Basic Auth
    private String basicUsername;
    private String basicPassword;
    
    // Bearer Token
    private String bearerToken;
    private String bearerPrefix;
    
    // API Key
    private String apiKeyName;
    private String apiKeyValue;
    private ApiKeyLocation apiKeyLocation;
    
    // OAuth2
    private String oauth2AccessToken;
    private String oauth2TokenUrl;
    private String oauth2ClientId;
    private String oauth2ClientSecret;
    private String oauth2Scope;
    private String oauth2GrantType;

    public AuthConfig() {
        this.authType = AuthType.NONE;
        this.bearerPrefix = "Bearer";
        this.apiKeyLocation = ApiKeyLocation.HEADER;
        this.oauth2GrantType = "client_credentials";
    }

    public static AuthConfig none() {
        return new AuthConfig();
    }

    public static AuthConfig basic(String username, String password) {
        AuthConfig config = new AuthConfig();
        config.setAuthType(AuthType.BASIC);
        config.setBasicUsername(username);
        config.setBasicPassword(password);
        return config;
    }

    public static AuthConfig bearer(String token) {
        AuthConfig config = new AuthConfig();
        config.setAuthType(AuthType.BEARER);
        config.setBearerToken(token);
        return config;
    }

    public static AuthConfig apiKey(String name, String value, ApiKeyLocation location) {
        AuthConfig config = new AuthConfig();
        config.setAuthType(AuthType.API_KEY);
        config.setApiKeyName(name);
        config.setApiKeyValue(value);
        config.setApiKeyLocation(location);
        return config;
    }

    // Getters and Setters
    public AuthType getAuthType() {
        return authType;
    }

    public void setAuthType(AuthType authType) {
        this.authType = authType;
    }

    public String getBasicUsername() {
        return basicUsername;
    }

    public void setBasicUsername(String basicUsername) {
        this.basicUsername = basicUsername;
    }

    public String getBasicPassword() {
        return basicPassword;
    }

    public void setBasicPassword(String basicPassword) {
        this.basicPassword = basicPassword;
    }

    public String getBearerToken() {
        return bearerToken;
    }

    public void setBearerToken(String bearerToken) {
        this.bearerToken = bearerToken;
    }

    public String getBearerPrefix() {
        return bearerPrefix;
    }

    public void setBearerPrefix(String bearerPrefix) {
        this.bearerPrefix = bearerPrefix;
    }

    public String getApiKeyName() {
        return apiKeyName;
    }

    public void setApiKeyName(String apiKeyName) {
        this.apiKeyName = apiKeyName;
    }

    public String getApiKeyValue() {
        return apiKeyValue;
    }

    public void setApiKeyValue(String apiKeyValue) {
        this.apiKeyValue = apiKeyValue;
    }

    public ApiKeyLocation getApiKeyLocation() {
        return apiKeyLocation;
    }

    public void setApiKeyLocation(ApiKeyLocation apiKeyLocation) {
        this.apiKeyLocation = apiKeyLocation;
    }

    public String getOauth2AccessToken() {
        return oauth2AccessToken;
    }

    public void setOauth2AccessToken(String oauth2AccessToken) {
        this.oauth2AccessToken = oauth2AccessToken;
    }

    public String getOauth2TokenUrl() {
        return oauth2TokenUrl;
    }

    public void setOauth2TokenUrl(String oauth2TokenUrl) {
        this.oauth2TokenUrl = oauth2TokenUrl;
    }

    public String getOauth2ClientId() {
        return oauth2ClientId;
    }

    public void setOauth2ClientId(String oauth2ClientId) {
        this.oauth2ClientId = oauth2ClientId;
    }

    public String getOauth2ClientSecret() {
        return oauth2ClientSecret;
    }

    public void setOauth2ClientSecret(String oauth2ClientSecret) {
        this.oauth2ClientSecret = oauth2ClientSecret;
    }

    public String getOauth2Scope() {
        return oauth2Scope;
    }

    public void setOauth2Scope(String oauth2Scope) {
        this.oauth2Scope = oauth2Scope;
    }

    public String getOauth2GrantType() {
        return oauth2GrantType;
    }

    public void setOauth2GrantType(String oauth2GrantType) {
        this.oauth2GrantType = oauth2GrantType;
    }

    /**
     * Returns the Authorization header value for this auth config.
     */
    public String getAuthorizationHeader() {
        if (authType == null || authType == AuthType.NONE) {
            return null;
        }
        switch (authType) {
            case BASIC:
                if (basicUsername != null && basicPassword != null) {
                    String credentials = basicUsername + ":" + basicPassword;
                    return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
                }
                break;
            case BEARER:
                if (bearerToken != null && !bearerToken.isEmpty()) {
                    String prefix = bearerPrefix != null ? bearerPrefix : "Bearer";
                    return prefix + " " + bearerToken;
                }
                break;
            case API_KEY:
                if (apiKeyLocation == ApiKeyLocation.HEADER && apiKeyName != null && apiKeyValue != null) {
                    return null; // API key header is separate, not Authorization
                }
                break;
            case OAUTH2:
                if (oauth2AccessToken != null && !oauth2AccessToken.isEmpty()) {
                    return "Bearer " + oauth2AccessToken;
                }
                break;
        }
        return null;
    }

    /**
     * Returns the API key header name and value if auth type is API_KEY with HEADER location.
     */
    public KeyValuePair getApiKeyHeader() {
        if (authType == AuthType.API_KEY && apiKeyLocation == ApiKeyLocation.HEADER 
                && apiKeyName != null && apiKeyValue != null) {
            return new KeyValuePair(apiKeyName, apiKeyValue);
        }
        return null;
    }

    /**
     * Returns the API key query param if auth type is API_KEY with QUERY_PARAM location.
     */
    public KeyValuePair getApiKeyQueryParam() {
        if (authType == AuthType.API_KEY && apiKeyLocation == ApiKeyLocation.QUERY_PARAM 
                && apiKeyName != null && apiKeyValue != null) {
            return new KeyValuePair(apiKeyName, apiKeyValue);
        }
        return null;
    }

    /**
     * Creates a copy of this auth config.
     */
    public AuthConfig copy() {
        AuthConfig copy = new AuthConfig();
        copy.setAuthType(this.authType);
        copy.setBasicUsername(this.basicUsername);
        copy.setBasicPassword(this.basicPassword);
        copy.setBearerToken(this.bearerToken);
        copy.setBearerPrefix(this.bearerPrefix);
        copy.setApiKeyName(this.apiKeyName);
        copy.setApiKeyValue(this.apiKeyValue);
        copy.setApiKeyLocation(this.apiKeyLocation);
        copy.setOauth2AccessToken(this.oauth2AccessToken);
        copy.setOauth2TokenUrl(this.oauth2TokenUrl);
        copy.setOauth2ClientId(this.oauth2ClientId);
        copy.setOauth2ClientSecret(this.oauth2ClientSecret);
        copy.setOauth2Scope(this.oauth2Scope);
        copy.setOauth2GrantType(this.oauth2GrantType);
        return copy;
    }
}

package com.ing.ide.main.mainui.components.apitester.util;

import com.ing.datalib.api.*;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * HTTP client wrapper for executing API requests.
 * Built on top of Java 11+ HttpClient.
 */
public class APIHttpClient {

    private static final Logger LOG = Logger.getLogger(APIHttpClient.class.getName());
    
    private final HttpClient httpClient;
    private final HttpClient insecureHttpClient;
    private boolean trustAllCertificates;
    private int defaultTimeout;
    private APIEnvironment environment;

    public APIHttpClient() {
        this.defaultTimeout = 30000; // 30 seconds
        this.trustAllCertificates = false;
        this.httpClient = createSecureClient();
        this.insecureHttpClient = createInsecureClient();
    }

    public APIHttpClient(APIEnvironment environment) {
        this();
        this.environment = environment;
    }

    private HttpClient createSecureClient() {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMillis(defaultTimeout))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    private HttpClient createInsecureClient() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
            };
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            
            return HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .connectTimeout(Duration.ofMillis(defaultTimeout))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .sslContext(sslContext)
                    .build();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            LOG.log(Level.WARNING, "Failed to create insecure client, falling back to secure", e);
            return createSecureClient();
        }
    }

    /**
     * Executes an API request and returns the response.
     */
    public APIResponse execute(APIRequest request) {
        try {
            // Resolve variables in URL
            String resolvedUrl = resolveVariables(request.getUrl());
            
            // Build query string
            String queryString = buildQueryString(request.getEnabledQueryParams());
            if (!queryString.isEmpty()) {
                resolvedUrl += (resolvedUrl.contains("?") ? "&" : "?") + queryString;
            }
            
            // Add API key as query param if configured
            if (request.getAuth() != null) {
                KeyValuePair apiKeyParam = request.getAuth().getApiKeyQueryParam();
                if (apiKeyParam != null) {
                    String paramStr = encode(apiKeyParam.getKey()) + "=" + encode(resolveVariables(apiKeyParam.getValue()));
                    resolvedUrl += (resolvedUrl.contains("?") ? "&" : "?") + paramStr;
                }
            }
            
            URI uri = URI.create(resolvedUrl);
            
            // Build the request
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofMillis(request.getTimeout() > 0 ? request.getTimeout() : defaultTimeout));
            
            // Add headers
            addHeaders(builder, request);
            
            // Set method and body
            setMethodAndBody(builder, request);
            
            HttpRequest httpRequest = builder.build();
            
            // Execute the request
            Instant start = Instant.now();
            HttpClient client = trustAllCertificates ? insecureHttpClient : httpClient;
            HttpResponse<String> httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            long responseTimeMs = Duration.between(start, Instant.now()).toMillis();
            
            // Build response
            Map<String, List<String>> headers = new HashMap<>(httpResponse.headers().map());
            APIResponse response = new APIResponse(
                    httpResponse.statusCode(),
                    httpResponse.body(),
                    headers,
                    responseTimeMs
            );
            response.setRequestId(request.getId());
            
            // Run assertions
            if (request.getAssertions() != null && !request.getAssertions().isEmpty()) {
                List<APIResponse.AssertionResult> results = runAssertions(request.getAssertions(), response);
                response.setAssertionResults(results);
            }
            
            return response;
            
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "IO error executing request", e);
            return APIResponse.error("Connection error: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.log(Level.WARNING, "Request interrupted", e);
            return APIResponse.error("Request interrupted");
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error executing request", e);
            return APIResponse.error("Error: " + e.getMessage());
        }
    }

    private void addHeaders(HttpRequest.Builder builder, APIRequest request) {
        // Add default headers
        Map<String, String> headers = new LinkedHashMap<>();
        
        // Add request headers
        for (KeyValuePair header : request.getEnabledHeaders()) {
            headers.put(header.getKey(), resolveVariables(header.getValue()));
        }
        
        // Add authorization header
        if (request.getAuth() != null) {
            String authHeader = request.getAuth().getAuthorizationHeader();
            if (authHeader != null) {
                headers.put("Authorization", resolveVariables(authHeader));
            }
            
            // Add API key header if configured
            KeyValuePair apiKeyHeader = request.getAuth().getApiKeyHeader();
            if (apiKeyHeader != null) {
                headers.put(apiKeyHeader.getKey(), resolveVariables(apiKeyHeader.getValue()));
            }
        }
        
        // Add Content-Type if body has content
        if (request.getBody() != null && request.getBody().hasContent()) {
            String contentType = request.getBody().getContentType();
            if (contentType != null && !headers.containsKey("Content-Type")) {
                headers.put("Content-Type", contentType);
            }
        }
        
        // Apply headers to builder
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            builder.header(entry.getKey(), entry.getValue());
        }
    }

    private void setMethodAndBody(HttpRequest.Builder builder, APIRequest request) {
        HttpRequest.BodyPublisher bodyPublisher = buildBodyPublisher(request.getBody());
        
        switch (request.getMethod()) {
            case GET:
                builder.GET();
                break;
            case POST:
                builder.POST(bodyPublisher);
                break;
            case PUT:
                builder.PUT(bodyPublisher);
                break;
            case PATCH:
                builder.method("PATCH", bodyPublisher);
                break;
            case DELETE:
                builder.DELETE();
                break;
            case HEAD:
                builder.method("HEAD", HttpRequest.BodyPublishers.noBody());
                break;
            case OPTIONS:
                builder.method("OPTIONS", HttpRequest.BodyPublishers.noBody());
                break;
            default:
                builder.GET();
        }
    }

    private HttpRequest.BodyPublisher buildBodyPublisher(RequestBody body) {
        if (body == null || !body.hasContent()) {
            return HttpRequest.BodyPublishers.noBody();
        }
        
        switch (body.getBodyType()) {
            case RAW:
                String content = resolveVariables(body.getRawContent());
                return HttpRequest.BodyPublishers.ofString(content);
                
            case URL_ENCODED:
                StringBuilder sb = new StringBuilder();
                for (KeyValuePair kvp : body.getUrlEncodedData()) {
                    if (kvp.isEnabled()) {
                        if (sb.length() > 0) sb.append("&");
                        sb.append(encode(kvp.getKey())).append("=").append(encode(resolveVariables(kvp.getValue())));
                    }
                }
                return HttpRequest.BodyPublishers.ofString(sb.toString());
                
            case GRAPHQL:
                // GraphQL is sent as JSON
                String query = body.getGraphqlQuery();
                String variables = body.getGraphqlVariables();
                String graphqlBody = "{\"query\":" + escapeJson(resolveVariables(query));
                if (variables != null && !variables.isEmpty()) {
                    graphqlBody += ",\"variables\":" + resolveVariables(variables);
                }
                graphqlBody += "}";
                return HttpRequest.BodyPublishers.ofString(graphqlBody);
                
            default:
                return HttpRequest.BodyPublishers.noBody();
        }
    }

    private String buildQueryString(List<KeyValuePair> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (KeyValuePair param : params) {
            if (param.isEnabled()) {
                if (sb.length() > 0) sb.append("&");
                sb.append(encode(param.getKey())).append("=").append(encode(resolveVariables(param.getValue())));
            }
        }
        return sb.toString();
    }

    private String resolveVariables(String input) {
        if (input == null || environment == null) {
            return input;
        }
        return environment.resolve(input);
    }

    private String encode(String value) {
        return URLEncoder.encode(value != null ? value : "", StandardCharsets.UTF_8);
    }

    private String escapeJson(String value) {
        if (value == null) return "null";
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                + "\"";
    }

    /**
     * Runs assertions against the response.
     */
    private List<APIResponse.AssertionResult> runAssertions(List<APIAssertion> assertions, APIResponse response) {
        List<APIResponse.AssertionResult> results = new ArrayList<>();
        
        for (APIAssertion assertion : assertions) {
            if (!assertion.isEnabled()) {
                continue;
            }
            
            APIResponse.AssertionResult result = new APIResponse.AssertionResult();
            result.setAssertionId(assertion.getId());
            result.setAssertionName(assertion.getName() != null ? assertion.getName() : assertion.toString());
            result.setExpectedValue(assertion.getExpectedValue());
            
            try {
                boolean passed = evaluateAssertion(assertion, response, result);
                result.setPassed(passed);
                if (passed) {
                    result.setMessage("Assertion passed");
                }
            } catch (Exception e) {
                result.setPassed(false);
                result.setMessage("Error: " + e.getMessage());
            }
            
            results.add(result);
        }
        
        return results;
    }

    private boolean evaluateAssertion(APIAssertion assertion, APIResponse response, APIResponse.AssertionResult result) {
        String actual;
        
        switch (assertion.getType()) {
            case STATUS_CODE:
                actual = String.valueOf(response.getStatusCode());
                result.setActualValue(actual);
                return compare(actual, assertion.getOperator(), assertion.getExpectedValue());
                
            case RESPONSE_TIME:
                actual = String.valueOf(response.getResponseTimeMs());
                result.setActualValue(actual + " ms");
                return compare(actual, assertion.getOperator(), assertion.getExpectedValue());
                
            case HEADER:
                actual = response.getHeader(assertion.getTarget());
                result.setActualValue(actual);
                return compare(actual, assertion.getOperator(), assertion.getExpectedValue());
                
            case CONTENT_TYPE:
                actual = response.getContentType();
                result.setActualValue(actual);
                return compare(actual, assertion.getOperator(), assertion.getExpectedValue());
                
            case BODY_CONTAINS:
                actual = response.getBody();
                result.setActualValue(actual != null && actual.length() > 100 ? actual.substring(0, 100) + "..." : actual);
                return actual != null && actual.contains(assertion.getExpectedValue());
                
            case BODY_EQUALS:
                actual = response.getBody();
                result.setActualValue(actual != null && actual.length() > 100 ? actual.substring(0, 100) + "..." : actual);
                return Objects.equals(actual, assertion.getExpectedValue());
                
            case BODY_MATCHES_REGEX:
                actual = response.getBody();
                result.setActualValue(actual != null && actual.length() > 100 ? actual.substring(0, 100) + "..." : actual);
                return actual != null && Pattern.matches(assertion.getExpectedValue(), actual);
                
            case JSON_PATH:
                try {
                    Object jsonValue = JsonPath.read(response.getBody(), assertion.getTarget());
                    actual = jsonValue != null ? jsonValue.toString() : null;
                    result.setActualValue(actual);
                    return compare(actual, assertion.getOperator(), assertion.getExpectedValue());
                } catch (PathNotFoundException e) {
                    result.setActualValue("(path not found)");
                    result.setMessage("JSON path not found: " + assertion.getTarget());
                    return assertion.getOperator() == APIAssertion.Operator.NOT_EXISTS;
                }
                
            default:
                result.setMessage("Unsupported assertion type: " + assertion.getType());
                return false;
        }
    }

    private boolean compare(String actual, APIAssertion.Operator operator, String expected) {
        switch (operator) {
            case EQUALS:
                return Objects.equals(actual, expected);
            case NOT_EQUALS:
                return !Objects.equals(actual, expected);
            case CONTAINS:
                return actual != null && actual.contains(expected);
            case NOT_CONTAINS:
                return actual == null || !actual.contains(expected);
            case STARTS_WITH:
                return actual != null && actual.startsWith(expected);
            case ENDS_WITH:
                return actual != null && actual.endsWith(expected);
            case EXISTS:
                return actual != null;
            case NOT_EXISTS:
                return actual == null;
            case IS_NULL:
                return actual == null || "null".equals(actual);
            case IS_NOT_NULL:
                return actual != null && !"null".equals(actual);
            case GREATER_THAN:
                return compareNumbers(actual, expected) > 0;
            case LESS_THAN:
                return compareNumbers(actual, expected) < 0;
            case GREATER_THAN_OR_EQUALS:
                return compareNumbers(actual, expected) >= 0;
            case LESS_THAN_OR_EQUALS:
                return compareNumbers(actual, expected) <= 0;
            case MATCHES_REGEX:
                return actual != null && Pattern.matches(expected, actual);
            default:
                return false;
        }
    }

    private int compareNumbers(String actual, String expected) {
        try {
            double a = Double.parseDouble(actual);
            double e = Double.parseDouble(expected);
            return Double.compare(a, e);
        } catch (NumberFormatException ex) {
            return actual.compareTo(expected);
        }
    }

    // Getters and Setters
    public boolean isTrustAllCertificates() {
        return trustAllCertificates;
    }

    public void setTrustAllCertificates(boolean trustAllCertificates) {
        this.trustAllCertificates = trustAllCertificates;
    }

    public int getDefaultTimeout() {
        return defaultTimeout;
    }

    public void setDefaultTimeout(int defaultTimeout) {
        this.defaultTimeout = defaultTimeout;
    }

    public APIEnvironment getEnvironment() {
        return environment;
    }

    public void setEnvironment(APIEnvironment environment) {
        this.environment = environment;
    }
}

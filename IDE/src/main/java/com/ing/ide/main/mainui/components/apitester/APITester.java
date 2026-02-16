package com.ing.ide.main.mainui.components.apitester;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.ing.datalib.api.*;
import com.ing.datalib.component.Project;
import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.component.TestStep;
import com.ing.ide.main.mainui.AppMainFrame;
import com.ing.ide.main.mainui.components.apitester.util.APIHttpClient;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main controller for the API Tester feature.
 * Manages collections, environments, history, and request execution.
 */
public class APITester {

    private static final Logger LOG = Logger.getLogger(APITester.class.getName());
    
    private final AppMainFrame mainFrame;
    private final APITesterUI apiTesterUI;
    private final APIHttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    private List<APICollection> collections;
    private List<APIEnvironment> environments;
    private List<APIRequest> history;
    private APIEnvironment activeEnvironment;
    
    private static final int MAX_HISTORY_SIZE = 50;
    
    public APITester(AppMainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.httpClient = new APIHttpClient();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        
        this.collections = new ArrayList<>();
        this.environments = new ArrayList<>();
        this.history = new ArrayList<>();
        
        this.apiTesterUI = new APITesterUI(this);
        
        // Load data if project is open
        if (mainFrame.getProject() != null) {
            loadData();
        }
    }

    public APITesterUI getAPITesterUI() {
        return apiTesterUI;
    }

    public AppMainFrame getMainFrame() {
        return mainFrame;
    }
    
    public APIHttpClient getHttpClient() {
        return httpClient;
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // Request Execution
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Executes an API request asynchronously.
     */
    public void executeRequest(APIRequest request, RequestCallback callback) {
        // Update HTTP client environment
        httpClient.setEnvironment(activeEnvironment);
        
        // Execute in background thread
        new Thread(() -> {
            try {
                APIResponse response = httpClient.execute(request);
                
                // Add to history
                addToHistory(request);
                
                // Callback on EDT
                javax.swing.SwingUtilities.invokeLater(() -> {
                    callback.onResponse(response);
                });
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Error executing request", e);
                javax.swing.SwingUtilities.invokeLater(() -> {
                    callback.onError(e);
                });
            }
        }, "API-Request-Executor").start();
    }
    
    public interface RequestCallback {
        void onResponse(APIResponse response);
        void onError(Exception error);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // Collections Management
    // ═══════════════════════════════════════════════════════════════════
    
    public List<APICollection> getCollections() {
        return collections;
    }
    
    public void addCollection(APICollection collection) {
        collections.add(collection);
        saveCollections();
        apiTesterUI.refreshCollectionsTree();
    }
    
    public void removeCollection(APICollection collection) {
        collections.remove(collection);
        saveCollections();
        apiTesterUI.refreshCollectionsTree();
    }
    
    public void updateCollection(APICollection collection) {
        saveCollections();
        apiTesterUI.refreshCollectionsTree();
    }
    
    public APICollection createNewCollection(String name) {
        APICollection collection = new APICollection(name);
        addCollection(collection);
        return collection;
    }
    
    public void saveRequestToCollection(APIRequest request, APICollection collection) {
        collection.addRequest(request);
        saveCollections();
        apiTesterUI.refreshCollectionsTree();
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // Environments Management
    // ═══════════════════════════════════════════════════════════════════
    
    public List<APIEnvironment> getEnvironments() {
        return environments;
    }
    
    public APIEnvironment getActiveEnvironment() {
        return activeEnvironment;
    }
    
    public void setActiveEnvironment(APIEnvironment environment) {
        // Deactivate previous
        if (activeEnvironment != null) {
            activeEnvironment.setActive(false);
        }
        
        this.activeEnvironment = environment;
        
        // Activate new
        if (environment != null) {
            environment.setActive(true);
        }
        
        httpClient.setEnvironment(environment);
        saveEnvironments();
        apiTesterUI.updateEnvironmentSelector();
    }
    
    public void addEnvironment(APIEnvironment environment) {
        environments.add(environment);
        saveEnvironments();
        apiTesterUI.updateEnvironmentSelector();
    }
    
    public void removeEnvironment(APIEnvironment environment) {
        environments.remove(environment);
        if (activeEnvironment == environment) {
            activeEnvironment = null;
            httpClient.setEnvironment(null);
        }
        saveEnvironments();
        apiTesterUI.updateEnvironmentSelector();
    }
    
    public APIEnvironment createNewEnvironment(String name) {
        APIEnvironment environment = new APIEnvironment(name);
        addEnvironment(environment);
        return environment;
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // History Management
    // ═══════════════════════════════════════════════════════════════════
    
    public List<APIRequest> getHistory() {
        return history;
    }
    
    public void addToHistory(APIRequest request) {
        // Create a copy for history
        APIRequest historyEntry = request.copy();
        historyEntry.setName(request.getMethod() + " " + request.getUrl());
        
        // Add at the beginning
        history.add(0, historyEntry);
        
        // Limit history size
        while (history.size() > MAX_HISTORY_SIZE) {
            history.remove(history.size() - 1);
        }
        
        saveHistory();
        apiTesterUI.refreshHistory();
    }
    
    public void clearHistory() {
        history.clear();
        saveHistory();
        apiTesterUI.refreshHistory();
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // Persistence
    // ═══════════════════════════════════════════════════════════════════
    
    private Path getApiDataPath() {
        if (mainFrame.getProject() == null) {
            return null;
        }
        return Path.of(mainFrame.getProject().getLocation(), "api");
    }
    
    public void loadData() {
        Path apiPath = getApiDataPath();
        if (apiPath == null) return;
        
        loadCollections();
        loadEnvironments();
        loadHistory();
        
        // Set first active environment
        for (APIEnvironment env : environments) {
            if (env.isActive()) {
                activeEnvironment = env;
                httpClient.setEnvironment(env);
                break;
            }
        }
    }
    
    public void saveData() {
        saveCollections();
        saveEnvironments();
        saveHistory();
    }
    
    private void loadCollections() {
        Path apiPath = getApiDataPath();
        if (apiPath == null) return;
        
        Path collectionsPath = apiPath.resolve("collections");
        if (!Files.exists(collectionsPath)) return;
        
        collections.clear();
        try {
            Files.list(collectionsPath)
                    .filter(p -> p.toString().endsWith(".json"))
                    .forEach(p -> {
                        try {
                            APICollection collection = objectMapper.readValue(p.toFile(), APICollection.class);
                            collections.add(collection);
                        } catch (IOException e) {
                            LOG.log(Level.WARNING, "Failed to load collection: " + p, e);
                        }
                    });
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to load collections", e);
        }
    }
    
    private void saveCollections() {
        Path apiPath = getApiDataPath();
        if (apiPath == null) return;
        
        Path collectionsPath = apiPath.resolve("collections");
        try {
            Files.createDirectories(collectionsPath);
            
            for (APICollection collection : collections) {
                Path filePath = collectionsPath.resolve(sanitizeFileName(collection.getName()) + ".json");
                objectMapper.writeValue(filePath.toFile(), collection);
            }
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to save collections", e);
        }
    }
    
    private void loadEnvironments() {
        Path apiPath = getApiDataPath();
        if (apiPath == null) return;
        
        Path envsPath = apiPath.resolve("environments");
        if (!Files.exists(envsPath)) return;
        
        environments.clear();
        try {
            Files.list(envsPath)
                    .filter(p -> p.toString().endsWith(".json"))
                    .forEach(p -> {
                        try {
                            APIEnvironment env = objectMapper.readValue(p.toFile(), APIEnvironment.class);
                            environments.add(env);
                        } catch (IOException e) {
                            LOG.log(Level.WARNING, "Failed to load environment: " + p, e);
                        }
                    });
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to load environments", e);
        }
    }
    
    private void saveEnvironments() {
        Path apiPath = getApiDataPath();
        if (apiPath == null) return;
        
        Path envsPath = apiPath.resolve("environments");
        try {
            Files.createDirectories(envsPath);
            
            for (APIEnvironment env : environments) {
                Path filePath = envsPath.resolve(sanitizeFileName(env.getName()) + ".json");
                objectMapper.writeValue(filePath.toFile(), env);
            }
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to save environments", e);
        }
    }
    
    private void loadHistory() {
        Path apiPath = getApiDataPath();
        if (apiPath == null) return;
        
        Path historyPath = apiPath.resolve("history").resolve("recent.json");
        if (!Files.exists(historyPath)) return;
        
        try {
            APIRequest[] requests = objectMapper.readValue(historyPath.toFile(), APIRequest[].class);
            history.clear();
            for (APIRequest r : requests) {
                history.add(r);
            }
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to load history", e);
        }
    }
    
    private void saveHistory() {
        Path apiPath = getApiDataPath();
        if (apiPath == null) return;
        
        Path historyPath = apiPath.resolve("history");
        try {
            Files.createDirectories(historyPath);
            objectMapper.writeValue(historyPath.resolve("recent.json").toFile(), history);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to save history", e);
        }
    }
    
    private String sanitizeFileName(String name) {
        if (name == null) return "unnamed";
        return name.replaceAll("[^a-zA-Z0-9.-]", "_");
    }
    
    /**
     * Saves a single collection to disk.
     */
    public void saveCollection(APICollection collection) {
        Path apiPath = getApiDataPath();
        if (apiPath == null) return;
        
        Path collectionsPath = apiPath.resolve("collections");
        try {
            Files.createDirectories(collectionsPath);
            Path filePath = collectionsPath.resolve(sanitizeFileName(collection.getName()) + ".json");
            objectMapper.writeValue(filePath.toFile(), collection);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to save collection: " + collection.getName(), e);
        }
    }
    
    /**
     * Deletes a collection from disk and memory.
     */
    public void deleteCollection(APICollection collection) {
        collections.remove(collection);
        
        Path apiPath = getApiDataPath();
        if (apiPath != null) {
            Path filePath = apiPath.resolve("collections")
                    .resolve(sanitizeFileName(collection.getName()) + ".json");
            try {
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Failed to delete collection file: " + filePath, e);
            }
        }
        
        apiTesterUI.refreshCollectionsTree();
    }
    
    /**
     * Exports a collection to a file.
     */
    public void exportCollection(APICollection collection, File file) throws IOException {
        objectMapper.writeValue(file, collection);
    }
    
    /**
     * Imports a collection from a file.
     */
    public APICollection importCollection(File file) throws IOException {
        APICollection collection = objectMapper.readValue(file, APICollection.class);
        addCollection(collection);
        return collection;
    }

    /**
     * Called when a project is opened/closed.
     */
    public void onProjectChanged() {
        if (mainFrame.getProject() != null) {
            loadData();
        } else {
            collections.clear();
            environments.clear();
            history.clear();
            activeEnvironment = null;
        }
        apiTesterUI.refresh();
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // Convert API Request to INGenious Test
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Converts an API request to an INGenious test case with test steps.
     * Uses the existing Webservice actions from Engine.
     * 
     * @param request The API request to convert
     * @param scenario The target scenario to add the test case to
     * @param testCaseName The name for the new test case
     * @return The created TestCase, or null if conversion failed
     */
    public TestCase convertRequestToTestCase(APIRequest request, Scenario scenario, String testCaseName) {
        if (request == null || scenario == null || testCaseName == null) {
            return null;
        }
        
        // Create the test case
        TestCase testCase = scenario.addTestCase(testCaseName);
        
        try {
            // Step 1: Set the endpoint URL
            TestStep setEndpointStep = testCase.addNewStep();
            setEndpointStep.setObject("Webservice");
            setEndpointStep.setDescription("Set API Endpoint");
            setEndpointStep.setAction("setEndPoint");
            setEndpointStep.setInput(resolveUrl(request));
            
            // Step 2: Add headers if present
            if (request.getHeaders() != null && !request.getHeaders().isEmpty()) {
                for (KeyValuePair header : request.getHeaders()) {
                    if (header.isEnabled()) {
                        TestStep headerStep = testCase.addNewStep();
                        headerStep.setObject("Webservice");
                        headerStep.setDescription("Add Header: " + header.getKey());
                        headerStep.setAction("addHeader");
                        headerStep.setInput(header.getKey());
                        headerStep.setCondition(header.getValue());
                    }
                }
            }
            
            // Step 3: Add authentication headers if configured
            addAuthSteps(testCase, request.getAuth());
            
            // Step 4: Execute the request based on HTTP method
            TestStep requestStep = testCase.addNewStep();
            requestStep.setObject("Webservice");
            requestStep.setDescription("Execute " + request.getMethod() + " Request");
            
            switch (request.getMethod()) {
                case GET:
                    requestStep.setAction("getRestRequest");
                    break;
                case POST:
                    requestStep.setAction("postRestRequest");
                    if (request.getBody() != null && request.getBody().getRawContent() != null) {
                        requestStep.setInput(request.getBody().getRawContent());
                    }
                    break;
                case PUT:
                    requestStep.setAction("putRestRequest");
                    if (request.getBody() != null && request.getBody().getRawContent() != null) {
                        requestStep.setInput(request.getBody().getRawContent());
                    }
                    break;
                case PATCH:
                    requestStep.setAction("patchRestRequest");
                    if (request.getBody() != null && request.getBody().getRawContent() != null) {
                        requestStep.setInput(request.getBody().getRawContent());
                    }
                    break;
                case DELETE:
                    if (request.getBody() != null && request.getBody().getRawContent() != null 
                            && !request.getBody().getRawContent().isEmpty()) {
                        requestStep.setAction("deleteWithPayload");
                        requestStep.setInput(request.getBody().getRawContent());
                    } else {
                        requestStep.setAction("deleteRestRequest");
                    }
                    break;
                default:
                    requestStep.setAction("getRestRequest");
            }
            
            // Step 5: Add assertions
            addAssertionSteps(testCase, request.getAssertions());
            
            // Save the test case
            testCase.save();
            
            // Add the test case to the Test Design tree so it's immediately visible
            if (mainFrame.getTestDesign() != null && mainFrame.getTestDesign().getProjectTree() != null) {
                mainFrame.getTestDesign().getProjectTree().getTreeModel().addTestCase(testCase);
            }
            
            LOG.info("Converted API request '" + request.getName() + "' to test case '" + testCaseName + "'");
            return testCase;
            
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to convert API request to test case", e);
            // Remove failed test case
            scenario.removeTestCase(testCase);
            return null;
        }
    }
    
    private String resolveUrl(APIRequest request) {
        String url = request.getUrl();
        
        // Append query parameters
        if (request.getQueryParams() != null && !request.getQueryParams().isEmpty()) {
            StringBuilder queryString = new StringBuilder();
            for (KeyValuePair param : request.getQueryParams()) {
                if (param.isEnabled()) {
                    if (queryString.length() > 0) {
                        queryString.append("&");
                    }
                    queryString.append(param.getKey()).append("=").append(param.getValue());
                }
            }
            if (queryString.length() > 0) {
                url += (url.contains("?") ? "&" : "?") + queryString;
            }
        }
        
        return url;
    }
    
    private void addAuthSteps(TestCase testCase, AuthConfig auth) {
        if (auth == null || auth.getAuthType() == AuthConfig.AuthType.NONE) {
            return;
        }
        
        TestStep authStep = testCase.addNewStep();
        authStep.setObject("Webservice");
        
        switch (auth.getAuthType()) {
            case BASIC:
                authStep.setDescription("Add Basic Auth Header");
                authStep.setAction("addHeader");
                authStep.setInput("Authorization");
                // Basic auth value
                String basicAuth = "Basic " + java.util.Base64.getEncoder().encodeToString(
                        (auth.getBasicUsername() + ":" + auth.getBasicPassword()).getBytes());
                authStep.setCondition(basicAuth);
                break;
                
            case BEARER:
                authStep.setDescription("Add Bearer Token Header");
                authStep.setAction("addHeader");
                authStep.setInput("Authorization");
                String prefix = auth.getBearerPrefix() != null ? auth.getBearerPrefix() : "Bearer";
                authStep.setCondition(prefix + " " + auth.getBearerToken());
                break;
                
            case API_KEY:
                authStep.setDescription("Add API Key Header");
                authStep.setAction("addHeader");
                authStep.setInput(auth.getApiKeyName() != null ? auth.getApiKeyName() : "X-API-Key");
                authStep.setCondition(auth.getApiKeyValue());
                break;
                
            default:
                // Remove the step if auth type not supported
                testCase.getTestSteps().remove(authStep);
        }
    }
    
    private void addAssertionSteps(TestCase testCase, List<APIAssertion> assertions) {
        if (assertions == null || assertions.isEmpty()) {
            return;
        }
        
        for (APIAssertion assertion : assertions) {
            if (!assertion.isEnabled()) continue;
            
            TestStep assertStep = testCase.addNewStep();
            assertStep.setObject("Webservice");
            
            switch (assertion.getType()) {
                case STATUS_CODE:
                    assertStep.setDescription("Assert Response Code");
                    assertStep.setAction("assertResponseCode");
                    assertStep.setInput(assertion.getExpectedValue());
                    break;
                    
                case JSON_PATH:
                    assertStep.setDescription("Assert JSON: " + assertion.getTarget());
                    if (assertion.getOperator() == APIAssertion.Operator.EQUALS) {
                        assertStep.setAction("assertJSONelementEquals");
                    } else if (assertion.getOperator() == APIAssertion.Operator.CONTAINS) {
                        assertStep.setAction("assertJSONelementContains");
                    } else {
                        assertStep.setAction("assertJSONelementEquals");
                    }
                    assertStep.setInput(assertion.getTarget());
                    assertStep.setCondition(assertion.getExpectedValue());
                    break;
                    
                case BODY_CONTAINS:
                    assertStep.setDescription("Assert Response Body Contains");
                    assertStep.setAction("assertResponsebodyalidate");
                    assertStep.setInput(assertion.getExpectedValue());
                    break;
                    
                case HEADER:
                    assertStep.setDescription("Assert Header: " + assertion.getTarget());
                    assertStep.setAction("assertHeader");
                    assertStep.setInput(assertion.getTarget());
                    assertStep.setCondition(assertion.getExpectedValue());
                    break;
                    
                default:
                    // Remove unsupported assertion type
                    testCase.getTestSteps().remove(assertStep);
            }
        }
    }
    
    /**
     * Gets all scenarios from the current project.
     */
    public List<Scenario> getAvailableScenarios() {
        List<Scenario> scenarios = new ArrayList<>();
        Project project = mainFrame.getProject();
        if (project != null) {
            scenarios.addAll(project.getScenarios());
        }
        return scenarios;
    }
    
    /**
     * Navigates to Test Design view and selects the specified test case.
     * @param testCase The test case to navigate to
     */
    public void navigateToTestCase(TestCase testCase) {
        if (testCase == null) return;
        
        javax.swing.SwingUtilities.invokeLater(() -> {
            // Switch to Test Design view
            mainFrame.showTestDesign();
            
            // Load the test case in the Test Case Component
            if (mainFrame.getTestDesign() != null) {
                mainFrame.getTestDesign().getTestCaseComp().loadTableModelForSelection(testCase);
            }
        });
    }
}

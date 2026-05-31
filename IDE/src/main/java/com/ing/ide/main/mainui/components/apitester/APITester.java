package com.ing.ide.main.mainui.components.apitester;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.ing.datalib.api.*;
import com.ing.datalib.component.Project;
import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.component.TestStep;
import com.ing.datalib.or.common.ObjectGroup;
import com.ing.datalib.or.structureddata.ResolvedStructuredDataObject;
import com.ing.datalib.or.structureddata.StructuredDataAttribute;
import com.ing.datalib.or.structureddata.StructuredDataOR;
import com.ing.datalib.or.structureddata.StructuredDataORObject;
import com.ing.datalib.or.structureddata.StructuredDataORPage;
import com.ing.datalib.or.web.WebOR;
import com.ing.ide.main.mainui.AppMainFrame;
import com.ing.ide.main.mainui.SlideShow;
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
 * Also listens for panel switches to auto-save when leaving the API Tester.
 */
public class APITester implements SlideShow.SlideChangeListener {
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

        // Set SSL verification based on per-request setting
        httpClient.setTrustAllCertificates(!request.isSslVerificationEnabled());

        // Execute in background thread
        new Thread(
            () -> {
                try {
                    APIResponse response = httpClient.execute(request);

                    // Add to history
                    addToHistory(request);

                    // Callback on EDT
                    javax.swing.SwingUtilities.invokeLater(
                        () -> {
                            callback.onResponse(response);
                        }
                    );
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, "Error executing request", e);
                    javax.swing.SwingUtilities.invokeLater(
                        () -> {
                            callback.onError(e);
                        }
                    );
                }
            },
            "API-Request-Executor"
        )
        .start();
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
        collection.addOrUpdateRequest(request);
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
        if (apiPath == null) {
            LOG.log(Level.WARNING, "Cannot load API Workbench data - no project is open");
            return;
        }

        LOG.log(Level.INFO, "Loading API Workbench data from: " + apiPath);

        loadCollections();
        loadEnvironments();
        loadHistory();

        LOG.log(
            Level.INFO,
            "Loaded {0} collections, {1} environments, {2} history entries",
            new Object[] { collections.size(), environments.size(), history.size() }
        );

        // Set first active environment
        for (APIEnvironment env : environments) {
            if (env.isActive()) {
                activeEnvironment = env;
                httpClient.setEnvironment(env);
                break;
            }
        }

        // Refresh UI to display loaded data
        apiTesterUI.refresh();
    }

    /**
     * Force save the currently edited request before saving all data.
     * This ensures that any unsaved changes to the current request are persisted to the backend.
     * Called by AppMainFrame during project save and autosave.
     */
    public void forceCurrentRequestSave() {
        if (apiTesterUI != null) {
            apiTesterUI.forceSaveCurrentRequest();
        }
    }

    public void saveData() {
        // First, ensure the currently edited request is saved to backend
        forceCurrentRequestSave();

        // Then save all collections, environments, and history
        saveCollections();
        saveEnvironments();
        saveHistory();
    }

    /**
     * Registers this APITester with the SlideShow to listen for panel switches.
     * Called when a project is loaded so we can auto-save when leaving the API Tester panel.
     */
    public void registerSlideChangeListener() {
        SlideShow slideShow = mainFrame.getSlideShow();
        if (slideShow != null) {
            slideShow.addSlideChangeListener(this);
        }
    }

    /**
     * Called by SlideShow when leaving the APITester slide to auto-save any edits.
     * This ensures all changes are persisted to backend files when switching panels.
     */
    @Override
    public void onSlideLeaving(String slideName) {
        if ("APITester".equals(slideName)) {
            // Auto-save the current request and all data when leaving API Tester panel
            forceCurrentRequestSave();
            saveData();
        }
    }

    private void loadCollections() {
        Path apiPath = getApiDataPath();
        if (apiPath == null) return;

        collections.clear(); // Always clear first
        Path collectionsPath = apiPath.resolve("collections");
        if (!Files.exists(collectionsPath)) {
            // Create default "My Collection" if folder doesn't exist
            try {
                Files.createDirectories(collectionsPath);
                APICollection defaultCollection = new APICollection("My Collection");
                collections.add(defaultCollection);
                saveCollections();
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Failed to create default collection", e);
            }
            return;
        }

        try {
            Files
                .list(collectionsPath)
                .filter(p -> p.toString().endsWith(".json"))
                .forEach(
                    p -> {
                        try {
                            APICollection collection = objectMapper.readValue(
                                p.toFile(),
                                APICollection.class
                            );
                            collections.add(collection);
                        } catch (IOException e) {
                            LOG.log(Level.WARNING, "Failed to load collection: " + p, e);
                        }
                    }
                );

            // If no collections were loaded, create default one
            if (collections.isEmpty()) {
                APICollection defaultCollection = new APICollection("My Collection");
                collections.add(defaultCollection);
                saveCollections();
            }
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
                Path filePath = collectionsPath.resolve(
                    sanitizeFileName(collection.getName()) + ".json"
                );
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
            Files
                .list(envsPath)
                .filter(p -> p.toString().endsWith(".json"))
                .forEach(
                    p -> {
                        try {
                            APIEnvironment env = objectMapper.readValue(
                                p.toFile(),
                                APIEnvironment.class
                            );
                            environments.add(env);
                        } catch (IOException e) {
                            LOG.log(Level.WARNING, "Failed to load environment: " + p, e);
                        }
                    }
                );
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
            APIRequest[] requests = objectMapper.readValue(
                historyPath.toFile(),
                APIRequest[].class
            );
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
            Path filePath = collectionsPath.resolve(
                sanitizeFileName(collection.getName()) + ".json"
            );
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
            Path filePath = apiPath
                .resolve("collections")
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
    public TestCase convertRequestToTestCase(
        APIRequest request,
        Scenario scenario,
        String testCaseName
    ) {
        if (request == null || scenario == null || testCaseName == null) {
            return null;
        }

        // Create the test case
        TestCase testCase = scenario.addTestCase(testCaseName);
        if (testCase == null) {
            LOG.warning("Test case '" + testCaseName + "' could not be created (likely already exists)");
            return null;
        }

        try {
            buildStepsForRequest(testCase, request);

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

    /**
     * Converts an API request to a Reusable INGenious test case (a "User Intent").
     * <p>
     * The provided {@link Scenario} must belong to {@code REUSABLE_COMPONENTS}. The created
     * test case is saved and added to the Reusables tree.
     *
     * @param request           The API request to convert (already in INGenious model form)
     * @param reusableScenario  Target reusable scenario (must be {@code isReusableScenario() == true})
     * @param testCaseName      Name for the new reusable test case
     * @return The created {@link TestCase}, or {@code null} on failure / name collision
     */
    public TestCase convertRequestToReusable(APIRequest request, Scenario reusableScenario, String testCaseName) {
        if (request == null || reusableScenario == null || testCaseName == null) {
            return null;
        }
        if (!reusableScenario.isReusableScenario()) {
            LOG.warning("convertRequestToReusable called with non-reusable scenario: " + reusableScenario.getName());
            return null;
        }

        TestCase testCase = reusableScenario.addTestCase(testCaseName);
        if (testCase == null) {
            LOG.warning("Reusable '" + testCaseName + "' could not be created (likely already exists)");
            return null;
        }

        try {
            buildStepsForRequest(testCase, request);
            testCase.save();

            // Refresh Reusables tree so the new reusable appears immediately
            if (mainFrame.getTestDesign() != null && mainFrame.getTestDesign().getReusableTree() != null) {
                mainFrame.getTestDesign().getReusableTree().getTreeModel().addTestCase(testCase);
            }

            LOG.info("Converted API request '" + request.getName() + "' to reusable '" + testCaseName + "'");
            return testCase;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to convert API request to reusable", e);
            reusableScenario.removeTestCase(testCase);
            return null;
        }
    }

    /**
     * Shared step-building logic used by both {@link #convertRequestToTestCase} and
     * {@link #convertRequestToReusable}. Creates Webservice steps for endpoint, headers,
     * authentication, the HTTP method call, and assertions.
     */
    private void buildStepsForRequest(TestCase testCase, APIRequest request) {
        // Step 1: Set the endpoint URL
        TestStep setEndpointStep = testCase.addNewStep();
        setEndpointStep.setObject("Webservice");
        setEndpointStep.setDescription("Set API Endpoint");
        setEndpointStep.setAction("setEndPoint");
        setEndpointStep.setInput("@" + resolveUrl(request));

        // Step 2: Add headers if present
        if (request.getHeaders() != null && !request.getHeaders().isEmpty()) {
            for (KeyValuePair header : request.getHeaders()) {
                if (header.isEnabled()) {
                    TestStep headerStep = testCase.addNewStep();
                    headerStep.setObject("Webservice");
                    headerStep.setDescription("Add Header: " + header.getKey());
                    headerStep.setAction("addHeader");
                    headerStep.setInput("@" + header.getKey() + "=" + header.getValue());
                    headerStep.setCondition("");
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
        addAssertionSteps(testCase, request);
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
        authStep.setAction("addHeader");
        authStep.setCondition("");

        switch (auth.getAuthType()) {
            case BASIC: {
                authStep.setDescription("Add Basic Auth Header");
                String basicAuth =
                    "Basic " +
                    java
                        .util.Base64.getEncoder()
                        .encodeToString(
                            (auth.getBasicUsername() + ":" + auth.getBasicPassword()).getBytes()
                        );
                authStep.setInput("@Authorization=" + basicAuth);
                break;
            }
            case BEARER: {
                authStep.setDescription("Add Bearer Token Header");
                String prefix = auth.getBearerPrefix() != null ? auth.getBearerPrefix() : "Bearer";
                authStep.setInput("@Authorization=" + prefix + " " + auth.getBearerToken());
                break;
            }
            case API_KEY: {
                String keyName = auth.getApiKeyName() != null ? auth.getApiKeyName() : "X-API-Key";
                authStep.setDescription("Add API Key Header: " + keyName);
                authStep.setInput("@" + keyName + "=" + (auth.getApiKeyValue() != null ? auth.getApiKeyValue() : ""));
                break;
            }
            default:
                // Remove the step if auth type not supported
                testCase.getTestSteps().remove(authStep);
        }
    }
    
    /**
     * Builds INGenious test steps for the assertions configured on the API request.
     * <p>
     * For JSON-path / XPath assertions the path itself is promoted to a
     * <em>Structured Data</em> object inside the project's API Object Repository
     * (one page per API request, one object per unique path) and the test step
     * references that OR object via Object/Reference columns. The expected value
     * (when applicable) goes into the Input column prefixed with {@code @} so
     * the engine treats it as a literal.
     */
    private void addAssertionSteps(TestCase testCase, APIRequest request) {
        if (request == null) return;
        List<APIAssertion> assertions = request.getAssertions();
        if (assertions == null || assertions.isEmpty()) {
            return;
        }

        boolean createdAnyORObject = false;

        for (APIAssertion assertion : assertions) {
            if (!assertion.isEnabled()) continue;

            switch (assertion.getType()) {
                case STATUS_CODE: {
                    TestStep step = testCase.addNewStep();
                    step.setObject("Webservice");
                    step.setDescription("Assert Response Code");
                    step.setAction("assertResponseCode");
                    step.setInput(prefixAtForLiteral(assertion.getExpectedValue()));
                    break;
                }

                case JSON_PATH:
                case XPATH: {
                    boolean isXPath = assertion.getType() == APIAssertion.AssertionType.XPATH;
                    ResolvedStructuredDataObject rsdo =
                            getOrCreateStructuredDataObject(request, assertion, isXPath);
                    if (rsdo == null) {
                        // OR not available (e.g. no project) - fall back to legacy behaviour
                        // so the test case still has a runnable assertion step.
                        addLegacyPathAssertionStep(testCase, assertion, isXPath);
                        break;
                    }
                    createdAnyORObject = true;

                    String action = pickPathActionName(isXPath, assertion.getOperator());
                    if (action == null) {
                        LOG.warning("Unsupported operator [" + assertion.getOperator()
                                + "] for " + (isXPath ? "XPATH" : "JSON_PATH") + " assertion - step skipped");
                        break;
                    }

                    TestStep step = testCase.addNewStep();
                    step.asObjectStep(rsdo); // sets Object + Reference
                    step.setAction(action);
                    step.setDescription("Assert " + (isXPath ? "XPath" : "JSON") + ": "
                            + assertion.getTarget());
                    // Existence checks ignore the expected value
                    if (assertion.getOperator() != APIAssertion.Operator.EXISTS
                            && assertion.getOperator() != APIAssertion.Operator.NOT_EXISTS) {
                        step.setInput(prefixAtForLiteral(assertion.getExpectedValue()));
                    }
                    break;
                }

                case BODY_CONTAINS: {
                    TestStep step = testCase.addNewStep();
                    step.setObject("Webservice");
                    step.setDescription("Assert Response Body Contains");
                    step.setAction("assertResponsebodycontains");
                    step.setInput(prefixAtForLiteral(assertion.getExpectedValue()));
                    break;
                }

                case HEADER: {
                    TestStep step = testCase.addNewStep();
                    step.setObject("Webservice");
                    step.setDescription("Assert Header: " + assertion.getTarget());
                    String action = (assertion.getOperator() == APIAssertion.Operator.CONTAINS)
                            ? "assertHeaderValueContains" : "assertHeaderValueEquals";
                    step.setAction(action);
                    step.setCondition(assertion.getTarget());                  // header name
                    step.setInput(prefixAtForLiteral(assertion.getExpectedValue())); // expected
                    break;
                }

                default:
                    LOG.warning("Unsupported assertion type for test-case conversion: "
                            + assertion.getType());
            }
        }

        if (createdAnyORObject) {
            refreshStructuredDataTree();
        }
    }

    /**
     * Maps an {@link APIAssertion.Operator} to the matching Engine action name on the
     * STRUCTUREDDATA object. Returns {@code null} when the operator has no engine
     * counterpart (so the caller can skip the step rather than emit a broken one).
     */
    private String pickPathActionName(boolean isXPath, APIAssertion.Operator op) {
        String prefix = isXPath ? "assertXmlPathResult" : "assertJsonPathResult";
        if (op == null) return prefix + "Equals";
        switch (op) {
            case EQUALS:        return prefix + "Equals";
            case NOT_EQUALS:    return prefix + "NotEquals";
            case CONTAINS:      return prefix + "Contains";
            case NOT_CONTAINS:  return prefix + "NotContains";
            case STARTS_WITH:   return prefix + "StartsWith";
            case ENDS_WITH:     return prefix + "EndsWith";
            case MATCHES_REGEX: return prefix + "MatchesRegex";
            case GREATER_THAN:  return prefix + "GreaterThan";
            case LESS_THAN:     return prefix + "LessThan";
            case EXISTS:        return isXPath ? "assertXmlPathExists"    : "assertJsonPathExists";
            case NOT_EXISTS:    return isXPath ? "assertXmlPathNotExists" : "assertJsonPathNotExists";
            default:            return null;
        }
    }

    /**
     * Legacy fallback used when no project is open and the Structured Data OR is
     * not available — keeps the original Webservice-only assertion shape but
     * still applies the {@code @} prefix convention for the expected value.
     */
    private void addLegacyPathAssertionStep(TestCase testCase, APIAssertion assertion, boolean isXPath) {
        TestStep step = testCase.addNewStep();
        step.setObject("Webservice");
        step.setDescription("Assert " + (isXPath ? "XPath" : "JSON") + ": " + assertion.getTarget());
        if (assertion.getOperator() == APIAssertion.Operator.CONTAINS) {
            step.setAction("assertJSONelementContains");
        } else {
            step.setAction("assertJSONelementEquals");
        }
        step.setCondition(assertion.getTarget());
        step.setInput(prefixAtForLiteral(assertion.getExpectedValue()));
    }

    /**
     * Get or create a Structured Data OR object representing the assertion's
     * JSON-path / XPath expression. The page is named after the API request and
     * is created on demand. Within a page, paths are de-duplicated: an existing
     * object with the same attribute value is reused.
     *
     * @return the resolved OR object, or {@code null} if no project / OR is available.
     */
    private ResolvedStructuredDataObject getOrCreateStructuredDataObject(
            APIRequest request, APIAssertion assertion, boolean isXPath) {
        Project project = mainFrame.getProject();
        if (project == null) return null;
        if (project.getObjectRepository() == null) return null;
        StructuredDataOR or = project.getObjectRepository().getStructuredDataOR();
        if (or == null) return null;

        String pathValue = assertion.getTarget();
        if (pathValue == null || pathValue.isEmpty()) return null;
        String attrName = isXPath ? "Xpath" : "JsonPath";

        // 1. Resolve / create the page
        String pageName = sanitizeIdentifier(request.getName(), "APIRequest");
        StructuredDataORPage page = or.getPageByName(pageName);
        if (page == null) {
            page = or.addPage(pageName);
            if (page == null) {
                // Re-fetch (race or naming collision)
                page = or.getPageByName(pageName);
            }
            if (page == null) return null;
        }

        // 2. Re-use any existing object with the same path value
        for (ObjectGroup<StructuredDataORObject> grp : page.getObjectGroups()) {
            for (StructuredDataORObject obj : grp.getObjects()) {
                for (StructuredDataAttribute attr : obj.getAttributes()) {
                    if (attrName.equalsIgnoreCase(attr.getName())
                            && pathValue.equals(attr.getValue())) {
                        return new ResolvedStructuredDataObject(
                                WebOR.ORScope.PROJECT,
                                page.getName(),
                                obj.getName(),
                                grp);
                    }
                }
            }
        }

        // 3. Create a new object whose name reflects the path. Disambiguate on collision.
        String baseName = sanitizeIdentifier(deriveObjectNameFromPath(pathValue), "Element");
        String objectName = baseName;
        int suffix = 2;
        while (page.getObjectGroupByName(objectName) != null) {
            objectName = baseName + "_" + suffix++;
            if (suffix > 1000) return null; // safety
        }

        ObjectGroup<StructuredDataORObject> group = page.addObjectGroup(objectName);
        if (group == null) {
            group = page.getObjectGroupByName(objectName);
            if (group == null) return null;
        }
        StructuredDataORObject obj = group.getObjectByName(objectName);
        if (obj == null && !group.getObjects().isEmpty()) {
            obj = group.getObjects().get(0);
        }
        if (obj == null) return null;

        // 4. Set the correct attribute (JsonPath or Xpath) value
        obj.addOrUpdateAttribute(attrName, pathValue);

        // 5. Persist the page so the new object survives across restarts
        or.setSaved(false);
        try {
            project.getObjectRepository().saveStructuredDataPageNow(page);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to save Structured Data page '" + page.getName() + "'", e);
        }

        return new ResolvedStructuredDataObject(
                WebOR.ORScope.PROJECT,
                page.getName(),
                objectName,
                group);
    }

    private void refreshStructuredDataTree() {
        try {
            if (mainFrame.getTestDesign() != null
                    && mainFrame.getTestDesign().getObjectRepo() != null
                    && mainFrame.getTestDesign().getObjectRepo().getStructuredDataOR() != null) {
                mainFrame.getTestDesign().getObjectRepo().getStructuredDataOR().load();
            }
        } catch (Exception e) {
            LOG.log(Level.FINE, "Could not refresh Structured Data OR tree", e);
        }
    }

    /**
     * Wraps an expected-value literal with the engine's {@code @} prefix so it
     * is treated as a literal instead of a data-sheet reference. Variable
     * expressions ({@code %var%}) and sheet references ({@code sheet:col}) are
     * left untouched.
     */
    private String prefixAtForLiteral(String value) {
        if (value == null) return "";
        if (value.isEmpty()) return value;
        if (value.charAt(0) == '@') return value;
        if (value.matches("%.+%")) return value;
        if (value.contains(":") && !value.contains(" ")) return value; // sheet:col
        return "@" + value;
    }

    /**
     * Sanitize an arbitrary string into a valid OR identifier (letters, digits,
     * underscore; must not start with a digit).
     */
    private String sanitizeIdentifier(String s, String fallback) {
        if (s == null) return fallback;
        String trimmed = s.trim();
        if (trimmed.isEmpty()) return fallback;
        String cleaned = trimmed.replaceAll("[^A-Za-z0-9_]+", "_")
                                .replaceAll("_+", "_")
                                .replaceAll("^_+|_+$", "");
        if (cleaned.isEmpty()) return fallback;
        if (Character.isDigit(cleaned.charAt(0))) {
            cleaned = "_" + cleaned;
        }
        return cleaned;
    }

    /**
     * Turn a JSON path / XPath expression into a readable object name.
     * Examples: {@code $.user.name}      -> {@code user_name}
     *           {@code $.items[2].id}    -> {@code items_2_id}
     *           {@code /root/item[1]/x}  -> {@code root_item_1_x}
     */
    private String deriveObjectNameFromPath(String path) {
        if (path == null || path.isEmpty()) return "Element";
        String s = path;
        if (s.startsWith("$.")) s = s.substring(2);
        else if (s.startsWith("$")) s = s.substring(1);
        // JSONPath bracket notation
        s = s.replace("['", ".").replace("']", "");
        // XPath separators
        s = s.replace("/", ".");
        // Array / index markers
        s = s.replace("[", "_").replace("]", "");
        return s;
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

        javax.swing.SwingUtilities.invokeLater(
            () -> {
                // Switch to Test Design view
                mainFrame.showTestDesign();

                // Load the test case in the Test Case Component
                if (mainFrame.getTestDesign() != null) {
                    mainFrame
                        .getTestDesign()
                        .getTestCaseComp()
                        .loadTableModelForSelection(testCase);
                }
            }
        );
    }
}

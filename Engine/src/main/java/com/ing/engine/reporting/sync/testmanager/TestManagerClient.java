package com.ing.engine.reporting.sync.testmanager;

import com.ing.engine.support.DLogger;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * REST client for the LambdaTest "Test Manager" Test Management module.
 *
 * Implements the API surface described in the integration plan:
 *  - Step 1a / 1b / 1b.1 / 1b.2 — Scenario folders + Test Cases
 *  - Step 1c / 1d              — Release + Test Set folders
 *  - Step 2 / 3a / 3b / 3c / 4 — Test Run lifecycle
 *
 * Each method is intentionally narrow and returns either a resolved ID or
 * a typed result so the orchestration in {@link TestManagerSync} stays small
 * and readable. Failures surface as {@link TestManagerApiException}.
 */
public class TestManagerClient {

    private static final Logger LOGGER = Logger.getLogger(TestManagerClient.class.getName());

    private final TestManagerHttpClient httpClient;
    private final String serverUrl;

    public TestManagerClient(String url, String username, String apiToken, Map config) {
        this.serverUrl = normalize(url);
        this.httpClient = new TestManagerHttpClient(toUrl(serverUrl), username, apiToken, config);
    }

    // -------------------------------------------------------------------
    // Test Connection (wired to the IDE button)
    // -------------------------------------------------------------------

    /**
     * Verifies connectivity and credentials by issuing
     * {@code GET <base>/projects/{projectId}} and accepting only a 2xx response.
     */
    public boolean isConnected(String projectId) {
        if (projectId == null || projectId.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "Test Manager: ProjectId is not configured");
            return false;
        }
        URL target = toUrl(serverUrl + "projects/" + projectId.trim());
        if (target == null) {
            return false;
        }
        HttpGet req = new HttpGet();
        try {
            req.setURI(target.toURI());
            httpClient.setHeader(req);
            HttpResponse response = httpClient.execute(req);
            int status = response.getStatusLine().getStatusCode();
            String body = response.getEntity() != null
                    ? EntityUtils.toString(response.getEntity()) : "";
            if (status >= 200 && status < 300) {
                DLogger.Log("Test Manager connection OK [" + status + "] for project " + projectId);
                return true;
            }
            LOGGER.log(Level.WARNING,
                    "Test Manager connection failed [{0}] for project {1}: {2}",
                    new Object[]{status, projectId, body});
            return false;
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Test Manager connection failed: " + ex.getMessage(), ex);
            return false;
        } finally {
            req.releaseConnection();
        }
    }

    // -------------------------------------------------------------------
    // Step 1a — Scenario folder (entity_type = project)
    // -------------------------------------------------------------------

    /** Returns the folder id matching {@code scenarioName}, or {@code null} if not found. */
    public String findScenarioFolderId(String projectId, String scenarioName) throws TestManagerApiException {
        JSONObject res = get("folder/entity/" + projectId);
        return findFolderRecursive(asArray(res.get("data")), scenarioName);
    }

    /** Creates a scenario folder under the project and returns its id. */
    public String createScenarioFolder(String projectId, String scenarioName) throws TestManagerApiException {
        JSONObject folder = new JSONObject();
        folder.put("name", scenarioName);
        folder.put("entity_id", projectId);
        folder.put("entity_type", "project");
        JSONArray folders = new JSONArray();
        folders.add(folder);
        JSONObject payload = new JSONObject();
        payload.put("folders", folders);

        JSONObject res = post("folder", payload.toJSONString());
        return requireId(res, "createScenarioFolder");
    }

    // -------------------------------------------------------------------
    // Step 1b — Test case lookup + create
    // -------------------------------------------------------------------

    /** Returns the test_case_id with title {@code testCaseName} in the folder, or {@code null}. */
    public String findTestCaseId(String projectId, String folderId, String testCaseName)
            throws TestManagerApiException {
        JSONObject res = get("projects/" + projectId + "/folder/" + folderId + "/test-cases");
        JSONArray data = asArray(res.get("data"));
        for (Object item : data) {
            if (!(item instanceof JSONObject)) {
                continue;
            }
            JSONObject tc = (JSONObject) item;
            if (testCaseName.equals(string(tc.get("title")))) {
                String id = string(tc.get("test_case_id"));
                return id != null ? id : string(tc.get("id"));
            }
        }
        return null;
    }

    /** Creates a single test case in the given folder and returns its id. */
    public String createTestCase(String projectId, String folderId, String testCaseName)
            throws TestManagerApiException {
        JSONObject testCase = new JSONObject();
        testCase.put("title", testCaseName);
        JSONArray testCases = new JSONArray();
        testCases.add(testCase);
        JSONObject payload = new JSONObject();
        payload.put("project_id", projectId);
        payload.put("folder_id", folderId);
        payload.put("test_cases", testCases);

        JSONObject res = post("test-cases", payload.toJSONString());
        Object id = res != null ? res.get("id") : null;
        if (id instanceof JSONArray && !((JSONArray) id).isEmpty()) {
            return string(((JSONArray) id).get(0));
        }
        if (id != null) {
            return string(id);
        }
        throw new TestManagerApiException("createTestCase: missing id in response: " + res);
    }

    // -------------------------------------------------------------------
    // Step 1b.1 / 1b.2 — snapshot + metadata update (optional)
    // -------------------------------------------------------------------

    public String getTestCaseSnapshotId(String testCaseId) throws TestManagerApiException {
        JSONObject res = getV2("test-cases/" + testCaseId);
        JSONObject data = (JSONObject) res.get("data");
        if (data == null) {
            throw new TestManagerApiException("getTestCaseSnapshotId: missing data block");
        }
        return string(data.get("snapshot_id"));
    }

    /** Best-effort metadata update. Returns the new snapshot id, or {@code null} on stale conflict. */
    public String updateTestCaseMetadata(String projectId, String testCaseId, String title,
                                         String status, String automationStatus, String priority,
                                         Collection<String> tags, String snapshotId)
            throws TestManagerApiException {
        JSONObject payload = new JSONObject();
        payload.put("title", title);
        payload.put("id", testCaseId);
        payload.put("project_id", projectId);
        payload.put("status", status);
        payload.put("automation_status", automationStatus);
        if (priority != null && !priority.isEmpty()) {
            payload.put("priority", priority);
        }
        if (tags != null) {
            JSONArray jsonTags = new JSONArray();
            jsonTags.addAll(tags);
            payload.put("tags", jsonTags);
        }
        payload.put("commit_message", "Updated by INGenious");
        payload.put("snapshot_id", snapshotId);
        payload.put("override", false);

        JSONObject res = putV2("test-cases", payload.toJSONString());
        JSONObject data = res != null ? (JSONObject) res.get("data") : null;
        return data != null ? string(data.get("snapshot_id")) : null;
    }

    // -------------------------------------------------------------------
    // Step 1c — Release folder (test-run scope)
    // -------------------------------------------------------------------

    public String findReleaseFolderId(String projectId, String releaseName) throws TestManagerApiException {
        JSONObject res = get("folder/test-run/entity/" + projectId);
        return findFolderTopLevel(asArray(res.get("data")), releaseName);
    }

    public String createReleaseFolder(String projectId, String releaseName) throws TestManagerApiException {
        JSONObject folder = new JSONObject();
        folder.put("name", releaseName);
        folder.put("description", "");
        folder.put("entity_id", projectId);
        JSONArray folders = new JSONArray();
        folders.add(folder);
        JSONObject payload = new JSONObject();
        payload.put("folders", folders);

        JSONObject res = post("folder/test-run", payload.toJSONString());
        return requireId(res, "createReleaseFolder");
    }

    // -------------------------------------------------------------------
    // Step 1d — Test Set folder (child of release)
    // -------------------------------------------------------------------

    public String findTestSetFolderId(String projectId, String releaseFolderId, String testSetName)
            throws TestManagerApiException {
        JSONObject res = get("folder/test-run/entity/" + projectId);
        for (Object item : asArray(res.get("data"))) {
            if (!(item instanceof JSONObject)) {
                continue;
            }
            JSONObject release = (JSONObject) item;
            if (!releaseFolderId.equals(string(release.get("id")))) {
                continue;
            }
            for (Object child : asArray(release.get("children"))) {
                if (!(child instanceof JSONObject)) {
                    continue;
                }
                JSONObject c = (JSONObject) child;
                if (testSetName.equals(string(c.get("name")))) {
                    return string(c.get("id"));
                }
            }
        }
        return null;
    }

    public String createTestSetFolder(String projectId, String releaseFolderId, String testSetName)
            throws TestManagerApiException {
        JSONObject folder = new JSONObject();
        folder.put("name", testSetName);
        folder.put("entity_id", projectId);
        folder.put("parent_id", releaseFolderId);
        JSONArray folders = new JSONArray();
        folders.add(folder);
        JSONObject payload = new JSONObject();
        payload.put("folders", folders);

        JSONObject res = post("folder/test-run", payload.toJSONString());
        return requireId(res, "createTestSetFolder");
    }

    // -------------------------------------------------------------------
    // Step 2 — Create Test Run
    // -------------------------------------------------------------------

    public String createTestRun(String projectId, String testSetFolderId, String runTitle)
            throws TestManagerApiException {
        JSONObject payload = new JSONObject();
        payload.put("title", runTitle);
        payload.put("project_id", projectId);
        payload.put("folder_id", testSetFolderId);
        JSONObject res = post("test-run", payload.toJSONString());
        return requireId(res, "createTestRun");
    }

    // -------------------------------------------------------------------
    // Step 3a — Add test cases (folder_selections)
    // -------------------------------------------------------------------

    public void addTestCasesToRun(String projectId, String testRunId, String runTitle,
                                  Map<String, ? extends Collection<String>> testCaseIdsByScenarioFolder)
            throws TestManagerApiException {
        JSONObject folderSelections = new JSONObject();
        for (Map.Entry<String, ? extends Collection<String>> e : testCaseIdsByScenarioFolder.entrySet()) {
            JSONArray ids = new JSONArray();
            ids.addAll(e.getValue());
            JSONObject entry = new JSONObject();
            entry.put("selected_testIds", ids);
            folderSelections.put(e.getKey(), entry);
        }
        JSONObject payload = new JSONObject();
        payload.put("title", runTitle);
        payload.put("objective", "");
        payload.put("folder_selections", folderSelections);
        payload.put("project_id", projectId);
        expectSuccess(put("test-run/" + testRunId, payload.toJSONString()), "addTestCasesToRun");
    }

    // -------------------------------------------------------------------
    // Step 3b — Fetch instance ids
    // -------------------------------------------------------------------

    /** Returns a list of (testCaseId, instanceId) pairs for the run. */
    public List<TestCaseInstance> getRunInstances(String testRunId) throws TestManagerApiException {
        JSONObject res = get("test-run/instances/" + testRunId);
        JSONObject envelope = (JSONObject) res.get("test_run_instances");
        JSONArray data = envelope != null ? asArray(envelope.get("data")) : asArray(res.get("data"));
        List<TestCaseInstance> out = new ArrayList<>();
        for (Object item : data) {
            if (!(item instanceof JSONObject)) {
                continue;
            }
            JSONObject inst = (JSONObject) item;
            String instanceId = string(inst.get("id"));
            String testCaseId = string(inst.get("test_case_id"));
            if (instanceId != null && testCaseId != null) {
                out.add(new TestCaseInstance(testCaseId, instanceId));
            }
        }
        return out;
    }

    // -------------------------------------------------------------------
    // Step 3c — Per-instance status
    // -------------------------------------------------------------------

    public void updateInstanceStatus(String instanceId, String status) throws TestManagerApiException {
        JSONObject payload = new JSONObject();
        payload.put("status", status);
        expectSuccess(put("test-run/instance/" + instanceId, payload.toJSONString()),
                "updateInstanceStatus(" + instanceId + ")");
    }

    // -------------------------------------------------------------------
    // Step 4 — Overall run status
    // -------------------------------------------------------------------

    public void updateRunStatus(String testRunId, String status) throws TestManagerApiException {
        JSONObject payload = new JSONObject();
        payload.put("status", status);
        expectSuccess(put("test-run/status/" + testRunId, payload.toJSONString()),
                "updateRunStatus(" + testRunId + ")");
    }

    // -------------------------------------------------------------------
    // Internals
    // -------------------------------------------------------------------

    private JSONObject get(String pathFromV1) throws TestManagerApiException {
        return invoke("GET", serverUrl + pathFromV1, null);
    }

    private JSONObject getV2(String pathFromV2) throws TestManagerApiException {
        return invoke("GET", v2Base() + pathFromV2, null);
    }

    private JSONObject post(String pathFromV1, String body) throws TestManagerApiException {
        return invoke("POST", serverUrl + pathFromV1, body);
    }

    private JSONObject put(String pathFromV1, String body) throws TestManagerApiException {
        return invoke("PUT", serverUrl + pathFromV1, body);
    }

    private JSONObject putV2(String pathFromV2, String body) throws TestManagerApiException {
        return invoke("PUT", v2Base() + pathFromV2, body);
    }

    private JSONObject invoke(String method, String url, String body) throws TestManagerApiException {
        URL target = toUrl(url);
        if (target == null) {
            throw new TestManagerApiException("Invalid URL: " + url);
        }
        try {
            switch (method) {
                case "GET":
                    return httpClient.Get(target);
                case "POST":
                    return httpClient.post(target, body == null ? "" : body);
                case "PUT":
                    return httpClient.put(target, body == null ? "" : body);
                default:
                    throw new TestManagerApiException("Unsupported method: " + method);
            }
        } catch (TestManagerApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new TestManagerApiException(method + " " + url + " failed: " + ex.getMessage(), ex);
        }
    }

    /** Derives the v2 base URL from the configured v1 base. */
    private String v2Base() {
        if (serverUrl.endsWith("/api/v1/")) {
            return serverUrl.substring(0, serverUrl.length() - "v1/".length()) + "v2/";
        }
        return serverUrl.replace("/api/v1/", "/api/v2/");
    }

    private static String requireId(JSONObject res, String op) throws TestManagerApiException {
        if (res == null) {
            throw new TestManagerApiException(op + ": null response");
        }
        Object id = res.get("id");
        if (id == null) {
            throw new TestManagerApiException(op + ": missing id in response: " + res);
        }
        return string(id);
    }

    private static void expectSuccess(JSONObject res, String op) throws TestManagerApiException {
        if (res == null) {
            throw new TestManagerApiException(op + ": null response");
        }
        Object type = res.get("type");
        if (type != null && !"Success".equalsIgnoreCase(type.toString())) {
            throw new TestManagerApiException(op + ": " + res);
        }
    }

    private static String findFolderRecursive(JSONArray data, String name) {
        if (data == null) {
            return null;
        }
        for (Object item : data) {
            if (!(item instanceof JSONObject)) {
                continue;
            }
            JSONObject f = (JSONObject) item;
            if (name.equals(string(f.get("name")))) {
                return string(f.get("id"));
            }
            String childMatch = findFolderRecursive(asArray(f.get("children")), name);
            if (childMatch != null) {
                return childMatch;
            }
        }
        return null;
    }

    private static String findFolderTopLevel(JSONArray data, String name) {
        if (data == null) {
            return null;
        }
        for (Object item : data) {
            if (!(item instanceof JSONObject)) {
                continue;
            }
            JSONObject f = (JSONObject) item;
            if (name.equals(string(f.get("name")))) {
                return string(f.get("id"));
            }
        }
        return null;
    }

    private static JSONArray asArray(Object obj) {
        if (obj instanceof JSONArray) {
            return (JSONArray) obj;
        }
        return new JSONArray();
    }

    private static String string(Object v) {
        return v == null ? null : v.toString();
    }

    private static String normalize(String url) {
        if (url == null || url.isEmpty()) {
            return "";
        }
        return url.endsWith("/") ? url : url + "/";
    }

    private static URL toUrl(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException ex) {
            LOGGER.log(Level.SEVERE, "Bad URL: " + url, ex);
            return null;
        }
    }

    /** Simple value object returned by {@link #getRunInstances}. */
    public static final class TestCaseInstance {
        public final String testCaseId;
        public final String instanceId;

        TestCaseInstance(String testCaseId, String instanceId) {
            this.testCaseId = testCaseId;
            this.instanceId = instanceId;
        }
    }
}


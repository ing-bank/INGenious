package com.ing.engine.reporting.sync.testmanager;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Mutable per-execution state shared across the Test Manager workflow steps.
 *
 * Stored at instance level on {@link TestManagerSync}; cleared once the
 * publish completes (or aborts). Holds the resolved IDs so subsequent steps
 * can be looked up cheaply without re-querying the API.
 */
final class TestManagerRunContext {
    final String correlationId;
    final String projectId;
    final String releaseName;
    final String testSetName;
    final String runTitle;

    String releaseFolderId;
    String testSetFolderId;
    String testRunId;

    /** scenarioName -> scenarioFolderId */
    final Map<String, String> scenarioFolderIds = new LinkedHashMap<>();

    /** scenarioFolderId -> set of testCaseIds (for folder_selections payload) */
    final Map<String, Set<String>> testCaseIdsByScenarioFolder = new LinkedHashMap<>();

    /** "scenario||testCase" -> testCaseId */
    final Map<String, String> testCaseIdByName = new LinkedHashMap<>();

    /** testCaseId -> testCaseInstanceId(s) returned by Step 3b */
    final Map<String, Set<String>> instanceIdsByTestCaseId = new LinkedHashMap<>();

    TestManagerRunContext(
        String correlationId,
        String projectId,
        String releaseName,
        String testSetName,
        String runTitle
    ) {
        this.correlationId = correlationId;
        this.projectId = projectId;
        this.releaseName = releaseName;
        this.testSetName = testSetName;
        this.runTitle = runTitle;
    }

    void recordTestCase(
        String scenarioName,
        String scenarioFolderId,
        String testCaseName,
        String testCaseId
    ) {
        scenarioFolderIds.put(scenarioName, scenarioFolderId);
        testCaseIdByName.put(key(scenarioName, testCaseName), testCaseId);
        testCaseIdsByScenarioFolder
            .computeIfAbsent(scenarioFolderId, k -> new LinkedHashSet<>())
            .add(testCaseId);
    }

    String getTestCaseId(String scenarioName, String testCaseName) {
        return testCaseIdByName.get(key(scenarioName, testCaseName));
    }

    private static String key(String scenario, String testCase) {
        return scenario + "||" + testCase;
    }
}

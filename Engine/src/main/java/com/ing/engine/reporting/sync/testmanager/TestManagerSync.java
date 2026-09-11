package com.ing.engine.reporting.sync.testmanager;

import com.ing.datalib.model.Tag;
import com.ing.datalib.model.Tags;
import com.ing.engine.core.Control;
import com.ing.engine.core.RunManager;
import com.ing.engine.reporting.sync.Sync;
import com.ing.engine.reporting.util.TestInfo;
import java.io.File;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONObject;

/**
 * Test Manager Sync facade.
 *
 * Result rows are queued in {@link #updateResults} during execution and
 * published in a single workflow at {@link #disConnect} (called from
 * {@code Control.endExecution()}). Workflow:
 *
 *   1a / 1b      — Find or create scenario folder + test case (per unique pair)
 *   1b.2         — For newly created test cases, mark them Automated
 *   1c           — Find or create release folder
 *   1d           — Find or create test set folder under the release
 *   2            — Create the Test Run
 *   3a           — Attach the discovered test cases to the run
 *   3b           — Fetch run instances to learn each test_case_instance_id
 *   3c           — Update each instance with Passed / Failed
 *   4            — Update overall run status
 */
public class TestManagerSync implements Sync {
    private static final Logger LOG = Logger.getLogger(TestManagerSync.class.getName());

    private static final String STATUS_PASSED = "Passed";
    private static final String STATUS_FAILED = "Failed";

    /**
     * Hardcoded metadata applied to test cases freshly created by INGenious.
     * Tags are NOT hardcoded — they are read per-test-case from the INGenious project
     * (see {@link #tagsFor(String, String)}).
     */
    private static final String TC_STATUS = "Ready";
    private static final String TC_AUTOMATION_STATUS = "Automated";
    private static final String TC_PRIORITY = "Medium";

    private TestManagerClient client;
    private final String projectId;
    private final List<TestManagerTestData> queued = new ArrayList<>();

    public TestManagerSync(
        String url,
        String username,
        String apiToken,
        String projectId,
        Map config
    ) {
        this.projectId = projectId == null ? "" : projectId;
        this.client = new TestManagerClient(url, username, apiToken, config);
    }

    /** Called by TMIntegration with the decrypted module Properties. */
    public TestManagerSync(Properties options) {
        this(
            options.getProperty("TestManager URL"),
            options.getProperty("Username"),
            options.getProperty("AccessKey"),
            options.getProperty("ProjectId"),
            options
        );
    }

    @Override
    public String getModule() {
        return "Test Manager";
    }

    @Override
    public boolean isConnected() {
        try {
            return client.isConnected(projectId);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Test Manager isConnected() failed", ex);
            return false;
        }
    }

    @Override
    public boolean updateResults(TestInfo tc, String status, List<File> attach) {
        queued.add(
            new TestManagerTestData(projectId, "", tc.testScenario, tc.testCase, status, attach)
        );
        return true;
    }

    @Override
    public void disConnect() {
        try {
            publish();
        } catch (Exception ex) {
            banner("Test Manager publish FAILED: " + ex.getMessage(), "❌");
            LOG.log(Level.SEVERE, "Test Manager publish failed: " + ex.getMessage(), ex);
        } finally {
            queued.clear();
            client = null;
        }
    }

    @Override
    public String createIssue(JSONObject issue, List<File> attach) {
        throw new UnsupportedOperationException(
            "Defect creation not supported by Test Manager module yet."
        );
    }

    // -------------------------------------------------------------------
    // Workflow
    // -------------------------------------------------------------------

    private void publish() throws TestManagerApiException {
        if (queued.isEmpty()) {
            info("Nothing to publish (result queue is empty).");
            return;
        }
        if (projectId.isEmpty()) {
            warn("ProjectId is not configured – skipping Test Manager publish.");
            return;
        }
        if (!Control.exe.getExecSettings().getRunSettings().isGridExecution()) {
            return;
        }

        // Silence noisy cookie warnings from Apache HttpClient so the report stays clean.
        silenceHttpClientNoise();

        String release = safeName(RunManager.getGlobalSettings().getRelease(), "Default Release");
        String testSet = safeName(RunManager.getGlobalSettings().getTestSet(), "Default Test Set");
        String runTitle = "Run_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String correlationId = UUID.randomUUID().toString();

        TestManagerRunContext ctx = new TestManagerRunContext(
            correlationId,
            projectId,
            release,
            testSet,
            runTitle
        );

        println("");
        println(
            "\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550"
        );
        println("  \uD83D\uDCE1  Test Manager — publishing results");
        println(
            "\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500"
        );
        println("  Project    : " + projectId);
        println("  Release    : " + release);
        println("  Test Set   : " + testSet);
        println("  Run Title  : " + runTitle);
        println("  Queued     : " + queued.size() + " result(s)");
        println(
            "\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550"
        );

        // Step 1a/1b/1b.2 — resolve scenario folders + test cases + metadata.
        section("Step 1 — Resolving scenarios & test cases");
        resolveScenariosAndTestCases(ctx);

        // Step 1c — release folder.
        section("Step 1c — Release folder");
        ctx.releaseFolderId = client.findReleaseFolderId(projectId, release);
        if (ctx.releaseFolderId == null) {
            ctx.releaseFolderId = client.createReleaseFolder(projectId, release);
            item("created release folder", release, ctx.releaseFolderId);
        } else {
            item("reused release folder", release, ctx.releaseFolderId);
        }

        // Step 1d — test set folder.
        section("Step 1d — Test Set folder");
        ctx.testSetFolderId = client.findTestSetFolderId(projectId, ctx.releaseFolderId, testSet);
        if (ctx.testSetFolderId == null) {
            ctx.testSetFolderId =
                client.createTestSetFolder(projectId, ctx.releaseFolderId, testSet);
            item("created test set folder", testSet, ctx.testSetFolderId);
        } else {
            item("reused test set folder", testSet, ctx.testSetFolderId);
        }

        // Step 2 — create the test run.
        section("Step 2 — Creating Test Run");
        ctx.testRunId = client.createTestRun(projectId, ctx.testSetFolderId, runTitle);
        item("created test run", runTitle, ctx.testRunId);

        // Step 3a — attach test cases.
        section("Step 3a — Adding test cases to the run");
        client.addTestCasesToRun(
            projectId,
            ctx.testRunId,
            runTitle,
            ctx.testCaseIdsByScenarioFolder
        );
        println(
            "    \u2022 added " +
            countTestCases(ctx) +
            " test case(s) across " +
            ctx.testCaseIdsByScenarioFolder.size() +
            " folder(s)"
        );

        // Step 3b — fetch instance ids.
        section("Step 3b — Fetching run instances");
        List<TestManagerClient.TestCaseInstance> instances = client.getRunInstances(ctx.testRunId);
        for (TestManagerClient.TestCaseInstance inst : instances) {
            ctx
                .instanceIdsByTestCaseId.computeIfAbsent(
                    inst.testCaseId,
                    k -> new LinkedHashSet<>()
                )
                .add(inst.instanceId);
        }
        println("    \u2022 received " + instances.size() + " instance(s)");

        // Step 3c — per-instance status (tolerate per-row failures).
        section("Step 3c — Updating per-test-case status");
        int updated = 0;
        int failedUpdates = 0;
        boolean anyFailed = false;
        for (TestManagerTestData row : queued) {
            String mapped = mapStatus(row.status);
            if (STATUS_FAILED.equals(mapped)) {
                anyFailed = true;
            }
            String testCaseId = ctx.getTestCaseId(row.suite, row.testcase);
            if (testCaseId == null) {
                warn(
                    "no test_case_id resolved for " +
                    row.suite +
                    " / " +
                    row.testcase +
                    " — skipped"
                );
                failedUpdates++;
                continue;
            }
            Set<String> instanceIds = ctx.instanceIdsByTestCaseId.get(testCaseId);
            if (instanceIds == null || instanceIds.isEmpty()) {
                warn(
                    "no instance for " +
                    row.suite +
                    " / " +
                    row.testcase +
                    " (testCaseId=" +
                    testCaseId +
                    ")"
                );
                failedUpdates++;
                continue;
            }
            for (String instanceId : instanceIds) {
                try {
                    client.updateInstanceStatus(instanceId, mapped);
                    println(
                        "    " +
                        statusIcon(mapped) +
                        "  " +
                        row.suite +
                        " / " +
                        row.testcase +
                        "  \u2192  " +
                        mapped
                    );
                    updated++;
                } catch (TestManagerApiException ex) {
                    failedUpdates++;
                    warn(
                        "failed to update instance " +
                        instanceId +
                        " (" +
                        row.suite +
                        " / " +
                        row.testcase +
                        "): " +
                        ex.getMessage()
                    );
                }
            }
        }
        println("    \u2022 updates ok=" + updated + "  failed=" + failedUpdates);

        // Step 4 — overall run status.
        section("Step 4 — Setting overall run status");
        String overall = anyFailed ? STATUS_FAILED : STATUS_PASSED;
        client.updateRunStatus(ctx.testRunId, overall);
        println("    " + statusIcon(overall) + "  run status: " + overall);

        println("");
        println(
            "\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550"
        );
        println("  " + statusIcon(overall) + "  Test Manager publish complete");
        println("  Run id     : " + ctx.testRunId);
        println("  Status     : " + overall);
        println(
            "\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550"
        );
        println("");

        // Detailed correlation id kept in the structured log for support / debugging.
        LOG.log(
            Level.FINE,
            "Test Manager publish done: cid={0} runId={1} status={2}",
            new Object[] { correlationId, ctx.testRunId, overall }
        );
    }

    private void resolveScenariosAndTestCases(TestManagerRunContext ctx)
        throws TestManagerApiException {
        // Collect unique scenarios and (scenario, testCase) pairs preserving order.
        Set<String> scenarios = new LinkedHashSet<>();
        Map<String, Set<String>> testCasesByScenario = new LinkedHashMap<>();
        for (TestManagerTestData row : queued) {
            scenarios.add(row.suite);
            testCasesByScenario
                .computeIfAbsent(row.suite, k -> new LinkedHashSet<>())
                .add(row.testcase);
        }

        // Step 1a — folder per scenario.
        for (String scenario : scenarios) {
            String folderId = client.findScenarioFolderId(projectId, scenario);
            if (folderId == null) {
                folderId = client.createScenarioFolder(projectId, scenario);
                item("created scenario folder", scenario, folderId);
            } else {
                item("reused scenario folder", scenario, folderId);
            }
            ctx.scenarioFolderIds.put(scenario, folderId);
        }

        // Step 1b — test case per (scenario, name). Step 1b.2 — mark newly-created ones as Automated.
        for (Map.Entry<String, Set<String>> e : testCasesByScenario.entrySet()) {
            String scenario = e.getKey();
            String folderId = ctx.scenarioFolderIds.get(scenario);
            for (String tcName : e.getValue()) {
                boolean created = false;
                String testCaseId = client.findTestCaseId(projectId, folderId, tcName);
                if (testCaseId == null) {
                    testCaseId = client.createTestCase(projectId, folderId, tcName);
                    created = true;
                    item("created test case", scenario + " / " + tcName, testCaseId);
                } else {
                    item("reused test case", scenario + " / " + tcName, testCaseId);
                }
                ctx.recordTestCase(scenario, folderId, tcName, testCaseId);

                if (created) {
                    markAutomated(testCaseId, scenario, tcName);
                }
            }
        }
    }

    /**
     * Step 1b.2 — best-effort metadata update for a freshly-created test case.
     * Status / automation status / priority are hardcoded; tags are sourced
     * dynamically from the INGenious project for this (scenario, testCase).
     * Failures are logged but do not abort the publish.
     */
    private void markAutomated(String testCaseId, String scenarioName, String testCaseName) {
        try {
            String snapshotId = client.getTestCaseSnapshotId(testCaseId);
            if (snapshotId == null || snapshotId.isEmpty()) {
                warn(
                    "no snapshot_id for newly created test case " +
                    testCaseName +
                    " (" +
                    testCaseId +
                    ") — metadata update skipped"
                );
                return;
            }
            List<String> tcTags = tagsFor(scenarioName, testCaseName);
            String newSnapshot = client.updateTestCaseMetadata(
                projectId,
                testCaseId,
                testCaseName,
                TC_STATUS,
                TC_AUTOMATION_STATUS,
                TC_PRIORITY,
                tcTags,
                snapshotId
            );
            if (newSnapshot == null) {
                warn(
                    "metadata update for " +
                    testCaseName +
                    " returned no snapshot (stale conflict?)"
                );
            } else {
                String tagsLabel = tcTags.isEmpty() ? "" : "  tags=" + tcTags;
                println(
                    "    \uD83E\uDD16  marked " +
                    testCaseName +
                    " — status=" +
                    TC_STATUS +
                    ", automation=" +
                    TC_AUTOMATION_STATUS +
                    ", priority=" +
                    TC_PRIORITY +
                    tagsLabel
                );
            }
        } catch (TestManagerApiException ex) {
            warn("metadata update failed for " + testCaseName + ": " + ex.getMessage());
        }
    }

    /**
     * Resolves the INGenious tags attached to a specific test case by looking
     * the entry up in the project's {@code ProjectInfo} data section. Returns
     * an empty list if the project is not available or the test case has no
     * tags. Never throws — a missing project must not break the publish.
     */
    private static List<String> tagsFor(String scenarioName, String testCaseName) {
        try {
            if (
                Control.getCurrentProject() == null ||
                Control.getCurrentProject().getInfo() == null ||
                Control.getCurrentProject().getInfo().getData() == null
            ) {
                return Collections.emptyList();
            }
            Tags tags = Control
                .getCurrentProject()
                .getInfo()
                .getData()
                .findOrCreate(testCaseName, scenarioName)
                .getTags();
            if (tags == null || tags.isEmpty()) {
                return Collections.emptyList();
            }
            List<String> out = new ArrayList<>();
            for (Tag t : tags) {
                if (t == null) {
                    continue;
                }
                String v = t.getValue();
                if (v != null && !v.isEmpty()) {
                    out.add(v.startsWith("@") ? v.substring(1) : v);
                }
            }
            return out;
        } catch (Exception ex) {
            LOG.log(Level.FINE, "Tag lookup failed for " + scenarioName + "/" + testCaseName, ex);
            return Collections.emptyList();
        }
    }

    // -------------------------------------------------------------------
    // Output helpers (engine-style banners)
    // -------------------------------------------------------------------

    /**
     * All banner output flushes immediately so it cannot be lost on shutdown.
     * Resolves {@link System#out} lazily so the output passes through whatever
     * stream {@link com.ing.engine.reporting.impl.ConsoleReport} has installed —
     * that tee writes the same bytes to the terminal AND to the per-run
     * {@code console.txt} (the Console report).
     */
    private static void println(String line) {
        PrintStream out = System.out;
        out.println(line);
        out.flush();
    }

    private static void section(String title) {
        println("");
        println("  \u25B8 " + title);
    }

    private static void item(String action, String name, String id) {
        println("    \u2022 " + action + ": " + name + "  (id " + id + ")");
    }

    private static void info(String msg) {
        println("  \u2139  " + msg);
    }

    private static void warn(String msg) {
        println("  \u26A0  " + msg);
        LOG.log(Level.WARNING, msg);
    }

    private static void banner(String msg, String icon) {
        println("");
        println(
            "\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550"
        );
        println("  " + icon + "  " + msg);
        println(
            "\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550"
        );
        println("");
    }

    /**
     * The Test Manager API responses include cookies that Apache HttpClient cannot parse,
     * which spam the console with WARNINGs (one per request). Silence them so the publish
     * report remains readable.
     */
    private static volatile boolean httpNoiseSilenced;

    private static synchronized void silenceHttpClientNoise() {
        if (httpNoiseSilenced) {
            return;
        }
        Logger
            .getLogger("org.apache.http.client.protocol.ResponseProcessCookies")
            .setLevel(Level.SEVERE);
        Logger.getLogger("org.apache.http").setLevel(Level.SEVERE);
        httpNoiseSilenced = true;
    }

    private static String statusIcon(String status) {
        return STATUS_PASSED.equalsIgnoreCase(status) ? "✅" : "❌";
    }

    private static int countTestCases(TestManagerRunContext ctx) {
        int n = 0;
        for (Set<String> ids : ctx.testCaseIdsByScenarioFolder.values()) {
            n += ids.size();
        }
        return n;
    }

    private static String mapStatus(String engineStatus) {
        if (engineStatus == null) {
            return STATUS_FAILED;
        }
        String s = engineStatus.trim();
        if (
            s.equalsIgnoreCase("PASS") ||
            s.equalsIgnoreCase("PASSED") ||
            s.equalsIgnoreCase("SUCCESS") ||
            s.equalsIgnoreCase("OK")
        ) {
            return STATUS_PASSED;
        }
        return STATUS_FAILED;
    }

    private static String safeName(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }
}

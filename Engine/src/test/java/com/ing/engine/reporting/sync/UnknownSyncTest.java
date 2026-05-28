package com.ing.engine.reporting.sync;

import static org.assertj.core.api.Assertions.assertThat;

import com.ing.engine.reporting.util.TestInfo;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONObject;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests for Unknown (Sync interface no-op implementation).
 * Verifies the contract: isConnected → false, updateResults → false,
 * createIssue → null, getModule → "UNKNOWN", disConnect → no-op.
 */
public class UnknownSyncTest {

    private Unknown unknown;

    @BeforeMethod
    public void setUp() {
        unknown = new Unknown();
    }

    @Test
    public void testIsConnectedReturnsFalse() {
        assertThat(unknown.isConnected()).isFalse();
    }

    @Test
    public void testUpdateResultsReturnsFalse() {
        TestInfo testInfo = new TestInfo("sc", "tc", "desc", "1", "0s",
                "2024-01-01", "10:00", "Chromium", "120", "macOS",
                new java.util.Date(), new java.util.Date());
        List<File> attach = new ArrayList<>();
        assertThat(unknown.updateResults(testInfo, "PASS", attach)).isFalse();
    }

    @Test
    public void testCreateIssueReturnsNull() {
        JSONObject issue = new JSONObject();
        List<File> attach = new ArrayList<>();
        assertThat(unknown.createIssue(issue, attach)).isNull();
    }

    @Test
    public void testGetModuleReturnsUnknown() {
        assertThat(unknown.getModule()).isEqualTo("UNKNOWN");
    }

    @Test
    public void testDisConnectDoesNotThrow() {
        unknown.disConnect();
        // No exception = success (no-op)
    }

    @Test
    public void testImplementsSyncInterface() {
        assertThat(unknown).isInstanceOf(Sync.class);
    }
}

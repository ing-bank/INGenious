package com.ing.engine.commands.browser;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests for RequestFulfill — endpoint map management via the static
 * {@code Command.mockEndPoints} map.
 * <p>
 * The actual handlePayloadorEndpoint / handleDataSheetVariables methods
 * are tightly coupled to Control.getCurrentProject(), so we test the
 * map-management aspects only.
 */
public class RequestFulfillTest {

    @BeforeMethod
    public void setUp() {
        Command.mockEndPoints.clear();
    }

    @AfterMethod
    public void tearDown() {
        Command.mockEndPoints.clear();
    }

    @Test
    public void testMockEndPointsMapInitiallyEmpty() {
        assertThat(Command.mockEndPoints).isEmpty();
    }

    @Test
    public void testMockEndPointsMapStoresEntry() {
        Command.mockEndPoints.put("key1", "https://api.example.com/users");
        assertThat(Command.mockEndPoints).containsEntry("key1", "https://api.example.com/users");
    }

    @Test
    public void testMockEndPointsMapOverwritesExistingKey() {
        Command.mockEndPoints.put("key1", "old-endpoint");
        Command.mockEndPoints.put("key1", "new-endpoint");
        assertThat(Command.mockEndPoints).hasSize(1);
        assertThat(Command.mockEndPoints.get("key1")).isEqualTo("new-endpoint");
    }

    @Test
    public void testMultipleEndpoints() {
        Command.mockEndPoints.put("k1", "/api/v1");
        Command.mockEndPoints.put("k2", "/api/v2");
        Command.mockEndPoints.put("k3", "/api/v3");

        assertThat(Command.mockEndPoints).hasSize(3);
    }

    @Test
    public void testMockEndPointsClearResetsMap() {
        Command.mockEndPoints.put("k", "v");
        Command.mockEndPoints.clear();
        assertThat(Command.mockEndPoints).isEmpty();
    }

    // ── Static response/request maps from Command ───────────────────────

    @Test
    public void testResponseBodiesMapOperations() {
        Command.responsebodies.clear();
        Command.responsebodies.put("key", "{\"status\":\"ok\"}");
        assertThat(Command.responsebodies.get("key")).isEqualTo("{\"status\":\"ok\"}");
        Command.responsebodies.clear();
    }

    @Test
    public void testResponseCodesMapOperations() {
        Command.responsecodes.clear();
        Command.responsecodes.put("key", "200");
        assertThat(Command.responsecodes.get("key")).isEqualTo("200");
        Command.responsecodes.clear();
    }

    @Test
    public void testHeadersMapOperations() {
        Command.headers.clear();
        var headerList = new java.util.ArrayList<String>();
        headerList.add("Content-Type=application/json");
        Command.headers.put("key", headerList);
        assertThat(Command.headers.get("key")).contains("Content-Type=application/json");
        Command.headers.clear();
    }

    @Test
    public void testUrlParamsMapOperations() {
        Command.urlParams.clear();
        var paramList = new java.util.ArrayList<String>();
        paramList.add("page=1");
        paramList.add("size=10");
        Command.urlParams.put("key", paramList);
        assertThat(Command.urlParams.get("key")).hasSize(2);
        Command.urlParams.clear();
    }
}

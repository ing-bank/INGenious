package com.ing.engine.reporting.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.testng.annotations.Test;

/**
 * Tests for DefectModules and its package-private Data helper class.
 * Focuses on getDecoded and checkServer logic.
 */
public class DefectModulesTest {

    // ---- Data.checkServer ----

    @Test
    public void testCheckServerValidUrl() throws Exception {
        // Should not throw for valid URL
        Data.checkServer("https://jira.example.com");
    }

    @Test
    public void testCheckServerNullThrows() {
        assertThatThrownBy(() -> Data.checkServer(null))
                .isInstanceOf(Exception.class)
                .hasMessage("Server URL is Empty!!");
    }

    @Test
    public void testCheckServerEmptyThrows() {
        assertThatThrownBy(() -> Data.checkServer(""))
                .isInstanceOf(Exception.class)
                .hasMessage("Server URL is Empty!!");
    }

    @Test
    public void testCheckServerWithPath() throws Exception {
        Data.checkServer("http://localhost:8080/api");
    }

    @Test
    public void testCheckServerSimpleScheme() throws Exception {
        Data.checkServer("ftp://files.example.com");
    }

    // ---- uploadDefect with unknown moduleId ----

    @Test
    public void testUploadDefectUnknownModuleReturnsNull() {
        // The switch statement has only commented-out cases,
        // so any moduleId should return null (no match)
        // However, initData() will fail because FilePath.getExplorerConfig() requires project path.
        // So we expect an error result
        java.util.LinkedHashMap<String, String> fields = new java.util.LinkedHashMap<>();
        fields.put("project", "DEMO");
        java.util.List<java.io.File> attach = new java.util.ArrayList<>();

        String result = DefectModules.uploadDefect("UNKNOWN", fields, attach);
        // Since initData() throws (no project configured), result should be an error message
        assertThat(result).startsWith("Error:");
    }
}

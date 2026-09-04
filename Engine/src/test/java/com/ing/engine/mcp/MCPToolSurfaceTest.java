package com.ing.engine.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.regex.Pattern;
import org.testng.annotations.Test;

/**
 * Conformance tests for the MCP tool surface: naming convention, schema
 * completeness and grammar-aware descriptions. New tools that bypass these
 * conventions fail here.
 */
public class MCPToolSurfaceTest {
    private static final Pattern TOOL_NAME = Pattern.compile("^ingenious_[a-z0-9_]+$");

    private JsonNode tools() {
        ObjectMapper json = new ObjectMapper();
        return new MCPTools(null).list(json).get("tools");
    }

    @Test
    public void everyToolFollowsNamingAndSchemaConventions() {
        JsonNode tools = tools();
        assertThat(tools.size()).isGreaterThanOrEqualTo(76);
        for (JsonNode t : tools) {
            String name = t.path("name").asText();
            assertThat(TOOL_NAME.matcher(name).matches())
                .as("tool name convention: %s", name)
                .isTrue();
            assertThat(t.path("description").asText()).as("description of %s", name).isNotEmpty();
            assertThat(t.path("inputSchema").path("type").asText())
                .as("inputSchema of %s", name)
                .isEqualTo("object");
        }
    }

    @Test
    public void parameterizeToolIsRegistered() {
        boolean found = false;
        for (JsonNode t : tools()) {
            if ("ingenious_testcase_parameterize".equals(t.path("name").asText())) {
                found = true;
                String desc = t.path("description").asText();
                assertThat(desc).contains("mode=scan");
                assertThat(desc).contains("Sheet:Column");
                assertThat(desc).contains("{Sheet:Column}");
            }
        }
        assertThat(found).isTrue();
    }

    @Test
    public void stepSchemaTeachesTheInputGrammar() {
        for (JsonNode t : tools()) {
            if (!"ingenious_testcase_create".equals(t.path("name").asText())) continue;
            JsonNode input = t
                .path("inputSchema")
                .path("properties")
                .path("steps")
                .path("items")
                .path("properties")
                .path("input");
            String desc = input.path("description").asText();
            assertThat(desc).contains("@-prefixed");
            assertThat(desc).contains("Sheet:Column");
            assertThat(desc).contains("{Sheet:Column}");
            return;
        }
        throw new AssertionError("ingenious_testcase_create not found");
    }

    @Test
    public void apiCollectionWorkflowToolsAreRegistered() {
        java.util.Set<String> names = new java.util.HashSet<>();
        for (JsonNode t : tools()) names.add(t.path("name").asText());
        assertThat(names)
            .contains(
                "ingenious_apicollection_import",
                "ingenious_apicollection_list",
                "ingenious_apicollection_show",
                "ingenious_apicollection_env_set",
                "ingenious_apicollection_run",
                "ingenious_apicollection_request_run",
                "ingenious_apicollection_to_testcase"
            );
    }
}

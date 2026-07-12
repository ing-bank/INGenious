package com.ing.engine.aicli.tools;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

public class ToolRegistryTest {

    @Test
    public void registryExposesFullMcpSurface() {
        ToolRegistry registry = ToolRegistry.create();
        assertThat(registry.all().size()).isGreaterThanOrEqualTo(75);
        assertThat(registry.get("testcase_create")).isNotNull();
        assertThat(registry.get("ingenious_testcase_create")).isNotNull();
        assertThat(registry.get("no_such_tool")).isNull();
    }

    @Test
    public void mutabilityClassification() {
        assertThat(ToolRegistry.classifyMutating("testcase_create")).isTrue();
        assertThat(ToolRegistry.classifyMutating("testcase_edit_step")).isTrue();
        assertThat(ToolRegistry.classifyMutating("object_import_page")).isTrue();
        assertThat(ToolRegistry.classifyMutating("gen_testcase")).isTrue();
        assertThat(ToolRegistry.classifyMutating("gen_from_openapi")).isTrue();
        assertThat(ToolRegistry.classifyMutating("data_set")).isTrue();
        assertThat(ToolRegistry.classifyMutating("report_export")).isTrue();
        assertThat(ToolRegistry.classifyMutating("browser_session_save")).isTrue();

        assertThat(ToolRegistry.classifyMutating("gen_list")).isFalse();
        assertThat(ToolRegistry.classifyMutating("testcase_list")).isFalse();
        assertThat(ToolRegistry.classifyMutating("testcase_validate")).isFalse();
        assertThat(ToolRegistry.classifyMutating("browser_session_start")).isFalse();
        assertThat(ToolRegistry.classifyMutating("run_status")).isFalse();
        assertThat(ToolRegistry.classifyMutating("doctor")).isFalse();
    }

    @Test
    public void categories() {
        assertThat(ToolRegistry.categoryOf("data_show", false)).isEqualTo("data");
        assertThat(ToolRegistry.categoryOf("env_create", true)).isEqualTo("data");
        assertThat(ToolRegistry.categoryOf("gen_testcase", true)).isEqualTo("generation");
        assertThat(ToolRegistry.categoryOf("import_curl", true)).isEqualTo("generation");
        assertThat(ToolRegistry.categoryOf("run_async", false)).isEqualTo("execution");
        assertThat(ToolRegistry.categoryOf("report_latest", false)).isEqualTo("reporting");
        assertThat(ToolRegistry.categoryOf("doctor", false)).isEqualTo("reporting");
        assertThat(ToolRegistry.categoryOf("browser_inspect", false)).isEqualTo("browser");
        assertThat(ToolRegistry.categoryOf("testcase_create", true)).isEqualTo("authoring");
        assertThat(ToolRegistry.categoryOf("testcase_list", false)).isEqualTo("discovery");
    }

    @Test
    public void promptCatalogListsEveryTool() {
        ToolRegistry registry = ToolRegistry.create();
        String catalog = registry.promptCatalog();
        for (Tool t : registry.all()) {
            assertThat(catalog).contains("- " + t.id() + "(");
        }
    }
}

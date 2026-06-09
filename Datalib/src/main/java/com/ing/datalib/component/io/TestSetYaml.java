package com.ing.datalib.component.io;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * YAML on-disk representation of a {@link com.ing.datalib.component.TestSet}.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonPropertyOrder({ "schemaVersion", "name", "release", "executions" })
public class TestSetYaml {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    @JsonProperty("schemaVersion")
    private Integer schemaVersion = CURRENT_SCHEMA_VERSION;

    @JsonProperty("name")
    private String name;

    @JsonProperty("release")
    private String release;

    @JsonProperty("executions")
    private List<ExecutionYaml> executions = new ArrayList<>();

    public Integer getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(Integer schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRelease() {
        return release;
    }

    public void setRelease(String release) {
        this.release = release;
    }

    public List<ExecutionYaml> getExecutions() {
        return executions;
    }

    public void setExecutions(List<ExecutionYaml> executions) {
        this.executions = executions == null ? new ArrayList<>() : executions;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonPropertyOrder(
        {
            "execute",
            "testScenario",
            "testCase",
            "iteration",
            "status",
            "browser",
            "browserVersion",
            "platform"
        }
    )
    public static class ExecutionYaml {
        @JsonProperty("execute")
        private Boolean execute;

        @JsonProperty("testScenario")
        private String testScenario;

        @JsonProperty("testCase")
        private String testCase;

        @JsonProperty("iteration")
        private String iteration;

        @JsonProperty("status")
        private String status;

        @JsonProperty("browser")
        private String browser;

        @JsonProperty("browserVersion")
        private String browserVersion;

        @JsonProperty("platform")
        private String platform;

        public Boolean getExecute() {
            return execute;
        }

        public void setExecute(Boolean execute) {
            this.execute = execute;
        }

        public String getTestScenario() {
            return testScenario;
        }

        public void setTestScenario(String testScenario) {
            this.testScenario = testScenario;
        }

        public String getTestCase() {
            return testCase;
        }

        public void setTestCase(String testCase) {
            this.testCase = testCase;
        }

        public String getIteration() {
            return iteration;
        }

        public void setIteration(String iteration) {
            this.iteration = iteration;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getBrowser() {
            return browser;
        }

        public void setBrowser(String browser) {
            this.browser = browser;
        }

        public String getBrowserVersion() {
            return browserVersion;
        }

        public void setBrowserVersion(String browserVersion) {
            this.browserVersion = browserVersion;
        }

        public String getPlatform() {
            return platform;
        }

        public void setPlatform(String platform) {
            this.platform = platform;
        }
    }
}

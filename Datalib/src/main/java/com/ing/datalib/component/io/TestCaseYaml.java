package com.ing.datalib.component.io;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import java.util.ArrayList;
import java.util.List;

/**
 * YAML on-disk representation of a {@link com.ing.datalib.component.TestCase}.
 *
 * <p>The kind of artifact is encoded by the top-level key:
 * <ul>
 *   <li>{@code testCase: <name>} — a test case living under {@code TestPlan/}.</li>
 *   <li>{@code reusable: <name>} — a reusable component living under {@code ReusableComponents/}.</li>
 * </ul>
 * The legacy {@code name: <name>} key and the legacy {@code reusable: true}
 * boolean flag are still accepted on read for backward compatibility.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonPropertyOrder({ "schemaVersion", "testCase", "reusable", "scenario", "tags", "steps" })
public class TestCaseYaml {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    @JsonProperty("schemaVersion")
    private Integer schemaVersion = CURRENT_SCHEMA_VERSION;

    /** Name when this artifact lives under {@code TestPlan/}. */
    @JsonProperty("testCase")
    @JsonAlias({ "name" })
    private String testCase;

    /** Name when this artifact lives under {@code ReusableComponents/}. */
    private String reusable;

    @JsonProperty("scenario")
    private String scenario;

    @JsonProperty("tags")
    private List<String> tags = new ArrayList<>();

    @JsonProperty("steps")
    private List<StepYaml> steps = new ArrayList<>();

    public Integer getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(Integer schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    @JsonProperty("testCase")
    public String getTestCase() {
        return testCase;
    }

    public void setTestCase(String testCase) {
        this.testCase = testCase;
    }

    @JsonProperty("reusable")
    public String getReusable() {
        return reusable;
    }

    /**
     * Accepts either the new {@code reusable: <name>} shape or the legacy
     * {@code reusable: true} boolean. When given {@code true} the existing
     * {@link #testCase} value is promoted to the reusable name.
     */
    @JsonSetter("reusable")
    public void setReusableRaw(Object value) {
        if (value == null) {
            this.reusable = null;
        } else if (value instanceof Boolean) {
            if (Boolean.TRUE.equals(value) && testCase != null) {
                this.reusable = testCase;
                this.testCase = null;
            }
        } else {
            this.reusable = value.toString();
        }
    }

    public void setReusable(String reusable) {
        this.reusable = reusable;
    }

    /** Returns whichever of {@link #testCase} / {@link #reusable} is populated. */
    @JsonIgnore
    public String getName() {
        return reusable != null ? reusable : testCase;
    }

    @JsonIgnore
    public boolean isReusable() {
        return reusable != null;
    }

    public String getScenario() {
        return scenario;
    }

    public void setScenario(String scenario) {
        this.scenario = scenario;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags == null ? new ArrayList<>() : tags;
    }

    public List<StepYaml> getSteps() {
        return steps;
    }

    public void setSteps(List<StepYaml> steps) {
        this.steps = steps == null ? new ArrayList<>() : steps;
    }

    /**
     * YAML representation of a single {@link com.ing.datalib.component.TestStep}.
     *
     * <p>The numeric {@code step} index is a plain integer. The CSV {@code step}
     * tag's {@code *} / {@code //} markers (see {@code TestStep#toggleBreakPoint}
     * / {@code TestStep#toggleComment}) are surfaced here as first-class
     * {@code breakpoint} / {@code comment} booleans.
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonPropertyOrder(
        {
            "step",
            "object",
            "description",
            "action",
            "input",
            "condition",
            "reference",
            "breakpoint",
            "comment",
            "hardAssertion"
        }
    )
    public static class StepYaml {
        @JsonProperty("step")
        private Integer step;

        @JsonProperty("object")
        private String object;

        @JsonProperty("description")
        private String description;

        @JsonProperty("action")
        private String action;

        @JsonProperty("input")
        private String input;

        @JsonProperty("condition")
        private String condition;

        @JsonProperty("reference")
        private String reference;

        @JsonProperty("breakpoint")
        private Boolean breakpoint;

        @JsonProperty("comment")
        private Boolean comment;

        @JsonProperty("hardAssertion")
        private Boolean hardAssertion;

        public Integer getStep() {
            return step;
        }

        public void setStep(Integer step) {
            this.step = step;
        }

        public String getObject() {
            return object;
        }

        public void setObject(String object) {
            this.object = object;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public String getInput() {
            return input;
        }

        public void setInput(String input) {
            this.input = input;
        }

        public String getCondition() {
            return condition;
        }

        public void setCondition(String condition) {
            this.condition = condition;
        }

        public String getReference() {
            return reference;
        }

        public void setReference(String reference) {
            this.reference = reference;
        }

        public Boolean getBreakpoint() {
            return breakpoint;
        }

        public void setBreakpoint(Boolean breakpoint) {
            this.breakpoint = breakpoint;
        }

        public Boolean getComment() {
            return comment;
        }

        public void setComment(Boolean comment) {
            this.comment = comment;
        }

        public Boolean getHardAssertion() {
            return hardAssertion;
        }

        public void setHardAssertion(Boolean hardAssertion) {
            this.hardAssertion = hardAssertion;
        }
    }
}

package com.ing.datalib.component;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a named, ordered grouping of Test Plan scenarios.
 * <p>
 * A {@code ScenarioGroup} associates a user-defined label (e.g. "Payment Initiation")
 * with the list of scenario names that belong to it. Groups are persisted per project
 * inside {@code TestPlan/.groups} and are used to organise scenarios visually in the
 * Test Design tree and to run all scenarios belonging to a group.
 * </p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScenarioGroup {
    @JsonProperty("name")
    private String name;

    @JsonProperty("scenarios")
    private List<String> scenarios = new ArrayList<>();

    /**
     * Default constructor for Jackson deserialization.
     */
    public ScenarioGroup() {}

    /**
     * Constructs a group with the given name and no scenarios.
     * @param name the group name
     */
    public ScenarioGroup(String name) {
        this.name = name;
    }

    /**
     * Returns the group name.
     * @return the group name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the group name.
     * @param name the new group name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the ordered list of scenario names belonging to this group.
     * @return the scenario names
     */
    public List<String> getScenarios() {
        return scenarios;
    }

    /**
     * Sets the ordered list of scenario names belonging to this group.
     * @param scenarios the scenario names
     */
    public void setScenarios(List<String> scenarios) {
        this.scenarios = scenarios != null ? scenarios : new ArrayList<>();
    }
}

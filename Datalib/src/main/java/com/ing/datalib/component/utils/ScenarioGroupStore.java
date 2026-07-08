package com.ing.datalib.component.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.ing.datalib.component.ScenarioGroup;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Persists and restores user-defined scenario groupings for a Test Plan directory.
 * <p>
 * Groups are stored in a hidden {@code .groups} JSON file inside the {@code TestPlan}
 * directory. Only named groups and their member scenarios are stored; "ungrouped"
 * scenarios are computed at load time as every scenario on disk that is not a member
 * of any named group. This keeps the file from going stale when scenarios are added or
 * removed outside the IDE.
 * </p>
 */
public class ScenarioGroupStore {
    private static final Logger LOGGER = Logger.getLogger(ScenarioGroupStore.class.getName());
    static final String GROUPS_FILE = ".groups";

    private static final ObjectMapper MAPPER = new ObjectMapper()
    .enable(SerializationFeature.INDENT_OUTPUT);

    private ScenarioGroupStore() {}

    /**
     * Container POJO mirroring the on-disk {@code .groups} JSON structure.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    static class GroupsFile {
        @JsonProperty("groups")
        public List<ScenarioGroup> groups = new ArrayList<>();
    }

    /**
     * Returns {@code true} if a {@code .groups} file exists in the given directory.
     * @param directory the Test Plan directory
     * @return whether grouping has been configured for this directory
     */
    public static boolean hasGroups(File directory) {
        return directory != null && new File(directory, GROUPS_FILE).exists();
    }

    /**
     * Loads the persisted named groups from {@code directory/.groups}.
     * Returns an empty list when no file exists or it cannot be read.
     * @param directory the Test Plan directory
     * @return the persisted named groups (never {@code null})
     */
    public static List<ScenarioGroup> load(File directory) {
        if (directory == null) {
            return new ArrayList<>();
        }
        File groupsFile = new File(directory, GROUPS_FILE);
        if (!groupsFile.exists()) {
            return new ArrayList<>();
        }
        try {
            GroupsFile parsed = MAPPER.readValue(groupsFile, GroupsFile.class);
            if (parsed != null && parsed.groups != null) {
                return parsed.groups;
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to read groups in " + directory.getPath(), e);
        }
        return new ArrayList<>();
    }

    /**
     * Saves the given named groups to {@code directory/.groups}, overwriting any existing file.
     * @param directory the Test Plan directory (must be an existing directory)
     * @param groups    the named groups to persist
     */
    public static void save(File directory, List<ScenarioGroup> groups) {
        if (directory == null || !directory.isDirectory()) {
            return;
        }
        File groupsFile = new File(directory, GROUPS_FILE);
        GroupsFile container = new GroupsFile();
        container.groups = groups != null ? groups : new ArrayList<>();
        try {
            MAPPER.writeValue(groupsFile, container);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to save groups in " + directory.getPath(), e);
        }
    }

    /**
     * Updates group membership when a scenario is renamed. Replaces {@code oldName} with
     * {@code newName} in whichever group contains it and rewrites the file. No-op when no
     * group file exists or the scenario is ungrouped.
     * @param directory the Test Plan directory
     * @param oldName   the scenario's previous name
     * @param newName   the scenario's new name
     */
    public static void renameScenario(File directory, String oldName, String newName) {
        if (!hasGroups(directory)) {
            return;
        }
        List<ScenarioGroup> groups = load(directory);
        boolean changed = false;
        for (ScenarioGroup group : groups) {
            int idx = group.getScenarios().indexOf(oldName);
            if (idx >= 0) {
                group.getScenarios().set(idx, newName);
                changed = true;
            }
        }
        if (changed) {
            save(directory, groups);
        }
    }

    /**
     * Removes a scenario from any group that contains it and rewrites the file.
     * No-op when no group file exists or the scenario is ungrouped.
     * @param directory    the Test Plan directory
     * @param scenarioName the scenario to remove from grouping
     */
    public static void removeScenario(File directory, String scenarioName) {
        if (!hasGroups(directory)) {
            return;
        }
        List<ScenarioGroup> groups = load(directory);
        boolean changed = false;
        for (ScenarioGroup group : groups) {
            if (group.getScenarios().remove(scenarioName)) {
                changed = true;
            }
        }
        if (changed) {
            save(directory, groups);
        }
    }
}

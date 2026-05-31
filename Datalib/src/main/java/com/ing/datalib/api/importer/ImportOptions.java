package com.ing.datalib.api.importer;

import java.io.Serializable;

/**
 * User-selected options that drive {@code ReusableImportEngine}.
 */
public class ImportOptions implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum HierarchyStrategy {
        /** All requests become reusables in one scenario named after the collection (or {@link #scenarioPrefix} + collection name). */
        FLATTEN,
        /** Each top-level folder of the source becomes its own reusable scenario. */
        SCENARIO_PER_TOP_FOLDER
    }

    public enum ConflictPolicy {
        /** Skip the request if a reusable with the same name already exists. */
        SKIP,
        /** Append a numeric suffix to avoid collisions. */
        RENAME_SUFFIX,
        /** Replace the existing reusable. */
        OVERWRITE
    }

    /**
     * Where the imported requests should land in the project tree.
     */
    public enum TargetType {
        /** Imported requests become reusable test cases (User Intents). */
        REUSABLE,
        /** Imported requests become regular test-design test cases. */
        TEST_CASE
    }

    private HierarchyStrategy hierarchyStrategy = HierarchyStrategy.FLATTEN;
    private ConflictPolicy conflictPolicy = ConflictPolicy.RENAME_SUFFIX;
    private TargetType targetType = TargetType.REUSABLE;
    private String scenarioPrefix = "API_";
    /** When non-null, all reusables go into this existing scenario. */
    private String targetScenarioName;
    private boolean importEnvironments = true;

    public HierarchyStrategy getHierarchyStrategy() { return hierarchyStrategy; }
    public void setHierarchyStrategy(HierarchyStrategy s) { this.hierarchyStrategy = s; }

    public ConflictPolicy getConflictPolicy() { return conflictPolicy; }
    public void setConflictPolicy(ConflictPolicy p) { this.conflictPolicy = p; }

    public TargetType getTargetType() { return targetType; }
    public void setTargetType(TargetType targetType) { this.targetType = targetType; }

    public String getScenarioPrefix() { return scenarioPrefix; }
    public void setScenarioPrefix(String scenarioPrefix) { this.scenarioPrefix = scenarioPrefix; }

    public String getTargetScenarioName() { return targetScenarioName; }
    public void setTargetScenarioName(String targetScenarioName) { this.targetScenarioName = targetScenarioName; }

    public boolean isImportEnvironments() { return importEnvironments; }
    public void setImportEnvironments(boolean importEnvironments) { this.importEnvironments = importEnvironments; }
}

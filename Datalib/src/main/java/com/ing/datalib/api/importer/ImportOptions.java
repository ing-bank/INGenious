package com.ing.datalib.api.importer;

import java.io.Serializable;

/**
 * User-selected options that drive {@code ReusableImportEngine}.
 */
public class ImportOptions implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum HierarchyStrategy {
        /** Each top-level folder of the source becomes its own reusable scenario. */
        SCENARIO_PER_TOP_FOLDER("ScenariosPerTopFolder"),
        /** All requests become reusables in one scenario named after the collection (or {@link #scenarioPrefix} + collection name). */
        FLATTEN("Flatten");

        private final String displayName;

        HierarchyStrategy(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public enum ConflictPolicy {
        /** Skip the request if a reusable with the same name already exists. */
        SKIP("Skip"),
        /** Append a numeric suffix to avoid collisions. */
        RENAME_SUFFIX("RenameSuffix"),
        /** Replace the existing reusable. */
        OVERWRITE("Overwrite");

        private final String displayName;

        ConflictPolicy(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
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

    /**
     * Naming convention to apply to all generated asset names.
     */
    public enum NamingConvention {
        /** PascalCase: each word capitalized, no separators */
        PASCAL_CASE("PascalCase"),
        /** camelCase: first word lowercase, subsequent words capitalized */
        CAMEL_CASE("camelCase"),
        /** snake_case: words separated by underscores, all lowercase */
        SNAKE_CASE("snake_case");

        private final String displayName;

        NamingConvention(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private HierarchyStrategy hierarchyStrategy = HierarchyStrategy.SCENARIO_PER_TOP_FOLDER;
    private ConflictPolicy conflictPolicy = ConflictPolicy.RENAME_SUFFIX;
    private TargetType targetType = TargetType.REUSABLE;
    private NamingConvention namingConvention = NamingConvention.PASCAL_CASE;
    private String scenarioPrefix = "";
    /** When non-null, all reusables go into this existing scenario. */
    private String targetScenarioName;
    /**
     * When true, imports Postman environments as INGenious Test Data datasheets:
     * - Creates a datasheet named after the collection
     * - Creates data environment folders for each Postman environment
     * - Populates columns from environment variable keys
     * - Populates values from environment variable values
     * - Converts %Variable% and {{Variable}} references in requests to {Datasheet:Column} syntax
     */
    private boolean importEnvironments = false;

    public HierarchyStrategy getHierarchyStrategy() {
        return hierarchyStrategy;
    }

    public void setHierarchyStrategy(HierarchyStrategy s) {
        this.hierarchyStrategy = s;
    }

    public ConflictPolicy getConflictPolicy() {
        return conflictPolicy;
    }

    public void setConflictPolicy(ConflictPolicy p) {
        this.conflictPolicy = p;
    }

    public TargetType getTargetType() {
        return targetType;
    }

    public void setTargetType(TargetType targetType) {
        this.targetType = targetType;
    }

    public NamingConvention getNamingConvention() {
        return namingConvention;
    }

    public void setNamingConvention(NamingConvention namingConvention) {
        this.namingConvention = namingConvention;
    }

    public String getScenarioPrefix() {
        return scenarioPrefix;
    }

    public void setScenarioPrefix(String scenarioPrefix) {
        this.scenarioPrefix = scenarioPrefix;
    }

    public String getTargetScenarioName() {
        return targetScenarioName;
    }

    public void setTargetScenarioName(String targetScenarioName) {
        this.targetScenarioName = targetScenarioName;
    }

    public boolean isImportEnvironments() {
        return importEnvironments;
    }

    public void setImportEnvironments(boolean importEnvironments) {
        this.importEnvironments = importEnvironments;
    }
}

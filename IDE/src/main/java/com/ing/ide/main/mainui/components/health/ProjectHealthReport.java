package com.ing.ide.main.mainui.components.health;

import java.util.ArrayList;
import java.util.List;

/**
 * Structured result of a project health analysis, rendered by
 * {@link ProjectHealthDialog}. Mirrors the dimensions of the CLI
 * {@code ingenious project validate} dashboard, computed from the in-memory
 * project model.
 */
public final class ProjectHealthReport {
    public String projectName = "";

    // Inventory
    public int scenarios;
    public int testCases;
    public int reusableScenarios;
    public int reusableComponents;
    public int releases;
    public int testSets;

    // Quality tallies
    public int emptyTestCases;
    public int taggedTestCases;
    public int totalSteps;
    public int reusableSteps;
    public int parameterisedInputs;
    public int hardcodedInputs;
    public int testCasesInTestSets;

    // Dimension scores (0-100)
    public int structureScore;
    public int modularityScore;
    public int dataScore;
    public int testSetScore;
    public int tagScore;

    // Overall
    public int overallScore;
    public String grade = "F";

    public final List<String> errors = new ArrayList<>();
    public final List<String> warnings = new ArrayList<>();
    public final List<Row> rows = new ArrayList<>();

    /** One per authored test case, shown in the detail table. */
    public static final class Row {
        public String scenario = "";
        public String name = "";
        public String kind = "Unknown";
        public int steps;
        public int reusablePct;
        public int dataPct;
        public boolean tagged;
    }
}

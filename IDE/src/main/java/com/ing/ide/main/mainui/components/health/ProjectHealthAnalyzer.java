package com.ing.ide.main.mainui.components.health;

import com.ing.datalib.component.ExecutionStep;
import com.ing.datalib.component.Project;
import com.ing.datalib.component.Release;
import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.component.TestSet;
import com.ing.datalib.component.TestStep;
import java.util.HashSet;
import java.util.Set;

/**
 * Computes a {@link ProjectHealthReport} from the in-memory project model.
 * Read-only; safe to run on the EDT for typical project sizes. The external
 * {@code ingenious project validate} CLI command is unaffected.
 */
public final class ProjectHealthAnalyzer {

    private ProjectHealthAnalyzer() {}

    public static ProjectHealthReport analyse(Project project) {
        ProjectHealthReport r = new ProjectHealthReport();
        if (project == null) {
            r.errors.add("No project is open.");
            r.grade = "F";
            return r;
        }
        r.projectName = project.getName();

        r.scenarios = project.getScenarios().size();
        r.reusableScenarios = project.getReusableScenarios().size();

        Set<String> testCasesInSets = new HashSet<>();

        for (Scenario scenario : project.getScenarios()) {
            if (scenario.getTestCases().isEmpty()) {
                r.warnings.add("Scenario '" + scenario.getName() + "' has no test cases.");
            }
            for (TestCase tc : scenario.getTestCases()) {
                r.testCases++;
                loadSteps(tc);
                ProjectHealthReport.Row row = analyseTestCase(scenario.getName(), tc);
                r.rows.add(row);
                r.totalSteps += row.steps;
                if (row.tagged) {
                    r.taggedTestCases++;
                }
                if (row.steps == 0) {
                    r.emptyTestCases++;
                    r.warnings.add("Empty test case: " + scenario.getName() + "/" + tc.getName());
                }
                accumulate(r, tc);
            }
        }

        for (Scenario rs : project.getReusableScenarios()) {
            r.reusableComponents += rs.getTestCases().size();
        }

        for (Release release : project.getReleases()) {
            r.releases++;
            if (release.getTestSets().isEmpty()) {
                r.warnings.add("Release '" + release.getName() + "' has no test sets.");
            }
            for (TestSet ts : release.getTestSets()) {
                r.testSets++;
                try {
                    ts.loadTestSetTableModel();
                } catch (Exception ignored) {
                    // best effort
                }
                for (ExecutionStep es : ts.getTestSteps()) {
                    String scn = es.getTestScenarioName();
                    String tcName = es.getTestCaseName();
                    if (scn != null && !scn.isEmpty() && tcName != null && !tcName.isEmpty()) {
                        testCasesInSets.add(scn + "/" + tcName);
                    }
                }
            }
        }

        // Test cases not referenced by any test set.
        if (r.testSets > 0) {
            for (Scenario scenario : project.getScenarios()) {
                for (TestCase tc : scenario.getTestCases()) {
                    String key = scenario.getName() + "/" + tc.getName();
                    if (testCasesInSets.contains(key)) {
                        r.testCasesInTestSets++;
                    } else {
                        r.warnings.add("Test case not in any test set: " + key);
                    }
                }
            }
        }

        if (r.testCases == 0) {
            r.errors.add("Project has no test cases in the Test Plan.");
        }

        score(r);
        return r;
    }

    private static void loadSteps(TestCase tc) {
        try {
            tc.loadTestCaseTableModel();
        } catch (Exception ignored) {
            // best effort; steps may already be loaded
        }
    }

    private static ProjectHealthReport.Row analyseTestCase(String scenario, TestCase tc) {
        ProjectHealthReport.Row row = new ProjectHealthReport.Row();
        row.scenario = scenario;
        row.name = tc.getName();
        int reusable = 0;
        int param = 0;
        int hard = 0;
        boolean tagged = false;
        int web = 0;
        int api = 0;
        int mobile = 0;
        int db = 0;
        int kafka = 0;

        for (TestStep step : tc.getTestSteps()) {
            if (step.isEmpty() || Boolean.TRUE.equals(step.isCommented())) {
                continue;
            }
            row.steps++;
            if (step.getTag() != null && !step.getTag().trim().isEmpty()) {
                tagged = true;
            }
            if (Boolean.TRUE.equals(step.isReusableStep())) {
                reusable++;
                continue;
            }
            String obj = step.getObject() == null ? "" : step.getObject().toLowerCase();
            if (obj.contains("webservice") || obj.contains("rest") || obj.contains("api")) {
                api++;
            } else if (obj.contains("mobile")) {
                mobile++;
            } else if (obj.contains("database") || obj.contains("db")) {
                db++;
            } else if (obj.contains("kafka")) {
                kafka++;
            } else if (!obj.isEmpty()) {
                web++;
            }

            String input = step.getInput() == null ? "" : step.getInput().trim();
            if (input.isEmpty()) {
                continue;
            }
            if (input.startsWith("@")) {
                hard++;
            } else if (
                input.startsWith("%") ||
                input.startsWith("=") ||
                Boolean.TRUE.equals(step.isTestDataStep())
            ) {
                param++;
            } else {
                hard++;
            }
        }

        row.tagged = tagged;
        row.reusablePct = row.steps == 0 ? 0 : (int) Math.round(reusable * 100.0 / row.steps);
        int inputs = param + hard;
        row.dataPct = inputs == 0 ? 100 : (int) Math.round(param * 100.0 / inputs);
        row.kind = kindLabel(web, api, mobile, db, kafka);
        return row;
    }

    private static String kindLabel(int web, int api, int mobile, int db, int kafka) {
        StringBuilder sb = new StringBuilder();
        appendKind(sb, web > 0, "UI");
        appendKind(sb, api > 0, "API");
        appendKind(sb, mobile > 0, "Mobile");
        appendKind(sb, db > 0, "DB");
        appendKind(sb, kafka > 0, "Kafka");
        return sb.length() == 0 ? "Unknown" : sb.toString();
    }

    private static void appendKind(StringBuilder sb, boolean present, String label) {
        if (present) {
            if (sb.length() > 0) {
                sb.append(" + ");
            }
            sb.append(label);
        }
    }

    private static void accumulate(ProjectHealthReport r, TestCase tc) {
        for (TestStep step : tc.getTestSteps()) {
            if (step.isEmpty() || Boolean.TRUE.equals(step.isCommented())) {
                continue;
            }
            if (Boolean.TRUE.equals(step.isReusableStep())) {
                r.reusableSteps++;
                continue;
            }
            String input = step.getInput() == null ? "" : step.getInput().trim();
            if (input.isEmpty()) {
                continue;
            }
            if (input.startsWith("@")) {
                r.hardcodedInputs++;
            } else if (
                input.startsWith("%") ||
                input.startsWith("=") ||
                Boolean.TRUE.equals(step.isTestDataStep())
            ) {
                r.parameterisedInputs++;
            } else {
                r.hardcodedInputs++;
            }
        }
    }

    private static void score(ProjectHealthReport r) {
        // Structure: penalise empty test cases and empty projects.
        if (r.testCases == 0) {
            r.structureScore = 0;
        } else {
            r.structureScore =
                (int) Math.round((r.testCases - r.emptyTestCases) * 100.0 / r.testCases);
        }

        // Modularity: how much reuse is present across all steps.
        r.modularityScore =
            r.totalSteps == 0 ? 0 : (int) Math.round(r.reusableSteps * 100.0 / r.totalSteps);
        // Reward the mere existence of reusable components.
        if (r.reusableComponents > 0) {
            r.modularityScore = Math.min(100, r.modularityScore + 20);
        }

        // Data: parameterised vs hard-coded inputs.
        int inputs = r.parameterisedInputs + r.hardcodedInputs;
        r.dataScore = inputs == 0 ? 100 : (int) Math.round(r.parameterisedInputs * 100.0 / inputs);

        // Test sets: coverage of test cases by test sets.
        r.testSetScore =
            r.testCases == 0
                ? 0
                : (
                    r.testSets == 0
                        ? 0
                        : (int) Math.round(r.testCasesInTestSets * 100.0 / r.testCases)
                );

        // Tagging: fraction of tagged test cases.
        r.tagScore =
            r.testCases == 0 ? 0 : (int) Math.round(r.taggedTestCases * 100.0 / r.testCases);

        r.overallScore =
            (int) Math.round(
                (r.structureScore + r.modularityScore + r.dataScore + r.testSetScore + r.tagScore) /
                5.0
            );
        r.grade = grade(r.overallScore);
    }

    private static String grade(int score) {
        if (score >= 90) {
            return "A";
        }
        if (score >= 80) {
            return "B";
        }
        if (score >= 70) {
            return "C";
        }
        if (score >= 60) {
            return "D";
        }
        return "F";
    }
}

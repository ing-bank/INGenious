package com.ing.engine.perf;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.component.TestStep;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Flattens a test case into an ordered step list: skips commented steps and
 * inlines reusable components ({@code Execute} / {@code Scenario:TestCase})
 * with cycle detection and a depth cap. Shared by the HTTP and browser
 * script generators.
 */
final class TestCaseFlattener {
    static final int MAX_DEPTH = 5;

    private TestCaseFlattener() {}

    static List<TestStep> flatten(Project project, TestCase testCase, List<String> warnings) {
        List<TestStep> out = new ArrayList<>();
        walk(project, testCase, out, warnings, new LinkedHashSet<String>(), 0);
        return out;
    }

    private static void walk(
        Project project,
        TestCase testCase,
        List<TestStep> out,
        List<String> warnings,
        Set<String> visiting,
        int depth
    ) {
        testCase.loadTestCaseTableModel();
        for (TestStep step : testCase.getTestSteps()) {
            if (step.isCommented()) {
                continue;
            }
            String action = step.getAction() == null ? "" : step.getAction().trim();
            if (action.isEmpty()) {
                continue;
            }
            if (step.isReusableStep()) {
                String[] ref = step.getReusableData();
                if (ref == null) {
                    warnings.add("Unparseable reusable step: " + step.getAction());
                    continue;
                }
                String id = ref[0] + ":" + ref[1];
                if (depth >= MAX_DEPTH || visiting.contains(id)) {
                    warnings.add(
                        "Skipped reusable '" + id + "' (cycle or depth > " + MAX_DEPTH + ")"
                    );
                    continue;
                }
                Scenario scenario = project.getReusableScenarioByName(ref[0]);
                TestCase reusable = scenario == null ? null : scenario.getTestCaseByName(ref[1]);
                if (reusable == null) {
                    warnings.add("Reusable not found in project: " + id);
                    continue;
                }
                visiting.add(id);
                walk(project, reusable, out, warnings, visiting, depth + 1);
                visiting.remove(id);
                continue;
            }
            out.add(step);
        }
    }

    /**
     * Apply the step input grammar: {@code @literal} is stripped; runtime
     * expressions and data references ({@code %var%}, {@code {Sheet:Column}},
     * {@code Sheet:Column}, {@code =func(...)}) survive verbatim with a
     * warning — Phase 6 parameterization rules resolve them properly.
     */
    static String resolveInput(String input, List<String> warnings, String context) {
        String s = input == null ? "" : input;
        if (s.startsWith("@")) {
            return s.substring(1);
        }
        if (s.isEmpty()) {
            return s;
        }
        if (
            s.matches("%\\w+%") ||
            s.startsWith("=") ||
            s.matches("\\{[^:{}]+:[^:{}]+\\}") ||
            s.matches("[^:@{}\\s]+:[^:{}]+")
        ) {
            warnings.add(
                "Dynamic input in " +
                context +
                " kept verbatim (parameterize before load-testing): " +
                s
            );
        }
        return s;
    }
}

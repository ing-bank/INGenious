package com.ing.ide.main.mainui.components.testexecution.tree.model;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.ide.main.mainui.components.testdesign.tree.model.TestPlanNode;
import com.ing.ide.main.mainui.components.testdesign.tree.model.TestPlanTreeModel;
import java.util.Comparator;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 *
 *
 */
public class FilterableTestPlanTreeModel extends TestPlanTreeModel {

    public FilterableTestPlanTreeModel(Project project, Predicate<Object> accept) {
        super(new FilteredTestPlanNode(accept));
        this.setProject(project);
    }
}

class FilteredTestPlanNode extends TestPlanNode {
    private final Predicate<Object> byPredicate;
    private static final Comparator<Scenario> SCENARIO_NAME_ORDER = Comparator.comparing(
        Scenario::getName,
        String.CASE_INSENSITIVE_ORDER
    );
    private static final Comparator<TestCase> TESTCASE_NAME_ORDER = Comparator.comparing(
        TestCase::getName,
        String.CASE_INSENSITIVE_ORDER
    );

    public FilteredTestPlanNode(Predicate<Object> accept) {
        this.byPredicate = accept;
    }

    @Override
    public void filterTestCases() {
        project
            .getScenarios()
            .stream()
            .sorted(SCENARIO_NAME_ORDER)
            .flatMap(this::toFilteredTestcases)
            .forEach(this::add);
    }

    public Stream<TestCase> toFilteredTestcases(Scenario scenario) {
        return byPredicate.test(scenario)
            ? scenario.getTestcasesAlone().stream().sorted(TESTCASE_NAME_ORDER)
            : scenario.getTestcasesAlone().stream().filter(byPredicate).sorted(TESTCASE_NAME_ORDER);
    }
}

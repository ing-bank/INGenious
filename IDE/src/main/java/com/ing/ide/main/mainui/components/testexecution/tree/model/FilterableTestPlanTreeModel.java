
package com.ing.ide.main.mainui.components.testexecution.tree.model;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.ide.main.mainui.components.testdesign.tree.model.TestPlanNode;
import com.ing.ide.main.mainui.components.testdesign.tree.model.TestPlanTreeModel;
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

    public FilteredTestPlanNode(Predicate<Object> accept) {
        this.byPredicate = accept;
    }

    @Override
    public void filterTestCases() {
        project.getScenarios().stream().flatMap(this::toFilteredTestcases).forEach(this::add);
    }

    public Stream<TestCase> toFilteredTestcases(Scenario scenario) {
        return byPredicate.test(scenario) ? scenario.getTestcasesAlone().stream()
                : scenario.getTestcasesAlone().stream().filter(byPredicate);
    }

}

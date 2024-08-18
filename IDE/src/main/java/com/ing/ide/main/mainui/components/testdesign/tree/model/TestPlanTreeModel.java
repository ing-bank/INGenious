
package com.ing.ide.main.mainui.components.testdesign.tree.model;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;

/**
 *
 * 
 */
public class TestPlanTreeModel extends ProjectTreeModel<TestPlanNode> {

    Project project;

    public TestPlanTreeModel() {
        super(new TestPlanNode());
    }

    public TestPlanTreeModel(TestPlanNode tpn) {
        super(tpn);
    }

    @Override
    public final void setProject(Project project) {
        this.project = project;
        getRoot().setProject(project);
    }

    @Override
    public TestPlanNode getRoot() {
        return (TestPlanNode) super.getRoot();
    }

    @Override
    public ScenarioNode addScenario(TestPlanNode groupNode, Scenario scenario) {
        if (groupNode == null) {
            groupNode = getRoot();
        }
        return super.addScenario(groupNode, scenario);
    }

    @Override
    public TestCaseNode addTestCase(TestCase testCase) {
        if (getRoot().getChildCount() > 0) {
            for (ScenarioNode scenarioNode : ScenarioNode.toList(getRoot().children())) {
                if (scenarioNode.getScenario().equals(testCase.getScenario())) {
                    return addTestCase(scenarioNode, testCase);
                }
            }
        }
        return addTestCase(addScenario(getRoot(), testCase.getScenario()), testCase);
    }

    @Override
    public void onScenarioRename(Scenario scenario) {
        if (getRoot().getChildCount() > 0) {
            ScenarioNode sNode = getRoot().getScenarioNodeBy(scenario);
            if (sNode != null) {
                reload(sNode);
            }
        }
    }

}

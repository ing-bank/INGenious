
package com.ing.ide.main.mainui.components.testdesign.tree.model;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;

/**
 *
 * 
 */
public class ReusableTreeModel extends ProjectTreeModel {

    private static final String DEFAULT_GROUP = "Reusable Components";

    Project project;

    public ReusableTreeModel() {
        super(new ReusableNode());
    }

    @Override
    public final void setProject(Project project) {
        this.project = project;
        getRoot().setProject(project);
    }

    @Override
    public ReusableNode getRoot() {
        return (ReusableNode) super.getRoot();
    }

    public GroupNode addGroup(String name) {
        if (getRoot().getGroupByName(name) == null) {
            GroupNode gNode = new GroupNode(name);
            insertNodeInto(gNode, getRoot(), getRoot().getChildCount());
            return gNode;
        }
        return null;
    }

    public void toggleAllTestCasesFrom(GroupNode groupNode) {
        for (ScenarioNode scenarioNode : ScenarioNode.toList(groupNode.children())) {
            for (TestCaseNode testCaseNode : TestCaseNode.toList(scenarioNode.children())) {
                project.moveTestCaseToTestPlan(testCaseNode.getTestCase());
            }
        }
    }

    @Override
    public TestCaseNode addTestCase(TestCase testCase) {
        GroupNode groupNode;
        if (getRoot().getChildCount() > 0) {
            for (GroupNode group : GroupNode.toList(getRoot().children())) {
                for (ScenarioNode scenarioNode : ScenarioNode.toList(group.children())) {
                    if (scenarioNode.getScenario().equals(testCase.getScenario())) {
                        return addTestCase(scenarioNode, testCase);
                    }
                }
            }
            groupNode = (GroupNode) getRoot().getChildAt(0);
        } else {
            groupNode = addGroup(DEFAULT_GROUP);
        }
        return addTestCase(addScenario(groupNode, testCase.getScenario()), testCase);
    }

    @Override
    public TestCaseNode addTestCase(ScenarioNode scNode, TestCase testCase) {
        return super.addTestCase(scNode, testCase);
    }

    @Override
    public void onScenarioRename(Scenario scenario) {
        if (getRoot().getChildCount() > 0) {
            for (GroupNode group : GroupNode.toList(getRoot().children())) {
                ScenarioNode sNode = group.getScenarioNodeBy(scenario);
                if (sNode != null) {
                    reload(sNode);
                }
            }
        }
    }

    public void save() {
        // No-op: reusable components are now inferred from directory placement.
    }

}

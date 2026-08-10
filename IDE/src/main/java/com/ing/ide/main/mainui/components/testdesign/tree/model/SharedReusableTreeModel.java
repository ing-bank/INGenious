package com.ing.ide.main.mainui.components.testdesign.tree.model;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.exception.TestCaseConversionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tree model for displaying and managing Shared Reusable Components.
 * Mirrors ReusableTreeModel but loads from shared reusable components directory.
 */
public class SharedReusableTreeModel extends ProjectTreeModel {
    private static final Logger LOGGER = Logger.getLogger(SharedReusableTreeModel.class.getName());
    private static final String DEFAULT_GROUP = "Shared Components";

    Project project;

    public SharedReusableTreeModel() {
        super(new SharedReusableNode());
    }

    @Override
    public final void setProject(Project project) {
        this.project = project;
        getRoot().setProject(project);
    }

    @Override
    public SharedReusableNode getRoot() {
        return (SharedReusableNode) super.getRoot();
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
                try {
                    project.moveTestCaseToTestPlan(testCaseNode.getTestCase());
                } catch (TestCaseConversionException e) {
                    LOGGER.log(
                        Level.WARNING,
                        "Failed to move test case to test plan: " + e.getMessage(),
                        e
                    );
                }
            }
        }
    }

    @Override
    public TestCaseNode addTestCase(TestCase testCase) {
        if (testCase == null || testCase.getScenario() == null) {
            return null;
        }
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
        // No-op: shared reusable components are now inferred from directory placement.
    }
}

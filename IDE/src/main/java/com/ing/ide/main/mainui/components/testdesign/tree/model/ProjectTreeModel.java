
package com.ing.ide.main.mainui.components.testdesign.tree.model;

import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.ide.main.utils.tree.CommonTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

/**
 *
 * 
 * @param <T>
 */
public abstract class ProjectTreeModel<T extends GroupNode> extends CommonTreeModel {

    public ProjectTreeModel(TreeNode tn) {
        super(tn);
    }

    @Override
    public void valueForPathChanged(TreePath tp, Object o) {
        //Do nothing
    }

    public ScenarioNode addScenario(T groupNode, Scenario scenario) {
        if (groupNode.getScenarioNodeBy(scenario) == null) {
            ScenarioNode sNode = new ScenarioNode(scenario);
            insertNodeInto(sNode, groupNode, groupNode.getChildCount());
            return sNode;
        }
        return null;
    }

    public TestCaseNode addTestCase(ScenarioNode scNode, TestCase testCase) {
        if (scNode.getTestCaseNodeBy(testCase) == null) {
            TestCaseNode tcNode = new TestCaseNode(testCase);
            insertNodeInto(tcNode, scNode, scNode.getChildCount());
            return tcNode;
        }
        return null;
    }

    public abstract TestCaseNode addTestCase(TestCase testCase);
    
    public abstract void onScenarioRename(Scenario scenario);
}

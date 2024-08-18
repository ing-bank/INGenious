
package com.ing.ide.main.mainui.components.testdesign.tree.model;

import com.ing.datalib.component.Scenario;
import com.ing.ide.main.utils.tree.CommonNode;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.tree.TreeNode;

/**
 *
 * 
 */
public class GroupNode extends CommonNode {

    private String name;

    public GroupNode(String name) {
        this.name = name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ScenarioNode addScenarioIfNotPresent(Scenario scenario) {
        addScenario(scenario);
        return getScenarioNodeBy(scenario);
    }

    public ScenarioNode addScenario(Scenario scenario) {
        if (getScenarioNodeBy(scenario) == null) {
            ScenarioNode node = new ScenarioNode(scenario);
            this.add(node);
            return node;
        }
        return null;
    }

    public ScenarioNode getScenarioNodeBy(Scenario scenario) {
        for (TreeNode scenarioNode : Collections.list(children())) {
            if (((ScenarioNode)scenarioNode).getScenario().equals(scenario)) {
                return (ScenarioNode)scenarioNode;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return name;
    }


    public boolean rename(String name) {
        ReusableNode rNode = (ReusableNode) getParent();
        if (rNode.getGroupByName(name) == null) {
            setName(name);
            for (TreeNode scenarioNode: Collections.list(children())) {
                for (TreeNode testCaseNode : Collections.list(scenarioNode.children())) {
                    ((TestCaseNode)testCaseNode).getTestCase().getReusable().setGroup(name);
                }
            }
            return true;
        }
        return false;
    }

   public static List<GroupNode> toList(Enumeration<TreeNode> children){
       return Collections.list(children).stream().map(tsNode -> (GroupNode) tsNode).collect(Collectors.toList());       
   }
}

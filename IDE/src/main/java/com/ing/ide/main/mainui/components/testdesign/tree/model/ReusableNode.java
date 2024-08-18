
package com.ing.ide.main.mainui.components.testdesign.tree.model;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
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
public class ReusableNode extends CommonNode {

    Project project;

    public void setProject(Project project) {
        removeAllChildren();
        this.project = project;
        filterGroups();
    }

    private void filterGroups() {
        for (Scenario scenario : project.getScenarios()) {
            for (TestCase reusable : scenario.getReusables()) {
                String groupName = reusable.getReusable().getGroup();
                addGroupIfNotPresent(groupName).addScenarioIfNotPresent(reusable.getScenario()).addTestCaseIfNotPresent(reusable);
            }
        }
    }

    public GroupNode addGroupIfNotPresent(String groupName) {
        addGroup(groupName);
        return getGroupByName(groupName);
    }

    public GroupNode addGroup(String groupName) {
        if (getGroupByName(groupName) == null) {
            GroupNode node = new GroupNode(groupName);
            add(node);
            return node;
        }
        return null;
    }

    public GroupNode getGroupByName(String groupName) {
        for (GroupNode group : GroupNode.toList(children())) {
            if (group.toString().equals(groupName)) {
                return group;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return project != null ? project.getName() : "Reusable";
    }

   public static List<ReusableNode> toList(Enumeration<TreeNode> children){
       return Collections.list(children).stream().map(tsNode -> (ReusableNode) tsNode).collect(Collectors.toList());
       
   }

}

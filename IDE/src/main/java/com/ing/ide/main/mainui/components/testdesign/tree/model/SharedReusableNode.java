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
 * Root node for shared reusable components tree.
 * Displays all shared reusable scenarios and test cases with [Shared] scope indicators.
 */
public class SharedReusableNode extends CommonNode {
    private static final String DEFAULT_GROUP = "Shared Components";

    Project project;

    public void setProject(Project project) {
        removeAllChildren();
        this.project = project;
        filterGroups();
    }

    private void filterGroups() {
        GroupNode groupNode = addGroupIfNotPresent(DEFAULT_GROUP);
        for (Scenario scenario : project.getSharedScenarios()) {
            for (TestCase testCase : scenario.getTestCases()) {
                groupNode
                    .addScenarioIfNotPresent(testCase.getScenario())
                    .addTestCaseIfNotPresent(testCase);
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

    public List<GroupNode> getGroups() {
        return GroupNode.toList(children());
    }

    public List<ScenarioNode> getScenarios() {
        List<ScenarioNode> scenarios = new java.util.ArrayList<>();
        for (GroupNode group : getGroups()) {
            scenarios.addAll(ScenarioNode.toList(group.children()));
        }
        return scenarios;
    }

    public List<TestCaseNode> getTestCases() {
        List<TestCaseNode> testCases = new java.util.ArrayList<>();
        for (ScenarioNode scenario : getScenarios()) {
            testCases.addAll(TestCaseNode.toList(scenario.children()));
        }
        return testCases;
    }
}

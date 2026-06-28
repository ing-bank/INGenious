package com.ing.ide.main.mainui.components.testdesign.tree.model;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.ScenarioGroup;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.component.utils.ScenarioGroupStore;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
        if (getRoot().isGrouped()) {
            TestPlanGroupNode ungrouped = ensureUngroupedNode();
            if (ungrouped.getScenarioNodeBy(scenario) == null) {
                ScenarioNode sNode = new ScenarioNode(scenario);
                insertNodeInto(sNode, ungrouped, ungrouped.getChildCount());
                return sNode;
            }
            return null;
        }
        if (groupNode == null) {
            groupNode = getRoot();
        }
        return super.addScenario(groupNode, scenario);
    }

    @Override
    public TestCaseNode addTestCase(TestCase testCase) {
        ScenarioNode scenarioNode = getRoot().getScenarioNodeBy(testCase.getScenario());
        if (scenarioNode != null) {
            return addTestCase(scenarioNode, testCase);
        }
        return addTestCase(addScenario(getRoot(), testCase.getScenario()), testCase);
    }

    @Override
    public void onScenarioRename(Scenario scenario) {
        ScenarioNode sNode = getRoot().getScenarioNodeBy(scenario);
        if (sNode != null) {
            reload(sNode);
        }
    }

    /**
     * Creates a new named group, converting a flat tree to grouped layout on first use.
     * @param name group name (must be unique)
     * @return the new group node, or {@code null} when a group of that name exists
     */
    public TestPlanGroupNode addGroup(String name) {
        TestPlanNode root = getRoot();
        if (!root.isGrouped()) {
            convertToGrouped();
        }
        if (root.getGroupByName(name) != null) {
            return null;
        }
        TestPlanGroupNode group = new TestPlanGroupNode(name, false);
        TestPlanGroupNode ungrouped = root.getUngroupedNode();
        int idx = ungrouped != null ? root.getIndex(ungrouped) : root.getChildCount();
        insertNodeInto(group, root, idx);
        saveGroups();
        return group;
    }

    /**
     * Adds a scenario directly into the given group (named or ungrouped) and persists.
     * @param group    destination group
     * @param scenario scenario to add
     * @return the new scenario node, or {@code null} if already present
     */
    public ScenarioNode addScenarioToGroup(TestPlanGroupNode group, Scenario scenario) {
        if (group.getScenarioNodeBy(scenario) == null) {
            ScenarioNode sNode = new ScenarioNode(scenario);
            insertNodeInto(sNode, group, group.getChildCount());
            saveGroups();
            return sNode;
        }
        return null;
    }

    /**
     * Renames a group. Renaming the {@code (Ungrouped)} node promotes it into a real
     * named group (its scenarios become members) and a fresh empty {@code (Ungrouped)}
     * node is created for future scenarios.
     * @param node    group to rename
     * @param newName new name
     * @return {@code true} if applied
     */
    public boolean renameGroup(TestPlanGroupNode node, String newName) {
        if (getRoot().getGroupByName(newName) != null) {
            return false;
        }
        if (node.isUngrouped()) {
            node.setUngrouped(false);
            node.setName(newName);
            TestPlanGroupNode fresh = new TestPlanGroupNode(TestPlanGroupNode.UNGROUPED, true);
            insertNodeInto(fresh, getRoot(), getRoot().getChildCount());
            reload(node);
            saveGroups();
            return true;
        }
        node.setName(newName);
        reload(node);
        saveGroups();
        return true;
    }

    /**
     * Deletes a named group, moving its scenarios to the {@code (Ungrouped)} node.
     * @param node group to delete
     */
    public void deleteGroup(TestPlanGroupNode node) {
        if (node.isUngrouped()) {
            return;
        }
        TestPlanGroupNode ungrouped = ensureUngroupedNode();
        for (ScenarioNode scenarioNode : ScenarioNode.toList(node.children())) {
            removeNodeFromParent(scenarioNode);
            insertNodeInto(scenarioNode, ungrouped, ungrouped.getChildCount());
        }
        removeNodeFromParent(node);
        saveGroups();
    }

    /**
     * Moves a scenario into the target group (named or ungrouped) and persists.
     * @param scenarioNode scenario to move
     * @param targetGroup  destination group
     */
    public void moveScenarioToGroup(ScenarioNode scenarioNode, TestPlanGroupNode targetGroup) {
        if (scenarioNode.getParent() == targetGroup) {
            return;
        }
        removeNodeFromParent(scenarioNode);
        insertNodeInto(scenarioNode, targetGroup, targetGroup.getChildCount());
        saveGroups();
    }

    /**
     * Serializes the current named groups (excluding {@code (Ungrouped)}) to
     * {@code TestPlan/.groups}.
     */
    public void saveGroups() {
        List<ScenarioGroup> groups = new ArrayList<>();
        for (TestPlanGroupNode groupNode : getRoot().getGroupNodes()) {
            if (groupNode.isUngrouped()) {
                continue;
            }
            ScenarioGroup group = new ScenarioGroup(groupNode.toString());
            for (ScenarioNode scenarioNode : ScenarioNode.toList(groupNode.children())) {
                group.getScenarios().add(scenarioNode.getScenario().getName());
            }
            groups.add(group);
        }
        ScenarioGroupStore.save(new File(project.getTestPlanPath()), groups);
    }

    /**
     * Converts a flat tree to grouped layout by wrapping every existing scenario
     * node inside a single {@code (Ungrouped)} node.
     */
    private void convertToGrouped() {
        TestPlanNode root = getRoot();
        List<ScenarioNode> existing = ScenarioNode.toList(root.children());
        root.removeAllChildren();
        TestPlanGroupNode ungrouped = new TestPlanGroupNode(TestPlanGroupNode.UNGROUPED, true);
        root.add(ungrouped);
        for (ScenarioNode scenarioNode : existing) {
            ungrouped.add(scenarioNode);
        }
        root.setGrouped(true);
        nodeStructureChanged(root);
    }

    private TestPlanGroupNode ensureUngroupedNode() {
        TestPlanNode root = getRoot();
        TestPlanGroupNode ungrouped = root.getUngroupedNode();
        if (ungrouped == null) {
            ungrouped = new TestPlanGroupNode(TestPlanGroupNode.UNGROUPED, true);
            insertNodeInto(ungrouped, root, root.getChildCount());
        }
        return ungrouped;
    }
}

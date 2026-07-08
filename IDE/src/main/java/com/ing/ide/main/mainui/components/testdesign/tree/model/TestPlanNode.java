package com.ing.ide.main.mainui.components.testdesign.tree.model;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.ScenarioGroup;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.component.utils.ScenarioGroupStore;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.tree.TreeNode;

/**
 *
 *
 */
public class TestPlanNode extends GroupNode {
    public Project project;

    /**
     * {@code true} when the tree is rendered with an intermediate group layer
     * ({@link TestPlanGroupNode} children). When {@code false} the children are
     * {@link ScenarioNode}s directly (backward-compatible flat layout).
     */
    private boolean grouped = false;

    public TestPlanNode() {
        super("TestPlan");
    }

    public void setProject(Project project) {
        removeAllChildren();
        this.project = project;
        setName(project.getName());
        filterTestCases();
    }

    public void filterTestCases() {
        List<ScenarioGroup> groups = ScenarioGroupStore.load(new File(project.getTestPlanPath()));
        grouped = !groups.isEmpty();
        if (grouped) {
            buildGrouped(groups);
        } else {
            buildFlat();
        }
    }

    private void buildFlat() {
        for (Scenario scenario : project.getScenarios()) {
            for (TestCase testCase : scenario.getTestcasesAlone()) {
                add(testCase);
            }
        }
    }

    private void buildGrouped(List<ScenarioGroup> groups) {
        Set<String> assigned = new HashSet<>();
        TestPlanGroupNode ungrouped = new TestPlanGroupNode(TestPlanGroupNode.UNGROUPED, true);
        for (ScenarioGroup group : groups) {
            TestPlanGroupNode groupNode = new TestPlanGroupNode(group.getName(), false);
            add(groupNode);
            for (String scenarioName : group.getScenarios()) {
                Scenario scenario = project.getScenarioByName(scenarioName);
                if (scenario != null) {
                    assigned.add(scenarioName);
                    populate(groupNode, scenario);
                }
            }
        }
        for (Scenario scenario : project.getScenarios()) {
            if (!assigned.contains(scenario.getName())) {
                populate(ungrouped, scenario);
            }
        }
        add(ungrouped);
    }

    private void populate(TestPlanGroupNode group, Scenario scenario) {
        ScenarioNode scNode = group.addScenarioIfNotPresent(scenario);
        for (TestCase testCase : scenario.getTestcasesAlone()) {
            scNode.addTestCaseIfNotPresent(testCase);
        }
    }

    public void add(TestCase testCase) {
        addScenarioIfNotPresent(testCase.getScenario()).addTestCaseIfNotPresent(testCase);
    }

    /**
     * @return whether the tree currently shows an intermediate group layer.
     */
    public boolean isGrouped() {
        return grouped;
    }

    /**
     * @return whether the tree currently shows an intermediate group layer.
     */
    public boolean hasGroups() {
        return grouped;
    }

    void setGrouped(boolean grouped) {
        this.grouped = grouped;
    }

    /**
     * Finds a (named or ungrouped) group child by its display name.
     * @param name group name
     * @return matching node or {@code null}
     */
    public TestPlanGroupNode getGroupByName(String name) {
        for (TreeNode child : Collections.list(children())) {
            if (child instanceof TestPlanGroupNode && child.toString().equals(name)) {
                return (TestPlanGroupNode) child;
            }
        }
        return null;
    }

    /**
     * @return the implicit {@code (Ungrouped)} node, or {@code null} when not grouped.
     */
    public TestPlanGroupNode getUngroupedNode() {
        for (TreeNode child : Collections.list(children())) {
            if (child instanceof TestPlanGroupNode && ((TestPlanGroupNode) child).isUngrouped()) {
                return (TestPlanGroupNode) child;
            }
        }
        return null;
    }

    /**
     * @return all group children (named groups plus the ungrouped node), in tree order.
     */
    public List<TestPlanGroupNode> getGroupNodes() {
        List<TestPlanGroupNode> list = new ArrayList<>();
        for (TreeNode child : Collections.list(children())) {
            if (child instanceof TestPlanGroupNode) {
                list.add((TestPlanGroupNode) child);
            }
        }
        return list;
    }

    /**
     * Locates a scenario node, transparently descending through the group layer
     * when the tree is grouped.
     * @param scenario scenario to find
     * @return matching node or {@code null}
     */
    @Override
    public ScenarioNode getScenarioNodeBy(Scenario scenario) {
        if (!grouped) {
            return super.getScenarioNodeBy(scenario);
        }
        for (TestPlanGroupNode group : getGroupNodes()) {
            ScenarioNode sNode = group.getScenarioNodeBy(scenario);
            if (sNode != null) {
                return sNode;
            }
        }
        return null;
    }

    /**
     * @return every scenario node in the tree, flattened through the group layer.
     */
    public List<ScenarioNode> getAllScenarioNodes() {
        List<ScenarioNode> result = new ArrayList<>();
        if (!grouped) {
            result.addAll(ScenarioNode.toList(children()));
        } else {
            for (TestPlanGroupNode group : getGroupNodes()) {
                result.addAll(ScenarioNode.toList(group.children()));
            }
        }
        return result;
    }

    @Override
    public boolean rename(String name) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}

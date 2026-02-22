
package com.ing.ide.main.mainui.components.testdesign.tree;

import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.ide.main.mainui.components.testdesign.TestDesign;
import com.ing.ide.main.mainui.components.testdesign.tree.model.GroupNode;
import com.ing.ide.main.mainui.components.testdesign.tree.model.ReusableTreeModel;
import com.ing.ide.main.mainui.components.testdesign.tree.model.ScenarioNode;
import com.ing.ide.main.mainui.components.testdesign.tree.model.TestCaseNode;
import com.ing.ide.main.utils.keys.Keystroke;
import com.ing.ide.util.Notification;
import com.ing.ide.util.Validator;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JCheckBox;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.tree.TreePath;

/**
 *
 *
 */
public class ReusableTree extends ProjectTree {

    private static final Logger LOGGER = Logger.getLogger(ReusableTree.class.getName());

    public ReusableTree(TestDesign testDesign) {
        super(testDesign);
    }

    @Override
    protected ReusableTreeModel getNewTreeModel() {
        return new ReusableTreeModel();
    }

    @Override
    ReusablePopupMenu getNewPopupMenu() {
        return new ReusablePopupMenu();
    }

    @Override
    public ReusableTreeModel getTreeModel() {
        return (ReusableTreeModel) super.getTreeModel();
    }

    @Override
    public void loadTableModelForSelection() {
        Object selected = getSelectedTestCase();
        if (selected != null) {
            super.loadTableModelForSelection();
        }
    }

    @Override
    protected void togglePopupMenu(Object selected) {
        if (isRootSelected()) {
            ((ReusablePopupMenu) popupMenu).forRoot();
        } else {
            super.togglePopupMenu(selected);
        }
    }

    @Override
    protected void onNewAction() {
        if (isRootSelected()) {
            addGroup();
        } else if (getSelectedScenarioNodeSafe() != null) {
            addReusableTestCase();
        } else if (getSelectedGroupNode() != null) {
            addReusableScenario();
        } else {
            super.onNewAction();
        }
    }

    @Override
    protected void onDeleteAction() {
        deleteGroups();
        super.onDeleteAction();
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        switch (ae.getActionCommand()) {
            case "Add Group":
                addGroup();
                break;
            case "Add Scenario":
                addReusableScenario();
                break;
            case "Add TestCase":
                addReusableTestCase();
                break;
            case "Rename Group":
                getTree().startEditingAtPath(new TreePath(getSelectedGroupNode().getPath()));
                break;
            case "Delete Group":
                deleteGroups();
                break;
            default:
                super.actionPerformed(ae);
        }
    }

    @Override
    protected Boolean checkAndRename() {
        String name = getTree().getCellEditor().getCellEditorValue().toString().trim();
        if (Validator.isValidName(name)) {
            GroupNode group = getSelectedGroupNode();
            if (group != null && !group.toString().equals(name)) {
                if (group.rename(name)) {
                    return true;
                } else {
                    Notification.show("Scenario " + name + " Already present");
                    return false;
                }
            }
        }
        return super.checkAndRename();
    }

    @Override
    void renameScenario(Scenario scenario) {
        getTestDesign().getProjectTree()
                .getTreeModel().onScenarioRename(scenario);
    }

    @Override
    void makeAsReusableRTestCase(TestCase testCase) {
        if (getProject().moveTestCaseToTestPlan(testCase)) {
            getProject().reload();
            getTestDesign().getProjectTree().load();
            load();
        } else {
            Notification.show("Unable to move test case to TestPlan");
        }
    }

    private void addGroup() {
        selectAndScrollTo(new TreePath(getTreeModel().addGroup(fetchNewGroupName()).getPath()));
    }

    private void addReusableScenario() {
        ScenarioNode scNode = getTreeModel().addScenario(getSelectedGroupNode(),
                getProject().addReusableScenario(fetchNewReusableScenarioName()));
        if (scNode != null) {
            selectAndScrollTo(new TreePath(scNode.getPath()));
        }
    }

    private void addReusableTestCase() {
        ScenarioNode scenarioNode = getSelectedScenarioNodeSafe();
        if (scenarioNode != null) {
            String testCaseName = fetchNewReusableTestCaseName(scenarioNode.getScenario());
            TestCase testcase = scenarioNode.getScenario().addTestCase(testCaseName);
            if (testcase != null) {
                getTestDesign().loadTableModelForSelection(testcase);
                selectAndScrollTo(new TreePath(getTreeModel().addTestCase(scenarioNode, testcase).getPath()));
            } else {
                Notification.show("Reusable test case already exists");
            }
        }
    }

    private ScenarioNode getSelectedScenarioNodeSafe() {
        List<ScenarioNode> nodes = getSelectedScenarioNodes();
        if (nodes.isEmpty()) {
            return null;
        }
        return nodes.get(0);
    }

    private void deleteGroups() {
        List<GroupNode> groupNodes = getSelectedGroupNodes();
        if (!groupNodes.isEmpty()) {

            String question = "<html><body><p style='width: 200px;'>"
                    + "Are you sure want to delete the following Groups?<br>"
                    + groupNodes
                    + "</p></body></html>";

            JCheckBox confirmBox = new JCheckBox("Move Reusables inside Group to TestPlan instead of deleting");

            int option = JOptionPane.showConfirmDialog(null,
                    new Object[]{question, confirmBox},
                    "Delete TestCase",
                    JOptionPane.YES_NO_OPTION);
            if (option == JOptionPane.YES_OPTION) {
                LOGGER.log(Level.INFO, "Delete Reusable Groups approved for {0}; {1}",
                        new Object[]{groupNodes.size(), groupNodes});
                for (GroupNode groupNode : groupNodes) {
                    if (confirmBox.isSelected()) {
                        getTreeModel().toggleAllTestCasesFrom(groupNode);
                    } else {
                        for (ScenarioNode scenarioNode : ScenarioNode.toList(groupNode.children())) {
                            for (TestCaseNode testCaseNode : TestCaseNode.toList(scenarioNode.children())) {
                                testCaseNode.getTestCase().delete();
                            }
                        }
                    }
                    getTreeModel().removeNodeFromParent(groupNode);
                }

                getProject().reload();
                getTestDesign().getProjectTree().load();
                load();
            }
        }
    }

    private String fetchNewGroupName() {
        String newGroupName = "NewGroup";
        for (int i = 0;; i++) {
            if (getTreeModel().getRoot().getGroupByName(newGroupName) == null) {
                break;
            }
            newGroupName = "NewGroup" + i;
        }
        return newGroupName;
    }

    private String fetchNewReusableScenarioName() {
        String newScenarioName = "NewScenario";
        for (int i = 0;; i++) {
            if (getProject().getReusableScenarioByName(newScenarioName) == null) {
                break;
            }
            newScenarioName = "NewScenario" + i;
        }
        return newScenarioName;
    }

    private String fetchNewReusableTestCaseName(Scenario scenario) {
        String newTestCaseName = "NewTestCase";
        for (int i = 0;; i++) {
            if (scenario.getTestCaseByName(newTestCaseName) == null
                    && !getProject().hasTestCaseInAnyScenario(scenario.getName(), newTestCaseName)) {
                break;
            }
            newTestCaseName = "NewTestCase" + i;
        }
        return newTestCaseName;
    }

    private Boolean isRootSelected() {
        TreePath path = getTree().getSelectionPath();
        if (path != null) {
            return path.getLastPathComponent().equals(getTreeModel().getRoot());
        }
        return false;
    }

    public void save() {
        getTreeModel().save();
    }

    class ReusablePopupMenu extends ProjectPopupMenu {

        JMenuItem addGroup;
        JMenuItem renameGroup;
        JMenuItem deleteGroup;

        public ReusablePopupMenu() {
            initMenu();
        }

        private void initMenu() {
            removeAll();
            add(addGroup = create("Add Group", Keystroke.NEW));
            add(renameGroup = create("Rename Group", Keystroke.RENAME));
            add(deleteGroup = create("Delete Group", Keystroke.DELETE));
            addSeparator();
            super.init();
            toggleReusable.setText("Make As TestCase");
        }

        @Override
        protected void forTestCase() {
            super.forTestCase();
            addGroup.setEnabled(false);
            renameGroup.setEnabled(false);
            deleteGroup.setEnabled(false);
        }

        @Override
        protected void forScenario() {
            super.forScenario();
            addGroup.setEnabled(false);
            renameGroup.setEnabled(false);
            deleteGroup.setEnabled(false);
        }

        @Override
        protected void forTestPlan() {
            super.forTestPlan();
            addGroup.setEnabled(false);
            renameGroup.setEnabled(true);
            deleteGroup.setEnabled(true);
        }

        protected void forRoot() {
            super.forTestPlan();
            addScenario.setEnabled(false);
            addGroup.setEnabled(true);
            renameGroup.setEnabled(false);
            deleteGroup.setEnabled(false);
        }
    }

}

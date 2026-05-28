
package com.ing.ide.main.mainui.components.testdesign.tree;

import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.exception.TestCaseConversionException;
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
 * UI tree component for displaying and managing Reusable Components scenarios and test cases.
 * Extends ProjectTree and overrides methods to handle reusable-specific operations.
 */
public class ReusableTree extends ProjectTree {

    private static final Logger LOGGER = Logger.getLogger(ReusableTree.class.getName());

    /**
     * Constructs a new ReusableTree for managing Reusable Components scenarios and test cases.
     * @param testDesign parent TestDesign component
     */
    public ReusableTree(TestDesign testDesign) {
        super(testDesign);
    }

    /**
     * Creates a new tree model for Reusable Components.
     * @return new ReusableTreeModel instance
     */
    @Override
    protected ReusableTreeModel getNewTreeModel() {
        return new ReusableTreeModel();
    }

    /**
     * Creates a new popup menu for the reusable tree.
     * @return new ReusablePopupMenu instance
     */
    @Override
    ReusablePopupMenu getNewPopupMenu() {
        return new ReusablePopupMenu();
    }

    /**
     * Returns the reusable tree model.
     * @return reusable tree model
     */
    @Override
    public ReusableTreeModel getTreeModel() {
        return (ReusableTreeModel) super.getTreeModel();
    }

    /**
     * Loads the table model for the selected test case only.
     */
    @Override
    public void loadTableModelForSelection() {
        Object selected = getSelectedTestCase();
        if (selected != null) {
            super.loadTableModelForSelection();
        }
    }

    /**
     * Toggles the popup menu based on selected node type, with special handling for root.
     * @param selected selected tree node
     */
    @Override
    protected void togglePopupMenu(Object selected) {
        if (isRootSelected()) {
            ((ReusablePopupMenu) popupMenu).forRoot();
        } else {
            super.togglePopupMenu(selected);
        }
    }

    /**
     * Handles the "New" action for groups, scenarios, and test cases.
     */
    @Override
    protected void onNewAction() {
        if (isRootSelected()) {
            // addGroup();
        } else if (getSelectedScenarioNodeSafe() != null) {
            addReusableTestCase();
        } else if (getSelectedGroupNode() != null) {
            addReusableScenario();
        } else {
            super.onNewAction();
        }
    }

    /**
     * Handles the "Delete" action for groups and other items.
     */
    @Override
    protected void onDeleteAction() {
        deleteGroups();
        super.onDeleteAction();
    }

    /**
     * Handles action events specific to reusable components (groups, scenarios, test cases).
     * @param ae action event
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        switch (ae.getActionCommand()) {
            // case "Add Group":
            //     addGroup();
            //     break;
            case "Add Scenario":
                addReusableScenario();
                break;
            case "Add TestCase":
                addReusableTestCase();
                break;
            // case "Rename Group":
            //     getTree().startEditingAtPath(new TreePath(getSelectedGroupNode().getPath()));
            //     break;
            // case "Delete Group":
            //     deleteGroups();
            //     break;
            default:
                super.actionPerformed(ae);
        }
    }

    /**
     * Validates and performs rename operation with group name validation.
     * @return true if rename was successful, false otherwise
     */
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
            ScenarioNode scenarioNode = super.getSelectedScenarioNode();
            if (scenarioNode != null && !scenarioNode.toString().equals(name)) {
                if (scenarioNode.getScenario().renameReusable(name)) {
                    getTreeModel().reload(scenarioNode);
                    renameScenario(scenarioNode.getScenario());
                    super.getTestDesign().getScenarioComp().refreshTitle();
                    return true;
                } else {
                    Notification.show("Scenario " + name + " Already present");
                    return false;
                }
            }
            TestCaseNode testCaseNode = super.getSelectedTestCaseNode();
            if (testCaseNode != null && !testCaseNode.toString().equals(name)) {
                if (testCaseNode.getTestCase().renameReusable(name)) {
                    getTreeModel().reload(testCaseNode);
                    super.getTestDesign().getTestCaseComp().refreshTitle();
                    return true;
                } else {
                    Notification.show("Testcase '" + name + "' Already present in Scenario - " + getSelectedTestCase().getScenario().getName());
                }
            }
        }
        return false;
    }

    /**
     * Notifies the project tree that a scenario has been renamed.
     * @param scenario renamed scenario
     */
    @Override
    void renameScenario(Scenario scenario) {
        getTestDesign().getProjectTree()
                .getTreeModel().onScenarioRename(scenario);
    }

    /**
     * Moves selected test cases from Reusable Components to Test Plan.
     * Shows error notifications for failures and reloads both trees on success.
     */
    @Override
    protected void makeAsReusableRTestCase() {
        if (!getSelectedTestCaseNodes().isEmpty()) {
            // Save ALL test cases to prevent data loss on reload
            getProject().save();
            
            boolean anySuccess = false;
            for (TestCaseNode testCaseNode : getSelectedTestCaseNodes()) {
                try {
                    getProject().moveTestCaseToTestPlan(testCaseNode.getTestCase());
                    anySuccess = true;
                } catch (TestCaseConversionException e) {
                    Notification.show(e.getMessage());
                }
            }
            if (anySuccess) {
                getProject().reload();
                getProject().save();
                getTestDesign().getProjectTree().load();
                load();
            }
        }
    }

    /**
     * Moves a test case from reusable to test plan and reloads trees.
     * @param testCase test case to move
     */
    @Override
    void makeAsReusableRTestCase(TestCase testCase) {
        // Save ALL test cases to prevent data loss on reload
        getProject().save();
        
        try {
            getProject().moveTestCaseToTestPlan(testCase);
            getProject().reload();
            getProject().save();
            getTestDesign().getProjectTree().load();
            load();
        } catch (TestCaseConversionException e) {
            Notification.show(e.getMessage());
        }
    }

    /**
     * Adds a new group to the reusable tree.
     */
    private void addGroup() {
        selectAndScrollTo(new TreePath(getTreeModel().addGroup(fetchNewGroupName()).getPath()));
    }

    /**
     * Adds a new reusable scenario to the selected group.
     */
    private void addReusableScenario() {
        ScenarioNode scNode = getTreeModel().addScenario(getSelectedGroupNode(),
                getProject().addReusableScenario(fetchNewReusableScenarioName()));
        if (scNode != null) {
            selectAndScrollTo(new TreePath(scNode.getPath()));
        }
    }

    /**
     * Adds a new reusable test case to the selected scenario.
     */
    /**
     * Adds a new reusable test case to the selected scenario.
     */
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

    /**
     * Returns the first selected scenario node safely.
     * @return selected ScenarioNode or null if none selected
     */
    private ScenarioNode getSelectedScenarioNodeSafe() {
        List<ScenarioNode> nodes = getSelectedScenarioNodes();
        if (nodes.isEmpty()) {
            return null;
        }
        return nodes.get(0);
    }

    /**
     * Deletes selected groups with option to move test cases to Test Plan.
     */
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

    /**
     * Generates a unique name for a new group.
     * @return unique group name
     */
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

    /**
     * Generates a unique name for a new reusable scenario.
     * @return unique scenario name
     */
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

    /**
     * Generates a unique name for a new reusable test case.
     * @param scenario scenario to check for existing test case names
     * @return unique test case name
     */
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

    /**
     * Checks if the root node is selected.
     * @return true if root is selected, false otherwise
     */
    private Boolean isRootSelected() {
        TreePath path = getTree().getSelectionPath();
        if (path != null) {
            return path.getLastPathComponent().equals(getTreeModel().getRoot());
        }
        return false;
    }

    /**
     * Saves the reusable tree model state.
     */
    public void save() {
        getTreeModel().save();
    }

    /**
     * Context menu for the reusable tree with group-specific actions.
     */
    class ReusablePopupMenu extends ProjectPopupMenu {

        // JMenuItem addGroup;
        // JMenuItem renameGroup;
        // JMenuItem deleteGroup;

        /**
         * Constructs a new ReusablePopupMenu and initializes menu items.
         */
        public ReusablePopupMenu() {
            initMenu();
        }

        /**
         * Initializes all menu items specific to reusable components.
         */
        private void initMenu() {
            removeAll();
            // add(addGroup = create("Add Group", Keystroke.NEW));
            // add(renameGroup = create("Rename Group", Keystroke.RENAME));
            // add(deleteGroup = create("Delete Group", Keystroke.DELETE));
            // addSeparator();
            super.init();
            toggleReusable.setText("Make As TestCase");
        }

        /**
         * Configures menu items for test case context in reusable tree.
         */
        @Override
        protected void forTestCase() {
            super.forTestCase();
            // addGroup.setEnabled(false);
            // renameGroup.setEnabled(false);
            // deleteGroup.setEnabled(false);
        }

        /**
         * Configures menu items for scenario context in reusable tree.
         */
        @Override
        protected void forScenario() {
            super.forScenario();
            // addGroup.setEnabled(false);
            // renameGroup.setEnabled(false);
            // deleteGroup.setEnabled(false);
        }

        /**
         * Configures menu items for test plan (group) context in reusable tree.
         */
        @Override
        protected void forTestPlan() {
            super.forTestPlan();
            // addGroup.setEnabled(false);
            // renameGroup.setEnabled(true);
            // deleteGroup.setEnabled(true);
        }

        /**
         * Configures menu items for root context in reusable tree.
         */
        protected void forRoot() {
            super.forTestPlan();
            addScenario.setEnabled(false);
            // addGroup.setEnabled(true);
            // renameGroup.setEnabled(false);
            // deleteGroup.setEnabled(false);
        }
    }

}

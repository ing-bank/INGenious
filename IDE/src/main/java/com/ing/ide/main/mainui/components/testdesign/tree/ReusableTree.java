package com.ing.ide.main.mainui.components.testdesign.tree;

import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.exception.TestCaseConversionException;
import com.ing.ide.main.mainui.components.testdesign.TestDesign;
import com.ing.ide.main.mainui.components.testdesign.tree.model.GroupNode;
import com.ing.ide.main.mainui.components.testdesign.tree.model.ReusableTreeModel;
import com.ing.ide.main.mainui.components.testdesign.tree.model.ScenarioNode;
import com.ing.ide.main.mainui.components.testdesign.tree.model.TestCaseNode;
import com.ing.ide.util.Notification;
import com.ing.ide.util.Validator;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
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
        } else if (getSelectedScenarioNodeSafe() != null || getSelectedTestCaseNode() != null) {
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
            case "Make As Shared Reusable":
                moveToSharedReusable();
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
                    Notification.show(
                        "Testcase '" +
                        name +
                        "' Already present in Scenario - " +
                        getSelectedTestCase().getScenario().getName()
                    );
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
        getTestDesign().getProjectTree().getTreeModel().onScenarioRename(scenario);
    }

    /**
     * Moves selected test cases from Reusable Components to Test Plan.
     * Shows error notifications for failures and reloads both trees on success.
     */
    @Override
    protected void makeAsReusableRTestCase() {
        if (getSelectedTestCaseNodes().isEmpty()) {
            Notification.showWarning("Select at least one reusable test case to make as TestCase.");
            return;
        }
        if (!getSelectedTestCaseNodes().isEmpty()) {
            // Save ALL test cases to prevent data loss on reload
            getProject().save();

            boolean anySuccess = false;
            int impactedUpdates = 0;
            for (TestCaseNode testCaseNode : getSelectedTestCaseNodes()) {
                try {
                    getProject().moveTestCaseToTestPlan(testCaseNode.getTestCase());
                    impactedUpdates +=
                        getProject().getAndResetLastImpactedReusableReferenceUpdates();
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
                showImpactedReferenceNotification("Moved to Test Plan", impactedUpdates);
            } else {
                Notification.showWarning("No reusable test cases were moved to Test Plan.");
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
            int impactedUpdates = getProject().getAndResetLastImpactedReusableReferenceUpdates();
            getProject().reload();
            getProject().save();
            getTestDesign().getProjectTree().load();
            load();
            showImpactedReferenceNotification("Moved to Test Plan", impactedUpdates);
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
        String scenarioName = fetchNewReusableScenarioName();
        Scenario scenario = getProject().addReusableScenario(scenarioName);
        if (scenario == null) {
            Notification.showWarning(
                "Scenario '" +
                scenarioName +
                "' already exists in Project Reusables. Please choose a different Project Reusable scenario name."
            );
            return;
        }
        ScenarioNode scNode = getTreeModel().addScenario(getSelectedGroupNode(), scenario);
        if (scNode != null) {
            selectAndScrollTo(new TreePath(scNode.getPath()));
            persistSortOrder(scNode.getParent());
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
        if (scenarioNode == null) {
            TestCaseNode tcNode = getSelectedTestCaseNode();
            if (tcNode != null && tcNode.getParent() instanceof ScenarioNode) {
                scenarioNode = (ScenarioNode) tcNode.getParent();
            }
        }
        if (scenarioNode != null) {
            String testCaseName = fetchNewReusableTestCaseName(scenarioNode.getScenario());
            TestCase testcase = scenarioNode.getScenario().addTestCase(testCaseName);
            if (testcase != null) {
                getTestDesign().loadTableModelForSelection(testcase);
                selectAndScrollTo(
                    new TreePath(getTreeModel().addTestCase(scenarioNode, testcase).getPath())
                );
                persistSortOrder(scenarioNode);
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
            JPanel messagePanel = new JPanel(new java.awt.BorderLayout(0, 8));
            messagePanel.add(
                new JLabel("Are you sure want to delete the following Groups?"),
                java.awt.BorderLayout.NORTH
            );

            JTextArea groupsArea = new JTextArea();
            groupsArea.setEditable(false);
            groupsArea.setLineWrap(false);
            groupsArea.setWrapStyleWord(false);

            StringBuilder content = new StringBuilder();
            for (GroupNode groupNode : groupNodes) {
                content.append(groupNode).append(System.lineSeparator());
            }
            groupsArea.setText(content.toString());
            groupsArea.setCaretPosition(0);

            JScrollPane scrollPane = new JScrollPane(groupsArea);
            scrollPane.setPreferredSize(new Dimension(360, 180));
            messagePanel.add(scrollPane, java.awt.BorderLayout.CENTER);

            JCheckBox confirmBox = new JCheckBox(
                "Move Reusables inside Group to TestPlan instead of deleting"
            );

            int option = JOptionPane.showConfirmDialog(
                null,
                new Object[] { messagePanel, confirmBox },
                "Delete TestCase",
                JOptionPane.YES_NO_OPTION
            );
            if (option == JOptionPane.YES_OPTION) {
                LOGGER.log(
                    Level.INFO,
                    "Delete Reusable Groups approved for {0}; {1}",
                    new Object[] { groupNodes.size(), groupNodes }
                );
                for (GroupNode groupNode : groupNodes) {
                    if (confirmBox.isSelected()) {
                        getTreeModel().toggleAllTestCasesFrom(groupNode);
                    } else {
                        for (ScenarioNode scenarioNode : ScenarioNode.toList(
                            groupNode.children()
                        )) {
                            for (TestCaseNode testCaseNode : TestCaseNode.toList(
                                scenarioNode.children()
                            )) {
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
     * Returns a unique reusable scenario name by checking all scopes.
     * @return unique scenario name
     */
    private String fetchNewReusableScenarioName() {
        String base = "NewScenario";
        // prefer plain base name if available (and not present in the tree)
        if (getProject().getReusableScenarioByName(base) == null && !treeHasScenarioName(base)) {
            return base;
        }
        int i = 0;
        String newScenarioName;
        for (;;) {
            newScenarioName = base + i;
            if (
                getProject().getReusableScenarioByName(newScenarioName) == null &&
                !treeHasScenarioName(newScenarioName)
            ) {
                break;
            }
            i++;
        }
        return newScenarioName;
    }

    private boolean treeHasScenarioName(String name) {
        if (getTreeModel() == null || getTreeModel().getRoot() == null) return false;
        for (GroupNode group : GroupNode.toList(getTreeModel().getRoot().children())) {
            for (ScenarioNode sc : ScenarioNode.toList(group.children())) {
                if (sc.getScenario().getName().equalsIgnoreCase(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Generates a unique name for a new reusable test case.
     * @param scenario scenario to check for existing test case names
     * @return unique test case name
     */
    private String fetchNewReusableTestCaseName(Scenario scenario) {
        String newTestCaseName = "NewTestCase";
        for (int i = 0;; i++) {
            if (scenario.getTestCaseByName(newTestCaseName) == null) {
                break;
            }
            newTestCaseName = "NewTestCase" + i;
        }
        return newTestCaseName;
    }

    private List<TestCase> collectSelectedReusableTestCases() {
        List<TestCase> selected = new ArrayList<>();
        for (TestCaseNode tcNode : getSelectedTestCaseNodes()) {
            selected.add(tcNode.getTestCase());
        }
        for (ScenarioNode scenarioNode : getSelectedScenarioNodes()) {
            selected.addAll(scenarioNode.getScenario().getTestCases());
        }
        return selected;
    }

    private void moveToSharedReusable() {
        List<TestCase> selected = collectSelectedReusableTestCases();
        if (selected.isEmpty()) {
            Notification.showWarning(
                "Select at least one reusable test case to make as Shared Reusable."
            );
            return;
        }

        int success = 0;
        int impactedUpdates = 0;

        // First confirm the high-level intent to make selected reusables Shared
        int option = JOptionPane.showConfirmDialog(
            null,
            "Move selected reusable test case(s) to Shared Reusable Components?",
            "Make As Shared Reusable",
            JOptionPane.YES_NO_OPTION
        );
        if (option != JOptionPane.YES_OPTION) {
            return;
        }

        // Ask project tree helper to detect/move project objects for all selected testcases
        if (!getTestDesign().getProjectTree().confirmAndMoveProjectObjectsForTestCases(selected)) {
            return; // user cancelled in helper
        }

        for (TestCase tc : selected) {
            try {
                getProject().moveTestCaseToSharedReusable(tc);
                impactedUpdates += getProject().getAndResetLastImpactedReusableReferenceUpdates();
                success++;
            } catch (TestCaseConversionException e) {
                Notification.show(e.getMessage());
            }
        }

        if (success > 0) {
            getProject().reload();
            getTestDesign().getReusableTree().load();
            getTestDesign().getSharedReusableTree().load();
            showImpactedReferenceNotification("Moved to Shared Reusable", impactedUpdates);
        } else {
            Notification.showWarning("No reusable test cases were moved to Shared Reusable.");
        }
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
            toggleTestCase.setVisible(true);
            toggleProjectReusable.setVisible(false);
            toggleSharedReusable.setVisible(true);
        }

        /**
         * Configures menu items for test case context in reusable tree.
         */
        @Override
        protected void forTestCase() {
            super.forTestCase();
            toggleTestCase.setEnabled(true);
            toggleSharedReusable.setEnabled(true);
            toggleProjectReusable.setEnabled(false);
            setGroupItemsVisible(false, false, false, false);
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
            toggleTestCase.setEnabled(false);
            toggleSharedReusable.setEnabled(false);
            toggleProjectReusable.setEnabled(false);
            setGroupItemsVisible(false, false, false, false);
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
            toggleTestCase.setEnabled(false);
            toggleSharedReusable.setEnabled(false);
            toggleProjectReusable.setEnabled(false);
            setGroupItemsVisible(false, false, false, false);
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
            toggleTestCase.setEnabled(false);
            toggleSharedReusable.setEnabled(false);
            toggleProjectReusable.setEnabled(false);
            setGroupItemsVisible(false, false, false, false);
            // addGroup.setEnabled(true);
            // renameGroup.setEnabled(false);
            // deleteGroup.setEnabled(false);
        }
    }
}

package com.ing.ide.main.mainui.components.testdesign.tree;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.exception.TestCaseConversionException;
import com.ing.ide.main.mainui.components.testdesign.TestDesign;
import com.ing.ide.main.mainui.components.testdesign.tree.model.GroupNode;
import com.ing.ide.main.mainui.components.testdesign.tree.model.ScenarioNode;
import com.ing.ide.main.mainui.components.testdesign.tree.model.SharedReusableTreeModel;
import com.ing.ide.main.mainui.components.testdesign.tree.model.TestCaseNode;
import com.ing.ide.util.Notification;
import com.ing.ide.util.Validator;
import java.awt.event.ActionEvent;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.tree.TreePath;

/**
 * UI tree component for displaying and managing Shared Reusable Components scenarios and test cases.
 * Extends ProjectTree and overrides methods to handle shared reusable-specific operations.
 * Displays components with [Shared] scope indicator in the UI.
 */
public class SharedReusableTree extends ProjectTree {
    private static final Logger LOGGER = Logger.getLogger(SharedReusableTree.class.getName());

    /**
     * Constructs a new SharedReusableTree for managing Shared Reusable Components scenarios and test cases.
     * @param testDesign parent TestDesign component
     */
    public SharedReusableTree(TestDesign testDesign) {
        super(testDesign);
    }

    /**
     * Creates a new tree model for Shared Reusable Components.
     * @return new SharedReusableTreeModel instance
     */
    @Override
    protected SharedReusableTreeModel getNewTreeModel() {
        return new SharedReusableTreeModel();
    }

    /**
     * Creates a new popup menu for the shared reusable tree.
     * @return new SharedReusablePopupMenu instance
     */
    @Override
    SharedReusablePopupMenu getNewPopupMenu() {
        return new SharedReusablePopupMenu();
    }

    /**
     * Returns the shared reusable tree model.
     * @return shared reusable tree model
     */
    @Override
    public SharedReusableTreeModel getTreeModel() {
        return (SharedReusableTreeModel) super.getTreeModel();
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
            ((SharedReusablePopupMenu) popupMenu).forRoot();
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
            addSharedReusableTestCase();
        } else if (getSelectedGroupNode() != null) {
            addSharedReusableScenario();
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
        if (!getSelectedTestCaseNodes().isEmpty() || !getSelectedScenarioNodes().isEmpty()) {
            // collect scenario names referenced by the selection
            List<String> selectedScenarioNames = new ArrayList<>();
            for (ScenarioNode sn : getSelectedScenarioNodes()) {
                selectedScenarioNames.add(sn.getScenario().getName());
            }
            for (TestCaseNode tcn : getSelectedTestCaseNodes()) {
                selectedScenarioNames.add(tcn.getTestCase().getScenario().getName());
            }

            // find projects that reference any of the selected shared scenarios
            List<String> referencingProjects = new ArrayList<>();
            for (String scnName : selectedScenarioNames) {
                List<String> projs = findProjectsUsingSharedScenario(scnName);
                for (String p : projs) {
                    if (!referencingProjects.contains(p)) referencingProjects.add(p);
                }
            }

            String message;
            if (!referencingProjects.isEmpty()) {
                message =
                    "The selected shared reusable components are referenced by the following projects:\n\n" +
                    String.join("\n", referencingProjects) +
                    "\n\nAre you sure you want to delete the selected shared reusable(s)?";
            } else {
                message = "Warning: You are deleting Shared Reusable component(s). Continue?";
            }

            int warning = JOptionPane.showConfirmDialog(
                null,
                message,
                "Shared Reusable Delete Warning",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );
            if (warning != JOptionPane.YES_OPTION) {
                return;
            }
        }
        super.onDeleteAction();
    }

    /**
     * Handles action events specific to shared reusable components.
     * @param ae action event
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        switch (ae.getActionCommand()) {
            case "Add Scenario":
                addSharedReusableScenario();
                break;
            case "Add TestCase":
                addSharedReusableTestCase();
                break;
            case "Make As Project Reusable":
            case "Move to Project Reusable":
                moveToProjectReusable();
                break;
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
                // Warn about projects that reference this shared reusable scenario
                var projects = findProjectsUsingSharedScenario(
                    scenarioNode.getScenario().getName()
                );
                if (!projects.isEmpty()) {
                    String msg =
                        "The shared reusable scenario '" +
                        scenarioNode.getScenario().getName() +
                        "'\n" +
                        "is referenced by the following projects:\n\n" +
                        String.join("\n", projects) +
                        "\n\nDo you want to continue renaming it to '" +
                        name +
                        "'?";
                    int opt = javax.swing.JOptionPane.showConfirmDialog(
                        null,
                        msg,
                        "Rename Shared Reusable - References Found",
                        javax.swing.JOptionPane.YES_NO_OPTION,
                        javax.swing.JOptionPane.WARNING_MESSAGE
                    );
                    if (opt != javax.swing.JOptionPane.YES_OPTION) {
                        return false;
                    }
                }
                if (scenarioNode.getScenario().renameSharedReusable(name)) {
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
                // Warn about projects that reference this shared reusable test case's scenario
                var projects = findProjectsUsingSharedScenario(
                    testCaseNode.getTestCase().getScenario().getName()
                );
                if (!projects.isEmpty()) {
                    String msg =
                        "The shared reusable test case '" +
                        testCaseNode.getTestCase().getName() +
                        "'\n" +
                        "(scenario: '" +
                        testCaseNode.getTestCase().getScenario().getName() +
                        "') is referenced by the following projects:\n\n" +
                        String.join("\n", projects) +
                        "\n\nDo you want to continue renaming it to '" +
                        name +
                        "'?";
                    int opt = javax.swing.JOptionPane.showConfirmDialog(
                        null,
                        msg,
                        "Rename Shared Reusable - References Found",
                        javax.swing.JOptionPane.YES_NO_OPTION,
                        javax.swing.JOptionPane.WARNING_MESSAGE
                    );
                    if (opt != javax.swing.JOptionPane.YES_OPTION) {
                        return false;
                    }
                }
                if (testCaseNode.getTestCase().renameSharedReusable(name)) {
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
     * Reads the projects.items JSON file from SharedReusableComponents and returns a list of
     * projects that use any shared reusable, excluding the currently loaded project.
     * Format: JSON array of objects with "name" and "path" fields.
     *
     * @param scenarioName shared reusable scenario name (not used for filtering, kept for compatibility)
     * @return list of project display names (excluding current project) that reference shared reusables
     */
    private List<String> findProjectsUsingSharedScenario(String scenarioName) {
        List<String> result = new ArrayList<>();
        try {
            // Get current loaded project to exclude it from display
            Project currentProject = getTestDesign().getProject();
            String currentProjectName = currentProject != null ? currentProject.getName() : null;
            String currentProjectPath = currentProject != null
                ? currentProject.getLocation()
                : null;

            Path projectsFile = Paths.get(
                Project.getSharedReusableComponentsPath(),
                "projects.items"
            );
            if (!Files.exists(projectsFile)) return result;

            String content = new String(Files.readAllBytes(projectsFile), StandardCharsets.UTF_8);
            if (content == null || content.trim().isEmpty()) return result;

            // Parse JSON array
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.List<java.util.Map<String, String>> projects = mapper.readValue(
                content,
                mapper
                    .getTypeFactory()
                    .constructCollectionType(java.util.List.class, java.util.Map.class)
            );

            // Format and display all projects EXCEPT the currently loaded one
            for (java.util.Map<String, String> proj : projects) {
                String projName = proj.get("name");
                String projPath = proj.get("path");
                if (projName == null || projName.isEmpty()) continue;

                // Skip if this is the currently loaded project
                if (
                    projName.equals(currentProjectName) &&
                    projPath != null &&
                    projPath.equals(currentProjectPath)
                ) {
                    continue;
                }

                // Add to results
                if (projPath != null && !projPath.isEmpty()) {
                    result.add(projName + " | " + projPath);
                } else {
                    result.add(projName);
                }
            }
        } catch (Exception e) {
            // best-effort: ignore and return empty list
            LOGGER.log(Level.FINE, "Failed to read Shared projects.items JSON", e);
        }
        return result;
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
     * Moves selected test cases from Shared Reusable Components to Test Plan.
     * Shows error notifications for failures and reloads both trees on success.
     */
    @Override
    protected void makeAsReusableRTestCase() {
        if (getSelectedTestCaseNodes().isEmpty()) {
            Notification.showWarning(
                "Select at least one shared reusable test case to make as TestCase."
            );
            return;
        }
        if (!getSelectedTestCaseNodes().isEmpty()) {
            int option = JOptionPane.showConfirmDialog(
                null,
                "Move selected Shared Reusable test case(s) to Test Plan?",
                "Make As TestCase",
                JOptionPane.YES_NO_OPTION
            );
            if (option != JOptionPane.YES_OPTION) {
                return;
            }

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
                Notification.showWarning("No shared reusable test cases were moved to Test Plan.");
            }
        }
    }

    /**
     * Moves a test case from shared reusable to test plan and reloads trees.
     * @param testCase test case to move
     */
    @Override
    void makeAsReusableRTestCase(TestCase testCase) {
        int option = JOptionPane.showConfirmDialog(
            null,
            "Move selected Shared Reusable test case to Test Plan?",
            "Make As TestCase",
            JOptionPane.YES_NO_OPTION
        );
        if (option != JOptionPane.YES_OPTION) {
            return;
        }

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
     * Adds a new shared reusable scenario to the selected group.
     */
    private void addSharedReusableScenario() {
        String scenarioName = fetchNewSharedReusableScenarioName();
        Scenario scenario = getProject().addSharedReusableScenario(scenarioName);
        if (scenario == null) {
            Notification.showWarning(
                "Scenario '" +
                scenarioName +
                "' already exists in another scope (Test Plan, Reusable, or Shared Reusable)."
            );
            return;
        }
        ScenarioNode scNode = getTreeModel().addScenario(getSelectedGroupNode(), scenario);
        if (scNode != null) {
            selectAndScrollTo(new TreePath(scNode.getPath()));
        }
    }

    /**
     * Adds a new shared reusable test case to the selected scenario.
     */
    private void addSharedReusableTestCase() {
        ScenarioNode scenarioNode = getSelectedScenarioNodeSafe();
        if (scenarioNode != null) {
            String testCaseName = fetchNewSharedReusableTestCaseName(scenarioNode.getScenario());
            TestCase testcase = scenarioNode.getScenario().addTestCase(testCaseName);
            if (testcase != null) {
                getTestDesign().loadTableModelForSelection(testcase);
                selectAndScrollTo(
                    new TreePath(getTreeModel().addTestCase(scenarioNode, testcase).getPath())
                );
            } else {
                Notification.show("Shared reusable test case already exists");
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
            String groupNodes_str = groupNodes
                .stream()
                .map(g -> g.toString())
                .reduce((a, b) -> a + "\n" + b)
                .get();
            String question =
                "<html><body><p style='width: 200px;'>" +
                "Are you sure want to delete the following Groups?<br>" +
                groupNodes_str +
                "</p></body></html>";

            JCheckBox confirmBox = new JCheckBox(
                "Move Shared Reusables inside Group to TestPlan instead of deleting"
            );

            int option = JOptionPane.showConfirmDialog(
                null,
                new Object[] { question, confirmBox },
                "Delete TestCase",
                JOptionPane.YES_NO_OPTION
            );
            if (option == JOptionPane.YES_OPTION) {
                LOGGER.log(
                    Level.INFO,
                    "Delete Shared Reusable Groups approved for {0}; {1}",
                    new Object[] { groupNodes.size(), groupNodes_str }
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
     * Generates a unique name for a new shared reusable scenario checking all scopes.
     * @return unique scenario name
     */
    private String fetchNewSharedReusableScenarioName() {
        String newScenarioName = "NewSharedScenario";
        for (int i = 0;; i++) {
            // Check if scenario exists in any scope
            if (
                getProject().getScenarioByName(newScenarioName) == null &&
                getProject().getReusableScenarioByName(newScenarioName) == null &&
                getProject().getSharedReusableScenarioByName(newScenarioName) == null
            ) {
                break;
            }
            newScenarioName = "NewSharedScenario" + i;
        }
        return newScenarioName;
    }

    /**
     * Generates a unique name for a new shared reusable test case.
     * @param scenario scenario to check for existing test case names
     * @return unique test case name
     */
    private String fetchNewSharedReusableTestCaseName(Scenario scenario) {
        String newTestCaseName = "NewSharedTestCase";
        for (int i = 0;; i++) {
            if (scenario.getTestCaseByName(newTestCaseName) == null) {
                break;
            }
            newTestCaseName = "NewSharedTestCase" + i;
        }
        return newTestCaseName;
    }

    private List<TestCase> collectSelectedSharedReusableTestCases() {
        List<TestCase> selected = new ArrayList<>();
        for (TestCaseNode tcNode : getSelectedTestCaseNodes()) {
            selected.add(tcNode.getTestCase());
        }
        for (ScenarioNode scenarioNode : getSelectedScenarioNodes()) {
            selected.addAll(scenarioNode.getScenario().getTestCases());
        }
        return selected;
    }

    private void moveToProjectReusable() {
        List<TestCase> selected = collectSelectedSharedReusableTestCases();
        if (selected.isEmpty()) {
            Notification.showWarning(
                "Select at least one shared reusable test case to make as Project Reusable."
            );
            return;
        }

        int warning = JOptionPane.showConfirmDialog(
            null,
            "Move selected Shared Reusable test case(s) to Project Reusable Components?",
            "Move to Project Reusable",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        if (warning != JOptionPane.YES_OPTION) {
            return;
        }

        int success = 0;
        int impactedUpdates = 0;
        for (TestCase tc : selected) {
            try {
                getProject().moveSharedReusableToReusable(tc);
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
            showImpactedReferenceNotification("Moved to Project Reusable", impactedUpdates);
        } else {
            Notification.showWarning(
                "No shared reusable test cases were moved to Project Reusable."
            );
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
     * Saves the shared reusable tree model state.
     */
    public void save() {
        getTreeModel().save();
    }

    /**
     * Context menu for the shared reusable tree with group-specific actions and [Shared] scope indicator.
     */
    class SharedReusablePopupMenu extends ProjectPopupMenu {

        /**
         * Constructs a new SharedReusablePopupMenu and initializes menu items.
         */
        public SharedReusablePopupMenu() {
            initMenu();
        }

        /**
         * Initializes all menu items specific to shared reusable components.
         */
        private void initMenu() {
            removeAll();
            super.init();
            toggleTestCase.setVisible(false);
            toggleProjectReusable.setVisible(false);
            toggleSharedReusable.setVisible(false);
        }

        /**
         * Configures menu items for test case context in shared reusable tree.
         */
        @Override
        protected void forTestCase() {
            super.forTestCase();
            toggleTestCase.setEnabled(true);
            toggleProjectReusable.setEnabled(true);
            toggleSharedReusable.setEnabled(false);
        }

        /**
         * Configures menu items for scenario context in shared reusable tree.
         */
        @Override
        protected void forScenario() {
            super.forScenario();
            toggleTestCase.setEnabled(false);
            toggleProjectReusable.setEnabled(false);
            toggleSharedReusable.setEnabled(false);
        }

        /**
         * Configures menu items for test plan (group) context in shared reusable tree.
         */
        @Override
        protected void forTestPlan() {
            super.forTestPlan();
            toggleTestCase.setEnabled(false);
            toggleProjectReusable.setEnabled(false);
            toggleSharedReusable.setEnabled(false);
        }

        /**
         * Configures menu items for root context in shared reusable tree.
         */
        protected void forRoot() {
            super.forTestPlan();
            addScenario.setEnabled(false);
            toggleTestCase.setEnabled(false);
            toggleProjectReusable.setEnabled(false);
            toggleSharedReusable.setEnabled(false);
        }
    }
}

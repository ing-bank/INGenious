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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
        LOGGER.log(Level.INFO, "=== onDeleteAction() called ===");
        deleteGroups();
        LOGGER.log(Level.INFO, "After deleteGroups()");

        // Collect test cases and scenarios for deletion with referencing projects check
        List<TestCaseNode> testCaseNodes = getSelectedTestCaseNodes();
        List<ScenarioNode> scenarioNodes = getSelectedScenarioNodes();

        LOGGER.log(Level.INFO, "Selected test case nodes: {0}", testCaseNodes.size());
        LOGGER.log(Level.INFO, "Selected scenario nodes: {0}", scenarioNodes.size());

        if (!testCaseNodes.isEmpty() || !scenarioNodes.isEmpty()) {
            LOGGER.log(Level.INFO, "Nodes found - proceeding with confirmation dialog");
            // collect scenario names referenced by the selection
            List<String> selectedScenarioNames = new ArrayList<>();
            for (ScenarioNode sn : scenarioNodes) {
                selectedScenarioNames.add(sn.getScenario().getName());
            }
            for (TestCaseNode tcn : testCaseNodes) {
                selectedScenarioNames.add(tcn.getTestCase().getScenario().getName());
            }

            LOGGER.log(
                Level.INFO,
                "Delete action: collecting referencing projects for scenarios: {0}",
                selectedScenarioNames
            );

            // find projects that reference any of the selected shared scenarios
            List<String> referencingProjects = new ArrayList<>();
            for (String scnName : selectedScenarioNames) {
                List<String> projs = findProjectsUsingSharedScenario(scnName);
                LOGGER.log(
                    Level.INFO,
                    "Projects referencing scenario ''{0}'': {1}",
                    new Object[] { scnName, projs }
                );
                for (String p : projs) {
                    if (!referencingProjects.contains(p)) referencingProjects.add(p);
                }
            }

            LOGGER.log(
                Level.INFO,
                "Total unique referencing projects found: {0}",
                referencingProjects.size()
            );

            String message;
            if (!referencingProjects.isEmpty()) {
                // Build detailed message showing what's being deleted
                StringBuilder itemsList = new StringBuilder();
                for (ScenarioNode sn : scenarioNodes) {
                    if (itemsList.length() > 0) itemsList.append("\n");
                    itemsList.append("  - Scenario: ").append(sn.getScenario().getName());
                }
                for (TestCaseNode tcn : testCaseNodes) {
                    if (itemsList.length() > 0) itemsList.append("\n");
                    itemsList
                        .append("  - Test Case: ")
                        .append(tcn.getTestCase().getName())
                        .append(" (Scenario: ")
                        .append(tcn.getTestCase().getScenario().getName())
                        .append(")");
                }

                message =
                    "The following shared reusable components will be deleted:\n\n" +
                    itemsList.toString() +
                    "\n\n" +
                    "These components are referenced by the following projects:\n\n" +
                    String.join("\n", referencingProjects) +
                    "\n\nAre you sure you want to delete these shared reusable component(s)?";
                LOGGER.log(Level.INFO, "Showing delete confirmation with detailed project list");
            } else {
                message =
                    "Are you sure you want to delete the selected Shared Reusable component(s)?";
                LOGGER.log(
                    Level.WARNING,
                    "No referencing projects found - showing generic warning"
                );
            }

            int warning = JOptionPane.showConfirmDialog(
                null,
                message,
                "Delete Shared Reusable Components",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );
            if (warning != JOptionPane.YES_OPTION) {
                LOGGER.log(Level.INFO, "Delete cancelled by user");
                return;
            }

            // User confirmed deletion - perform it here instead of calling super
            LOGGER.log(Level.INFO, "Performing deletion of scenarios and test cases");

            // Delete test cases first
            if (!testCaseNodes.isEmpty()) {
                Set<ScenarioNode> affectedScenarios = new HashSet<>();
                for (TestCaseNode tcNode : testCaseNodes) {
                    if (tcNode.getParent() instanceof ScenarioNode) {
                        affectedScenarios.add((ScenarioNode) tcNode.getParent());
                    }
                }
                for (TestCaseNode tcNode : testCaseNodes) {
                    tcNode.getTestCase().delete();
                    getTreeModel().removeNodeFromParent(tcNode);
                }
                for (ScenarioNode s : affectedScenarios) {
                    persistSortOrder(s);
                }
            }

            // Delete scenarios
            if (!scenarioNodes.isEmpty()) {
                Set<GroupNode> affectedGroups = new HashSet<>();
                for (ScenarioNode scenarioNode : scenarioNodes) {
                    if (scenarioNode.getParent() instanceof GroupNode) {
                        affectedGroups.add((GroupNode) scenarioNode.getParent());
                    }
                }
                for (ScenarioNode scenarioNode : scenarioNodes) {
                    // Delete all test cases in the scenario first
                    List<TestCaseNode> testCasesInScenario = TestCaseNode.toList(
                        scenarioNode.children()
                    );
                    for (TestCaseNode tcNode : testCasesInScenario) {
                        tcNode.getTestCase().delete();
                        getTreeModel().removeNodeFromParent(tcNode);
                    }
                    // Then delete the scenario itself
                    scenarioNode.getScenario().delete();
                    getTreeModel().removeNodeFromParent(scenarioNode);
                }
                for (GroupNode g : affectedGroups) {
                    persistSortOrder(g);
                }
            }

            LOGGER.log(Level.INFO, "Deletion completed successfully");
        } else {
            LOGGER.log(
                Level.INFO,
                "No test cases or scenarios selected - skipping delete confirmation"
            );
        }
        LOGGER.log(Level.INFO, "=== onDeleteAction() completed ===");
    }

    /**
     * Handles action events specific to shared reusable components.
     * Intercepts delete operations to show referencing projects.
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
            case "Delete Scenario":
                handleDeleteScenarioWithReferences();
                break;
            case "Delete TestCase":
                handleDeleteTestCaseWithReferences();
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
     * Handles delete scenario action with referencing projects confirmation.
     */
    private void handleDeleteScenarioWithReferences() {
        List<ScenarioNode> scenarioNodes = getSelectedScenarioNodes();
        LOGGER.log(
            Level.INFO,
            "handleDeleteScenarioWithReferences: {0} scenarios selected",
            scenarioNodes.size()
        );

        if (scenarioNodes.isEmpty()) {
            return;
        }

        // collect scenario names
        List<String> selectedScenarioNames = new ArrayList<>();
        for (ScenarioNode sn : scenarioNodes) {
            selectedScenarioNames.add(sn.getScenario().getName());
        }

        // find projects that reference these scenarios
        List<String> referencingProjects = new ArrayList<>();
        for (String scnName : selectedScenarioNames) {
            List<String> projs = findProjectsUsingSharedScenario(scnName);
            LOGGER.log(
                Level.INFO,
                "Projects referencing scenario ''{0}'': {1}",
                new Object[] { scnName, projs }
            );
            for (String p : projs) {
                if (!referencingProjects.contains(p)) {
                    referencingProjects.add(p);
                }
            }
        }

        String message;
        if (!referencingProjects.isEmpty()) {
            // Build detailed message with scenarios and referencing projects
            StringBuilder itemsList = new StringBuilder();
            for (ScenarioNode sn : scenarioNodes) {
                if (itemsList.length() > 0) {
                    itemsList.append("\n");
                }
                itemsList.append("  - Scenario: ").append(sn.getScenario().getName());
            }

            message =
                "The following shared reusable components will be deleted:\n\n" +
                itemsList.toString() +
                "\n\nThese components are referenced by the following projects:\n\n" +
                String.join("\n", referencingProjects) +
                "\n\nAre you sure you want to delete these shared reusable scenario(s)?";
            LOGGER.log(Level.INFO, "Showing delete scenario confirmation with projects list");
        } else {
            message = "Are you sure you want to delete the selected shared reusable scenario(s)?";
            LOGGER.log(Level.WARNING, "No referencing projects found for scenarios");
        }

        int option = JOptionPane.showConfirmDialog(
            null,
            message,
            "Delete Shared Reusable Scenarios",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (option == JOptionPane.YES_OPTION) {
            LOGGER.log(Level.INFO, "User confirmed delete - performing deletion");
            // Delete scenarios directly
            Set<GroupNode> affectedGroups = new HashSet<>();
            for (ScenarioNode scenarioNode : scenarioNodes) {
                if (scenarioNode.getParent() instanceof GroupNode) {
                    affectedGroups.add((GroupNode) scenarioNode.getParent());
                }
            }
            for (ScenarioNode scenarioNode : scenarioNodes) {
                // Delete all test cases in the scenario first
                List<TestCaseNode> testCasesInScenario = TestCaseNode.toList(
                    scenarioNode.children()
                );
                for (TestCaseNode tcNode : testCasesInScenario) {
                    tcNode.getTestCase().delete();
                    getTreeModel().removeNodeFromParent(tcNode);
                }
                // Then delete the scenario itself
                scenarioNode.getScenario().delete();
                getTreeModel().removeNodeFromParent(scenarioNode);
            }
            for (GroupNode g : affectedGroups) {
                persistSortOrder(g);
            }
            LOGGER.log(Level.INFO, "Scenario deletion completed");
        } else {
            LOGGER.log(Level.INFO, "User cancelled delete");
        }
    }

    /**
     * Handles delete test case action with referencing projects confirmation.
     */
    private void handleDeleteTestCaseWithReferences() {
        List<TestCaseNode> testCaseNodes = getSelectedTestCaseNodes();
        LOGGER.log(
            Level.INFO,
            "handleDeleteTestCaseWithReferences: {0} test cases selected",
            testCaseNodes.size()
        );

        if (testCaseNodes.isEmpty()) {
            return;
        }

        // collect scenario names referenced by test cases
        List<String> selectedScenarioNames = new ArrayList<>();
        for (TestCaseNode tcn : testCaseNodes) {
            selectedScenarioNames.add(tcn.getTestCase().getScenario().getName());
        }

        // find projects that reference these scenarios
        List<String> referencingProjects = new ArrayList<>();
        for (String scnName : selectedScenarioNames) {
            List<String> projs = findProjectsUsingSharedScenario(scnName);
            LOGGER.log(
                Level.INFO,
                "Projects referencing test case scenario ''{0}'': {1}",
                new Object[] { scnName, projs }
            );
            for (String p : projs) {
                if (!referencingProjects.contains(p)) {
                    referencingProjects.add(p);
                }
            }
        }

        String message;
        if (!referencingProjects.isEmpty()) {
            // Build detailed message with test cases and referencing projects
            StringBuilder itemsList = new StringBuilder();
            for (TestCaseNode tcn : testCaseNodes) {
                if (itemsList.length() > 0) {
                    itemsList.append("\n");
                }
                itemsList
                    .append("  - Test Case: ")
                    .append(tcn.getTestCase().getName())
                    .append(" (Scenario: ")
                    .append(tcn.getTestCase().getScenario().getName())
                    .append(")");
            }

            message =
                "The following shared reusable components will be deleted:\n\n" +
                itemsList.toString() +
                "\n\nThese components are referenced by the following projects:\n\n" +
                String.join("\n", referencingProjects) +
                "\n\nAre you sure you want to delete these shared reusable test case(s)?";
            LOGGER.log(Level.INFO, "Showing delete test case confirmation with projects list");
        } else {
            message = "Are you sure you want to delete the selected shared reusable test case(s)?";
            LOGGER.log(Level.WARNING, "No referencing projects found for test cases");
        }

        int option = JOptionPane.showConfirmDialog(
            null,
            message,
            "Delete Shared Reusable Test Cases",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (option == JOptionPane.YES_OPTION) {
            LOGGER.log(Level.INFO, "User confirmed delete - performing deletion");
            // Delete test cases directly
            Set<ScenarioNode> affectedScenarios = new HashSet<>();
            for (TestCaseNode tcNode : testCaseNodes) {
                if (tcNode.getParent() instanceof ScenarioNode) {
                    affectedScenarios.add((ScenarioNode) tcNode.getParent());
                }
            }
            for (TestCaseNode tcNode : testCaseNodes) {
                tcNode.getTestCase().delete();
                getTreeModel().removeNodeFromParent(tcNode);
            }
            for (ScenarioNode s : affectedScenarios) {
                persistSortOrder(s);
            }
            LOGGER.log(Level.INFO, "Test case deletion completed");
        } else {
            LOGGER.log(Level.INFO, "User cancelled delete");
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
                String oldScenarioName = scenarioNode.getScenario().getName();
                LOGGER.log(
                    Level.INFO,
                    "Rename scenario ''{0}'' to ''{1}'' - checking referencing projects",
                    new Object[] { oldScenarioName, name }
                );

                var projects = findProjectsUsingSharedScenario(oldScenarioName);

                LOGGER.log(
                    Level.INFO,
                    "Projects referencing scenario ''{0}'': {1}",
                    new Object[] { oldScenarioName, projects }
                );

                if (!projects.isEmpty()) {
                    String msg =
                        "The shared reusable scenario '" +
                        oldScenarioName +
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
                        LOGGER.log(
                            Level.INFO,
                            "Rename cancelled by user for scenario ''{0}''",
                            oldScenarioName
                        );
                        return false;
                    }
                } else {
                    LOGGER.log(
                        Level.WARNING,
                        "No referencing projects found for scenario ''{0}'' - continuing with rename",
                        oldScenarioName
                    );
                }
                if (scenarioNode.getScenario().renameSharedReusable(name)) {
                    getTreeModel().reload(scenarioNode);
                    renameScenario(scenarioNode.getScenario());
                    super.getTestDesign().getScenarioComp().refreshTitle();
                    LOGGER.log(
                        Level.INFO,
                        "Scenario successfully renamed from ''{0}'' to ''{1}''",
                        new Object[] { oldScenarioName, name }
                    );
                    return true;
                } else {
                    Notification.show("Scenario " + name + " Already present");
                    return false;
                }
            }
            TestCaseNode testCaseNode = super.getSelectedTestCaseNode();
            if (testCaseNode != null && !testCaseNode.toString().equals(name)) {
                // Warn about projects that reference this shared reusable test case's scenario
                String scenarioName = testCaseNode.getTestCase().getScenario().getName();
                String oldTestCaseName = testCaseNode.getTestCase().getName();
                LOGGER.log(
                    Level.INFO,
                    "Rename test case ''{0}'' to ''{1}'' in scenario ''{2}'' - checking referencing projects",
                    new Object[] { oldTestCaseName, name, scenarioName }
                );

                var projects = findProjectsUsingSharedScenario(scenarioName);

                LOGGER.log(
                    Level.INFO,
                    "Projects referencing test case scenario ''{0}'': {1}",
                    new Object[] { scenarioName, projects }
                );

                if (!projects.isEmpty()) {
                    String msg =
                        "The shared reusable test case '" +
                        oldTestCaseName +
                        "'\n" +
                        "(scenario: '" +
                        scenarioName +
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
                        LOGGER.log(
                            Level.INFO,
                            "Rename cancelled by user for test case ''{0}''",
                            oldTestCaseName
                        );
                        return false;
                    }
                } else {
                    LOGGER.log(
                        Level.WARNING,
                        "No referencing projects found for test case scenario ''{0}'' - continuing with rename",
                        scenarioName
                    );
                }
                if (testCaseNode.getTestCase().renameSharedReusable(name)) {
                    getTreeModel().reload(testCaseNode);
                    super.getTestDesign().getTestCaseComp().refreshTitle();
                    LOGGER.log(
                        Level.INFO,
                        "Test case successfully renamed from ''{0}'' to ''{1}''",
                        new Object[] { oldTestCaseName, name }
                    );
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
    @SuppressWarnings("unused")
    private List<String> findProjectsUsingSharedScenario(String scenarioName) {
        List<String> result = new ArrayList<>();
        try {
            // Get current loaded project to exclude it from display
            Project currentProject = getTestDesign().getProject();
            String currentProjectName = currentProject != null ? currentProject.getName() : null;
            String currentProjectPath = currentProject != null
                ? currentProject.getLocation()
                : null;

            String sharedReusablePath = Project.getSharedReusableComponentsPath();
            Path projectsFile = Paths.get(sharedReusablePath, "projects.items");

            LOGGER.log(Level.INFO, "=== findProjectsUsingSharedScenario DEBUG START ===");
            LOGGER.log(Level.INFO, "Scenario name: {0}", scenarioName);
            LOGGER.log(Level.INFO, "System user.dir: {0}", System.getProperty("user.dir"));
            LOGGER.log(Level.INFO, "Shared reusable path resolved to: {0}", sharedReusablePath);
            LOGGER.log(
                Level.INFO,
                "Looking for projects.items at: {0}",
                projectsFile.toAbsolutePath().toString()
            );
            LOGGER.log(Level.INFO, "File exists: {0}", Files.exists(projectsFile));

            if (!Files.exists(projectsFile)) {
                LOGGER.log(
                    Level.WARNING,
                    "projects.items file NOT FOUND at: {0}",
                    projectsFile.toAbsolutePath()
                );
                LOGGER.log(
                    Level.INFO,
                    "=== findProjectsUsingSharedScenario DEBUG END (file not found) ==="
                );
                return result;
            }

            String content = new String(Files.readAllBytes(projectsFile), StandardCharsets.UTF_8);
            LOGGER.log(Level.INFO, "File size: {0} bytes", content.length());

            if (content == null || content.trim().isEmpty()) {
                LOGGER.log(Level.WARNING, "projects.items file is EMPTY at: {0}", projectsFile);
                LOGGER.log(
                    Level.INFO,
                    "=== findProjectsUsingSharedScenario DEBUG END (empty file) ==="
                );
                return result;
            }

            LOGGER.log(
                Level.INFO,
                "projects.items content (first 300 chars): {0}",
                content.length() > 300 ? content.substring(0, 300) + "..." : content
            );

            // Parse JSON array
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.List<java.util.Map<String, String>> projects = mapper.readValue(
                content,
                mapper
                    .getTypeFactory()
                    .constructCollectionType(java.util.List.class, java.util.Map.class)
            );

            LOGGER.log(
                Level.INFO,
                "Successfully parsed {0} projects from projects.items",
                projects.size()
            );
            LOGGER.log(
                Level.INFO,
                "Current project to exclude: name={0}, path={1}",
                new Object[] { currentProjectName, currentProjectPath }
            );

            // Format and display all projects EXCEPT the currently loaded one
            for (java.util.Map<String, String> proj : projects) {
                String projName = proj.get("name");
                String projPath = proj.get("path");
                LOGGER.log(
                    Level.INFO,
                    "Processing project from file: name={0}, path={1}",
                    new Object[] { projName, projPath }
                );

                if (projName == null || projName.isEmpty()) {
                    LOGGER.log(Level.INFO, "Skipping project with null/empty name");
                    continue;
                }

                // Skip if this is the currently loaded project
                boolean isCurrentProject =
                    projName.equals(currentProjectName) &&
                    projPath != null &&
                    projPath.equals(currentProjectPath);

                if (isCurrentProject) {
                    LOGGER.log(Level.INFO, "Skipping current project: {0}", projName);
                    continue;
                }

                // Add to results
                if (projPath != null && !projPath.isEmpty()) {
                    String displayString = projName + " | " + projPath;
                    result.add(displayString);
                    LOGGER.log(Level.INFO, "Added to results: {0}", displayString);
                } else {
                    result.add(projName);
                    LOGGER.log(Level.INFO, "Added to results (no path): {0}", projName);
                }
            }

            LOGGER.log(
                Level.INFO,
                "Total referencing projects after filtering: {0}",
                result.size()
            );
            for (String p : result) {
                LOGGER.log(Level.INFO, "  Result item: {0}", p);
            }
            LOGGER.log(Level.INFO, "=== findProjectsUsingSharedScenario DEBUG END (success) ===");
        } catch (Exception e) {
            // Log with WARNING level so it's visible in normal operations
            LOGGER.log(Level.WARNING, "Failed to read Shared projects.items JSON", e);
            LOGGER.log(Level.SEVERE, "Exception details: {0}", e.toString());
            LOGGER.log(Level.INFO, "=== findProjectsUsingSharedScenario DEBUG END (exception) ===");
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
                "' already exists in Shared Reusables. Please choose a different Shared Reusable scenario name."
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
            String groupNodesStr = content.toString();
            groupsArea.setText(groupNodesStr);
            groupsArea.setCaretPosition(0);

            JScrollPane scrollPane = new JScrollPane(groupsArea);
            scrollPane.setPreferredSize(new Dimension(360, 180));
            messagePanel.add(scrollPane, java.awt.BorderLayout.CENTER);

            JCheckBox confirmBox = new JCheckBox(
                "Move Shared Reusables inside Group to TestPlan instead of deleting"
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
                    "Delete Shared Reusable Groups approved for {0}; {1}",
                    new Object[] { groupNodes.size(), groupNodesStr }
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
        String base = "NewSharedScenario";
        // prefer plain base name if available (and not present in the tree)
        if (
            getProject().getSharedReusableScenarioByName(base) == null && !treeHasScenarioName(base)
        ) {
            return base;
        }
        int i = 0;
        String newScenarioName;
        for (;;) {
            newScenarioName = base + i;
            if (
                getProject().getSharedReusableScenarioByName(newScenarioName) == null &&
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
            // Hide group items - grouping is not supported in Shared Reusable Components
            setGroupItemsVisible(false, false, false, false);
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
            // Hide group items - grouping is not supported in Shared Reusable Components
            setGroupItemsVisible(false, false, false, false);
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
            // Hide group items - grouping is not supported in Shared Reusable Components
            setGroupItemsVisible(false, false, false, false);
        }
    }
}

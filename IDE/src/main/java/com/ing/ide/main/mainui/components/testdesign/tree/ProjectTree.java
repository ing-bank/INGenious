package com.ing.ide.main.mainui.components.testdesign.tree;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.component.TestStep;
import com.ing.datalib.component.utils.SortOrderStore;
import com.ing.datalib.exception.TestCaseConversionException;
import com.ing.datalib.model.DataItem;
import com.ing.datalib.model.Meta;
import com.ing.datalib.model.Tag;
import com.ing.datalib.or.mobile.ResolvedMobileObject;
import com.ing.datalib.or.sap.ResolvedSapObject;
import com.ing.datalib.or.structureddata.ResolvedStructuredDataObject;
import com.ing.datalib.or.web.ResolvedWebObject;
import com.ing.ide.main.mainui.components.testdesign.TestDesign;
import com.ing.ide.main.mainui.components.testdesign.testcase.validation.TestCaseValidation;
import com.ing.ide.main.mainui.components.testdesign.tree.model.GroupNode;
import com.ing.ide.main.mainui.components.testdesign.tree.model.ProjectTreeModel;
import com.ing.ide.main.mainui.components.testdesign.tree.model.ScenarioNode;
import com.ing.ide.main.mainui.components.testdesign.tree.model.TestCaseNode;
import com.ing.ide.main.mainui.components.testdesign.tree.model.TestPlanGroupNode;
import com.ing.ide.main.mainui.components.testdesign.tree.model.TestPlanNode;
import com.ing.ide.main.mainui.components.testdesign.tree.model.TestPlanTreeModel;
import com.ing.ide.main.ui.ProjectProperties;
import com.ing.ide.main.utils.Utils;
import com.ing.ide.main.utils.dnd.TransferActionListener;
import com.ing.ide.main.utils.keys.Keystroke;
import com.ing.ide.main.utils.tree.TreeSelectionRenderer;
import com.ing.ide.settings.IconSettings;
import com.ing.ide.util.Canvas;
import com.ing.ide.util.Notification;
import com.ing.ide.util.Validator;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.tree.TreePath;

/**
 * UI tree component for displaying and managing Test Plan scenarios and test cases.
 * Provides context menus, drag-and-drop support, and editing capabilities.
 */
public class ProjectTree implements ActionListener {
    private static final Logger LOGGER = Logger.getLogger(ProjectTree.class.getName());

    /** Colour used to mark scenario/test-case nodes that have validation errors. */
    private static final Color VALIDATION_ERROR_COLOR = new Color(0xCE2323);

    ProjectPopupMenu popupMenu;

    private final ProjectProperties projectProperties;

    private final JTree tree;

    private final TestDesign testDesign;

    ProjectTreeModel treeModel = new TestPlanTreeModel();

    /**
     * Constructs a new ProjectTree for managing Test Plan scenarios and test cases.
     * @param testDesign parent TestDesign component
     */
    public ProjectTree(TestDesign testDesign) {
        this.testDesign = testDesign;
        tree = new JTree();
        projectProperties = new ProjectProperties(testDesign.getsMainFrame());
        init();
    }

    /**
     * Creates a new tree model for Test Plan.
     * @return new ProjectTreeModel instance
     */
    ProjectTreeModel getNewTreeModel() {
        return new TestPlanTreeModel();
    }

    /**
     * Creates a new popup menu for the tree.
     * @return new ProjectPopupMenu instance
     */
    /**
     * Creates a new popup menu for the tree.
     * @return new ProjectPopupMenu instance
     */
    ProjectPopupMenu getNewPopupMenu() {
        return new ProjectPopupMenu();
    }

    /**
     * Initializes the tree component with event handlers, keybindings, and UI settings.
     */
    private void init() {
        try {
            //create the font to use. Specify the size!
            Font customFont = Font.createFont(
                Font.TRUETYPE_FONT,
                new File("resources/ui/resources/fonts/ingme_regular.ttf")
            ); //.deriveFont(12f);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            //register the font
            ge.registerFont(customFont);
        } catch (IOException | FontFormatException e) {
            //   e.printStackTrace();
        }

        popupMenu = getNewPopupMenu();
        treeModel = getNewTreeModel();
        tree.setModel(treeModel);

        alterTreeDefaultKeyBindings();

        tree.setToggleClickCount(0);
        tree.setEditable(true);
        tree.setInvokesStopCellEditing(true);
        tree.setComponentPopupMenu(popupMenu);
        tree.setDragEnabled(true);
        tree.setTransferHandler(new ProjectDnD(this));
        tree.setFont(new Font("ING Me", Font.PLAIN, 11));
        tree.getInputMap(JComponent.WHEN_FOCUSED).put(Keystroke.NEW, "New");
        tree.getInputMap(JComponent.WHEN_FOCUSED).put(Keystroke.DELETE, "Delete");
        tree.getInputMap(JComponent.WHEN_FOCUSED).put(Keystroke.RENAME, "Rename");
        tree.getInputMap(JComponent.WHEN_FOCUSED).put(Keystroke.ALTENTER, "AltEnter");
        tree.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ESCAPE"), "Escape");

        tree
            .getActionMap()
            .put(
                "AltEnter",
                new AbstractAction() {

                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        showDetails();
                    }
                }
            );

        tree
            .getActionMap()
            .put(
                "New",
                new AbstractAction() {

                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        onNewAction();
                    }
                }
            );

        tree
            .getActionMap()
            .put(
                "Delete",
                new AbstractAction() {

                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        onDeleteAction();
                    }
                }
            );

        tree
            .getActionMap()
            .put(
                "Rename",
                new AbstractAction() {

                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        ScenarioNode scenarioNode = getSelectedScenarioNode();
                        if (scenarioNode != null) {
                            tree.startEditingAtPath(new TreePath(scenarioNode.getPath()));
                            return;
                        }
                        TestCaseNode testCaseNode = getSelectedTestCaseNode();
                        if (testCaseNode != null) {
                            tree.startEditingAtPath(new TreePath(testCaseNode.getPath()));
                        }
                    }
                }
            );

        tree
            .getActionMap()
            .put(
                "Escape",
                new AbstractAction() {

                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        if (tree.isEditing()) {
                            tree.cancelEditing();
                        }
                    }
                }
            );

        tree.addMouseListener(
            new MouseAdapter() {

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                        loadTableModelForSelection();
                    }
                }
            }
        );
        popupMenu.addPopupMenuListener(
            new PopupMenuListener() {

                @Override
                public void popupMenuWillBecomeVisible(PopupMenuEvent pme) {
                    onRightClick();
                }

                @Override
                public void popupMenuWillBecomeInvisible(PopupMenuEvent pme) {
                    // Not Needed
                }

                @Override
                public void popupMenuCanceled(PopupMenuEvent pme) {
                    // Not Needed
                }
            }
        );
        setTreeIcon();
        tree
            .getCellEditor()
            .addCellEditorListener(
                new CellEditorListener() {

                    @Override
                    public void editingStopped(ChangeEvent ce) {
                        if (!checkAndRename()) {
                            tree.getCellEditor().cancelCellEditing();
                        }
                    }

                    @Override
                    public void editingCanceled(ChangeEvent ce) {
                        //   Not Needed
                    }
                }
            );
    }

    /**
     * Sets the custom icons for tree nodes based on node type.
     */
    private void setTreeIcon() {
        try {
            //create the font to use. Specify the size!
            Font customFont = Font.createFont(
                Font.TRUETYPE_FONT,
                new File("resources/ui/resources/fonts/ingme_regular.ttf")
            ); //.deriveFont(12f);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            //register the font
            ge.registerFont(customFont);
        } catch (IOException | FontFormatException e) {
            //  e.printStackTrace();
        }
        tree.setFont(new Font("ING Me", Font.PLAIN, 11));
        new TreeSelectionRenderer(tree) {

            @Override
            public Component getTreeCellRendererComponent(
                JTree tree,
                Object value,
                boolean selected,
                boolean expanded,
                boolean isLeaf,
                int row,
                boolean focused
            ) {
                Component c = super.getTreeCellRendererComponent(
                    tree,
                    value,
                    selected,
                    expanded,
                    isLeaf,
                    row,
                    focused
                );
                if (value instanceof TestPlanGroupNode) {
                    setIcons(IconSettings.getIconSettings().getTestPlanGroup());
                } else if (value instanceof GroupNode) {
                    setIcons(IconSettings.getIconSettings().getReusableFolder());
                } else if (value instanceof ScenarioNode) {
                    setIcons(IconSettings.getIconSettings().getTestPlanScenario());
                    // setText(withScopeBadge(value));
                } else if (value instanceof TestCaseNode) {
                    if (ProjectTree.this instanceof ReusableTree) {
                        setIcons(IconSettings.getIconSettings().getReusableTestCase());
                    } else {
                        setIcons(IconSettings.getIconSettings().getTestPlanTestCase());
                    }
                    // setText(withScopeBadge(value));
                } else {
                    setIcons(IconSettings.getIconSettings().getTestPlanRoot());
                }
                markValidationError(c, value);
                return c;
            }

            /**
             * Marks a scenario or test case node in red when it (or any
             * reusable it references) contains an IDE-level validation error.
             *
             * @param comp  the rendered tree cell component
             * @param value the tree node being rendered
             */
            void markValidationError(Component comp, Object value) {
                boolean error = false;
                if (value instanceof ScenarioNode) {
                    error = TestCaseValidation.hasError(((ScenarioNode) value).getScenario());
                } else if (value instanceof TestCaseNode) {
                    error = TestCaseValidation.hasError(((TestCaseNode) value).getTestCase());
                }
                if (error) {
                    comp.setForeground(VALIDATION_ERROR_COLOR);
                }
            }

            void setIcons(Icon icon) {
                setLeafIcon(icon);
                setClosedIcon(icon);
                setOpenIcon(icon);
                setIcon(icon);
            }
        };
    }

    /**
     * Loads the table model for the selected tree node (scenario or test case).
     */
    public void loadTableModelForSelection() {
        Object selected = getSelectedTestCase();
        if (selected == null) {
            selected = getSelectedScenario();
        }
        testDesign.loadTableModelForSelection(selected);
    }

    /**
     * Handles right-click events on tree nodes to show context menu.
     */
    private void onRightClick() {
        TreePath path = tree.getSelectionPath();
        if (path != null) {
            togglePopupMenu(tree.getSelectionPath().getLastPathComponent());
        } else {
            popupMenu.setVisible(false);
        }
    }

    /**
     * Toggles the popup menu based on the selected node type.
     * @param selected selected tree node
     */
    protected void togglePopupMenu(Object selected) {
        if (selected instanceof ScenarioNode) {
            popupMenu.forScenario();
        } else if (selected instanceof TestCaseNode) {
            popupMenu.forTestCase();
        } else if (selected instanceof TestPlanGroupNode) {
            popupMenu.forGroup();
        } else if (selected instanceof GroupNode) {
            popupMenu.forTestPlan();
        }
    }

    /**
     * Handles the "New" action based on current selection.
     */
    protected void onNewAction() {
        if (getSelectedScenarioNode() != null || getSelectedTestCaseNode() != null) {
            addTestCase();
        } else if (getSelectedGroupNode() != null) {
            addScenario();
        }
    }

    /**
     * Handles the "Delete" action for selected test cases and scenarios.
     */
    protected void onDeleteAction() {
        deleteTestCases();
        deleteScenarios();
    }

    /**
     * Handles action events from menu items and context menus.
     * @param ae action event
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        switch (ae.getActionCommand()) {
            case "Add Scenario":
                addScenario();
                break;
            case "Rename Scenario":
                tree.startEditingAtPath(new TreePath(getSelectedScenarioNode().getPath()));
                break;
            case "Delete Scenario":
                deleteScenarios();
                break;
            case "Add TestCase":
                addTestCase();
                break;
            case "Rename TestCase":
                tree.startEditingAtPath(new TreePath(getSelectedTestCaseNode().getPath()));
                break;
            case "Delete TestCase":
                deleteTestCases();
                break;
            case "New Group":
                addGroup();
                break;
            case "Rename Group":
                renameGroup();
                break;
            case "Delete Group":
                deleteGroup();
                break;
            case "Sort":
                sort();
                break;
            case "Edit Tag":
                editTag();
                break;
            case "Make As TestCase":
                makeAsReusableRTestCase();
                break;
            case "Make As Project Reusable":
                makeAsReusableRTestCase();
                break;
            case "Make As Shared Reusable":
                moveTestCaseToSharedReusable();
                break;
            case "Details":
                showDetails();
                break;
            case "Manual Testcase":
                {
                    try {
                        convertToManual();
                    } catch (IOException ex) {
                        Logger.getLogger(ProjectTree.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                break;
            case "Get Impacted TestCases":
                getImpactedTestCases();
                break;
            case "Get CmdLine Syntax":
                getCmdLineSyntax();
                break;
            default:
                throw new UnsupportedOperationException();
        }
    }

    /**
     * Returns the tree model.
     * @return tree model
     */
    public ProjectTreeModel getTreeModel() {
        return treeModel;
    }

    /**
     * Adds a new scenario to the project.
     */
    private void addScenario() {
        String scenarioName = fetchNewScenarioName();
        Scenario scenario = testDesign.getProject().addScenario(scenarioName);
        if (scenario == null) {
            Notification.showWarning(
                "Scenario '" +
                scenarioName +
                "' already exists in the Test Plan. Please choose a different Test Plan scenario name."
            );
            return;
        }
        ScenarioNode scNode;
        TestPlanGroupNode selectedGroup = getSelectedTestPlanGroupNode();
        if (selectedGroup != null && treeModel instanceof TestPlanTreeModel) {
            scNode = ((TestPlanTreeModel) treeModel).addScenarioToGroup(selectedGroup, scenario);
        } else {
            scNode = treeModel.addScenario(getSelectedGroupNode(), scenario);
        }
        selectAndScrollTo(new TreePath(scNode.getPath()));
        persistSortOrder(scNode.getParent());
    }

    /**
     * Generates a unique name for a new scenario checking all scopes.
     * @return unique scenario name
     */
    private String fetchNewScenarioName() {
        String base = "NewScenario";
        // prefer plain base name if available
        if (
            testDesign.getProject().getTestPlanScenarioByName(base) == null &&
            !treeHasScenarioName(base)
        ) {
            return base;
        }
        int i = 0;
        String newScenarioName;
        for (;;) {
            newScenarioName = base + i;
            if (
                testDesign.getProject().getTestPlanScenarioByName(newScenarioName) == null &&
                !treeHasScenarioName(newScenarioName)
            ) {
                break;
            }
            i++;
        }
        return newScenarioName;
    }

    private boolean treeHasScenarioName(String name) {
        if (treeModel == null || treeModel.getRoot() == null) return false;
        javax.swing.tree.TreeNode rootNode = (javax.swing.tree.TreeNode) treeModel.getRoot();
        for (javax.swing.tree.TreeNode child : Collections.list(rootNode.children())) {
            if (child instanceof GroupNode) {
                for (ScenarioNode sc : ScenarioNode.toList(((GroupNode) child).children())) {
                    if (sc.getScenario().getName().equalsIgnoreCase(name)) {
                        return true;
                    }
                }
            } else if (child instanceof ScenarioNode) {
                if (((ScenarioNode) child).getScenario().getName().equalsIgnoreCase(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Adds a new test case to the selected scenario.
     */
    private void addTestCase() {
        ScenarioNode scenarioNode = getSelectedScenarioNode();
        if (scenarioNode == null) {
            TestCaseNode tcNode = getSelectedTestCaseNode();
            if (tcNode != null && tcNode.getParent() instanceof ScenarioNode) {
                scenarioNode = (ScenarioNode) tcNode.getParent();
            }
        }
        if (scenarioNode != null) {
            String testCaseName = fetchNewTestCaseName(scenarioNode.getScenario());
            TestCase testcase = scenarioNode.getScenario().addTestCase(testCaseName);
            if (testcase == null) {
                Notification.showWarning(
                    "Failed to add test case: a test case with this name already exists."
                );
                return;
            }
            testDesign.loadTableModelForSelection(testcase);
            TestCaseNode tcNode = treeModel.addTestCase(scenarioNode, testcase);
            if (tcNode != null && tcNode.getTestCase() != null) {
                selectAndScrollTo(new TreePath(tcNode.getPath()));
                persistSortOrder(scenarioNode);
            }
        }
    }

    /**
     * Generates a unique name for a new test case within a scenario.
     * @param scenario scenario to check for existing test case names
     * @return unique test case name
     */
    private String fetchNewTestCaseName(Scenario scenario) {
        String newTestCaseName = "NewTestCase";
        for (int i = 0;; i++) {
            if (scenario.getTestCaseByName(newTestCaseName) == null) {
                break;
            }
            newTestCaseName = "NewTestCase" + i;
        }
        return newTestCaseName;
    }

    /**
     * Validates and performs rename operation on selected scenario or test case.
     * @return true if rename was successful, false otherwise
     */
    protected Boolean checkAndRename() {
        String name = tree.getCellEditor().getCellEditorValue().toString().trim();
        if (Validator.isValidName(name)) {
            ScenarioNode scenarioNode = getSelectedScenarioNode();
            if (scenarioNode != null && !scenarioNode.toString().equals(name)) {
                if (scenarioNode.getScenario().rename(name)) {
                    getTreeModel().reload(scenarioNode);
                    renameScenario(scenarioNode.getScenario());
                    testDesign.getScenarioComp().refreshTitle();
                    persistSortOrder(scenarioNode.getParent());
                    return true;
                } else {
                    Notification.show("Scenario " + name + " Already present");
                    return false;
                }
            }
            TestPlanGroupNode groupNode = getSelectedTestPlanGroupNode();
            if (
                groupNode != null &&
                !groupNode.toString().equals(name) &&
                treeModel instanceof TestPlanTreeModel
            ) {
                if (((TestPlanTreeModel) treeModel).renameGroup(groupNode, name)) {
                    return true;
                } else {
                    Notification.show("Group " + name + " Already present");
                    return false;
                }
            }
            TestCaseNode testCaseNode = getSelectedTestCaseNode();
            if (testCaseNode != null && !testCaseNode.toString().equals(name)) {
                if (testCaseNode.getTestCase().rename(name)) {
                    getTreeModel().reload(testCaseNode);
                    testDesign.getTestCaseComp().refreshTitle();
                    persistSortOrder(testCaseNode.getParent());
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
     * Notifies the reusable tree that a scenario has been renamed.
     * @param scenario renamed scenario
     */
    void renameScenario(Scenario scenario) {
        getTestDesign().getReusableTree().getTreeModel().onScenarioRename(scenario);
    }

    /**
     * Deletes selected scenarios after user confirmation.
     */
    private void deleteScenarios() {
        List<ScenarioNode> scenarioNodes = getSelectedScenarioNodes();
        if (!scenarioNodes.isEmpty()) {
            int option = showScrollableDeleteConfirmation(
                "Delete Scenario",
                "Scenarios",
                scenarioNodes
            );
            if (option == JOptionPane.YES_OPTION) {
                LOGGER.log(
                    Level.INFO,
                    "Delete Scenarios approved for {0}; {1}",
                    new Object[] { scenarioNodes.size(), scenarioNodes }
                );
                Set<GroupNode> affectedGroups = new HashSet<>();
                for (ScenarioNode scenarioNode : scenarioNodes) {
                    if (scenarioNode.getParent() instanceof GroupNode) {
                        affectedGroups.add((GroupNode) scenarioNode.getParent());
                    }
                }
                for (ScenarioNode scenarioNode : scenarioNodes) {
                    deleteTestCases(TestCaseNode.toList(scenarioNode.children()));
                    scenarioNode.getScenario().delete();
                    getTreeModel().removeNodeFromParent(scenarioNode);
                }
                for (GroupNode g : affectedGroups) {
                    persistSortOrder(g);
                }
            }
        }
    }

    /**
     * Deletes selected test cases after user confirmation.
     */
    private void deleteTestCases() {
        List<TestCaseNode> testcaseNodes = getSelectedTestCaseNodes();
        if (!testcaseNodes.isEmpty()) {
            int option = showScrollableDeleteConfirmation(
                "Delete TestCase",
                "TestCases",
                testcaseNodes
            );
            if (option == JOptionPane.YES_OPTION) {
                LOGGER.log(
                    Level.INFO,
                    "Delete TestCases approved for {0}; {1}",
                    new Object[] { testcaseNodes.size(), testcaseNodes }
                );
                deleteTestCases(testcaseNodes);
            }
        }
    }

    /**
     * Shows a delete confirmation dialog with a scrollable list so action buttons stay visible.
     * @param title dialog title
     * @param itemType display name for the selected item type
     * @param selectedItems selected items to display
     * @return JOptionPane option value
     */
    private int showScrollableDeleteConfirmation(
        String title,
        String itemType,
        List<?> selectedItems
    ) {
        JPanel messagePanel = new JPanel(new java.awt.BorderLayout(0, 8));
        messagePanel.add(
            new JLabel("Are you sure want to delete the following " + itemType + "?"),
            java.awt.BorderLayout.NORTH
        );

        JTextArea itemsArea = new JTextArea();
        itemsArea.setEditable(false);
        itemsArea.setLineWrap(false);
        itemsArea.setWrapStyleWord(false);

        StringBuilder content = new StringBuilder();
        for (Object item : selectedItems) {
            content.append(item).append(System.lineSeparator());
        }
        itemsArea.setText(content.toString());
        itemsArea.setCaretPosition(0);

        JScrollPane scrollPane = new JScrollPane(itemsArea);
        scrollPane.setPreferredSize(new Dimension(360, 180));
        messagePanel.add(scrollPane, java.awt.BorderLayout.CENTER);

        return JOptionPane.showConfirmDialog(
            null,
            messagePanel,
            title,
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
    }

    /**
     * Deletes the specified test cases and resets table if needed.
     * @param testcaseNodes list of test case nodes to delete
     */
    private void deleteTestCases(List<TestCaseNode> testcaseNodes) {
        Set<ScenarioNode> affectedScenarios = new HashSet<>();
        for (TestCaseNode tcNode : testcaseNodes) {
            if (tcNode.getParent() instanceof ScenarioNode) {
                affectedScenarios.add((ScenarioNode) tcNode.getParent());
            }
        }
        TestCase loadedTestCase = testDesign.getTestCaseComp().getCurrentTestCase();
        Boolean shouldRemove = false;

        for (TestCaseNode testcaseNode : testcaseNodes) {
            if (!shouldRemove) {
                shouldRemove = Objects.equals(loadedTestCase, testcaseNode.getTestCase());
            }
            testcaseNode.getTestCase().delete();
            getTreeModel().removeNodeFromParent(testcaseNode);
        }

        if (shouldRemove) {
            testDesign.getTestCaseComp().resetTable();
        }
        for (ScenarioNode sn : affectedScenarios) {
            persistSortOrder(sn);
        }
    }

    /**
     * Returns the currently selected scenario.
     * @return selected scenario or null if none selected
     */
    private Scenario getSelectedScenario() {
        ScenarioNode scenarioNode = getSelectedScenarioNode();
        if (scenarioNode != null) {
            return scenarioNode.getScenario();
        }
        return null;
    }

    /**
     * Returns all selected scenarios.
     * @return list of selected scenarios
     */
    private List<Scenario> getSelectedScenarios() {
        List<Scenario> scenarios = new ArrayList<>();
        TreePath[] paths = tree.getSelectionPaths();
        if (paths != null && paths.length > 0) {
            for (TreePath path : paths) {
                if (path.getLastPathComponent() instanceof ScenarioNode) {
                    scenarios.add(((ScenarioNode) path.getLastPathComponent()).getScenario());
                }
            }
        }
        return scenarios;
    }

    /**
     * Returns all selected test cases.
     * @return list of selected test cases
     */
    private List<TestCase> getSelectedTestCases() {
        List<TestCase> testcases = new ArrayList<>();
        TreePath[] paths = tree.getSelectionPaths();
        if (paths != null && paths.length > 0) {
            for (TreePath path : paths) {
                if (path.getLastPathComponent() instanceof TestCaseNode) {
                    testcases.add(((TestCaseNode) path.getLastPathComponent()).getTestCase());
                }
            }
        }
        return testcases;
    }

    /**
     * Returns the first selected group node.
     * @return selected group node or null if none selected
     */
    protected GroupNode getSelectedGroupNode() {
        List<GroupNode> groups = getSelectedGroupNodes();
        if (groups.isEmpty()) {
            return null;
        }
        return groups.get(0);
    }

    /**
     * Returns all selected group nodes.
     * @return list of selected group nodes
     */
    protected List<GroupNode> getSelectedGroupNodes() {
        List<GroupNode> groupNodes = new ArrayList<>();
        TreePath[] paths = tree.getSelectionPaths();
        if (paths != null && paths.length > 0) {
            for (TreePath path : paths) {
                if (path.getLastPathComponent() instanceof GroupNode) {
                    groupNodes.add((GroupNode) path.getLastPathComponent());
                }
            }
        }
        return groupNodes;
    }

    /**
     * Returns the first selected scenario node.
     * @return selected scenario node or null if none selected
     */
    public ScenarioNode getSelectedScenarioNode() {
        List<ScenarioNode> scenarioNodes = getSelectedScenarioNodes();
        if (scenarioNodes.isEmpty()) {
            return null;
        }
        return scenarioNodes.get(0);
    }

    /**
     * Returns all selected scenario nodes.
     * @return list of selected scenario nodes
     */
    protected List<ScenarioNode> getSelectedScenarioNodes() {
        List<ScenarioNode> scenarioNodes = new ArrayList<>();
        TreePath[] paths = tree.getSelectionPaths();
        if (paths != null && paths.length > 0) {
            for (TreePath path : paths) {
                if (path.getLastPathComponent() instanceof ScenarioNode) {
                    scenarioNodes.add((ScenarioNode) path.getLastPathComponent());
                }
            }
        }
        return scenarioNodes;
    }

    /**
     * Returns the currently selected test case.
     * @return selected test case or null if none selected
     */
    protected TestCase getSelectedTestCase() {
        TestCaseNode testcaseNode = getSelectedTestCaseNode();
        if (testcaseNode != null) {
            return testcaseNode.getTestCase();
        }
        return null;
    }

    /**
     * Returns the first selected test case node.
     * @return selected test case node or null if none selected
     */
    public TestCaseNode getSelectedTestCaseNode() {
        List<TestCaseNode> tcNodes = getSelectedTestCaseNodes();
        if (tcNodes.isEmpty()) {
            return null;
        }
        return tcNodes.get(0);
    }

    /**
     * Returns all selected test case nodes.
     * @return list of selected test case nodes
     */
    protected List<TestCaseNode> getSelectedTestCaseNodes() {
        List<TestCaseNode> tcNodes = new ArrayList<>();
        TreePath[] paths = tree.getSelectionPaths();
        if (paths != null && paths.length > 0) {
            for (TreePath path : paths) {
                if (path.getLastPathComponent() instanceof TestCaseNode) {
                    tcNodes.add((TestCaseNode) path.getLastPathComponent());
                }
            }
        }
        return tcNodes;
    }

    /**
     * Selects and scrolls to the specified tree path.
     * @param path tree path to select and scroll to
     */
    protected void selectAndScrollTo(final TreePath path) {
        SwingUtilities.invokeLater(
            new Runnable() {

                @Override
                public void run() {
                    tree.setSelectionPath(path);
                    tree.scrollPathToVisible(path);
                }
            }
        );
    }

    /**
     * Moves selected test cases from Test Plan to Reusable Components.
     * Shows error notifications for failures and reloads both trees on success.
     */
    protected void makeAsReusableRTestCase() {
        if (getSelectedTestCaseNodes().isEmpty()) {
            Notification.showWarning("Select at least one test case to make as Project Reusable.");
            return;
        }
        if (!getSelectedTestCaseNodes().isEmpty()) {
            // Save ALL test cases to prevent data loss on reload
            getProject().save();

            boolean anySuccess = false;
            int impactedUpdates = 0;
            for (TestCaseNode testCaseNode : getSelectedTestCaseNodes()) {
                try {
                    getProject().moveTestCaseToReusable(testCaseNode.getTestCase());
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
                load();
                getTestDesign().getReusableTree().load();
                showImpactedReferenceNotification("Moved to Project Reusable", impactedUpdates);
            } else {
                Notification.showWarning("No test cases were moved to Project Reusable.");
            }
        }
    }

    /**
     * Adds a test case to the reusable tree model.
     * @param testCase test case to add to reusable components
     */
    void makeAsReusableRTestCase(TestCase testCase) {
        getTestDesign().getReusableTree().getTreeModel().addTestCase(testCase);
    }

    /**
     * Moves selected test case(s) from Test Plan to Shared Reusable Components.
     */
    private void moveTestCaseToSharedReusable() {
        if (getSelectedTestCaseNodes().isEmpty()) {
            Notification.showWarning("Select at least one test case to make as Shared Reusable.");
            return;
        }
        if (!getSelectedTestCaseNodes().isEmpty()) {
            int option = JOptionPane.showConfirmDialog(
                null,
                "Move selected test case(s) to Shared Reusable Components?",
                "Make As Shared Reusable",
                JOptionPane.YES_NO_OPTION
            );
            if (option != JOptionPane.YES_OPTION) {
                return;
            }
            // Save ALL test cases to prevent data loss on reload
            getProject().save();

            boolean anySuccess = false;
            int impactedUpdates = 0;

            // Move test cases first and record which ones moved successfully.
            List<TestCase> movedSuccessfully = new ArrayList<>();
            for (TestCaseNode testCaseNode : getSelectedTestCaseNodes()) {
                try {
                    getProject().moveTestCaseToSharedReusable(testCaseNode.getTestCase());
                    impactedUpdates +=
                        getProject().getAndResetLastImpactedReusableReferenceUpdates();
                    anySuccess = true;
                    movedSuccessfully.add(testCaseNode.getTestCase());
                } catch (TestCaseConversionException e) {
                    Notification.show(e.getMessage());
                }
            }

            // Only after test cases have been moved successfully, detect and optionally move project-scoped objects
            if (!movedSuccessfully.isEmpty()) {
                // If the user cancels the second confirmation, we simply skip moving objects but do not revert moved test cases.
                try {
                    confirmAndMoveProjectObjectsForTestCases(movedSuccessfully);
                } catch (Exception ex) {
                    LOGGER.log(
                        Level.WARNING,
                        "Error during optional object move after test case migration",
                        ex
                    );
                }
            }
            if (anySuccess) {
                getProject().reload();
                getProject().save();
                load();
                getTestDesign().getSharedReusableTree().load();
                showImpactedReferenceNotification("Moved to Shared Reusable", impactedUpdates);
            } else {
                Notification.showWarning("No test cases were moved to Shared Reusable.");
            }
        }
    }

    protected void showImpactedReferenceNotification(String operationName, int impactedUpdates) {
        if (impactedUpdates > 0) {
            Notification.showSuccess(
                operationName +
                " completed. All impacted test cases have been updated (" +
                impactedUpdates +
                ")."
            );
        } else {
            Notification.showSuccess(
                operationName + " completed. No impacted test case references required updates."
            );
        }
    }

    /**
     * Detects project-scoped object references used by the provided test cases.
     * If any project-only objects are found, prompts the user whether to move those
     * objects/pages to Shared OR. If the user agrees, moves objects/pages to Shared.
     * Returns true when the caller should proceed with moving test cases to Shared; false if cancelled.
     */
    public boolean confirmAndMoveProjectObjectsForTestCases(List<TestCase> testCases) {
        try {
            var repo = getProject().getObjectRepository();
            if (repo == null) return true; // nothing to do

            // Collect project-only references as pairs of pageName -> set(objectName)
            Map<String, Set<String>> projectRefs = new HashMap<>();

            for (TestCase tc : testCases) {
                tc.loadTableModel();
                for (TestStep step : tc.getTestSteps()) {
                    if (!step.isPageObjectStep()) continue;
                    String ref = step.getReference();
                    String obj = step.getObject();
                    if (ref == null || ref.isBlank() || obj == null || obj.isBlank()) continue;

                    // Parse the page reference first to handle scoped tokens like "[Project] Home"
                    ResolvedWebObject.PageRef wref = ResolvedWebObject.PageRef.parse(ref);
                    var wres = repo.resolveWebObject(wref, obj);
                    if (wres != null) {
                        if (wres.isFromProject()) {
                            projectRefs
                                .computeIfAbsent(wres.getPageName(), k -> new HashSet<>())
                                .add(obj);
                        }
                        continue;
                    }

                    ResolvedMobileObject.PageRef mref = ResolvedMobileObject.PageRef.parse(ref);
                    var mres = repo.resolveMobileObject(mref, obj);
                    if (mres != null) {
                        if (mres.isFromProject()) {
                            projectRefs
                                .computeIfAbsent(mres.getPageName(), k -> new HashSet<>())
                                .add(obj);
                        }
                        continue;
                    }

                    ResolvedStructuredDataObject.PageRef sref = ResolvedStructuredDataObject.PageRef.parse(
                        ref
                    );
                    var sres = repo.resolveStructuredDataObject(sref, obj);
                    if (sres != null) {
                        if (sres.isFromProject()) {
                            projectRefs
                                .computeIfAbsent(sres.getPageName(), k -> new HashSet<>())
                                .add(obj);
                        }
                        continue;
                    }

                    ResolvedSapObject.PageRef sapref = ResolvedSapObject.PageRef.parse(ref);
                    var sapres = repo.resolveSapObject(sapref, obj);
                    if (sapres != null) {
                        if (sapres.isFromProject()) {
                            projectRefs
                                .computeIfAbsent(sapres.getPageName(), k -> new HashSet<>())
                                .add(obj);
                        }
                        continue;
                    }
                }
            }

            if (projectRefs.isEmpty()) return true; // no project-only objects found

            // Build confirmation message
            StringBuilder sb = new StringBuilder();
            sb.append(
                "The selected test case(s) reference project-scoped Object Repository items:\n\n"
            );
            for (var e : projectRefs.entrySet()) {
                sb
                    .append("Page: ")
                    .append(e.getKey())
                    .append(" -> Objects: ")
                    .append(e.getValue())
                    .append("\n");
            }
            sb.append(
                "\nDo you want to move these objects/pages to Shared Object Repository as well?\n"
            );

            int opt = JOptionPane.showConfirmDialog(
                null,
                sb.toString(),
                "Move referenced Project Objects to Shared?",
                JOptionPane.YES_NO_OPTION
            );

            // if (opt == JOptionPane.CANCEL_OPTION || opt == JOptionPane.CLOSED_OPTION) return false;
            if (opt != JOptionPane.YES_OPTION) return true; // proceed without moving objects

            // User agreed to move objects/pages to Shared OR.
            // Only move the specific objects referenced by the selected test cases.
            for (String pageName : projectRefs.keySet()) {
                try {
                    for (String obj : projectRefs.get(pageName)) {
                        // try web
                        var r = repo.resolveWebObjectWithScope(pageName, obj);
                        if (r != null && r.isFromProject()) {
                            repo.moveWebObject(r, pageName);
                            continue;
                        }
                        var rm = repo.resolveMobileObjectWithScope(pageName, obj);
                        if (rm != null && rm.isFromProject()) {
                            repo.moveMobileObject(rm, pageName);
                            continue;
                        }
                        var rs = repo.resolveStructuredDataObjectWithScope(pageName, obj);
                        if (rs != null && rs.isFromProject()) {
                            repo.moveStructuredDataObject(rs, pageName);
                            continue;
                        }
                        var rsp = repo.resolveSapObjectWithScope(pageName, obj);
                        if (rsp != null && rsp.isFromProject()) {
                            repo.moveSapObject(rsp, pageName);
                            continue;
                        }
                    }
                } catch (Exception ex) {
                    // ignore individual failures and continue
                }
            }

            // Save repository and refresh UI.
            // Remove any now-empty source pages (moved all objects) to keep repository consistent
            try {
                com.ing.datalib.or.web.WebOR projectWebOR = repo.getWebOR();
                if (projectWebOR != null) {
                    for (String pageName : projectRefs.keySet()) {
                        com.ing.datalib.or.web.WebORPage sourcePage = projectWebOR.getPageByName(
                            pageName
                        );
                        if (sourcePage != null && sourcePage.getObjectGroups().isEmpty()) {
                            sourcePage.removeFromParent();
                        }
                    }
                }
            } catch (Throwable t) {
                // Ignore - best effort cleanup
            }

            repo.save();
            // Ensure UI reload runs on the Swing EDT to avoid potential threading/race issues
            javax.swing.SwingUtilities.invokeLater(() -> getTestDesign().getObjectRepo().load());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return true; // allow proceed on error
        }
    }

    /**
     * Saves the currently displayed test case in the editor if it exists.
     * This ensures unsaved changes are persisted before operations like moving test cases.
     */
    protected void saveCurrentTestCaseIfDisplayed() {
        TestCase currentTestCase = getTestDesign().getTestCaseComp().getCurrentTestCase();
        if (currentTestCase != null) {
            currentTestCase.save();
        }
    }

    /**
     * Converts selected scenarios or test cases to manual test case CSV format.
     * @throws IOException if file writing fails
     */
    private void convertToManual() throws IOException {
        if (!getSelectedScenarios().isEmpty()) {
            testDesign
                .getsMainFrame()
                .getStepMap()
                .convertScenarios(Utils.saveDialog("Manual TestCase.csv"), getSelectedScenarios());
        } else if (!getSelectedTestCases().isEmpty()) {
            testDesign
                .getsMainFrame()
                .getStepMap()
                .convertTestCase(Utils.saveDialog("Manual TestCase.csv"), getSelectedTestCases());
        } else {
            testDesign
                .getsMainFrame()
                .getStepMap()
                .convertScenarios(
                    Utils.saveDialog("Manual TestCase.csv"),
                    getProject().getScenarios()
                );
        }
    }

    /**
     * Sorts the children of the selected tree node.
     */
    private void sort() {
        if (tree.getSelectionPath() != null) {
            Object node = tree.getSelectionPath().getLastPathComponent();
            getTreeModel().sort(node);
            persistSortOrder(node);
        }
    }

    /**
     * @return whether this is a Test Plan tree currently showing a group layer.
     */
    boolean isTestPlanGrouped() {
        return (
            treeModel instanceof TestPlanTreeModel &&
            ((TestPlanTreeModel) treeModel).getRoot().isGrouped()
        );
    }

    /**
     * Persists the current Test Plan grouping to {@code TestPlan/.groups} when this
     * is a Test Plan tree. No-op for other trees.
     */
    private void saveGroupsIfTestPlan() {
        if (treeModel instanceof TestPlanTreeModel) {
            ((TestPlanTreeModel) treeModel).saveGroups();
        }
    }

    /**
     * @return the currently selected {@link TestPlanGroupNode}, or {@code null}.
     */
    protected TestPlanGroupNode getSelectedTestPlanGroupNode() {
        TreePath path = tree.getSelectionPath();
        if (path != null && path.getLastPathComponent() instanceof TestPlanGroupNode) {
            return (TestPlanGroupNode) path.getLastPathComponent();
        }
        return null;
    }

    /**
     * Prompts for a name and creates a new scenario group.
     */
    private void addGroup() {
        if (!(treeModel instanceof TestPlanTreeModel)) {
            return;
        }
        String name = JOptionPane.showInputDialog(
            tree,
            "Group name:",
            "New Group",
            JOptionPane.PLAIN_MESSAGE
        );
        if (name == null) {
            return;
        }
        name = name.trim();
        if (name.isEmpty() || name.equals(TestPlanGroupNode.UNGROUPED)) {
            return;
        }
        if (!Validator.isValidName(name)) {
            return;
        }
        TestPlanGroupNode group = ((TestPlanTreeModel) treeModel).addGroup(name);
        if (group == null) {
            Notification.show("Group '" + name + "' already exists.");
            return;
        }
        selectAndScrollTo(new TreePath(group.getPath()));
    }

    /**
     * Starts inline editing of the selected group's name.
     */
    private void renameGroup() {
        TestPlanGroupNode group = getSelectedTestPlanGroupNode();
        if (group != null) {
            tree.startEditingAtPath(new TreePath(group.getPath()));
        }
    }

    /**
     * Deletes the selected group after confirmation, moving its scenarios to the
     * {@code (Ungrouped)} node.
     */
    private void deleteGroup() {
        TestPlanGroupNode group = getSelectedTestPlanGroupNode();
        if (group == null || group.isUngrouped() || !(treeModel instanceof TestPlanTreeModel)) {
            return;
        }
        int option = JOptionPane.showConfirmDialog(
            null,
            "<html><body><p style='width: 200px;'>" +
            "Delete group '" +
            group +
            "'? Its scenarios will move to " +
            TestPlanGroupNode.UNGROUPED +
            ".</p></body></html>",
            "Delete Group",
            JOptionPane.YES_NO_OPTION
        );
        if (option == JOptionPane.YES_OPTION) {
            ((TestPlanTreeModel) treeModel).deleteGroup(group);
        }
    }

    /**
     * Moves all selected scenarios into the given target group.
     * @param target destination group
     */
    private void moveSelectedScenariosToGroup(TestPlanGroupNode target) {
        if (!(treeModel instanceof TestPlanTreeModel)) {
            return;
        }
        for (ScenarioNode scenarioNode : getSelectedScenarioNodes()) {
            ((TestPlanTreeModel) treeModel).moveScenarioToGroup(scenarioNode, target);
        }
    }

    /**
     * Rebuilds the "Move to Group" submenu with the current groups, excluding the
     * selected scenario's current parent group.
     * @param menu the submenu to populate
     */
    void buildMoveToGroupSubmenu(JMenu menu) {
        menu.removeAll();
        if (!(treeModel instanceof TestPlanTreeModel)) {
            return;
        }
        TestPlanNode root = ((TestPlanTreeModel) treeModel).getRoot();
        ScenarioNode selected = getSelectedScenarioNode();
        Object currentParent = selected != null ? selected.getParent() : null;
        for (final TestPlanGroupNode group : root.getGroupNodes()) {
            if (group == currentParent) {
                continue;
            }
            JMenuItem item = new JMenuItem(group.toString());
            item.setFont(new Font("ING Me", Font.PLAIN, 11));
            item.addActionListener(e -> moveSelectedScenariosToGroup(group));
            menu.add(item);
        }
    }

    /**
     * Persists the sort order of the given node's children to a {@code .sort_order} file on disk.
     * Call this after any operation that changes the membership or order of a node's children.
     * @param node the tree node whose children order should be saved (GroupNode or ScenarioNode)
     */
    protected void persistSortOrder(Object node) {
        if (node instanceof ScenarioNode) {
            ScenarioNode scenarioNode = (ScenarioNode) node;
            File scenarioDir = new File(scenarioNode.getScenario().getLocation());
            List<String> names = new ArrayList<>();
            for (TestCaseNode tcNode : TestCaseNode.toList(scenarioNode.children())) {
                if (tcNode == null || tcNode.getTestCase() == null) {
                    continue;
                }
                names.add(tcNode.getTestCase().getName());
            }
            SortOrderStore.save(scenarioDir, names);
        } else if (node instanceof TestPlanGroupNode) {
            // Scenario order within a Test Plan group is persisted via .groups.
            saveGroupsIfTestPlan();
        } else if (node instanceof TestPlanNode && ((TestPlanNode) node).isGrouped()) {
            // Group order at the Test Plan root is persisted via .groups.
            saveGroupsIfTestPlan();
        } else if (node instanceof GroupNode) {
            List<ScenarioNode> scenarioNodes = ScenarioNode.toList(((GroupNode) node).children());
            if (!scenarioNodes.isEmpty()) {
                File parentDir = new File(scenarioNodes.get(0).getScenario().getLocation())
                .getParentFile();
                List<String> names = new ArrayList<>();
                for (ScenarioNode sn : scenarioNodes) {
                    names.add(sn.getScenario().getName());
                }
                SortOrderStore.save(parentDir, names);
            }
        }
    }

    /**
     * Opens the tag editor for selected items.
     */
    private void editTag() {
        TreePath[] sel = tree.getSelectionPaths();
        if (sel != null && sel.length > 0) {
            if (sel.length > 1) {
                editTag(Arrays.asList(sel));
            } else {
                editTag(sel[0]);
            }
        }
    }

    /**
     * Adds a new tag to the project.
     * @param tag tag name
     * @return created tag
     */
    private Tag onAddTag(String tag) {
        getProject().getInfo().addMeta(Meta.createTag(tag));
        return Tag.create(tag);
    }

    /**
     * Removes a tag from the project.
     * @param tag tag to remove
     */
    private void onRemoveTag(Tag tag) {
        getProject().getInfo().removeAll(tag);
    }

    /**
     * Renames a tag across the entire project by delegating to
     * {@link com.ing.datalib.model.ProjectInfo#renameAll(String, String)}.
     * This ensures all test cases, scenarios, meta entries, and project-level
     * references receive the updated name.
     *
     * @param tag      the tag to rename (its value is updated in-place)
     * @param newValue the new tag name
     */
    private void onUpdateTag(Tag tag, String newValue) {
        getProject().getInfo().renameAll(tag.getValue(), newValue);
        tag.setValue(newValue);
    }

    /**
     * Opens the tag editor for a test case data item and, when a corresponding
     * {@link TestCase} is supplied, re-saves its YAML so the new tag set is
     * mirrored on disk.
     */
    private void editTag(DataItem tc, TestCase testCase) {
        TagEditorDialog
            .build(
                testDesign.getsMainFrame(),
                getProject().getInfo().getAllTags(tc.getTags()),
                tc.getTags(),
                this::onRemoveTag,
                this::onAddTag,
                this::onUpdateTag
            )
            .withTitle(editTagTitle(tc.getName()))
            .show(
                tags -> {
                    tc.setTags(tags);
                    if (testCase != null) {
                        testCase.saveMetadata();
                    }
                }
            );
    }

    /**
     * Opens the tag editor for a scenario metadata.
     * @param scn scenario metadata
     */
    private void editTag(Meta scn) {
        TagEditorDialog
            .build(
                testDesign.getsMainFrame(),
                getProject().getInfo().getAllTags(scn.getTags()),
                scn.getTags(),
                this::onRemoveTag,
                this::onAddTag,
                this::onUpdateTag
            )
            .withTitle(editTagTitle(scn.getName()))
            .show(scn::setTags);
    }

    /**
     * Creates the tag editor dialog title.
     * @param t item name
     * @return formatted title string
     */
    private String editTagTitle(String t) {
        return String.format("Edit Tag: %s", t);
    }

    /**
     * Opens the tag editor for a tree path (scenario or test case).
     * @param path tree path to edit tags for
     */
    private void editTag(TreePath path) {
        if (path.getLastPathComponent() instanceof TestCaseNode) {
            TestCase tcn = ((TestCaseNode) path.getLastPathComponent()).getTestCase();
            editTag(
                getProject()
                    .getInfo()
                    .getData()
                    .findOrCreate(tcn.getName(), tcn.getScenario().getName()),
                tcn
            );
        } else if (path.getLastPathComponent() instanceof ScenarioNode) {
            Scenario scn = ((ScenarioNode) path.getLastPathComponent()).getScenario();
            editTag(getProject().getInfo().findScenarioOrCreate(scn.getName()));
        }
    }

    /**
     * Opens the tag editor for multiple tree paths.
     * @param paths list of tree paths to edit tags for
     */
    private void editTag(List<TreePath> paths) {
        paths.stream().forEach(this::editTag);
    }

    /**
     * Shows impacted test cases for the selected test case.
     */
    private void getImpactedTestCases() {
        TestCase testCase = getSelectedTestCase();
        if (testCase != null) {
            String scenarioName = testCase.getScenario().getName();
            String testCaseName = testCase.getName();
            testDesign
                .getImpactUI()
                .loadForTestCase(
                    getProject().getImpactedTestCaseTestCases(scenarioName, testCaseName),
                    scenarioName,
                    testCaseName
                );
        } else {
            Notification.show("Select a Valid TestCase");
        }
    }

    /**
     * Generates and copies command line syntax for running the selected test case.
     */
    private void getCmdLineSyntax() {
        TestCase testCase = getSelectedTestCase();
        if (testCase != null) {
            String scenarioName = testCase.getScenario().getName();
            String testCaseName = testCase.getName();
            String syntax = String.format(
                "%s -run -project_location \"%s\" -scenario \"%s\" -testcase \"%s\" -browser \"%s\"",
                getBatRCommand(),
                getProject().getLocation(),
                scenarioName,
                testCaseName,
                getTestDesign().getDefaultBrowser()
            );
            Utils.copyTextToClipboard(syntax);
            Notification.show("Syntax has been copied to Clipboard");
        } else {
            Notification.show("Select a Valid TestCase");
        }
    }

    /**
     * Returns the appropriate run command based on the operating system.
     * @return "Run.bat" for Windows, "Run.command" for others
     */
    private String getBatRCommand() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("windows")) {
            return "ingenious.bat";
        }
        return "ingenious.command";
    }

    /**
     * Shows the project details dialog if a tree path is selected.
     */
    private void showDetails() {
        TreePath path = tree.getSelectionPath();
        if (path != null) {
            showProjDetails();
        }
    }

    /**
     * Displays the project properties dialog.
     */
    private void showProjDetails() {
        projectProperties.loadForCurrentProject();
        //        projectProperties.pack();
        projectProperties.setLocationRelativeTo(null);
        projectProperties.setVisible(true);
    }

    /**
     * Returns the JTree component.
     * @return tree component
     */
    public final JTree getTree() {
        return tree;
    }

    /**
     * Returns the current project.
     * @return project instance
     */
    public final Project getProject() {
        return testDesign.getProject();
    }

    /**
     * Returns the parent TestDesign component.
     * @return test design component
     */
    public final TestDesign getTestDesign() {
        return testDesign;
    }

    /**
     * Loads the project into the tree model and refreshes the view.
     */
    public final void load() {
        treeModel.setProject(testDesign.getProject());
        treeModel.reload();
        getTree().setSelectionPath(new TreePath(treeModel.getFirstNode().getPath()));
        loadTableModelForSelection();
    }

    /**
     * Context menu for the project tree with actions for scenarios and test cases.
     */
    class ProjectPopupMenu extends JPopupMenu {
        protected JMenuItem addScenario;
        protected JMenuItem renameScenario;
        protected JMenuItem deleteScenario;
        protected JMenuItem addGroup;
        protected JMenuItem renameGroup;
        protected JMenuItem deleteGroup;
        protected JMenu moveToGroup;
        protected JMenuItem addTestCase;
        protected JMenuItem renameTestCase;
        protected JMenuItem deleteTestCase;

        protected JMenuItem toggleTestCase;
        protected JMenuItem toggleSharedReusable;
        protected JMenuItem toggleProjectReusable;

        protected JMenuItem impactAnalysis;

        protected JMenuItem copy;
        protected JMenuItem cut;
        protected JMenuItem paste;
        protected JMenuItem sort;

        protected JMenuItem getCmdSyntax;
        protected JMenuItem getAzDo;

        /**
         * Constructs a new ProjectPopupMenu and initializes menu items.
         */
        public ProjectPopupMenu() {
            init();
        }

        /**
         * Initializes all menu items and adds them to the popup menu.
         */
        protected final void init() {
            add(addScenario = create("Add Scenario", Keystroke.NEW));
            add(renameScenario = create("Rename Scenario", Keystroke.RENAME));
            add(deleteScenario = create("Delete Scenario", Keystroke.DELETE));
            addSeparator();
            add(addGroup = create("New Group", null));
            add(renameGroup = create("Rename Group", null));
            add(deleteGroup = create("Delete Group", null));
            moveToGroup = new JMenu("Move to Group");
            moveToGroup.setFont(new Font("ING Me", Font.PLAIN, 11));
            add(moveToGroup);
            addSeparator();
            add(addTestCase = create("Add TestCase", Keystroke.NEW));
            add(renameTestCase = create("Rename TestCase", Keystroke.RENAME));
            add(deleteTestCase = create("Delete TestCase", Keystroke.DELETE));

            addSeparator();
            JMenu menu = new JMenu("Export As");
            menu.setFont(UIManager.getFont("TableMenu.font"));
            menu.add(create("Manual Testcase", null));
            add(menu);
            add(toggleTestCase = create("Make As TestCase", null));
            toggleTestCase.setText("Make As TestCase");
            toggleTestCase.setVisible(false);
            add(toggleProjectReusable = create("Make As Project Reusable", null));
            toggleProjectReusable.setText("Make As Project Reusable");
            toggleProjectReusable.setVisible(true);
            add(toggleSharedReusable = create("Make As Shared Reusable", null));
            toggleSharedReusable.setText("Make As Shared Reusable");
            toggleSharedReusable.setVisible(true);
            addSeparator();
            setCCP();
            addSeparator();
            add(impactAnalysis = create("Get Impacted TestCases", null));
            add(getCmdSyntax = create("Get CmdLine Syntax", null));

            addSeparator();
            add(sort = create("Sort", null));
            addSeparator();
            add(create("Details", Keystroke.ALTENTER));
            sort.setIcon(Canvas.EmptyIcon);
        }

        /**
         * Configures menu items for scenario context.
         */
        protected void forScenario() {
            renameScenario.setEnabled(true);
            deleteScenario.setEnabled(true);
            addTestCase.setEnabled(true);

            addScenario.setEnabled(false);
            renameTestCase.setEnabled(false);
            deleteTestCase.setEnabled(false);
            toggleTestCase.setEnabled(false);
            toggleSharedReusable.setEnabled(false);
            toggleProjectReusable.setEnabled(false);

            impactAnalysis.setEnabled(false);
            getCmdSyntax.setEnabled(false);

            copy.setEnabled(true);
            copy.setFont(UIManager.getFont("TableMenu.font"));
            cut.setEnabled(false);
            cut.setFont(UIManager.getFont("TableMenu.font"));
            paste.setEnabled(true);
            paste.setFont(UIManager.getFont("TableMenu.font"));

            sort.setEnabled(true);
            boolean grouped = isTestPlanGrouped();
            setGroupItemsVisible(false, false, false, grouped);
            if (grouped) {
                buildMoveToGroupSubmenu(moveToGroup);
            }
        }

        /**
         * Configures menu items for test case context.
         */
        protected void forTestCase() {
            addScenario.setEnabled(false);
            renameScenario.setEnabled(false);
            deleteScenario.setEnabled(false);

            addTestCase.setEnabled(false);

            renameTestCase.setEnabled(true);
            deleteTestCase.setEnabled(true);
            toggleTestCase.setEnabled(true);
            toggleSharedReusable.setEnabled(true);
            toggleProjectReusable.setEnabled(true);

            impactAnalysis.setEnabled(true);

            getCmdSyntax.setEnabled(true);

            copy.setEnabled(true);
            copy.setFont(UIManager.getFont("TableMenu.font"));
            cut.setEnabled(true);
            cut.setFont(UIManager.getFont("TableMenu.font"));
            paste.setEnabled(true);
            paste.setFont(UIManager.getFont("TableMenu.font"));

            sort.setEnabled(false);
            setGroupItemsVisible(false, false, false, false);
        }

        /**
         * Configures menu items for test plan (group) context.
         */
        protected void forTestPlan() {
            addScenario.setEnabled(true);

            renameScenario.setEnabled(false);
            deleteScenario.setEnabled(false);

            addTestCase.setEnabled(false);
            renameTestCase.setEnabled(false);
            deleteTestCase.setEnabled(false);
            toggleTestCase.setEnabled(false);
            toggleSharedReusable.setEnabled(false);
            toggleProjectReusable.setEnabled(false);

            impactAnalysis.setEnabled(false);
            getCmdSyntax.setEnabled(false);

            copy.setEnabled(false);
            copy.setFont(UIManager.getFont("TableMenu.font"));
            cut.setEnabled(false);
            cut.setFont(UIManager.getFont("TableMenu.font"));
            paste.setEnabled(true);
            paste.setFont(UIManager.getFont("TableMenu.font"));

            sort.setEnabled(true);
            setGroupItemsVisible(true, false, false, false);
        }

        /**
         * Configures menu items for a scenario group context (Test Plan tree only).
         */
        protected void forGroup() {
            TestPlanGroupNode group = getSelectedTestPlanGroupNode();
            boolean named = group != null && !group.isUngrouped();

            addScenario.setEnabled(true);
            renameScenario.setEnabled(false);
            deleteScenario.setEnabled(false);
            addTestCase.setEnabled(false);
            renameTestCase.setEnabled(false);
            deleteTestCase.setEnabled(false);
            toggleTestCase.setEnabled(false);
            toggleSharedReusable.setEnabled(false);
            toggleProjectReusable.setEnabled(false);

            impactAnalysis.setEnabled(false);
            getCmdSyntax.setEnabled(false);

            copy.setEnabled(false);
            copy.setFont(UIManager.getFont("TableMenu.font"));
            cut.setEnabled(false);
            cut.setFont(UIManager.getFont("TableMenu.font"));
            paste.setEnabled(true);
            paste.setFont(UIManager.getFont("TableMenu.font"));

            sort.setEnabled(true);
            setGroupItemsVisible(false, true, named, false);
        }

        /**
         * Controls visibility of the grouping menu items.
         * @param addG   show "New Group"
         * @param rename show "Rename Group"
         * @param delete show "Delete Group"
         * @param move   show "Move to Group" submenu
         */
        protected void setGroupItemsVisible(
            boolean addG,
            boolean rename,
            boolean delete,
            boolean move
        ) {
            addGroup.setVisible(addG);
            renameGroup.setVisible(rename);
            deleteGroup.setVisible(delete);
            moveToGroup.setVisible(move);

            // Clean up orphaned separators after visibility changes
            cleanupOrphanedSeparators();
        }

        /**
         * Hides separators that are orphaned or duplicate.
         * A separator is hidden if:
         * 1. It's between two groups where one or both have no visible items, AND
         * 2. There's another separator adjacent to it with only hidden items between them (consecutive duplicate).
         *
         * This ensures that when a group of items (like group actions) is completely hidden,
         * we don't get multiple consecutive separators, but keep exactly one separator between
         * the surrounding visible item groups.
         *
         * Since the menu is reused, separators are hidden rather than removed to allow proper restoration.
         */
        private void cleanupOrphanedSeparators() {
            java.util.List<Integer> separatorIndices = new java.util.ArrayList<>();

            // Find all separator indices
            for (int i = 0; i < getComponentCount(); i++) {
                java.awt.Component comp = getComponent(i);
                if (comp instanceof javax.swing.JSeparator) {
                    separatorIndices.add(i);
                }
            }

            // For each separator, determine if it should be hidden
            for (int sepIdx : separatorIndices) {
                boolean hasVisibleBefore = false;
                boolean hasVisibleAfter = false;

                // Find the nearest visible item before this separator
                // (stop at the previous separator, don't look across separator boundaries)
                for (int i = sepIdx - 1; i >= 0; i--) {
                    java.awt.Component comp = getComponent(i);
                    if (comp != null && comp instanceof javax.swing.JSeparator) {
                        // Hit another separator, stop looking
                        break;
                    }
                    if (comp != null && comp.isVisible()) {
                        hasVisibleBefore = true;
                        break;
                    }
                }

                // Find the nearest visible item after this separator
                // (stop at the next separator, don't look across separator boundaries)
                for (int i = sepIdx + 1; i < getComponentCount(); i++) {
                    java.awt.Component comp = getComponent(i);
                    if (comp != null && comp instanceof javax.swing.JSeparator) {
                        // Hit another separator, stop looking
                        break;
                    }
                    if (comp != null && comp.isVisible()) {
                        hasVisibleAfter = true;
                        break;
                    }
                }

                java.awt.Component separator = getComponent(sepIdx);
                if (separator == null) {
                    continue;
                }

                // If one or both sides have no visible items (orphaned separator)
                if (!hasVisibleBefore || !hasVisibleAfter) {
                    // Check if this is a duplicate separator (has another orphaned separator adjacent)
                    // Only keep the first one, hide subsequent duplicates
                    boolean isDuplicate = false;

                    // Check if there's another orphaned separator before this one
                    for (int idx : separatorIndices) {
                        if (idx >= sepIdx) {
                            break; // Only check separators before this one
                        }
                        // Check if that separator is also orphaned
                        boolean thatHasVisibleAfter = false;
                        for (int i = idx + 1; i < getComponentCount(); i++) {
                            java.awt.Component comp = getComponent(i);
                            if (comp != null && comp instanceof javax.swing.JSeparator) {
                                break;
                            }
                            if (comp != null && comp.isVisible()) {
                                thatHasVisibleAfter = true;
                                break;
                            }
                        }
                        if (!thatHasVisibleAfter) {
                            // Previous separator is also orphaned with nothing after it
                            isDuplicate = true;
                            break;
                        }
                    }

                    separator.setVisible(!isDuplicate);
                } else {
                    // Keep separator if it has visible items on both sides
                    separator.setVisible(true);
                }
            }
        }

        /**
         * Creates a menu item with the specified name and keystroke.
         * @param name menu item name
         * @param keyStroke keyboard shortcut
         * @return created menu item
         */
        protected JMenuItem create(String name, KeyStroke keyStroke) {
            try {
                //create the font to use. Specify the size!
                Font customFont = Font.createFont(
                    Font.TRUETYPE_FONT,
                    new File("resources/ui/resources/fonts/ingme_regular.ttf")
                ); //.deriveFont(12f);
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                //register the font
                ge.registerFont(customFont);
            } catch (IOException | FontFormatException e) {
                //  e.printStackTrace();
            }

            JMenuItem menuItem = new JMenuItem(name);

            menuItem.setActionCommand(name);
            menuItem.setAccelerator(keyStroke);
            menuItem.addActionListener(ProjectTree.this);
            menuItem.setFont(new Font("ING Me", Font.PLAIN, 11));
            return menuItem;
        }

        /**
         * Sets up Cut/Copy/Paste menu items.
         */
        private void setCCP() {
            TransferActionListener actionListener = new TransferActionListener();

            cut = new JMenuItem("Cut");
            cut.setActionCommand((String) TransferHandler.getCutAction().getValue(Action.NAME));
            cut.addActionListener(actionListener);
            cut.setAccelerator(Keystroke.CUT);
            cut.setMnemonic(KeyEvent.VK_T);
            add(cut);

            copy = new JMenuItem("Copy");
            copy.setActionCommand((String) TransferHandler.getCopyAction().getValue(Action.NAME));
            copy.addActionListener(actionListener);
            copy.setAccelerator(Keystroke.COPY);
            copy.setMnemonic(KeyEvent.VK_C);
            add(copy);

            paste = new JMenuItem("Paste");
            paste.setActionCommand((String) TransferHandler.getPasteAction().getValue(Action.NAME));
            paste.addActionListener(actionListener);
            paste.setAccelerator(Keystroke.PASTE);
            paste.setMnemonic(KeyEvent.VK_P);
            add(paste);
        }
    }

    /**
     * Alters default tree key bindings to support cut, copy, and paste operations.
     */
    private void alterTreeDefaultKeyBindings() {
        int menuShortcutKeyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        tree.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_X, menuShortcutKeyMask), "none");
        tree.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_C, menuShortcutKeyMask), "none");
        tree.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_V, menuShortcutKeyMask), "none");

        tree.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_X, menuShortcutKeyMask), "cut");
        tree.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_C, menuShortcutKeyMask), "copy");
        tree.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_V, menuShortcutKeyMask), "paste");
    }
}

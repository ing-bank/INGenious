package com.ing.ide.main.mainui.components.testdesign.tree;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.exception.TestCaseConversionException;
import com.ing.datalib.model.DataItem;
import com.ing.datalib.model.Meta;
import com.ing.datalib.model.Tag;
import com.ing.ide.main.mainui.components.testdesign.TestDesign;
import com.ing.ide.main.mainui.components.testdesign.tree.model.GroupNode;
import com.ing.ide.main.mainui.components.testdesign.tree.model.ProjectTreeModel;
import com.ing.ide.main.mainui.components.testdesign.tree.model.ScenarioNode;
import com.ing.ide.main.mainui.components.testdesign.tree.model.TestCaseNode;
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
import java.awt.Component;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.tree.TreePath;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;

/**
 * UI tree component for displaying and managing Test Plan scenarios and test cases.
 * Provides context menus, drag-and-drop support, and editing capabilities.
 */
public class ProjectTree implements ActionListener {

    private static final Logger LOGGER = Logger.getLogger(ProjectTree.class.getName());

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
            Font customFont = Font.createFont(Font.TRUETYPE_FONT, new File("resources/ui/resources/fonts/ingme_regular.ttf"));//.deriveFont(12f);
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

        tree.getActionMap().put("AltEnter", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                showDetails();
            }
        });

        tree.getActionMap().put("New", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                onNewAction();
            }
        });

        tree.getActionMap().put("Delete", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                onDeleteAction();
            }
        });

        tree.getActionMap().put("Rename", new AbstractAction() {

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
        });

        tree.getActionMap().put("Escape", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                if (tree.isEditing()) {
                    tree.cancelEditing();
                }
            }
        });

        tree.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    loadTableModelForSelection();
                }
            }
        });
        popupMenu.addPopupMenuListener(new PopupMenuListener() {

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
        });
        setTreeIcon();
        tree.getCellEditor().addCellEditorListener(new CellEditorListener() {
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
        });
    }

    /**
     * Sets the custom icons for tree nodes based on node type.
     */
    private void setTreeIcon() {
        try {
            //create the font to use. Specify the size!
            Font customFont = Font.createFont(Font.TRUETYPE_FONT, new File("resources/ui/resources/fonts/ingme_regular.ttf"));//.deriveFont(12f);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            //register the font
            ge.registerFont(customFont);
        } catch (IOException | FontFormatException e) {
            //  e.printStackTrace();
        }
        tree.setFont(new Font("ING Me", Font.PLAIN, 11));
        new TreeSelectionRenderer(tree) {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean isLeaf, int row, boolean focused) {
                Component c = super.getTreeCellRendererComponent(tree, value, selected, expanded, isLeaf, row, focused);
                if (value instanceof GroupNode) {
                    setIcons(IconSettings.getIconSettings().getReusableFolder());
                } else if (value instanceof ScenarioNode) {
                    setIcons(IconSettings.getIconSettings().getTestPlanScenario());
                } else if (value instanceof TestCaseNode) {
                    if (ProjectTree.this instanceof ReusableTree) {
                        setIcons(IconSettings.getIconSettings().getReusableTestCase());
                    } else {
                        setIcons(IconSettings.getIconSettings().getTestPlanTestCase());
                    }
                } else {
                    setIcons(IconSettings.getIconSettings().getTestPlanRoot());
                }
                return c;
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
        } else if (selected instanceof GroupNode) {
            popupMenu.forTestPlan();
        }
    }

    /**
     * Handles the "New" action based on current selection.
     */
    protected void onNewAction() {
        if (getSelectedScenarioNode() != null) {
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
            case "Sort":
                sort();
                break;
            case "Edit Tag":
                editTag();
                break;
            case "Make As Reusable/TestCase":
                makeAsReusableRTestCase();
                break;
            case "Details":
                showDetails();
                break;
            case "Manual Testcase": {
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
        ScenarioNode scNode = treeModel.addScenario(getSelectedGroupNode(),
                testDesign.getProject().addScenario(fetchNewScenarioName()));
        selectAndScrollTo(new TreePath(scNode.getPath()));
    }

    /**
     * Generates a unique name for a new scenario.
     * @return unique scenario name
     */
    private String fetchNewScenarioName() {
        String newScenarioName = "NewScenario";
        for (int i = 0;; i++) {
            if (testDesign.getProject().getScenarioByName(newScenarioName) == null) {
                break;
            }
            newScenarioName = "NewScenario" + i;
        }
        return newScenarioName;
    }

    /**
     * Adds a new test case to the selected scenario.
     */
    private void addTestCase() {
        ScenarioNode scenarioNode = getSelectedScenarioNode();
        if (scenarioNode != null) {
            TestCase testcase;
            String testCaseName = fetchNewTestCaseName(scenarioNode.getScenario());
            testcase = scenarioNode.getScenario().addTestCase(testCaseName);
            testDesign.loadTableModelForSelection(testcase);
            selectAndScrollTo(new TreePath(treeModel.
                    addTestCase(scenarioNode, testcase).getPath()));
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
            if (scenario.getTestCaseByName(newTestCaseName) == null 
                    && !getProject().hasTestCaseInAnyScenario(scenario.getName(), newTestCaseName)) {
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
                    return true;
                } else {
                    Notification.show("Scenario " + name + " Already present");
                    return false;
                }
            }
            TestCaseNode testCaseNode = getSelectedTestCaseNode();
            if (testCaseNode != null && !testCaseNode.toString().equals(name)) {
                if (testCaseNode.getTestCase().rename(name)) {
                    getTreeModel().reload(testCaseNode);
                    testDesign.getTestCaseComp().refreshTitle();
                    return true;
                } else {
                    Notification.show("Testcase '" + name + "' Already present in Scenario - " + getSelectedTestCase().getScenario().getName());
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
        getTestDesign().getReusableTree()
                .getTreeModel().onScenarioRename(scenario);
    }

    /**
     * Deletes selected scenarios after user confirmation.
     */
    private void deleteScenarios() {
        List<ScenarioNode> scenarioNodes = getSelectedScenarioNodes();
        if (!scenarioNodes.isEmpty()) {
            int option = JOptionPane.showConfirmDialog(null,
                    "<html><body><p style='width: 200px;'>"
                    + "Are you sure want to delete the following Scenarios?<br>"
                    + scenarioNodes
                    + "</p></body></html>",
                    "Delete Scenario",
                    JOptionPane.YES_NO_OPTION);
            if (option == JOptionPane.YES_OPTION) {
                LOGGER.log(Level.INFO, "Delete Scenarios approved for {0}; {1}",
                        new Object[]{scenarioNodes.size(), scenarioNodes});
                for (ScenarioNode scenarioNode : scenarioNodes) {
                    deleteTestCases(TestCaseNode.toList(scenarioNode.children()));
                    scenarioNode.getScenario().delete();
                    getTreeModel().removeNodeFromParent(scenarioNode);
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
            int option = JOptionPane.showConfirmDialog(null,
                    "<html><body><p style='width: 200px;'>"
                    + "Are you sure want to delete the following TestCases?<br>"
                    + testcaseNodes
                    + "</p></body></html>",
                    "Delete TestCase",
                    JOptionPane.YES_NO_OPTION);
            if (option == JOptionPane.YES_OPTION) {
                LOGGER.log(Level.INFO, "Delete TestCases approved for {0}; {1}",
                        new Object[]{testcaseNodes.size(), testcaseNodes});
                deleteTestCases(testcaseNodes);
            }
        }
    }

    /**
     * Deletes the specified test cases and resets table if needed.
     * @param testcaseNodes list of test case nodes to delete
     */
    private void deleteTestCases(List<TestCaseNode> testcaseNodes) {
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
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                tree.setSelectionPath(path);
                tree.scrollPathToVisible(path);
                tree.removeSelectionPath(path);
                tree.addSelectionPaths(new TreePath[]{path.getParentPath(), path});
            }
        });
    }

    /**
     * Moves selected test cases from Test Plan to Reusable Components.
     * Shows error notifications for failures and reloads both trees on success.
     */
    protected void makeAsReusableRTestCase() {
        if (!getSelectedTestCaseNodes().isEmpty()) {
            // Save ALL test cases to prevent data loss on reload
            getProject().save();
            
            boolean anySuccess = false;
            for (TestCaseNode testCaseNode : getSelectedTestCaseNodes()) {
                try {
                    getProject().moveTestCaseToReusable(testCaseNode.getTestCase());
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
            testDesign.getsMainFrame().getStepMap().convertScenarios(
                    Utils.saveDialog("Manual TestCase.csv"), getSelectedScenarios());
        } else if (!getSelectedTestCases().isEmpty()) {
            testDesign.getsMainFrame().getStepMap().convertTestCase(
                    Utils.saveDialog("Manual TestCase.csv"), getSelectedTestCases());
        } else {
            testDesign.getsMainFrame().getStepMap().convertScenarios(
                    Utils.saveDialog("Manual TestCase.csv"), getProject().getScenarios());
        }
    }

    /**
     * Sorts the children of the selected tree node.
     */
    private void sort() {
        if (tree.getSelectionPath() != null) {
            getTreeModel().sort(tree.getSelectionPath().getLastPathComponent());
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
     * Opens the tag editor for a test case data item.
     * @param tc test case data item
     */
    private void editTag(DataItem tc) {
        TagEditorDialog.build(testDesign.getsMainFrame(),
                getProject().getInfo().getAllTags(tc.getTags()), tc.getTags(),
                this::onRemoveTag, this::onAddTag)
                .withTitle(editTagTitle(tc.getName())).show(tc::setTags);

    }

    /**
     * Opens the tag editor for a scenario metadata.
     * @param scn scenario metadata
     */
    private void editTag(Meta scn) {
        TagEditorDialog.build(testDesign.getsMainFrame(),
                getProject().getInfo().getAllTags(scn.getTags()), scn.getTags(),
                this::onRemoveTag, this::onAddTag)
                .withTitle(editTagTitle(scn.getName())).show(scn::setTags);

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
            editTag(getProject().getInfo().getData()
                    .findOrCreate(tcn.getName(), tcn.getScenario().getName()));
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
            testDesign.getImpactUI().loadForTestCase(getProject()
                    .getImpactedTestCaseTestCases(scenarioName, testCaseName), scenarioName, testCaseName);
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
                    getTestDesign().getDefaultBrowser());
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
        protected JMenuItem addTestCase;
        protected JMenuItem renameTestCase;
        protected JMenuItem deleteTestCase;

        protected JMenuItem toggleReusable;

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
            add(addTestCase = create("Add TestCase", Keystroke.NEW));
            add(renameTestCase = create("Rename TestCase", Keystroke.RENAME));
            add(deleteTestCase = create("Delete TestCase", Keystroke.DELETE));

            addSeparator();
            JMenu menu = new JMenu("Export As");
            menu.setFont(UIManager.getFont("TableMenu.font"));
            menu.add(create("Manual Testcase", null));
            add(menu);
            add(toggleReusable = create("Make As Reusable/TestCase", null));
            toggleReusable.setText("Make As Reusable");
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
            toggleReusable.setEnabled(false);

            impactAnalysis.setEnabled(false);
            getCmdSyntax.setEnabled(false);

            copy.setEnabled(true);
            copy.setFont(UIManager.getFont("TableMenu.font"));
            cut.setEnabled(false);
            cut.setFont(UIManager.getFont("TableMenu.font"));
            paste.setEnabled(true);
            paste.setFont(UIManager.getFont("TableMenu.font"));

            sort.setEnabled(true);
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
            toggleReusable.setEnabled(true);

            impactAnalysis.setEnabled(true);

            getCmdSyntax.setEnabled(true);

            copy.setEnabled(true);
            copy.setFont(UIManager.getFont("TableMenu.font"));
            cut.setEnabled(true);
            cut.setFont(UIManager.getFont("TableMenu.font"));
            paste.setEnabled(true);
            paste.setFont(UIManager.getFont("TableMenu.font"));

            sort.setEnabled(false);
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
            toggleReusable.setEnabled(false);

            impactAnalysis.setEnabled(false);
            getCmdSyntax.setEnabled(false);

            copy.setEnabled(false);
            copy.setFont(UIManager.getFont("TableMenu.font"));
            cut.setEnabled(false);
            cut.setFont(UIManager.getFont("TableMenu.font"));
            paste.setEnabled(true);
            paste.setFont(UIManager.getFont("TableMenu.font"));

            sort.setEnabled(true);
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
                Font customFont = Font.createFont(Font.TRUETYPE_FONT, new File("resources/ui/resources/fonts/ingme_regular.ttf"));//.deriveFont(12f);
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

package com.ing.ide.main.mainui.components.testdesign.tree;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
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
 *
 *
 */
public class ProjectTree implements ActionListener {

    private static final Logger LOGGER = Logger.getLogger(ProjectTree.class.getName());

    ProjectPopupMenu popupMenu;

    private final ProjectProperties projectProperties;

    private final JTree tree;

    private final TestDesign testDesign;

    ProjectTreeModel treeModel = new TestPlanTreeModel();

    public ProjectTree(TestDesign testDesign) {
        this.testDesign = testDesign;
        tree = new JTree();
        projectProperties = new ProjectProperties(testDesign.getsMainFrame());
        init();
    }

    ProjectTreeModel getNewTreeModel() {
        return new TestPlanTreeModel();
    }

    ProjectPopupMenu getNewPopupMenu() {
        return new ProjectPopupMenu();
    }

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
                    setIcons(IconSettings.getIconSettings().getTestPlanTestCase());
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

    public void loadTableModelForSelection() {
        Object selected = getSelectedTestCase();
        if (selected == null) {
            selected = getSelectedScenario();
        }
        testDesign.loadTableModelForSelection(selected);
    }

    private void onRightClick() {
        TreePath path = tree.getSelectionPath();
        if (path != null) {
            togglePopupMenu(tree.getSelectionPath().getLastPathComponent());
        } else {
            popupMenu.setVisible(false);
        }
    }

    protected void togglePopupMenu(Object selected) {
        if (selected instanceof ScenarioNode) {
            popupMenu.forScenario();
        } else if (selected instanceof TestCaseNode) {
            popupMenu.forTestCase();
        } else if (selected instanceof GroupNode) {
            popupMenu.forTestPlan();
        }
    }

    protected void onNewAction() {
        if (getSelectedScenarioNode() != null) {
            addTestCase();
        } else if (getSelectedGroupNode() != null) {
            addScenario();
        }
    }

    protected void onDeleteAction() {
        deleteTestCases();
        deleteScenarios();
    }

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

    public ProjectTreeModel getTreeModel() {
        return treeModel;
    }

    private void addScenario() {
        ScenarioNode scNode = treeModel.addScenario(getSelectedGroupNode(),
                testDesign.getProject().addScenario(fetchNewScenarioName()));
        selectAndScrollTo(new TreePath(scNode.getPath()));
    }

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

    void renameScenario(Scenario scenario) {
        getTestDesign().getReusableTree()
                .getTreeModel().onScenarioRename(scenario);
    }

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

    private Scenario getSelectedScenario() {
        ScenarioNode scenarioNode = getSelectedScenarioNode();
        if (scenarioNode != null) {
            return scenarioNode.getScenario();
        }
        return null;
    }

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

    protected GroupNode getSelectedGroupNode() {
        List<GroupNode> groups = getSelectedGroupNodes();
        if (groups.isEmpty()) {
            return null;
        }
        return groups.get(0);
    }

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

    private ScenarioNode getSelectedScenarioNode() {
        List<ScenarioNode> scenarioNodes = getSelectedScenarioNodes();
        if (scenarioNodes.isEmpty()) {
            return null;
        }
        return scenarioNodes.get(0);
    }

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

    protected TestCase getSelectedTestCase() {
        TestCaseNode testcaseNode = getSelectedTestCaseNode();
        if (testcaseNode != null) {
            return testcaseNode.getTestCase();
        }
        return null;
    }

    private TestCaseNode getSelectedTestCaseNode() {
        List<TestCaseNode> tcNodes = getSelectedTestCaseNodes();
        if (tcNodes.isEmpty()) {
            return null;
        }
        return tcNodes.get(0);
    }

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

    private void makeAsReusableRTestCase() {
        if (!getSelectedTestCaseNodes().isEmpty()) {
            for (TestCaseNode testCaseNode : getSelectedTestCaseNodes()) {
                testCaseNode.getTestCase().toggleAsReusable();
                getTreeModel().removeNodeFromParent(testCaseNode);
                makeAsReusableRTestCase(testCaseNode.getTestCase());
            }
        }
    }

    void makeAsReusableRTestCase(TestCase testCase) {
        getTestDesign().getReusableTree().getTreeModel().addTestCase(testCase);
    }

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

    private void sort() {
        if (tree.getSelectionPath() != null) {
            getTreeModel().sort(tree.getSelectionPath().getLastPathComponent());
        }
    }

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

    private Tag onAddTag(String tag) {
        getProject().getInfo().addMeta(Meta.createTag(tag));
        return Tag.create(tag);
    }

    private void onRemoveTag(Tag tag) {
        getProject().getInfo().removeAll(tag);
    }

    private void editTag(DataItem tc) {
        TagEditorDialog.build(testDesign.getsMainFrame(),
                getProject().getInfo().getAllTags(tc.getTags()), tc.getTags(),
                this::onRemoveTag, this::onAddTag)
                .withTitle(editTagTitle(tc.getName())).show(tc::setTags);

    }

    private void editTag(Meta scn) {
        TagEditorDialog.build(testDesign.getsMainFrame(),
                getProject().getInfo().getAllTags(scn.getTags()), scn.getTags(),
                this::onRemoveTag, this::onAddTag)
                .withTitle(editTagTitle(scn.getName())).show(scn::setTags);

    }

    private String editTagTitle(String t) {
        return String.format("Edit Tag: %s", t);
    }

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

    private void editTag(List<TreePath> paths) {
        paths.stream().forEach(this::editTag);
    }

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

    private String getBatRCommand() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("windows")) {
            return "Run.bat";
        }
        return "Run.command";
    }

    private void showDetails() {
        TreePath path = tree.getSelectionPath();
        if (path != null) {
            showProjDetails();
        }
    }

    private void showProjDetails() {
        projectProperties.loadForCurrentProject();
//        projectProperties.pack();
        projectProperties.setLocationRelativeTo(null);
        projectProperties.setVisible(true);
    }

    public final JTree getTree() {
        return tree;
    }

    public final Project getProject() {
        return testDesign.getProject();
    }

    public final TestDesign getTestDesign() {
        return testDesign;
    }

    public final void load() {
        treeModel.setProject(testDesign.getProject());
        treeModel.reload();
        getTree().setSelectionPath(new TreePath(treeModel.getFirstNode().getPath()));
        loadTableModelForSelection();
    }

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

        public ProjectPopupMenu() {
            init();
        }

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

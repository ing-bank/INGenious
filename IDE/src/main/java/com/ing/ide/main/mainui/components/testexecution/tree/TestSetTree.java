
package com.ing.ide.main.mainui.components.testexecution.tree;

import com.ing.datalib.component.Release;
import com.ing.datalib.component.TestSet;
import com.ing.ide.main.mainui.components.testexecution.TestExecution;
import com.ing.ide.main.mainui.components.testexecution.tree.model.ReleaseNode;
import com.ing.ide.main.mainui.components.testexecution.tree.model.TestLabModel;
import com.ing.ide.main.mainui.components.testexecution.tree.model.TestSetNode;
import com.ing.ide.main.utils.Utils;
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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

/**
 *
 *
 */
public class TestSetTree implements ActionListener {

    private static final Logger LOGGER = Logger.getLogger(TestSetTree.class.getName());

    private final TestSetPopupMenu popupMenu;
    private final JTree tree;

    private final TestExecution testExecution;

    private final TestLabModel treeModel;

    public TestSetTree(TestExecution testExecution) {
        this.testExecution = testExecution;
        this.popupMenu = new TestSetPopupMenu();
        tree = new JTree();
        treeModel = new TestLabModel();
        init();
    }

    private void init() {
        tree.setModel(treeModel);
        tree.setEditable(true);
        tree.setComponentPopupMenu(popupMenu);
        tree.setDragEnabled(true);
        tree.setInvokesStopCellEditing(true);
        tree.setToggleClickCount(0);
        tree.getInputMap(JComponent.WHEN_FOCUSED).put(Keystroke.NEW, "New");
        tree.getInputMap(JComponent.WHEN_FOCUSED).put(Keystroke.DELETE, "Delete");
        tree.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ESCAPE"), "Escape");

        tree.getActionMap().put("New", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                if (getSelectedRelease() != null) {
                    addTestSet();
                } else {
                    addRelease();
                }
            }
        });
        tree.getActionMap().put("Delete", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                deleteTestSets();
                deleteReleases();
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
                onRightClick(null);
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent pme) {
                //
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent pme) {
                //
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
//                checkAndRename();
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
            // e.printStackTrace();
        }
        tree.setFont(new Font("ING Me", Font.PLAIN, 11));
        new TreeSelectionRenderer(tree) {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean isLeaf, int row, boolean focused) {
                Component c = super.getTreeCellRendererComponent(tree, value, selected, expanded, isLeaf, row, focused);
                if (value instanceof ReleaseNode) {
                    setIcons(IconSettings.getIconSettings().getTestLabRelease());
                } else if (value instanceof TestSetNode) {
                    setIcons(IconSettings.getIconSettings().getTestLabTestSet());
                } else {
                    setIcons(IconSettings.getIconSettings().getTestLabRoot());
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
        Object testSet = getSelectedTestSet();
        if (testSet != null) {
            testExecution.getTestSetComp().loadTableModelForSelection(testSet);
        } else {
            testExecution.getTestSetComp().resetTable();
        }
    }

    private void onRightClick(MouseEvent e) {
        TreePath path = tree.getSelectionPath();
        if (path != null) {
            togglePopupMenu(tree.getSelectionPath().getLastPathComponent());
        } else {
            popupMenu.setVisible(false);
        }
    }

    private void togglePopupMenu(Object selected) {
        if (selected instanceof ReleaseNode) {
            popupMenu.forRelease();
        } else if (selected instanceof TestSetNode) {
            popupMenu.forTestSet();
        } else {
            popupMenu.forRoot();
        }
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        switch (ae.getActionCommand()) {
            case "Add Release":
                addRelease();
                break;
            case "Rename Release":
                tree.startEditingAtPath(new TreePath(getSelectedReleaseNode().getPath()));
                break;
            case "Delete Release":
                deleteReleases();
                break;
            case "Add TestSet":
                addTestSet();
                break;
            case "Rename TestSet":
                tree.startEditingAtPath(new TreePath(getSelectedTestSetNode().getPath()));
                break;
            case "Delete TestSet":
                deleteTestSets();
                break;
            case "Cut":
                break;
            case "Copy":
                break;
            case "Paste":
                break;
            case "Sort":
                sort();
                break;
            case "Get CmdLine Syntax":
                getCmdLineSyntax();
                break;
            default:
                throw new UnsupportedOperationException();
        }
    }

    private void addRelease() {
        ReleaseNode releaseNode = treeModel.addRelease(
                testExecution.getProject().addRelease(fetchNewReleaseName()));
        selectAndScrollTo(new TreePath(releaseNode.getPath()));
    }

    private String fetchNewReleaseName() {
        String newReleaseName = "NewRelease";
        for (int i = 0;; i++) {
            if (testExecution.getProject().getReleaseByName(newReleaseName) == null) {
                break;
            }
            newReleaseName = "NewRelease" + i;
        }
        return newReleaseName;
    }

    private void addTestSet() {
        ReleaseNode releaseNode = getSelectedReleaseNode();
        if (releaseNode != null) {
            String testSetName = fetchNewTestSetName(releaseNode.getRelease());
            TestSet testset = releaseNode.getRelease().addTestSet(testSetName);
            testExecution.getTestSetComp().loadTableModelForSelection(testset);
            selectAndScrollTo(new TreePath(treeModel.
                    addTestSet(releaseNode, testset).getPath()));
        }
    }

    private void selectAndScrollTo(final TreePath path) {
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

    private String fetchNewTestSetName(Release release) {
        String newTestSetName = "NewTestSet";
        for (int i = 0;; i++) {
            if (release.getTestSetByName(newTestSetName) == null) {
                break;
            }
            newTestSetName = "NewTestSet" + i;
        }
        return newTestSetName;
    }

    private Boolean checkAndRename() {
        String name = tree.getCellEditor().getCellEditorValue().toString().trim();
        if (Validator.isValidName(name)) {
            ReleaseNode releaseNode = getSelectedReleaseNode();
            if (releaseNode != null && !releaseNode.toString().equals(name)) {
                if (releaseNode.getRelease().rename(name)) {
                    treeModel.reload(releaseNode);
                    testExecution.getTestSetComp().refreshTitle();
                    return true;
                } else {
                    Notification.show("Release " + name + " Already present");
                    return false;
                }
            }
            TestSetNode testSetNode = getSelectedTestSetNode();
            if (testSetNode != null && !testSetNode.toString().equals(name)) {
                if (testSetNode.getTestSet().rename(name)) {
                    treeModel.reload(testSetNode);
                    testExecution.getTestSetComp().refreshTitle();
                    return true;
                } else {
                    Notification.show("TestSet '" + name + "' Already present in Release - " + getSelectedTestSet().getRelease().getName());
                }
            }

        }
        return false;
    }

    private void deleteReleases() {
        List<ReleaseNode> releases = getSelectedReleaseNodes();
        if (!releases.isEmpty()) {
            int option = JOptionPane.showConfirmDialog(null,
                    "<html><body><p style='width: 200px;'>"
                            + "Are you sure want to delete the following Releases?<br>"
                            + releases
                            + "</p></body></html>",
                    "Delete Release",
                    JOptionPane.YES_NO_OPTION);
            if (option == JOptionPane.YES_OPTION) {
                LOGGER.log(Level.INFO, "Delete Releases approved for {0}; {1}",
                        new Object[]{releases.size(), releases});
                for (ReleaseNode releaseNode : releases) {
                    deleteTestSets(Collections.list(releaseNode.children()));
                    releaseNode.getRelease().delete();
                    treeModel.removeNodeFromParent(releaseNode);
                }
            }
        }
    }

    private void deleteTestSets() {
        List<TestSetNode> testsets = getSelectedTestSetNodes();
        if (!testsets.isEmpty()) {
            int option = JOptionPane.showConfirmDialog(null,
                    "<html><body><p style='width: 200px;'>"
                            + "Are you sure want to delete the following TestSets?<br>"
                            + testsets
                            + "</p></body></html>",
                    "Delete TestSet",
                    JOptionPane.YES_NO_OPTION);
            if (option == JOptionPane.YES_OPTION) {
                LOGGER.log(Level.INFO, "Delete TestSets approved for {0}; {1}",
                        new Object[]{testsets.size(), testsets});
                deleteTestSets(testsets.stream().map(tsNode -> (TreeNode) tsNode).collect(Collectors.toList()));
            }
        }
    }

    private void deleteTestSets(List<TreeNode> testsets) {
        TestSet loadedTestSet = testExecution.getTestSetComp().getCurrentTestSet();
        Boolean shouldRemove = false;
        for (TreeNode testsetNode : testsets) {
            if (!shouldRemove) {
                shouldRemove = Objects.equals(loadedTestSet,(((TestSetNode)testsetNode).getTestSet()));
            }
            ((TestSetNode)testsetNode).getTestSet().delete();
            treeModel.removeNodeFromParent((TestSetNode)testsetNode);
        }
        if (shouldRemove) {
            testExecution.getTestSetComp().resetTable();
        }
    }

    private Release getSelectedRelease() {
        ReleaseNode releaseNode = getSelectedReleaseNode();
        if (releaseNode != null) {
            return releaseNode.getRelease();
        }
        return null;
    }

    private ReleaseNode getSelectedReleaseNode() {
        List<ReleaseNode> releaseNodes = getSelectedReleaseNodes();
        if (releaseNodes.isEmpty()) {
            return null;
        }
        return releaseNodes.get(0);
    }

    protected List<ReleaseNode> getSelectedReleaseNodes() {
        List<ReleaseNode> scenarioNodes = new ArrayList<>();
        TreePath[] paths = tree.getSelectionPaths();
        if (paths != null && paths.length > 0) {
            for (TreePath path : paths) {
                if (path.getLastPathComponent() instanceof ReleaseNode) {
                    scenarioNodes.add((ReleaseNode) path.getLastPathComponent());
                }
            }
        }
        return scenarioNodes;
    }

    protected TestSet getSelectedTestSet() {
        TestSetNode testSetNode = getSelectedTestSetNode();
        if (testSetNode != null) {
            return testSetNode.getTestSet();
        }
        return null;
    }

    private TestSetNode getSelectedTestSetNode() {
        List<TestSetNode> tcNodes = getSelectedTestSetNodes();
        if (tcNodes.isEmpty()) {
            return null;
        }
        return tcNodes.get(0);
    }

    protected List<TestSetNode> getSelectedTestSetNodes() {
        List<TestSetNode> tcNodes = new ArrayList<>();
        TreePath[] paths = tree.getSelectionPaths();
        if (paths != null && paths.length > 0) {
            for (TreePath path : paths) {
                if (path.getLastPathComponent() instanceof TestSetNode) {
                    tcNodes.add((TestSetNode) path.getLastPathComponent());
                }
            }
        }
        return tcNodes;
    }

    private void sort() {
        if (tree.getSelectionPath() != null) {
            treeModel.sort(tree.getSelectionPath().getLastPathComponent());
        }
    }

    private void getCmdLineSyntax() {
        TestSet testSet = getSelectedTestSet();
        if (testSet != null) {
            String releaseName = testSet.getRelease().getName();
            String testSetName = testSet.getName();
            String syntax = String.format(
                    "%s -run -project_location \"%s\" -release \"%s\" -testset \"%s\"",
                    getBatRCommand(),
                    testSet.getProject().getLocation(),
                    releaseName,
                    testSetName);
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

    private void getAzDoYaml() {
        TestSet testSet = getSelectedTestSet();
        if (testSet != null) {
            String releaseName = testSet.getRelease().getName();
            String testSetName = testSet.getName();
            String yaml = generateYAML(testSet.getProject().getName(),releaseName,testSetName);
            Utils.copyTextToClipboard(yaml);
            Notification.show("Yaml Content has been copied to Clipboard");
        } else {
            Notification.show("Select a Valid TestCase");
        }
    }

    public JTree getTree() {
        return tree;
    }

    public TestLabModel getModel() {
        return treeModel;
    }

    public void load() {
        treeModel.setProject(testExecution.getProject());
        treeModel.reload();
        getTree().setSelectionPath(new TreePath(treeModel.getFirstNode().getPath()));
        loadTableModelForSelection();
    }

    private String generateYAML(String Project,String Release,String TestSet) {
        StringBuilder yamlBuilder = new StringBuilder();
        return yamlBuilder.toString();
    }

    class TestSetPopupMenu extends JPopupMenu {

        private JMenuItem addRelease;
        private JMenuItem renameRelease;
        private JMenuItem deleteRelease;
        private JMenuItem addTestSet;
        private JMenuItem renameTestSet;
        private JMenuItem deleteTestSet;

        private JMenuItem sort;
        protected JMenuItem getCmdSyntax;

        public TestSetPopupMenu() {
            init();
        }

        private void init() {
            add(addRelease = create("Add Release", Keystroke.NEW));
            add(renameRelease = create("Rename Release", Keystroke.RENAME));
            add(deleteRelease = create("Delete Release", Keystroke.DELETE));
            addSeparator();
            add(addTestSet = create("Add TestSet", Keystroke.NEW));
            add(renameTestSet = create("Rename TestSet", Keystroke.RENAME));
            add(deleteTestSet = create("Delete TestSet", Keystroke.DELETE));
            addSeparator();
            add(getCmdSyntax = create("Get CmdLine Syntax", null));
            addSeparator();
            add(sort = create("Sort", null));
            sort.setIcon(Canvas.EmptyIcon);
        }

        private void forRelease() {
            renameRelease.setEnabled(true);
            deleteRelease.setEnabled(true);
            addTestSet.setEnabled(true);

            addRelease.setEnabled(false);
            renameTestSet.setEnabled(false);
            deleteTestSet.setEnabled(false);

            getCmdSyntax.setEnabled(false);
            sort.setEnabled(true);
        }

        private void forTestSet() {
            addRelease.setEnabled(false);
            renameRelease.setEnabled(false);
            deleteRelease.setEnabled(false);

            addTestSet.setEnabled(false);

            renameTestSet.setEnabled(true);
            deleteTestSet.setEnabled(true);

            getCmdSyntax.setEnabled(true);
            sort.setEnabled(false);
        }

        private void forRoot() {
            addRelease.setEnabled(true);

            renameRelease.setEnabled(false);
            deleteRelease.setEnabled(false);

            addTestSet.setEnabled(false);
            renameTestSet.setEnabled(false);
            deleteTestSet.setEnabled(false);

            getCmdSyntax.setEnabled(false);
            sort.setEnabled(true);
        }

        private JMenuItem create(String name, KeyStroke keyStroke) {
            try {
                //create the font to use. Specify the size!
                Font customFont = Font.createFont(Font.TRUETYPE_FONT, new File("resources/ui/resources/fonts/ingme_regular.ttf"));//.deriveFont(12f);
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                //register the font
                ge.registerFont(customFont);
            } catch (IOException | FontFormatException e) {
                //   e.printStackTrace();
            }
            JMenuItem menuItem = new JMenuItem(name);
            menuItem.setActionCommand(name);
            menuItem.setAccelerator(keyStroke);
            menuItem.addActionListener(TestSetTree.this);
            menuItem.setFont(new Font("ING Me", Font.PLAIN, 11));
            return menuItem;
        }



    }
}

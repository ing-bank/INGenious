package com.ing.ide.main.mainui.components.testdesign.or.structureddata;

import com.ing.datalib.component.Project;
import com.ing.datalib.or.common.ObjectGroup;
import com.ing.datalib.or.structureddata.StructuredDataORObject;
import com.ing.ide.main.mainui.components.testdesign.TestDesign;
import com.ing.ide.main.utils.tree.TreeSearch;
import java.awt.BorderLayout;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.tree.TreePath;

/**
 * Panel for Structured Data Object Repository.
 * Contains tree view and property table for API objects.
 */
public class StructuredDataORPanel extends JPanel {
    private final StructuredDataObjectTree projectTree;
    private final StructuredDataObjectTree sharedTree;
    private final StructuredDataORTable objectTable;
    private final TestDesign testDesign;

    private JSplitPane splitPane;
    private JTabbedPane tabs;

    public StructuredDataORPanel(TestDesign testDesign) {
        this.testDesign = testDesign;
        this.projectTree =
            new StructuredDataObjectTree(this, StructuredDataObjectTree.ORSource.PROJECT);
        this.sharedTree =
            new StructuredDataObjectTree(this, StructuredDataObjectTree.ORSource.SHARED);
        this.objectTable = new StructuredDataORTable(this);
        init();
    }

    private void init() {
        setLayout(new BorderLayout());

        tabs = new JTabbedPane();

        JComponent projectTreeWithSearch = TreeSearch.installForOR(projectTree.getTree());
        tabs.addTab("Project", projectTreeWithSearch);

        JComponent sharedTreeWithSearch = TreeSearch.installForOR(sharedTree.getTree());
        tabs.addTab("Shared", sharedTreeWithSearch);

        tabs.addChangeListener(
            new ChangeListener() {

                @Override
                public void stateChanged(ChangeEvent e) {
                    updateTableForCurrentSelection();
                }
            }
        );

        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setOneTouchExpandable(true);
        splitPane.setTopComponent(tabs);
        splitPane.setBottomComponent(objectTable);
        splitPane.setResizeWeight(0.5);

        add(splitPane, BorderLayout.CENTER);

        javax.swing.SwingUtilities.invokeLater(
            () -> {
                splitPane.setDividerLocation(0.5);
            }
        );

        hookSelectionToTable(projectTree);
        hookSelectionToTable(sharedTree);
    }

    private void hookSelectionToTable(StructuredDataObjectTree tree) {
        tree
            .getTree()
            .addTreeSelectionListener(
                e -> {
                    if (isTreeOnCurrentTab(tree)) {
                        loadTableModelForSelection(getSelectedNodeUserObject(tree));
                    }
                }
            );
    }

    private boolean isTreeOnCurrentTab(StructuredDataObjectTree tree) {
        int idx = tabs.getSelectedIndex();
        String title = (idx >= 0) ? tabs.getTitleAt(idx) : "";
        return (
            (tree == projectTree && "Project".equals(title)) ||
            (tree == sharedTree && "Shared".equals(title))
        );
    }

    private Object getSelectedNodeUserObject(StructuredDataObjectTree tree) {
        TreePath path = tree.getTree().getSelectionPath();
        if (path == null) return null;

        Object node = path.getLastPathComponent();
        if (node instanceof javax.swing.tree.DefaultMutableTreeNode) {
            return ((javax.swing.tree.DefaultMutableTreeNode) node).getUserObject();
        }
        return node;
    }

    private void updateTableForCurrentSelection() {
        StructuredDataObjectTree activeTree = getActiveTree();
        Object selected = (activeTree != null) ? getSelectedNodeUserObject(activeTree) : null;
        loadTableModelForSelection(selected);
    }

    public StructuredDataObjectTree getActiveTree() {
        int idx = tabs.getSelectedIndex();
        if (idx == 0) return projectTree;
        if (idx == 1) return sharedTree;
        return null;
    }

    void loadTableModelForSelection(Object object) {
        if (object instanceof StructuredDataORObject) {
            objectTable.loadObject((StructuredDataORObject) object);
        } else if (object instanceof ObjectGroup) {
            objectTable.loadObject((StructuredDataORObject) ((ObjectGroup) object).getChildAt(0));
        } else {
            objectTable.reset();
        }
    }

    public TestDesign getTestDesign() {
        return testDesign;
    }

    public Project getProject() {
        return testDesign.getProject();
    }

    public void load() {
        objectTable.reset();
        sharedTree.load();
        projectTree.load();
    }

    public void adjustUI() {
        int height = splitPane.getHeight();
        if (height > 0) {
            splitPane.setDividerLocation(height / 2);
        } else {
            splitPane.setDividerLocation(0.5);
        }
    }

    public Boolean navigateToObject(String objectName, String pageName) {
        StructuredDataObjectTree active = getActiveTree();
        if (
            active != null && Boolean.TRUE.equals(active.navigateToObject(objectName, pageName))
        ) return true;

        StructuredDataObjectTree other = (active == projectTree) ? sharedTree : projectTree;
        return (other != null) ? other.navigateToObject(objectName, pageName) : false;
    }

    public StructuredDataORTable getObjectTable() {
        return objectTable;
    }

    public StructuredDataObjectTree getProjectTree() {
        return projectTree;
    }

    public StructuredDataObjectTree getSharedTree() {
        return sharedTree;
    }

    public List<com.ing.datalib.or.common.ORObjectInf> getSelectedObjectsFromActiveTab() {
        StructuredDataObjectTree active = getActiveTree();
        return (active != null) ? active.getSelectedObjects() : java.util.Collections.emptyList();
    }
}

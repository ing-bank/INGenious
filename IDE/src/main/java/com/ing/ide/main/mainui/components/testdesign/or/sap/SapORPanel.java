
package com.ing.ide.main.mainui.components.testdesign.or.sap;

import com.ing.datalib.component.Project;
import com.ing.datalib.or.common.ObjectGroup;
import com.ing.datalib.or.sap.SapORObject;
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
 * Main panel for the SAP Object Repository (OR) UI, containing:
 * <ul>
 *   <li>Project and Shared OR trees (with search support)</li>
 *   <li>A properties table for displaying and modifying object attributes</li>
 * </ul>
 * <p>
 * This panel manages tree–table interaction, updates the table based on the
 * active tab, and provides navigation utilities for locating specific OR objects.
 * It serves as the central coordinator for loading, displaying, and interacting
 * with SAP OR data in Test Design.
 */
public class SapORPanel extends JPanel {

    private final SapObjectTree projectTree;
    private final SapObjectTree sharedTree;
    private final SapORTable objectTable;
    private final TestDesign testDesign;

    private JSplitPane splitPane;
    private JTabbedPane tabs;

    public SapORPanel(TestDesign testDesign) {
        this.testDesign = testDesign;
        this.projectTree = new SapObjectTree(this, SapObjectTree.ORSource.PROJECT);
        this.sharedTree  = new SapObjectTree(this, SapObjectTree.ORSource.SHARED);
        this.objectTable = new SapORTable(this);
        init();
    }

    private void init() {
        setLayout(new BorderLayout());

        tabs = new JTabbedPane();

        JComponent projectTreeWithSearch = TreeSearch.installForOR(projectTree.getTree());
        tabs.addTab("Project", projectTreeWithSearch);

        JComponent sharedTreeWithSearch = TreeSearch.installForOR(sharedTree.getTree());
        tabs.addTab("Shared", sharedTreeWithSearch);

        tabs.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                updateTableForCurrentSelection();
            }
        });

        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setOneTouchExpandable(true);
        splitPane.setTopComponent(tabs);
        splitPane.setBottomComponent(objectTable);
        splitPane.setResizeWeight(0.5);

        add(splitPane, BorderLayout.CENTER);
        
        javax.swing.SwingUtilities.invokeLater(() -> {
            splitPane.setDividerLocation(0.5);
        });

        hookSelectionToTable(projectTree);
        hookSelectionToTable(sharedTree);
    }

    private void hookSelectionToTable(SapObjectTree tree) {
        tree.getTree().addTreeSelectionListener(e -> {
            if (isTreeOnCurrentTab(tree)) {
                loadTableModelForSelection(getSelectedNodeUserObject(tree));
            }
        });
    }

    private boolean isTreeOnCurrentTab(SapObjectTree tree) {
        int idx = tabs.getSelectedIndex();
        String title = (idx >= 0) ? tabs.getTitleAt(idx) : "";
        return (tree == projectTree && "Project".equals(title))
            || (tree == sharedTree  && "Shared".equals(title));
    }

    private Object getSelectedNodeUserObject(SapObjectTree tree) {
        TreePath path = tree.getTree().getSelectionPath();
        if (path == null) return null;

        Object node = path.getLastPathComponent();
        if (node instanceof javax.swing.tree.DefaultMutableTreeNode) {
            return ((javax.swing.tree.DefaultMutableTreeNode) node).getUserObject();
        }
        return node;
    }

    private void updateTableForCurrentSelection() {
        SapObjectTree activeTree = getActiveTree();
        Object selected = (activeTree != null) ? getSelectedNodeUserObject(activeTree) : null;
        loadTableModelForSelection(selected);
    }

    public SapObjectTree getActiveTree() {
        int idx = tabs.getSelectedIndex();
        if (idx == 0) return projectTree;
        if (idx == 1) return sharedTree;
        return null;
    }

    void loadTableModelForSelection(Object object) {
        if (object instanceof SapORObject) {
            objectTable.loadObject((SapORObject) object);
        } else if (object instanceof ObjectGroup) {
            objectTable.loadObject((SapORObject) ((ObjectGroup) object).getChildAt(0));
        } else {
            objectTable.reset();
        }
    }

    public SapObjectTree getObjectTree() {
        return getActiveTree();
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
        splitPane.setDividerLocation(0.5);
    }

    public Boolean navigateToObject(String objectName, String pageName) {
        SapObjectTree active = getActiveTree();
        if (active != null && Boolean.TRUE.equals(active.navigateToObject(objectName, pageName))) return true;

        SapObjectTree other = (active == projectTree) ? sharedTree : projectTree;
        return (other != null) ? other.navigateToObject(objectName, pageName) : false;
    }

    public SapORTable getObjectTable() {
        return objectTable;
    }

    public SapObjectTree getProjectTree() {
        return projectTree;
    }

    public SapObjectTree getSharedTree() {
        return sharedTree;
    }

    public List<com.ing.datalib.or.common.ORObjectInf> getSelectedObjectsFromActiveTab() {
        SapObjectTree active = getActiveTree();
        return (active != null) ? active.getSelectedObjects()
                                : java.util.Collections.emptyList();
    }
}

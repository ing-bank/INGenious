package com.ing.ide.main.mainui.components.testdesign.or.mobile;

import com.ing.datalib.component.Project;
import com.ing.datalib.or.common.ObjectGroup;
import com.ing.datalib.or.mobile.MobileORObject;
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
 * Main panel for the Mobile Object Repository (OR) UI, containing:
 * <ul>
 *   <li>Project and Shared OR trees (with search support)</li>
 *   <li>A properties table for displaying and modifying object attributes</li>
 * </ul>
 * <p>
 * This panel manages tree–table interaction, updates the table based on the
 * active tab, and provides navigation utilities for locating specific OR objects.
 * It serves as the central coordinator for loading, displaying, and interacting
 * with mobile OR data in Test Design.
 */

public class MobileORPanel extends JPanel {

    private final MobileObjectTree projectTree;
    private final MobileObjectTree sharedTree;
    private final MobileORTable objectTable;
    private final TestDesign testDesign;

    private JSplitPane splitPane;
    private JTabbedPane tabs;

    public MobileORPanel(TestDesign testDesign) {
        this.testDesign = testDesign;
        this.projectTree = new MobileObjectTree(this, MobileObjectTree.ORSource.PROJECT);
        this.sharedTree  = new MobileObjectTree(this, MobileObjectTree.ORSource.SHARED);
        this.objectTable = new MobileORTable(this);
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
        //splitPane.setDividerLocation(0.5);

        add(splitPane, BorderLayout.CENTER);
        
        javax.swing.SwingUtilities.invokeLater(() -> {
            splitPane.setDividerLocation(0.5);
        });


        hookSelectionToTable(projectTree);
        hookSelectionToTable(sharedTree);
    }

    private void hookSelectionToTable(MobileObjectTree tree) {
        tree.getTree().addTreeSelectionListener(e -> {
            if (isTreeOnCurrentTab(tree)) {
                loadTableModelForSelection(getSelectedNodeUserObject(tree));
            }
        });
    }

    private boolean isTreeOnCurrentTab(MobileObjectTree tree) {
        int idx = tabs.getSelectedIndex();
        String title = (idx >= 0) ? tabs.getTitleAt(idx) : "";
        return (tree == projectTree && "Project".equals(title))
            || (tree == sharedTree  && "Shared".equals(title));
    }

    private Object getSelectedNodeUserObject(MobileObjectTree tree) {
        TreePath path = tree.getTree().getSelectionPath();
        if (path == null) return null;

        Object node = path.getLastPathComponent();
        if (node instanceof javax.swing.tree.DefaultMutableTreeNode) {
            return ((javax.swing.tree.DefaultMutableTreeNode) node).getUserObject();
        }
        return node;
    }

    private void updateTableForCurrentSelection() {
        MobileObjectTree activeTree = getActiveTree();
        Object selected = (activeTree != null) ? getSelectedNodeUserObject(activeTree) : null;
        loadTableModelForSelection(selected);
    }

    public MobileObjectTree getActiveTree() {
        int idx = tabs.getSelectedIndex();
        if (idx == 0) return projectTree;
        if (idx == 1) return sharedTree;
        return null;
    }

    void loadTableModelForSelection(Object object) {
        if (object instanceof MobileORObject) {
            objectTable.loadObject((MobileORObject) object);
        } else if (object instanceof ObjectGroup) {
            objectTable.loadObject((MobileORObject) ((ObjectGroup) object).getChildAt(0));
        } else {
            objectTable.reset();
        }
    }

    public TestDesign getTestDesign() { return testDesign; }
    public Project getProject() { return testDesign.getProject(); }

    public void load() {
        objectTable.reset();
        sharedTree.load();
        projectTree.load();
        //splitPane.setDividerLocation(0.5);
    }

    public void adjustUI() {
        //splitPane.setDividerLocation(0.5);
    }

    public Boolean navigateToObject(String objectName, String pageName) {
        MobileObjectTree active = getActiveTree();
        if (active != null && Boolean.TRUE.equals(active.navigateToObject(objectName, pageName))) return true;

        MobileObjectTree other = (active == projectTree) ? sharedTree : projectTree;
        return (other != null) ? other.navigateToObject(objectName, pageName) : false;
    }

    public MobileORTable getObjectTable() { 
        return objectTable; 
    }

    public MobileObjectTree getProjectTree() { 
        return projectTree; 
    }
    
    public MobileObjectTree getSharedTree() { 
        return sharedTree; 
    }
    
    public List<com.ing.datalib.or.common.ORObjectInf> getSelectedObjectsFromActiveTab() {
        MobileObjectTree active = getActiveTree();
        return (active != null) ? active.getSelectedObjects()
                                : java.util.Collections.emptyList();
    }
}
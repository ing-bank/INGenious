
package com.ing.ide.main.mainui.components.testdesign.or.web;

import com.ing.datalib.component.Project;
import com.ing.datalib.or.common.ORObjectInf;
import com.ing.datalib.or.common.ObjectGroup;
import com.ing.datalib.or.web.WebORObject;
import com.ing.ide.main.mainui.components.testdesign.TestDesign;
import com.ing.ide.main.mainui.components.testdesign.or.web.WebObjectTree.ORSource;
import com.ing.ide.main.utils.tree.TreeSearch;
import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.tree.TreePath;

/**
 *
 *
 */
public class WebORPanel extends JPanel {
    private final WebObjectTree projectTree;
    private final WebObjectTree sharedTree;
    private final WebORTable objectTable;
    private final TestDesign testDesign;
    private JSplitPane splitPane;
    private JTabbedPane tabs;

    public WebORPanel(TestDesign testDesign) {
        this.testDesign = testDesign;
        this.projectTree = new WebObjectTree(this, ORSource.PROJECT);
        this.sharedTree  = new WebObjectTree(this, ORSource.SHARED);
        this.objectTable = new WebORTable(this);
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
        splitPane.setDividerLocation(0.5);
        add(splitPane, BorderLayout.CENTER);

        hookSelectionToTable(projectTree);
        hookSelectionToTable(sharedTree);
    }

    private void hookSelectionToTable(WebObjectTree tree) {
        tree.getTree().addTreeSelectionListener(e -> {
            if (isTreeOnCurrentTab(tree)) {
                loadTableModelForSelection(getSelectedNodeUserObject(tree));
            }
        });
    }

    private boolean isTreeOnCurrentTab(WebObjectTree tree) {
        int idx = tabs.getSelectedIndex();
        String title = (idx >= 0) ? tabs.getTitleAt(idx) : "";
        return (tree == projectTree && "Project".equals(title))
            || (tree == sharedTree  && "Shared".equals(title));
    }

    private Object getSelectedNodeUserObject(WebObjectTree tree) {
        TreePath path = tree.getTree().getSelectionPath();
        if (path == null) return null;
        Object node = path.getLastPathComponent();
        if (node instanceof javax.swing.tree.DefaultMutableTreeNode) {
            return ((javax.swing.tree.DefaultMutableTreeNode) node).getUserObject();
        }
        return node;
    }

    private void updateTableForCurrentSelection() {
        WebObjectTree activeTree = getActiveTree();
        Object selected = (activeTree != null) ? getSelectedNodeUserObject(activeTree) : null;
        loadTableModelForSelection(selected);
    }

    public WebObjectTree getActiveTree() {
        int idx = tabs.getSelectedIndex();
        if (idx == 0) {
            return projectTree;
        } else if (idx == 1) {
            return sharedTree;
        }
        return null;
    }

    void loadTableModelForSelection(Object object) {
        if (object instanceof WebORObject) {
            objectTable.loadObject((WebORObject) object);
        } else if (object instanceof ObjectGroup) {
            objectTable.loadObject((WebORObject) ((ObjectGroup) object).getChildAt(0));
        } else {
            objectTable.reset();
        }
    }

    void changeFrameData(String frameText) {
        WebObjectTree activeTree = getActiveTree();
        if (activeTree != null) {
            activeTree.changeFrameData(frameText);
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
        splitPane.setDividerLocation(0.5);
    }

    public void adjustUI() {
        splitPane.setDividerLocation(0.5);
    }

    public Boolean navigateToObject(String objectName, String pageName) {
        WebObjectTree active = getActiveTree();
        if (active != null && Boolean.TRUE.equals(active.navigateToObject(objectName, pageName))) {
            return true;
        }
        WebObjectTree other = (active == projectTree) ? sharedTree : projectTree;
        return (other != null) ? other.navigateToObject(objectName, pageName) : false;
    }

    public WebObjectTree getProjectTree() {
        return projectTree;
    }

    public WebObjectTree getSharedTree() {
        return sharedTree;
    }

    public WebORTable getObjectTable() {
        return objectTable;
    }

    public List<com.ing.datalib.or.common.ORObjectInf> getSelectedObjectsFromActiveTab() {
        WebObjectTree active = getActiveTree();
        return (active != null) ? active.getSelectedObjects()
                                : java.util.Collections.emptyList();
    }
}
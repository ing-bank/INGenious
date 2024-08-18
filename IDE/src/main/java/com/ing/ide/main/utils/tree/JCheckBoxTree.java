
package com.ing.ide.main.utils.tree;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.event.EventListenerList;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

public abstract class JCheckBoxTree extends JTree {

    private static final long serialVersionUID = -4194122328392241790L;

    JCheckBoxTree selfPointer = this;

    // Defining data structure that will enable to fast check-indicate the state of each node
    // It totally replaces the "selection" mechanism of the JTree
    private class CheckedNode {

        boolean isSelected;
        boolean hasChildren;
        boolean allChildrenSelected;

        public CheckedNode(boolean isSelected_, boolean hasChildren_, boolean allChildrenSelected_) {
            isSelected = isSelected_;
            hasChildren = hasChildren_;
            allChildrenSelected = allChildrenSelected_;
        }
    }
    HashMap<TreePath, CheckedNode> nodesCheckingState;
    List<TreePath> checkedPaths = new ArrayList<TreePath>() {
        @Override
        public boolean add(TreePath e) {
            if (!contains(e)) {
                return super.add(e);
            }
            return false;
        }

    };

    // Defining a new event type for the checking mechanism and preparing event-handling mechanism
    protected EventListenerList listenerList = new EventListenerList();

    public class CheckChangeEvent extends EventObject {

        private static final long serialVersionUID = -8100230309044193368L;

        public CheckChangeEvent(Object source) {
            super(source);
        }
    }

    public interface CheckChangeEventListener extends EventListener {

        public void checkStateChanged(CheckChangeEvent event);
    }

    public final void addCheckChangeEventListener(CheckChangeEventListener listener) {
        listenerList.add(CheckChangeEventListener.class, listener);
    }

    public final void removeCheckChangeEventListener(CheckChangeEventListener listener) {
        listenerList.remove(CheckChangeEventListener.class, listener);
    }

    void fireCheckChangeEvent(CheckChangeEvent evt) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = 0; i < listeners.length; i++) {
            if (listeners[i] == CheckChangeEventListener.class) {
                ((CheckChangeEventListener) listeners[i + 1]).checkStateChanged(evt);
            }
        }
    }

    @Override
    public final void setModel(TreeModel newModel) {
        super.setModel(newModel);
        resetCheckingState();
    }

    // New method that returns only the checked paths (totally ignores original "selection" mechanism)
    public final TreePath[] getCheckedPaths() {
        return checkedPaths.toArray(new TreePath[checkedPaths.size()]);
    }

    // Returns true in case that the node is selected, has children but not all of them are selected
    public final boolean isSelectedPartially(TreePath path) {
        CheckedNode cn = nodesCheckingState.get(path);
        return cn.isSelected && cn.hasChildren && !cn.allChildrenSelected;
    }

    private void resetCheckingState() {
        nodesCheckingState = new HashMap<>();
        checkedPaths = new ArrayList<TreePath>() {
            @Override
            public boolean add(TreePath e) {
                if (!contains(e)) {
                    return super.add(e);
                }
                return false;
            }

        };
        if (getModel() == null) {
            return;
        }
        TreeNode node = (TreeNode) getModel().getRoot();
        if (node == null) {
            return;
        }
        addSubtreeToCheckingStateTracking(node);
    }

    // Creating data structure of the current model for the checking mechanism
    private void addSubtreeToCheckingStateTracking(TreeNode node) {
        TreeNode[] path = getPath(node);
        TreePath tp = new TreePath(path);
        CheckedNode cn = new CheckedNode(false, getChildCount(node) > 0, false);
        nodesCheckingState.put(tp, cn);
        for (int i = 0; i < getChildCount(node); i++) {
            addSubtreeToCheckingStateTracking((TreeNode) tp.pathByAddingChild(getChildAt(node, i)).getLastPathComponent());
        }
    }

    // Overriding cell renderer by a class that ignores the original "selection" mechanism
    // It decides how to show the nodes due to the checking-mechanism
    private class CheckBoxCellRenderer extends JPanel implements TreeCellRenderer {

        private static final long serialVersionUID = -7341833835878991719L;
        private final Icon selectedIcon = new ImageIcon(getClass().getResource("/ui/resources/checked.png"));
        JCheckBox checkBox;

        public CheckBoxCellRenderer() {
            super();
            this.setLayout(new BorderLayout());
            checkBox = new JCheckBox();
            checkBox.setSelectedIcon(selectedIcon);
            add(checkBox, BorderLayout.CENTER);
            setOpaque(false);
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                boolean selected, boolean expanded, boolean leaf, int row,
                boolean hasFocus) {
            TreeNode node = (TreeNode) value;
            TreePath tp = new TreePath(getPath(node));
            CheckedNode cn = nodesCheckingState.get(tp);
            checkBox.setIcon(getIcon(value));
            if (cn == null) {
                return this;
            }
            checkBox.setSelected(cn.isSelected);
            checkBox.setText(node.toString());
            checkBox.setOpaque(cn.isSelected && cn.hasChildren && !cn.allChildrenSelected);
            setFont(UIManager.getFont("TableMenu.font"));
            return this;
        }
    }

    public abstract Icon getIcon(Object value);

    public JCheckBoxTree() {
        super();
        // Disabling toggling by double-click
        this.setToggleClickCount(0);
        // Overriding cell renderer by new one defined above
        CheckBoxCellRenderer cellRenderer = new CheckBoxCellRenderer();
        this.setCellRenderer(cellRenderer);

        // Overriding selection model by an empty one
        DefaultTreeSelectionModel dtsm = new DefaultTreeSelectionModel() {
            private static final long serialVersionUID = -8190634240451667286L;

            // Totally disabling the selection mechanism
            @Override
            public void setSelectionPath(TreePath path) {
            }

            @Override
            public void addSelectionPath(TreePath path) {
            }

            @Override
            public void removeSelectionPath(TreePath path) {
            }

            @Override
            public void setSelectionPaths(TreePath[] pPaths) {
            }
        };
        // Calling checking mechanism on mouse click
        this.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent arg0) {
                TreePath tp = selfPointer.getPathForLocation(arg0.getX(), arg0.getY());
                if (tp == null) {
                    return;
                }
                boolean checkMode = !nodesCheckingState.get(tp).isSelected;
                checkSubTree(tp, checkMode);
                updatePredecessorsWithCheckMode(tp, checkMode);
                // Firing the check change event
                fireCheckChangeEvent(new CheckChangeEvent(new Object()));
                // Repainting tree after the data structures were updated
                selfPointer.repaint();
            }

            @Override
            public void mouseEntered(MouseEvent arg0) {
            }

            @Override
            public void mouseExited(MouseEvent arg0) {
            }

            @Override
            public void mousePressed(MouseEvent arg0) {
            }

            @Override
            public void mouseReleased(MouseEvent arg0) {
            }
        });
        this.setSelectionModel(dtsm);
    }

    // When a node is checked/unchecked, updating the states of the predecessors
    protected void updatePredecessorsWithCheckMode(TreePath tp, boolean check) {
        TreePath parentPath = tp.getParentPath();
        // If it is the root, stop the recursive calls and return
        if (parentPath == null) {
            return;
        }
        CheckedNode parentCheckedNode = nodesCheckingState.get(parentPath);
        TreeNode parentNode = (TreeNode) parentPath.getLastPathComponent();
        parentCheckedNode.allChildrenSelected = true;
        parentCheckedNode.isSelected = false;
        for (int i = 0; i < getChildCount(parentNode); i++) {
            TreePath childPath = parentPath.pathByAddingChild(getChildAt(parentNode, i));
            CheckedNode childCheckedNode = nodesCheckingState.get(childPath);
            // It is enough that even one subtree is not fully selected
            // to determine that the parent is not fully selected
            if (!childCheckedNode.allChildrenSelected) {
                parentCheckedNode.allChildrenSelected = false;
            }
            // If at least one child is selected, selecting also the parent
            if (childCheckedNode.isSelected) {
                parentCheckedNode.isSelected = true;
            }
        }
        if (parentCheckedNode.isSelected) {
            checkedPaths.add(parentPath);
        } else {
            checkedPaths.remove(parentPath);
        }
        // Go to upper predecessor
        updatePredecessorsWithCheckMode(parentPath, check);
    }

    // Recursively checks/unchecks a subtree
    protected void checkSubTree(TreePath tp, boolean check) {
        CheckedNode cn = nodesCheckingState.get(tp);
        cn.isSelected = check;
        TreeNode node = (TreeNode) tp.getLastPathComponent();
        for (int i = 0; i < getChildCount(node); i++) {
            checkSubTree(tp.pathByAddingChild(getChildAt(node, i)), check);
        }
        cn.allChildrenSelected = check;
        if (check) {
            checkedPaths.add(tp);
        } else {
            checkedPaths.remove(tp);
        }
    }

    private TreeNode[] getPath(TreeNode node) {
        return getPathToRoot(node, 0);
    }

    protected TreeNode[] getPathToRoot(TreeNode node, int depth) {
        if (node == null) {
            if (depth == 0) {
                return null;
            }
            return new TreeNode[depth];
        }
        TreeNode[] path = getPathToRoot(node.getParent(), depth + 1);
        path[path.length - depth - 1] = node;
        return path;
    }

    /**
     * refresh the tree
     * <p>
     * call if the tree elements not displayed properly<p>
     * expands and collapse the tree
     *
     */
    public final void refresh() {
        expandAll(true);
        expandAll(false);
        expandPath(new TreePath(getModel().getRoot()));
    }

    /**
     *
     * @param expand if true expands else collapse the tree
     */
    private void expandAll(boolean expand) {
        TreeNode root = (TreeNode) getModel().getRoot();
        expandAll(new TreePath(root), expand);
    }

    /**
     * iterate through every child and do the action
     *
     * @param parent
     * @param expand
     */
    private void expandAll(TreePath parent, boolean expand) {
        TreeNode node = (TreeNode) parent.getLastPathComponent();
        if (getChildCount(node) >= 0) {
            for (int i = 0; i < getChildCount(node); i++) {
                TreePath path = parent.pathByAddingChild(getChildAt(node, i));
                expandAll(path, expand);
            }
        }
        if (expand) {
            expandPath(parent);
        } else {
            collapsePath(parent);
        }

    }

    public int getChildCount(TreeNode parent) {
        return parent.getChildCount();
    }

    public TreeNode getChildAt(TreeNode parent, int index) {
        return parent.getChildAt(index);
    }

}

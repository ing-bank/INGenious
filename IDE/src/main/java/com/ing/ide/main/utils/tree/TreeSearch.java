
package com.ing.ide.main.utils.tree;

import com.ing.datalib.or.common.ORObjectInf;
import com.ing.ide.main.utils.Utils;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractAction;
import static javax.swing.Action.ACTION_COMMAND_KEY;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

/**
 *
 * 
 */
public class TreeSearch extends JPanel implements ActionListener {

    JToolBar searchBar;
    JTextField searchField;
    JTree tree;

    public static TreeSearch installFor(JTree tree) {
        return new TreeSearch(tree);
    }

    public static TreeSearch installForOR(JTree tree) {
        return new TreeSearch(tree) {
            @Override
            public void selectAndSrollTo(TreeNode node) {
                if (node instanceof ORObjectInf) {
                    TreePath path = ((ORObjectInf) node).getTreePath();
                    tree.setSelectionPath(path);
                    tree.scrollPathToVisible(path);
                } else {
                    super.selectAndSrollTo(node);
                }
            }
        };
    }

    public TreeSearch(JTree tree) {
        this.tree = tree;
        init();
    }

    private void init() {
        setLayout(new BorderLayout());
        createToolBar();
        addSearchListener();
        addTreeListener();
        add(new JScrollPane(tree), BorderLayout.CENTER);
        add(searchBar, BorderLayout.SOUTH);
        searchBar.setVisible(false);
    }

    private void createToolBar() {
        searchBar = new JToolBar();
        searchBar.setFloatable(false);
        searchBar.setLayout(new BoxLayout(searchBar, BoxLayout.X_AXIS));
        searchBar.setBorder(BorderFactory.createEtchedBorder());

        JLabel searchLabel = new JLabel(Utils.getIconByResourceName("/ui/resources/search"));

        searchField = new JTextField();
        searchField.setActionCommand("SearchField");
        searchField.addActionListener(this);

        searchBar.add(searchLabel);
        searchBar.add(new javax.swing.Box.Filler(new java.awt.Dimension(5, 0),
                new java.awt.Dimension(5, 0),
                new java.awt.Dimension(5, 32767)));
        searchBar.add(searchField);

    }

    private void addSearchListener() {

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent de) {
                search();
            }

            @Override
            public void removeUpdate(DocumentEvent de) {
                search();
            }

            @Override
            public void changedUpdate(DocumentEvent de) {
                search();
            }

        });

        searchField.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("F3"), "Next");
        AbstractAction nextAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                TreeSearch.this.actionPerformed(ae);
            }
        };
        nextAction.putValue(ACTION_COMMAND_KEY, "Next");
        searchField.getActionMap().put("Next", nextAction);

        searchField.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("shift F3"), "Previous");
        AbstractAction prevAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                TreeSearch.this.actionPerformed(ae);
            }
        };
        prevAction.putValue(ACTION_COMMAND_KEY, "Previous");
        searchField.getActionMap().put("Previous", prevAction);

        searchField.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ESCAPE"), "Hide");
        searchField.getActionMap().put("Hide", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                searchBar.setVisible(false);
                tree.requestFocusInWindow();
            }
        });

        searchField.setToolTipText("<html>"
                + "Press <b>F3</b> to go to next search"
                + "<br/>"
                + "Press <b>Shift+F3</b> to go to previous search"
                + "<br/>"
                + "Press <b>Escape</b> to hide the searchBox"
                + "<br/>"
                //                + "To perfrom regex search add <b>$</b> before the search string"
                //                + "<br/>"
                + "</html>");
    }

    private void addTreeListener() {

        tree.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ctrl F"), "Search");

        tree.getActionMap().put("Search", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                searchBar.setVisible(true);
                searchField.requestFocusInWindow();
                searchField.selectAll();
            }
        });

        tree.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ESCAPE"), "Hide");

        tree.getActionMap().put("Hide", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                searchBar.setVisible(false);
                tree.requestFocusInWindow();
            }
        });

//        tree.addKeyListener(new KeyAdapter() {
//            @Override
//            public void keyTyped(KeyEvent ke) {
//                if (!searchBar.isVisible()) {
//                    searchBar.setVisible(true);
//                    searchField.setText("" + ke.getKeyChar());
//                    searchField.requestFocusInWindow();
//                }
//            }
//        });
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        switch (ae.getActionCommand()) {
            case "SearchField":
                searchBar.setVisible(false);
                break;
            case "Next":
                goToNext();
                break;
            case "Previous":
                goToPrevious();
                break;
        }
    }

    private void search() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                search(searchField.getText());
            }
        });
    }

    private void goToNext() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                goToNext(searchField.getText());
            }
        });
    }

    private void goToPrevious() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                goToPrevious(searchField.getText());
            }
        });
    }

    private TreeNode getSelectedNode() {
        TreePath path = tree.getSelectionPath();
        if (path != null) {
            return (TreeNode) path.getLastPathComponent();
        }
        return null;
    }

    private void search(String text) {
        TreeNode currNode = (TreeNode) tree.getModel().getRoot();
        while (currNode != null) {
            if (currNode.toString().contains(text)) {
                selectAndSrollTo(currNode);
                break;
            }
            currNode = getNextNode(currNode);
        }
    }

    private void goToNext(String text) {
        TreeNode currNode = getSelectedNode();
        if (currNode == null) {
            currNode = (TreeNode) tree.getModel().getRoot();
        } else {
            currNode = getNextNode(currNode);
        }
        while (currNode != null) {
            if (currNode.toString().contains(text)) {
                selectAndSrollTo(currNode);
                break;
            }
            currNode = getNextNode(currNode);
        }
    }

    private void goToPrevious(String text) {
        TreeNode currNode = getSelectedNode();
        if (currNode == null) {
            return;
        } else {
            currNode = getPreviousNode(currNode);
        }
        while (currNode != null) {
            if (currNode.toString().contains(text)) {
                selectAndSrollTo(currNode);
                break;
            }
            currNode = getPreviousNode(currNode);
        }
    }

    public void selectAndSrollTo(TreeNode node) {
        TreePath path = new TreePath(getPath(node));
        tree.setSelectionPath(path);
        tree.scrollPathToVisible(path);
    }

    /**
     * Returns the node that follows this node in a preorder traversal of this
     * node's tree. Returns null if this node is the last node of the traversal.
     * This is an inefficient way to traverse the entire tree; use an
     * enumeration, instead.
     *
     * @param node
     * @see #preorderEnumeration
     * @return the node that follows this node in a preorder traversal, or null
     * if this node is last
     */
    public TreeNode getNextNode(TreeNode node) {
        if (node.getChildCount() == 0) {
            // No children, so look for nextSibling
            TreeNode nextSibling = getNextSibling(node);

            if (nextSibling == null) {
                TreeNode aNode = node.getParent();

                do {
                    if (aNode == null) {
                        return null;
                    }

                    nextSibling = getNextSibling(aNode);
                    if (nextSibling != null) {
                        return nextSibling;
                    }

                    aNode = (TreeNode) aNode.getParent();
                } while (true);
            } else {
                return nextSibling;
            }
        } else {
            return (TreeNode) node.getChildAt(0);
        }
    }

    /**
     * Returns the next sibling of this node in the parent's children array.
     * Returns null if this node has no parent or is the parent's last child.
     * This method performs a linear search that is O(n) where n is the number
     * of children; to traverse the entire array, use the parent's child
     * enumeration instead.
     *
     * @param node
     * @see #children
     * @return the sibling of this node that immediately follows this node
     */
    public TreeNode getNextSibling(TreeNode node) {
        TreeNode retval;

        TreeNode myParent = node.getParent();

        if (myParent == null) {
            retval = null;
        } else {
            retval = getChildAfter(myParent, node);      // linear search
        }

        if (retval != null && !isNodeSibling(node, retval)) {
            //            throw new Error("child of parent is not a sibling");
        }

        return retval;
    }

    /**
     * Returns the child in this node's child array that immediately follows
     * <code>aChild</code>, which must be a child of this node. If
     * <code>aChild</code> is the last child, returns null. This method performs
     * a linear search of this node's children for <code>aChild</code> and is
     * O(n) where n is the number of children; to traverse the entire array of
     * children, use an enumeration instead.
     *
     * @param parent
     * @param aChild
     * @see #children
     * @exception IllegalArgumentException if <code>aChild</code> is null or is
     * not a child of this node
     * @return the child of this node that immediately follows
     * <code>aChild</code>
     */
    public TreeNode getChildAfter(TreeNode parent, TreeNode aChild) {
        if (aChild == null) {
            throw new IllegalArgumentException("argument is null");
        }

        int index = parent.getIndex(aChild);           // linear search

        if (index == -1) {
            throw new IllegalArgumentException("node is not a child");
        }

        if (index < parent.getChildCount() - 1) {
            return parent.getChildAt(index + 1);
        } else {
            return null;
        }
    }

    /**
     * Returns the node that precedes this node in a preorder traversal of this
     * node's tree. Returns <code>null</code> if this node is the first node of
     * the traversal -- the root of the tree. This is an inefficient way to
     * traverse the entire tree; use an enumeration, instead.
     *
     * @param node
     * @see #preorderEnumeration
     * @return the node that precedes this node in a preorder traversal, or null
     * if this node is the first
     */
    public TreeNode getPreviousNode(TreeNode node) {
        TreeNode previousSibling;
        TreeNode myParent = (TreeNode) node.getParent();

        if (myParent == null) {
            return null;
        }

        previousSibling = getPreviousSibling(node);

        if (previousSibling != null) {
            if (previousSibling.getChildCount() == 0) {
                return previousSibling;
            } else {
                return getLastLeaf(previousSibling);
            }
        } else {
            return myParent;
        }
    }

    /**
     * Returns the previous sibling of this node in the parent's children array.
     * Returns null if this node has no parent or is the parent's first child.
     * This method performs a linear search that is O(n) where n is the number
     * of children.
     *
     * @param node
     * @return the sibling of this node that immediately precedes this node
     */
    public TreeNode getPreviousSibling(TreeNode node) {
        TreeNode retval;

        TreeNode myParent = (TreeNode) node.getParent();

        if (myParent == null) {
            retval = null;
        } else {
            retval = getChildBefore(myParent, node);     // linear search
        }

        if (retval != null && !isNodeSibling(node, retval)) {
//            throw new Error("child of parent is not a sibling");
        }

        return retval;
    }

    /**
     * Returns the child in this node's child array that immediately precedes
     * <code>aChild</code>, which must be a child of this node. If
     * <code>aChild</code> is the first child, returns null. This method
     * performs a linear search of this node's children for <code>aChild</code>
     * and is O(n) where n is the number of children.
     *
     * @param parent
     * @param aChild
     * @exception IllegalArgumentException if <code>aChild</code> is null or is
     * not a child of this node
     * @return the child of this node that immediately precedes
     * <code>aChild</code>
     */
    public TreeNode getChildBefore(TreeNode parent, TreeNode aChild) {
        if (aChild == null) {
            throw new IllegalArgumentException("argument is null");
        }

        int index = parent.getIndex(aChild);           // linear search

        if (index == -1) {
            throw new IllegalArgumentException("argument is not a child");
        }

        if (index > 0) {
            return parent.getChildAt(index - 1);
        } else {
            return null;
        }
    }

    /**
     * Finds and returns the last leaf that is a descendant of this node --
     * either this node or its last child's last leaf. Returns this node if it
     * is a leaf.
     *
     * @param node
     * @see #isLeaf
     * @see #isNodeDescendant
     * @return the last leaf in the subtree rooted at this node
     */
    public TreeNode getLastLeaf(TreeNode node) {

        while (!node.isLeaf()) {
            node = getLastChild(node);
        }

        return node;
    }

    /**
     * Returns this node's last child. If this node has no children, throws
     * NoSuchElementException.
     *
     * @param node
     * @return the last child of this node
     */
    public TreeNode getLastChild(TreeNode node) {
        if (node.getChildCount() == 0) {
            throw new Error("node has no children");
        }
        return node.getChildAt(node.getChildCount() - 1);
    }

    /**
     * Returns true if <code>anotherNode</code> is a sibling of (has the same
     * parent as) this node. A node is its own sibling. If
     * <code>anotherNode</code> is null, returns false.
     *
     * @param node
     * @param anotherNode node to test as sibling of this node
     * @return true if <code>anotherNode</code> is a sibling of this node
     */
    public boolean isNodeSibling(TreeNode node, TreeNode anotherNode) {
        boolean retval;

        if (anotherNode == null) {
            retval = false;
        } else if (anotherNode == node) {
            retval = true;
        } else {
            TreeNode myParent = node.getParent();
            retval = (myParent != null && myParent == anotherNode.getParent());

            if (retval && !isNodeChild(node.getParent(), anotherNode)) {
                throw new Error("sibling has different parent");
            }
        }

        return retval;
    }

    /**
     * Returns true if <code>aNode</code> is a child of this node. If
     * <code>aNode</code> is null, this method returns false.
     *
     * @param parent
     * @param aNode
     * @return true if <code>aNode</code> is a child of this node; false if
     * <code>aNode</code> is null
     */
    public boolean isNodeChild(TreeNode parent, TreeNode aNode) {
        boolean retval;

        if (aNode == null) {
            retval = false;
        } else if (parent.getChildCount() == 0) {
            retval = false;
        } else {
            retval = (aNode.getParent() == parent);
        }

        return retval;
    }

    /**
     * Returns the path from the root, to get to this node. The last element in
     * the path is this node.
     *
     * @param node
     * @return an array of TreeNode objects giving the path, where the first
     * element in the path is the root and the last element is this node.
     */
    public TreeNode[] getPath(TreeNode node) {
        return getPathToRoot(node, 0);
    }

    /**
     * Builds the parents of node up to and including the root node, where the
     * original node is the last element in the returned array. The length of
     * the returned array gives the node's depth in the tree.
     *
     * @param aNode the TreeNode to get the path for
     * @param depth an int giving the number of steps already taken towards the
     * root (on recursive calls), used to size the returned array
     * @return an array of TreeNodes giving the path from the root to the
     * specified node
     */
    protected TreeNode[] getPathToRoot(TreeNode aNode, int depth) {
        TreeNode[] retNodes;

        /* Check for null, in case someone passed in a null node, or
           they passed in an element that isn't rooted at root. */
        if (aNode == null) {
            if (depth == 0) {
                return null;
            } else {
                retNodes = new TreeNode[depth];
            }
        } else {
            depth++;
            retNodes = getPathToRoot(aNode.getParent(), depth);
            retNodes[retNodes.length - depth] = aNode;
        }
        return retNodes;
    }

}

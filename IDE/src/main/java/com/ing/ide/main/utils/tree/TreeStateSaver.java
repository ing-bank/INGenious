package com.ing.ide.main.utils.tree;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JTree;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

/**
 * Captures the expanded-node and selected-node state of a {@link JTree}
 * so it can be restored after the underlying model is rebuilt.
 * <p>
 * Node identity is keyed by the {@code toString()} value of each node in
 * the path. This survives model rebuilds in which {@link TreeNode}
 * instances are recreated but their displayed labels are preserved
 * (the typical pattern for {@code Project.reload()}).
 */
public final class TreeStateSaver {

    private static final String SEP = "\u0000";

    private final Set<String> expandedKeys = new HashSet<>();
    private final List<String> selectionKeys = new ArrayList<>();

    private TreeStateSaver() {
    }

    /**
     * Captures the expansion and selection state of {@code tree}.
     * Safe to call with any tree (including one that has no model).
     */
    public static TreeStateSaver capture(JTree tree) {
        TreeStateSaver snap = new TreeStateSaver();
        if (tree == null || tree.getModel() == null || tree.getModel().getRoot() == null) {
            return snap;
        }
        Object root = tree.getModel().getRoot();
        TreePath rootPath = new TreePath(root);
        Enumeration<TreePath> expanded = tree.getExpandedDescendants(rootPath);
        if (expanded != null) {
            while (expanded.hasMoreElements()) {
                snap.expandedKeys.add(keyFor(expanded.nextElement()));
            }
        }
        // Always remember that the root itself was visible/expanded.
        if (tree.isExpanded(rootPath)) {
            snap.expandedKeys.add(keyFor(rootPath));
        }
        TreePath[] selected = tree.getSelectionPaths();
        if (selected != null) {
            for (TreePath p : selected) {
                snap.selectionKeys.add(keyFor(p));
            }
        }
        return snap;
    }

    /**
     * Restores the previously captured expansion and selection state.
     * Paths in the new tree are matched by node {@code toString()} values.
     * Missing nodes (e.g. deleted externally) are silently skipped.
     */
    public void restore(JTree tree) {
        if (tree == null || tree.getModel() == null || tree.getModel().getRoot() == null) {
            return;
        }
        Object root = tree.getModel().getRoot();
        // Re-expand everything we had open. Walk the saved keys longest-first
        // so parents are expanded before their children when needed.
        List<String> ordered = new ArrayList<>(expandedKeys);
        ordered.sort((a, b) -> Integer.compare(
                a.split(SEP, -1).length, b.split(SEP, -1).length));
        for (String key : ordered) {
            TreePath path = resolve(root, key);
            if (path != null) {
                tree.expandPath(path);
            }
        }
        // Restore selection (best-effort — first match wins).
        List<TreePath> selPaths = new ArrayList<>();
        for (String key : selectionKeys) {
            TreePath p = resolve(root, key);
            if (p != null) {
                selPaths.add(p);
            }
        }
        if (!selPaths.isEmpty()) {
            tree.setSelectionPaths(selPaths.toArray(new TreePath[0]));
            tree.scrollPathToVisible(selPaths.get(0));
        }
    }

    private static String keyFor(TreePath path) {
        StringBuilder sb = new StringBuilder();
        Object[] parts = path.getPath();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                sb.append(SEP);
            }
            sb.append(String.valueOf(parts[i]));
        }
        return sb.toString();
    }

    private static TreePath resolve(Object root, String key) {
        String[] names = key.split(SEP, -1);
        if (names.length == 0 || !String.valueOf(root).equals(names[0])) {
            return null;
        }
        Object current = root;
        Object[] resolved = new Object[names.length];
        resolved[0] = current;
        for (int i = 1; i < names.length; i++) {
            Object child = findChild(current, names[i]);
            if (child == null) {
                return null;
            }
            resolved[i] = child;
            current = child;
        }
        return new TreePath(resolved);
    }

    private static Object findChild(Object parent, String name) {
        if (!(parent instanceof TreeNode)) {
            return null;
        }
        TreeNode tn = (TreeNode) parent;
        int count = tn.getChildCount();
        for (int i = 0; i < count; i++) {
            TreeNode child = tn.getChildAt(i);
            if (name.equals(String.valueOf(child))) {
                return child;
            }
        }
        return null;
    }
}

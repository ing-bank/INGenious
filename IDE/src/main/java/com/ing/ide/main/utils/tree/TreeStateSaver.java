package com.ing.ide.main.utils.tree;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JTree;
import javax.swing.tree.TreePath;

/**
 * Captures and restores a {@link JTree}'s expanded paths and selection across a
 * model rebuild, keyed by node label strings. This survives reloads where the
 * underlying {@code TreeNode} instances are recreated (e.g. a project reload
 * from disk) as long as the displayed labels stay the same.
 */
public final class TreeStateSaver {

    private TreeStateSaver() {}

    /** Opaque snapshot of a tree's expansion + selection. */
    public static final class State {
        private final Set<String> expanded = new HashSet<>();
        private String selected;
    }

    /** Captures the current expansion and selection of {@code tree}. */
    public static State capture(JTree tree) {
        State state = new State();
        if (tree == null || tree.getModel() == null || tree.getModel().getRoot() == null) {
            return state;
        }
        TreePath rootPath = new TreePath(tree.getModel().getRoot());
        Enumeration<TreePath> expanded = tree.getExpandedDescendants(rootPath);
        if (expanded != null) {
            while (expanded.hasMoreElements()) {
                state.expanded.add(key(expanded.nextElement()));
            }
        }
        TreePath sel = tree.getSelectionPath();
        if (sel != null) {
            state.selected = key(sel);
        }
        return state;
    }

    /** Restores a previously captured expansion and selection onto {@code tree}. */
    public static void restore(JTree tree, State state) {
        if (tree == null || state == null) {
            return;
        }
        // Expand matching rows; the row count grows as parents expand and reveal
        // children, so re-scan until no further expansion happens.
        boolean changed = true;
        int guard = 0;
        while (changed && guard++ < 1000) {
            changed = false;
            for (int i = 0; i < tree.getRowCount(); i++) {
                TreePath path = tree.getPathForRow(i);
                if (path != null && state.expanded.contains(key(path)) && tree.isCollapsed(path)) {
                    tree.expandPath(path);
                    changed = true;
                }
            }
        }
        if (state.selected != null) {
            for (int i = 0; i < tree.getRowCount(); i++) {
                TreePath path = tree.getPathForRow(i);
                if (path != null && state.selected.equals(key(path))) {
                    tree.setSelectionPath(path);
                    tree.scrollPathToVisible(path);
                    break;
                }
            }
        }
    }

    /** Joins node labels with NUL separators so labels containing '/' or '.' don't collide. */
    private static String key(TreePath path) {
        StringBuilder sb = new StringBuilder();
        for (Object node : path.getPath()) {
            sb.append(node == null ? "" : node.toString()).append('\u0000');
        }
        return sb.toString();
    }
}

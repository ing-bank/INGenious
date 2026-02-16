package com.ing.ide.main.utils.tree;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.EventObject;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;

/**
 *
 *
 */
public class TreeSelectionRenderer extends DefaultTreeCellRenderer {

    public Boolean cellFocused = false;

    public TreeSelectionRenderer(JTree tree) {
        editLaF(tree);
        install(tree);
        tree.setCellRenderer(this);
        // Set the renderer's internal selection colors from UIManager
        updateSelectionColors();
    }
    
    /**
     * Update the renderer's internal selection colors from UIManager.
     * Call this after theme changes.
     */
    public void updateSelectionColors() {
        Color selBg = UIManager.getColor("ing.selectionBackground");
        if (selBg == null) {
            selBg = UIManager.getColor("Tree.selectionBackground");
        }
        if (selBg != null) {
            setBackgroundSelectionColor(selBg);
        }
        
        Color selFg = UIManager.getColor("ing.selectionForeground");
        if (selFg == null) {
            selFg = UIManager.getColor("Tree.selectionForeground");
        }
        if (selFg != null) {
            setTextSelectionColor(selFg);
        }
        
        Color bg = UIManager.getColor("Tree.background");
        if (bg != null) {
            setBackgroundNonSelectionColor(bg);
        }
        
        Color fg = UIManager.getColor("Tree.foreground");
        if (fg != null) {
            setTextNonSelectionColor(fg);
        }
    }
    
    @Override
    public void updateUI() {
        super.updateUI();
        // Refresh selection colors when L&F changes
        updateSelectionColors();
    }

    public final void editLaF(JTree tree) {
        tree.setOpaque(true);
        // Propagate tree background to viewport/scrollpane for consistent panel color
        tree.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.PARENT_CHANGED) != 0) {
                Container parent = tree.getParent();
                if (parent instanceof javax.swing.JViewport) {
                    parent.setBackground(tree.getBackground());
                    Container sp = parent.getParent();
                    if (sp instanceof javax.swing.JScrollPane) {
                        sp.setBackground(tree.getBackground());
                    }
                }
            }
        });
    }

    public final void install(JTree tree) {
        editLaF(tree);
        tree.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent fe) {
                cellFocused = true;
                repaint();
            }

            @Override
            public void focusLost(FocusEvent fe) {
                cellFocused = false;
                repaint();
            }
        });
        tree.setCellEditor(new CustomTreeCellEditor(
                tree, (DefaultTreeCellRenderer) this) {
            @Override
            public Color getBorderSelectionColor() {
                return Color.darkGray;
            }

            @Override
            public boolean isCellEditable(EventObject arg0) {
                if (arg0 instanceof MouseEvent) {
                    return false;
                }
                return super.isCellEditable(arg0);
            }
        });
    }

    public static void installFor(JTree tree) {
        TreeSelectionRenderer renderer = (TreeSelectionRenderer) tree.getCellRenderer();
        renderer.install(tree);
    }

    @Override
    public Component getTreeCellRendererComponent(JTree jtree, Object o, boolean bln, boolean bln1, boolean bln2, int i, boolean bln3) {
        JComponent comp = (JComponent) super.getTreeCellRendererComponent(jtree, o, bln, bln1, bln2, i, bln3);
        comp.setOpaque(true);
        if (selected) {
            Color selBg;
            if (cellFocused) {
                selBg = UIManager.getColor("ing.selectionBackground");
            } else {
                selBg = UIManager.getColor("ing.selectionInactiveBackground");
            }
            comp.setBackground(selBg != null ? selBg : Color.decode("#D4EDFD"));
            Color selFg = UIManager.getColor("ing.selectionForeground");
            comp.setForeground(selFg != null ? selFg : Color.decode("#4D0020"));
        } else {
            Color bg = UIManager.getColor("Tree.background");
            comp.setBackground(bg != null ? bg : jtree.getBackground());
            Color fg = UIManager.getColor("Tree.foreground");
            if (fg != null) {
                comp.setForeground(fg);
            }
        }
        return comp;
    }

    public class CustomTreeCellEditor extends DefaultTreeCellEditor {

        public CustomTreeCellEditor(JTree tree, DefaultTreeCellRenderer renderer) {
            super(tree, renderer);
        }

        @Override
        public Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected, boolean expanded, boolean leaf, int row) {
            Component editorComponent = super.getTreeCellEditorComponent(tree, value, isSelected, expanded, leaf, row);

            if (editorComponent instanceof Container) {
                for (Component comp : ((Container) editorComponent).getComponents()) {
                    if (comp instanceof JTextField) {
                        JTextField editor = (JTextField) comp;
                        int menuShortcutKeyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

                        // Remove default Ctrl key bindings
                        editor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_A, menuShortcutKeyMask), "none");
                        editor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_X, menuShortcutKeyMask), "none");
                        editor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_C, menuShortcutKeyMask), "none");
                        editor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_V, menuShortcutKeyMask), "none");

                        // Add Cmd key bindings
                        editor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_X, menuShortcutKeyMask), "cut");
                        editor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_C, menuShortcutKeyMask), "copy");
                        editor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_V, menuShortcutKeyMask), "paste");
                        editor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_A, menuShortcutKeyMask), "selectAll");
                        editor.getActionMap().put("selectAll", new AbstractAction() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            editor.selectAll();
                        }
                    });
                    }
                }
            }

            return editorComponent;
        }
    }

}

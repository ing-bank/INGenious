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
import javax.swing.UIDefaults;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;

/**
 *
 *
 */
public class TreeSelectionRenderer extends DefaultTreeCellRenderer {

    public Boolean cellFocused = false;
    final Color DEF_SELECTION_COLOR = Color.decode("#ffcfb2");
    final Color NOFOCUS_SELECTION_COLOR = Color.decode("#ffcfb2");

    public TreeSelectionRenderer(JTree tree) {
        editLaF(tree);
        install(tree);
        tree.setCellRenderer(this);
    }

    public final void editLaF(JTree tree) {
        UIDefaults paneDefaults = new UIDefaults();
        paneDefaults.put("Tree.selectionBackground", null);
        tree.putClientProperty("Nimbus.Overrides", paneDefaults);
        tree.putClientProperty("Nimbus.Overrides.InheritDefaults", true);
        tree.setBackground(Color.WHITE);
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
            if (cellFocused) {
                comp.setBackground(DEF_SELECTION_COLOR);
            } else {
                comp.setBackground(NOFOCUS_SELECTION_COLOR);
            }
            comp.setForeground(Color.decode("#0000ff"));
        } else {
            comp.setBackground(jtree.getBackground());
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

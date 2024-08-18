
package com.ing.ide.main.utils.tree;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.util.EventObject;
import javax.swing.JComponent;
import javax.swing.JTree;
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
        tree.setCellEditor(new DefaultTreeCellEditor(
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

}

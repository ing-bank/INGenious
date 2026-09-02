package com.ing.ide.main.utils.keys;

import com.ing.ide.main.utils.table.XTable;
import com.ing.ide.main.utils.table.XTableUtils;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.Action;
import javax.swing.JTable;

/**
 *
 *
 */
public class ClipboardKeyAdapter extends KeyAdapter {
    private final JTable table;

    public ClipboardKeyAdapter(JTable table) {
        this.table = table;
    }

    @Override
    public void keyReleased(KeyEvent event) {
        if (event.isControlDown() || event.isMetaDown()) {
            switch (event.getKeyCode()) {
                case KeyEvent.VK_C:
                    // Copy
                    cancelEditing();
                    Action copyAction = table.getActionMap().get("Copy");
                    if (copyAction == null) {
                        copyAction = table.getActionMap().get("copy");
                    }
                    if (copyAction != null) {
                        copyAction.actionPerformed(
                            new ActionEvent(table, ActionEvent.ACTION_PERFORMED, "Copy")
                        );
                    } else if (table instanceof XTable) {
                        ((XTable) table).copy();
                    } else {
                        XTableUtils.copyToClipboard(table, false);
                    }
                    break;
                case KeyEvent.VK_X:
                    // Cut
                    cancelEditing();
                    Action cutAction = table.getActionMap().get("Cut");
                    if (cutAction == null) {
                        cutAction = table.getActionMap().get("cut");
                    }
                    if (cutAction != null) {
                        cutAction.actionPerformed(
                            new ActionEvent(table, ActionEvent.ACTION_PERFORMED, "Cut")
                        );
                    } else if (table instanceof XTable) {
                        ((XTable) table).cut();
                    } else {
                        XTableUtils.copyToClipboard(table, true);
                    }
                    break;
                case KeyEvent.VK_V:
                    // Paste
                    cancelEditing();
                    Action pasteAction = table.getActionMap().get("Paste");
                    if (pasteAction == null) {
                        pasteAction = table.getActionMap().get("paste");
                    }
                    if (pasteAction != null) {
                        pasteAction.actionPerformed(
                            new ActionEvent(table, ActionEvent.ACTION_PERFORMED, "Paste")
                        );
                    } else if (table instanceof XTable) {
                        ((XTable) table).paste();
                    } else {
                        XTableUtils.pasteFromClipboard(table);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private void cancelEditing() {
        if (table.getCellEditor() != null) {
            table.getCellEditor().cancelCellEditing();
        }
    }
}


package com.ing.ide.main.utils.keys;

import com.ing.ide.main.utils.table.XTableUtils;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
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
                    XTableUtils.copyToClipboard(table, false);
                    break;
                case KeyEvent.VK_X:
                    // Cut
                    cancelEditing();
                    XTableUtils.copyToClipboard(table, true);
                    break;
                case KeyEvent.VK_V:
                    // Paste
                    cancelEditing();
                    XTableUtils.pasteFromClipboard(table);
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

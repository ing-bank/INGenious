package com.ing.ide.main.utils.table;

import java.awt.Component;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import javax.swing.AbstractCellEditor;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

/**
 * A cell editor that displays cell values as read-only labels.
 * Users cannot edit the cell value; it can only be updated programmatically.
 * Useful for auto-populated fields like Scope in Test Datasheets.
 */
public class ReadOnlyCellEditor extends AbstractCellEditor implements TableCellEditor {
    private final JLabel label = new JLabel();
    private Object cellValue;

    public ReadOnlyCellEditor() {
        label.setOpaque(true);
        label.setBorder(null);
        label.addFocusListener(
            new FocusAdapter() {

                @Override
                public void focusLost(FocusEvent e) {
                    cancelCellEditing();
                }
            }
        );
    }

    @Override
    public Object getCellEditorValue() {
        return cellValue;
    }

    @Override
    public Component getTableCellEditorComponent(
        JTable table,
        Object value,
        boolean isSelected,
        int row,
        int column
    ) {
        cellValue = value;
        String displayValue = value != null ? value.toString() : "";
        label.setText(displayValue);

        // Apply table styling
        if (isSelected) {
            label.setBackground(table.getSelectionBackground());
            label.setForeground(table.getSelectionForeground());
        } else {
            label.setBackground(table.getBackground());
            label.setForeground(table.getForeground());
        }

        // Set light gray background to indicate read-only field
        label.setBackground(new java.awt.Color(240, 240, 240));

        return label;
    }
}


package com.ing.ide.main.utils.table.autosuggest;

import java.awt.*;
import javax.swing.*;

/**
 * 
 * 
 */
public abstract class ComboSeparatorsRenderer implements ListCellRenderer {

    private final ListCellRenderer delegate;
    private final JPanel separatorPanel = new JPanel(new BorderLayout());
    private final JSeparator separator = new JSeparator();

    public ComboSeparatorsRenderer(ListCellRenderer delegate) {
        this.delegate = delegate;
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        Component comp = delegate.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (index != -1 && addSeparatorAfter(list, value, index)) { // index==1 if renderer is used to paint current value in combo
            separatorPanel.removeAll();
            separatorPanel.add(comp, BorderLayout.CENTER);
            separatorPanel.add(separator, BorderLayout.SOUTH);
            return separatorPanel;
        } else {
            return comp;
        }
    }

    protected abstract boolean addSeparatorAfter(JList list, Object value, int index);
}

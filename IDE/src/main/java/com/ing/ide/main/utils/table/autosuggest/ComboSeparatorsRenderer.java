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
    public Component getListCellRendererComponent(
        JList list,
        Object value,
        int index,
        boolean isSelected,
        boolean cellHasFocus
    ) {
        Component comp = delegate.getListCellRendererComponent(
            list,
            value,
            index,
            isSelected,
            cellHasFocus
        );
        customizeListItemComponent(comp, list, value, index, isSelected, cellHasFocus);
        if (index != -1) {
            boolean addHeader = addHeaderBefore(list, value, index);
            boolean addSeparator = addSeparatorAfter(list, value, index);

            if (addHeader || addSeparator) {
                separatorPanel.removeAll();
                if (addHeader) {
                    separatorPanel.add(
                        createHeaderLabel(
                            getHeaderLabel(list, value, index),
                            getHeaderForeground(list, value, index, comp)
                        ),
                        BorderLayout.NORTH
                    );
                }
                separatorPanel.add(comp, BorderLayout.CENTER);
                if (addSeparator) {
                    separatorPanel.add(separator, BorderLayout.SOUTH);
                }
                return separatorPanel;
            }
        }
        return comp;
    }

    protected boolean addHeaderBefore(JList list, Object value, int index) {
        return false;
    }

    protected String getHeaderLabel(JList list, Object value, int index) {
        return "";
    }

    protected void customizeListItemComponent(
        Component comp,
        JList list,
        Object value,
        int index,
        boolean isSelected,
        boolean cellHasFocus
    ) {
        // no-op by default
    }

    protected Color getHeaderForeground(JList list, Object value, int index, Component comp) {
        return comp == null ? Color.DARK_GRAY : comp.getForeground();
    }

    private JLabel createHeaderLabel(String text, Color foreground) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD, 11f));
        label.setBorder(BorderFactory.createEmptyBorder(3, 6, 1, 6));
        label.setOpaque(true);
        label.setBackground(UIManager.getColor("List.background"));
        label.setForeground(foreground != null ? foreground : Color.DARK_GRAY);
        return label;
    }

    protected abstract boolean addSeparatorAfter(JList list, Object value, int index);
}

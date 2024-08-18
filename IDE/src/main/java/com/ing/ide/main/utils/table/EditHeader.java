
package com.ing.ide.main.utils.table;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.MatteBorder;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;

/**
 *
 * 
 */
public class EditHeader {

    private final JTableHeader header;
    private final JPopupMenu renamePopup;
    private final JTextField text;
    private TableColumn column;
    Action action;
    Integer[] dontEditTheseColums;

    Boolean cancelEdit = false;

    private final MouseAdapter editAdatper;

    public static EditHeader setEditableHeader(JTable table, Integer... dontEditTheseColums) {
        return new EditHeader(table, null, dontEditTheseColums);
    }

    public static EditHeader setEditableHeader(JTable table, Action action, Integer... dontEditTheseColums) {
        return new EditHeader(table, action, dontEditTheseColums);
    }

    private EditHeader(JTable table, Action action, Integer... dontEditTheseColums) {
        header = table.getTableHeader();
        this.action = action;
        this.dontEditTheseColums = dontEditTheseColums;
        this.editAdatper = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if ((SwingUtilities.isLeftMouseButton(event) && event.getClickCount() == 2)
                        || SwingUtilities.isMiddleMouseButton(event)) {
                    editColumnAt(event.getPoint());
                }
            }
        };
        header.addMouseListener(editAdatper);
        text = new JTextField();
        text.setBorder(null);
        text.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                renameColumn();
            }
        });

        text.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent fe) {
                if (!cancelEdit) {
                    renameColumn();
                }
                cancelEdit = false;
            }

        });

        text.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ESCAPE"), "Cancel");

        text.getActionMap().put("Cancel", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                cancelEdit = true;
                renamePopup.setVisible(false);
            }
        });

        renamePopup = new JPopupMenu();
        renamePopup.setBorder(new MatteBorder(0, 1, 1, 1, Color.DARK_GRAY));
        renamePopup.add(text);
    }

    private void editColumnAt(Point p) {
        int columnIndex = header.columnAtPoint(p);
        if (columnIndex != -1 && dontEditTheseColums != null && !Arrays.asList(dontEditTheseColums).contains(columnIndex)) {
            column = header.getColumnModel().getColumn(columnIndex);
            Rectangle columnRectangle = header.getHeaderRect(columnIndex);

            text.setText(column.getHeaderValue().toString());
            renamePopup.setPreferredSize(new Dimension(columnRectangle.width, columnRectangle.height - 1));
            renamePopup.show(header, columnRectangle.x, 0);

            text.requestFocusInWindow();
            text.selectAll();
        }
    }

    private void renameColumn() {
        Object oldvalue = column.getHeaderValue();
        String newvalue = text.getText();
        if (!newvalue.isEmpty() && !newvalue.equals(oldvalue.toString()) && isItOkToNameThisColumnWith(newvalue)) {
            if (action != null) {
                action.putValue("oldvalue", oldvalue);
                action.putValue("newvalue", newvalue);
                action.actionPerformed(null);
                if (action.getValue("rename").equals(true)) {
                    column.setHeaderValue(newvalue);
                }
            } else {
                column.setHeaderValue(newvalue);
            }
        }
        renamePopup.setVisible(false);
        header.repaint();
    }

    private Boolean isItOkToNameThisColumnWith(String name) {
        for (int i = 0; i < header.getColumnModel().getColumnCount(); i++) {
            if (header.getColumnModel().getColumn(i).getHeaderValue().toString().equals(name)) {
                return false;
            }
        }
        return true;
    }

    public final void enableEdit() {
        header.addMouseListener(editAdatper);
    }

    public final void disableEdit() {
        header.removeMouseListener(editAdatper);
    }
}

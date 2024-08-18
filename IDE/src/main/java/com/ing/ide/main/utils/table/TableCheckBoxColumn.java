
package com.ing.ide.main.utils.table;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

public class TableCheckBoxColumn {

    public static void installFor(JTable table, int column) {
        installFor(table, null, column);
    }

    public static void installFor(JTable table, JPopupMenu popupMenu, int column) {
        removeOldHeaderMouseListener(table);
        TableModel model = table.getModel();
        model.addTableModelListener(new HeaderCheckBoxHandler(table));
        table.getTableHeader().addMouseListener(new HeaderMouseListener(popupMenu, column));
        TableCellRenderer r = new HeaderRenderer(table.getTableHeader(), popupMenu, column);
        table.getColumnModel().getColumn(column).setHeaderRenderer(r);
        TableCellRenderer leftAlign = new LeftAlignHeaderRenderer();
        for (int i = 0; i < table.getColumnCount(); i++) {
            if (i != column) {
                table.getColumnModel().getColumn(i).setHeaderRenderer(leftAlign);
            }
        }
    }

    private static void removeOldHeaderMouseListener(JTable table) {
        for (MouseListener listener
                : table.getTableHeader().getMouseListeners()) {
            if (listener instanceof HeaderMouseListener) {
                table.getTableHeader().removeMouseListener(listener);
            }
        }
    }
}

class HeaderMouseListener extends MouseAdapter {

    private JPopupMenu popupMenu;
    int targetColumnIndex;

    public HeaderMouseListener(JPopupMenu popupMenu,
            int targetColumnIndex) {
        this.popupMenu = popupMenu;
        this.targetColumnIndex = targetColumnIndex;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        JTableHeader header = (JTableHeader) e.getSource();
        JTable table = header.getTable();
        TableColumnModel columnModel = table.getColumnModel();
        int vci = columnModel.getColumnIndexAtX(e.getX());
        int mci = table.convertColumnIndexToModel(vci);
        if (mci == targetColumnIndex) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                TableColumn column = columnModel.getColumn(vci);
                Object v = column.getHeaderValue();
                boolean b = Status.DESELECTED.equals(v);
                TableModel m = table.getModel();
                for (int i = 0; i < m.getRowCount(); i++) {
                    m.setValueAt(b, i, mci);
                }
                column.setHeaderValue(b ? Status.SELECTED : Status.DESELECTED);
            } else if (SwingUtilities.isRightMouseButton(e)) {
                if (popupMenu != null) {
                    popupMenu.show(table, e.getX(), 0);
                }
            }
        }
    }
}

/**
 *
 * 
 */
class HeaderRenderer extends JCheckBox implements TableCellRenderer {

    MouseAdapter adapter;

    public HeaderRenderer(JTableHeader header, final int targetColumnIndex) {
        this(header, null, targetColumnIndex);
    }

    public HeaderRenderer(JTableHeader header, final JPopupMenu popupMenu, final int targetColumnIndex) {
        super((String) null);
        setOpaque(false);
        setFont(header.getFont());
    }

    @Override
    public Component getTableCellRendererComponent(
            JTable tbl, Object val, boolean isS, boolean hasF, int row, int col) {
        if (val instanceof Status) {
            switch ((Status) val) {
                case SELECTED:
                    setSelected(true);
                    setEnabled(true);
                    break;
                case DESELECTED:
                    setSelected(false);
                    setEnabled(true);
                    break;
                case INDETERMINATE:
                    setSelected(true);
                    setEnabled(false);
                    break;
            }
        } else {
            setSelected(true);
            setEnabled(false);
        }
        TableCellRenderer r = tbl.getTableHeader().getDefaultRenderer();
        JLabel l = (JLabel) r.getTableCellRendererComponent(tbl, null, isS, hasF, row, col);

        l.setIcon(new CheckBoxIcon(this));
        l.setText(null);
        l.setHorizontalAlignment(SwingConstants.CENTER);
//        l.setComponentPopupMenu(popupMenu);

        return l;
    }
}

class LeftAlignHeaderRenderer implements TableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(
            JTable t, Object v, boolean isS, boolean hasF, int row, int col) {
        TableCellRenderer r = t.getTableHeader().getDefaultRenderer();
        JLabel l = (JLabel) r.getTableCellRendererComponent(t, v, isS, hasF, row, col);
        l.setHorizontalAlignment(SwingConstants.LEFT);
        return l;
    }
}
//</ins>

enum Status {
    SELECTED, DESELECTED, INDETERMINATE
}

class CheckBoxIcon implements Icon {

    private final JCheckBox check;

    public CheckBoxIcon(JCheckBox check) {
        this.check = check;
    }

    @Override
    public int getIconWidth() {
        return check.getPreferredSize().width;
    }

    @Override
    public int getIconHeight() {
        return check.getPreferredSize().height;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        SwingUtilities.paintComponent(
                g, check, (Container) c, x, y, getIconWidth(), getIconHeight());
    }
}

class HeaderCheckBoxHandler implements TableModelListener {

    private final JTable table;

    public HeaderCheckBoxHandler(JTable table) {
        this.table = table;
    }

    @Override
    public void tableChanged(TableModelEvent e) {
        if (e.getType() == TableModelEvent.UPDATE && e.getColumn() == 0) {
            int mci = 0;
            int vci = table.convertColumnIndexToView(mci);
            TableColumn column = table.getColumnModel().getColumn(vci);
            Object title = column.getHeaderValue();
            if (!Status.INDETERMINATE.equals(title)) {
                column.setHeaderValue(Status.INDETERMINATE);
            } else {
                int selected = 0, deselected = 0;
                TableModel m = table.getModel();
                for (int i = 0; i < m.getRowCount(); i++) {
                    if (Boolean.TRUE.equals(m.getValueAt(i, mci))) {
                        selected++;
                    } else {
                        deselected++;
                    }
                }
                if (selected == 0) {
                    column.setHeaderValue(Status.DESELECTED);
                } else if (deselected == 0) {
                    column.setHeaderValue(Status.SELECTED);
                } else {
                    return;
                }
            }
            table.getTableHeader().repaint();
        }
    }
}

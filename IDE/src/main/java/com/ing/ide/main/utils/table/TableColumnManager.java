package com.ing.ide.main.utils.table;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

/**
 * The TableColumnManager can be used to manage TableColumns. It will give the
 * user the ability to hide columns and then reshow them in their last viewed
 * position. This functionality is supported by a popup menu added to the table
 * header of the table. The TableColumnModel is still used to control the view
 * for the table. The manager will inovoke the appropriate methods of the
 * TableColumnModel to hide/show columns as required.
 *
 */
public class TableColumnManager
    implements MouseListener, ActionListener, TableColumnModelListener, PropertyChangeListener {
    private JTable table;
    private TableColumnModel tcm;
    private boolean menuPopup;

    private List<TableColumn> allColumns;

    private Runnable onVisibilityChanged;

    /**
     * Convenience constructor for creating a TableColumnManager for a table.
     * Support for a popup menu on the table header will be enabled.
     *
     * @param table the table whose TableColumns will managed.
     */
    public TableColumnManager(JTable table) {
        this(table, true);
    }

    /**
     * Create a TableColumnManager for a table.
     *
     * @param table the table whose TableColumns will managed.
     * @param menuPopup enable or disable a popup menu to allow the users to
     * manager the visibility of TableColumns.
     */
    public TableColumnManager(JTable table, boolean menuPopup) {
        this.table = table;
        setMenuPopup(menuPopup);

        table.addPropertyChangeListener(this);
        reset();
    }

    /**
     * Reset the TableColumnManager to only manage the TableColumns that are
     * currently visible in the table.
     *
     * Generally this method should only be invoked by the TableColumnManager
     * when the TableModel of the table is changed.
     */
    public final void reset() {
        table.getColumnModel().removeColumnModelListener(this);
        tcm = table.getColumnModel();
        tcm.addColumnModelListener(this);

        //  Keep a duplicate TableColumns for managing hidden TableColumns
        int count = tcm.getColumnCount();
        allColumns = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            allColumns.add(tcm.getColumn(i));
        }
    }

    /**
     * Register a callback fired whenever the user changes column visibility
     * (via the header popup). Used to persist the selection.
     */
    public void setOnVisibilityChanged(Runnable callback) {
        this.onVisibilityChanged = callback;
    }

    private void fireVisibilityChanged() {
        if (onVisibilityChanged != null) {
            onVisibilityChanged.run();
        }
    }

    /**
     * @return the currently visible column header names, in view order.
     */
    public List<String> getVisibleColumnNames() {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < tcm.getColumnCount(); i++) {
            names.add(tcm.getColumn(i).getHeaderValue().toString());
        }
        return names;
    }

    /**
     * Apply a persisted visibility set: show every managed column whose name is
     * in {@code visible} and hide the rest, restoring each column to its natural
     * position. Unknown/duplicate names are ignored; an empty or fully-invalid
     * set leaves the table unchanged (all columns visible). Does not fire the
     * visibility callback, since this reflects already-persisted state.
     *
     * @param visible header names to keep visible
     */
    public void applyVisible(List<String> visible) {
        if (visible == null || visible.isEmpty()) {
            return;
        }
        java.util.Set<String> known = new java.util.HashSet<>();
        for (TableColumn c : allColumns) {
            known.add(c.getHeaderValue().toString());
        }
        List<String> wanted = new ArrayList<>();
        for (String name : visible) {
            if (known.contains(name) && !wanted.contains(name)) {
                wanted.add(name);
            }
        }
        if (wanted.isEmpty()) {
            return;
        }
        for (TableColumn c : new ArrayList<>(allColumns)) {
            String name = c.getHeaderValue().toString();
            boolean currentlyVisible;
            try {
                tcm.getColumnIndex(name);
                currentlyVisible = true;
            } catch (IllegalArgumentException e) {
                currentlyVisible = false;
            }
            if (wanted.contains(name) && !currentlyVisible) {
                showColumn(name);
            } else if (!wanted.contains(name) && currentlyVisible) {
                hideColumn(name);
            }
        }
    }

    /**
     * Get the popup support.
     *
     * @return the popup support
     */
    public final boolean isMenuPopup() {
        return menuPopup;
    }

    /**
     * Add/remove support for a popup menu to the table header. The popup menu
     * will give the user control over which columns are visible.
     *
     * @param menuPopup when true support for displaying a popup menu is added
     * otherwise the popup menu is removed.
     */
    public final void setMenuPopup(boolean menuPopup) {
        table.getTableHeader().removeMouseListener(this);

        if (menuPopup) {
            table.getTableHeader().addMouseListener(this);
        }

        this.menuPopup = menuPopup;
    }

    /**
     * Hide a column from view in the table.
     *
     * @param modelColumn the column index from the TableModel of the column to
     * be removed
     */
    public void hideColumn(int modelColumn) {
        int viewColumn = table.convertColumnIndexToView(modelColumn);

        if (viewColumn != -1) {
            TableColumn column = tcm.getColumn(viewColumn);
            hideColumn(column);
        }
    }

    /**
     * Hide a column from view in the table.
     *
     * @param columnName the column name of the column to be removed
     */
    public void hideColumn(Object columnName) {
        if (columnName == null) {
            return;
        }

        for (int i = 0; i < tcm.getColumnCount(); i++) {
            TableColumn column = tcm.getColumn(i);

            if (columnName.equals(column.getHeaderValue())) {
                hideColumn(column);
                break;
            }
        }
    }

    /**
     * Hide a column from view in the table.
     *
     * @param column the TableColumn to be removed from the TableColumnModel of
     * the table
     */
    public void hideColumn(TableColumn column) {
        if (tcm.getColumnCount() == 1) {
            return;
        }

        //  Ignore changes to the TableColumnModel made by the TableColumnManager
        tcm.removeColumnModelListener(this);
        tcm.removeColumn(column);
        tcm.addColumnModelListener(this);
    }

    /**
     * Show a hidden column in the table.
     *
     * @param modelColumn the column index from the TableModel of the column to
     * be added
     */
    public void showColumn(int modelColumn) {
        for (TableColumn column : allColumns) {
            if (column.getModelIndex() == modelColumn) {
                showColumn(column);
                break;
            }
        }
    }

    /**
     * Show a hidden column in the table.
     *
     * @param columnName the column name from the TableModel of the column to be
     * added
     */
    public void showColumn(Object columnName) {
        for (TableColumn column : allColumns) {
            if (column.getHeaderValue().equals(columnName)) {
                showColumn(column);
                break;
            }
        }
    }

    /**
     * Show a hidden column in the table. The column will be positioned at its
     * proper place in the view of the table.
     *
     * @param column the TableColumn to be shown.
     */
    private void showColumn(TableColumn column) {
        //  Ignore changes to the TableColumnModel made by the TableColumnManager

        tcm.removeColumnModelListener(this);

        //  Add the column to the end of the table
        tcm.addColumn(column);

        //  Move the column to its position before it was hidden.
        //  (Multiple columns may be hidden so we need to find the first
        //  visible column before this column so the column can be moved
        //  to the appropriate position)
        int position = allColumns.indexOf(column);
        int from = tcm.getColumnCount() - 1;
        int to = 0;

        for (int i = position - 1; i > -1; i--) {
            try {
                TableColumn visibleColumn = allColumns.get(i);
                to = tcm.getColumnIndex(visibleColumn.getHeaderValue()) + 1;
                break;
            } catch (IllegalArgumentException e) {}
        }

        tcm.moveColumn(from, to);

        tcm.addColumnModelListener(this);
    }

    //
    //  Implement MouseListener
    //

    @Override
    public void mousePressed(MouseEvent e) {
        checkForPopup(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        checkForPopup(e);
    }

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}

    private void checkForPopup(MouseEvent e) {
        if (e.isPopupTrigger()) {
            JTableHeader header = (JTableHeader) e.getComponent();
            int column = header.columnAtPoint(e.getPoint());
            showPopup(column);
        }
    }

    /*
     *  Show a popup containing items for all the columns found in the
     *  table column manager. The popup will be displayed below the table
     *  header columns that was clicked.
     *
     *  @param  index  index of the table header column that was clicked
     */
    private void showPopup(int index) {
        Object headerValue = tcm.getColumn(index).getHeaderValue();
        JPopupMenu popup = buildColumnMenu();

        //  Pre-select the item for the column that was clicked.
        for (Component c : popup.getComponents()) {
            if (
                c instanceof JCheckBoxMenuItem &&
                ((JCheckBoxMenuItem) c).getText().equals(headerValue.toString())
            ) {
                popup.setSelected((JCheckBoxMenuItem) c);
                break;
            }
        }

        //  Display the popup below the TableHeader
        JTableHeader header = table.getTableHeader();
        Rectangle r = header.getHeaderRect(index);
        popup.show(header, r.x, r.height);
    }

    /**
     * Build a checkbox popup menu listing every managed column with its current
     * visibility state. Reused by the header right-click and by external UI
     * (e.g. a toolbar "Columns" button). The last visible column's item is
     * disabled so the table can never be emptied.
     *
     * @return a fresh column-selection popup menu
     */
    public JPopupMenu buildColumnMenu() {
        int columnCount = tcm.getColumnCount();
        JPopupMenu popup = new SelectPopupMenu();

        for (TableColumn tableColumn : allColumns) {
            Object value = tableColumn.getHeaderValue();
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(value.toString());
            item.addActionListener(this);

            try {
                tcm.getColumnIndex(value);
                item.setSelected(true);

                if (columnCount == 1) {
                    item.setEnabled(false);
                }
            } catch (IllegalArgumentException e) {
                item.setSelected(false);
            }

            popup.add(item);
        }

        return popup;
    }

    //
    //  Implement ActionListener
    //

    /*
     *  A table column will either be added to the table or removed from the
     *  table depending on the state of the menu item that was clicked.
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        JMenuItem item = (JMenuItem) event.getSource();

        if (item.isSelected()) {
            showColumn(item.getText());
        } else {
            hideColumn(item.getText());
        }

        fireVisibilityChanged();
    }

    //
    //  Implement TableColumnModelListener
    //

    @Override
    public void columnAdded(TableColumnModelEvent e) {
        //  A table column was added to the TableColumnModel so we need
        //  to update the manager to track this column

        TableColumn column = tcm.getColumn(e.getToIndex());

        if (allColumns.contains(column)) {} else {
            allColumns.add(column);
        }
    }

    @Override
    public void columnMoved(TableColumnModelEvent e) {
        if (e.getFromIndex() == e.getToIndex()) {
            return;
        }

        //  A table column has been moved one position to the left or right
        //  in the view of the table so we need to update the manager to
        //  track the new location
        int index = e.getToIndex();
        TableColumn column = tcm.getColumn(index);
        allColumns.remove(column);

        if (index == 0) {
            allColumns.add(0, column);
        } else {
            index--;
            TableColumn visibleColumn = tcm.getColumn(index);
            int insertionColumn = allColumns.indexOf(visibleColumn);
            allColumns.add(insertionColumn + 1, column);
        }
    }

    @Override
    public void columnMarginChanged(ChangeEvent e) {}

    @Override
    public void columnRemoved(TableColumnModelEvent e) {}

    @Override
    public void columnSelectionChanged(ListSelectionEvent e) {}

    //
    //  Implement PropertyChangeListener
    //

    @Override
    public void propertyChange(PropertyChangeEvent e) {
        if ("model".equals(e.getPropertyName())) {
            if (table.getAutoCreateColumnsFromModel()) {
                reset();
            }
        }
    }

    /*
     *  Allows you to select a specific menu item when the popup is
     *  displayed. (ie. this is a bug? fix)
     */
    class SelectPopupMenu extends JPopupMenu {

        @Override
        public void setSelected(Component sel) {
            int index = getComponentIndex(sel);
            getSelectionModel().setSelectedIndex(index);
            final MenuElement me[] = new MenuElement[2];
            me[0] = (MenuElement) this;
            me[1] = getSubElements()[index];

            SwingUtilities.invokeLater(
                new Runnable() {

                    @Override
                    public void run() {
                        MenuSelectionManager.defaultManager().setSelectedPath(me);
                    }
                }
            );
        }
    }
}

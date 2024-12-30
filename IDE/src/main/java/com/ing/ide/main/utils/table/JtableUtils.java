
package com.ing.ide.main.utils.table;

import com.ing.ide.main.utils.keys.Keystroke;
import com.ing.ide.util.Canvas;
import com.google.common.primitives.Ints;
import java.awt.HeadlessException;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import static javax.swing.JComponent.WHEN_FOCUSED;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Add-on functions for the <code>JTable</code>
 *
 */
public class JtableUtils {

    public static TableColumn column = null;
    public static JPopupMenu renamePopup = null;
    public static JTableHeader head = null;
    public static Object[][] nullRow = null;
    private static final String LINE_BREAK = "\n";
    private static final String CELL_BREAK = "\t";
    private static final Clipboard CLIPBOARD = Toolkit.getDefaultToolkit().getSystemClipboard();
    public static final JMenuItem COPY = new JMenuItem("Copy");
    public static final JMenuItem CUT = new JMenuItem("Cut");
    public static final JMenuItem PASTE = new JMenuItem("Paste");

    static {        
        
        COPY.setIcon(new Canvas.EmptyIcon());
        COPY.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {                           
                JPopupMenu p = (JPopupMenu) COPY.getParent();
                Object val = p.getClientProperty("Table");
                if (val != null) {
                    JTable tble = (JTable) val;
                    JtableUtils.cancelEditing(tble);
                    JtableUtils.copyToClipboard(false, tble);
                }
            }
        });
        COPY.setAccelerator(Keystroke.COPY);
        CUT.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                JPopupMenu p = (JPopupMenu) CUT.getParent();
                Object val = p.getClientProperty("Table");
                if (val != null) {
                    JTable tble = (JTable) val;
                    JtableUtils.cancelEditing(tble);
                    JtableUtils.copyToClipboard(true, tble);
                }
            }
        });
        CUT.setAccelerator(Keystroke.CUT);
        PASTE.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                JPopupMenu p = (JPopupMenu) PASTE.getParent();
                Object val = p.getClientProperty("Table");
                if (val != null) {
                    JTable tble = (JTable) val;
                    JtableUtils.cancelEditing(tble);
                    JtableUtils.pasteFromClipboard(tble);
                }
            }
        });
        PASTE.setAccelerator(Keystroke.PASTE);
    }

    /**
     * Adds the key listeners for <code>CUT,COPY,PASTE,add new Row </code> and
     * <code>DELETE</code>
     *
     * @param table to add the add-on functionalities
     * @param hasFixedRows flag to know whether the table has fixed rows are not
     */
    public static void addlisteners(final JTable table, Boolean hasFixedRows) {
        table.setSurrendersFocusOnKeystroke(false);  
        table.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.isControlDown() || e.isMetaDown() && !e.isShiftDown() && !e.isAltDown()) {
               // if ((e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0 || (e.getModifiersEx() & KeyEvent.META_DOWN_MASK) != 0){
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_C:
                            cancelEditing(table);
                            copyToClipboard(false, table);
                            break;
                        case KeyEvent.VK_X:
                            cancelEditing(table);
                            copyToClipboard(true, table);
                            break;
                        case KeyEvent.VK_V:
                            cancelEditing(table);
                            pasteFromClipboard(table);
                            break;
                        case KeyEvent.VK_D:
                            cancelEditing(table);
                            pasteFromAbove(table);
                            break;
                        default:
                            break;
                    }

                }
            }
        });

        if (!hasFixedRows) {
            Object val = table.getClientProperty("Popup");
            if (val != null) {
                table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(CUT.getAccelerator(), "cut");
                final JPopupMenu pop = (JPopupMenu) val;
                pop.addPopupMenuListener(new PopupMenuListener() {
                    @Override
                    public void popupMenuWillBecomeVisible(PopupMenuEvent pme) {
                        pop.add(CUT);
                        pop.add(COPY);
                        pop.add(PASTE);
                    }

                    @Override
                    public void popupMenuCanceled(PopupMenuEvent pme) {
                    }

                    @Override
                    public void popupMenuWillBecomeInvisible(PopupMenuEvent pme) {

                    }
                });
            }
            table.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent event) {
                    addEmptyRowatEnd(table);
                }
            });
            table.addKeyListener(new KeyAdapter() {

                @Override
                public void keyTyped(KeyEvent e) {
                    addEmptyRowatEnd(table);
                }
            });
        }
        clearTableSelectionOnDelete(table);
    }

    public static void pasteFromAbove(JTable table) {
        int startRow = table.getSelectedRows()[0];
        int[] cols = table.getSelectedColumns();
        for (int col : cols) {
            table.setValueAt(table.getValueAt(startRow - 1, col), startRow, col);
        }
    }

    /**
     * clear selection by setting empty values
     *
     * @param table to be cleared
     */
    private static void ClearSelection(JTable table) {
        int[] srow = table.getSelectedRows();
        int[] scol = table.getSelectedColumns();
        int lastSrow = srow.length;
        int lastScol = scol.length;
        for (int i = 0; i < lastSrow; i++) {
            for (int j = 0; j < lastScol; j++) {
                if (table.isCellEditable(srow[i], scol[j])) {
                    table.setValueAt("", srow[i], scol[j]);
                }
            }
        }
    }

    /**
     * Reads the cell values of selected cells of the <code>tmodel</code> and
     * uploads into clipboard in supported format
     *
     * @param isCut CUT flag,<code>true</code> for CUT and <code>false</code>
     * for COPY
     * @param table the source for the action
     * @see #escape(java.lang.Object)
     */
    private static void copyToClipboard(boolean isCut, JTable table) {
        try {
            int numCols = table.getSelectedColumnCount();
            int numRows = table.getSelectedRowCount();
            int[] rowsSelected = table.getSelectedRows();
            int[] colsSelected = table.getSelectedColumns();
            if (numRows != rowsSelected[rowsSelected.length - 1] - rowsSelected[0] + 1 || numRows != rowsSelected.length
                    || numCols != colsSelected[colsSelected.length - 1] - colsSelected[0] + 1 || numCols != colsSelected.length) {
                JOptionPane.showMessageDialog(null, "Invalid Selection", "Invalid Selection", JOptionPane.ERROR_MESSAGE);
                return;
            }
            StringBuilder excelStr = new StringBuilder();
            for (int i = 0; i < numRows; i++) {
                for (int j = 0; j < numCols; j++) {
                    excelStr.append(escape(table.getValueAt(rowsSelected[i], colsSelected[j])));
                    if (isCut) {
                        if (table.isCellEditable(rowsSelected[i], colsSelected[j])) {
                            table.setValueAt("", rowsSelected[i], colsSelected[j]);
                        }
                    }
                    if (j < numCols - 1) {
                        excelStr.append(CELL_BREAK);
                    }
                }
                if (i < numRows - 1) {
                    excelStr.append(LINE_BREAK);
                }
            }
            if (!excelStr.toString().isEmpty()) {
                StringSelection sel = new StringSelection(excelStr.toString());
                CLIPBOARD.setContents(sel, sel);
            }

        } catch (HeadlessException ex) {
            Logger.getLogger(JtableUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Reads clipboard data and converts it into supported format and fills the
     * tmodel cells
     *
     * @param table the target tmodel
     */
    private static void pasteFromClipboard(JTable table) {
        int startRow = table.getSelectedRows()[0];
        int startCol = table.getSelectedColumns()[0];
        String pasteString;
        try {
            pasteString = (String) (CLIPBOARD.getContents(CLIPBOARD).getTransferData(DataFlavor.stringFlavor));
        } catch (UnsupportedFlavorException | IOException ex) {
            Logger.getLogger(JtableUtils.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        String[] lines = pasteString.split(LINE_BREAK);
        for (int i = 0; i < lines.length; i++) {
            String[] cells = lines[i].split(CELL_BREAK);
            if (table.getRowCount() <= startRow + i) {
                ((DefaultTableModel) table.getModel()).addRow(nullRow);
            }
            for (int j = 0; j < cells.length; j++) {
                if (table.getColumnCount() > startCol + j) {
                    if (table.isCellEditable(startRow + i, startCol + j)) {
                        table.setValueAt(cells[j], startRow + i, startCol + j);
                    }
                }
            }
        }
    }

    /**
     * stops the tmodel cell editing
     *
     * @param table the target tmodel
     */
    public static void cancelEditing(JTable table) {
        if (table.getCellEditor() != null) {
            table.getCellEditor().cancelCellEditing();
        }
    }

    public static void stopEditing(JTable table) {
        if (null != table.getCellEditor()) {
            table.getCellEditor().stopCellEditing();
        }
    }

    /**
     * removes the <code>LINE_BREAK</code> and <code>CELL_BREAK</code>
     *
     * @param cell data to be escaped
     * @return the escaped data
     */
    private static String escape(Object cell) {
        if (cell != null) {
            return cell.toString().replace(LINE_BREAK, " ").replace(CELL_BREAK, " ");
        } else {
            return "";
        }
    }

    /**
     * Adds input action map for the tmodel<p>
     * needed as <code>OnKeyPress Edit</code> in autosuggest overriding the
     * basic <code>Delete</code> key press
     *
     * @param table the target tmodel
     */
    private static void clearTableSelectionOnDelete(final JTable table) {
        InputMap inputMap = table.getInputMap(WHEN_FOCUSED);
        ActionMap actionMap = table.getActionMap();
        inputMap.put(Keystroke.DELETE, "delete");
        actionMap.put("delete", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent evt) {
                cancelEditing(table);
                ClearSelection(table);
            }

        });
    }

    /**
     * Moves the selected nullRoww one nullRoww UP., unless it the first
     * nullRoww
     *
     * @param table the target tmodel
     */
    public static void moverowup(JTable table) {
        int sRow = table.getSelectedRow();
        if (sRow > 0) {
            ((DefaultTableModel) table.getModel()).moveRow(sRow, sRow, sRow - 1);
            table.getSelectionModel().setSelectionInterval(sRow - 1, sRow - 1);
        }

    }
    
    public static void moveColumnLeft(JTable table) {
        try {
            TableColumnModel tcm = table.getColumnModel();
            int sCol  = table.getSelectedColumn();
            
            tcm.moveColumn(sCol, sCol-1);

        } catch (Exception e) {
            Logger.getLogger(JtableUtils.class.getName()).log(Level.SEVERE, null, e);
        }

    }
    
    public static void moveColumnRight(JTable table) {
        try {
            TableColumnModel tcm = table.getColumnModel();
            int sCol  = table.getSelectedColumn();
            
            tcm.moveColumn(sCol, sCol+1);

        } catch (Exception e) {
            Logger.getLogger(JtableUtils.class.getName()).log(Level.SEVERE, null, e);
        }

    }

    /**
     * Moves the selected nullRoww one nullRoww DOWN., unless it is the last
     * nullRoww
     *
     * @param table the target tmodel
     */
    public static void moverowdown(JTable table) {
        int sRow = table.getSelectedRow();
        if (sRow < table.getRowCount() - 1 && sRow >= 0) {
            ((DefaultTableModel) table.getModel()).moveRow(sRow, sRow, sRow + 1);
            table.getSelectionModel().setSelectionInterval(sRow + 1, sRow + 1);
        }
    }

    /**
     * Overriding the default re-ordering functionality.<p>
     * Re-ordering allowed if both of the i.e <code>columnIndex,newIndex</code>
     * are outside the limit.
     *
     * @param table the target tmodel
     * @param lmt re-ordering limit
     */
    public static void setReorderColumn(JTable table, final int lmt) {
        table.setColumnModel(new DefaultTableColumnModel() {
            private static final long serialVersionUID = 1L;

            @Override
            public void moveColumn(int columnIndex, int newIndex) {
                if (columnIndex <= lmt) {
                    return;
                } else if (newIndex <= lmt) {
                    return;
                }
                super.moveColumn(columnIndex, newIndex);
            }
        });
    }

    /**
     * Loads the Data Array into the tmodel
     *
     * @param table to be populated
     * @param header column header
     * @param rows nullRoww data
     * @return populated tmodel
     */
    public static JTable populatetable(JTable table, String[] header, List<String[]> rows) {
        removeRowSelection(table);
        DefaultTableModel tablemodel = (DefaultTableModel) table.getModel();
        tablemodel.setRowCount(0);
        for (String col : header) {
            tablemodel.addColumn(col);
        }
        for (String[] row : rows) {
            tablemodel.addRow(row);
        }
        table.setModel(tablemodel);
        return table;
    }

    /**
     * Loads the Data Array into the tmodel with custom datatype
     *
     * @param table to be populated
     * @param header column header
     * @param rows nullRoww data
     * @return populated tmodel
     */
    public static void populatetable(JTable table, List<String[]> rows) {
        removeRowSelection(table);
        DefaultTableModel tablemodel = (DefaultTableModel) table.getModel();
        table.setRowSorter(null);
        tablemodel.setRowCount(0);
        for (String[] row : rows) {
            int colsize = row.length;
            Object[] newRow = new Object[colsize];
            for (int col = 0; col < colsize; col++) {
                newRow[col] = col > 0 ? row[col] : Boolean.valueOf(row[0]);
            }
            tablemodel.addRow(newRow);
        }
        table.setModel(tablemodel);
    }

    /**
     * Removes all unnecessary null Rows
     *
     * @param table to remove unnecessary null Rows
     */
    public static void removeEmptyRows(JTable table, int... excludeColumns) {
        removeEmptyRowsFromModel(table, excludeColumns);
    }

    /**
     * writes the table model data into the output-file while omitting the empty
     * columns and Rows
     *
     * @param table tmodel to be saved
     * @param file output file
     * @param hflag whether to write header also
     * @throws IOException
     */
    public static void exportToCSV(JTable table, File file, Boolean hflag) throws IOException {
        exportModelToCSV(table, file, hflag);

    }

    public static void exportModelToCSV(JTable table, File file, Boolean hflag) throws IOException {
        removeEmptyRows(table);
        cancelEditing(table);
        ArrayList<String[]> data = new ArrayList<>();
        ArrayList<Integer> expList = new ArrayList<>();
        ArrayList<String> row = new ArrayList<>();
        int colCount = table.getColumnModel().getColumnCount(),
                rowCount = table.getModel().getRowCount(), i;
        for (i = 0; i < colCount; i++) {
            Object hval = table.getColumnName(i);
            if (hval == null || "".equals(hval.toString())) {
                expList.add(i);
            } else {
                row.add(hval.toString());
            }
        }
        data.add(row.toArray(new String[row.size()]));
        for (i = 0; i < rowCount; i++) {
            row.clear();
            for (int j = 0; j < colCount; j++) {
                if (!expList.contains(j)) {
                    Object val = table.getModel().getValueAt(i, j);
                    row.add((val == null) ? "" : val.toString());
                }
            }
            data.add(row.toArray(new String[row.size()]));
        }
//        try (CSVWriter<String[]> csvWriter = CSVWriterBuilder.newDefaultWriter(new FileWriter(file))) {
//            csvWriter.writeAll(data);
//        }

    }

    public static void removeEmptyRowsFromModel(JTable table, int... excludeColumns) {
        int rCount = table.getModel().getRowCount();
        int cCount = table.getColumnModel().getColumnCount();

        stopEditing(table);
        List<Integer> excludeList = new ArrayList<>();
        if (excludeColumns != null && excludeColumns.length > 0) {
            for (int excludeColumn : excludeColumns) {
                excludeList.add(excludeColumn);
            }
        }
        try {
            for (int i = rCount - 1; i >= 0; i--) {
                Boolean empty = true;
                for (int j = 0; j < cCount; j++) {
                    if (!excludeList.contains(j)) {
                        if (!Objects.toString(table.getModel().getValueAt(i, j), "").isEmpty()) {
                            empty = false;
                            break;
                        }
                    }
                }
                if (empty) {
                    ((DefaultTableModel) table.getModel()).removeRow(i);
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(JtableUtils.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    /**
     * returns the list of duplicated rows customized for test set Table
     *
     * @param table
     * @return the list
     *
     */
    public static ArrayList<Integer> getDuplicateRows(JTable table) {
        removeEmptyRows(table);
        cancelEditing(table);
        ArrayList<Integer> dRows = new ArrayList<>();
        HashMap<String, Integer> uniqRows = new HashMap<>();
        List<Integer> dontCheckIndex = Arrays.asList(new Integer[]{0, 4});//left the first column [ececute] and 5 column status
        int colCount = table.getColumnCount(),
                rowCount = table.getModel().getRowCount(), i;
        for (i = 0; i < rowCount; i++) {
            String row = "";
            for (int j = 0; j < colCount; j++) {
                if (dontCheckIndex.contains(j)) {
                    continue;
                }
                Object val = table.getValueAt(i, j);
                row += ((val == null) ? "" : val.toString());
            }
            if (uniqRows.containsKey(row)) {
                String exe = table.getValueAt(i, dontCheckIndex.get(0)).toString();
                String status = table.getValueAt(i, dontCheckIndex.get(1)).toString();
                if ("false".equals(exe)) {
                    dRows.add(i);
                } else if ("norun".equalsIgnoreCase(status)) {
                    dRows.add(i);
                } else {
                    dRows.add(uniqRows.get(row));
                    uniqRows.put(row, i);
                }
            } else {
                uniqRows.put(row, i);
            }
        }

        return dRows;
    }

    /**
     * removes the given list of rows from the table
     *
     * @param table
     * @param rows
     */
    public static void removeRows(JTable table, ArrayList<Integer> rows) {
        /**
         * removing rows from last prevent row index change at run time
         */
        Collections.sort(rows, Collections.reverseOrder());
        for (int row : rows) {
            try {
                ((DefaultTableModel) table.getModel()).removeRow(row);
            } catch (Exception ex) {
                Logger.getLogger(JtableUtils.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    /**
     * if the selected row is last row then add an empty row at the end
     *
     * @param table the tmodel to add new empty row
     */
    static void addEmptyRowatEnd(JTable table) {
        if ((table.getRowCount() - 1) == table.getSelectedRow()) {
            ((DefaultTableModel) table.getModel()).addRow(nullRow);
        }
    }

    /**
     * get all selected rows and DELETE one by one from the last.
     *
     * @param table the table to DELETE rows
     */
    public static void deleterow(JTable table) {
        int[] selectedrows = table.getSelectedRows();
        for (int row : selectedrows) {
            row = table.getSelectedRow();
            if (table.getRowSorter() != null) {
                row = table.getRowSorter().convertRowIndexToModel(row);
            }
            ((DefaultTableModel) table.getModel()).removeRow(row);
        }

    }

    /**
     * deletes all selected columns if it is not present in the <code>exp</code>
     * List
     *
     * @param table the table to DELETE columns
     * @param exp columns to avoid deleting
     * @see #deletecol(javax.swing.JTable, int)
     */
    static void deletecols(JTable table, int[] exp) {
        Integer[] selcols;
        try {
            TableColumnModel tcm = table.getColumnModel();
            selcols = ArrayUtils.toObject(table.getSelectedColumns());
            Arrays.sort(selcols, Collections.reverseOrder());
            List<Integer> explist = Ints.asList(exp);
            for (int i : selcols) {
                if (!explist.contains(i)) {
                    tcm.removeColumn(tcm.getColumn(i));
                }
            }

        } catch (Exception e) {
            Logger.getLogger(JtableUtils.class.getName()).log(Level.SEVERE, null, e);
        }

    }

    /**
     * create new header vector for modified tableModel * @param table target
     * table
     *
     * @deprecated
     * @param col target column
     * @return the modified column model
     */
    static Vector<String> getColumnIdentifiersremovecol(JTable table, int col) {
        Vector<String> columnIdentifiers = new Vector<>();
        int j = 0, colCount = table.getColumnCount();
        while (j < colCount) {
            columnIdentifiers.add(table.getColumnName(j));
            j++;
        }
        columnIdentifiers.remove(col);
        return columnIdentifiers;
    }

    /**
     * create new data data vector for modified tableModel
     *
     * @deprecated
     * @param source source data vector
     * @param col target column to DELETE
     * @return the modified data vector
     */
    static Vector<Vector<?>> newvector(Vector<?> source, int col) {
        Vector<Vector<?>> result = new Vector<>();
        try {
            Vector<?> row;
            Object[] data = source.toArray();
            int i = 0, rowCount = data.length;
            while (i < rowCount) {
                row = (Vector) data[i];
                row.remove(col);
                result.add(row);
                i++;
            }
        } catch (Exception e) {
            Logger.getLogger(JtableUtils.class.getName()).log(Level.SEVERE, null, e);
        }
        return result;
    }

    /**
     * Adding column is done by creating new model by modifying older one.<p>
     *
     * Insert new column if column is outside the <code>limit</code> Adds new
     * column if selected column inside the <code>limit</code>table@param _table
     * target table
     *
     * @param limit the range to avoid inserting
     */
    static void addcol(JTable table, int limit) {
        try {
            int sc = table.getSelectedColumn();
            if (sc < limit - 1) {
                sc = table.getColumnCount() - 1;
            }

            DefaultTableModel tableM = (DefaultTableModel) table.getModel();
            DefaultTableModel tableM1 = new DefaultTableModel();
            TableModelListener[] listeners = tableM.getTableModelListeners();

            tableM1.setDataVector(newvectoraddcol(tableM.getDataVector(), sc), getColumnIdentifiersaddcol(sc + 1, table));
            table.setModel(tableM1);
            for (TableModelListener l : listeners) {
                tableM1.addTableModelListener(l);
            }

        } catch (Exception ex) {
            Logger.getLogger(JtableUtils.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    /**
     * creates modified column model
     *
     * @param scol position to insert new column
     * @param table target table
     * @return modified column model
     */
    private static Vector<String> getColumnIdentifiersaddcol(int scol, JTable table) {
        Vector<String> columnIdentifiers = new Vector<>();
        int j = 0;
        while (j < table.getColumnCount()) {
            columnIdentifiers.add(table.getColumnName(j));
            j++;
        }
        columnIdentifiers.add(scol, "");
        return columnIdentifiers;
    }

    /**
     * create new data data vector for modified tableModel
     *
     * @param source source data vector
     * @param col target column to insert
     * @return the modified data vector
     */
    private static Vector<Vector<?>> newvectoraddcol(Vector<?> source, int j) {
        Vector<Vector<?>> result = new Vector<>();
        try {
            Vector<?> row;
            Object[] data = source.toArray();
            int i = 0;
            while (i < data.length) {
                row = (Vector) data[i];
                row.add(j + 1, null);
                result.add(row);
                i++;
            }
        } catch (Exception ex) {
            Logger.getLogger(JtableUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        return result;
    }

    /**
     * Inserts new row if any row is selected else adds new row at the end
     *
     * @param table
     */
    public static void addrow(JTable table) {
        int rowindex = table.getRowCount();
        if (table.getSelectedRow() > -1) {
            ((DefaultTableModel) table.getModel()).addRow(nullRow);
            ((DefaultTableModel) table.getModel()).moveRow(table.getRowCount() - 1, table.getRowCount() - 1, table.getSelectedRow());
            rowindex = table.getSelectedRow();
        } else {
            ((DefaultTableModel) table.getModel()).addRow(nullRow);
        }
        table.scrollRectToVisible(new Rectangle(table.getCellRect(rowindex, 0, true)));
    }

    /**
     * replace the new column name at the specified index
     *
     * @param table target table
     * @param index target index
     * @param text new name
     * @return the modified column model
     */
    static Vector<String> getColumnIdentifiers(JTable table, int index, String text) {//create new header vector for modified tableM
        Vector<String> columnIdentifiers = new Vector<>();
        int j = 0;
        while (j < table.getColumnCount()) {
            if (j == index) {
                columnIdentifiers.add(text);

            } else {
                columnIdentifiers.add(table.getColumnName(j));
            }
            j++;
        }
        return columnIdentifiers;
    }

    /**
     * check whether the column with the <code>text</code> present or not
     *
     * @param table target table
     * @param name name to check
     * @return <code>true</code> if present else <code>false</code>
     */
    public static Boolean checkColumn(JTable table, String name) {

        int j = 0;
        while (j < table.getColumnCount()) {

            if (table.getColumnName(j).equals(name)) {
                return true;
            }
            j++;
        }
        return false;
    }

    /**
     * check whether the  <code>row</code> present or not
     *
     * @param table target table
     * @param row target row
     * @return  <code>true</code> if present else <code>false</code>
     */
    static boolean checkforduplicaterow(JTable table, Object[] row) {
        String value = "", value1 = "";
        int cRow = table.getRowCount(), cCol = table.getColumnCount();
        for (Object cell : row) {
            value1 += cell != null ? cell.toString() : "";
        }
        for (int i = 0; i < cRow; i++) {
            for (int j = 0; j < cCol; j++) {
                Object tmp = table.getModel().getValueAt(i, j);
                if (j == 4) {
                    tmp = "NoRun";
                }
                value += tmp != null ? tmp.toString() : "";
            }
            if (value.equals(value1)) {
                return true;
            }
            value = "";
        }
        return false;
    }

    /**
     * Removes the  <code>row</code> selection
     *
     * @param table target table
     */
    public static void removeRowSelection(JTable table) {
        if (table.getRowCount() > 0) {
            table.removeRowSelectionInterval(0, table.getRowCount() - 1);
        }
    }

    public static void clearTable(JTable table) {
        stopEditing(table);
        ((DefaultTableModel) table.getModel()).setRowCount(0);
    }

    public static void emptyTable(JTable table) {
        emptyTable(table, -100);
    }

    public static void emptyTable(JTable table, int column) {
        for (int i = 0; i < table.getRowCount(); i++) {
            for (int j = 0; j < table.getColumnCount(); j++) {
                try {
                    if (column != j) {
                        table.setValueAt(null, i, j);
                    }
                } catch (Exception ex) {
                    Logger.getLogger(JtableUtils.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    

}

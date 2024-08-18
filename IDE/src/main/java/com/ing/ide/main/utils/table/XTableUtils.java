
package com.ing.ide.main.utils.table;

import com.ing.datalib.component.DataModel;
import com.ing.datalib.undoredo.UndoRedoModel;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JTable;

/**
 *
 * 
 */
public class XTableUtils {

    private static final String LINE_BREAK = "\n";
    private static final String CELL_BREAK = "\t";
    private static final Clipboard CLIPBOARD = Toolkit.getDefaultToolkit().getSystemClipboard();

    public static void copyToClipboard(JTable table, boolean isCut) {
        int numCols = table.getSelectedColumnCount();
        int numRows = table.getSelectedRowCount();
        int[] rowsSelected = table.getSelectedRows();
        int[] colsSelected = table.getSelectedColumns();
        if (numRows != rowsSelected[rowsSelected.length - 1] - rowsSelected[0] + 1 || numRows != rowsSelected.length
                || numCols != colsSelected[colsSelected.length - 1] - colsSelected[0] + 1 || numCols != colsSelected.length) {

            Logger.getLogger(XTableUtils.class.getName()).info("Invalid Copy Selection");
            return;
        }
        if (table.getModel() instanceof UndoRedoModel) {
            ((UndoRedoModel) table.getModel()).startGroupEdit();
        }

        StringBuilder excelStr = new StringBuilder();

        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
                excelStr.append(escape(table.getValueAt(rowsSelected[i], colsSelected[j])));
                if (isCut) {
                    table.setValueAt("", rowsSelected[i], colsSelected[j]);
                }
                if (j < numCols - 1) {
                    excelStr.append(CELL_BREAK);
                }
            }
            excelStr.append(LINE_BREAK);
        }

        if (table.getModel() instanceof UndoRedoModel) {
            ((UndoRedoModel) table.getModel()).stopGroupEdit();
        }

        StringSelection sel = new StringSelection(excelStr.toString());

        CLIPBOARD.setContents(sel, sel);
    }

    private static String escape(Object cell) {
        return Objects.toString(cell, "").replace(LINE_BREAK, " ").replace(CELL_BREAK, " ");
    }

    public static void pasteFromClipboard(JTable table) {
        int startRow = table.getSelectedRows()[0];
        int startCol = table.getSelectedColumns()[0];

        String pasteString;
        try {
            pasteString = (String) (CLIPBOARD.getContents(null).getTransferData(DataFlavor.stringFlavor));
        } catch (Exception e) {
            Logger.getLogger(XTableUtils.class.getName()).log(Level.WARNING, "Invalid Paste Type", e);
            return;
        }
        if (table.getModel() instanceof UndoRedoModel) {
            ((UndoRedoModel) table.getModel()).startGroupEdit();
        }
        String[] lines = pasteString.split(LINE_BREAK);

        for (int i = 0; i < lines.length; i++) {
            String[] cells = lines[i].split(CELL_BREAK);
            for (int j = 0; j < cells.length; j++) {
                if (table.getRowCount() <= startRow + i) {
                    if (table.getModel() instanceof DataModel) {
                        if (!((DataModel) table.getModel()).addRow()) {
                            return;
                        }
                    }
                }
                if (table.getRowCount() > startRow + i && table.getColumnCount() > startCol + j) {
                    table.setValueAt(cells[j], startRow + i, startCol + j);
                }
            }
        }
        if (table.getModel() instanceof UndoRedoModel) {
            ((UndoRedoModel) table.getModel()).stopGroupEdit();
        }
    }
}

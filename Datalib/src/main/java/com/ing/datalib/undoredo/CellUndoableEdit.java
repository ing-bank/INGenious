
package com.ing.datalib.undoredo;

import javax.swing.table.AbstractTableModel;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 *
 * 
 */
public class CellUndoableEdit extends CommonUndoableEdit {

    private final int row, column;
    private final Object oldValue, newValue;

    public CellUndoableEdit(AbstractTableModel model, UndoRedoProgress progress,
            int row, int column, Object oldValue, Object newValue) {
        super(model, progress);
        this.row = row;
        this.column = column;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    @Override
    public void redo() throws CannotRedoException {
        startProgress();
        getModel().setValueAt(newValue, row, column);
        super.redo();
        stopProgress();
    }

    @Override
    public void undo() throws CannotUndoException {
        startProgress();
        getModel().setValueAt(oldValue, row, column);
        super.undo();
        stopProgress();
    }

}

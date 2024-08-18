
package com.ing.datalib.undoredo;

import javax.swing.table.AbstractTableModel;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 *
 * 
 */
public class RowUndoableEdit extends CommonUndoableEdit {

    private final int row;
    private final Object[] values;

    private final Boolean added;

    public RowUndoableEdit(AbstractTableModel model, UndoRedoProgress progress,
            int row, Boolean added) {
        super(model, progress);
        this.row = row;
        this.values = new Object[getModel().getColumnCount()];
        for (int i = 0; i < values.length; i++) {
            values[i] = getModel().getValueAt(row, i);
        }
        this.added = added;
    }

    public RowUndoableEdit(AbstractTableModel model, UndoRedoProgress progress,
            int row, Object[] values, Boolean added) {
        super(model, progress);
        this.row = row;
        this.values = values;
        this.added = added;
    }

    @Override
    public void redo() throws CannotRedoException {
        startProgress();
        if (added) {
            getModel().insertRow(row, values);
        } else {
            getModel().removeRow(row);
        }
        super.redo();
        stopProgress();
    }

    @Override
    public void undo() throws CannotUndoException {
        startProgress();
        if (added) {
            getModel().removeRow(row);
        } else {
            getModel().insertRow(row, values);
        }
        super.undo();
        stopProgress();
    }

    @Override
    public final UndoRedoModel getModel() {
        return (UndoRedoModel) super.getModel();
    }

}

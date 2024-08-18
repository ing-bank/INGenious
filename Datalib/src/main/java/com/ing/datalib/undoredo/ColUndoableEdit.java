
package com.ing.datalib.undoredo;

import javax.swing.table.AbstractTableModel;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 *
 * 
 */
public class ColUndoableEdit extends CommonUndoableEdit {

    private final String colName;
    private final int colIndex;
    private final Object[] values;

    private final Boolean added;

    public ColUndoableEdit(AbstractTableModel model, UndoRedoProgress progress,
            String colName, int colIndex, Boolean added) {
        super(model, progress);
        this.colName = colName;
        this.colIndex = colIndex;
        this.added = added;
        this.values = new Object[model.getRowCount()];
        loadValuesFromModel();
    }

    public ColUndoableEdit(AbstractTableModel model, UndoRedoProgress progress,
            int colIndex, Boolean added) {
        super(model, progress);
        this.colName = getModel().getColumnName(colIndex);
        this.colIndex = colIndex;
        this.added = added;
        this.values = new Object[model.getRowCount()];
        loadValuesFromModel();
    }

    public ColUndoableEdit(AbstractTableModel model, UndoRedoProgress progress,
            String colName, Boolean added) {
        super(model, progress);
        this.colName = colName;
        this.colIndex = getModel().findColumn(colName);
        this.added = added;
        this.values = new Object[model.getRowCount()];
        loadValuesFromModel();
    }

    private void loadValuesFromModel() {
        for (int i = 0; i < values.length; i++) {
            values[i] = getModel().getValueAt(i, colIndex);
        }
    }

    @Override
    public void redo() throws CannotRedoException {
        startProgress();
        if (added) {
            getModel().insertColumnAt(colIndex, colName, values);
        } else {
            getModel().removeColumn(colIndex);
        }
        super.redo();
        stopProgress();
    }

    @Override
    public void undo() throws CannotUndoException {
        startProgress();
        if (added) {
            getModel().removeColumn(colIndex);
        } else {
            getModel().insertColumnAt(colIndex, colName, values);
        }
        super.undo();
        stopProgress();
    }

    @Override
    public final UndoRedoModel getModel() {
        return (UndoRedoModel) super.getModel();
    }
}

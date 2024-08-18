
package com.ing.datalib.undoredo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.swing.table.AbstractTableModel;

/**
 *
 * 
 */
public abstract class UndoRedoModel extends AbstractTableModel {

    private final CommonUndoManager undoManager;

    public UndoRedoModel() {
        undoManager = new CommonUndoManager();
    }

    @JsonIgnore
    public CommonUndoManager getUndoManager() {
        return undoManager;
    }

    public void clearUndoRedo() {
        undoManager.clearEdits();
    }

    @Override
    public void setValueAt(Object value, int row, int column) {
        Object oldValue = getValueAt(row, column);
        undoManager.addEdit(new CellUndoableEdit(
                this, undoManager.getProgress(),
                row, column, oldValue, value));
    }

    public void clearValues(int[] rows, int columns[]) {
        startGroupEdit();
        for (int row : rows) {
            for (int column : columns) {
                setValueAt("", row, column);
            }
        }
        stopGroupEdit();
    }

    public void startGroupEdit() {
        undoManager.startGroupEdit();
    }

    public void stopGroupEdit() {
        undoManager.stopGroupEdit();
    }

    public void rowDeleted(int row) {
        undoManager.addEdit(new RowUndoableEdit(
                this, undoManager.getProgress(), row, false));
    }

    public void rowAdded(int row) {
        undoManager.addEdit(new RowUndoableEdit(
                this, undoManager.getProgress(), row, true));
    }

    public void columnAdded(String name) {
        undoManager.addEdit(new ColUndoableEdit(
                this, undoManager.getProgress(), name, true));
    }

    public void columnRemoved(int index) {
        undoManager.addEdit(new ColUndoableEdit(
                this, undoManager.getProgress(), index, false));
    }

    public abstract void removeRow(int row);

    public abstract void insertRow(int row, Object[] values);

    public abstract void insertColumnAt(int colIndex, String colName, Object[] values);

    public abstract void removeColumn(int colIndex);

}

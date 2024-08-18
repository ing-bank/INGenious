
package com.ing.datalib.undoredo;

import javax.swing.table.AbstractTableModel;
import javax.swing.undo.AbstractUndoableEdit;

/**
 *
 * 
 */
public class CommonUndoableEdit extends AbstractUndoableEdit {

    private final AbstractTableModel model;
    private final UndoRedoProgress progress;

    public CommonUndoableEdit(AbstractTableModel model, UndoRedoProgress progress) {
        this.model = model;
        this.progress = progress;
    }

    protected void startProgress() {
        progress.start();
    }

    protected void stopProgress() {
        progress.stop();
    }

    public AbstractTableModel getModel() {
        return model;
    }

}

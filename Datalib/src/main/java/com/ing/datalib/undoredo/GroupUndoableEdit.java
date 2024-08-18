
package com.ing.datalib.undoredo;

import java.util.Collections;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;

/**
 *
 * 
 */
public class GroupUndoableEdit extends CommonUndoableEdit {

    private final List<UndoableEdit> groupEdits;

    public GroupUndoableEdit(AbstractTableModel model, UndoRedoProgress progress,
            List<UndoableEdit> groupEdits) {
        super(model, progress);
        Collections.reverse(groupEdits);
        this.groupEdits = groupEdits;
    }

    @Override
    public synchronized void redo() throws CannotRedoException {
        startProgress();
        for (int i = groupEdits.size() - 1; i >= 0; i--) {
            groupEdits.get(i).redo();
        }
        super.redo();
        stopProgress();
    }

    @Override
    public synchronized void undo() throws CannotUndoException {
        startProgress();
        for (int i = 0; i < groupEdits.size(); i++) {
            groupEdits.get(i).undo();
        }
        super.undo();
        stopProgress();
    }

}

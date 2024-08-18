
package com.ing.datalib.undoredo;

import java.util.ArrayList;
import java.util.List;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

/**
 *
 * 
 */
public class CommonUndoManager extends UndoManager {

    private final UndoRedoProgress progress;

    private final List<UndoableEdit> groupEdits;

    private Boolean isGroupEdit = false;

    public CommonUndoManager() {
        progress = new UndoRedoProgress();
        groupEdits = new ArrayList<>();
    }

    public void startGroupEdit() {
        groupEdits.clear();
        isGroupEdit = true;
    }

    @Override
    public synchronized boolean addEdit(UndoableEdit ue) {
        if (!progress.isInProgress()) {
            if (isGroupEdit) {
                groupEdits.add(ue);
                return false;
            } else {
                return super.addEdit(ue);
            }
        }
        return false;
    }

    public void stopGroupEdit() {
        isGroupEdit = false;
        addEdit(new GroupUndoableEdit(null, progress, groupEdits));
    }

    public void removeEdits(int till) {
        int to = super.edits.size() - 1;
        int from = super.edits.size() - till;
        trimEdits(from, to);
    }

    public void clearEdits() {
        super.discardAllEdits();
    }

    public UndoRedoProgress getProgress() {
        return progress;
    }

    @Override
    public UndoableEdit lastEdit() {
        return super.lastEdit();
    }

    @Override
    public synchronized void redo() throws CannotRedoException {
        if (canRedo()) {
            super.redo();
        }
    }

    @Override
    public synchronized void undo() throws CannotUndoException {
        if (canUndo()) {
            super.undo();
        }
    }

}

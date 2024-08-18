
package com.ing.datalib.undoredo;

public class UndoRedoProgress {

    private boolean progress = false;

    public void start() {
        progress = true;
    }

    public void stop() {
        progress = false;
    }

    public boolean isInProgress() {
        return progress;
    }
}


package com.ing.datalib.component;

import com.ing.datalib.undoredo.UndoRedoModel;

/**
 *
 * 
 */
public abstract class DataModel extends UndoRedoModel {

    public abstract void loadTableModel();

    public abstract Boolean rename(String newName);

    public abstract Boolean delete();

    public abstract Boolean addRow();

    @Override
    public void insertColumnAt(int colIndex, String name, Object[] values) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void removeColumn(int colIndex) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}

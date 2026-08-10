package com.ing.ide.main.utils.table.autosuggest;

import javax.swing.DefaultCellEditor;

/**
 *
 * @author Julie Ann Ayap
 */
public class InputAutoSuggestCellEditor extends DefaultCellEditor {
    InputMainAutoSuggest autosugg;

    public InputAutoSuggestCellEditor(InputMainAutoSuggest jcb) {
        super(jcb);
        autosugg = jcb;
        setClickCountToStart(2);
    }

    @Override
    public boolean stopCellEditing() {
        if (!autosugg.isEditing()) {
            Boolean flag = super.stopCellEditing();
            if (flag) {
                autosugg.reset();
            }
            return flag;
        }
        return false;
    }
}

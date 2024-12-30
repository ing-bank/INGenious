
package com.ing.ide.main.utils.table.autosuggest;

import javax.swing.DefaultCellEditor;

public class AutoSuggestCellEditor extends DefaultCellEditor {

    AutoSuggest autosugg;

    public AutoSuggestCellEditor(AutoSuggest jcb) {
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

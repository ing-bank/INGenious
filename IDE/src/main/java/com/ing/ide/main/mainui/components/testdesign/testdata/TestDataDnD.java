
package com.ing.ide.main.mainui.components.testdesign.testdata;

import com.ing.datalib.testdata.model.TestDataModel;
import com.ing.ide.main.utils.dnd.DataFlavors;
import com.ing.ide.main.utils.dnd.TransferableNode;
import com.ing.ide.main.utils.table.XTable;
import java.awt.Toolkit;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.DefaultCellEditor;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.TransferHandler;

/**
 *
 * 
 */
public class TestDataDnD extends TransferHandler {

    @Override
    public int getSourceActions(JComponent c) {
        return COPY_OR_MOVE;
    }

    @Override
    protected Transferable createTransferable(JComponent source) {
        JTable table = (JTable) source;
        if (table.getModel() instanceof TestDataModel) {
            TestDataModel tdm = (TestDataModel) table.getModel();
            TestDataDetail td = new TestDataDetail();
            td.setSheetName(tdm.getName());
            for (int col : table.getSelectedColumns()) {
                if (col > 3) {
                    td.getColumnNames().add(table.getColumnName(col));
                }
            }
            return new TransferableNode(td, DataFlavors.TESTDATA_FLAVOR);
        }
        return null;
    }
    
}

   

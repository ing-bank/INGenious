
package com.ing.ide.main.mainui.components.testdesign.testcase;

import com.ing.datalib.component.TestCase;
import com.ing.datalib.component.TestStep.HEADERS;
import com.ing.datalib.or.common.ORPageInf;
import com.ing.ide.main.mainui.components.testdesign.or.ObjectDnD;
import com.ing.ide.main.mainui.components.testdesign.or.ObjectRepDnD;
import com.ing.ide.main.mainui.components.testdesign.testdata.TestDataDetail;
import com.ing.ide.main.mainui.components.testdesign.tree.ProjectDnD;
import com.ing.ide.main.mainui.components.testdesign.tree.TestCaseDnD;
import com.ing.ide.main.mainui.components.testdesign.tree.model.TestCaseNode;
import com.ing.ide.main.utils.dnd.DataFlavors;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JTable;
import javax.swing.TransferHandler;

/**
 *
 * 
 */
public class TestCaseTableDnD extends TransferHandler {

    private transient Object dropObject;
    private final int objectColumn = 1;
    private final int actionColumn = 3;
    private final int inputColumn = 4;
    private final int conditionColumn = 5;
    private final int referenceColumn = 6;

    @Override
    public boolean canImport(TransferHandler.TransferSupport support) {
        if (!support.isDrop()) {
            return false;
        }
        try {
            if (support.isDataFlavorSupported(ObjectDnD.OBJECT_FLAVOR)) {
                ObjectRepDnD dropNode = (ObjectRepDnD) support.getTransferable().getTransferData(ObjectDnD.OBJECT_FLAVOR);
                dropObject = dropNode;
                return true;
            } else if (support.isDataFlavorSupported(DataFlavors.TESTDATA_FLAVOR)) {
                TestDataDetail dropNode = (TestDataDetail) support.getTransferable().getTransferData(DataFlavors.TESTDATA_FLAVOR);
                dropObject = dropNode;
                return true;
            } else if (support.isDataFlavorSupported(ProjectDnD.TESTCASE_FLAVOR)) {
                dropObject = support.getTransferable().getTransferData(ProjectDnD.TESTCASE_FLAVOR);
                return true;
            } else {
                return false;
            }
        } catch (UnsupportedFlavorException | IOException ex) {
            Logger.getLogger(TestCaseTableDnD.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    @Override
    public boolean importData(TransferHandler.TransferSupport support) {
        if (!canImport(support)) {
            return false;
        }
        JTable.DropLocation dl = (JTable.DropLocation) support.getDropLocation();
        JTable table = (JTable) support.getComponent();
        int row = dl.getRow();
        int column = dl.getColumn();

        if (row == -1) {
            return false;
        }

        if (dropObject instanceof ObjectRepDnD) {
            switch (column) {
                case inputColumn:
                    putInput(table, row);
                    break;
                case conditionColumn:
                    putRelativeObject(table, row);
                    break;
                default:
                    putWebObjects(table, row);
                    break;
            }

        } else if (dropObject instanceof TestDataDetail) {
            putTestData(table, row);
        } else if (dropObject instanceof TestCaseDnD) {
            putReusables(table, row);
        } else {
            return false;
        }
        return super.importData(support);
    }

    private void putWebObjects(JTable table, int row) {
        TestCase testCase = (TestCase) table.getModel();
        testCase.startGroupEdit();
        putWebObjects((ObjectRepDnD) dropObject, row, testCase);
        testCase.stopGroupEdit();
    }

    private void putWebObjects(ObjectRepDnD objs, int row, TestCase testCase) {
        if (objs.isPage()) {
            for (Object page : objs.getComponents()) {
                ORPageInf pageInf = (ORPageInf) page;
                putWebObjects(new ObjectRepDnD().withObjectGroups(pageInf.getObjectGroups()), row, testCase);
                row += pageInf.getChildCount();
            }
        } else {
            for (String val : objs.getValues()) {
                if (row < testCase.getRowCount()) {
                    testCase.setValueAt(objs.getObjectName(val), row, HEADERS.ObjectName.getIndex());
                    testCase.setValueAt(objs.getPageName(val), row, HEADERS.Reference.getIndex());
                    testCase.fireTableRowsUpdated(row, row);
                } else {
                    testCase.addObjectStep(row, objs.getObjectName(val), objs.getPageName(val));
                }
                row++;
            }
        }
    }

    private void putRelativeObject(JTable table, int row) {
        String val = ((ObjectRepDnD) dropObject).getObjectName(((ObjectRepDnD) dropObject).getValues().get(0));
        if (val != null) {
            table.setValueAt(val, row, conditionColumn);
        }
    }

    private void putInput(JTable table, int row) {
        table.setValueAt("@" + ((ObjectRepDnD) dropObject).getPageName(((ObjectRepDnD) dropObject).getValues().get(0)), row, inputColumn);
    }

    private void putReusables(JTable table, int row) {
        TestCaseDnD testCaseDnD = (TestCaseDnD) dropObject;
        if (!testCaseDnD.getTestCaseList().isEmpty()) {
            TestCase testCase = (TestCase) table.getModel();
            testCase.startGroupEdit();
            testCase.removeSteps(new int[]{row});
            for (TestCaseNode testCaseNode : testCaseDnD.getTestCaseList()) {
                String reusable = testCaseNode.getParent().toString() + ":"
                        + testCaseNode.toString();
                testCase.addReusableStep(row, reusable);
            }
            testCase.stopGroupEdit();
        }
    }

    private void putTestData(JTable table, int row) {
        TestCase testCase = (TestCase) table.getModel();
        testCase.startGroupEdit();
        TestDataDetail td = (TestDataDetail) dropObject;
        for (String col : td.getColumnNames()) {
            if (row > table.getRowCount() - 1) {
                testCase.addNewStep();
            }
            table.setValueAt(td.getSheetName() + ":" + col, row++, inputColumn);
        }
        testCase.stopGroupEdit();
    }
}

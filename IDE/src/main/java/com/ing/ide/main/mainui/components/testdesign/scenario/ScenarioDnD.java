
package com.ing.ide.main.mainui.components.testdesign.scenario;

import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.ide.main.mainui.components.testdesign.tree.ProjectDnD;
import com.ing.ide.main.mainui.components.testdesign.tree.TestCaseDnD;
import com.ing.ide.main.mainui.components.testdesign.tree.model.TestCaseNode;
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
public class ScenarioDnD extends TransferHandler {

    private transient Object dropObject;

    @Override
    public boolean canImport(TransferHandler.TransferSupport support) {
        if (!support.isDrop()) {
            return false;
        }
        JTable.DropLocation dl = (JTable.DropLocation) support.getDropLocation();
        if (dl.getColumn() < 1) {
            return false;
        }
        try {
            if (support.isDataFlavorSupported(ProjectDnD.TESTCASE_FLAVOR)) {
                dropObject = support.getTransferable().getTransferData(ProjectDnD.TESTCASE_FLAVOR);
                return true;
            } else {
                return false;
            }
        } catch (UnsupportedFlavorException | IOException ex) {
            Logger.getLogger(ScenarioDnD.class.getName()).log(Level.SEVERE, null, ex);
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
        int tcRow = dl.getColumn() - 1;
        if (row == -1) {
            return false;
        }

        Scenario scenario = (Scenario) table.getModel();
        TestCase testCase = scenario.getTestCaseByName(
                table.getValueAt(row, 0).toString());

        if (dropObject instanceof TestCaseDnD) {
            putReusables(testCase, tcRow);
        } else {
            return false;
        }
        return super.importData(support);
    }

    private void putReusables(TestCase testCase, int row) {
        TestCaseDnD testCaseDnD = (TestCaseDnD) dropObject;
        if (!testCaseDnD.getTestCaseList().isEmpty()) {
            testCase.removeSteps(new int[]{row});
            for (TestCaseNode testCaseNode : testCaseDnD.getTestCaseList()) {
                String reusable = testCaseNode.getParent().toString() + ":"
                        + testCaseNode.toString();
                testCase.addReusableStep(row, reusable);
            }
        }
    }

}

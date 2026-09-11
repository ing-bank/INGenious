package com.ing.ide.main.mainui.components.testdesign.testdata;

import com.ing.ide.main.mainui.AppMainFrame;
import com.ing.ide.main.mainui.components.testdesign.TestDesign;
import java.io.File;
import java.io.IOException;

/**
 * Entry point for the "Test Data &rarr; Import TestData" menu action.
 *
 * <p>Opens {@link ImportTestDataDialog} so the user can choose the target scope (Project or
 * Shared Test Data) and one or more environments, then imports the selected CSV datasheet(s)
 * into every chosen environment.</p>
 */
public class ImportTestData {
    private final AppMainFrame sMainFrame;

    public ImportTestData(AppMainFrame sMainFrame) throws IOException {
        this.sMainFrame = sMainFrame;
    }

    public void importTestData() {
        TestDesign testDesign = sMainFrame.getTestDesign();
        if (testDesign == null || testDesign.getProject() == null) {
            return;
        }

        ImportTestDataDialog dialog = ImportTestDataDialog.showDialog(
            sMainFrame,
            testDesign.getTestDesignUI().isSharedTestDataTabSelected(),
            testDesign.getTestDatacomp().getListOfEnvironements(),
            testDesign.getSharedTestDataComp().getListOfEnvironements()
        );

        if (!dialog.isConfirmed()) {
            return;
        }

        TestDataComponent target = dialog.isShared()
            ? testDesign.getSharedTestDataComp()
            : testDesign.getTestDatacomp();

        for (File file : dialog.getSelectedFiles()) {
            target.importTestData(file, dialog.getSelectedEnvironments());
        }

        testDesign.getTestDesignUI().selectTestDataTab(dialog.isShared());
    }
}

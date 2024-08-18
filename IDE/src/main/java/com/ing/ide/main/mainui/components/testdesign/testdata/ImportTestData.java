
package com.ing.ide.main.mainui.components.testdesign.testdata;

import com.ing.ide.main.mainui.AppMainFrame;
import java.io.File;
import java.io.IOException;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 *
 * 
 */
public class ImportTestData {

    private final AppMainFrame sMainFrame;

    JFileChooser tdFileChooser;

    public ImportTestData(AppMainFrame sMainFrame) throws IOException {
        this.sMainFrame = sMainFrame;
        tdFileChooser = new JFileChooser(new File(new File(System.getProperty("user.dir")).getCanonicalPath()));
        tdFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        tdFileChooser.setFileFilter(new FileNameExtensionFilter("TestData Files", "csv"));
    }

    public void importTestData() {
        int val = tdFileChooser.showOpenDialog(sMainFrame);
        if (val == JFileChooser.APPROVE_OPTION) {
            File file = tdFileChooser.getSelectedFile();
            sMainFrame.getTestDesign().getTestDatacomp().importTestData(file);
        }
    }
}

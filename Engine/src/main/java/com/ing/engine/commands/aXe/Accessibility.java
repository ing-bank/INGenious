package com.ing.engine.commands.aXe;

import com.ing.engine.commands.browser.General;
import com.ing.engine.core.CommandControl;
import com.deque.html.axecore.playwright.*;
import com.deque.html.axecore.results.AxeResults;
import com.deque.html.axecore.playwright.Reporter;
import com.ing.engine.commands.browser.Performance;
import com.ing.engine.constants.FilePath;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Accessibility extends General {

    public Accessibility(CommandControl cc) {
        super(cc);
    }

    @Action(object = ObjectType.BROWSER,
            input = InputType.YES,
            desc = "To Test the Accessibility of the Page")
    public void testAccessibility() {
        try {
            AxeResults accessibilityScanResults = new AxeBuilder(Page).analyze();
            int violationCount = accessibilityScanResults.getViolations().size();
            int passCount = accessibilityScanResults.getPasses().size();
            if (violationCount != 0) {
                Report.updateTestLog(Action, "Accessibility Tests have found [" + violationCount + "] violation(s) and [" + passCount + "] pass(es). Check the 'Results' location for further details ", Status.WARNING);
            } else {
                Report.updateTestLog(Action, "Accessibility Tests have found no violations for this page. Total 'Pass' checks are : [" + passCount + "]. Visit the 'Results' location for further details ", Status.PASSNS);
            }
            saveAccessibilityResults(accessibilityScanResults);
        } catch (Exception ex) {
            Report.updateTestLog(Action, "Unable to check the Accessibility of this Page : " + ex.getMessage(),
                    Status.FAIL);
            Logger.getLogger(Performance.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }
    }
    
    private void saveAccessibilityResults(AxeResults accessibilityScanResults) throws FileNotFoundException, IOException{
        String prefix = userData.getScenario() + "_" + userData.getCurrentTestCase();
        File accessibilityFolder = new File(FilePath.getCurrentTestCaseAccessibilityLocation());
        accessibilityFolder.mkdir();
        String accessibilityReportPath = accessibilityFolder.getAbsolutePath() + File.separator + prefix + "_"+"axe-results.json"; 
        new Reporter().JSONStringify(accessibilityScanResults, accessibilityReportPath);
        System.out.println("\n-----------------------------------------------------");
        System.out.println("Accessibility Report generated");
        System.out.println("-----------------------------------------------------\n");
    }

}

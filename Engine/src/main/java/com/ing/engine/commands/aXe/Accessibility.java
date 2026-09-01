package com.ing.engine.commands.aXe;

import com.deque.html.axecore.playwright.*;
import com.deque.html.axecore.playwright.Reporter;
import com.deque.html.axecore.results.AxeResults;
import com.deque.html.axecore.results.Rule;
import com.ing.engine.commands.browser.General;
import com.ing.engine.commands.browser.Performance;
import com.ing.engine.constants.FilePath;
import com.ing.engine.core.CommandControl;
import com.ing.engine.core.Control;
import com.ing.engine.core.RunManager;
import com.ing.ingenious.api.annotation.Action;
import com.ing.ingenious.api.status.Status;
import com.ing.ingenious.api.types.InputType;
import com.ing.ingenious.api.types.ObjectType;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Accessibility extends General {

    public Accessibility(CommandControl cc) {
        super(cc);
    }

    public enum Severity {
        CRITICAL("critical", 4),
        SERIOUS("serious", 3),
        MODERATE("moderate", 2),
        MINOR("minor", 1);

        private final String reportString;
        private final int severity;

        Severity(String reportString, int severity) {
            this.reportString = reportString;
            this.severity = severity;
        }

        public String getReportString() {
            return this.reportString;
        }

        public int getSeverity() {
            return this.severity;
        }

        // Reverse lookup method
        public static Severity fromStringValue(String name) {
            for (Severity e : values()) {
                if (e.reportString.toLowerCase().equals(name.toLowerCase())) {
                    return e;
                }
            }
            throw new IllegalArgumentException("Unknown enum value");
        }
    }

    @Action(
        object = ObjectType.BROWSER,
        input = InputType.YES,
        desc = "To Test the Accessibility of the Page",
        condition = InputType.OPTIONAL
    )
    public void testAccessibility() {
        try {
            AxeResults accessibilityScanResults = new AxeBuilder(Page).analyze();
            int violationCount = accessibilityScanResults.getViolations().size();
            int violationCountAboveThreshold = 0;
            int passCount = accessibilityScanResults.getPasses().size();
            int threshold = 0; //Default to 0 or lower than Minor (1)
            String thresholdString = "";
            try {
                if (Condition.isBlank() || Condition.isEmpty()) {
                    threshold = Severity.fromStringValue(getSeverityThreshold()).getSeverity();
                    thresholdString = getSeverityThreshold();
                } else {
                    threshold = Severity.fromStringValue(Condition).getSeverity();
                    thresholdString = Condition;
                }
            } catch (Exception e) {
                // Do nothing
            }

            for (Rule rule : accessibilityScanResults.getViolations()) {
                if (Severity.fromStringValue(rule.getImpact()).getSeverity() >= threshold) {
                    violationCountAboveThreshold++;
                }
            }
            boolean hasViolationsAboveThreshold = violationCountAboveThreshold > 0;
            String violationSummary =
                violationCount + " violation" + ((violationCount != 1) ? "s" : "");
            String passSummary =
                " A total of " +
                passCount +
                " pass" +
                ((passCount != 1) ? "es have" : " has") +
                " been executed. Check the report for further details.";

            String message;
            Status status;
            if (hasViolationsAboveThreshold) {
                String thresholdSummary =
                    " where " +
                    violationCountAboveThreshold +
                    ((violationCountAboveThreshold != 1) ? " violations are" : " violation is") +
                    " at or above the severity threshold (" +
                    thresholdString +
                    ").";

                message =
                    "The accessibility test has found " +
                    violationSummary +
                    thresholdSummary +
                    passSummary;
                status = Status.FAILNS;
            } else if (violationCount > 0) {
                String thresholdSummary =
                    " where none are at or above the severity threshold (" + thresholdString + ").";

                message =
                    "The accessibility test has found " +
                    violationSummary +
                    thresholdSummary +
                    passSummary;
                status = Status.PASSNS;
            } else {
                message =
                    "The accessibility test has found no violations for this page." + passSummary;
                status = Status.PASSNS;
            }

            Report.updateTestLog(Action, message, status);
            saveAccessibilityResults(accessibilityScanResults);
        } catch (Exception ex) {
            Report.updateTestLog(
                Action,
                "Unable to check the Accessibility of this Page : " + ex.getMessage(),
                Status.FAIL
            );
            Logger.getLogger(Performance.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    private void saveAccessibilityResults(AxeResults accessibilityScanResults)
        throws FileNotFoundException, IOException {
        // Use getCurrentTestCase() which returns the reusable name when inside a reusable,
        // or the test case name when running directly. This ensures the frontend can find
        // the report whether execution is direct or within a reusable.
        String prefix = userData.getScenario() + "_" + userData.getCurrentTestCase();
        File accessibilityFolder = new File(FilePath.getCurrentTestCaseAccessibilityLocation());
        accessibilityFolder.mkdir();
        String accessibilityReportPath =
            accessibilityFolder.getAbsolutePath() +
            File.separator +
            prefix +
            "_" +
            "axe-results.json";
        new Reporter().JSONStringify(accessibilityScanResults, accessibilityReportPath);
        System.out.println("\n-----------------------------------------------------");
        System.out.println("Accessibility Report generated: " + prefix + "_axe-results.json");
        System.out.println("-----------------------------------------------------\n");
    }

    public static String getSeverityThreshold() {
        try {
            String severityThreshold = Control
                .exe.getExecSettings()
                .getRunSettings()
                .getAxeSeverityThreshold();

            if (severityThreshold == null || severityThreshold.isBlank()) {
                return "Minor";
            }
            return severityThreshold;
        } catch (Exception ex) {
            return "Minor";
        }
    }
}

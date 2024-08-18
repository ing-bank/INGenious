
package com.ing.ide.main.mainui.components.testexecution.testset;

import com.ing.datalib.component.ExecutionStep;
import com.ing.datalib.component.TestSet;
import com.ing.ide.main.utils.table.XTable;
import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 *
 * 
 */
public class TestSetValidator extends DefaultTableCellRenderer {

    private final Color errorColor = Color.RED.darker();

    XTable testSetTable;

    public TestSetValidator(XTable table) {
        this.testSetTable = table;
    }

    public void init() {
        testSetTable.setDefaultRenderer(Object.class, this);
    }

    private TestSet getCurrentTestSet() {
        return (TestSet) testSetTable.getModel();
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
        JComponent comp = (JComponent) super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, col);
        List<Integer> dupList = getDuplicate(row);
        if (!dupList.isEmpty()) {
            comp.setForeground(errorColor);
            comp.setToolTipText("Duplicate with Step " + dupList);
        } else if (col == ExecutionStep.HEADERS.Status.getIndex()) {
            if ("Passed".equals(value)) {
                comp.setForeground(Color.GREEN.darker());
            } else if ("Failed".equals(value)) {
                comp.setForeground(errorColor);
            } else {
                comp.setForeground(table.getForeground());
            }
        } else {
            comp.setForeground(table.getForeground());
            comp.setToolTipText(null);
        }
        return comp;
    }

    private void checkForStatus() {

    }

    private List<Integer> getDuplicate(int index) {
        ExecutionStep compareStep = getCurrentTestSet().getTestSteps().get(index);
        final List<Integer> duplicatedObjects = new ArrayList<>();
        int i = 0;
        for (ExecutionStep testStep : getCurrentTestSet().getTestSteps()) {
            if (i != index) {
                if (isDuplicate(compareStep, testStep)) {
                    duplicatedObjects.add(i + 1);
                }
            }
            i++;
        }
        return duplicatedObjects;
    }

    private Boolean isDuplicate(ExecutionStep testStep1, ExecutionStep testStep2) {
        Boolean flag = testStep1.isDuplicate(testStep2);
        if (!flag) {
            return Objects.equals(testStep1.getTestScenarioName(), testStep2.getTestScenarioName())
                    && Objects.equals(testStep1.getTestCaseName(), testStep2.getTestCaseName())
                    && Objects.equals(testStep1.getBrowser(), testStep2.getBrowser())
                    && Objects.equals(testStep1.getIteration(), testStep2.getIteration())
                    && Objects.equals(testStep1.getBrowserVersion(), testStep2.getBrowserVersion())
                    && Objects.equals(testStep1.getPlatform(), testStep2.getPlatform());
        }
        return true;
    }

    private Boolean removeInvalidSteps() {
        int i = 0;
        List<Integer> steps = new ArrayList<>();
        for (ExecutionStep testStep1 : getCurrentTestSet().getTestSteps()) {
            Boolean isFull = !Objects.toString(testStep1.getTestScenarioName(), "").isEmpty()
                    && !Objects.toString(testStep1.getTestCaseName(), "").isEmpty()
                    && !Objects.toString(testStep1.getBrowser(), "").isEmpty()
                    && !Objects.toString(testStep1.getIteration(), "").isEmpty()
                    && !Objects.toString(testStep1.getBrowserVersion(), "").isEmpty()
                    && !Objects.toString(testStep1.getPlatform(), "").isEmpty();
            if (!isFull) {
                steps.add(i);
            }
            i++;
        }
        if (!steps.isEmpty()) {
            int val = JOptionPane.showConfirmDialog(null,
                    "The TestSet has Invalid entries."
                    + steps
                    + ". Do you want to remove them?",
                    "Remove Invalid Entries", JOptionPane.YES_NO_CANCEL_OPTION);
            if (val == JOptionPane.CANCEL_OPTION) {
                return false;
            } else if (val == JOptionPane.YES_OPTION) {
                Collections.reverse(steps);
                getCurrentTestSet().removeSteps(steps);
            }
        }
        return true;
    }

    private List<CustomCompareStep> getDuplicates() {
        List<CustomCompareStep> customSteps = new ArrayList<>();
        List<CustomCompareStep> dupSteps = new ArrayList<>();
        for (ExecutionStep testStep : getCurrentTestSet().getTestSteps()) {
            CustomCompareStep cStep = new CustomCompareStep(testStep);
            if (!customSteps.contains(cStep)) {
                customSteps.add(cStep);
            } else {
                dupSteps.add(cStep);
            }
        }
        return dupSteps;
    }

    Boolean validateSteps() {
        Boolean flag = removeInvalidSteps();
        if (flag) {
            List<CustomCompareStep> dupls = getDuplicates();
            if (!dupls.isEmpty()) {
                int val = JOptionPane.showConfirmDialog(null,
                        "The TestSet has duplicate entries."
                        + dupls
                        + ". Do you want to remove them?",
                        "Remove Duplicates", JOptionPane.YES_NO_CANCEL_OPTION);
                if (val == JOptionPane.CANCEL_OPTION) {
                    return false;
                }
                if (val == JOptionPane.YES_OPTION) {
                    List<Integer> dupList = new ArrayList<>();
                    for (CustomCompareStep dupl : dupls) {
                        dupList.add(getCurrentTestSet().getTestSteps().indexOf(dupl.step));
                    }
                    Collections.reverse(dupList);
                    getCurrentTestSet().removeSteps(dupList);
                }
            }
        } else {
            return false;
        }
        return true;
    }

    class CustomCompareStep {

        ExecutionStep step;

        public CustomCompareStep(ExecutionStep step) {
            this.step = step;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final CustomCompareStep other = (CustomCompareStep) obj;
            return isDuplicate(step, other.step);
        }

        @Override
        public String toString() {
            return getCurrentTestSet().getTestSteps().indexOf(step) + 1 + "";
        }

    }

}

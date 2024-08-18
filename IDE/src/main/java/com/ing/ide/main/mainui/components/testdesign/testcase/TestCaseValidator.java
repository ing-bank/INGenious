
package com.ing.ide.main.mainui.components.testdesign.testcase;

import com.ing.datalib.component.TestStep;
import com.ing.ide.main.mainui.components.testdesign.testcase.validation.ActionRenderer;
import com.ing.ide.main.mainui.components.testdesign.testcase.validation.ConditionRenderer;
import com.ing.ide.main.mainui.components.testdesign.testcase.validation.InputRenderer;
import com.ing.ide.main.mainui.components.testdesign.testcase.validation.ObjectRenderer;
import com.ing.ide.main.mainui.components.testdesign.testcase.validation.ReferenceRenderer;
import com.ing.ide.main.mainui.components.testdesign.testcase.validation.StepRenderer;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

public class TestCaseValidator {

    StepRenderer stepRenderer;
    ObjectRenderer objectRenderer;
    ReferenceRenderer referenceRenderer;
    ActionRenderer actionRenderer;
    InputRenderer inputRenderer;
    ConditionRenderer conditionRenderer;

    private Boolean validate = true;
    private final JTable testCaseTable;

    public TestCaseValidator(JTable testCaseTable) {
        this.testCaseTable = testCaseTable;
        init();
    }

    private void init() {
        stepRenderer = new StepRenderer();
        objectRenderer = new ObjectRenderer();
        referenceRenderer = new ReferenceRenderer();
        actionRenderer = new ActionRenderer();
        inputRenderer = new InputRenderer();
        conditionRenderer = new ConditionRenderer();
    }

    public void initValidations() {
        validate();
    }

    private void setValidations() {
        testCaseTable.getColumnModel().getColumn(TestStep.HEADERS.Step.getIndex())
                .setCellRenderer(stepRenderer);
        testCaseTable.getColumnModel().getColumn(TestStep.HEADERS.ObjectName.getIndex())
                .setCellRenderer(objectRenderer);
        testCaseTable.getColumnModel().getColumn(TestStep.HEADERS.Reference.getIndex())
                .setCellRenderer(referenceRenderer);
        testCaseTable.getColumnModel().getColumn(TestStep.HEADERS.Action.getIndex())
                .setCellRenderer(actionRenderer);
        testCaseTable.getColumnModel().getColumn(TestStep.HEADERS.Input.getIndex())
                .setCellRenderer(inputRenderer);
        testCaseTable.getColumnModel().getColumn(TestStep.HEADERS.Condition.getIndex())
                .setCellRenderer(conditionRenderer);
    }

    private void removeValidations() {
        testCaseTable.getColumnModel().getColumn(TestStep.HEADERS.Step.getIndex())
                .setCellRenderer(null);
        testCaseTable.getColumnModel().getColumn(TestStep.HEADERS.ObjectName.getIndex())
                .setCellRenderer(null);
        testCaseTable.getColumnModel().getColumn(TestStep.HEADERS.Reference.getIndex())
                .setCellRenderer(null);
        testCaseTable.getColumnModel().getColumn(TestStep.HEADERS.Action.getIndex())
                .setCellRenderer(null);
        testCaseTable.getColumnModel().getColumn(TestStep.HEADERS.Input.getIndex())
                .setCellRenderer(null);
        testCaseTable.getColumnModel().getColumn(TestStep.HEADERS.Condition.getIndex())
                .setCellRenderer(null);
    }

    public final void enableValidation() {
        validate = true;
        validate();
    }

    public final void toggleValidation() {
        validate = !validate;
        validate();
    }

    public final void disableValidation() {
        validate = false;
        validate();
    }

    private void validate() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (validate) {
                    setValidations();
                } else {
                    removeValidations();
                }
                testCaseTable.repaint();
            }
        });
    }

}

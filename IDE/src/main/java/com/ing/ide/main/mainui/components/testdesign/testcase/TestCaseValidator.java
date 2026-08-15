package com.ing.ide.main.mainui.components.testdesign.testcase;

import com.ing.datalib.component.TestStep;
import com.ing.ide.main.mainui.components.testdesign.testcase.validation.ActionRenderer;
import com.ing.ide.main.mainui.components.testdesign.testcase.validation.ConditionRenderer;
import com.ing.ide.main.mainui.components.testdesign.testcase.validation.DescriptionRenderer;
import com.ing.ide.main.mainui.components.testdesign.testcase.validation.InputRenderer;
import com.ing.ide.main.mainui.components.testdesign.testcase.validation.ObjectRenderer;
import com.ing.ide.main.mainui.components.testdesign.testcase.validation.ReferenceRenderer;
import com.ing.ide.main.mainui.components.testdesign.testcase.validation.StepRenderer;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellRenderer;

public class TestCaseValidator {
    StepRenderer stepRenderer;
    ObjectRenderer objectRenderer;
    ReferenceRenderer referenceRenderer;
    ActionRenderer actionRenderer;
    InputRenderer inputRenderer;
    ConditionRenderer conditionRenderer;
    DescriptionRenderer descriptionRenderer;

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
        descriptionRenderer = new DescriptionRenderer();
    }

    public void initValidations() {
        validate();
    }

    /**
     * Assign a cell renderer to the column identified by its model index,
     * resolving to the current view index. Columns hidden by the user return a
     * view index of -1 and are skipped, so renderer wiring stays correct
     * regardless of which columns are visible.
     */
    private void setRenderer(TestStep.HEADERS header, TableCellRenderer renderer) {
        int view = testCaseTable.convertColumnIndexToView(header.getIndex());
        if (view != -1) {
            testCaseTable.getColumnModel().getColumn(view).setCellRenderer(renderer);
        }
    }

    private void setValidations() {
        setRenderer(TestStep.HEADERS.Step, stepRenderer);
        setRenderer(TestStep.HEADERS.ObjectName, objectRenderer);
        setRenderer(TestStep.HEADERS.Description, descriptionRenderer);
        setRenderer(TestStep.HEADERS.Reference, referenceRenderer);
        setRenderer(TestStep.HEADERS.Action, actionRenderer);
        setRenderer(TestStep.HEADERS.Input, inputRenderer);
        setRenderer(TestStep.HEADERS.Condition, conditionRenderer);
    }

    private void removeValidations() {
        setRenderer(TestStep.HEADERS.Step, null);
        setRenderer(TestStep.HEADERS.ObjectName, null);
        // Description stays muted even when validation is toggled off.
        setRenderer(TestStep.HEADERS.Description, descriptionRenderer);
        setRenderer(TestStep.HEADERS.Reference, null);
        setRenderer(TestStep.HEADERS.Action, null);
        setRenderer(TestStep.HEADERS.Input, null);
        setRenderer(TestStep.HEADERS.Condition, null);
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
        SwingUtilities.invokeLater(
            new Runnable() {

                @Override
                public void run() {
                    if (validate) {
                        setValidations();
                    } else {
                        removeValidations();
                    }
                    testCaseTable.repaint();
                }
            }
        );
    }
}

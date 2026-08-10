package com.ing.ide.main.mainui.components.testdesign.testcase.validation;

import com.ing.datalib.component.TestStep;
import java.awt.Color;
import javax.swing.JComponent;
import javax.swing.UIManager;

/**
 *
 *
 */
public class StepRenderer extends AbstractRenderer {

    public StepRenderer() {
        super(null);
    }

    @Override
    public void render(JComponent comp, TestStep step, Object value) {
        comp.setForeground(getColor(step));
    }

    private Color getColor(TestStep step) {
        if (step.isNewlyRecorded()) {
            Color c = UIManager.getColor("ing.newlyRecordedForeground");
            return c != null ? c : new Color(0, 153, 51);
        } else if (step.isCommented()) {
            Color c = UIManager.getColor("ing.commentedForeground");
            return c != null ? c : Color.lightGray;
        } else if (step.hasBreakPoint()) {
            Color c = UIManager.getColor("ing.breakpointForeground");
            return c != null ? c : Color.BLUE;
        }
        return UIManager.getColor("text");
    }

    @Override
    protected Object getColumnValue(TestStep step) {
        // StepRenderer never flags a validation error; value is unused.
        return step.getObject();
    }
}


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
        if (step.isCommented()) {
            Color c = UIManager.getColor("ing.commentedForeground");
            return c != null ? c : Color.lightGray;
        } else if (step.hasBreakPoint()) {
            Color c = UIManager.getColor("ing.breakpointForeground");
            return c != null ? c : Color.BLUE;
        }
        return UIManager.getColor("text");
    }

}

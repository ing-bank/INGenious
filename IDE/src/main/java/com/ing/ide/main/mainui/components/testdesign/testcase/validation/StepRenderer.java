
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
            return Color.lightGray;
        } else if (step.hasBreakPoint()) {
            return Color.BLUE;
        }
        return UIManager.getColor("text");
    }

}

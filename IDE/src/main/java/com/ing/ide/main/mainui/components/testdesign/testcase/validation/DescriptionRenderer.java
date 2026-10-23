package com.ing.ide.main.mainui.components.testdesign.testcase.validation;

import com.ing.datalib.component.TestStep;
import java.awt.Color;
import java.awt.Font;
import javax.swing.JComponent;
import javax.swing.UIManager;

/**
 * Renders the Description column in a muted, non-bold tone so it reads as
 * informative metadata rather than a prominent field. View-only; it never
 * flags validation errors.
 */
public class DescriptionRenderer extends AbstractRenderer {

    public DescriptionRenderer() {
        super("");
    }

    @Override
    protected Object getColumnValue(TestStep step) {
        return step.getDescription();
    }

    @Override
    public void render(JComponent comp, TestStep step, Object value) {
        if (step.isCommented()) {
            Color c = UIManager.getColor("ing.commentedForeground");
            comp.setForeground(c != null ? c : Color.lightGray);
            comp.setFont(new Font("Default", Font.ITALIC, 11));
            return;
        }
        comp.setBorder(null);
        comp.setFont(comp.getFont().deriveFont(Font.PLAIN));
        Color muted = UIManager.getColor("ing.mutedForeground");
        comp.setForeground(muted != null ? muted : new Color(0x8A, 0x90, 0x99));
        comp.setToolTipText(null);
    }
}


package com.ing.ide.main.mainui.components.testdesign.testcase.validation;

import com.ing.datalib.component.TestStep;
import java.awt.Color;
import java.awt.Font;
import java.util.Objects;
import javax.swing.JComponent;

/**
 *
 * 
 */
public class ReferenceRenderer extends AbstractRenderer {

    String objNotPresent = "Object is not present in the Object Repository";

    public ReferenceRenderer() {
        super("Reference Shouldn't be empty, except if Object is one of [Execute,App,Browser]");
    }

    @Override
    public void render(JComponent comp, TestStep step, Object value) {
        if (!step.isCommented()) {
            if (isEmpty(value)) {
                if (isOptional(step)) {
                    setDefault(comp);
                } else {
                    setEmpty(comp);
                }
            } else if (step.isPageObjectStep()) {
                if (isObjectPresent(step)) {
                    setDefault(comp);
                } else {
                    setNotPresent(comp, objNotPresent);
                }
            } else {
                setDefault(comp);
            }
        } else {
            setDefault(comp);
            comp.setForeground(Color.lightGray);
            comp.setFont(new Font("Default", Font.ITALIC, 11));
        }
    }
	
	private Color getColor(Object value) {
        String val = Objects.toString(value, "").trim();
        switch (val) {
            case "Execute":
                return Color.BLUE;//.darker();
            case "Mobile":
                return Color.CYAN;//.darker();
            case "Browser":
                return Color.RED;//.darker();
            default:
                return new Color(204, 0, 255);
        }
    }

    private Boolean isOptional(TestStep step) {
        return step.getObject().matches("Execute|Mobile|Browser|Database|Webservice|Kafka|Synthetic Data|Queue|File");
    }

    private Boolean isObjectPresent(TestStep step) {
        return step.getProject().getObjectRepository()
                .isObjectPresent(step.getReference(), step.getObject());
    }

}

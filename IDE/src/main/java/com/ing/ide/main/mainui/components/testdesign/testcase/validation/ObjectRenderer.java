
package com.ing.ide.main.mainui.components.testdesign.testcase.validation;

import com.ing.datalib.component.TestStep;
import java.awt.Color;
import java.awt.Font;
import java.util.Objects;
import javax.swing.JComponent;
import javax.swing.UIManager;

/**
 *
 * 
 */
public class ObjectRenderer extends AbstractRenderer {

    String objNotPresent = "Object is not present in the Object Repository";

    public ObjectRenderer() {
        super("Object Shouldn't be empty.It should be one of[Execute,App,Browser or Object]");
    }

    @Override
    public void render(JComponent comp, TestStep step, Object value) {
        if (!step.isCommented()) {
            if (isEmpty(value)) {
                setEmpty(comp);
            } else if (step.isPageObjectStep()) {
                if (isObjectPresent(step)) {
                    setDefault(comp);
                } else {
                    setNotPresent(comp, objNotPresent);
                }
            } else if (isValidObject(value)) {
                setDefault(comp);
            } else {
                setNotPresent(comp, objNotPresent);
            }
        } else {
            setDefault(comp);
            Color c = UIManager.getColor("ing.commentedForeground");
            comp.setForeground(c != null ? c : Color.lightGray);
            comp.setFont(new Font("Default", Font.ITALIC, 11));
        }
    }

	private Color getColor(Object value) {
        String val = Objects.toString(value, "").trim();
        switch (val) {
            case "Execute":
                Color bpColor = UIManager.getColor("ing.breakpointForeground");
                return bpColor != null ? bpColor : Color.BLUE;
            case "Mobile":
                return UIManager.getColor("ing.focusedSelectionBackground") != null
                        ? UIManager.getColor("ing.focusedSelectionBackground") : Color.CYAN;
            case "Browser":
                Color errColor = UIManager.getColor("ing.errorForeground");
                return errColor != null ? errColor : Color.RED;
            default:
                Color wsColor = UIManager.getColor("ing.webserviceRequestForeground");
                return wsColor != null ? wsColor : new Color(204, 0, 255);
        }
    }

    private Boolean isObjectPresent(TestStep step) {
        return step.getProject().getObjectRepository()
                .isObjectPresent(step.getReference(), step.getObject());
    }

    private Boolean isValidObject(Object value) {
        return Objects.toString(value, "").trim()
                .matches("Execute|Mobile|Browser|Database|Webservice|Kafka|Synthetic Data|Queue|File|General|String Operations");
    }

}

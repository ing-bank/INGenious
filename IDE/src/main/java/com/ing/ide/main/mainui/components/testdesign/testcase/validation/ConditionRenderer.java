
package com.ing.ide.main.mainui.components.testdesign.testcase.validation;

import com.ing.datalib.component.TestStep;
import java.awt.Color;
import com.ing.engine.support.methodInf.MethodInfoManager;
import java.awt.Font;
import java.util.Objects;
import javax.swing.JComponent;
import javax.swing.UIManager;

public class ConditionRenderer extends AbstractRenderer {

    public ConditionRenderer() {
        super("Condition Shouldn't be empty. Additonal Object/Data is needed for the action");
    }

    @Override
    public void render(JComponent comp, TestStep step, Object value) {
        if (!step.isCommented() && isEmpty(value) && !isOptional(step)) {
            setEmpty(comp);
        } else if(step.isCommented())
        {
            Color c = UIManager.getColor("ing.commentedForeground");
            comp.setForeground(c != null ? c : Color.lightGray);
            comp.setFont(new Font("Default", Font.ITALIC, 11));
        }
        else {
            setDefault(comp);
        }
    }

    private Boolean isOptional(TestStep step) {
        if (MethodInfoManager.methodInfoMap.containsKey(step.getAction())) {
            return !MethodInfoManager.methodInfoMap.get(step.getAction()).condition().isMandatory();
        }
        return true;
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

}

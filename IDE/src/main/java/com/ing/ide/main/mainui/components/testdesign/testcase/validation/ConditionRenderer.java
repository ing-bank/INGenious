package com.ing.ide.main.mainui.components.testdesign.testcase.validation;

import com.ing.datalib.component.TestStep;
import com.ing.engine.commands.aXe.Accessibility;
import com.ing.engine.support.methodInf.MethodInfoManager;
import java.awt.Color;
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
        } else if (
            !step.isCommented() && !isEmpty(value) && !isValidConditionForAction(step, value)
        ) {
            setNotPresent(comp, "Invalid condition value for action: " + step.getAction());
        } else if (step.isCommented()) {
            Color c = UIManager.getColor("ing.commentedForeground");
            comp.setForeground(c != null ? c : Color.lightGray);
            comp.setFont(new Font("Default", Font.ITALIC, 11));
        } else {
            setDefault(comp);
        }
    }

    private Boolean isOptional(TestStep step) {
        if (MethodInfoManager.containsAction(step.getAction())) {
            return !MethodInfoManager.getActionFor(step.getAction()).condition().isMandatory();
        }
        return true;
    }

    /**
     * Validates the condition value is valid for the given action.
     * For testAccessibility action, only valid Severity values are accepted.
     *
     * @param step  the test step containing the action
     * @param value the condition value to validate
     * @return {@code true} if the condition value is valid for this action
     */
    private Boolean isValidConditionForAction(TestStep step, Object value) {
        String action = step.getAction();
        String conditionValue = Objects.toString(value, "").trim();

        // Validate testAccessibility action condition
        if ("testAccessibility".equals(action)) {
            try {
                // Convert to lowercase to match Severity enum's report strings
                Accessibility.Severity.fromStringValue(conditionValue.toLowerCase());
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }

        // All other actions are valid by default
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
                    ? UIManager.getColor("ing.focusedSelectionBackground")
                    : Color.CYAN;
            case "Browser":
                Color errColor = UIManager.getColor("ing.errorForeground");
                return errColor != null ? errColor : Color.RED;
            default:
                Color wsColor = UIManager.getColor("ing.webserviceRequestForeground");
                return wsColor != null ? wsColor : new Color(204, 0, 255);
        }
    }

    @Override
    protected Object getColumnValue(TestStep step) {
        return step.getCondition();
    }
}

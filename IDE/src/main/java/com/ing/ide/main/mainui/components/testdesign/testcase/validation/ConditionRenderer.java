package com.ing.ide.main.mainui.components.testdesign.testcase.validation;

import com.ing.datalib.component.TestStep;
import com.ing.engine.mcp.ActionSpecCatalog;
import com.ing.engine.mcp.ArgSpec;
import com.ing.engine.support.methodInf.MethodInfoManager;
import com.ing.ingenious.api.types.ConditionKind;
import java.awt.Color;
import java.awt.Font;
import java.util.Objects;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.UIManager;

public class ConditionRenderer extends AbstractRenderer {

    public ConditionRenderer() {
        super("Condition Shouldn't be empty. Additonal Object/Data is needed for the action");
    }

    @Override
    public void render(JComponent comp, TestStep step, Object value) {
        if (step.isCommented()) {
            Color c = UIManager.getColor("ing.commentedForeground");
            comp.setForeground(c != null ? c : Color.lightGray);
            comp.setFont(new Font("Default", Font.ITALIC, 11));
            return;
        }
        boolean empty = isEmpty(value);
        if (empty && !isOptional(step)) {
            setEmpty(comp);
            applyConditionGhost(comp, step, true);
        } else if (empty) {
            setDefault(comp);
            applyConditionGhost(comp, step, false);
        } else {
            setDefault(comp);
        }
    }

    /**
     * Paint always-visible ghost text describing the expected Condition in an
     * empty cell (e.g. "JSONPath to the element", "optional timeout in ms"), so
     * the user never has to guess. Only for explicit specs whose action takes a
     * condition.
     */
    private void applyConditionGhost(JComponent comp, TestStep step, boolean required) {
        try {
            ArgSpec spec = ActionSpecCatalog.forAction(step.getAction());
            if (spec == null || !spec.isExplicit()) return;
            if (spec.conditionKind() == ConditionKind.NONE) return;
            String hint = spec.conditionHint();
            if (hint == null || hint.isEmpty()) return;
            if (comp instanceof JLabel) {
                ((JLabel) comp).setText(hint);
            }
            comp.setFont(comp.getFont().deriveFont(Font.ITALIC));
            if (!required) {
                comp.setForeground(new Color(140, 140, 140));
            }
            comp.setToolTipText(hint);
        } catch (Throwable ignore) {
            // never let a ghost hint break rendering
        }
    }

    private Boolean isOptional(TestStep step) {
        if (MethodInfoManager.containsAction(step.getAction())) {
            return !MethodInfoManager.getActionFor(step.getAction()).condition().isMandatory();
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

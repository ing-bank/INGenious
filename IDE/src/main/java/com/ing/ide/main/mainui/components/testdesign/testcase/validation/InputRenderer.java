package com.ing.ide.main.mainui.components.testdesign.testcase.validation;

import com.ing.datalib.component.TestStep;
import com.ing.datalib.testdata.model.TestDataModel;
import com.ing.engine.mcp.ActionSpecCatalog;
import com.ing.engine.mcp.ArgSpec;
import com.ing.engine.support.methodInf.MethodInfoManager;
import com.ing.ingenious.api.annotation.Action;
import java.awt.Color;
import java.awt.Font;
import java.util.List;
import java.util.Objects;
import javax.swing.JComponent;
import javax.swing.JLabel;

public class InputRenderer extends AbstractRenderer {
    String testDataNotPresent = "TestData/Column not avaliable in the Project";
    String inValidInput =
        "Syntax error. Input should be one of [@val , #val, %var% ,=Function ,Sheet:Column]";
    String shouldBeEmpty = "Syntax error. Input should be empty for the Action";

    public InputRenderer() {
        super(
            "Input Shouldn't be empty.It should be one of [@val ,%var% ,=Function ,Sheet:Column]"
        );
    }

    public void render(JComponent comp, TestStep step, Object value) {
        if (!step.isCommented().booleanValue()) {
            if (isEmpty(value).booleanValue()) {
                if (!isOptional(step).booleanValue()) {
                    setEmpty(comp);
                    applyInputGhost(comp, step, true);
                } else {
                    setDefault(comp);
                    applyInputGhost(comp, step, false);
                }
            } else if (isNotNeeded(step).booleanValue()) {
                setNotPresent(comp, this.shouldBeEmpty);
            } else if (
                step.isTestDataStep().booleanValue() &&
                !step.getObject().matches("String Operations")
            ) {
                if (isTestDataPresent(step).booleanValue()) {
                    setDefault(comp);
                } else {
                    setNotPresent(comp, this.testDataNotPresent);
                }
            } else if (isInputValid(value, step.getObject()).booleanValue()) {
                String specMsg = specViolation(step, value);
                if (specMsg != null) {
                    setNotPresent(comp, specMsg);
                } else {
                    setDefault(comp);
                    applyHint(comp, step);
                }
            } else {
                setNotPresent(comp, this.inValidInput);
            }
        } else {
            setDefault(comp);
            comp.setForeground(Color.lightGray);
            comp.setFont(new Font("Default", Font.ITALIC, 11));
        }
    }

    private Boolean isOptional(TestStep step) {
        if (step.getObject().matches("Execute")) return Boolean.valueOf(true);
        if (MethodInfoManager.containsAction(step.getAction())) {
            return Boolean.valueOf(
                !MethodInfoManager
                    .getActionFor(step.getAction())
                    .input()
                    .isMandatory()
                    .booleanValue()
            );
        }
        return Boolean.valueOf(true);
    }

    private Boolean isNotNeeded(TestStep step) {
        if (step.getObject().matches("Execute")) return Boolean.valueOf(false);
        if (MethodInfoManager.containsAction(step.getAction())) {
            return MethodInfoManager.getActionFor(step.getAction()).input().isNotNeeded();
        }
        return Boolean.valueOf(true);
    }

    private Boolean isTestDataPresent(TestStep step) {
        String[] data = step.getTestDataFromInput();
        return Boolean.valueOf(
            step
                .getProject()
                .getTestData()
                .getAllEnvironments()
                .stream()
                .map(sTestData -> sTestData.getByName(data[0]))
                .anyMatch(tdModelDef -> hasColumn(tdModelDef, data[1]))
        );
    }

    private boolean hasColumn(TestDataModel tdModel, String column) {
        return (tdModel != null && tdModel.getColumnIndex(column) >= 0);
    }

    private Boolean isInputValid(Object value, String objectName) {
        String val = Objects.toString(value, "").trim();
        if (objectName.matches("String Operations")) {
            return true;
        } else {
            if (val.matches("(@.+)|(=.+)|(%.+%)|(#.+)")) return true; else if ( // return Boolean.valueOf(val.matches("(@.+)|(=.+)|(%.+%)"));
                val.startsWith("<") || val.startsWith("{") || val.startsWith("[")
            ) return true; else return false;
        }
    }

    /**
     * Type-specific validation against the action's {@link ArgSpec}. Returns a
     * human message (expected format + example) when the Input does not match
     * the action's declared format, or {@code null} when it is fine. Only
     * explicit specs validate, so custom/un-specced actions are never flagged.
     */
    private String specViolation(TestStep step, Object value) {
        try {
            ArgSpec spec = ActionSpecCatalog.forAction(step.getAction());
            if (spec == null || !spec.isExplicit()) return null;
            String input = Objects.toString(value, "");
            String condition = step.getCondition() == null ? "" : step.getCondition();
            List<String> v = spec.validate(input, condition);
            return v.isEmpty() ? null : v.get(0);
        } catch (Throwable ignore) {
            return null;
        }
    }

    /** Show the expected format + example as a hover hint on a valid cell. */
    private void applyHint(JComponent comp, TestStep step) {
        try {
            ArgSpec spec = ActionSpecCatalog.forAction(step.getAction());
            if (spec == null || !spec.isExplicit()) return;
            String hint = spec.inputHint();
            if (hint != null && !hint.isEmpty()) comp.setToolTipText(hint);
        } catch (Throwable ignore) {
            // never let a hint break rendering
        }
    }

    /**
     * Paint always-visible ghost text describing the expected Input in an empty
     * cell, so the user never has to guess. Only for explicit specs on actions
     * that actually take an input.
     */
    private void applyInputGhost(JComponent comp, TestStep step, boolean required) {
        try {
            if (step.getObject() != null && step.getObject().matches("Execute")) return;
            if (isNotNeeded(step).booleanValue()) return; // action takes no input
            ArgSpec spec = ActionSpecCatalog.forAction(step.getAction());
            if (spec == null || !spec.isExplicit()) return;
            String hint = spec.inputHint();
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

    @Override
    protected Object getColumnValue(TestStep step) {
        return step.getInput();
    }
}

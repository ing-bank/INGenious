package com.ing.ide.main.mainui.components.testdesign.testcase.validation;

import com.ing.datalib.component.TestStep;
import com.ing.datalib.testdata.model.TestDataModel;
import com.ing.ingenious.api.annotation.Action;
import com.ing.engine.support.methodInf.MethodInfoManager;
import java.awt.Color;
import java.awt.Font;
import java.util.Objects;
import javax.swing.JComponent;

public class InputRenderer extends AbstractRenderer {
	String testDataNotPresent = "TestData/Column not avaliable in the Project";
	String inValidInput = "Syntax error. Input should be one of [@val , #val, %var% ,=Function ,Sheet:Column]";
	String shouldBeEmpty = "Syntax error. Input should be empty for the Action";

	public InputRenderer() {
            super("Input Shouldn't be empty.It should be one of [@val ,%var% ,=Function ,Sheet:Column]");
        }

	public void render(JComponent comp, TestStep step, Object value) {
            if (!step.isCommented().booleanValue()) {
                if (isEmpty(value).booleanValue()) {
                        if (!isOptional(step).booleanValue()) {
                                setEmpty(comp);
                        } else {
                                setDefault(comp);
                        }
                } else if (isNotNeeded(step).booleanValue()) {
                        setNotPresent(comp, this.shouldBeEmpty);
                } else if (step.isTestDataStep().booleanValue() && !step.getObject().matches("String Operations")) {
                        if (isTestDataPresent(step).booleanValue()) {
                                setDefault(comp);
                        } else {
                                setNotPresent(comp, this.testDataNotPresent);
                        }
                }else if (isInputValid(value, step.getObject()).booleanValue()) {
                        setDefault(comp);
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
		if (step.getObject().matches("Execute"))
			return Boolean.valueOf(true);
		if (MethodInfoManager.containsAction(step.getAction())) {
			return Boolean.valueOf(!MethodInfoManager.getActionFor(step.getAction()).input()
					.isMandatory().booleanValue());
		}
		return Boolean.valueOf(true);
	}

	private Boolean isNotNeeded(TestStep step) {
		if (step.getObject().matches("Execute"))
			return Boolean.valueOf(false);
		if (MethodInfoManager.containsAction(step.getAction())) {
			return MethodInfoManager.getActionFor(step.getAction()).input().isNotNeeded();
		}
		return Boolean.valueOf(true);
	}

	private Boolean isTestDataPresent(TestStep step) {
		String[] data = step.getTestDataFromInput();
		return Boolean.valueOf(step.getProject().getTestData().getAllEnvironments().stream()
				.map(sTestData -> sTestData.getByName(data[0])).anyMatch(tdModelDef -> hasColumn(tdModelDef, data[1])));
	}

	private boolean hasColumn(TestDataModel tdModel, String column) {
		return (tdModel != null && tdModel.getColumnIndex(column) >= 0);
	}

	private Boolean isInputValid(Object value, String objectName) {
		String val = Objects.toString(value, "").trim();
                if (objectName.matches("String Operations")){
                    return true;
                } else{
                    if (val.matches("(@.+)|(=.+)|(%.+%)|(#.+)"))
                    // return Boolean.valueOf(val.matches("(@.+)|(=.+)|(%.+%)"));
                       return true;
                    else if (val.startsWith("<") || val.startsWith("{") || val.startsWith("["))
                       return true;
                    else
                       return false;
                }
	}
}

package com.ing.ide.main.mainui.components.testdesign.testcase.validation;

import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestStep;
import com.ing.datalib.or.common.ORPageInf;
import com.ing.engine.support.methodInf.MethodInfoManager;
import com.ing.engine.support.methodInf.ObjectType;
import java.awt.Color;
import java.awt.Font;
import java.util.Objects;
import javax.swing.JComponent;

/**
 *
 *
 */
public class ActionRenderer extends AbstractRenderer {

    final String actionNotPresent = "Action not available/Not a valid action";
    final String reusableNotPresent = "Reusable is not available in the Project";

    public ActionRenderer() {
        super("Action Shouldn't be empty.It should be either an action or Reusable");
    }

    @Override
    public void render(JComponent comp, TestStep step, Object value) {
        if (!step.isCommented()) {
            if (isEmpty(value)) {
                setEmpty(comp);
            } else if (step.isReusableStep()) {
                if (isReusablePresent(step)) {
                    setDefault(comp);
                } else {
                    setNotPresent(comp, reusableNotPresent);
                }
            } else if (step.isWebserviceStartStep()) {
                setWebserviceStart(comp);
            } else if (step.isWebserviceStopStep()) {
                setWebserviceStop(comp);
            } else if ((step.isWebserviceRequestStep())) {
                setWebserviceRequest(comp);
            } else if ((step.getObject().equals("Execute"))) {
                setReusable(comp);
            } else if (isActionValid(step, value)) {
                setDefault(comp);
            } else {
                setNotPresent(comp, actionNotPresent);
            }
        } else {
            setDefault(comp);
            comp.setForeground(Color.lightGray);
            comp.setFont(new Font("Default", Font.ITALIC, 11));
        }
    }

    private Boolean isReusablePresent(TestStep step) {
        String[] data = step.getReusableData();
        Scenario scenario = step.getProject().getScenarioByName(data[0]);
        if (scenario != null) {
            return scenario.getTestCaseByName(data[1]) != null;
        }
        return false;
    }

    private String getDesc(Object value) {
        String val = MethodInfoManager.getDescriptionFor(
                value.toString());
        return val.isEmpty() ? null : val;
    }

    private Boolean isActionValid(TestStep step, Object value) {
        String action = Objects.toString(value, "").trim();
        String objectName = step.getObject();
        Boolean valid = false;

        switch (objectName) {
            case "Execute":
                valid = true;
                break;
            case "Browser":
                valid = MethodInfoManager.getMethodListFor(ObjectType.BROWSER)
                        .contains(action);
                break;
            case "Mobile":
                valid = MethodInfoManager.getMethodListFor(ObjectType.MOBILE)
                        .contains(action);
                break;
            case "Database":
                valid = MethodInfoManager.getMethodListFor(ObjectType.DATABASE)
                        .contains(action);
                break;
            case "ProtractorJS":
                valid = MethodInfoManager.getMethodListFor(ObjectType.PROTRACTORJS)
                        .contains(action);
                break;
            case "Webservice":
                valid = MethodInfoManager.getMethodListFor(ObjectType.WEBSERVICE)
                        .contains(action);
                break;
            case "File":
                valid = MethodInfoManager.getMethodListFor(ObjectType.FILE)
                        .contains(action);
                break;
            case "Synthetic Data":
                valid = MethodInfoManager.getMethodListFor(ObjectType.DATA)
                        .contains(action);
                break;
            case "Queue":
                valid = MethodInfoManager.getMethodListFor(ObjectType.QUEUE)
                        .contains(action);
                break;
            case "Kafka":
                valid = MethodInfoManager.getMethodListFor(ObjectType.KAFKA)
                        .contains(action);
                break;
            default:
                if (isWebObject(step)) {
                    valid = MethodInfoManager.getMethodListFor(ObjectType.PLAYWRIGHT, ObjectType.WEB).contains(action);
                } else if (isMobileObject(step)) {
                    valid = MethodInfoManager.getMethodListFor(ObjectType.APP).contains(action);
                }
                break;
        }

        if (!valid) {
            valid = MethodInfoManager.getMethodListFor(ObjectType.ANY)
                    .contains(action);
        }
        return valid;
    }

    private boolean isWebObject(TestStep step) {
        ORPageInf page = step.getProject().
                getObjectRepository().getWebOR().getPageByName(step.getReference());
        return page != null && page.getObjectGroupByName(step.getObject()) != null;
    }

    private boolean isMobileObject(TestStep step) {
        ORPageInf page = step.getProject().
                getObjectRepository().getMobileOR().getPageByName(step.getReference());
        return page != null && page.getObjectGroupByName(step.getObject()) != null;
    }
}

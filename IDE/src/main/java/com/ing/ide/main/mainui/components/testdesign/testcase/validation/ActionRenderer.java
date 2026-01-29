package com.ing.ide.main.mainui.components.testdesign.testcase.validation;

import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestStep;
import com.ing.datalib.or.common.ORPageInf;
import com.ing.datalib.or.web.ResolvedWebObject;
import com.ing.engine.support.methodInf.MethodInfoManager;
import com.ing.engine.support.methodInf.ObjectType;
import java.awt.Color;
import java.awt.Font;
import java.util.Objects;
import javax.swing.JComponent;

/**
 * Renderer for the “Action” column of a test step, validating actions and
 * reusable-step references while applying appropriate visual feedback in the UI.
 * <p>
 * {@code ActionRenderer} evaluates whether an action is empty, belongs to a
 * valid action set for the object type (browser, mobile, file, webservice,
 * etc.), or refers to a valid reusable test step. It highlights invalid actions,
 * marks valid ones with default styling, and applies special formatting to
 * webservice start/stop/request and text-entry operations.
 * </p>
 *
 * <p>
 * The renderer uses {@link MethodInfoManager} to resolve valid action lists,
 * checks reusable existence via the project’s scenario/testcase structure, and
 * detects whether the referenced object is web, mobile, or general-purpose
 * through Object Repository lookups.
 * </p>
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
            } else if ((step.isSetTextStep())) {
                setText(comp);
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
            case "General":
                valid = MethodInfoManager.getMethodListFor(ObjectType.GENERAL)
                        .contains(action);
                break;   
            case "String Operations":
                valid = MethodInfoManager.getMethodListFor(ObjectType.STRINGOPERATIONS)
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
        var repo = step.getProject().getObjectRepository();
        String pageToken = step.getReference();
        String objectName = step.getObject();
        ResolvedWebObject.PageRef ref = ResolvedWebObject.PageRef.parse(pageToken);
        ResolvedWebObject r =
            (ref != null && ref.name != null && ref.scope != null)
            ? repo.resolveWebObject(ref, objectName)
            : repo.resolveWebObjectWithScope(pageToken, objectName);  
        return r != null && r.isPresent();
    }

    private boolean isMobileObject(TestStep step) {
        ORPageInf page = step.getProject().
                getObjectRepository().getMobileOR().getPageByName(step.getReference());
        return page != null && page.getObjectGroupByName(step.getObject()) != null;
    }
}

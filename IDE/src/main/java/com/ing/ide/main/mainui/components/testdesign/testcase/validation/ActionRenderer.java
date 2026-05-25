package com.ing.ide.main.mainui.components.testdesign.testcase.validation;

import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestStep;
import com.ing.datalib.or.web.ResolvedWebObject;
import com.ing.datalib.or.mobile.ResolvedMobileObject;
import com.ing.engine.support.ObjectTypeUtil;
import com.ing.engine.support.methodInf.MethodInfoManager;
import com.ing.ingenious.api.types.ObjectType;
import java.awt.Color;
import java.awt.Font;
import java.util.Objects;
import javax.swing.JComponent;
import javax.swing.UIManager;

/**
 * Renderer for the “Action” column of a test step, validating actions and
 * reusable-step references while applying appropriate visual feedback in the UI.
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
                if (isPristineStep(step)) {
                    setDefault(comp);
                } else {
                    setEmpty(comp);
                }
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
                setExecute(comp);
            } else if (isActionValid(step, value)) {
                setDefault(comp);
            } else {
                setNotPresent(comp, actionNotPresent);
            }
        } else {
            setDefault(comp);
            Color c = UIManager.getColor("ing.commentedForeground");
            comp.setForeground(c != null ? c : Color.lightGray);
            comp.setFont(new Font("Default", Font.ITALIC, 11));
        }
    }

    private Boolean isReusablePresent(TestStep step) {
        String[] data = step.getReusableData();
        Scenario scenario = step.getProject().getReusableScenarioByName(data[0]);
        if (scenario != null) {
            return scenario.getTestCaseByName(data[1]) != null;
        }
        return false;
    }

    private String getDesc(Object value) {
        String val = MethodInfoManager.getDescriptionFor(value.toString());
        return val.isEmpty() ? null : val;
    }

    /**
     * Validates if the given action is valid for the test step's object type.
     * <p>
     * Execute objects always accept any action. For other objects, checks if the action
     * is available in the method list for known object types (Browser, Database, etc.),
     * web/mobile page objects from the repository, or falls back to generic actions.
     * </p>
     *
     * @param step the test step containing the object type
     * @param value the action to validate
     * @return true if the action is valid for the object type, false otherwise
     */
    private Boolean isActionValid(TestStep step, Object value) {
        String action = Objects.toString(value, "").trim();
        String objectName = step.getObject();

        // Execute always accepts any action (reusable)
        if ("Execute".equals(objectName)) {
            return true;
        }

        // Check if it's a known object type (Browser, Mobile, Database, etc.)
        if (ObjectTypeUtil.isKnownType(objectName)) {
            return MethodInfoManager.getMethodListFor(objectName).contains(action);
        }

        // Check if it's a web or mobile page object
        if (isWebObject(step)) {
            return MethodInfoManager.getMethodListFor(ObjectType.PLAYWRIGHT, ObjectType.WEB).contains(action);
        }
        if (isMobileObject(step)) {
            return MethodInfoManager.getMethodListFor(ObjectType.APP).contains(action);
        }

        // Fallback to generic actions available for any object
        return MethodInfoManager.getMethodListFor(ObjectType.ANY).contains(action);
    }

    private boolean isWebObject(TestStep step) {
        var repo = step.getProject().getObjectRepository();
        String pageToken = step.getReference();
        String objectName = step.getObject();

        ResolvedWebObject.PageRef ref = ResolvedWebObject.PageRef.parse(pageToken);
        ResolvedWebObject r = (ref != null && ref.name != null && ref.scope != null)
            ? repo.resolveWebObject(ref, objectName)
            : repo.resolveWebObjectWithScope(pageToken, objectName);
        return r != null && r.isPresent();
    }

    private boolean isMobileObject(TestStep step) {
        var repo = step.getProject().getObjectRepository();
        String pageToken = step.getReference();
        String objectName = step.getObject();

        ResolvedMobileObject.PageRef ref = ResolvedMobileObject.PageRef.parse(pageToken);
        ResolvedMobileObject r = (ref != null && ref.name != null && ref.scope != null)
            ? repo.resolveMobileObject(ref, objectName)
            : repo.resolveMobileObjectWithScope(pageToken, objectName);

        return r != null && r.isPresent();
    }
}
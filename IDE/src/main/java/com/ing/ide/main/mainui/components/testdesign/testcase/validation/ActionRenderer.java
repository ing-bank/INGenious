package com.ing.ide.main.mainui.components.testdesign.testcase.validation;

import com.ing.datalib.component.ReusableRef;
import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestStep;
import com.ing.datalib.or.mobile.ResolvedMobileObject;
import com.ing.datalib.or.sap.ResolvedSapObject;
import com.ing.datalib.or.structureddata.ResolvedStructuredDataObject;
import com.ing.datalib.or.web.ResolvedWebObject;
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
    final String reusableHasError = "Reusable has IDE validation error(s)";
    final String reusableNotPresentScoped = "Reusable is not available in %s scope";

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
                if (!isReusablePresent(step)) {
                    // Use scope-aware error message if reference is scoped
                    String errorMsg = getReusableErrorMessage(step.getAction());
                    setNotPresent(comp, errorMsg);
                } else if (TestCaseValidation.reusableHasError(step)) {
                    setNotPresent(comp, reusableHasError);
                } else {
                    setDefault(comp);
                    applyReusableScopeColor(comp, step.getReference());
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
        ReusableRef ref;
        try {
            ref = step.getEffectiveReusableRef();
        } catch (IllegalArgumentException ex) {
            return false;
        }
        if (ref == null) {
            return false;
        }

        String scenarioName = ref.getScenarioName();
        String testCaseName = ref.getTestCaseName();

        if (
            ref.getScope() == ReusableRef.Scope.PROJECT ||
            ref.getScope() == ReusableRef.Scope.UNSCOPED
        ) {
            Scenario scenario = step.getProject().getReusableScenarioByName(scenarioName);
            if (scenario != null && scenario.getTestCaseByName(testCaseName) != null) {
                return true;
            }
        }

        if (
            ref.getScope() == ReusableRef.Scope.SHARED ||
            ref.getScope() == ReusableRef.Scope.UNSCOPED
        ) {
            Scenario sharedScenario = step
                .getProject()
                .getSharedReusableScenarioByName(scenarioName);
            if (sharedScenario != null && sharedScenario.getTestCaseByName(testCaseName) != null) {
                return true;
            }
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

        if (isWebObject(step)) {
            return MethodInfoManager
                .getMethodListFor(ObjectType.PLAYWRIGHT, ObjectType.WEB)
                .contains(action);
        }

        if (isMobileObject(step)) {
            return MethodInfoManager.getMethodListFor(ObjectType.APP).contains(action);
        }

        if (isStructuredDataObject(step)) {
            return MethodInfoManager.getMethodListFor(ObjectType.STRUCTUREDDATA).contains(action);
        }

        if (isSapObject(step)) {
            return MethodInfoManager.getMethodListFor(ObjectType.SAP).contains(action);
        }

        // Fallback to generic actions available for any object
        return MethodInfoManager.getMethodListFor(ObjectType.ANY).contains(action);
    }

    @Override
    protected Object getColumnValue(TestStep step) {
        return step.getAction();
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

    private boolean isStructuredDataObject(TestStep step) {
        var repo = step.getProject().getObjectRepository();
        String pageToken = step.getReference();
        String objectName = step.getObject();

        ResolvedStructuredDataObject.PageRef ref = ResolvedStructuredDataObject.PageRef.parse(
            pageToken
        );
        ResolvedStructuredDataObject r = (ref != null && ref.name != null && ref.scope != null)
            ? repo.resolveStructuredDataObject(ref, objectName)
            : repo.resolveStructuredDataObjectWithScope(pageToken, objectName);

        return r != null && r.isPresent();
    }

    private boolean isSapObject(TestStep step) {
        var repo = step.getProject().getObjectRepository();
        String pageToken = step.getReference();
        String objectName = step.getObject();

        ResolvedSapObject.PageRef ref = ResolvedSapObject.PageRef.parse(pageToken);
        ResolvedSapObject r = (ref != null && ref.name != null && ref.scope != null)
            ? repo.resolveSapObject(ref, objectName)
            : repo.resolveSapObjectWithScope(pageToken, objectName);

        return r != null && r.isPresent();
    }

    /**
     * Gets the scope-aware error message for reusable validation.
     * If the reference is scoped (e.g., [PROJECT] or [SHARED]), includes scope in the message.
     *
     * @param refString the reusable reference string
     * @return appropriate error message with scope information if applicable
     */
    private String getReusableErrorMessage(String refString) {
        try {
            ReusableRef ref = ReusableRef.parse(refString);
            if (ref.getScope() != ReusableRef.Scope.UNSCOPED) {
                return String.format(reusableNotPresentScoped, ref.getScope());
            }
        } catch (IllegalArgumentException ex) {
            return reusableNotPresent;
        }
        return reusableNotPresent;
    }

    private void applyReusableScopeColor(JComponent comp, String reference) {
        String ref = Objects.toString(reference, "").trim();
        if (ref.startsWith("[Shared]")) {
            comp.setForeground(new Color(0, 128, 0));
        } else if (ref.startsWith("[Project]")) {
            comp.setForeground(Color.BLACK);
        }
    }
}

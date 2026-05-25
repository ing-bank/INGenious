package com.ing.ide.main.mainui.components.testdesign.testcase.validation;

import com.ing.datalib.component.TestStep;
import com.ing.datalib.or.web.ResolvedWebObject;
import com.ing.datalib.or.mobile.ResolvedMobileObject;

import com.ing.engine.support.ObjectTypeUtil;
import java.awt.Color;
import java.awt.Font;
import java.util.Objects;
import javax.swing.JComponent;
import javax.swing.UIManager;

/**
 * Renderer responsible for validating and visually marking the “Object” column
 * of a test step within the Test Design UI.
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
                if (isPristineStep(step)) {
                    setDefault(comp);
                } else {
                    setEmpty(comp);
                }
            } else if ("Execute".equals(Objects.toString(value, "").trim())) {
                setExecute(comp);
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

    private Boolean isObjectPresent(TestStep step) {
        var repo = step.getProject().getObjectRepository();
        String pageToken = step.getReference();
        String objectName = step.getObject();
        ResolvedWebObject.PageRef wref = ResolvedWebObject.PageRef.parse(pageToken);
        if (wref != null && wref.name != null && wref.scope != null) {
            if (repo.resolveWebObject(wref, objectName) != null) {
                return true;
            }
        } else if (repo.resolveWebObjectWithScope(pageToken, objectName) != null) {
            return true;
        }
        ResolvedMobileObject.PageRef mref = ResolvedMobileObject.PageRef.parse(pageToken);
        if (mref != null && mref.name != null && mref.scope != null) {
            return repo.resolveMobileObject(mref, objectName) != null;
        }
        return repo.resolveMobileObjectWithScope(pageToken, objectName) != null;
    }

    /**
     * Validates if the given value represents a known object type.
     * <p>
     * Checks if the value is a recognized system object type (e.g., Execute, Browser,
     * Mobile, Database, Webservice, etc.) that the framework supports.
     * </p>
     *
     * @param value the object type value to validate
     * @return true if the value is a known object type, false otherwise
     */
    private Boolean isValidObject(Object value) {
        return ObjectTypeUtil.isKnownType(Objects.toString(value, "").trim());
    }
}
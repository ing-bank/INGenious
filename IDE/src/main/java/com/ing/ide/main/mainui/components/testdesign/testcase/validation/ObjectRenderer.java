package com.ing.ide.main.mainui.components.testdesign.testcase.validation;

import com.ing.datalib.component.TestStep;
import com.ing.datalib.or.web.ResolvedWebObject;
import com.ing.datalib.or.mobile.ResolvedMobileObject;

import java.awt.Color;
import java.awt.Font;
import javax.swing.JComponent;

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
                setEmpty(comp);
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
            comp.setForeground(Color.lightGray);
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

    private Boolean isValidObject(Object value) {
        String v = java.util.Objects.toString(value, "").trim();
        return v.matches("^(Execute|App|Browser|Database|Webservice|Kafka|Synthetic Data|Queue|File|General|String Operations|Mobile)$");
    }
}
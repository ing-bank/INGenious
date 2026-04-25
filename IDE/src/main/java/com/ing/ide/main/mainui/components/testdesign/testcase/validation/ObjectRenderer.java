package com.ing.ide.main.mainui.components.testdesign.testcase.validation;

import com.ing.datalib.component.TestStep;
import com.ing.datalib.or.web.ResolvedWebObject;
import com.ing.datalib.or.mobile.ResolvedMobileObject;

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
            String objectName = Objects.toString(step.getObject(), "").trim();
            String reference  = Objects.toString(step.getReference(), "").trim();

            if (objectName.isEmpty() || reference.isEmpty()) {
                setDefault(comp);
                return;
            }
            String pageName = reference
                    .replace("[Project] ", "")
                    .replace("[Shared] ", "")
                    .trim();

            ResolvedWebObject rwo =
                step.getProject()
                    .getObjectRepository()
                    .resolveWebObjectWithScope(pageName, objectName, step);

            if (rwo == null) {
                setNotPresent(comp, objNotPresent);
            } else {
                setDefault(comp);
                comp.setEnabled(true);
                comp.setFont(comp.getFont().deriveFont(java.awt.Font.BOLD));
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
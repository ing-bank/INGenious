package com.ing.ide.main.mainui.components.testdesign.testcase.validation;

import com.ing.datalib.component.TestStep;
import com.ing.datalib.or.web.ResolvedWebObject;
import com.ing.datalib.or.mobile.ResolvedMobileObject;
import com.ing.engine.support.ObjectTypeUtil;
import java.awt.Color;
import java.awt.Font;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.JComponent;
import javax.swing.UIManager;

/**
 * Renderer for the “Reference” column of a test step, responsible for validating
 * and decorating page references used in object-based steps.
 *
 */
public class ReferenceRenderer extends AbstractRenderer {

    private static final Set<String> CATEGORY_OBJECTS = Set.of(
        "Execute",
        "App",
        "Browser",
        "Mobile",
        "Database",
        "Webservice",
        "Kafka",
        "Synthetic Data",
        "Queue",
        "File",
        "General",
        "String Operations"
    );

    String objNotPresent = "Object is not present in the Object Repository";

    public ReferenceRenderer() {
        super(buildEmptyRefMessage());
    }

    private static String buildEmptyRefMessage() {
        String allowed = CATEGORY_OBJECTS.stream().collect(Collectors.joining(","));
        return "Reference Shouldn't be empty, except if Object is one of [" + allowed + "]";
    }

    @Override
    public void render(JComponent comp, TestStep step, Object value) {
        String ref = step.getReference();
        String decorated = ref;
        var repo = step.getProject().getObjectRepository();

        var wref = ResolvedWebObject.PageRef.parse(ref);
        var wres = repo.resolveWebObject(wref, step.getObject());

        if (wres == null) {
            var mref = ResolvedMobileObject.PageRef.parse(ref);
            var mres = repo.resolveMobileObject(mref, step.getObject());
            if (mres != null) {
                if (mres.isFromShared()) {
                    decorated = "[Shared] " + mres.getPageName();
                } else if (mres.isFromProject()) {
                    decorated = "[Project] " + mres.getPageName();
                }
            } else {
                decorated = ref;
            }
        } else {
            if (wres.isFromShared()) {
                decorated = "[Shared] " + wres.getPageName();
            } else if (wres.isFromProject()) {
                decorated = "[Project] " + wres.getPageName();
            }
        }

        if (comp instanceof javax.swing.JLabel) {
            javax.swing.JLabel lbl = (javax.swing.JLabel) comp;
            lbl.setText(decorated);
        }

        if (!step.isCommented()) {
            if (isEmpty(value)) {
                if (isOptional(step)) setDefault(comp);
                else setEmpty(comp);
            } else if (step.isPageObjectStep()) {
                if (isObjectPresent(step)) setDefault(comp);
                else setNotPresent(comp, objNotPresent);
            } else {
                setDefault(comp);
            }
        } else {
            setDefault(comp);
            Color c = UIManager.getColor("ing.commentedForeground");
            comp.setForeground(c != null ? c : Color.lightGray);
            comp.setFont(new Font("Default", Font.ITALIC, 11));
        }
    }


    /**
     * Checks if the Reference field is optional for the given test step.
     * <p>
     * The Reference field is optional when the object type is a known system type
     * (e.g., Execute, Browser, Mobile, Database, Webservice) that doesn't require
     * an object repository reference.
     * </p>
     *
     * @param step the test step to check
     * @return true if the Reference field is optional, false otherwise
     */
    private Boolean isOptional(TestStep step) {
        return ObjectTypeUtil.isKnownType(step.getObject());
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
}
package com.ing.ide.main.mainui.components.testdesign.testcase.validation;

import com.ing.datalib.component.TestStep;
import com.ing.datalib.or.web.ResolvedWebObject;
import com.ing.datalib.or.structureddata.ResolvedStructuredDataObject;
import com.ing.datalib.or.mobile.ResolvedMobileObject;
import com.ing.engine.support.ObjectTypeUtil;
import com.ing.datalib.or.sap.ResolvedSapObject;
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
                decorated = decorate(mres, ref);
            } else {
                var sref = ResolvedStructuredDataObject.PageRef.parse(ref);
                var sres = repo.resolveStructuredDataObject(sref, step.getObject());

                if (sres != null) {
                    decorated = decorate(sres, ref);
                } else {
                    var sapref = ResolvedSapObject.PageRef.parse(ref);
                    var sapres = repo.resolveSapObject(sapref, step.getObject());
                    
                    if (sapres != null) {
                        decorated = decorate(sapres, ref);
                    } else {
                        decorated = ref;
                    }
                }
            }
        } else {
            decorated = decorate(wres, ref);
        }

        if (comp instanceof javax.swing.JLabel) {
            javax.swing.JLabel lbl = (javax.swing.JLabel) comp;
            lbl.setText(decorated);
        }

        if (!step.isCommented()) {
            if (isEmpty(value)) {
                if (isPristineStep(step) || isOptional(step)) {
                    setDefault(comp);
                } else {
                    setEmpty(comp);
                }
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

    private String decorate(Object res, String fallback) {
        if (res == null) return fallback;

        if (res instanceof ResolvedWebObject) {
            ResolvedWebObject wres = (ResolvedWebObject) res;
            if (wres.isFromShared()) return "[Shared] " + wres.getPageName();
            if (wres.isFromProject()) return "[Project] " + wres.getPageName();

        } else if (res instanceof ResolvedMobileObject) {
            ResolvedMobileObject mres = (ResolvedMobileObject) res;
            if (mres.isFromShared()) return "[Shared] " + mres.getPageName();
            if (mres.isFromProject()) return "[Project] " + mres.getPageName();

        } else if (res instanceof ResolvedStructuredDataObject) {
            ResolvedStructuredDataObject sres = (ResolvedStructuredDataObject) res;
            if (sres.isFromShared()) return "[Shared] " + sres.getPageName();
            if (sres.isFromProject()) return "[Project] " + sres.getPageName();

        } else if (res instanceof ResolvedSapObject) {
            ResolvedSapObject sapres = (ResolvedSapObject) res;
            if (sapres.isFromShared()) return "[Shared] " + sapres.getPageName();
            if (sapres.isFromProject()) return "[Project] " + sapres.getPageName();
        }

        return fallback;
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

        // Check Web OR
        ResolvedWebObject.PageRef wref = ResolvedWebObject.PageRef.parse(pageToken);
        if ((wref != null && wref.name != null && wref.scope != null) && (repo.resolveWebObject(wref, objectName) != null)
                || (repo.resolveWebObjectWithScope(pageToken, objectName) != null)) {
            return true;
        }
        
        ResolvedMobileObject.PageRef mref = ResolvedMobileObject.PageRef.parse(pageToken);
        if ((mref != null && mref.name != null && mref.scope != null) && (repo.resolveMobileObject(mref, objectName) != null)
                || (repo.resolveMobileObjectWithScope(pageToken, objectName) != null )) {
            return true;
        }
        
        ResolvedStructuredDataObject.PageRef aref = ResolvedStructuredDataObject.PageRef.parse(pageToken);
        if ((aref != null && aref.name != null && aref.scope != null) && (repo.resolveStructuredDataObject(aref, objectName) != null)
                || (repo.resolveStructuredDataObjectWithScope(pageToken, objectName) != null )) {
            return true;
        }

        // Check SAP OR
        ResolvedSapObject.PageRef sref = ResolvedSapObject.PageRef.parse(pageToken);
        if ((sref != null && sref.name != null && sref.scope != null) && (repo.resolveSapObject(sref, objectName) != null)
                || (repo.resolveSapObjectWithScope(pageToken, objectName) != null)) {
            return true;
        }

        return false;
    }
}
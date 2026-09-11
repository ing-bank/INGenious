package com.ing.engine.drivers;

import com.ing.datalib.or.ObjectRepository;
import com.ing.datalib.or.common.ObjectGroup;
import com.ing.datalib.or.sap.ResolvedSapObject;
import com.ing.datalib.or.sap.SapORObject;
import com.ing.engine.constants.ObjectProperty;
import com.ing.engine.core.CommandControl;
import com.ing.engine.core.Control;
import com.ing.engine.drivers.SAPObject.SAPFindType;
import com.ing.engine.drivers.sap.SapGuiSession;
import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.Dispatch;
import java.util.HashMap;
import java.util.Map;

public class SAPObject {
    ActiveXComponent session;
    String pageName;
    String objectName;
    SAPFindType findType;

    public static HashMap<String, Map<String, Map<String, String>>> dynamicValue = new HashMap<>();
    public static HashMap<String, String> globalDynamicValue = new HashMap<>();
    public static String Action = "";

    public enum SAPFindType {
        GLOBAL_OBJECT,
        DEFAULT;

        public static SAPFindType fromString(String val) {
            switch (val.toLowerCase()) {
                case "globalobject":
                    return GLOBAL_OBJECT;
                default:
                    return DEFAULT;
            }
        }
    }

    public SAPObject(CommandControl cc) {
        super();
    }

    public SAPObject(ActiveXComponent Session) {
        this.session = Session;
    }

    /** Driverless model: unwrap the raw COM handle (null when not connected / for fakes). */
    public SAPObject(SapGuiSession sapSession) {
        this.session =
            (sapSession == null || sapSession.raw() == null)
                ? null
                : (ActiveXComponent) sapSession.raw();
    }

    public void setSession(ActiveXComponent session) {
        this.session = session;
    }

    public Dispatch findSAPElement(String objectKey, String pageKey, SAPFindType condition) {
        pageName = pageKey;
        objectName = objectKey;
        findType = condition;
        // Check if object exists in SAP OR before attempting to find properties
        if (getSapObject(pageKey, objectKey) == null) {
            System.out.println(
                "Error: SAP object [" +
                objectKey +
                "] not found on page [" +
                pageKey +
                "] - possible non-SAP action on SAP browser"
            );
            return null;
        }
        String id = getRuntimeValue(getObjectProperty(pageKey, objectKey, ObjectProperty.Id));
        String text = getRuntimeValue(getObjectProperty(pageKey, objectKey, ObjectProperty.Text));
        if (id == null) {
            System.out.println(
                "Error: Element ID is null for object [" + objectKey + "] on page [" + pageKey + "]"
            );
            return null;
        }
        if (session == null) {
            System.out.println(
                "Error: no live SAP session - run SAP.initConnection before SAP element steps"
            );
            return null;
        }
        Dispatch parentElement = findByIdWithModalWait(id);
        if (parentElement == null) {
            return null;
        }
        if (text == null || text.isEmpty()) {
            return parentElement;
        } else {
            Dispatch childrenElement = Dispatch.call(parentElement, "Children").toDispatch();
            int count = Dispatch.get(childrenElement, "Count").getInt();
            boolean isTextPresent = false;
            for (int i = 0; i < count; i++) {
                Dispatch child = Dispatch.call(childrenElement, "Item", i).toDispatch();
                String elementText = Dispatch.get(child, "Text").toString();
                if (text.equals(elementText)) {
                    isTextPresent = true;
                    return child;
                }
            }
            if (!isTextPresent) {
                System.out.println("Warning : Element not exist with text : " + text);
            }
        }
        return null;
    }

    private static final java.util.regex.Pattern MODAL_WINDOW = java.util.regex.Pattern.compile(
        "^wnd\\[(\\d+)\\]"
    );
    private static final long MODAL_WAIT_TIMEOUT_MS = 5000;
    private static final long MODAL_WAIT_POLL_MS = 250;

    /**
     * Popups (wnd[1], wnd[2], ...) render asynchronously after the step that triggers them, so a
     * FindById issued immediately after can miss a modal still on its way up. Poll briefly for
     * those ids instead of failing on the first miss; wnd[0] (and non-windowed ids) resolve
     * exactly as before - a single attempt, exception propagates as-is.
     */
    private Dispatch findByIdWithModalWait(String id) {
        java.util.regex.Matcher m = MODAL_WINDOW.matcher(id);
        if (!m.find() || "0".equals(m.group(1))) {
            return session.invoke("FindById", id).toDispatch();
        }
        long deadline = System.currentTimeMillis() + MODAL_WAIT_TIMEOUT_MS;
        Exception last = null;
        while (System.currentTimeMillis() < deadline) {
            try {
                return session.invoke("FindById", id).toDispatch();
            } catch (Exception ex) {
                last = ex;
                try {
                    Thread.sleep(MODAL_WAIT_POLL_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        System.out.println(
            "Error: modal window for [" +
            id +
            "] did not appear within " +
            MODAL_WAIT_TIMEOUT_MS +
            "ms" +
            (last != null ? " (" + last.getMessage() + ")" : "")
        );
        return null;
    }

    public String getObjectProperty(String pageName, String objectName, String propertyName) {
        SapORObject sapObj = getSapObject(pageName, objectName);
        if (sapObj == null) {
            System.out.println(
                "Warning: SAP object [" +
                objectName +
                "] not found on page [" +
                pageName +
                "]. Returning null."
            );
            return null;
        }
        return sapObj.getAttributeByName(propertyName);
    }

    public SapORObject getSapObject(String page, String object) {
        ObjectRepository objRep = Control.getCurrentProject().getObjectRepository();

        try {
            ResolvedSapObject.PageRef ref = ResolvedSapObject.PageRef.parse(page);
            ResolvedSapObject resolved = objRep.resolveSapObject(ref, object);
            if (
                resolved != null &&
                resolved.getGroup() != null &&
                !resolved.getGroup().getObjects().isEmpty()
            ) {
                return (SapORObject) resolved.getGroup().getObjects().get(0);
            }
        } catch (Exception ignore) {}

        // Check SAP project OR
        if (objRep.getSapOR() != null) {
            if (objRep.getSapOR().getPageByName(page) != null) {
                ObjectGroup<SapORObject> group = objRep
                    .getSapOR()
                    .getPageByName(page)
                    .getObjectGroupByName(object);
                if (group != null && !group.getObjects().isEmpty()) {
                    return group.getObjects().get(0);
                }
            }
        }
        // Check SAP shared OR
        if (objRep.getSapSharedOR() != null) {
            if (objRep.getSapSharedOR().getPageByName(page) != null) {
                ObjectGroup<SapORObject> group = objRep
                    .getSapSharedOR()
                    .getPageByName(page)
                    .getObjectGroupByName(object);
                if (group != null && !group.getObjects().isEmpty()) {
                    return group.getObjects().get(0);
                }
            }
        }
        return null;
    }

    public ObjectGroup<SapORObject> getSapObjects(String page, String object) {
        ObjectRepository objRep = Control.getCurrentProject().getObjectRepository();

        try {
            ResolvedSapObject.PageRef ref = ResolvedSapObject.PageRef.parse(page);
            ResolvedSapObject resolved = objRep.resolveSapObject(ref, object);
            if (resolved != null && resolved.getGroup() != null) {
                return (ObjectGroup<SapORObject>) resolved.getGroup();
            }
        } catch (Exception ignore) {}

        if (objRep.getSapOR() != null && objRep.getSapOR().getPageByName(page) != null) {
            ObjectGroup<SapORObject> group = objRep
                .getSapOR()
                .getPageByName(page)
                .getObjectGroupByName(object);
            if (group != null) {
                return group;
            }
        } else if (
            objRep.getSapSharedOR() != null && objRep.getSapSharedOR().getPageByName(page) != null
        ) {
            ObjectGroup<SapORObject> group = objRep
                .getSapSharedOR()
                .getPageByName(page)
                .getObjectGroupByName(object);
            if (group != null) {
                return group;
            }
        }
        System.out.println(
            "Warning: SAP object group [" +
            object +
            "] not found on page [" +
            page +
            "]. Returning null."
        );
        return null;
    }

    private String getRuntimeValue(String value) {
        if (value == null) {
            return null;
        }
        if (findType != null && findType.equals(SAPFindType.GLOBAL_OBJECT)) {
            for (String Key : globalDynamicValue.keySet()) {
                value = value.replace(Key, globalDynamicValue.get(Key));
            }
        }
        if (
            dynamicValue.containsKey(pageName) && dynamicValue.get(pageName).containsKey(objectName)
        ) {
            for (String Key : dynamicValue.get(pageName).get(objectName).keySet()) {
                value = value.replace(Key, dynamicValue.get(pageName).get(objectName).get(Key));
            }
        }
        return value;
    }
}

package com.ing.ide.main.mainui.components.testdesign.or;

import com.ing.datalib.or.common.ORObjectInf;
import com.ing.datalib.or.common.ORPageInf;
import com.ing.datalib.or.common.ObjectGroup;
import com.ing.datalib.or.mobile.MobileOR;
import com.ing.datalib.or.sap.SapOR;
import com.ing.datalib.or.structureddata.StructuredDataOR;
import com.ing.datalib.or.web.WebOR;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper model for Object Repository drag-and-drop (DnD) operations in the Test Design UI.
 * <p>
 * {@code ObjectRepDnD} captures the type of items being dragged (pages, object groups, or objects),
 * stores both the original components and their encoded string representations, and provides
 * convenience methods to extract page/object identifiers from the encoded values.
 * </p>
 *
 * <p>
 * Drag payload values are encoded using a fixed separator and include page scope information
 * (e.g., Project vs Shared) to preserve context across DnD operations.
 * </p>
 */
public class ObjectRepDnD {
    Boolean isPage = false;
    Boolean isGroup = false;
    Boolean isObject = false;
    List<String> values = new ArrayList<>();
    List<Object> components = new ArrayList<>();
    private static final String SEP = "###";

    public Boolean isPage() {
        return isPage;
    }

    public Boolean isGroup() {
        return isGroup;
    }

    public Boolean isObject() {
        return isObject;
    }

    public List<String> getValues() {
        return values;
    }

    public List<Object> getComponents() {
        return components;
    }

    public ObjectRepDnD withPages(List<ORPageInf> pages) {
        isPage = true;
        for (ORPageInf page : pages) {
            values.add(pageToken(page));
            components.add(page);
        }
        return this;
    }

    public ObjectRepDnD withObjectGroups(List<ObjectGroup> groups) {
        isGroup = true;
        for (ObjectGroup group : groups) {
            ORPageInf parent = (ORPageInf) group.getParent();
            values.add(group.getName() + SEP + pageToken(parent));
            components.add(group);
        }
        return this;
    }

    public ObjectRepDnD withObjects(List<ORObjectInf> objects) {
        isObject = true;
        for (ORObjectInf object : objects) {
            ORPageInf page = object.getPage();
            values.add(
                object.getName() + SEP + object.getParent().toString() + SEP + pageToken(page)
            );
            components.add(object);
        }
        return this;
    }

    public String getPageName(String value) {
        if (isPage()) {
            return value;
        }
        if (isGroup()) {
            return value.split(SEP)[1];
        }
        if (isObject()) {
            return value.split(SEP)[2];
        }
        return null;
    }

    public String getObjectName(String value) {
        if (isGroup()) {
            return value.split("###")[0];
        }
        if (isObject()) {
            return value.split("###")[1];
        }
        return null;
    }

    private String scopeOf(ORPageInf page) {
        if (page == null) {
            return "PROJECT";
        }
        Object parent = page.getParent();
        if (parent instanceof WebOR) {
            WebOR root = (WebOR) parent;
            return root.getScope().name();
        }
        if (parent instanceof MobileOR) {
            MobileOR root = (MobileOR) parent;
            return root.getScope().name();
        }
        if (parent instanceof StructuredDataOR) {
            StructuredDataOR root = (StructuredDataOR) parent;
            return root.getScope().name();
        }
        if (parent instanceof SapOR) {
            SapOR root = (SapOR) parent;
            return root.getScope().name();
        }
        return "PROJECT";
    }

    private String pageToken(ORPageInf page) {
        String scope = scopeOf(page);
        if ("SHARED".equalsIgnoreCase(scope)) {
            return "[Shared] " + page.getName();
        } else {
            return "[Project] " + page.getName();
        }
    }
}

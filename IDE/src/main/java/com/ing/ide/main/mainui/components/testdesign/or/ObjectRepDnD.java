
package com.ing.ide.main.mainui.components.testdesign.or;

import com.ing.datalib.or.common.ORObjectInf;
import com.ing.datalib.or.common.ORPageInf;
import com.ing.datalib.or.common.ObjectGroup;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * 
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
            values.add(object.getName() + SEP + object.getParent().toString() + SEP + pageToken(page));
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
        try {
            var m = page.getClass().getMethod("getSource");
            Object src = m.invoke(page);
            if (src != null && src.toString().equalsIgnoreCase("SHARED")) return "SHARED";
        } catch (Exception ignore) { }
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

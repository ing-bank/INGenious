
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
            values.add(page.getName());
            components.add(page);
        }
        return this;
    }

    public ObjectRepDnD withObjectGroups(List<ObjectGroup> groups) {
        isGroup = true;
        for (ObjectGroup group : groups) {
            values.add(
                    group.getName()
                    + "###"
                    + group.getParent().getName());
            components.add(group);
        }
        return this;
    }

    public ObjectRepDnD withObjects(List<ORObjectInf> objects) {
        isObject = true;
        for (ORObjectInf object : objects) {
            values.add(
                    object.getName()
                    + "###"
                    + object.getParent().toString()
                    + "###"
                    + object.getPage().getName()
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
            return value.split("###")[1];
        }
        if (isObject()) {
            return value.split("###")[2];
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
}

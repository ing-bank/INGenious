
package com.ing.datalib.or.web;

import com.ing.datalib.component.utils.FileUtils;
import com.ing.datalib.or.common.ORPageInf;
import com.ing.datalib.or.common.ORUtils;
import com.ing.datalib.or.common.ObjectGroup;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties({"root"})
public class WebORPage implements ORPageInf<WebORObject, WebOR> {

    @JacksonXmlProperty(isAttribute = true, localName = "ref")
    private String name;

    @JacksonXmlProperty(isAttribute = true)
    private String title;

    @JacksonXmlProperty(localName = "ObjectGroup")
    @JacksonXmlElementWrapper(useWrapping = false, localName = "ObjectGroup")
    private List<ObjectGroup<WebORObject>> objectGroups;

    @JsonIgnore
    private WebOR root;

    public WebORPage() {
        this.objectGroups = new ArrayList<>();
    }

    public WebORPage(String name, WebOR root) {
        this.name = name;
        this.root = root;
        this.title = "";
        this.objectGroups = new ArrayList<>();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public List<ObjectGroup<WebORObject>> getObjectGroups() {
        return objectGroups;
    }

    @Override
    public void setObjectGroups(List<ObjectGroup<WebORObject>> objectGroups) {
        this.objectGroups = objectGroups;
        for (ObjectGroup<WebORObject> objectGroup : objectGroups) {
            objectGroup.setParent(this);
        }
    }

    @JsonIgnore
    @Override
    public void removeFromParent() {
        root.setSaved(false);
        root.getPages().remove(this);
        FileUtils.deleteFile(getRepLocation());
    }

    @JsonIgnore
    @Override
    public ObjectGroup<WebORObject> getObjectGroupByName(String groupName) {
        for (ObjectGroup<WebORObject> group : objectGroups) {
            if (group.getName().equalsIgnoreCase(groupName)) {
                return group;
            }
        }
        return null;
    }

    @JsonIgnore
    @Override
    public ObjectGroup<WebORObject> addObjectGroup() {
        String oName = "WObjectGroup";
        int i = 0;
        String objectName;
        do {
            objectName = oName + i++;
        } while (getObjectGroupByName(objectName) != null);

        return addObjectGroup(objectName);
    }

    @JsonIgnore
    @Override
    public ObjectGroup<WebORObject> addObjectGroup(String groupName) {
        if (getObjectGroupByName(groupName) == null) {
            ObjectGroup<WebORObject> group = new ObjectGroup<>(groupName, this);
            objectGroups.add(group);
            new File(group.getRepLocation()).mkdirs();
            group.addObject(groupName);
            root.setSaved(false);
            return group;
        }
        return null;
    }

    @JsonIgnore
    @Override
    public WebORObject getNewObject(String objectName, ObjectGroup<WebORObject> group) {
        return new WebORObject(objectName, group);
    }

    @JsonIgnore
    @Override
    public WebORObject addObject() {
        String oName = "WObject";
        int i = 0;
        String objectName;
        do {
            objectName = oName + i++;
        } while (getObjectGroupByName(objectName) != null);
        return addObject(objectName);
    }

    @JsonIgnore
    @Override
    public WebORObject addObject(String objectName) {
        ObjectGroup<WebORObject> group = addObjectGroup(objectName);
        if (group != null) {
            return group.getObjects().get(0);
        }
        return null;
    }

    @JsonIgnore
    @Override
    public void deleteObjectGroup(String groupName) {
        ObjectGroup<WebORObject> group = getObjectGroupByName(groupName);
        if (group != null) {
            objectGroups.remove(group);
            root.setSaved(false);
        }
    }

    @JsonIgnore
    @Override
    public TreeNode getChildAt(int i) {
        if (objectGroups.get(i).getChildCount() > 1) {
            return objectGroups.get(i);
        }
        return objectGroups.get(i).getChildAt(0);
    }

    @JsonIgnore
    @Override
    public int getChildCount() {
        return objectGroups == null ? 0
                : objectGroups.size();
    }

    @JsonIgnore
    @Override
    public WebOR getParent() {
        return root;
    }

    @JsonIgnore
    @Override
    public int getIndex(TreeNode tn) {
        return objectGroups.indexOf(tn);
    }

    @JsonIgnore
    @Override
    public boolean getAllowsChildren() {
        return true;
    }

    @JsonIgnore
    @Override
    public boolean isLeaf() {
        return getChildCount() == 0;
    }

    @JsonIgnore
    @Override
    public Enumeration children() {
        return Collections.enumeration(objectGroups);
    }

    @JsonIgnore
    @Override
    public WebOR getRoot() {
        return root;
    }

    @JsonIgnore
    @Override
    public void setRoot(WebOR root) {
        this.root = root;
    }

    @Override
    public String toString() {
        return name;
    }

    @JsonIgnore
    @Override
    public TreeNode[] getPath() {
        return (TreeNode[]) ORUtils.getPath(this).getPath();
    }

    @JsonIgnore
    @Override
    public TreePath getTreePath() {
        return ORUtils.getPath(this);
    }

    @Override
    public Boolean rename(String newName) {
        if (getParent().getPageByName(newName) == null) {
            if (FileUtils.renameFile(getRepLocation(), newName)) {
                getRoot().getObjectRepository().renamePage(this, newName);
                setName(newName);
                getParent().setSaved(false);
                return true;
            }
        }
        return false;
    }

    @JsonIgnore
    @Override
    public String getRepLocation() {
        return getParent().getRepLocation() + File.separator + getName();
    }

    @JsonIgnore
    @Override
    public void sort() {
        ORUtils.sort(this);
    }
}

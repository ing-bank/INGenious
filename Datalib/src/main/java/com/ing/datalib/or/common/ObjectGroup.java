
package com.ing.datalib.or.common;

import com.ing.datalib.component.utils.FileUtils;
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
@JsonIgnoreProperties({"parent"})
public class ObjectGroup<T extends ORObjectInf> implements TreeNode {

    @JacksonXmlProperty(isAttribute = true, localName = "ref")
    private String name;

    @JacksonXmlProperty(localName = "Object")
    @JacksonXmlElementWrapper(useWrapping = false, localName = "Object")
    private List<T> objects;

    @JsonIgnore
    private ORPageInf parent;

    public ObjectGroup() {
    }

    public ObjectGroup(String name, ORPageInf parent) {
        this.name = name;
        this.parent = parent;
        this.objects = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<T> getObjects() {
        return objects;
    }

    public void setObjects(List<T> objects) {
        this.objects = objects;
        for (T object : objects) {
            object.setParent(this);
        }
    }

    @JsonIgnore
    public void removeFromParent() {
        parent.getRoot().setSaved(false);
        parent.getObjectGroups().remove(this);
        FileUtils.deleteFile(getRepLocation());
    }

    @JsonIgnore
    public T getObjectByName(String objectName) {
        for (T object : objects) {
            if (object.getName().equalsIgnoreCase(objectName)) {
                return object;
            }
        }
        return null;
    }

    @JsonIgnore
    public T addObject() {
        String oName = "Object";
        int i = 0;
        String objectName;
        do {
            objectName = oName + i++;
        } while (getObjectByName(objectName) != null);

        return addObject(objectName);
    }

    @JsonIgnore
    public T addObject(String objectName) {
        if (getObjectByName(objectName) == null) {
            T object = getNewObject(objectName, this);
            objects.add(object);
            new File(object.getRepLocation()).mkdirs();
            parent.getRoot().setSaved(false);
            return object;
        }
        return null;
    }

    @JsonIgnore
    public void deleteObject(String objectName) {
        T object = getObjectByName(objectName);
        if (object != null) {
            objects.remove(object);
            parent.getRoot().setSaved(false);
        }
    }

    @JsonIgnore
    public T getNewObject(String objectName, ObjectGroup<T> group) {
        return (T) parent.getNewObject(objectName, group);
    }

    @JsonIgnore
    @Override
    public TreeNode getChildAt(int i) {
        return objects.get(i);
    }

    @JsonIgnore
    @Override
    public int getChildCount() {
        return objects == null ? 0
                : objects.size();
    }

    @JsonIgnore
    @Override
    public ORPageInf getParent() {
        return parent;
    }

    @JsonIgnore
    public void setParent(ORPageInf parent) {
        this.parent = parent;
    }

    @JsonIgnore
    @Override
    public int getIndex(TreeNode tn) {
        return objects.indexOf(tn);
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
        return Collections.enumeration(objects);
    }

    @Override
    public String toString() {
        return name;
    }

    @JsonIgnore
    public TreeNode[] getPath() {
        return (TreeNode[]) ORUtils.getPath(this).getPath();
    }

    @JsonIgnore
    public TreePath getTreePath() {
        return ORUtils.getPath(this);
    }

    @JsonIgnore
    public Boolean rename(String newName) {
        if (getParent().getObjectGroupByName(newName) == null) {
            if (FileUtils.renameFile(getRepLocation(), newName)) {
                getParent().getRoot().getObjectRepository().renameObject(this, newName);
                setName(newName);
                getParent().getRoot().setSaved(false);
                return true;
            }
        }
        return false;
    }

    @JsonIgnore
    public String getRepLocation() {
        return getParent().getRepLocation() + File.separator + getName();
    }

    @JsonIgnore
    public ObjectGroup<T> clone(ObjectGroup<T> grp) {
        grp.getObjects().clear();
        for (T object : objects) {
            T obj = grp.addObject(object.getName());
            object.clone(obj);
        }
        return grp;
    }

    @JsonIgnore
    public void sort() {
        ORUtils.sort(this);
    }
}

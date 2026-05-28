
package com.ing.datalib.or.sap;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.ing.datalib.component.utils.FileUtils;
import com.ing.datalib.or.common.ORPageInf;
import com.ing.datalib.or.common.ORUtils;
import com.ing.datalib.or.common.ObjectGroup;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SapORPage implements ORPageInf<SapORObject, SapOR> {

    @JacksonXmlProperty(isAttribute = true, localName = "ref")
    private String name;

    @JacksonXmlProperty(isAttribute = true)
    private String packageName;

    @JacksonXmlProperty(localName = "ObjectGroup")
    @JacksonXmlElementWrapper(useWrapping = false, localName = "ObjectGroup")
    private List<ObjectGroup<SapORObject>> objectGroups;

    @JsonIgnore
    private SapOR root;
    
    @JacksonXmlProperty(isAttribute = true, localName = "source")
    private SapOR.ORScope source = SapOR.ORScope.PROJECT;

    public SapORPage() {
        this.objectGroups = new ArrayList<>();
    }

    public SapORPage(String name, SapOR root) {
        this.name = name;
        this.root = root;
        this.packageName = "";
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

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    @Override
    public List<ObjectGroup<SapORObject>> getObjectGroups() {
        return objectGroups;
    }

    @Override
    public void setObjectGroups(List<ObjectGroup<SapORObject>> objectGroups) {
        this.objectGroups = objectGroups;
        for (ObjectGroup<SapORObject> objectGroup : objectGroups) {
            objectGroup.setParent(this);
            // Ensure nested objects also have their parents set
            if (objectGroup.getObjects() != null && !objectGroup.getObjects().isEmpty()) {
                objectGroup.setObjects(objectGroup.getObjects());
            }
        }
    }

    @JsonIgnore
    @Override
    public void removeFromParent() {
        root.setSaved(false);
        root.getPages().remove(this);
        if (root.getObjectRepository().isUsingYamlFormat()) {
            root.getObjectRepository().deleteSapPageYaml(getName(), root.getScope());
        } else {
            FileUtils.deleteFile(getRepLocation());
        }
    }

    @JsonIgnore
    @Override
    public ObjectGroup<SapORObject> getObjectGroupByName(String groupName) {
        for (ObjectGroup<SapORObject> group : objectGroups) {
            if (group.getName().equalsIgnoreCase(groupName)) {
                return group;
            }
        }
        return null;
    }

    @JsonIgnore
    @Override
    public ObjectGroup<SapORObject> addObjectGroup() {
        String oName = "SapObjectGroup";
        int i = 0;
        String objectName;
        do {
            objectName = oName + i++;
        } while (getObjectGroupByName(objectName) != null);

        return addObjectGroup(objectName);
    }

    @JsonIgnore
    @Override
    public ObjectGroup<SapORObject> addObjectGroup(String groupName) {
        if (getObjectGroupByName(groupName) == null) {
            ObjectGroup<SapORObject> group = new ObjectGroup<>(groupName, this);
            objectGroups.add(group);
            // Only create folder for non-YAML formats
            if (root.getObjectRepository() == null || !root.getObjectRepository().isUsingYamlFormat()) {
                new File(group.getRepLocation()).mkdirs();
            }
            group.addObject(groupName);
            root.setSaved(false);
            
            // Auto-save for YAML format
            if (root.getObjectRepository() != null 
                && root.getObjectRepository().isUsingYamlFormat()) {
                root.getObjectRepository().saveSapPageNow(this);
            }
            return group;
        }
        return null;
    }

    @JsonIgnore
    @Override
    public SapORObject getNewObject(String objectName, ObjectGroup<SapORObject> group) {
        return new SapORObject(objectName, group);
    }

    @JsonIgnore
    @Override
    public SapORObject addObject() {
        String oName = "SapObject";
        int i = 0;
        String objectName;
        do {
            objectName = oName + i++;
        } while (getObjectGroupByName(objectName) != null);

        return addObject(objectName);
    }

    @JsonIgnore
    @Override
    public SapORObject addObject(String objectName) {
        ObjectGroup<SapORObject> group = addObjectGroup(objectName);
        if (group != null) {
            return group.getObjects().get(0);
        }
        return null;
    }

    @JsonIgnore
    @Override
    public void deleteObjectGroup(String groupName) {
        ObjectGroup<SapORObject> group = getObjectGroupByName(groupName);
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
    public SapOR getParent() {
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
    public SapOR getRoot() {
        return root;
    }

    @JsonIgnore
    @Override
    public void setRoot(SapOR root) {
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
    
    public SapOR.ORScope getSource() {
        return source;
    }

    public void setSource(SapOR.ORScope source) {
        this.source = source;
    }
}

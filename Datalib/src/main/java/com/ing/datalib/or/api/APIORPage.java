package com.ing.datalib.or.api;

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

/**
 * API Object Repository page class.
 * Contains object groups for API objects.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties({"root"})
public class APIORPage implements ORPageInf<APIORObject, APIOR> {

    @JacksonXmlProperty(isAttribute = true, localName = "ref")
    private String name;

    @JacksonXmlProperty(isAttribute = true)
    private String title;

    @JacksonXmlProperty(localName = "ObjectGroup")
    @JacksonXmlElementWrapper(useWrapping = false, localName = "ObjectGroup")
    private List<ObjectGroup<APIORObject>> objectGroups;

    @JsonIgnore
    private APIOR root;

    public APIORPage() {
        this.objectGroups = new ArrayList<>();
    }

    public APIORPage(String name, APIOR root) {
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
    public List<ObjectGroup<APIORObject>> getObjectGroups() {
        return objectGroups;
    }

    @Override
    public void setObjectGroups(List<ObjectGroup<APIORObject>> objectGroups) {
        this.objectGroups = objectGroups;
        for (ObjectGroup<APIORObject> objectGroup : objectGroups) {
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
    public ObjectGroup<APIORObject> getObjectGroupByName(String groupName) {
        for (ObjectGroup<APIORObject> group : objectGroups) {
            if (group.getName().equalsIgnoreCase(groupName)) {
                return group;
            }
        }
        return null;
    }

    @JsonIgnore
    @Override
    public ObjectGroup<APIORObject> addObjectGroup() {
        String oName = "APIObjectGroup";
        int i = 0;
        String objectName;
        do {
            objectName = oName + i++;
        } while (getObjectGroupByName(objectName) != null);

        return addObjectGroup(objectName);
    }

    @JsonIgnore
    @Override
    public ObjectGroup<APIORObject> addObjectGroup(String groupName) {
        if (getObjectGroupByName(groupName) == null) {
            ObjectGroup<APIORObject> group = new ObjectGroup<>(groupName, this);
            objectGroups.add(group);
            // API OR uses YAML format - no folder creation needed
            group.addObject(groupName);
            root.setSaved(false);
            
            // Auto-save for YAML format
            if (root.getObjectRepository() != null 
                && root.getObjectRepository().isUsingYamlFormat()) {
                root.getObjectRepository().saveAPIPageNow(this);
            }
            return group;
        }
        return null;
    }

    @JsonIgnore
    @Override
    public APIORObject getNewObject(String objectName, ObjectGroup<APIORObject> group) {
        return new APIORObject(objectName, group);
    }

    @JsonIgnore
    @Override
    public APIORObject addObject() {
        String oName = "APIObject";
        int i = 0;
        String objectName;
        do {
            objectName = oName + i++;
        } while (getObjectGroupByName(objectName) != null);
        return addObject(objectName);
    }

    @JsonIgnore
    @Override
    public APIORObject addObject(String objectName) {
        ObjectGroup<APIORObject> group = addObjectGroup(objectName);
        if (group != null) {
            return group.getObjectByName(objectName);
        }
        return null;
    }

    @JsonIgnore
    @Override
    public void setRoot(APIOR root) {
        this.root = root;
    }

    @JsonIgnore
    @Override
    public APIOR getRoot() {
        return root;
    }

    @JsonIgnore
    @Override
    public void deleteObjectGroup(String groupName) {
        ObjectGroup<APIORObject> group = getObjectGroupByName(groupName);
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
        return objectGroups.size();
    }

    @JsonIgnore
    @Override
    public TreeNode getParent() {
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

    @Override
    public String toString() {
        return name;
    }

    @JsonIgnore
    @Override
    public TreeNode[] getPath() {
        return new TreeNode[]{root, this};
    }

    @JsonIgnore
    @Override
    public TreePath getTreePath() {
        return new TreePath(getPath());
    }

    @JsonIgnore
    @Override
    public String getRepLocation() {
        return root.getRepLocation() + File.separator + "API" + File.separator + name;
    }

    @JsonIgnore
    @Override
    public void sort() {
        ORUtils.sort(this);
    }

    @JsonIgnore
    @Override
    public Boolean rename(String newName) {
        // Check if using YAML format
        if (root.getObjectRepository().isUsingYamlFormat()) {
            // For YAML format, rename the page YAML file
            if (root.getObjectRepository().renameAPIPageYaml(name, newName)) {
                String oldName = name;
                root.getObjectRepository().renamePage(this, newName);
                setName(newName);
                root.setSaved(false);
                return true;
            }
            return false;
        } else {
            // Use original XML folder-based rename
            if (FileUtils.renameFile(getRepLocation(), newName)) {
                String oldName = name;
                root.getObjectRepository().renamePage(this, newName);
                setName(newName);
                root.setSaved(false);
                return true;
            }
        }
        return false;
    }
}

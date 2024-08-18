
package com.ing.datalib.or.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.swing.table.TableModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

/**
 *
 * 
 */
public interface ORObjectInf extends TreeNode, TableModel {

    public String getName();

    public void setName(String name);

    @JsonIgnore
    public void setParent(ObjectGroup group);

    @JsonIgnore
    public TreeNode[] getPath();

    @JsonIgnore
    public ORPageInf getPage();

    @JsonIgnore
    public void removeFromParent();

    @JsonIgnore
    public Boolean rename(String newName);

    @JsonIgnore
    public String getRepLocation();

    @JsonIgnore
    public ORObjectInf clone(ORObjectInf obj);

    @JsonIgnore
    @Override
    public ObjectGroup getParent();

    @JsonIgnore
    public TreePath getTreePath();

    @JsonIgnore
    public Boolean isEqualOf(ORObjectInf object);

}

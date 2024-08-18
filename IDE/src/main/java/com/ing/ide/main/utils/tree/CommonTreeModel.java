
package com.ing.ide.main.utils.tree;

import com.ing.datalib.component.Project;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

/**
 *
 * 
 */
public abstract class CommonTreeModel extends DefaultTreeModel {

    public CommonTreeModel(TreeNode tn) {
        super(tn);
    }

    public abstract void setProject(Project project);

    public void sort(Object selectedObject) {
        if (selectedObject instanceof CommonNode) {
            CommonNode selectedNode = (CommonNode) selectedObject;
            selectedNode.sort();
            reload(selectedNode);
        }
    }

    public DefaultMutableTreeNode getFirstNode() {
        return ((DefaultMutableTreeNode) getRoot()).getFirstLeaf();
    }
}


package com.ing.ide.main.mainui.components.testexecution.tree.model;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.Release;
import com.ing.datalib.component.TestSet;
import com.ing.ide.main.utils.tree.CommonTreeModel;
import javax.swing.tree.TreePath;

/**
 *
 * 
 */
public class TestLabModel extends CommonTreeModel {

    Project project;

    public TestLabModel() {
        super(new TestLabNode());
    }

    @Override
    public TestLabNode getRoot() {
        return (TestLabNode) super.getRoot();
    }

    @Override
    public void setProject(Project project) {
        this.project = project;
        getRoot().setProject(project);
    }

    @Override
    public void valueForPathChanged(TreePath tp, Object o) {
        //Not Needed
    }

    public ReleaseNode addRelease(Release release) {
        if (getRoot().getReleaseBy(release) == null) {
            ReleaseNode rNode = new ReleaseNode(release);
            insertNodeInto(rNode, getRoot(), getRoot().getChildCount());
            return rNode;
        }
        return null;
    }

    public TestSetNode addTestSet(ReleaseNode rNode, TestSet testSet) {
        if (rNode.getTestSetBy(testSet) == null) {
            TestSetNode tsNode = new TestSetNode(testSet);
            insertNodeInto(tsNode, rNode, rNode.getChildCount());
            return tsNode;
        }
        return null;
    }

    public TestSetNode addTestSet(TestSet testset) {
        if (getRoot().getChildCount() > 0) {
            for (ReleaseNode releaseNode : ReleaseNode.toList(getRoot().children())) {
                if (releaseNode.getRelease().equals(testset.getRelease())) {
                    return addTestSet(releaseNode, testset);
                }
            }
        }
        return addTestSet(addRelease(testset.getRelease()), testset);
    }

    

}

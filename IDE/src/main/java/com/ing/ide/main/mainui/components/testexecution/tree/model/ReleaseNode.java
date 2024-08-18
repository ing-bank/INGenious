
package com.ing.ide.main.mainui.components.testexecution.tree.model;

import com.ing.datalib.component.Release;
import com.ing.datalib.component.TestSet;
import com.ing.ide.main.utils.tree.CommonNode;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.tree.TreeNode;

/**
 *
 * 
 */
public class ReleaseNode extends CommonNode {

    Release release;

    public ReleaseNode(Release release) {
        this.release = release;
    }

    public Release getRelease() {
        return release;
    }

    public TestSetNode addTestSetIfNotPresent(TestSet testSet) {
        addTestSet(testSet);
        return getTestSetBy(testSet);
    }

    public TestSetNode addTestSet(TestSet testSet) {
        if (getTestSetBy(testSet) == null) {
            TestSetNode node = new TestSetNode(testSet);
            add(node);
            return node;
        }
        return null;
    }

     public TestSetNode getTestSetBy(TestSet testSet) {
        for (TestSetNode testSetNode : TestSetNode.toList(children())) {
            if (testSetNode.getTestSet().equals(testSet)) {
                return testSetNode;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return release.getName();
    }

   public static List<ReleaseNode> toList(Enumeration<TreeNode> children){
       return Collections.list(children).stream().map(tsNode -> (ReleaseNode) tsNode).collect(Collectors.toList());
       
   }
}

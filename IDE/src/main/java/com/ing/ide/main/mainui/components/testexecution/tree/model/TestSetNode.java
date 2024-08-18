
package com.ing.ide.main.mainui.components.testexecution.tree.model;

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
public class TestSetNode extends CommonNode {

    TestSet testSet;

    public TestSetNode(TestSet testSet) {
        this.testSet = testSet;
    }

    public TestSet getTestSet() {
        return testSet;
    }

    @Override
    public String toString() {
        return testSet.getName();
    }

   public static List<TestSetNode> toList(Enumeration<TreeNode> children){
       return Collections.list(children).stream().map(tsNode -> (TestSetNode) tsNode).collect(Collectors.toList());       
   }
}

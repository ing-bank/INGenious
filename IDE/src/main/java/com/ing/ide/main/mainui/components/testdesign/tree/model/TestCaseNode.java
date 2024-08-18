
package com.ing.ide.main.mainui.components.testdesign.tree.model;

import com.ing.datalib.component.TestCase;
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
public class TestCaseNode extends CommonNode {

    TestCase testCase;

    public TestCaseNode(TestCase testCase) {
        this.testCase = testCase;
    }

    public TestCase getTestCase() {
        return testCase;
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public boolean isLeaf() {
        return true;
    }

    @Override
    public String toString() {
        return testCase.getName();
    }

    public static List<TestCaseNode> toList(Enumeration<TreeNode> children) {
        return Collections.list(children).stream().map(tsNode -> (TestCaseNode) tsNode).collect(Collectors.toList());
    }
}

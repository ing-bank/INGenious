
package com.ing.ide.main.mainui.components.testdesign.tree.model;

import com.ing.datalib.component.Scenario;
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
public class ScenarioNode extends CommonNode {

    Scenario scenario;

    public ScenarioNode(Scenario scenario) {
        this.scenario = scenario;
    }

    public TestCaseNode addTestCaseIfNotPresent(TestCase testCase) {
        addTestCase(testCase);
        return getTestCaseNodeBy(testCase);
    }

    public TestCaseNode addTestCase(TestCase testCase) {
        if (getTestCaseNodeBy(testCase) == null) {
            TestCaseNode node = new TestCaseNode(testCase);
            add(node);
            return node;
        }
        return null;
    }

    public TestCaseNode getTestCaseNodeBy(TestCase testCaseName) {
        for (TestCaseNode testCase : TestCaseNode.toList(children())) {
            if (testCase.getTestCase().equals(testCaseName)) {
                return testCase;
            }
        }
        return null;
    }

    public Scenario getScenario() {
        return scenario;
    }

    @Override
    public String toString() {
        return scenario.getName();
    }

   public static List<ScenarioNode> toList(Enumeration<TreeNode> children){
       return Collections.list(children).stream().map(tsNode -> (ScenarioNode) tsNode).collect(Collectors.toList());
       
   }
   
 
}

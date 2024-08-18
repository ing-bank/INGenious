
package com.ing.ide.main.mainui.components.testdesign.tree;

import com.ing.ide.main.mainui.components.testdesign.tree.model.ProjectTreeModel;
import com.ing.ide.main.mainui.components.testdesign.tree.model.ScenarioNode;
import com.ing.ide.main.mainui.components.testdesign.tree.model.TestCaseNode;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * 
 */
public class TestCaseDnD {

    final ProjectTreeModel model;
    
    private List<TestCaseNode> testCaseList = new ArrayList<>();

    private List<ScenarioNode> scenarioList = new ArrayList<>();

    public TestCaseDnD(ProjectTreeModel model) {
        this.model = model;
    }

    public List<TestCaseNode> getTestCaseList() {
        return testCaseList;
    }

    public TestCaseDnD withTestCaseList(List<TestCaseNode> testCaseList) {
        this.testCaseList = testCaseList;
        return this;
    }

    public List<ScenarioNode> getScenarioList() {
        return scenarioList;
    }

    public TestCaseDnD withScenarioList(List<ScenarioNode> scenarioList) {
        this.scenarioList = scenarioList;
        return this;
    }

    public Boolean isTestCases() {
        return !testCaseList.isEmpty();
    }

}

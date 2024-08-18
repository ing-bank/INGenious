
package com.ing.ide.main.mainui.components.testdesign.tree.model;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;

/**
 *
 * 
 */
public class TestPlanNode extends GroupNode {

    public Project project;

    public TestPlanNode() {
        super("TestPlan");
    }

    public void setProject(Project project) {
        removeAllChildren();
        this.project = project;
        setName(project.getName());
        filterTestCases();
    }

    public void filterTestCases() {
        for (Scenario scenario : project.getScenarios()) {
            for (TestCase testCase : scenario.getTestcasesAlone()) {
                add(testCase);
            }
        }
    }

    public void add(TestCase testCase) {
        addScenarioIfNotPresent(testCase.getScenario()).addTestCaseIfNotPresent(testCase);
    }

    @Override
    public boolean rename(String name) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}

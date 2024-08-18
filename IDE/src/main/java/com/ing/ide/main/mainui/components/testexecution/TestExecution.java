
package com.ing.ide.main.mainui.components.testexecution;

import com.ing.datalib.component.Project;
import com.ing.ide.main.mainui.AppMainFrame;
import com.ing.ide.main.mainui.components.testexecution.testset.TestsetComponent;
import com.ing.ide.main.mainui.components.testexecution.tree.TestSetTree;

/**
 *
 * 
 */
public class TestExecution {

    private final TestExecutionUI testExecutionUI;

    private final TestsetComponent testSetComp;

    private final TestSetTree testSetTree;

    private final AppMainFrame sMainFrame;

    public TestExecution(AppMainFrame sMainFrame) {
        this.sMainFrame = sMainFrame;
        testSetComp = new TestsetComponent(this);
        testSetTree = new TestSetTree(this);
        testExecutionUI = new TestExecutionUI(this);
    }

    public TestExecutionUI getTestExecutionUI() {
        return testExecutionUI;
    }

    public TestsetComponent getTestSetComp() {
        return testSetComp;
    }

    public TestSetTree getTestSetTree() {
        return testSetTree;
    }

    public AppMainFrame getsMainFrame() {
        return sMainFrame;
    }

    public final void load() {
        testSetComp.load();
        testSetTree.load();
    }

    public Project getProject() {
        return sMainFrame.getProject();
    }

    public final void afterProjectChange() {
        testExecutionUI.loadTestPlanModel();
        testExecutionUI.adjustUI();
    }

    public void reloadBrowsers() {
        testSetComp.loadBrowsers();
    }

    public String getDefaultBrowser() {
        return sMainFrame.getTestDesign().getDefaultBrowser();
    }
}

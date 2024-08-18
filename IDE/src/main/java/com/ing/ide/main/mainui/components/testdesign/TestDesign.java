
package com.ing.ide.main.mainui.components.testdesign;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.ide.main.mainui.AppMainFrame;
import com.ing.ide.main.mainui.components.testdesign.or.ObjectRepo;
import com.ing.ide.main.mainui.components.testdesign.scenario.ScenarioComponent;
import com.ing.ide.main.mainui.components.testdesign.testcase.TestCaseComponent;
import com.ing.ide.main.mainui.components.testdesign.testdata.TestDataComponent;
import com.ing.ide.main.mainui.components.testdesign.tree.ProjectTree;
import com.ing.ide.main.mainui.components.testdesign.tree.ReusableTree;
import com.ing.ide.main.ui.ImpactUI;
import java.awt.CardLayout;
import javax.swing.JPanel;

/**
 *
 * 
 */
public class TestDesign {

    private final TestDesignUI testDesignUI;

    private final ScenarioComponent scenarioComp;

    private final TestCaseComponent testcaseComp;

    private final TestDataComponent testDataComp;

    private final JPanel testcaseMirage;

    private final ProjectTree projectTree;

    private final ReusableTree reusableTree;

    private final ObjectRepo objectRepo;

    private final AppMainFrame sMainFrame;

    private CardLayout testCaseScenarioCard;

    private final ImpactUI impactUI;

    public TestDesign(AppMainFrame sMainFrame) {
        this.sMainFrame = sMainFrame;
        projectTree = new ProjectTree(this);
        reusableTree = new ReusableTree(this);
        scenarioComp = new ScenarioComponent(this);
        testcaseComp = new TestCaseComponent(this);
        testDataComp = new TestDataComponent(this);
        objectRepo = new ObjectRepo(this);
        testcaseMirage = new JPanel();
        testDesignUI = new TestDesignUI(this);
        impactUI = new ImpactUI(this);
        init();
    }

    private void init() {
        testCaseScenarioCard = new CardLayout();
        testcaseMirage.setLayout(testCaseScenarioCard);
        testcaseMirage.add(scenarioComp, "scenario");
        testcaseMirage.add(testcaseComp, "testcase");
    }

    public void loadTableModelForSelection(Object selectedNode) {
        if (selectedNode instanceof Scenario) {
            testCaseScenarioCard.show(testcaseMirage, "scenario");
            scenarioComp.loadTableModelForSelection(selectedNode);
        } else if (selectedNode instanceof TestCase) {
            testCaseScenarioCard.show(testcaseMirage, "testcase");
            testcaseComp.loadTableModelForSelection(selectedNode);
        }
    }

    public void loadReusableTableModelForSelection(Object selectedNode) {
        if (selectedNode instanceof TestCase) {
            testCaseScenarioCard.show(testcaseMirage, "testcase");
            testcaseComp.loadTableModelForSelection(selectedNode);
        }
    }

    public TestDesignUI getTestDesignUI() {
        return testDesignUI;
    }

    public TestCaseComponent getTestCaseComp() {
        return testcaseComp;
    }

    public TestDataComponent getTestDatacomp() {
        return testDataComp;
    }

    public ScenarioComponent getScenarioComp() {
        return scenarioComp;
    }

    public JPanel getTestCaseComponent() {
        return testcaseMirage;
    }

    public JPanel getTestCasePanelForExploratory() {
        testcaseComp.getToolBar().setVisible(false);
        return testcaseComp;
    }

    public void resetTestCasePanelAfterExploratory() {
        testcaseComp.getToolBar().setVisible(true);
        testcaseMirage.add(testcaseComp, "testcase");
        testCaseScenarioCard.show(testcaseMirage, "testcase");
    }

    public ProjectTree getProjectTree() {
        return projectTree;
    }

    public ReusableTree getReusableTree() {
        return reusableTree;
    }

    public ObjectRepo getObjectRepo() {
        return objectRepo;
    }

    public ImpactUI getImpactUI() {
        return impactUI;
    }

    public AppMainFrame getsMainFrame() {
        return sMainFrame;
    }

    public final void load() {
        scenarioComp.load();
        testcaseComp.load();
        testDataComp.load();
        reusableTree.load();
        projectTree.load();
        objectRepo.load();
    }

    public final void afterProjectChange() {
        getTestDesignUI().adjustUI();
    }

    public final void save() {
        reusableTree.save();
    }

    public Project getProject() {
        return sMainFrame.getProject();
    }

    public void reloadBrowsers() {
        testcaseComp.loadBrowsers();
    }

    public String getDefaultBrowser() {
        return testcaseComp.getDefaultBrowser();
    }

}

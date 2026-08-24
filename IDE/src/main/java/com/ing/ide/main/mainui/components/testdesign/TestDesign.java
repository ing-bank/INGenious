package com.ing.ide.main.mainui.components.testdesign;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.ide.main.mainui.AppMainFrame;
import com.ing.ide.main.mainui.components.testdesign.or.ObjectRepo;
import com.ing.ide.main.mainui.components.testdesign.scenario.ScenarioComponent;
import com.ing.ide.main.mainui.components.testdesign.testcase.TestCaseComponent;
import com.ing.ide.main.mainui.components.testdesign.testcase.validation.TestCaseValidation;
import com.ing.ide.main.mainui.components.testdesign.testdata.TestDataComponent;
import com.ing.ide.main.mainui.components.testdesign.tree.ProjectTree;
import com.ing.ide.main.mainui.components.testdesign.tree.ReusableTree;
import com.ing.ide.main.mainui.components.testdesign.tree.SharedReusableTree;
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

    private final TestDataComponent sharedTestDataComp;

    private final JPanel testcaseMirage;

    private final ProjectTree projectTree;

    private final ReusableTree reusableTree;

    private final SharedReusableTree sharedReusableTree;

    private final ObjectRepo objectRepo;

    private final AppMainFrame sMainFrame;

    private CardLayout testCaseScenarioCard;

    private final ImpactUI impactUI;

    public TestDesign(AppMainFrame sMainFrame) {
        this.sMainFrame = sMainFrame;
        projectTree = new ProjectTree(this);
        reusableTree = new ReusableTree(this);
        sharedReusableTree = new SharedReusableTree(this);
        scenarioComp = new ScenarioComponent(this);
        testcaseComp = new TestCaseComponent(this, this.sMainFrame);
        testDataComp = new TestDataComponent(this);
        sharedTestDataComp = new TestDataComponent(this, true);
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
            // Save current test case before switching to scenario view
            TestCase currentTestCase = testcaseComp.getCurrentTestCase();
            if (currentTestCase != null && !currentTestCase.isSaved()) {
                currentTestCase.save();
            }

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

    public TestDataComponent getSharedTestDataComp() {
        return sharedTestDataComp;
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

    public SharedReusableTree getSharedReusableTree() {
        return sharedReusableTree;
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
        sharedTestDataComp.load();
        reusableTree.load();
        sharedReusableTree.load();
        projectTree.load();
        objectRepo.load();
        validateProjectAsync();
    }

    /**
     * Kicks off a one-time background validation pass so that scenarios and
     * test cases with IDE-level validation errors are marked in red as soon as
     * the project is opened, without the user having to open each test case.
     */
    private void validateProjectAsync() {
        TestCaseValidation.clearCache();
        TestCaseValidation.validateAllAsync(
            getProject(),
            () -> {
                projectTree.getTree().repaint();
                reusableTree.getTree().repaint();
            }
        );
    }

    public final void afterProjectChange() {
        getTestDesignUI().adjustUI();
    }

    public final void save() {
        reusableTree.save();
        sharedReusableTree.save();
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

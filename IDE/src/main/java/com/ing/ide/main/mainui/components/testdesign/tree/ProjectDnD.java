
package com.ing.ide.main.mainui.components.testdesign.tree;

import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.ide.main.mainui.components.testdesign.tree.model.GroupNode;
import com.ing.ide.main.mainui.components.testdesign.tree.model.ProjectTreeModel;
import com.ing.ide.main.mainui.components.testdesign.tree.model.ScenarioNode;
import com.ing.ide.main.mainui.components.testdesign.tree.model.TestCaseNode;
import com.ing.ide.main.mainui.components.testdesign.tree.model.TestPlanNode;
import com.ing.ide.main.utils.dnd.TransferableNode;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import static javax.swing.TransferHandler.COPY_OR_MOVE;
import static javax.swing.TransferHandler.MOVE;
import javax.swing.tree.TreePath;

/**
 *
 * 
 */
public class ProjectDnD extends TransferHandler {

    public static final DataFlavor TESTCASE_FLAVOR = new DataFlavor(TestCaseDnD.class,
            TestCaseDnD.class.getSimpleName());

    private final ProjectTree pTree;

    private ProjectTreeModel sourceTreeModel;

    private Boolean isCut = false;

    public ProjectDnD(ProjectTree pTree) {
        this.pTree = pTree;
    }

    @Override
    public int getSourceActions(JComponent c) {
        return COPY_OR_MOVE;
    }

    @Override
    protected Transferable createTransferable(JComponent source) {
        List<ScenarioNode> scenarios = pTree.getSelectedScenarioNodes();
        if (!scenarios.isEmpty()) {
            return new TransferableNode(new TestCaseDnD(pTree.getTreeModel()).
                    withScenarioList(scenarios), TESTCASE_FLAVOR);
        }
        List<TestCaseNode> testcases = pTree.getSelectedTestCaseNodes();
        if (!testcases.isEmpty()) {
            return new TransferableNode(new TestCaseDnD(pTree.getTreeModel()).
                    withTestCaseList(testcases), TESTCASE_FLAVOR);
        }
        return null;
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport ts) {
        return getDestinationObject(ts) != null
                && ts.isDataFlavorSupported(TESTCASE_FLAVOR);
    }

    @Override
    public boolean importData(TransferHandler.TransferSupport ts) {
        if (ts.isDataFlavorSupported(TESTCASE_FLAVOR)) {
            try {
                TestCaseDnD testCaseDnD
                        = (TestCaseDnD) ts.getTransferable()
                        .getTransferData(TESTCASE_FLAVOR);
                sourceTreeModel = testCaseDnD.model;
                if (testCaseDnD.isTestCases()) {
                    return importTestCases(testCaseDnD.getTestCaseList(), ts);
                } else {
                    return importScenarios(testCaseDnD.getScenarioList(), ts);
                }

            } catch (UnsupportedFlavorException | IOException ex) {
                Logger.getLogger(ProjectDnD.class
                        .getName()).log(Level.SEVERE, null, ex);
                return false;
            }

        }
        return false;
    }

    private Boolean importTestCases(List<TestCaseNode> testCaseNodes,
            TransferHandler.TransferSupport ts) {
        Boolean shouldCut = ts.isDrop() ? ts.getDropAction() == MOVE : isCut;
        Object destObject = getDestinationObject(ts);
        ScenarioNode scNode = getScenarioNode(destObject);
        if (scNode != null) {
            copySelectedTestCases(testCaseNodes, scNode, shouldCut);
            return true;
        }
        if (!(destObject instanceof TestPlanNode)
                && destObject instanceof GroupNode) {
            copySelectedTestCases(testCaseNodes, (GroupNode) destObject, shouldCut);
            return true;
        }
        return false;
    }

    private ScenarioNode getScenarioNode(Object obj) {
        if (obj instanceof ScenarioNode) {
            return (ScenarioNode) obj;
        }
        if (obj instanceof TestCaseNode) {
            return (ScenarioNode) ((TestCaseNode) obj).getParent();
        }
        return null;
    }

    private Boolean importScenarios(List<ScenarioNode> scenarioNodes,
            TransferHandler.TransferSupport ts) {
        Boolean shouldCut = ts.isDrop() ? ts.getDropAction() == MOVE : isCut;
        if (shouldCut) {
            return false;
        }
        Object destObject = getDestinationObject(ts);
        if (destObject instanceof GroupNode) {
            for (ScenarioNode scenarioNode : scenarioNodes) {
                addScenario(scenarioNode.getScenario(), (GroupNode) destObject);
            }
            return true;
        }
        return false;
    }

    private Object getDestinationObject(TransferHandler.TransferSupport ts) {
        TreePath path;
        if (ts.isDrop()) {
            path = ((JTree.DropLocation) ts.getDropLocation()).getPath();
        } else {
            path = ((JTree) ts.getComponent()).getSelectionPath();
        }
        if (path != null) {
            return path.getLastPathComponent();
        }
        return null;
    }

    @Override
    protected void exportDone(JComponent source, Transferable data, int action) {
        isCut = action == MOVE;
        super.exportDone(source, data, action);
    }

    private void copySelectedTestCases(List<TestCaseNode> testCaseNodes,
            ScenarioNode dropscenario, Boolean isCut) {
        for (TestCaseNode testCaseNode : testCaseNodes) {
            Scenario scenario = testCaseNode.getTestCase().getScenario();
            TestCase testCase = testCaseNode.getTestCase();
            testCase.loadTableModel();
            if (isCut) {
                if (testCase.equals(dropscenario.getScenario()
                        .getTestCaseByName(testCaseNode.toString()))) {
                    continue;
                }
            }
            TestCaseNode newTestCaseNode
                    = addTestCase(dropscenario.getScenario(), testCaseNode.toString());
            testCase.copyValuesTo(newTestCaseNode.getTestCase());
            newTestCaseNode.getTestCase().setReusable(testCase.getReusable());
            if (isCut) {
                scenario.removeTestCase(testCase);
                sourceTreeModel.removeNodeFromParent(testCaseNode);
                pTree.getProject().refactorTestCaseScenario(
                        testCaseNode.toString(),
                        scenario.getName(),
                        dropscenario.toString());
            }
        }
    }

    private void copySelectedTestCases(List<TestCaseNode> testCaseNodes,
            GroupNode dropGroup, Boolean isCut) {
        for (TestCaseNode testCaseNode : testCaseNodes) {
            Scenario scenario = testCaseNode.getTestCase().getScenario();
            TestCase testCase = testCaseNode.getTestCase();
            ScenarioNode scNode = dropGroup.addScenarioIfNotPresent(scenario);
            pTree.getTreeModel().addTestCase(scNode, testCase);
            if (isCut) {
                sourceTreeModel.removeNodeFromParent(testCaseNode);
            }
        }
        pTree.getTreeModel().reload(dropGroup);
    }

    private TestCaseNode addTestCase(Scenario scenario, String name) {
        String newName = name;
        int i = 1;
        while (scenario.getTestCaseByName(newName) != null) {
            newName = name + " Copy(" + i++ + ")";
        }
        return pTree.getTreeModel().addTestCase(scenario.addTestCase(newName));
    }

    private void addScenario(Scenario scenario, GroupNode gNode) {
        String newName = scenario.getName();
        int i = 1;
        while (scenario.getProject().getScenarioByName(newName) != null) {
            newName = scenario.getName() + " Copy(" + i++ + ")";
        }
        ScenarioNode sNode = pTree.getTreeModel().addScenario(gNode,
                scenario.getProject().addScenario(newName));
        List<TestCase> testcases;
        if (pTree.getTreeModel().getRoot() instanceof TestPlanNode) {
            testcases = scenario.getTestcasesAlone();
        } else {
            testcases = scenario.getReusables();
        }
        for (TestCase testcase : testcases) {
            testcase.loadTableModel();
            TestCase newTestCase = sNode.getScenario().
                    addTestCase(testcase.getName());
            testcase.copyValuesTo(newTestCase);
            sNode.addTestCase(newTestCase);
        }
    }

}

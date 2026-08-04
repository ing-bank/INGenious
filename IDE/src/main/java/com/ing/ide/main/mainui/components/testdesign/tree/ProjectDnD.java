package com.ing.ide.main.mainui.components.testdesign.tree;

import static javax.swing.TransferHandler.COPY_OR_MOVE;
import static javax.swing.TransferHandler.MOVE;

import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.component.utils.NamingUtils;
import com.ing.ide.main.mainui.components.testdesign.tree.model.GroupNode;
import com.ing.ide.main.mainui.components.testdesign.tree.model.ProjectTreeModel;
import com.ing.ide.main.mainui.components.testdesign.tree.model.ReusableNode;
import com.ing.ide.main.mainui.components.testdesign.tree.model.ScenarioNode;
import com.ing.ide.main.mainui.components.testdesign.tree.model.SharedReusableNode;
import com.ing.ide.main.mainui.components.testdesign.tree.model.TestCaseNode;
import com.ing.ide.main.mainui.components.testdesign.tree.model.TestPlanGroupNode;
import com.ing.ide.main.mainui.components.testdesign.tree.model.TestPlanNode;
import com.ing.ide.main.mainui.components.testdesign.tree.model.TestPlanTreeModel;
import com.ing.ide.main.utils.dnd.TransferableNode;
import com.ing.ide.util.Notification;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.TreePath;

/**
 *
 *
 */
public class ProjectDnD extends TransferHandler {
    public static final DataFlavor TESTCASE_FLAVOR = new DataFlavor(
        TestCaseDnD.class,
        TestCaseDnD.class.getSimpleName()
    );

    private final ProjectTree pTree;

    /**
     * Clipboard cut intent must be shared across tree handler instances
     * (source and destination trees have different ProjectDnD objects).
     */
    private static volatile boolean clipboardCutInProgress = false;

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
            return new TransferableNode(
                new TestCaseDnD(pTree.getTreeModel()).withScenarioList(scenarios),
                TESTCASE_FLAVOR
            );
        }
        List<TestCaseNode> testcases = pTree.getSelectedTestCaseNodes();
        if (!testcases.isEmpty()) {
            return new TransferableNode(
                new TestCaseDnD(pTree.getTreeModel()).withTestCaseList(testcases),
                TESTCASE_FLAVOR
            );
        }
        return null;
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport ts) {
        return getDestinationObject(ts) != null && ts.isDataFlavorSupported(TESTCASE_FLAVOR);
    }

    @Override
    public boolean importData(TransferHandler.TransferSupport ts) {
        if (ts.isDataFlavorSupported(TESTCASE_FLAVOR)) {
            try {
                TestCaseDnD testCaseDnD = (TestCaseDnD) ts
                    .getTransferable()
                    .getTransferData(TESTCASE_FLAVOR);
                sourceTreeModel = testCaseDnD.model;
                if (testCaseDnD.isTestCases()) {
                    return importTestCases(testCaseDnD.getTestCaseList(), ts);
                } else {
                    return importScenarios(testCaseDnD.getScenarioList(), ts);
                }
            } catch (UnsupportedFlavorException | IOException ex) {
                Logger.getLogger(ProjectDnD.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
        }
        return false;
    }

    private Boolean importTestCases(
        List<TestCaseNode> testCaseNodes,
        TransferHandler.TransferSupport ts
    ) {
        Boolean shouldCut = ts.isDrop() ? ts.getDropAction() == MOVE : clipboardCutInProgress;
        if (shouldCut && isMoveFromTestPlanOrProjectToShared()) {
            Notification.showWarning(
                "Cut/Move from Test Plan or Project Reusable to Shared Reusable is not allowed. Use 'Make As Shared Reusable' instead."
            );
            return false;
        }
        if (shouldCut) {
            String warning = getCrossScopeMoveWarning();
            if (warning != null) {
                Notification.showWarning(warning);
                return false;
            }
        }
        if (shouldCut && isRestrictedMoveToSharedTree()) {
            Notification.showWarning(
                "Move-paste into Shared Reusables is not supported from Test Plan or Project Reusables. Use 'Make As Shared Reusable' instead."
            );
            return false;
        }
        Object destObject = getDestinationObject(ts);
        ScenarioNode scNode = getScenarioNode(destObject);
        if (scNode != null) {
            copySelectedTestCases(testCaseNodes, scNode, shouldCut);
            pTree.persistSortOrder(scNode);
            return true;
        }
        if (!(destObject instanceof TestPlanNode) && destObject instanceof GroupNode) {
            copySelectedTestCases(testCaseNodes, (GroupNode) destObject, shouldCut);
            pTree.persistSortOrder((GroupNode) destObject);
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

    private Boolean importScenarios(
        List<ScenarioNode> scenarioNodes,
        TransferHandler.TransferSupport ts
    ) {
        Object dropTarget = getDestinationObject(ts);
        if (
            dropTarget instanceof TestPlanGroupNode &&
            pTree.getTreeModel() instanceof TestPlanTreeModel
        ) {
            TestPlanTreeModel model = (TestPlanTreeModel) pTree.getTreeModel();
            TestPlanGroupNode target = (TestPlanGroupNode) dropTarget;
            boolean moved = false;
            for (ScenarioNode scenarioNode : scenarioNodes) {
                if (scenarioNode.getParent() instanceof TestPlanGroupNode) {
                    model.moveScenarioToGroup(scenarioNode, target);
                    moved = true;
                }
            }
            if (moved) {
                return true;
            }
        }
        Boolean shouldCut = ts.isDrop() ? ts.getDropAction() == MOVE : clipboardCutInProgress;
        if (shouldCut && isMoveFromTestPlanOrProjectToShared()) {
            Notification.showWarning(
                "Cut/Move from Test Plan or Project Reusable to Shared Reusable is not allowed. Use 'Make As Shared Reusable' instead."
            );
            return false;
        }
        if (shouldCut) {
            String warning = getCrossScopeMoveWarning();
            if (warning != null) {
                Notification.showWarning(warning);
            }
            return false;
        }
        Object destObject = getDestinationObject(ts);
        if (destObject instanceof GroupNode) {
            for (ScenarioNode scenarioNode : scenarioNodes) {
                addScenario(scenarioNode.getScenario(), (GroupNode) destObject);
            }
            pTree.persistSortOrder((GroupNode) destObject);
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
        clipboardCutInProgress = action == MOVE;
        super.exportDone(source, data, action);
    }

    @Override
    public void exportToClipboard(JComponent comp, Clipboard clip, int action)
        throws IllegalStateException {
        // Keyboard/menu Cut uses clipboard export path; preserve MOVE intent for paste guards.
        isCut = action == MOVE;
        clipboardCutInProgress = action == MOVE;
        super.exportToClipboard(comp, clip, action);
    }

    private void copySelectedTestCases(
        List<TestCaseNode> testCaseNodes,
        ScenarioNode dropscenario,
        Boolean isCut
    ) {
        int skipped = 0;
        for (TestCaseNode testCaseNode : testCaseNodes) {
            Scenario scenario = testCaseNode.getTestCase().getScenario();
            TestCase testCase = testCaseNode.getTestCase();
            testCase.loadTableModel();
            if (isCut) {
                if (
                    testCase.equals(
                        dropscenario.getScenario().getTestCaseByName(testCaseNode.toString())
                    )
                ) {
                    skipped++;
                    continue;
                }
            }
            TestCaseNode newTestCaseNode;
            if (isCut) {
                // Name collisions in the destination are resolved with a "_n" suffix (same as
                // copy) rather than rejected - remove the source first so a move onto the same
                // scenario it already lives in doesn't collide with itself.
                scenario.removeTestCase(testCase);
                newTestCaseNode = addTestCase(dropscenario.getScenario(), testCaseNode.toString());
                if (newTestCaseNode == null || newTestCaseNode.getTestCase() == null) {
                    scenario.getTestCases().add(testCase);
                    Logger
                        .getLogger(ProjectDnD.class.getName())
                        .log(
                            Level.WARNING,
                            "Skipping move for test case ''{0}'' into scenario ''{1}'' due to naming collision or creation failure",
                            new Object[] { testCaseNode.toString(), dropscenario.toString() }
                        );
                    skipped++;
                    continue;
                }
            } else {
                newTestCaseNode = addTestCase(dropscenario.getScenario(), testCaseNode.toString());
                if (newTestCaseNode == null || newTestCaseNode.getTestCase() == null) {
                    Logger
                        .getLogger(ProjectDnD.class.getName())
                        .log(
                            Level.WARNING,
                            "Skipping copy for test case ''{0}'' into scenario ''{1}'' due to naming collision or creation failure",
                            new Object[] { testCaseNode.toString(), dropscenario.toString() }
                        );
                    skipped++;
                    continue;
                }
            }

            testCase.copyValuesTo(newTestCaseNode.getTestCase());
            newTestCaseNode.getTestCase().setReusable(testCase.getReusable());
            if (isCut) {
                sourceTreeModel.removeNodeFromParent(testCaseNode);
                String oldTestCaseName = testCaseNode.toString();
                String newTestCaseName = newTestCaseNode.getTestCase().getName();
                if (!oldTestCaseName.equals(newTestCaseName)) {
                    // The suffix-on-conflict rename means any Execute step reference pointing at
                    // the old name must be repointed at the new one before the scenario move.
                    pTree
                        .getProject()
                        .refactorTestCase(scenario.getName(), oldTestCaseName, newTestCaseName);
                }
                pTree
                    .getProject()
                    .refactorTestCaseScenario(
                        newTestCaseName,
                        scenario.getName(),
                        dropscenario.toString()
                    );
            }
        }
        showSkippedPasteNotification(skipped);
    }

    private void copySelectedTestCases(
        List<TestCaseNode> testCaseNodes,
        GroupNode dropGroup,
        Boolean isCut
    ) {
        int skipped = 0;
        // Test cases pasted together from the same source scenario in this batch must land in
        // the same newly created destination scenario rather than each minting their own copy.
        Map<Scenario, Scenario> destinationScenarios = new HashMap<>();
        for (TestCaseNode testCaseNode : testCaseNodes) {
            Scenario scenario = testCaseNode.getTestCase().getScenario();
            TestCase testCase = testCaseNode.getTestCase();
            Scenario destinationScenario = destinationScenarios.computeIfAbsent(
                scenario,
                this::createDestinationScenarioForRootPaste
            );
            if (destinationScenario == null) {
                skipped++;
                continue;
            }
            ScenarioNode scNode = dropGroup.addScenarioIfNotPresent(destinationScenario);

            TestCaseNode inserted = addTestCase(destinationScenario, testCaseNode.toString());

            if (inserted == null || inserted.getTestCase() == null) {
                skipped++;
                continue;
            }

            testCase.loadTableModel();
            testCase.copyValuesTo(inserted.getTestCase());
            inserted.getTestCase().setReusable(testCase.getReusable());
            if (isCut) {
                sourceTreeModel.removeNodeFromParent(testCaseNode);
                if (scenario != destinationScenario) {
                    scenario.removeTestCase(testCase);
                }
            }
        }
        pTree.getTreeModel().reload(dropGroup);
        showSkippedPasteNotification(skipped);
    }

    /**
     * Root-level (Group/tree-root drop target) paste of standalone test cases never merges into
     * a pre-existing same-named scenario in the destination scope - it always creates a new,
     * independently-suffixed scenario, same as copying a whole Scenario does. (The Live Recorder
     * has its own separate, intentionally different, merge-by-name scenario resolution and is
     * unaffected by this.)
     */
    private Scenario createDestinationScenarioForRootPaste(Scenario sourceScenario) {
        String uniqueScenarioName = buildCopiedScenarioName(sourceScenario);
        return createScenarioInDestinationScope(sourceScenario, uniqueScenarioName);
    }

    private TestCaseNode addTestCase(Scenario scenario, String name) {
        String newName = NamingUtils.generateUniqueName(
            name,
            candidate -> scenario.getTestCaseByName(candidate) != null
        );
        TestCase created = scenario.addTestCase(newName);
        if (created == null) {
            return null;
        }
        return pTree.getTreeModel().addTestCase(created);
    }

    private void addScenario(Scenario scenario, GroupNode gNode) {
        String copiedScenarioName = buildCopiedScenarioName(scenario);
        Scenario createdScenario = createScenarioInDestinationScope(scenario, copiedScenarioName);
        if (createdScenario == null) {
            Notification.showWarning(
                "Skipped scenario '" +
                scenario.getName() +
                "' due to naming conflict or invalid destination."
            );
            return;
        }

        ScenarioNode sNode = pTree.getTreeModel().addScenario(gNode, createdScenario);
        if (sNode == null || sNode.getScenario() == null) {
            Notification.showWarning(
                "Skipped scenario '" + scenario.getName() + "' due to tree insertion failure."
            );
            return;
        }

        copyTestCases(sNode, scenario);
    }

    private void copyTestCases(ScenarioNode sNode, Scenario scenario) {
        List<TestCase> testcases;
        int skipped = 0;
        // Scenario copy should include all testcases under the source scenario.
        testcases = scenario.getTestCases();
        for (TestCase testcase : testcases) {
            testcase.loadTableModel();
            String baseName = testcase.getName();
            String newName = NamingUtils.generateUniqueName(
                baseName,
                candidate -> sNode.getScenario().getTestCaseByName(candidate) != null
            );
            TestCase newTestCase = sNode.getScenario().addTestCase(newName);
            if (newTestCase == null) {
                skipped++;
                continue;
            }
            testcase.copyValuesTo(newTestCase);
            sNode.addTestCase(newTestCase);
        }
        showSkippedPasteNotification(skipped);
    }

    private String buildCopiedScenarioName(Scenario sourceScenario) {
        String baseName = sourceScenario.getName();
        return NamingUtils.generateUniqueName(
            baseName,
            candidate -> scenarioExistsInDestination(sourceScenario, candidate)
        );
    }

    private boolean scenarioExistsInDestination(Scenario sourceScenario, String scenarioName) {
        if (pTree.getTreeModel().getRoot() instanceof TestPlanNode) {
            return sourceScenario.getProject().getTestPlanScenarioByName(scenarioName) != null;
        }
        if (pTree.getTreeModel().getRoot() instanceof ReusableNode) {
            return sourceScenario.getProject().getReusableScenarioByName(scenarioName) != null;
        }
        if (pTree.getTreeModel().getRoot() instanceof SharedReusableNode) {
            return (
                sourceScenario.getProject().getSharedReusableScenarioByName(scenarioName) != null
            );
        }
        return false;
    }

    private Scenario createScenarioInDestinationScope(
        Scenario sourceScenario,
        String scenarioName
    ) {
        if (pTree.getTreeModel().getRoot() instanceof TestPlanNode) {
            Scenario created = new Scenario(
                sourceScenario.getProject(),
                scenarioName,
                Scenario.Source.TEST_PLAN
            );
            sourceScenario.getProject().getScenarios().add(created);
            return created;
        }
        if (pTree.getTreeModel().getRoot() instanceof ReusableNode) {
            Scenario created = new Scenario(
                sourceScenario.getProject(),
                scenarioName,
                Scenario.Source.REUSABLE_COMPONENTS
            );
            sourceScenario.getProject().getReusableScenarios().add(created);
            return created;
        }
        if (pTree.getTreeModel().getRoot() instanceof SharedReusableNode) {
            Scenario created = new Scenario(
                sourceScenario.getProject(),
                scenarioName,
                Scenario.Source.SHARED_REUSABLE_COMPONENTS
            );
            sourceScenario.getProject().getSharedScenarios().add(created);
            return created;
        }
        return null;
    }

    private boolean isRestrictedMoveToSharedTree() {
        if (!(pTree instanceof SharedReusableTree)) {
            return false;
        }
        if (sourceTreeModel == null || sourceTreeModel.getRoot() == null) {
            return false;
        }
        Object sourceRoot = sourceTreeModel.getRoot();
        return sourceRoot instanceof TestPlanNode || sourceRoot instanceof ReusableNode;
    }

    private boolean isMoveFromTestPlanOrProjectToShared() {
        if (
            sourceTreeModel == null ||
            sourceTreeModel.getRoot() == null ||
            pTree == null ||
            pTree.getTreeModel() == null ||
            pTree.getTreeModel().getRoot() == null
        ) {
            return false;
        }

        String sourceScope = getScopeName(sourceTreeModel.getRoot());
        String destinationScope = getScopeName(pTree.getTreeModel().getRoot());
        return (
            "shared".equals(destinationScope) &&
            ("testplan".equals(sourceScope) || "project".equals(sourceScope))
        );
    }

    private String getCrossScopeMoveWarning() {
        if (
            sourceTreeModel == null ||
            sourceTreeModel.getRoot() == null ||
            pTree == null ||
            pTree.getTreeModel() == null ||
            pTree.getTreeModel().getRoot() == null
        ) {
            return null;
        }

        String sourceScope = getScopeName(sourceTreeModel.getRoot());
        String destinationScope = getScopeName(pTree.getTreeModel().getRoot());
        if (
            sourceScope == null || destinationScope == null || sourceScope.equals(destinationScope)
        ) {
            return null;
        }

        if ("testplan".equals(sourceScope) && "project".equals(destinationScope)) {
            return "Cut/Move from Test Plan to Project Reusable is not allowed. Use 'Make As Project Reusable' instead.";
        }
        if ("testplan".equals(sourceScope) && "shared".equals(destinationScope)) {
            return "Cut/Move from Test Plan to Shared Reusable is not allowed. Use 'Make As Shared Reusable' instead.";
        }
        if ("project".equals(sourceScope) && "shared".equals(destinationScope)) {
            return "Cut/Move from Project Reusable to Shared Reusable is not allowed. Use 'Make As Shared Reusable' instead.";
        }
        if ("project".equals(sourceScope) && "testplan".equals(destinationScope)) {
            return "Cut/Move from Project Reusable to Test Plan is not allowed. Use 'Make As TestCase' instead.";
        }
        if ("shared".equals(sourceScope) && "project".equals(destinationScope)) {
            return "Cut/Move from Shared Reusable to Project Reusable action is not allowed.";
        }
        if ("shared".equals(sourceScope) && "testplan".equals(destinationScope)) {
            return "Cut/Move from Shared Reusable to Test Plan action is not allowed.";
        }

        return "Cross-scope cut/move is not allowed. Use the appropriate 'Make As ...' action instead.";
    }

    private String getScopeName(Object rootNode) {
        if (rootNode instanceof TestPlanNode) {
            return "testplan";
        }
        if (rootNode instanceof ReusableNode) {
            return "project";
        }
        if (rootNode instanceof SharedReusableNode) {
            return "shared";
        }
        return null;
    }

    private void showSkippedPasteNotification(int skippedCount) {
        if (skippedCount > 0) {
            Notification.showWarning(
                "Skipped " +
                skippedCount +
                " pasted item(s) due to name conflicts or invalid destination."
            );
        }
    }
}

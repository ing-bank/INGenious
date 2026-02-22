
package com.ing.ide.main.mainui.components.testexecution;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.model.Tag;
import com.ing.ide.main.Main;
import com.ing.ide.main.fx.FXPanelHeader;
import com.ing.ide.main.mainui.components.testdesign.tree.TagEditorDialog;
import com.ing.ide.main.mainui.components.testdesign.tree.model.ScenarioNode;
import com.ing.ide.main.mainui.components.testdesign.tree.model.TestCaseNode;
import com.ing.ide.main.mainui.components.testexecution.tree.model.FilterableTestPlanTreeModel;
import com.ing.ide.main.utils.ConsolePanel;
import com.ing.ide.main.utils.Utils;
import com.ing.ide.main.utils.tree.JCheckBoxTree;
import com.ing.ide.main.utils.tree.TreeSearch;
import com.ing.ide.settings.IconSettings;
import com.ing.ide.util.Notification;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.stream.Collectors.toList;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

/**
 *
 * 
 */
public class TestExecutionUI extends JPanel implements ActionListener {

    TestExecution testExecution;

    private TestPlanPullPanel testPullPanel;

    JSplitPane testSetCompNtestPlan;
    JSplitPane treeSNTableSplitPane;

    JSplitPane testSettreeNSettingsSplitPane;

    JSplitPane testplanTreeNSettingsSplitPane;

    JSplitPane executionAndConsoleSplitPane;

    ConsolePanel consolePanel;

    public TestExecutionUI(TestExecution testExecution) {
        this.testExecution = testExecution;
        init();
    }

    private void init() {

        setLayout(new BorderLayout());

        testSetCompNtestPlan = new JSplitPane();
        testSetCompNtestPlan.setOneTouchExpandable(true);

        treeSNTableSplitPane = new JSplitPane();
        treeSNTableSplitPane.setOneTouchExpandable(true);

        testSettreeNSettingsSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        testSettreeNSettingsSplitPane.setOneTouchExpandable(true);

        testplanTreeNSettingsSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        testplanTreeNSettingsSplitPane.setOneTouchExpandable(true);

        executionAndConsoleSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        executionAndConsoleSplitPane.setOneTouchExpandable(true);

        testSettreeNSettingsSplitPane.setTopComponent(
                getCompInPanel(
                        "TestLab",
                        TreeSearch.installFor(testExecution.getTestSetTree().getTree())));

        testSettreeNSettingsSplitPane.setBottomComponent(
                getCompInPanel(
                        "QuickSettings",
                        new JScrollPane(
                                testExecution.getTestSetComp()
                                .getQuickSettings().getUILeft(this))));

        testSettreeNSettingsSplitPane.setDividerLocation(0.5);

        treeSNTableSplitPane.setResizeWeight(0.25);
        treeSNTableSplitPane.setLeftComponent(testSettreeNSettingsSplitPane);

        executionAndConsoleSplitPane.setTopComponent(testExecution.getTestSetComp());
        executionAndConsoleSplitPane.setOneTouchExpandable(true);

        consolePanel = new ConsolePanel();

        treeSNTableSplitPane.setRightComponent(executionAndConsoleSplitPane);

        testSetCompNtestPlan.setLeftComponent(treeSNTableSplitPane);

        testPullPanel = new TestPlanPullPanel();
        testplanTreeNSettingsSplitPane.setTopComponent(testPullPanel);
        testplanTreeNSettingsSplitPane.getTopComponent().setFont(UIManager.getFont("TableMenu.font"));

        testplanTreeNSettingsSplitPane.setBottomComponent(
                getCompInPanel(
                        "QuickSettings",
                        new JScrollPane(
                                testExecution.getTestSetComp()
                                .getQuickSettings().getUIRight(this))));
        testSetCompNtestPlan.setRightComponent(testplanTreeNSettingsSplitPane);
        testSetCompNtestPlan.setResizeWeight(0.8);

        add(testSetCompNtestPlan, BorderLayout.CENTER);
        
        // Apply initial pane backgrounds
        applyPaneBackgrounds();
    }
    
    /**
     * Applies themed backgrounds to the test execution panes.
     * Called at init and when theme changes via adjustUI().
     * Only applies custom backgrounds in dark mode - light mode uses default FlatLaf colors.
     */
    public void applyPaneBackgrounds() {
        // Only apply custom backgrounds in dark mode
        // Light mode should use default FlatLaf styling
        if (!Main.isDarkMode()) {
            return;
        }
        
        Color sidebarColor = UIManager.getColor("ing.sidebarPane");
        Color editorColor = UIManager.getColor("ing.editorPane");
        Color dividerColor = UIManager.getColor("ing.dividerColor");
        
        if (sidebarColor == null) {
            sidebarColor = UIManager.getColor("Panel.background");
        }
        if (editorColor == null) {
            editorColor = UIManager.getColor("Panel.background");
        }
        if (dividerColor == null) {
            dividerColor = UIManager.getColor("SplitPane.dividerColor");
        }
        
        // Apply colors to all split panes
        applyBackgroundRecursively(testSetCompNtestPlan, sidebarColor, dividerColor);
        applyBackgroundRecursively(treeSNTableSplitPane, sidebarColor, dividerColor);
        applyBackgroundRecursively(testSettreeNSettingsSplitPane, sidebarColor, dividerColor);
        applyBackgroundRecursively(testplanTreeNSettingsSplitPane, sidebarColor, dividerColor);
        applyBackgroundRecursively(executionAndConsoleSplitPane, sidebarColor, dividerColor);
    }
    
    /**
     * Recursively applies background color to a component and all its children.
     * Handles special cases for JScrollPane, JSplitPane, JTable, JTree, JList.
     */
    private void applyBackgroundRecursively(java.awt.Component comp, Color bgColor, Color dividerColor) {
        if (comp == null) return;
        
        // Skip FXPanelHeader (has its own styling)
        if (comp instanceof FXPanelHeader) {
            return;
        }
        
        // Handle JSplitPane specially - set divider color
        if (comp instanceof JSplitPane) {
            JSplitPane split = (JSplitPane) comp;
            split.setBackground(dividerColor);
            split.setOpaque(true);
            // Recurse into children
            applyBackgroundRecursively(split.getLeftComponent(), bgColor, dividerColor);
            applyBackgroundRecursively(split.getRightComponent(), bgColor, dividerColor);
            applyBackgroundRecursively(split.getTopComponent(), bgColor, dividerColor);
            applyBackgroundRecursively(split.getBottomComponent(), bgColor, dividerColor);
            return;
        }
        
        // Handle JScrollPane - set background on pane and viewport
        if (comp instanceof JScrollPane) {
            JScrollPane scroll = (JScrollPane) comp;
            scroll.setBackground(bgColor);
            scroll.setOpaque(true);
            scroll.getViewport().setBackground(bgColor);
            scroll.getViewport().setOpaque(true);
            // Also set background on the view component
            java.awt.Component view = scroll.getViewport().getView();
            if (view != null) {
                applyBackgroundRecursively(view, bgColor, dividerColor);
            }
            return;
        }
        
        // Handle JTable
        if (comp instanceof javax.swing.JTable) {
            javax.swing.JTable table = (javax.swing.JTable) comp;
            table.setBackground(bgColor);
            if (table.getTableHeader() != null) {
                table.getTableHeader().setBackground(UIManager.getColor("TableHeader.background"));
            }
            return;
        }
        
        // Handle JTree
        if (comp instanceof javax.swing.JTree) {
            javax.swing.JTree tree = (javax.swing.JTree) comp;
            tree.setBackground(bgColor);
            // Force tree to pick up new L&F colors including selection colors
            SwingUtilities.updateComponentTreeUI(tree);
            return;
        }
        
        // Handle JList
        if (comp instanceof javax.swing.JList) {
            comp.setBackground(bgColor);
            return;
        }
        
        // Handle JToolBar - keep its styled background
        if (comp instanceof JToolBar) {
            return;
        }
        
        // Handle general JPanel and Container
        if (comp instanceof java.awt.Container) {
            if (comp instanceof JPanel) {
                ((JPanel) comp).setOpaque(true);
            }
            comp.setBackground(bgColor);
            // Recurse into children
            java.awt.Container container = (java.awt.Container) comp;
            for (java.awt.Component child : container.getComponents()) {
                applyBackgroundRecursively(child, bgColor, dividerColor);
            }
        }
    }

    public void loadTestPlanModel() {
        testPullPanel.loadTestPlanModel();
    }

    private JPanel getCompInPanel(String labelText, JComponent comp) {
        
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        FXPanelHeader header = new FXPanelHeader(labelText);
        panel.add(header, BorderLayout.NORTH);
        comp.setFont(UIManager.getFont("Table.font"));
        
        panel.add(comp, BorderLayout.CENTER);
        return panel;
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        switch (ae.getActionCommand()) {
            case "Pull":
                if (testExecution.getTestSetComp().getCurrentTestSet() != null) {
                    testExecution.getTestSetComp().pullTestCases(testPullPanel.getSelectedTestCases());
                } else {
                    Notification.show("Please select/load a TestSet from the TestLab tree");
                }
                break;
            case "Export":
                if (testPullPanel.isChecked()) {
            try {
                testExecution.getsMainFrame().getStepMap().convertTestCase(Utils.saveDialog("Manual TestCase.csv"), testPullPanel.getSelectedTestCases());
            } catch (IOException ex) {
                Logger.getLogger(TestExecutionUI.class.getName()).log(Level.SEVERE, null, ex);
            }
                }
                break;
            case "Filter":
                testPullPanel.showFilterTag();
                break;
            default:
                throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    public void adjustUI() {
        treeSNTableSplitPane.setDividerLocation(0.25);
        testSetCompNtestPlan.setDividerLocation(0.8);
        treeSNTableSplitPane.setDividerLocation(0.25);
        testSettreeNSettingsSplitPane.setDividerLocation(0.5);
        testplanTreeNSettingsSplitPane.setDividerLocation(0.5);
        
        // Reapply pane backgrounds for theme changes
        applyPaneBackgrounds();
    }

    class TestPlanPullPanel extends JPanel {

        JCheckBoxTree testPlanTree;
        
        TreeModelListener modelListener;

        List<Tag> tags;
        List<String> sTags;

        private JButton filterButton;

        public TestPlanPullPanel() {
            init();
        }

        private void init() {
            setFont(UIManager.getFont("TableMenu.font"));
            setLayout(new BorderLayout());

            testPlanTree = new TestPlanCheckBoxTree();
            testPlanTree.setFont(UIManager.getFont("TableMenu.font"));
            JScrollPane scrollPane = new JScrollPane(testPlanTree);
            add(scrollPane, BorderLayout.CENTER);
            add(createToolbar(), BorderLayout.NORTH);
            modelListener = getModelListener();
        }

        private boolean containsAny(List<String> sTags, List<Tag> nTags) {
            return nTags.stream().map(Tag::getValue).filter(sTags::contains).findFirst().isPresent();
        }

        private boolean doFilter(Object o) {
            if (sTags != null && !sTags.isEmpty()) {
                if (o instanceof Scenario) {
                    return containsAny(sTags, getTags((Scenario) o));
                } else if (o instanceof TestCase) {
                    return containsAny(sTags, getTags((TestCase) o));
                }
            }
            return true;
        }

        private List<Tag> getTags(Scenario scn) {
            return testExecution.getProject().getInfo()
                    .findScenarioOrCreate(scn.getName())
                    .getTags();
        }

        private List<Tag> getTags(TestCase tc) {
            return testExecution.getProject().getInfo().getData()
                    .findOrCreate(tc.getName(), tc.getScenario().getName())
                    .getTags();

        }

        private void showFilterTag() {
            TagEditorDialog.build(testExecution.getsMainFrame(),
                    testExecution.getProject().getInfo().getAllTags(null), tags,
                    null, null).withTitle("Filter Tags").show(this::setFilterTags);
        }

        private void setFilterTags(List<Tag> tags) {
            this.tags = tags.stream().distinct().collect(toList());
            this.sTags = tags.stream().map(Tag::getValue).distinct().collect(toList());
            reloadModel();
            if (tags.isEmpty()) {
                resetFilter();
            } else {
                enableFilter();
            }
        }

        private JToolBar createToolbar() {
            JToolBar toolBar = new JToolBar();
            toolBar.setFloatable(false);
            toolBar.setOpaque(false);
            toolBar.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Separator.foreground")));
            toolBar.setLayout(new javax.swing.BoxLayout(toolBar, javax.swing.BoxLayout.X_AXIS));

            JButton pull = Utils.createButton("Pull", TestExecutionUI.this);
            pull.setToolTipText("Pull Selected TestCases to TestSet");
            pull.setIcon(Utils.getIconByResourceName("/ui/resources/testExecution/pull"));
            JButton export = Utils.createButton("Export", TestExecutionUI.this);
            export.setToolTipText("Export Selected TestCases into Manual TestCases");
            export.setIcon(Utils.getIconByResourceName("/ui/resources/testExecution/export"));
            filterButton = Utils.createButton("Filter", TestExecutionUI.this);
            filterButton.setToolTipText("Filter TestCases By Tags");
            filterButton.setIcon(Utils.getIconByResourceName("/ui/resources/toolbar/tag"));
            toolBar.add(pull);
            toolBar.add(export);
            toolBar.add(filterButton);
            return toolBar;
        }

        public void loadTestPlanModel() {
            testExecution.getsMainFrame().getTestDesign().getProjectTree()
                    .getTree().getModel().addTreeModelListener(modelListener);
            reloadModel();
            this.tags = null;
            this.sTags = null;
            resetFilter();
        }

        private void enableFilter() {
            filterButton.setIcon(Utils.getIconByResourceName("/ui/resources/toolbar/tagsel"));
        }

        private void resetFilter() {
            filterButton.setIcon(Utils.getIconByResourceName("/ui/resources/toolbar/tag"));
        }

        private TreeModelListener getModelListener() {
            return new TreeModelListener() {
                @Override
                public void treeNodesChanged(TreeModelEvent tme) {
                    reloadModel();
                }

                @Override
                public void treeNodesInserted(TreeModelEvent tme) {
                    reloadModel();
                }

                @Override
                public void treeNodesRemoved(TreeModelEvent tme) {
                    reloadModel();
                }

                @Override
                public void treeStructureChanged(TreeModelEvent tme) {
                    reloadModel();
                }
            };
        }

        private void reloadModel() {
            SwingUtilities.invokeLater(() -> {
                testPlanTree.setModel(null);
                testPlanTree.setModel(getModel(testExecution.getProject()));
                testPlanTree.setFont(UIManager.getFont("TableMenu.font"));
                testPlanTree.refresh();
            });
        }

        private TreeModel getModel(Project project) {
            return new FilterableTestPlanTreeModel(project, this::doFilter);
        }

        private List<TestCase> getSelectedTestCases() {
            List<TestCase> testcases = new ArrayList<>();
            TreePath[] paths = testPlanTree.getCheckedPaths();
            for (TreePath path : paths) {
                if (path.getLastPathComponent() instanceof TestCaseNode) {
                    testcases.add(((TestCaseNode) path.getLastPathComponent())
                            .getTestCase());
                }
            }
            return testcases;
        }

        private Boolean isChecked() {
            return testPlanTree.getCheckedPaths().length > 0;
        }
    }

    class TestPlanCheckBoxTree extends JCheckBoxTree {

        @Override
        public Icon getIcon(Object value) {
            if (value instanceof ScenarioNode) {
                return IconSettings.getIconSettings().getTestPlanScenario();
            } else if (value instanceof TestCaseNode) {
                return IconSettings.getIconSettings().getTestPlanTestCase();
            } else {
                return IconSettings.getIconSettings().getTestPlanRoot();
            }
        }
    }

    public ConsolePanel getConsolePanel() {
        return consolePanel;
    }

    public void toggleConsolePanel(Object source) {
        if (source instanceof JToggleButton) {
            final Boolean flag = ((JToggleButton) source).isSelected();
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (flag) {
                        executionAndConsoleSplitPane.setBottomComponent(consolePanel);
                        executionAndConsoleSplitPane.setDividerLocation(0.5);
                    } else {
                        executionAndConsoleSplitPane.remove(consolePanel);
                    }
                }
            });
        }
    }

}

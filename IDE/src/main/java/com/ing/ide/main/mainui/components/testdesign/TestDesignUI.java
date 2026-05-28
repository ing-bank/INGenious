
package com.ing.ide.main.mainui.components.testdesign;

import com.ing.ide.main.Main;
import com.ing.ide.main.fx.FXPanelHeader;
import com.ing.ide.main.utils.tree.TreeSearch;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 *
 * 
 */
public class TestDesignUI extends JPanel {

    TestDesign testDesign;

    JSplitPane projectNReusableTreeSplitPane;

    JSplitPane testCaseNTestDataSplitPane;

    JSplitPane oneTwo;

    JSplitPane oneThree;

    JPanel appReusablePanel;
    JPanel testPlanPanel;

    JButton reusableSwitch;

    public TestDesignUI(TestDesign testDesign) {
        this.testDesign = testDesign;
        init();
    }

    private void init() {
        setLayout(new BorderLayout());
        projectNReusableTreeSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        projectNReusableTreeSplitPane.setOneTouchExpandable(true);
        projectNReusableTreeSplitPane.setResizeWeight(0.5);

        testPlanPanel = getTreeInPanel("Test Plan", testDesign.getProjectTree().getTree());
        projectNReusableTreeSplitPane.setTopComponent(testPlanPanel);

        appReusablePanel = getRTreeInPanel("Reusable Component", testDesign.getReusableTree().getTree());
        projectNReusableTreeSplitPane.setBottomComponent(appReusablePanel);

        testCaseNTestDataSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        testCaseNTestDataSplitPane.setOneTouchExpandable(true);
        testCaseNTestDataSplitPane.setResizeWeight(0.5);

        testCaseNTestDataSplitPane.setTopComponent(testDesign.getTestCaseComponent());
        testCaseNTestDataSplitPane.setBottomComponent(testDesign.getTestDatacomp());

        oneTwo = new JSplitPane();
        oneTwo.setOneTouchExpandable(true);
        oneTwo.setResizeWeight(0.25);

        oneTwo.setLeftComponent(projectNReusableTreeSplitPane);
        oneTwo.setRightComponent(testCaseNTestDataSplitPane);

        oneThree = new JSplitPane();
        oneThree.setOneTouchExpandable(true);
        oneThree.setResizeWeight(0.8);

        oneThree.setLeftComponent(oneTwo);
        oneThree.setRightComponent(testDesign.getObjectRepo());

        add(oneThree);
        
        // Apply initial pane backgrounds
        applyPaneBackgrounds();
    }
    
    /**
     * Applies themed backgrounds to the panes.
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
        
        // Sidebar panes (dark black in dark mode)
        if (testPlanPanel != null) {
            applyBackgroundRecursively(testPlanPanel, sidebarColor, dividerColor);
        }
        if (appReusablePanel != null) {
            applyBackgroundRecursively(appReusablePanel, sidebarColor, dividerColor);
        }
        
        // Object Repository pane (dark black in dark mode)
        applyBackgroundRecursively(testDesign.getObjectRepo(), sidebarColor, dividerColor);
        
        // Test Data pane (dark black in dark mode)
        applyBackgroundRecursively(testDesign.getTestDatacomp(), sidebarColor, dividerColor);
        
        // Test Steps pane (slightly lighter in dark mode)
        applyBackgroundRecursively(testDesign.getTestCaseComponent(), editorColor, dividerColor);
        
        // Split pane dividers
        projectNReusableTreeSplitPane.setBackground(dividerColor);
        testCaseNTestDataSplitPane.setBackground(dividerColor);
        oneTwo.setBackground(dividerColor);
        oneThree.setBackground(dividerColor);
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
            // Recurse into children
            applyBackgroundRecursively(split.getLeftComponent(), bgColor, dividerColor);
            applyBackgroundRecursively(split.getRightComponent(), bgColor, dividerColor);
            return;
        }
        
        // Handle JScrollPane - set background on pane and viewport
        if (comp instanceof javax.swing.JScrollPane) {
            javax.swing.JScrollPane scroll = (javax.swing.JScrollPane) comp;
            scroll.setBackground(bgColor);
            scroll.getViewport().setBackground(bgColor);
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
            // Also update the cell renderer if it has updateSelectionColors method
            if (tree.getCellRenderer() instanceof com.ing.ide.main.utils.tree.TreeSelectionRenderer) {
                ((com.ing.ide.main.utils.tree.TreeSelectionRenderer) tree.getCellRenderer()).updateSelectionColors();
            }
            return;
        }
        
        // Handle JList
        if (comp instanceof javax.swing.JList) {
            comp.setBackground(bgColor);
            return;
        }
        
        // Handle JToolBar - keep its styled background
        if (comp instanceof javax.swing.JToolBar) {
            return;
        }
        
        // Handle general JPanel and Container
        if (comp instanceof java.awt.Container) {
            comp.setBackground(bgColor);
            if (comp instanceof JPanel) {
                ((JPanel) comp).setOpaque(true);
            }
            // Recurse into children
            java.awt.Container container = (java.awt.Container) comp;
            for (java.awt.Component child : container.getComponents()) {
                applyBackgroundRecursively(child, bgColor, dividerColor);
            }
        }
    }

    public void resetAfterRecorder() {
        testCaseNTestDataSplitPane.setTopComponent(testDesign.getTestCaseComponent());
        testCaseNTestDataSplitPane.setDividerLocation(0.5);
    }

    private JPanel getTreeInPanel(String labelText, JTree tree) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        registerFont();

        FXPanelHeader header = new FXPanelHeader(labelText,
                new FXPanelHeader.HeaderAction(
                        "Go to Previous TestCase",
                        "upOneLevel",
                        () -> SwingUtilities.invokeLater(() ->
                                testDesign.getTestCaseComp().actionPerformed(
                                        new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "Up One Level")))),
                new FXPanelHeader.HeaderAction(
                        "Add/Remove Tags",
                        "tag",
                        () -> SwingUtilities.invokeLater(() ->
                                testDesign.getProjectTree().actionPerformed(
                                        new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "Edit Tag"))))
        );

        panel.add(header, BorderLayout.NORTH);
        panel.add(TreeSearch.installFor(tree), BorderLayout.CENTER);
        return panel;
    }

    private JPanel getRTreeInPanel(String labelText, JTree tree) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        registerFont();

        // Reusable panel uses a clickable header (like legacy reusableSwitch button)
        reusableSwitch = new JButton(labelText);
        reusableSwitch.setFont(new Font("ING Me", Font.BOLD, 12));
        reusableSwitch.setContentAreaFilled(false);

        FXPanelHeader header = new FXPanelHeader(labelText);
        panel.add(header, BorderLayout.NORTH);
        panel.add(TreeSearch.installFor(tree), BorderLayout.CENTER);
        return panel;
    }

    private void registerFont() {
        try {
            Font customFont = Font.createFont(Font.TRUETYPE_FONT,
                    new File("resources/ui/resources/fonts/ingme_regular.ttf"));
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(customFont);
        } catch (IOException | FontFormatException e) {
            // Font registration is best-effort
        }
    }

    public void adjustUI() {
        oneTwo.setDividerLocation(0.25);
        oneThree.setDividerLocation(0.8);
        oneTwo.setDividerLocation(0.25);
        projectNReusableTreeSplitPane.setDividerLocation(0.5);
        testCaseNTestDataSplitPane.setDividerLocation(0.5);
        testDesign.getObjectRepo().adjustUI();
        
        // Reapply pane backgrounds for theme changes
        applyPaneBackgrounds();
    }

}

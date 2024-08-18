
package com.ing.ide.main.mainui.components.testdesign;

import com.ing.ide.main.utils.Utils;
import com.ing.ide.main.utils.tree.TreeSearch;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

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

        projectNReusableTreeSplitPane.setTopComponent(getTreeInPanel("Test Plan", testDesign.getProjectTree().getTree()));

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

    }

    public void resetAfterRecorder() {
        testCaseNTestDataSplitPane.setTopComponent(testDesign.getTestCaseComponent());
        testCaseNTestDataSplitPane.setDividerLocation(0.5);
    }

    private JPanel getTreeInPanel(String labelText, JTree tree) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setBorder(new EmptyBorder(5,5,5,5));//
        toolBar.setMargin(new Insets(5,5,5,5));//
        JLabel label = new JLabel(labelText);
        
        try {
            //create the font to use. Specify the size!
            Font customFont = Font.createFont(Font.TRUETYPE_FONT, new File("resources/ui/resources/fonts/ingme_regular.ttf"));//.deriveFont(12f);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            //register the font
            ge.registerFont(customFont);
        } catch (IOException | FontFormatException e) {
           // e.printStackTrace();
        }
        
        label.setFont(new Font("Jost", Font.BOLD, 12));
        toolBar.add(new javax.swing.Box.Filler(new java.awt.Dimension(10, 0),
                new java.awt.Dimension(10, 0),
                new java.awt.Dimension(10, 32767)));
        toolBar.add(label);
        toolBar.add(new javax.swing.Box.Filler(new java.awt.Dimension(0, 0),
                new java.awt.Dimension(0, 0),
                new java.awt.Dimension(32767, 32767)));
        toolBar.add(getPrevoiusTestCaseButton());

        toolBar.add(getEditTagButton());
        toolBar.setPreferredSize(new java.awt.Dimension(toolBar.getPreferredSize().width, 30));
        panel.add(toolBar, BorderLayout.NORTH);
        panel.add(TreeSearch.installFor(tree), BorderLayout.CENTER);
        return panel;
    }

    private JButton getPrevoiusTestCaseButton() {
        return Utils.createButton(
                "Up One Level",
                "uponelevel",
                "Go to Prevoius TestCase",
                testDesign.getTestCaseComp());
    }

    private JButton getEditTagButton() {
        return Utils.createButton(
                "Edit Tag",
                "tag",
                "Add/Remove Tags",
                testDesign.getProjectTree());
    }

    private JPanel getRTreeInPanel(String labelText, JTree tree) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        //toolBar.setBorder(BorderFactory.createEtchedBorder());
        toolBar.setBorder(new EmptyBorder(5,5,5,5));//
        toolBar.setMargin(new Insets(5,5,5,5));//
        reusableSwitch = new JButton(labelText);
        
        try {
            //create the font to use. Specify the size!
            Font customFont = Font.createFont(Font.TRUETYPE_FONT, new File("resources/ui/resources/fonts/ingme_regular.ttf"));//.deriveFont(12f);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            //register the font
            ge.registerFont(customFont);
        } catch (IOException | FontFormatException e) {
          //  e.printStackTrace();
        }
        
        reusableSwitch.setFont(new Font("ING Me", Font.BOLD, 12));
        reusableSwitch.setContentAreaFilled(false);

        toolBar.add(new javax.swing.Box.Filler(new java.awt.Dimension(10, 0),
                new java.awt.Dimension(10, 0),
                new java.awt.Dimension(10, 32767)));
        toolBar.add(reusableSwitch);
        toolBar.setPreferredSize(new java.awt.Dimension(toolBar.getPreferredSize().width, 30));

        panel.add(toolBar, BorderLayout.NORTH);
        panel.add(TreeSearch.installFor(tree), BorderLayout.CENTER);
        return panel;
    }

    public void adjustUI() {
        oneTwo.setDividerLocation(0.25);
        oneThree.setDividerLocation(0.8);
        oneTwo.setDividerLocation(0.25);
        projectNReusableTreeSplitPane.setDividerLocation(0.5);
        testCaseNTestDataSplitPane.setDividerLocation(0.5);
        testDesign.getObjectRepo().adjustUI();
    }

}

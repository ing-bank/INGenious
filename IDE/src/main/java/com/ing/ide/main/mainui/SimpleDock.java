
package com.ing.ide.main.mainui;

import com.ing.ide.main.utils.Utils;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.Box.Filler;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;

/**
 *
 * 
 */
public class SimpleDock extends JPanel implements ActionListener {

    private JButton testDesignButton;
    private JButton testExecutionButton;
    private JButton dashBoardButton;

    private final AppMainFrame mainFrame;

    public SimpleDock(AppMainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setBackground(new Color(0, 0, 0, 0));
        setOpaque(false);
        setLayout(new BorderLayout());
        add(getDock(), BorderLayout.WEST);
        initFiller();
    }

    private void initFiller() {
        Filler filler = new Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(32767, 32767));
        filler.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent me) {
                mainFrame.getGlassPane().setVisible(false);
            }
        });
        add(filler, BorderLayout.CENTER);
    }

    private JPanel getDock() {

        JPanel dPanel = new JPanel(new GridLayout(7, 1));
        dPanel.setOpaque(false);
        dPanel.setBackground(new Color(0, 0, 0, 0));
        
        testDesignButton = create("TestDesign");
        testExecutionButton = create("TestExecution");
        dashBoardButton = create("DashBoard");

        dPanel.add(getLeftFiller());
        dPanel.add(getLeftFiller());

        dPanel.add(testDesignButton);
        dPanel.add(testExecutionButton);
        dPanel.add(dashBoardButton);

        dPanel.add(getLeftFiller());
        dPanel.add(getLeftFiller());

        return dPanel;
    }

    private Filler getLeftFiller() {
        Filler filler = new Filler(
                new Dimension(0, 0),
                new Dimension(0, 0),
                new Dimension(32767, 32767));
        filler.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                mainFrame.getGlassPane().setVisible(false);
            }
        });

        return filler;
    }

    private JButton create(String text) {
        JButton button = new JButton();
        button.setIcon(Utils.getIconByResourceName("/ui/resources/dock/" + text.toLowerCase()));
        button.setActionCommand(text);
        button.setHorizontalTextPosition(SwingConstants.CENTER);
        //button.setToolTipText(text);
        //button.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        button.setBackground(Color.WHITE);
       
        button.addActionListener(this);
        return button;
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        switch (ae.getActionCommand()) {
            case "TestDesign":
                mainFrame.showTestDesign();
                break;
            case "TestExecution":
                mainFrame.showTestExecution();
                break;
            case "DashBoard":
                mainFrame.showDashBoard();
                break;
        }
    }

}

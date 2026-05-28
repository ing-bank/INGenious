
package com.ing.ide.main.mainui;

import com.ing.ide.main.fx.INGIcons;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.Box.Filler;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Modern slide-out dock panel with large icons and labels for
 * Test Design, Test Execution, and Dashboard navigation.
 */
public class SimpleDock extends JPanel implements ActionListener {

    // ING Brand Colors
    private static final Color ING_YELLOW = Color.decode("#FFE100"); // Test Design theme
    private static final Color ING_ORANGE = Color.decode("#FF6200"); // Dashboard theme
    private static final Color ING_GREEN = Color.decode("#349651");  // Test Execution theme
    private static final Color DOCK_BG = Color.decode("#F7F4F1"); //Dock background
    private static final Color BUTTON_HOVER_BG = new Color(255, 255, 255, 40);
    private static final Color BUTTON_NORMAL_BG = new Color(0, 0, 0, 0);
    
    private static final int ICON_SIZE = 32;
    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_HEIGHT = 80;

    private DockButton testDesignButton;
    private DockButton testExecutionButton;
    private DockButton dashBoardButton;

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
        JPanel dPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Draw rounded rectangle background - 20% shorter from top and bottom, 5% trimmed from left
                int leftOffset = (int)(getWidth() * 0.05);
                int topOffset = (int)(getHeight() * 0.30);
                int bottomOffset = (int)(getHeight() * 0.30);
                int bgHeight = getHeight() - topOffset - bottomOffset;
                int bgWidth = getWidth() - leftOffset;
                g2d.setColor(DOCK_BG);
                g2d.fillRoundRect(leftOffset, topOffset, bgWidth, bgHeight, 16, 16);
                g2d.dispose();
                super.paintComponent(g);
            }
        };
        dPanel.setLayout(new GridBagLayout());
        dPanel.setOpaque(false);
        dPanel.setBackground(new Color(0, 0, 0, 0));
        dPanel.setBorder(BorderFactory.createEmptyBorder(12, 8, 12, 8));
        
        testDesignButton = createDockButton("TestDesign", "Design", "testdesign", ING_YELLOW);
        testExecutionButton = createDockButton("TestExecution", "Execution", "testexecution", ING_GREEN);
        dashBoardButton = createDockButton("DashBoard", "Dashboard", "dashboard", ING_ORANGE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.insets = new Insets(6, 0, 6, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridy = 0;
        dPanel.add(testDesignButton, gbc);
        gbc.gridy = 1;
        dPanel.add(testExecutionButton, gbc);
        gbc.gridy = 2;
        dPanel.add(dashBoardButton, gbc);

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

    private DockButton createDockButton(String actionCommand, String displayText, String iconKey, Color iconColor) {
        Icon icon = INGIcons.swing(iconKey, ICON_SIZE, iconColor);
        DockButton button = new DockButton(displayText, icon, iconColor);
        button.setActionCommand(actionCommand);
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

    /**
     * Custom styled button for the dock with icon, label, and hover effects.
     */
    private static class DockButton extends JButton {
        private boolean isHovered = false;
        private final Color accentColor;
        
        public DockButton(String text, Icon icon, Color accentColor) {
            super();
            this.accentColor = accentColor;
            
            setLayout(new BorderLayout(0, 4));
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            
            // Icon at top
            JLabel iconLabel = new JLabel(icon);
            iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
            add(iconLabel, BorderLayout.CENTER);
            
            // Text label at bottom
            JLabel textLabel = new JLabel(text);
            textLabel.setHorizontalAlignment(SwingConstants.CENTER);
            textLabel.setForeground(Color.DARK_GRAY);
            textLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
            add(textLabel, BorderLayout.SOUTH);
            
            setPreferredSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
            setMinimumSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
            setMaximumSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
            
            // Hover effects
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    isHovered = true;
                    repaint();
                }
                
                @Override
                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    repaint();
                }
            });
            
            setToolTipText(text);
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            if (isHovered) {
                // Draw hover background with accent color tint
                g2d.setColor(new Color(
                    accentColor.getRed(), 
                    accentColor.getGreen(), 
                    accentColor.getBlue(), 
                    50));
                g2d.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 12, 12);
                
                // Draw accent border
                g2d.setColor(new Color(
                    accentColor.getRed(), 
                    accentColor.getGreen(), 
                    accentColor.getBlue(), 
                    150));
                g2d.drawRoundRect(2, 2, getWidth() - 5, getHeight() - 5, 12, 12);
            }
            
            g2d.dispose();
            super.paintComponent(g);
        }
    }

}

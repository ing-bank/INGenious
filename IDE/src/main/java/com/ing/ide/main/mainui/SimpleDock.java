package com.ing.ide.main.mainui;

import com.ing.ide.main.fx.INGIcons;
import java.awt.BasicStroke;
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
import javax.swing.BoxLayout;
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
    // Dock background
    private static final Color DOCK_BG = Color.decode("#F0EDE8");

    // Per-button normal and hover background colors
    private static final Color BG_DESIGN_NORMAL = Color.decode("#FFEE73");
    private static final Color BG_DESIGN_HOVER = Color.decode("#FFE100");
    private static final Color BG_EXEC_NORMAL = Color.decode("#CDF4DB");
    private static final Color BG_EXEC_HOVER = Color.decode("#1E8700");
    private static final Color BG_DASH_NORMAL = Color.decode("#F1E9FF");
    private static final Color BG_DASH_HOVER = Color.decode("#B487FF");
    private static final Color BG_API_NORMAL = Color.decode("#E4F5FF");
    private static final Color BG_API_HOVER = Color.decode("#BEE8FE");

    private static final Color ICON_NORMAL = Color.decode("#222222");
    private static final Color ICON_WHITE = Color.WHITE;
    private static final Color TEXT_DARK = Color.decode("#1A1A1A");

    private static final int ICON_SIZE = 30;
    private static final int BUTTON_WIDTH = 115;
    private static final int BUTTON_HEIGHT = 84;

    private DockButton testDesignButton;
    private DockButton testExecutionButton;
    private DockButton dashBoardButton;
    private DockButton apiWorkbenchButton;

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
        Filler filler = new Filler(
            new Dimension(0, 0),
            new Dimension(0, 0),
            new Dimension(32767, 32767)
        );
        filler.addMouseListener(
            new MouseAdapter() {

                @Override
                public void mouseEntered(MouseEvent me) {
                    mainFrame.getGlassPane().setVisible(false);
                }
            }
        );
        add(filler, BorderLayout.CENTER);
    }

    private JPanel getDock() {
        JPanel dPanel = new JPanel() {

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
                );
                // Draw rounded rectangle background - 20% shorter from top and bottom, 5% trimmed from left
                int leftOffset = (int) (getWidth() * 0.05);
                int topOffset = (int) (getHeight() * 0.30);
                int bottomOffset = (int) (getHeight() * 0.30);
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

        testDesignButton =
            createDockButton(
                "TestDesign",
                "Design",
                "testdesign",
                BG_DESIGN_NORMAL,
                BG_DESIGN_HOVER,
                false
            );
        testExecutionButton =
            createDockButton(
                "TestExecution",
                "Execution",
                "testexecution",
                BG_EXEC_NORMAL,
                BG_EXEC_HOVER,
                true
            );
        dashBoardButton =
            createDockButton(
                "DashBoard",
                "Dashboard",
                "dashboard",
                BG_DASH_NORMAL,
                BG_DASH_HOVER,
                true
            );
        apiWorkbenchButton =
            createDockButton(
                "APIWorkbench",
                "API Workbench",
                "apidock",
                BG_API_NORMAL,
                BG_API_HOVER,
                false
            );

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
        gbc.gridy = 3;
        dPanel.add(apiWorkbenchButton, gbc);

        return dPanel;
    }

    private Filler getLeftFiller() {
        Filler filler = new Filler(
            new Dimension(0, 0),
            new Dimension(0, 0),
            new Dimension(32767, 32767)
        );
        filler.addMouseListener(
            new MouseAdapter() {

                @Override
                public void mouseClicked(MouseEvent me) {
                    mainFrame.getGlassPane().setVisible(false);
                }
            }
        );

        return filler;
    }

    private DockButton createDockButton(
        String actionCommand,
        String displayText,
        String iconKey,
        Color normalBg,
        Color hoverBg,
        boolean invertOnHover
    ) {
        Icon normalIcon = INGIcons.swing(iconKey, ICON_SIZE, ICON_NORMAL);
        Icon hoverIcon = INGIcons.swing(
            iconKey,
            ICON_SIZE,
            invertOnHover ? ICON_WHITE : ICON_NORMAL
        );
        DockButton button = new DockButton(
            displayText,
            normalIcon,
            hoverIcon,
            normalBg,
            hoverBg,
            invertOnHover
        );
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
            case "APIWorkbench":
                mainFrame.showAPITester();
                break;
        }
    }

    /**
     * Custom dock button with solid per-button background, shadow, and
     * icon/text colour inversion on hover where requested.
     */
    private static class DockButton extends JButton {
        private boolean isHovered = false;
        private final Color normalBg;
        private final Color hoverBg;
        private final boolean invertOnHover;
        private final JLabel iconLabel;
        private final JLabel textLabel;
        private final Icon normalIcon;
        private final Icon hoverIcon;
        private final JPanel contentPanel;

        public DockButton(
            String text,
            Icon normalIcon,
            Icon hoverIcon,
            Color normalBg,
            Color hoverBg,
            boolean invertOnHover
        ) {
            super();
            this.normalBg = normalBg;
            this.hoverBg = hoverBg;
            this.invertOnHover = invertOnHover;
            this.normalIcon = normalIcon;
            this.hoverIcon = hoverIcon;

            setLayout(new BorderLayout());
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            // Keep content centered and away from the bottom edge.
            contentPanel = new JPanel();
            contentPanel.setOpaque(false);
            contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
            contentPanel.setBorder(BorderFactory.createEmptyBorder(6, 0, 10, 0));

            iconLabel = new JLabel(normalIcon);
            iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
            iconLabel.setAlignmentX(0.5f);
            contentPanel.add(iconLabel);
            contentPanel.add(
                new javax.swing.Box.Filler(
                    new Dimension(0, 8),
                    new Dimension(0, 8),
                    new Dimension(0, 8)
                )
            );

            // Label with no minimum width so long text scales down without "..."
            textLabel = new JLabel(text, SwingConstants.CENTER);
            textLabel.setForeground(TEXT_DARK);
            textLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
            textLabel.setMinimumSize(new Dimension(0, textLabel.getPreferredSize().height));
            textLabel.setAlignmentX(0.5f);
            contentPanel.add(textLabel);

            add(contentPanel, BorderLayout.CENTER);

            setPreferredSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
            setMinimumSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
            setMaximumSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));

            addMouseListener(
                new MouseAdapter() {

                    @Override
                    public void mouseEntered(MouseEvent e) {
                        isHovered = true;
                        iconLabel.setIcon(hoverIcon);
                        textLabel.setForeground(invertOnHover ? Color.WHITE : TEXT_DARK);
                        repaint();
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        isHovered = false;
                        iconLabel.setIcon(normalIcon);
                        textLabel.setForeground(TEXT_DARK);
                        repaint();
                    }
                }
            );

            setToolTipText(text);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            );
            g2d.setRenderingHint(
                RenderingHints.KEY_STROKE_CONTROL,
                RenderingHints.VALUE_STROKE_PURE
            );

            Color bg = isHovered ? hoverBg : normalBg;

            // Drop shadow (bottom-right offset)
            g2d.setColor(new Color(0, 0, 0, 22));
            g2d.fillRoundRect(3, 4, getWidth() - 4, getHeight() - 4, 14, 14);

            // Solid background fill
            g2d.setColor(bg);
            g2d.fillRoundRect(1, 1, getWidth() - 4, getHeight() - 5, 14, 14);

            // Border — auto-derived darker tint of the active background
            float[] hsb = Color.RGBtoHSB(bg.getRed(), bg.getGreen(), bg.getBlue(), null);
            Color border = Color.getHSBColor(
                hsb[0],
                Math.min(1f, hsb[1] * 1.6f),
                Math.max(0f, hsb[2] * 0.65f)
            );
            g2d.setColor(border);
            g2d.setStroke(new BasicStroke(isHovered ? 1.5f : 1.0f));
            g2d.drawRoundRect(1, 1, getWidth() - 5, getHeight() - 6, 14, 14);

            g2d.dispose();
            super.paintComponent(g);
        }
    }
}

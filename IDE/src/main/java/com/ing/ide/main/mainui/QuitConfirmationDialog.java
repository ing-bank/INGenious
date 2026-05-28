package com.ing.ide.main.mainui;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignE;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignS;
import org.kordamp.ikonli.swing.FontIcon;

/**
 * Modern styled quit confirmation dialog for INGenious Studio
 */
public class QuitConfirmationDialog extends JDialog {
    
    // Theme colors matching INGenious Studio
    private static final Color ING_PURPLE = new Color(119, 36, 255);      // #7724FF
    private static final Color DIALOG_BG = new Color(45, 45, 50);          // Dark background
    private static final Color CARD_BG = new Color(55, 55, 60);            // Card background
    private static final Color TEXT_PRIMARY = Color.WHITE;
    private static final Color TEXT_SECONDARY = new Color(180, 180, 185);
    private static final Color BORDER_COLOR = new Color(70, 70, 75);
    private static final Color HOVER_COLOR = new Color(65, 65, 70);
    
    // Button colors
    private static final Color SAVE_BTN_BG = ING_PURPLE;
    private static final Color SAVE_BTN_HOVER = new Color(139, 56, 255);
    private static final Color DONT_SAVE_BTN_BG = new Color(75, 75, 80);
    private static final Color DONT_SAVE_BTN_HOVER = new Color(85, 85, 90);
    private static final Color CANCEL_BTN_BG = CARD_BG;
    private static final Color CANCEL_BTN_HOVER = new Color(65, 65, 70);
    
    private int result = JOptionPane.CANCEL_OPTION;
    private boolean includeCancel;
    
    /**
     * Creates a styled quit confirmation dialog
     * @param parent Parent frame
     * @param includeCancel Whether to include the Cancel button (for YES_NO_CANCEL option)
     */
    public QuitConfirmationDialog(Frame parent, boolean includeCancel) {
        super(parent, "Quit INGenious Studio", true);
        this.includeCancel = includeCancel;
        initComponents();
    }
    
    private void initComponents() {
        setUndecorated(true);
        setSize(520, 200);  // Wider to fit all buttons
        setLocationRelativeTo(getParent());
        setBackground(new Color(0, 0, 0, 0));
        
        // Main panel with rounded corners
        JPanel mainPanel = new JPanel(new BorderLayout(0, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(DIALOG_BG);
                g2d.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
                g2d.setColor(BORDER_COLOR);
                g2d.setStroke(new BasicStroke(1));
                g2d.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 16, 16));
                g2d.dispose();
            }
        };
        mainPanel.setOpaque(false);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        
        // Header with icon and title
        JPanel headerPanel = new JPanel(new BorderLayout(12, 0));
        headerPanel.setOpaque(false);
        
        // Exit icon
        FontIcon exitIcon = FontIcon.of(MaterialDesignE.EXIT_TO_APP, 32, ING_PURPLE);
        JLabel iconLabel = new JLabel(exitIcon);
        headerPanel.add(iconLabel, BorderLayout.WEST);
        
        // Title and message
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        
        JLabel titleLabel = new JLabel("Quit INGenious Studio");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel messageLabel = new JLabel("Do you want to save the Project before quitting?");
        messageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        messageLabel.setForeground(TEXT_SECONDARY);
        messageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        messageLabel.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        
        textPanel.add(titleLabel);
        textPanel.add(messageLabel);
        
        headerPanel.add(textPanel, BorderLayout.CENTER);
        
        // Close button
        JButton closeBtn = createCloseButton();
        headerPanel.add(closeBtn, BorderLayout.EAST);
        
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        
        // Spacer
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        spacer.setPreferredSize(new Dimension(0, 20));
        mainPanel.add(spacer, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);
        
        // Cancel button (optional)
        if (includeCancel) {
            JButton cancelBtn = createButton("Cancel", MaterialDesignC.CLOSE, CANCEL_BTN_BG, CANCEL_BTN_HOVER, false);
            cancelBtn.addActionListener(e -> {
                result = JOptionPane.CANCEL_OPTION;
                dispose();
            });
            buttonPanel.add(cancelBtn);
        }
        
        // Don't Save button
        JButton dontSaveBtn = createButton("Don't Save", MaterialDesignE.EXIT_RUN, DONT_SAVE_BTN_BG, DONT_SAVE_BTN_HOVER, false);
        dontSaveBtn.addActionListener(e -> {
            result = JOptionPane.NO_OPTION;
            dispose();
        });
        buttonPanel.add(dontSaveBtn);
        
        // Save & Quit button (primary)
        JButton saveBtn = createButton("Save & Quit", MaterialDesignS.SEND, SAVE_BTN_BG, SAVE_BTN_HOVER, true);
        saveBtn.addActionListener(e -> {
            result = JOptionPane.YES_OPTION;
            dispose();
        });
        buttonPanel.add(saveBtn);
        
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        setContentPane(mainPanel);
        
        // Allow dragging the dialog
        addDragSupport(mainPanel);
        
        // Handle escape key
        getRootPane().registerKeyboardAction(e -> {
            result = JOptionPane.CANCEL_OPTION;
            dispose();
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        
        // Set focus to save button
        saveBtn.requestFocusInWindow();
    }
    
    private JButton createCloseButton() {
        FontIcon closeIcon = FontIcon.of(MaterialDesignC.CLOSE, 18, TEXT_SECONDARY);
        JButton btn = new JButton(closeIcon);
        btn.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(28, 28));
        
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setContentAreaFilled(true);
                btn.setBackground(HOVER_COLOR);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setContentAreaFilled(false);
            }
        });
        
        btn.addActionListener(e -> {
            result = JOptionPane.CANCEL_OPTION;
            dispose();
        });
        
        return btn;
    }
    
    private JButton createButton(String text, Ikon iconType, Color bgColor, Color hoverColor, boolean isPrimary) {
        FontIcon icon = FontIcon.of(iconType, 16, isPrimary ? Color.WHITE : TEXT_SECONDARY);
        JButton btn = new JButton(text, icon);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(isPrimary ? Color.WHITE : TEXT_SECONDARY);
        btn.setBackground(bgColor);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(isPrimary ? ING_PURPLE : BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(8, 14, 8, 14)
        ));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setIconTextGap(8);
        
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(hoverColor);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(bgColor);
            }
        });
        
        return btn;
    }
    
    private void addDragSupport(JPanel panel) {
        final Point[] dragOffset = {null};
        
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragOffset[0] = e.getPoint();
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                dragOffset[0] = null;
            }
        });
        
        panel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragOffset[0] != null) {
                    Point loc = getLocation();
                    setLocation(loc.x + e.getX() - dragOffset[0].x, 
                               loc.y + e.getY() - dragOffset[0].y);
                }
            }
        });
    }
    
    /**
     * Shows the dialog and returns the user's choice
     * @return JOptionPane.YES_OPTION, JOptionPane.NO_OPTION, or JOptionPane.CANCEL_OPTION
     */
    public int showDialog() {
        setVisible(true);
        return result;
    }
    
    /**
     * Static method to show the quit confirmation dialog
     * @param parent Parent frame
     * @param optionType JOptionPane.YES_NO_OPTION or JOptionPane.YES_NO_CANCEL_OPTION
     * @return User's choice
     */
    public static int showConfirmation(Frame parent, int optionType) {
        boolean includeCancel = (optionType == JOptionPane.YES_NO_CANCEL_OPTION);
        QuitConfirmationDialog dialog = new QuitConfirmationDialog(parent, includeCancel);
        return dialog.showDialog();
    }
}

package com.ing.ide.main.utils;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * Modern styled confirmation dialog with rounded corners and modern UI design.
 */
public class StyledConfirmDialog extends JDialog {
    
    // Dialog types
    public static final int CONFIRM = 0;
    public static final int WARNING = 1;
    public static final int INFO = 2;
    public static final int ERROR = 3;
    
    // Return values
    public static final int YES_OPTION = 0;
    public static final int NO_OPTION = 1;
    public static final int OK_OPTION = 0;
    public static final int CANCEL_OPTION = 2;
    
    private int result = CANCEL_OPTION;
    
    // Modern color scheme
    private static final Color PRIMARY_COLOR = new Color(33, 150, 243);      // Blue
    private static final Color WARNING_COLOR = new Color(255, 152, 0);       // Orange
    private static final Color ERROR_COLOR = new Color(244, 67, 54);         // Red
    private static final Color SUCCESS_COLOR = new Color(76, 175, 80);       // Green
    private static final Color BACKGROUND = new Color(250, 250, 250);
    private static final Color DARK_TEXT = new Color(33, 33, 33);
    private static final Color LIGHT_TEXT = new Color(117, 117, 117);
    private static final Color BUTTON_BG = new Color(240, 240, 240);
    private static final Color BUTTON_HOVER = new Color(230, 230, 230);
    
    private StyledConfirmDialog(Component parent, String message, String title, int dialogType, boolean showCancel) {
        super(parent instanceof Window ? (Window) parent : SwingUtilities.getWindowAncestor(parent), 
              title, ModalityType.APPLICATION_MODAL);
        
        initDialog(message, title, dialogType, showCancel);
    }
    
    private void initDialog(String message, String title, int dialogType, boolean showCancel) {
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Draw shadow
                for (int i = 0; i < 10; i++) {
                    float alpha = (10 - i) / 100f;
                    g2.setColor(new Color(0, 0, 0, (int)(alpha * 50)));
                    g2.fill(new RoundRectangle2D.Float(i, i, getWidth() - i * 2, getHeight() - i * 2, 15, 15));
                }
                
                // Draw main background
                g2.setColor(BACKGROUND);
                g2.fill(new RoundRectangle2D.Float(10, 10, getWidth() - 20, getHeight() - 20, 15, 15));
                g2.dispose();
            }
        };
        mainPanel.setOpaque(false);
        mainPanel.setLayout(new BorderLayout(0, 10));
        mainPanel.setBorder(new EmptyBorder(20, 30, 20, 30));
        
        // Header panel with icon and colored strip
        JPanel headerPanel = new JPanel(new BorderLayout(15, 0));
        headerPanel.setOpaque(false);
        
        // Icon based on dialog type
        JLabel iconLabel = new JLabel(getIconForType(dialogType));
        iconLabel.setVerticalAlignment(SwingConstants.TOP);
        headerPanel.add(iconLabel, BorderLayout.WEST);
        
        // Title label
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(DARK_TEXT);
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        
        // Message panel
        JTextArea messageArea = new JTextArea(message);
        messageArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        messageArea.setForeground(LIGHT_TEXT);
        messageArea.setBackground(BACKGROUND);
        messageArea.setEditable(false);
        messageArea.setWrapStyleWord(true);
        messageArea.setLineWrap(true);
        messageArea.setBorder(new EmptyBorder(5, 0, 15, 0));
        messageArea.setOpaque(false);
        
        mainPanel.add(messageArea, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);
        
        if (showCancel) {
            JButton cancelButton = createStyledButton("Cancel", BUTTON_BG, DARK_TEXT);
            cancelButton.addActionListener(e -> {
                result = CANCEL_OPTION;
                dispose();
            });
            buttonPanel.add(cancelButton);
            
            JButton noButton = createStyledButton("No", BUTTON_BG, DARK_TEXT);
            noButton.addActionListener(e -> {
                result = NO_OPTION;
                dispose();
            });
            buttonPanel.add(noButton);
        } else {
            JButton cancelButton = createStyledButton("Cancel", BUTTON_BG, DARK_TEXT);
            cancelButton.addActionListener(e -> {
                result = NO_OPTION;
                dispose();
            });
            buttonPanel.add(cancelButton);
        }
        
        Color primaryColor = getColorForType(dialogType);
        JButton okButton = createStyledButton(showCancel ? "Yes" : "OK", primaryColor, Color.WHITE);
        okButton.addActionListener(e -> {
            result = YES_OPTION;
            dispose();
        });
        buttonPanel.add(okButton);
        
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
        
        // Set dialog size
        setSize(450, 200);
        setLocationRelativeTo(getOwner());
        
        // Make dialog draggable
        makeDraggable(mainPanel);
        
        // ESC to close
        getRootPane().registerKeyboardAction(
            e -> {
                result = CANCEL_OPTION;
                dispose();
            },
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }
    
    private JButton createStyledButton(String text, Color bg, Color fg) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (getModel().isPressed()) {
                    g2.setColor(bg.darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(bg.equals(BUTTON_BG) ? BUTTON_HOVER : bg.brighter());
                } else {
                    g2.setColor(bg);
                }
                
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setForeground(fg);
        button.setPreferredSize(new Dimension(90, 35));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }
    
    private String getIconForType(int type) {
        switch (type) {
            case WARNING: return "⚠️";
            case ERROR: return "❌";
            case INFO: return "ℹ️";
            default: return "❓";
        }
    }
    
    private Color getColorForType(int type) {
        switch (type) {
            case WARNING: return WARNING_COLOR;
            case ERROR: return ERROR_COLOR;
            case INFO: return PRIMARY_COLOR;
            default: return PRIMARY_COLOR;
        }
    }
    
    private void makeDraggable(JPanel panel) {
        final Point[] mouseDownCompCoords = new Point[1];
        
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                mouseDownCompCoords[0] = e.getPoint();
            }
        });
        
        panel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                Point currCoords = e.getLocationOnScreen();
                setLocation(currCoords.x - mouseDownCompCoords[0].x, 
                           currCoords.y - mouseDownCompCoords[0].y);
            }
        });
    }
    
    /**
     * Show a confirmation dialog (Yes/No/Cancel).
     */
    public static int showConfirm(Component parent, String message, String title, int type) {
        StyledConfirmDialog dialog = new StyledConfirmDialog(parent, message, title, type, true);
        dialog.setVisible(true);
        return dialog.result;
    }
    
    /**
     * Show a simple confirmation dialog (Yes/No).
     */
    public static int showYesNo(Component parent, String message, String title, int type) {
        StyledConfirmDialog dialog = new StyledConfirmDialog(parent, message, title, type, false);
        dialog.setVisible(true);
        return dialog.result;
    }
    
    /**
     * Show an OK/Cancel dialog.
     */
    public static int showOkCancel(Component parent, String message, String title, int type) {
        return showYesNo(parent, message, title, type);
    }
    
    /**
     * Show delete confirmation dialog.
     */
    public static boolean showDeleteConfirm(Component parent, String message) {
        int result = showYesNo(parent, message, "Delete Confirmation", WARNING);
        return result == YES_OPTION;
    }
}

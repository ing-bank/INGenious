
package com.ing.ide.main.utils;

import org.kordamp.ikonli.materialdesign2.MaterialDesignM;
import org.kordamp.ikonli.swing.FontIcon;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Stylized search box with search icon, rounded corners, and focus effects.
 */
public class SearchBox extends JTextField implements DocumentListener, FocusListener {

    private final ActionListener actionListener;
    private TextBoxPlaceHolder textPrompt;
    
    // Style constants (defaults for light mode, overridden in dark mode)
    private static final int ARC_SIZE = 16;
    
    private boolean hasFocus = false;
    private Icon searchIcon;
    private Icon searchIconFocused;
    
    // Dynamic colors that adapt to theme
    private Color borderColor;
    private Color borderFocusColor;
    private Color backgroundColor;
    private Color iconColor;
    private Color iconFocusColor;
    private Color shadowColor;

    public SearchBox(ActionListener actionListener) {
        this.actionListener = actionListener;
        init();
    }

    private void init() {
        setOpaque(false);
        updateThemeColors();
        
        // Create search icons
        iconColor = isDarkMode() ? new Color(160, 150, 180) : new Color(140, 140, 160);
        iconFocusColor = new Color(119, 36, 255);
        searchIcon = FontIcon.of(MaterialDesignM.MAGNIFY, 16, iconColor);
        searchIconFocused = FontIcon.of(MaterialDesignM.MAGNIFY, 16, iconFocusColor);
        
        // Set up placeholder text
        textPrompt = new TextBoxPlaceHolder("Search Text", this);
        Color placeholderColor = isDarkMode() ? new Color(120, 115, 135) : new Color(156, 163, 175);
        textPrompt.setForeground(placeholderColor);
        textPrompt.setFont(UIManager.getFont("Table.font"));
        
        // Style the text field
        setBackground(backgroundColor);
        setForeground(isDarkMode() ? new Color(230, 225, 240) : Color.BLACK);
        setCaretColor(isDarkMode() ? new Color(230, 225, 240) : Color.BLACK);
        setPreferredSize(new Dimension(200, 32));
        
        setActionCommand("Search");

        setToolTipText("<html>"
                + "Press <b>F3</b> to go to next search"
                + "<br/>"
                + "Press <b>Shift+F3</b> to go to previous search"
                + "<br/>"
                + "To perfrom regex search add <b>$</b> before the search string"
                + "<br/>"
                + "Press <b>Esc</b> to Clear"
                + "<br/>"
                + "</html>");
        addActionListener(actionListener);
        addFocusListener(this);

        getDocument().addDocumentListener(this);
        
        getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ENTER"), "Search");

        getActionMap().put("GoToPrevoiusSearch", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                actionListener.actionPerformed(new ActionEvent(ae.getSource(), ae.getID(), "GoToPrevoiusSearch"));
            }
        });

        getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("shift F3"), "GoToPrevoiusSearch");

        getActionMap().put("GoToNextSearch", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                actionListener.actionPerformed(new ActionEvent(ae.getSource(), ae.getID(), "GoToNextSearch"));
            }
        });
        getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("F3"), "GoToNextSearch");
        int menuShortcutKeyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_X, menuShortcutKeyMask), "cut");
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_C, menuShortcutKeyMask), "copy");
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_V, menuShortcutKeyMask), "paste");
        getActionMap().put("ClearText", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                setText("");
            }
        });
        getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ESCAPE"), "ClearText");

    }

    public void focus() {
        requestFocusInWindow();
        selectAll();
    }

    @Override
    public void insertUpdate(DocumentEvent de) {
        actionListener.actionPerformed(new ActionEvent(this, 0, "Search"));
    }

    @Override
    public void removeUpdate(DocumentEvent de) {
        actionListener.actionPerformed(new ActionEvent(this, 0, "Search"));
    }

    @Override
    public void changedUpdate(DocumentEvent de) {
        actionListener.actionPerformed(new ActionEvent(this, 0, "Search"));
    }

    public void setPlaceHolder(String text, String toolTip) {
        textPrompt.setText("<html>Search in [ <b><font color='#7724FF'>" + text + "</font></b> ]<html>");
        textPrompt.setToolTipText(toolTip);
        textPrompt.setFont(UIManager.getFont("Table.font"));
    }
    
    /**
     * Check if dark mode is active.
     */
    private boolean isDarkMode() {
        return com.ing.ide.main.Main.isDarkMode();
    }
    
    /**
     * Update colors based on current theme.
     */
    private void updateThemeColors() {
        if (isDarkMode()) {
            // Dark mode colors
            backgroundColor = UIManager.getColor("searchBox");
            if (backgroundColor == null) backgroundColor = new Color(0x2D, 0x28, 0x38);
            borderColor = new Color(0x3A, 0x35, 0x45);
            borderFocusColor = new Color(255, 102, 0); // ING Orange
            shadowColor = new Color(0, 0, 0, 30);
        } else {
            // Light mode colors
            backgroundColor = Color.WHITE;
            borderColor = new Color(200, 200, 210);
            borderFocusColor = new Color(119, 36, 255); // Purple accent
            shadowColor = new Color(0, 0, 0, 15);
        }
    }
    
    /**
     * Called when theme changes to refresh colors.
     */
    @Override
    public void updateUI() {
        super.updateUI();
        // Guard against calls during super constructor before fields are initialized
        if (searchIcon == null) return;
        updateThemeColors();
        // Apply background color
        setBackground(backgroundColor);
        // Refresh icons with new theme colors
        iconColor = isDarkMode() ? new Color(160, 150, 180) : new Color(140, 140, 160);
        iconFocusColor = isDarkMode() ? new Color(255, 102, 0) : new Color(119, 36, 255);
        searchIcon = FontIcon.of(MaterialDesignM.MAGNIFY, 16, iconColor);
        searchIconFocused = FontIcon.of(MaterialDesignM.MAGNIFY, 16, iconFocusColor);
        setForeground(isDarkMode() ? new Color(230, 225, 240) : Color.BLACK);
        setCaretColor(isDarkMode() ? new Color(230, 225, 240) : Color.BLACK);
        if (textPrompt != null) {
            Color placeholderColor = isDarkMode() ? new Color(120, 115, 135) : new Color(156, 163, 175);
            textPrompt.setForeground(placeholderColor);
        }
        repaint();
    }
    
    /**
     * Compute colors dynamically based on current theme.
     * This ensures the colors are always correct even if updateUI() wasn't called.
     */
    private Color getThemeBackgroundColor() {
        if (isDarkMode()) {
            Color c = UIManager.getColor("searchBox");
            return c != null ? c : new Color(0x2D, 0x28, 0x38);
        } else {
            return Color.WHITE;
        }
    }
    
    private Color getThemeBorderColor() {
        return isDarkMode() ? new Color(0x3A, 0x35, 0x45) : new Color(200, 200, 210);
    }
    
    private Color getThemeShadowColor() {
        return isDarkMode() ? new Color(0, 0, 0, 30) : new Color(0, 0, 0, 15);
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        
        int w = getWidth();
        int h = getHeight();
        
        // Always compute colors based on current theme
        Color currentBg = getThemeBackgroundColor();
        Color currentBorder = getThemeBorderColor();
        Color currentShadow = getThemeShadowColor();
        
        // Paint subtle shadow
        g2.setColor(currentShadow);
        g2.fill(new RoundRectangle2D.Float(1, 2, w - 2, h - 2, ARC_SIZE, ARC_SIZE));
        
        // Paint rounded background
        g2.setColor(currentBg);
        g2.fill(new RoundRectangle2D.Float(1, 1, w - 3, h - 3, ARC_SIZE, ARC_SIZE));
        
        // Paint rounded border (thicker when focused)
        if (hasFocus) {
            Color focusGlow = isDarkMode() ? new Color(255, 102, 0, 60) : new Color(119, 36, 255, 60);
            g2.setColor(focusGlow);
            g2.setStroke(new java.awt.BasicStroke(3f));
            g2.draw(new RoundRectangle2D.Float(1.5f, 1.5f, w - 4, h - 4, ARC_SIZE, ARC_SIZE));
            Color fc = isDarkMode() ? new Color(255, 102, 0) : new Color(119, 36, 255);
            g2.setColor(fc);
            g2.setStroke(new java.awt.BasicStroke(1.5f));
            g2.draw(new RoundRectangle2D.Float(1.5f, 1.5f, w - 4, h - 4, ARC_SIZE, ARC_SIZE));
        } else {
            g2.setColor(currentBorder);
            g2.setStroke(new java.awt.BasicStroke(1f));
            g2.draw(new RoundRectangle2D.Float(1, 1, w - 3, h - 3, ARC_SIZE, ARC_SIZE));
        }
        
        g2.dispose();
        super.paintComponent(g);
        
        // Paint search icon on top - use current theme color
        Icon icon;
        if (hasFocus) {
            Color fc = isDarkMode() ? new Color(255, 102, 0) : new Color(119, 36, 255);
            icon = FontIcon.of(MaterialDesignM.MAGNIFY, 16, fc);
        } else {
            Color ic = isDarkMode() ? new Color(160, 150, 180) : new Color(140, 140, 160);
            icon = FontIcon.of(MaterialDesignM.MAGNIFY, 16, ic);
        }
        int iconY = (getHeight() - icon.getIconHeight()) / 2;
        icon.paintIcon(this, g, 10, iconY);
    }
    
    @Override
    public void focusGained(FocusEvent e) {
        hasFocus = true;
        repaint();
    }
    
    @Override
    public void focusLost(FocusEvent e) {
        hasFocus = false;
        repaint();
    }
    
    @Override
    public Insets getInsets() {
        return new Insets(8, 34, 8, 12);
    }

}

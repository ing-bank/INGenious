package com.ing.ide.main.utils.table;

import org.kordamp.ikonli.materialdesign2.*;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom renderer for Object Properties table attribute column.
 * Displays colorful icons next to each attribute name for better visual identification.
 */
public class PropertyAttributeRenderer extends DefaultTableCellRenderer {
    
    private static final int ICON_SIZE = 14;
    
    // Color palette for different attribute categories
    private static final Color CLR_ROLE = new Color(119, 36, 255);      // Purple - Role
    private static final Color CLR_TEXT = new Color(33, 136, 255);      // Blue - Text-based
    private static final Color CLR_LABEL = new Color(52, 150, 81);      // Green - Label
    private static final Color CLR_PLACEHOLDER = new Color(212, 136, 15); // Amber - Placeholder
    private static final Color CLR_XPATH = new Color(207, 34, 46);      // Red - xpath
    private static final Color CLR_CSS = new Color(3, 102, 214);        // Dark Blue - css
    private static final Color CLR_ALT = new Color(111, 66, 193);       // Violet - AltText
    private static final Color CLR_TITLE = new Color(0, 134, 114);      // Teal - Title
    private static final Color CLR_TESTID = new Color(232, 65, 24);     // Orange - TestId
    private static final Color CLR_CHAIN = new Color(130, 80, 223);     // Purple variant - ChainedLocator
    private static final Color CLR_DEFAULT = new Color(100, 100, 100);  // Gray - default
    
    // Mobile-specific colors
    private static final Color CLR_MOBILE_AUTO = new Color(16, 185, 129);   // Emerald - UiAutomator/UiAutomation
    private static final Color CLR_MOBILE_ID = new Color(245, 158, 11);     // Amber - id
    private static final Color CLR_MOBILE_ACCESS = new Color(99, 102, 241); // Indigo - Accessibility
    private static final Color CLR_MOBILE_NAME = new Color(139, 92, 246);   // Violet - name
    private static final Color CLR_MOBILE_TAG = new Color(236, 72, 153);    // Pink - tagName
    private static final Color CLR_MOBILE_LINK = new Color(6, 182, 212);    // Cyan - link_text
    private static final Color CLR_MOBILE_CLASS = new Color(234, 88, 12);   // Orange - class
    
    // Icon and color mapping for known attributes
    private static final Map<String, AttributeStyle> ATTRIBUTE_STYLES = new HashMap<>();
    
    static {
        // ===== Web Attributes =====
        ATTRIBUTE_STYLES.put("Role", new AttributeStyle(MaterialDesignA.ACCOUNT_BOX, CLR_ROLE));
        ATTRIBUTE_STYLES.put("Text", new AttributeStyle(MaterialDesignF.FORMAT_TEXT, CLR_TEXT));
        ATTRIBUTE_STYLES.put("Label", new AttributeStyle(MaterialDesignL.LABEL, CLR_LABEL));
        ATTRIBUTE_STYLES.put("Placeholder", new AttributeStyle(MaterialDesignF.FORM_TEXTBOX, CLR_PLACEHOLDER));
        ATTRIBUTE_STYLES.put("xpath", new AttributeStyle(MaterialDesignC.CODE_TAGS, CLR_XPATH));
        ATTRIBUTE_STYLES.put("css", new AttributeStyle(MaterialDesignL.LANGUAGE_CSS3, CLR_CSS));
        ATTRIBUTE_STYLES.put("AltText", new AttributeStyle(MaterialDesignI.IMAGE_TEXT, CLR_ALT));
        ATTRIBUTE_STYLES.put("Title", new AttributeStyle(MaterialDesignF.FORMAT_TITLE, CLR_TITLE));
        ATTRIBUTE_STYLES.put("TestId", new AttributeStyle(MaterialDesignI.IDENTIFIER, CLR_TESTID));
        ATTRIBUTE_STYLES.put("ChainedLocator", new AttributeStyle(MaterialDesignL.LINK_VARIANT, CLR_CHAIN));
        
        // ===== Mobile Attributes =====
        ATTRIBUTE_STYLES.put("UiAutomator", new AttributeStyle(MaterialDesignA.ANDROID, CLR_MOBILE_AUTO));
        ATTRIBUTE_STYLES.put("UiAutomation", new AttributeStyle(MaterialDesignA.APPLE, CLR_MOBILE_AUTO));
        ATTRIBUTE_STYLES.put("id", new AttributeStyle(MaterialDesignI.IDENTIFIER, CLR_MOBILE_ID));
        ATTRIBUTE_STYLES.put("Accessibility", new AttributeStyle(MaterialDesignH.HUMAN_HANDSUP, CLR_MOBILE_ACCESS));
        ATTRIBUTE_STYLES.put("name", new AttributeStyle(MaterialDesignT.TAG_TEXT, CLR_MOBILE_NAME));
        ATTRIBUTE_STYLES.put("tagName", new AttributeStyle(MaterialDesignC.CODE_BRACKETS, CLR_MOBILE_TAG));
        ATTRIBUTE_STYLES.put("link_text", new AttributeStyle(MaterialDesignL.LINK, CLR_MOBILE_LINK));
        ATTRIBUTE_STYLES.put("class", new AttributeStyle(MaterialDesignC.CODE_BRACES, CLR_MOBILE_CLASS));
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, 
            boolean isSelected, boolean hasFocus, int row, int column) {
        
        JLabel label = (JLabel) super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);
        
        String attrName = value != null ? value.toString() : "";
        AttributeStyle style = ATTRIBUTE_STYLES.getOrDefault(attrName, null);
        
        if (style != null) {
            // Create icon with the attribute's color
            FontIcon icon = FontIcon.of(style.icon, ICON_SIZE, style.color);
            label.setIcon(icon);
            label.setIconTextGap(6);
            
            // Set text color for non-selected rows
            if (!isSelected) {
                label.setForeground(style.color);
            }
        } else {
            // Default styling for custom/unknown attributes
            FontIcon icon = FontIcon.of(MaterialDesignC.CIRCLE_SMALL, ICON_SIZE, CLR_DEFAULT);
            label.setIcon(icon);
            label.setIconTextGap(6);
            if (!isSelected) {
                label.setForeground(UIManager.getColor("Table.foreground"));
            }
        }
        
        // Add left padding
        label.setBorder(new EmptyBorder(2, 8, 2, 4));
        
        return label;
    }
    
    /**
     * Helper class to hold attribute styling information
     */
    private static class AttributeStyle {
        final org.kordamp.ikonli.Ikon icon;
        final Color color;
        
        AttributeStyle(org.kordamp.ikonli.Ikon icon, Color color) {
            this.icon = icon;
            this.color = color;
        }
    }
}

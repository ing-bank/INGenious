package com.ing.ide.main.utils.table;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

/**
 * Utility class for styling fixed/frozen columns in test data tables.
 * The first N columns (typically 4: Scenario, Flow, Iteration, SubIteration) 
 * are styled differently to distinguish them from data columns.
 */
public class FixedColumnTable {
    
    // Fixed column styling - Light theme
    private static final Color FIXED_COLUMN_BG_LIGHT = new Color(248, 245, 255);
    private static final Color FIXED_COLUMN_SELECTED_BG_LIGHT = new Color(200, 170, 255);
    private static final Color FIXED_COLUMN_HEADER_BG_LIGHT = new Color(235, 225, 255);
    private static final Color FIXED_COLUMN_BORDER_LIGHT = new Color(200, 180, 230);
    private static final Color ING_PURPLE = new Color(119, 36, 255);
    private static final Color ING_BURGUNDY = Color.decode("#4D0020");
    
    // Fixed column styling - Dark theme
    private static final Color FIXED_COLUMN_BG_DARK = new Color(45, 40, 55);
    private static final Color FIXED_COLUMN_SELECTED_BG_DARK = new Color(80, 60, 110);
    private static final Color FIXED_COLUMN_HEADER_BG_DARK = new Color(55, 50, 65);
    private static final Color FIXED_COLUMN_BORDER_DARK = new Color(70, 60, 90);
    private static final Color FIXED_COLUMN_FG_DARK = new Color(200, 195, 210);
    private static final Color ING_ORANGE = new Color(255, 102, 0);
    
    // Theme-aware color getters
    private static boolean isDarkMode() {
        return com.ing.ide.main.Main.isDarkMode();
    }
    
    private static Color getFixedColumnBg() {
        return isDarkMode() ? FIXED_COLUMN_BG_DARK : FIXED_COLUMN_BG_LIGHT;
    }
    
    private static Color getFixedColumnSelectedBg() {
        return isDarkMode() ? FIXED_COLUMN_SELECTED_BG_DARK : FIXED_COLUMN_SELECTED_BG_LIGHT;
    }
    
    private static Color getFixedColumnHeaderBg() {
        return isDarkMode() ? FIXED_COLUMN_HEADER_BG_DARK : FIXED_COLUMN_HEADER_BG_LIGHT;
    }
    
    private static Color getFixedColumnBorder() {
        return isDarkMode() ? FIXED_COLUMN_BORDER_DARK : FIXED_COLUMN_BORDER_LIGHT;
    }
    
    private static Color getFixedColumnFg() {
        return isDarkMode() ? FIXED_COLUMN_FG_DARK : ING_BURGUNDY;
    }
    
    private static Color getFixedColumnHeaderFg() {
        return isDarkMode() ? ING_ORANGE : ING_PURPLE;
    }
    
    /**
     * Apply fixed column styling to the first N columns of a table.
     * @param table The table to style
     * @param fixedColumnCount Number of fixed columns (typically 4 for test data)
     */
    public static void applyFixedColumnStyling(JTable table, int fixedColumnCount) {
        // Cell renderer for fixed columns
        DefaultTableCellRenderer fixedCellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                if (!isSelected) {
                    setBackground(getFixedColumnBg());
                    setForeground(getFixedColumnFg());
                } else {
                    setBackground(getFixedColumnSelectedBg());
                    setForeground(Color.WHITE);
                }
                
                // Add right border on last fixed column
                if (column == fixedColumnCount - 1) {
                    setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 0, 2, getFixedColumnBorder()),
                        BorderFactory.createEmptyBorder(2, 6, 2, 6)
                    ));
                } else {
                    setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
                }
                
                return this;
            }
        };
        
        // Header renderer for fixed columns
        DefaultTableCellRenderer fixedHeaderRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                setBackground(getFixedColumnHeaderBg());
                setForeground(getFixedColumnHeaderFg());
                setFont(getFont().deriveFont(Font.BOLD));
                
                // Add right border on last fixed column header
                if (column == fixedColumnCount - 1) {
                    setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 2, getFixedColumnBorder()),
                        BorderFactory.createEmptyBorder(4, 8, 4, 4)
                    ));
                } else {
                    setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, getFixedColumnBorder()),
                        BorderFactory.createEmptyBorder(4, 8, 4, 4)
                    ));
                }
                
                return this;
            }
        };
        
        // Apply renderers to fixed columns
        TableColumnModel cm = table.getColumnModel();
        for (int i = 0; i < Math.min(fixedColumnCount, cm.getColumnCount()); i++) {
            TableColumn col = cm.getColumn(i);
            col.setCellRenderer(fixedCellRenderer);
            col.setHeaderRenderer(fixedHeaderRenderer);
        }
    }
    
    private FixedColumnTable() {
        // Utility class - no instantiation
    }
}

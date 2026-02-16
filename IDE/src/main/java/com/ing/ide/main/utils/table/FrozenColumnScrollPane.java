package com.ing.ide.main.utils.table;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

/**
 * Creates a JScrollPane with frozen (fixed) columns on the left side.
 * The fixed columns do not scroll horizontally with the rest of the table.
 */
public class FrozenColumnScrollPane extends JScrollPane {
    
    /**
     * Functional interface for providing custom cell editors.
     */
    @FunctionalInterface
    public interface CellEditorProvider {
        TableCellEditor getCellEditor(int row, int column, TableCellEditor defaultEditor);
    }
    
    private final JTable mainTable;
    private JTable fixedTable;
    private final int fixedColumnCount;
    private TableModelListener modelListener;
    private CellEditorProvider cellEditorProvider;
    
    // Fixed column styling - Light theme colors
    private static final Color FIXED_COLUMN_BG_LIGHT = new Color(248, 245, 255);
    private static final Color FIXED_COLUMN_SELECTED_BG_LIGHT = new Color(200, 170, 255);
    private static final Color FIXED_COLUMN_HEADER_BG_LIGHT = new Color(235, 225, 255);
    private static final Color FIXED_COLUMN_BORDER_LIGHT = new Color(200, 180, 230);
    private static final Color ING_PURPLE = new Color(119, 36, 255);
    private static final Color ING_BURGUNDY = Color.decode("#4D0020");
    
    // Fixed column styling - Dark theme colors
    private static final Color FIXED_COLUMN_BG_DARK = new Color(45, 40, 55);       // Dark purple-grey
    private static final Color FIXED_COLUMN_SELECTED_BG_DARK = new Color(80, 60, 110); // Brighter purple
    private static final Color FIXED_COLUMN_HEADER_BG_DARK = new Color(55, 50, 65);  // Slightly lighter header
    private static final Color FIXED_COLUMN_BORDER_DARK = new Color(70, 60, 90);     // Subtle purple border
    private static final Color FIXED_COLUMN_FG_DARK = new Color(200, 195, 210);      // Light grey-purple text
    private static final Color ING_ORANGE_DARK = new Color(255, 102, 0);             // Orange accent for dark theme
    
    // Theme-aware color getters
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
        return isDarkMode() ? ING_ORANGE_DARK : ING_PURPLE;
    }
    
    private static boolean isDarkMode() {
        return com.ing.ide.main.Main.isDarkMode();
    }
    
    private static Color getScrollPaneBackground() {
        if (isDarkMode()) {
            return FIXED_COLUMN_BG_DARK;  // Use fixed column bg for consistency
        }
        Color panelBg = UIManager.getColor("Panel.background");
        return panelBg != null ? panelBg : Color.WHITE;
    }
    
    /**
     * Create a frozen column scroll pane.
     * @param table The table to display
     * @param fixedColumnCount Number of columns to freeze on the left
     */
    public FrozenColumnScrollPane(JTable table, int fixedColumnCount) {
        super(table);
        this.mainTable = table;
        this.fixedColumnCount = fixedColumnCount;
        
        // CRITICAL: Prevent automatic column model recreation when model structure changes
        mainTable.setAutoCreateColumnsFromModel(false);
        
        // Create the fixed column table
        this.fixedTable = createFixedTable();
        
        // Set up the row header with fixed columns
        setupRowHeader();
        
        // Apply styling
        applyFixedColumnStyling();
        
        // Remove fixed columns from main table view
        removeFixedColumnsFromMainTable();
        
        // Setup model listener for handling structure changes
        setupModelListener();
        
        // Synchronize the tables
        setupSynchronization();
        
        // Style the scroll pane with theme-aware colors
        Color bgColor = getScrollPaneBackground();
        setBackground(bgColor);
        getViewport().setBackground(bgColor);
        setBorder(BorderFactory.createEmptyBorder());
        
        // Set fixed table background for empty areas
        fixedTable.setBackground(getFixedColumnBg());
        mainTable.setBackground(bgColor);
        
        // Set all corners to eliminate white gaps
        setCornerPanels(bgColor);
    }
    
    private void setCornerPanels(Color bgColor) {
        // Create themed corner panels for any empty scroll pane corners
        JPanel lowerLeftCorner = new JPanel() {
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                g.setColor(getFixedColumnBg());
                g.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
        };
        lowerLeftCorner.setBackground(getFixedColumnBg());
        lowerLeftCorner.setOpaque(true);
        setCorner(ScrollPaneConstants.LOWER_LEFT_CORNER, lowerLeftCorner);
        
        JPanel lowerRightCorner = new JPanel();
        lowerRightCorner.setBackground(bgColor);
        lowerRightCorner.setOpaque(true);
        setCorner(ScrollPaneConstants.LOWER_RIGHT_CORNER, lowerRightCorner);
        
        JPanel upperRightCorner = new JPanel();
        upperRightCorner.setBackground(bgColor);
        upperRightCorner.setOpaque(true);
        setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, upperRightCorner);
    }
    
    private JTable createFixedTable() {
        // Create a table sharing the same model but different column model
        // Using 'this' reference to access cellEditorProvider from inner class
        final FrozenColumnScrollPane scrollPane = this;
        JTable fixed = new JTable(mainTable.getModel()) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // Delegate to the model - fixed columns should maintain their original editability
                return mainTable.getModel().isCellEditable(row, column);
            }
            
            @Override
            public void setValueAt(Object aValue, int row, int column) {
                mainTable.getModel().setValueAt(aValue, row, column);
            }
            
            @Override
            public TableCellEditor getCellEditor(int row, int column) {
                // Use custom cell editor provider if available
                if (scrollPane.cellEditorProvider != null) {
                    return scrollPane.cellEditorProvider.getCellEditor(row, column, super.getCellEditor(row, column));
                }
                return super.getCellEditor(row, column);
            }
            
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                // Fill the entire component with the background color first
                // This ensures empty areas below rows have the correct color
                g.setColor(getFixedColumnBg());
                g.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
            
            @Override
            public Color getBackground() {
                return getFixedColumnBg();
            }
        };
        
        // CRITICAL: Prevent automatic column model recreation
        fixed.setAutoCreateColumnsFromModel(false);
        
        // Copy properties from main table
        fixed.setSelectionModel(mainTable.getSelectionModel());
        fixed.setRowHeight(mainTable.getRowHeight());
        fixed.setFont(mainTable.getFont());
        fixed.setIntercellSpacing(new Dimension(0, 0));  // Remove intercell spacing to avoid white lines
        fixed.setShowGrid(false);  // Disable grid lines to avoid white lines
        fixed.setFillsViewportHeight(true);
        fixed.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        fixed.getTableHeader().setReorderingAllowed(false);
        fixed.getTableHeader().setFont(mainTable.getTableHeader().getFont());
        
        // Apply theme-aware backgrounds
        fixed.setBackground(getFixedColumnBg());
        fixed.getTableHeader().setBackground(getFixedColumnHeaderBg());
        fixed.setOpaque(true);
        
        // Remove columns beyond the fixed count
        TableColumnModel cm = fixed.getColumnModel();
        while (cm.getColumnCount() > fixedColumnCount) {
            cm.removeColumn(cm.getColumn(cm.getColumnCount() - 1));
        }
        
        // Copy column widths from main table
        for (int i = 0; i < Math.min(fixedColumnCount, mainTable.getColumnCount()); i++) {
            int width = mainTable.getColumnModel().getColumn(i).getPreferredWidth();
            fixed.getColumnModel().getColumn(i).setPreferredWidth(width);
        }
        
        return fixed;
    }
    
    private void setupRowHeader() {
        // Create a viewport for the fixed table that properly paints its background
        JViewport viewport = new JViewport() {
            @Override
            public Dimension getPreferredSize() {
                Dimension d = fixedTable.getPreferredSize();
                d.width = calculateFixedColumnsWidth();
                return d;
            }
            
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                // Fill the entire viewport with background color first
                g.setColor(getFixedColumnBg());
                g.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
            
            @Override
            public Color getBackground() {
                return getFixedColumnBg();
            }
        };
        viewport.setView(fixedTable);
        viewport.setBackground(getFixedColumnBg());  // Theme-aware background
        viewport.setOpaque(true);
        
        setRowHeaderView(viewport);
        
        // Set corner component with theme-aware header background
        JTableHeader cornerHeader = fixedTable.getTableHeader();
        cornerHeader.setBackground(getFixedColumnHeaderBg());
        setCorner(ScrollPaneConstants.UPPER_LEFT_CORNER, cornerHeader);
    }
    
    private void removeFixedColumnsFromMainTable() {
        TableColumnModel cm = mainTable.getColumnModel();
        // Store the columns to remove (can't modify while iterating)
        for (int i = 0; i < fixedColumnCount && cm.getColumnCount() > 0; i++) {
            cm.removeColumn(cm.getColumn(0));
        }
    }
    
    private int calculateFixedColumnsWidth() {
        int width = 0;
        TableColumnModel cm = fixedTable.getColumnModel();
        for (int i = 0; i < cm.getColumnCount(); i++) {
            width += cm.getColumn(i).getPreferredWidth();
        }
        return width + 3; // +3 for border
    }
    
    private void setupModelListener() {
        // Listen for model structure changes to handle column additions
        modelListener = new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (e.getFirstRow() == TableModelEvent.HEADER_ROW) {
                    // Structure change (column added/removed) - sync column models
                    syncColumnModels();
                }
                // Repaint both tables for any changes
                fixedTable.repaint();
            }
        };
        mainTable.getModel().addTableModelListener(modelListener);
    }
    
    private void syncColumnModels() {
        TableModel model = mainTable.getModel();
        int modelColumnCount = model.getColumnCount();
        TableColumnModel mainCM = mainTable.getColumnModel();
        
        // Calculate expected column count in main table (all columns beyond fixed)
        int expectedMainColumns = Math.max(0, modelColumnCount - fixedColumnCount);
        
        // Add any new columns to main table's column model
        while (mainCM.getColumnCount() < expectedMainColumns) {
            int newColIndex = fixedColumnCount + mainCM.getColumnCount();
            if (newColIndex < modelColumnCount) {
                TableColumn newCol = new TableColumn(newColIndex);
                newCol.setHeaderValue(model.getColumnName(newColIndex));
                mainCM.addColumn(newCol);
            }
        }
        
        revalidate();
        repaint();
    }
    
    private void setupSynchronization() {
        // Sync vertical scrolling
        getViewport().addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                Point p = getViewport().getViewPosition();
                getRowHeader().setViewPosition(new Point(0, p.y));
            }
        });
        
        // Sync selection repaints
        mainTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                fixedTable.repaint();
            }
        });
    }
    
    private void applyFixedColumnStyling() {
        // Cell renderer for fixed columns (theme-aware)
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
                if (column == fixedTable.getColumnCount() - 1) {
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
        
        // Header renderer for fixed columns (theme-aware)
        DefaultTableCellRenderer fixedHeaderRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                setBackground(getFixedColumnHeaderBg());
                setForeground(getFixedColumnHeaderFg());
                setFont(getFont().deriveFont(Font.BOLD));
                
                // Add right border on last fixed column header
                if (column == fixedTable.getColumnCount() - 1) {
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
        
        // Apply to fixed table columns
        TableColumnModel cm = fixedTable.getColumnModel();
        for (int i = 0; i < cm.getColumnCount(); i++) {
            TableColumn col = cm.getColumn(i);
            col.setCellRenderer(fixedCellRenderer);
            col.setHeaderRenderer(fixedHeaderRenderer);
        }
    }
    
    /**
     * Update the tables when the main table's model is completely replaced.
     * Call this after setting a new model on the main table.
     */
    public void updateModel() {
        TableModel model = mainTable.getModel();
        
        // Ensure main table doesn't auto-create columns
        mainTable.setAutoCreateColumnsFromModel(false);
        
        // Rebuild main table's column model (columns beyond fixed count)
        TableColumnModel mainCM = mainTable.getColumnModel();
        while (mainCM.getColumnCount() > 0) {
            mainCM.removeColumn(mainCM.getColumn(0));
        }
        for (int i = fixedColumnCount; i < model.getColumnCount(); i++) {
            TableColumn col = new TableColumn(i);
            col.setHeaderValue(model.getColumnName(i));
            mainCM.addColumn(col);
        }
        
        // Update fixed table's model and column model
        fixedTable.setModel(model);
        fixedTable.setAutoCreateColumnsFromModel(false);
        
        TableColumnModel fixedCM = fixedTable.getColumnModel();
        while (fixedCM.getColumnCount() > 0) {
            fixedCM.removeColumn(fixedCM.getColumn(0));
        }
        for (int i = 0; i < Math.min(fixedColumnCount, model.getColumnCount()); i++) {
            TableColumn col = new TableColumn(i);
            col.setHeaderValue(model.getColumnName(i));
            fixedCM.addColumn(col);
        }
        
        // Re-apply styling to fixed table
        applyFixedColumnStyling();
        
        // Re-sync selection model
        fixedTable.setSelectionModel(mainTable.getSelectionModel());
        
        // Add model listener to new model
        model.addTableModelListener(modelListener);
        
        // Update row header view
        revalidate();
        repaint();
    }
    
    /**
     * Get the fixed columns table.
     */
    public JTable getFixedTable() {
        return fixedTable;
    }
    
    /**
     * Get the main (scrollable) table.
     */
    public JTable getMainTable() {
        return mainTable;
    }
    
    /**
     * Set a custom cell editor provider for the fixed columns table.
     * This allows custom cell editors (like auto-suggest dropdowns) to be used.
     * @param provider The cell editor provider
     */
    public void setCellEditorProvider(CellEditorProvider provider) {
        this.cellEditorProvider = provider;
    }
}

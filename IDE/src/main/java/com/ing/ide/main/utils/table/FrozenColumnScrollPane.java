package com.ing.ide.main.utils.table;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;
import java.util.function.IntConsumer;
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

    private InsertRowPromptFeature fixedInsertRowPromptFeature;

    // Table that owns the column selection anchor for the current selection gesture.
    private JTable columnAnchorTable;

    // Theme-aware color getters
    private static Color getFixedColumnBg() {
        return isDarkMode() ? TableColor.FIXED_COLUMN_BG_DARK : TableColor.FIXED_COLUMN_BG_LIGHT;
    }

    private static Color getFixedColumnSelectedBg() {
        return isDarkMode()
            ? TableColor.FIXED_COLUMN_SELECTED_BG_DARK
            : TableColor.FIXED_COLUMN_SELECTED_BG_LIGHT;
    }

    private static Color getFixedColumnHeaderBg() {
        return isDarkMode()
            ? TableColor.FIXED_COLUMN_HEADER_BG_DARK
            : TableColor.FIXED_COLUMN_HEADER_BG_LIGHT;
    }

    private static Color getFixedColumnBorder() {
        return isDarkMode()
            ? TableColor.FIXED_COLUMN_BORDER_DARK
            : TableColor.FIXED_COLUMN_BORDER_LIGHT;
    }

    private static Color getFixedColumnFg() {
        return isDarkMode() ? TableColor.FIXED_COLUMN_FG_DARK : TableColor.ING_BURGUNDY;
    }

    private static Color getFixedColumnHeaderFg() {
        return isDarkMode() ? TableColor.ING_ORANGE_DARK : TableColor.ING_PURPLE;
    }

    private static boolean isDarkMode() {
        return com.ing.ide.main.Main.isDarkMode();
    }

    private static Color getScrollPaneBackground() {
        if (isDarkMode()) {
            return TableColor.FIXED_COLUMN_BG_DARK; // Use fixed column bg for consistency
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

        // Temporarily disabled as the action is misfiring
        // causing selection in the editable data columns
        // to be unselected when context menu is displayed.
        //
        // FocusListener focusListener = new FocusAdapter(){
        //     @Override
        //     public void focusGained(FocusEvent e) {}

        //     @Override
        //     public void focusLost(FocusEvent e) {
        //         JTable table = (JTable) e.getSource();
        //         table.clearSelection();
        //     }
        // };

        // fixedTable.addFocusListener(focusListener);
        // mainTable.addFocusListener(focusListener);

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
                    return scrollPane.cellEditorProvider.getCellEditor(
                        row,
                        column,
                        super.getCellEditor(row, column)
                    );
                }
                return super.getCellEditor(row, column);
            }

            @Override
            protected void paintComponent(Graphics g) {
                // Fill the entire component with the background color first
                // This ensures empty areas below rows have the correct color
                g.setColor(getFixedColumnBg());
                g.fillRect(0, 0, getWidth(), getHeight());

                super.paintComponent(g);

                if (fixedInsertRowPromptFeature != null) {
                    fixedInsertRowPromptFeature.paint(g);
                }
            }

            @Override
            protected void processMouseEvent(MouseEvent e) {
                if (
                    fixedInsertRowPromptFeature != null &&
                    fixedInsertRowPromptFeature.processMouseEvent(e)
                ) {
                    return;
                }

                super.processMouseEvent(e);
            }

            @Override
            protected void processMouseMotionEvent(MouseEvent e) {
                if (
                    fixedInsertRowPromptFeature != null &&
                    fixedInsertRowPromptFeature.processMouseMotionEvent(e)
                ) {
                    return;
                }

                super.processMouseMotionEvent(e);
            }

            @Override
            public Color getBackground() {
                return getFixedColumnBg();
            }

            @Override
            public void tableChanged(TableModelEvent e) {
                if (e == null || e.getFirstRow() == TableModelEvent.HEADER_ROW) {
                    super.tableChanged(e);
                    return;
                }
                if (
                    e.getType() == TableModelEvent.INSERT || e.getType() == TableModelEvent.DELETE
                ) {
                    // Since fixedTable shares selectionModel with mainTable,
                    // mainTable already handles selectionModel adjustment.
                    // Avoid duplicate selectionModel index shifting on the shared model.
                    repaint();
                    return;
                }
                super.tableChanged(e);
            }
        };

        // CRITICAL: Prevent automatic column model recreation
        fixed.setAutoCreateColumnsFromModel(false);

        // Copy properties from main table
        fixed.setSelectionModel(mainTable.getSelectionModel());
        fixed.setCellSelectionEnabled(true);
        fixed.setRowSelectionAllowed(true);
        fixed.setColumnSelectionAllowed(true);
        fixed.setRowHeight(mainTable.getRowHeight());
        fixed.setFont(mainTable.getFont());
        fixed.setIntercellSpacing(new Dimension(0, 0)); // Remove intercell spacing to avoid white lines
        fixed.setShowGrid(false); // Disable grid lines to avoid white lines
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

        fixedInsertRowPromptFeature = new InsertRowPromptFeature(fixed);
        fixedInsertRowPromptFeature.install();
        fixedInsertRowPromptFeature.setEnabled(false);

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
        viewport.setBackground(getFixedColumnBg()); // Theme-aware background
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
        modelListener =
            new TableModelListener() {

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

        // Keep main table column model perfectly aligned with model indexes/headers.
        int expectedMainColumns = Math.max(0, modelColumnCount - fixedColumnCount);
        boolean rebuildMain = mainCM.getColumnCount() != expectedMainColumns;
        if (!rebuildMain) {
            for (int i = 0; i < mainCM.getColumnCount(); i++) {
                int expectedModelIndex = fixedColumnCount + i;
                TableColumn column = mainCM.getColumn(i);
                if (
                    column.getModelIndex() != expectedModelIndex ||
                    !Objects.equals(
                        column.getHeaderValue(),
                        model.getColumnName(expectedModelIndex)
                    )
                ) {
                    rebuildMain = true;
                    break;
                }
            }
        }
        if (rebuildMain) {
            while (mainCM.getColumnCount() > 0) {
                mainCM.removeColumn(mainCM.getColumn(0));
            }
            for (int i = fixedColumnCount; i < modelColumnCount; i++) {
                TableColumn col = new TableColumn(i);
                col.setHeaderValue(model.getColumnName(i));
                mainCM.addColumn(col);
            }
        }

        // Also keep fixed table headers aligned if fixed columns are renamed/updated.
        TableColumnModel fixedCM = fixedTable.getColumnModel();
        int expectedFixedColumns = Math.min(fixedColumnCount, modelColumnCount);
        boolean rebuildFixed = fixedCM.getColumnCount() != expectedFixedColumns;
        if (!rebuildFixed) {
            for (int i = 0; i < fixedCM.getColumnCount(); i++) {
                TableColumn column = fixedCM.getColumn(i);
                if (
                    column.getModelIndex() != i ||
                    !Objects.equals(column.getHeaderValue(), model.getColumnName(i))
                ) {
                    rebuildFixed = true;
                    break;
                }
            }
        }
        if (rebuildFixed) {
            while (fixedCM.getColumnCount() > 0) {
                fixedCM.removeColumn(fixedCM.getColumn(0));
            }
            for (int i = 0; i < expectedFixedColumns; i++) {
                TableColumn col = new TableColumn(i);
                col.setHeaderValue(model.getColumnName(i));
                fixedCM.addColumn(col);
            }
            applyFixedColumnStyling();
        }

        revalidate();
        repaint();
    }

    private void setupSynchronization() {
        // Sync vertical scrolling
        getViewport()
            .addChangeListener(
                new ChangeListener() {

                    @Override
                    public void stateChanged(ChangeEvent e) {
                        Point p = getViewport().getViewPosition();
                        getRowHeader().setViewPosition(new Point(0, p.y));
                    }
                }
            );

        // Sync selection repaints
        mainTable
            .getSelectionModel()
            .addListSelectionListener(
                e -> {
                    if (!e.getValueIsAdjusting()) {
                        fixedTable.repaint();
                        mainTable.repaint();
                    }
                }
            );

        mainTable
            .getColumnModel()
            .getSelectionModel()
            .addListSelectionListener(
                e -> {
                    if (!e.getValueIsAdjusting()) {
                        fixedTable.repaint();
                        mainTable.repaint();
                    }
                }
            );

        fixedTable
            .getColumnModel()
            .getSelectionModel()
            .addListSelectionListener(
                e -> {
                    if (!e.getValueIsAdjusting()) {
                        fixedTable.repaint();
                        mainTable.repaint();
                    }
                }
            );

        mainTable.addMouseListener(
            new MouseAdapter() {

                @Override
                public void mousePressed(MouseEvent e) {
                    onTableMousePressed(mainTable, fixedTable, e);
                }
            }
        );

        fixedTable.addMouseListener(
            new MouseAdapter() {

                @Override
                public void mousePressed(MouseEvent e) {
                    onTableMousePressed(fixedTable, mainTable, e);
                }
            }
        );

        fixedTable.addFocusListener(
            new FocusAdapter() {

                @Override
                public void focusGained(FocusEvent e) {
                    fixedTable.repaint();
                    mainTable.repaint();
                }

                @Override
                public void focusLost(FocusEvent e) {
                    fixedTable.repaint();
                    mainTable.repaint();
                }
            }
        );

        mainTable.addFocusListener(
            new FocusAdapter() {

                @Override
                public void focusGained(FocusEvent e) {
                    fixedTable.repaint();
                    mainTable.repaint();
                }

                @Override
                public void focusLost(FocusEvent e) {
                    fixedTable.repaint();
                    mainTable.repaint();
                }
            }
        );
    }

    /**
     * Indicates whether a click should extend the existing selection instead of replacing it.
     *
     * <p>Modifier clicks let a selection span the fixed and scrollable tables, so the other
     * table's column selection must be preserved.</p>
     *
     * @param e mouse event to inspect
     * @return true when the click extends the current selection
     */
    private boolean isSelectionExtendingClick(MouseEvent e) {
        return e.isShiftDown() || e.isControlDown() || e.isMetaDown();
    }

    /**
     * Keeps column selection coherent when the user clicks across the fixed/scrollable split.
     *
     * <p>A plain click resets the other table and becomes the new anchor. An extending click in
     * the table that does not own the anchor produces a contiguous range across the split,
     * ignoring the clicked table's own stale anchor.</p>
     *
     * @param clicked table that received the click
     * @param other the counterpart table
     * @param e mouse event to inspect
     */
    private void onTableMousePressed(JTable clicked, JTable other, MouseEvent e) {
        if (!isSelectionExtendingClick(e)) {
            columnAnchorTable = clicked;
            if (other.getColumnModel().getSelectionModel().getMinSelectionIndex() >= 0) {
                other.getColumnModel().getSelectionModel().clearSelection();
                other.repaint();
            }
            return;
        }

        if (columnAnchorTable == null || columnAnchorTable == clicked) {
            return;
        }

        int anchorColumn = other.getColumnModel().getSelectionModel().getAnchorSelectionIndex();
        int clickedColumn = clicked.columnAtPoint(e.getPoint());
        if (
            other.getColumnModel().getSelectionModel().getMinSelectionIndex() < 0 ||
            anchorColumn < 0 ||
            anchorColumn >= other.getColumnCount() ||
            clickedColumn < 0
        ) {
            return;
        }

        // The anchor table keeps everything from its anchor up to the split; the clicked table
        // is filled from the split up to the clicked column.
        if (clicked == mainTable) {
            other.setColumnSelectionInterval(anchorColumn, other.getColumnCount() - 1);
            clicked.setColumnSelectionInterval(0, clickedColumn);
        } else {
            clicked.setColumnSelectionInterval(clickedColumn, clicked.getColumnCount() - 1);
            other.setColumnSelectionInterval(0, anchorColumn);
        }

        clicked.repaint();
        other.repaint();
    }

    private boolean isRowSelected(JTable table, int row) {
        if (table == null) {
            return false;
        }
        for (int selectedRow : table.getSelectedRows()) {
            if (selectedRow == row) {
                return true;
            }
        }
        return false;
    }

    private boolean isPassiveRowHighlightFor(JTable table, int row) {
        JTable activeTable = mainTable.hasFocus()
            ? mainTable
            : fixedTable.hasFocus() ? fixedTable : null;
        if (activeTable == null || activeTable == table) {
            return false;
        }
        return isRowSelected(activeTable, row);
    }

    private void applyFixedColumnStyling() {
        // Cell renderer for fixed columns (theme-aware)
        DefaultTableCellRenderer fixedCellRenderer = new DefaultTableCellRenderer() {

            @Override
            public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
            ) {
                super.getTableCellRendererComponent(
                    table,
                    value,
                    isSelected,
                    hasFocus,
                    row,
                    column
                );

                // Only the currently focused table should act as the selected row source.
                // The other table mirrors that row as a passive highlight without owning the actual selection.
                boolean passiveRowHighlight = !isSelected && isPassiveRowHighlightFor(table, row);

                if (isSelected && (table.hasFocus() || hasFocus)) {
                    setBackground(getFixedColumnSelectedBg());
                    setForeground(Color.WHITE);
                } else if (isSelected) {
                    setBackground(
                        isDarkMode()
                            ? TableColor.FIXED_COLUMN_SELECTED_BG_DARK
                            : TableColor.FIXED_COLUMN_SELECTED_BG_LIGHT
                    );
                    setForeground(getFixedColumnFg());
                } else if (passiveRowHighlight) {
                    Color rowSelBg = UIManager.getColor("ing.selectionBackground");
                    setBackground(
                        rowSelBg != null
                            ? rowSelBg
                            : (
                                isDarkMode()
                                    ? TableColor.FIXED_COLUMN_SELECTED_BG_DARK
                                    : TableColor.FIXED_COLUMN_SELECTED_BG_LIGHT
                            )
                    );
                    setForeground(getFixedColumnFg());
                } else {
                    setBackground(getFixedColumnBg());
                    setForeground(getFixedColumnFg());
                }

                // Add right border on last fixed column
                if (column == fixedTable.getColumnCount() - 1) {
                    setBorder(
                        BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 0, 0, 2, getFixedColumnBorder()),
                            BorderFactory.createEmptyBorder(2, 6, 2, 6)
                        )
                    );
                } else {
                    setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
                }

                return this;
            }
        };

        // Header renderer for fixed columns (theme-aware)
        DefaultTableCellRenderer fixedHeaderRenderer = new DefaultTableCellRenderer() {

            @Override
            public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
            ) {
                super.getTableCellRendererComponent(
                    table,
                    value,
                    isSelected,
                    hasFocus,
                    row,
                    column
                );

                setBackground(getFixedColumnHeaderBg());
                setForeground(getFixedColumnHeaderFg());
                setFont(getFont().deriveFont(Font.BOLD));

                // Add right border on last fixed column header
                if (column == fixedTable.getColumnCount() - 1) {
                    setBorder(
                        BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 0, 1, 2, getFixedColumnBorder()),
                            BorderFactory.createEmptyBorder(4, 8, 4, 4)
                        )
                    );
                } else {
                    setBorder(
                        BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 0, 1, 0, getFixedColumnBorder()),
                            BorderFactory.createEmptyBorder(4, 8, 4, 4)
                        )
                    );
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
        fixedTable.removeRowSelectionInterval(0, fixedTable.getRowCount() - 1);

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

    /**
     * Enables or disables the insert-row prompt on the fixed columns table.
     *
     * @param enabled true to enable the fixed insert-row prompt
     */
    public void setFixedInsertRowPromptEnabled(boolean enabled) {
        if (fixedInsertRowPromptFeature != null) {
            fixedInsertRowPromptFeature.setEnabled(enabled);
        }
    }

    /**
     * Sets the insert-row handler for the fixed columns table.
     *
     * @param insertRowHandler callback receiving the insertion row index
     */
    public void setFixedInsertRowHandler(IntConsumer insertRowHandler) {
        if (fixedInsertRowPromptFeature != null) {
            fixedInsertRowPromptFeature.setInsertRowHandler(insertRowHandler);
        }
    }
}

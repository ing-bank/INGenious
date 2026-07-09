package com.ing.ide.main.utils.table;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.IntConsumer;
import javax.swing.Action;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

/**
 * Provides a visual insert-column prompt for a table body, allowing users
 * to insert columns at specific positions via a hoverable plus icon displayed
 * between cells in a hovered row.
 */
public class InsertColumnPromptFeature {
    private static final Color INSERT_INDICATOR_COLOR = Color.decode("#7724FF");

    private static final float INSERT_LINE_OPACITY = 0.45f;
    private static final float INSERT_PLUS_OPACITY = 0.90f;

    private static final int INSERT_PLUS_SIZE = 18;
    private static final int INSERT_PLUS_HIT_PADDING = 2;
    private static final int INSERT_HOVER_ZONE = INSERT_PLUS_SIZE / 2 + INSERT_PLUS_HIT_PADDING;

    private static final int INSERT_MOUSE_SUPPRESSION_MS = 140;

    private final JTable table;

    private int hoverInsertColumn = -1;
    private int hoverRow = -1;
    private boolean insertingColumn = false;
    private boolean enabled = false;
    private boolean installed = false;

    private long suppressMouseEventsUntil = 0L;

    private IntConsumer insertColumnHandler;

    private MouseAdapter insertColumnMouseAdapter;

    private int minimumInsertColumn = 0;

    /**
     * Creates a new insert-column prompt feature for the specified table.
     *
     * @param table target table
     */
    public InsertColumnPromptFeature(JTable table) {
        this.table = table;
    }

    /**
     * Installs the mouse listeners required to support column insertion prompts.
     * Subsequent calls have no effect.
     */
    public void install() {
        if (installed) {
            return;
        }

        installed = true;

        insertColumnMouseAdapter =
            new MouseAdapter() {

                @Override
                public void mouseMoved(MouseEvent e) {
                    if (!enabled) {
                        clearHoverState();
                        return;
                    }

                    if (isSuppressingMouseEvents()) {
                        e.consume();
                        return;
                    }

                    int newHoverRow = table.rowAtPoint(e.getPoint());
                    int newHoverInsertColumn = getInsertColumnForPoint(e.getPoint());

                    if (newHoverInsertColumn == -1) {
                        newHoverRow = -1;
                    }

                    if (newHoverInsertColumn != hoverInsertColumn || newHoverRow != hoverRow) {
                        hoverInsertColumn = newHoverInsertColumn;
                        hoverRow = newHoverRow;
                        table.repaint();
                    }

                    if (hoverInsertColumn != -1 && isPointOnPlus(e.getPoint())) {
                        table.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    } else {
                        table.setCursor(Cursor.getDefaultCursor());
                    }
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    if (!enabled) {
                        return;
                    }

                    if (isSuppressingMouseEvents()) {
                        e.consume();
                        return;
                    }

                    if (
                        !insertingColumn && hoverInsertColumn != -1 && isPointOnPlus(e.getPoint())
                    ) {
                        insertColumnAtHoverPosition();
                        e.consume();
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    clearHoverState();
                }
            };

        table.addMouseMotionListener(insertColumnMouseAdapter);
        table.addMouseListener(insertColumnMouseAdapter);
    }

    /**
     * Removes installed listeners from the table.
     */
    public void uninstall() {
        if (!installed) {
            return;
        }

        if (insertColumnMouseAdapter != null) {
            table.removeMouseMotionListener(insertColumnMouseAdapter);
            table.removeMouseListener(insertColumnMouseAdapter);
        }

        table.setCursor(Cursor.getDefaultCursor());
        table.repaint();

        insertColumnMouseAdapter = null;
        installed = false;
        hoverInsertColumn = -1;
        hoverRow = -1;
    }

    /**
     * Enables or disables the feature.
     *
     * @param enabled {@code true} to enable column insertion prompts
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;

        install();

        if (!enabled) {
            clearHoverState();
        }

        table.repaint();
    }

    /**
     * Returns whether the feature is currently enabled.
     *
     * @return {@code true} if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets a custom column insertion handler.
     *
     * @param insertColumnHandler callback that receives the insertion index
     */
    public void setInsertColumnHandler(IntConsumer insertColumnHandler) {
        this.insertColumnHandler = insertColumnHandler;
    }

    /**
     * Sets the minimum visible/view insertion column boundary.
     *
     * For example, GlobalData can set this to 1 so the insert prompt does not
     * appear before column 0, keeping GlobalDataID fixed as the first column.
     *
     * @param minimumInsertColumn minimum allowed insertion boundary
     */
    public void setMinimumInsertColumn(int minimumInsertColumn) {
        this.minimumInsertColumn = Math.max(0, minimumInsertColumn);

        if (hoverInsertColumn != -1 && !isInsertColumnAllowed(hoverInsertColumn)) {
            clearHoverState();
        }

        table.repaint();
    }

    /**
     * Paints the insert-column indicator when a valid insertion location is hovered.
     *
     * This should be called by the owning table after normal table painting.
     *
     * @param g graphics context
     */
    public void paint(Graphics g) {
        if (enabled && hoverInsertColumn != -1 && hoverRow != -1) {
            paintInsertColumnIndicator(g);
        }
    }

    private boolean isInsertColumnAllowed(int insertColumn) {
        return insertColumn >= minimumInsertColumn && insertColumn <= table.getColumnCount();
    }

    private boolean isSuppressingMouseEvents() {
        return System.currentTimeMillis() < suppressMouseEventsUntil;
    }

    /**
     * Temporarily suppresses mouse events to avoid unintended interactions
     * immediately after a column insertion.
     */
    private void suppressMouseEventsBriefly() {
        suppressMouseEventsUntil = System.currentTimeMillis() + INSERT_MOUSE_SUPPRESSION_MS;
    }

    /**
     * Resolves the column insertion index for the specified point.
     *
     * @param point mouse location within the table body
     * @return insertion column index, or {@code -1} if none applies
     */
    private int getInsertColumnForPoint(Point point) {
        int columnCount = table.getColumnCount();

        if (columnCount <= 0 || table.getRowCount() <= 0) {
            return -1;
        }

        int row = table.rowAtPoint(point);

        if (row == -1) {
            return -1;
        }

        int column = table.columnAtPoint(point);

        if (column == -1) {
            return getInsertColumnForPointOutsideColumns(point, row);
        }

        Rectangle cellBounds = table.getCellRect(row, column, true);

        int cellLeft = cellBounds.x;
        int cellRight = cellBounds.x + cellBounds.width;

        /*
         * If a custom handler exists, insertion before the first visible column
         * is allowed. This is needed for tables whose visible columns are mapped
         * to different model columns, such as test data tables with frozen columns.
         *
         * Without a handler, fallback "Add Column" can only insert after a selected
         * column, so before-first-column insertion is not reliable.
         */
        if (
            Math.abs(point.x - cellLeft) <= INSERT_HOVER_ZONE &&
            (column > 0 || insertColumnHandler != null)
        ) {
            int candidateInsertColumn = column;

            if (isInsertColumnAllowed(candidateInsertColumn)) {
                return candidateInsertColumn;
            }
        }

        if (Math.abs(point.x - cellRight) <= INSERT_HOVER_ZONE) {
            int candidateInsertColumn = column + 1;

            if (isInsertColumnAllowed(candidateInsertColumn)) {
                return candidateInsertColumn;
            }
        }

        return -1;
    }

    /**
     * Determines an insertion index when the pointer is outside the bounds of
     * any visible column but still inside a table row.
     *
     * @param point mouse location
     * @param row row index under the pointer
     * @return insertion column index, or {@code -1} if none applies
     */
    private int getInsertColumnForPointOutsideColumns(Point point, int row) {
        int columnCount = table.getColumnCount();

        if (columnCount <= 0 || row < 0) {
            return -1;
        }

        Rectangle lastCell = table.getCellRect(row, columnCount - 1, true);
        int lastCellRight = lastCell.x + lastCell.width;

        if (Math.abs(point.x - lastCellRight) <= INSERT_HOVER_ZONE) {
            int candidateInsertColumn = columnCount;

            if (isInsertColumnAllowed(candidateInsertColumn)) {
                return candidateInsertColumn;
            }
        }

        return -1;
    }

    /**
     * Returns the horizontal position of the insertion indicator line.
     *
     * @param insertColumn insertion index
     * @return x-coordinate of the insertion line
     */
    private int getInsertLineX(int insertColumn) {
        int columnCount = table.getColumnCount();

        if (columnCount <= 0) {
            return 0;
        }

        int safeRow = getSafeHoverRow();

        if (insertColumn <= 0) {
            Rectangle firstCell = table.getCellRect(safeRow, 0, true);
            return firstCell.x;
        }

        if (insertColumn >= columnCount) {
            Rectangle lastCell = table.getCellRect(safeRow, columnCount - 1, true);
            return lastCell.x + lastCell.width;
        }

        Rectangle targetCell = table.getCellRect(safeRow, insertColumn, true);
        return targetCell.x;
    }

    /**
     * Returns a safe row to use for geometry calculations.
     *
     * @return valid row index
     */
    private int getSafeHoverRow() {
        int rowCount = table.getRowCount();

        if (rowCount <= 0) {
            return 0;
        }

        return Math.max(0, Math.min(hoverRow, rowCount - 1));
    }

    /**
     * Checks whether the specified point intersects the plus icon hit area.
     *
     * @param point mouse location
     * @return {@code true} if the point is on the plus icon
     */
    private boolean isPointOnPlus(Point point) {
        if (hoverInsertColumn == -1 || hoverRow == -1) {
            return false;
        }

        Rectangle hitBounds = getPlusHitBounds(hoverInsertColumn);
        return hitBounds.contains(point);
    }

    /**
     * Returns the visual bounds of the plus icon for an insertion position.
     *
     * @param insertColumn insertion index
     * @return plus icon bounds
     */
    private Rectangle getPlusBounds(int insertColumn) {
        if (hoverRow == -1 || table.getRowCount() <= 0) {
            return new Rectangle();
        }

        int safeRow = getSafeHoverRow();

        Rectangle rowBounds = table.getCellRect(safeRow, 0, true);

        int x = getInsertLineX(insertColumn);
        int y = rowBounds.y + rowBounds.height / 2;

        int half = INSERT_PLUS_SIZE / 2;

        return new Rectangle(x - half, y - half, INSERT_PLUS_SIZE, INSERT_PLUS_SIZE);
    }

    /**
     * Returns the clickable hit bounds of the plus icon.
     *
     * @param insertColumn insertion index
     * @return expanded hit area bounds
     */
    private Rectangle getPlusHitBounds(int insertColumn) {
        Rectangle visual = getPlusBounds(insertColumn);
        int padding = INSERT_PLUS_HIT_PADDING;

        return new Rectangle(
            visual.x - padding,
            visual.y - padding,
            visual.width + padding * 2,
            visual.height + padding * 2
        );
    }

    /**
     * Inserts a column at the currently hovered insertion position.
     */
    private void insertColumnAtHoverPosition() {
        if (insertingColumn) {
            return;
        }

        final int insertIndex = hoverInsertColumn;

        if (insertIndex < 0) {
            return;
        }

        if (!isInsertColumnAllowed(insertIndex)) {
            return;
        }

        insertingColumn = true;

        try {
            if (table.isEditing() && table.getCellEditor() != null) {
                boolean editingStopped = table.getCellEditor().stopCellEditing();

                if (!editingStopped) {
                    return;
                }
            }

            suppressMouseEventsBriefly();

            clearHoverState();

            if (insertColumnHandler != null) {
                insertColumnHandler.accept(insertIndex);
            } else {
                triggerDefaultInsertColumnAction(insertIndex);
            }

            selectInsertedColumn(insertIndex);
        } finally {
            hoverInsertColumn = -1;
            hoverRow = -1;
            insertingColumn = false;
            resetTableCursor();
            table.repaint();
        }

        SwingUtilities.invokeLater(
            () -> {
                selectInsertedColumn(insertIndex);
                table.repaint();
            }
        );
    }

    /**
     * Executes the default table column insertion behavior.
     *
     * @param insertIndex desired insertion index
     */
    private void triggerDefaultInsertColumnAction(int insertIndex) {
        int columnCount = table.getColumnCount();

        if (columnCount <= 0) {
            triggerTableAction("Add Column");
            return;
        }

        /*
         * Fallback behavior:
         * Existing Add Column behavior usually inserts after the selected column.
         * So boundary N means select N - 1 before triggering "Add Column".
         */
        int contextColumn = Math.max(0, Math.min(insertIndex - 1, columnCount - 1));

        if (table.getRowCount() > 0) {
            table.changeSelection(0, contextColumn, false, false);
        } else {
            table.setColumnSelectionInterval(contextColumn, contextColumn);
        }

        triggerTableAction("Add Column");
    }

    /**
     * Invokes a named action from the table's action map.
     *
     * @param actionName action key
     */
    private void triggerTableAction(String actionName) {
        Action action = table.getActionMap().get(actionName);

        if (action != null) {
            action.actionPerformed(
                new ActionEvent(table, ActionEvent.ACTION_PERFORMED, actionName)
            );
        }
    }

    /**
     * Selects the newly inserted column and scrolls it into view when possible.
     *
     * @param insertedColumnIndex inserted column index
     */
    private void selectInsertedColumn(int insertedColumnIndex) {
        int columnCount = table.getColumnCount();

        if (columnCount <= 0) {
            return;
        }

        int safeColumn = Math.max(0, Math.min(insertedColumnIndex, columnCount - 1));

        table.clearSelection();

        if (table.getRowCount() > 0) {
            table.changeSelection(0, safeColumn, false, false);
            table.addColumnSelectionInterval(safeColumn, safeColumn);
            table.scrollRectToVisible(table.getCellRect(0, safeColumn, true));
        } else {
            table.addColumnSelectionInterval(safeColumn, safeColumn);
        }
    }

    /**
     * Paints the insertion guide line and plus icon in the table body.
     *
     * @param g graphics context
     */
    private void paintInsertColumnIndicator(Graphics g) {
        if (!enabled || hoverInsertColumn == -1 || hoverRow == -1) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();

        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int x = getInsertLineX(hoverInsertColumn);

            Color accentColor = INSERT_INDICATOR_COLOR;

            g2.setComposite(
                AlphaComposite.getInstance(AlphaComposite.SRC_OVER, INSERT_LINE_OPACITY)
            );
            g2.setColor(accentColor);
            g2.setStroke(new BasicStroke(2f));
            int safeRow = getSafeHoverRow();
            Rectangle rowBounds = table.getCellRect(safeRow, 0, true);

            g2.drawLine(x, rowBounds.y, x, rowBounds.y + rowBounds.height);

            Rectangle plusBounds = getPlusBounds(hoverInsertColumn);

            g2.setComposite(
                AlphaComposite.getInstance(AlphaComposite.SRC_OVER, INSERT_PLUS_OPACITY)
            );
            g2.setColor(accentColor);
            g2.fillOval(plusBounds.x, plusBounds.y, plusBounds.width, plusBounds.height);

            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2f));

            int centerX = plusBounds.x + plusBounds.width / 2;
            int centerY = plusBounds.y + plusBounds.height / 2;

            g2.drawLine(centerX - 5, centerY, centerX + 5, centerY);
            g2.drawLine(centerX, centerY - 5, centerX, centerY + 5);
        } finally {
            g2.dispose();
        }
    }

    /**
     * Clears the current hover state and resets the table cursor.
     */
    private void clearHoverState() {
        if (hoverInsertColumn == -1 && hoverRow == -1) {
            resetTableCursor();
            return;
        }

        hoverInsertColumn = -1;
        hoverRow = -1;
        resetTableCursor();
        table.repaint();
    }

    /**
     * Restores the default cursor on the table.
     */
    private void resetTableCursor() {
        table.setCursor(Cursor.getDefaultCursor());
    }
}

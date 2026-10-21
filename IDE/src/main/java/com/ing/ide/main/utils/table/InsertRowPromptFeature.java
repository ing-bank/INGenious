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
 * Provides a visual insert-row prompt for a table, allowing users to insert
 * rows at a specific position via a hoverable plus icon.
 *
 * The prompt is triggered from a thin, invisible hover rail along the left edge
 * of the table instead of the full width of the first column, so the prompt can
 * no longer be activated while working inside table cells.
 */
public class InsertRowPromptFeature {
    private static final Color INSERT_INDICATOR_COLOR = Color.decode("#7724FF");

    private static final float INSERT_LINE_OPACITY = 0.45f;
    private static final float INSERT_PLUS_OPACITY = 0.90f;

    /*
     * Width of the invisible hover rail along the left edge of the table. Only
     * pointer positions inside this rail (or on the plus icon itself) arm the
     * insert-row prompt.
     */
    private static final int INSERT_RAIL_WIDTH = 6;

    private static final int INSERT_PLUS_HIT_PADDING = 2;
    private static final int INSERT_PLUS_SIZE = 9;

    private static final int INSERT_HOVER_ZONE = INSERT_PLUS_SIZE / 2 + INSERT_PLUS_HIT_PADDING;

    private static final int INSERT_MOUSE_SUPPRESSION_MS = 140;

    private final JTable table;

    private int hoverInsertRow = -1;
    private boolean insertingRow = false;
    private boolean installed = false;

    private long suppressMouseEventsUntil = 0L;

    private IntConsumer insertRowHandler;

    private boolean enabled = true;

    /**
     * Creates a new insert-row prompt feature for the specified table.
     *
     * @param table target table
     */
    public InsertRowPromptFeature(JTable table) {
        this.table = table;
    }

    /**
     * Installs the mouse listeners required to support row insertion prompts.
     * Subsequent calls have no effect.
     */
    public void install() {
        if (installed) {
            return;
        }

        installed = true;

        MouseAdapter insertRowMouseAdapter = new MouseAdapter() {

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

                int newHoverInsertRow = getInsertRowForPoint(e.getPoint());

                if (newHoverInsertRow != hoverInsertRow) {
                    hoverInsertRow = newHoverInsertRow;
                    table.repaint();
                }

                if (hoverInsertRow != -1 && isPointOnPlus(e.getPoint())) {
                    table.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                } else {
                    table.setCursor(Cursor.getDefaultCursor());
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                clearHoverState();
            }
        };

        table.addMouseMotionListener(insertRowMouseAdapter);
        table.addMouseListener(insertRowMouseAdapter);
    }

    /**
     * Enables or disables the feature.
     *
     * @param enabled {@code true} to enable row insertion prompts
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;

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
     * Sets a custom row insertion handler.
     *
     * @param insertRowHandler callback that receives the insertion index
     */
    public void setInsertRowHandler(IntConsumer insertRowHandler) {
        this.insertRowHandler = insertRowHandler;
    }

    /**
     * Paints the insert-row indicator when a valid insertion location is
     * hovered. The hover rail itself is intentionally not painted so the table
     * gutter stays clean until the prompt is armed.
     *
     * @param g graphics context
     */
    public void paint(Graphics g) {
        if (!enabled) {
            return;
        }

        if (hoverInsertRow != -1) {
            paintInsertRowIndicator(g);
        }
    }

    /**
     * Processes mouse events that may trigger a row insertion.
     *
     * @param e mouse event
     * @return {@code true} if the event was consumed
     */
    public boolean processMouseEvent(MouseEvent e) {
        if (!enabled) {
            return false;
        }

        if (isSuppressingMouseEvents()) {
            e.consume();
            return true;
        }

        if (e.getID() == MouseEvent.MOUSE_PRESSED) {
            if (!insertingRow && hoverInsertRow != -1 && isPointOnPlus(e.getPoint())) {
                insertRowAtHoverPosition();
                e.consume();
                return true;
            }
        }

        return false;
    }

    /**
     * Processes mouse motion events during temporary event suppression periods.
     *
     * @param e mouse event
     * @return {@code true} if the event was consumed
     */
    public boolean processMouseMotionEvent(MouseEvent e) {
        if (!enabled) {
            return false;
        }

        if (isSuppressingMouseEvents()) {
            e.consume();
            return true;
        }

        return false;
    }

    private boolean isSuppressingMouseEvents() {
        return System.currentTimeMillis() < suppressMouseEventsUntil;
    }

    /**
     * Temporarily suppresses mouse events to avoid unintended interactions
     * immediately after a row insertion.
     */
    private void suppressMouseEventsBriefly() {
        suppressMouseEventsUntil = System.currentTimeMillis() + INSERT_MOUSE_SUPPRESSION_MS;
    }

    /**
     * Resolves the row insertion index for the specified point.
     *
     * The prompt is only armed while the pointer is inside the thin left rail or
     * already on the plus icon. Inside the rail the nearest row boundary is used,
     * so the whole rail segment next to a row acts as the hover target.
     *
     * @param point mouse location
     * @return insertion row index, or {@code -1} if none applies
     */
    private int getInsertRowForPoint(Point point) {
        if (hoverInsertRow != -1 && isPointInArmedCorridor(point)) {
            return hoverInsertRow;
        }

        if (!isPointInRail(point)) {
            return -1;
        }

        int rowCount = table.getRowCount();

        if (rowCount == 0) {
            int emptyInsertLineY = getEmptyTableInsertLineY();

            if (Math.abs(point.y - emptyInsertLineY) <= INSERT_HOVER_ZONE) {
                return 0;
            }

            return -1;
        }

        int row = table.rowAtPoint(point);

        if (row != -1) {
            Rectangle rowBounds = table.getCellRect(row, 0, true);

            boolean nearerToTop = point.y - rowBounds.y < rowBounds.height / 2;

            return nearerToTop ? row : row + 1;
        }

        return getInsertRowForPointOutsideRows(point);
    }

    /**
     * Checks whether the specified point lies inside the thin hover rail along
     * the left edge of the table.
     *
     * @param point mouse location
     * @return {@code true} if the point is inside the rail
     */
    private boolean isPointInRail(Point point) {
        return point.x >= 0 && point.x <= INSERT_RAIL_WIDTH;
    }

    /**
     * Checks whether the specified point is inside the corridor that keeps an
     * already armed prompt alive.
     *
     * The plus icon sits at the left edge of the table, so the band along the
     * insertion line between the rail and the icon must stay armed while the
     * pointer travels towards it. The corridor never arms the prompt by itself.
     *
     * @param point mouse location
     * @return {@code true} if the armed prompt should be kept
     */
    private boolean isPointInArmedCorridor(Point point) {
        if (hoverInsertRow == -1) {
            return false;
        }

        Rectangle plusHitBounds = getPlusHitBounds(hoverInsertRow);

        Rectangle corridor = new Rectangle(
            0,
            plusHitBounds.y,
            plusHitBounds.x + plusHitBounds.width,
            plusHitBounds.height
        );

        return corridor.contains(point);
    }

    /**
     * Determines an insertion index when the pointer is outside the bounds of
     * any table row.
     *
     * @param point mouse location
     * @return insertion row index, or {@code -1} if none applies
     */
    private int getInsertRowForPointOutsideRows(Point point) {
        int rowCount = table.getRowCount();

        if (rowCount == 0) {
            return -1;
        }

        int rowHeight = Math.max(1, table.getRowHeight());

        Rectangle lastRow = table.getCellRect(rowCount - 1, 0, true);
        int lastRowBottom = lastRow.y + lastRow.height;

        if (point.y >= lastRowBottom && point.y - lastRowBottom <= rowHeight) {
            return rowCount;
        }

        Rectangle firstRow = table.getCellRect(0, 0, true);
        int firstRowTop = firstRow.y;

        if (point.y <= firstRowTop && firstRowTop - point.y <= rowHeight) {
            return 0;
        }

        return -1;
    }

    /**
     * Returns the vertical position of the insertion indicator line.
     *
     * @param insertRow insertion index
     * @return y-coordinate of the insertion line
     */
    private int getInsertLineY(int insertRow) {
        int rowCount = table.getRowCount();

        if (rowCount == 0) {
            return getEmptyTableInsertLineY();
        }

        if (insertRow <= 0) {
            Rectangle firstRow = table.getCellRect(0, 0, true);
            return firstRow.y;
        }

        if (insertRow >= rowCount) {
            Rectangle lastRow = table.getCellRect(rowCount - 1, 0, true);
            return lastRow.y + lastRow.height;
        }

        Rectangle targetRow = table.getCellRect(insertRow, 0, true);
        return targetRow.y;
    }

    /**
     * Computes the insertion line position for an empty table.
     *
     * @return y-coordinate of the insertion line
     */
    private int getEmptyTableInsertLineY() {
        int halfPlus = INSERT_PLUS_SIZE / 2;
        int preferredY = Math.max(halfPlus, table.getRowHeight() / 2);

        int maxVisibleY = Math.max(halfPlus, table.getHeight() - halfPlus);

        return Math.min(preferredY, maxVisibleY);
    }

    /**
     * Checks whether the specified point intersects the plus icon hit area.
     *
     * @param point mouse location
     * @return {@code true} if the point is on the plus icon
     */
    private boolean isPointOnPlus(Point point) {
        if (hoverInsertRow == -1) {
            return false;
        }

        Rectangle hitBounds = getPlusHitBounds(hoverInsertRow);
        return hitBounds.contains(point);
    }

    /**
     * Returns the visual bounds of the plus icon for an insertion position.
     *
     * @param insertRow insertion index
     * @return plus icon bounds
     */
    private Rectangle getPlusBounds(int insertRow) {
        int y = getInsertLineY(insertRow);
        int x = getPlusX();
        int half = INSERT_PLUS_SIZE / 2;

        return new Rectangle(x - half, y - half, INSERT_PLUS_SIZE, INSERT_PLUS_SIZE);
    }

    /**
     * Returns the horizontal center of the plus icon, which sits at the
     * leftmost edge of the table so the icon rests against the start of the
     * first column instead of covering cell content.
     *
     * @return x-coordinate of the plus icon center
     */
    private int getPlusX() {
        return INSERT_PLUS_SIZE / 2;
    }

    /**
     * Returns the clickable hit bounds of the plus icon.
     *
     * @param insertRow insertion index
     * @return expanded hit area bounds
     */
    private Rectangle getPlusHitBounds(int insertRow) {
        Rectangle visual = getPlusBounds(insertRow);
        int padding = INSERT_PLUS_HIT_PADDING;

        return new Rectangle(
            visual.x - padding,
            visual.y - padding,
            visual.width + padding * 2,
            visual.height + padding * 2
        );
    }

    /**
     * Inserts a row at the currently hovered insertion position.
     */
    private void insertRowAtHoverPosition() {
        if (insertingRow) {
            return;
        }

        final int insertIndex = hoverInsertRow;

        if (insertIndex < 0) {
            return;
        }

        insertingRow = true;

        try {
            if (table.isEditing() && table.getCellEditor() != null) {
                boolean editingStopped = table.getCellEditor().stopCellEditing();

                if (!editingStopped) {
                    return;
                }
            }

            suppressMouseEventsBriefly();

            table.clearSelection();

            clearHoverState();

            if (insertRowHandler != null) {
                insertRowHandler.accept(insertIndex);
            } else {
                triggerDefaultInsertRowAction(insertIndex);
            }

            selectInsertedRow(insertIndex);
        } finally {
            hoverInsertRow = -1;
            insertingRow = false;
            table.setCursor(Cursor.getDefaultCursor());
            table.repaint();
        }

        SwingUtilities.invokeLater(
            () -> {
                selectInsertedRow(insertIndex);
                table.repaint();
            }
        );
    }

    /**
     * Selects the newly inserted row while preserving selection model state.
     *
     * @param insertedRowIndex inserted row index
     */
    private void selectInsertedRow(int insertedRowIndex) {
        if (table.getRowCount() == 0 || table.getColumnCount() == 0) {
            table.clearSelection();
            return;
        }

        int safeRow = Math.max(0, Math.min(insertedRowIndex, table.getRowCount() - 1));

        final boolean oldRowAdjusting = table.getSelectionModel().getValueIsAdjusting();
        final boolean oldColumnAdjusting = table
            .getColumnModel()
            .getSelectionModel()
            .getValueIsAdjusting();

        try {
            table.getSelectionModel().setValueIsAdjusting(true);
            table.getColumnModel().getSelectionModel().setValueIsAdjusting(true);

            table.clearSelection();

            table.addRowSelectionInterval(safeRow, safeRow);
            table.addColumnSelectionInterval(0, table.getColumnCount() - 1);

            table.getSelectionModel().setLeadSelectionIndex(safeRow);
            table.getColumnModel().getSelectionModel().setLeadSelectionIndex(0);
        } finally {
            table.getSelectionModel().setValueIsAdjusting(oldRowAdjusting);
            table.getColumnModel().getSelectionModel().setValueIsAdjusting(oldColumnAdjusting);
        }
    }

    /**
     * Paints the insertion guide line and plus icon.
     *
     * @param g graphics context
     */
    private void paintInsertRowIndicator(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();

        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int y = getInsertLineY(hoverInsertRow);

            Color accentColor = INSERT_INDICATOR_COLOR;

            g2.setComposite(
                AlphaComposite.getInstance(AlphaComposite.SRC_OVER, INSERT_LINE_OPACITY)
            );
            g2.setColor(accentColor);
            g2.setStroke(new BasicStroke(2f));
            g2.drawLine(0, y, table.getWidth(), y);

            Rectangle plusBounds = getPlusBounds(hoverInsertRow);

            g2.setComposite(
                AlphaComposite.getInstance(AlphaComposite.SRC_OVER, INSERT_PLUS_OPACITY)
            );
            g2.setColor(accentColor);
            g2.fillOval(plusBounds.x, plusBounds.y, plusBounds.width, plusBounds.height);

            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(Math.max(1f, plusBounds.width / 9f)));

            int centerX = plusBounds.x + plusBounds.width / 2;
            int centerY = plusBounds.y + plusBounds.height / 2;
            int arm = Math.max(2, Math.round(plusBounds.width * 0.28f));

            g2.drawLine(centerX - arm, centerY, centerX + arm, centerY);
            g2.drawLine(centerX, centerY - arm, centerX, centerY + arm);
        } finally {
            g2.dispose();
        }
    }

    /**
     * Clears the current hover state and resets the cursor.
     */
    private void clearHoverState() {
        if (hoverInsertRow == -1) {
            table.setCursor(Cursor.getDefaultCursor());
            return;
        }

        hoverInsertRow = -1;
        table.setCursor(Cursor.getDefaultCursor());
        table.repaint();
    }

    /**
     * Executes the default table insert behavior for the specified row index.
     *
     * @param rowIndex desired insertion index
     */
    private void triggerDefaultInsertRowAction(int rowIndex) {
        int rowCount = table.getRowCount();

        if (rowCount == 0) {
            triggerTableAction("Add");
            return;
        }

        if (rowIndex >= rowCount) {
            table.changeSelection(rowCount - 1, 0, false, false);
            triggerTableAction("Add");
            return;
        }

        int safeRow = Math.max(0, Math.min(rowIndex, rowCount - 1));

        if (table.getColumnCount() > 0) {
            table.changeSelection(safeRow, 0, false, false);
        }

        triggerTableAction("Insert");
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
}

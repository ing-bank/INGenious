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
import javax.swing.table.JTableHeader;

/**
 * Provides a visual insert-column prompt driven from a thin, invisible hover
 * strip along the bottom of the table header.
 *
 * Hovering the strip arms the prompt and shows a plus icon on the nearest column
 * boundary, so the prompt can no longer be triggered while working inside table
 * cells.
 */
public class InsertColumnPromptFeature {
    private static final Color INSERT_INDICATOR_COLOR = Color.decode("#7724FF");

    private static final float INSERT_LINE_OPACITY = 0.45f;
    private static final float INSERT_PLUS_OPACITY = 0.90f;

    /*
     * Height of the invisible hover strip along the bottom of the table header. Only
     * pointer positions inside this strip (or on the plus icon itself) arm the
     * insert-column prompt, while the rest of the header keeps its native
     * column resize behavior.
     */
    private static final int INSERT_STRIP_HEIGHT = 6;

    private static final int INSERT_PLUS_SIZE = 9;
    private static final int INSERT_PLUS_HIT_PADDING = 2;

    /*
     * Matches the resize margin used by BasicTableHeaderUI so the native resize
     * handle keeps priority over the insert prompt.
     */
    private static final int COLUMN_RESIZE_MARGIN = 3;

    private static final int INSERT_MOUSE_SUPPRESSION_MS = 140;

    private final JTable table;

    private int hoverInsertColumn = -1;
    private boolean insertingColumn = false;
    private boolean enabled = false;
    private boolean installed = false;

    private long suppressMouseEventsUntil = 0L;

    private IntConsumer insertColumnHandler;

    private MouseAdapter insertColumnMouseAdapter;

    private JTableHeader listeningHeader;

    private int minimumInsertColumn = 0;

    private boolean cursorOwnedByInsertColumnPrompt = false;

    private Boolean previousHeaderResizingAllowed;

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
     * Listeners are attached to the table header, where the hover strip lives.
     * Subsequent calls have no effect.
     */
    public void install() {
        if (installed) {
            return;
        }

        JTableHeader header = table.getTableHeader();

        if (header == null) {
            return;
        }

        installed = true;
        listeningHeader = header;

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

                    int newHoverInsertColumn = getInsertColumnForHeaderPoint(e.getPoint());

                    if (newHoverInsertColumn != hoverInsertColumn) {
                        hoverInsertColumn = newHoverInsertColumn;
                        repaintPrompt();
                    }

                    if (hoverInsertColumn != -1 && isPointOnPlus(e.getPoint())) {
                        setPromptCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    } else {
                        if (cursorOwnedByInsertColumnPrompt) {
                            resetTableCursor();
                        }

                        applyResizeCursorIfNeeded(e.getPoint());
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

        header.addMouseMotionListener(insertColumnMouseAdapter);
        header.addMouseListener(insertColumnMouseAdapter);
    }

    /**
     * Removes installed listeners from the table header.
     */
    public void uninstall() {
        if (!installed) {
            return;
        }

        if (insertColumnMouseAdapter != null && listeningHeader != null) {
            listeningHeader.removeMouseMotionListener(insertColumnMouseAdapter);
            listeningHeader.removeMouseListener(insertColumnMouseAdapter);
        }

        resetTableCursor();
        repaintPrompt();

        insertColumnMouseAdapter = null;
        listeningHeader = null;
        installed = false;
        hoverInsertColumn = -1;
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

        repaintPrompt();
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

        repaintPrompt();
    }

    /**
     * Paints the insert-column guide line across the table body when a valid
     * insertion location is hovered in the header strip.
     *
     * This should be called by the owning table after normal table painting.
     *
     * @param g graphics context
     */
    public void paint(Graphics g) {
        if (enabled && hoverInsertColumn != -1) {
            paintInsertColumnGuideLine(g);
        }
    }

    /**
     * Paints the insert-column indicator inside the table header when armed.
     * The hover strip itself is intentionally not painted so the header stays
     * clean until the prompt is armed.
     *
     * This should be called by the table header after normal header painting.
     *
     * @param g graphics context
     */
    public void paintHeader(Graphics g) {
        if (!enabled) {
            return;
        }

        if (hoverInsertColumn != -1) {
            paintInsertColumnIndicator(g);
        }
    }

    /**
     * Repaints both the table body and its header, which together render the
     * insert-column prompt.
     */
    private void repaintPrompt() {
        table.repaint();

        JTableHeader header = table.getTableHeader();

        if (header != null) {
            header.repaint();
        }
    }

    /**
     * Applies a cursor owned by this prompt to the table header.
     *
     * @param cursor cursor to apply
     */
    private void setPromptCursor(Cursor cursor) {
        JTableHeader header = table.getTableHeader();

        if (header != null) {
            header.setCursor(cursor);

            /*
             * The plus icon sits exactly on a column boundary, which is also the
             * column resize handle. Resizing is suspended while the plus is
             * armed so a click cannot start a resize drag instead.
             */
            if (previousHeaderResizingAllowed == null) {
                previousHeaderResizingAllowed = header.getResizingAllowed();
                header.setResizingAllowed(false);
            }
        }

        cursorOwnedByInsertColumnPrompt = true;
    }

    /**
     * Restores the header resizing behavior suspended while the plus was armed.
     */
    private void restoreHeaderResizing() {
        JTableHeader header = table.getTableHeader();

        if (header != null && previousHeaderResizingAllowed != null) {
            header.setResizingAllowed(previousHeaderResizingAllowed);
        }

        previousHeaderResizingAllowed = null;
    }

    /**
     * Shows the column resize cursor when the pointer sits on a resize handle.
     *
     * The header UI evaluates the cursor before this listener runs, so the
     * cursor is applied here directly to avoid a stale cursor during the event
     * that re-enables resizing.
     *
     * @param point mouse location within the table header
     */
    private void applyResizeCursorIfNeeded(Point point) {
        if (!isPointInColumnResizeZone(point)) {
            return;
        }

        JTableHeader header = table.getTableHeader();

        if (header != null) {
            header.setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
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
     * Resolves the column insertion index for the specified header point.
     *
     * The prompt is only armed while the pointer is inside the thin header strip
     * or already on the plus icon. Inside the strip the nearest column boundary
     * is used, so the whole strip segment above a column acts as hover target.
     *
     * @param point mouse location within the table header
     * @return insertion column index, or {@code -1} if none applies
     */
    private int getInsertColumnForHeaderPoint(Point point) {
        if (hoverInsertColumn != -1 && isPointInArmedCorridor(point)) {
            return hoverInsertColumn;
        }

        if (!isPointInHeaderStrip(point)) {
            return -1;
        }

        int columnCount = table.getColumnCount();

        if (columnCount <= 0) {
            return -1;
        }

        int column = table.columnAtPoint(new Point(point.x, 0));

        if (column == -1) {
            return getInsertColumnForPointOutsideColumns(point);
        }

        Rectangle columnBounds = getColumnBounds(column);

        boolean nearerToLeft = point.x - columnBounds.x < columnBounds.width / 2;

        /*
         * If a custom handler exists, insertion before the first visible column
         * is allowed. This is needed for tables whose visible columns are mapped
         * to different model columns, such as test data tables with frozen columns.
         *
         * Without a handler, fallback "Add Column" can only insert after a selected
         * column, so before-first-column insertion is not reliable.
         */
        int candidateInsertColumn = nearerToLeft ? column : column + 1;

        if (candidateInsertColumn == 0 && insertColumnHandler == null) {
            candidateInsertColumn = 1;
        }

        return isInsertColumnAllowed(candidateInsertColumn) ? candidateInsertColumn : -1;
    }

    /**
     * Checks whether the specified point lies inside the thin hover strip along
     * the bottom of the table header.
     *
     * @param point mouse location within the table header
     * @return {@code true} if the point is inside the strip
     */
    private boolean isPointInHeaderStrip(Point point) {
        return point.y >= getHeaderStripTop() && point.y <= getHeaderHeight();
    }

    /**
     * Returns the y-coordinate where the hover strip starts, measured in table
     * header coordinates.
     *
     * @return top of the hover strip
     */
    private int getHeaderStripTop() {
        return Math.max(0, getHeaderHeight() - INSERT_STRIP_HEIGHT);
    }

    /**
     * Checks whether the specified point is inside the corridor that keeps an
     * already armed prompt alive.
     *
     * The plus icon sits at the bottom of the header, so the band along the
     * insertion line between the strip and the icon must stay armed while the
     * pointer travels towards it. The corridor never arms the prompt by itself.
     *
     * @param point mouse location within the table header
     * @return {@code true} if the armed prompt should be kept
     */
    private boolean isPointInArmedCorridor(Point point) {
        if (hoverInsertColumn == -1) {
            return false;
        }

        Rectangle plusHitBounds = getPlusHitBounds(hoverInsertColumn);

        Rectangle corridor = new Rectangle(
            plusHitBounds.x,
            0,
            plusHitBounds.width,
            getHeaderHeight()
        );

        return corridor.contains(point);
    }

    /**
     * Returns the horizontal bounds of a visible column, expressed in the shared
     * coordinate space of the table and its header.
     *
     * @param column visible column index
     * @return column bounds
     */
    private Rectangle getColumnBounds(int column) {
        int x = 0;

        for (int i = 0; i < column; i++) {
            x += table.getColumnModel().getColumn(i).getWidth();
        }

        int width = table.getColumnModel().getColumn(column).getWidth();

        return new Rectangle(x, 0, width, 1);
    }

    /**
     * Determines an insertion index when the pointer is past the last visible
     * column in the header strip.
     *
     * @param point mouse location within the table header
     * @return insertion column index, or {@code -1} if none applies
     */
    private int getInsertColumnForPointOutsideColumns(Point point) {
        int columnCount = table.getColumnCount();

        if (columnCount <= 0) {
            return -1;
        }

        Rectangle lastColumn = getColumnBounds(columnCount - 1);
        int lastColumnRight = lastColumn.x + lastColumn.width;

        if (point.x >= lastColumnRight) {
            return isInsertColumnAllowed(columnCount) ? columnCount : -1;
        }

        return -1;
    }

    /**
     * Returns the horizontal position of the insertion indicator line, shared by
     * the header strip and the table body.
     *
     * @param insertColumn insertion index
     * @return x-coordinate of the insertion line
     */
    private int getInsertLineX(int insertColumn) {
        int columnCount = table.getColumnCount();

        if (columnCount <= 0) {
            return 0;
        }

        if (insertColumn <= 0) {
            return getColumnBounds(0).x;
        }

        if (insertColumn >= columnCount) {
            Rectangle lastColumn = getColumnBounds(columnCount - 1);
            return lastColumn.x + lastColumn.width;
        }

        return getColumnBounds(insertColumn).x;
    }

    /**
     * Checks whether the specified point intersects the plus icon hit area.
     *
     * @param point mouse location within the table header
     * @return {@code true} if the point is on the plus icon
     */
    private boolean isPointOnPlus(Point point) {
        if (hoverInsertColumn == -1) {
            return false;
        }

        if (isPointInColumnResizeZone(point)) {
            return false;
        }

        Rectangle hitBounds = getPlusHitBounds(hoverInsertColumn);
        return hitBounds.contains(point);
    }

    /**
     * Checks whether the specified point sits on a column resize handle.
     *
     * The hover strip is excluded so the plus icon stays reachable there, while
     * the rest of the header keeps its native resize behavior.
     *
     * @param point mouse location within the table header
     * @return {@code true} if the point is on a resize handle
     */
    private boolean isPointInColumnResizeZone(Point point) {
        if (point.y >= getHeaderStripTop()) {
            return false;
        }

        JTableHeader header = table.getTableHeader();

        if (header == null || !isHeaderResizingAllowed(header)) {
            return false;
        }

        int columnCount = table.getColumnCount();
        int boundaryX = 0;

        for (int i = 0; i < columnCount; i++) {
            boundaryX += table.getColumnModel().getColumn(i).getWidth();

            if (Math.abs(point.x - boundaryX) <= COLUMN_RESIZE_MARGIN) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns whether the header allows resizing, ignoring any suspension
     * applied by this prompt.
     *
     * @param header table header
     * @return {@code true} if the header natively allows column resizing
     */
    private boolean isHeaderResizingAllowed(JTableHeader header) {
        return previousHeaderResizingAllowed != null
            ? previousHeaderResizingAllowed
            : header.getResizingAllowed();
    }

    /**
     * Returns the visual bounds of the plus icon, expressed in table header
     * coordinates.
     *
     * @param insertColumn insertion index
     * @return plus icon bounds
     */
    private Rectangle getPlusBounds(int insertColumn) {
        if (table.getColumnCount() <= 0) {
            return new Rectangle();
        }

        int x = getInsertLineX(insertColumn);
        int half = INSERT_PLUS_SIZE / 2;

        /*
         * The icon is centered on the header/first-row boundary so it sits on a
         * real row/column intersection. The header paints its upper half and the
         * table body paints the lower half.
         */
        int y = getHeaderHeight();

        return new Rectangle(x - half, y - half, INSERT_PLUS_SIZE, INSERT_PLUS_SIZE);
    }

    /**
     * Returns the current table header height.
     *
     * @return header height in pixels
     */
    private int getHeaderHeight() {
        JTableHeader header = table.getTableHeader();

        return header != null ? header.getHeight() : INSERT_PLUS_SIZE;
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
            insertingColumn = false;
            resetTableCursor();
            repaintPrompt();
        }

        SwingUtilities.invokeLater(
            () -> {
                selectInsertedColumn(insertIndex);
                repaintPrompt();
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
     * Paints the insertion guide line and the upper half of the plus icon inside
     * the table header.
     *
     * @param g graphics context
     */
    private void paintInsertColumnIndicator(Graphics g) {
        if (!enabled || hoverInsertColumn == -1) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();

        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int x = getInsertLineX(hoverInsertColumn);

            g2.setComposite(
                AlphaComposite.getInstance(AlphaComposite.SRC_OVER, INSERT_LINE_OPACITY)
            );
            g2.setColor(INSERT_INDICATOR_COLOR);
            g2.setStroke(new BasicStroke(2f));
            g2.drawLine(x, 0, x, getHeaderHeight());

            paintPlusIcon(g2, getPlusBounds(hoverInsertColumn));
        } finally {
            g2.dispose();
        }
    }

    /**
     * Paints the plus icon at the specified bounds. Callers position the bounds
     * in their own coordinate space, so the icon can straddle the header and the
     * table body.
     *
     * @param g2 graphics context
     * @param plusBounds plus icon bounds
     */
    private void paintPlusIcon(Graphics2D g2, Rectangle plusBounds) {
        if (plusBounds.isEmpty()) {
            return;
        }

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, INSERT_PLUS_OPACITY));
        g2.setColor(INSERT_INDICATOR_COLOR);
        g2.fillOval(plusBounds.x, plusBounds.y, plusBounds.width, plusBounds.height);

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(Math.max(1f, plusBounds.width / 9f)));

        int centerX = plusBounds.x + plusBounds.width / 2;
        int centerY = plusBounds.y + plusBounds.height / 2;
        int arm = Math.max(2, Math.round(plusBounds.width * 0.28f));

        g2.drawLine(centerX - arm, centerY, centerX + arm, centerY);
        g2.drawLine(centerX, centerY - arm, centerX, centerY + arm);
    }

    /**
     * Paints the insertion guide line across the table body together with the
     * lower half of the plus icon, which is centered on the boundary between the
     * header and the first visible row.
     *
     * @param g graphics context
     */
    private void paintInsertColumnGuideLine(Graphics g) {
        if (table.getRowCount() <= 0 || table.getColumnCount() <= 0) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();

        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setComposite(
                AlphaComposite.getInstance(AlphaComposite.SRC_OVER, INSERT_LINE_OPACITY)
            );
            g2.setColor(INSERT_INDICATOR_COLOR);
            g2.setStroke(new BasicStroke(2f));

            int x = getInsertLineX(hoverInsertColumn);

            Rectangle firstRowBounds = table.getCellRect(0, 0, true);
            Rectangle lastRowBounds = table.getCellRect(table.getRowCount() - 1, 0, true);

            int lineTop = firstRowBounds.y;
            int lineBottom = lastRowBounds.y + lastRowBounds.height;

            g2.drawLine(x, lineTop, x, lineBottom);

            int half = INSERT_PLUS_SIZE / 2;
            int plusCenterY = Math.max(lineTop, table.getVisibleRect().y);

            paintPlusIcon(
                g2,
                new Rectangle(x - half, plusCenterY - half, INSERT_PLUS_SIZE, INSERT_PLUS_SIZE)
            );
        } finally {
            g2.dispose();
        }
    }

    /**
     * Clears the current hover state and resets the cursor.
     */
    private void clearHoverState() {
        if (hoverInsertColumn == -1) {
            resetTableCursor();
            return;
        }

        hoverInsertColumn = -1;
        resetTableCursor();
        repaintPrompt();
    }

    /**
     * Restores the default cursor on the table header.
     */
    private void resetTableCursor() {
        restoreHeaderResizing();

        if (cursorOwnedByInsertColumnPrompt) {
            JTableHeader header = table.getTableHeader();

            if (header != null) {
                header.setCursor(Cursor.getDefaultCursor());
            }

            cursorOwnedByInsertColumnPrompt = false;
        }
    }
}

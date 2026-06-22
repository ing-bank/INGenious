package com.ing.ide.main.utils.table;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.IntConsumer;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.JTableHeader;

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
    private boolean insertingColumn = false;
    private boolean enabled = false;

    private long suppressMouseEventsUntil = 0L;

    private IntConsumer insertColumnHandler;

    private JTableHeader installedHeader;
    private MouseAdapter insertColumnMouseAdapter;
    private ComponentAdapter headerComponentAdapter;
    private HeaderOverlay overlay;

    public InsertColumnPromptFeature(JTable table) {
        this.table = table;
    }

    public void install() {
        JTableHeader header = table.getTableHeader();

        if (header == null) {
            return;
        }

        if (header == installedHeader) {
            ensureOverlayBounds();
            return;
        }

        uninstallFromCurrentHeader();

        installedHeader = header;

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

                    int newHoverInsertColumn = getInsertColumnForPoint(e.getPoint());

                    if (newHoverInsertColumn != hoverInsertColumn) {
                        hoverInsertColumn = newHoverInsertColumn;
                        repaintHeader();
                    }

                    if (hoverInsertColumn != -1 && isPointOnPlus(e.getPoint())) {
                        installedHeader.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    } else {
                        installedHeader.setCursor(Cursor.getDefaultCursor());
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

        headerComponentAdapter =
            new ComponentAdapter() {

                @Override
                public void componentResized(ComponentEvent e) {
                    ensureOverlayBounds();
                }
            };

        overlay = new HeaderOverlay();
        overlay.setOpaque(false);
        overlay.setEnabled(false);
        overlay.setFocusable(false);

        /*
         * JTableHeader normally does not use child components.
         * The overlay paints after the normal header rendering, giving us
         * a clean indicator without replacing the header renderer.
         */
        header.setLayout(null);
        header.add(overlay);
        header.addMouseMotionListener(insertColumnMouseAdapter);
        header.addMouseListener(insertColumnMouseAdapter);
        header.addComponentListener(headerComponentAdapter);

        ensureOverlayBounds();
        repaintHeader();
    }

    public void uninstall() {
        uninstallFromCurrentHeader();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;

        install();

        if (!enabled) {
            clearHoverState();
        }

        repaintHeader();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setInsertColumnHandler(IntConsumer insertColumnHandler) {
        this.insertColumnHandler = insertColumnHandler;
    }

    private void uninstallFromCurrentHeader() {
        if (installedHeader == null) {
            return;
        }

        if (insertColumnMouseAdapter != null) {
            installedHeader.removeMouseMotionListener(insertColumnMouseAdapter);
            installedHeader.removeMouseListener(insertColumnMouseAdapter);
        }

        if (headerComponentAdapter != null) {
            installedHeader.removeComponentListener(headerComponentAdapter);
        }

        if (overlay != null) {
            installedHeader.remove(overlay);
        }

        installedHeader.setCursor(Cursor.getDefaultCursor());
        installedHeader.repaint();

        installedHeader = null;
        insertColumnMouseAdapter = null;
        headerComponentAdapter = null;
        overlay = null;
        hoverInsertColumn = -1;
    }

    private boolean isSuppressingMouseEvents() {
        return System.currentTimeMillis() < suppressMouseEventsUntil;
    }

    private void suppressMouseEventsBriefly() {
        suppressMouseEventsUntil = System.currentTimeMillis() + INSERT_MOUSE_SUPPRESSION_MS;
    }

    private int getInsertColumnForPoint(Point point) {
        int columnCount = table.getColumnCount();

        if (columnCount <= 0 || installedHeader == null) {
            return -1;
        }

        int column = installedHeader.columnAtPoint(point);

        if (column == -1) {
            return getInsertColumnForPointOutsideColumns(point);
        }

        Rectangle columnBounds = installedHeader.getHeaderRect(column);

        int columnLeft = columnBounds.x;
        int columnRight = columnBounds.x + columnBounds.width;

        /*
         * If a custom handler exists, insertion before the first visible column
         * is allowed. This is needed for TestData tables with frozen columns:
         * visible column 0 maps to model column 4.
         *
         * Without a handler, fallback "Add Column" can only insert after a selected
         * column, so before-first-column insertion is not reliable.
         */
        if (
            Math.abs(point.x - columnLeft) <= INSERT_HOVER_ZONE &&
            (column > 0 || insertColumnHandler != null)
        ) {
            return column;
        }

        if (Math.abs(point.x - columnRight) <= INSERT_HOVER_ZONE) {
            return column + 1;
        }

        return -1;
    }

    private int getInsertColumnForPointOutsideColumns(Point point) {
        int columnCount = table.getColumnCount();

        if (columnCount <= 0 || installedHeader == null) {
            return -1;
        }

        Rectangle lastColumn = installedHeader.getHeaderRect(columnCount - 1);
        int lastColumnRight = lastColumn.x + lastColumn.width;

        if (Math.abs(point.x - lastColumnRight) <= INSERT_HOVER_ZONE) {
            return columnCount;
        }

        return -1;
    }

    private int getInsertLineX(int insertColumn) {
        int columnCount = table.getColumnCount();

        if (columnCount <= 0 || installedHeader == null) {
            return 0;
        }

        if (insertColumn <= 0) {
            Rectangle firstColumn = installedHeader.getHeaderRect(0);
            return firstColumn.x;
        }

        if (insertColumn >= columnCount) {
            Rectangle lastColumn = installedHeader.getHeaderRect(columnCount - 1);
            return lastColumn.x + lastColumn.width;
        }

        Rectangle targetColumn = installedHeader.getHeaderRect(insertColumn);
        return targetColumn.x;
    }

    private boolean isPointOnPlus(Point point) {
        if (hoverInsertColumn == -1) {
            return false;
        }

        Rectangle hitBounds = getPlusHitBounds(hoverInsertColumn);
        return hitBounds.contains(point);
    }

    private Rectangle getPlusBounds(int insertColumn) {
        int x = getInsertLineX(insertColumn);
        int y = installedHeader != null
            ? Math.max(INSERT_PLUS_SIZE / 2, installedHeader.getHeight() / 2)
            : INSERT_PLUS_SIZE / 2;

        int half = INSERT_PLUS_SIZE / 2;

        return new Rectangle(x - half, y - half, INSERT_PLUS_SIZE, INSERT_PLUS_SIZE);
    }

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

    private void insertColumnAtHoverPosition() {
        if (insertingColumn) {
            return;
        }

        final int insertIndex = hoverInsertColumn;

        if (insertIndex < 0) {
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
            resetHeaderCursor();
            repaintHeader();
            table.repaint();
        }

        SwingUtilities.invokeLater(
            () -> {
                install();
                selectInsertedColumn(insertIndex);
                repaintHeader();
                table.repaint();
            }
        );
    }

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

    private void triggerTableAction(String actionName) {
        Action action = table.getActionMap().get(actionName);

        if (action != null) {
            action.actionPerformed(
                new ActionEvent(table, ActionEvent.ACTION_PERFORMED, actionName)
            );
        }
    }

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

    private void paintInsertColumnIndicator(Graphics g) {
        if (!enabled || hoverInsertColumn == -1 || installedHeader == null) {
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
            g2.drawLine(x, 0, x, installedHeader.getHeight());

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

    private void clearHoverState() {
        if (hoverInsertColumn == -1) {
            resetHeaderCursor();
            return;
        }

        hoverInsertColumn = -1;
        resetHeaderCursor();
        repaintHeader();
    }

    private void resetHeaderCursor() {
        if (installedHeader != null) {
            installedHeader.setCursor(Cursor.getDefaultCursor());
        }
    }

    private void repaintHeader() {
        if (installedHeader != null) {
            installedHeader.repaint();
        }

        if (overlay != null) {
            overlay.repaint();
        }
    }

    private void ensureOverlayBounds() {
        if (installedHeader == null || overlay == null) {
            return;
        }

        Dimension size = installedHeader.getSize();
        overlay.setBounds(0, 0, size.width, size.height);
        overlay.revalidate();
        overlay.repaint();
    }

    private class HeaderOverlay extends JComponent {

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            paintInsertColumnIndicator(g);
        }
    }
}

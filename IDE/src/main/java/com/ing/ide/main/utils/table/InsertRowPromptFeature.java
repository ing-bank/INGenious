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

public class InsertRowPromptFeature {
    private static final Color INSERT_INDICATOR_COLOR = Color.decode("#7724FF");

    private static final float INSERT_LINE_OPACITY = 0.45f;
    private static final float INSERT_PLUS_OPACITY = 0.90f;

    private static final int INSERT_PLUS_X = 24;
    private static final int INSERT_PLUS_HIT_PADDING = 2;
    private static final int INSERT_PLUS_SIZE = 18;

    private static final int INSERT_HOVER_ZONE = INSERT_PLUS_SIZE / 2 + INSERT_PLUS_HIT_PADDING;

    private final JTable table;

    private int hoverInsertRow = -1;
    private boolean insertingRow = false;

    private IntConsumer insertRowHandler;

    public InsertRowPromptFeature(JTable table) {
        this.table = table;
    }

    public void install() {
        MouseAdapter insertRowMouseAdapter = new MouseAdapter() {

            @Override
            public void mouseMoved(MouseEvent e) {
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
                if (hoverInsertRow != -1) {
                    hoverInsertRow = -1;
                    table.setCursor(Cursor.getDefaultCursor());
                    table.repaint();
                }
            }
        };

        table.addMouseMotionListener(insertRowMouseAdapter);
        table.addMouseListener(insertRowMouseAdapter);
    }

    public void setInsertRowHandler(IntConsumer insertRowHandler) {
        this.insertRowHandler = insertRowHandler;
    }

    public void paint(Graphics g) {
        if (hoverInsertRow != -1) {
            paintInsertRowIndicator(g);
        }
    }

    public boolean processMouseEvent(MouseEvent e) {
        if (e.getID() == MouseEvent.MOUSE_PRESSED) {
            if (!insertingRow && hoverInsertRow != -1 && isPointOnPlus(e.getPoint())) {
                insertRowAtHoverPosition();
                e.consume();
                return true;
            }
        }

        return false;
    }

    private int getInsertRowForPoint(Point point) {
        if (table.getRowCount() == 0) {
            return -1;
        }

        int row = table.rowAtPoint(point);

        if (row == -1) {
            return -1;
        }

        Rectangle rowBounds = table.getCellRect(row, 0, true);

        int rowTop = rowBounds.y;
        int rowBottom = rowBounds.y + rowBounds.height;

        if (Math.abs(point.y - rowTop) <= INSERT_HOVER_ZONE) {
            return row;
        }

        if (Math.abs(point.y - rowBottom) <= INSERT_HOVER_ZONE) {
            return row + 1;
        }

        return -1;
    }

    private int getInsertLineY(int insertRow) {
        if (table.getRowCount() == 0) {
            return 0;
        }

        if (insertRow <= 0) {
            Rectangle firstRow = table.getCellRect(0, 0, true);
            return firstRow.y;
        }

        if (insertRow >= table.getRowCount()) {
            Rectangle lastRow = table.getCellRect(table.getRowCount() - 1, 0, true);
            return lastRow.y + lastRow.height;
        }

        Rectangle targetRow = table.getCellRect(insertRow, 0, true);
        return targetRow.y;
    }

    private boolean isPointOnPlus(Point point) {
        if (hoverInsertRow == -1) {
            return false;
        }

        Rectangle hitBounds = getPlusHitBounds(hoverInsertRow);
        return hitBounds.contains(point);
    }

    private Rectangle getPlusBounds(int insertRow) {
        int y = getInsertLineY(insertRow);

        int x = INSERT_PLUS_X;

        int half = INSERT_PLUS_SIZE / 2;

        return new Rectangle(x - half, y - half, INSERT_PLUS_SIZE, INSERT_PLUS_SIZE);
    }

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

    private void insertRowAtHoverPosition() {
        if (insertingRow) {
            return;
        }

        int insertIndex = hoverInsertRow;

        if (insertIndex < 0) {
            return;
        }

        insertingRow = true;

        if (table.isEditing() && table.getCellEditor() != null) {
            boolean editingStopped = table.getCellEditor().stopCellEditing();

            if (!editingStopped) {
                insertingRow = false;
                return;
            }
        }

        try {
            table.clearSelection();

            if (insertRowHandler != null) {
                insertRowHandler.accept(insertIndex);
            } else {
                triggerDefaultInsertRowAction(insertIndex);
            }

            table.clearSelection();
        } finally {
            hoverInsertRow = -1;
            insertingRow = false;
            table.setCursor(Cursor.getDefaultCursor());
            table.repaint();
        }

        SwingUtilities.invokeLater(
            () -> {
                table.clearSelection();
                table.repaint();
            }
        );
    }

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
            g2.setStroke(new BasicStroke(2f));

            int centerX = plusBounds.x + plusBounds.width / 2;
            int centerY = plusBounds.y + plusBounds.height / 2;

            g2.drawLine(centerX - 5, centerY, centerX + 5, centerY);
            g2.drawLine(centerX, centerY - 5, centerX, centerY + 5);
        } finally {
            g2.dispose();
        }
    }

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

    private void triggerTableAction(String actionName) {
        Action action = table.getActionMap().get(actionName);

        if (action != null) {
            action.actionPerformed(
                new ActionEvent(table, ActionEvent.ACTION_PERFORMED, actionName)
            );
        }
    }
}

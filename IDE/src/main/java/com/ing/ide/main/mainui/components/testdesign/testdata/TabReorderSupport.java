package com.ing.ide.main.mainui.components.testdesign.testdata;

import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.function.IntPredicate;
import javax.swing.Icon;
import javax.swing.JTabbedPane;

/**
 * Adds drag-and-drop tab reordering support for JTabbedPane.
 */
final class TabReorderSupport extends MouseAdapter implements MouseMotionListener {
    private static final double SWAP_THRESHOLD_RATIO = 0.35;

    private final JTabbedPane tabbedPane;
    private final IntPredicate isTabReorderable;
    private final Runnable onReorder;

    private int draggingIndex = -1;
    private boolean reordered;

    private TabReorderSupport(
        JTabbedPane tabbedPane,
        IntPredicate isTabReorderable,
        Runnable onReorder
    ) {
        this.tabbedPane = tabbedPane;
        this.isTabReorderable = isTabReorderable;
        this.onReorder = onReorder;
    }

    static void install(JTabbedPane tabbedPane, IntPredicate isTabReorderable) {
        install(tabbedPane, isTabReorderable, () -> {});
    }

    static void install(JTabbedPane tabbedPane, IntPredicate isTabReorderable, Runnable onReorder) {
        TabReorderSupport support = new TabReorderSupport(tabbedPane, isTabReorderable, onReorder);
        tabbedPane.addMouseListener(support);
        tabbedPane.addMouseMotionListener(support);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        int index = tabbedPane.indexAtLocation(e.getX(), e.getY());
        draggingIndex = isReorderable(index) ? index : -1;
        reordered = false;
    }

    /**
     * Handles drag-based tab reordering using proximity and threshold checks.
     *
     * <p>When dragging enters/exits tab bounds, this method resolves a valid reorder target,
     * enforces reorderability constraints, and swaps tabs only after crossing a directional
     * threshold to prevent jitter.</p>
     *
     * @param e mouse drag event
     */
    @Override
    public void mouseDragged(MouseEvent e) {
        if (draggingIndex == -1) {
            return;
        }

        int targetIndex = tabbedPane.indexAtLocation(e.getX(), e.getY());

        // If mouse is outside tab bounds, determine target based on position
        if (targetIndex == -1) {
            // Get the dragged tab's bounds
            int draggedTabX = tabbedPane.getBoundsAt(draggingIndex).x;
            int draggedTabRight = draggedTabX + tabbedPane.getBoundsAt(draggingIndex).width;

            if (e.getX() > draggedTabRight) {
                // Dragging to the right - move to the next reorderable tab
                targetIndex = draggingIndex + 1;
                while (targetIndex < tabbedPane.getTabCount() && !isReorderable(targetIndex)) {
                    targetIndex++;
                }
                if (targetIndex >= tabbedPane.getTabCount()) {
                    return;
                }
            } else if (e.getX() < draggedTabX) {
                // Dragging to the left - move to the previous reorderable tab
                targetIndex = draggingIndex - 1;
                while (targetIndex >= 0 && !isReorderable(targetIndex)) {
                    targetIndex--;
                }
                if (targetIndex < 0) {
                    return;
                }
            } else {
                return;
            }
        }

        if (!isReorderable(targetIndex) || targetIndex == draggingIndex) {
            return;
        }

        if (!hasCrossedSwapThreshold(draggingIndex, targetIndex, e.getX())) {
            return;
        }

        moveTab(draggingIndex, targetIndex);
        draggingIndex = targetIndex;
        reordered = true;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (reordered) {
            onReorder.run();
        }
        draggingIndex = -1;
        reordered = false;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // no-op
    }

    private boolean isReorderable(int index) {
        return index >= 0 && index < tabbedPane.getTabCount() && isTabReorderable.test(index);
    }

    /**
     * Returns whether the mouse has crossed the swap threshold inside the target tab.
     *
     * <p>The threshold avoids accidental tab swaps by requiring the cursor to move a minimum
     * distance into the neighboring tab before reordering is triggered.</p>
     *
     * @param fromIndex the currently dragged tab index
     * @param toIndex the target tab index under/near the cursor
     * @param mouseX the current mouse x-coordinate in tabbed pane space
     * @return {@code true} when the cursor has crossed the directional swap threshold
     */
    private boolean hasCrossedSwapThreshold(int fromIndex, int toIndex, int mouseX) {
        Rectangle targetBounds = tabbedPane.getBoundsAt(toIndex);
        int rightMoveThreshold =
            targetBounds.x + (int) Math.round(targetBounds.width * SWAP_THRESHOLD_RATIO);
        int leftMoveThreshold =
            targetBounds.x + (int) Math.round(targetBounds.width * (1.0 - SWAP_THRESHOLD_RATIO));

        if (toIndex > fromIndex) {
            return mouseX > rightMoveThreshold;
        }
        if (toIndex < fromIndex) {
            return mouseX < leftMoveThreshold;
        }
        return false;
    }

    /**
     * Moves a tab from one index to another while preserving tab metadata and selection state.
     *
     * <p>This method keeps title, icons, tooltip, enabled state, mnemonic settings, colors,
     * custom tab component and selected state intact after insertion at the new position.</p>
     *
     * @param fromIndex source tab index
     * @param toIndex destination tab index
     */
    private void moveTab(int fromIndex, int toIndex) {
        if (fromIndex == toIndex) {
            return;
        }

        Component tabComponent = tabbedPane.getTabComponentAt(fromIndex);
        Component content = tabbedPane.getComponentAt(fromIndex);
        String title = tabbedPane.getTitleAt(fromIndex);
        Icon icon = tabbedPane.getIconAt(fromIndex);
        Icon disabledIcon = tabbedPane.getDisabledIconAt(fromIndex);
        String tooltip = tabbedPane.getToolTipTextAt(fromIndex);
        boolean enabled = tabbedPane.isEnabledAt(fromIndex);
        int mnemonic = tabbedPane.getMnemonicAt(fromIndex);
        int displayedMnemonicIndex = tabbedPane.getDisplayedMnemonicIndexAt(fromIndex);
        Color foreground = tabbedPane.getForegroundAt(fromIndex);
        Color background = tabbedPane.getBackgroundAt(fromIndex);
        boolean selected = tabbedPane.getSelectedIndex() == fromIndex;

        tabbedPane.removeTabAt(fromIndex);
        int insertIndex = toIndex;

        tabbedPane.insertTab(title, icon, content, tooltip, insertIndex);
        tabbedPane.setTabComponentAt(insertIndex, tabComponent);
        tabbedPane.setDisabledIconAt(insertIndex, disabledIcon);
        tabbedPane.setEnabledAt(insertIndex, enabled);
        tabbedPane.setMnemonicAt(insertIndex, mnemonic);
        if (displayedMnemonicIndex >= 0) {
            tabbedPane.setDisplayedMnemonicIndexAt(insertIndex, displayedMnemonicIndex);
        }
        tabbedPane.setForegroundAt(insertIndex, foreground);
        tabbedPane.setBackgroundAt(insertIndex, background);

        if (selected) {
            tabbedPane.setSelectedIndex(insertIndex);
        }
    }
}

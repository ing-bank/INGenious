
package com.ing.ide.main.mainui.components.testdesign.testdata;

import com.formdev.flatlaf.ui.FlatTabbedPaneUI;
import java.awt.Graphics;
import java.awt.Rectangle;
import javax.swing.JTabbedPane;

public class XJTabbedPane extends JTabbedPane {

    private boolean showTabsHeader = false;

    public XJTabbedPane() {
        setUI(new MyTabbedPaneUI());
    }

    public void setShowTabsHeader(boolean showTabsHeader) {
        this.showTabsHeader = showTabsHeader;
        // Refresh the UI when visibility changes
        revalidate();
        repaint();
    }

    public boolean isShowTabsHeader() {
        return showTabsHeader;
    }

    /**
     * Custom FlatLaf-based UI that supports hiding the tab area.
     * Extends FlatTabbedPaneUI to maintain proper theme support.
     */
    private class MyTabbedPaneUI extends FlatTabbedPaneUI {

        @Override
        protected int calculateTabAreaHeight(
                int tabPlacement, int horizRunCount, int maxTabHeight) {
            if (isShowTabsHeader()) {
                return super.calculateTabAreaHeight(
                        tabPlacement, horizRunCount, maxTabHeight);
            } else {
                return 0;
            }
        }

        @Override
        protected void paintTab(
                Graphics g, int tabPlacement, Rectangle[] rects, int tabIndex,
                Rectangle iconRect, Rectangle textRect) {
            if (isShowTabsHeader()) {
                super.paintTab(
                        g, tabPlacement, rects, tabIndex, iconRect, textRect);
            }
        }

        @Override
        protected void paintContentBorder(
                Graphics g, int tabPlacement, int selectedIndex) {
            if (isShowTabsHeader()) {
                super.paintContentBorder(g, tabPlacement, selectedIndex);
            }
        }

        @Override
        public int tabForCoordinate(JTabbedPane pane, int x, int y) {
            if (isShowTabsHeader()) {
                return super.tabForCoordinate(pane, x, y);
            } else {
                return -1;
            }
        }
    }
}

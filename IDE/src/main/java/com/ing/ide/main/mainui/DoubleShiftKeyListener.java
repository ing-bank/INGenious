package com.ing.ide.main.mainui;

import java.awt.KeyEventDispatcher;
import java.awt.event.KeyEvent;

public class DoubleShiftKeyListener implements KeyEventDispatcher {

    private final AppMainFrame mainFrame;
    private long lastShiftPressTime = 0;
    private static final long DOUBLE_PRESS_INTERVAL = 500;

    public DoubleShiftKeyListener(AppMainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (e.getID() == KeyEvent.KEY_PRESSED &&
                (e.getKeyCode() == KeyEvent.VK_SHIFT)) {

            long currentTime = System.currentTimeMillis();
            long timeSinceLastPress = currentTime - lastShiftPressTime;

            if (timeSinceLastPress < DOUBLE_PRESS_INTERVAL && timeSinceLastPress > 0) {
                mainFrame.showGlobalSearch();
                lastShiftPressTime = 0;
            } else {
                lastShiftPressTime = currentTime;
            }
        }

        return false;
    }
}

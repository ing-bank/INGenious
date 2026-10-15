package com.ing.ide.main.mobilerecorder;

import org.openqa.selenium.Dimension;

/**
 * Converts a point on the raw device screenshot (in screenshot pixels) into the
 * device's logical/window coordinate space, which is what page-source element
 * bounds are expressed in.
 *
 * <p>On Android these two spaces are normally identical (scale 1). On
 * iOS/Retina devices the screenshot is often 2x or 3x the logical point size, so
 * a scale factor is derived once per session from the window size.
 */
public final class CoordinateMapper {
    private double scaleFactor = 1.0;
    private int windowWidth;
    private int windowHeight;
    private boolean initialized;

    /** Must be called once after a session starts, before mapping any points. */
    public synchronized void initialize(
        Dimension window,
        int screenshotPixelWidth,
        int screenshotPixelHeight
    ) {
        this.windowWidth = window.getWidth();
        this.windowHeight = window.getHeight();
        if (windowWidth > 0 && windowHeight > 0) {
            double scaleX = (double) screenshotPixelWidth / windowWidth;
            double scaleY = (double) screenshotPixelHeight / windowHeight;
            this.scaleFactor = (scaleX + scaleY) / 2.0;
        } else {
            this.scaleFactor = 1.0;
        }
        if (this.scaleFactor <= 0) {
            this.scaleFactor = 1.0;
        }
        this.initialized = true;
    }

    public synchronized boolean isInitialized() {
        return initialized;
    }

    /** Maps a point in raw screenshot pixels to device logical/window coordinates. */
    public synchronized DevicePoint toDevicePoint(int screenshotPixelX, int screenshotPixelY) {
        int deviceX = (int) Math.round(screenshotPixelX / scaleFactor);
        int deviceY = (int) Math.round(screenshotPixelY / scaleFactor);
        deviceX = clamp(deviceX, 0, Math.max(0, windowWidth - 1));
        deviceY = clamp(deviceY, 0, Math.max(0, windowHeight - 1));
        return new DevicePoint(deviceX, deviceY);
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    /** A point in device logical/window coordinates. */
    public static final class DevicePoint {
        private final int x;
        private final int y;

        public DevicePoint(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int x() {
            return x;
        }

        public int y() {
            return y;
        }
    }
}

package com.ing.ide.e2e.support;

import java.awt.AWTException;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.assertj.swing.edt.GuiActionRunner;

/**
 * Captures images of the IDE and draws annotations on them.
 * <ul>
 *   <li>{@link InputMode#ROBOT}: the whole desktop, as the user sees it.</li>
 *   <li>{@link InputMode#DIRECT}: the IDE's own windows painted at their screen positions. No
 *   screen-capture permission needed, and other apps never appear. Native title bars are not painted
 *   by Swing, so the window title is drawn in their place.</li>
 * </ul>
 */
final class ScreenCapture {
    static final Color PASS_COLOR = new Color(46, 125, 50);
    static final Color FAIL_COLOR = new Color(198, 40, 40);
    static final Color STEP_COLOR = new Color(75, 0, 130);

    private static final Color CLICK_COLOR = new Color(255, 98, 0);
    private static final int CLICK_RADIUS = 18;
    // Classic arrow cursor, tip at (0, 0)
    private static final int[] POINTER_X = { 0, 0, 4, 7, 10, 7, 12 };
    private static final int[] POINTER_Y = { 0, 17, 13, 20, 19, 12, 12 };
    private static final Color TITLE_BAR_COLOR = new Color(236, 236, 236);
    private static final Color EMPTY_SCREEN_COLOR = new Color(48, 48, 48);
    private static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 14);
    private static final Font TITLE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 13);
    private static final int BOX_PADDING = 3;
    private static final int BOX_STROKE = 3;
    private static final int LABEL_PADDING = 8;

    private static Robot desktop;

    private ScreenCapture() {}

    /**
     * An image and the screen position of its top-left corner.
     */
    static final class Shot {
        final BufferedImage image;
        final Point origin;

        Shot(final BufferedImage image, final Point origin) {
            this.image = image;
            this.origin = origin;
        }
    }

    /**
     * @param mode input mode
     * @return the desktop (robot) or the area covered by the IDE windows (direct)
     */
    static Shot windows(final InputMode mode) {
        if (mode == InputMode.ROBOT) {
            return desktop();
        }
        return GuiActionRunner.execute(
            () -> {
                final List<Window> windows = showingWindows();
                if (windows.isEmpty()) {
                    return new Shot(
                        new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB),
                        new Point(0, 0)
                    );
                }
                final Rectangle area = windows
                    .stream()
                    .map(ScreenCapture::screenBounds)
                    .reduce(Rectangle::union)
                    .orElseThrow();
                return paint(windows, area, null);
            }
        );
    }

    /**
     * Captures a fixed, screen-sized frame so every frame of a recording has the same size.
     *
     * @param mode input mode
     * @return the desktop (robot) or the IDE windows on a neutral screen-sized background (direct)
     */
    static Shot screen(final InputMode mode) {
        if (mode == InputMode.ROBOT) {
            return desktop();
        }
        final Rectangle screen = defaultScreenBounds();
        return GuiActionRunner.execute(() -> paint(showingWindows(), screen, EMPTY_SCREEN_COLOR));
    }

    /**
     * Boxes a target and captions it with PASSED or FAILED.
     *
     * @param shot the image to draw on
     * @param target the checked area and its description
     * @param passed whether the check passed
     */
    static void annotate(final Shot shot, final Highlight target, final boolean passed) {
        final Color color = passed ? PASS_COLOR : FAIL_COLOR;
        final Rectangle box = target.screenBounds();
        box.translate(-shot.origin.x, -shot.origin.y);
        box.grow(BOX_PADDING, BOX_PADDING);
        // Keep the whole outline visible when the target touches the image edge (e.g. a title bar)
        final int inset = BOX_STROKE / 2 + 1;
        box.setBounds(
            box.intersection(
                new Rectangle(
                    inset,
                    inset,
                    shot.image.getWidth() - 2 * inset,
                    shot.image.getHeight() - 2 * inset
                )
            )
        );
        final Graphics2D g = graphics(shot.image);
        try {
            if (box.width > 2 * BOX_PADDING && box.height > 2 * BOX_PADDING) {
                g.setColor(color);
                g.setStroke(new BasicStroke(BOX_STROKE));
                g.drawRoundRect(box.x, box.y, box.width, box.height, 8, 8);
            }
            drawLabel(
                g,
                shot.image,
                box,
                (passed ? "PASSED: " : "FAILED: ") + target.label(),
                color
            );
        } finally {
            g.dispose();
        }
    }

    /**
     * Draws a caption in the bottom-left corner, e.g. the current step of a recording.
     *
     * @param shot the image to draw on
     * @param text the caption
     * @param color background colour of the caption
     */
    static void caption(final Shot shot, final String text, final Color color) {
        final Graphics2D g = graphics(shot.image);
        try {
            g.setFont(LABEL_FONT);
            final int height = g.getFontMetrics().getHeight() + LABEL_PADDING;
            final Rectangle anchor = new Rectangle(
                LABEL_PADDING,
                shot.image.getHeight() - height - LABEL_PADDING,
                0,
                0
            );
            drawLabel(g, shot.image, anchor, text, color);
        } finally {
            g.dispose();
        }
    }

    /**
     * Where to draw the pointer. OS screen captures never contain the cursor, and direct mode has no
     * real cursor at all, so the pointer is always drawn by the tests.
     *
     * @param mode input mode
     * @param lastActionPoint where the last user action happened, or {@code null}
     * @return the real cursor (robot) or the last action point (direct); {@code null} if unknown
     */
    static Point currentPointer(final InputMode mode, final Point lastActionPoint) {
        if (mode == InputMode.ROBOT) {
            final PointerInfo info = MouseInfo.getPointerInfo();
            return info == null ? null : info.getLocation();
        }
        return lastActionPoint;
    }

    /**
     * Draws a mouse pointer, and a click ripple around it when {@code clicked}.
     *
     * @param shot the image to draw on
     * @param screenPoint pointer position in screen coordinates
     * @param clicked whether to mark a click
     */
    static void pointer(final Shot shot, final Point screenPoint, final boolean clicked) {
        final int x = screenPoint.x - shot.origin.x;
        final int y = screenPoint.y - shot.origin.y;
        final Graphics2D g = graphics(shot.image);
        try {
            if (clicked) {
                g.setColor(
                    new Color(
                        CLICK_COLOR.getRed(),
                        CLICK_COLOR.getGreen(),
                        CLICK_COLOR.getBlue(),
                        70
                    )
                );
                g.fillOval(x - CLICK_RADIUS, y - CLICK_RADIUS, 2 * CLICK_RADIUS, 2 * CLICK_RADIUS);
                g.setColor(CLICK_COLOR);
                g.setStroke(new BasicStroke(BOX_STROKE));
                g.drawOval(x - CLICK_RADIUS, y - CLICK_RADIUS, 2 * CLICK_RADIUS, 2 * CLICK_RADIUS);
            }
            g.translate(x, y);
            g.setColor(Color.BLACK);
            g.fillPolygon(POINTER_X, POINTER_Y, POINTER_X.length);
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(1.5f));
            g.drawPolygon(POINTER_X, POINTER_Y, POINTER_X.length);
        } finally {
            g.dispose();
        }
    }

    // Plain java.awt.Robot capture, off the EDT. AssertJ's ScreenshotTaker is not used: it captures
    // on the EDT and waits for an idle UI twice per shot, which made recordings take ~2 s per frame.
    private static synchronized Shot desktop() {
        if (desktop == null) {
            try {
                desktop = new Robot();
            } catch (AWTException e) {
                throw new IllegalStateException("Cannot capture the screen", e);
            }
        }
        final Rectangle screen = defaultScreenBounds();
        return new Shot(desktop.createScreenCapture(screen), screen.getLocation());
    }

    private static Rectangle defaultScreenBounds() {
        return GraphicsEnvironment
            .getLocalGraphicsEnvironment()
            .getDefaultScreenDevice()
            .getDefaultConfiguration()
            .getBounds();
    }

    // Must run on the EDT. Frames are painted before dialogs so dialogs end up on top.
    private static List<Window> showingWindows() {
        return Arrays
            .stream(Window.getWindows())
            .filter(Window::isShowing)
            .filter(w -> w.getWidth() > 0 && w.getHeight() > 0)
            .sorted(Comparator.comparing(w -> w instanceof Dialog))
            .collect(Collectors.toList());
    }

    private static Shot paint(
        final List<Window> windows,
        final Rectangle area,
        final Color background
    ) {
        final BufferedImage image = new BufferedImage(
            area.width,
            area.height,
            BufferedImage.TYPE_INT_ARGB
        );
        final Graphics2D g = image.createGraphics();
        try {
            if (background != null) {
                g.setColor(background);
                g.fillRect(0, 0, area.width, area.height);
            }
            for (final Window window : windows) {
                final Rectangle bounds = screenBounds(window);
                final Graphics2D wg = (Graphics2D) g.create(
                    bounds.x - area.x,
                    bounds.y - area.y,
                    bounds.width,
                    bounds.height
                );
                try {
                    window.paintAll(wg);
                    paintTitle(wg, window);
                } finally {
                    wg.dispose();
                }
            }
        } finally {
            g.dispose();
        }
        return new Shot(image, area.getLocation());
    }

    private static void paintTitle(final Graphics2D g, final Window window) {
        final String title = window instanceof Frame
            ? ((Frame) window).getTitle()
            : window instanceof Dialog ? ((Dialog) window).getTitle() : null;
        final int height = window.getInsets().top;
        if (title == null || title.isEmpty() || height <= 0) {
            return;
        }
        g.setColor(TITLE_BAR_COLOR);
        g.fillRect(0, 0, window.getWidth(), height);
        g.setRenderingHint(
            RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON
        );
        g.setFont(TITLE_FONT);
        g.setColor(Color.DARK_GRAY);
        final FontMetrics metrics = g.getFontMetrics();
        g.drawString(
            title,
            (window.getWidth() - metrics.stringWidth(title)) / 2,
            (height - metrics.getHeight()) / 2 + metrics.getAscent()
        );
    }

    // Places the caption below the anchor, or above it when there is no room below.
    private static void drawLabel(
        final Graphics2D g,
        final BufferedImage image,
        final Rectangle anchor,
        final String text,
        final Color color
    ) {
        g.setFont(LABEL_FONT);
        final FontMetrics metrics = g.getFontMetrics();
        final int width = metrics.stringWidth(text) + 2 * LABEL_PADDING;
        final int height = metrics.getHeight() + LABEL_PADDING;
        int y = anchor.y + anchor.height + (anchor.height > 0 ? BOX_STROKE : 0);
        if (y + height > image.getHeight()) {
            y = Math.max(0, anchor.y - height - BOX_STROKE);
        }
        final int x = Math.max(0, Math.min(anchor.x, image.getWidth() - width));
        g.setColor(color);
        g.fillRoundRect(x, y, width, height, 6, 6);
        g.setColor(Color.WHITE);
        g.drawString(text, x + LABEL_PADDING, y + LABEL_PADDING / 2 + metrics.getAscent());
    }

    private static Graphics2D graphics(final BufferedImage image) {
        final Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(
            RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON
        );
        return g;
    }

    private static Rectangle screenBounds(final Window window) {
        final Point location = window.getLocationOnScreen();
        return new Rectangle(location.x, location.y, window.getWidth(), window.getHeight());
    }
}

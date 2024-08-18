
package com.ing.ide.util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Icon;
import javax.swing.ImageIcon;
//import org.sikuli.script.Region;

public class Canvas {

    private final List<Object> boxes;
    private static final BasicStroke STROKE_OFFSET = new BasicStroke(1.3f);
    public static EmptyIcon EmptyIcon = new EmptyIcon();

    public Canvas() {
        boxes = new ArrayList<>();
    }

    public Canvas addBox(Rectangle r) {
        boxes.add(null);
        return this;
    }

    public void display(final float sec) {
        for (final Object box : boxes) {
            Thread th = new Thread("UI:CanvasHighlightBox") {
                @Override
                public void run() {
                   // box.highlight(sec);
                }
            };
            th.start();
        }
    }

    public void clear() {
        boxes.clear();
    }

    public static Image iconToImage(Icon icon) {
        if (icon instanceof ImageIcon) {
            return ((ImageIcon) icon).getImage();
        } else {
            int w = icon.getIconWidth();
            int h = icon.getIconHeight();
            GraphicsEnvironment ge
                    = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice gd = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gd.getDefaultConfiguration();
            BufferedImage image = gc.createCompatibleImage(w, h);
            Graphics2D g = image.createGraphics();
            icon.paintIcon(null, g, 0, 0);
            g.dispose();
            return image;
        }
    }

    public static void paintOffset(Graphics2D g2d, Point p) {

        g2d.setColor(Color.red);
        int r = 4, x = p.x, y = p.y, dz = 4;
        g2d.setStroke(STROKE_OFFSET);
        g2d.drawLine(x, y - (r + dz), x, y + (r + dz));// --
        g2d.drawLine(x - (r + dz), y, x + (r + dz), y);// |
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setStroke(STROKE_OFFSET);
        g2d.drawOval(x - r, y - r, 2 * r, 2 * r);
        g2d.dispose();

    }

    public static class EmptyIcon implements Icon {

        private final int width = 10, height = 10;

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
        }

        @Override
        public int getIconWidth() {
            return width;
        }

        @Override
        public int getIconHeight() {
            return height;
        }

    }

    public static class Window {

        private static final Dimension D = Toolkit.getDefaultToolkit().getScreenSize();
        public static final int W = D.width, H = D.height;
        public static BufferedImage Screen = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        public static Dimension screenCenter = new Dimension(D.width / 2, D.height / 2);
        public static Rectangle screen = new Rectangle(0, 0, D.width, D.height);
        public static Point winStart;

        static {
            if (SystemInfo.isWindows()) {
                winStart = new Point(0, 0);
            } else {
                winStart = new Point(0, 23);
            }
        }
    }
}

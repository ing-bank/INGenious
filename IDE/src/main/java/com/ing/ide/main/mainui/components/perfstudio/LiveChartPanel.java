package com.ing.ide.main.mainui.components.perfstudio;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.JPanel;
import javax.swing.UIManager;

/**
 * Minimal dependency-free live line chart for the Performance Studio:
 * multiple named series, rolling window, auto-scaled Y axis, theme-aware
 * colors. Feed it with {@link #addPoint(String, double)} + {@link #tick()}
 * from the polling timer.
 */
public class LiveChartPanel extends JPanel {
    private static final Color[] PALETTE = {
        new Color(0x4E9AF1),
        new Color(0xF1734E),
        new Color(0x53C987),
        new Color(0xC953B4)
    };

    private final String title;
    private final int maxPoints;
    private final Map<String, List<Double>> series = new LinkedHashMap<>();

    public LiveChartPanel(String title, int maxPoints) {
        this.title = title;
        this.maxPoints = maxPoints;
        setOpaque(true);
    }

    /** Record the current value for a series (call once per tick per series). */
    public synchronized void addPoint(String name, double value) {
        List<Double> values = series.computeIfAbsent(name, k -> new ArrayList<Double>());
        values.add(Double.valueOf(value));
        while (values.size() > maxPoints) {
            values.remove(0);
        }
    }

    /** Repaint after a polling round. */
    public void tick() {
        repaint();
    }

    /** Drop all data (new run). */
    public synchronized void reset() {
        series.clear();
        repaint();
    }

    @Override
    protected synchronized void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        Color bg = UIManager.getColor("Panel.background");
        Color fg = UIManager.getColor("Label.foreground");
        if (bg == null) {
            bg = Color.WHITE;
        }
        if (fg == null) {
            fg = Color.DARK_GRAY;
        }
        g2.setColor(bg);
        g2.fillRect(0, 0, w, h);

        int padLeft = 46;
        int padRight = 8;
        int padTop = 22;
        int padBottom = 16;
        int plotW = Math.max(1, w - padLeft - padRight);
        int plotH = Math.max(1, h - padTop - padBottom);

        // title + legend
        g2.setFont(getFont().deriveFont(Font.BOLD, 11f));
        g2.setColor(fg);
        g2.drawString(title, padLeft, 14);
        int legendX = padLeft + g2.getFontMetrics().stringWidth(title) + 16;
        int colorIndex = 0;
        g2.setFont(getFont().deriveFont(10f));
        for (String name : series.keySet()) {
            g2.setColor(PALETTE[colorIndex % PALETTE.length]);
            g2.fillRect(legendX, 7, 8, 8);
            g2.setColor(fg);
            g2.drawString(name, legendX + 11, 14);
            legendX += 11 + g2.getFontMetrics().stringWidth(name) + 14;
            colorIndex++;
        }

        // scale
        double max = 0;
        for (List<Double> values : series.values()) {
            for (Double v : values) {
                if (v.doubleValue() > max) {
                    max = v.doubleValue();
                }
            }
        }
        if (max <= 0) {
            max = 1;
        }
        max = max * 1.15; // headroom

        // grid + axis labels
        g2.setColor(new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 40));
        for (int i = 0; i <= 4; i++) {
            int y = padTop + (plotH * i) / 4;
            g2.drawLine(padLeft, y, w - padRight, y);
        }
        g2.setColor(fg);
        for (int i = 0; i <= 4; i++) {
            int y = padTop + (plotH * i) / 4;
            double value = max * (4 - i) / 4;
            g2.drawString(compact(value), 4, y + 4);
        }

        // series
        colorIndex = 0;
        for (Map.Entry<String, List<Double>> e : series.entrySet()) {
            List<Double> values = e.getValue();
            g2.setColor(PALETTE[colorIndex % PALETTE.length]);
            g2.setStroke(new BasicStroke(1.6f));
            int n = values.size();
            if (n >= 2) {
                int denominator = Math.max(1, maxPoints - 1);
                int prevX = 0;
                int prevY = 0;
                for (int i = 0; i < n; i++) {
                    int x = padLeft + (plotW * i) / denominator;
                    int y =
                        padTop +
                        plotH -
                        (int) Math.round(plotH * (values.get(i).doubleValue() / max));
                    if (i > 0) {
                        g2.drawLine(prevX, prevY, x, y);
                    }
                    prevX = x;
                    prevY = y;
                }
            }
            colorIndex++;
        }
        g2.dispose();
    }

    private static String compact(double v) {
        if (v >= 1000000) {
            return String.format(Locale.ROOT, "%.1fM", v / 1000000);
        }
        if (v >= 1000) {
            return String.format(Locale.ROOT, "%.1fk", v / 1000);
        }
        if (v >= 10) {
            return String.format(Locale.ROOT, "%.0f", v);
        }
        return String.format(Locale.ROOT, "%.1f", v);
    }
}

package com.ing.ide.main.mainui.components.perfstudio;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JLabel;

/**
 * Rounded, color-coded status badge for the Performance Studio:
 * RUNNING (blue), DRAINING (amber), PASSED/OK (green), FAILED (red),
 * neutral for everything else.
 */
public class StatusPill extends JLabel {

    /** Semantic status kinds mapped to pill colors. */
    public enum Kind {
        NEUTRAL(new Color(0x9E9E9E)),
        RUNNING(new Color(0x1E88E5)),
        DRAINING(new Color(0xF9A825)),
        OK(new Color(0x43A047)),
        FAILED(new Color(0xE53935));

        final Color color;

        Kind(Color color) {
            this.color = color;
        }
    }

    private Kind kind = Kind.NEUTRAL;

    public StatusPill() {
        super("Idle");
        setOpaque(false);
        setForeground(java.awt.Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(3, 12, 3, 12));
        setFont(getFont().deriveFont(java.awt.Font.BOLD, 11f));
    }

    public void setStatus(String text, Kind kind) {
        this.kind = kind;
        setText(text);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int h = getHeight();
        g2.setColor(kind.color);
        g2.fillRoundRect(0, 0, getWidth(), h, h, h);
        g2.dispose();
        super.paintComponent(g);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        d.height = Math.max(d.height, 22);
        return d;
    }
}

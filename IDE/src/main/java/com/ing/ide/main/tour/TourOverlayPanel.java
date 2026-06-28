package com.ing.ide.main.tour;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JButton;
import javax.swing.JPanel;

/**
 * A transparent full-frame JPanel overlay that renders the tour UI.
 * <p>
 * Added to the main frame's {@link javax.swing.JLayeredPane} at
 * {@link javax.swing.JLayeredPane#POPUP_LAYER} so it appears above all other
 * Swing content. Paints:
 * <ul>
 *   <li>A semi-transparent dark veil over the whole frame</li>
 *   <li>An optional rounded spotlight cutout that reveals a target component</li>
 *   <li>A branded callout card showing the step title, description and navigation</li>
 * </ul>
 * All mouse events are consumed so users cannot accidentally interact with
 * underlying components during the tour.
 */
public class TourOverlayPanel extends JPanel {
    // ── ING Brand Palette ──
    private static final Color ING_ORANGE = new Color(0xFF6200);
    private static final Color ING_BURGUNDY = new Color(0x4D0020);
    private static final Color OVERLAY_COLOR = new Color(0, 0, 0, 175);

    // ── Card Dimensions ──
    private static final int CARD_W = 440;
    private static final int CARD_H = 230;
    private static final int CARD_ARC = 20;
    private static final int ACCENT_H = 6;
    private static final int CARD_PAD = 22;
    private static final int BTN_W = 110;
    private static final int BTN_H = 36;

    // ── Step state ──
    private String title = "";
    private String description = "";
    private int currentStep = 0;
    private int totalSteps = 1;
    private Rectangle spotlight = null;
    private boolean isDarkMode = false;

    // ── Callbacks ──
    private Runnable onNext;
    private Runnable onPrev;
    private Runnable onSkip;

    // ── Buttons ──
    private final JButton prevBtn;
    private final JButton nextBtn;
    private final JButton skipBtn;

    public TourOverlayPanel() {
        setOpaque(false);
        setLayout(null);
        setFocusable(true);

        prevBtn = makeButton("\u2190 Back", false);
        nextBtn = makeButton("Next \u2192", true);
        skipBtn = makeButton("Skip Tour", false);

        add(prevBtn);
        add(nextBtn);
        add(skipBtn);

        prevBtn.addActionListener(
            e -> {
                if (onPrev != null) onPrev.run();
            }
        );
        nextBtn.addActionListener(
            e -> {
                if (onNext != null) onNext.run();
            }
        );
        skipBtn.addActionListener(
            e -> {
                if (onSkip != null) onSkip.run();
            }
        );

        // Consume all mouse events on the overlay background so underlying
        // components are not accidentally triggered during the tour.
        addMouseListener(new MouseAdapter() {});
        addMouseMotionListener(new MouseMotionAdapter() {});

        // Keyboard shortcuts: Esc = skip, → = next, ← = prev
        addKeyListener(
            new KeyAdapter() {

                @Override
                public void keyPressed(KeyEvent e) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_ESCAPE:
                            if (onSkip != null) onSkip.run();
                            break;
                        case KeyEvent.VK_RIGHT:
                        case KeyEvent.VK_ENTER:
                            if (onNext != null) onNext.run();
                            break;
                        case KeyEvent.VK_LEFT:
                            if (onPrev != null) onPrev.run();
                            break;
                        default:
                            break;
                    }
                }
            }
        );
    }

    private JButton makeButton(String text, boolean primary) {
        JButton btn = new JButton(text);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (primary) {
            btn.setBackground(ING_ORANGE);
            btn.setForeground(Color.WHITE);
        } else {
            btn.setBackground(new Color(180, 180, 185));
            btn.setForeground(new Color(50, 50, 60));
        }
        return btn;
    }

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Updates all display state for the current step and repaints.
     *
     * @param step        the step data to show
     * @param current     0-based step index
     * @param total       total number of steps
     * @param spotRect    rectangle to spotlight, or {@code null} for no spotlight
     * @param dark        whether dark mode is active
     */
    public void update(TourStep step, int current, int total, Rectangle spotRect, boolean dark) {
        this.title = step.getTitle();
        this.description = step.getDescription();
        this.currentStep = current;
        this.totalSteps = total;
        this.spotlight = spotRect;
        this.isDarkMode = dark;

        prevBtn.setVisible(current > 0);

        boolean isLast = (current == total - 1);
        nextBtn.setText(isLast ? "Finish \u2713" : "Next \u2192");
        if (isLast) {
            nextBtn.setBackground(new Color(0x349651)); // green for finish
        } else {
            nextBtn.setBackground(ING_ORANGE);
        }
        skipBtn.setVisible(!isLast);

        positionButtons();
        requestFocusInWindow();
        repaint();
    }

    // ── Layout ─────────────────────────────────────────────────────────────

    @Override
    public void doLayout() {
        super.doLayout();
        positionButtons();
    }

    private void positionButtons() {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        int[] xy = cardTopLeft(w, h);
        int cardX = xy[0];
        int cardY = xy[1];

        // Buttons sit at the bottom of the card, 14px from edge
        int btnY = cardY + CARD_H - BTN_H - 14;
        skipBtn.setBounds(cardX + CARD_PAD, btnY, BTN_W, BTN_H);
        nextBtn.setBounds(cardX + CARD_W - BTN_W - CARD_PAD, btnY, BTN_W, BTN_H);
        prevBtn.setBounds(cardX + CARD_W - BTN_W * 2 - CARD_PAD - 8, btnY, BTN_W, BTN_H);
    }

    /** Returns [x, y] of the top-left corner of the callout card. */
    private int[] cardTopLeft(int w, int h) {
        int cardX = (w - CARD_W) / 2;
        int cardY = (h - CARD_H) / 2;

        if (spotlight != null) {
            int spotBottom = spotlight.y + spotlight.height;
            int spotMidY = spotlight.y + spotlight.height / 2;
            if (spotMidY < h / 2) {
                // Spotlight in upper half → card below spotlight
                int desired = spotBottom + 36;
                cardY = Math.min(h - CARD_H - 16, desired);
            } else {
                // Spotlight in lower half → card above spotlight
                int desired = spotlight.y - CARD_H - 36;
                cardY = Math.max(16, desired);
            }
        }
        return new int[] { cardX, cardY };
    }

    // ── Painting ───────────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(
            RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB
        );

        int w = getWidth();
        int h = getHeight();

        drawOverlay(g2, w, h);
        drawCard(g2, w, h);

        g2.dispose();
    }

    private void drawOverlay(Graphics2D g2, int w, int h) {
        if (spotlight != null && spotlight.width > 0 && spotlight.height > 0) {
            // Paint dark overlay with a spotlight hole cut out
            int pad = 14;
            RoundRectangle2D spot = new RoundRectangle2D.Float(
                spotlight.x - pad,
                spotlight.y - pad,
                spotlight.width + pad * 2,
                spotlight.height + pad * 2,
                18,
                18
            );
            Area full = new Area(new Rectangle(0, 0, w, h));
            full.subtract(new Area(spot));
            g2.setColor(OVERLAY_COLOR);
            g2.fill(full);

            // Animated-feel orange glow border around spotlight
            g2.setColor(new Color(0xFF6200));
            g2.setStroke(new BasicStroke(2.5f));
            g2.draw(spot);

            // Subtle inner glow
            g2.setColor(new Color(0xFF6200, true));
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.15f));
            RoundRectangle2D innerGlow = new RoundRectangle2D.Float(
                spotlight.x - pad + 4,
                spotlight.y - pad + 4,
                spotlight.width + (pad - 4) * 2,
                spotlight.height + (pad - 4) * 2,
                14,
                14
            );
            g2.fill(innerGlow);
            g2.setComposite(AlphaComposite.SrcOver);
        } else {
            g2.setColor(OVERLAY_COLOR);
            g2.fillRect(0, 0, w, h);
        }
    }

    private void drawCard(Graphics2D g2, int w, int h) {
        int[] xy = cardTopLeft(w, h);
        int cx = xy[0];
        int cy = xy[1];

        // Drop shadow
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillRoundRect(cx + 5, cy + 8, CARD_W, CARD_H, CARD_ARC, CARD_ARC);

        // Card background
        Color bg = isDarkMode ? new Color(32, 28, 42) : Color.WHITE;
        g2.setColor(bg);
        g2.fillRoundRect(cx, cy, CARD_W, CARD_H, CARD_ARC, CARD_ARC);

        // Top accent bar
        g2.setColor(ING_ORANGE);
        g2.fillRoundRect(cx, cy, CARD_W, ACCENT_H + CARD_ARC, CARD_ARC, CARD_ARC);
        g2.fillRect(cx, cy + ACCENT_H, CARD_W, CARD_ARC);

        // Step counter pill (top-right of card)
        drawStepPill(g2, cx, cy);

        // Title
        Font titleFont = new Font("SansSerif", Font.BOLD, 16);
        g2.setFont(titleFont);
        g2.setColor(isDarkMode ? new Color(255, 200, 130) : ING_BURGUNDY);
        g2.drawString(title, cx + CARD_PAD, cy + ACCENT_H + 32);

        // Description (word-wrapped)
        Font descFont = new Font("SansSerif", Font.PLAIN, 13);
        g2.setFont(descFont);
        g2.setColor(isDarkMode ? new Color(215, 210, 228) : new Color(50, 50, 65));
        drawWrapped(g2, description, cx + CARD_PAD, cy + ACCENT_H + 58, CARD_W - CARD_PAD * 2);

        // Keyboard hint at bottom-left below buttons
        Font hintFont = new Font("SansSerif", Font.PLAIN, 10);
        g2.setFont(hintFont);
        g2.setColor(isDarkMode ? new Color(130, 125, 145) : new Color(150, 150, 165));
        g2.drawString(
            "  \u2190 \u2192 arrow keys  \u2022  Esc to skip",
            cx + CARD_PAD,
            cy + CARD_H - 7
        );
    }

    private void drawStepPill(Graphics2D g2, int cx, int cy) {
        String text = (currentStep + 1) + " / " + totalSteps;
        Font pillFont = new Font("SansSerif", Font.BOLD, 10);
        g2.setFont(pillFont);
        FontMetrics fm = g2.getFontMetrics();
        int pw = fm.stringWidth(text) + 16;
        int ph = 18;
        int px = cx + CARD_W - pw - 12;
        int py = cy + 10;
        g2.setColor(ING_BURGUNDY);
        g2.fillRoundRect(px, py, pw, ph, 9, 9);
        g2.setColor(Color.WHITE);
        g2.drawString(text, px + 8, py + 12);
    }

    private void drawWrapped(Graphics2D g2, String text, int x, int y, int maxW) {
        if (text == null || text.isEmpty()) return;
        FontMetrics fm = g2.getFontMetrics();
        int lineH = fm.getHeight() + 2;
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        int curY = y;
        for (String word : words) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (fm.stringWidth(candidate) > maxW && line.length() > 0) {
                g2.drawString(line.toString(), x, curY);
                curY += lineH;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (line.length() > 0) {
            g2.drawString(line.toString(), x, curY);
        }
    }

    // ── Callback Setters ───────────────────────────────────────────────────

    public void setOnNext(Runnable r) {
        this.onNext = r;
    }

    public void setOnPrev(Runnable r) {
        this.onPrev = r;
    }

    public void setOnSkip(Runnable r) {
        this.onSkip = r;
    }
}

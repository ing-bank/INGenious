
package com.ing.ide.main.shr.image.crop;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

/**
 *
 *
 */
public class CropPanel extends JPanel {

    Cropper sys;
    boolean cropping = false;
    private final Color rx = new Color(240, 236, 236, 120);
    Rectangle2D selection;

    public CropPanel(Cropper sys) {
        this.sys = sys;
        selection = new Rectangle2D.Double();
        init();
        initListener();

    }

    private void init() {
        setSize(sys.getSize());
        setDoubleBuffered(true);
        setOpaque(false);
    }

    private void initListener() {
        Action escape = new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                sys.hideCropper();
            }
        };
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ESCAPE"), "escape");
        getActionMap().put("escape", escape);
    }

    public void doCroping() {
        cropping = true;
        paintComponent(getGraphics());
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D g2d = (Graphics2D) g;
        drawImage(g2d);
        g2d.dispose();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics gx = getGraphics();
        Graphics2D g2d = (Graphics2D) gx;
        drawImage(g2d);
        if (cropping) {
            drawSelection(g2d);
            cropping = false;
        }
        g2d.dispose();
    }

    private void drawImage(Graphics2D g2d) {
        g2d.drawImage(sys.screen, 0, 0, null);
    }

    private void drawOverLay(Graphics2D g2d) {
        g2d.setColor(rx);
        g2d.fillRect(0, 0, getWidth(), getHeight());
    }

    private void drawSelection(Graphics2D g2d) {

        selection.setFrameFromDiagonal(sys.start, sys.end);
        Rectangle select = this.selection.getBounds();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_OFF);
        g2d.setStroke(new BasicStroke(1.8f));
        g2d.setColor(Color.RED);
        g2d.drawRect(select.x, select.y, select.width, select.height);

    }

    private BufferedImage getSubimage(Rectangle select) {
        if ((select.width > 0) && (select.height > 0)) {
            return sys.screen.getSubimage(select.x, select.y, select.width, select.height);
        }
        return null;
    }

}

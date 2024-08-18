
package com.ing.ide.util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import static javax.swing.JComponent.WHEN_FOCUSED;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/**
 *
 * 
 */
public class SelectionManager implements MouseListener, MouseMotionListener, KeyListener {

    private final JComponent com;
    private Point click, start, end;
    public boolean drag = false;
    private static final int MIN_SIZE = 40;
    private Action escape;
    public Rectangle2D selection;

    public SelectionManager(JComponent com) {
        this.com = com;
        init();
    }

    private void init() {
        start = new Point(0, 0);
        end = new Point(0, 0);
        click = new Point(0, 0);
        selection = new Rectangle2D.Double();
        escape = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                removeSelection();
            }
        };
        com.getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escape");
        com.getActionMap().put("escape", escape);
        com.setFocusable(true);
        com.requestFocus();

    }

    private void initListeners() {
        addListener();
    }

    public void addListener() {
        com.addMouseListener(this);
        com.addMouseMotionListener(this);
        com.addKeyListener(this);
    }

    public void setCoordinates(Rectangle rect) {
        selection.setRect(rect);
    }

    public void removeListener() {
        for (MouseListener ml : com.getMouseListeners()) {
            com.removeMouseListener(ml);
        }
        for (MouseMotionListener mml : com.getMouseMotionListeners()) {
            com.removeMouseMotionListener(mml);
        }
        for (KeyListener kl : com.getKeyListeners()) {
            com.removeKeyListener(kl);
        }
        reset();
        com.repaint();
    }

    public void reset() {
        selection.setRect(0, 0, 0, 0);
        start.x = 0;
        start.y = 0;
        end.x = 0;
        end.y = 0;

    }

    public String getSelectedCoordAsString() {
        return selection.getBounds().x + ","
                + selection.getBounds().y + ","
                + selection.getBounds().width + ","
                + selection.getBounds().height;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            drag = false;
            click.x = e.getX();
            click.y = e.getY();
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            drag = true;
            start.x = click.x;
            start.y = click.y;
            end.x = e.getX();
            end.y = e.getY();
            drawSelection(com.getGraphics());
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e) && isvalid() && drag) {
            end.x = e.getX();
            end.y = e.getY();
            drag = false;
        }

    }

    private void removeSelection() {
        com.repaint();
    }

    public boolean isvalid() {
        return Math.abs(start.x - end.x) > MIN_SIZE && Math.abs(start.y - end.y) > MIN_SIZE;
    }

    public void drawSelection() {
        drawSelection(com.getGraphics());
    }

    private void drawSelection(Graphics g) {
        com.paintImmediately(com.getVisibleRect());
        Graphics2D g2d = (Graphics2D) g;
        drawSelection(g2d);
        g2d.dispose();
    }

    private void drawSelection(Graphics2D g2d) {
        selection.setFrameFromDiagonal(start, end);
        Rectangle select = this.selection.getBounds();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_OFF);

        g2d.setStroke(new BasicStroke(1.8f));
        g2d.setColor(Color.RED);
        g2d.drawRect(select.x, select.y, select.width, select.height);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

}

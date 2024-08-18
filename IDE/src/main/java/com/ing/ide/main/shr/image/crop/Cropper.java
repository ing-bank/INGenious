
package com.ing.ide.main.shr.image.crop;

import com.ing.ide.util.Canvas;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;

/**
 *
 *
 */
public class Cropper extends JWindow implements MouseListener, MouseMotionListener {

    private static final long serialVersionUID = 29051411;
    private Cursor normal, crop;
    private final CropUIController uiController;
    BufferedImage screen;
    private Point click;
    Point start, end;
    private boolean drag, rcDisArm = true;
    private CropPanel editor;
    private final int minSize = 4;
    public Point offsetMark, offset;

    public Cropper(CropUIController ctrl) {
        uiController = ctrl;
        init();
        initListeners();
    }

    private void init() {
        normal = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
        crop = getCropCursor();
        start = new Point(0, 0);
        end = new Point(0, 0);
        click = new Point(0, 0);
        offsetMark = new Point(0, 0);
        offset = new Point(0, 0);
        setLayout(null);
        this.setIgnoreRepaint(true);
        editor = new CropPanel(this);
        add(editor);
    }

    private void initListeners() {

        addMouseMotionListener(this);
        addMouseListener(this);
    }

    public void hideCropper() {
        setCursor(normal);
        clearPoints();
        super.setVisible(false);
        editor.repaint();
        uiController.done();
    }

    public void showCropper(BufferedImage screen) {
        this.screen = screen;
        drag = false;
        setSize(getSize());
        setLocationRelativeTo(null);
        setCursor(crop);
        setVisible(true);
        editor.requestFocusInWindow();
    }

    @Override
    public void setVisible(boolean flag) {
        if (flag) {
            setCursor(crop);
            super.setVisible(flag);
            editor.repaint();
        } else {
            hideCropper();
        }
    }

    Image getCropImage() {
        Rectangle r = editor.selection.getBounds();
        return screen.getSubimage(r.x, r.y, r.width, r.height);
    }

    @Override
    public Dimension getSize() {
        return Toolkit.getDefaultToolkit().getScreenSize();
    }

    private Cursor getCropCursor() {
        return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    }

    void setRCDisArm(boolean disArmEnable) {
        rcDisArm = disArmEnable;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            setOffsetMarker(e);
            drag = false;
            click.x = e.getX();
            click.y = e.getY();
        } else if (SwingUtilities.isRightMouseButton(e) && rcDisArm) {
            hideCropper();
        }

    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            if (isvalid() && drag) {
                end.x = e.getX();
                end.y = e.getY();
                uiController.previewImage(getCropImage());
                offset.x = 0;
                offset.y = 0;
            }
            drag = false;
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            drag = true;
            start.x = click.x;
            start.y = click.y;
            if (start.y < 20 || e.getY() < 20) {
                uiController.getFrame().setVisible(false);
            }
            end.x = e.getX();
            end.y = e.getY();
            editor.doCroping();
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (e.getY() > 20) {
            if (!uiController.getFrame().isVisible()) {
                uiController.getFrame().setVisible(true);
            }
        }
    }

    boolean isvalid() {
        return Math.abs(start.x - end.x) > minSize && Math.abs(start.y - end.y) > minSize;
    }

    private void clearPoints() {
        start.x = 0;
        start.y = 0;
        end.x = 0;
        end.y = 0;
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    private void setOffsetMarker(MouseEvent e) {
        if (rcDisArm && isvalid()) {
            offsetMark.x = e.getX();
            offsetMark.y = e.getY();
            offset.x = (int) (e.getX() - editor.selection.getCenterX());
            offset.y = (int) (e.getY() - editor.selection.getCenterY());
            editor.doCroping();
            paintOffsetmarker(editor.getGraphics());
        }
    }

    private void paintOffsetmarker(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        Canvas.paintOffset(g2d, offsetMark);
    }

    public Rectangle getSelection() {
        return editor.selection.getBounds();
    }

}

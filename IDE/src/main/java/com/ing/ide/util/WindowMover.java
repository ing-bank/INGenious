
package com.ing.ide.util;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.HashMap;
import java.util.Map;

/**
 * The WindowMover allows you to resize a component by dragging a border of the
 * component.
 */
public class WindowMover extends MouseAdapter {

    private final Component window;
    private final Component com;
    private final Point sp = new Point(0, 0);
    private final Point dl = new Point(0, 0);
    private Point loc = new Point(0, 0);
    private MouseAdapter ma;
    private MouseMotionAdapter mma;
    private static final Map<Component, WindowMover> REGISTRY = new HashMap<>();
    public static int MOVE_VERTICAL = -1,
            MOVE_HORIZONDAL = 1,
            MOVE_BOTH = 0;

    WindowMover(Component parent, Component component, int movePolicy) {
        window = parent;
        com = component;
        loc = component.getLocation();
        init(movePolicy);
        addListeners(com);

    }

    public static void register(Component parent, Component component, int movePolicy) {
        REGISTRY.put(component, new WindowMover(parent, component, movePolicy));
    }

    public static void deregister(Component component) {
        if (REGISTRY.containsKey(component)) {
            WindowMover obj = REGISTRY.get(component);
            if (obj != null) {
                obj.removeListeners(obj.window);
                obj = null;
            }

        }
    }

    void init() {
        ma = new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                sp.x = e.getX();
                sp.y = e.getY();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 || e.isControlDown()) {
                    window.setLocation(loc);
                }
            }

        };

    }

    void init(int movePolicy) {
        init();
        if (movePolicy == WindowMover.MOVE_VERTICAL) {
            mma = new MouseMotionAdapter() {

                @Override
                public void mouseDragged(MouseEvent e) {
                    int cY = window.getLocation().y;
                    int yMoved = (cY + e.getY()) - (cY + sp.y);
                    int y = cY + yMoved;
                    setLocation(window.getLocation().x, y);
                }
            };
        } else if (movePolicy == WindowMover.MOVE_HORIZONDAL) {
            mma = new MouseMotionAdapter() {

                @Override
                public void mouseDragged(MouseEvent e) {
                    int cX = window.getLocation().x;
                    int xMoved = (cX + e.getX()) - (cX + sp.x);
                    int x = cX + xMoved;
                    setLocation(x, window.getLocation().y);

                }
            };
        } else {
            mma = new MouseMotionAdapter() {

                @Override
                public void mouseDragged(MouseEvent e) {
                    int cX = window.getLocation().x;
                    int cY = window.getLocation().y;
                    int xMoved = (cX + e.getX()) - (cX + sp.x);
                    int yMoved = (cY + e.getY()) - (cY + sp.y);
                    int x = cX + xMoved;
                    int y = cY + yMoved;
                    setLocation(x, y);
                }
            };
        }
    }

    void setLocation(int x, int y) {
        Rectangle screen = Canvas.Window.screen;
        Point wmin = new Point(x + 2, y + 2), wmax = new Point(x + window.getWidth() - 2, y + window.getHeight() - 2);
        if (screen.contains(wmin) && screen.contains(wmax)) {
            window.setLocation(x, y);
        }
    }

    private void addListeners(Component com) {

        com.addMouseListener(ma);
        com.addMouseMotionListener(mma);

    }

    private void removeListeners(Component com) {
        com.removeMouseListener(ma);
        com.removeMouseMotionListener(mma);

    }

}

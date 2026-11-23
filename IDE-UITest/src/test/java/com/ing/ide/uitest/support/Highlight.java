package com.ing.ide.uitest.support;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import org.assertj.swing.edt.GuiActionRunner;

/**
 * An on-screen area to box in a screenshot, with a caption describing what is checked there.
 */
public final class Highlight {
    private final Rectangle screenBounds;
    private final String label;

    private Highlight(final Rectangle screenBounds, final String label) {
        this.screenBounds = new Rectangle(screenBounds);
        this.label = label;
    }

    /**
     * @param screenBounds area in screen coordinates; an empty area shows only the caption
     * @param label caption, e.g. what the assertion expects
     * @return the highlight
     */
    public static Highlight onScreen(final Rectangle screenBounds, final String label) {
        return new Highlight(screenBounds, label);
    }

    /**
     * Highlights a Swing component.
     *
     * @param component the component, must be showing
     * @param label caption, e.g. what the assertion expects
     * @return the highlight
     */
    public static Highlight component(final Component component, final String label) {
        return new Highlight(
            GuiActionRunner.execute(
                () -> {
                    final Point location = component.getLocationOnScreen();
                    return new Rectangle(
                        location.x,
                        location.y,
                        component.getWidth(),
                        component.getHeight()
                    );
                }
            ),
            label
        );
    }

    /**
     * A caption without a box, for checks that have nothing on screen to point at (e.g. files on disk).
     *
     * @param label caption
     * @return the highlight
     */
    public static Highlight captionOnly(final String label) {
        return new Highlight(new Rectangle(), label);
    }

    /**
     * Highlights a JavaFX node, e.g. a menu, a menu item or a toolbar button.
     *
     * @param node the node, must be on screen
     * @param label caption, e.g. what the assertion expects
     * @return the highlight; an empty area (caption only) if the node is not on screen
     */
    public static Highlight fxNode(final javafx.scene.Node node, final String label) {
        final Rectangle bounds = FxNodes.screenBounds(node);
        return new Highlight(bounds == null ? new Rectangle() : bounds, label);
    }

    /**
     * @return area in screen coordinates
     */
    public Rectangle screenBounds() {
        return new Rectangle(screenBounds);
    }

    /**
     * @return caption
     */
    public String label() {
        return label;
    }
}

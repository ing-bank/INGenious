package com.ing.ide.e2e.support;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Labeled;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.stage.Window;
import org.testfx.util.WaitForAsyncUtils;

/**
 * JavaFX queries used by drivers and page objects. Every query runs on the JavaFX thread.
 */
public final class FxNodes {
    private static final long QUERY_TIMEOUT_SECONDS = 10;

    private FxNodes() {}

    /**
     * Runs a query on the JavaFX thread and waits for its result.
     *
     * @param query the query
     * @param <T> result type
     * @return the result
     * @throws IllegalStateException if the query fails or times out
     */
    public static <T> T onFx(final Callable<T> query) {
        try {
            return WaitForAsyncUtils.asyncFx(query).get(QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while querying JavaFX", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException("JavaFX query failed", e);
        }
    }

    /**
     * Finds the clickable title of a menu in a menu bar, e.g. "File".
     *
     * @param menu the menu
     * @return the menu bar button, if the menu bar is showing
     */
    public static Optional<Node> menuTitle(final Menu menu) {
        return onFx(
            () ->
                Window
                    .getWindows()
                    .stream()
                    .filter(Window::isShowing)
                    .filter(w -> w.getScene() != null)
                    .flatMap(w -> w.getScene().getRoot().lookupAll(".menu-bar .menu").stream())
                    .filter(
                        n -> n instanceof Labeled && menu.getText().equals(((Labeled) n).getText())
                    )
                    .findFirst()
        );
    }

    /**
     * @param item a menu item
     * @return the item's node in its open popup, if the menu is open
     */
    public static Optional<Node> itemNode(final MenuItem item) {
        return onFx(() -> Optional.ofNullable(item.getStyleableNode()).filter(Node::isVisible));
    }

    /**
     * @param item a menu item
     * @return the keyboard shortcut text shown next to the item, empty if none is shown
     */
    public static String shownShortcut(final MenuItem item) {
        return onFx(
            () -> {
                final Node node = item.getStyleableNode();
                if (node == null) {
                    return "";
                }
                final Node label = node.lookup(".accelerator-text");
                return (
                        label instanceof Labeled &&
                        label.isVisible() &&
                        ((Labeled) label).getText() != null
                    )
                    ? ((Labeled) label).getText().trim()
                    : "";
            }
        );
    }

    /**
     * @param node a node
     * @return its bounds on screen, or {@code null} if it is not on screen
     */
    public static Rectangle screenBounds(final Node node) {
        return onFx(
            () -> {
                final Bounds b = node.localToScreen(node.getBoundsInLocal());
                return b == null
                    ? null
                    : new Rectangle(
                        (int) b.getMinX(),
                        (int) b.getMinY(),
                        (int) b.getWidth(),
                        (int) b.getHeight()
                    );
            }
        );
    }

    /**
     * @param node a node
     * @return its center on screen, or {@code null} if it is not on screen
     */
    public static Point center(final Node node) {
        final Rectangle b = screenBounds(node);
        return b == null ? null : new Point((int) b.getCenterX(), (int) b.getCenterY());
    }
}

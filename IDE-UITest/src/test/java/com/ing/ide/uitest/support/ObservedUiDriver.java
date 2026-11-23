package com.ing.ide.uitest.support;

import java.awt.AWTException;
import java.awt.Component;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Robot;
import java.awt.Window;
import java.io.File;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Labeled;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputControl;
import javax.swing.AbstractButton;
import javax.swing.JFileChooser;
import javax.swing.JTextField;
import org.assertj.swing.edt.GuiActionRunner;
import org.testfx.util.WaitForAsyncUtils;

/**
 * Moves the pointer to each action's target before delegating the action, and reports every step
 * to an {@link ActionObserver}.
 * <p>
 * In robot mode the real mouse glides to the target (eased, in {@link #GLIDE_STEPS} steps), like a
 * user's hand, instead of jumping there; direct mode glides a virtual pointer from the previous
 * action's position. Either way the observer sees the same path, so recordings show the movement.
 */
final class ObservedUiDriver implements UiDriver {
    /** Steps per glide; with one recorded frame per step this is about 0.7 s of video at 12 fps. */
    static final int GLIDE_STEPS = 8;

    private static final long ROBOT_STEP_DELAY_MS = 20;
    private static final long FX_QUERY_TIMEOUT_SECONDS = 10;
    private static final int CLOSE_BUTTON_OFFSET = 20;

    private final UiDriver delegate;
    private final ActionObserver observer;
    private final Robot mouse;
    private Point virtualPointer;

    /**
     * @param delegate performs the actions
     * @param observer follows the actions
     * @param mode robot mode moves the real mouse, direct mode only a virtual pointer
     */
    ObservedUiDriver(final UiDriver delegate, final ActionObserver observer, final InputMode mode) {
        this.delegate = delegate;
        this.observer = observer;
        this.mouse = mode == InputMode.ROBOT ? createRobot() : null;
    }

    @Override
    public void click(final Node node) {
        perform(center(node), "Click " + onFx(() -> describe(node)), () -> delegate.click(node));
    }

    @Override
    public void type(final TextInputControl field, final String text) {
        perform(center(field), "Type \"" + text + "\"", () -> delegate.type(field, text));
    }

    @Override
    public void click(final AbstractButton button) {
        final Point target = GuiActionRunner.execute(
            () -> {
                final Point location = button.getLocationOnScreen();
                return new Point(
                    location.x + button.getWidth() / 2,
                    location.y + button.getHeight() / 2
                );
            }
        );
        perform(
            target,
            "Click \"" + GuiActionRunner.execute(button::getText) + "\"",
            () -> delegate.click(button)
        );
    }

    @Override
    public void requestClose(final Window window) {
        perform(
            GuiActionRunner.execute(() -> closeButton(window)),
            "Close window",
            () -> delegate.requestClose(window)
        );
    }

    @Override
    public void openMenu(final Menu menu) {
        perform(
            FxNodes.menuTitle(menu).map(FxNodes::center).orElse(null),
            "Open menu \"" + menu.getText() + "\"",
            () -> delegate.openMenu(menu)
        );
    }

    @Override
    public void chooseMenuItem(final MenuItem item) {
        perform(
            FxNodes.itemNode(item).map(FxNodes::center).orElse(null),
            "Click \"" + item.getText() + "\"",
            () -> delegate.chooseMenuItem(item)
        );
    }

    @Override
    public void clickOutside(final Menu menu, final Component outside) {
        perform(
            swingCenter(outside),
            "Click outside the menu",
            () -> delegate.clickOutside(menu, outside)
        );
    }

    @Override
    public void hover(final Component component) {
        perform(swingCenter(component), "Hover", () -> delegate.hover(component));
    }

    @Override
    public void typeAndEnter(final JTextField field, final String text) {
        perform(
            swingCenter(field),
            "Type \"" + text + "\" + Enter",
            () -> delegate.typeAndEnter(field, text)
        );
    }

    @Override
    public void chooseFile(final JFileChooser chooser, final File file) {
        perform(
            swingCenter(chooser),
            "Choose \"" + file.getName() + "\"",
            () -> delegate.chooseFile(chooser, file)
        );
    }

    private static Point swingCenter(final Component component) {
        return GuiActionRunner.execute(
            () -> {
                if (!component.isShowing()) {
                    return null;
                }
                final Point location = component.getLocationOnScreen();
                return new Point(
                    location.x + component.getWidth() / 2,
                    location.y + component.getHeight() / 2
                );
            }
        );
    }

    private void perform(final Point target, final String description, final Runnable action) {
        observer.actionStarting();
        try {
            if (target != null) {
                glideTo(target, description);
            }
            observer.beforeAction(target, description);
            action.run();
        } finally {
            observer.actionFinished();
        }
    }

    private void glideTo(final Point target, final String description) {
        final Point start = currentPointer();
        if (start == null || start.equals(target)) {
            moveTo(target, description);
            return;
        }
        for (int step = 1; step <= GLIDE_STEPS; step++) {
            final double t = (double) step / GLIDE_STEPS;
            final double eased = t * t * (3 - 2 * t);
            moveTo(
                new Point(
                    (int) Math.round(start.x + (target.x - start.x) * eased),
                    (int) Math.round(start.y + (target.y - start.y) * eased)
                ),
                description
            );
        }
    }

    private void moveTo(final Point point, final String description) {
        if (mouse != null) {
            mouse.mouseMove(point.x, point.y);
            mouse.delay((int) ROBOT_STEP_DELAY_MS);
        }
        virtualPointer = point;
        observer.pointerMoved(point, description);
    }

    private Point currentPointer() {
        if (mouse == null) {
            return virtualPointer;
        }
        final PointerInfo info = MouseInfo.getPointerInfo();
        return info == null ? null : info.getLocation();
    }

    private static Robot createRobot() {
        try {
            return new Robot();
        } catch (AWTException e) {
            throw new IllegalStateException("Cannot control the mouse for robot mode", e);
        }
    }

    private static Point center(final Node node) {
        return onFx(
            () -> {
                final Bounds bounds = node.localToScreen(node.getBoundsInLocal());
                return bounds == null
                    ? null
                    : new Point((int) bounds.getCenterX(), (int) bounds.getCenterY());
            }
        );
    }

    private static String describe(final Node node) {
        if (node instanceof Labeled && ((Labeled) node).getText() != null) {
            return "\"" + ((Labeled) node).getText() + "\"";
        }
        return node.getClass().getSimpleName();
    }

    // Close button position in the native title bar: left on macOS, right elsewhere.
    private static Point closeButton(final Window window) {
        final int titleBarHeight = window.getInsets().top;
        if (titleBarHeight <= 0) {
            return null;
        }
        final Point location = window.getLocationOnScreen();
        final boolean mac = System
            .getProperty("os.name", "")
            .toLowerCase(Locale.ROOT)
            .contains("mac");
        final int x = mac
            ? location.x + CLOSE_BUTTON_OFFSET
            : location.x + window.getWidth() - CLOSE_BUTTON_OFFSET;
        return new Point(x, location.y + titleBarHeight / 2);
    }

    private static <T> T onFx(final Callable<T> query) {
        try {
            return WaitForAsyncUtils.asyncFx(query).get(FX_QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException | TimeoutException e) {
            return null;
        }
    }

    @Override
    public void activate(final Window window, final Component neutralSpot) {
        perform(
            swingCenter(neutralSpot),
            "Click into the window",
            () -> delegate.activate(window, neutralSpot)
        );
    }
}

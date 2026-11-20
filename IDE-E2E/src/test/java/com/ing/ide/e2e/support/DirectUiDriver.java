package com.ing.ide.e2e.support;

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputControl;
import javax.swing.AbstractButton;
import javax.swing.JFileChooser;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.assertj.swing.core.Robot;
import org.testfx.util.WaitForAsyncUtils;

/**
 * Drives the IDE by calling its controls on their UI threads. The OS mouse and keyboard are never
 * used, so tests run silently next to other work. Actions are posted asynchronously, like real
 * input, so an action that opens a modal dialog does not block the test thread.
 */
final class DirectUiDriver implements UiDriver {
    private final Robot swing;

    DirectUiDriver(final Robot swing) {
        this.swing = swing;
    }

    @Override
    public void click(final Node node) {
        if (!(node instanceof ButtonBase)) {
            throw new IllegalArgumentException("Direct mode can only click buttons, got " + node);
        }
        onFx(((ButtonBase) node)::fire);
    }

    @Override
    public void type(final TextInputControl field, final String text) {
        onFx(
            () -> {
                field.requestFocus();
                field.appendText(text);
            }
        );
    }

    @Override
    public void click(final AbstractButton button) {
        SwingUtilities.invokeLater(button::doClick);
        swing.waitForIdle();
    }

    @Override
    public void requestClose(final Window window) {
        Toolkit
            .getDefaultToolkit()
            .getSystemEventQueue()
            .postEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING));
        swing.waitForIdle();
    }

    private static void onFx(final Runnable action) {
        Platform.runLater(action);
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Override
    public void openMenu(final Menu menu) {
        onFx(menu::show);
    }

    // A real click hides the menu, then runs the item's action
    @Override
    public void chooseMenuItem(final MenuItem item) {
        onFx(
            () -> {
                if (item.getParentMenu() != null) {
                    item.getParentMenu().hide();
                }
                item.fire();
            }
        );
        swing.waitForIdle();
    }

    // There is no real click in direct mode: hide the menu like JavaFX's auto-hide does
    @Override
    public void clickOutside(final Menu menu, final Component outside) {
        onFx(menu::hide);
    }

    @Override
    public void hover(final Component component) {
        Toolkit
            .getDefaultToolkit()
            .getSystemEventQueue()
            .postEvent(
                new MouseEvent(
                    component,
                    MouseEvent.MOUSE_ENTERED,
                    System.currentTimeMillis(),
                    0,
                    component.getWidth() / 2,
                    component.getHeight() / 2,
                    0,
                    false
                )
            );
        swing.waitForIdle();
    }

    @Override
    public void typeAndEnter(final JTextField field, final String text) {
        SwingUtilities.invokeLater(
            () -> {
                field.requestFocusInWindow();
                field.setText(text);
                field.postActionEvent();
            }
        );
        swing.waitForIdle();
    }

    @Override
    public void chooseFile(final JFileChooser chooser, final File file) {
        SwingUtilities.invokeLater(
            () -> {
                chooser.setSelectedFile(file);
                chooser.approveSelection();
            }
        );
        swing.waitForIdle();
    }

    // Direct input does not depend on which window is active
    @Override
    public void activate(final Window window, final Component neutralSpot) {
        SwingUtilities.invokeLater(window::toFront);
        swing.waitForIdle();
    }
}

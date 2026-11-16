package com.ing.ide.e2e.support;

import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.WindowEvent;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.TextInputControl;
import javax.swing.AbstractButton;
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
}

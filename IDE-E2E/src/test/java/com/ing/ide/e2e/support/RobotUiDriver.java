package com.ing.ide.e2e.support;

import java.awt.Window;
import javafx.scene.Node;
import javafx.scene.control.TextInputControl;
import javax.swing.AbstractButton;
import org.assertj.swing.core.Robot;
import org.testfx.api.FxRobot;
import org.testfx.util.WaitForAsyncUtils;

/**
 * Drives the IDE with the real OS mouse and keyboard: AssertJ Swing for Swing, TestFX for JavaFX.
 */
final class RobotUiDriver implements UiDriver {
    private final Robot swing;
    private final FxRobot fx;

    RobotUiDriver(final Robot swing, final FxRobot fx) {
        this.swing = swing;
        this.fx = fx;
    }

    @Override
    public void click(final Node node) {
        fx.clickOn(node);
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Override
    public void type(final TextInputControl field, final String text) {
        fx.clickOn(field).write(text);
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Override
    public void click(final AbstractButton button) {
        swing.click(button);
    }

    @Override
    public void requestClose(final Window window) {
        swing.close(window);
    }
}

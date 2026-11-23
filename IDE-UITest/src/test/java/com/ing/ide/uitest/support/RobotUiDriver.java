package com.ing.ide.uitest.support;

import java.awt.Component;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.io.File;
import javafx.scene.Node;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputControl;
import javax.swing.AbstractButton;
import javax.swing.JFileChooser;
import javax.swing.JTextField;
import org.assertj.swing.core.Robot;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.JFileChooserFixture;
import org.assertj.swing.fixture.JTextComponentFixture;
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

    @Override
    public void openMenu(final Menu menu) {
        click(
            FxNodes
                .menuTitle(menu)
                .orElseThrow(
                    () -> new IllegalStateException("Menu not on screen: " + menu.getText())
                )
        );
    }

    @Override
    public void chooseMenuItem(final MenuItem item) {
        click(
            FxNodes
                .itemNode(item)
                .orElseThrow(
                    () -> new IllegalStateException("Menu item not shown: " + item.getText())
                )
        );
    }

    @Override
    public void clickOutside(final Menu menu, final Component outside) {
        swing.click(outside);
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Override
    public void hover(final Component component) {
        swing.moveMouse(component);
        swing.waitForIdle();
    }

    @Override
    public void typeAndEnter(final JTextField field, final String text) {
        new JTextComponentFixture(swing, field).enterText(text);
        swing.pressAndReleaseKeys(KeyEvent.VK_ENTER);
    }

    // AssertJ selects the file through the chooser's API, then clicks the approve button for real
    @Override
    public void chooseFile(final JFileChooser chooser, final File file) {
        new JFileChooserFixture(swing, chooser).selectFile(file).approve();
    }

    @Override
    public void activate(final Window window, final Component neutralSpot) {
        // Always click: on macOS Java reports the window active even when the app is not frontmost,
        // and the first click into a background app only brings it forward (covering new popups)
        swing.click(neutralSpot);
        Waits.until("window to become active", () -> GuiActionRunner.execute(window::isActive));
        swing.waitForIdle();
    }
}

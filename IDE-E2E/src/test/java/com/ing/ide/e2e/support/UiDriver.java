package com.ing.ide.e2e.support;

import java.awt.Window;
import javafx.scene.Node;
import javafx.scene.control.TextInputControl;
import javax.swing.AbstractButton;

/**
 * User actions used by page objects. Implementations decide whether the action is performed with
 * the real mouse and keyboard ({@link RobotUiDriver}) or by calling the control ({@link DirectUiDriver}),
 * so page objects and tests are identical in both {@link InputMode}s.
 */
public interface UiDriver {
    /**
     * Clicks a JavaFX control.
     *
     * @param node the control
     */
    void click(Node node);

    /**
     * Types text into a JavaFX text control.
     *
     * @param field the control
     * @param text the text to enter
     */
    void type(TextInputControl field, String text);

    /**
     * Clicks a Swing button.
     *
     * @param button the button
     */
    void click(AbstractButton button);

    /**
     * Asks a window to close, like the title-bar close button. Returns without waiting, since
     * closing may open a modal confirmation dialog.
     *
     * @param window the window
     */
    void requestClose(Window window);
}

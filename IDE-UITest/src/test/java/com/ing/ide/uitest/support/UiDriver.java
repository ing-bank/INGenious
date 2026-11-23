package com.ing.ide.uitest.support;

import java.awt.Component;
import java.awt.Window;
import java.io.File;
import javafx.scene.Node;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputControl;
import javax.swing.AbstractButton;
import javax.swing.JFileChooser;
import javax.swing.JTextField;

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

    /**
     * Opens a menu of a JavaFX menu bar, like clicking its title.
     *
     * @param menu the menu
     */
    void openMenu(Menu menu);

    /**
     * Chooses an item of an open JavaFX menu, like clicking it; the menu closes.
     *
     * @param item the item
     */
    void chooseMenuItem(MenuItem item);

    /**
     * Closes an open JavaFX menu by clicking outside it, on the given component.
     *
     * @param menu the open menu
     * @param outside a component outside the menu to click on
     */
    void clickOutside(Menu menu, Component outside);

    /**
     * Moves the pointer onto a Swing component without clicking.
     *
     * @param component the component
     */
    void hover(Component component);

    /**
     * Types text into a Swing text field and presses Enter.
     *
     * @param field the field
     * @param text the text to enter
     */
    void typeAndEnter(JTextField field, String text);

    /**
     * Selects a file or folder in an open file chooser and confirms it.
     *
     * @param chooser the chooser
     * @param file the file or folder to choose
     */
    void chooseFile(JFileChooser chooser, File file);

    /**
     * Makes a window the active one, as a user clicking into it. In robot mode the first click into
     * an inactive app only activates it (macOS), so tests do this before their first step.
     *
     * @param window the window
     * @param neutralSpot a component inside the window where a click has no effect
     */
    void activate(Window window, Component neutralSpot);
}

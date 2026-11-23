package com.ing.ide.uitest.pages;

import com.ing.ide.uitest.support.FxNodes;
import com.ing.ide.uitest.support.Highlight;
import com.ing.ide.uitest.support.UiDriver;
import com.ing.ide.uitest.support.Waits;
import java.awt.Component;
import javafx.scene.Node;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

/**
 * Page object for one menu of the IDE's JavaFX menu bar, e.g. "File" or "Window".
 */
public final class MenuPage {
    private final Menu menu;
    private final UiDriver driver;

    MenuPage(final Menu menu, final UiDriver driver) {
        this.menu = menu;
        this.driver = driver;
    }

    /**
     * Opens the menu and waits until its dropdown is shown.
     *
     * @return this page
     */
    public MenuPage open() {
        driver.openMenu(menu);
        Waits.until("menu \"" + menu.getText() + "\" to open", this::itemsShown);
        return this;
    }

    /**
     * @return whether the dropdown is open and its items are on screen
     */
    public boolean isOpen() {
        return FxNodes.onFx(menu::isShowing) && itemsShown();
    }

    /**
     * Waits until the dropdown has closed.
     */
    public void waitUntilClosed() {
        Waits.until("menu \"" + menu.getText() + "\" to close", () -> !isOpen());
    }

    /**
     * @param text the item's text, e.g. "New Project"
     * @return the item
     * @throws AssertionError if the menu has no such item
     */
    public MenuItem item(final String text) {
        return FxNodes.onFx(
            () ->
                menu
                    .getItems()
                    .stream()
                    .filter(i -> text.equals(i.getText()))
                    .findFirst()
                    .orElseThrow(
                        () ->
                            new AssertionError(
                                "Menu \"" + menu.getText() + "\" has no item \"" + text + "\""
                            )
                    )
        );
    }

    /**
     * @param text the item's text
     * @return whether the item is shown in the open dropdown and enabled
     */
    public boolean isVisibleAndEnabled(final String text) {
        final MenuItem item = item(text);
        return (
            FxNodes.itemNode(item).isPresent() &&
            FxNodes.onFx(() -> item.isVisible() && !item.isDisable())
        );
    }

    /**
     * @param text the item's text
     * @return the keyboard shortcut shown next to the item in the open dropdown, empty if none
     */
    public String shownShortcut(final String text) {
        return FxNodes.shownShortcut(item(text));
    }

    /**
     * Clicks an item of the open dropdown.
     *
     * @param text the item's text
     */
    public void choose(final String text) {
        driver.chooseMenuItem(item(text));
    }

    /**
     * Closes the open dropdown by clicking outside it.
     *
     * @param outside a component outside the menu
     */
    public void clickOutside(final Component outside) {
        driver.clickOutside(menu, outside);
    }

    /**
     * @param label caption
     * @return highlight of the whole open dropdown
     */
    public Highlight dropdown(final String label) {
        return Highlight.fxNode(firstItemNode().getScene().getRoot(), label);
    }

    /**
     * @param text the item's text
     * @param label caption
     * @return highlight of one item of the open dropdown
     */
    public Highlight itemHighlight(final String text, final String label) {
        return Highlight.fxNode(FxNodes.itemNode(item(text)).orElseThrow(), label);
    }

    private boolean itemsShown() {
        return FxNodes.onFx(
            () ->
                menu
                    .getItems()
                    .stream()
                    .anyMatch(
                        i ->
                            !(i instanceof SeparatorMenuItem) &&
                            i.getStyleableNode() != null &&
                            i.getStyleableNode().getScene() != null &&
                            i.getStyleableNode().getScene().getWindow() != null &&
                            i.getStyleableNode().getScene().getWindow().isShowing()
                    )
        );
    }

    private Node firstItemNode() {
        return FxNodes.onFx(
            () ->
                menu
                    .getItems()
                    .stream()
                    .filter(i -> !(i instanceof SeparatorMenuItem))
                    .map(MenuItem::getStyleableNode)
                    .filter(n -> n != null && n.getScene() != null)
                    .findFirst()
                    .orElseThrow(
                        () -> new AssertionError("Menu \"" + menu.getText() + "\" is not open")
                    )
        );
    }
}

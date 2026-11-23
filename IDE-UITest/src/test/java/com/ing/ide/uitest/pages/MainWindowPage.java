package com.ing.ide.uitest.pages;

import com.ing.ide.main.fx.FXMenuBar;
import com.ing.ide.main.fx.FXStatusBar;
import com.ing.ide.main.fx.FXToolBar;
import com.ing.ide.main.mainui.AppMainFrame;
import com.ing.ide.main.utils.toasterNotification.ToasterDialog;
import com.ing.ide.uitest.support.FxNodes;
import com.ing.ide.uitest.support.Highlight;
import com.ing.ide.uitest.support.UiDriver;
import com.ing.ide.uitest.support.Waits;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import javafx.scene.Node;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javax.swing.AbstractButton;
import javax.swing.JSplitPane;
import javax.swing.text.JTextComponent;
import org.assertj.swing.core.Robot;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;

/**
 * Page object for the Swing main window ({@code AppMainFrame}).
 */
public final class MainWindowPage {
    private final FrameFixture frame;
    private final Robot robot;
    private final UiDriver driver;

    /**
     * @param frame the main window fixture
     * @param robot the AssertJ Swing robot, used to find windows and components
     * @param driver performs the user input
     */
    public MainWindowPage(final FrameFixture frame, final Robot robot, final UiDriver driver) {
        this.frame = frame;
        this.robot = robot;
        this.driver = driver;
    }

    /**
     * @return the window title as currently shown in the title bar
     */
    public String title() {
        return GuiActionRunner.execute(() -> frame.target().getTitle());
    }

    /**
     * Marks the title bar for an annotated screenshot.
     *
     * @param label what is checked in the title bar
     * @return the title bar area; empty when the window has no native title bar (e.g. no window manager)
     */
    public Highlight titleBar(final String label) {
        final Rectangle bounds = GuiActionRunner.execute(
            () -> {
                final Point location = frame.target().getLocationOnScreen();
                return new Rectangle(
                    location.x,
                    location.y,
                    frame.target().getWidth(),
                    frame.target().getInsets().top
                );
            }
        );
        return Highlight.onScreen(bounds, label);
    }

    /**
     * Closes the window like clicking the title-bar close button, then answers the quit
     * confirmation with "Don't Save", and waits until the window is gone.
     */
    public void quitWithoutSaving() {
        driver.requestClose(frame.target());
        QuitDialogPage.waitFor(robot, driver).dontSave();
        waitUntilClosed();
    }

    /**
     * @return whether the main window is still on screen
     */
    public boolean isShowing() {
        return GuiActionRunner.execute(() -> frame.target().isShowing());
    }

    /**
     * @return the main window as the IDE class
     */
    public AppMainFrame frame() {
        return (AppMainFrame) frame.target();
    }

    /**
     * Waits until the JavaFX menu bar and toolbar have replaced the startup chrome.
     *
     * @return this page
     */
    public MainWindowPage waitUntilReady() {
        Waits.until(
            "menu bar and toolbar to be ready",
            () -> fxMenuBar().isPresent() && fxToolBar().isPresent()
        );
        return this;
    }

    /**
     * Clicks into the main window (on the status bar) so it is the active window.
     *
     * @return this page
     */
    public MainWindowPage activate() {
        driver.activate(frame.target(), statusBar());
        return this;
    }

    /**
     * @param name the menu's title, e.g. "File" or "Window"
     * @return the menu of the menu bar
     */
    public MenuPage menu(final String name) {
        final FXMenuBar bar = fxMenuBar()
            .orElseThrow(() -> new AssertionError("Menu bar not shown"));
        final Menu menu = FxNodes.onFx(
            () -> {
                final MenuBar menuBar = (MenuBar) bar.getScene().getRoot().lookup(".menu-bar");
                return menuBar
                    .getMenus()
                    .stream()
                    .filter(m -> name.equals(m.getText()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No menu \"" + name + "\""));
            }
        );
        return new MenuPage(menu, driver);
    }

    /**
     * @param tooltip the button's tooltip, e.g. "New Project"
     * @return the toolbar button
     */
    public ButtonBase toolbarButton(final String tooltip) {
        final FXToolBar bar = fxToolBar()
            .orElseThrow(() -> new AssertionError("Toolbar not shown"));
        return FxNodes.onFx(
            () ->
                bar
                    .getScene()
                    .getRoot()
                    .lookupAll(".button")
                    .stream()
                    .filter(n -> n instanceof ButtonBase)
                    .map(n -> (ButtonBase) n)
                    .filter(b -> b.getTooltip() != null && tooltip.equals(b.getTooltip().getText()))
                    .findFirst()
                    .orElseThrow(
                        () ->
                            new AssertionError("No toolbar button with tooltip \"" + tooltip + "\"")
                    )
        );
    }

    /**
     * @param button a toolbar button
     * @return whether it is on screen and enabled
     */
    public boolean isVisibleAndEnabled(final Node button) {
        return (
            FxNodes.onFx(() -> button.isVisible() && !button.isDisabled()) &&
            FxNodes.screenBounds(button) != null
        );
    }

    /**
     * Clicks a toolbar button.
     *
     * @param button the button
     */
    public void click(final Node button) {
        driver.click(button);
    }

    /**
     * @return the view currently shown
     */
    public String currentView() {
        return Waits.swing(() -> frame().getCurrentSlide());
    }

    /**
     * Waits until a view is shown, including its slide-in animation.
     *
     * @param view the view
     */
    public void waitForView(final View view) {
        Waits.until("view " + view + " to be shown", () -> isShowing(view));
    }

    /**
     * @param view a view
     * @return whether it is the current view, its content is on screen and its switch animation ended
     */
    public boolean isShowing(final View view) {
        return Waits.swing(
            () -> {
                final Component content = view.content(frame());
                if (!view.slide().equals(frame().getCurrentSlide()) || !content.isShowing()) {
                    return false;
                }
                // A switch animates for ~0.4 s with two slides visible; it is complete when only
                // the target slide is left. (The IDE does not cancel a running switch animation,
                // so a second switch started too early can end on the wrong slide.)
                return (
                    Arrays
                        .stream(content.getParent().getComponents())
                        .filter(Component::isVisible)
                        .count() ==
                    1
                );
            }
        );
    }

    /**
     * Switches views through the IDE's own API. For preconditions only, not for the step under test.
     *
     * @param view the view to show
     */
    public void arrangeView(final View view) {
        Waits.swing(
            () -> {
                switch (view) {
                    case TEST_DESIGN:
                        frame().showTestDesign();
                        break;
                    case TEST_EXECUTION:
                        frame().showTestExecution();
                        break;
                    case DASHBOARD:
                        frame().showDashBoard();
                        break;
                    default:
                        frame().showAPITester();
                        break;
                }
                return null;
            }
        );
        waitForView(view);
    }

    /**
     * @param view a view
     * @param label caption
     * @return highlight of the view's content
     */
    public Highlight viewContent(final View view, final String label) {
        return Highlight.component(Waits.swing(() -> view.content(frame())), label);
    }

    /**
     * @return the folder of the open project
     */
    public Path projectLocation() {
        return Paths.get(Waits.swing(() -> frame().getProject().getLocation()));
    }

    /**
     * @return the outer split pane of the Test Design view (left part vs object repository)
     */
    public JSplitPane testDesignSplitPane() {
        return Waits.swing(
            () ->
                Arrays
                    .stream(((Container) View.TEST_DESIGN.content(frame())).getComponents())
                    .filter(c -> c instanceof JSplitPane)
                    .map(c -> (JSplitPane) c)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Test Design has no split pane"))
        );
    }

    /**
     * @return the thin strip on the left edge of the window that reveals the view selector on hover
     */
    public Component leftEdge() {
        return Waits.swing(
            () ->
                ((BorderLayout) frame().getContentPane().getLayout()).getLayoutComponent(
                        BorderLayout.WEST
                    )
        );
    }

    /**
     * Moves the pointer onto a component without clicking.
     *
     * @param component the component
     */
    public void hover(final Component component) {
        driver.hover(component);
    }

    /**
     * @param text the selector button's text, e.g. "API Workbench"
     * @return the button of the side view selector, once the selector is shown
     */
    public AbstractButton viewSelectorButton(final String text) {
        Waits.until(
            "side view selector to show",
            () -> Waits.swing(() -> frame().getGlassPane().isShowing())
        );
        return Waits.swing(
            () ->
                find((Container) frame().getGlassPane(), AbstractButton.class)
                    .stream()
                    .filter(b -> b.isShowing() && textOf(b).contains(text))
                    .findFirst()
                    .orElseThrow(
                        () ->
                            new AssertionError("Side view selector has no \"" + text + "\" button")
                    )
        );
    }

    /**
     * Clicks a Swing button.
     *
     * @param button the button
     */
    public void click(final AbstractButton button) {
        driver.click(button);
    }

    /**
     * @return whether the side view selector is shown
     */
    public boolean isViewSelectorShowing() {
        return Waits.swing(() -> frame().getGlassPane().isShowing());
    }

    /**
     * @return the status bar, a safe place to click on
     */
    public Component statusBar() {
        return Waits.swing(
            () ->
                find(frame().getContentPane(), FXStatusBar.class)
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Status bar not shown"))
        );
    }

    /**
     * Waits for a toast notification containing the text.
     *
     * @param text expected text
     * @return the notification window
     */
    public Window waitForNotification(final String text) {
        final Window[] found = new Window[1];
        Waits.until(
            "notification \"" + text + "\"",
            () ->
                Waits.swing(
                    () -> {
                        for (final Window w : Window.getWindows()) {
                            if (
                                w instanceof ToasterDialog &&
                                w.isShowing() &&
                                find((Container) w, JTextComponent.class)
                                    .stream()
                                    .anyMatch(t -> t.getText().contains(text))
                            ) {
                                found[0] = w;
                                return true;
                            }
                        }
                        return false;
                    }
                )
        );
        return found[0];
    }

    /**
     * Waits until the main window has closed.
     */
    public void waitUntilClosed() {
        Waits.until("main window to close", () -> !isShowing());
    }

    private Optional<FXMenuBar> fxMenuBar() {
        return Waits.swing(
            () ->
                find(frame().getContentPane(), FXMenuBar.class)
                    .stream()
                    .filter(Component::isShowing)
                    .findFirst()
        );
    }

    private Optional<FXToolBar> fxToolBar() {
        return Waits.swing(
            () ->
                find(frame().getContentPane(), FXToolBar.class)
                    .stream()
                    .filter(Component::isShowing)
                    .findFirst()
        );
    }

    private static String textOf(final AbstractButton button) {
        final StringBuilder text = new StringBuilder(
            button.getText() == null ? "" : button.getText()
        );
        find(button, javax.swing.JLabel.class).forEach(l -> text.append(' ').append(l.getText()));
        return text.toString();
    }

    private static <T extends Component> java.util.List<T> find(
        final Container root,
        final Class<T> type
    ) {
        final java.util.List<T> found = new java.util.ArrayList<>();
        for (final Component c : root.getComponents()) {
            if (type.isInstance(c)) {
                found.add(type.cast(c));
            }
            if (c instanceof Container) {
                found.addAll(find((Container) c, type));
            }
        }
        return found;
    }
}

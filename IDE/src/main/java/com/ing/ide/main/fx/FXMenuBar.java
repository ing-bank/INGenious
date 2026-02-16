package com.ing.ide.main.fx;

import com.ing.ide.main.mainui.AppActionListener;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.VBox;
import javax.swing.SwingUtilities;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JavaFX-based MenuBar wrapped in a JFXPanel for embedding in Swing.
 * Provides CSS-styled modern menus with the same actions as the Swing AppMenuBar.
 * <p>
 * Actions are bridged to the existing AppActionListener via SwingUtilities.invokeLater().
 */
public class FXMenuBar extends JFXPanel {

    private static final Logger LOG = Logger.getLogger(FXMenuBar.class.getName());

    private final AppActionListener actionListener;
    private MenuBar menuBar;
    private CheckMenuItem darkModeItem;
    private CheckMenuItem multiEnvItem;

    public FXMenuBar(AppActionListener actionListener) {
        this.actionListener = actionListener;
        // Build scene synchronously: block until FX thread has set the scene.
        // Prevents macOS NSTrackingRectTag crash when JFXPanel is resized
        // before its Glass view tracking rects are initialised.
        CountDownLatch sceneReady = new CountDownLatch(1);
        Platform.runLater(() -> {
            initFX();
            sceneReady.countDown();
        });
        try {
            sceneReady.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void initFX() {
        menuBar = new MenuBar();
        menuBar.setUseSystemMenuBar(false);

        menuBar.getMenus().addAll(
                createFileMenu(),
                createAutomationMenu(),
                createTestDataMenu(),
                createConfigurationMenu(),
                createToolsMenu(),
                createWindowMenu(),
                createHelpMenu()
        );

        // Set colored icons on top-level menus
        setMenuGraphic(menuBar.getMenus().get(0), "FileMenu", 14);
        setMenuGraphic(menuBar.getMenus().get(1), "AutomationMenu", 14);
        setMenuGraphic(menuBar.getMenus().get(2), "TestDataMenu", 14);
        setMenuGraphic(menuBar.getMenus().get(3), "ConfigurationsMenu", 14);
        setMenuGraphic(menuBar.getMenus().get(4), "ToolsMenu", 14);
        setMenuGraphic(menuBar.getMenus().get(5), "WindowMenu", 14);
        setMenuGraphic(menuBar.getMenus().get(6), "HelpMenu", 14);

        VBox root = new VBox(menuBar);
        root.getStyleClass().add("light-theme");

        Scene scene = new Scene(root);
        FXTheme.registerScene(scene);
        setScene(scene);
    }

    private void setMenuGraphic(Menu menu, String iconName, int size) {
        org.kordamp.ikonli.javafx.FontIcon icon = INGIcons.fxColored(iconName, size);
        if (icon != null) menu.setGraphic(icon);
    }

    // ── File Menu ──

    private Menu createFileMenu() {
        Menu file = new Menu("File");
        file.getItems().addAll(
                menuItem("New Project", "NewProject", KeyCode.N, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN),
                menuItem("Open Project", "OpenProject", KeyCode.O, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN),
                menuItem("Save Project", "SaveProject", KeyCode.S, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN),
                new SeparatorMenuItem(),
                menuItem("Restart", "refresh"),
                menuItem("Quit", "close", KeyCode.X, KeyCombination.ALT_DOWN)
        );
        return file;
    }

    // ── Automation Menu ──

    private Menu createAutomationMenu() {
        Menu automation = new Menu("Automation");
        automation.getItems().addAll(
                menuItem("Object Spy", "objectSpy", KeyCode.P, KeyCombination.SHORTCUT_DOWN, KeyCombination.ALT_DOWN),
                menuItem("Object Heal", "objectHeal", KeyCode.H, KeyCombination.SHORTCUT_DOWN, KeyCombination.ALT_DOWN),
                new SeparatorMenuItem(),
                menuItem("Image Spy", "imageSpy", KeyCode.I, KeyCombination.SHORTCUT_DOWN, KeyCombination.ALT_DOWN),
                menuItem("Mobile Spy", "mobileSpy", KeyCode.M, KeyCombination.SHORTCUT_DOWN, KeyCombination.ALT_DOWN)
        );
        return automation;
    }

    // ── Test Data Menu ──

    private Menu createTestDataMenu() {
        Menu testData = new Menu("Test Data");
        testData.getItems().add(menuItem("Import TestData", "Inject"));

        multiEnvItem = new CheckMenuItem("Multiple Environment");
        multiEnvItem.setOnAction(e -> fireSwingAction("Multiple Environment"));
        testData.getItems().add(multiEnvItem);

        return testData;
    }

    // ── Configurations Menu ──

    private Menu createConfigurationMenu() {
        Menu config = new Menu("Configurations");
        config.getItems().addAll(
                menuItem("Run Settings", "settings", KeyCode.S, KeyCombination.SHORTCUT_DOWN, KeyCombination.ALT_DOWN),
                menuItem("Browser Configuration", "BrowserConfiguration"),
                new SeparatorMenuItem(),
                menuItem("Options", "settings")
        );

        darkModeItem = new CheckMenuItem("Dark Mode");
        darkModeItem.setOnAction(e -> fireSwingAction("Dark Mode"));
        config.getItems().add(darkModeItem);

        return config;
    }

    // ── Tools Menu ──

    private Menu createToolsMenu() {
        Menu tools = new Menu("Tools");
        tools.getItems().add(
                menuItem("Exploratory", "explorer", KeyCode.E, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN));

        Menu bdd = new Menu("BDD");
        bdd.getItems().addAll(
                menuItem("Import Feature File", "Inject"),
                menuItem("Open Feature Editor", "testdesign")
        );
        tools.getItems().add(bdd);

        Menu playwright = new Menu("Import Playwright Recording");
        playwright.getItems().add(menuItem("Import Playwright Recording", "recorder"));
        tools.getItems().add(playwright);

        tools.getItems().add(
                menuItem("Har Compare", "search", KeyCode.H, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN));

        return tools;
    }

    // ── Window Menu ──

    private Menu createWindowMenu() {
        Menu window = new Menu("Window");
        window.getItems().addAll(
                menuItem("Test Design", "testdesign", KeyCode.N, KeyCombination.SHIFT_DOWN, KeyCombination.ALT_DOWN),
                menuItem("Test Execution", "testexecution", KeyCode.E, KeyCombination.SHIFT_DOWN, KeyCombination.ALT_DOWN),
                menuItem("Dashboard", "dashboard", KeyCode.D, KeyCombination.SHIFT_DOWN, KeyCombination.ALT_DOWN),
                menuItem("API Tester", "api", KeyCode.T, KeyCombination.SHIFT_DOWN, KeyCombination.ALT_DOWN),
                menuItem("AdjustUI", "settings", KeyCode.A, KeyCombination.SHIFT_DOWN, KeyCombination.ALT_DOWN)
        );
        return window;
    }

    // ── Help Menu ──

    private Menu createHelpMenu() {
        Menu help = new Menu("Help");
        help.getItems().addAll(
                menuItem("Help", "help", KeyCode.F1),
                menuItem("About", "info", KeyCode.F3),
                menuItem("Show Log", "console", KeyCode.F9)
        );
        return help;
    }

    // ── Factory Methods ──

    private MenuItem menuItem(String text, String iconName, KeyCode key, KeyCombination.Modifier... modifiers) {
        MenuItem item = new MenuItem(text);
        if (iconName != null) {
            org.kordamp.ikonli.javafx.FontIcon icon = INGIcons.fxColored(iconName, 14);
            if (icon != null) {
                item.setGraphic(icon);
            }
        }
        if (key != null) {
            if (modifiers.length > 0) {
                item.setAccelerator(new KeyCodeCombination(key, modifiers));
            } else {
                item.setAccelerator(new KeyCodeCombination(key));
            }
        }
        item.setOnAction(e -> fireSwingAction(text));
        return item;
    }

    private MenuItem menuItem(String text, String iconName) {
        return menuItem(text, iconName, null);
    }

    private MenuItem menuItem(String text) {
        return menuItem(text, (String) null, null);
    }

    // ── Swing Bridge ──

    /**
     * Fires a Swing ActionEvent on the EDT with the given command string.
     * This bridges JavaFX menu clicks to the existing AppActionListener.
     */
    private void fireSwingAction(String command) {
        SwingUtilities.invokeLater(() -> {
            java.awt.event.ActionEvent swingEvent = new java.awt.event.ActionEvent(
                    this, java.awt.event.ActionEvent.ACTION_PERFORMED, command);
            actionListener.actionPerformed(swingEvent);
        });
    }

    // ── Public API ──

    /**
     * Syncs the Multiple Environment checkbox state.
     * Called from AppMainFrame.afterProjectChange().
     */
    public void setMultiEnvironment(boolean selected) {
        Platform.runLater(() -> {
            if (multiEnvItem != null) {
                multiEnvItem.setSelected(selected);
            }
        });
    }
}

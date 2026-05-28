package com.ing.ide.main.fx;

import com.ing.ide.main.mainui.AppActionListener;
import com.ing.ide.main.utils.recentItem.RecentItem;
import com.ing.ide.util.Notification;
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
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.List;
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
    private Menu recentProjectsMenu;

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
        registerSwingAccelerators();
    }

    private void initFX() {
        menuBar = new MenuBar();
        menuBar.setUseSystemMenuBar(false);

        menuBar.getMenus().addAll(
                createFileMenu(),
                createTestDataMenu(),
                createConfigurationMenu(),
                createToolsMenu(),
                createWindowMenu(),
                createHelpMenu()
        );

        // Set colored icons on top-level menus
        setMenuGraphic(menuBar.getMenus().get(0), "FileMenu", 14);
        setMenuGraphic(menuBar.getMenus().get(1), "TestDataMenu", 14);
        setMenuGraphic(menuBar.getMenus().get(2), "ConfigurationsMenu", 14);
        setMenuGraphic(menuBar.getMenus().get(3), "ToolsMenu", 14);
        setMenuGraphic(menuBar.getMenus().get(4), "WindowMenu", 14);
        setMenuGraphic(menuBar.getMenus().get(5), "HelpMenu", 14);

        VBox root = new VBox(menuBar);
        root.getStyleClass().add("light-theme");

        Scene scene = new Scene(root);
        FXTheme.registerScene(scene);
        setScene(scene);
    }

    private void registerSwingAccelerators() {
        setFocusable(true);
        int shortcutMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        bindAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, shortcutMask | InputEvent.SHIFT_DOWN_MASK), "New Project");
        bindAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, shortcutMask | InputEvent.SHIFT_DOWN_MASK), "Open Project");
        bindAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, shortcutMask | InputEvent.SHIFT_DOWN_MASK), "Save Project");
        bindAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.ALT_DOWN_MASK), "Quit");

        bindAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, shortcutMask | InputEvent.ALT_DOWN_MASK), "Object Spy");
        bindAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, shortcutMask | InputEvent.ALT_DOWN_MASK), "Object Heal");
        bindAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, shortcutMask | InputEvent.ALT_DOWN_MASK), "Image Spy");
        bindAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, shortcutMask | InputEvent.ALT_DOWN_MASK), "Mobile Spy");
        bindAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, shortcutMask | InputEvent.ALT_DOWN_MASK), "Run Settings");

        bindAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, shortcutMask | InputEvent.SHIFT_DOWN_MASK), "Exploratory");
        bindAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, shortcutMask | InputEvent.SHIFT_DOWN_MASK), "Har Compare");

        bindAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.SHIFT_DOWN_MASK | InputEvent.ALT_DOWN_MASK), "Test Design");
        bindAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.SHIFT_DOWN_MASK | InputEvent.ALT_DOWN_MASK), "Test Execution");
        bindAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.SHIFT_DOWN_MASK | InputEvent.ALT_DOWN_MASK), "Dashboard");
        bindAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.SHIFT_DOWN_MASK | InputEvent.ALT_DOWN_MASK), "API Tester");
        bindAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.SHIFT_DOWN_MASK | InputEvent.ALT_DOWN_MASK), "AdjustUI");

        bindAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), "Help");
        bindAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), "About");
        bindAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0), "Show Log");
    }

    private void bindAccelerator(KeyStroke keyStroke, String command) {
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, command);
        getActionMap().put(command, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fireSwingAction(command);
            }
        });
    }

    private void setMenuGraphic(Menu menu, String iconName, int size) {
        org.kordamp.ikonli.javafx.FontIcon icon = INGIcons.fxColored(iconName, size);
        if (icon != null) menu.setGraphic(icon);
    }

    // ── File Menu ──

    private Menu createFileMenu() {
        Menu file = new Menu("File");
        
        // Create Recent Projects submenu
        recentProjectsMenu = new Menu("Recent Projects");
        updateRecentProjectsMenu(null); // Initialize as empty
        
        file.getItems().addAll(
                menuItem("New Project", "NewProject", KeyCode.N, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN),
                menuItem("Open Project", "OpenProject", KeyCode.O, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN),
                menuItem("Save Project", "SaveProject", KeyCode.S, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN),
                new SeparatorMenuItem(),
                recentProjectsMenu,
                new SeparatorMenuItem(),
                menuItem("Restart", "refresh"),
                menuItem("Quit", "close", KeyCode.X, KeyCombination.ALT_DOWN)
        );
        return file;
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

        // darkModeItem = new CheckMenuItem("Dark Mode");
        // darkModeItem.setOnAction(e -> fireSwingAction("Dark Mode"));
        // config.getItems().add(darkModeItem);

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

        Menu sapRecording = new Menu("Import SAP Recording");
        sapRecording.getItems().addAll(
                createSapImportItem("Java (.java, .jsh)", "recorder", "Java")
        );
        tools.getItems().add(sapRecording);

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
                menuItem("API Workbench", "APITester", KeyCode.T, KeyCombination.SHIFT_DOWN, KeyCombination.ALT_DOWN),
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

    /**
     * Creates a menu item for SAP import with language-specific action command.
     */
    private MenuItem createSapImportItem(String text, String iconName, String language) {
        MenuItem item = new MenuItem(text);
        if (iconName != null) {
            org.kordamp.ikonli.javafx.FontIcon icon = INGIcons.fxColored(iconName, 14);
            if (icon != null) {
                item.setGraphic(icon);
            }
        }
        // Use format: "Import SAP Recording:Language"
        item.setOnAction(e -> fireSwingAction("Import SAP Recording:" + language));
        return item;
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

    /**
     * Updates the Recent Projects menu with the current list of recent items.
     * Called from AppMainFrame after a project is loaded.
     */
    public void updateRecentProjects(List<RecentItem> recentItems) {
        Platform.runLater(() -> updateRecentProjectsMenu(recentItems));
    }

    /**
     * Internal method to populate the Recent Projects menu on the JavaFX thread.
     */
    /**
     * Internal method to populate the Recent Projects menu on the JavaFX thread.
     */
    private void updateRecentProjectsMenu(List<RecentItem> recentItems) {
        if (recentProjectsMenu == null) {
            return;
        }
        
        recentProjectsMenu.getItems().clear();
        
        if (recentItems == null || recentItems.isEmpty()) {
            MenuItem emptyItem = new MenuItem("(No recent projects)");
            emptyItem.setDisable(true);
            recentProjectsMenu.getItems().add(emptyItem);
            return;
        }
        
        for (RecentItem item : recentItems) {
            String projectName = item.getProjectName();
            String location = item.getLocation();
            
            MenuItem projectItem = new MenuItem(projectName);
            
            // Add action to load the project with validation
            projectItem.setOnAction(e -> {
                // Validate that the project path exists
                if (!new File(location).exists()) {
                    SwingUtilities.invokeLater(() -> {
                        Notification.show("Project path no longer exists: " + location);
                        actionListener.getMainFrame().getRecentItems().removeItemByLocation(location);
                        actionListener.getMainFrame().getRecentItems().save();
                        updateRecentProjects(actionListener.getMainFrame().getRecentItems().getRECENT_ITEMS());
                    });
                    return;
                }
                
                SwingUtilities.invokeLater(() -> {
                    actionListener.getMainFrame().loadProject(location);
                });
            });
            
            recentProjectsMenu.getItems().add(projectItem);
        }
    }

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

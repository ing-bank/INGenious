package com.ing.ide.main.fx;

import com.ing.ide.main.Main;
import com.ing.ide.main.mainui.AppActionListener;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javax.swing.SwingUtilities;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * JavaFX-based ToolBar wrapped in a JFXPanel for embedding in Swing.
 * Provides CSS-styled modern toolbar buttons with the same actions as AppToolBar.
 * Icons are rendered via Ikonli web font icons (INGIcons).
 */
public class FXToolBar extends JFXPanel {

    private static final Logger LOG = Logger.getLogger(FXToolBar.class.getName());

    private final AppActionListener actionListener;
    private ToggleButton autoSaveToggle;
    private ToggleButton darkModeToggle;
    private javafx.scene.control.ToolBar toolBar;

    public FXToolBar(AppActionListener actionListener) {
        this.actionListener = actionListener;
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
        toolBar = new javafx.scene.control.ToolBar();

        toolBar.getItems().addAll(
                createButton("New Project", "NewProject"),
                createButton("Open Project", "OpenProject"),
                new Separator(),
                createButton("Save Project", "SaveProject"),
                new Separator(),
                createAutoSaveSection(),
                new Separator(),
                createButton("Run Settings", "RunSettings"),
                createButton("Browser Configuration", "BrowserConfiguration"),
                new Separator(),
                createAPITesterButton(),
                createSpacer(),
                createDarkModeToggle()
        );

        VBox root = new VBox(toolBar);
        root.getStyleClass().add("light-theme");

        Scene scene = new Scene(root);
        FXTheme.registerScene(scene);
        setScene(scene);
    }

    private Button createButton(String action, String iconName) {
        Button btn = new Button();
        btn.setTooltip(new Tooltip(action));

        // Use colorful Ikonli web font icon
        org.kordamp.ikonli.javafx.FontIcon icon = INGIcons.fxColored(iconName, 18);
        if (icon != null) {
            btn.setGraphic(icon);
        } else {
            btn.setText(action);
        }

        btn.setOnAction(e -> fireSwingAction(action));
        return btn;
    }

    private Button createAPITesterButton() {
        Button btn = new Button("API Tester");
        btn.getStyleClass().add("api-tester-btn");
        btn.setTooltip(new Tooltip("Open API Testing Console - Test REST APIs like Postman"));
        
        // API icon with vibrant cyan color - use registered icon from INGIcons
        org.kordamp.ikonli.javafx.FontIcon icon = INGIcons.fxColored("APITester", 16);
        if (icon != null) {
            icon.setIconColor(javafx.scene.paint.Color.WHITE);
            btn.setGraphic(icon);
        }
        
        btn.setOnAction(e -> fireSwingAction("API Tester"));
        return btn;
    }

    private HBox createAutoSaveSection() {
        Label label = new Label("Auto Save");
        label.getStyleClass().add("auto-save-label");

        autoSaveToggle = new ToggleButton("OFF");
        autoSaveToggle.setOnAction(e -> {
            if (autoSaveToggle.isSelected()) {
                autoSaveToggle.setText("ON");
            } else {
                autoSaveToggle.setText("OFF");
            }
            fireSwingAction("Auto Save");
        });

        HBox section = new HBox(6, label, autoSaveToggle);
        section.setAlignment(Pos.CENTER);
        return section;
    }

    private HBox createDarkModeToggle() {
        // Sun icon for light mode, Moon icon for dark mode
        org.kordamp.ikonli.javafx.FontIcon sunIcon = new org.kordamp.ikonli.javafx.FontIcon("fas-sun");
        sunIcon.setIconSize(14);
        sunIcon.setIconColor(javafx.scene.paint.Color.web("#FF6200"));
        
        org.kordamp.ikonli.javafx.FontIcon moonIcon = new org.kordamp.ikonli.javafx.FontIcon("fas-moon");
        moonIcon.setIconSize(14);
        moonIcon.setIconColor(javafx.scene.paint.Color.web("#7724FF"));

        darkModeToggle = new ToggleButton();
        darkModeToggle.getStyleClass().add("dark-mode-pill");
        darkModeToggle.setSelected(Main.isDarkMode());
        updateDarkModeToggle();
        
        darkModeToggle.setOnAction(e -> {
            fireSwingAction("Dark Mode");
            // Small delay to let theme switch, then update toggle appearance
            Platform.runLater(() -> {
                try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                Platform.runLater(this::updateDarkModeToggle);
            });
        });

        HBox section = new HBox(6, darkModeToggle);
        section.setAlignment(Pos.CENTER);
        return section;
    }
    
    private void updateDarkModeToggle() {
        boolean isDark = Main.isDarkMode();
        if (isDark) {
            org.kordamp.ikonli.javafx.FontIcon moonIcon = new org.kordamp.ikonli.javafx.FontIcon("fas-moon");
            moonIcon.setIconSize(14);
            moonIcon.setIconColor(javafx.scene.paint.Color.web("#89D6FD"));
            darkModeToggle.setGraphic(moonIcon);
            darkModeToggle.setText("Dark");
            darkModeToggle.setSelected(true);
        } else {
            org.kordamp.ikonli.javafx.FontIcon sunIcon = new org.kordamp.ikonli.javafx.FontIcon("fas-sun");
            sunIcon.setIconSize(14);
            sunIcon.setIconColor(javafx.scene.paint.Color.web("#FF6200"));
            darkModeToggle.setGraphic(sunIcon);
            darkModeToggle.setText("Light");
            darkModeToggle.setSelected(false);
        }
    }

    private Pane createSpacer() {
        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    /**
     * Bridges JavaFX button clicks to Swing AppActionListener.
     */
    private void fireSwingAction(String command) {
        SwingUtilities.invokeLater(() -> {
            java.awt.event.ActionEvent swingEvent = new java.awt.event.ActionEvent(
                    this, java.awt.event.ActionEvent.ACTION_PERFORMED, command);
            actionListener.actionPerformed(swingEvent);
        });
    }

    /**
     * Returns the auto-save toggle button for external state sync.
     */
    public ToggleButton getAutoSaveToggle() {
        return autoSaveToggle;
    }
}

package com.ing.ide.main.fx;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * JavaFX-based status bar for the bottom of the application frame.
 * Shows current view, project name, and other status indicators.
 */
public class FXStatusBar extends JFXPanel {

    private static final Logger LOG = Logger.getLogger(FXStatusBar.class.getName());

    private Label viewLabel;
    private Label projectLabel;
    private Label statusLabel;
    private HBox container;

    public FXStatusBar() {
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
        container = new HBox();
        container.setAlignment(Pos.CENTER_LEFT);
        container.setSpacing(0);
        container.getStyleClass().add("status-bar");

        viewLabel = new Label("Ready");
        viewLabel.getStyleClass().add("status-view-label");
        viewLabel.getStyleClass().add("status-design");
        // Add a white icon to the view label (matches dock testdesign icon)
        org.kordamp.ikonli.javafx.FontIcon viewIcon = INGIcons.fx("testdesign", 12, javafx.scene.paint.Color.WHITE);
        if (viewIcon != null) {
            viewLabel.setGraphic(viewIcon);
            viewLabel.setGraphicTextGap(5);
        }
        viewLabel.setTextFill(javafx.scene.paint.Color.WHITE);

        Separator sep1 = new Separator(javafx.geometry.Orientation.VERTICAL);

        projectLabel = new Label("");
        projectLabel.getStyleClass().add("status-project-label");
        // Add folder icon to project label
        org.kordamp.ikonli.javafx.FontIcon projIcon = INGIcons.fxColored("OpenProject", 12);
        if (projIcon != null) {
            projectLabel.setGraphic(projIcon);
            projectLabel.setGraphicTextGap(4);
        }

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        statusLabel = new Label("INGenious Playwright Studio");
        statusLabel.getStyleClass().add("status-info-label");
        // Add flask icon to branding label
        org.kordamp.ikonli.javafx.FontIcon brandIcon = INGIcons.fxColored("favicon", 11);
        if (brandIcon != null) {
            statusLabel.setGraphic(brandIcon);
            statusLabel.setGraphicTextGap(4);
        }

        container.getChildren().addAll(viewLabel, sep1, projectLabel, spacer, statusLabel);

        Scene scene = new Scene(container);
        scene.setFill(null);
        FXTheme.registerScene(scene);
        setScene(scene);
    }

    /**
     * Updates the current view indicator.
     */
    public void setCurrentView(String view) {
        Platform.runLater(() -> {
            if (viewLabel != null) {
                viewLabel.setText(view);
                // Apply style based on view
                viewLabel.getStyleClass().removeAll("status-design", "status-execution", "status-dashboard");
                
                // Update icon to match the dock icon for each view
                String iconKey;
                switch (view) {
                    case "Test Design":
                        viewLabel.getStyleClass().add("status-design");
                        iconKey = "testdesign";
                        break;
                    case "Test Execution":
                        viewLabel.getStyleClass().add("status-execution");
                        iconKey = "testexecution";
                        break;
                    case "DashBoard":
                        viewLabel.getStyleClass().add("status-dashboard");
                        iconKey = "dashboard";
                        break;
                    default:
                        iconKey = "testdesign";
                        break;
                }
                
                // Set the icon with white color
                org.kordamp.ikonli.javafx.FontIcon viewIcon = INGIcons.fx(iconKey, 12, javafx.scene.paint.Color.WHITE);
                if (viewIcon != null) {
                    viewLabel.setGraphic(viewIcon);
                    viewLabel.setGraphicTextGap(5);
                }
                
                // Force white text color programmatically
                viewLabel.setTextFill(javafx.scene.paint.Color.WHITE);
            }
        });
    }

    /**
     * Updates the project name display.
     */
    public void setProjectName(String name) {
        Platform.runLater(() -> {
            if (projectLabel != null) {
                projectLabel.setText(name != null && !name.isEmpty() ? name : "No Project");
            }
        });
    }

    /**
     * Updates the status message.
     */
    public void setStatus(String message) {
        Platform.runLater(() -> {
            if (statusLabel != null) {
                statusLabel.setText(message);
            }
        });
    }
}

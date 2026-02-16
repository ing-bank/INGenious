package com.ing.ide.main.fx;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * A reusable JavaFX-styled panel header for embedding in Swing panels.
 * Displays a modern styled label with optional action buttons.
 * Icons are rendered via Ikonli web font icons (INGIcons).
 */
public class FXPanelHeader extends JFXPanel {

    private static final Logger LOG = Logger.getLogger(FXPanelHeader.class.getName());

    private final String title;
    private final HeaderAction[] actions;
    private HBox container;

    /**
     * Creates a panel header with a title and optional action buttons.
     *
     * @param title   The header label text
     * @param actions Optional action buttons to display on the right
     */
    public FXPanelHeader(String title, HeaderAction... actions) {
        this.title = title;
        this.actions = actions;
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
        container.setSpacing(4);
        container.getStyleClass().add("fx-panel-header");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("fx-panel-header-title");

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        container.getChildren().addAll(titleLabel, spacer);

        // Add action buttons
        for (HeaderAction action : actions) {
            Button btn = new Button();
            btn.setTooltip(new Tooltip(action.tooltip));
            btn.getStyleClass().add("fx-panel-header-btn");

            if (action.iconName != null) {
                org.kordamp.ikonli.javafx.FontIcon icon = INGIcons.fxColored(action.iconName, 14);
                if (icon != null) {
                    btn.setGraphic(icon);
                } else {
                    btn.setText(action.tooltip);
                }
            } else {
                btn.setText(action.tooltip);
            }

            btn.setOnAction(e -> {
                if (action.handler != null) {
                    action.handler.run();
                }
            });
            container.getChildren().add(btn);
        }

        Scene scene = new Scene(container);
        scene.setFill(null);
        FXTheme.registerScene(scene);
        setScene(scene);
    }

    /**
     * Represents an action button in the panel header.
     */
    public static class HeaderAction {
        final String tooltip;
        final String iconName;
        final Runnable handler;

        public HeaderAction(String tooltip, String iconName, Runnable handler) {
            this.tooltip = tooltip;
            this.iconName = iconName;
            this.handler = handler;
        }
    }
}

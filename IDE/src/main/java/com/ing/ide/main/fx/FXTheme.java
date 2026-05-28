package com.ing.ide.main.fx;

import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the JavaFX CSS theme for the application.
 * Handles light/dark mode toggling across all registered scenes.
 */
public class FXTheme {

    private static final Logger LOG = Logger.getLogger(FXTheme.class.getName());
    private static final List<Scene> scenes = new ArrayList<>();
    private static boolean isDark = false;

    private FXTheme() {
        // Utility class
    }

    /**
     * Returns the stylesheet URL for the ING theme.
     */
    public static String getStylesheet() {
        return FXTheme.class.getResource("/fx/ing-theme.css").toExternalForm();
    }

    /**
     * Registers a scene for theme management.
     * The current theme will be applied immediately.
     */
    public static void registerScene(Scene scene) {
        scenes.add(scene);
        applyTheme(scene);
    }

    /**
     * Toggles between light and dark themes on all registered scenes.
     * Must be called from any thread — will dispatch to FX thread if needed.
     */
    public static void toggleTheme(boolean dark) {
        isDark = dark;
        if (Platform.isFxApplicationThread()) {
            updateAllScenes();
        } else {
            Platform.runLater(FXTheme::updateAllScenes);
        }
    }

    public static boolean isDark() {
        return isDark;
    }

    private static void updateAllScenes() {
        // Remove closed scenes
        scenes.removeIf(s -> s.getWindow() == null || !s.getWindow().isShowing());
        for (Scene scene : scenes) {
            applyTheme(scene);
        }
    }

    private static void applyTheme(Scene scene) {
        Parent root = scene.getRoot();
        if (root == null) return;

        root.getStyleClass().removeAll("dark-theme", "light-theme");
        root.getStyleClass().add(isDark ? "dark-theme" : "light-theme");

        // Ensure stylesheet is applied
        String css = getStylesheet();
        if (!scene.getStylesheets().contains(css)) {
            scene.getStylesheets().add(css);
        }

        LOG.log(Level.FINE, "Applied {0} theme to scene", isDark ? "dark" : "light");
    }
}

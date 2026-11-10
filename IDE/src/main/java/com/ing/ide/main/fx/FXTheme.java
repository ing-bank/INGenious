package com.ing.ide.main.fx;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;

/**
 * Manages the JavaFX CSS theme for the application.
 * Handles light/dark mode toggling across all registered scenes.
 */
public class FXTheme {
    private static final Logger LOG = Logger.getLogger(FXTheme.class.getName());
    private static final List<Scene> scenes = new ArrayList<>();
    private static boolean isDark = false;
    private static boolean fontsLoaded = false;

    /** Classpath directories searched for the bundled INGMe fonts, in order. */
    private static final String[] FONT_DIRS = { "/ui/resources/fonts/", "/fonts/" };

    private static final String[] FONT_FILES = {
        "ingme_regular.ttf",
        "ingme_italic.ttf",
        "ingme_bold.ttf",
        "ingme_bolditalic.ttf"
    };

    private FXTheme() {
        // Utility class
    }

    /**
     * Registers the bundled ING Me faces with the JavaFX font subsystem.
     *
     * <p>JavaFX maintains its own font registry and does <em>not</em> see faces
     * registered with AWT via {@code GraphicsEnvironment.registerFont}. Without
     * this step the {@code "ING Me"} family referenced by the stylesheet only
     * resolves on machines where the font happens to be installed system-wide;
     * everywhere else JavaFX silently falls back to the generic System font,
     * leaving the FX chrome visually inconsistent with the Swing UI.</p>
     *
     * <p>Idempotent, and safe to call before any scene is styled. Must run on
     * the JavaFX application thread.</p>
     */
    public static void ensureFontsLoaded() {
        if (fontsLoaded) {
            return;
        }
        fontsLoaded = true;
        for (String file : FONT_FILES) {
            loadFont(file);
        }
    }

    private static void loadFont(String fileName) {
        for (String dir : FONT_DIRS) {
            try (java.io.InputStream in = FXTheme.class.getResourceAsStream(dir + fileName)) {
                if (in == null) {
                    continue;
                }
                // Size is irrelevant here; the call registers the face so CSS
                // can resolve it at whatever size each rule asks for.
                if (Font.loadFont(in, 12) != null) {
                    return;
                }
                LOG.log(Level.FINE, "JavaFX rejected bundled font {0}", dir + fileName);
            } catch (Exception ex) {
                LOG.log(Level.FINE, "Unable to load bundled font " + dir + fileName, ex);
            }
        }
        LOG.log(Level.FINE, "Bundled font not found on classpath: {0}", fileName);
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
        ensureFontsLoaded();
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

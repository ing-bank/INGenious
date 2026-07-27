package com.ing.ide.main.mainui.plugins;

import com.ing.engine.plugin.loader.PluginLoader;
import com.ing.ingenious.api.contract.ui.StudioPanelApi;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Discovers plugin-contributed Studio screens ({@link StudioPanelApi}) from the plugins
 * directory and hands them to the UI.
 *
 * <p>Discovery reuses the engine's existing {@link PluginLoader}, so panel plugins are
 * packaged and loaded exactly like action plugins — same folder, same manifest attribute,
 * same child-first class loading.
 *
 * <p>Everything here is defensive on purpose. A third-party plugin must never be able to
 * stop Studio from starting: a missing plugins directory, a class that cannot be
 * instantiated, or a panel whose constructor throws are all logged and skipped.
 */
public final class StudioPanelPlugins {
    /** Action-command prefix used by toolbar buttons to select a plugin screen. */
    public static final String ACTION_PREFIX = "Plugin Panel:";

    private static final Logger LOG = Logger.getLogger(StudioPanelPlugins.class.getName());

    private static List<StudioPanelApi> cached;

    private StudioPanelPlugins() {}

    /**
     * Loads the plugin panels once and caches them, so the frame and the toolbar see the
     * same instances.
     *
     * @return the discovered panels; empty when there are none
     */
    public static synchronized List<StudioPanelApi> load() {
        if (cached != null) {
            return cached;
        }
        List<StudioPanelApi> found = new ArrayList<>();
        List<Class<?>> entryClasses;
        try {
            entryClasses = PluginLoader.loadAllPluginsEntryClasses();
        } catch (RuntimeException ex) {
            // No plugins directory is the normal case for a stock install.
            LOG.log(Level.FINE, "No plugin panels loaded: {0}", ex.getMessage());
            cached = Collections.emptyList();
            return cached;
        }
        for (Class<?> clazz : entryClasses) {
            if (!StudioPanelApi.class.isAssignableFrom(clazz)) {
                continue;
            }
            try {
                StudioPanelApi panel = (StudioPanelApi) clazz
                    .getDeclaredConstructor()
                    .newInstance();
                String title = panel.getTitle();
                if (title == null || title.trim().isEmpty()) {
                    LOG.log(
                        Level.WARNING,
                        "Skipping panel plugin with a blank title: {0}",
                        clazz.getName()
                    );
                    continue;
                }
                found.add(panel);
                LOG.log(Level.INFO, "Loaded Studio panel plugin: {0}", title);
            } catch (ReflectiveOperationException | RuntimeException ex) {
                LOG.log(Level.WARNING, "Cannot instantiate panel plugin " + clazz.getName(), ex);
            }
        }
        cached = Collections.unmodifiableList(found);
        return cached;
    }

    /**
     * Slide identity for a panel. Namespaced so a plugin cannot collide with a built-in
     * screen such as {@code TestDesign}.
     *
     * @param title the panel title
     * @return the slide name
     */
    public static String slideName(String title) {
        return "Plugin:" + title;
    }
}
